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
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.during;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.incrementConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for DHIS2 Tracker API endpoints.
 *
 * <p>Tests three programs from the Sierra Leone demo DB:
 *
 * <ul>
 *   <li><b>MNCH / PNC</b> ({@code uy2gU8kT1jF}) -- tracker program, import only
 *   <li><b>Child Programme</b> ({@code IpHINAT79UW}) -- tracker program, import + export
 *   <li><b>Antenatal care visit</b> ({@code lxAQ7Zs9VYR}) -- event program, import + export
 * </ul>
 *
 * <p>Import data is pre-generated <a href="https://github.com/synthetichealth/synthea">Synthea</a>
 * patient data fetched from S3 (see {@code SyntheaToNdjson}). All scenarios run sequentially:
 * imports first (MNCH, Child, ANC), then exports (ANC Events, Child Programme).
 *
 * <p><b>User provisioning:</b> The test provisions users via {@code -DprovisionUsers} (defaults to
 * an estimated value that should accommodate the peak concurrent users for the profiles' default
 * parameters). This value must be >= the maximum concurrent users during the test; otherwise users
 * may be reused concurrently, causing login conflicts that invalidate sessions.
 *
 * <p><b>Test mode:</b> {@code -DtestMode} controls which phases run:
 *
 * <ul>
 *   <li>{@code all} (default) -- import then export
 *   <li>{@code import} -- import only, skip export
 *   <li>{@code export} -- export only, skip import (DB must be seeded)
 * </ul>
 *
 * <p><b>Import parameters:</b>
 *
 * <ul>
 *   <li>{@code -DimportEntitiesPerRequest} -- target entities (TEs + enrollments + events) per HTTP
 *       request (default: 500 for load, 50 for smoke)
 *   <li>{@code -DimportMaxEntitiesPerProgram} -- cap on entities imported per program. The actual
 *       count may be lower due to integer division rounding -- this is a cap, not a target.
 *       (default: 30,000 for load, 500 for smoke)
 *   <li>{@code -DimportUsers} -- concurrent import users (default: 4 for load, 1 for smoke)
 * </ul>
 *
 * <p><b>Profiles:</b>
 *
 * <ul>
 *   <li><b>smoke</b> (default) - Single user with repeat loop <br>
 *       Uses the closed injection model with constantConcurrentUsers(1) <br>
 *       Uses: repeat (default: 100) <br>
 *       Example: {@code -Dprofile=smoke -Drepeat=50}
 *   <li><b>load</b> - Gradual ramp-up to sustained load <br>
 *       Shape: ___/‾‾‾‾‾‾‾‾‾ <br>
 *       Uses the closed injection model: a fixed pool of concurrent users log in once and loop via
 *       {@code during()} for the injection duration. <br>
 *       Uses: concurrentUsers, rampDurationSec (ramp-up), durationSec (sustained) <br>
 *       Default: 30s ramp -> 3min sustained (3.5min total) <br>
 *       Example: {@code -Dprofile=load -DconcurrentUsers=4 -DrampDurationSec=30 -DdurationSec=180}
 *       <p>For soak testing, use load with longer duration: {@code -Dprofile=load
 *       -DconcurrentUsers=4 -DdurationSec=3600}
 *   <li><b>capacity</b> - Staircase pattern to find maximum sustainable users <br>
 *       Shape: <br>
 *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...
 *       #steps <br>
 *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/‾‾ <br>
 *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;/‾‾ <br>
 *       __/‾‾ <br>
 *       Uses the closed injection model: concurrent users log in once and loop via {@code during()}
 *       for the injection duration. <br>
 *       Uses: concurrentUsers (target), rampDurationSec (between steps), durationSec (per step),
 *       steps <br>
 *       Default: 5 steps x 30s with 10s ramps (3.2min total) <br>
 *       Example: {@code -Dprofile=capacity -DconcurrentUsers=10 -Dsteps=5 -DrampDurationSec=10
 *       -DdurationSec=30}
 * </ul>
 *
 * @see <a
 *     href="https://docs.gatling.io/guides/optimize-scripts/writing-realistic-tests/#injection-profiles">Gatling
 *     Injection Profiles</a>
 */
