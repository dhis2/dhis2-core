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
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
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
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TrackerTest extends Simulation {

  public TrackerTest() {
    String eventProgram = System.getProperty("eventProgram", "VBqh0ynB2wv");
    String trackerProgram = System.getProperty("trackerProgram", "ur1Edk5Oe2n");

    // Injection profile configuration
    String profile = System.getProperty("profile", "load");
    int users = Integer.getInteger("users", 10);
    int duration = Integer.getInteger("duration", 600); // seconds (10 minutes)
    int rampDuration = Integer.getInteger("rampDuration", 300); // seconds (5 minutes)

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .maxConnectionsPerHost(100)
            .userAgentHeader("Gatling/Performance Test")
            .warmUp(
                "http://localhost:8080/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching() // to repeat the same request without HTTP cache influence (304)
            .check(status().is(200)); // global check for all requests

    // only one user at a time
    ScenarioWithRequests eventScenario = eventProgramScenario(eventProgram);
    ScenarioWithRequests trackerScenario = trackerProgramScenario(trackerProgram);

    List<Assertion> allAssertions = new ArrayList<>();
    allAssertions.add(forAll().successfulRequests().percent().gte(100d));
    allAssertions.addAll(eventScenario.requests().stream().map(Request::assertion).toList());
    allAssertions.addAll(trackerScenario.requests().stream().map(Request::assertion).toList());

    OpenInjectionStep[] injectionProfile = getInjectionProfile(profile, users, duration, rampDuration);

    setUp(
            eventScenario
                .scenario()
                .injectOpen(injectionProfile)
                .andThen(trackerScenario.scenario().injectOpen(injectionProfile)))
        .protocols(httpProtocolBuilder)
        .assertions(allAssertions);
  }

  /**
   * Returns the injection profile based on the specified test type.
   *
   * @param profile Profile type: load, stress, spike, soak, capacity
   * @param users Number of users (semantics vary by profile)
   * @param duration Main test duration in seconds
   * @param rampDuration Ramp-up duration in seconds
   * @return OpenInjectionStep for the specified profile
   */
  private OpenInjectionStep[] getInjectionProfile(
      String profile, int users, int duration, int rampDuration) {
    return switch (profile.toLowerCase()) {
      case "load" ->
          // Load Testing: Gradual ramp-up → Sustained peak
          new OpenInjectionStep[] {
            rampUsersPerSec(1).to(users).during(Duration.ofSeconds(rampDuration)),
            constantUsersPerSec(users).during(Duration.ofSeconds(duration))
          };

      case "stress" ->
          // Stress Testing: Stepped progressive increases (staircase pattern)
          new OpenInjectionStep[] {
            incrementUsersPerSec((double) users / 10)
                .times(10)
                .eachLevelLasting(Duration.ofSeconds(duration / 10))
                .separatedByRampsLasting(Duration.ofSeconds(60))
                .startingFrom((double) users / 10)
          };

      case "spike" ->
          // Spike Testing: Baseline → Instant spike → Brief peak
          new OpenInjectionStep[] {
            constantUsersPerSec((double) users / 5).during(Duration.ofSeconds(300)),
            atOnceUsers(users),
            constantUsersPerSec((double) users / 2).during(Duration.ofSeconds(120))
          };

      case "soak" ->
          // Soak Testing: Extended duration for memory leaks
          new OpenInjectionStep[] {
            rampUsersPerSec(1).to(users).during(Duration.ofSeconds(rampDuration)),
            constantUsersPerSec(users).during(Duration.ofHours(4))
          };

      case "capacity" ->
          // Capacity Testing: Continuous increase to breaking point
          new OpenInjectionStep[] {
            rampUsersPerSec(0).to(users * 2).during(Duration.ofSeconds(duration))
          };

      default ->
          throw new IllegalArgumentException(
              "Unknown profile: "
                  + profile
                  + ". Valid options: load, stress, spike, soak, capacity");
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
            .exec(login())
            .group("Get a list of single events")
                    .on(
                        exec(goToFirstPage.action())
                            .exec(goToSecondPage.action())
                            .exec(
                                searchSingleEvents
                                    .action()
                                    .check(jsonPath("$.events[0].event").saveAs("eventUid")))
                            .group("Get one single event")
                            .on(
                                exec(getFirstEvent.action())
                                    .exec(getRelationshipsForFirstEvent.action())));

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
            .exec(login())
            .group("Get a list of TEs")
                    .on(
                        exec(notFoundTeByNameWithLikeOperator.action())
                            .exec(notFoundTeByNationalIdWithEqualOperator.action())
                            .exec(searchTeByNameWithLikeOperator.action())
                            .exec(searchTeByNationalIdWithEqualOperator.action())
                            .exec(
                                searchEventsByProgramStage
                                    .action()
                                    .check(
                                        jsonPath("$.events[*].trackedEntity")
                                            .findAll()
                                            .transform(
                                                list ->
                                                    String.join(
                                                        ",", list.stream().distinct().toList()))
                                            .saveAs("trackedEntityUids")))
                            .exec(getTrackedEntitiesForEvents.action())
                            .exec(
                                getFirstPageOfTEs
                                    .action()
                                    .check(
                                        jsonPath("$.trackedEntities[0].trackedEntity")
                                            .saveAs("trackedEntityUid")))
                            .group("Go to single enrollment")
                            .on(
                                exec(getFirstTrackedEntity
                                        .action()
                                        .check(
                                            jsonPath("$.enrollments[0].enrollment")
                                                .saveAs("enrollmentUid"))
                                        .check(
                                            jsonPath("$.enrollments[0].events[0].event")
                                                .saveAs("eventUid")))
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
        .body(StringBody("{\"username\":\"admin\",\"password\":\"district\"}"))
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
