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
package org.hisp.dhis.test.tracker;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for tracker imports of TEAVs that use reserved values (RANDOM text pattern).
 *
 * <p>Measures the throughput improvement from removing the synchronous {@code DELETE FROM
 * reservedvalue} that the old {@code handleReservedValue()} path executed on every TEAV save for
 * generated attributes.
 *
 * <h2>Data setup (one-time, skipped if already present)</h2>
 *
 * <ol>
 *   <li>Creates 20 TEAs with RANDOM patterns via {@code POST /api/metadata}.
 *   <li>Seeds 1M tracked entities and 10M TEAVs directly in PostgreSQL via {@code docker exec
 *       psql}:
 *       <ul>
 *         <li>1M sequential-format TEAVs for {@code perfTestUid} (PerfTea0001)
 *         <li>1M random-format TEAVs for PerfTea0002 (realistic noise)
 *         <li>8M regular TEAVs across PerfTea0003–PerfTea0010 (production-scale bulk)
 *       </ul>
 *   <li>Generates 100k reserved values via {@code GET
 *       /api/trackedEntityAttributes/{uid}/generateAndReserve} (5k per TEA × 20 TEAs, batched at 1k
 *       per call).
 *   <li>Writes {@code rv_1k.ndjson.gz} (1 000 TE payloads) and {@code rv_100k.ndjson.gz} (100 000
 *       TE payloads) to {@code ~/.cache/dhis2/perf/reservedvalue/}.
 * </ol>
 *
 * <h2>Scenarios</h2>
 *
 * <ul>
 *   <li><b>Import 1k</b> — 20 requests × 50 TEs, single user
 *   <li><b>Import 100k</b> — 2 000 requests × 50 TEs, single user
 * </ul>
 *
 * <h2>Key system properties</h2>
 *
 * <ul>
 *   <li>{@code -Dinstance} — DHIS2 base URL (default: {@code http://localhost:8080})
 *   <li>{@code -DdbContainer} — Docker container name for the DB (default: {@code dhis2-core-db-1})
 *   <li>{@code -DskipSeed} — set to {@code true} to skip SQL seeding (e.g. already seeded)
 * </ul>
 */
public class ReservedValuePerformanceTest extends Simulation {

  private static final Logger log = LoggerFactory.getLogger(ReservedValuePerformanceTest.class);

  // ── Configuration ───────────────────────────────────────────────────────────

  private static final String BASE_URL = System.getProperty("instance", "http://localhost:8080");
  private static final String ADMIN_USER = System.getProperty("adminUser", "admin");
  private static final String ADMIN_PASSWORD = System.getProperty("adminPassword", "district");
  private static final String DB_CONTAINER = System.getProperty("dbContainer", "dhis2-core-db-1");
  private static final String DB_NAME = System.getProperty("dbName", "dhis");
  private static final String DB_USER = System.getProperty("dbUser", "dhis");
  private static final boolean SKIP_SEED = Boolean.getBoolean("skipSeed");

  // ── Metadata UIDs (Sierra Leone demo DB) ────────────────────────────────────

  /** Person TrackedEntityType from the Sierra Leone demo DB. */
  private static final String PERSON_TET_UID = "nEenWmSyUEp";

  /** Ngelehun CHC — a leaf org unit from the Sierra Leone demo DB. */
  private static final String ORG_UNIT_UID = "DiszpKrYNg8";

  // ── Perf TEA UIDs (fixed, deterministic across runs) ────────────────────────

  /**
   * 20 TEAs with RANDOM patterns. Fixed UIDs so the setup can be idempotent (skipped if already
   * present).
   */
  private static final List<String> TEA_UIDS =
      List.of(
          "PerfTea0001",
          "PerfTea0002",
          "PerfTea0003",
          "PerfTea0004",
          "PerfTea0005",
          "PerfTea0006",
          "PerfTea0007",
          "PerfTea0008",
          "PerfTea0009",
          "PerfTea0010",
          "PerfTea0011",
          "PerfTea0012",
          "PerfTea0013",
          "PerfTea0014",
          "PerfTea0015",
          "PerfTea0016",
          "PerfTea0017",
          "PerfTea0018",
          "PerfTea0019",
          "PerfTea0020");

  /**
   * The TEA that receives 1M sequential-format TEAVs during background seeding. Corresponds to
   * "perfTestUid" in the test description.
   */
  private static final String PERF_TEST_UID = TEA_UIDS.get(0); // PerfTea0001

  /** One RANDOM pattern per TEA — varying format gives realistic distribution. */
  private static final List<String> PATTERNS =
      List.of(
          "RANDOM(########)", // 10^8 values — needed because 1M TEAVs are seeded for this TEA
          "RANDOM(XXX###)",
          "RANDOM(XXXXXX)",
          "RANDOM(XX####)",
          "RANDOM(#####X)",
          "RANDOM(X#####)",
          "RANDOM(##XX##)",
          "RANDOM(####XX)",
          "RANDOM(XXXXX#)",
          "RANDOM(#XXXXX)",
          "RANDOM(##XXXX)",
          "RANDOM(XXXX##)",
          "RANDOM(###XXX)",
          "RANDOM(####X#)",
          "RANDOM(X#X#X#)",
          "RANDOM(##X##X)",
          "RANDOM(X###X#)",
          "RANDOM(#XX###)",
          "RANDOM(XXX##X)",
          "RANDOM(##XXX#)");

  // ── Scenario sizing ──────────────────────────────────────────────────────────

  private static final int SMALL_IMPORT = 1_000;
  private static final int LARGE_IMPORT = 100_000;
  private static final int PER_REQUEST = 50;
  private static final int RESERVED_PER_TEA = LARGE_IMPORT / TEA_UIDS.size(); // 5 000
  private static final int MAX_RESERVE_PER_CALL = 1_000;

  // ── State ────────────────────────────────────────────────────────────────────

  private final NdjsonFeeder feeder1k;
  private final NdjsonFeeder feeder100k;

  // ── Constructor (Gatling runs this before the simulation) ───────────────────

  public ReservedValuePerformanceTest() {
    log.info("=== ReservedValuePerformanceTest — setup start ===");

    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      ObjectMapper mapper = new ObjectMapper();
      String auth =
          Base64.getEncoder()
              .encodeToString((ADMIN_USER + ":" + ADMIN_PASSWORD).getBytes(StandardCharsets.UTF_8));

      // Phase 1: metadata
      log.info("Phase 1/3 — creating {} TEAs with RANDOM patterns...", TEA_UIDS.size());
      createTeAs(httpClient, auth, mapper);

      // Phase 2: background data
      if (SKIP_SEED) {
        log.info("Phase 2/3 — SQL seeding skipped (-DskipSeed=true)");
      } else {
        log.info("Phase 2/3 — seeding background data (1M TEs, 10M TEAVs) via {}...", DB_CONTAINER);
        seedBackgroundData();
      }

      // Phase 3: reserved values + ndjson files
      log.info(
          "Phase 3/3 — generating {} reserved values ({}/TEA × {} TEAs)...",
          LARGE_IMPORT,
          RESERVED_PER_TEA,
          TEA_UIDS.size());
      Path cacheDir =
          Path.of(System.getProperty("user.home"), ".cache", "dhis2", "perf", "reservedvalue");
      Files.createDirectories(cacheDir);

      List<String[]> reservedValues = generateReservedValues(httpClient, auth, mapper);
      log.info("Reserved {} values total", reservedValues.size());

      Path small = cacheDir.resolve("rv_1k.ndjson.gz");
      Path large = cacheDir.resolve("rv_100k.ndjson.gz");
      writeNdjson(small, reservedValues.subList(0, Math.min(SMALL_IMPORT, reservedValues.size())));
      writeNdjson(large, reservedValues);

      this.feeder1k = new NdjsonFeeder(small);
      this.feeder100k = new NdjsonFeeder(large);

    } catch (Exception e) {
      throw new RuntimeException("Setup failed", e);
    }

    log.info("=== Setup complete — starting Gatling scenarios ===");

    HttpProtocolBuilder protocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/ReservedValuePerfTest")
            .disableCaching()
            .disableFollowRedirect()
            .check(status().is(200));

    ScenarioBuilder small1k =
        buildImportScenario("Import 1k reserved values", feeder1k, SMALL_IMPORT);
    ScenarioBuilder large100k =
        buildImportScenario("Import 100k reserved values", feeder100k, LARGE_IMPORT);

    setUp(small1k.injectOpen(atOnceUsers(1)).andThen(large100k.injectOpen(atOnceUsers(1))))
        .protocols(protocol)
        .assertions(forAll().successfulRequests().percent().gte(100d));
  }

  // ── Phase 1: create TEAs ─────────────────────────────────────────────────────

  private void createTeAs(HttpClient httpClient, String auth, ObjectMapper mapper)
      throws Exception {
    // Build metadata payload with all 20 TEAs; the bundle hook will parse the pattern
    // string into a TextPattern on save.
    StringBuilder attrs = new StringBuilder();
    for (int i = 0; i < TEA_UIDS.size(); i++) {
      if (i > 0) attrs.append(",\n");
      attrs.append(teaJson(TEA_UIDS.get(i), i + 1, PATTERNS.get(i)));
    }
    String body =
        """
        {
          "trackedEntityAttributes": [
            %s
          ]
        }
        """
            .formatted(attrs);

    HttpRequest req =
        HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/api/metadata?importStrategy=CREATE_AND_UPDATE"))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    if (resp.statusCode() != 200) {
      throw new RuntimeException(
          "Metadata import failed: HTTP " + resp.statusCode() + " — " + resp.body());
    }

    JsonNode summary = mapper.readTree(resp.body());
    log.info("Metadata import: {}", summary.path("stats").toString());
  }

  private static String teaJson(String uid, int index, String pattern) {
    return """
        {
          "id": "%s",
          "name": "Perf Generated Attr %d",
          "shortName": "PGA%02d",
          "code": "PERF_GEN_%02d",
          "valueType": "TEXT",
          "aggregationType": "NONE",
          "generated": true,
          "pattern": "%s"
        }"""
        .formatted(uid, index, index, index, pattern);
  }

  // ── Phase 2: SQL seeding ─────────────────────────────────────────────────────

  /**
   * Seeds background data directly in PostgreSQL via {@code docker exec psql}. Idempotent: skips if
   * {@code Perf0000001} already exists in the {@code trackedentity} table.
   *
   * <p>Distribution:
   *
   * <pre>
   *   1M TEs          — UIDs Perf0000001 … Perf1000000
   *   1M TEAVs        — PerfTea0001 ("perfTestUid"), sequential-format values SEQ0000001 …
   *   1M TEAVs        — PerfTea0002, md5-derived random values (realistic noise)
   *   8M TEAVs        — PerfTea0003 … PerfTea0010, 1M each (production-scale bulk)
   *   ─────────────────────────────────────────────────────────────────────────────
   *   10M TEAVs total  ≈ 10 per TE
   * </pre>
   */
  private void seedBackgroundData() throws Exception {
    String sql =
        """
        DO $$
        DECLARE
            v_tet_id   INTEGER;
            v_ou_id    INTEGER;
            v_tea_ids  INTEGER[];
            v_base_id  BIGINT;
            i          INTEGER;
        BEGIN
            -- guard: skip if already seeded
            IF EXISTS (SELECT 1 FROM trackedentity WHERE uid = 'Perf0000001') THEN
                RAISE NOTICE 'Background data already present, skipping seed.';
                RETURN;
            END IF;

            SELECT trackedentitytypeid  INTO v_tet_id FROM trackedentitytype  WHERE uid = 'nEenWmSyUEp';
            SELECT organisationunitid   INTO v_ou_id  FROM organisationunit   WHERE uid = 'DiszpKrYNg8';

            IF v_tet_id IS NULL THEN
                RAISE EXCEPTION 'TrackedEntityType nEenWmSyUEp not found — is the Sierra Leone demo DB loaded?';
            END IF;
            IF v_ou_id IS NULL THEN
                RAISE EXCEPTION 'OrgUnit DiszpKrYNg8 not found.';
            END IF;

            -- resolve the 10 TEA IDs used for TEAVs
            v_tea_ids := ARRAY(
                SELECT trackedentityattributeid
                FROM   trackedentityattribute
                WHERE  uid IN (
                    'PerfTea0001','PerfTea0002','PerfTea0003','PerfTea0004','PerfTea0005',
                    'PerfTea0006','PerfTea0007','PerfTea0008','PerfTea0009','PerfTea0010'
                )
                ORDER BY uid
            );

            IF array_length(v_tea_ids, 1) < 10 THEN
                RAISE EXCEPTION 'Expected 10 perf TEAs, found %. Run Phase 1 first.',
                    COALESCE(array_length(v_tea_ids, 1), 0);
            END IF;

            SELECT COALESCE(MAX(trackedentityid), 0) INTO v_base_id FROM trackedentity;

            -- 1M tracked entities
            RAISE NOTICE 'Inserting 1M tracked entities (base_id = %)...', v_base_id;
            INSERT INTO trackedentity
                (trackedentityid, uid, created, lastupdated,
                 trackedentitytypeid, organisationunitid, inactive, deleted)
            SELECT
                v_base_id + gs,
                'Perf' || lpad(gs::text, 7, '0'),
                NOW() - ((gs % 365) || ' days')::interval,
                NOW() - ((gs % 30)  || ' days')::interval,
                v_tet_id,
                v_ou_id,
                false,
                false
            FROM generate_series(1, 1000000) gs;

            -- 1M sequential-format TEAVs for PerfTea0001 (perfTestUid)
            RAISE NOTICE 'Inserting 1M sequential TEAVs for PerfTea0001...';
            INSERT INTO trackedentityattributevalue
                (trackedentityid, trackedentityattributeid, created, lastupdated, value)
            SELECT
                v_base_id + gs,
                v_tea_ids[1],
                NOW(),
                NOW(),
                'SEQ' || lpad(gs::text, 7, '0')
            FROM generate_series(1, 1000000) gs;

            -- 1M random-format TEAVs for PerfTea0002 (noise)
            RAISE NOTICE 'Inserting 1M random TEAVs for PerfTea0002...';
            INSERT INTO trackedentityattributevalue
                (trackedentityid, trackedentityattributeid, created, lastupdated, value)
            SELECT
                v_base_id + gs,
                v_tea_ids[2],
                NOW(),
                NOW(),
                upper(substring(md5(gs::text), 1, 6))
            FROM generate_series(1, 1000000) gs;

            -- 8M regular TEAVs across PerfTea0003..PerfTea0010 (1M per TEA)
            FOR i IN 3..10 LOOP
                RAISE NOTICE 'Inserting 1M regular TEAVs for TEA index %/10...', i;
                INSERT INTO trackedentityattributevalue
                    (trackedentityid, trackedentityattributeid, created, lastupdated, value)
                SELECT
                    v_base_id + gs,
                    v_tea_ids[i],
                    NOW(),
                    NOW(),
                    'VAL' || lpad(i::text, 2, '0') || lpad(gs::text, 6, '0')
                FROM generate_series(1, 1000000) gs;
            END LOOP;

            -- Advance sequences past the bulk-inserted IDs so DHIS2 doesn't collide
            PERFORM setval('hibernate_sequence',              (SELECT MAX(trackedentityid) FROM trackedentity));
            PERFORM setval('trackedentityinstance_sequence',  (SELECT MAX(trackedentityid) FROM trackedentity));
            RAISE NOTICE 'Seeding complete: 1M TEs, 10M TEAVs. Sequence advanced to %.',
                (SELECT last_value FROM trackedentityinstance_sequence);
        END $$;
        """;

    runSql(sql, /* timeoutSeconds= */ 600);
  }

  /** Runs SQL against the DB container via {@code docker exec psql}. */
  private void runSql(String sql, int timeoutSeconds) throws Exception {
    ProcessBuilder pb =
        new ProcessBuilder(
            "docker",
            "exec",
            "-i",
            DB_CONTAINER,
            "psql",
            "-U",
            DB_USER,
            "-d",
            DB_NAME,
            "-v",
            "ON_ERROR_STOP=1");
    pb.redirectErrorStream(true);
    Process process = pb.start();
    process.getOutputStream().write(sql.getBytes(StandardCharsets.UTF_8));
    process.getOutputStream().close();

    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

    if (!finished) {
      process.destroyForcibly();
      throw new RuntimeException("SQL timed out after " + timeoutSeconds + "s");
    }
    if (process.exitValue() != 0) {
      throw new RuntimeException("psql failed (exit " + process.exitValue() + "):\n" + output);
    }
    // Print NOTICE lines so the user sees seeding progress
    for (String line : output.split("\n")) {
      if (line.startsWith("NOTICE")) log.info("  DB: {}", line);
    }
  }

  // ── Phase 3: reserved values + ndjson ───────────────────────────────────────

  /**
   * Calls {@code GET /api/trackedEntityAttributes/{uid}/generateAndReserve?numberToReserve=1000} in
   * batches of 1 000 until {@link #RESERVED_PER_TEA} values are collected for each of the 20 TEAs,
   * for {@link #LARGE_IMPORT} (100 000) total.
   *
   * @return list of {@code [teaUid, value]} pairs
   */
  private List<String[]> generateReservedValues(
      HttpClient httpClient, String auth, ObjectMapper mapper) throws Exception {
    List<String[]> all = new ArrayList<>(LARGE_IMPORT);
    int callsPerTea = (int) Math.ceil((double) RESERVED_PER_TEA / MAX_RESERVE_PER_CALL);

    for (String teaUid : TEA_UIDS) {
      int collected = 0;
      for (int batch = 0; batch < callsPerTea && collected < RESERVED_PER_TEA; batch++) {
        int need = Math.min(MAX_RESERVE_PER_CALL, RESERVED_PER_TEA - collected);
        String url =
            BASE_URL
                + "/api/trackedEntityAttributes/"
                + teaUid
                + "/generateAndReserve?numberToReserve="
                + need;

        HttpRequest req =
            HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Basic " + auth)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
          throw new RuntimeException(
              "generateAndReserve failed for "
                  + teaUid
                  + ": HTTP "
                  + resp.statusCode()
                  + " — "
                  + resp.body());
        }

        JsonNode arr = mapper.readTree(resp.body());
        for (JsonNode rv : arr) {
          all.add(new String[] {teaUid, rv.get("value").asText()});
          collected++;
        }
      }
      log.info("  {} reserved values for {}", collected, teaUid);
    }
    return all;
  }

  /**
   * Writes ndjson.gz where each line is a TE JSON that imports one TEAV using a reserved value. The
   * reserved values cycle through the 20 TEAs (round-robin).
   */
  private void writeNdjson(Path target, List<String[]> reservedValues) throws IOException {
    try (BufferedWriter w =
        new BufferedWriter(
            new OutputStreamWriter(
                new GZIPOutputStream(new FileOutputStream(target.toFile())),
                StandardCharsets.UTF_8))) {
      for (String[] rv : reservedValues) {
        String teaUid = rv[0];
        String value = rv[1];
        w.write(teJson(teaUid, value));
        w.newLine();
      }
    }
    log.info("Wrote {} lines → {}", reservedValues.size(), target);
  }

  private static String teJson(String teaUid, String value) {
    return """
        {"trackedEntityType":"%s","orgUnit":"%s","attributes":[{"attribute":"%s","value":"%s"}]}"""
        .formatted(PERSON_TET_UID, ORG_UNIT_UID, teaUid, value);
  }

  // ── Scenario builder ─────────────────────────────────────────────────────────

  /**
   * Builds a single-user import scenario that sends {@code totalTes / PER_REQUEST} requests, each
   * carrying {@link #PER_REQUEST} TEs wrapped in a {@code trackedEntities} envelope.
   */
  private ScenarioBuilder buildImportScenario(String name, NdjsonFeeder feeder, int totalTes) {
    int requests = totalTes / PER_REQUEST;
    return scenario(name)
        .exec(session -> session.set("username", ADMIN_USER).set("password", ADMIN_PASSWORD))
        .exec(
            http("Login")
                .post("/api/auth/login")
                .header("Content-Type", "application/json")
                .body(
                    StringBody(
                        "{\"username\":\""
                            + ADMIN_USER
                            + "\",\"password\":\""
                            + ADMIN_PASSWORD
                            + "\"}"))
                .check(status().is(200)))
        .exitHereIfFailed()
        .repeat(requests)
        .on(
            feed(feeder, PER_REQUEST)
                .exec(
                    http(name)
                        .post("/api/tracker?async=false")
                        .header("Content-Type", "application/json")
                        .body(StringBody(session -> wrapPayload(session.getList("payload"))))
                        .check(status().is(200))));
  }

  private static String wrapPayload(List<?> payloads) {
    StringBuilder sb = new StringBuilder(payloads.size() * 256);
    sb.append("{\"trackedEntities\":[");
    for (int i = 0; i < payloads.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append(payloads.get(i));
    }
    sb.append("]}");
    return sb.toString();
  }
}
