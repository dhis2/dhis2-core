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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.StringJoiner;

/**
 * Performance test for the data integrity summary job via {@code POST /api/dataIntegrity/summary}.
 *
 * <p>Mirrors the flow used by the DHIS2 UI:
 *
 * <ol>
 *   <li><b>Cache clear</b> — {@code POST /api/maintenance?cacheClear=true} ensures each iteration
 *       starts with a cold application cache, so results are not skewed by Hibernate's second-level
 *       or query cache from a previous run.
 *   <li><b>Submit</b> — {@code POST /api/dataIntegrity/summary} starts an async job and returns a
 *       job ID in {@code response.id}.
 *   <li><b>Poll</b> — {@code GET /api/system/tasks/DATA_INTEGRITY/{jobId}} is called repeatedly
 *       until an entry with {@code "completed": true} appears in the response array.
 *   <li><b>Fetch results</b> — {@code GET /api/dataIntegrity/summary} retrieves the completed
 *       summary and logs the {@code averageExecutionTime} (ms) reported by each check, sorted
 *       slowest-first.
 * </ol>
 *
 * <p>The test fails if the job does not complete within {@code maxPolls * pollIntervalMs}
 * milliseconds, which is expressed as a 100% success-rate assertion on the submit request.
 *
 * <p>Available properties (with defaults):
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file (optional)
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code iterations} (default: {@code 3})
 *   <li>{@code pollIntervalMs} (default: {@code 200})
 *   <li>{@code maxPolls} (default: {@code 300} — 60 s timeout at 200 ms interval)
 *   <li>{@code maxCheckMs} (default: {@code 100} — max allowed {@code averageExecutionTime} per
 *       check; set low to intentionally trigger failures and see the output)
 * </ul>
 */
