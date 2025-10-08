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
    String program = System.getProperty("program", "VBqh0ynB2wv");

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .maxConnectionsPerHost(100)
            .header("Content-Type", "application/json")
            .userAgentHeader("Gatling/Performance Test")
            .warmUp(
                "http://localhost:8080/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching(); // to repeat the same request without HTTP cache influence (304)

    String singleEventUrl = "/api/tracker/events/#{eventUid}";
    String relationshipUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    // get a 100 requests per run irrespective of the response times so comparisons are likely
    // to be more accurate
    String getEventsUrl =
        "/api/tracker/events?program="
            + program
            + "&fields=dataValues,occurredAt,event,status,orgUnit,program,programType,updatedAt,createdAt,assignedUser,&orgUnit=DiszpKrYNg8&orgUnitMode=SELECTED&order=occurredAt:desc";

    ScenarioBuilder scenario = scenario("Single Events");

    scenario =
        scenario
            .exec(
                http("Login")
                    .post("/api/auth/login")
                    .body(StringBody("{\"username\":\"admin\",\"password\":\"district\"}"))
                    .check(status().is(200)))
            .repeat(Integer.parseInt(repeat))
            .on(
                exec(http("Go to first page of program " + program)
                        .get(getEventsUrl)
                        .check(status().is(200)))
                    .exec(
                        http("Go to second page of program " + program)
                            .get(getEventsUrl + "&page=2")
                            .check(status().is(200)))
                    .exec(
                        http("Go back to first page of program " + program)
                            .get(getEventsUrl)
                            .check(status().is(200))
                            .check(jsonPath("$.events[0].event").saveAs("eventUid")))
                    .exec(http("Get first event").get(singleEventUrl).check(status().is(200)))
                    .exec(
                        http("Get relationships for first event")
                            .get(relationshipUrl)
                            .check(status().is(200))));

    // only one user at a time
    setUp(scenario.injectClosed(constantConcurrentUsers(1).during(1)))
        .protocols(httpProtocolBuilder)
        .assertions(
            forAll().successfulRequests().percent().gte(100d),
            details("Go to first page of program " + program)
                .responseTime()
                .percentile(90)
                .lte(100),
            details("Go to second page of program " + program)
                .responseTime()
                .percentile(90)
                .lte(100),
            details("Go back to first page of program " + program)
                .responseTime()
                .percentile(90)
                .lte(100),
            details("Get first event").responseTime().percentile(90).lte(25),
            details("Get relationships for first event").responseTime().percentile(90).lte(10));
  }
}
