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
package org.hisp.dhis.test.analytics.te.query;

import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.hisp.dhis.test.analytics.TestDefinitions.BASE_URL;
import static org.hisp.dhis.test.analytics.TestDefinitions.loginChain;
import static org.hisp.dhis.test.analytics.TestDefinitions.simpleUsersRumpUp;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class AnalyticsTrackedEntityQuery1 extends Simulation {

  private static final String GET_TRACKED_ENTITY_QUERY = "GET TRACKED ENTITY QUERY 1";
  public static final String URL_QUERY =
      "/api/analytics/trackedEntities/query/nEenWmSyUEp?dimension=ou:USER_ORGUNIT,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6&headers=ouname,gHGyrwKPzej,ciq2USN94oJ,cejWyOfXge6,IpHINAT79UW.A03MvHHogjR.bx6fsa0t90x,IpHINAT79UW.A03MvHHogjR.a3kGcGDCuk6,created&totalPages=false&rowContext=true&created=LAST_YEAR&displayProperty=NAME&pageSize=100&page=1&includeMetadataDetails=true&desc=created&relativePeriodDate=2018-01-28";

  public AnalyticsTrackedEntityQuery1() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + URL_QUERY)
            .disableCaching();

    // The scenario includes a login step and the target API call step.
    // The scenarios are grouped, so we can assert on the target API call only (login stats are
    // ignored).
    ScenarioBuilder scenario =
        scenario("Analytics tracked entity query test")
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_TRACKED_ENTITY_QUERY)
            .on(
                repeat(1)
                    .on(
                        exec(http(GET_TRACKED_ENTITY_QUERY)
                                .get(URL_QUERY)
                                .basicAuth("admin", "district"))
                            .pause(1)));

    // How users should enter the scenarios.
    OpenInjectionStep injectionStep = simpleUsersRumpUp(1, 10);

    // Bringing all parts together (scenarios, injection, protocol, assertions).
    setUp(scenario.injectOpen(injectionStep))
        .protocols(httpProtocol)
        .assertions(
            details(GET_TRACKED_ENTITY_QUERY).responseTime().percentile(95).lt(215),
            details(GET_TRACKED_ENTITY_QUERY).responseTime().max().lt(300),
            details(GET_TRACKED_ENTITY_QUERY).successfulRequests().percent().is(100D),
            details(GET_TRACKED_ENTITY_QUERY).successfulRequests().percent().is(100D));
  }
}
