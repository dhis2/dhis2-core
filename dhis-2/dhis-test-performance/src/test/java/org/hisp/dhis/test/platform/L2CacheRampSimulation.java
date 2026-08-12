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

import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.flushCookieJar;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.PopulationBuilder;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * Base class for the L2-cache concurrency-baseline ramp simulations.
 *
 * <p>Runs the same workflow at a sequence of fixed concurrency plateaus (default 10, 50, 100, 200,
 * 400 concurrent users), one closed-model population per step, chained with {@code andThen} so the
 * steps never overlap. Every request name is prefixed with the step label ({@code c010}, {@code
 * c050}, ...) so per-step p50/p95/p99 fall out of the standard Gatling stats without timestamp
 * arithmetic.
 *
 * <p>Virtual users authenticate once (session login, separately named request so bcrypt cost does
 * not pollute endpoint latencies) and then loop the workflow with no think time for the step
 * duration: this is a deliberate worst-case pressure profile for the Hibernate second-level cache
 * region locks ({@code AbstractReadWriteAccess}), not a realistic user model.
 *
 * <p>No Gatling assertions on purpose: these simulations produce a measurement baseline (cache ON
 * vs OFF, baseline vs candidate); a failed run must still yield its artifact bundle. Regression
 * gates belong to the before/after comparison consuming the bundles, not to the simulation.
 *
 * <p>Available properties (system property first, then optional {@code -DconfigFile=} properties
 * file, then default):
 *
 * <ul>
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code steps} (default: {@code 10,50,100,200,400} concurrent users)
 *   <li>{@code stepDurationSec} (default: {@code 60}) plateau duration per step
 *   <li>{@code rampDurationSec} (default: {@code 10}) ramp-up into each plateau
 *   <li>{@code orgUnitUid} (default: {@code ImspTQPwCqd} — Sierra Leone root org unit)
 *   <li>{@code writePercent} (default: {@code 5}) share of workflow iterations that issue a
 *       metadata write (only used by the write-mixed simulation)
 * </ul>
 *
 * @author Morten Svanæs
 */
