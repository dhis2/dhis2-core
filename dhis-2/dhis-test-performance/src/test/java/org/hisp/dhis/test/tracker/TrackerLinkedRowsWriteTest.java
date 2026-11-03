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
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.PopulationBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Write-path test for the tables that reference tracked entities, enrollments and events, but are
 * not tracked entities, enrollments or events themselves: {@code relationshipitem}, {@code
 * programmessage} (plus {@code programmessage_deliverychannels}) and {@code
 * programnotificationinstance}. Each scenario writes {@code -Drows} rows (default 200,000) into its
 * table through the API, in requests of {@code -DbatchSize} (default 1,000), one user, one request
 * at a time. Compare two DHIS2 images (for example with and without a migration adding indexes to
 * these tables) with {@code performance-tests-compare.yml}.
 *
 * <p>Scenarios, run one after the other on the Sierra Leone demo DB:
 *
 * <ul>
 *   <li><b>relationships</b> -- tracker import ({@code POST /api/tracker}) of {@code rows / 2}
 *       relationships from a Child Programme birth event (tracker event) to an Inpatient morbidity
 *       and mortality event (single event). Each relationship writes 2 {@code relationshipitem}
 *       rows, one with {@code trackereventid} and one with {@code singleeventid} set. A new
 *       relationship type is created on every run, so repeated runs (warmups) never import
 *       duplicate relationships.
 *   <li><b>messages</b> -- {@code POST /api/messages} of {@code rows} program messages, one per
 *       request ({@code batchSize} does not apply, see {@link #messagesScenario()}), with one SMS
 *       delivery channel each, cycling through: enrollment with the tracked entity as recipient,
 *       tracker event, single event. No SMS gateway is configured, so the messages are saved as
 *       failed. Tracked entities without a phone number attribute lose their SMS channel, so not
 *       every message gets a {@code programmessage_deliverychannels} row.
 *   <li><b>notifications</b> -- tracker import of Child Programme tracked entities (the same
 *       Synthea data {@link TrackerTest} imports: 1 tracked entity, 1 enrollment, 2 events each).
 *       Setup adds a program rule to that program that schedules a notification for the enrollment
 *       and for every event, so each tracked entity writes 3 {@code programnotificationinstance}
 *       rows (1 with {@code enrollmentid}, 2 with {@code trackereventid} set). Requests carry
 *       {@code batchSize / 4} tracked entities, and enough are imported for at least {@code rows}
 *       notifications. The time includes importing the tracked entities themselves. Notifications
 *       are saved in a background task after each import responds, so they are not part of the
 *       import response times. With {@code -DcountUser} and {@code -DcountPassword} (a user with
 *       the {@code ALL} authority, which admin lacks in the Sierra Leone DB), the scenario also
 *       waits until all notifications are saved, polling {@code /api/programNotificationInstances}
 *       every second, and fails if they are not. Program rule notifications for single events are
 *       not used: the tracker importer never saves them.
 * </ul>
 *
 * <p>Each scenario is a Gatling group, so the report shows its total duration next to the response
 * time of every request.
 *
 * <p><b>Parameters:</b>
 *
 * <ul>
 *   <li>{@code -Drows} -- rows written per scenario (default: 200000)
 *   <li>{@code -DbatchSize} -- rows per request (default: 1000)
 *   <li>{@code -Dscenarios} -- comma separated subset of {@code relationships,messages,
 *       notifications} (default: all three)
 *   <li>{@code -DcountUser}, {@code -DcountPassword} -- user allowed to count notification
 *       instances, see the notifications scenario (default: not set, no waiting)
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * DHIS2_IMAGE=dhis2/core-pr:26050 \
 * SIMULATION_CLASS=org.hisp.dhis.test.tracker.TrackerLinkedRowsWriteTest \
 * MVN_ARGS="-Drows=200000 -DbatchSize=1000" \
 * ./run-simulation.sh
 * }</pre>
 */
public class TrackerLinkedRowsWriteTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(TrackerLinkedRowsWriteTest.class);

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();
  private static final SecureRandom RANDOM = new SecureRandom();

  // Sierra Leone demo DB
  private static final String CHILD_PROGRAM = "IpHINAT79UW";
  private static final String CHILD_BIRTH_STAGE = "A03MvHHogjR";
  // event program with ~100k single events (Antenatal care visit has only 3)
  private static final String SINGLE_EVENT_PROGRAM = "eBAyeGv0exc";

  // fixed so that repeated runs update instead of duplicating them
  private static final String NOTIFICATION_TEMPLATE = "PerfNotTpl2";
  private static final String NOTIFICATION_RULE = "PerfNotRul2";
  private static final String NOTIFICATION_RULE_ACTION = "PerfNotAct2";
  private static final String NOTIFICATION_DATE = "2099-12-31";
  private static final String NOTIFICATION_COUNT_URL =
      "/api/programNotificationInstances?scheduledAt="
          + NOTIFICATION_DATE
          + "&pageSize=1&totalPages=true";

  private static final String RELATIONSHIPS = "relationships";
  private static final String MESSAGES = "messages";
  private static final String NOTIFICATIONS = "notifications";

  private final String instance;
  private final String adminUser;
  private final String adminPassword;
  private final int rows;
  private final int batchSize;
  private final Set<String> scenarios;
  private final HttpClient client = HttpClient.newHttpClient();

  private String relationshipType;
  private List<String> trackerEvents;
  private List<String> singleEvents;
  private List<String[]> enrollments; // [enrollment, trackedEntity]
  private NdjsonFeeder childFeeder;
  private final String countUser;
  private final String countPassword;
  private long notificationsBefore = -1; // -1: cannot count, don't wait

  public TrackerLinkedRowsWriteTest() {
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");
    this.rows = Integer.getInteger("rows", 200_000);
    this.batchSize = Integer.getInteger("batchSize", 1_000);
    this.countUser = System.getProperty("countUser", "");
    this.countPassword = System.getProperty("countPassword", "");
    this.scenarios =
        Arrays.stream(
                System.getProperty(
                        "scenarios", RELATIONSHIPS + "," + MESSAGES + "," + NOTIFICATIONS)
                    .split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    for (String s : this.scenarios) {
      if (!Set.of(RELATIONSHIPS, MESSAGES, NOTIFICATIONS).contains(s)) {
        throw new IllegalArgumentException(
            "Unknown scenario: " + s + ". Valid options: relationships, messages, notifications");
      }
    }
    if (this.rows % this.batchSize != 0) {
      throw new IllegalArgumentException("rows must be a multiple of batchSize");
    }

    try {
      setUpData();
    } catch (Exception e) {
      throw new RuntimeException("Test setup failed", e);
    }

    List<PopulationBuilder> populations = new ArrayList<>();
    if (this.scenarios.contains(RELATIONSHIPS)) {
      populations.add(relationshipsScenario());
    }
    if (this.scenarios.contains(MESSAGES)) {
      populations.add(messagesScenario());
    }
    if (this.scenarios.contains(NOTIFICATIONS)) {
      populations.add(notificationsScenario());
    }
    PopulationBuilder population = populations.get(0);
    for (PopulationBuilder next : populations.subList(1, populations.size())) {
      population = population.andThen(next);
    }

    HttpProtocolBuilder httpProtocol =
        http.baseUrl(this.instance)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .userAgentHeader("Gatling/Performance Test")
            .disableFollowRedirect()
            .warmUp(this.instance + "/api/ping")
            .disableCaching()
            .check(status().is(200));

    setUp(population)
        .protocols(httpProtocol)
        .assertions(forAll().successfulRequests().percent().gte(100d));
  }

  private void setUpData() throws Exception {
    if (this.scenarios.contains(RELATIONSHIPS) || this.scenarios.contains(MESSAGES)) {
      // 1,000 x 1,000 distinct (tracker event, single event) pairs, enough for 1M relationships
      this.trackerEvents =
          fetchUids(
              "/api/tracker/events?program="
                  + CHILD_PROGRAM
                  + "&programStage="
                  + CHILD_BIRTH_STAGE
                  + "&fields=event&pageSize=1000",
              "events",
              "event");
      this.singleEvents =
          fetchUids(
              "/api/tracker/events?program=" + SINGLE_EVENT_PROGRAM + "&fields=event&pageSize=1000",
              "events",
              "event");
      long pairs = (long) this.trackerEvents.size() * this.singleEvents.size();
      if (pairs < this.rows / 2) {
        throw new IllegalStateException(
            "Not enough events for %d distinct relationships: %d tracker events, %d single events"
                .formatted(this.rows / 2, this.trackerEvents.size(), this.singleEvents.size()));
      }
    }

    if (this.scenarios.contains(RELATIONSHIPS)) {
      this.relationshipType = createRelationshipType();
    }

    if (this.scenarios.contains(MESSAGES)) {
      JsonNode page =
          get(
              "/api/tracker/enrollments?program="
                  + CHILD_PROGRAM
                  + "&fields=enrollment,trackedEntity&pageSize=1000");
      this.enrollments = new ArrayList<>();
      for (JsonNode e : page.path("enrollments")) {
        this.enrollments.add(
            new String[] {e.path("enrollment").asText(), e.path("trackedEntity").asText()});
      }
      if (this.enrollments.isEmpty()) {
        throw new IllegalStateException("No enrollments found in program " + CHILD_PROGRAM);
      }
    }

    if (this.scenarios.contains(NOTIFICATIONS)) {
      createNotificationRule();
      String s3Base =
          System.getProperty(
              "importS3Url",
              "https://s3.eu-west-1.amazonaws.com/databases.dhis2.org/tracker/synthea/import");
      Path defaultCacheDir =
          Path.of(System.getProperty("user.home"), ".cache", "dhis2", "perf", "tracker");
      Path cacheDir = Path.of(System.getProperty("importCacheDir", defaultCacheDir.toString()));
      S3Fetcher.fetchAll(s3Base, cacheDir, "child.ndjson.gz");
      this.childFeeder = new NdjsonFeeder(cacheDir.resolve("child.ndjson.gz"));
      if (!this.countUser.isBlank()) {
        this.notificationsBefore = countNotifications();
        logger.info("{} scheduled notifications before the test", this.notificationsBefore);
      } else {
        logger.warn(
            "No -DcountUser: not waiting for the notifications saved in the background after the imports");
      }
    }
  }

  private String createRelationshipType() throws Exception {
    String uid = generateUid();
    // an event constraint needs a program stage; an event program has exactly one
    String singleEventStage =
        get("/api/programs/" + SINGLE_EVENT_PROGRAM + "?fields=programStages[id]")
            .path("programStages")
            .path(0)
            .path("id")
            .asText();
    String body =
        """
        {"relationshipTypes":[{"id":"%s","name":"Perf birth to inpatient event %s",
          "fromToName":"Inpatient event","toFromName":"Birth","bidirectional":false,
          "fromConstraint":{"relationshipEntity":"PROGRAM_STAGE_INSTANCE","programStage":{"id":"%s"}},
          "toConstraint":{"relationshipEntity":"PROGRAM_STAGE_INSTANCE","programStage":{"id":"%s"}},
          "sharing":{"public":"rwrw----"}}]}
        """
            .formatted(uid, uid, CHILD_BIRTH_STAGE, singleEventStage);
    postMetadata(body);
    logger.info("Created relationship type {}", uid);
    return uid;
  }

  /** Creates the rule once; updating it through the metadata import fails on rule actions. */
  private void createNotificationRule() throws Exception {
    if (exists("/api/programRules/" + NOTIFICATION_RULE)) {
      return;
    }
    String body =
        """
        {"programNotificationTemplates":[{"id":"%s","name":"Perf scheduled Child Programme notification",
          "notificationTrigger":"PROGRAM_RULE","notificationRecipient":"ORGANISATION_UNIT_CONTACT",
          "deliveryChannels":["SMS"],"messageTemplate":"Perf test notification","sendRepeatable":true}],
         "programRules":[{"id":"%s","name":"Perf schedule Child Programme notification","condition":"true",
          "program":{"id":"%s"},"programRuleActions":[{"id":"%s"}]}],
         "programRuleActions":[{"id":"%s","programRuleActionType":"SCHEDULEMESSAGE",
          "data":"'%s'","templateUid":"%s","programRule":{"id":"%s"}}]}
        """
            .formatted(
                NOTIFICATION_TEMPLATE,
                NOTIFICATION_RULE,
                CHILD_PROGRAM,
                NOTIFICATION_RULE_ACTION,
                NOTIFICATION_RULE_ACTION,
                NOTIFICATION_DATE,
                NOTIFICATION_TEMPLATE,
                NOTIFICATION_RULE);
    postMetadata(body);
    logger.info(
        "Created program rule {} scheduling notifications on {}", NOTIFICATION_RULE, CHILD_PROGRAM);
  }

  private long countNotifications() throws Exception {
    HttpResponse<String> response =
        this.client.send(
            HttpRequest.newBuilder(URI.create(this.instance + NOTIFICATION_COUNT_URL))
                .header("Authorization", basicAuth(this.countUser, this.countPassword))
                .header("Accept", "application/json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "Counting notifications as %s failed: HTTP %d %s"
              .formatted(this.countUser, response.statusCode(), response.body()));
    }
    return JSON.readTree(response.body()).path("pager").path("total").asLong();
  }

  private PopulationBuilder relationshipsScenario() {
    int relationshipsPerRequest = this.batchSize / 2;
    int requests = this.rows / this.batchSize;
    String name = "Import relationships";
    ChainBuilder request =
        http(name)
            .post("/api/tracker?async=false&importStrategy=CREATE")
            .header("X-Request-ID", session -> nextRequestId(name))
            .body(StringBody(session -> relationshipsPayload(session, relationshipsPerRequest)))
            .check(jsonPath("$.status").is("OK"))
            .toChainBuilder();
    return writeScenario(RELATIONSHIPS, "relationshipitem", requests, this.batchSize, request);
  }

  private String relationshipsPayload(Session session, int perRequest) {
    int start = session.getInt("i") * perRequest;
    int singles = this.singleEvents.size();
    StringBuilder sb = new StringBuilder(perRequest * 120).append("{\"relationships\":[");
    for (int k = start; k < start + perRequest; k++) {
      if (k > start) sb.append(',');
      sb.append("{\"relationshipType\":\"")
          .append(this.relationshipType)
          .append("\",\"from\":{\"event\":{\"event\":\"")
          .append(this.trackerEvents.get(k / singles))
          .append("\"}},\"to\":{\"event\":{\"event\":\"")
          .append(this.singleEvents.get(k % singles))
          .append("\"}}}");
    }
    return sb.append("]}").toString();
  }

  /**
   * One message per request: {@code POST /api/messages} saves the messages of a request in a
   * parallel stream whose worker threads have no logged-in user, so a request with more than one
   * message fails with "No authentication found".
   */
  private PopulationBuilder messagesScenario() {
    String name = "Send program message";
    ChainBuilder request =
        http(name)
            .post("/api/messages")
            .header("X-Request-ID", session -> nextRequestId(name))
            .body(StringBody(session -> messagePayload(session.getInt("i"))))
            .toChainBuilder();
    return writeScenario(MESSAGES, "programmessage", this.rows, 1, request);
  }

  private String messagePayload(int k) {
    StringBuilder sb =
        new StringBuilder(300)
            .append(
                "{\"programMessages\":[{\"text\":\"Perf test message\",\"deliveryChannels\":[\"SMS\"],\"recipients\":{\"phoneNumbers\":[\"+4700000000\"]");
    switch (k % 3) {
      case 0 -> {
        String[] enrollment = this.enrollments.get((k / 3) % this.enrollments.size());
        sb.append(",\"trackedEntity\":{\"id\":\"")
            .append(enrollment[1])
            .append("\"}},\"enrollment\":{\"id\":\"")
            .append(enrollment[0])
            .append("\"}}");
      }
      case 1 ->
          sb.append("},\"event\":{\"id\":\"")
              .append(this.trackerEvents.get((k / 3) % this.trackerEvents.size()))
              .append("\"}}");
      default ->
          sb.append("},\"event\":{\"id\":\"")
              .append(this.singleEvents.get((k / 3) % this.singleEvents.size()))
              .append("\"}}");
    }
    return sb.append("]}").toString();
  }

  private PopulationBuilder notificationsScenario() {
    int trackedEntitiesPerRequest = Math.max(1, this.batchSize / 4);
    int notificationsPerRequest = 3 * trackedEntitiesPerRequest;
    int requests = (this.rows + notificationsPerRequest - 1) / notificationsPerRequest;
    long expected = (long) requests * notificationsPerRequest;
    String name = "Import Child Programme tracked entities";
    ChainBuilder imports =
        repeat(requests, "i")
            .on(
                feed(this.childFeeder, trackedEntitiesPerRequest)
                    .exec(
                        http(name)
                            .post("/api/tracker?async=false")
                            .header("X-Request-ID", session -> nextRequestId(name))
                            .body(
                                StringBody(
                                    session -> trackedEntitiesPayload(session.getList("payload"))))
                            .check(jsonPath("$.status").is("OK"))));
    ChainBuilder chain = imports;
    if (this.notificationsBefore >= 0) {
      long target = this.notificationsBefore + expected;
      String auth = basicAuth(this.countUser, this.countPassword);
      ChainBuilder wait =
          exec(session -> session.set("notifications", 0L))
              .asLongAsDuring(
                  session -> session.getLong("notifications") < target, Duration.ofMinutes(30))
              .on(
                  pause(1)
                      .exec(
                          http("Count notifications")
                              .get(NOTIFICATION_COUNT_URL)
                              .header("Authorization", auth)
                              .check(jsonPath("$.pager.total").ofLong().saveAs("notifications"))))
              .exec(
                  http("All notifications saved")
                      .get(NOTIFICATION_COUNT_URL)
                      .header("Authorization", auth)
                      .check(jsonPath("$.pager.total").ofLong().gte(target)));
      chain = imports.exec(wait);
    }
    logger.info(
        "{} tracked entities per request, {} notifications expected",
        trackedEntitiesPerRequest,
        expected);
    return writeScenario(
        NOTIFICATIONS, "programnotificationinstance", requests, notificationsPerRequest, chain);
  }

  private static String trackedEntitiesPayload(List<?> trackedEntities) {
    StringBuilder sb =
        new StringBuilder(trackedEntities.size() * 4096).append("{\"trackedEntities\":[");
    for (int i = 0; i < trackedEntities.size(); i++) {
      if (i > 0) sb.append(',');
      sb.append(trackedEntities.get(i));
    }
    return sb.append("]}").toString();
  }

  /** One user logs in, then sends {@code requests} requests in a row inside one Gatling group. */
  private PopulationBuilder writeScenario(
      String scenario, String table, int requests, int rowsPerRequest, ChainBuilder request) {
    String group = "%s: %d %s rows".formatted(scenario, this.rows, table);
    logger.info("{}: {} requests of {} rows", group, requests, rowsPerRequest);
    ChainBuilder body =
        NOTIFICATIONS.equals(scenario) ? request : repeat(requests, "i").on(request);
    return scenario(group)
        .exec(login())
        .exitHereIfFailed()
        .group(group)
        .on(body)
        .injectOpen(atOnceUsers(1));
  }

  private ChainBuilder login() {
    return exec(
        http("Login")
            .post("/api/auth/login")
            .header("X-Request-ID", session -> nextRequestId("Login"))
            .body(
                StringBody(
                    "{\"username\":\"%s\",\"password\":\"%s\"}"
                        .formatted(this.adminUser, this.adminPassword))));
  }

  private List<String> fetchUids(String path, String arrayKey, String field) throws Exception {
    List<String> uids = new ArrayList<>();
    for (JsonNode node : get(path).path(arrayKey)) {
      uids.add(node.path(field).asText());
    }
    return uids;
  }

  private JsonNode get(String path) throws Exception {
    HttpResponse<String> response =
        this.client.send(
            HttpRequest.newBuilder(URI.create(this.instance + path))
                .header("Authorization", basicAuth())
                .header("Accept", "application/json")
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException(
          "GET %s failed: HTTP %d %s".formatted(path, response.statusCode(), response.body()));
    }
    return JSON.readTree(response.body());
  }

  private void postMetadata(String body) throws Exception {
    HttpResponse<String> response =
        this.client.send(
            HttpRequest.newBuilder(
                    URI.create(this.instance + "/api/metadata?importStrategy=CREATE_AND_UPDATE"))
                .header("Authorization", basicAuth())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200
        || !"OK".equals(JSON.readTree(response.body()).path("status").asText())) {
      throw new IllegalStateException(
          "Metadata import failed: HTTP %d %s".formatted(response.statusCode(), response.body()));
    }
  }

  private String basicAuth() {
    return basicAuth(this.adminUser, this.adminPassword);
  }

  private static String basicAuth(String user, String password) {
    return "Basic "
        + Base64.getEncoder()
            .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
  }

  private boolean exists(String path) throws Exception {
    return this.client
            .send(
                HttpRequest.newBuilder(URI.create(this.instance + path))
                    .header("Authorization", basicAuth())
                    .header("Accept", "application/json")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.discarding())
            .statusCode()
        == 200;
  }

  private static String generateUid() {
    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String chars = letters + "0123456789";
    StringBuilder sb = new StringBuilder(11);
    sb.append(letters.charAt(RANDOM.nextInt(letters.length())));
    for (int i = 1; i < 11; i++) {
      sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
    }
    return sb.toString();
  }

  private static String nextRequestId(String name) {
    String id = "g-" + REQUEST_COUNTER.incrementAndGet();
    logger.debug("X-Request-ID: {} -> {}", id, name);
    return id;
  }
}
