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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance test for single-user CRUD operations on {@code /api/users}.
 *
 * <p>Five scenarios, all running against the Sierra Leone demo DB by default:
 *
 * <ol>
 *   <li><b>POST</b> — creates a new user (with optional group assignment)
 *   <li><b>GET</b> — fetches a single pre-created user by UID
 *   <li><b>PUT</b> — full-replace of a pre-created user
 *   <li><b>PATCH</b> — partial update via RFC 6902 JSON Patch on a pre-created user
 *   <li><b>DELETE</b> — deletes pre-created users (separate pool, timing is clean)
 * </ol>
 *
 * <p>All scenarios operate exclusively on users created in {@code before()} or by the POST scenario
 * itself — no existing database users are touched.
 *
 * <p>Run with {@code -DuserGroupUid=<uid>} pointing at a group with large membership to expose N+1
 * problems in POST and DELETE. The default points at "Administrators" on the SL demo DB.
 *
 * <p>Configuration can be provided via a {@code .properties} file instead of individual {@code -D}
 * flags:
 *
 * <pre>{@code
 * mvn gatling:test -DconfigFile=dhis-test-performance/scripts/remote-perf.properties
 * }</pre>
 *
 * Individual {@code -D} flags always override values from the config file.
 *
 * <p>Available properties (with SL demo DB defaults):
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file (optional)
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code userRoleUid} (default: {@code Euq3XfEIEbx} — "Data entry clerk")
 *   <li>{@code orgUnitUid} (default: {@code ImspTQPwCqd} — "Sierra Leone" root)
 *   <li>{@code userGroupUid} (default: {@code wl5cDMuUhmF} — "Administrators")
 *   <li>{@code iterations} (default: {@code 3})
 *   <li>{@code mode} (default: {@code parallel}; use {@code sequential} to chain scenarios)
 * </ul>
 */
public class UsersPerformanceTest extends Simulation {

