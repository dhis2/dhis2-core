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
package org.hisp.dhis.test.analytics.event.query;

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

public class AnalyticsEventQuery2 extends Simulation {

  private static final String GET_EVENT_QUERY = "GET EVENT QUERY 2";
  public static final String URL_QUERY =
      "/api/analytics/events/query/eBAyeGv0exc.json?dimension=ou:O6uvpzGd5pu,eMyVanycQSC,qrur9Dvnyt5-Yf6UHoPkdS6:IN:TvM2MQgD7Jd,tUdBD1JDxpn:GT:21,sGna2pquXOO,Kswd1r4qWLh,gWxh7DiRmG7,x7PaHGvgWY2:GT:20,XCMi7Wvnplm:GE:22,hlPt8H4bUOQ,Thkx2BnO5Kq,Y7hKDSuqEtH,K6uUAvq500H:IN:D303,msodh3rEMJa,oZg33kd9taw,GieVkTxp4HH-TBxGTceyzwy:IN:wgbW2ZQnlIc,HS8QXAJtuKV,fWIAEtYVEGk,SWfdB5lX0fk,vV9UWAZohSf-OrkEzxZEH4X&headers=eventdate,ouname,eMyVanycQSC,qrur9Dvnyt5,tUdBD1JDxpn,sGna2pquXOO,Kswd1r4qWLh,gWxh7DiRmG7,x7PaHGvgWY2,XCMi7Wvnplm,hlPt8H4bUOQ,Thkx2BnO5Kq,Y7hKDSuqEtH,K6uUAvq500H,msodh3rEMJa,oZg33kd9taw,GieVkTxp4HH,HS8QXAJtuKV,fWIAEtYVEGk,SWfdB5lX0fk,vV9UWAZohSf&totalPages=false&eventDate=THIS_YEAR&displayProperty=NAME&pageSize=100&page=1&includeMetadataDetails=true&outputType=EVENT&stage=Zj7UnCAulEk&relativePeriodDate=2022-07-01";

  public AnalyticsEventQuery2() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + URL_QUERY)
            .disableCaching();

    // The scenario includes a login step and the target API call step.
    // The scenarios are grouped, so we can assert on the target API call only (login stats are
    // ignored).
    ScenarioBuilder scenario =
        scenario("Analytics event query test")
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_EVENT_QUERY)
            .on(
                repeat(1)
                    .on(
                        exec(http(GET_EVENT_QUERY).get(URL_QUERY).basicAuth("admin", "district"))
                            .pause(1)));

    // How users should enter the scenarios.
    OpenInjectionStep injectionStep = simpleUsersRumpUp(1, 10);

    // Bringing all parts together (scenarios, injection, protocol, assertions).
    setUp(scenario.injectOpen(injectionStep))
        .protocols(httpProtocol)
        .assertions(
            details(GET_EVENT_QUERY).responseTime().percentile(95).lt(240),
            details(GET_EVENT_QUERY).responseTime().max().lt(350),
            details(GET_EVENT_QUERY).successfulRequests().percent().is(100D),
            details(GET_EVENT_QUERY).successfulRequests().percent().is(100D));
  }
}
