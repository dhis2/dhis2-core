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
 * Performance test for {@code GET /api/dataEntry/metadata} (DHIS2-fetch-size baseline).
 *
 * <p>The endpoint takes no parameters and returns the full data-entry metadata payload (data sets,
 * data elements, indicators, category combos/categories/options, option sets) via {@code
 * DefaultDataSetMetadataExportService#getDataSetMetadata()}. That service has no server-side result
 * cache — every request rebuilds the payload from the database unless the client sends a matching
 * {@code If-None-Match} header, which this test never does, so every iteration measures a genuine
 * cold fetch.
 *
 * <p>Each virtual user logs in once via {@code POST /api/auth/login} to establish a session, like a
 * typical logged-in browser user (not HTTP Basic Auth, which re-authenticates on every request and
 * would mix login cost into the endpoint measurement). The login step is tracked under a separate
 * "Authentication" group, so the assertions below — scoped to {@code GET DataEntry - metadata} only
 * — measure just the metadata endpoint on an already-authenticated session.
 *
 * <p>Assumes a Sierra Leone demo database (richer metadata than the default dev DB) at {@code
 * localhost:8080} by default.
 *
 * <p>Available properties (with defaults):
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file (optional)
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code iterations} (default: {@code 20}) — requests per virtual user
 *   <li>{@code concurrentUsers} (default: {@code 1}) — virtual users ramped up concurrently; total
 *       requests = {@code concurrentUsers * iterations}
 *   <li>{@code rampDurationSeconds} (default: {@code 1}) — time to ramp from 0 to {@code
 *       concurrentUsers}
 * </ul>
 */
public class DataEntryMetadataPerformanceTest extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println("[DataEntryMetadataPerformanceTest] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[DataEntryMetadataPerformanceTest] Warning: could not load configFile="
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
  private static final int ITERATIONS = Integer.parseInt(prop("iterations", "20"));
  private static final int CONCURRENT_USERS = Integer.parseInt(prop("concurrentUsers", "1"));
  private static final int RAMP_DURATION_SECONDS =
      Integer.parseInt(prop("rampDurationSeconds", "1"));

  private static final String METADATA_REQUEST = "GET DataEntry - metadata";

  public DataEntryMetadataPerformanceTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + "/api/ping")
            .disableCaching();

    // Note: the repeated metadata request below is intentionally NOT wrapped in a `.group(...)`
    // sharing its name. With a single virtual user, Gatling would then only have one sample of
    // the group's *cumulative* duration (summed across all ITERATIONS requests), and
    // `details(METADATA_REQUEST)` would resolve to that one bogus cumulative sample instead of
    // the per-request distribution across all repeats.
    ScenarioBuilder scenario =
        scenario("DataEntry Metadata")
            .group("Authentication")
            .on(exec(loginChain()))
            .repeat(ITERATIONS)
            .on(
                exec(http(METADATA_REQUEST).get("/api/dataEntry/metadata").check(status().is(200)))
                    .pause(1));

    ClosedInjectionStep injection =
        rampConcurrentUsers(0).to(CONCURRENT_USERS).during(RAMP_DURATION_SECONDS);

    // Thresholds calibrated from 3 repeated single-user (default profile) runs on the SL demo DB,
    // combined via gstat: fix/hibernate-fetch-size p95/p99 500/535ms, master p95/p99 585/609ms
    // (worse of the two branches, matching the value this test is meant to guard against
    // regressing on either branch), with headroom for run-to-run variance.
    setUp(scenario.injectClosed(injection))
        .protocols(httpProtocol)
        .assertions(
            details(METADATA_REQUEST).responseTime().percentile(95).lt(800),
            details(METADATA_REQUEST).responseTime().max().lt(900),
            details(METADATA_REQUEST).successfulRequests().percent().is(100D));
  }

  private ChainBuilder loginChain() {
    return exec(
        http("Login")
            .post("/api/auth/login")
            .header("Content-Type", "application/json")
            .body(
                StringBody("{\"username\":\"" + USERNAME + "\",\"password\":\"" + PASSWORD + "\"}"))
            .check(status().is(200)));
  }
}
