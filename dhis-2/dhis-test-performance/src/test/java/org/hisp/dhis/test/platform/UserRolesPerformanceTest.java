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
import java.util.Base64;
import java.util.Properties;

/**
 * Performance test for scalar JSON Patch updates on {@code /api/userRoles/{uid}}.
 *
 * <p>Motivated by a production incident where {@code PATCH /api/userRoles/{uid}} on a role with
 * many members hydrated the entire lazy {@code UserRole.members} collection during {@code
 * JsonPatchManager.apply}, making a one-column update cost O(members) SQL and hundreds of MB of
 * allocation. A scalar PATCH must be independent of role membership size; this simulation makes
 * that regression visible by patching both an empty control role and a large-membership role.
 *
 * <p>Scenarios (sequential, single virtual user):
 *
 * <ol>
 *   <li><b>PATCH empty role</b>: scalar description patch on a role created in {@code before()}
 *       with zero members (control; establishes the membership-independent baseline)
 *   <li><b>PATCH large role</b>: the same scalar patch on a pre-existing role with large membership
 *       ({@code userRoleUid})
 *   <li><b>GET large role</b>: narrow-fields read of the large role (control; verifies reads stay
 *       cheap and the role is intact)
 * </ol>
 *
 * <p>Available properties (with platform-perf DB defaults), settable via {@code -D} flags or a
 * {@code -DconfigFile=<path>.properties}:
 *
 * <ul>
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code userRoleUid} (default: {@code MoRvPzDH7lc}, a role with ~83k users on the
 *       platform-perf DB)
 *   <li>{@code iterations} (default: {@code 10})
 * </ul>
 *
 * <p>No p95/max threshold assertions yet, only 100% success. Thresholds should be calibrated from
 * nightly baselines once this simulation has history (see {@link UsersPerformanceTest} for the
 * calibration workflow). On an unfixed server, the large-role PATCH may exceed Gatling's default
 * 60s request timeout; raise it with {@code -Dgatling.http.requestTimeout=600000}.
 *
 * @author Morten Svanæs
 */
