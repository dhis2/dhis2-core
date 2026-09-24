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
import static io.gatling.javaapi.core.CoreDsl.constantConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.during;
import static io.gatling.javaapi.core.CoreDsl.exec;
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
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
 * Experimental copy of {@link TrackerTest} for the synthetic Nigeria database of <a
 * href="https://github.com/teleivo/dhis2-perf-db-starsim">dhis2-perf-db-starsim</a> (DHIS2-21406),
 * with the WHO Antenatal Care Registry (ANC) and Electronic Immunization Registry (EIR) packages.
 *
 * <p>It runs the export scenario of {@link TrackerTest}'s tracker program (Child Programme) against
 * EIR, the program of the same shape: a child with a non-repeatable birth stage. The event program
 * scenario and the imports are left out; the database has no single-event program, and import
 * payloads for ANC and EIR do not exist yet.
 *
 * <p>Differences to {@link TrackerTest}, for the data this database holds:
 *
 * <ul>
 *   <li>The org unit is the Gezawa (Kano) facility with the most children, and the replicated user
 *       one of its data clerks. A clerk captures in her facility and searches in her LGA.
 *   <li>EIR needs two attributes to search outside the capture scope, so every name search filters
 *       on the given and the family name, where {@link TrackerTest} filters on the first name.
 *   <li>The filter values are names of the database: {@code like:an} on both names matches about 1%
 *       of the children, {@code eq} a given and family name pair of 78 children.
 *   <li>EIR is PROTECTED. The requests stay within what a clerk owns, as in {@link TrackerTest}.
 * </ul>
 *
 * <p>The p95 thresholds are {@link TrackerTest}'s, calibrated on Sierra Leone, so a run shows where
 * this database behaves differently.
 *
 * <p>Every UID and filter value is a system property, with the defaults of the {@code lga} build
 * (Gezawa, seed 42): {@code -Dprogram}, {@code -DbirthStage}, {@code -DorgUnit}, {@code
 * -DgivenName}, {@code -DfamilyName}, {@code -DnameLike}, {@code -DgivenEq}, {@code -DfamilyEq},
 * {@code -DreplicaUser} and {@code -DreplicaPassword}. Profiles and their parameters are those of
 * {@link TrackerTest}.
 */
