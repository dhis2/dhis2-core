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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Performance test for {@code GET /api/trackedEntityAttributes/{uid}/generateAndReserve}.
 *
 * <p>Measures how long it takes for a single user to reserve {@code valuesToReserve} (default: 100)
 * values from a {@code RANDOM(####)} pattern (10 000 possible values) against a realistically
 * loaded database:
 *
 * <ul>
 *   <li>{@code backgroundReservedValues} (default: 5 000 000) reserved values for a different
 *       attribute, to stress-test table-size sensitivity
 *   <li>{@code backgroundTrackedEntities} (default: 10 000) tracked entities
 *   <li>{@code backgroundTeaCount} (default: 1 000) background tracked entity attributes
 * </ul>
 *
 * <p>Background reserved values are seeded via {@code psql} inside the running Docker container
 * ({@code dockerContainer} property). Seeding is skipped with a warning if the container or {@code
 * psql} is unreachable.
 *
 * <p>Run:
 *
 * <pre>{@code
 * mvn gatling:test \
 *   -Dgatling.simulationClass=org.hisp.dhis.test.platform.ReserveValuesPerformanceTest \
 *   -DbaseUrl=http://localhost:8080 \
 *   --file dhis-2/pom.xml -pl dhis-test-performance
 * }</pre>
 *
 * <p>Available properties (all optional — Sierra Leone demo DB defaults apply):
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code orgUnitUid} (default: {@code ImspTQPwCqd} — Sierra Leone root)
 *   <li>{@code teTypeUid} (default: {@code nEenWmSyUEp} — Person)
 *   <li>{@code backgroundReservedValues} (default: {@code 5000000})
 *   <li>{@code backgroundTrackedEntities} (default: {@code 10000})
 *   <li>{@code backgroundTeaCount} (default: {@code 1000})
 *   <li>{@code valuesToReserve} (default: {@code 100})
 *   <li>{@code dockerContainer} (default: {@code dhis2-core-db-1})
 *   <li>{@code dbUser} (default: {@code dhis})
 *   <li>{@code dbName} (default: {@code dhis})
 * </ul>
 */
public class ReserveValuesPerformanceTest extends Simulation {

  private static final Properties CONFIG = loadConfig();

  private static Properties loadConfig() {
    String path = System.getProperty("configFile");
    Properties props = new Properties();
    if (path != null) {
      try (FileInputStream fis = new FileInputStream(path)) {
        props.load(fis);
      } catch (IOException e) {
        System.err.println(
            "[ReserveValuesPerformanceTest] Warning: could not load configFile="
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
  private static final String ORG_UNIT_UID = prop("orgUnitUid", "ImspTQPwCqd");
  private static final String TE_TYPE_UID = prop("teTypeUid", "nEenWmSyUEp");
  private static final int BACKGROUND_RESERVED_VALUES =
      Integer.parseInt(prop("backgroundReservedValues", "5000000"));
  private static final int BACKGROUND_TRACKED_ENTITIES =
      Integer.parseInt(prop("backgroundTrackedEntities", "10000"));
  private static final int BACKGROUND_TEA_COUNT =
      Integer.parseInt(prop("backgroundTeaCount", "1000"));
  private static final int VALUES_TO_RESERVE = Integer.parseInt(prop("valuesToReserve", "100"));
  private static final String DOCKER_CONTAINER = prop("dockerContainer", "dhis2-core-db-1");
  private static final String DB_USER = prop("dbUser", "dhis");
  private static final String DB_NAME = prop("dbName", "dhis");

  private static final String RESERVE_REQUEST =
      "GET generateAndReserve - " + VALUES_TO_RESERVE + " values";

  private static volatile String teaUid;

  private static String auth;
  private static HttpClient httpClient;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static String post(String path, String body) throws Exception {
    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .header("Content-Type", "application/json")
            .header("Authorization", "Basic " + auth)
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body();
  }

  private static String createGeneratedTea() throws Exception {
    String body =
        """
        {
          "name": "PerfTest RANDOM(####) attr",
          "shortName": "PerfTestRandAttr",
          "valueType": "TEXT",
          "aggregationType": "NONE",
          "generated": true,
          "textPattern": {
            "segments": [{"segmentType": "RANDOM", "rawSegment": "RANDOM(####)"}]
          }
        }
        """;
    String response = post("/api/trackedEntityAttributes", body);
    String uid = MAPPER.readTree(response).path("response").path("uid").asText();
    if (uid.isBlank()) {
      throw new IllegalStateException("Failed to create test TEA: " + response);
    }
    return uid;
  }

  private static void createBackgroundTeas(int count) throws InterruptedException {
    System.out.printf("  Creating %d background TEAs (10 threads)...%n", count);
    ExecutorService pool = Executors.newFixedThreadPool(10);
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final int idx = i;
      futures.add(
          pool.submit(
              () -> {
                String body =
                    """
                    {"name":"BgTea%d","shortName":"BgTea%d","valueType":"TEXT","aggregationType":"NONE"}
                    """
                        .formatted(idx, idx);
                try {
                  post("/api/trackedEntityAttributes", body);
                } catch (Exception e) {
                  System.err.printf("  Background TEA %d failed: %s%n", idx, e.getMessage());
                }
                return null;
              }));
    }
    pool.shutdown();
    pool.awaitTermination(10, TimeUnit.MINUTES);
    System.out.println("  Background TEAs done.");
  }

  private static void createTrackedEntities(int total) throws Exception {
    int batchSize = 1000;
    System.out.printf("  Creating %d tracked entities in batches of %d...%n", total, batchSize);
    for (int start = 0; start < total; start += batchSize) {
      int count = Math.min(batchSize, total - start);
      StringBuilder tes = new StringBuilder("[");
      for (int i = 0; i < count; i++) {
        if (i > 0) tes.append(',');
        tes.append(
            """
            {"orgUnit":"%s","trackedEntityType":"%s"}"""
                .formatted(ORG_UNIT_UID, TE_TYPE_UID));
      }
      tes.append(']');
      post("/api/tracker?async=false", "{\"trackedEntities\":" + tes + "}");
      System.out.printf("  Tracked entities: %d/%d%n", start + count, total);
    }
  }

  private static void seedReservedValues(int count) {
    System.out.printf("  Seeding %d background reserved values via psql...%n", count);
    // owneruid 'seedBgAtr00' is a fictional 11-char uid that never conflicts with the test TEA.
    String sql =
        "INSERT INTO reservedvalue (created, expirydate, ownerobject, owneruid, key, value) "
            + "SELECT now(), now() + interval '1 year', "
            + "'TRACKEDENTITYATTRIBUTE', 'seedBgAtr00', 'seed', lpad(i::text, 10, '0') "
            + "FROM generate_series(1, "
            + count
            + ") AS i "
            + "ON CONFLICT DO NOTHING";

    try {
      Process p =
          new ProcessBuilder(
                  "docker", "exec", DOCKER_CONTAINER,
                  "psql", "-U", DB_USER, "-d", DB_NAME, "-c", sql)
              .redirectErrorStream(true)
              .start();
      String output =
          new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      int exit = p.waitFor();
      if (exit == 0) {
        System.out.println("  Reserved value seed done: " + output);
      } else {
        System.err.println("  psql seed failed (exit " + exit + "): " + output);
      }
    } catch (Exception e) {
      System.err.println(
          "  Could not seed reserved values (docker not reachable?): " + e.getMessage());
    }
  }

  @Override
  public void before() {
    auth =
        Base64.getEncoder()
            .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    httpClient = HttpClient.newHttpClient();

    try {
      System.out.println("=== ReserveValuesPerformanceTest setup ===");

      System.out.println("[1/4] Creating test TEA with RANDOM(####)...");
      teaUid = createGeneratedTea();
      System.out.println("      TEA uid: " + teaUid);

      System.out.println("[2/4] Creating " + BACKGROUND_TEA_COUNT + " background TEAs...");
      createBackgroundTeas(BACKGROUND_TEA_COUNT);

      System.out.println("[3/4] Creating " + BACKGROUND_TRACKED_ENTITIES + " tracked entities...");
      createTrackedEntities(BACKGROUND_TRACKED_ENTITIES);

      System.out.println(
          "[4/4] Seeding " + BACKGROUND_RESERVED_VALUES + " background reserved values...");
      seedReservedValues(BACKGROUND_RESERVED_VALUES);

      System.out.println("=== Setup complete — starting scenario ===");
    } catch (Exception e) {
      throw new RuntimeException("Setup failed", e);
    }
  }

  public ReserveValuesPerformanceTest() {
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .disableCaching()
            .basicAuth(USERNAME, PASSWORD);

    ScenarioBuilder reserve =
        scenario("Reserve " + VALUES_TO_RESERVE + " values - RANDOM(####)")
            .exec(session -> session.set("teaUid", teaUid))
            .exec(
                http(RESERVE_REQUEST)
                    .get(
                        "/api/trackedEntityAttributes/#{teaUid}/generateAndReserve"
                            + "?numberToReserve="
                            + VALUES_TO_RESERVE)
                    .check(status().is(200)));

    setUp(reserve.injectOpen(atOnceUsers(1))).protocols(httpProtocol);
  }
}
