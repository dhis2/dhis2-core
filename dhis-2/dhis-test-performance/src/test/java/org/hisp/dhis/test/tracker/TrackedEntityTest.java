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

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Focused performance test for /api/tracker/trackedEntities endpoint.
 *
 * <p>This test isolates the trackedEntities endpoint to analyze connection pool behavior. Each user
 * session makes exactly ONE request to /trackedEntities, making it easy to correlate users/sec with
 * connection pool pressure.
 *
 * <p><b>Usage:</b>
 *
 * <pre>
 * # Smoke test (1 user, 10 repeats)
 * mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.tracker.TrackedEntityTest
 *
 * # Load test (ramp to 6 users/sec)
 * mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.tracker.TrackedEntityTest \
 *     -Dprofile=load -DusersPerSec=6
 *
 * # Capacity test (staircase to find breaking point)
 * mvn gatling:test -Dgatling.simulationClass=org.hisp.dhis.test.tracker.TrackedEntityTest \
 *     -Dprofile=capacity -DusersPerSec=10 -Dsteps=5
 * </pre>
 *
 * <p><b>Key metrics to watch:</b>
 *
 * <ul>
 *   <li>Response time P95 - should stay under threshold
 *   <li>Connection wait_ms in dhis.log - indicates pool pressure
 *   <li>active/idle/waiting in HikariCP errors - shows pool exhaustion
 * </ul>
 */
