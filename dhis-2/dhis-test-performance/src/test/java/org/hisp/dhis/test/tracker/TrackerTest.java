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
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for DHIS2 Tracker API endpoints.
 *
 * <p><b>Note:</b> This test runs two scenarios (Single Events and Tracker Program) sequentially, so
 * the total test duration is 2 x the profile duration shown below.
 *
 * <p><b>User provisioning:</b> The test provisions users via {@code -DprovisionUsers} (defaults to
 * an estimated value that should accommodate the peak concurrent users for the profiles' default
 * parameters). This value must be >= the maximum concurrent users during the test; otherwise users
 * may be reused concurrently, causing login conflicts that invalidate sessions.
 *
 * <p><b>Profiles:</b>
 *
 * <ul>
 *   <li><b>smoke</b> (default) - Single user with repeat loop <br>
 *       Uses the closed injection model with constantConcurrentUsers(1) <br>
 *       Uses: repeat (default: 100) <br>
 *       Example: {@code -Dprofile=smoke -Drepeat=50}
 *   <li><b>load</b> - Gradual ramp-up to sustained load <br>
 *       Shape: ___/‾‾‾‾‾‾‾‾‾ <br>
 *       Uses: usersPerSec, rampDurationSec (ramp-up), durationSec (sustained), repeat (default: 1)
 *       <br>
 *       Default: 30s ramp -> 3min sustained (3.5min total) <br>
 *       Example: {@code -Dprofile=load -DusersPerSec=4 -DrampDurationSec=30 -DdurationSec=180}
 *       <p>For soak testing, use load with longer duration: {@code -Dprofile=load -DusersPerSec=4
 *       -DdurationSec=3600}
 *   <li><b>capacity</b> - Staircase pattern to find maximum sustainable users <br>
 *       Shape: <br>
 *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
 *       #steps <br>
 *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/‾‾ <br>
 *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/‾‾ <br>
 *       __/‾‾ <br>
 *       Uses: usersPerSec (target), rampDurationSec (between steps), durationSec (per step), steps,
 *       repeat (default: 1) <br>
 *       Default: 5 steps × 30s with 10s ramps (3.2min total) <br>
 *       Example: {@code -Dprofile=capacity -DusersPerSec=10 -Dsteps=5 -DrampDurationSec=10
 *       -DdurationSec=30}
 * </ul>
 *
 * @see <a
 *     href="https://docs.gatling.io/guides/optimize-scripts/writing-realistic-tests/#injection-profiles">Gatling
 *     Injection Profiles</a>
 */