abstract class L2CacheRampSimulation extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
        System.out.println("[L2CacheRampSimulation] Loaded config from: " + path);
      } catch (IOException e) {
        System.err.println(
            "[L2CacheRampSimulation] Warning: could not load configFile="
                + path
                + ": "
                + e.getMessage());
      }
    }
    return props;
  }

  protected static String prop(String key, String defaultValue) {
    String sys = System.getProperty(key);
    if (sys != null) return sys;
    String file = CONFIG.getProperty(key);
    return file != null ? file : defaultValue;
  }

  protected static final String BASE_URL = prop("baseUrl", "http://localhost:8080");
  protected static final String USERNAME = prop("username", "admin");
  protected static final String PASSWORD = prop("password", "district");
  protected static final String BASIC_AUTH =
      Base64.getEncoder()
          .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
  protected static final String ORG_UNIT_UID = prop("orgUnitUid", "ImspTQPwCqd");
  protected static final int STEP_DURATION_SEC = Integer.parseInt(prop("stepDurationSec", "60"));
  protected static final int RAMP_DURATION_SEC = Integer.parseInt(prop("rampDurationSec", "10"));
  protected static final int WRITE_PERCENT = Integer.parseInt(prop("writePercent", "5"));

  protected static final List<Integer> STEPS =
      Arrays.stream(prop("steps", "10,50,100,200,400").split(","))
          .map(String::trim)
          .map(Integer::parseInt)
          .toList();

  /**
   * Data element UIDs fetched once at simulation start; fed into by-id reads and metadata writes so
   * load spreads over real entities instead of one hardcoded UID.
   */
  protected static final FeederBuilder<Object> DATA_ELEMENT_FEEDER =
      listFeeder(fetchDataElementUids()).random();

  private static List<Map<String, Object>> fetchDataElementUids() {
    String url = BASE_URL + "/api/dataElements.json?fields=id&paging=false";
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .header("Authorization", "Basic " + BASIC_AUTH)
              .GET()
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "Fetching data element UIDs failed with HTTP " + response.statusCode());
      }
      JsonNode root = new ObjectMapper().readTree(response.body());
      List<Map<String, Object>> records = new ArrayList<>();
      for (JsonNode de : root.path("dataElements")) {
        records.add(Map.of("deUid", de.path("id").asText()));
      }
      if (records.isEmpty()) {
        throw new IllegalStateException("No data elements found at " + url);
      }
      System.out.println(
          "[L2CacheRampSimulation] Fetched " + records.size() + " data element UIDs");
      return records;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to fetch data element UIDs from " + url, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while fetching data element UIDs", e);
    }
  }

  /** Hot-metadata read chain shared by both simulations; {@code p} is the step name prefix. */
  protected static ChainBuilder reads(String p) {
    return exec(http(p + " me").get("/api/me").check(status().is(200)))
        .exec(
            http(p + " dataElement byId").get("/api/dataElements/#{deUid}").check(status().is(200)))
        .exec(
            http(p + " dataElements list")
                .get("/api/dataElements")
                .queryParam("pageSize", "50")
                .queryParam("page", "#{randomInt(1,5)}")
                .check(status().is(200)))
        .exec(
            http(p + " dataElements filtered")
                .get("/api/dataElements")
                .queryParam(
                    "fields", "id,name,categoryCombo[id,name,categoryOptionCombos[id,name]]")
                .queryParam("pageSize", "50")
                .queryParam("page", "#{randomInt(1,5)}")
                .check(status().is(200)))
        .exec(
            http(p + " categoryCombos filtered")
                .get("/api/categoryCombos")
                .queryParam("fields", "id,name,categories[id,name,categoryOptions[id,name]]")
                .check(status().is(200)))
        .exec(
            http(p + " orgUnits filtered")
                .get("/api/organisationUnits")
                .queryParam("fields", "id,name,level,parent[id,name]")
                .queryParam("pageSize", "100")
                .queryParam("page", "#{randomInt(1,10)}")
                .check(status().is(200)))
        .exec(
            http(p + " orgUnit subtree")
                .get("/api/organisationUnits/" + ORG_UNIT_UID)
                .queryParam("fields", "id,name,children[id,name,children[id,name]]")
                .check(status().is(200)));
  }

  /**
   * Builds one closed-model population per concurrency step and installs them sequentially. Called
   * exactly once from the concrete simulation's constructor.
   *
   * @param workflowFactory step-name-prefix -> workflow chain executed in a loop by every virtual
   *     user for the step duration
   */
  protected void install(Function<String, ChainBuilder> workflowFactory) {
    // No protocol-level basicAuth: DHIS2 is stateful, so authenticate once per virtual user via a
    // separately-named request and let the session cookie carry the rest -- same pattern as
    // UsersPerformanceTest.
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL).acceptHeader("application/json").disableCaching();

    PopulationBuilder all = null;
    for (int users : STEPS) {
      String p = String.format("c%03d", users);
      ChainBuilder authenticate =
          exec(flushCookieJar())
              .exec(
                  http(p + " login")
                      .get("/api/me")
                      .header("Authorization", "Basic " + BASIC_AUTH)
                      .check(status().is(200)));

      ScenarioBuilder scn =
          scenario(getClass().getSimpleName() + " " + p)
              .exec(authenticate)
              .during(Duration.ofSeconds(STEP_DURATION_SEC))
              .on(feed(DATA_ELEMENT_FEEDER).exec(workflowFactory.apply(p)));

      PopulationBuilder pop =
          scn.injectClosed(
              rampConcurrentUsers(0).to(users).during(Duration.ofSeconds(RAMP_DURATION_SEC)),
              constantConcurrentUsers(users).during(Duration.ofSeconds(STEP_DURATION_SEC)));

      all = all == null ? pop : all.andThen(pop);
    }

    // ramp + plateau per step, plus a full plateau of tail allowance (users started late in the
    // injection window run their whole during() loop after injection stops)
    int totalSeconds = STEPS.size() * (RAMP_DURATION_SEC + 2 * STEP_DURATION_SEC);
    setUp(all).protocols(httpProtocol).maxDuration(Duration.ofSeconds(totalSeconds + 300));
  }
}
