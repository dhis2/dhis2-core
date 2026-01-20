/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.test.tracker;

import static io.gatling.javaapi.core.CoreDsl.RawFileBody;
import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for DHIS2 Tracker import API.
 *
 * <p>This test imports tracker data from a JSON file using POST requests to /api/tracker
 *
 * <p>Place your tracker.json file in src/test/resources/tracker.json
 *
 * <p><b>Profiles:</b>
 *
 * <ul>
 *   <li><b>smoke</b> (default) - Single user with repeat loop for profiling and baseline testing
 *       <br>
 *       Uses: {@code repeat} (default: 10) <br>
 *       Example: {@code -Dprofile=smoke -Drepeat=50}
 *   <li><b>throughput</b> - Closed model with concurrent users to find optimal throughput <br>
 *       Shape: ___/‾‾‾‾‾‾‾‾‾ (ramp up then sustain) <br>
 *       Uses: {@code concurrentUsers} (default: 4), {@code rampDurationSec} (default: 10), {@code
 *       durationSec} (default: 60) <br>
 *       Example: {@code -Dprofile=throughput -DconcurrentUsers=8 -DdurationSec=120}
 *       <p>The throughput profile uses a closed injection model where the specified number of
 *       concurrent users continuously send requests. This helps find the maximum sustainable
 *       throughput while monitoring p95 latency. Gatling reports req/s in the output.
 *       <p>To find optimal concurrency, run multiple tests with increasing {@code concurrentUsers}
 *       (e.g., 1, 2, 4, 8) and observe where p95 latency becomes unacceptable or throughput
 *       plateaus.
 * </ul>
 *
 * <p><b>Common Parameters:</b>
 *
 * <ul>
 *   <li>{@code -Dinstance} - DHIS2 instance URL (default: http://localhost:8080)
 *   <li>{@code -DtrackerFile} - JSON file to import from src/test/resources (default: tracker.json)
 *   <li>{@code -DtrackerArgs} - Additional query parameters for /api/tracker (default:
 *       skipRuleEngine=true). async=false is always set. Example: {@code
 *       -DtrackerArgs="skipRuleEngine=true&skipSideEffects=true"}
 * </ul>
 */
public class TrackerImportTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(TrackerImportTest.class);

  private static final List<Map<String, Object>> userCredentials = new ArrayList<>();

  private final Profile profile;
  private final String instance;
  private final String adminUser;
  private final String adminPassword;
  private final int repeat;
  private final int concurrentUsers;
  private final int durationSec;
  private final int rampDurationSec;
  private final String trackerArgs;
  private final String trackerFile;

  private enum Profile {
    SMOKE,
    THROUGHPUT;

    static Profile fromString(String profile) {
      try {
        return valueOf(profile.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Unknown profile: " + profile + ". Valid options: smoke, throughput");
      }
    }
  }

  public TrackerImportTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");
    this.trackerArgs = System.getProperty("trackerArgs", "skipRuleEngine=true");
    this.trackerFile = System.getProperty("trackerFile", "tracker.json");

    record ProfileDefaults(int repeat, int concurrentUsers, int rampDurationSec, int durationSec) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          case SMOKE -> new ProfileDefaults(10, 1, 1, 1);
          case THROUGHPUT -> new ProfileDefaults(1, 4, 10, 60);
        };
    this.repeat = Integer.getInteger("repeat", defaults.repeat());
    this.concurrentUsers = Integer.getInteger("concurrentUsers", defaults.concurrentUsers());
    this.rampDurationSec = Integer.getInteger("rampDurationSec", defaults.rampDurationSec());
    this.durationSec = Integer.getInteger("durationSec", defaults.durationSec());

    try {
      prepareUser();
      disableUniqueConstraint();
    } catch (Exception e) {
      throw new RuntimeException("Setup failed", e);
    }

    ScenarioBuilder scenarioBuilder =
        scenario("Tracker Import")
            .exec(login())
            .exitHereIfFailed()
            .repeat(this.repeat)
            .on(exec(importTracker()));

    PopulationBuilder populationBuilder = buildPopulation(scenarioBuilder);

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl(this.instance)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/Performance Test")
            .disableFollowRedirect()
            .warmUp(this.instance + "/api/ping")
            .disableCaching()
            .check(status().is(200));

    List<Assertion> assertions =
        List.of(details("Tracker Import").successfulRequests().percent().gte(100d));

    setUp(populationBuilder).protocols(httpProtocolBuilder).assertions(assertions);
  }

  private PopulationBuilder buildPopulation(ScenarioBuilder scenarioBuilder) {
    return switch (this.profile) {
      case SMOKE -> {
        // Single user, repeat N times - good for profiling and baseline
        ClosedInjectionStep injection = constantConcurrentUsers(1).during(1);
        yield scenarioBuilder.injectClosed(injection);
      }
      case THROUGHPUT -> {
        // Ramp up to N concurrent users, then sustain for duration
        // Closed model: users send next request immediately after previous completes
        // Throughput = concurrentUsers / avgResponseTime
        List<ClosedInjectionStep> injection =
            List.of(
                rampConcurrentUsers(1)
                    .to(this.concurrentUsers)
                    .during(Duration.ofSeconds(this.rampDurationSec)),
                constantConcurrentUsers(this.concurrentUsers)
                    .during(Duration.ofSeconds(this.durationSec)));
        yield scenarioBuilder.injectClosed(injection);
      }
    };
  }

  private void prepareUser() throws Exception {
    logger.debug("Preparing admin user credentials...");
    userCredentials.add(Map.of("username", this.adminUser, "password", this.adminPassword));
    logger.debug("User preparation complete!");
  }

  /**
   * Disables the unique constraint on the "National identifier" attribute to allow importing
   * tracked entities with duplicate values.
   */
  private void disableUniqueConstraint() throws Exception {
    logger.debug("Disabling unique constraint on National identifier attribute...");

    HttpClient client = HttpClient.newBuilder().build();
    String auth =
        Base64.getEncoder()
            .encodeToString(
                (this.adminUser + ":" + this.adminPassword).getBytes(StandardCharsets.UTF_8));

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(this.instance + "/api/trackedEntityAttributes/AuPLng5hLbE"))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json-patch+json")
            .method(
                "PATCH",
                HttpRequest.BodyPublishers.ofString(
                    "[{\"op\": \"replace\", \"path\": \"/unique\", \"value\": false}]"))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new RuntimeException(
          "Failed to disable unique constraint: " + response.statusCode() + " " + response.body());
    }

    logger.debug("Unique constraint disabled successfully");
  }

  private HttpRequestActionBuilder login() {
    return http("Login")
        .post("/api/auth/login")
        .header("Content-Type", "application/json")
        .body(
            StringBody(
                "{\"username\":\""
                    + this.adminUser
                    + "\",\"password\":\""
                    + this.adminPassword
                    + "\"}"))
        .check(status().is(200));
  }

  private HttpRequestActionBuilder importTracker() {
    return http("Tracker Import")
        .post("/api/tracker?async=false&" + this.trackerArgs)
        .header("Content-Type", "application/json")
        .body(RawFileBody(this.trackerFile))
        .check(status().is(200));
  }
}
