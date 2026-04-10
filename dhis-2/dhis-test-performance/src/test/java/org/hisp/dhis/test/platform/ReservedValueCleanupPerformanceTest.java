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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Performance test for the {@code REMOVE_USED_OR_EXPIRED_RESERVED_VALUES} scheduler job.
 *
 * <p>Triggers the job via {@code POST /api/jobConfigurations/{uid}/execute}, then polls {@code GET
 * /api/system/tasks/REMOVE_USED_OR_EXPIRED_RESERVED_VALUES/{uid}} until the job reports {@code
 * completed: true}. Total scenario duration captures end-to-end job execution time.
 *
 * <p>The test is designed for a single iteration: the DB is pre-seeded with 11M reserved values
 * (~8M expired, ~3M valid), which are consumed on the first run. Use {@code WARMUP=0} when
 * comparing baseline vs candidate.
 *
 * <p>Available properties (with defaults):
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file (optional)
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code pollIntervalMs} (default: {@code 1000})
 *   <li>{@code maxPolls} (default: {@code 1800} — 30 min timeout)
 * </ul>
 */
public class ReservedValueCleanupPerformanceTest extends Simulation {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Properties CONFIG = loadConfig();

  private static final String JOB_TYPE = "REMOVE_USED_OR_EXPIRED_RESERVED_VALUES";
  private static final String EXECUTE_REQUEST = "POST JobConfiguration - execute";
  private static final String POLL_REQUEST = "GET Tasks - poll " + JOB_TYPE;

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
      } catch (IOException e) {
        System.err.println(
            "[ReservedValueCleanup] Warning: could not load configFile=" + path + ": " + e.getMessage());
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
  private static final int POLL_INTERVAL_MS = Integer.parseInt(prop("pollIntervalMs", "10000"));
  private static final int MAX_POLLS = Integer.parseInt(prop("maxPolls", "180"));

  private final String jobConfigUid = fetchJobConfigUid();

  private static String fetchJobConfigUid() {
    String auth =
        Base64.getEncoder()
            .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(
                  URI.create(
                      BASE_URL
                          + "/api/jobConfigurations?filter=jobType:eq:"
                          + JOB_TYPE
                          + "&fields=id"))
              .header("Authorization", "Basic " + auth)
              .header("Accept", "application/json")
              .GET()
              .build();
      HttpResponse<String> response =
          client.send(request, HttpResponse.BodyHandlers.ofString());
      JsonNode root = MAPPER.readTree(response.body());
      String uid = root.at("/jobConfigurations/0/id").asText(null);
      if (uid == null || uid.isBlank()) {
        throw new IllegalStateException(
            "No job configuration found for " + JOB_TYPE + ". Response: " + response.body());
      }
      System.out.println("[ReservedValueCleanup] Job config UID: " + uid);
      return uid;
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch job configuration UID for " + JOB_TYPE, e);
    }
  }

  public ReservedValueCleanupPerformanceTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .disableCaching()
            .basicAuth(USERNAME, PASSWORD);

    ScenarioBuilder scenario =
        scenario("Reserved Value Cleanup Job")
            .exec(
                http(EXECUTE_REQUEST)
                    .post("/api/jobConfigurations/" + jobConfigUid + "/execute")
                    .check(status().is(200)))
            .exec(session -> session.set("jobDone", false).set("pollCount", 0))
            .asLongAs(
                session ->
                    !Boolean.TRUE.equals(session.get("jobDone"))
                        && session.getInt("pollCount") < MAX_POLLS)
            .on(
                exec(
                        http(POLL_REQUEST)
                            .get(
                                "/api/system/tasks/"
                                    + JOB_TYPE
                                    + "/"
                                    + jobConfigUid)
                            .check(status().is(200))
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
                                "[ReservedValueCleanup] Job completed after " + count + " poll(s).");
                          } else if (count >= MAX_POLLS) {
                            System.err.println(
                                "[ReservedValueCleanup] Job timed out after "
                                    + count
                                    + " poll(s).");
                          }
                          return session.set("jobDone", done).set("pollCount", count);
                        })
                    .doIf(session -> !Boolean.TRUE.equals(session.get("jobDone")))
                    .then(pause(Duration.ofMillis(POLL_INTERVAL_MS))))
            .exec(
                session ->
                    Boolean.TRUE.equals(session.get("jobDone"))
                        ? session
                        : session.markAsFailed());

    setUp(scenario.injectClosed(rampConcurrentUsers(0).to(1).during(1)))
        .protocols(httpProtocol)
        .assertions(
            details(EXECUTE_REQUEST).responseTime().max().lt(5000),
            details(EXECUTE_REQUEST).successfulRequests().percent().is(100D),
            details(POLL_REQUEST).successfulRequests().percent().is(100D));
  }
}
