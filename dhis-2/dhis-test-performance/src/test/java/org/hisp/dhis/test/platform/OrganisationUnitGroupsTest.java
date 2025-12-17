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
package org.hisp.dhis.test.platform;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

/**
 * Simulation that tests the /api/organisationUnitGroups endpoint in multiple ways:
 *
 * <p>1. Calls the GET /api/organisationUnitGroups endpoint
 *
 * <p>2. Calls the GET /api/organisationUnitGroups?fields=* endpoint
 *
 * <p>10 concurrent users in total, make calls every second for 10 seconds. Users start at 0 and
 * ramp up every second to a max of 5 per endpoint.
 */
public class OrganisationUnitGroupsTest extends Simulation {

  public static final String BASE_URL = "http://localhost:8080";
  private static final String GET_ORG_UNIT_GROUPS = "GET Organisation Unit Groups";
  private static final String GET_ORG_UNIT_GROUPS_ALL_FIELDS =
      "GET Organisation Unit Groups - all fields";

  /**
   * Setup users before simulation runs. This could be a test scenario in its own right, but the aim
   * is to isolate this problematic endpoint in its own test.
   */
  @Override
  public void before() {
    MetadataImporter.importJsonFile("platform/superuser-data-sl-db.json", "admin", "district");
  }

  public OrganisationUnitGroupsTest() {
    // http protocol
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + "/api/ping")
            .disableCaching();

    // virtual user feeder
    FeederBuilder<Object> circularUserFeeder =
        UserFeeder.createUserFeederFromFile(
            "platform/superuser-data-sl-db.json", UserFeeder.Strategy.CIRCULAR);

    // scenarios
    ScenarioBuilder getOrgUnitGroupsScenario =
        scenario(GET_ORG_UNIT_GROUPS)
            .feed(circularUserFeeder)
            .repeat(1)
            .on(
                exec(http(GET_ORG_UNIT_GROUPS)
                        .get("/api/organisationUnitGroups")
                        .basicAuth("#{username}", "#{password}"))
                    .pause(1));

    ScenarioBuilder getOrgUnitGroupsAllFieldsScenario =
        scenario(GET_ORG_UNIT_GROUPS_ALL_FIELDS)
            .feed(circularUserFeeder)
            .repeat(1)
            .on(
                exec(http(GET_ORG_UNIT_GROUPS_ALL_FIELDS)
                        .get("/api/organisationUnitGroups")
                        .queryParam("fields", "*")
                        .basicAuth("#{username}", "#{password}"))
                    .pause(1));

    // how users should enter the scenarios
    ClosedInjectionStep closedInjection = rampConcurrentUsers(0).to(5).during(10);

    // final setup, bringing all parts together (scenarios, injection, protocol, assertions)
    setUp(
            getOrgUnitGroupsScenario.injectClosed(closedInjection),
            getOrgUnitGroupsAllFieldsScenario.injectClosed(closedInjection))
        .protocols(httpProtocol)
        .assertions(
            details(GET_ORG_UNIT_GROUPS).responseTime().percentile(95).lt(400),
            details(GET_ORG_UNIT_GROUPS).responseTime().max().lt(800),
            details(GET_ORG_UNIT_GROUPS).successfulRequests().percent().is(100D),
            details(GET_ORG_UNIT_GROUPS_ALL_FIELDS).responseTime().percentile(95).lt(600),
            details(GET_ORG_UNIT_GROUPS_ALL_FIELDS).responseTime().max().lt(1000),
            details(GET_ORG_UNIT_GROUPS_ALL_FIELDS).successfulRequests().percent().is(100D));
  }
}