public class DataIntegrityPerformanceTest extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println("[DataIntegrityPerformanceTest] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[DataIntegrityPerformanceTest] Warning: could not load configFile="
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
  private static final int ITERATIONS = Integer.parseInt(prop("iterations", "3"));
  private static final int POLL_INTERVAL_MS = Integer.parseInt(prop("pollIntervalMs", "200"));
  // Default: 300 polls × 200 ms = 60 s timeout
  private static final int MAX_POLLS = Integer.parseInt(prop("maxPolls", "300"));
  private static final long MAX_CHECK_MS = Long.parseLong(prop("maxCheckMs", "50"));

  private static final String CACHE_CLEAR_REQUEST = "POST Maintenance - clear application cache";
  private static final String SUBMIT_REQUEST = "POST DataIntegrity - submit summary job";
  private static final String POLL_REQUEST = "GET DataIntegrity - poll job status";
  private static final String RESULTS_REQUEST = "GET DataIntegrity - fetch summary results";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DataIntegrityPerformanceTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .disableCaching()
            .basicAuth(USERNAME, PASSWORD);

    ScenarioBuilder scenario =
        scenario("Data Integrity Summary Job")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(
                // Step 1: clear the application cache so each iteration starts cold
                exec(http(CACHE_CLEAR_REQUEST)
                        .post("/api/maintenance?cacheClear=true")
                        .check(status().is(200)))

                    // Step 2: submit the job and save the job ID
                    .exec(
                        http(SUBMIT_REQUEST)
                            .post("/api/dataIntegrity/summary")
                            .check(status().is(200))
                            .check(jsonPath("$.response.id").saveAs("jobId")))
                    .exec(session -> session.set("jobDone", false).set("pollCount", 0))

                    // Step 3: poll until completed:true or timeout
                    .asLongAs(
                        session ->
                            !Boolean.TRUE.equals(session.get("jobDone"))
                                && session.getInt("pollCount") < MAX_POLLS)
                    .on(
                        exec(http(POLL_REQUEST)
                                .get("/api/system/tasks/DATA_INTEGRITY/#{jobId}")
                                .check(status().is(200))
                                // Selects all array elements where completed == true;
                                // optional so the check does not fail while the job is running.
                                .check(
                                    jsonPath("$[?(@.completed == true)]")
                                        .findAll()
                                        .optional()
                                        .saveAs("completedEvents")))
                            .exec(
                                session -> {
                                  Object events = session.get("completedEvents");
                                  boolean done = events instanceof List<?> list && !list.isEmpty();
                                  int count = session.getInt("pollCount") + 1;
                                  if (done) {
                                    System.out.println(
                                        "[DataIntegrity] Job "
                                            + session.getString("jobId")
                                            + " completed after "
                                            + count
                                            + " poll(s).");
                                  } else if (count >= MAX_POLLS) {
                                    System.err.println(
                                        "[DataIntegrity] Job "
                                            + session.getString("jobId")
                                            + " timed out after "
                                            + count
                                            + " poll(s) — marking iteration failed.");
                                  }
                                  return session.set("jobDone", done).set("pollCount", count);
                                })
                            // Pause after the poll so a job that completes quickly
                            // is detected on the very first attempt with no added delay.
                            .doIf(session -> !Boolean.TRUE.equals(session.get("jobDone")))
                            .then(pause(Duration.ofMillis(POLL_INTERVAL_MS))))

                    // Step 4: fail the iteration if the job never completed
                    .exec(
                        session ->
                            Boolean.TRUE.equals(session.get("jobDone"))
                                ? session
                                : session.markAsFailed())

                    // Step 5: fetch and log per-check execution times from the summary results
                    .doIf(session -> Boolean.TRUE.equals(session.get("jobDone")))
                    .then(
                        exec(http(RESULTS_REQUEST)
                                .get("/api/dataIntegrity/summary")
                                .check(status().is(200))
                                .check(bodyString().saveAs("summaryBody"))
                                .check(
                                    bodyString()
                                        .transform(
                                            body -> {
                                              try {
                                                JsonNode root = OBJECT_MAPPER.readTree(body);
                                                StringJoiner slow = new StringJoiner(", ");
                                                root.fields()
                                                    .forEachRemaining(
                                                        e -> {
                                                          long ms =
                                                              e.getValue()
                                                                  .path("averageExecutionTime")
                                                                  .asLong(-1);
                                                          if (ms > MAX_CHECK_MS)
                                                            slow.add(e.getKey() + "=" + ms + "ms");
                                                        });
                                                return slow.toString();
                                              } catch (Exception ex) {
                                                return "parse error: " + ex.getMessage();
                                              }
                                            })
                                        .is("")))
                            .exec(
                                session -> {
                                  String body = session.getString("summaryBody");
                                  try {
                                    JsonNode root = OBJECT_MAPPER.readTree(body);
                                    List<String[]> timings = new ArrayList<>();
                                    root.fields()
                                        .forEachRemaining(
                                            e -> {
                                              long ms =
                                                  e.getValue()
                                                      .path("averageExecutionTime")
                                                      .asLong(-1);
                                              if (ms >= 0)
                                                timings.add(new String[] {e.getKey(), "" + ms});
                                            });
                                    timings.sort(
                                        Comparator.comparingLong(
                                                (String[] r) -> Long.parseLong(r[1]))
                                            .reversed());
                                    System.out.println(
                                        "[DataIntegrity] Per-check execution times (slowest first):");
                                    for (String[] row : timings) {
                                      System.out.printf(
                                          "[DataIntegrity]   %-60s %s ms%n", row[0], row[1]);
                                    }
                                  } catch (Exception e) {
                                    System.err.println(
                                        "[DataIntegrity] Failed to parse summary results: "
                                            + e.getMessage());
                                  }
                                  return session;
                                })));

    ClosedInjectionStep singleUser = rampConcurrentUsers(0).to(1).during(1);

    setUp(scenario.injectClosed(singleUser))
        .protocols(httpProtocol)
        .assertions(
            // Submit should be quick
            details(SUBMIT_REQUEST).responseTime().percentile(95).lt(1000),
            details(SUBMIT_REQUEST).responseTime().max().lt(2000),
            details(SUBMIT_REQUEST).successfulRequests().percent().is(100D),
            // Poll endpoint itself should always respond quickly
            details(POLL_REQUEST).responseTime().percentile(95).lt(500),
            details(POLL_REQUEST).responseTime().max().lt(1000),
            details(POLL_REQUEST).successfulRequests().percent().is(100D),
            // Results fetch is a read of cached data
            details(RESULTS_REQUEST).responseTime().percentile(95).lt(500),
            details(RESULTS_REQUEST).responseTime().max().lt(1000),
            details(RESULTS_REQUEST).successfulRequests().percent().is(100D));
  }
}
