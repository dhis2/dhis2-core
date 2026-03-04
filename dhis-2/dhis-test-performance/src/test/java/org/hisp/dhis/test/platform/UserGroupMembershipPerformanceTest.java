/*
 * Copyright (c) 2004-2026, University of Oslo
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance test for user-group membership workflows on {@code /api/userGroups}.
 *
 * <p>Each iteration performs a full lifecycle:
 *
 * <ol>
 *   <li><b>POST</b> creates a new user group with an initial set of users
 *   <li><b>PATCH</b> replaces the group's {@code users} collection with a larger set
 *   <li><b>PUT</b> full-replaces the group with another user set
 *   <li><b>DELETE</b> removes the group
 * </ol>
 *
 * <p>Existing users are discovered in {@code before()} and reused across iterations so timed
 * requests focus on membership mutation instead of test-data creation.
 */
public class UserGroupMembershipPerformanceTest extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static final String BASE_URL = prop("baseUrl", "http://localhost:8080");
  private static final String USERNAME = prop("username", "admin");
  private static final String PASSWORD = prop("password", "district");
  private static final int ITERATIONS = Integer.parseInt(prop("iterations", "3"));
  private static final int INITIAL_USER_COUNT = Integer.parseInt(prop("initialUserCount", "3"));
  private static final int PATCH_USER_COUNT = Integer.parseInt(prop("patchUserCount", "6"));
  private static final int PUT_USER_COUNT = Integer.parseInt(prop("putUserCount", "9"));
  private static final int REQUIRED_USER_COUNT =
      Math.max(INITIAL_USER_COUNT, Math.max(PATCH_USER_COUNT, PUT_USER_COUNT));

  private static final String POST_REQUEST = "POST UserGroup - create with users";
  private static final String PATCH_REQUEST = "PATCH UserGroup - replace users";
  private static final String PUT_REQUEST = "PUT UserGroup - full replace";
  private static final String DELETE_REQUEST = "DELETE UserGroup - delete";

  private static final List<String> EXISTING_USER_UIDS = new ArrayList<>();
  private static final Queue<String> CREATED_GROUP_UIDS = new ConcurrentLinkedQueue<>();
  private static final AtomicInteger GROUP_COUNTER =
      new AtomicInteger((int) (System.currentTimeMillis() % 10_000_000));

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println("[UserGroupMembershipPerformanceTest] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[UserGroupMembershipPerformanceTest] Warning: could not load configFile="
                + path
                + ": "
                + e.getMessage());
      }
    }
    return props;
  }

  private static String prop(String key, String defaultValue) {
    String sys = System.getProperty(key);
    if (sys != null) return sys;
    String file = CONFIG.getProperty(key);
    return file != null ? file : defaultValue;
  }

  private static String usersJson(int count) {
    StringBuilder builder = new StringBuilder("[");
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        builder.append(',');
      }
      builder.append("{\"id\":\"").append(EXISTING_USER_UIDS.get(i)).append("\"}");
    }
    return builder.append(']').toString();
  }

  private static String groupBody(String id, String name, int userCount) {
    String idField = id != null ? "\"id\":\"%s\",".formatted(id) : "";
    String code = name.replace('-', '_');
    return """
        {%s"name":"%s","code":"%s","users":%s,"managedGroups":[],"attributeValues":[]}\
        """
        .formatted(idField, name, code, usersJson(userCount));
  }

  private static String patchUsersBody(int userCount) {
    return """
        [{"op":"replace","path":"/users","value":%s}]\
        """
        .formatted(usersJson(userCount));
  }

  private static void fetchExistingUserUids(
      HttpClient client, ObjectMapper mapper, String authHeader, int userCount) throws IOException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/users?fields=id&pageSize=" + userCount))
            .header("Authorization", "Basic " + authHeader)
            .header("Accept", "application/json")
            .GET()
            .build();

    try {
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IOException("GET /api/users failed with HTTP " + response.statusCode());
      }

      JsonNode users = mapper.readTree(response.body()).path("users");
      for (JsonNode user : users) {
        String uid = user.path("id").asText();
        if (!uid.isBlank()) {
          EXISTING_USER_UIDS.add(uid);
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while fetching users", e);
    }
  }

  private static void deleteUserGroup(
      HttpClient client, String authHeader, String uid, ObjectMapper mapper) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL + "/api/userGroups/" + uid))
              .header("Authorization", "Basic " + authHeader)
              .header("Accept", "application/json")
              .DELETE()
              .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200 && response.statusCode() != 404) {
        String status = mapper.readTree(response.body()).path("status").asText();
        System.err.println(
            "Cleanup delete failed for user group "
                + uid
                + " (HTTP "
                + response.statusCode()
                + ", status="
                + status
                + ")");
      }
    } catch (Exception e) {
      System.err.println("Cleanup delete failed for user group " + uid + ": " + e.getMessage());
    }
  }

  @Override
  public void before() {
    if (INITIAL_USER_COUNT <= 0
        || PATCH_USER_COUNT < INITIAL_USER_COUNT
        || PUT_USER_COUNT < PATCH_USER_COUNT) {
      throw new IllegalArgumentException(
          "User counts must satisfy 0 < initialUserCount <= patchUserCount <= putUserCount");
    }

    String authHeader =
        Base64.getEncoder()
            .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    HttpClient client = HttpClient.newHttpClient();
    ObjectMapper mapper = new ObjectMapper();

    try {
      fetchExistingUserUids(client, mapper, authHeader, REQUIRED_USER_COUNT);
    } catch (IOException e) {
      throw new RuntimeException("Failed to fetch existing users for setup", e);
    }

    if (EXISTING_USER_UIDS.size() < REQUIRED_USER_COUNT) {
      throw new IllegalStateException(
          "Need at least "
              + REQUIRED_USER_COUNT
              + " existing users, found "
              + EXISTING_USER_UIDS.size());
    }

    System.out.println(
        "Using "
            + REQUIRED_USER_COUNT
            + " existing users for membership test: "
            + EXISTING_USER_UIDS.subList(0, REQUIRED_USER_COUNT));
  }

  @Override
  public void after() {
    if (CREATED_GROUP_UIDS.isEmpty()) {
      return;
    }

    String authHeader =
        Base64.getEncoder()
            .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    HttpClient client = HttpClient.newHttpClient();
    ObjectMapper mapper = new ObjectMapper();

    String uid;
    while ((uid = CREATED_GROUP_UIDS.poll()) != null) {
      deleteUserGroup(client, authHeader, uid, mapper);
    }
  }

  public UserGroupMembershipPerformanceTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .disableCaching()
            .basicAuth(USERNAME, PASSWORD);

    ChainBuilder workflow =
        exec(session -> {
              int num = GROUP_COUNTER.getAndIncrement();
              String name = "perftest_group_" + String.format("%07d", num);
              return session
                  .set("groupName", name)
                  .set("postBody", groupBody(null, name, INITIAL_USER_COUNT))
                  .set("patchBody", patchUsersBody(PATCH_USER_COUNT));
            })
            .exec(
                http(POST_REQUEST)
                    .post("/api/userGroups")
                    .header("Content-Type", "application/json")
                    .body(StringBody("#{postBody}"))
                    .check(status().in(200, 201))
                    .check(jsonPath("$.response.uid").saveAs("groupUid")))
            .exec(
                session -> {
                  CREATED_GROUP_UIDS.offer(session.getString("groupUid"));
                  return session;
                })
            .exec(
                http(PATCH_REQUEST)
                    .patch("/api/userGroups/#{groupUid}")
                    .header("Content-Type", "application/json-patch+json")
                    .body(StringBody("#{patchBody}"))
                    .check(status().is(200)))
            .exec(
                session ->
                    session.set(
                        "putBody",
                        groupBody(
                            session.getString("groupUid"),
                            session.getString("groupName"),
                            PUT_USER_COUNT)))
            .exec(
                http(PUT_REQUEST)
                    .put("/api/userGroups/#{groupUid}?skipSharing=true")
                    .header("Content-Type", "application/json")
                    .body(StringBody("#{putBody}"))
                    .check(status().is(200)))
            .exec(
                http(DELETE_REQUEST).delete("/api/userGroups/#{groupUid}").check(status().is(200)))
            .exec(
                session -> {
                  CREATED_GROUP_UIDS.remove(session.getString("groupUid"));
                  return session;
                });

    ScenarioBuilder scenario =
        scenario("UserGroup membership workflow")
            .exec(flushCookieJar())
            .repeat(ITERATIONS)
            .on(workflow);

    ClosedInjectionStep singleUser = rampConcurrentUsers(0).to(1).during(1);

    setUp(scenario.injectClosed(singleUser))
        .protocols(httpProtocol)
        .assertions(
            details(POST_REQUEST).responseTime().percentile(95).lt(500),
            details(POST_REQUEST).responseTime().max().lt(700),
            details(POST_REQUEST).successfulRequests().percent().is(100D),
            details(PATCH_REQUEST).responseTime().percentile(95).lt(700),
            details(PATCH_REQUEST).responseTime().max().lt(900),
            details(PATCH_REQUEST).successfulRequests().percent().is(100D),
            details(PUT_REQUEST).responseTime().percentile(95).lt(800),
            details(PUT_REQUEST).responseTime().max().lt(1000),
            details(PUT_REQUEST).successfulRequests().percent().is(100D),
            details(DELETE_REQUEST).responseTime().percentile(95).lt(600),
            details(DELETE_REQUEST).responseTime().max().lt(800),
            details(DELETE_REQUEST).successfulRequests().percent().is(100D));
  }
}
