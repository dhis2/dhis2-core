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
package org.hisp.dhis.test.osiv;

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

/**
 * Performance test demonstrating that OSIV-excluded endpoints don't consume database connections.
 *
 * <p><b>Purpose:</b> This test demonstrates the ConditionalOpenEntityManagerInViewFilter by showing
 * that excluded endpoints (/api/ping, /api/metrics, /api/tracker/**, /api/system/ping) do not hold
 * database connections during request processing, preventing connection pool exhaustion under high
 * concurrent load.
 *
 * <p><b>Test Strategy:</b>
 *
 * <ul>
 *   <li>Use default connection pool size (80 connections)
 *   <li>Enable HikariCP metrics via ./home/dhis.conf to monitor active/idle connections
 *   <li>Run 100+ concurrent requests to /api/ping (OSIV-excluded endpoint)
 *   <li>Verify all requests succeed (200 OK) with minimal connection usage
 *   <li>Monitor via Prometheus to confirm connections remain available (not held during request)
 * </ul>
 *
 * <p><b>Expected Results:</b>
 *
 * <ul>
 *   <li><b>With ConditionalOpenEntityManagerInViewFilter:</b> /api/ping returns 200 OK for all
 *       concurrent requests with hikaricp_connections_active staying at 0 (no DB access needed)
 *   <li><b>For OSIV-enabled endpoints like /api/organisationUnits:</b> Each concurrent request
 *       holds a connection for the entire request duration, visible in hikaricp_connections_active
 * </ul>
 *
 * <p><b>Configuration (./home/dhis.conf):</b>
 *
 * <pre>
 * # Enable HikariCP metrics for Prometheus
 * monitoring.dbpool.enabled = on
 * monitoring.api.enabled = on
 * </pre>
 *
 * <p><b>Prometheus Queries for Visualization:</b>
 *
 * <pre>
 * # Active connections over time
 * hikaricp_connections_active{pool="HikariPool-1"}
 *
 * # Max connections (should be flat at 10)
 * hikaricp_connections_max{pool="HikariPool-1"}
 *
 * # Connection usage percentage
 * (hikaricp_connections_active / hikaricp_connections_max) * 100
 * </pre>
 *
 * <p><b>Running the test:</b>
 *
 * <pre>
 * # Test OSIV-excluded endpoint (should succeed)
 * mvn test -Dtest=ConnectionPoolTest -Dendpoint=/api/ping -DconcurrentUsers=100
 *
 * # Test OSIV-enabled endpoint (should fail/timeout before the fix)
 * mvn test -Dtest=ConnectionPoolTest -Dendpoint=/api/organisationUnits -DconcurrentUsers=100
 * </pre>
 *
 * @see org.hisp.dhis.webapi.filter.ConditionalOpenEntityManagerInViewFilter
 */
public class ConnectionPoolTest extends Simulation {

  private final String instance;
  private final String endpoint;
  private final int concurrentUsers;
  private final int durationSec;
  private final String adminUser;
  private final String adminPassword;

  public ConnectionPoolTest() {
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.endpoint = System.getProperty("endpoint", "/api/sleep?ms=35000");
    this.concurrentUsers = Integer.getInteger("concurrentUsers", 100);
    this.durationSec = Integer.getInteger("durationSec", 100);
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");

    ScenarioBuilder scenario =
        scenario("Connection Pool Test - " + endpoint)
            .exec(
                http("Request " + endpoint)
                    .get(endpoint)
                    .basicAuth(adminUser, adminPassword)
                    .check(status().is(200)));

    HttpProtocolBuilder httpProtocol =
        http.baseUrl(instance)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/ConnectionPoolTest")
            .disableFollowRedirect()
            .disableCaching();

    setUp(scenario.injectClosed(constantConcurrentUsers(concurrentUsers).during(durationSec)))
        .protocols(httpProtocol)
        .assertions(
            // All requests must succeed - no connection pool exhaustion
            io.gatling.javaapi.core.CoreDsl.global().successfulRequests().percent().gte(100d));
  }
}
