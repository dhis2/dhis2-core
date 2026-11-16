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
 * Tracker exports of the WHO Electronic Immunization Registry (EIR) on the synthetic Perf Land MNCH
 * database (DHIS2-21406), as data clerks of district QP1613 send them: they search children by name
 * and by their unique identifier, list them (first page, enrollment status, Birth events), and open
 * one with its enrollment, event and relationships. The database also holds the WHO Antenatal Care
 * Registry (ANC), which the test reads only as the mother at the other end of a child's
 * relationship.
 *
 * <p>The requests are those of {@link TrackerTest}'s export scenario for its tracker program (Child
 * Programme), which has the shape of EIR: a child with a non-repeatable birth stage. The event
 * program scenario and the imports are left out; the database has no single-event program, and
 * import payloads for ANC and EIR do not exist yet.
 *
 * <p>Differences to {@link TrackerTest}, for the data this database holds:
 *
 * <ul>
 *   <li>Each iteration takes the next child of {@code perfland-mnch/children.csv}: a child of a
 *       facility of district QP1613 and the data clerk of that facility. A clerk captures in her
 *       facility and searches in her district. The test logs in as the child's clerk, dbgen's
 *       users, instead of replicating one. She searches the child by name and EPI and opens it, as
 *       clerks work on different children all day: 60 per clerk, picked at random with a fixed seed
 *       by {@code perfland-mnch/children.sql}, so a smoke run opens none twice. The rows alternate
 *       the clerks, and the circular feeder gives every run, baseline and candidate, the same
 *       order.
 *   <li>The clerks' facilities differ by their number of EIR children (small, median, large,
 *       largest; large is in the middle of the first page's slowest plan, about 1,000 to 1,400), as
 *       the query plans differ between them. Each facility's number of children is far from the
 *       sizes at which a request switches to another plan, so the statistics that ANALYZE samples
 *       before each run don't flip its plan. In smoke, a request whose plan depends on the facility
 *       is named after its band, such as "Get first page of TEs [small]", and has a p95 threshold
 *       per band: one p95 over all bands is about the slowest band's p80, so a faster band could
 *       get several times slower unnoticed. Load has about 9 iterations per band, too few for a p95
 *       each: it names and asserts these requests over all bands, for what concurrency adds. How
 *       many records a name matches changes the time of a search, not its plan, so the names don't
 *       get bands.
 *   <li>EIR needs two attributes to search outside the capture scope, so every name search filters
 *       on the given and the family name, where {@link TrackerTest} filters on the first name. The
 *       like search takes the first 3 letters of each name, as a user types them
 *       (minCharactersToSearch is 3).
 *   <li>A child is linked to its mother by a Mother and child relationship when she is enrolled in
 *       ANC, so the relationships of a tracked entity are not always empty. Events have none.
 *   <li>Every EIR child has a Unique System Identifier (EPI), a unique attribute. The test also
 *       searches tracked entities and Birth events by the EPI of the child, which {@link
 *       TrackerTest} does not. The tracked entity search is Capture's.
 *   <li>The test opens the first child of the working list's first page, as {@link TrackerTest}
 *       does, and the child it searched by its EPI, which can be of any age: "Get searched tracked
 *       entity" and the other requests of "Go to searched enrollment".
 *   <li>{@link TrackerTest}'s lists are those of Capture before v106.2.8 ({@code fields=*} and
 *       {@code :all}), which its CSV download still sends. They stay, as they read relationships
 *       and notes that Capture's lists no longer do, and fixes such as #26186 were measured with
 *       them. Next to them the test sends what Capture 107.9.1 and the android-sdk 1.15.0 send,
 *       named "... as Capture" and "Sync TEs as Android client": the working lists, the dashboard's
 *       tracked entity, and a page of a first sync, which reads the relationships of every tracked
 *       entity, enrollment and event.
 * </ul>
 *
 * <p>The children are bound to the records of the {@code perfland-mnch} dumps, which are the same
 * for every DHIS2 version: re-generate them with {@code children.sql} when the dumps change. The eq
 * pair and the like letters of a child match a child of the clerk's facility among the first 5
 * matches in the district, the page of these searches, so DHIS2 2.42, which drops what the clerk
 * does not own after paging, returns her too. {@code -Dchildren} reads another file.
 *
 * <p>The thresholds of requests without a band are {@link TrackerTest}'s, calibrated on Sierra
 * Leone. The banded ones, the searches by EPI and the Capture and Android ones have none yet.
 *
 * <p>The UIDs of the program, stage and attributes are constants, as {@link TrackerTest}'s
 * attributes: the values of {@code children.csv} belong to them. Profiles and their parameters are
 * those of {@link TrackerTest}.
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

  // Capture 107.9.1's LISTING_FIELDS of its working lists.
  private static final String CAPTURE_TE_LIST_FIELDS =
      "trackedEntity,createdAt,inactive,attributes[attribute,value],programOwners[orgUnit,program]";
  private static final String CAPTURE_EVENT_LIST_FIELDS =
      "event,status,program,enrollment,trackedEntity,occurredAt,scheduledAt,orgUnit,"
          + "assignedUser[uid,username,firstName,surname],dataValues[dataElement,value]";

  // The android-sdk 1.15.0's NewTrackedEntityInstanceFields.allFields, expanded.
  private static final String ANDROID_RELATIONSHIP_FIELDS =
      "relationship,relationshipName,relationshipType,createdAt,updatedAt,"
          + "from[trackedEntity[trackedEntity],enrollment[enrollment],event[event]],"
          + "to[trackedEntity[trackedEntity],enrollment[enrollment],event[event]]";
  private static final String ANDROID_NOTE_FIELDS = "notes[note,value,storedAt,storedBy]";
  private static final String ANDROID_EVENT_FIELDS =
      "event,enrollment,createdAt,updatedAt,createdAtClient,updatedAtClient,status,geometry,"
          + "program,programStage,orgUnit,occurredAt,completedAt,scheduledAt,deleted,"
          + "attributeOptionCombo,assignedUser,completedBy,"
          + ANDROID_NOTE_FIELDS
          + ",relationships["
          + ANDROID_RELATIONSHIP_FIELDS
          + "],dataValues[dataElement,createdBy,value,createdAt,updatedAt,providedElsewhere]";
  private static final String ANDROID_ENROLLMENT_FIELDS =
      "enrollment,createdAt,updatedAt,createdAtClient,updatedAtClient,orgUnit,program,enrolledAt,"
          + "occurredAt,completedAt,followUp,status,deleted,trackedEntity,geometry,"
          + "attributeOptionCombo,events["
          + ANDROID_EVENT_FIELDS
          + "],"
          + ANDROID_NOTE_FIELDS
          + ",relationships["
          + ANDROID_RELATIONSHIP_FIELDS
          + "]";
  private static final String ANDROID_TE_ALL_FIELDS =
      "trackedEntity,createdAt,updatedAt,createdAtClient,updatedAtClient,orgUnit,"
          + "trackedEntityType,geometry,deleted,attributes[attribute,value,createdAt,updatedAt],"
          + "relationships["
          + ANDROID_RELATIONSHIP_FIELDS
          + "],enrollments["
          + ANDROID_ENROLLMENT_FIELDS
          + "],programOwners";

  private final Profile profile;
  private final String instance;
  private final FeederBuilder<String> children;
  private final int concurrentUsers;
  private final int repeat;
  private final int durationSec;
  private final int rampDurationSec;
  private final int steps;

  public PerfLandMnchTrackerTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");
    // Circular: every iteration takes the next child, so each band gets the same share of a run.
    this.children = csv(System.getProperty("children", "perfland-mnch/children.csv")).circular();

    record ProfileDefaults(
        int concurrentUsers, int repeat, int rampDurationSec, int durationSec, int steps) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          // 240 children: 60 iterations per facility band instead of 30. A band's p95 then sits
          // below its 3 slowest samples instead of its 2 slowest, so one slow sample moves it less.
          case SMOKE -> new ProfileDefaults(1, 240, 1, 1, 1);
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
            // Capture 107.9.1's fields
            + "&fields=attributes,enrollments,trackedEntity,orgUnit,inactive&program="
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

    // Capture's search by a unique attribute, without fields.
    String searchTEByUniqueId =
        "/api/tracker/trackedEntities?filter="
            + UNIQUE_ID
            + ":eq:#{epi}&program="
            + PROGRAM
            + "&orgUnitMode=ACCESSIBLE";

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

    // Capture's working lists of a facility, since v106.2.8.
    String captureTEsUrl =
        "/api/tracker/trackedEntities?order=createdAt:desc&page=1&pageSize=15&orgUnits="
            + "#{orgUnit}"
            + "&orgUnitMode=SELECTED&program="
            + PROGRAM
            + "&fields="
            + CAPTURE_TE_LIST_FIELDS;
    String captureActiveTEsUrl = captureTEsUrl + "&enrollmentStatus=ACTIVE";
    String captureBirthEventsUrl =
        "/api/tracker/events?enrollmentStatus=ACTIVE&order=createdAt:desc&page=1&pageSize=15"
            + "&orgUnit=#{orgUnit}&orgUnitMode=SELECTED&program="
            + PROGRAM
            + "&programStage="
            + BIRTH_STAGE
            + "&fields="
            + CAPTURE_EVENT_LIST_FIELDS;
    String captureBirthEventsByUniqueIdUrl =
        captureBirthEventsUrl + "&filterAttributes=" + UNIQUE_ID + ":eq:#{epi}";
    String captureTEsFromEventsUrl =
        "/api/tracker/trackedEntities?program="
            + PROGRAM
            + "&pageSize=15&trackedEntities=#{captureTrackedEntityUids}"
            + "&fields=trackedEntity,createdAt,attributes[attribute,value],programOwners[orgUnit],"
            + "enrollments[enrollment]";

    // A page of the android-sdk's first sync of a facility: TrackerExporterNetworkHandlerImpl
    // with NewTrackedEntityInstanceFields.allFields.
    String androidSyncTEsUrl =
        "/api/tracker/trackedEntities?orgUnits=#{orgUnit}&orgUnitMode=SELECTED&program="
            + PROGRAM
            + "&order=createdAt:desc&paging=true&page=1&pageSize=50&includeDeleted=true&fields="
            + ANDROID_TE_ALL_FIELDS;

    // Request names and the p95 thresholds of requests without a band are TrackerTest's, so runs
    // of both compare side by side. The banded ones are to calibrate on this database.
    // Bands only in smoke: 60 iterations per band (1 user, 240 iterations), against about 9 in
    // load, too few for a p95 per band.
    Band facility = this.profile == Profile.SMOKE ? Band.FACILITY : Band.NONE;
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
        Request.banded(searchBirthEvents, facility, Map.of(), "Search Birth events", "Get EIR TEs");
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
        Request.banded(getTEsUrl, facility, Map.of(), "Get first page of TEs", "Get EIR TEs");
    Request getTEsWithEnrollmentStatus =
        Request.banded(
            getTEsWithEnrollmentStatusUrl,
            facility,
            Map.of(),
            "Get TEs with enrollment status",
            "Get EIR TEs");
    // The user opens a child from the working list, the first of its first page as TrackerTest
    // does, and the child she searched by its EPI, which can be of any age.
    OpenRequests openListed =
        OpenRequests.of(
            "listed",
            "Go to single enrollment",
            List.of(
                "Get first tracked entity",
                "Get first tracked entity as Capture",
                "Get first enrollment",
                "Get relationships for first tracked entity",
                "Get first event from enrollment",
                "Get relationships for first event"),
            List.of(
                Map.of(Profile.SMOKE, 41, Profile.LOAD, 96),
                Map.of(),
                Map.of(Profile.SMOKE, 25, Profile.LOAD, 46),
                Map.of(Profile.SMOKE, 25, Profile.LOAD, 25),
                Map.of(Profile.SMOKE, 41, Profile.LOAD, 107),
                Map.of(Profile.SMOKE, 25, Profile.LOAD, 28)));
    OpenRequests openSearched =
        OpenRequests.of(
            "searched",
            "Go to searched enrollment",
            List.of(
                "Get searched tracked entity",
                "Get searched tracked entity as Capture",
                "Get searched enrollment",
                "Get relationships for searched tracked entity",
                "Get first event from searched enrollment",
                "Get relationships for first event of searched enrollment"),
            List.of(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of()));

    Request searchTEsAsCapture =
        Request.banded(
            captureTEsUrl, facility, Map.of(), "Get first page of TEs as Capture", "Get EIR TEs");
    Request searchActiveTEsAsCapture =
        Request.banded(
            captureActiveTEsUrl,
            facility,
            Map.of(),
            "Get TEs with enrollment status as Capture",
            "Get EIR TEs");
    Request searchBirthEventsAsCapture =
        Request.banded(
            captureBirthEventsUrl,
            facility,
            Map.of(),
            "Search Birth events as Capture",
            "Get EIR TEs");
    Request searchBirthEventsByUniqueIdAsCapture =
        Request.of(
            captureBirthEventsByUniqueIdUrl,
            Map.of(),
            "Search Birth events by unique identifier as Capture",
            "Get EIR TEs");
    Request getTrackedEntitiesForEventsAsCapture =
        Request.of(
            captureTEsFromEventsUrl, Map.of(), "Get TEs from events as Capture", "Get EIR TEs");
    Request syncTEsAsAndroidClient =
        Request.banded(
            androidSyncTEsUrl, facility, Map.of(), "Sync TEs as Android client", "Get EIR TEs");

    var exportRequests =
        exec(session ->
                session
                    .remove("trackedEntityUids")
                    .remove("captureTrackedEntityUids")
                    .removeAll(openListed.keys())
                    .removeAll(openSearched.keys()))
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
                            // Android client downloads the first page of its facility
                            .exec(
                                syncTEsAsAndroidClient
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                            // User searches by the child's EPI, then lists its Birth event by it
                            .exec(
                                searchTeByUniqueId
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().is(1))
                                    .check(
                                        jsonPath("$.trackedEntities[0].trackedEntity")
                                            .isEL("#{trackedEntity}")
                                            .saveAs("searched")))
                            // Capture opens the dashboard of a unique match itself
                            .doIf(session -> session.contains("searched"))
                            .then(openSearched.chain())
                            .pause(1, 3) // user goes back to the search
                            .exec(
                                searchBirthEventsByUniqueId
                                    .action()
                                    .check(jsonPath("$.events[*]").count().is(1)))
                            .exec(
                                searchBirthEventsByUniqueIdAsCapture
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
                            .exec(
                                searchBirthEventsAsCapture
                                    .action()
                                    .check(jsonPath("$.events[*]").count().gte(1))
                                    .check(
                                        jsonPath("$.events[*].trackedEntity")
                                            .findAll()
                                            .transform(
                                                list ->
                                                    String.join(
                                                        ",", list.stream().distinct().toList()))
                                            .saveAs("captureTrackedEntityUids")))
                            .doIf(session -> session.contains("captureTrackedEntityUids"))
                            .then(
                                exec(
                                    getTrackedEntitiesForEventsAsCapture
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
                                            .saveAs("listed")))
                            .exec(
                                searchTEsAsCapture
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                            .pause(1, 3) // user reads TE list, picks one
                            .doIf(session -> session.contains("listed"))
                            .then(openListed.chain())
                            .pause(1, 3) // user goes back to list
                            // User opens filtered TE list (enrollment status)
                            .exec(
                                getTEsWithEnrollmentStatus
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                            .exec(
                                searchActiveTEsAsCapture
                                    .action()
                                    .check(jsonPath("$.trackedEntities[*]").count().gte(1)))));

    ScenarioBuilder scenarioBuilder =
        scenario("EIR export")
            .exec(loopForProfile(feed(this.children).exec(loginAsClerk()).exec(exportRequests)));

    return new ScenarioWithRequests(
        scenarioBuilder,
        Stream.of(
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
                    searchTEsAsCapture,
                    searchActiveTEsAsCapture,
                    searchBirthEventsAsCapture,
                    searchBirthEventsByUniqueIdAsCapture,
                    getTrackedEntitiesForEventsAsCapture,
                    syncTEsAsAndroidClient),
                openListed.requests(),
                openSearched.requests())
            .flatMap(List::stream)
            .toList());
  }

  /** Filters on the given and the family name: EIR needs two attributes to search. */
  private String nameFilters(String operator, String given, String family) {
    return "&filter=%s:%s:%s&filter=%s:%s:%s"
        .formatted(GIVEN_NAME, operator, given, FAMILY_NAME, operator, family);
  }

  private ChainBuilder loopForProfile(ChainBuilder chain) {
    if (this.profile == Profile.SMOKE) {
      return exec(repeat(this.repeat).on(chain));
    }
    return exec(during(Duration.ofSeconds(injectionDurationSec())).on(chain));
  }

  private long injectionDurationSec() {
    return switch (this.profile) {
      case SMOKE -> throw new IllegalArgumentException("SMOKE profile uses repeat, not during");
      case LOAD -> this.rampDurationSec + this.durationSec;
      case CAPACITY ->
          (long) this.steps * this.durationSec + (long) (this.steps - 1) * this.rampDurationSec;
    };
  }

  /**
   * Logs in as the clerk of the child fed last, unless the session is already hers: the rows
   * alternate the clerks, so every iteration logs in, except when a csv of {@code -Dchildren}
   * repeats a clerk in consecutive rows.
   */
  private ChainBuilder loginAsClerk() {
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

  /** The column of {@code children.csv} a request's plan depends on, and its values. */
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
   * value, or {@code ""} for {@link Band#NONE}, to the p95 thresholds of each profile: a banded
   * request's load threshold is under {@code ""}, as it has no band in load. A missing threshold
   * asserts nothing.
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

  /**
   * The requests Capture sends when a user opens a child: the tracked entity, its enrollment and
   * relationships, then the enrollment's first event and the event's relationships. The session
   * holds the tracked entity's UID under {@code key}, {@link #chain()} saves the enrollment's and
   * the event's under {@code key + "Enrollment"} and {@code key + "Event"}.
   */
  private record OpenRequests(
      String key,
      String groupName,
      Request trackedEntity,
      Request trackedEntityAsCapture,
      Request enrollment,
      Request relationshipsForTrackedEntity,
      Request event,
      Request relationshipsForEvent) {

    /**
     * {@code names} and {@code p95Thresholds} follow the order of the requests: the tracked entity,
     * the tracked entity as Capture, the enrollment, the tracked entity's relationships, the event
     * and the event's relationships.
     */
    static OpenRequests of(
        String key, String group, List<String> names, List<Map<Profile, Integer>> p95Thresholds) {
      String trackedEntityUrl = "/api/tracker/trackedEntities/#{" + key + "}?program=" + PROGRAM;
      String event = "#{" + key + "Event}";
      String[] groups = {"Get EIR TEs", group};
      String[] eventGroups = {"Get EIR TEs", group, "Get one event"};
      return new OpenRequests(
          key,
          group,
          Request.of(
              trackedEntityUrl + "&fields=programOwners[orgUnit],enrollments",
              p95Thresholds.get(0),
              names.get(0),
              groups),
          // The heaviest of the requests Capture sends for its enrollment dashboard.
          Request.of(
              trackedEntityUrl
                  + "&fields=enrollments[*,!attributes],attributes,inactive,programOwners[program,orgUnit]",
              p95Thresholds.get(1),
              names.get(1),
              groups),
          Request.of(
              "/api/tracker/enrollments/#{"
                  + key
                  + "Enrollment}?fields=enrollment,trackedEntity,program,status,orgUnit,enrolledAt,occurredAt,followUp,deleted,createdBy,updatedBy,updatedAt,geometry",
              p95Thresholds.get(2),
              names.get(2),
              groups),
          Request.of(
              "/api/tracker/relationships?trackedEntity=#{"
                  + key
                  + "}&paging=false&fields=relationship,relationshipType,createdAt,from[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]],to[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]]",
              p95Thresholds.get(3),
              names.get(3),
              groups),
          Request.of(
              "/api/tracker/events/"
                  + event
                  + "?fields=event,relationships[relationship,relationshipType,relationshipName,bidirectional,from[event[event,dataValues,occurredAt,scheduledAt,status,orgUnit,programStage,program]],to[event[event,dataValues,*,occurredAt,scheduledAt,status,orgUnit,programStage,program]]]",
              p95Thresholds.get(4),
              names.get(4),
              eventGroups),
          Request.of(
              "/api/tracker/relationships?event="
                  + event
                  + "&fields=from,to,relationshipType,relationship,createdAt",
              p95Thresholds.get(5),
              names.get(5),
              eventGroups));
    }

    /** The session keys of the opened child, to remove at the start of an iteration. */
    String[] keys() {
      return new String[] {key, key + "Enrollment", key + "Event"};
    }

    List<Request> requests() {
      return List.of(
          trackedEntity,
          trackedEntityAsCapture,
          enrollment,
          relationshipsForTrackedEntity,
          event,
          relationshipsForEvent);
    }

    /** Capture loads the tracked entity, its enrollment and relationships together. */
    ChainBuilder chain() {
      String enrollmentKey = key + "Enrollment";
      String eventKey = key + "Event";
      return exec(
          group(groupName)
              .on(
                  exec(trackedEntity
                          .action()
                          .check(jsonPath("$.enrollments[*]").count().gte(1))
                          .check(jsonPath("$.enrollments[0].enrollment").saveAs(enrollmentKey))
                          .check(jsonPath("$.enrollments[0].events[*]").count().gte(1))
                          .check(jsonPath("$.enrollments[0].events[0].event").saveAs(eventKey)))
                      .exec(
                          trackedEntityAsCapture
                              .action()
                              .check(jsonPath("$.enrollments[*]").count().gte(1)))
                      .doIf(session -> session.contains(enrollmentKey))
                      .then(
                          exec(enrollment.action().check(jsonPath("$.enrollment").exists()))
                              // A child is linked to its mother when she is enrolled in ANC, so it
                              // has none or one.
                              .exec(
                                  relationshipsForTrackedEntity
                                      .action()
                                      .check(jsonPath("$.relationships").exists()))
                              .pause(1, 3) // user reads enrollment details
                              // User clicks on event within enrollment
                              .doIf(session -> session.contains(eventKey))
                              .then(
                                  group("Get one event")
                                      .on(
                                          exec(event.action().check(jsonPath("$.event").exists()))
                                              .exec(
                                                  relationshipsForEvent
                                                      .action()
                                                      .check(
                                                          jsonPath("$.relationships[*]")
                                                              .count()
                                                              .is(0))))))));
    }
  }

  private record ScenarioWithRequests(ScenarioBuilder scenario, List<Request> requests) {}
}
