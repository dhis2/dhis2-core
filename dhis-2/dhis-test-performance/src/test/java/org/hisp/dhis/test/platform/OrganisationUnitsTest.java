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
import java.util.*;
import java.util.stream.Stream;

/**
 * Performance test simulation for the /api/organisationUnits endpoint.
 *
 * <p>This test verifies the performance improvement from skipping query cache for OrganisationUnit
 * queries, which avoids N+1 queries during cache hydration when loading parent relationships.
 *
 * <p>Scenarios tested:
 *
 * <ul>
 *   <li>GET /api/organisationUnits - default fields
 *   <li>GET /api/organisationUnits?fields=* - all fields
 *   <li>GET /api/organisationUnits?fields=id,name,parent[id,name] - with parent expansion (triggers
 *       N+1 if cached incorrectly)
 * </ul>
 *
 * <p>Configuration via system properties:
 *
 * <ul>
 *   <li>-Dusername=admin (default: admin)
 *   <li>-Dpassword=district (default: district)
 *   <li>-DbaseUrl=http://localhost:8080 (default: http://localhost:8080)
 *   <li>-DorgunitFiles=6 (default: 6, number of orgunit files to import, 0000-0005)
 *   <li>-DskipImport=true (default: false, skip the metadata import phase)
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class OrganisationUnitsTest extends Simulation {

  // Configurable via system properties
  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String USERNAME = System.getProperty("username", "admin");
  private static final String PASSWORD = System.getProperty("password", "district");
  private static final int ORGUNIT_FILES =
      Integer.parseInt(System.getProperty("orgunitFiles", "6"));
  private static final boolean SKIP_IMPORT =
      Boolean.parseBoolean(System.getProperty("skipImport", "false"));

  private static final String GET_ORG_UNITS = "GET Organisation Units";
  private static final String GET_ORG_UNITS_REQUEST = "GET Organisation Units REQUEST";

  private static final String GET_ORG_UNITS_ALL_FIELDS = "GET Organisation Units - all fields";
  private static final String GET_ORG_UNITS_ALL_FIELDS_REQUEST =
      "GET Organisation Units - all fields REQUEST";

  private static final String GET_ORG_UNITS_WITH_PARENT =
      "GET Organisation Units - with parent expansion";
  private static final String GET_ORG_UNITS_WITH_PARENT_REQUEST =
      "GET Organisation Units - with parent expansion REQUEST";

  // Unpaged scenarios - for testing N+1 cache hydration with full dataset
  private static final String GET_ORG_UNITS_UNPAGED = "GET Organisation Units - unpaged";
  private static final String GET_ORG_UNITS_UNPAGED_REQUEST =
      "GET Organisation Units - unpaged REQUEST";

  private static final String GET_ORG_UNITS_UNPAGED_PARENT =
      "GET Organisation Units - unpaged with parent";
  private static final String GET_ORG_UNITS_UNPAGED_PARENT_REQUEST =
      "GET Organisation Units - unpaged with parent REQUEST";

  // Deep hierarchy scenarios
  private static final String GET_ORG_UNITS_DEEP_2 = "GET Organisation Units - 2 level hierarchy";
  private static final String GET_ORG_UNITS_DEEP_2_REQUEST =
      "GET Organisation Units - 2 level hierarchy REQUEST";

  private static final String GET_ORG_UNITS_DEEP_3 = "GET Organisation Units - 3 level hierarchy";
  private static final String GET_ORG_UNITS_DEEP_3_REQUEST =
      "GET Organisation Units - 3 level hierarchy REQUEST";

  private static final String GET_ORG_UNITS_ANCESTORS = "GET Organisation Units - ancestors";
  private static final String GET_ORG_UNITS_ANCESTORS_REQUEST =
      "GET Organisation Units - ancestors REQUEST";

  /**
   * Import organisation unit metadata before running the simulation. Uses idempotent import to
   * tolerate already-existing data.
   */
  @Override
  public void before() {
    if (SKIP_IMPORT) {
      System.out.println("Skipping metadata import (skipImport=true)");
      return;
    }

    System.out.println("Importing " + ORGUNIT_FILES + " organisation unit file(s)...");
    for (int i = 0; i < ORGUNIT_FILES; i++) {
      String fileName = String.format("platform/orgunits/orgunits_%04d.json", i);
      System.out.println("Importing: " + fileName);
      MetadataImporter.importJsonFileIdempotent(fileName, BASE_URL, USERNAME, PASSWORD);
    }
    System.out.println("Metadata import completed.");
  }

  public OrganisationUnitsTest() {
    // http protocol
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .warmUp(BASE_URL + "/api/ping")
            .disableCaching();

    // Simple feeder with configured credentials
    Iterator<Map<String, Object>> feeder =
        Stream.generate(
                (java.util.function.Supplier<Map<String, Object>>)
                    () -> Map.of("username", USERNAME, "password", PASSWORD))
            .iterator();

    // Scenario 1: Basic list query with default fields (paged for large datasets)
    ScenarioBuilder getOrgUnitsScenario =
        scenario(GET_ORG_UNITS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNITS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNITS_REQUEST)
                                .get("/api/organisationUnits")
                                .queryParam("pageSize", "1000")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 2: Query with all fields expansion (paged for large datasets)
    ScenarioBuilder getOrgUnitsAllFieldsScenario =
        scenario(GET_ORG_UNITS_ALL_FIELDS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNITS_ALL_FIELDS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNITS_ALL_FIELDS_REQUEST)
                                .get("/api/organisationUnits")
                                .queryParam("fields", "*")
                                .queryParam("pageSize", "500")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 3: Query with parent field expansion (paged for large datasets)
    // This is the key scenario that would trigger N+1 queries if query cache was enabled
    ScenarioBuilder getOrgUnitsWithParentScenario =
        scenario(GET_ORG_UNITS_WITH_PARENT)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNITS_WITH_PARENT_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNITS_WITH_PARENT_REQUEST)
                                .get("/api/organisationUnits")
                                .queryParam("fields", "id,name,parent[id,name]")
                                .queryParam("pageSize", "1000")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 4: Unpaged query - fetches ALL org units (tests cache hydration with full dataset)
    ScenarioBuilder getOrgUnitsUnpagedScenario =
        scenario(GET_ORG_UNITS_UNPAGED)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNITS_UNPAGED_REQUEST)
            .on(
                exec(http(GET_ORG_UNITS_UNPAGED_REQUEST)
                        .get("/api/organisationUnits")
                        .queryParam("paging", "false")
                        .queryParam("fields", "id,name")
                        .basicAuth("#{username}", "#{password}"))
                    .pause(2));

    // Scenario 5: Unpaged with parent expansion - key N+1 test scenario
    ScenarioBuilder getOrgUnitsUnpagedParentScenario =
        scenario(GET_ORG_UNITS_UNPAGED_PARENT)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNITS_UNPAGED_PARENT_REQUEST)
            .on(
                exec(http(GET_ORG_UNITS_UNPAGED_PARENT_REQUEST)
                        .get("/api/organisationUnits")
                        .queryParam("paging", "false")
                        .queryParam("fields", "id,name,parent[id,name]")
                        .basicAuth("#{username}", "#{password}"))
                    .pause(2));

    // Scenario 6: 2-level deep hierarchy (paged)
    ScenarioBuilder getOrgUnitsDeep2Scenario =
        scenario(GET_ORG_UNITS_DEEP_2)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNITS_DEEP_2_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNITS_DEEP_2_REQUEST)
                                .get("/api/organisationUnits")
                                .queryParam("fields", "id,name,parent[id,name,parent[id,name]]")
                                .queryParam("pageSize", "500")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 7: 3-level deep hierarchy (paged)
    ScenarioBuilder getOrgUnitsDeep3Scenario =
        scenario(GET_ORG_UNITS_DEEP_3)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNITS_DEEP_3_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNITS_DEEP_3_REQUEST)
                                .get("/api/organisationUnits")
                                .queryParam(
                                    "fields",
                                    "id,name,parent[id,name,parent[id,name,parent[id,name]]]")
                                .queryParam("pageSize", "500")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 8: Ancestors expansion (paged) - fetches full ancestry chain
    ScenarioBuilder getOrgUnitsAncestorsScenario =
        scenario(GET_ORG_UNITS_ANCESTORS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNITS_ANCESTORS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNITS_ANCESTORS_REQUEST)
                                .get("/api/organisationUnits")
                                .queryParam("fields", "id,name,ancestors[id,name]")
                                .queryParam("pageSize", "500")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // how users should enter the scenarios - ramp up 3 users over 10 seconds per scenario
    ClosedInjectionStep closedInjection = rampConcurrentUsers(0).to(3).during(10);
    // Single user for unpaged scenarios (very heavy)
    ClosedInjectionStep singleUserInjection = rampConcurrentUsers(0).to(1).during(5);

    // final setup, bringing all parts together (scenarios, injection, protocol, assertions)
    setUp(
            // Paged scenarios with multiple users
            getOrgUnitsScenario.injectClosed(closedInjection),
            getOrgUnitsAllFieldsScenario.injectClosed(closedInjection),
            getOrgUnitsWithParentScenario.injectClosed(closedInjection),
            // Unpaged scenarios with single user (very heavy)
            getOrgUnitsUnpagedScenario.injectClosed(singleUserInjection),
            getOrgUnitsUnpagedParentScenario.injectClosed(singleUserInjection),
            // Deep hierarchy scenarios
            getOrgUnitsDeep2Scenario.injectClosed(closedInjection),
            getOrgUnitsDeep3Scenario.injectClosed(closedInjection),
            getOrgUnitsAncestorsScenario.injectClosed(closedInjection))
        .protocols(httpProtocol)
        .assertions(
            // === PAGED SCENARIOS - strict timing ===
            // Basic query - paged, should be reasonably fast
            details(GET_ORG_UNITS_REQUEST).responseTime().percentile(95).lt(5000),
            details(GET_ORG_UNITS_REQUEST).successfulRequests().percent().is(100D),
            // All fields query - paged but more data per record
            details(GET_ORG_UNITS_ALL_FIELDS_REQUEST).responseTime().percentile(95).lt(10000),
            details(GET_ORG_UNITS_ALL_FIELDS_REQUEST).successfulRequests().percent().is(100D),
            // Parent expansion query - this is where the N+1 fix matters most
            details(GET_ORG_UNITS_WITH_PARENT_REQUEST).responseTime().percentile(95).lt(5000),
            details(GET_ORG_UNITS_WITH_PARENT_REQUEST).successfulRequests().percent().is(100D),

            // === UNPAGED SCENARIOS - only check success (timing will vary greatly) ===
            details(GET_ORG_UNITS_UNPAGED_REQUEST).successfulRequests().percent().is(100D),
            details(GET_ORG_UNITS_UNPAGED_PARENT_REQUEST).successfulRequests().percent().is(100D),

            // === DEEP HIERARCHY SCENARIOS - relaxed timing ===
            details(GET_ORG_UNITS_DEEP_2_REQUEST).responseTime().percentile(95).lt(15000),
            details(GET_ORG_UNITS_DEEP_2_REQUEST).successfulRequests().percent().is(100D),
            details(GET_ORG_UNITS_DEEP_3_REQUEST).responseTime().percentile(95).lt(20000),
            details(GET_ORG_UNITS_DEEP_3_REQUEST).successfulRequests().percent().is(100D),
            details(GET_ORG_UNITS_ANCESTORS_REQUEST).responseTime().percentile(95).lt(15000),
            details(GET_ORG_UNITS_ANCESTORS_REQUEST).successfulRequests().percent().is(100D));
  }

  private ChainBuilder loginChain() {
    return exec(
        http("Login")
            .post("/api/auth/login")
            .header("Content-Type", "application/json")
            .body(StringBody("{\"username\":\"#{username}\",\"password\":\"#{password}\"}"))
            .check(status().is(200)));
  }
}
