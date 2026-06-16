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

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for {@link org.hisp.dhis.reservedvalue.RemoveUsedOrExpiredReservedValuesJob}
 * and the {@code getNumberOfUsedValues} query under DRC-scale load.
 *
 * <h2>Dataset (seeded once via docker exec psql)</h2>
 *
 * <ul>
 *   <li>1M tracked entities, 10M TEAVs:
 *       <ul>
 *         <li>1M sequential TEAVs for PerfTea0001 — values {@code '00000001'} – {@code
 *             '01000000'}; the first 20 000 ({@code '00000001'} – {@code '00020000'}) match
 *             reserved values and are the target of {@code removeUsedValues()}
 *         <li>1M random TEAVs for PerfTea0002 (noise)
 *         <li>8M regular TEAVs across PerfTea0003–PerfTea0010
 *       </ul>
 *   <li>11M reserved values for PerfTea0001 ({@code key=''}):
 *       <ul>
 *         <li>8M expired ({@code expirydate < now()}) — cleared by {@code removeExpiredValues()}
 *         <li>20K valid matching TEAVs — cleared by {@code removeUsedValues()}
 *         <li>2.98M valid non-matching — survive the cleanup run
 *       </ul>
 * </ul>
 *
 * <h2>Scenarios (run sequentially)</h2>
 *
 * <ol>
 *   <li><b>getNumberOfUsedValues (pre-cleanup)</b> — {@code generateAndReserve} call before the
 *       cleanup job; exercises the NOT EXISTS + UNION ALL query with 3M reserved values, 1M TEAVs,
 *       and 20K overlap. Must complete in under {@code getNumberMaxMs} (default 30 s).
 *   <li><b>Reserved value cleanup job</b> — triggers {@code
 *       RemoveUsedOrExpiredReservedValuesJob} via {@code POST /api/jobConfigurations/{uid}/execute}
 *       and polls until completed. Deletes 8M expired + 20K used rows.
 *   <li><b>getNumberOfUsedValues (post-cleanup)</b> — same {@code generateAndReserve} call after
 *       cleanup; validates steady-state performance with no overlap.
 * </ol>
 *
 * <h2>System properties</h2>
 *
 * <ul>
 *   <li>{@code -Dinstance} — DHIS2 base URL (default: {@code http://localhost:8080})
 *   <li>{@code -DdbContainer} — Docker container running PostgreSQL (default: {@code
 *       dhis2-core-db-1})
 *   <li>{@code -DskipSeed} — skip SQL seeding when data is already present (default: false)
 *   <li>{@code -DjobMaxPolls} — max poll attempts for the cleanup job (default: 120)
 *   <li>{@code -DjobPollIntervalS} — seconds between polls (default: 10)
 *   <li>{@code -DgetNumberMaxMs} — max allowed ms for generateAndReserve (default: 30000)
 * </ul>
 */
public class ReservedValuePerformanceTest extends Simulation {

  private static final Logger log = LoggerFactory.getLogger(ReservedValuePerformanceTest.class);

  // ── Configuration ─────────────────────────────────────────────────────────────

  private static final String BASE_URL = System.getProperty("instance", "http://localhost:8080");
  private static final String ADMIN_USER = System.getProperty("adminUser", "admin");
  private static final String ADMIN_PASSWORD = System.getProperty("adminPassword", "district");
  private static final String DB_CONTAINER =
      System.getProperty("dbContainer", "dhis2-core-db-1");
  private static final String DB_NAME = System.getProperty("dbName", "dhis");
  private static final String DB_USER = System.getProperty("dbUser", "dhis");
  private static final boolean SKIP_SEED = Boolean.getBoolean("skipSeed");
  private static final int JOB_MAX_POLLS =
      Integer.parseInt(System.getProperty("jobMaxPolls", "120"));
  private static final int JOB_POLL_INTERVAL_S =
      Integer.parseInt(System.getProperty("jobPollIntervalS", "10"));
  private static final int GET_NUMBER_MAX_MS =
      Integer.parseInt(System.getProperty("getNumberMaxMs", "30000"));

  // ── Metadata UIDs (Sierra Leone demo DB) ──────────────────────────────────────

  private static final String PERSON_TET_UID = "nEenWmSyUEp";
  private static final String ORG_UNIT_UID = "DiszpKrYNg8";

  // ── Perf TEA UIDs ─────────────────────────────────────────────────────────────

  /** Target of the reserved value test — RANDOM(########), no static prefix, key=''. */
  private static final String PERF_TEA_UID = "PerfTea0001";

  private static final List<String> ALL_TEA_UIDS =
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
          "PerfTea0010");

  private static final List<String> PATTERNS =
      List.of(
          "RANDOM(########)", // PerfTea0001 — target; 10^8 space easily covers 11M rows
          "RANDOM(XXX###)", // PerfTea0002 — noise
          "RANDOM(XXXXXX)", // PerfTea0003–0010 — regular bulk
          "RANDOM(XX####)",
          "RANDOM(#####X)",
          "RANDOM(X#####)",
          "RANDOM(##XX##)",
          "RANDOM(####XX)",
          "RANDOM(XXXXX#)",
          "RANDOM(#XXXXX)");

  // ── Request labels ─────────────────────────────────────────────────────────────

  private static final String PRE_CLEANUP_REQUEST = "GET generateAndReserve (pre-cleanup)";
  private static final String POST_CLEANUP_REQUEST = "GET generateAndReserve (post-cleanup)";
  private static final String EXECUTE_JOB_REQUEST = "POST execute cleanup job";
  private static final String POLL_JOB_REQUEST = "GET job status (poll)";

  // ── Constructor ───────────────────────────────────────────────────────────────

  public ReservedValuePerformanceTest() {
    log.info("=== ReservedValuePerformanceTest — setup start ===");

    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      ObjectMapper mapper = new ObjectMapper();
      String auth =
          Base64.getEncoder()
              .encodeToString(
                  (ADMIN_USER + ":" + ADMIN_PASSWORD).getBytes(StandardCharsets.UTF_8));

      log.info("Phase 1/2 — creating {} TEAs with RANDOM patterns...", ALL_TEA_UIDS.size());
      createTeAs(httpClient, auth, mapper);

      if (SKIP_SEED) {
        log.info("Phase 2/2 — SQL seeding skipped (-DskipSeed=true)");
      } else {
        log.info(
            "Phase 2/2 — seeding 1M TEs, 10M TEAVs, 11M reserved values via {}...", DB_CONTAINER);
        seedData();
      }
    } catch (Exception e) {
      throw new RuntimeException("Setup failed", e);
    }

    log.info("=== Setup complete — starting Gatling scenarios ===");

    HttpProtocolBuilder protocol =
        http.baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .disableCaching()
            .basicAuth(ADMIN_USER, ADMIN_PASSWORD);

    ScenarioBuilder preCleanup =
        scenario("getNumberOfUsedValues (pre-cleanup)")
            .exec(
                http(PRE_CLEANUP_REQUEST)
                    .get(
                        "/api/trackedEntityAttributes/"
                            + PERF_TEA_UID
                            + "/generateAndReserve?numberToReserve=1")
                    .check(status().is(200)));

    ScenarioBuilder cleanupJob = buildCleanupJobScenario();

    ScenarioBuilder postCleanup =
        scenario("getNumberOfUsedValues (post-cleanup)")
            .exec(
                http(POST_CLEANUP_REQUEST)
                    .get(
                        "/api/trackedEntityAttributes/"
                            + PERF_TEA_UID
                            + "/generateAndReserve?numberToReserve=1")
                    .check(status().is(200)));

    setUp(
            preCleanup
                .injectOpen(atOnceUsers(1))
                .andThen(
                    cleanupJob
                        .injectOpen(atOnceUsers(1))
                        .andThen(postCleanup.injectOpen(atOnceUsers(1)))))
        .protocols(protocol)
        .assertions(
            details(PRE_CLEANUP_REQUEST).responseTime().max().lt(GET_NUMBER_MAX_MS),
            details(POST_CLEANUP_REQUEST).responseTime().max().lt(GET_NUMBER_MAX_MS),
            forAll().successfulRequests().percent().gte(100d));
  }

  // ── Phase 1: create TEAs ──────────────────────────────────────────────────────

  private void createTeAs(HttpClient httpClient, String auth, ObjectMapper mapper)
      throws Exception {
    StringBuilder attrs = new StringBuilder();
    for (int i = 0; i < ALL_TEA_UIDS.size(); i++) {
      if (i > 0) attrs.append(",\n");
      attrs.append(teaJson(ALL_TEA_UIDS.get(i), i + 1, PATTERNS.get(i)));
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
          "name": "Perf Reserved Attr %d",
          "shortName": "PRA%02d",
          "code": "PERF_RSV_%02d",
          "valueType": "TEXT",
          "aggregationType": "NONE",
          "generated": true,
          "pattern": "%s"
        }"""
        .formatted(uid, index, index, index, pattern);
  }

  // ── Phase 2: seed data ────────────────────────────────────────────────────────

  /**
   * Inserts via {@code docker exec psql}. Idempotent: skips if {@code PerfRv00001} already exists
   * in the {@code trackedentity} table.
   *
   * <p>Reserved value layout for PerfTea0001 ({@code key=''}):
   *
   * <pre>
   *   '00000001' – '00020000'  (20 000 rows)  valid, match first 20K TEAVs → removeUsedValues target
   *   '01000001' – '03980000'  (2 980 000)    valid, no matching TEAV
   *   '03980001' – '11980000'  (8 000 000)    expired                       → removeExpiredValues target
   *   ──────────────────────────────────────────────────────────────────────────────────────────────
   *   11 000 000 total
   * </pre>
   */
  private void seedData() throws Exception {
    String teaUidList = "'" + String.join("','", ALL_TEA_UIDS) + "'";

    String sql =
        """
        DO $$
        DECLARE
            v_tet_id   INTEGER;
            v_ou_id    INTEGER;
            v_tea_ids  INTEGER[];
            v_tea1_id  INTEGER;
            v_base_id  BIGINT;
            i          INTEGER;
        BEGIN
            IF EXISTS (SELECT 1 FROM trackedentity WHERE uid = 'Te000000001') THEN
                RAISE NOTICE 'Perf data already present — skipping seed.';
                RETURN;
            END IF;

            SELECT trackedentitytypeid INTO v_tet_id FROM trackedentitytype  WHERE uid = '%s';
            SELECT organisationunitid  INTO v_ou_id  FROM organisationunit   WHERE uid = '%s';

            IF v_tet_id IS NULL THEN
                RAISE EXCEPTION 'TrackedEntityType %s not found — is the Sierra Leone demo DB loaded?';
            END IF;
            IF v_ou_id IS NULL THEN
                RAISE EXCEPTION 'OrgUnit %s not found.';
            END IF;

            v_tea_ids := ARRAY(
                SELECT trackedentityattributeid
                FROM   trackedentityattribute
                WHERE  uid IN (%s)
                ORDER BY uid
            );
            v_tea1_id := v_tea_ids[1];

            IF array_length(v_tea_ids, 1) < 10 THEN
                RAISE EXCEPTION 'Expected 10 perf TEAs, found %%. Run Phase 1 first.',
                    COALESCE(array_length(v_tea_ids, 1), 0);
            END IF;

            SELECT COALESCE(MAX(trackedentityid), 0) INTO v_base_id FROM trackedentity;

            -- 1M tracked entities
            RAISE NOTICE 'Inserting 1M tracked entities (base_id = %%)...', v_base_id;
            INSERT INTO trackedentity
                (trackedentityid, uid, created, lastupdated,
                 trackedentitytypeid, organisationunitid, inactive, deleted)
            SELECT
                v_base_id + gs,
                'Te' || lpad(gs::text, 9, '0'),
                NOW() - ((gs %% 365) || ' days')::interval,
                NOW() - ((gs %% 30)  || ' days')::interval,
                v_tet_id, v_ou_id, false, false
            FROM generate_series(1, 1000000) gs;

            -- Advance the tracked entity PK sequence so DHIS2 doesn't collide
            PERFORM setval(
                pg_get_serial_sequence('trackedentity', 'trackedentityid'),
                (SELECT MAX(trackedentityid) FROM trackedentity));

            -- 1M sequential TEAVs for PerfTea0001
            -- Values '00000001' – '00020000' intentionally match the 20K valid reserved values
            RAISE NOTICE 'Inserting 1M sequential TEAVs for PerfTea0001...';
            INSERT INTO trackedentityattributevalue
                (trackedentityid, trackedentityattributeid, created, lastupdated, value)
            SELECT
                v_base_id + gs,
                v_tea1_id,
                NOW(), NOW(),
                lpad(gs::text, 8, '0')
            FROM generate_series(1, 1000000) gs;

            -- 1M random TEAVs for PerfTea0002 (noise)
            RAISE NOTICE 'Inserting 1M random TEAVs for PerfTea0002...';
            INSERT INTO trackedentityattributevalue
                (trackedentityid, trackedentityattributeid, created, lastupdated, value)
            SELECT
                v_base_id + gs,
                v_tea_ids[2],
                NOW(), NOW(),
                upper(substring(md5(gs::text), 1, 6))
            FROM generate_series(1, 1000000) gs;

            -- 8M regular TEAVs across PerfTea0003–PerfTea0010
            FOR i IN 3..10 LOOP
                RAISE NOTICE 'Inserting 1M regular TEAVs for TEA index %%/10...', i;
                INSERT INTO trackedentityattributevalue
                    (trackedentityid, trackedentityattributeid, created, lastupdated, value)
                SELECT
                    v_base_id + gs,
                    v_tea_ids[i],
                    NOW(), NOW(),
                    'VAL' || lpad(i::text, 2, '0') || lpad(gs::text, 6, '0')
                FROM generate_series(1, 1000000) gs;
            END LOOP;

            -- 11M reserved values for PerfTea0001 ─────────────────────────────────

            -- 8M expired (cleared by removeExpiredValues)
            RAISE NOTICE 'Inserting 8M expired reserved values...';
            INSERT INTO reservedvalue
                (created, expirydate, key, ownerobject, owneruid, value, trackedentityattributeid)
            SELECT
                NOW(),
                NOW() - INTERVAL '1 day',
                '',
                'TRACKEDENTITYATTRIBUTE',
                'PerfTea0001',
                lpad((3980000 + gs)::text, 8, '0'),
                v_tea1_id
            FROM generate_series(1, 8000000) gs;

            -- 20K valid matching TEAVs (cleared by removeUsedValues)
            RAISE NOTICE 'Inserting 20K matching valid reserved values...';
            INSERT INTO reservedvalue
                (created, expirydate, key, ownerobject, owneruid, value, trackedentityattributeid)
            SELECT
                NOW(),
                NOW() + INTERVAL '7 days',
                '',
                'TRACKEDENTITYATTRIBUTE',
                'PerfTea0001',
                lpad(gs::text, 8, '0'),
                v_tea1_id
            FROM generate_series(1, 20000) gs;

            -- 2.98M valid non-matching (survive the cleanup run)
            RAISE NOTICE 'Inserting 2.98M non-matching valid reserved values...';
            INSERT INTO reservedvalue
                (created, expirydate, key, ownerobject, owneruid, value, trackedentityattributeid)
            SELECT
                NOW(),
                NOW() + INTERVAL '7 days',
                '',
                'TRACKEDENTITYATTRIBUTE',
                'PerfTea0001',
                lpad((1000000 + gs)::text, 8, '0'),
                v_tea1_id
            FROM generate_series(1, 2980000) gs;

            RAISE NOTICE 'Seeding complete: 1M TEs, 10M TEAVs, 11M reserved values.';
        END $$;
        """
            .formatted(
                PERSON_TET_UID,
                ORG_UNIT_UID,
                PERSON_TET_UID,
                ORG_UNIT_UID,
                teaUidList);

    runSql(sql, 1800);
  }

  private void runSql(String sql, int timeoutSeconds) throws Exception {
    ProcessBuilder pb =
        new ProcessBuilder(
            "docker", "exec", "-i", DB_CONTAINER,
            "psql", "-U", DB_USER, "-d", DB_NAME, "-v", "ON_ERROR_STOP=1");
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
    for (String line : output.split("\n")) {
      if (line.startsWith("NOTICE")) log.info("  DB: {}", line);
    }
  }

  // ── Cleanup job scenario ──────────────────────────────────────────────────────

  /**
   * Finds the job configuration UID for {@code REMOVE_USED_OR_EXPIRED_RESERVED_VALUES}, captures
   * the current {@code lastExecuted} timestamp as a baseline, triggers an immediate run, then polls
   * every {@link #JOB_POLL_INTERVAL_S} seconds until {@code lastExecuted} advances past the
   * baseline and {@code lastExecutedStatus} is no longer {@code RUNNING}.
   */
  private ScenarioBuilder buildCleanupJobScenario() {
    return scenario("Reserved value cleanup job")
        // 1. Resolve the job configuration UID at runtime
        .exec(
            http("GET job config UID")
                .get(
                    "/api/jobConfigurations"
                        + "?jobType=REMOVE_USED_OR_EXPIRED_RESERVED_VALUES&fields=id")
                .check(status().is(200))
                .check(jsonPath("$.jobConfigurations[0].id").saveAs("jobConfigUid")))

        // 2. Capture the baseline lastExecuted timestamp
        .exec(
            http("GET job config (baseline)")
                .get("/api/jobConfigurations/#{jobConfigUid}?fields=lastExecuted")
                .check(status().is(200))
                .check(jsonPath("$.lastExecuted").optional().saveAs("baselineExecuted")))

        // 3. Trigger the job
        .exec(
            http(EXECUTE_JOB_REQUEST)
                .post("/api/jobConfigurations/#{jobConfigUid}/execute")
                .check(status().is(200)))
        .exec(session -> session.set("jobDone", false).set("pollCount", 0))

        // 4. Poll until a new completion is detected or max polls exceeded
        .asLongAs(
            session ->
                !Boolean.TRUE.equals(session.get("jobDone"))
                    && session.getInt("pollCount") < JOB_MAX_POLLS)
        .on(
            exec(
                    http(POLL_JOB_REQUEST)
                        .get(
                            "/api/jobConfigurations/#{jobConfigUid}"
                                + "?fields=lastExecuted,lastExecutedStatus")
                        .check(status().is(200))
                        .check(jsonPath("$.lastExecuted").optional().saveAs("lastExecuted"))
                        .check(
                            jsonPath("$.lastExecutedStatus").optional().saveAs("lastExecutedStatus")))
                .exec(
                    session -> {
                      String baseline = session.getString("baselineExecuted");
                      String current = session.getString("lastExecuted");
                      String jobStatus = session.getString("lastExecutedStatus");
                      int count = session.getInt("pollCount") + 1;

                      // A new completion is detected when lastExecuted advanced past the baseline
                      // and the status is no longer RUNNING (i.e. COMPLETED or FAILED).
                      boolean advanced = current != null && !current.equals(baseline);
                      boolean settled = !"RUNNING".equals(jobStatus);
                      boolean done = advanced && settled;

                      if (done) {
                        log.info(
                            "Cleanup job finished after {} poll(s), status={}.", count, jobStatus);
                      } else if (count >= JOB_MAX_POLLS) {
                        log.error(
                            "Cleanup job did not complete within {} polls ({} s).",
                            JOB_MAX_POLLS,
                            (long) JOB_MAX_POLLS * JOB_POLL_INTERVAL_S);
                      }

                      return session.set("jobDone", done).set("pollCount", count);
                    })
                .doIf(session -> !Boolean.TRUE.equals(session.get("jobDone")))
                .then(pause(Duration.ofSeconds(JOB_POLL_INTERVAL_S))));
  }
}
