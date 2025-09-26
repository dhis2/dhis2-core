/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class EnrollmentsTest extends Simulation {

  public EnrollmentsTest() {
    String baseUrl = System.getProperty("instance", "http://localhost:8080");
    String repeat = System.getProperty("repeat", "100");
    String pageSize = System.getProperty("pageSize");
    // TODO maybe try this to see the effect on the response times
    // https://docs.gatling.io/concepts/scenario/#pace
    // String pause = System.getProperty("pause", "0");

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl(baseUrl)
            .acceptHeader("application/json")
            .maxConnectionsPerHost(100)
            .basicAuth("admin", "district")
            .header("Content-Type", "application/json")
            .userAgentHeader("Gatling/Performance Test")
            .warmUp(
                baseUrl
                    + "/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching(); // to repeat the same request without HTTP cache influence (304)

    // https://docs.gatling.io/reference/script/http/protocol/#shareconnections
    // This only has an influence on tests with multiple virtual users (VU).
    //
    // https://stackoverflow.com/questions/34987476/gatling-repeat-with-connection-re-use
    // > By default, Gatling has one connection pool per virtual user, so each of them do re-use
    // > connections between sequential requests, and can have more than one concurrent connection
    // when
    // > dealing with resource fetching, which you do as you enabled inferHtmlResources. This way,
    // virtual
    // > users behave as independent browsers.
    //
    // https://groups.google.com/g/gatling/c/wesGpk4-_eQ/m/FG1V3KDaUtMJ
    // > shareConnections means that you have one single global HTTP connection pool, not one per
    // virtual
    // > user.
    if (System.getProperty("shareConnections") != null) {
      httpProtocolBuilder.shareConnections();
    }

    // get a 100 requests per run irrespective of the response times so comparisons are likely
    // to be more accurate
    String query =
        "/api/tracker/enrollments/KpknKHptul0?fields=enrollment,trackedEntity,program,status,orgUnit,enrolledAt,occurredAt,followUp,deleted,createdBy,updatedBy,updatedAt,geometry";
    if (pageSize != null) {
      query = query + "&pageSize=" + pageSize;
    }
    ScenarioBuilder scenario =
        scenario(query)
            .repeat(Integer.parseInt(repeat))
            .on(http(query).get(query).check(status().is(200)));

    // only one user at a time
    setUp(scenario.injectOpen(OpenInjectionStep.atOnceUsers(1)))
        // setUp(scenario.injectClosed(constantSingleUser(15)))
        .protocols(httpProtocolBuilder)
        .assertions(
            details(query).successfulRequests().percent().gte(100d),
            details(query).responseTime().percentile(90).lte(5000));
  }
}