public class TrackerTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(TrackerTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  private static final List<Map<String, Object>> userCredentials = new ArrayList<>();
  private static FeederBuilder<Object> userFeeder;

  private final Profile profile;
  private final String instance;
  private final String eventProgram;
  private final String trackerProgram;
  private final String adminUser;
  private final String adminPassword;
  private final String replicaUser;
  private final String replicaPassword;
  private final int provisionUsers;
  private final int concurrentUsers;
  private final int repeat;
  private final int durationSec;
  private final int rampDurationSec;
  private final int steps;
  private final TestMode testMode;
  private final int importEntitiesPerRequest;
  private final int importMaxEntitiesPerProgram;
  private final int importUsers;
  private final boolean skipSideEffects;
  private NdjsonFeeder mnchFeeder;
  private NdjsonFeeder childFeeder;
  private NdjsonFeeder ancFeeder;

  private enum Profile {
    SMOKE,
    LOAD,
    CAPACITY;

    static Profile fromString(String profile) {
      try {
        return valueOf(profile.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Unknown profile: " + profile + ". Valid options: smoke, load, capacity");
      }
    }
  }

  private enum TestMode {
    ALL,
    IMPORT,
    EXPORT;

    static TestMode fromString(String mode) {
      try {
        return valueOf(mode.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Unknown testMode: " + mode + ". Valid options: all, import, export");
      }
    }
  }

  private record Request(
      String url, EnumMap<Profile, Integer> p95Thresholds, String name, String... groups) {
    HttpRequestActionBuilder action() {
      return http(name).get(url).header("X-Request-ID", session -> nextRequestId(name));
    }

    Optional<Assertion> assertion(Profile profile) {
      return Optional.ofNullable(p95Thresholds.get(profile))
          .map(
              threshold -> {
                String[] parts = Arrays.copyOf(groups, groups.length + 1);
                parts[groups.length] = name;
                return details(parts).responseTime().percentile(95).lte(threshold);
              });
    }
  }

  private record ScenarioWithRequests(ScenarioBuilder scenario, List<Request> requests) {}

  public TrackerTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.eventProgram = System.getProperty("eventProgram", "lxAQ7Zs9VYR"); // Antenatal care visit
    this.trackerProgram = System.getProperty("trackerProgram", "IpHINAT79UW"); // Child Programme
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");
    this.replicaUser = System.getProperty("replicaUser", "tracker2");
    this.replicaPassword = System.getProperty("replicaPassword", "Tracker123@");

    record ProfileDefaults(
        int concurrentUsers,
        int provisionUsers,
        int repeat,
        int rampDurationSec,
        int durationSec,
        int steps,
        int importEntitiesPerRequest,
        int importMaxEntitiesPerProgram,
        int importUsers) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          case SMOKE -> new ProfileDefaults(1, 1, 100, 1, 1, 1, 50, 500, 1);
          case LOAD -> new ProfileDefaults(4, 4, 1, 15, 180, 1, 500, 30_000, 4);
          case CAPACITY -> new ProfileDefaults(8, 8, 1, 10, 30, 4, 500, 30_000, 4);
        };
    this.concurrentUsers = Integer.getInteger("concurrentUsers", defaults.concurrentUsers());
    this.repeat = Integer.getInteger("repeat", defaults.repeat());
    this.provisionUsers = Integer.getInteger("provisionUsers", defaults.provisionUsers());
    this.rampDurationSec = Integer.getInteger("rampDurationSec", defaults.rampDurationSec());
    this.durationSec = Integer.getInteger("durationSec", defaults.durationSec());
    this.steps = Integer.getInteger("steps", defaults.steps());
    this.testMode = TestMode.fromString(System.getProperty("testMode", "all"));
    this.importEntitiesPerRequest =
        Integer.getInteger("importEntitiesPerRequest", defaults.importEntitiesPerRequest());
    this.importMaxEntitiesPerProgram =
        Integer.getInteger("importMaxEntitiesPerProgram", defaults.importMaxEntitiesPerProgram());
    this.importUsers = Integer.getInteger("importUsers", defaults.importUsers());
    this.skipSideEffects = Boolean.getBoolean("skipSideEffects");

    if (this.testMode != TestMode.EXPORT) {
      String s3Base =
          System.getProperty(
              "importS3Url",
              "https://s3.eu-west-1.amazonaws.com/databases.dhis2.org/tracker/synthea/import");
      Path defaultCacheDir =
          Path.of(System.getProperty("user.home"), ".cache", "dhis2", "perf", "tracker");
      Path cacheDir = Path.of(System.getProperty("importCacheDir", defaultCacheDir.toString()));
      S3Fetcher.fetchAll(s3Base, cacheDir, "mnch.ndjson.gz", "child.ndjson.gz", "anc.ndjson.gz");
      this.mnchFeeder = new NdjsonFeeder(cacheDir.resolve("mnch.ndjson.gz"));
      this.childFeeder = new NdjsonFeeder(cacheDir.resolve("child.ndjson.gz"));
      this.ancFeeder = new NdjsonFeeder(cacheDir.resolve("anc.ndjson.gz"));
    }

    try {
      provisionUsers();
    } catch (Exception e) {
      throw new RuntimeException("User provisioning failed", e);
    }

    if (Boolean.getBoolean("setupNotifications")) {
      try {
        setupNotifications();
      } catch (Exception e) {
        throw new RuntimeException("Notification setup failed", e);
      }
    }

    ScenarioWithRequests eventScenario = exportEnabled() ? eventProgramScenario() : null;
    ScenarioWithRequests trackerScenario = exportEnabled() ? trackerProgramScenario() : null;

    PopulationBuilder populationBuilder =
        switch (this.testMode) {
          case ALL -> importScenarios().andThen(exportScenarios(eventScenario, trackerScenario));
          case IMPORT -> importScenarios();
          case EXPORT -> exportScenarios(eventScenario, trackerScenario);
        };

    HttpProtocolBuilder httpProtocolBuilder =
        http.baseUrl(this.instance)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/Performance Test")
            .disableFollowRedirect() // we don't expect redirects
            .warmUp(
                this.instance
                    + "/api/ping") // https://docs.gatling.io/reference/script/http/protocol/#warmup
            .disableCaching() // to repeat the same request without HTTP cache influence (304)
            .check(status().is(200)); // global check for all requests

    List<Assertion> assertions =
        exportEnabled() ? getAssertions(this.profile, eventScenario, trackerScenario) : List.of();
    setUp(populationBuilder).protocols(httpProtocolBuilder).assertions(assertions);
  }

  /** Provisions test users by replicating a source user via DHIS2 API. */
  private void provisionUsers() throws Exception {
    logger.debug("Provisioning {} test users...", this.provisionUsers);

    HttpClient client = HttpClient.newBuilder().build();
    String auth =
        Base64.getEncoder()
            .encodeToString(
                (this.adminUser + ":" + this.adminPassword).getBytes(StandardCharsets.UTF_8));

    HttpRequest getUserRequest =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    this.instance
                        + "/api/users?filter=username:eq:"
                        + this.replicaUser
                        + "&fields=id"))
            .header("Authorization", "Basic " + auth)
            .header("Accept", "application/json")
            .GET()
            .build();

    HttpResponse<String> getUserResponse =
        client.send(getUserRequest, HttpResponse.BodyHandlers.ofString());

    if (getUserResponse.statusCode() != 200) {
      throw new RuntimeException(
          "Failed to get source user: "
              + getUserResponse.statusCode()
              + " "
              + getUserResponse.body());
    }

    Pattern pattern = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    Matcher matcher = pattern.matcher(getUserResponse.body());
    if (!matcher.find()) {
      throw new RuntimeException("Could not find source user '" + this.replicaUser + "'");
    }
    String userId = matcher.group(1);
    logger.debug("Found source user '{}' with ID: {}", this.replicaUser, userId);

    // Throttle user creation to avoid overwhelming the system
    int provisionDelayMs = Integer.getInteger("provisionDelayMs", 100);
    for (int i = 1; i <= this.provisionUsers; i++) {
      String username = "%s_user_%03d".formatted(this.replicaUser, i);
      String requestBody =
          """
          {"username":"%s","password":"%s"}
          """
              .formatted(username, this.replicaPassword)
              .trim();

      HttpRequest replicateRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(this.instance + "/api/users/" + userId + "/replica"))
              .header("Authorization", "Basic " + auth)
              .header("Content-Type", "application/json")
              .header("Accept", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> replicateResponse =
          client.send(replicateRequest, HttpResponse.BodyHandlers.ofString());

      if (replicateResponse.statusCode() == 201) {
        userCredentials.add(Map.of("username", username, "password", this.replicaPassword));
        logger.debug("Created user {}/{}: {}", i, this.provisionUsers, username);
      } else if (replicateResponse.statusCode() == 409
          && replicateResponse.body().contains("Username already taken")) {
        userCredentials.add(Map.of("username", username, "password", this.replicaPassword));
        logger.debug("User already exists {}/{}: {}", i, this.provisionUsers, username);
      } else {
        throw new RuntimeException(
            "Failed to create user "
                + username
                + ": HTTP "
                + replicateResponse.statusCode()
                + " - "
                + replicateResponse.body());
      }

      if (i < this.provisionUsers && provisionDelayMs > 0) {
        Thread.sleep(provisionDelayMs);
      }
    }

    // Use circular to reuse users across multiple VU executions
    userFeeder = io.gatling.javaapi.core.CoreDsl.listFeeder(userCredentials).circular();

    logger.debug("User provisioning complete! Total users: {}", userCredentials.size());

    // Wait for DHIS2 to stabilize after user creation
    int pauseAfterProvisioningSec = Integer.getInteger("pauseAfterProvisioningSec", 5);
    if (pauseAfterProvisioningSec > 0) {
      logger.debug("Waiting {}s for system to stabilize...", pauseAfterProvisioningSec);
      Thread.sleep(pauseAfterProvisioningSec * 1000L);
      logger.debug("Starting test execution...");
    }
  }

  /**
   * Creates notification templates and a program rule on the test programs so that notifications
   * fire during import. Gated by {@code -DsetupNotifications=true}.
   *
   * <p>Uses individual API calls rather than bulk /api/metadata because notification templates must
   * be created first, then linked to programs/stages via their collection endpoints.
   */
  private void setupNotifications() throws Exception {
    logger.debug("Setting up notification templates on test programs...");

    HttpClient client = HttpClient.newBuilder().build();
    String auth =
        Base64.getEncoder()
            .encodeToString(
                (this.adminUser + ":" + this.adminPassword).getBytes(StandardCharsets.UTF_8));

    String mnchProgram = "uy2gU8kT1jF";
    String[] mnchStages = {"eaDHS084uMp", "grIfo3oOf4Y", "Xgk8Wvl0jHr", "oRySG82BKE6"};
    String childProgram = "IpHINAT79UW";
    String[] childStages = {"A03MvHHogjR", "ZzYYXq4fJie"};
    String ancStage = "dBwrot7S420";

    // Create user group with admin user
    String adminUserUid = apiGet(client, auth, "/api/me?fields=id", "id");
    apiPost(
        client,
        auth,
        "/api/userGroups",
        "{\"id\":\"PerfNtGrp01\",\"name\":\"Perf Notification Group\","
            + "\"users\":[{\"id\":\""
            + adminUserUid
            + "\"}]}");

    int counter = 0;

    // MNCH program templates
    String mnchEnroll = createTemplate(client, auth, counter++, "MNCH enrollment", "ENROLLMENT");
    String mnchCompl = createTemplate(client, auth, counter++, "MNCH completion", "COMPLETION");
    linkTemplateToProgram(client, auth, mnchProgram, mnchEnroll);
    linkTemplateToProgram(client, auth, mnchProgram, mnchCompl);

    // MNCH stage templates
    for (String stage : mnchStages) {
      String uid = createTemplate(client, auth, counter++, "MNCH stage " + stage, "COMPLETION");
      linkTemplateToProgramStage(client, auth, stage, uid);
    }

    // Child programme templates
    String childEnroll = createTemplate(client, auth, counter++, "Child enrollment", "ENROLLMENT");
    linkTemplateToProgram(client, auth, childProgram, childEnroll);
    for (String stage : childStages) {
      String uid = createTemplate(client, auth, counter++, "Child stage " + stage, "COMPLETION");
      linkTemplateToProgramStage(client, auth, stage, uid);
    }

    // ANC stage template
    String ancUid = createTemplate(client, auth, counter++, "ANC completion", "COMPLETION");
    linkTemplateToProgramStage(client, auth, ancStage, ancUid);

    // Rule engine: SENDMESSAGE on MNCH
    String ruleTemplateUid =
        createTemplate(client, auth, counter++, "MNCH rule engine", "PROGRAM_RULE");
    linkTemplateToProgram(client, auth, mnchProgram, ruleTemplateUid);

    // Program rule
    apiPost(
        client,
        auth,
        "/api/programRules",
        "{\"id\":\"PerfPrRule1\",\"name\":\"Perf send message\","
            + "\"program\":{\"id\":\""
            + mnchProgram
            + "\"},\"condition\":\"true\"}");

    // Program rule action
    apiPost(
        client,
        auth,
        "/api/programRuleActions",
        "{\"id\":\"PerfRlActn1\",\"programRuleActionType\":\"SENDMESSAGE\","
            + "\"templateUid\":\""
            + ruleTemplateUid
            + "\","
            + "\"notificationTemplate\":{\"id\":\""
            + ruleTemplateUid
            + "\"},"
            + "\"programRule\":{\"id\":\"PerfPrRule1\"}}");

    logger.debug("Notification setup complete: {} templates + 1 rule", counter);
  }

  private String createTemplate(
      HttpClient client, String auth, int index, String name, String trigger) throws Exception {
    String uid = "PerfNtTmp" + String.format("%02d", index);
    apiPost(
        client,
        auth,
        "/api/programNotificationTemplates",
        "{\"id\":\""
            + uid
            + "\",\"name\":\""
            + name
            + "\","
            + "\"notificationTrigger\":\""
            + trigger
            + "\","
            + "\"notificationRecipient\":\"USER_GROUP\","
            + "\"recipientUserGroup\":{\"id\":\"PerfNtGrp01\"},"
            + "\"messageTemplate\":\"perf test\","
            + "\"subjectTemplate\":\""
            + name
            + "\"}");
    return uid;
  }

  private void linkTemplateToProgram(
      HttpClient client, String auth, String programUid, String templateUid) throws Exception {
    apiPost(
        client, auth, "/api/programs/" + programUid + "/notificationTemplates/" + templateUid, "");
  }

  private void linkTemplateToProgramStage(
      HttpClient client, String auth, String stageUid, String templateUid) throws Exception {
    apiPost(
        client,
        auth,
        "/api/programStages/" + stageUid + "/notificationTemplates/" + templateUid,
        "");
  }

  private String apiGet(HttpClient client, String auth, String path, String field)
      throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(this.instance + path))
            .header("Authorization", "Basic " + auth)
            .header("Accept", "application/json")
            .GET()
            .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new RuntimeException("GET " + path + " failed: " + response.statusCode());
    }
    Pattern pattern = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
    Matcher matcher = pattern.matcher(response.body());
    if (!matcher.find()) {
      throw new RuntimeException("Field " + field + " not found in: " + response.body());
    }
    return matcher.group(1);
  }

  private void apiPost(HttpClient client, String auth, String path, String body) throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder()
            .uri(URI.create(this.instance + path))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json");
    HttpRequest request =
        body.isEmpty()
            ? builder.POST(HttpRequest.BodyPublishers.noBody()).build()
            : builder.POST(HttpRequest.BodyPublishers.ofString(body)).build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    int status = response.statusCode();
    // Accept 2xx (created/ok) and 409 (already exists -- idempotent)
    if ((status < 200 || status >= 300) && status != 409) {
      throw new RuntimeException("POST " + path + " failed: " + status + " " + response.body());
    }
  }

  private HttpRequestActionBuilder login() {
    return http("Login")
        .post("/api/auth/login")
        .header("Content-Type", "application/json")
        .header("X-Request-ID", session -> nextRequestId("Login"))
        .body(StringBody("{\"username\":\"#{username}\",\"password\":\"#{password}\"}"))
        .check(status().is(200));
  }

  private boolean exportEnabled() {
    return this.testMode != TestMode.IMPORT;
  }

  private PopulationBuilder importScenarios() {
    // entitiesPerLine: MNCH = 1 TE + 2 enrollments + 6 events, Child = 1 TE + 1 enrollment + 2
    // events
    return importProgram("MNCH import", this.mnchFeeder, 9, "trackedEntities")
        .andThen(importProgram("Child Programme import", this.childFeeder, 4, "trackedEntities"))
        .andThen(importProgram("ANC import", this.ancFeeder, 1, "events"));
  }

  /**
   * Builds an import scenario for a single program. Each ndjson line may contain multiple entities
   * (e.g. 1 TE + enrollments + events). The number of import requests per user is derived from
   * {@code importMaxEntitiesPerProgram}, {@code importEntitiesPerRequest}, and {@code importUsers}.
   * The actual number of imported entities is capped at {@code importMaxEntitiesPerProgram} but may
   * undershoot due to integer division rounding -- it is not a target.
   *
   * <p>Example for MNCH (entitiesPerLine=9, feeder lineCount=94538) with
   * importEntitiesPerRequest=500, importMaxEntitiesPerProgram=5000, importUsers=4:
   *
   * <pre>
   *   linesPerRequest   = importEntitiesPerRequest / entitiesPerLine     = 500 / 9              = 55
   *   availableEntities = feeder.lineCount * entitiesPerLine             = 94538 * 9            = 850842
   *   entitiesPerUser   = min(importMaxEntitiesPerProgram, available) / importUsers = min(5000, 850842) / 4 = 1250
   *   requestsPerUser   = entitiesPerUser / importEntitiesPerRequest     = 1250 / 500           = 2
   *   actual entities   = requestsPerUser * linesPerRequest * entitiesPerLine * importUsers = 2 * 55 * 9 * 4 = 3960
   * </pre>
   */
  private PopulationBuilder importProgram(
      String name, NdjsonFeeder feeder, int entitiesPerLine, String wrapperKey) {
    int linesPerRequest = this.importEntitiesPerRequest / entitiesPerLine;
    int availableEntities = feeder.lineCount() * entitiesPerLine;
    int entitiesPerUser =
        Math.min(this.importMaxEntitiesPerProgram, availableEntities) / this.importUsers;
    int requestsPerUser = entitiesPerUser / this.importEntitiesPerRequest;
    logger.debug(
        "Import {}: {} lines, {} lines/request, {} requests/user, {} users, skipSideEffects={}",
        name,
        feeder.lineCount(),
        linesPerRequest,
        requestsPerUser,
        this.importUsers,
        this.skipSideEffects);
    return scenario(name)
        .exec(
            session -> session.set("username", this.adminUser).set("password", this.adminPassword))
        .exec(login())
        .exitHereIfFailed()
        .repeat(requestsPerUser)
        .on(
            feed(feeder, linesPerRequest)
                .exec(
                    http(name)
                        .post(
                            "/api/tracker?async=false&importStrategy=CREATE_AND_UPDATE"
                                + (skipSideEffects ? "&skipSideEffects=true" : ""))
                        .header("Content-Type", "application/json")
                        .header("X-Request-ID", session -> nextRequestId(name))
                        .body(
                            StringBody(
                                session -> wrapPayload(session.getList("payload"), wrapperKey)))
                        .check(status().is(200))))
        .injectOpen(atOnceUsers(this.importUsers));
  }

  /** Wraps a list of pre-built JSON objects into a tracker import envelope. */
  private static String wrapPayload(List<?> payloads, String wrapperKey) {
    StringBuilder sb = new StringBuilder(payloads.size() * 2048);
    sb.append("{\"").append(wrapperKey).append("\":[");
    for (int i = 0; i < payloads.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append(payloads.get(i));
    }
    sb.append("]}");
    return sb.toString();
  }

  private PopulationBuilder exportScenarios(
      ScenarioWithRequests eventScenario, ScenarioWithRequests trackerScenario) {
    List<ClosedInjectionStep> closedProfile = buildClosedInjectionProfile();
    return eventScenario
        .scenario()
        .injectClosed(closedProfile)
        .andThen(trackerScenario.scenario().injectClosed(closedProfile));
  }

  private ScenarioWithRequests eventProgramScenario() {
    String singleEventUrl = "/api/tracker/events/#{eventUid}";
    String relationshipUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    String getEventsUrl =
        "/api/tracker/events?program="
            + this.eventProgram
            + "&fields=dataValues,occurredAt,event,status,orgUnit,program,programType,updatedAt,createdAt,assignedUser"
            + "&orgUnit=DiszpKrYNg8"
            + "&orgUnitMode=SELECTED"
            + "&order=occurredAt:desc";

    Request goToFirstPage =
        new Request(
            getEventsUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 84, Profile.LOAD, 25)),
            "Go to first page",
            "Get ANC events");
    Request goToSecondPage =
        new Request(
            getEventsUrl + "&page=2",
            new EnumMap<>(Map.of(Profile.SMOKE, 84, Profile.LOAD, 58)),
            "Go to second page",
            "Get ANC events");
    Request searchEventsByDateRange =
        new Request(
            getEventsUrl + "&occurredAfter=2020-01-01&occurredBefore=2025-12-31",
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 54)),
            "Search by date range",
            "Get ANC events");
    Request searchEventsNotAssigned =
        new Request(
            getEventsUrl + "&assignedUserMode=NONE",
            new EnumMap<>(Map.of(Profile.SMOKE, 82, Profile.LOAD, 56)),
            "Search not assigned",
            "Get ANC events");
    Request getFirstEvent =
        new Request(
            singleEventUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 44, Profile.LOAD, 119)),
            "Get first event",
            "Get ANC events",
            "Get one event");
    Request getRelationshipsForFirstEvent =
        new Request(
            relationshipUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 25)),
            "Get relationships for first event",
            "Get ANC events",
            "Get one event");

    var exportRequests =
        exec(session -> session.remove("eventUid"))
            .exec(
                group("Get ANC events")
                    .on(
                        // User opens event list
                        exec(goToFirstPage.action().check(jsonPath("$.events[*]").count().gte(1)))
                            .pause(1, 3) // user reads the list
                            // User paginates
                            .exec(
                                goToSecondPage
                                    .action()
                                    .check(jsonPath("$.events[*]").count().gte(1)))
                            .pause(1, 3) // user reads page 2
                            // User filters by unassigned
                            .exec(
                                searchEventsNotAssigned
                                    .action()
                                    .check(jsonPath("$.events[*]").count().gte(1)))
                            .pause(1, 3) // user reads filtered results
                            // User filters by date range
                            .exec(
                                searchEventsByDateRange
                                    .action()
                                    .check(jsonPath("$.events[*]").count().gte(1))
                                    .check(jsonPath("$.events[0].event").saveAs("eventUid")))
                            .pause(1, 3) // user reads results, picks an event
                            // User clicks on an event -- Capture fires event + relationships
                            // together
                            .doIf(session -> session.contains("eventUid"))
                            .then(
                                group("Get one event")
                                    .on(
                                        exec(getFirstEvent
                                                .action()
                                                .check(jsonPath("$.event").exists()))
                                            .exec(
                                                getRelationshipsForFirstEvent
                                                    .action()
                                                    .check(
                                                        jsonPath("$.relationships[*]")
                                                            .count()
                                                            .is(0)))))));

    ScenarioBuilder scenarioBuilder =
        scenario("ANC Events export")
            .feed(userFeeder)
            .exec(login())
            .exitHereIfFailed()
            .exec(loopForProfile(exportRequests));

    return new ScenarioWithRequests(
        scenarioBuilder,
        List.of(
            goToFirstPage,
            goToSecondPage,
            searchEventsNotAssigned,
            searchEventsByDateRange,
            getFirstEvent,
            getRelationshipsForFirstEvent));
  }

  private ScenarioWithRequests trackerProgramScenario() {
    String getTEsUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc&page=1&pageSize=15&orgUnits=DiszpKrYNg8&orgUnitMode=SELECTED&program="
            + this.trackerProgram
            + "&fields=:all,!relationships,programOwners[orgUnit,program]";

    String getTEsWithEnrollmentStatusUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc&page=1&pageSize=15&orgUnitMode=ACCESSIBLE&program="
            + this.trackerProgram
            + "&filter=w75KJ2mc4zz:ge:A"
            + "&enrollmentStatus=ACTIVE"
            + "&fields=:all,!relationships,programOwners[orgUnit,program]";

    String notFoundTEByName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:notfoundname"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:like:an"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String notFoundTEByExactName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:eq:notfoundname"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByExactName =
        "/api/tracker/trackedEntities?filter=w75KJ2mc4zz:eq:John"
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.trackerProgram
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchBirthEvents =
        "/api/tracker/events?order=createdAt:desc&page=1"
            + "&pageSize=15&orgUnit=DiszpKrYNg8&orgUnitMode=SELECTED&program="
            + this.trackerProgram
            + "&programStage=A03MvHHogjR&fields=*";

    String getTEsFromEvents =
        "/api/tracker/trackedEntities?pageSize=15&program="
            + this.trackerProgram
            + "&trackedEntities=#{trackedEntityUids}&fields=trackedEntity,createdAt,attributes[attribute,value],programOwners[orgUnit],enrollments[enrollment,status,orgUnit,enrolledAt]";

    String singleTrackedEntityUrl =
        "/api/tracker/trackedEntities/#{trackedEntityUid}?program="
            + this.trackerProgram
            + "&fields=programOwners[orgUnit],enrollments";
    String singleEnrollmentUrl =
        "/api/tracker/enrollments/#{enrollmentUid}?fields=enrollment,trackedEntity,program,status,orgUnit,enrolledAt,occurredAt,followUp,deleted,createdBy,updatedBy,updatedAt,geometry";

    String relationshipForTrackedEntityUrl =
        "/api/tracker/relationships?trackedEntity=#{trackedEntityUid}&paging=false&fields=relationship,relationshipType,createdAt,from[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]],to[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]]";

    String relationshipForEventUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    String eventUrl =
        "/api/tracker/events/#{eventUid}?fields=event,relationships[relationship,relationshipType,relationshipName,bidirectional,from[event[event,dataValues,occurredAt,scheduledAt,status,orgUnit,programStage,program]],to[event[event,dataValues,*,occurredAt,scheduledAt,status,orgUnit,programStage,program]]]";

    Request notFoundTeByNameWithLikeOperator =
        new Request(
            notFoundTEByName,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 103)),
            "Not found TE by name with like operator",
            "Get Child Programme TEs");
    Request notFoundTeByNameWithEqOperator =
        new Request(
            notFoundTEByExactName,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 33)),
            "Not found TE by name with eq operator",
            "Get Child Programme TEs");
    Request searchTeByNameWithLikeOperator =
        new Request(
            searchTEByName,
            new EnumMap<>(Map.of(Profile.SMOKE, 36, Profile.LOAD, 184)),
            "Search TE by name with like operator",
            "Get Child Programme TEs");
    Request searchTeByNameWithEqOperator =
        new Request(
            searchTEByExactName,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 136)),
            "Search TE by name with eq operator",
            "Get Child Programme TEs");
    Request searchBirthEventsByStage =
        new Request(
            searchBirthEvents,
            new EnumMap<>(Map.of(Profile.SMOKE, 38, Profile.LOAD, 169)),
            "Search Birth events",
            "Get Child Programme TEs");
    Request getTrackedEntitiesForEvents =
        new Request(
            getTEsFromEvents,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 25)),
            "Get TEs from events",
            "Get Child Programme TEs");
    Request getFirstPageOfTEs =
        new Request(
            getTEsUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 40, Profile.LOAD, 153)),
            "Get first page of TEs",
            "Get Child Programme TEs");
    Request getTEsWithEnrollmentStatus =
        new Request(
            getTEsWithEnrollmentStatusUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 52, Profile.LOAD, 193)),
            "Get TEs with enrollment status",
            "Get Child Programme TEs");
    Request getFirstTrackedEntity =
        new Request(
            singleTrackedEntityUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 33, Profile.LOAD, 128)),
            "Get first tracked entity",
            "Get Child Programme TEs",
            "Go to single enrollment");
    Request getFirstEnrollment =
        new Request(
            singleEnrollmentUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 26, Profile.LOAD, 46)),
            "Get first enrollment",
            "Get Child Programme TEs",
            "Go to single enrollment");
    Request getRelationshipsForTrackedEntity =
        new Request(
            relationshipForTrackedEntityUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 25)),
            "Get relationships for first tracked entity",
            "Get Child Programme TEs",
            "Go to single enrollment");
    Request getFirstEventFromEnrollment =
        new Request(
            eventUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 64, Profile.LOAD, 143)),
            "Get first event from enrollment",
            "Get Child Programme TEs",
            "Go to single enrollment",
            "Get one event");
    Request getRelationshipsForEvent =
        new Request(
            relationshipForEventUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 25)),
            "Get relationships for first event",
            "Get Child Programme TEs",
            "Go to single enrollment",
            "Get one event");

    var exportRequests =
        exec(session ->
                session
                    .remove("trackedEntityUids")
                    .remove("trackedEntityUid")
                    .remove("enrollmentUid")
                    .remove("eventUid"))
            .exec(
                group("Get Child Programme TEs")
                    .on(
                        // User searches for TE -- tries different searches
                        exec(notFoundTeByNameWithLikeOperator
                                .action()
                                .check(jsonPath("$.trackedEntities[*]").count().is(0)))
                            .pause(1, 3) // user reads "no results", tries exact match
                            .exec(
                                notFoundTeByNameWithEqOperator
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().is(0)))
                            .pause(1, 3) // user reads "no results", tries different name
                            .exec(
                                searchTeByNameWithLikeOperator
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                            .pause(1, 3) // user reads results, refines search
                            .exec(
                                searchTeByNameWithEqOperator
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                            .pause(1, 3) // user reads results
                            // User opens event working list (Birth stage)
                            // Capture fires events first, then enriches with TE data (sequential)
                            .exec(
                                searchBirthEventsByStage
                                    .action()
                                    .check(jsonPath("$.events[*]").count().gte(1))
                                    .check(
                                        jsonPath("$.events[*].trackedEntity")
                                            .findAll()
                                            .transform(
                                                list ->
                                                    String.join(
                                                        ",", list.stream().distinct().toList()))
                                            .saveAs("trackedEntityUids")))
                            .doIf(session -> session.contains("trackedEntityUids"))
                            .then(
                                exec(
                                    getTrackedEntitiesForEvents
                                        .action()
                                        .check(jsonPath("$.trackedEntities[*]").count().gte(1))))
                            .pause(1, 3) // user reads event list
                            // User opens TE working list
                            .exec(
                                getFirstPageOfTEs
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1))
                                    .check(
                                        jsonPath("$.trackedEntities[0].trackedEntity")
                                            .saveAs("trackedEntityUid")))
                            .pause(1, 3) // user reads TE list, picks one
                            // User clicks on TE -- Capture loads TE + enrollment + relationships
                            // together
                            .doIf(session -> session.contains("trackedEntityUid"))
                            .then(
                                group("Go to single enrollment")
                                    .on(
                                        exec(getFirstTrackedEntity
                                                .action()
                                                .check(jsonPath("$.enrollments[*]").count().gte(1))
                                                .check(
                                                    jsonPath("$.enrollments[0].enrollment")
                                                        .saveAs("enrollmentUid"))
                                                .check(
                                                    jsonPath("$.enrollments[0].events[*]")
                                                        .count()
                                                        .gte(1))
                                                .check(
                                                    jsonPath("$.enrollments[0].events[0].event")
                                                        .saveAs("eventUid")))
                                            .doIf(session -> session.contains("enrollmentUid"))
                                            .then(
                                                exec(getFirstEnrollment
                                                        .action()
                                                        .check(jsonPath("$.enrollment").exists()))
                                                    .exec(
                                                        getRelationshipsForTrackedEntity
                                                            .action()
                                                            .check(
                                                                jsonPath("$.relationships[*]")
                                                                    .count()
                                                                    .is(0)))
                                                    .pause(1, 3) // user reads enrollment details
                                                    // User clicks on event within enrollment
                                                    .doIf(session -> session.contains("eventUid"))
                                                    .then(
                                                        group("Get one event")
                                                            .on(
                                                                exec(getFirstEventFromEnrollment
                                                                        .action()
                                                                        .check(
                                                                            jsonPath("$.event")
                                                                                .exists()))
                                                                    .exec(
                                                                        getRelationshipsForEvent
                                                                            .action()
                                                                            .check(
                                                                                jsonPath(
                                                                                        "$.relationships[*]")
                                                                                    .count()
                                                                                    .is(0))))))))
                            .pause(1, 3) // user goes back to list
                            // User opens filtered TE list (enrollment status)
                            .exec(
                                getTEsWithEnrollmentStatus
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1)))));

    ScenarioBuilder scenarioBuilder =
        scenario("Child Programme export")
            .feed(userFeeder)
            .exec(login())
            .exitHereIfFailed()
            .exec(loopForProfile(exportRequests));

    return new ScenarioWithRequests(
        scenarioBuilder,
        List.of(
            notFoundTeByNameWithLikeOperator,
            notFoundTeByNameWithEqOperator,
            searchTeByNameWithLikeOperator,
            searchTeByNameWithEqOperator,
            searchBirthEventsByStage,
            getTrackedEntitiesForEvents,
            getFirstPageOfTEs,
            getTEsWithEnrollmentStatus,
            getFirstTrackedEntity,
            getFirstEnrollment,
            getRelationshipsForTrackedEntity,
            getFirstEventFromEnrollment,
            getRelationshipsForEvent));
  }

  /**
   * Builds the closed injection profile for all profiles. A fixed pool of concurrent users stays
   * alive for the injection duration, each logging in once and looping via {@code during()}.
   *
   * @see <a
   *     href="https://docs.gatling.io/guides/optimize-scripts/writing-realistic-tests/#injection-profiles">Gatling
   *     Injection Profiles</a>
   * @return List of ClosedInjectionStep for the configured profile
   */
  private List<ClosedInjectionStep> buildClosedInjectionProfile() {
    return switch (this.profile) {
      case SMOKE -> List.of(constantConcurrentUsers(1).during(1));
      case LOAD ->
          List.of(
              rampConcurrentUsers(0)
                  .to(this.concurrentUsers)
                  .during(Duration.ofSeconds(this.rampDurationSec)),
              constantConcurrentUsers(this.concurrentUsers)
                  .during(Duration.ofSeconds(this.durationSec)));
      case CAPACITY ->
          List.of(
              incrementConcurrentUsers(this.concurrentUsers / this.steps)
                  .times(this.steps)
                  .eachLevelLasting(Duration.ofSeconds(this.durationSec))
                  .separatedByRampsLasting(Duration.ofSeconds(this.rampDurationSec))
                  .startingFrom(this.concurrentUsers / this.steps));
    };
  }

  /**
   * Returns the total injection duration in seconds for the current profile. Used as the {@code
   * during()} loop duration to keep virtual users alive for the full injection period.
   */
  private long injectionDurationSec() {
    return switch (this.profile) {
      case SMOKE -> throw new IllegalArgumentException("SMOKE profile uses repeat, not during");
      case LOAD -> this.rampDurationSec + this.durationSec;
      case CAPACITY ->
          (long) this.steps * this.durationSec + (long) (this.steps - 1) * this.rampDurationSec;
    };
  }

  /**
   * Returns either a {@code repeat()} or {@code during()} loop wrapping the given chain, depending
   * on the profile. SMOKE uses {@code repeat()} for deterministic iteration count. LOAD and
   * CAPACITY use {@code during()} to keep users alive for the full injection duration.
   */
  private ChainBuilder loopForProfile(ChainBuilder chain) {
    if (this.profile == Profile.SMOKE) {
      return exec(repeat(this.repeat).on(chain));
    }
    return exec(during(Duration.ofSeconds(injectionDurationSec())).on(chain));
  }

  /**
   * Returns assertions for the given profile, including a global success rate assertion and any
   * profile-specific P95 response time assertions defined in the requests.
   *
   * @param profile The test profile
   * @param scenarios The scenarios with their requests
   * @return List of assertions for the profile
   */
  private List<Assertion> getAssertions(Profile profile, ScenarioWithRequests... scenarios) {
    return Stream.concat(
            Stream.of(forAll().successfulRequests().percent().gte(100d)),
            Arrays.stream(scenarios)
                .flatMap(scenario -> scenario.requests().stream())
                .flatMap(r -> r.assertion(profile).stream()))
        .toList();
  }

  /**
   * Generates a unique {@code X-Request-ID} header value and logs the mapping to the Gatling
   * request name. The ID is a simple counter ({@code g-1}, {@code g-2}, ...) valid per {@link
   * org.hisp.dhis.webapi.filter.RequestIdFilter}. The logged mapping allows correlating SQL queries
   * (grouped by {@code request_id} in SQL comments) back to the Gatling request that triggered
   * them.
   */
  private static String nextRequestId(String name) {
    String id = "g-" + REQUEST_COUNTER.incrementAndGet();
    logger.debug("X-Request-ID: {} -> {}", id, name);
    return id;
  }
}
