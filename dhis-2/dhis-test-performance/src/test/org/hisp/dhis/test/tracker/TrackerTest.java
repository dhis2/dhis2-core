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
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class TrackerTest extends Simulation {

  public TrackerTest() {
    String repeat = System.getProperty("repeat", "100");
    String eventProgram = System.getProperty("eventProgram", "VBqh0ynB2wv");
    String trackerProgram = System.getProperty("trackerProgram", "ur1Edk5Oe2n");

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .maxConnectionsPerHost(100)
            .header("Content-Type", "application/json")
            .userAgentHeader("Gatling/Performance Test")
            .warmUp(
                "http://localhost:8080/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching(); // to repeat the same request without HTTP cache influence (304)

    // only one user at a time
    setUp(
            eventProgramScenario(repeat, eventProgram)
                .injectClosed(constantConcurrentUsers(1).during(1))
                .andThen(
                    trackerProgramScenario(repeat, trackerProgram)
                        .injectClosed(constantConcurrentUsers(1).during(1))))
        .protocols(httpProtocolBuilder)
        .assertions(
            forAll().successfulRequests().percent().gte(100d),
            details("Get a list of single events", "Go to first page of program " + eventProgram)
                .responseTime()
                .percentile(90)
                .lte(100),
            details("Get a list of single events", "Go to second page of program " + eventProgram)
                .responseTime()
                .percentile(90)
                .lte(100),
            details(
                    "Get a list of single events",
                    "Go back to first page of program " + eventProgram)
                .responseTime()
                .percentile(90)
                .lte(100),
            details("Get a list of single events", "Get one single event", "Get first event")
                .responseTime()
                .percentile(90)
                .lte(25),
            details(
                    "Get a list of single events",
                    "Get one single event",
                    "Get relationships for first event")
                .responseTime()
                .percentile(90)
                .lte(10),
            details("Get a list of TEs", "Not found TE by name")
                .responseTime()
                .percentile(90)
                .lte(200),
            details("Get a list of TEs", "Not found TE by national id")
                .responseTime()
                .percentile(90)
                .lte(10),
            details("Get a list of TEs", "Search TE by attributes")
                .responseTime()
                .percentile(90)
                .lte(200),
            details("Get a list of TEs", "Search TE by national id")
                .responseTime()
                .percentile(90)
                .lte(10),
            details("Get a list of TEs", "Search events by program stage")
                .responseTime()
                .percentile(90)
                .lte(100),
            details("Get a list of TEs", "Get first page of TEs of program" + trackerProgram)
                .responseTime()
                .percentile(90)
                .lte(200),
            details("Get a list of TEs", "Go to single enrollment", "Get first tracked entity")
                .responseTime()
                .percentile(90)
                .lte(50),
            details("Get a list of TEs", "Go to single enrollment", "Get first enrollment")
                .responseTime()
                .percentile(90)
                .lte(15),
            details(
                    "Get a list of TEs",
                    "Go to single enrollment",
                    "Get relationships for first tracked entity")
                .responseTime()
                .percentile(90)
                .lte(10),
            details(
                    "Get a list of TEs",
                    "Go to single enrollment",
                    "Get one event",
                    "Get relationships for first event")
                .responseTime()
                .percentile(90)
                .lte(10),
            details(
                    "Get a list of TEs",
                    "Go to single enrollment",
                    "Get one event",
                    "Get first event from enrollment")
                .responseTime()
                .percentile(90)
                .lte(25));
  }

  private ScenarioBuilder eventProgramScenario(String repeat, String eventProgram) {
    String singleEventUrl = "/api/tracker/events/#{eventUid}";
    String relationshipUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    // get a 100 requests per run irrespective of the response times so comparisons are likely
    // to be more accurate
    String getEventsUrl =
        "/api/tracker/events?program="
            + eventProgram
            + "&fields=dataValues,occurredAt,event,status,orgUnit,program,programType,updatedAt,createdAt,assignedUser,&orgUnit=DiszpKrYNg8&orgUnitMode=SELECTED&order=occurredAt:desc";

    return scenario("Single Events")
        .exec(
            http("Login")
                .post("/api/auth/login")
                .body(StringBody("{\"username\":\"admin\",\"password\":\"district\"}"))
                .check(status().is(200)))
        .repeat(Integer.parseInt(repeat))
        .on(
            group("Get a list of single events")
                .on(
                    exec(http("Go to first page of program " + eventProgram)
                            .get(getEventsUrl)
                            .check(status().is(200)))
                        .exec(
                            http("Go to second page of program " + eventProgram)
                                .get(getEventsUrl + "&page=2")
                                .check(status().is(200)))
                        .exec(
                            http("Go back to first page of program " + eventProgram)
                                .get(getEventsUrl)
                                .check(status().is(200))
                                .check(jsonPath("$.events[0].event").saveAs("eventUid")))
                        .group("Get one single event")
                        .on(
                            exec(http("Get first event")
                                    .get(singleEventUrl)
                                    .check(status().is(200)))
                                .exec(
                                    http("Get relationships for first event")
                                        .get(relationshipUrl)
                                        .check(status().is(200))))));
  }

  private ScenarioBuilder trackerProgramScenario(String repeat, String trackerProgram) {
    String getTEsUrl =
        "/api/tracker/trackedEntities?order=createdAt:desc &page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program="
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

    String searchTEByAttributes =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:Ines&filter=zDhUuAYrxNC:like:Bebea"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchEventByProgramStage =
        "/api/tracker/events?filter=yLIPuJHRgey:ge:50&order=createdAt:desc&page=1"
            + "&pageSize=15&orgUnit=DiszpKrYNg8&orgUnitMode=SELECTED&program="
            + trackerProgram
            + "&programStage=jdRD35YwbRH&fields=*";

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

    return scenario("Tracker Program")
        .exec(
            http("Login")
                .post("/api/auth/login")
                .body(StringBody("{\"username\":\"admin\",\"password\":\"district\"}"))
                .check(status().is(200)))
        .repeat(Integer.parseInt(repeat))
        .on(
            group("Get a list of TEs")
                .on(
                    exec(http("Not found TE by name").get(notFoundTEByName).check(status().is(200)))
                        .exec(
                            http("Not found TE by national id")
                                .get(notFoundByNationalId)
                                .check(status().is(200)))
                        .exec(
                            http("Search TE by attributes")
                                .get(searchTEByAttributes)
                                .check(status().is(200)))
                        .exec(
                            http("Search TE by national id")
                                .get(searchForTEByNationalId)
                                .check(status().is(200)))
                        .exec(
                            http("Search events by program stage")
                                .get(searchEventByProgramStage)
                                .check(status().is(200)))
                        .exec(
                            http("Get first page of TEs of program" + trackerProgram)
                                .get(getTEsUrl)
                                .check(status().is(200))
                                .check(
                                    jsonPath("$.trackedEntities[0].trackedEntity")
                                        .saveAs("trackedEntityUid")))
                        .exec(
                            group("Go to single enrollment")
                                .on(
                                    exec(http("Get first tracked entity")
                                            .get(singleTrackedEntityUrl)
                                            .check(status().is(200))
                                            .check(
                                                jsonPath("$.enrollments[0].enrollment")
                                                    .saveAs("enrollmentUid"))
                                            .check(
                                                jsonPath("$.enrollments[0].events[0].event")
                                                    .saveAs("eventUid")))
                                        .exec(
                                            http("Get first enrollment")
                                                .get(singleEnrollmentUrl)
                                                .check(status().is(200)))
                                        .exec(
                                            http("Get relationships for first tracked entity")
                                                .get(relationshipForTrackedEntityUrl)
                                                .check(status().is(200)))
                                        .exec(
                                            group("Get one event")
                                                .on(
                                                    exec(http("Get first event from enrollment")
                                                            .get(eventUrl)
                                                            .check(status().is(200)))
                                                        .exec(
                                                            http("Get relationships for first event")
                                                                .get(relationshipForEventUrl)
                                                                .check(status().is(200)))))))));
  }
}
