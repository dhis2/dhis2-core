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
import java.time.Duration;
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
 * <p>Two complementary scenarios:
 *
 * <ul>
 *   <li><b>degradation</b> — 1 user, {@code degradationIterations} (default: 90) sequential
 *       requests of {@code valuesToReserve} (default: 100) values from a {@code RANDOM(####)}
 *       pattern (10 000-value pool). By the end, 90% of the pool is consumed; latency should climb
 *       visibly as available values thin out.
 *   <li><b>concurrency</b> — ramp to {@code concurrencyUsers} (default: 20) concurrent users, each
 *       making a single reservation request per virtual-user iteration, against a {@code
 *       RANDOM(XXXXXX)} pattern (≈308 M-value pool that never exhausts). Measures throughput and
 *       p95 under concurrent load.
 *   <li><b>both</b> (default) — runs degradation first, then concurrency.
 * </ul>
 *
 * <p>Background load applied to both scenarios:
 *
 * <ul>
 *   <li>{@code backgroundReservedValues} (default: 5 000 000) reserved values for a different
 *       attribute, to stress-test table-size sensitivity
 *   <li>{@code backgroundTrackedEntities} (default: 10 000) tracked entities
 *   <li>{@code backgroundTeaCount} (default: 1 000) background tracked entity attributes
 *   <li>{@code backgroundTeavCount} (default: 10 000 000) tracked entity attribute values
 * </ul>
 *
 * <p>Background reserved values and TEAVs are seeded via {@code psql} inside the running Docker
 * container ({@code dockerContainer} property). Seeding is skipped with a warning if the container
 * or {@code psql} is unreachable.
 *
 * <p>Run:
 *
 * <pre>{@code
 * mvn gatling:test \
 *   -Dgatling.simulationClass=org.hisp.dhis.test.platform.ReserveValuesPerformanceTest \
 *   -DbaseUrl=http://localhost:8080 \
 *   -f dhis-2/dhis-test-performance/pom.xml
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
 *   <li>{@code backgroundTeavCount} (default: {@code 10000000})
 *   <li>{@code valuesToReserve} (default: {@code 100})
 *   <li>{@code dockerContainer} (default: {@code dhis2-core-db-1})
 *   <li>{@code dbUser} (default: {@code dhis})
 *   <li>{@code dbName} (default: {@code dhis})
 *   <li>{@code mode} (default: {@code both}; options: {@code degradation}, {@code concurrency},
 *       {@code both})
 *   <li>{@code degradationIterations} (default: {@code 90})
 *   <li>{@code concurrencyUsers} (default: {@code 20})
 *   <li>{@code concurrencyRampSeconds} (default: {@code 30})
 *   <li>{@code concurrencySustainSeconds} (default: {@code 60})
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
  private static final int BACKGROUND_TEAV_COUNT =
      Integer.parseInt(prop("backgroundTeavCount", "10000000"));
  private static final int BACKGROUND_TRACKED_ENTITIES =
      Integer.parseInt(prop("backgroundTrackedEntities", "10000"));
  private static final int BACKGROUND_TEA_COUNT =
      Integer.parseInt(prop("backgroundTeaCount", "1000"));
  private static final int VALUES_TO_RESERVE = Integer.parseInt(prop("valuesToReserve", "100"));
  private static final String DOCKER_CONTAINER = prop("dockerContainer", "dhis2-core-db-1");
  private static final String DB_USER = prop("dbUser", "dhis");
  private static final String DB_NAME = prop("dbName", "dhis");
  private static final String MODE = prop("mode", "both");
  private static final int DEGRADATION_ITERATIONS =
      Integer.parseInt(prop("degradationIterations", "95"));
  private static final int CONCURRENCY_USERS =
      Integer.parseInt(prop("concurrencyUsers", "20"));
  private static final int CONCURRENCY_RAMP_SECONDS =
      Integer.parseInt(prop("concurrencyRampSeconds", "30"));
  private static final int CONCURRENCY_SUSTAIN_SECONDS =
      Integer.parseInt(prop("concurrencySustainSeconds", "60"));

  private static volatile String degradationRandomTeaUid;
  private static volatile String degradationSequentialTeaUid;
  private static volatile String concurrencyRandomTeaUid;
  private static volatile String concurrencySequentialTeaUid;

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

  private static String createGeneratedTea(
      String pattern, String namePrefix, String shortPrefix) throws Exception {
    String suffix = String.valueOf(System.currentTimeMillis() % 100_000);
    String body =
        """
        {
          "name": "%s %s",
          "shortName": "%s%s",
          "valueType": "TEXT",
          "aggregationType": "NONE",
          "generated": true,
          "pattern": "%s"
        }
        """
            .formatted(namePrefix, suffix, shortPrefix, suffix, pattern);
    String response = post("/api/trackedEntityAttributes", body);
    String uid = MAPPER.readTree(response).path("response").path("uid").asText();
    if (uid.isBlank()) {
      throw new IllegalStateException(
          "Failed to create test TEA (" + namePrefix + "): " + response);
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
    // owneruid 'seedBgAtr00' is a fictional 11-char uid that never conflicts with the test TEAs.
    String sql =
        "INSERT INTO reservedvalue (reservedvalueid, created, expirydate, ownerobject, owneruid, key, value) "
            + "SELECT nextval('reservedvalue_sequence'), now(), now() + interval '1 year', "
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

  private static void seedTrackedEntityAttributeValues(List<String> excludeTeaUids, int count) {
    System.out.printf("  Seeding %d TEAVs via psql (cross join TEAs × TEs)...%n", count);
    // Cross joins the most recently created TEAs (excluding the test TEAs) with the most recently
    // created TEs. Using DESC order picks our setup data, avoiding conflicts with existing
    // Sierra Leone demo TEAVs. 1 000 TEAs × 10 000 TEs = 10M rows; LIMIT caps the total.
    String excludeList =
        String.join(
            ", ",
            excludeTeaUids.stream().map(uid -> "'" + uid + "'").toList());
    String sql =
        "WITH teas AS ("
            + "  SELECT trackedentityattributeid FROM trackedentityattribute"
            + "  WHERE uid NOT IN ("
            + excludeList
            + ") ORDER BY trackedentityattributeid DESC LIMIT 1000"
            + "), tes AS ("
            + "  SELECT trackedentityid FROM trackedentity"
            + "  ORDER BY trackedentityid DESC LIMIT 10000"
            + "), candidates AS ("
            + "  SELECT tea.trackedentityattributeid, te.trackedentityid"
            + "  FROM teas tea CROSS JOIN tes te LIMIT "
            + count
            + ") "
            + "INSERT INTO trackedentityattributevalue"
            + " (trackedentityattributeid, trackedentityid, value, created, lastupdated, storedby)"
            + " SELECT trackedentityattributeid, trackedentityid,"
            + " 'perf-test', now(), now(), 'perf-seed'"
            + " FROM candidates"
            + " ON CONFLICT DO NOTHING";

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
        System.out.println("  TEAV seed done: " + output);
      } else {
        System.err.println("  TEAV psql seed failed (exit " + exit + "): " + output);
      }
    } catch (Exception e) {
      System.err.println(
          "  Could not seed TEAVs (docker not reachable?): " + e.getMessage());
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

      System.out.println("[1/6] Creating degradation TEAs...");
      degradationRandomTeaUid = createGeneratedTea("RANDOM(####)", "PerfTest Deg Random", "PerfDegR");
      degradationSequentialTeaUid = createGeneratedTea("SEQUENTIAL(####)", "PerfTest Deg Sequential", "PerfDegS");
      System.out.println("      Degradation random TEA uid: " + degradationRandomTeaUid);
      System.out.println("      Degradation sequential TEA uid: " + degradationSequentialTeaUid);

      System.out.println("[2/6] Creating concurrency TEAs...");
      concurrencyRandomTeaUid = createGeneratedTea("RANDOM(XXXXXX)", "PerfTest Con Random", "PerfConR");
      concurrencySequentialTeaUid = createGeneratedTea("SEQUENTIAL(######)", "PerfTest Con Sequential", "PerfConS");
      System.out.println("      Concurrency random TEA uid: " + concurrencyRandomTeaUid);
      System.out.println("      Concurrency sequential TEA uid: " + concurrencySequentialTeaUid);

      System.out.println("[3/6] Creating " + BACKGROUND_TEA_COUNT + " background TEAs...");
      createBackgroundTeas(BACKGROUND_TEA_COUNT);

      System.out.println("[4/6] Creating " + BACKGROUND_TRACKED_ENTITIES + " tracked entities...");
      createTrackedEntities(BACKGROUND_TRACKED_ENTITIES);

      System.out.println(
          "[5/6] Seeding " + BACKGROUND_RESERVED_VALUES + " background reserved values...");
      seedReservedValues(BACKGROUND_RESERVED_VALUES);

      System.out.println("[6/6] Seeding " + BACKGROUND_TEAV_COUNT + " background TEAVs...");
      seedTrackedEntityAttributeValues(
          List.of(degradationRandomTeaUid, degradationSequentialTeaUid,
              concurrencyRandomTeaUid, concurrencySequentialTeaUid),
          BACKGROUND_TEAV_COUNT);

      System.out.println("=== Setup complete — starting scenario: " + MODE + " ===");
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

    int halfValues = VALUES_TO_RESERVE / 2;

    // Degradation: 1 user, N sequential iterations. Each iteration reserves half values from
    // RANDOM(####) (10k pool — degrades as pool drains) and half from SEQUENTIAL(####).
    ScenarioBuilder degradationScenario =
        scenario("Pool Degradation - RANDOM(####) + SEQUENTIAL(####)")
            .exec(
                session ->
                    session
                        .set("randomTeaUid", degradationRandomTeaUid)
                        .set("sequentialTeaUid", degradationSequentialTeaUid))
            .repeat(DEGRADATION_ITERATIONS)
            .on(
                exec(
                        http("GET generateAndReserve - " + halfValues + " values [degradation-random]")
                            .get(
                                "/api/trackedEntityAttributes/#{randomTeaUid}/generateAndReserve"
                                    + "?numberToReserve="
                                    + halfValues)
                            .check(status().in(200, 409)))
                    .exec(
                        http(
                                "GET generateAndReserve - "
                                    + halfValues
                                    + " values [degradation-sequential]")
                            .get(
                                "/api/trackedEntityAttributes/#{sequentialTeaUid}/generateAndReserve"
                                    + "?numberToReserve="
                                    + halfValues)
                            .check(status().in(200, 409))));

    // Concurrency: ramp to N users. Each iteration reserves half values from RANDOM(XXXXXX)
    // (~308M pool) and half from SEQUENTIAL(######) (1M pool — neither exhausts under load).
    ScenarioBuilder concurrencyScenario =
        scenario("Concurrency - RANDOM(XXXXXX) + SEQUENTIAL(######)")
            .exec(
                session ->
                    session
                        .set("randomTeaUid", concurrencyRandomTeaUid)
                        .set("sequentialTeaUid", concurrencySequentialTeaUid))
            .exec(
                http("GET generateAndReserve - " + halfValues + " values [concurrency-random]")
                    .get(
                        "/api/trackedEntityAttributes/#{randomTeaUid}/generateAndReserve"
                            + "?numberToReserve="
                            + halfValues)
                    .check(status().is(200)))
            .exec(
                http("GET generateAndReserve - " + halfValues + " values [concurrency-sequential]")
                    .get(
                        "/api/trackedEntityAttributes/#{sequentialTeaUid}/generateAndReserve"
                            + "?numberToReserve="
                            + halfValues)
                    .check(status().is(200)));

    PopulationBuilder degradationPop = degradationScenario.injectOpen(atOnceUsers(1));
    PopulationBuilder concurrencyPop =
        concurrencyScenario.injectClosed(
            rampConcurrentUsers(0)
                .to(CONCURRENCY_USERS)
                .during(Duration.ofSeconds(CONCURRENCY_RAMP_SECONDS)),
            constantConcurrentUsers(CONCURRENCY_USERS)
                .during(Duration.ofSeconds(CONCURRENCY_SUSTAIN_SECONDS)));

    PopulationBuilder population =
        switch (MODE) {
          case "degradation" -> degradationPop;
          case "concurrency" -> concurrencyPop;
          default -> degradationPop.andThen(concurrencyPop);
        };

    setUp(population).protocols(httpProtocol);
  }
}
