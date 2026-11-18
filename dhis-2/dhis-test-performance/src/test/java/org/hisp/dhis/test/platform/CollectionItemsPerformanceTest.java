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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
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
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

/**
 * Performance test for the collection items endpoints on a large collection: {@code POST
 * /api/organisationUnitGroups/{uid}/organisationUnits} with {@code additions} and {@code deletions}
 * on an org unit group with 100,000 members (DHIS2-20613).
 *
 * <p>Each iteration sends two requests:
 *
 * <ol>
 *   <li>{@value #CHANGE_REQUEST}: adds {@code additions} org units that are not members and removes
 *       {@code deletions} members
 *   <li>{@value #REVERT_REQUEST}: undoes the first request, so every iteration starts from the same
 *       group
 * </ol>
 *
 * <p>The group and its members are created by {@code scripts/populate-collection-items.sh}, which
 * {@code run-simulation.sh} runs before the simulation:
 *
 * <pre>{@code
 * POPULATE_SCRIPT=scripts/populate-collection-items.sh \
 * DHIS2_IMAGE=dhis2/core-dev:latest \
 * SIMULATION_CLASS=org.hisp.dhis.test.platform.CollectionItemsPerformanceTest \
 * ./run-simulation.sh
 * }</pre>
 *
 * Available properties:
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file (optional)
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code groupUid} (default: {@code PfCollItems}, {@code POPULATE_GROUP_UID})
 *   <li>{@code members} — members of the group (default: {@code 100000}, {@code POPULATE_MEMBERS})
 *   <li>{@code additions} — org units added per iteration (default: {@code 6}, at most {@code
 *       POPULATE_NON_MEMBERS})
 *   <li>{@code deletions} — members removed per iteration (default: {@code 3})
 *   <li>{@code iterations} (default: {@code 30})
 * </ul>
 *
 * @author Morten Svanæs
 */
public class CollectionItemsPerformanceTest extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static final String BASE_URL = prop("baseUrl", "http://localhost:8080");
  private static final String USERNAME = prop("username", "admin");
  private static final String PASSWORD = prop("password", "district");
  private static final String BASIC_AUTH =
      Base64.getEncoder()
          .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
  private static final String GROUP_UID = prop("groupUid", "PfCollItems");
  private static final int MEMBERS = Integer.parseInt(prop("members", "100000"));
  private static final int ADDITIONS = Integer.parseInt(prop("additions", "6"));
  private static final int DELETIONS = Integer.parseInt(prop("deletions", "3"));
  private static final int ITERATIONS = Integer.parseInt(prop("iterations", "30"));

  private static final String CHANGE_REQUEST = "POST OrganisationUnitGroup items - add and remove";
  private static final String REVERT_REQUEST = "POST OrganisationUnitGroup items - revert";

  private static final String ITEMS_PATH =
      "/api/organisationUnitGroups/" + GROUP_UID + "/organisationUnits";

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println("[CollectionItemsPerformanceTest] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[CollectionItemsPerformanceTest] Warning: could not load configFile="
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

  /** UIDs of the org units made by scripts/populate/collection-items.sql. */
  private static List<String> orgUnitUids(String prefix, int count) {
    return IntStream.rangeClosed(1, count).mapToObj(n -> "%s%07d".formatted(prefix, n)).toList();
  }

  private static String itemsBody(List<String> additions, List<String> deletions) {
    return """
        {"additions":%s,"deletions":%s}\
        """
        .formatted(references(additions), references(deletions));
  }

  private static String references(List<String> uids) {
    return uids.stream()
        .map("{\"id\":\"%s\"}"::formatted)
        .reduce((a, b) -> a + "," + b)
        .map(list -> "[" + list + "]")
        .orElse("[]");
  }

  /**
   * Fails fast unless the group has exactly {@code members} members, so a missing or stale
   * population (or a revert that did not apply) never yields timings of a smaller collection.
   */
  private static void checkGroupSize(String when) {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + ITEMS_PATH + "/gist?fields=id&pageSize=1&total=true"))
            .header("Authorization", "Basic " + BASIC_AUTH)
            .header("Accept", "application/json")
            .GET()
            .build();
    try {
      HttpResponse<String> response =
          HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "Org unit group %s not found (HTTP %d), run scripts/populate-collection-items.sh"
                .formatted(GROUP_UID, response.statusCode()));
      }
      int total = new ObjectMapper().readTree(response.body()).path("pager").path("total").asInt();
      if (total != MEMBERS) {
        throw new IllegalStateException(
            "Org unit group %s has %d members %s the simulation, expected %d"
                .formatted(GROUP_UID, total, when, MEMBERS));
      }
      System.out.printf(
          "[CollectionItemsPerformanceTest] Org unit group %s has %d members %s the simulation%n",
          GROUP_UID, total, when);
    } catch (IOException e) {
      throw new IllegalStateException("Could not read the members of " + GROUP_UID, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while reading the members of " + GROUP_UID, e);
    }
  }

  @Override
  public void before() {
    checkGroupSize("before");
  }

  @Override
  public void after() {
    checkGroupSize("after");
  }

  public CollectionItemsPerformanceTest() {
    List<String> nonMembers = orgUnitUids("PfCa", ADDITIONS);
    List<String> members = orgUnitUids("PfCm", DELETIONS);
    String changeBody = itemsBody(nonMembers, members);
    String revertBody = itemsBody(members, nonMembers);

    // Authenticate once per virtual user, so the measured requests do not pay for the bcrypt
    // password check; same pattern as UsersPerformanceTest.
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .disableCaching();

    ChainBuilder authenticate =
        exec(flushCookieJar())
            .exec(
                http("Authenticate (session login)")
                    .get("/api/me")
                    .header("Authorization", "Basic " + BASIC_AUTH)
                    .check(status().is(200)));

    // Every item must change the collection: an ignored item means the group is not in the
    // expected state and the request measured less work than intended.
    ChainBuilder workflow =
        exec(
            http(CHANGE_REQUEST)
                .post(ITEMS_PATH)
                .body(StringBody(changeBody))
                .check(status().is(200))
                .check(jsonPath("$.response.stats.ignored").ofInt().is(0)),
            http(REVERT_REQUEST)
                .post(ITEMS_PATH)
                .body(StringBody(revertBody))
                .check(status().is(200))
                .check(jsonPath("$.response.stats.ignored").ofInt().is(0)));

    ScenarioBuilder scenario =
        scenario("OrganisationUnitGroup collection items (DHIS2-20613)")
            .exec(authenticate)
            .repeat(ITERATIONS)
            .on(workflow);

    // Regression guard, not a latency target: a request that loads the whole member collection
    // takes seconds with 100,000 members, while writing only the changed join table rows takes
    // well under the ceiling.
    //
    // One user runs the iterations one after the other, so every run has the same sample size
    // whatever the response times are.
    setUp(scenario.injectOpen(atOnceUsers(1)))
        .protocols(httpProtocol)
        .assertions(
            details(CHANGE_REQUEST).responseTime().percentile(95).lt(1_000),
            details(CHANGE_REQUEST).successfulRequests().percent().is(100D),
            details(REVERT_REQUEST).responseTime().percentile(95).lt(1_000),
            details(REVERT_REQUEST).successfulRequests().percent().is(100D));
  }
}
