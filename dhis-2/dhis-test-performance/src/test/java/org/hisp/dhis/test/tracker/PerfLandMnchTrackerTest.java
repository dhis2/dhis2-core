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
import static io.gatling.javaapi.core.CoreDsl.csv;
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.doIf;
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
import static io.gatling.javaapi.http.HttpDsl.flushCookieJar;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.FeederBuilder;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Experimental copy of {@link TrackerTest} for the synthetic Perf Land MNCH database (DHIS2-21406),
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
 *   <li>Each iteration takes the next case of {@code perfland-mnch/cases.csv}: a data clerk of a
 *       facility of district QP1613 and the names she searches for. A clerk captures in her
 *       facility and searches in her district. The test logs in as the clerk of the case, dbgen's
 *       users, instead of replicating one.
 *   <li>The cases cover facilities by their number of EIR children (small, median, large, largest;
 *       large is just above 1,000, where the first page's plan changes to its slowest), as the
 *       query plans differ between them. A request whose plan depends on the facility is named
 *       after its band, such as "Get first page of TEs [small]", and has a p95 threshold per band:
 *       one p95 over all bands is the slowest band's only. The names are common ones of the clerk's
 *       children: how many records a name matches changes the time of a search, not its plan.
 *   <li>EIR needs two attributes to search outside the capture scope, so every name search filters
 *       on the given and the family name, where {@link TrackerTest} filters on the first name. The
 *       like search takes the first 3 letters of each name, as a user types them
 *       (minCharactersToSearch is 3).
 *   <li>A child is linked to its mother by a Mother and child relationship when she is enrolled in
 *       ANC, so the relationships of a tracked entity are not always empty. Events have none.
 *   <li>Every EIR child has a Unique System Identifier (EPI), a unique attribute. The test also
 *       searches tracked entities and Birth events by the EPI of the newest child of the clerk's
 *       facility, which {@link TrackerTest} does not.
 * </ul>
 *
 * <p>The cases are bound to the records of the {@code perfland-mnch} dumps, which are the same for
 * every DHIS2 version. The eq pair and the like letters of a case match a child of the clerk's
 * facility among the first 5 matches in the district, so DHIS2 2.42, which drops what the clerk
 * does not own after paging, returns her too. {@code -Dcases} reads another file.
 *
 * <p>The thresholds of requests without a band are {@link TrackerTest}'s, calibrated on Sierra
 * Leone. The banded ones and the searches by EPI have none yet.
 *
 * <p>The UIDs of the program, stage and attributes are constants, as {@link TrackerTest}'s
 * attributes: the cases' values belong to them. Profiles and their parameters are those of {@link
 * TrackerTest}.
 */
public class PerfLandMnchTrackerTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(PerfLandMnchTrackerTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  private static final String PROGRAM = "SSLpOM0r1U7"; // Electronic Immunization Registry
  private static final String BIRTH_STAGE = "RcbCl5ww8XY"; // Birth details
  private static final String GIVEN_NAME = "sB1IHYu2xQT"; // GEN - Given name
  private static final String FAMILY_NAME = "ENRjVGxVL6l"; // GEN - Family name
  private static final String UNIQUE_ID = "KSr2yTdu1AI"; // Unique System Identifier (EPI)
  // Of every dbgen user. Not a property: run-simulation.sh passes the admin's as -Dpassword.
  private static final String PASSWORD = "Perf-Test-2026!";

  private final Profile profile;
  private final String instance;
  private final FeederBuilder<String> cases;
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

  /** The column of a case a request's plan depends on, and its values. */
  private enum Band {
    NONE(null),
    FACILITY("facilityBand", "small", "median", "large", "largest");

    private final String column;
    private final List<String> values;

    Band(String column, String... values) {
      this.column = column;
      this.values = List.of(values);
    }
  }

  /**
   * A request, named {@code name [band]} if it has a band. {@code p95Thresholds} maps a band's
   * value, or {@code ""} for {@link Band#NONE}, to the p95 thresholds of each profile. A missing
   * threshold asserts nothing.
   */
  private record Request(
      String url,
      Band band,
      Map<String, Map<Profile, Integer>> p95Thresholds,
      String name,
      String... groups) {
    static Request of(
        String url, Map<Profile, Integer> p95Thresholds, String name, String... groups) {
      return new Request(url, Band.NONE, Map.of("", p95Thresholds), name, groups);
    }

    static Request banded(
        String url,
        Band band,
        Map<String, Map<Profile, Integer>> p95Thresholds,
        String name,
        String... groups) {
      return new Request(url, band, p95Thresholds, name, groups);
    }

    private String name(Session session) {
      return band == Band.NONE ? name : nameOf(session.getString(band.column));
    }

    private String nameOf(String value) {
      return band == Band.NONE ? name : name + " [" + value + "]";
    }

    HttpRequestActionBuilder action() {
      return http(this::name)
          .get(url)
          .header("X-Request-ID", session -> nextRequestId(name(session)));
    }

    Stream<Assertion> assertions(Profile profile) {
      List<String> values = band == Band.NONE ? List.of("") : band.values;
      return values.stream()
          .flatMap(
              value -> {
                Integer threshold = p95Thresholds.getOrDefault(value, Map.of()).get(profile);
                if (threshold == null) {
                  return Stream.empty();
                }
                String[] parts = Arrays.copyOf(groups, groups.length + 1);
                parts[groups.length] = nameOf(value);
                return Stream.of(details(parts).responseTime().percentile(95).lte(threshold));
              });
    }
  }

  private record ScenarioWithRequests(ScenarioBuilder scenario, List<Request> requests) {}

  public PerfLandMnchTrackerTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");
    // Circular: every iteration takes the next case, so each band gets the same share of a run.
    this.cases = csv(System.getProperty("cases", "perfland-mnch/cases.csv")).circular();

    record ProfileDefaults(
        int concurrentUsers, int repeat, int rampDurationSec, int durationSec, int steps) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          // 120 is a multiple of the 4 cases: 30 iterations per facility band.
          case SMOKE -> new ProfileDefaults(1, 120, 1, 1, 1);
          case LOAD -> new ProfileDefaults(4, 1, 15, 180, 1);
          case CAPACITY -> new ProfileDefaults(8, 1, 10, 30, 4);
        };
    this.concurrentUsers = Integer.getInteger("concurrentUsers", defaults.concurrentUsers());
    this.repeat = Integer.getInteger("repeat", defaults.repeat());
    this.rampDurationSec = Integer.getInteger("rampDurationSec", defaults.rampDurationSec());
    this.durationSec = Integer.getInteger("durationSec", defaults.durationSec());
    this.steps = Integer.getInteger("steps", defaults.steps());

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

  /**
   * Logs in as the clerk of the case fed last, unless the session is already hers: with one case
   * per clerk, every iteration logs in, except when a csv of {@code -Dcases} repeats a clerk.
   */
  private ChainBuilder loginAsCaseUser() {
    return doIf(session -> !session.getString("username").equals(session.getString("loggedIn")))
        .then(
            exec(flushCookieJar())
                .exec(
                    http("Login")
                        .post("/api/auth/login")
                        .header("Content-Type", "application/json")
                        .header("X-Request-ID", session -> nextRequestId("Login"))
                        .body(
                            StringBody(
                                "{\"username\":\"#{username}\",\"password\":\"%s\"}"
                                    .formatted(PASSWORD)))
                        .check(status().is(200)))
                .exitHereIfFailed()
                .exec(session -> session.set("loggedIn", session.getString("username"))));
  }

  /** Filters on the given and the family name: EIR needs two attributes to search. */
  private String nameFilters(String operator, String given, String family) {
    return "&filter=%s:%s:%s&filter=%s:%s:%s"
        .formatted(GIVEN_NAME, operator, given, FAMILY_NAME, operator, family);
  }

  private ScenarioWithRequests trackerProgramScenario() {
    String getTEsUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc&page=1&pageSize=15&orgUnits="
            + "#{orgUnit}"
            + "&orgUnitMode=SELECTED&program="
            + PROGRAM
            + "&fields=:all,!relationships,programOwners[orgUnit,program]";

    String getTEsWithEnrollmentStatusUrl =
        "/api/tracker/trackedEntities?"
            + "order=createdAt:desc&page=1&pageSize=15&orgUnitMode=ACCESSIBLE&program="
            + PROGRAM
            + nameFilters("ge", "A", "A")
            + "&enrollmentStatus=ACTIVE"
            + "&fields=:all,!relationships,programOwners[orgUnit,program]";

    String notFoundTEByName =
        "/api/tracker/trackedEntities?"
            + nameFilters("like", "notfoundname", "notfoundname")
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + PROGRAM
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByName =
        "/api/tracker/trackedEntities?"
            + nameFilters("like", "#{givenLike}", "#{familyLike}")
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + PROGRAM
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String notFoundTEByExactName =
        "/api/tracker/trackedEntities?"
            + nameFilters("eq", "notfoundname", "notfoundname")
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + PROGRAM
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    String searchTEByExactName =
        "/api/tracker/trackedEntities?"
            + nameFilters("eq", "#{givenEq}", "#{familyEq}")
            + "&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + PROGRAM
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    // The android-sdk's NewTrackedEntityInstanceFields.asSearchFields, see TrackerTest.
    String androidSearchTEsUrl =
        "/api/tracker/trackedEntities?page=1&pageSize=50&orgUnitMode=ACCESSIBLE&program="
            + PROGRAM
            + nameFilters("like", "#{givenLike}", "#{familyLike}")
            + "&fields=trackedEntity,createdAt,updatedAt,createdAtClient,updatedAtClient,orgUnit,"
            + "trackedEntityType,geometry,deleted,attributes[attribute,value,createdAt,updatedAt],"
            + "programOwners";

    String searchTEByUniqueId =
        "/api/tracker/trackedEntities?filter="
            + UNIQUE_ID
            + ":eq:#{epi}&fields=attributes,enrollments,trackedEntity,orgUnit&program="
            + PROGRAM
            + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";

    // Events filtered by a tracked entity attribute, as #26109 measured them.
    String searchBirthEventsByUniqueIdUrl =
        "/api/tracker/events?program="
            + PROGRAM
            + "&programStage="
            + BIRTH_STAGE
            + "&filterAttributes="
            + UNIQUE_ID
            + ":eq:#{epi}&fields=*";

    String searchBirthEvents =
        "/api/tracker/events?order=createdAt:desc&page=1"
            + "&pageSize=15&orgUnit="
            + "#{orgUnit}"
            + "&orgUnitMode=SELECTED&program="
            + PROGRAM
            + "&programStage="
            + BIRTH_STAGE
            + "&fields=*";

    String getTEsFromEvents =
        "/api/tracker/trackedEntities?pageSize=15&program="
            + PROGRAM
            + "&trackedEntities=#{trackedEntityUids}&fields=trackedEntity,createdAt,attributes[attribute,value],programOwners[orgUnit],enrollments[enrollment,status,orgUnit,enrolledAt]";

    String singleTrackedEntityUrl =
        "/api/tracker/trackedEntities/#{trackedEntityUid}?program="
            + PROGRAM
            + "&fields=programOwners[orgUnit],enrollments";
    String singleEnrollmentUrl =
        "/api/tracker/enrollments/#{enrollmentUid}?fields=enrollment,trackedEntity,program,status,orgUnit,enrolledAt,occurredAt,followUp,deleted,createdBy,updatedBy,updatedAt,geometry";

    String relationshipForTrackedEntityUrl =
        "/api/tracker/relationships?trackedEntity=#{trackedEntityUid}&paging=false&fields=relationship,relationshipType,createdAt,from[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]],to[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]]";

    String relationshipForEventUrl =
        "/api/tracker/relationships?event=#{eventUid}&fields=from,to,relationshipType,relationship,createdAt";

    String eventUrl =
        "/api/tracker/events/#{eventUid}?fields=event,relationships[relationship,relationshipType,relationshipName,bidirectional,from[event[event,dataValues,occurredAt,scheduledAt,status,orgUnit,programStage,program]],to[event[event,dataValues,*,occurredAt,scheduledAt,status,orgUnit,programStage,program]]]";

    // Request names and the p95 thresholds of requests without a band are TrackerTest's, so runs
    // of both compare side by side. The banded ones are to calibrate on this database.
    Request notFoundTeByNameWithLikeOperator =
        Request.of(
            notFoundTEByName,
            Map.of(Profile.SMOKE, 25, Profile.LOAD, 116),
            "Not found TE by name with like operator",
            "Get EIR TEs");
    Request notFoundTeByNameWithEqOperator =
        Request.of(
            notFoundTEByExactName,
            Map.of(Profile.SMOKE, 25, Profile.LOAD, 35),
            "Not found TE by name with eq operator",
            "Get EIR TEs");
    Request searchTeByNameWithLikeOperator =
        Request.of(
            searchTEByName,
            Map.of(Profile.SMOKE, 51, Profile.LOAD, 172),
            "Search TE by name with like operator",
            "Get EIR TEs");
    Request searchTeByNameWithEqOperator =
        Request.of(
            searchTEByExactName,
            Map.of(Profile.SMOKE, 42, Profile.LOAD, 102),
            "Search TE by name with eq operator",
            "Get EIR TEs");
    Request searchTeByUniqueId =
        Request.of(searchTEByUniqueId, Map.of(), "Search TE by unique identifier", "Get EIR TEs");
    Request searchBirthEventsByUniqueId =
        Request.of(
            searchBirthEventsByUniqueIdUrl,
            Map.of(),
            "Search Birth events by unique identifier",
            "Get EIR TEs");
    Request searchBirthEventsByStage =
        Request.banded(
            searchBirthEvents, Band.FACILITY, Map.of(), "Search Birth events", "Get EIR TEs");
    Request getTrackedEntitiesForEvents =
        Request.of(
            getTEsFromEvents,
            Map.of(Profile.SMOKE, 25, Profile.LOAD, 28),
            "Get TEs from events",
            "Get EIR TEs");
    Request searchTEsAsAndroidClient =
        Request.of(
            androidSearchTEsUrl,
            Map.of(Profile.SMOKE, 29, Profile.LOAD, 151),
            "Search TEs as Android client",
            "Get EIR TEs");
    Request getFirstPageOfTEs =
        Request.banded(getTEsUrl, Band.FACILITY, Map.of(), "Get first page of TEs", "Get EIR TEs");
    Request getTEsWithEnrollmentStatus =
        Request.banded(
            getTEsWithEnrollmentStatusUrl,
            Band.FACILITY,
            Map.of(),
            "Get TEs with enrollment status",
            "Get EIR TEs");
    Request getFirstTrackedEntity =
        Request.of(
            singleTrackedEntityUrl,
            Map.of(Profile.SMOKE, 41, Profile.LOAD, 96),
            "Get first tracked entity",
            "Get EIR TEs",
            "Go to single enrollment");
    Request getFirstEnrollment =
        Request.of(
            singleEnrollmentUrl,
            Map.of(Profile.SMOKE, 25, Profile.LOAD, 46),
            "Get first enrollment",
            "Get EIR TEs",
            "Go to single enrollment");
    Request getRelationshipsForTrackedEntity =
        Request.of(
            relationshipForTrackedEntityUrl,
            Map.of(Profile.SMOKE, 25, Profile.LOAD, 25),
            "Get relationships for first tracked entity",
            "Get EIR TEs",
            "Go to single enrollment");
    Request getFirstEventFromEnrollment =
        Request.of(
            eventUrl,
            Map.of(Profile.SMOKE, 41, Profile.LOAD, 107),
            "Get first event from enrollment",
            "Get EIR TEs",
            "Go to single enrollment",
            "Get one event");
    Request getRelationshipsForEvent =
        Request.of(
            relationshipForEventUrl,
            Map.of(Profile.SMOKE, 25, Profile.LOAD, 28),
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
                            // User searches by the child's EPI, then lists its Birth event by it
                            .exec(
                                searchTeByUniqueId
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().is(1)))
                            .pause(1, 3) // user reads the result
                            .exec(
                                searchBirthEventsByUniqueId
                                    .action()
                                    .check(jsonPath("$.events[*]").count().is(1)))
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
                                                    // A child is linked to its mother when she is
                                                    // enrolled in ANC, so it has none or one.
                                                    .exec(
                                                        getRelationshipsForTrackedEntity
                                                            .action()
                                                            .check(
                                                                jsonPath("$.relationships")
                                                                    .exists()))
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
            .exec(loopForProfile(feed(this.cases).exec(loginAsCaseUser()).exec(exportRequests)));

    return new ScenarioWithRequests(
        scenarioBuilder,
        List.of(
            notFoundTeByNameWithLikeOperator,
            notFoundTeByNameWithEqOperator,
            searchTeByNameWithLikeOperator,
            searchTeByNameWithEqOperator,
            searchTEsAsAndroidClient,
            searchTeByUniqueId,
            searchBirthEventsByUniqueId,
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
            scenario.requests().stream().flatMap(r -> r.assertions(profile)))
        .toList();
  }

  /** A unique {@code X-Request-ID} per request, see TrackerTest. */
  private static String nextRequestId(String name) {
    String id = "g-" + REQUEST_COUNTER.incrementAndGet();
    logger.debug("X-Request-ID: {} -> {}", id, name);
    return id;
  }
}
