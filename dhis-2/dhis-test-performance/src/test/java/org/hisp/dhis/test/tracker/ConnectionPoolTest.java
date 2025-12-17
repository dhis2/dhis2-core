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

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/**
 * Demonstrates the N×M connection pool exhaustion issue in the tracker API.
 *
 * <p>This test makes sequential requests with increasing pageSize to show how connection usage
 * scales with the number of tracked entities returned. The relationship is approximately:
 *
 * <pre>
 *   connections ≈ 2 (base) + N (per TE for enrollments) + N×M (per enrollment for events)
 * </pre>
 *
 * <p>With Sierra Leone data and the "martha" filter (from DHIS2-20484 analysis):
 *
 * <table>
 *   <tr><th>TEs</th><th>Connections</th><th>Connections/TE</th></tr>
 *   <tr><td>5</td><td>25</td><td>5.0</td></tr>
 *   <tr><td>25</td><td>105</td><td>4.2</td></tr>
 *   <tr><td>50</td><td>205</td><td>4.1</td></tr>
 *   <tr><td>100</td><td>405</td><td>4.05</td></tr>
 * </table>
 *
 * <h2>Usage</h2>
 *
 * <pre>
 * # Start with monitoring profile to enable Prometheus
 * DHIS_CONF_FILE=dhis-metrics.conf docker compose --profile monitoring up -d
 *
 * # Run the test
 * DHIS2_IMAGE=dhis2/core-dev:latest \
 * SIMULATION_CLASS=org.hisp.dhis.test.tracker.ConnectionPoolTest \
 * ./run-simulation.sh
 *
 * # View connection metrics in Prometheus (http://localhost:9090):
 * #   jdbc_connections_active{pool="actual"}
 * </pre>
 *
 * <h2>Expected Result</h2>
 *
 * <p>The Prometheus graph of {@code jdbc_connections_active} should show staircase spikes: 25 → 105
 * → 205 → 405 active connections, clearly demonstrating linear scaling with TEs.
 *
 * @see <a href="https://dhis2.atlassian.net/browse/DHIS2-20484">DHIS2-20484</a>
 */
public class ConnectionPoolTest extends Simulation {

  private final String instance = System.getProperty("instance", "http://localhost:8080");
  private final String adminUser = System.getProperty("adminUser", "admin");
  private final String adminPassword = System.getProperty("adminPassword", "district");
  private final String trackerProgram = System.getProperty("trackerProgram", "ur1Edk5Oe2n");

  // Pause between requests to let metrics scrape capture the connection spike
  private final int pauseBetweenSec = Integer.getInteger("pauseBetweenSec", 5);

  public ConnectionPoolTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(instance)
            .acceptHeader("application/json")
            .basicAuth(adminUser, adminPassword)
            .disableCaching()
            .check(status().is(200));

    // Base URL for tracked entities query - "martha" returns 200+ TEs in Sierra Leone DB
    String baseUrl =
        "/api/tracker/trackedEntities"
            + "?filter=w75KJ2mc4zz:like:martha"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit"
            + "&program="
            + trackerProgram
            + "&orgUnitMode=ACCESSIBLE"
            + "&page=1";

    // Sequential requests with increasing page sizes to demonstrate N×M scaling
    // Expected connections from DHIS2-20484 analysis: 5→25, 25→105, 50→205, 100→405
    ScenarioBuilder scenario =
        scenario("Connection Pool N×M Demo")
            .exec(http("pageSize=5").get(baseUrl + "&pageSize=5").check(status().is(200)))
            .pause(pauseBetweenSec)
            .exec(http("pageSize=25").get(baseUrl + "&pageSize=25").check(status().is(200)))
            .pause(pauseBetweenSec)
            .exec(http("pageSize=50").get(baseUrl + "&pageSize=50").check(status().is(200)))
            .pause(pauseBetweenSec)
            .exec(http("pageSize=100").get(baseUrl + "&pageSize=100").check(status().is(200)))
            .pause(pauseBetweenSec);

    setUp(scenario.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
