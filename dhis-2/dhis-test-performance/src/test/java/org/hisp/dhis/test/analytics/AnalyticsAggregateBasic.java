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
package org.hisp.dhis.test.analytics;

import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static org.hisp.dhis.test.analytics.TestDefinitions.BASE_URL;
import static org.hisp.dhis.test.analytics.TestDefinitions.loginChain;

import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class AnalyticsAggregateBasic extends Simulation {

  private static final String GET_AGGREGATED_ANALYTICS = "GET AGGREGATED ANALYTICS";
  public static final String URL_QUERY =
      "/api/analytics?dimension=dx:GSae40Fyppf,pe:LAST_10_YEARS;&filter=ou:USER_ORGUNIT&displayProperty=NAME&includeNumDen=true&skipMeta=true&skipData=false&relativePeriodDate=2026-04-30";

  @Override
  public void before() {
    // TODO: This test assumes that the export process was fully executed. Needs to review it.
  }

  public AnalyticsAggregateBasic() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + URL_QUERY)
            .disableCaching();

    // The scenario includes a login step and the target API call step.
    // The scenarios are grouped, so we can assert on the target API call only (login stats are
    // ignored).
    ScenarioBuilder scenario =
        scenario("Analytics test")
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_AGGREGATED_ANALYTICS)
            .on(
                repeat(1)
                    .on(
                        exec(http(GET_AGGREGATED_ANALYTICS)
                                .get(URL_QUERY)
                                .basicAuth("admin", "district"))
                            .pause(1)));

    // How users should enter the scenarios.
    ClosedInjectionStep closedInjection = rampConcurrentUsers(0).to(1).during(10);

    // Bringing all parts together (scenarios, injection, protocol, assertions).
    setUp(scenario.injectClosed(closedInjection))
        .protocols(httpProtocol)
        .assertions(
            details(GET_AGGREGATED_ANALYTICS).responseTime().percentile(95).lt(380),
            details(GET_AGGREGATED_ANALYTICS).responseTime().max().lt(400),
            details(GET_AGGREGATED_ANALYTICS).successfulRequests().percent().is(100D),
            details(GET_AGGREGATED_ANALYTICS).successfulRequests().percent().is(100D));
  }
}
