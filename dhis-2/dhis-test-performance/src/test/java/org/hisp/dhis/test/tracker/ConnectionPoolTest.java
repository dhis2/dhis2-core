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

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

/**
 * Demonstrates the N×M connection pool exhaustion issue in the tracker API.
 *
 * <p>This test runs a single user making repeated requests for each pageSize, sustaining the load
 * long enough for Prometheus to capture the connection usage pattern.
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
 * <p>The Prometheus graph of {@code jdbc_connections_active} should show staircase pattern: each
 * step corresponds to increasing pageSize (10, 20, 30, 40, 50) with proportionally more active
 * connections.
 *
 * @see <a href="https://dhis2.atlassian.net/browse/DHIS2-20484">DHIS2-20484</a>
 */
public class ConnectionPoolTest extends Simulation {

  private final String instance = System.getProperty("instance", "http://localhost:8080");
  private final String adminUser = System.getProperty("adminUser", "admin");
  private final String adminPassword = System.getProperty("adminPassword", "district");
  private final String trackerProgram = System.getProperty("trackerProgram", "ur1Edk5Oe2n");

  // Duration to sustain each pageSize level (must be long enough for Prometheus to scrape)
  // At 1s scrape interval, 30s gives ~30 samples per step for clear visualization
  private final int stepDurationSec = Integer.getInteger("stepDurationSec", 30);

  // pageSize increments: start at 10, increment by 10, for 5 steps (10, 20, 30, 40, 50)
  private final int pageSizeStart = Integer.getInteger("pageSizeStart", 10);
  private final int pageSizeIncrement = Integer.getInteger("pageSizeIncrement", 10);
  private final int steps = Integer.getInteger("steps", 5);

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

    // Build sequential scenarios for each pageSize step
    // Each step runs 1 concurrent user for stepDurationSec seconds
    PopulationBuilder population = null;

    for (int i = 0; i < steps; i++) {
      int pageSize = pageSizeStart + (i * pageSizeIncrement);
      String url = baseUrl + "&pageSize=" + pageSize;

      ScenarioBuilder step =
          scenario("pageSize=" + pageSize).exec(http("pageSize=" + pageSize).get(url));

      PopulationBuilder stepPopulation =
          step.injectClosed(constantConcurrentUsers(1).during(Duration.ofSeconds(stepDurationSec)));

      population = (population == null) ? stepPopulation : population.andThen(stepPopulation);
    }

    setUp(population).protocols(httpProtocol);
  }
}
