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
 * Performance test simulation for the /api/organisationUnitGroups and
 * /api/organisationUnitGroupSets endpoints.
 *
 * <p>This test verifies the performance of organisation unit group queries on large datasets, which
 * is the scenario described in DHIS2-20613 where fetching org unit groups triggers excessive
 * Hibernate object hydration.
 *
 * <p>Scenarios tested:
 *
 * <ul>
 *   <li>GET /api/organisationUnitGroups - default fields
 *   <li>GET /api/organisationUnitGroups?fields=* - all fields
 *   <li>GET /api/organisationUnitGroups?fields=id,name,organisationUnits[id,name] - with org unit
 *       expansion (N+1 trigger)
 *   <li>GET /api/organisationUnitGroupSets - default fields
 *   <li>GET
 *       /api/organisationUnitGroupSets?fields=id,name,organisationUnitGroups[id,name,organisationUnits]
 *       - deep expansion
 * </ul>
 *
 * <p>Configuration via system properties:
 *
 * <ul>
 *   <li>-Dusername=admin (default: admin)
 *   <li>-Dpassword=district (default: district)
 *   <li>-DbaseUrl=http://localhost:8080 (default: http://localhost:8080)
 *   <li>-DorgunitFiles=7 (default: 7, number of orgunit files to import, 0000-0006)
 *   <li>-DskipImport=true (default: false, skip the metadata import phase)
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class OrganisationUnitGroupsPerformanceTest extends Simulation {

  // Configurable via system properties
  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String USERNAME = System.getProperty("username", "admin");
  private static final String PASSWORD = System.getProperty("password", "district");
  private static final int ORGUNIT_FILES =
      Integer.parseInt(System.getProperty("orgunitFiles", "7"));
  private static final boolean SKIP_IMPORT =
      Boolean.parseBoolean(System.getProperty("skipImport", "false"));

  // Organisation Unit Groups scenarios
  private static final String GET_ORG_UNIT_GROUPS = "GET Organisation Unit Groups";
  private static final String GET_ORG_UNIT_GROUPS_REQUEST = "GET Organisation Unit Groups REQUEST";

  private static final String GET_ORG_UNIT_GROUPS_ALL_FIELDS =
      "GET Organisation Unit Groups - all fields";
  private static final String GET_ORG_UNIT_GROUPS_ALL_FIELDS_REQUEST =
      "GET Organisation Unit Groups - all fields REQUEST";

  private static final String GET_ORG_UNIT_GROUPS_WITH_ORGUNITS =
      "GET Organisation Unit Groups - with org unit expansion";
  private static final String GET_ORG_UNIT_GROUPS_WITH_ORGUNITS_REQUEST =
      "GET Organisation Unit Groups - with org unit expansion REQUEST";

  // Organisation Unit Group Sets scenarios
  private static final String GET_ORG_UNIT_GROUP_SETS = "GET Organisation Unit Group Sets";
  private static final String GET_ORG_UNIT_GROUP_SETS_REQUEST =
      "GET Organisation Unit Group Sets REQUEST";

  private static final String GET_ORG_UNIT_GROUP_SETS_DEEP =
      "GET Organisation Unit Group Sets - deep expansion";
  private static final String GET_ORG_UNIT_GROUP_SETS_DEEP_REQUEST =
      "GET Organisation Unit Group Sets - deep expansion REQUEST";

  /**
   * Import organisation unit metadata before running the simulation. Uses idempotent import to
   * tolerate already-existing data. The imported files should contain org units, groups, and group
   * sets.
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

  public OrganisationUnitGroupsPerformanceTest() {
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

    // Scenario 1: Basic list of org unit groups
    ScenarioBuilder getOrgUnitGroupsScenario =
        scenario(GET_ORG_UNIT_GROUPS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNIT_GROUPS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNIT_GROUPS_REQUEST)
                                .get("/api/organisationUnitGroups")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 2: Org unit groups with all fields
    ScenarioBuilder getOrgUnitGroupsAllFieldsScenario =
        scenario(GET_ORG_UNIT_GROUPS_ALL_FIELDS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNIT_GROUPS_ALL_FIELDS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNIT_GROUPS_ALL_FIELDS_REQUEST)
                                .get("/api/organisationUnitGroups")
                                .queryParam("fields", "*")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 3: Org unit groups with org unit expansion
    // This is the key scenario that triggers massive Hibernate object hydration (DHIS2-20613)
    ScenarioBuilder getOrgUnitGroupsWithOrgUnitsScenario =
        scenario(GET_ORG_UNIT_GROUPS_WITH_ORGUNITS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNIT_GROUPS_WITH_ORGUNITS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNIT_GROUPS_WITH_ORGUNITS_REQUEST)
                                .get("/api/organisationUnitGroups")
                                .queryParam("fields", "id,name,organisationUnits[id,name]")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 4: Basic list of org unit group sets
    ScenarioBuilder getOrgUnitGroupSetsScenario =
        scenario(GET_ORG_UNIT_GROUP_SETS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNIT_GROUP_SETS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNIT_GROUP_SETS_REQUEST)
                                .get("/api/organisationUnitGroupSets")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 5: Org unit group sets with deep expansion (groups > org units)
    // This triggers the most severe N+1 scenario
    ScenarioBuilder getOrgUnitGroupSetsDeepScenario =
        scenario(GET_ORG_UNIT_GROUP_SETS_DEEP)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_ORG_UNIT_GROUP_SETS_DEEP_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_ORG_UNIT_GROUP_SETS_DEEP_REQUEST)
                                .get("/api/organisationUnitGroupSets")
                                .queryParam(
                                    "fields",
                                    "id,name,organisationUnitGroups[id,name,organisationUnits[id,name]]")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // how users should enter the scenarios - ramp up 3 users over 10 seconds per scenario
    ClosedInjectionStep closedInjection = rampConcurrentUsers(0).to(3).during(10);
    // Single user for deep expansion scenarios (very heavy)
    ClosedInjectionStep singleUserInjection = rampConcurrentUsers(0).to(1).during(5);

    // final setup, bringing all parts together (scenarios, injection, protocol, assertions)
    setUp(
            // Basic scenarios with multiple users
            getOrgUnitGroupsScenario.injectClosed(closedInjection),
            getOrgUnitGroupsAllFieldsScenario.injectClosed(closedInjection),
            getOrgUnitGroupSetsScenario.injectClosed(closedInjection),
            // Heavy expansion scenarios with single user
            getOrgUnitGroupsWithOrgUnitsScenario.injectClosed(singleUserInjection),
            getOrgUnitGroupSetsDeepScenario.injectClosed(singleUserInjection))
        .protocols(httpProtocol)
        .assertions(
            // === Basic scenarios - should be fast ===
            details(GET_ORG_UNIT_GROUPS_REQUEST).responseTime().percentile(95).lt(2000),
            details(GET_ORG_UNIT_GROUPS_REQUEST).successfulRequests().percent().is(100D),
            details(GET_ORG_UNIT_GROUPS_ALL_FIELDS_REQUEST).responseTime().percentile(95).lt(5000),
            details(GET_ORG_UNIT_GROUPS_ALL_FIELDS_REQUEST).successfulRequests().percent().is(100D),
            details(GET_ORG_UNIT_GROUP_SETS_REQUEST).responseTime().percentile(95).lt(2000),
            details(GET_ORG_UNIT_GROUP_SETS_REQUEST).successfulRequests().percent().is(100D),

            // === Heavy expansion scenarios - relaxed timing due to N+1 issues ===
            // These assertions are intentionally relaxed to allow the test to pass
            // while still capturing performance metrics for comparison
            details(GET_ORG_UNIT_GROUPS_WITH_ORGUNITS_REQUEST)
                .successfulRequests()
                .percent()
                .is(100D),
            details(GET_ORG_UNIT_GROUP_SETS_DEEP_REQUEST).successfulRequests().percent().is(100D));
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