public class TrackedEntityTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(TrackedEntityTest.class);

  private static final List<Map<String, Object>> userCredentials = new ArrayList<>();
  private static FeederBuilder<Object> userFeeder;

  private final Profile profile;
  private final String instance;
  private final String trackerProgram;
  private final String adminUser;
  private final String adminPassword;
  private final String replicaUser;
  private final String replicaPassword;
  private final int provisionUsers;
  private final int usersPerSec;
  private final int repeat;
  private final int durationSec;
  private final int rampDurationSec;
  private final int steps;
  private final int pageSize;

  private enum Profile {
    SMOKE,
    LOAD,
    CAPACITY;

    static Profile fromString(String profile) {
      try {
        return valueOf(profile.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Unknown profile: " + profile + ". Valid options: smoke, load, capacity");
      }
    }
  }

  public TrackedEntityTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.trackerProgram = System.getProperty("trackerProgram", "ur1Edk5Oe2n");
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");
    this.replicaUser = System.getProperty("replicaUser", "tracker");
    this.replicaPassword = System.getProperty("replicaPassword", "Tracker123!");
    this.pageSize = Integer.getInteger("pageSize", 15);

    record ProfileDefaults(
        int usersPerSec,
        int provisionUsers,
        int repeat,
        int rampDurationSec,
        int durationSec,
        int steps) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          case SMOKE -> new ProfileDefaults(1, 1, 10, 1, 1, 1);
          case LOAD -> new ProfileDefaults(5, 50, 1, 30, 180, 1);
          case CAPACITY -> new ProfileDefaults(10, 100, 1, 10, 30, 5);
        };
    this.usersPerSec = Integer.getInteger("usersPerSec", defaults.usersPerSec());
    this.repeat = Integer.getInteger("repeat", defaults.repeat());
    this.provisionUsers = Integer.getInteger("provisionUsers", defaults.provisionUsers());
    this.rampDurationSec = Integer.getInteger("rampDurationSec", defaults.rampDurationSec());
    this.durationSec = Integer.getInteger("durationSec", defaults.durationSec());
    this.steps = Integer.getInteger("steps", defaults.steps());

    logger.info(
        "TrackedEntityTest config: profile={}, usersPerSec={}, pageSize={}, duration={}s",
        this.profile,
        this.usersPerSec,
        this.pageSize,
        this.durationSec);

    try {
      provisionUsers();
    } catch (Exception e) {
      throw new RuntimeException("User provisioning failed", e);
    }

    ScenarioBuilder scenario = buildScenario();

    PopulationBuilder populationBuilder;
    if (this.profile == Profile.SMOKE) {
      ClosedInjectionStep closedInjection = constantConcurrentUsers(1).during(1);
      populationBuilder = scenario.injectClosed(closedInjection);
    } else {
      List<OpenInjectionStep> injectionProfile = buildInjectionProfile();
      populationBuilder = scenario.injectOpen(injectionProfile);
    }

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl(this.instance)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/TrackedEntityTest")
            .disableFollowRedirect()
            .warmUp(this.instance + "/api/ping")
            .disableCaching()
            .check(status().is(200));

    List<Assertion> assertions = List.of(forAll().successfulRequests().percent().gte(99d));
    setUp(populationBuilder).protocols(httpProtocolBuilder).assertions(assertions);
  }

  private void provisionUsers() throws Exception {
    logger.info("Provisioning {} test users...", this.provisionUsers);

    HttpClient client = HttpClient.newBuilder().build();
    String auth =
        Base64.getEncoder()
            .encodeToString(
                (this.adminUser + ":" + this.adminPassword).getBytes(StandardCharsets.UTF_8));

    HttpRequest getUserRequest =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    this.instance
                        + "/api/users?filter=username:eq:"
                        + this.replicaUser
                        + "&fields=id"))
            .header("Authorization", "Basic " + auth)
            .header("Accept", "application/json")
            .GET()
            .build();

    HttpResponse<String> getUserResponse =
        client.send(getUserRequest, HttpResponse.BodyHandlers.ofString());

    if (getUserResponse.statusCode() != 200) {
      throw new RuntimeException(
          "Failed to get source user: "
              + getUserResponse.statusCode()
              + " "
              + getUserResponse.body());
    }

    Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    Matcher matcher = pattern.matcher(getUserResponse.body());
    if (!matcher.find()) {
      throw new RuntimeException("Could not find source user '" + this.replicaUser + "'");
    }
    String userId = matcher.group(1);
    logger.debug("Found source user '{}' with ID: {}", this.replicaUser, userId);

    int provisionDelayMs = Integer.getInteger("provisionDelayMs", 100);
    for (int i = 1; i <= this.provisionUsers; i++) {
      String username = "%s_%03d".formatted(this.replicaUser, i);
      String requestBody =
          """
          {"username":"%s","password":"%s"}
          """
              .formatted(username, this.replicaPassword)
              .trim();

      HttpRequest replicateRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(this.instance + "/api/users/" + userId + "/replica"))
              .header("Authorization", "Basic " + auth)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> replicateResponse =
          client.send(replicateRequest, HttpResponse.BodyHandlers.ofString());

      if (replicateResponse.statusCode() == 201) {
        userCredentials.add(Map.of("username", username, "password", this.replicaPassword));
        logger.debug("Created user {}/{}: {}", i, this.provisionUsers, username);
      } else if (replicateResponse.statusCode() == 409
          && replicateResponse.body().contains("Username already taken")) {
        userCredentials.add(Map.of("username", username, "password", this.replicaPassword));
        logger.debug("User already exists {}/{}: {}", i, this.provisionUsers, username);
      } else {
        throw new RuntimeException(
            "Failed to create user "
                + username
                + ": HTTP "
                + replicateResponse.statusCode()
                + " - "
                + replicateResponse.body());
      }

      if (i < this.provisionUsers && provisionDelayMs > 0) {
        Thread.sleep(provisionDelayMs);
      }
    }

    userFeeder = io.gatling.javaapi.core.CoreDsl.listFeeder(userCredentials).circular();
    logger.info("User provisioning complete! Total users: {}", userCredentials.size());

    int pauseAfterProvisioningSec = Integer.getInteger("pauseAfterProvisioningSec", 5);
    if (pauseAfterProvisioningSec > 0) {
      logger.debug("Waiting {}s for system to stabilize...", pauseAfterProvisioningSec);
      Thread.sleep(pauseAfterProvisioningSec * 1000L);
    }
  }

  private ScenarioBuilder buildScenario() {
    // Single request to /trackedEntities with configurable pageSize
    // This is the request that triggers the N+1 connection pattern
    String trackedEntitiesUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc"
            + "&page=1"
            + "&pageSize="
            + this.pageSize
            + "&orgUnits=DiszpKrYNg8"
            + "&orgUnitMode=SELECTED"
            + "&program="
            + this.trackerProgram
            + "&fields=attributes,enrollments,trackedEntity,orgUnit";

    return scenario("TrackedEntities")
        .feed(userFeeder)
        .exec(
            session -> {
              String username = session.getString("username");
              String userNum = username.substring(username.lastIndexOf('_') + 1);
              String paddedNum = String.format("%05d", Integer.parseInt(userNum));
              String randomSuffix =
                  java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 28);
              String requestId = "g-" + paddedNum + "-" + randomSuffix;
              return session.set("requestId", requestId);
            })
        .exec(
            http("Login")
                .post("/api/auth/login")
                .header("Content-Type", "application/json")
                .header("X-Request-ID", "#{requestId}")
                .body(StringBody("{\"username\":\"#{username}\",\"password\":\"#{password}\"}"))
                .check(status().is(200)))
        .exitHereIfFailed()
        .repeat(this.repeat)
        .on(
            exec(
                http("Get TrackedEntities (pageSize=" + this.pageSize + ")")
                    .get(trackedEntitiesUrl)
                    .header("X-Request-ID", "#{requestId}")));
  }

  private List<OpenInjectionStep> buildInjectionProfile() {
    return switch (this.profile) {
      case LOAD ->
          List.of(
              rampUsersPerSec(1)
                  .to(this.usersPerSec)
                  .during(Duration.ofSeconds(this.rampDurationSec)),
              constantUsersPerSec(this.usersPerSec).during(Duration.ofSeconds(this.durationSec)));

      case CAPACITY ->
          List.of(
              incrementUsersPerSec((double) this.usersPerSec / this.steps)
                  .times(this.steps)
                  .eachLevelLasting(Duration.ofSeconds(this.durationSec))
                  .separatedByRampsLasting(Duration.ofSeconds(this.rampDurationSec))
                  .startingFrom((double) this.usersPerSec / this.steps));

      case SMOKE ->
          throw new IllegalArgumentException(
              "SMOKE profile uses closed injection, not open injection");
    };
  }
}
