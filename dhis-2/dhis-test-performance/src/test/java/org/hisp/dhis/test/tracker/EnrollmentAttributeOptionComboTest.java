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
package org.hisp.dhis.test.tracker;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.during;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.FeederBuilder;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for the attribute option combo (AOC) access check of the tracked entity export.
 *
 * <p>A tracked entity is only exported for a program if the user can read the AOC of one of its
 * enrollments in that program, meaning the user has data read access to every category option of
 * the AOC. The check runs for every enrollment the query looks at, so it is most expensive when the
 * user can read few enrollments in a wide scope: the query scans on to fill a page, or scans every
 * enrollment in scope when the user can read none.
 *
 * <p><b>Database:</b> the Sierra Leone demo DB has the default AOC on every enrollment, which every
 * user can read. Populate it with {@code scripts/populate-enrollment-attribute-option-combo.sh}
 * first. It gives every Child Programme enrollment an AOC of two category options, makes only
 * {@code POPULATE_READABLE_PERCENT} of them readable, and creates the user {@code perf_aoc} with
 * national scope and membership in every user group. Run the same populated database against the
 * baseline and the candidate image:
 *
 * <pre>{@code
 * POPULATE_SCRIPT=scripts/populate-enrollment-attribute-option-combo.sh \
 * POPULATE_PROFILE=worst-case \
 * DHIS2_IMAGE=dhis2/core-dev:latest \
 * SIMULATION_CLASS=org.hisp.dhis.test.tracker.EnrollmentAttributeOptionComboTest \
 * ./run-simulation.sh
 * }</pre>
 *
 * <p><b>Requests:</b> each covers one way the export finds the enrollments of a program:
 *
 * <ul>
 *   <li><b>Get first page of TEs</b> -- an {@code exists} on the enrollments, stops once the page
 *       is full
 *   <li><b>Get first page of TEs ordered by enrolledAt</b> -- a join on the enrollments, sorts
 *       every enrollment in scope before taking the page
 *   <li><b>Get first page of TEs with total pages</b> -- also counts every tracked entity in scope
 * </ul>
 *
 * <p>Responses are not checked for content, as their size depends on {@code
 * POPULATE_READABLE_PERCENT} (none are returned in the worst case). There are no response time
 * thresholds yet: compare a baseline and a candidate run with {@code gstat compare}.
 *
 * <p><b>Profiles:</b>
 *
 * <ul>
 *   <li><b>smoke</b> (default) - Single user with repeat loop <br>
 *       Uses: repeat (default: 100) <br>
 *       Example: {@code -Dprofile=smoke -Drepeat=50}
 *   <li><b>load</b> - Gradual ramp-up to sustained load <br>
 *       Uses: concurrentUsers, rampDurationSec (ramp-up), durationSec (sustained) <br>
 *       Default: 15s ramp -> 3min sustained <br>
 *       Example: {@code -Dprofile=load -DconcurrentUsers=4 -DrampDurationSec=30 -DdurationSec=180}
 * </ul>
 *
 * @see TrackerTest
 */
