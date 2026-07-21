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
import java.util.Properties;

/**
 * Regression guard for DHIS2-21867: {@code FieldPathHelper.visitFieldPath} used to walk into every
 * requested sub-path under a {@code @PropertyTransformer} property (e.g. {@code
 * OrganisationUnit.users}, serialized via {@code UserPropertyTransformer}), invoking {@code
 * getUserRoles()} on every member user while looking for {@code access}/{@code sharing} segments
 * that a transformer-backed subtree can never contain. On an org unit with many users this was an
 * N+1 lazy-load storm (250,054 JDBC queries / ~70s on the platform-perf DB's root org unit, fixed
 * by PR #24514).
 *
 * <p>Targets the platform-perf DB (~250k users) by default, using the same root org unit UID as
 * {@link UsersPerformanceTest}.
 *
 * <pre>{@code
 * mvn gatling:test \
 *   -Dgatling.simulationClass=org.hisp.dhis.test.platform.OrganisationUnitUsersFieldFilterPerformanceTest \
 *   --file dhis-2/pom.xml -pl dhis-test-performance
 * }</pre>
 *
 * Available properties:
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file (optional)
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code orgUnitUid} (default: {@code VCCdfC9pvMA} — platform-perf root org unit)
 *   <li>{@code fields} (default: {@code id,name,users[id,name,userRoles[id,name]]} — the exact
 *       DHIS2-21867 repro)
 *   <li>{@code iterations} (default: {@code 3})
 * </ul>
 */
public class OrganisationUnitUsersFieldFilterPerformanceTest extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println(
            "[OrganisationUnitUsersFieldFilterPerformanceTest] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[OrganisationUnitUsersFieldFilterPerformanceTest] Warning: could not load configFile="
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
  private static final String ORG_UNIT_UID = prop("orgUnitUid", "VCCdfC9pvMA");
  private static final String FIELDS = prop("fields", "id,name,users[id,name,userRoles[id,name]]");
  private static final int ITERATIONS = Integer.parseInt(prop("iterations", "3"));

  private static final String GET_REQUEST = "GET OrganisationUnit - users+userRoles field filter";

  public OrganisationUnitUsersFieldFilterPerformanceTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .disableCaching()
            .basicAuth(USERNAME, PASSWORD);

    ChainBuilder workflow =
        exec(
            http(GET_REQUEST)
                .get("/api/organisationUnits/" + ORG_UNIT_UID)
                .queryParam("fields", FIELDS)
                .check(status().is(200)));

    ScenarioBuilder scenario =
        scenario("OrganisationUnit users/userRoles field filter (DHIS2-21867)")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(workflow);

    ClosedInjectionStep singleUser = rampConcurrentUsers(0).to(1).during(1);

    // Generous ceiling: not a tight latency target (a separate, unfixed bottleneck --
    // OrganisationUnit.users still fully materializes via Hibernate, plus Ehcache
    // putFromLoad's per-entity serialization cost -- keeps this endpoint far from "fast" even
    // post-fix, and its own 2nd-level-cache-warmth-dependent variance is real). This threshold
    // exists only to catch a regression back to the N+1 storm this test guards against, not to
    // enforce a performance target. Calibrated from a real baseline-vs-candidate CI run (perf
    // runner, platform-perf DB, 10 iterations, DHIS2-21867/#24514): master (unfixed) was a flat
    // ~51-52s every request (min 50,773ms / p95 52,212ms); this fix ranged 21,597-42,634ms
    // (p95 42,634ms). 48s sits with headroom above the fixed range and below master's floor.
    setUp(scenario.injectClosed(singleUser))
        .protocols(httpProtocol)
        .assertions(
            details(GET_REQUEST).responseTime().percentile(95).lt(48_000),
            details(GET_REQUEST).successfulRequests().percent().is(100D));
  }
}