  /**
   * Loads a {@code .properties} file from the path given by {@code -DconfigFile=...}, or returns an
   * empty {@code Properties} if no config file was specified.
   */
  private static final Properties CONFIG = loadConfig();

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println("[UsersPerformanceTest] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[UsersPerformanceTest] Warning: could not load configFile="
                + path
                + ": "
                + e.getMessage());
      }
    }
    return props;
  }

  /**
   * Returns the value for {@code key}, preferring a {@code -D} system property over the config
   * file, falling back to {@code defaultValue}.
   */
  private static String prop(String key, String defaultValue) {
    String sys = System.getProperty(key);
    if (sys != null) return sys;
    String file = CONFIG.getProperty(key);
    return file != null ? file : defaultValue;
  }

  private static final String BASE_URL = prop("baseUrl", "http://localhost:8080");
  private static final String USERNAME = prop("username", "admin");
  private static final String PASSWORD = prop("password", "district");
  private static final String USER_ROLE_UID = prop("userRoleUid", "Euq3XfEIEbx");
  private static final String ORG_UNIT_UID = prop("orgUnitUid", "ImspTQPwCqd");
  private static final String USER_GROUP_UID = prop("userGroupUid", "wl5cDMuUhmF");
  private static final int ITERATIONS = Integer.parseInt(prop("iterations", "3"));
  private static final String MODE = prop("mode", "parallel");

  // Timestamp-based offset so each run generates unique usernames
  private static final int RUN_OFFSET = (int) (System.currentTimeMillis() % 10_000_000);
  private static final AtomicInteger POST_COUNTER = new AtomicInteger(RUN_OFFSET);
  private static final AtomicInteger PRE_CREATE_COUNTER = new AtomicInteger(RUN_OFFSET + 500_000);

  /**
   * Pre-created users for GET/PUT/PATCH scenarios. Stored as [uid, username] pairs. Scenarios cycle
   * through this list, so it is never exhausted.
   */
  private static final List<String[]> READ_WRITE_USERS = new ArrayList<>();

  private static final AtomicInteger GET_INDEX = new AtomicInteger(0);
  private static final AtomicInteger PUT_INDEX = new AtomicInteger(0);
  private static final AtomicInteger PATCH_INDEX = new AtomicInteger(0);

  /**
   * Pre-created users for the DELETE scenario. Consumed one-at-a-time; sized generously so the
   * queue is never exhausted within a normal test run.
   */
  private static final BlockingQueue<String> DELETE_UID_QUEUE = new LinkedBlockingQueue<>();

  /** Builds a minimal valid user JSON body for POST/PUT. Pass {@code null} for id on creation. */
  private static String userBody(String id, String username, String firstName) {
    String idField = id != null ? "\"id\":\"" + id + "\"," : "";
    String groupsField =
        USER_GROUP_UID.isBlank() ? "" : ",\"userGroups\":[{\"id\":\"" + USER_GROUP_UID + "\"}]";
    return "{"
        + idField
        + "\"username\":\""
        + username
        + "\","
        + "\"firstName\":\""
        + firstName
        + "\","
        + "\"surname\":\"Test\","
        + "\"password\":\"Test1234@\","
        + "\"userRoles\":[{\"id\":\""
        + USER_ROLE_UID
        + "\"}],"
        + "\"organisationUnits\":[{\"id\":\""
        + ORG_UNIT_UID
        + "\"}],"
        + "\"dataViewOrganisationUnits\":[{\"id\":\""
        + ORG_UNIT_UID
        + "\"}]"
        + groupsField
        + "}";
  }

  /**
   * Creates a user via the DHIS2 API and returns its UID, or {@code null} on failure. Used by
   * {@code before()} to populate the pre-created user pools without Gatling DSL.
   */
  private static String createUser(
      HttpClient client, ObjectMapper mapper, String auth, String username, String firstName) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL + "/api/users"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Basic " + auth)
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(userBody(null, username, firstName)))
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      String uid = mapper.readTree(response.body()).path("response").path("uid").asText();
      if (!uid.isEmpty()) return uid;
      System.err.println(
          "Pre-create failed for " + username + " (HTTP " + response.statusCode() + ")");
    } catch (Exception e) {
      System.err.println("Pre-create error for " + username + ": " + e.getMessage());
    }
    return null;
  }

  /**
   * Pre-creates all users needed by the test:
   *
   * <ul>
   *   <li>A pool of read/write users for the GET, PUT, and PATCH scenarios (cycled, never deleted)
   *   <li>A pool of disposable users for the DELETE scenario (one per DELETE request)
   * </ul>
   *
   * No existing database users are touched by the test.
   */
  @Override
  public void before() {
    // Conservative pool sizes: enough headroom for ~3× concurrent virtual users per scenario.
    int rwNeeded = ITERATIONS * 3 + 5;
    int delNeeded = ITERATIONS * 3 + 5;
    String groupLabel = USER_GROUP_UID.isBlank() ? "" : " (group: " + USER_GROUP_UID + ")";
    System.out.println(
        "Pre-creating "
            + rwNeeded
            + " read/write users and "
            + delNeeded
            + " delete users"
            + groupLabel
            + "...");

    String auth =
        Base64.getEncoder()
            .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    HttpClient client = HttpClient.newHttpClient();
    ObjectMapper mapper = new ObjectMapper();

    for (int i = 0; i < rwNeeded; i++) {
      int num = PRE_CREATE_COUNTER.getAndIncrement();
      String username = "perftest_rw_" + String.format("%07d", num);
      String uid = createUser(client, mapper, auth, username, "ReadWrite");
      if (uid != null) READ_WRITE_USERS.add(new String[] {uid, username});
    }

    for (int i = 0; i < delNeeded; i++) {
      int num = PRE_CREATE_COUNTER.getAndIncrement();
      String username = "perftest_del_" + String.format("%07d", num);
      String uid = createUser(client, mapper, auth, username, "Delete");
      if (uid != null) DELETE_UID_QUEUE.offer(uid);
    }

    System.out.println(
        "Pre-created "
            + READ_WRITE_USERS.size()
            + "/"
            + rwNeeded
            + " read/write users, "
            + DELETE_UID_QUEUE.size()
            + "/"
            + delNeeded
            + " delete users.");
  }

  public UsersPerformanceTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .disableCaching()
            .basicAuth(USERNAME, PASSWORD);

    // ── Scenario: POST /api/users ────────────────────────────────────────────
    ScenarioBuilder postScenario =
        scenario("POST User - create")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(
                exec(session -> {
                      int num = POST_COUNTER.getAndIncrement();
                      String username = "perftest_post_" + String.format("%07d", num);
                      return session.set("postBody", userBody(null, username, "Post"));
                    })
                    .exec(
                        http("POST User - create")
                            .post("/api/users")
                            .header("Content-Type", "application/json")
                            .body(StringBody("#{postBody}"))
                            .check(status().in(200, 201))));

    // ── Scenario: GET /api/users/{uid} ──────────────────────────────────────
    ScenarioBuilder getScenario =
        scenario("GET User - by uid")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(
                exec(session -> {
                      String[] user =
                          READ_WRITE_USERS.get(
                              GET_INDEX.getAndIncrement() % READ_WRITE_USERS.size());
                      return session.set("getUid", user[0]);
                    })
                    .exec(
                        http("GET User - by uid")
                            .get("/api/users/#{getUid}")
                            .check(status().is(200))));

    // ── Scenario: PUT /api/users/{uid} ──────────────────────────────────────
    ScenarioBuilder putScenario =
        scenario("PUT User - full update")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(
                exec(session -> {
                      String[] user =
                          READ_WRITE_USERS.get(
                              PUT_INDEX.getAndIncrement() % READ_WRITE_USERS.size());
                      return session
                          .set("putUid", user[0])
                          .set("putBody", userBody(user[0], user[1], "PutUpdated"));
                    })
                    .exec(
                        http("PUT User - full update")
                            .put("/api/users/#{putUid}")
                            .header("Content-Type", "application/json")
                            .body(StringBody("#{putBody}"))
                            .check(status().is(200))));

    // ── Scenario: PATCH /api/users/{uid} ────────────────────────────────────
    ScenarioBuilder patchScenario =
        scenario("PATCH User - partial update")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(
                exec(session -> {
                      String[] user =
                          READ_WRITE_USERS.get(
                              PATCH_INDEX.getAndIncrement() % READ_WRITE_USERS.size());
                      return session.set("patchUid", user[0]);
                    })
                    .exec(
                        http("PATCH User - partial update")
                            .patch("/api/users/#{patchUid}")
                            .header("Content-Type", "application/json-patch+json")
                            .body(
                                StringBody(
                                    "[{\"op\":\"replace\",\"path\":\"/firstName\",\"value\":\"PerfPatched\"}]"))
                            .check(status().is(200))));

    // ── Scenario: DELETE /api/users/{uid} ───────────────────────────────────
    // Users are pre-created in before(), so this scenario measures only DELETE time.
    ScenarioBuilder deleteScenario =
        scenario("DELETE User - delete")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(
                exec(session -> {
                      String uid = DELETE_UID_QUEUE.poll();
                      if (uid == null) {
                        System.err.println(
                            "DELETE_UID_QUEUE exhausted — increase iterations buffer");
                        return session.markAsFailed();
                      }
                      return session.set("deleteUid", uid);
                    })
                    .exitHereIfFailed()
                    .exec(
                        http("DELETE User - delete")
                            .delete("/api/users/#{deleteUid}")
                            .check(status().is(200))));

    ClosedInjectionStep singleUser = rampConcurrentUsers(0).to(1).during(1);

    PopulationBuilder postPopulation = postScenario.injectClosed(singleUser);
    PopulationBuilder getPopulation = getScenario.injectClosed(singleUser);
    PopulationBuilder putPopulation = putScenario.injectClosed(singleUser);
    PopulationBuilder patchPopulation = patchScenario.injectClosed(singleUser);
    PopulationBuilder deletePopulation = deleteScenario.injectClosed(singleUser);

    if ("sequential".equals(MODE)) {
      setUp(
              postPopulation
                  .andThen(getPopulation)
                  .andThen(putPopulation)
                  .andThen(patchPopulation)
                  .andThen(deletePopulation))
          .protocols(httpProtocol);
    } else {
      setUp(postPopulation, getPopulation, putPopulation, patchPopulation, deletePopulation)
          .protocols(httpProtocol);
    }
  }
}
