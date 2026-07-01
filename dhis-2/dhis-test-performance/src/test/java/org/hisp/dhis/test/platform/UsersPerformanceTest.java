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

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Performance test for single-user CRUD operations on {@code /api/users}.
 *
 * <p>Seven scenarios, all running against the platform-perf DB (~250k users / ~250k org units) by
 * default:
 *
 * <ol>
 *   <li><b>POST</b> — creates a new user (with optional group assignment)
 *   <li><b>GET</b> — fetches a single existing user by UID
 *   <li><b>PATCH</b> — partial update via RFC 6902 JSON Patch on an existing user
 *   <li><b>REPLICA</b> — replicates an existing user via {@code POST /api/users/{uid}/replica}
 *   <li><b>PUT</b> — full-replace of an existing user
 *   <li><b>PATCH userGroups</b> — updates group assignment via the user-side PATCH path
 *   <li><b>DELETE</b> — deletes existing users
 * </ol>
 *
 * <p>Except for POST (which creates new users), every scenario operates on <i>existing</i> perf-DB
 * users — chosen precisely because they carry heavy userGroup/userRole relationships, so the
 * measured ops exercise the associated N+1 paths. {@code before()} fetches a pool of existing users
 * and partitions it into disjoint, consume-once slices — one per scenario — so each user is
 * operated on at most once. This keeps every measured op against a still-heavy user (PUT and
 * PATCH-userGroups shrink the user on first write) and guarantees scenarios never interfere; in
 * sequential mode they run least → most destructive with DELETE last.
 *
 * <p>Because DELETE/PUT mutate real users with no DB reset between runs, run with {@code WARMUP=0}
 * (the concurrency ramp already warms the server) — a destructive warmup would consume users the
 * measured run then expects to find. The configured admin {@code username} is excluded from the
 * pool so the test never mutates the account it authenticates with.
 *
 * <p>Run with {@code -DuserGroupUid=<uid>} pointing at a group with large membership to expose N+1
 * problems in POST. The default points at a ~83k-member group on the platform-perf DB.
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
 * <p>Available properties (with platform-perf DB defaults):
 *
 * <ul>
 *   <li>{@code configFile} — path to a {@code .properties} file (optional)
 *   <li>{@code baseUrl} (default: {@code http://localhost:8080})
 *   <li>{@code username} (default: {@code admin})
 *   <li>{@code password} (default: {@code district})
 *   <li>{@code userRoleUid} (default: {@code MoRvPzDH7lc} — generic role with ~83k users)
 *   <li>{@code orgUnitUid} (default: {@code VCCdfC9pvMA} — root org unit)
 *   <li>{@code userGroupUid} (default: {@code KOvR9SAEeEZ} — group with ~83k members)
 *   <li>{@code concurrency} (default: {@code 10}) — peak concurrent virtual users per scenario
 *   <li>{@code rampSeconds} (default: {@code 30}) — seconds to ramp concurrency from 1 to peak
 *   <li>{@code sustainSeconds} (default: {@code 120}) — seconds to hold peak concurrency
 *   <li>{@code paceMillis} (default: {@code 1000}) — minimum interval between a virtual user's
 *       requests; controls cadence (≈ concurrency ÷ paceMillis req/s per scenario). Set {@code 0}
 *       to fire as fast as the server answers
 *   <li>{@code userPoolSize} (default: {@code 30000}) — existing users fetched and split across the
 *       six existing-user scenarios; raise it if a scenario reports "slice exhausted"
 *   <li>{@code mode} (default: {@code sequential}; runs each scenario in isolation so timings
 *       reflect single-endpoint latency. Use {@code parallel} to run all scenarios concurrently as
 *       a mixed-load stress test.)
 * </ul>
 */
public class UsersPerformanceTest extends Simulation {

  /**
   * Loads a {@code .properties} file from the path given by {@code -DconfigFile=...}, or returns an
   * empty {@code Properties} if no config file was specified.
   */
  private static final Properties CONFIG = loadConfig();

  // Consider to extract to a helper function if other tests want to follow this pattern
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
  private static final String BASIC_AUTH =
      Base64.getEncoder()
          .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
  private static final String USER_ROLE_UID = prop("userRoleUid", "MoRvPzDH7lc");
  private static final String ORG_UNIT_UID = prop("orgUnitUid", "VCCdfC9pvMA");
  private static final String USER_GROUP_UID = prop("userGroupUid", "KOvR9SAEeEZ");
  // Default to sequential so each scenario is measured in isolation (single-endpoint latency).
  // Set -Dmode=parallel for a mixed-load stress test where all scenarios run concurrently.
  private static final String MODE = prop("mode", "sequential");

  // Closed-model load profile: ramp concurrency from 1 to CONCURRENCY over RAMP_SECONDS, then hold
  // CONCURRENCY for SUSTAIN_SECONDS. In sequential mode each scenario runs this full profile in its
  // own phase; in parallel mode all scenarios run it concurrently.
  private static final int CONCURRENCY = Integer.parseInt(prop("concurrency", "10"));
  private static final int RAMP_SECONDS = Integer.parseInt(prop("rampSeconds", "30"));
  private static final int SUSTAIN_SECONDS = Integer.parseInt(prop("sustainSeconds", "120"));

  // Each virtual user loops for this long (the full ramp + sustain window) then exits. Using a
  // time-bounded loop rather than forever() guarantees the closed-injection population drains when
  // the window ends — forever() loopers are not terminated by the injection profile and would run
  // indefinitely. Users injected during the ramp finish up to RAMP_SECONDS after the window, a
  // bounded overshoot.
  private static final int TEST_DURATION_SECONDS = RAMP_SECONDS + SUSTAIN_SECONDS;

  // Existing perf-DB users to fetch and partition across the existing-user scenarios. Must be large
  // enough that no scenario's disjoint slice is exhausted within the sustained window (see
  // before()). With pacing on (paceMillis > 0) each scenario consumes ~(concurrency × window /
  // paceMillis) users — e.g. 10 × 150s / 1s = 1500.
  private static final int USER_POOL_SIZE = Integer.parseInt(prop("userPoolSize", "30000"));

  // Minimum interval between consecutive requests from a single virtual user. The test paces each
  // user to at most one request per this period, so request cadence is controlled rather than
  // open-throttle: at the default 1000ms with CONCURRENCY=10 each scenario runs at ~10 req/s. A
  // request that itself takes longer than the pace simply proceeds at its natural rate (pace never
  // speeds requests up, only spaces fast ones out). Set paceMillis=0 to disable pacing.
  private static final int PACE_MILLIS = Integer.parseInt(prop("paceMillis", "1000"));

  private enum Profile {
    SMOKE,
    LOAD;

    static Profile fromString(String s) {
      return "smoke".equalsIgnoreCase(s) ? SMOKE : LOAD;
    }
  }

  private static final Profile PROFILE = Profile.fromString(prop("profile", "load"));

  private record Thresholds(int p95, int max) {}

  // Request names — used in both scenario definitions and assertions
  private static final String POST_REQUEST = "POST User - create";
  private static final String GET_REQUEST = "GET User - by uid";
  private static final String PUT_REQUEST = "PUT User - full update";
  private static final String PATCH_REQUEST = "PATCH User - partial update";
  private static final String PATCH_GROUPS_REQUEST = "PATCH User - replace userGroups";
  private static final String REPLICA_REQUEST = "POST User - replicate";
  private static final String DELETE_REQUEST = "DELETE User - delete";
  private static final int PATCH_GROUP_COUNT = Integer.parseInt(prop("patchGroupCount", "7"));

  // Thresholds per profile, (p95, max) in ms. Recalibrated 2026-06-22 from fresh sequential
  // baselines analysed over 2026-06-16–06-22 (LOAD nightly × 10 iter, SMOKE nightly × 3 iter).
  //
  // The test runs scenarios sequentially (single-endpoint latency in isolation) and authenticates
  // once per virtual user, keeping the one-time session-establishing bcrypt out of the measured
  // requests. This is far cheaper than the old parallel regime, where CPU contention on the shared
  // CI runner inflated the tail (GET p95 reached 783ms vs ~81ms in isolation) — hence the much
  // tighter values below (e.g. GET LOAD dropped from 700/1050 to 50/150).
  // Recalibrate with: scripts/download-user-perf-results.sh --test-name users-smoke
  //                   scripts/analyze-user-perf-results.py --profile smoke
  private static final Map<Profile, Thresholds> POST_THRESH =
      Map.of(Profile.SMOKE, new Thresholds(200, 200), Profile.LOAD, new Thresholds(200, 250));

  private static final Map<Profile, Thresholds> GET_THRESH =
      Map.of(Profile.SMOKE, new Thresholds(50, 60), Profile.LOAD, new Thresholds(50, 150));

  private static final Map<Profile, Thresholds> PUT_THRESH =
      Map.of(Profile.SMOKE, new Thresholds(300, 300), Profile.LOAD, new Thresholds(300, 350));

  private static final Map<Profile, Thresholds> PATCH_THRESH =
      Map.of(Profile.SMOKE, new Thresholds(50, 60), Profile.LOAD, new Thresholds(50, 100));

  private static final Map<Profile, Thresholds> PATCH_GROUPS_THRESH =
      Map.of(Profile.SMOKE, new Thresholds(150, 150), Profile.LOAD, new Thresholds(150, 150));

  private static final Map<Profile, Thresholds> REPLICA_THRESH =
      Map.of(Profile.SMOKE, new Thresholds(150, 160), Profile.LOAD, new Thresholds(150, 300));

  private static final Map<Profile, Thresholds> DELETE_THRESH =
      Map.of(Profile.SMOKE, new Thresholds(250, 260), Profile.LOAD, new Thresholds(250, 280));

  // Timestamp-based offset so each run generates unique usernames for the POST/REPLICA scenarios,
  // the only scenarios that create new users.
  private static final int RUN_OFFSET = (int) (System.currentTimeMillis() % 10_000_000);
  private static final AtomicInteger POST_COUNTER = new AtomicInteger(RUN_OFFSET);
  private static final AtomicInteger REPLICA_COUNTER = new AtomicInteger(RUN_OFFSET + 1_000_000);

  // Rotates which user groups a PATCH-userGroups request assigns, for variety across requests.
  private static final AtomicInteger PATCH_GROUPS_ROTATION = new AtomicInteger();

  /**
   * Existing perf-DB users (heavy userGroup/userRole relationships) fetched in {@link #before()},
   * stored as [uid, username] pairs. The list is partitioned into disjoint per-scenario slices so
   * each user is operated on at most once across the whole run — keeping every measured op against
   * a still-heavy user, and ensuring scenarios never interfere (e.g. DELETE can't remove a user a
   * read/write scenario will later touch).
   */
  private static final List<String[]> EXISTING_USERS = new ArrayList<>();

  private static final List<String> PATCH_GROUP_UIDS = new ArrayList<>();

  // One disjoint, consume-once slice of EXISTING_USERS per scenario that operates on existing
  // users.
  // Slice bounds are assigned in before() once the fetched pool size is known.
  private static final UserSlice GET_USERS = new UserSlice();
  private static final UserSlice PATCH_USERS = new UserSlice();
  private static final UserSlice REPLICA_USERS = new UserSlice();
  private static final UserSlice PUT_USERS = new UserSlice();
  private static final UserSlice PATCH_GROUPS_USERS = new UserSlice();
  private static final UserSlice DELETE_USERS = new UserSlice();

  /**
   * A cursor over a contiguous, disjoint slice of {@link #EXISTING_USERS} that hands out each user
   * exactly once. Returns {@code null} once the slice is exhausted, so the scenario fails loudly
   * rather than silently reusing (and thus mutating twice) a user.
   */
  private static final class UserSlice {
    private final AtomicInteger cursor = new AtomicInteger();
    private int end;

    void init(int start, int end) {
      this.end = end;
      cursor.set(start);
    }

    String[] next() {
      int i = cursor.getAndIncrement();
      return i < end ? EXISTING_USERS.get(i) : null;
    }
  }

  private static String patchUserGroupsBody(int startIndex) {
    int count = Math.min(PATCH_GROUP_COUNT, PATCH_GROUP_UIDS.size());
    StringBuilder groups = new StringBuilder("[");
    for (int i = 0; i < count; i++) {
      if (i > 0) {
        groups.append(',');
      }
      String uid = PATCH_GROUP_UIDS.get((startIndex + i) % PATCH_GROUP_UIDS.size());
      groups.append("{\"id\":\"").append(uid).append("\"}");
    }
    groups.append(']');

    return """
        [{"op":"add","path":"/userGroups","value":%s},{"op":"add","path":"/attributeValues","value":[]}]\
        """
        .formatted(groups);
  }

  private static void fetchUserGroupUids(
      HttpClient client, ObjectMapper mapper, String auth, int needed) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(BASE_URL + "/api/userGroups?fields=id&pageSize=" + needed))
              .header("Authorization", "Basic " + auth)
              .header("Accept", "application/json")
              .GET()
              .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        System.err.println("Fetching user groups failed (HTTP " + response.statusCode() + ")");
        return;
      }

      JsonNode groups = mapper.readTree(response.body()).path("userGroups");
      for (JsonNode group : groups) {
        String uid = group.path("id").asText();
        if (!uid.isBlank()) {
          PATCH_GROUP_UIDS.add(uid);
        }
      }
    } catch (Exception e) {
      System.err.println("Fetching user groups failed: " + e.getMessage());
    }
  }

  /** Builds a minimal valid user JSON body for POST/PUT. Pass {@code null} for id on creation. */
  private static String userBody(String id, String username, String firstName) {
    String idField = id != null ? "\"id\":\"%s\",".formatted(id) : "";
    String groupsField =
        USER_GROUP_UID.isBlank()
            ? ""
            : """
                ,"userGroups":[{"id":"%s"}]\
                """
                .formatted(USER_GROUP_UID);
    return """
        {%s"username":"%s","firstName":"%s","surname":"Test","password":"Test1234@",\
        "userRoles":[{"id":"%s"}],"organisationUnits":[{"id":"%s"}],\
        "dataViewOrganisationUnits":[{"id":"%s"}]%s}\
        """
        .formatted(
            idField, username, firstName, USER_ROLE_UID, ORG_UNIT_UID, ORG_UNIT_UID, groupsField);
  }

  /**
   * Fetches up to {@code needed} existing users (id + username) from the perf DB into {@link
   * #EXISTING_USERS}, paging as required. The configured admin {@link #USERNAME} is skipped so the
   * test never mutates or deletes the account it authenticates with.
   */
  private static void fetchExistingUsers(
      HttpClient client, ObjectMapper mapper, String auth, int needed) {
    int pageSize = Math.min(needed, 10_000);
    int page = 1;
    while (EXISTING_USERS.size() < needed) {
      try {
        HttpRequest request =
            HttpRequest.newBuilder()
                .uri(
                    URI.create(
                        BASE_URL
                            + "/api/users?fields=id,username&pageSize="
                            + pageSize
                            + "&page="
                            + page))
                .header("Authorization", "Basic " + auth)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
          System.err.println("Fetching existing users failed (HTTP " + response.statusCode() + ")");
          return;
        }
        JsonNode users = mapper.readTree(response.body()).path("users");
        if (!users.elements().hasNext()) {
          return; // no more pages
        }
        for (JsonNode user : users) {
          String uid = user.path("id").asText();
          String username = user.path("username").asText();
          if (!uid.isBlank() && !username.isBlank() && !username.equals(USERNAME)) {
            EXISTING_USERS.add(new String[] {uid, username});
            if (EXISTING_USERS.size() >= needed) {
              return;
            }
          }
        }
        page++;
      } catch (Exception e) {
        System.err.println("Fetching existing users failed: " + e.getMessage());
        return;
      }
    }
  }

  // Scenarios that operate on existing users, each paired with the slice it consumes. The order
  // also drives the sequential run order (least → most destructive), so DELETE runs last.
  private static final List<UserSlice> EXISTING_USER_SLICES =
      List.of(GET_USERS, PATCH_USERS, REPLICA_USERS, PUT_USERS, PATCH_GROUPS_USERS, DELETE_USERS);

  /**
   * Fetches a pool of existing perf-DB users and partitions it into equal, disjoint slices — one
   * per existing-user scenario. Each slice is consumed once (never cycled), so every measured op
   * runs against a still-heavy user and no two scenarios ever touch the same user. POST is the only
   * scenario that creates users, so it draws from no slice.
   */
  @Override
  public void before() {
    System.out.println(
        "Fetching up to %d existing users to partition across %d scenarios..."
            .formatted(USER_POOL_SIZE, EXISTING_USER_SLICES.size()));

    String auth =
        Base64.getEncoder()
            .encodeToString((USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));
    HttpClient client = HttpClient.newHttpClient();
    ObjectMapper mapper = new ObjectMapper();

    fetchExistingUsers(client, mapper, auth, USER_POOL_SIZE);

    int sliceCount = EXISTING_USER_SLICES.size();
    int sliceSize = EXISTING_USERS.size() / sliceCount;
    if (sliceSize == 0) {
      throw new IllegalStateException(
          "Fetched only %d existing users — need at least %d (one per scenario). Check baseUrl/auth."
              .formatted(EXISTING_USERS.size(), sliceCount));
    }
    for (int i = 0; i < sliceCount; i++) {
      EXISTING_USER_SLICES.get(i).init(i * sliceSize, (i + 1) * sliceSize);
    }

    fetchUserGroupUids(client, mapper, auth, PATCH_GROUP_COUNT * 4);
    if (PATCH_GROUP_UIDS.isEmpty() && !USER_GROUP_UID.isBlank()) {
      PATCH_GROUP_UIDS.add(USER_GROUP_UID);
    }
    if (PATCH_GROUP_UIDS.isEmpty()) {
      throw new IllegalStateException(
          "Could not fetch any user groups for PATCH /api/users scenario");
    }

    System.out.println(
        "Fetched %d existing users → %d per scenario slice; loaded %d user groups."
            .formatted(EXISTING_USERS.size(), sliceSize, PATCH_GROUP_UIDS.size()));
  }

  public UsersPerformanceTest() {
    // No protocol-level basicAuth. DHIS2 is stateful (SessionCreationPolicy.IF_REQUIRED +
    // HttpSessionSecurityContextRepository), so once a session exists Spring Security reuses the
    // SecurityContext and skips re-authentication; bcrypt password verification (~70ms) is only
    // paid on the FIRST request that establishes the session. With protocol basicAuth + the default
    // cookie jar, that first-request bcrypt landed inside the measured GET/PUT/... requests and
    // surfaced in their p95 (a fixed ~80ms auth artifact, not endpoint latency). Instead we
    // authenticate once per virtual user via a separately-named request (see `authenticate`) so the
    // measured requests reflect endpoint cost only.
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(BASE_URL).acceptHeader("application/json").disableCaching();

    // Authenticate once per virtual user (paying the one-time bcrypt to establish the session),
    // then
    // reuse the JSESSIONID cookie for all measured requests. The login request is named separately
    // so its bcrypt cost is NOT counted in the per-endpoint assertions. Relies on CSRF being
    // disabled (the DHIS2 default) so session-cookie writes (POST/PUT/PATCH/DELETE) are accepted
    // without a CSRF token.
    ChainBuilder authenticate =
        exec(flushCookieJar())
            .exec(
                http("Authenticate (session login)")
                    .get("/api/me")
                    .header("Authorization", "Basic " + BASIC_AUTH)
                    .check(status().is(200)));

    // Paces each virtual user to at most one request per PACE_MILLIS. Prepended to every per-user
    // loop body so the test controls request cadence (≈ CONCURRENCY/PACE_MILLIS req/s per scenario)
    // rather than firing as fast as the server can answer. Safe to share across scenarios: the pace
    // marker lives in each virtual user's own session. paceMillis=0 disables it (no-op chain).
    ChainBuilder pacing =
        PACE_MILLIS > 0 ? pace(Duration.ofMillis(PACE_MILLIS)) : exec(session -> session);

    // ── Scenario: POST /api/users ────────────────────────────────────────────
    ScenarioBuilder postScenario =
        scenario(POST_REQUEST)
            .exec(authenticate)
            .during(Duration.ofSeconds(TEST_DURATION_SECONDS))
            .on(
                pacing
                    .exec(
                        session -> {
                          int num = POST_COUNTER.getAndIncrement();
                          String username = "perftest_post_" + String.format("%07d", num);
                          return session.set("postBody", userBody(null, username, "Post"));
                        })
                    .exec(
                        http(POST_REQUEST)
                            .post("/api/users")
                            .header("Content-Type", "application/json")
                            .body(StringBody("#{postBody}"))
                            .check(status().in(200, 201))));

    // ── Scenario: GET /api/users/{uid} ──────────────────────────────────────
    ScenarioBuilder getScenario =
        scenario(GET_REQUEST)
            .exec(authenticate)
            .during(Duration.ofSeconds(TEST_DURATION_SECONDS))
            .on(
                pacing
                    .exec(
                        session -> {
                          String[] user = GET_USERS.next();
                          if (user == null) {
                            System.err.println("GET user slice exhausted — increase userPoolSize");
                            return session.markAsFailed();
                          }
                          // markAsSucceeded clears any KO left on the session by the previous
                          // iteration's request, so exitHereIfFailed only fires on slice exhaustion
                          // (above), not on a prior failed response.
                          return session.markAsSucceeded().set("getUid", user[0]);
                        })
                    .exitHereIfFailed()
                    .exec(http(GET_REQUEST).get("/api/users/#{getUid}").check(status().is(200))));

    // ── Scenario: PUT /api/users/{uid} ──────────────────────────────────────
    ScenarioBuilder putScenario =
        scenario(PUT_REQUEST)
            .exec(authenticate)
            .during(Duration.ofSeconds(TEST_DURATION_SECONDS))
            .on(
                pacing
                    .exec(
                        session -> {
                          String[] user = PUT_USERS.next();
                          if (user == null) {
                            System.err.println("PUT user slice exhausted — increase userPoolSize");
                            return session.markAsFailed();
                          }
                          return session
                              .markAsSucceeded()
                              .set("putUid", user[0])
                              .set("putBody", userBody(user[0], user[1], "PutUpdated"));
                        })
                    .exitHereIfFailed()
                    .exec(
                        http(PUT_REQUEST)
                            .put("/api/users/#{putUid}")
                            .header("Content-Type", "application/json")
                            .body(StringBody("#{putBody}"))
                            .check(status().is(200))));

    // ── Scenario: PATCH /api/users/{uid} ────────────────────────────────────
    ScenarioBuilder patchScenario =
        scenario(PATCH_REQUEST)
            .exec(authenticate)
            .during(Duration.ofSeconds(TEST_DURATION_SECONDS))
            .on(
                pacing
                    .exec(
                        session -> {
                          String[] user = PATCH_USERS.next();
                          if (user == null) {
                            System.err.println(
                                "PATCH user slice exhausted — increase userPoolSize");
                            return session.markAsFailed();
                          }
                          return session.markAsSucceeded().set("patchUid", user[0]);
                        })
                    .exitHereIfFailed()
                    .exec(
                        http(PATCH_REQUEST)
                            .patch("/api/users/#{patchUid}")
                            .header("Content-Type", "application/json-patch+json")
                            .body(
                                StringBody(
                                    """
                                    [{"op":"replace","path":"/firstName","value":"PerfPatched"}]\
                                    """))
                            .check(status().is(200))));

    // ── Scenario: PATCH /api/users/{uid} userGroups ────────────────────────
    ScenarioBuilder patchGroupsScenario =
        scenario(PATCH_GROUPS_REQUEST)
            .exec(authenticate)
            .during(Duration.ofSeconds(TEST_DURATION_SECONDS))
            .on(
                pacing
                    .exec(
                        session -> {
                          String[] user = PATCH_GROUPS_USERS.next();
                          if (user == null) {
                            System.err.println(
                                "PATCH userGroups slice exhausted — increase userPoolSize");
                            return session.markAsFailed();
                          }
                          return session
                              .markAsSucceeded()
                              .set("patchUserUid", user[0])
                              .set(
                                  "patchGroupsBody",
                                  patchUserGroupsBody(PATCH_GROUPS_ROTATION.getAndIncrement()));
                        })
                    .exitHereIfFailed()
                    .exec(
                        http(PATCH_GROUPS_REQUEST)
                            .patch("/api/users/#{patchUserUid}")
                            .header("Content-Type", "application/json-patch+json")
                            .body(StringBody("#{patchGroupsBody}"))
                            .check(status().is(200))));

    // ── Scenario: POST /api/users/{uid}/replica ─────────────────────────────
    ScenarioBuilder replicaScenario =
        scenario(REPLICA_REQUEST)
            .exec(authenticate)
            .during(Duration.ofSeconds(TEST_DURATION_SECONDS))
            .on(
                pacing
                    .exec(
                        session -> {
                          String[] user = REPLICA_USERS.next();
                          if (user == null) {
                            System.err.println(
                                "REPLICA source slice exhausted — increase userPoolSize");
                            return session.markAsFailed();
                          }
                          int num = REPLICA_COUNTER.getAndIncrement();
                          String replicaUsername = "perftest_rep_" + String.format("%07d", num);
                          return session
                              .markAsSucceeded()
                              .set("replicaSourceUid", user[0])
                              .set(
                                  "replicaBody",
                                  "{\"username\":\"%s\",\"password\":\"Test1234@\"}"
                                      .formatted(replicaUsername));
                        })
                    .exitHereIfFailed()
                    .exec(
                        http(REPLICA_REQUEST)
                            .post("/api/users/#{replicaSourceUid}/replica")
                            .header("Content-Type", "application/json")
                            .body(StringBody("#{replicaBody}"))
                            .check(status().is(201))));

    // ── Scenario: DELETE /api/users/{uid} ───────────────────────────────────
    // Deletes existing perf-DB users (one per request, never reused). Runs last so no other
    // scenario operates on a user this one has deleted.
    ScenarioBuilder deleteScenario =
        scenario(DELETE_REQUEST)
            .exec(authenticate)
            .during(Duration.ofSeconds(TEST_DURATION_SECONDS))
            .on(
                pacing
                    .exec(
                        session -> {
                          String[] user = DELETE_USERS.next();
                          if (user == null) {
                            System.err.println(
                                "DELETE user slice exhausted — increase userPoolSize");
                            return session.markAsFailed();
                          }
                          return session.markAsSucceeded().set("deleteUid", user[0]);
                        })
                    .exitHereIfFailed()
                    .exec(
                        http(DELETE_REQUEST)
                            .delete("/api/users/#{deleteUid}")
                            .check(status().is(200))));

    // Ramp concurrency from 1 to CONCURRENCY over RAMP_SECONDS, then hold CONCURRENCY for
    // SUSTAIN_SECONDS. The same profile drives every scenario (steps are immutable, so sharing the
    // array is safe).
    ClosedInjectionStep[] loadProfile = {
      rampConcurrentUsers(1).to(CONCURRENCY).during(RAMP_SECONDS),
      constantConcurrentUsers(CONCURRENCY).during(SUSTAIN_SECONDS)
    };

    PopulationBuilder postPopulation = postScenario.injectClosed(loadProfile);
    PopulationBuilder getPopulation = getScenario.injectClosed(loadProfile);
    PopulationBuilder putPopulation = putScenario.injectClosed(loadProfile);
    PopulationBuilder patchPopulation = patchScenario.injectClosed(loadProfile);
    PopulationBuilder patchGroupsPopulation = patchGroupsScenario.injectClosed(loadProfile);
    PopulationBuilder replicaPopulation = replicaScenario.injectClosed(loadProfile);
    PopulationBuilder deletePopulation = deleteScenario.injectClosed(loadProfile);

    var sim =
        "sequential".equals(MODE)
            // Order least → most destructive so DELETE runs last. Slices are disjoint, so this is
            // belt-and-suspenders, but it keeps the run order intuitive.
            ? setUp(
                postPopulation
                    .andThen(getPopulation)
                    .andThen(patchPopulation)
                    .andThen(replicaPopulation)
                    .andThen(putPopulation)
                    .andThen(patchGroupsPopulation)
                    .andThen(deletePopulation))
            : setUp(
                postPopulation,
                getPopulation,
                patchPopulation,
                replicaPopulation,
                putPopulation,
                patchGroupsPopulation,
                deletePopulation);

    sim.protocols(httpProtocol)
        .assertions(
            details(POST_REQUEST).responseTime().percentile(95).lt(POST_THRESH.get(PROFILE).p95()),
            details(POST_REQUEST).responseTime().max().lt(POST_THRESH.get(PROFILE).max()),
            details(POST_REQUEST).successfulRequests().percent().is(100D),
            details(GET_REQUEST).responseTime().percentile(95).lt(GET_THRESH.get(PROFILE).p95()),
            details(GET_REQUEST).responseTime().max().lt(GET_THRESH.get(PROFILE).max()),
            details(GET_REQUEST).successfulRequests().percent().is(100D),
            details(PUT_REQUEST).responseTime().percentile(95).lt(PUT_THRESH.get(PROFILE).p95()),
            details(PUT_REQUEST).responseTime().max().lt(PUT_THRESH.get(PROFILE).max()),
            details(PUT_REQUEST).successfulRequests().percent().is(100D),
            details(PATCH_REQUEST)
                .responseTime()
                .percentile(95)
                .lt(PATCH_THRESH.get(PROFILE).p95()),
            details(PATCH_REQUEST).responseTime().max().lt(PATCH_THRESH.get(PROFILE).max()),
            details(PATCH_REQUEST).successfulRequests().percent().is(100D),
            details(PATCH_GROUPS_REQUEST)
                .responseTime()
                .percentile(95)
                .lt(PATCH_GROUPS_THRESH.get(PROFILE).p95()),
            details(PATCH_GROUPS_REQUEST)
                .responseTime()
                .max()
                .lt(PATCH_GROUPS_THRESH.get(PROFILE).max()),
            details(PATCH_GROUPS_REQUEST).successfulRequests().percent().is(100D),
            details(REPLICA_REQUEST)
                .responseTime()
                .percentile(95)
                .lt(REPLICA_THRESH.get(PROFILE).p95()),
            details(REPLICA_REQUEST).responseTime().max().lt(REPLICA_THRESH.get(PROFILE).max()),
            details(REPLICA_REQUEST).successfulRequests().percent().is(100D),
            details(DELETE_REQUEST)
                .responseTime()
                .percentile(95)
                .lt(DELETE_THRESH.get(PROFILE).p95()),
            details(DELETE_REQUEST).responseTime().max().lt(DELETE_THRESH.get(PROFILE).max()),
            details(DELETE_REQUEST).successfulRequests().percent().is(100D));
  }
}
