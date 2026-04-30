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
package org.hisp.dhis.test.analytics.enrollment.query;

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

public class AnalyticsEnrollmentQuery1 extends Simulation {

  private static final String GET_ENROLLMENT_QUERY = "GET ENROLLMENT QUERY 1";
  public static final String URL_QUERY =
      "/api/analytics/enrollments/query/ur1Edk5Oe2n?dimension=ou:O6uvpzGd5pu,J5jldMd8OHv,w75KJ2mc4zz,cejWyOfXge6:IN:Female,GUOBQt5K2WI:LIKE:Cape,ZkbAXlQUYJG.U5ubm6PPYrM,lZGmxYbs97q,OvY4VVhSDeJ:LE:45:NE:NV:!EQ:44&headers=ouname,J5jldMd8OHv,w75KJ2mc4zz,cejWyOfXge6,GUOBQt5K2WI,ZkbAXlQUYJG.U5ubm6PPYrM,lZGmxYbs97q,OvY4VVhSDeJ,lastupdated,createdbydisplayname,lastupdatedbydisplayname,enrollmentdate,programstatus&totalPages=false&rowContext=true&lastUpdated=LAST_10_YEARS&programStatus=ACTIVE&displayProperty=NAME&pageSize=100&page=1&includeMetadataDetails=true&outputType=ENROLLMENT&asc=OvY4VVhSDeJ&relativePeriodDate=2022-07-01";

  public AnalyticsEnrollmentQuery1() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + URL_QUERY)
            .disableCaching();

    // The scenario includes a login step and the target API call step.
    // The scenarios are grouped, so we can assert on the target API call only (login stats are
    // ignored).
    ScenarioBuilder scenario =
        scenario("Analytics enrollment query test")
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ENROLLMENT_QUERY)
            .on(
                repeat(1)
                    .on(
                        exec(http(GET_ENROLLMENT_QUERY)
                                .get(URL_QUERY)
                                .basicAuth("admin", "district"))
                            .pause(1)));

    // How users should enter the scenarios.
    OpenInjectionStep injectionStep = simpleUsersRumpUp(1, 10);

    // Bringing all parts together (scenarios, injection, protocol, assertions).
    setUp(scenario.injectOpen(injectionStep))
        .protocols(httpProtocol)
        .assertions(
            details(GET_ENROLLMENT_QUERY).responseTime().percentile(95).lt(140),
            details(GET_ENROLLMENT_QUERY).responseTime().max().lt(200),
            details(GET_ENROLLMENT_QUERY).successfulRequests().percent().is(100D),
            details(GET_ENROLLMENT_QUERY).successfulRequests().percent().is(100D));
  }
}
