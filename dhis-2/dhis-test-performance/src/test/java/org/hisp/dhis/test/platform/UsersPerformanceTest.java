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
 * Performance test simulation for the /api/users endpoint.
 *
 * <p>This test investigates user API performance issues (DHIS2-20614), particularly N+1 query
 * problems when expanding relationships like userRoles, userGroups, and organisationUnits.
 *
 * <p>Scenarios tested:
 *
 * <ul>
 *   <li>GET /api/users - basic paged list (baseline)
 *   <li>GET /api/users?fields=* - all fields expansion (triggers lazy loading)
 *   <li>GET /api/users?fields=id,name,userRoles[id,name] - user roles expansion
 *   <li>GET /api/users?fields=id,name,userGroups[id,name] - user groups expansion
 *   <li>GET /api/users?fields=id,name,organisationUnits[id,name] - org units expansion
 *   <li>GET
 *       /api/users?fields=id,name,username,userRoles[...],userGroups[...],organisationUnits[...] -
 *       combined common fields (realistic admin UI query)
 *   <li>GET /api/users?query=user - query filter search
 *   <li>GET /api/users?fields=id,name,userRoles[id,name]&pageSize=500 - large page size
 * </ul>
 *
 * <p>Configuration via system properties:
 *
 * <ul>
 *   <li>-Dusername=admin (default: admin)
 *   <li>-Dpassword=district (default: district)
 *   <li>-DbaseUrl=http://localhost:8080 (default: http://localhost:8080)
 *   <li>-DuserFiles=10 (default: 10, number of user batch files to import)
 *   <li>-DorgunitFiles=3 (default: 3, number of orgunit files to import, users reference these)
 *   <li>-DskipImport=true (default: false, skip all metadata import)
 *   <li>-DskipOrgUnitImport=true (default: false, skip org unit import only)
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class UsersPerformanceTest extends Simulation {

  // Configurable via system properties
  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String USERNAME = System.getProperty("username", "admin");
  private static final String PASSWORD = System.getProperty("password", "district");
  private static final int USER_FILES = Integer.parseInt(System.getProperty("userFiles", "10"));
  private static final int ORGUNIT_FILES =
      Integer.parseInt(System.getProperty("orgunitFiles", "3"));
  private static final boolean SKIP_IMPORT =
      Boolean.parseBoolean(System.getProperty("skipImport", "false"));
  private static final boolean SKIP_ORGUNIT_IMPORT =
      Boolean.parseBoolean(System.getProperty("skipOrgUnitImport", "false"));

  // Scenario 1: Basic user list (paged)
  private static final String GET_USERS = "GET Users";
  private static final String GET_USERS_REQUEST = "GET Users REQUEST";

  // Scenario 2: All fields expansion
  private static final String GET_USERS_ALL_FIELDS = "GET Users - all fields";
  private static final String GET_USERS_ALL_FIELDS_REQUEST = "GET Users - all fields REQUEST";

  // Scenario 3: User roles expansion
  private static final String GET_USERS_ROLES = "GET Users - userRoles expansion";
  private static final String GET_USERS_ROLES_REQUEST = "GET Users - userRoles expansion REQUEST";

  // Scenario 4: User groups expansion
  private static final String GET_USERS_GROUPS = "GET Users - userGroups expansion";
  private static final String GET_USERS_GROUPS_REQUEST = "GET Users - userGroups expansion REQUEST";

  // Scenario 5: Org units expansion
  private static final String GET_USERS_ORGUNITS = "GET Users - organisationUnits expansion";
  private static final String GET_USERS_ORGUNITS_REQUEST =
      "GET Users - organisationUnits expansion REQUEST";

  // Scenario 6: Combined common fields (realistic admin UI query)
  private static final String GET_USERS_COMBINED = "GET Users - combined common fields";
  private static final String GET_USERS_COMBINED_REQUEST =
      "GET Users - combined common fields REQUEST";

  // Scenario 7: Query filter (search)
  private static final String GET_USERS_QUERY = "GET Users - query filter";
  private static final String GET_USERS_QUERY_REQUEST = "GET Users - query filter REQUEST";

  // Scenario 8: Large page size
  private static final String GET_USERS_LARGE_PAGE = "GET Users - large page size";
  private static final String GET_USERS_LARGE_PAGE_REQUEST = "GET Users - large page size REQUEST";

  /**
   * Import metadata before running the simulation. Org units must be imported first since users
   * reference them.
   */
  @Override
  public void before() {
    if (SKIP_IMPORT) {
      System.out.println("Skipping all metadata import (skipImport=true)");
      return;
    }

    // Import org units first (users reference org units)
    if (!SKIP_ORGUNIT_IMPORT) {
      System.out.println("Importing " + ORGUNIT_FILES + " organisation unit file(s)...");
      for (int i = 0; i < ORGUNIT_FILES; i++) {
        String fileName = String.format("platform/orgunits/orgunits_%04d.json", i);
        System.out.println("Importing: " + fileName);
        MetadataImporter.importJsonFileIdempotent(fileName, BASE_URL, USERNAME, PASSWORD);
      }
      System.out.println("Organisation unit import completed.");
    } else {
      System.out.println("Skipping org unit import (skipOrgUnitImport=true)");
    }

    // Import users
    System.out.println("Importing " + USER_FILES + " user file(s)...");
    for (int i = 0; i < USER_FILES; i++) {
      String fileName = String.format("platform/users/users_%04d.json", i);
      System.out.println("Importing: " + fileName);
      MetadataImporter.importJsonFileIdempotent(fileName, BASE_URL, USERNAME, PASSWORD);
    }
    System.out.println("User import completed.");
  }

  public UsersPerformanceTest() {
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

    // Scenario 1: Basic user list (paged, default fields)
    ScenarioBuilder getUsersScenario =
        scenario(GET_USERS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_USERS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_USERS_REQUEST)
                                .get("/api/users")
                                .queryParam("pageSize", "50")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 2: All fields expansion (triggers lazy loading of ALL relationships)
    ScenarioBuilder getUsersAllFieldsScenario =
        scenario(GET_USERS_ALL_FIELDS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_USERS_ALL_FIELDS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_USERS_ALL_FIELDS_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "*")
                                .queryParam("pageSize", "50")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 3: User roles expansion (N+1 on userRoles)
    ScenarioBuilder getUsersRolesScenario =
        scenario(GET_USERS_ROLES)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_USERS_ROLES_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_USERS_ROLES_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "id,name,userRoles[id,name]")
                                .queryParam("pageSize", "50")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 4: User groups expansion (N+1 on userGroups)
    ScenarioBuilder getUsersGroupsScenario =
        scenario(GET_USERS_GROUPS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_USERS_GROUPS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_USERS_GROUPS_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "id,name,userGroups[id,name]")
                                .queryParam("pageSize", "50")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 5: Org units expansion (N+1 on organisationUnits)
    ScenarioBuilder getUsersOrgunitsScenario =
        scenario(GET_USERS_ORGUNITS)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_USERS_ORGUNITS_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_USERS_ORGUNITS_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "id,name,organisationUnits[id,name]")
                                .queryParam("pageSize", "50")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 6: Combined common fields (realistic admin UI query)
    ScenarioBuilder getUsersCombinedScenario =
        scenario(GET_USERS_COMBINED)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_USERS_COMBINED_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_USERS_COMBINED_REQUEST)
                                .get("/api/users")
                                .queryParam(
                                    "fields",
                                    "id,name,username,userRoles[id,name],userGroups[id,name],organisationUnits[id,name]")
                                .queryParam("pageSize", "50")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 7: Query filter (search) - tests getPreQueryMatches + HQL path
    ScenarioBuilder getUsersQueryScenario =
        scenario(GET_USERS_QUERY)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_USERS_QUERY_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_USERS_QUERY_REQUEST)
                                .get("/api/users")
                                .queryParam("query", "perftest")
                                .queryParam("pageSize", "50")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Scenario 8: Large page size (amplifies N+1 issues)
    ScenarioBuilder getUsersLargePageScenario =
        scenario(GET_USERS_LARGE_PAGE)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(GET_USERS_LARGE_PAGE_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(http(GET_USERS_LARGE_PAGE_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "id,name,userRoles[id,name]")
                                .queryParam("pageSize", "500")
                                .basicAuth("#{username}", "#{password}"))
                            .pause(1)));

    // Injection profiles
    ClosedInjectionStep closedInjection = rampConcurrentUsers(0).to(3).during(10);
    // Single user for heavy scenarios
    ClosedInjectionStep singleUserInjection = rampConcurrentUsers(0).to(1).during(5);

    // Setup: all scenarios with injection profiles, protocol, and assertions
    setUp(
            // Basic and expansion scenarios with multiple users
            getUsersScenario.injectClosed(closedInjection),
            getUsersAllFieldsScenario.injectClosed(singleUserInjection),
            getUsersRolesScenario.injectClosed(closedInjection),
            getUsersGroupsScenario.injectClosed(closedInjection),
            getUsersOrgunitsScenario.injectClosed(closedInjection),
            getUsersCombinedScenario.injectClosed(singleUserInjection),
            // Search and large page scenarios
            getUsersQueryScenario.injectClosed(closedInjection),
            getUsersLargePageScenario.injectClosed(singleUserInjection))
        .protocols(httpProtocol)
        .assertions(
            // === BASIC LIST - should be fast ===
            details(GET_USERS_REQUEST).responseTime().percentile(95).lt(5000),
            details(GET_USERS_REQUEST).successfulRequests().percent().is(100D),

            // === ALL FIELDS - heavy, relaxed threshold ===
            details(GET_USERS_ALL_FIELDS_REQUEST).responseTime().percentile(95).lt(15000),
            details(GET_USERS_ALL_FIELDS_REQUEST).successfulRequests().percent().is(100D),

            // === INDIVIDUAL EXPANSIONS ===
            details(GET_USERS_ROLES_REQUEST).responseTime().percentile(95).lt(10000),
            details(GET_USERS_ROLES_REQUEST).successfulRequests().percent().is(100D),
            details(GET_USERS_GROUPS_REQUEST).responseTime().percentile(95).lt(10000),
            details(GET_USERS_GROUPS_REQUEST).successfulRequests().percent().is(100D),
            details(GET_USERS_ORGUNITS_REQUEST).responseTime().percentile(95).lt(10000),
            details(GET_USERS_ORGUNITS_REQUEST).successfulRequests().percent().is(100D),

            // === COMBINED FIELDS - realistic admin UI query, relaxed ===
            details(GET_USERS_COMBINED_REQUEST).responseTime().percentile(95).lt(15000),
            details(GET_USERS_COMBINED_REQUEST).successfulRequests().percent().is(100D),

            // === QUERY FILTER ===
            details(GET_USERS_QUERY_REQUEST).responseTime().percentile(95).lt(10000),
            details(GET_USERS_QUERY_REQUEST).successfulRequests().percent().is(100D),

            // === LARGE PAGE SIZE - amplifies issues, relaxed ===
            details(GET_USERS_LARGE_PAGE_REQUEST).responseTime().percentile(95).lt(15000),
            details(GET_USERS_LARGE_PAGE_REQUEST).successfulRequests().percent().is(100D));
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
