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
import java.util.concurrent.atomic.AtomicInteger;
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
 *   <li>-DuseJdbc=true (default: false, use direct JDBC batch import for users - much faster)
 *   <li>-DdbUrl=jdbc:postgresql://localhost:5432/dhis2 (default, JDBC URL for direct import)
 *   <li>-DdbUser=dhis (default, database username for direct import)
 *   <li>-DdbPassword=dhis (default, database password for direct import)
 * </ul>
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class UsersPerformanceTest extends Simulation {

  // Configurable via system properties
  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String USERNAME = System.getProperty("username", "admin");
  private static final String PASSWORD = System.getProperty("password", "district");
  private static final int USER_FILES = Integer.parseInt(System.getProperty("userFiles", "2"));
  private static final int ORGUNIT_FILES =
      Integer.parseInt(System.getProperty("orgunitFiles", "3"));
  private static final boolean SKIP_IMPORT =
      Boolean.parseBoolean(System.getProperty("skipImport", "false"));
  private static final boolean SKIP_ORGUNIT_IMPORT =
      Boolean.parseBoolean(System.getProperty("skipOrgUnitImport", "false"));
  private static final boolean USE_JDBC =
      Boolean.parseBoolean(System.getProperty("useJdbc", "false"));
  private static final String DB_URL =
      System.getProperty("dbUrl", "jdbc:postgresql://localhost:5432/dhis2");
  private static final String DB_USER = System.getProperty("dbUser", "dhis");
  private static final String DB_PASSWORD = System.getProperty("dbPassword", "dhis");

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

  // Scenario 9: POST - create a new user
  private static final String POST_USER = "POST User - create";
  private static final String POST_USER_REQUEST = "POST User - create REQUEST";

  // Scenario 10: PUT - full update of an existing user
  private static final String PUT_USER = "PUT User - full update";
  private static final String PUT_USER_REQUEST = "PUT User - full update REQUEST";

  // Scenario 11: PATCH - partial update (JSON Patch)
  private static final String PATCH_USER = "PATCH User - partial update";
  private static final String PATCH_USER_REQUEST = "PATCH User - partial update REQUEST";

  // Scenario 12: DELETE - delete a user
  private static final String DELETE_USER = "DELETE User - delete";
  private static final String DELETE_USER_REQUEST = "DELETE User - delete REQUEST";

  // Scenario 13: Metadata import - single user via /api/metadata
  private static final String METADATA_IMPORT_USER = "POST Metadata Import - single user";
  private static final String METADATA_IMPORT_USER_REQUEST =
      "POST Metadata Import - single user REQUEST";

  // Counters for write scenarios (unique per request)
  private static final AtomicInteger POST_COUNTER = new AtomicInteger(300001);
  private static final AtomicInteger PUT_PAGE = new AtomicInteger(1);
  private static final AtomicInteger PATCH_PAGE = new AtomicInteger(1000);
  private static final AtomicInteger DELETE_COUNTER = new AtomicInteger(500001);
  private static final AtomicInteger METADATA_IMPORT_COUNTER = new AtomicInteger(600001);

  // Well-known UIDs from the test data
  private static final String USER_ROLE_UID = "yrB6vc5Ip3r";
  private static final String ORG_UNIT_UID = "Me1z6JJcfOJ";

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
    if (USE_JDBC) {
      System.out.println("Using JDBC batch import for users (fast mode)...");
      int total = JdbcBatchUserImporter.importAllUserFiles(USER_FILES, DB_URL, DB_USER, DB_PASSWORD);
      System.out.println("JDBC user import completed. Total users inserted: " + total);
    } else {
      for (int i = 0; i < USER_FILES; i++) {
        String fileName = String.format("platform/users/users_%04d.json", i);
        System.out.println("Importing: " + fileName);
        MetadataImporter.importJsonFileIdempotent(fileName, BASE_URL, USERNAME, PASSWORD);
      }
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

    // Scenario 9: POST - create a new user
    ScenarioBuilder postUserScenario =
        scenario(POST_USER)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(POST_USER_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(
                                session -> {
                                  int num = POST_COUNTER.getAndIncrement();
                                  String body =
                                      "{\"username\":\"perftest_post_"
                                          + String.format("%07d", num)
                                          + "\","
                                          + "\"firstName\":\"Post\","
                                          + "\"surname\":\"User"
                                          + num
                                          + "\","
                                          + "\"password\":\"Test123!\","
                                          + "\"userRoles\":[{\"id\":\""
                                          + USER_ROLE_UID
                                          + "\"}],"
                                          + "\"organisationUnits\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}],"
                                          + "\"dataViewOrganisationUnits\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}]}";
                                  return session.set("postBody", body);
                                })
                            .exec(
                                http(POST_USER_REQUEST)
                                    .post("/api/users")
                                    .header("Content-Type", "application/json")
                                    .body(StringBody("#{postBody}"))
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().in(200, 201)))
                            .pause(1)));

    // Scenario 10: PUT - full update of an existing user
    // Fetches an existing user, modifies firstName, PUTs the full payload back
    ScenarioBuilder putUserScenario =
        scenario(PUT_USER)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(PUT_USER_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(
                                session ->
                                    session.set("putPage", PUT_PAGE.getAndIncrement()))
                            .exec(
                                http("Fetch user UID for PUT")
                                    .get("/api/users")
                                    .queryParam("pageSize", "1")
                                    .queryParam("page", "#{putPage}")
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().is(200))
                                    .check(
                                        jsonPath("$.users[0].id").saveAs("putUserId")))
                            .exitHereIfFailed()
                            .exec(
                                http("Fetch full user for PUT")
                                    .get("/api/users/#{putUserId}")
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().is(200))
                                    .check(bodyString().saveAs("putUserBody")))
                            .exitHereIfFailed()
                            .exec(
                                session -> {
                                  String body = session.getString("putUserBody");
                                  String modified =
                                      body.replaceFirst(
                                          "\"firstName\"\\s*:\\s*\"[^\"]*\"",
                                          "\"firstName\":\"PutUpdated"
                                              + System.currentTimeMillis()
                                              + "\"");
                                  return session.set("putModifiedBody", modified);
                                })
                            .exec(
                                http(PUT_USER_REQUEST)
                                    .put("/api/users/#{putUserId}")
                                    .header("Content-Type", "application/json")
                                    .body(StringBody("#{putModifiedBody}"))
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().is(200)))
                            .pause(1)));

    // Scenario 11: PATCH - partial update using JSON Patch (RFC 6902)
    // Content-Type: application/json-patch+json
    ScenarioBuilder patchUserScenario =
        scenario(PATCH_USER)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(PATCH_USER_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(
                                session ->
                                    session.set(
                                        "patchPage", PATCH_PAGE.getAndIncrement()))
                            .exec(
                                http("Fetch user UID for PATCH")
                                    .get("/api/users")
                                    .queryParam("pageSize", "1")
                                    .queryParam("page", "#{patchPage}")
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().is(200))
                                    .check(
                                        jsonPath("$.users[0].id")
                                            .saveAs("patchUserId")))
                            .exitHereIfFailed()
                            .exec(
                                session -> {
                                  String patchBody =
                                      "[{\"op\":\"add\",\"path\":\"/firstName\","
                                          + "\"value\":\"Patched"
                                          + System.currentTimeMillis()
                                          + "\"},"
                                          + "{\"op\":\"add\",\"path\":\"/organisationUnits\","
                                          + "\"value\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}]},"
                                          + "{\"op\":\"add\",\"path\":\"/attributeValues\","
                                          + "\"value\":[]}]";
                                  return session.set("patchBody", patchBody);
                                })
                            .exec(
                                http(PATCH_USER_REQUEST)
                                    .patch("/api/users/#{patchUserId}")
                                    .header(
                                        "Content-Type",
                                        "application/json-patch+json")
                                    .body(StringBody("#{patchBody}"))
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().is(200)))
                            .pause(1)));

    // Scenario 12: DELETE - create a disposable user then delete it
    ScenarioBuilder deleteUserScenario =
        scenario(DELETE_USER)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(DELETE_USER_REQUEST)
            .on(
                repeat(6)
                    .on(
                        // Step 1: Create a disposable user via POST
                        exec(
                                session -> {
                                  int num = DELETE_COUNTER.getAndIncrement();
                                  String body =
                                      "{\"username\":\"perftest_del_"
                                          + String.format("%07d", num)
                                          + "\","
                                          + "\"firstName\":\"Delete\","
                                          + "\"surname\":\"User"
                                          + num
                                          + "\","
                                          + "\"password\":\"Test123!\","
                                          + "\"userRoles\":[{\"id\":\""
                                          + USER_ROLE_UID
                                          + "\"}],"
                                          + "\"organisationUnits\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}],"
                                          + "\"dataViewOrganisationUnits\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}]}";
                                  return session.set("deletePostBody", body);
                                })
                            .exec(
                                http("Create user for DELETE")
                                    .post("/api/users")
                                    .header("Content-Type", "application/json")
                                    .body(StringBody("#{deletePostBody}"))
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().in(200, 201))
                                    .check(
                                        jsonPath("$.response.uid")
                                            .saveAs("deleteUserId")))
                            .exitHereIfFailed()
                            // Step 2: DELETE the user we just created
                            .exec(
                                http(DELETE_USER_REQUEST)
                                    .delete("/api/users/#{deleteUserId}")
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().is(200)))
                            .pause(1)));

    // Scenario 13: Metadata import - single user via /api/metadata
    // This is the same endpoint used in the setup import, now measured as a perf scenario
    ScenarioBuilder metadataImportUserScenario =
        scenario(METADATA_IMPORT_USER)
            .feed(feeder)
            .group("Authentication")
            .on(exec(loginChain()))
            .group(METADATA_IMPORT_USER_REQUEST)
            .on(
                repeat(6)
                    .on(
                        exec(
                                session -> {
                                  int num = METADATA_IMPORT_COUNTER.getAndIncrement();
                                  String body =
                                      "{\"users\":[{"
                                          + "\"username\":\"perftest_meta_"
                                          + String.format("%07d", num)
                                          + "\","
                                          + "\"firstName\":\"Meta\","
                                          + "\"surname\":\"Import"
                                          + num
                                          + "\","
                                          + "\"password\":\"Test123!\","
                                          + "\"userRoles\":[{\"id\":\""
                                          + USER_ROLE_UID
                                          + "\"}],"
                                          + "\"organisationUnits\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}],"
                                          + "\"dataViewOrganisationUnits\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}]"
                                          + "}]}";
                                  return session.set("metadataImportBody", body);
                                })
                            .exec(
                                http(METADATA_IMPORT_USER_REQUEST)
                                    .post("/api/metadata")
                                    .queryParam("atomicMode", "NONE")
                                    .queryParam("importStrategy", "CREATE_AND_UPDATE")
                                    .header("Content-Type", "application/json")
                                    .body(StringBody("#{metadataImportBody}"))
                                    .basicAuth("#{username}", "#{password}")
                                    .check(status().in(200, 409)))
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
            getUsersLargePageScenario.injectClosed(singleUserInjection),
            // Write scenarios (single user to avoid conflicts)
            postUserScenario.injectClosed(singleUserInjection),
            putUserScenario.injectClosed(singleUserInjection),
            patchUserScenario.injectClosed(singleUserInjection),
            deleteUserScenario.injectClosed(singleUserInjection),
            metadataImportUserScenario.injectClosed(singleUserInjection))
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
            details(GET_USERS_LARGE_PAGE_REQUEST).successfulRequests().percent().is(100D),

            // === POST - create user ===
            details(POST_USER_REQUEST).responseTime().percentile(95).lt(30000),
            details(POST_USER_REQUEST).successfulRequests().percent().is(100D),

            // === PUT - full update ===
            details(PUT_USER_REQUEST).responseTime().percentile(95).lt(30000),
            details(PUT_USER_REQUEST).successfulRequests().percent().is(100D),

            // === PATCH - partial update ===
            details(PATCH_USER_REQUEST).responseTime().percentile(95).lt(30000),
            details(PATCH_USER_REQUEST).successfulRequests().percent().is(100D),

            // === DELETE - delete user ===
            details(DELETE_USER_REQUEST).responseTime().percentile(95).lt(30000),
            details(DELETE_USER_REQUEST).successfulRequests().percent().is(100D),

            // === METADATA IMPORT - single user via /api/metadata ===
            details(METADATA_IMPORT_USER_REQUEST).responseTime().percentile(95).lt(30000),
            details(METADATA_IMPORT_USER_REQUEST).successfulRequests().percent().is(100D));
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
