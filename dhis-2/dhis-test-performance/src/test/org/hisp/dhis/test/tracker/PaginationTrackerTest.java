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

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.util.ArrayList;
import java.util.List;

public class PaginationTrackerTest extends Simulation {

  public PaginationTrackerTest() {
    String repeat = System.getProperty("repeat", "100");
    String eventProgram = System.getProperty("eventProgram", "VBqh0ynB2wv");
    String trackerProgram = System.getProperty("trackerProgram", "IpHINAT79UW");

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .maxConnectionsPerHost(100)
            //            .basicAuth("admin", "district")
            .userAgentHeader("Gatling/Performance Test")
            .warmUp(
                "http://localhost:8080/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching() // to repeat the same request without HTTP cache influence (304)
            .check(status().is(200)); // global check for all requests

    // only one user at a time
    ScenarioWithRequests scenario = paginationScenario(repeat);

    List<Assertion> allAssertions = new ArrayList<>();
    allAssertions.add(forAll().successfulRequests().percent().gte(100d));
    allAssertions.addAll(scenario.requests().stream().map(Request::assertion).toList());

    setUp(scenario.scenario().injectClosed(constantConcurrentUsers(1).during(1)))
        .protocols(httpProtocolBuilder)
        .assertions(allAssertions);
  }

  private ScenarioWithRequests paginationScenario(String repeat) {
    String singleEventUrl = "/api/tracker/events/#{eventUid}";
    String getEventsUrl =
        "/api/tracker/events?"
            + "occurredAfter=2024-01-01"
            + "&occurredBefore=2024-12-31"
            + "&fields=:all,dataValues[value,dataElement,providedElsewhere,storedBy]"
            + "&order=event"
            + "&pageSize=50"
            + "&orgUnitMode=ACCESSIBLE";

    Request goToFirstPage =
        new Request(getEventsUrl, 100, "Go to first page", "Get a list of events");
    Request goToPage45 =
        new Request(getEventsUrl + "&page=45", 100, "Go to page 45 ", "Get a list of events");
    Request goToFirstPageAndGetAllPages =
        new Request(
            getEventsUrl + "&totalPages=true",
            450,
            "Go to first page with totalPages",
            "Get a list of events");
    Request goToPage45AndGetAllPages =
        new Request(
            getEventsUrl + "&page=45&totalPages=true",
            850,
            "Go to page 45 with totalPages",
            "Get a list of events");

    Request getFirstEvent =
        new Request(singleEventUrl, 25, "Get first event", "Get a list of events", "Get one event");

    ScenarioBuilder scenarioBuilder =
        scenario("Pagination for Events")
            .exec(login())
            .repeat(Integer.parseInt(repeat))
            .on(
                group("Get a list of events")
                    .on(
                        exec(goToFirstPage.action())
                            .exec(goToFirstPageAndGetAllPages.action())
                            .exec(goToPage45.action())
                            .exec(
                                goToPage45AndGetAllPages
                                    .action()
                                    .check(jsonPath("$.events[0].event").saveAs("eventUid")))
                            .group("Get one event")
                            .on(exec(getFirstEvent.action()))));

    return new ScenarioWithRequests(
        scenarioBuilder, List.of(goToPage45AndGetAllPages, getFirstEvent));
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
