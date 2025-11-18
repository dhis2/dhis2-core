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
import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.incrementUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.OpenInjectionStep;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackerTest extends Simulation {

  private static final List<Map<String, Object>> userCredentials = new ArrayList<>();
  private static FeederBuilder<Object> userFeeder;

  public TrackerTest() {
    String instance = System.getProperty("instance", "http://localhost:8080");
    String eventProgram = System.getProperty("eventProgram", "VBqh0ynB2wv");
    String trackerProgram = System.getProperty("trackerProgram", "ur1Edk5Oe2n");

    // Injection profile configuration
    String profile = System.getProperty("profile", "load");
    int users = Integer.getInteger("users", 4);
    int provisionUsers = Integer.getInteger("provisionUsers", users);

    // Set profile-aware defaults for duration and rampDuration
    int defaultDuration =
        switch (profile.toLowerCase()) {
          case "load" -> 300; // 5 minutes sustained
          case "capacity" -> 30; // 30 seconds per step
          default ->
              throw new IllegalArgumentException(
                  "Unknown profile: " + profile + ". Valid options: load, capacity");
        };

    int defaultRampDuration =
        switch (profile.toLowerCase()) {
          case "load" -> 60; // 1 minute ramp-up
          case "capacity" -> 10; // 10 seconds between steps
          default ->
              throw new IllegalArgumentException(
                  "Unknown profile: " + profile + ". Valid options: load, capacity");
        };

    int duration = Integer.getInteger("duration", defaultDuration);
    int rampDuration = Integer.getInteger("rampDuration", defaultRampDuration);
    int steps = Integer.getInteger("steps", 10); // number of steps for stress profile

    // Provision users via DHIS2 API
    String adminUser = System.getProperty("adminUser", "admin");
    String adminPassword = System.getProperty("adminPassword", "district");
    String replicaUser = System.getProperty("replicaUser", "tracker");
    String replicaPassword = System.getProperty("replicaPassword", "Tracker123!");

    try {
      provisionUsers(
          instance, adminUser, adminPassword, replicaUser, replicaPassword, provisionUsers);
    } catch (Exception e) {
      throw new RuntimeException("User provisioning failed", e);
    }

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl(instance)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/Performance Test")
            .disableFollowRedirect()
            .warmUp(
                instance
                    + "/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching() // to repeat the same request without HTTP cache influence (304)
            .check(status().is(200)); // global check for all requests

    // only one user at a time
    ScenarioWithRequests eventScenario = eventProgramScenario(eventProgram);
    ScenarioWithRequests trackerScenario = trackerProgramScenario(trackerProgram);

    List<Assertion> allAssertions = new ArrayList<>();
    allAssertions.add(forAll().successfulRequests().percent().gte(100d));
    allAssertions.addAll(eventScenario.requests().stream().map(Request::assertion).toList());
    allAssertions.addAll(trackerScenario.requests().stream().map(Request::assertion).toList());

    OpenInjectionStep[] injectionProfile =
        getInjectionProfile(profile, users, duration, rampDuration, steps);

    setUp(
            eventScenario
                .scenario()
                .injectOpen(injectionProfile)
                .andThen(trackerScenario.scenario().injectOpen(injectionProfile)))
        .protocols(httpProtocolBuilder)
        .assertions(allAssertions);
  }

  /**
   * Provisions test users by replicating a source user via DHIS2 API.
   *
   * @param baseUrl DHIS2 instance URL
   * @param adminUser Admin username
   * @param adminPassword Admin password
   * @param replicaUser Source user to replicate
   * @param replicaPassword Password for new users
   * @param count Number of users to create
   */
  private void provisionUsers(
      String baseUrl,
      String adminUser,
      String adminPassword,
      String replicaUser,
      String replicaPassword,
      int count)
      throws Exception {
    System.out.println("Provisioning " + count + " test users...");

    HttpClient client = HttpClient.newBuilder().build();
    String auth =
        Base64.getEncoder()
            .encodeToString((adminUser + ":" + adminPassword).getBytes(StandardCharsets.UTF_8));

    // Get source user ID
    HttpRequest getUserRequest =
        HttpRequest.newBuilder()
            .uri(
                URI.create(baseUrl + "/api/users?filter=username:eq:" + replicaUser + "&fields=id"))
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

    // Extract user ID using regex (simple JSON parsing)
    Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    Matcher matcher = pattern.matcher(getUserResponse.body());
    if (!matcher.find()) {
      throw new RuntimeException("Could not find source user '" + replicaUser + "'");
    }
    String userId = matcher.group(1);
    System.out.println("Found source user '" + replicaUser + "' with ID: " + userId);

    // Create replicas with throttling to avoid hammering the system
    int provisionDelay =
        Integer.getInteger("provisionDelay", 100); // milliseconds between user creations
    for (int i = 1; i <= count; i++) {
      String username = String.format("%s_user_%03d", replicaUser, i);
      String requestBody =
          String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, replicaPassword);

      HttpRequest replicateRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(baseUrl + "/api/users/" + userId + "/replica"))
              .header("Authorization", "Basic " + auth)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> replicateResponse =
          client.send(replicateRequest, HttpResponse.BodyHandlers.ofString());

      if (replicateResponse.statusCode() == 201) {
        // User successfully created
        Map<String, Object> userCred = new HashMap<>();
        userCred.put("username", username);
        userCred.put("password", replicaPassword);
        userCredentials.add(userCred);
        System.out.println("  Created user " + i + "/" + count + ": " + username);
      } else if (replicateResponse.statusCode() == 409
          && replicateResponse.body().contains("Username already taken")) {
        // User already exists - this is OK, add to credentials
        Map<String, Object> userCred = new HashMap<>();
        userCred.put("username", username);
        userCred.put("password", replicaPassword);
        userCredentials.add(userCred);
        System.out.println("  User already exists " + i + "/" + count + ": " + username);
      } else {
        // Actual error - fail fast
        throw new RuntimeException(
            "Failed to create user "
                + username
                + ": HTTP "
                + replicateResponse.statusCode()
                + " - "
                + replicateResponse.body());
      }

      // Throttle user creation to avoid overwhelming the system
      if (i < count && provisionDelay > 0) {
        Thread.sleep(provisionDelay);
      }
    }

    // Initialize feeder with provisioned credentials
    // Use circular to reuse users across multiple VU executions
    // As long as provisionUsers >= peak concurrent VUs, no concurrent sessions for same user
    userFeeder = io.gatling.javaapi.core.CoreDsl.listFeeder(userCredentials).circular();

    System.out.println("User provisioning complete! Total users: " + userCredentials.size());

    // Wait for system to stabilize after user creation
    int pauseAfterProvisioning = Integer.getInteger("pauseAfterProvisioning", 5); // seconds
    if (pauseAfterProvisioning > 0) {
      System.out.println("Waiting " + pauseAfterProvisioning + "s for system to stabilize...");
      Thread.sleep(pauseAfterProvisioning * 1000L);
      System.out.println("Starting test execution...");
    }
  }

  /**
   * Returns the injection profile based on the specified test type.
   *
   * @see <a
   *     href="https://docs.gatling.io/guides/optimize-scripts/writing-realistic-tests/#injection-profiles">Gatling
   *     Injection Profiles</a>
   *     <p><b>Profiles:</b>
   *     <ul>
   *       <li><b>load</b> - Gradual ramp-up to sustained load <br>
   *           Shape: ___/‾‾‾‾‾‾‾‾‾ <br>
   *           Uses: users, duration (sustained), rampDuration (ramp-up) <br>
   *           Default: 1min ramp → 5min sustained (6min total) <br>
   *           Example: {@code -Dprofile=load -Dusers=4 -DrampDuration=60 -Dduration=300}
   *           <p>For soak testing, use load with longer duration: {@code -Dprofile=load -Dusers=4
   *           -Dduration=3600}
   *       <li><b>capacity</b> - Staircase pattern to find maximum sustainable users <br>
   *           Shape: <br>
   *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
   *           #steps <br>
   *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/‾‾ <br>
   *           &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/‾‾ <br>
   *           __/‾‾ <br>
   *           Uses: users (target), duration (per step), rampDuration (between steps), steps <br>
   *           Default: 10 steps × 30s with 10s ramps (6.5min total) <br>
   *           Example: {@code -Dprofile=capacity -Dusers=10 -Dsteps=10 -Dduration=30
   *           -DrampDuration=10}
   *     </ul>
   *
   * @param profile Profile type: load, capacity
   * @param users Target users per second (meaning varies by profile)
   * @param duration Duration in seconds (meaning varies by profile)
   * @param rampDuration Ramp duration in seconds (meaning varies by profile)
   * @param steps Number of steps for capacity profile
   * @return OpenInjectionStep array for the specified profile
   */
  private OpenInjectionStep[] getInjectionProfile(
      String profile, int users, int duration, int rampDuration, int steps) {
    return switch (profile.toLowerCase()) {
      case "load" ->
          // Load Testing: Gradual ramp-up → Sustained peak
          new OpenInjectionStep[] {
            rampUsersPerSec(1).to(users).during(Duration.ofSeconds(rampDuration)),
            constantUsersPerSec(users).during(Duration.ofSeconds(duration))
          };

      case "capacity" ->
          // Capacity Testing: Stepped progressive increases (staircase pattern)
          // duration = time to hold each step, rampDuration = time to ramp between steps
          new OpenInjectionStep[] {
            incrementUsersPerSec((double) users / steps)
                .times(steps)
                .eachLevelLasting(Duration.ofSeconds(duration))
                .separatedByRampsLasting(Duration.ofSeconds(rampDuration))
                .startingFrom((double) users / steps)
          };

      default ->
          throw new IllegalArgumentException(
              "Unknown profile: " + profile + ". Valid options: load, capacity");
    };
  }

  private ScenarioWithRequests eventProgramScenario(String eventProgram) {
    String singleEventUrl = "/api/tracker/events/#{eventUid}";
    String relationshipUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    String getEventsUrl =
        "/api/tracker/events?program="
            + eventProgram
            + "&fields=dataValues,occurredAt,event,status,orgUnit,program,programType,updatedAt,createdAt,assignedUser,"
            + "&orgUnit=DiszpKrYNg8"
            + "&orgUnitMode=SELECTED"
            + "&order=occurredAt:desc";

    Request goToFirstPage =
        new Request(
            getEventsUrl,
            100,
            "Go to first page of program " + eventProgram,
            "Get a list of single events");
    Request goToSecondPage =
        new Request(
            getEventsUrl + "&page=2",
            100,
            "Go to second page of program " + eventProgram,
            "Get a list of single events");
    Request searchSingleEvents =
        new Request(
            getEventsUrl + "&occurredAfter=2024-01-01&occurredBefore=2024-12-31",
            100,
            "Search single events in date interval in program " + eventProgram,
            "Get a list of single events");
    Request getFirstEvent =
        new Request(
            singleEventUrl,
            25,
            "Get first event",
            "Get a list of single events",
            "Get one single event");
    Request getRelationshipsForFirstEvent =
        new Request(
            relationshipUrl,
            10,
            "Get relationships for first event",
            "Get a list of single events",
            "Get one single event");

    ScenarioBuilder scenarioBuilder =
        scenario("Single Events")
            .feed(userFeeder)
            .exec(login())
            .exitHereIfFailed()
            .group("Get a list of single events")
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
                    .on(exec(getFirstEvent.action()).exec(getRelationshipsForFirstEvent.action())));

    return new ScenarioWithRequests(
        scenarioBuilder,
        List.of(
            goToFirstPage,
            goToSecondPage,
            searchSingleEvents,
            getFirstEvent,
            getRelationshipsForFirstEvent));
  }

  private ScenarioWithRequests trackerProgramScenario(String trackerProgram) {
    String getTEsUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc &page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program="
            + trackerProgram
            + "&fields=:all,!relationships,programOwner[orgUnit,program]";

    String searchForTEByNationalId =
        "/api/tracker/trackedEntities?orgUnitMode=ACCESSIBLE&program="
            + trackerProgram
            + "&filter=AuPLng5hLbE:eq:123";

    String notFoundByNationalId =
        "/api/tracker/trackedEntities?orgUnitMode=ACCESSIBLE&program="
            + trackerProgram
            + "&filter=AuPLng5hLbE:eq:aaa";

    String notFoundTEByName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:notfoundname"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:Ines"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchEventByProgramStage =
        "/api/tracker/events?filter=yLIPuJHRgey:ge:50&order=createdAt:desc&page=1"
            + "&pageSize=15&orgUnit=DiszpKrYNg8&orgUnitMode=SELECTED&program="
            + trackerProgram
            + "&programStage=jdRD35YwbRH&fields=*";

    String getTEsFromEvents =
        "/api/tracker/trackedEntities?pageSize=15&program="
            + trackerProgram
            + "&trackedEntities=#{trackedEntityUids}&fields=trackedEntity,createdAt,attributes[attribute,value],programOwners[orgUnit],enrollments[enrollment,status,orgUnit,enrolledAt]";

    String singleTrackedEntityUrl =
        "/api/tracker/trackedEntities/#{trackedEntityUid}?program"
            + trackerProgram
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
            notFoundTEByName, 200, "Not found TE by name with like operator", "Get a list of TEs");
    Request notFoundTeByNationalIdWithEqualOperator =
        new Request(
            notFoundByNationalId,
            10,
            "Not found TE by national id with eq operator",
            "Get a list of TEs");
    Request searchTeByNameWithLikeOperator =
        new Request(
            searchTEByName, 200, "Search TE by name with like operator", "Get a list of TEs");
    Request searchTeByNationalIdWithEqualOperator =
        new Request(
            searchForTEByNationalId,
            10,
            "Search TE by national id with eq operator",
            "Get a list of TEs");
    Request searchEventsByProgramStage =
        new Request(
            searchEventByProgramStage, 25, "Search events by program stage", "Get a list of TEs");
    Request getTrackedEntitiesForEvents =
        new Request(getTEsFromEvents, 25, "Get tracked entities from events", "Get a list of TEs");
    Request getFirstPageOfTEs =
        new Request(
            getTEsUrl,
            200,
            "Get first page of TEs of program " + trackerProgram,
            "Get a list of TEs");
    Request getFirstTrackedEntity =
        new Request(
            singleTrackedEntityUrl,
            50,
            "Get first tracked entity",
            "Get a list of TEs",
            "Go to single enrollment");
    Request getFirstEnrollment =
        new Request(
            singleEnrollmentUrl,
            15,
            "Get first enrollment",
            "Get a list of TEs",
            "Go to single enrollment");
    Request getRelationshipsForTrackedEntity =
        new Request(
            relationshipForTrackedEntityUrl,
            10,
            "Get relationships for first tracked entity",
            "Get a list of TEs",
            "Go to single enrollment");
    Request getFirstEventFromEnrollment =
        new Request(
            eventUrl,
            25,
            "Get first event from enrollment",
            "Get a list of TEs",
            "Go to single enrollment",
            "Get one event");
    Request getRelationshipsForEvent =
        new Request(
            relationshipForEventUrl,
            10,
            "Get relationships for first event",
            "Get a list of TEs",
            "Go to single enrollment",
            "Get one event");

    ScenarioBuilder scenarioBuilder =
        scenario("Tracker Program")
            .feed(userFeeder)
            .exec(login())
            .exitHereIfFailed()
            .group("Get a list of TEs")
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
                                        list -> String.join(",", list.stream().distinct().toList()))
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
                                    jsonPath("$.enrollments[0].enrollment").saveAs("enrollmentUid"))
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
                                    .exec(getRelationshipsForEvent.action()))));

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

  private HttpRequestActionBuilder login() {
    return http("Login")
        .post("/api/auth/login")
        .header("Content-Type", "application/json")
        .body(StringBody("{\"username\":\"#{username}\",\"password\":\"#{password}\"}"))
        .check(status().is(200));
  }

  private record Request(String url, int ninetyPercentile, String name, String... groups) {
    HttpRequestActionBuilder action() {
      return http(name).get(url);
    }

    Assertion assertion() {
      String[] allParts = new String[groups.length + 1];
      System.arraycopy(groups, 0, allParts, 0, groups.length);
      allParts[groups.length] = name;
      return details(allParts).responseTime().percentile(90).lte(ninetyPercentile);
    }
  }

  private record ScenarioWithRequests(ScenarioBuilder scenario, List<Request> requests) {}
}