public class TrackerTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(TrackerTest.class);

  private static final List<Map<String, Object>> userCredentials = new ArrayList<>();
  private static FeederBuilder<Object> userFeeder;

  private final Profile profile;
  private final String instance;
  private final String eventProgram;
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

  private record Request(
      String url, EnumMap<Profile, Integer> p95Thresholds, String name, String... groups) {
    HttpRequestActionBuilder action() {
      return http(name).get(url);
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

  private record ScenarioWithRequests(ScenarioBuilder scenario, List<Request> requests) {}

  public TrackerTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.eventProgram = System.getProperty("eventProgram", "VBqh0ynB2wv");
    this.trackerProgram = System.getProperty("trackerProgram", "ur1Edk5Oe2n");
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");
    this.replicaUser = System.getProperty("replicaUser", "tracker");
    this.replicaPassword = System.getProperty("replicaPassword", "Tracker123!");

    record ProfileDefaults(
        int usersPerSec,
        int provisionUsers,
        int repeat,
        int rampDurationSec,
        int durationSec,
        int steps) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          case SMOKE -> new ProfileDefaults(1, 1, 100, 1, 1, 1);
          case LOAD -> new ProfileDefaults(8, 30, 1, 30, 180, 1);
          case CAPACITY -> new ProfileDefaults(8, 100, 1, 10, 30, 4);
        };
    this.usersPerSec = Integer.getInteger("usersPerSec", defaults.usersPerSec());
    this.repeat = Integer.getInteger("repeat", defaults.repeat());
    this.provisionUsers = Integer.getInteger("provisionUsers", defaults.provisionUsers());
    this.rampDurationSec = Integer.getInteger("rampDurationSec", defaults.rampDurationSec());
    this.durationSec = Integer.getInteger("durationSec", defaults.durationSec());
    this.steps = Integer.getInteger("steps", defaults.steps());

    try {
      provisionUsers();
    } catch (Exception e) {
      throw new RuntimeException("User provisioning failed", e);
    }

    ScenarioWithRequests eventScenario = eventProgramScenario();
    ScenarioWithRequests trackerScenario = trackerProgramScenario();

    PopulationBuilder populationBuilder;
    if (this.profile == Profile.SMOKE) {
      ClosedInjectionStep closedInjection = constantConcurrentUsers(1).during(1);
      populationBuilder =
          eventScenario
              .scenario()
              .injectClosed(closedInjection)
              .andThen(trackerScenario.scenario().injectClosed(closedInjection));
    } else {
      List<OpenInjectionStep> injectionProfile = buildInjectionProfile();
      populationBuilder =
          eventScenario
              .scenario()
              .injectOpen(injectionProfile)
              .andThen(trackerScenario.scenario().injectOpen(injectionProfile));
    }

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

    List<Assertion> assertions = getAssertions(this.profile, eventScenario, trackerScenario);
    setUp(populationBuilder).protocols(httpProtocolBuilder).assertions(assertions);
  }

  /** Provisions test users by replicating a source user via DHIS2 API. */
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
      throw new RuntimeException("Could not find source user '" + this.replicaUser + "'");
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

    // Wait for DHIS2 to stabilize after user creation
    int pauseAfterProvisioningSec = Integer.getInteger("pauseAfterProvisioningSec", 5);
    if (pauseAfterProvisioningSec > 0) {
      logger.debug("Waiting {}s for system to stabilize...", pauseAfterProvisioningSec);
      Thread.sleep(pauseAfterProvisioningSec * 1000L);
      logger.debug("Starting test execution...");
    }
  }

  private ScenarioWithRequests eventProgramScenario() {
    String singleEventUrl = "/api/tracker/events/#{eventUid}";
    String relationshipUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    String getEventsUrl =
        "/api/tracker/events?program="
            + this.eventProgram
            + "&fields=dataValues,occurredAt,event,status,orgUnit,program,programType,updatedAt,createdAt,assignedUser,"
            + "&orgUnit=DiszpKrYNg8"
            + "&orgUnitMode=SELECTED"
            + "&order=occurredAt:desc";

    Request goToFirstPage =
        new Request(
            getEventsUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 63, Profile.LOAD, 72)),
            "Go to first page of program " + this.eventProgram,
            "Get a list of single events");
    Request goToSecondPage =
        new Request(
            getEventsUrl + "&page=2",
            new EnumMap<>(Map.of(Profile.SMOKE, 99, Profile.LOAD, 105)),
            "Go to second page of program " + this.eventProgram,
            "Get a list of single events");
    Request searchSingleEvents =
        new Request(
            getEventsUrl + "&occurredAfter=2024-01-01&occurredBefore=2024-12-31",
            new EnumMap<>(Map.of(Profile.SMOKE, 57, Profile.LOAD, 64)),
            "Search single events in date interval in program " + this.eventProgram,
            "Get a list of single events");
    Request getFirstEvent =
        new Request(
            singleEventUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 42, Profile.LOAD, 57)),
            "Get first event",
            "Get a list of single events",
            "Get one single event");
    Request getRelationshipsForFirstEvent =
        new Request(
            relationshipUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 8, Profile.LOAD, 8)),
            "Get relationships for first event",
            "Get a list of single events",
            "Get one single event");

    ScenarioBuilder scenarioBuilder =
        scenario("Single Events")
            .feed(userFeeder)
            .exec(login())
            .exitHereIfFailed()
            .repeat(this.repeat)
            .on(
                group("Get a list of single events")
                    .on(
                        exec(goToFirstPage.action())
                            .exec(goToSecondPage.action())
                            .exec(
                                searchSingleEvents
                                    .action()
                                    .check(jsonPath("$.events").exists())
                                    .check(jsonPath("$.events[0]").exists())
                                    .check(jsonPath("$.events[0].event").saveAs("eventUid")))
                            .exitHereIfFailed()
                            .group("Get one single event")
                            .on(
                                exec(getFirstEvent.action())
                                    .exec(getRelationshipsForFirstEvent.action()))));

    return new ScenarioWithRequests(
        scenarioBuilder,
        List.of(
            goToFirstPage,
            goToSecondPage,
            searchSingleEvents,
            getFirstEvent,
            getRelationshipsForFirstEvent));
  }

  private HttpRequestActionBuilder login() {
    return http("Login")
        .post("/api/auth/login")
        .header("Content-Type", "application/json")
        .body(StringBody("{\"username\":\"#{username}\",\"password\":\"#{password}\"}"))
        .check(status().is(200));
  }

  private ScenarioWithRequests trackerProgramScenario() {
    String getTEsUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc &page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program="
            + this.trackerProgram
            + "&fields=:all,!relationships,programOwner[orgUnit,program]";

    String searchForTEByNationalId =
        "/api/tracker/trackedEntities?orgUnitMode=ACCESSIBLE&program="
            + this.trackerProgram
            + "&filter=AuPLng5hLbE:eq:123";

    String notFoundByNationalId =
        "/api/tracker/trackedEntities?orgUnitMode=ACCESSIBLE&program="
            + this.trackerProgram
            + "&filter=AuPLng5hLbE:eq:aaa";

    String notFoundTEByName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:notfoundname"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:Ines"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchEventByProgramStage =
        "/api/tracker/events?filter=yLIPuJHRgey:ge:50&order=createdAt:desc&page=1"
            + "&pageSize=15&orgUnit=DiszpKrYNg8&orgUnitMode=SELECTED&program="
            + this.trackerProgram
            + "&programStage=jdRD35YwbRH&fields=*";

    String getTEsFromEvents =
        "/api/tracker/trackedEntities?pageSize=15&program="
            + this.trackerProgram
            + "&trackedEntities=#{trackedEntityUids}&fields=trackedEntity,createdAt,attributes[attribute,value],programOwners[orgUnit],enrollments[enrollment,status,orgUnit,enrolledAt]";

    String singleTrackedEntityUrl =
        "/api/tracker/trackedEntities/#{trackedEntityUid}?program"
            + this.trackerProgram
            + "&fields=programOwners[orgUnit],enrollments";
    String singleEnrollmentUrl =
        "/api/tracker/enrollments/#{enrollmentUid}?fields=enrollment,trackedEntity,program,status,orgUnit,enrolledAt,occurredAt,followUp,deleted,createdBy,updatedBy,updatedAt,geometry";

    String relationshipForTrackedEntityUrl =
        "/api/tracker/relationships?trackedEntity=#{trackedEntityUid}&paging=false&fields=relationship,relationshipType,createdAt,from[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]],to[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]]";

    String relationshipForEventUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    String eventUrl =
        "/api/tracker/events/#{eventUid}?fields=event,relationships[relationship,relationshipType,relationshipName,bidirectional,from[event[event,dataValues,occurredAt,scheduledAt,status,orgUnit,programStage,program]],to[event[event,dataValues,*,occurredAt,scheduledAt,status,orgUnit,programStage,program]]]";

    Request notFoundTeByNameWithLikeOperator =
        new Request(
            notFoundTEByName,
            new EnumMap<>(Map.of(Profile.SMOKE, 204, Profile.LOAD, 280)),
            "Not found TE by name with like operator",
            "Get a list of TEs");
    Request notFoundTeByNationalIdWithEqualOperator =
        new Request(
            notFoundByNationalId,
            new EnumMap<>(Map.of(Profile.SMOKE, 6, Profile.LOAD, 16)),
            "Not found TE by national id with eq operator",
            "Get a list of TEs");
    Request searchTeByNameWithLikeOperator =
        new Request(
            searchTEByName,
            new EnumMap<>(Map.of(Profile.SMOKE, 197, Profile.LOAD, 280)),
            "Search TE by name with like operator",
            "Get a list of TEs");
    Request searchTeByNationalIdWithEqualOperator =
        new Request(
            searchForTEByNationalId,
            new EnumMap<>(Map.of(Profile.SMOKE, 11, Profile.LOAD, 22)),
            "Search TE by national id with eq operator",
            "Get a list of TEs");
    Request searchEventsByProgramStage =
        new Request(
            searchEventByProgramStage,
            new EnumMap<>(Map.of(Profile.SMOKE, 11, Profile.LOAD, 33)),
            "Search events by program stage",
            "Get a list of TEs");
    Request getTrackedEntitiesForEvents =
        new Request(
            getTEsFromEvents,
            new EnumMap<>(Map.of(Profile.SMOKE, 22, Profile.LOAD, 50)),
            "Get tracked entities from events",
            "Get a list of TEs");
    Request getFirstPageOfTEs =
        new Request(
            getTEsUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 147, Profile.LOAD, 100)),
            "Get first page of TEs of program " + this.trackerProgram,
            "Get a list of TEs");
    Request getFirstTrackedEntity =
        new Request(
            singleTrackedEntityUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 44, Profile.LOAD, 65)),
            "Get first tracked entity",
            "Get a list of TEs",
            "Go to single enrollment");
    Request getFirstEnrollment =
        new Request(
            singleEnrollmentUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 15, Profile.LOAD, 28)),
            "Get first enrollment",
            "Get a list of TEs",
            "Go to single enrollment");
    Request getRelationshipsForTrackedEntity =
        new Request(
            relationshipForTrackedEntityUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 8, Profile.LOAD, 8)),
            "Get relationships for first tracked entity",
            "Get a list of TEs",
            "Go to single enrollment");
    Request getFirstEventFromEnrollment =
        new Request(
            eventUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 32, Profile.LOAD, 114)),
            "Get first event from enrollment",
            "Get a list of TEs",
            "Go to single enrollment",
            "Get one event");
    Request getRelationshipsForEvent =
        new Request(
            relationshipForEventUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 6, Profile.LOAD, 8)),
            "Get relationships for first event",
            "Get a list of TEs",
            "Go to single enrollment",
            "Get one event");

    ScenarioBuilder scenarioBuilder =
        scenario("Tracker Program")
            .feed(userFeeder)
            .exec(login())
            .exitHereIfFailed()
            .repeat(this.repeat)
            .on(
                group("Get a list of TEs")
                    .on(
                        exec(notFoundTeByNameWithLikeOperator.action())
                            .exec(notFoundTeByNationalIdWithEqualOperator.action())
                            .exec(searchTeByNameWithLikeOperator.action())
                            .exec(searchTeByNationalIdWithEqualOperator.action())
                            .exec(
                                searchEventsByProgramStage
                                    .action()
                                    .check(jsonPath("$.events").exists())
                                    .check(jsonPath("$.events[0]").exists())
                                    .check(
                                        jsonPath("$.events[*].trackedEntity")
                                            .findAll()
                                            .transform(
                                                list ->
                                                    String.join(
                                                        ",", list.stream().distinct().toList()))
                                            .saveAs("trackedEntityUids")))
                            .exitHereIfFailed()
                            .exec(getTrackedEntitiesForEvents.action())
                            .exec(
                                getFirstPageOfTEs
                                    .action()
                                    .check(jsonPath("$.trackedEntities").exists())
                                    .check(jsonPath("$.trackedEntities[0]").exists())
                                    .check(
                                        jsonPath("$.trackedEntities[0].trackedEntity")
                                            .saveAs("trackedEntityUid")))
                            .exitHereIfFailed()
                            .group("Go to single enrollment")
                            .on(
                                exec(getFirstTrackedEntity
                                        .action()
                                        .check(jsonPath("$.enrollments").exists())
                                        .check(jsonPath("$.enrollments[0]").exists())
                                        .check(
                                            jsonPath("$.enrollments[0].enrollment")
                                                .saveAs("enrollmentUid"))
                                        .check(jsonPath("$.enrollments[0].events").exists())
                                        .check(jsonPath("$.enrollments[0].events[0]").exists())
                                        .check(
                                            jsonPath("$.enrollments[0].events[0].event")
                                                .saveAs("eventUid")))
                                    .exitHereIfFailed()
                                    .exec(getFirstEnrollment.action())
                                    .exec(getRelationshipsForTrackedEntity.action())
                                    .group("Get one event")
                                    .on(
                                        exec(getFirstEventFromEnrollment.action())
                                            .exec(getRelationshipsForEvent.action())))));

    return new ScenarioWithRequests(
        scenarioBuilder,
        List.of(
            notFoundTeByNameWithLikeOperator,
            notFoundTeByNationalIdWithEqualOperator,
            searchTeByNameWithLikeOperator,
            searchTeByNationalIdWithEqualOperator,
            searchEventsByProgramStage,
            getTrackedEntitiesForEvents,
            getFirstPageOfTEs,
            getFirstTrackedEntity,
            getFirstEnrollment,
            getRelationshipsForTrackedEntity,
            getFirstEventFromEnrollment,
            getRelationshipsForEvent));
  }

  /**
   * Builds the open injection profile for load and capacity test types based on test configuration.
   *
   * @see <a
   *     href="https://docs.gatling.io/guides/optimize-scripts/writing-realistic-tests/#injection-profiles">Gatling
   *     Injection Profiles</a>
   * @return List of OpenInjectionStep for the configured profile
   */
  private List<OpenInjectionStep> buildInjectionProfile() {
    return switch (this.profile) {
      case LOAD ->
          // Load Testing: Gradual ramp-up -> Sustained peak
          List.of(
              rampUsersPerSec(1)
                  .to(this.usersPerSec)
                  .during(Duration.ofSeconds(this.rampDurationSec)),
              constantUsersPerSec(this.usersPerSec).during(Duration.ofSeconds(this.durationSec)));

      case CAPACITY ->
          // Capacity Testing: Stepped progressive increases (staircase pattern)
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

  /**
   * Returns assertions for the given profile, including a global success rate assertion and any
   * profile-specific P95 response time assertions defined in the requests.
   *
   * @param profile The test profile
   * @param scenarios The scenarios with their requests
   * @return List of assertions for the profile
   */
  private List<Assertion> getAssertions(Profile profile, ScenarioWithRequests... scenarios) {
    return Stream.concat(
            Stream.of(forAll().successfulRequests().percent().gte(100d)),
            Arrays.stream(scenarios)
                .flatMap(scenario -> scenario.requests().stream())
                .flatMap(r -> r.assertion(profile).stream()))
        .toList();
  }
}
