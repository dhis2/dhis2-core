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
import static io.gatling.javaapi.core.CoreDsl.during;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ChainBuilder;
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
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for DHIS2 Tracker import API using a static JSON payload.
 *
 * <p>This test imports tracker data from a JSON file using POST requests to /api/tracker. Place
 * your tracker payload file in {@code src/test/resources/} (default: {@code tracker.json}).
 *
 * <p><b>Profile: load</b> (only profile) - Gradual ramp-up to sustained load <br>
 * Shape: ___/‾‾‾‾‾‾‾‾‾ <br>
 * Uses the closed injection model: a fixed pool of concurrent users log in once and loop via {@code
 * during()} for the injection duration. <br>
 * Uses: concurrentUsers, rampDurationSec (ramp-up), durationSec (sustained) <br>
 * Default: 15s ramp -> 3min sustained (3.25min total) <br>
 * Example: {@code -Dprofile=load -DconcurrentUsers=4 -DrampDurationSec=15 -DdurationSec=180}
 *
 * <p>For soak testing, use longer duration: {@code -DconcurrentUsers=4 -DdurationSec=3600}
 *
 * <p><b>Parameters:</b>
 *
 * <ul>
 *   <li>{@code -Dinstance} - DHIS2 instance URL (default: http://localhost:8080)
 *   <li>{@code -DtrackerFile} - JSON file to import from src/test/resources (default: tracker.json)
 *   <li>{@code -DtrackerArgs} - Additional query parameters for /api/tracker (default:
 *       skipRuleEngine=true&amp;skipSideEffects=true). {@code async=false} is always set.
 *   <li>{@code -DconcurrentUsers} - Number of concurrent users (default: 4)
 *   <li>{@code -DrampDurationSec} - Ramp-up duration in seconds (default: 15)
 *   <li>{@code -DdurationSec} - Sustained load duration in seconds (default: 180)
 * </ul>
 */
public class TrackerImportTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(TrackerImportTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  private final String instance;
  private final String adminUser;
  private final String adminPassword;
  private final int concurrentUsers;
  private final int durationSec;
  private final int rampDurationSec;
  private final String trackerArgs;
  private final String trackerFile;

  public TrackerImportTest() {
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");
    this.trackerArgs =
        System.getProperty("trackerArgs", "skipRuleEngine=true&skipSideEffects=true");
    this.trackerFile = System.getProperty("trackerFile", "tracker.json");
    this.concurrentUsers = Integer.getInteger("concurrentUsers", 4);
    this.rampDurationSec = Integer.getInteger("rampDurationSec", 15);
    this.durationSec = Integer.getInteger("durationSec", 180);

    try {
      disableUniqueConstraint();
    } catch (Exception e) {
      throw new RuntimeException("Setup failed", e);
    }

    long injectionDurationSec = this.rampDurationSec + this.durationSec;
    ChainBuilder importLoop =
        exec(during(Duration.ofSeconds(injectionDurationSec)).on(exec(importTracker())));

    ScenarioBuilder scenarioBuilder =
        scenario("Tracker Import").exec(login()).exitHereIfFailed().exec(importLoop);

    List<ClosedInjectionStep> injection =
        List.of(
            rampConcurrentUsers(0)
                .to(this.concurrentUsers)
                .during(Duration.ofSeconds(this.rampDurationSec)),
            constantConcurrentUsers(this.concurrentUsers)
                .during(Duration.ofSeconds(this.durationSec)));

    PopulationBuilder populationBuilder = scenarioBuilder.injectClosed(injection);

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
        .header("X-Request-ID", session -> nextRequestId("Login"))
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
        .header("X-Request-ID", session -> nextRequestId("Tracker Import"))
        .body(RawFileBody(this.trackerFile))
        .check(status().is(200));
  }

  /**
   * Generates a unique {@code X-Request-ID} header value and logs the mapping to the Gatling
   * request name. The ID is a simple counter ({@code g-1}, {@code g-2}, ...) valid per {@link
   * org.hisp.dhis.webapi.filter.RequestIdFilter}. The logged mapping allows correlating SQL queries
   * (grouped by {@code request_id} in SQL comments) back to the Gatling request that triggered
   * them.
   */
  private static String nextRequestId(String name) {
    String id = "g-" + REQUEST_COUNTER.incrementAndGet();
    logger.debug("X-Request-ID: {} -> {}", id, name);
    return id;
  }
}
