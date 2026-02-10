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
 *   <li>-Dmode=parallel (default: parallel, "parallel" = all scenarios at once, "sequential" = one
 *       after another)
 *   <li>-Dscenarios=all (default: all, "read" = GET only (1-8), "write" = POST/PUT/PATCH/DELETE/
 *       metadata (9-13), "all" = everything)
 *   <li>-Diterations=6 (default: 6, number of times each virtual user repeats the request)
 *   <li>-Dconcurrency=3 (default: 3, max concurrent virtual users for multi-user scenarios)
 *   <li>-DrampDuration=10 (default: 10, ramp-up duration in seconds)
 *   <li>-Dpause=1 (default: 1, pause in seconds between iterations)
 *   <li>-DpageSize=50 (default: 50, page size for GET scenarios)
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

  // Test execution configuration
  private static final String MODE = System.getProperty("mode", "parallel");
  private static final String SCENARIOS = System.getProperty("scenarios", "all");
  private static final int ITERATIONS = Integer.parseInt(System.getProperty("iterations", "6"));
  private static final int CONCURRENCY = Integer.parseInt(System.getProperty("concurrency", "3"));
  private static final int RAMP_DURATION =
      Integer.parseInt(System.getProperty("rampDuration", "10"));
  private static final int PAUSE_SECONDS = Integer.parseInt(System.getProperty("pause", "1"));
  private static final int PAGE_SIZE = Integer.parseInt(System.getProperty("pageSize", "50"));

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

  // Timestamp-based offset so each test run generates unique usernames (avoids 409 on reruns)
  private static final int RUN_OFFSET = (int) (System.currentTimeMillis() % 10_000_000);

  // Counters for write scenarios (unique per request)
  private static final AtomicInteger POST_COUNTER = new AtomicInteger(RUN_OFFSET);
  private static final AtomicInteger PUT_PAGE = new AtomicInteger(100);
  private static final AtomicInteger PATCH_PAGE = new AtomicInteger(2000);
  private static final AtomicInteger DELETE_COUNTER = new AtomicInteger(RUN_OFFSET + 5_000_000);
  private static final AtomicInteger METADATA_IMPORT_COUNTER =
      new AtomicInteger(RUN_OFFSET + 8_000_000);

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
            .disableCaching()
            .basicAuth(USERNAME, PASSWORD);


    // Scenario 1: Basic user list (paged, default fields)
    ScenarioBuilder getUsersScenario =
        scenario(GET_USERS)
            .exec(flushCookieJar())
            .group(GET_USERS_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(http(GET_USERS_REQUEST)
                                .get("/api/users")
                                .queryParam("pageSize", String.valueOf(PAGE_SIZE))
)
                            .pause(PAUSE_SECONDS)));

    // Scenario 2: All fields expansion (triggers lazy loading of ALL relationships)
    ScenarioBuilder getUsersAllFieldsScenario =
        scenario(GET_USERS_ALL_FIELDS)
            .exec(flushCookieJar())
            .group(GET_USERS_ALL_FIELDS_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(http(GET_USERS_ALL_FIELDS_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "*")
                                .queryParam("pageSize", String.valueOf(PAGE_SIZE))
)
                            .pause(PAUSE_SECONDS)));

    // Scenario 3: User roles expansion (N+1 on userRoles)
    ScenarioBuilder getUsersRolesScenario =
        scenario(GET_USERS_ROLES)
            .exec(flushCookieJar())
            .group(GET_USERS_ROLES_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(http(GET_USERS_ROLES_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "id,name,userRoles[id,name]")
                                .queryParam("pageSize", String.valueOf(PAGE_SIZE))
)
                            .pause(PAUSE_SECONDS)));

    // Scenario 4: User groups expansion (N+1 on userGroups)
    ScenarioBuilder getUsersGroupsScenario =
        scenario(GET_USERS_GROUPS)
            .exec(flushCookieJar())
            .group(GET_USERS_GROUPS_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(http(GET_USERS_GROUPS_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "id,name,userGroups[id,name]")
                                .queryParam("pageSize", String.valueOf(PAGE_SIZE))
)
                            .pause(PAUSE_SECONDS)));

    // Scenario 5: Org units expansion (N+1 on organisationUnits)
    ScenarioBuilder getUsersOrgunitsScenario =
        scenario(GET_USERS_ORGUNITS)
            .exec(flushCookieJar())
            .group(GET_USERS_ORGUNITS_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(http(GET_USERS_ORGUNITS_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "id,name,organisationUnits[id,name]")
                                .queryParam("pageSize", String.valueOf(PAGE_SIZE))
)
                            .pause(PAUSE_SECONDS)));

    // Scenario 6: Combined common fields (realistic admin UI query)
    ScenarioBuilder getUsersCombinedScenario =
        scenario(GET_USERS_COMBINED)
            .exec(flushCookieJar())
            .group(GET_USERS_COMBINED_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(http(GET_USERS_COMBINED_REQUEST)
                                .get("/api/users")
                                .queryParam(
                                    "fields",
                                    "id,name,username,userRoles[id,name],userGroups[id,name],organisationUnits[id,name]")
                                .queryParam("pageSize", String.valueOf(PAGE_SIZE))
)
                            .pause(PAUSE_SECONDS)));

    // Scenario 7: Query filter (search) - tests getPreQueryMatches + HQL path
    ScenarioBuilder getUsersQueryScenario =
        scenario(GET_USERS_QUERY)
            .exec(flushCookieJar())
            .group(GET_USERS_QUERY_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(http(GET_USERS_QUERY_REQUEST)
                                .get("/api/users")
                                .queryParam("query", "perftest")
                                .queryParam("pageSize", String.valueOf(PAGE_SIZE))
)
                            .pause(PAUSE_SECONDS)));

    // Scenario 8: Large page size (amplifies N+1 issues)
    ScenarioBuilder getUsersLargePageScenario =
        scenario(GET_USERS_LARGE_PAGE)
            .exec(flushCookieJar())
            .group(GET_USERS_LARGE_PAGE_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(http(GET_USERS_LARGE_PAGE_REQUEST)
                                .get("/api/users")
                                .queryParam("fields", "id,name,userRoles[id,name]")
                                .queryParam("pageSize", String.valueOf(PAGE_SIZE * 10))
)
                            .pause(PAUSE_SECONDS)));

    // Scenario 9: POST - create a new user
    ScenarioBuilder postUserScenario =
        scenario(POST_USER)
            .exec(flushCookieJar())
            .group(POST_USER_REQUEST)
            .on(
                repeat(ITERATIONS)
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
                                    .check(status().in(200, 201)))
                            .pause(PAUSE_SECONDS)));

    // Scenario 10: PUT - full update of an existing user
    // Fetches an existing user, modifies firstName, PUTs the full payload back
    ScenarioBuilder putUserScenario =
        scenario(PUT_USER)
            .exec(flushCookieJar())
            .group(PUT_USER_REQUEST)
            .on(
                repeat(ITERATIONS)
                    .on(
                        exec(
                                session ->
                                    session.set("putPage", PUT_PAGE.getAndIncrement()))
                            .exec(
                                http("Fetch user for PUT")
                                    .get("/api/users")
                                    .queryParam("pageSize", "1")
                                    .queryParam("page", "#{putPage}")
                                    .queryParam(
                                        "fields",
                                        "id,username,firstName,surname,userRoles[id],organisationUnits[id],dataViewOrganisationUnits[id]")
                                    .check(status().is(200))
                                    .check(jsonPath("$.users[0].id").saveAs("putUserId"))
                                    .check(
                                        jsonPath("$.users[0].username")
                                            .saveAs("putUsername"))
                                    .check(
                                        jsonPath("$.users[0].surname")
                                            .saveAs("putSurname")))
                            .exitHereIfFailed()
                            .exec(
                                session -> {
                                  String body =
                                      "{\"username\":\""
                                          + session.getString("putUsername")
                                          + "\","
                                          + "\"firstName\":\"PutUpdated"
                                          + System.currentTimeMillis()
                                          + "\","
                                          + "\"surname\":\""
                                          + session.getString("putSurname")
                                          + "\","
                                          + "\"userRoles\":[{\"id\":\""
                                          + USER_ROLE_UID
                                          + "\"}],"
                                          + "\"organisationUnits\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}],"
                                          + "\"dataViewOrganisationUnits\":[{\"id\":\""
                                          + ORG_UNIT_UID
                                          + "\"}]}";
                                  return session.set("putModifiedBody", body);
                                })
                            .exec(
                                http(PUT_USER_REQUEST)
                                    .put("/api/users/#{putUserId}")
                                    .header("Content-Type", "application/json")
                                    .body(StringBody("#{putModifiedBody}"))
                                    .check(status().is(200)))
                            .pause(PAUSE_SECONDS)));

    // Scenario 11: PATCH - partial update using JSON Patch (RFC 6902)
    // Content-Type: application/json-patch+json
    ScenarioBuilder patchUserScenario =
        scenario(PATCH_USER)
            .exec(flushCookieJar())
            .group(PATCH_USER_REQUEST)
            .on(
                repeat(ITERATIONS)
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
                                    .check(status().is(200))
                                    .check(
                                        jsonPath("$.users[0].id")
                                            .saveAs("patchUserId")))
                            .exitHereIfFailed()
                            .exec(
                                session -> {
                                  String patchBody =
                                      "[{\"op\":\"replace\",\"path\":\"/firstName\","
                                          + "\"value\":\"Patched"
                                          + System.currentTimeMillis()
                                          + "\"}]";
                                  return session.set("patchBody", patchBody);
                                })
                            .exec(
                                http(PATCH_USER_REQUEST)
                                    .patch("/api/users/#{patchUserId}")
                                    .header(
                                        "Content-Type",
                                        "application/json-patch+json")
                                    .body(StringBody("#{patchBody}"))
                                    .check(status().is(200)))
                            .pause(PAUSE_SECONDS)));

    // Scenario 12: DELETE - create a disposable user then delete it
    ScenarioBuilder deleteUserScenario =
        scenario(DELETE_USER)
            .exec(flushCookieJar())
            .group(DELETE_USER_REQUEST)
            .on(
                repeat(ITERATIONS)
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
                                    .check(status().in(200, 201))
                                    .check(
                                        jsonPath("$.response.uid")
                                            .saveAs("deleteUserId")))
                            .exitHereIfFailed()
                            // Step 2: DELETE the user we just created
                            .exec(
                                http(DELETE_USER_REQUEST)
                                    .delete("/api/users/#{deleteUserId}")
                                    .check(status().is(200)))
                            .pause(PAUSE_SECONDS)));

    // Scenario 13: Metadata import - single user via /api/metadata
    // This is the same endpoint used in the setup import, now measured as a perf scenario
    ScenarioBuilder metadataImportUserScenario =
        scenario(METADATA_IMPORT_USER)
            .exec(flushCookieJar())
            .group(METADATA_IMPORT_USER_REQUEST)
            .on(
                repeat(ITERATIONS)
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
                                    .check(status().in(200, 409)))
                            .pause(PAUSE_SECONDS)));

    // Injection profiles (configurable)
    ClosedInjectionStep multiUserInjection =
        rampConcurrentUsers(0).to(CONCURRENCY).during(RAMP_DURATION);
    ClosedInjectionStep singleUserInjection =
        rampConcurrentUsers(0).to(1).during(Math.max(1, RAMP_DURATION / 2));

    // Build scenario populations based on the "scenarios" property
    List<PopulationBuilder> populations = new ArrayList<>();
    List<Assertion> assertions = new ArrayList<>();

    boolean includeRead = "all".equals(SCENARIOS) || "read".equals(SCENARIOS);
    boolean includeWrite = "all".equals(SCENARIOS) || "write".equals(SCENARIOS);

    if (includeRead) {
      populations.add(getUsersScenario.injectClosed(multiUserInjection));
      populations.add(getUsersAllFieldsScenario.injectClosed(singleUserInjection));
      populations.add(getUsersRolesScenario.injectClosed(multiUserInjection));
      populations.add(getUsersGroupsScenario.injectClosed(multiUserInjection));
      populations.add(getUsersOrgunitsScenario.injectClosed(multiUserInjection));
      populations.add(getUsersCombinedScenario.injectClosed(singleUserInjection));
      populations.add(getUsersQueryScenario.injectClosed(multiUserInjection));
      populations.add(getUsersLargePageScenario.injectClosed(singleUserInjection));

      assertions.add(details(GET_USERS_REQUEST).responseTime().percentile(95).lt(5000));
      assertions.add(details(GET_USERS_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(
          details(GET_USERS_ALL_FIELDS_REQUEST).responseTime().percentile(95).lt(15000));
      assertions.add(
          details(GET_USERS_ALL_FIELDS_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(details(GET_USERS_ROLES_REQUEST).responseTime().percentile(95).lt(10000));
      assertions.add(details(GET_USERS_ROLES_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(details(GET_USERS_GROUPS_REQUEST).responseTime().percentile(95).lt(10000));
      assertions.add(
          details(GET_USERS_GROUPS_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(
          details(GET_USERS_ORGUNITS_REQUEST).responseTime().percentile(95).lt(10000));
      assertions.add(
          details(GET_USERS_ORGUNITS_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(
          details(GET_USERS_COMBINED_REQUEST).responseTime().percentile(95).lt(15000));
      assertions.add(
          details(GET_USERS_COMBINED_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(details(GET_USERS_QUERY_REQUEST).responseTime().percentile(95).lt(10000));
      assertions.add(
          details(GET_USERS_QUERY_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(
          details(GET_USERS_LARGE_PAGE_REQUEST).responseTime().percentile(95).lt(15000));
      assertions.add(
          details(GET_USERS_LARGE_PAGE_REQUEST).successfulRequests().percent().is(100D));
    }

    if (includeWrite) {
      populations.add(postUserScenario.injectClosed(singleUserInjection));
      populations.add(putUserScenario.injectClosed(singleUserInjection));
      populations.add(patchUserScenario.injectClosed(singleUserInjection));
      populations.add(deleteUserScenario.injectClosed(singleUserInjection));
      populations.add(metadataImportUserScenario.injectClosed(singleUserInjection));

      assertions.add(details(POST_USER_REQUEST).responseTime().percentile(95).lt(30000));
      assertions.add(details(POST_USER_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(details(PUT_USER_REQUEST).responseTime().percentile(95).lt(30000));
      assertions.add(details(PUT_USER_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(details(PATCH_USER_REQUEST).responseTime().percentile(95).lt(30000));
      assertions.add(details(PATCH_USER_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(details(DELETE_USER_REQUEST).responseTime().percentile(95).lt(30000));
      assertions.add(details(DELETE_USER_REQUEST).successfulRequests().percent().is(100D));
      assertions.add(
          details(METADATA_IMPORT_USER_REQUEST).responseTime().percentile(95).lt(30000));
      assertions.add(
          details(METADATA_IMPORT_USER_REQUEST).successfulRequests().percent().is(100D));
    }

    if (populations.isEmpty()) {
      throw new IllegalArgumentException(
          "No scenarios selected. Use -Dscenarios=all|read|write (got: " + SCENARIOS + ")");
    }

    System.out.println(
        "Running "
            + populations.size()
            + " scenario(s) in "
            + MODE
            + " mode [iterations="
            + ITERATIONS
            + ", concurrency="
            + CONCURRENCY
            + ", rampDuration="
            + RAMP_DURATION
            + "s, pause="
            + PAUSE_SECONDS
            + "s, pageSize="
            + PAGE_SIZE
            + "]");

    // Sequential mode: chain scenarios with andThen() so they run one after another
    // Parallel mode: all scenarios run concurrently (default/original behavior)
    if ("sequential".equals(MODE)) {
      PopulationBuilder chain = populations.get(0);
      for (int i = 1; i < populations.size(); i++) {
        chain = chain.andThen(populations.get(i));
      }
      setUp(chain)
          .protocols(httpProtocol)
          .assertions(assertions.toArray(new Assertion[0]));
    } else {
      setUp(populations.toArray(new PopulationBuilder[0]))
          .protocols(httpProtocol)
          .assertions(assertions.toArray(new Assertion[0]));
    }
  }

}