public class UserRolesPerformanceTest extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println("[UserRolesPerformanceTest] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[UserRolesPerformanceTest] Warning: could not load configFile="
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

  private static final String BASE_URL = prop("baseUrl", "http://localhost:8080");
  private static final String USERNAME = prop("username", "admin");
  private static final String PASSWORD = prop("password", "district");
  private static final String BASIC_AUTH =
      Base64.getEncoder()
          .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
  private static final String LARGE_ROLE_UID = prop("userRoleUid", "MoRvPzDH7lc");
  private static final int ITERATIONS = Integer.parseInt(prop("iterations", "10"));

  private static final String PATCH_EMPTY_REQUEST = "PATCH UserRole - scalar (empty role)";
  private static final String PATCH_LARGE_REQUEST = "PATCH UserRole - scalar (large role)";
  private static final String GET_LARGE_REQUEST = "GET UserRole - narrow fields (large role)";

  private static final String PATCH_BODY_TEMPLATE =
      """
      [{"op":"replace","path":"/description","value":"perf-patched %s"}]\
      """;

  /** UID of the empty control role created in {@link #before()}. */
  private static volatile String emptyRoleUid;

  /**
   * Creates the zero-member control role and verifies the large role exists. Fails fast if either
   * precondition cannot be met, so a misconfigured {@code userRoleUid} does not produce a
   * misleadingly green run.
   */
  @Override
  public void before() {
    HttpClient client = HttpClient.newHttpClient();
    ObjectMapper mapper = new ObjectMapper();
    try {
      String name = "PerfTest empty role " + System.currentTimeMillis();
      HttpRequest create =
          HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL + "/api/userRoles"))
              .header("Content-Type", "application/json")
              .header("Authorization", "Basic " + BASIC_AUTH)
              .header("Accept", "application/json")
              .POST(
                  HttpRequest.BodyPublishers.ofString(
                      "{\"name\":\"%s\",\"description\":\"perf control role\"}".formatted(name)))
              .build();
      HttpResponse<String> createResponse =
          client.send(create, HttpResponse.BodyHandlers.ofString());
      emptyRoleUid = mapper.readTree(createResponse.body()).path("response").path("uid").asText();
      if (emptyRoleUid.isEmpty()) {
        throw new IllegalStateException(
            "Could not create control role (HTTP "
                + createResponse.statusCode()
                + "): "
                + createResponse.body());
      }

      HttpRequest verify =
          HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL + "/api/userRoles/" + LARGE_ROLE_UID + "?fields=id,name"))
              .header("Authorization", "Basic " + BASIC_AUTH)
              .header("Accept", "application/json")
              .GET()
              .build();
      HttpResponse<String> verifyResponse =
          client.send(verify, HttpResponse.BodyHandlers.ofString());
      if (verifyResponse.statusCode() != 200) {
        throw new IllegalStateException(
            "Large role %s not found (HTTP %d), set -DuserRoleUid to a role with many members"
                .formatted(LARGE_ROLE_UID, verifyResponse.statusCode()));
      }
      System.out.println(
          "[UserRolesPerformanceTest] control role %s, large role %s, %d iterations"
              .formatted(emptyRoleUid, LARGE_ROLE_UID, ITERATIONS));
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Setup failed: " + e.getMessage(), e);
    }
  }

  /** Deletes the control role created in {@link #before()}. */
  @Override
  public void after() {
    if (emptyRoleUid == null || emptyRoleUid.isEmpty()) {
      return;
    }
    try {
      HttpClient.newHttpClient()
          .send(
              HttpRequest.newBuilder()
                  .uri(URI.create(BASE_URL + "/api/userRoles/" + emptyRoleUid))
                  .header("Authorization", "Basic " + BASIC_AUTH)
                  .DELETE()
                  .build(),
              HttpResponse.BodyHandlers.discarding());
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      System.err.println("Cleanup of control role failed: " + e.getMessage());
    }
  }

  public UserRolesPerformanceTest() {
    // Same session strategy as UsersPerformanceTest: authenticate once per virtual user via a
    // separately-named request so the one-time bcrypt cost stays out of the measured requests.
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL).acceptHeader("application/json").disableCaching();

    ChainBuilder authenticate =
        exec(flushCookieJar())
            .exec(
                http("Authenticate (session login)")
                    .get("/api/me")
                    .header("Authorization", "Basic " + BASIC_AUTH)
                    .check(status().is(200)));

    ScenarioBuilder patchEmptyScenario =
        scenario(PATCH_EMPTY_REQUEST)
            .exec(authenticate)
            .repeat(ITERATIONS)
            .on(
                exec(
                    http(PATCH_EMPTY_REQUEST)
                        .patch(session -> "/api/userRoles/" + emptyRoleUid)
                        .header("Content-Type", "application/json-patch+json")
                        .body(
                            StringBody(session -> PATCH_BODY_TEMPLATE.formatted(System.nanoTime())))
                        .check(status().is(200))));

    ScenarioBuilder patchLargeScenario =
        scenario(PATCH_LARGE_REQUEST)
            .exec(authenticate)
            .repeat(ITERATIONS)
            .on(
                exec(
                    http(PATCH_LARGE_REQUEST)
                        .patch("/api/userRoles/" + LARGE_ROLE_UID)
                        .header("Content-Type", "application/json-patch+json")
                        .body(
                            StringBody(session -> PATCH_BODY_TEMPLATE.formatted(System.nanoTime())))
                        .check(status().is(200))));

    ScenarioBuilder getLargeScenario =
        scenario(GET_LARGE_REQUEST)
            .exec(authenticate)
            .repeat(ITERATIONS)
            .on(
                exec(
                    http(GET_LARGE_REQUEST)
                        .get("/api/userRoles/" + LARGE_ROLE_UID)
                        .queryParam("fields", "id,name,description,authorities")
                        .check(status().is(200))));

    ClosedInjectionStep singleUser = rampConcurrentUsers(0).to(1).during(1);

    setUp(
            patchEmptyScenario
                .injectClosed(singleUser)
                .andThen(patchLargeScenario.injectClosed(singleUser))
                .andThen(getLargeScenario.injectClosed(singleUser)))
        .protocols(httpProtocol)
        .assertions(
            details(PATCH_EMPTY_REQUEST).successfulRequests().percent().is(100D),
            details(PATCH_LARGE_REQUEST).successfulRequests().percent().is(100D),
            details(GET_LARGE_REQUEST).successfulRequests().percent().is(100D));
  }
}
