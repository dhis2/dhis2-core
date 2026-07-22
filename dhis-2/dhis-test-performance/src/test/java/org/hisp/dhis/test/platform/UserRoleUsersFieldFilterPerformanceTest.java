/*
 * Copyright (c) 2004-2026, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.test.platform;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/**
 * Regression guard for DHIS2-21856: {@code GET /api/userRoles/{id}?fields=users[*]} used to walk
 * into every property of every member user while looking for {@code access}/{@code sharing}
 * segments beneath a {@code @PropertyTransformer}-backed collection ({@code UserRole.users},
 * serialized via {@code UserPropertyTransformer}), which can never contain them -- an N+1 lazy-load
 * storm. On the role used here (~83k members) this was 139.7s / ~583k queries before the fix
 * (DHIS2-21856/#24519, DHIS2-21867/#24514, DHIS2-21872/#24520), 5.76s after (live-validated via
 * Glowroot trace inspection of the actual query breakdown, not just wall-clock timing).
 *
 * <p>Runs three field-spec variants against the same role each iteration:
 *
 * <ol>
 *   <li>no {@code fields} param -- default-view baseline, independent of field-filter behavior
 *   <li>{@code fields=id,name,users} -- bare collection reference, no wildcard expansion
 *   <li>{@code fields=id,name,users[*]} -- wildcard-expanded collection, the exact DHIS2-21856
 *       repro
 * </ol>
 *
 * <p>Post-fix, a {@code @PropertyTransformer}-backed property always serializes the same fixed DTO
 * shape regardless of how it's requested, so (2) and (3) should cost about the same. Asserting on
 * all three turns this into a guard against a specific regression mode: {@code users[*]} drifting
 * back to costing meaningfully more than bare {@code users}, which is what the original bug looked
 * like (wildcard/subtree expansion walking into {@code getUserRoles()} on every member).
 *
 * <p>Targets the platform-perf DB by default.
 *
 * <pre>{@code
 * cd dhis-2/dhis-test-performance && mvn gatling:test \
 *   -Dgatling.simulationClass=org.hisp.dhis.test.platform.UserRoleUsersFieldFilterPerformanceTest
 * }</pre>
 *
 * Available properties:
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file (optional)
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code roleUid} (default: {@code I4GjoZqpkjv} — platform-perf generic role, ~83k members)
 *   <li>{@code iterations} (default: {@code 3})
 * </ul>
 */
public class UserRoleUsersFieldFilterPerformanceTest extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println("[UserRoleUsersFieldFilterPerformanceTest] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[UserRoleUsersFieldFilterPerformanceTest] Warning: could not load configFile="
                + path
                + ": "
                + e.getMessage());
      }
    }
    return props;
  }

  private static String prop(String key, String defaultValue) {
    String sys = System.getProperty(key);
    if (sys != null) return sys;
    String file = CONFIG.getProperty(key);
    return file != null ? file : defaultValue;
  }

  private static final String BASE_URL = prop("baseUrl", "http://localhost:8080");
  private static final String USERNAME = prop("username", "admin");
  private static final String PASSWORD = prop("password", "district");
  private static final String BASIC_AUTH =
      Base64.getEncoder()
          .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
  private static final String ROLE_UID = prop("roleUid", "I4GjoZqpkjv");
  private static final int ITERATIONS = Integer.parseInt(prop("iterations", "3"));

  private static final String DEFAULT_FIELDS_REQUEST = "GET UserRole - default fields";
  private static final String BARE_USERS_REQUEST = "GET UserRole - users (bare)";
  private static final String WILDCARD_USERS_REQUEST = "GET UserRole - users (wildcard)";

  public UserRoleUsersFieldFilterPerformanceTest() {
    // No protocol-level basicAuth. DHIS2 is stateful (SessionCreationPolicy.IF_REQUIRED +
    // HttpSessionSecurityContextRepository), so once a session exists Spring Security reuses the
    // SecurityContext and skips re-authentication; bcrypt password verification (~70ms) is only
    // paid on the FIRST request that establishes the session. Authenticate once per virtual user
    // via a separately-named request instead, so the measured GETs reflect endpoint cost only --
    // same pattern as UsersPerformanceTest and OrganisationUnitUsersFieldFilterPerformanceTest.
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL).acceptHeader("application/json").disableCaching();

    ChainBuilder authenticate =
        exec(flushCookieJar())
            .exec(
                http("Authenticate (session login)")
                    .get("/api/me")
                    .header("Authorization", "Basic " + BASIC_AUTH)
                    .check(status().is(200)));

    ChainBuilder workflow =
        exec(http(DEFAULT_FIELDS_REQUEST).get("/api/userRoles/" + ROLE_UID).check(status().is(200)))
            .exec(
                http(BARE_USERS_REQUEST)
                    .get("/api/userRoles/" + ROLE_UID)
                    .queryParam("fields", "id,name,users")
                    .check(status().is(200)))
            .exec(
                http(WILDCARD_USERS_REQUEST)
                    .get("/api/userRoles/" + ROLE_UID)
                    .queryParam("fields", "id,name,users[*]")
                    .check(status().is(200)));

    ScenarioBuilder scenario =
        scenario("UserRole users field filter (DHIS2-21856)")
            .exec(authenticate)
            .repeat(ITERATIONS)
            .on(workflow);

    ClosedInjectionStep singleUser = rampConcurrentUsers(0).to(1).during(1);

    // Thresholds calibrated from a real baseline-vs-candidate CI run (perf runner, platform-perf
    // DB, 10 iterations, DHIS2-21856, run 29935165075, 2026-07-22): baseline (pre-fix, commit
    // 0cea25cd6a) p95 -- default fields 8,843ms, bare users 9,057ms; wildcard users: all 10/10
    // iterations hit Gatling's 60s HTTP client timeout (KO, zero completions -- the pre-fix N+1
    // storm is documented above, via Glowroot trace inspection, at ~139.7s, well beyond what this
    // client timeout can even measure). Candidate (master with the fix, commit 894a102ac0) p95 --
    // default fields 6,872ms, bare users 6,223ms, wildcard users 6,206ms, 0 KO across all 31
    // requests: post-fix, all three variants cost about the same, as expected. Default/bare
    // thresholds sit with headroom above the candidate p95 and below the clean (non-bug-affected)
    // baseline p95; the wildcard threshold -- the exact DHIS2-21856 repro -- sits far below both
    // the 60s client timeout and the documented pre-fix latency, since the available margin there
    // is enormous. This is a regression guard against the N+1 storm returning, not a performance
    // target.
    setUp(scenario.injectClosed(singleUser))
        .protocols(httpProtocol)
        .assertions(
            details(DEFAULT_FIELDS_REQUEST).responseTime().percentile(95).lt(8_500),
            details(DEFAULT_FIELDS_REQUEST).successfulRequests().percent().is(100D),
            details(BARE_USERS_REQUEST).responseTime().percentile(95).lt(8_700),
            details(BARE_USERS_REQUEST).successfulRequests().percent().is(100D),
            details(WILDCARD_USERS_REQUEST).responseTime().percentile(95).lt(20_000),
            details(WILDCARD_USERS_REQUEST).successfulRequests().percent().is(100D));
  }
}