public class NigeriaTrackerTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(NigeriaTrackerTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  private static final List<Map<String, Object>> userCredentials = new ArrayList<>();
  private static FeederBuilder<Object> userFeeder;

  private final Profile profile;
  private final String instance;
  private final String program;
  private final String birthStage;
  private final String orgUnit;
  private final String givenName;
  private final String familyName;
  private final String nameLike;
  private final String givenEq;
  private final String familyEq;
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

  public NigeriaTrackerTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");
    this.program = System.getProperty("program", "SSLpOM0r1U7"); // Electronic Immunization Registry
    this.birthStage = System.getProperty("birthStage", "RcbCl5ww8XY"); // Birth details
    this.orgUnit = System.getProperty("orgUnit", "opdvNRq1aHr"); // Gezawa, most EIR children
    this.givenName = System.getProperty("givenName", "sB1IHYu2xQT"); // GEN - Given name
    this.familyName = System.getProperty("familyName", "ENRjVGxVL6l"); // GEN - Family name
    this.nameLike = System.getProperty("nameLike", "an");
    this.givenEq = System.getProperty("givenEq", "Musa");
    this.familyEq = System.getProperty("familyEq", "Yahaya");
    this.adminUser = System.getProperty("adminUser", "admin");
    this.adminPassword = System.getProperty("adminPassword", "district");
    this.replicaUser = System.getProperty("replicaUser", "anc.sn_64a4a35d8f.1"); // clerk of orgUnit
    this.replicaPassword = System.getProperty("replicaPassword", "Anc-Perf-2026!");

    record ProfileDefaults(
        int concurrentUsers,
        int provisionUsers,
        int repeat,
        int rampDurationSec,
        int durationSec,
        int steps) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          case SMOKE -> new ProfileDefaults(1, 1, 100, 1, 1, 1);
          case LOAD -> new ProfileDefaults(4, 4, 1, 15, 180, 1);
          case CAPACITY -> new ProfileDefaults(8, 8, 1, 10, 30, 4);
        };
    this.concurrentUsers = Integer.getInteger("concurrentUsers", defaults.concurrentUsers());
    this.repeat = Integer.getInteger("repeat", defaults.repeat());
    this.provisionUsers = Integer.getInteger("provisionUsers", defaults.provisionUsers());
    this.rampDurationSec = Integer.getInteger("rampDurationSec", defaults.rampDurationSec());
    this.durationSec = Integer.getInteger("durationSec", defaults.durationSec());
    this.steps = Integer.getInteger("steps", defaults.steps());

    try {
      provisionUsers();
    } catch (Exception e) {
      throw new RuntimeException("User provisioning failed", e);
    }

    ScenarioWithRequests trackerScenario = trackerProgramScenario();

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

    SetUp setUp =
        setUp(trackerScenario.scenario().injectClosed(buildClosedInjectionProfile()))
            .protocols(httpProtocolBuilder)
            .assertions(getAssertions(this.profile, trackerScenario));
    // Pauses model a user reading the screen between requests, see TrackerTest.
    if (this.profile == Profile.SMOKE || Boolean.getBoolean("disablePauses")) {
      setUp.disablePauses();
    }
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

  private HttpRequestActionBuilder login() {
    return http("Login")
        .post("/api/auth/login")
        .header("Content-Type", "application/json")
        .header("X-Request-ID", session -> nextRequestId("Login"))
        .body(StringBody("{\"username\":\"#{username}\",\"password\":\"#{password}\"}"))
        .check(status().is(200));
  }

  /** Filters on the given and the family name: EIR needs two attributes to search. */
  private String nameFilters(String operator, String given, String family) {
    return "&filter=%s:%s:%s&filter=%s:%s:%s"
        .formatted(this.givenName, operator, given, this.familyName, operator, family);
  }

  private ScenarioWithRequests trackerProgramScenario() {
    String getTEsUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc&page=1&pageSize=15&orgUnits="
            + this.orgUnit
            + "&orgUnitMode=SELECTED&program="
            + this.program
            + "&fields=:all,!relationships,programOwners[orgUnit,program]";

    String getTEsWithEnrollmentStatusUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc&page=1&pageSize=15&orgUnitMode=ACCESSIBLE&program="
            + this.program
            + nameFilters("ge", "A", "A")
            + "&enrollmentStatus=ACTIVE"
            + "&fields=:all,!relationships,programOwners[orgUnit,program]";

    String notFoundTEByName =
        "/api/tracker/trackedEntities?"
            + nameFilters("like", "notfoundname", "notfoundname")
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.program
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByName =
        "/api/tracker/trackedEntities?"
            + nameFilters("like", this.nameLike, this.nameLike)
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.program
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String notFoundTEByExactName =
        "/api/tracker/trackedEntities?"
            + nameFilters("eq", "notfoundname", "notfoundname")
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.program
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByExactName =
        "/api/tracker/trackedEntities?"
            + nameFilters("eq", this.givenEq, this.familyEq)
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + this.program
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    // The android-sdk's NewTrackedEntityInstanceFields.asSearchFields, see TrackerTest.
    String androidSearchTEsUrl =
        "/api/tracker/trackedEntities?page=1&pageSize=50&orgUnitMode=ACCESSIBLE&program="
            + this.program
            + nameFilters("like", this.nameLike, this.nameLike)
            + "&fields=trackedEntity,createdAt,updatedAt,createdAtClient,updatedAtClient,orgUnit,"
            + "trackedEntityType,geometry,deleted,attributes[attribute,value,createdAt,updatedAt],"
            + "programOwners";

    String searchBirthEvents =
        "/api/tracker/events?order=createdAt:desc&page=1"
            + "&pageSize=15&orgUnit="
            + this.orgUnit
            + "&orgUnitMode=SELECTED&program="
            + this.program
            + "&programStage="
            + this.birthStage
            + "&fields=*";

    String getTEsFromEvents =
        "/api/tracker/trackedEntities?pageSize=15&program="
            + this.program
            + "&trackedEntities=#{trackedEntityUids}&fields=trackedEntity,createdAt,attributes[attribute,value],programOwners[orgUnit],enrollments[enrollment,status,orgUnit,enrolledAt]";

    String singleTrackedEntityUrl =
        "/api/tracker/trackedEntities/#{trackedEntityUid}?program="
            + this.program
            + "&fields=programOwners[orgUnit],enrollments";
    String singleEnrollmentUrl =
        "/api/tracker/enrollments/#{enrollmentUid}?fields=enrollment,trackedEntity,program,status,orgUnit,enrolledAt,occurredAt,followUp,deleted,createdBy,updatedBy,updatedAt,geometry";

    String relationshipForTrackedEntityUrl =
        "/api/tracker/relationships?trackedEntity=#{trackedEntityUid}&paging=false&fields=relationship,relationshipType,createdAt,from[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]],to[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]]";

    String relationshipForEventUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    String eventUrl =
        "/api/tracker/events/#{eventUid}?fields=event,relationships[relationship,relationshipType,relationshipName,bidirectional,from[event[event,dataValues,occurredAt,scheduledAt,status,orgUnit,programStage,program]],to[event[event,dataValues,*,occurredAt,scheduledAt,status,orgUnit,programStage,program]]]";

    // Request names and p95 thresholds are TrackerTest's, so runs of both compare side by side.
    Request notFoundTeByNameWithLikeOperator =
        new Request(
            notFoundTEByName,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 116)),
            "Not found TE by name with like operator",
            "Get EIR TEs");
    Request notFoundTeByNameWithEqOperator =
        new Request(
            notFoundTEByExactName,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 35)),
            "Not found TE by name with eq operator",
            "Get EIR TEs");
    Request searchTeByNameWithLikeOperator =
        new Request(
            searchTEByName,
            new EnumMap<>(Map.of(Profile.SMOKE, 51, Profile.LOAD, 172)),
            "Search TE by name with like operator",
            "Get EIR TEs");
    Request searchTeByNameWithEqOperator =
        new Request(
            searchTEByExactName,
            new EnumMap<>(Map.of(Profile.SMOKE, 42, Profile.LOAD, 102)),
            "Search TE by name with eq operator",
            "Get EIR TEs");
    Request searchBirthEventsByStage =
        new Request(
            searchBirthEvents,
            new EnumMap<>(Map.of(Profile.SMOKE, 54, Profile.LOAD, 201)),
            "Search Birth events",
            "Get EIR TEs");
    Request getTrackedEntitiesForEvents =
        new Request(
            getTEsFromEvents,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 28)),
            "Get TEs from events",
            "Get EIR TEs");
    Request searchTEsAsAndroidClient =
        new Request(
            androidSearchTEsUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 29, Profile.LOAD, 151)),
            "Search TEs as Android client",
            "Get EIR TEs");
    Request getFirstPageOfTEs =
        new Request(
            getTEsUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 53, Profile.LOAD, 166)),
            "Get first page of TEs",
            "Get EIR TEs");
    Request getTEsWithEnrollmentStatus =
        new Request(
            getTEsWithEnrollmentStatusUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 62, Profile.LOAD, 188)),
            "Get TEs with enrollment status",
            "Get EIR TEs");
    Request getFirstTrackedEntity =
        new Request(
            singleTrackedEntityUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 41, Profile.LOAD, 96)),
            "Get first tracked entity",
            "Get EIR TEs",
            "Go to single enrollment");
    Request getFirstEnrollment =
        new Request(
            singleEnrollmentUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 46)),
            "Get first enrollment",
            "Get EIR TEs",
            "Go to single enrollment");
    Request getRelationshipsForTrackedEntity =
        new Request(
            relationshipForTrackedEntityUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 25)),
            "Get relationships for first tracked entity",
            "Get EIR TEs",
            "Go to single enrollment");
    Request getFirstEventFromEnrollment =
        new Request(
            eventUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 41, Profile.LOAD, 107)),
            "Get first event from enrollment",
            "Get EIR TEs",
            "Go to single enrollment",
            "Get one event");
    Request getRelationshipsForEvent =
        new Request(
            relationshipForEventUrl,
            new EnumMap<>(Map.of(Profile.SMOKE, 25, Profile.LOAD, 28)),
            "Get relationships for first event",
            "Get EIR TEs",
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
                group("Get EIR TEs")
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
                            // Android client performs an online TE search (attributes +
                            // programOwners, no enrollments)
                            .exec(
                                searchTEsAsAndroidClient
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                            .pause(1, 3) // user reads results
                            // User opens event working list (Birth details stage)
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
        scenario("EIR export")
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
            searchTEsAsAndroidClient,
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

  /** The closed injection profiles of TrackerTest. */
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

  private long injectionDurationSec() {
    return switch (this.profile) {
      case SMOKE -> throw new IllegalArgumentException("SMOKE profile uses repeat, not during");
      case LOAD -> this.rampDurationSec + this.durationSec;
      case CAPACITY ->
          (long) this.steps * this.durationSec + (long) (this.steps - 1) * this.rampDurationSec;
    };
  }

  private ChainBuilder loopForProfile(ChainBuilder chain) {
    if (this.profile == Profile.SMOKE) {
      return exec(repeat(this.repeat).on(chain));
    }
    return exec(during(Duration.ofSeconds(injectionDurationSec())).on(chain));
  }

  private List<Assertion> getAssertions(Profile profile, ScenarioWithRequests scenario) {
    return Stream.concat(
            Stream.of(forAll().successfulRequests().percent().gte(100d)),
            scenario.requests().stream().flatMap(r -> r.assertion(profile).stream()))
        .toList();
  }

  /** A unique {@code X-Request-ID} per request, see TrackerTest. */
  private static String nextRequestId(String name) {
    String id = "g-" + REQUEST_COUNTER.incrementAndGet();
    logger.debug("X-Request-ID: {} -> {}", id, name);
    return id;
  }
}