public class EnrollmentAttributeOptionComboTest extends Simulation {
  private static final Logger logger =
      LoggerFactory.getLogger(EnrollmentAttributeOptionComboTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  private static final List<Map<String, Object>> userCredentials = new ArrayList<>();
  private static FeederBuilder<Object> userFeeder;

  private final Profile profile;
  private final String instance;
  private final String trackerProgram;
  private final String orgUnit;
  private final String adminUser;
  private final String adminPassword;
  private final String replicaUser;
  private final String replicaPassword;
  private final int provisionUsers;
  private final int concurrentUsers;
  private final int repeat;
  private final int durationSec;
  private final int rampDurationSec;

  private enum Profile {
    SMOKE,
    LOAD;

    static Profile fromString(String profile) {
      try {
        return valueOf(profile.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Unknown profile: " + profile + ". Valid options: smoke, load");
      }
    }
  }

  private record Request(
      String url, EnumMap<Profile, Integer> p95Thresholds, String name, String... groups) {
    HttpRequestActionBuilder action() {
      return http(name).get(url).header("X-Request-ID", session -> nextRequestId(name));
    }

    Optional<Assertion> assertion(Profile profile) {
      return Optional.ofNullable(p95Thresholds.get(profile))
          .map(
              threshold -> {
                String[] parts = Arrays.copyOf(groups, groups.length + 1);
                parts[groups.length] = name;
                return details(parts).responseTime().percentile(95).lte(threshold);
              });
    }
  }

  public EnrollmentAttributeOptionComboTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.trackerProgram = System.getProperty("trackerProgram", "IpHINAT79UW"); // Child Programme
    this.orgUnit = System.getProperty("orgUnit", "ImspTQPwCqd"); // Sierra Leone
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");
    this.replicaUser = System.getProperty("replicaUser", "perf_aoc");
    this.replicaPassword = System.getProperty("replicaPassword", "Tracker123@");

    record ProfileDefaults(
        int concurrentUsers,
        int provisionUsers,
        int repeat,
        int rampDurationSec,
        int durationSec) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          case SMOKE -> new ProfileDefaults(1, 1, 100, 1, 1);
          case LOAD -> new ProfileDefaults(4, 4, 1, 15, 180);
        };
    this.concurrentUsers = Integer.getInteger("concurrentUsers", defaults.concurrentUsers());
    this.repeat = Integer.getInteger("repeat", defaults.repeat());
    this.provisionUsers = Integer.getInteger("provisionUsers", defaults.provisionUsers());
    this.rampDurationSec = Integer.getInteger("rampDurationSec", defaults.rampDurationSec());
    this.durationSec = Integer.getInteger("durationSec", defaults.durationSec());

    try {
      provisionUsers();
    } catch (Exception e) {
      throw new RuntimeException("User provisioning failed", e);
    }

    List<Request> requests = requests();

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl(this.instance)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/Performance Test")
            .disableFollowRedirect() // we don't expect redirects
            .warmUp(
                this.instance
                    + "/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching() // to repeat the same request without HTTP cache influence (304)
            .check(status().is(200)); // global check for all requests

    SetUp setUp =
        setUp(trackedEntitiesScenario(requests).injectClosed(buildClosedInjectionProfile()))
            .protocols(httpProtocolBuilder)
            .assertions(getAssertions(this.profile, requests));
    // Pauses model a user reading the screen between requests. SMOKE has none by design, and
    // -DdisablePauses=true opts any profile out.
    if (this.profile == Profile.SMOKE || Boolean.getBoolean("disablePauses")) {
      setUp.disablePauses();
    }
  }

  /**
   * Provisions test users by replicating a source user via DHIS2 API. The source user is created by
   * {@code scripts/populate-enrollment-attribute-option-combo.sh}, and replicas keep its user
   * groups and org units.
   */
  private void provisionUsers() throws Exception {
    logger.debug("Provisioning {} test users...", this.provisionUsers);

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
      throw new RuntimeException(
          "Could not find source user '"
              + this.replicaUser
              + "'. Run scripts/populate-enrollment-attribute-option-combo.sh first.");
    }
    String userId = matcher.group(1);
    logger.debug("Found source user '{}' with ID: {}", this.replicaUser, userId);

    // Throttle user creation to avoid overwhelming the system
    int provisionDelayMs = Integer.getInteger("provisionDelayMs", 100);
    for (int i = 1; i <= this.provisionUsers; i++) {
      String username = "%s_user_%03d".formatted(this.replicaUser, i);
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

    // Use circular to reuse users across multiple VU executions
    userFeeder = io.gatling.javaapi.core.CoreDsl.listFeeder(userCredentials).circular();

    logger.debug("User provisioning complete! Total users: {}", userCredentials.size());
  }

  private HttpRequestActionBuilder login() {
    return http("Login")
        .post("/api/auth/login")
        .header("Content-Type", "application/json")
        .header("X-Request-ID", session -> nextRequestId("Login"))
        .body(StringBody("{\"username\":\"#{username}\",\"password\":\"#{password}\"}"))
        .check(status().is(200));
  }

  private List<Request> requests() {
    // Capture's tracked entity working list, over the whole country
    String getTEsUrl =
        "/api/tracker/trackedEntities?page=1&pageSize=15&orgUnits="
            + this.orgUnit
            + "&orgUnitMode=DESCENDANTS&program="
            + this.trackerProgram
            + "&fields=:all,!relationships,programOwners[orgUnit,program]";

    Request getFirstPageOfTEs =
        new Request(
            getTEsUrl + "&order=createdAt:desc",
            new EnumMap<>(Profile.class),
            "Get first page of TEs",
            "Get Child Programme TEs");
    Request getFirstPageOfTEsOrderedByEnrolledAt =
        new Request(
            getTEsUrl + "&order=enrolledAt:desc",
            new EnumMap<>(Profile.class),
            "Get first page of TEs ordered by enrolledAt",
            "Get Child Programme TEs");
    Request getFirstPageOfTEsWithTotalPages =
        new Request(
            getTEsUrl + "&order=createdAt:desc&totalPages=true",
            new EnumMap<>(Profile.class),
            "Get first page of TEs with total pages",
            "Get Child Programme TEs");

    return List.of(
        getFirstPageOfTEs, getFirstPageOfTEsOrderedByEnrolledAt, getFirstPageOfTEsWithTotalPages);
  }

  private ScenarioBuilder trackedEntitiesScenario(List<Request> requests) {
    Request getFirstPageOfTEs = requests.get(0);
    Request getFirstPageOfTEsOrderedByEnrolledAt = requests.get(1);
    Request getFirstPageOfTEsWithTotalPages = requests.get(2);

    var exportRequests =
        exec(
            group("Get Child Programme TEs")
                .on(
                    // User opens TE working list
                    exec(getFirstPageOfTEs.action())
                        .pause(1, 3) // user reads TE list, sorts by enrollment date
                        .exec(getFirstPageOfTEsOrderedByEnrolledAt.action())
                        .pause(1, 3) // user reads TE list, asks for the number of pages
                        .exec(getFirstPageOfTEsWithTotalPages.action())));

    return scenario("Child Programme export")
        .feed(userFeeder)
        .exec(login())
        .exitHereIfFailed()
        .exec(loopForProfile(exportRequests));
  }

  /**
   * Builds the closed injection profile. A fixed pool of concurrent users stays alive for the
   * injection duration, each logging in once and looping via {@code during()}.
   */
  private List<ClosedInjectionStep> buildClosedInjectionProfile() {
    return switch (this.profile) {
      case SMOKE -> List.of(constantConcurrentUsers(1).during(1));
      case LOAD ->
          List.of(
              rampConcurrentUsers(0)
                  .to(this.concurrentUsers)
                  .during(Duration.ofSeconds(this.rampDurationSec)),
              constantConcurrentUsers(this.concurrentUsers)
                  .during(Duration.ofSeconds(this.durationSec)));
    };
  }

  /**
   * Returns either a {@code repeat()} or {@code during()} loop wrapping the given chain, depending
   * on the profile. SMOKE uses {@code repeat()} for deterministic iteration count. LOAD uses {@code
   * during()} to keep users alive for the full injection duration.
   */
  private ChainBuilder loopForProfile(ChainBuilder chain) {
    if (this.profile == Profile.SMOKE) {
      return exec(repeat(this.repeat).on(chain));
    }
    return exec(
        during(Duration.ofSeconds((long) this.rampDurationSec + this.durationSec)).on(chain));
  }

  /**
   * Returns assertions for the given profile, including a global success rate assertion and any
   * profile-specific P95 response time assertions defined in the requests.
   */
  private static List<Assertion> getAssertions(Profile profile, List<Request> requests) {
    return Stream.concat(
            Stream.of(forAll().successfulRequests().percent().gte(100d)),
            requests.stream().flatMap(r -> r.assertion(profile).stream()))
        .toList();
  }

  /**
   * Generates a unique {@code X-Request-ID} header value and logs the mapping to the Gatling
   * request name, to correlate SQL queries back to the Gatling request that triggered them.
   */
  private static String nextRequestId(String name) {
    String id = "g-" + REQUEST_COUNTER.incrementAndGet();
    logger.debug("X-Request-ID: {} -> {}", id, name);
    return id;
  }
}
