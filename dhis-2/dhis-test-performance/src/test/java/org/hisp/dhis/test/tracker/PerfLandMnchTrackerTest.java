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
import io.gatling.javaapi.core.CheckBuilder;
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
 * <p>The requests are grouped into {@link Step}s after the screens of Capture they belong to, and
 * the Android client's, each built for a {@link TrackerProgram}. The UIDs of the program, stage and
 * attributes are those of {@link #EIR}, as {@link TrackerTest}'s attributes are constants: the
 * values of {@code children.csv} belong to them. Profiles and their parameters are those of {@link
 * TrackerTest}.
 */
public class PerfLandMnchTrackerTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(PerfLandMnchTrackerTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  // Request names and the p95 thresholds of requests without a band are TrackerTest's, so runs of
  // both compare side by side. The banded ones are to calibrate on this database.
  private static final TrackerProgram EIR =
      new TrackerProgram(
          "SSLpOM0r1U7", // Electronic Immunization Registry
          "Get EIR TEs",
          "RcbCl5ww8XY", // Birth details
          "Birth",
          "sB1IHYu2xQT", // GEN - Given name
          "ENRjVGxVL6l", // GEN - Family name
          "KSr2yTdu1AI", // Unique System Identifier (EPI)
          Map.ofEntries(
              Map.entry(
                  "Not found TE by name with like operator",
                  Map.of(Profile.SMOKE, 25, Profile.LOAD, 116)),
              Map.entry(
                  "Not found TE by name with eq operator",
                  Map.of(Profile.SMOKE, 25, Profile.LOAD, 35)),
              Map.entry(
                  "Search TE by name with like operator",
                  Map.of(Profile.SMOKE, 51, Profile.LOAD, 172)),
              Map.entry(
                  "Search TE by name with eq operator",
                  Map.of(Profile.SMOKE, 42, Profile.LOAD, 102)),
              Map.entry("Get TEs from events", Map.of(Profile.SMOKE, 25, Profile.LOAD, 28)),
              Map.entry(
                  "Search TEs as Android client", Map.of(Profile.SMOKE, 29, Profile.LOAD, 151)),
              Map.entry("Get first tracked entity", Map.of(Profile.SMOKE, 41, Profile.LOAD, 96)),
              Map.entry("Get first enrollment", Map.of(Profile.SMOKE, 25, Profile.LOAD, 46)),
              Map.entry(
                  "Get relationships for first tracked entity",
                  Map.of(Profile.SMOKE, 25, Profile.LOAD, 25)),
              Map.entry(
                  "Get first event from enrollment", Map.of(Profile.SMOKE, 41, Profile.LOAD, 107)),
              Map.entry(
                  "Get relationships for first event",
                  Map.of(Profile.SMOKE, 25, Profile.LOAD, 28))));

  // Of every dbgen user. Not a property: run-simulation.sh passes the admin's as -Dpassword.
  private static final String PASSWORD = "Perf-Test-2026!";

  // Capture 107.9.1's LISTING_FIELDS of its working lists.
  private static final String CAPTURE_TE_LIST_FIELDS =
      "trackedEntity,createdAt,inactive,attributes[attribute,value],programOwners[orgUnit,program]";
  private static final String CAPTURE_EVENT_LIST_FIELDS =
      "event,status,program,enrollment,trackedEntity,occurredAt,scheduledAt,orgUnit,"
          + "assignedUser[uid,username,firstName,surname],dataValues[dataElement,value]";

  // The android-sdk 1.15.0's NewTrackedEntityInstanceFields.asSearchFields.
  private static final String ANDROID_TE_SEARCH_FIELDS =
      "trackedEntity,createdAt,updatedAt,createdAtClient,updatedAtClient,orgUnit,"
          + "trackedEntityType,geometry,deleted,attributes[attribute,value,createdAt,updatedAt],"
          + "programOwners";

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

    ScenarioWithRequests trackerScenario = trackerProgramScenario(EIR);

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

  private ScenarioWithRequests trackerProgramScenario(TrackerProgram program) {
    // Bands only in smoke: 60 iterations per band (1 user, 240 iterations), against about 9 in
    // load, too few for a p95 per band.
    Band facility = this.profile == Profile.SMOKE ? Band.FACILITY : Band.NONE;
    NameSearch nameSearch = NameSearch.of(program);
    AndroidClient android = AndroidClient.of(program, facility);
    // The user opens the child she searched by its EPI, which can be of any age, and the first
    // child of the working list's first page, as TrackerTest does.
    UniqueIdSearch uniqueIdSearch =
        UniqueIdSearch.of(
            program,
            OpenRequests.of(
                program,
                "searched",
                "Go to searched enrollment",
                List.of(
                    "Get searched tracked entity",
                    "Get searched tracked entity as Capture",
                    "Get searched enrollment",
                    "Get relationships for searched tracked entity",
                    "Get first event from searched enrollment",
                    "Get relationships for first event of searched enrollment")));
    EventWorkingList eventList = EventWorkingList.of(program, facility);
    TrackedEntityWorkingList trackedEntityList =
        TrackedEntityWorkingList.of(
            program,
            facility,
            OpenRequests.of(
                program,
                "listed",
                "Go to single enrollment",
                List.of(
                    "Get first tracked entity",
                    "Get first tracked entity as Capture",
                    "Get first enrollment",
                    "Get relationships for first tracked entity",
                    "Get first event from enrollment",
                    "Get relationships for first event")));
    List<Step> steps = List.of(nameSearch, android, uniqueIdSearch, eventList, trackedEntityList);
    String[] keys = steps.stream().flatMap(step -> step.keys().stream()).toArray(String[]::new);

    ChainBuilder iteration =
        exec(session -> session.removeAll(keys))
            .exec(
                group(program.group())
                    .on(
                        exec(nameSearch.chain())
                            .pause(1, 3) // user reads results
                            .exec(android.chain())
                            .exec(uniqueIdSearch.chain())
                            .pause(1, 3) // user goes back to the search
                            // User lists the child's Birth event by its EPI
                            .exec(eventList.filteredByUniqueIdChain())
                            .pause(1, 3) // user reads results
                            .exec(eventList.chain())
                            .pause(1, 3) // user reads event list
                            .exec(trackedEntityList.chain())));

    ScenarioBuilder scenarioBuilder =
        scenario("EIR export")
            .exec(loopForProfile(feed(this.children).exec(loginAsClerk()).exec(iteration)));

    return new ScenarioWithRequests(
        scenarioBuilder, steps.stream().flatMap(step -> step.requests().stream()).toList());
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
   * A tracker program, the attributes its tracked entities are searched by and the stage of its
   * event working list. Its requests are grouped under {@code group}, the event ones named after
   * {@code stageName}. {@code p95Thresholds} maps the name of a request, with its band such as
   * {@code "Get first page of TEs [small]"}, to its p95 threshold per profile. A missing threshold
   * asserts nothing.
   */
  private record TrackerProgram(
      String uid,
      String group,
      String stage,
      String stageName,
      String givenName,
      String familyName,
      String uniqueId,
      Map<String, Map<Profile, Integer>> p95Thresholds) {

    /** Filters on the given and the family name: EIR needs two attributes to search. */
    String nameFilters(String operator, String given, String family) {
      return "&filter=%s:%s:%s&filter=%s:%s:%s"
          .formatted(givenName, operator, given, familyName, operator, family);
    }

    /** A request in this program's group, and in {@code subgroups} within it. */
    Request request(Band band, String url, String name, String... subgroups) {
      String[] groups =
          Stream.concat(Stream.of(group), Arrays.stream(subgroups)).toArray(String[]::new);
      return new Request(url, band, p95Thresholds, name, groups);
    }

    Request request(String url, String name, String... subgroups) {
      return request(Band.NONE, url, name, subgroups);
    }
  }

  /**
   * A request, named {@code name [band]} if it has a band. {@code p95Thresholds} maps such a name
   * to the p95 thresholds of each profile: a banded request has no band in load, so its load
   * threshold is under {@code name}. A missing threshold asserts nothing.
   */
  private record Request(
      String url,
      Band band,
      Map<String, Map<Profile, Integer>> p95Thresholds,
      String name,
      String... groups) {

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
                Integer threshold =
                    p95Thresholds.getOrDefault(nameOf(value), Map.of()).get(profile);
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
   * The requests of a screen of Capture, or of the Android client, and the chain that sends them.
   * The scenario puts the steps in the order a clerk goes through the screens, and the pauses of
   * moving from one to the next.
   */
  private interface Step {
    List<Request> requests();

    ChainBuilder chain();

    /** The session keys the chain saves, removed at the start of each iteration. */
    default List<String> keys() {
      return List.of();
    }
  }

  /**
   * Capture's search by names: a name that matches no one, with like and eq, then the child's first
   * 3 letters with like and its names with eq.
   */
  private record NameSearch(Request notFoundLike, Request notFoundEq, Request like, Request eq)
      implements Step {
    static NameSearch of(TrackerProgram program) {
      return new NameSearch(
          program.request(
              searchUrl(program, "like", "notfoundname", "notfoundname", ""),
              "Not found TE by name with like operator"),
          program.request(
              searchUrl(program, "eq", "notfoundname", "notfoundname", ""),
              "Not found TE by name with eq operator"),
          // Capture 107.9.1's fields
          program.request(
              searchUrl(program, "like", "#{givenLike}", "#{familyLike}", ",inactive"),
              "Search TE by name with like operator"),
          program.request(
              searchUrl(program, "eq", "#{givenEq}", "#{familyEq}", ""),
              "Search TE by name with eq operator"));
    }

    private static String searchUrl(
        TrackerProgram program, String operator, String given, String family, String moreFields) {
      return "/api/tracker/trackedEntities?"
          + program.nameFilters(operator, given, family)
          + "&fields=attributes,enrollments,trackedEntity,orgUnit"
          + moreFields
          + "&program="
          + program.uid()
          + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";
    }

    @Override
    public List<Request> requests() {
      return List.of(notFoundLike, notFoundEq, like, eq);
    }

    @Override
    public ChainBuilder chain() {
      return exec(notFoundLike.action().check(jsonPath("$.trackedEntities[*]").count().is(0)))
          .pause(1, 3) // user reads "no results", tries exact match
          .exec(notFoundEq.action().check(jsonPath("$.trackedEntities[*]").count().is(0)))
          .pause(1, 3) // user reads "no results", tries different name
          .exec(like.action().check(jsonPath("$.trackedEntities[*]").count().gte(1)))
          .pause(1, 3) // user reads results, refines search
          .exec(eq.action().check(jsonPath("$.trackedEntities[*]").count().gte(1)));
    }
  }

  /**
   * The Android client's online search by names (attributes and program owners, no enrollments),
   * and a page of its first sync of the clerk's facility: the android-sdk 1.15.0's
   * TrackerExporterNetworkHandlerImpl with NewTrackedEntityInstanceFields.allFields.
   */
  private record AndroidClient(Request search, Request sync) implements Step {
    static AndroidClient of(TrackerProgram program, Band facility) {
      return new AndroidClient(
          program.request(
              "/api/tracker/trackedEntities?page=1&pageSize=50&orgUnitMode=ACCESSIBLE&program="
                  + program.uid()
                  + program.nameFilters("like", "#{givenLike}", "#{familyLike}")
                  + "&fields="
                  + ANDROID_TE_SEARCH_FIELDS,
              "Search TEs as Android client"),
          program.request(
              facility,
              "/api/tracker/trackedEntities?orgUnits=#{orgUnit}&orgUnitMode=SELECTED&program="
                  + program.uid()
                  + "&order=createdAt:desc&paging=true&page=1&pageSize=50&includeDeleted=true"
                  + "&fields="
                  + ANDROID_TE_ALL_FIELDS,
              "Sync TEs as Android client"));
    }

    @Override
    public List<Request> requests() {
      return List.of(search, sync);
    }

    @Override
    public ChainBuilder chain() {
      return exec(search.action().check(jsonPath("$.trackedEntities[*]").count().gte(1)))
          .pause(1, 3) // user reads results
          .exec(sync.action().check(jsonPath("$.trackedEntities[*]").count().gte(1)));
    }
  }

  /**
   * Capture's search by the child's EPI, without fields. Capture opens the dashboard of a unique
   * match itself.
   */
  private record UniqueIdSearch(Request search, OpenRequests open) implements Step {
    static UniqueIdSearch of(TrackerProgram program, OpenRequests open) {
      return new UniqueIdSearch(
          program.request(
              "/api/tracker/trackedEntities?filter="
                  + program.uniqueId()
                  + ":eq:#{epi}&program="
                  + program.uid()
                  + "&orgUnitMode=ACCESSIBLE",
              "Search TE by unique identifier"),
          open);
    }

    @Override
    public List<Request> requests() {
      return Stream.concat(Stream.of(search), open.requests().stream()).toList();
    }

    @Override
    public List<String> keys() {
      return open.keys();
    }

    @Override
    public ChainBuilder chain() {
      return exec(search
              .action()
              .check(jsonPath("$.trackedEntities[*]").count().is(1))
              .check(
                  jsonPath("$.trackedEntities[0].trackedEntity")
                      .isEL("#{trackedEntity}")
                      .saveAs(open.key())))
          .doIf(session -> session.contains(open.key()))
          .then(open.chain());
    }
  }

  /**
   * The event working list of the program's stage in the clerk's facility, {@link TrackerTest}'s
   * and Capture's: the events, then the tracked entities of the page's events (sequential). And the
   * stage's events filtered by the child's EPI, as #26109 measured them, and Capture's list
   * filtered by it.
   */
  private record EventWorkingList(
      Request events,
      Request trackedEntities,
      Request eventsAsCapture,
      Request trackedEntitiesAsCapture,
      Request eventsByUniqueId,
      Request eventsByUniqueIdAsCapture)
      implements Step {
    private static final String TRACKED_ENTITIES = "trackedEntityUids";
    private static final String CAPTURE_TRACKED_ENTITIES = "captureTrackedEntityUids";

    static EventWorkingList of(TrackerProgram program, Band facility) {
      String eventsName = "Search " + program.stageName() + " events";
      String captureEventsUrl =
          "/api/tracker/events?enrollmentStatus=ACTIVE&order=createdAt:desc&page=1&pageSize=15"
              + "&orgUnit=#{orgUnit}&orgUnitMode=SELECTED&program="
              + program.uid()
              + "&programStage="
              + program.stage()
              + "&fields="
              + CAPTURE_EVENT_LIST_FIELDS;
      return new EventWorkingList(
          program.request(
              facility,
              "/api/tracker/events?order=createdAt:desc&page=1&pageSize=15&orgUnit=#{orgUnit}"
                  + "&orgUnitMode=SELECTED&program="
                  + program.uid()
                  + "&programStage="
                  + program.stage()
                  + "&fields=*",
              eventsName),
          program.request(
              "/api/tracker/trackedEntities?pageSize=15&program="
                  + program.uid()
                  + "&trackedEntities=#{"
                  + TRACKED_ENTITIES
                  + "}&fields=trackedEntity,createdAt,attributes[attribute,value],"
                  + "programOwners[orgUnit],enrollments[enrollment,status,orgUnit,enrolledAt]",
              "Get TEs from events"),
          program.request(facility, captureEventsUrl, eventsName + " as Capture"),
          program.request(
              "/api/tracker/trackedEntities?program="
                  + program.uid()
                  + "&pageSize=15&trackedEntities=#{"
                  + CAPTURE_TRACKED_ENTITIES
                  + "}&fields=trackedEntity,createdAt,attributes[attribute,value],"
                  + "programOwners[orgUnit],enrollments[enrollment]",
              "Get TEs from events as Capture"),
          program.request(
              "/api/tracker/events?program="
                  + program.uid()
                  + "&programStage="
                  + program.stage()
                  + "&filterAttributes="
                  + program.uniqueId()
                  + ":eq:#{epi}&fields=*",
              eventsName + " by unique identifier"),
          program.request(
              captureEventsUrl + "&filterAttributes=" + program.uniqueId() + ":eq:#{epi}",
              eventsName + " by unique identifier as Capture"));
    }

    @Override
    public List<Request> requests() {
      return List.of(
          eventsByUniqueId,
          eventsByUniqueIdAsCapture,
          events,
          trackedEntities,
          eventsAsCapture,
          trackedEntitiesAsCapture);
    }

    @Override
    public List<String> keys() {
      return List.of(TRACKED_ENTITIES, CAPTURE_TRACKED_ENTITIES);
    }

    /** The child's event of the stage, by its EPI. */
    ChainBuilder filteredByUniqueIdChain() {
      return exec(eventsByUniqueId.action().check(jsonPath("$.events[*]").count().is(1)))
          .exec(eventsByUniqueIdAsCapture.action().check(jsonPath("$.events[*]").count().is(1)));
    }

    @Override
    public ChainBuilder chain() {
      return exec(events
              .action()
              .check(jsonPath("$.events[*]").count().gte(1))
              .check(saveTrackedEntities(TRACKED_ENTITIES)))
          .doIf(session -> session.contains(TRACKED_ENTITIES))
          .then(
              exec(trackedEntities.action().check(jsonPath("$.trackedEntities[*]").count().gte(1))))
          .exec(
              eventsAsCapture
                  .action()
                  .check(jsonPath("$.events[*]").count().gte(1))
                  .check(saveTrackedEntities(CAPTURE_TRACKED_ENTITIES)))
          .doIf(session -> session.contains(CAPTURE_TRACKED_ENTITIES))
          .then(
              exec(
                  trackedEntitiesAsCapture
                      .action()
                      .check(jsonPath("$.trackedEntities[*]").count().gte(1))));
    }

    /** Saves the distinct tracked entities of the page's events under {@code key}. */
    private static CheckBuilder saveTrackedEntities(String key) {
      return jsonPath("$.events[*].trackedEntity")
          .findAll()
          .transform(list -> String.join(",", list.stream().distinct().toList()))
          .saveAs(key);
    }
  }

  /**
   * The tracked entity working list of the clerk's facility, {@link TrackerTest}'s and Capture's.
   * The user opens the first child of its first page, then filters the list by enrollment status:
   * {@link TrackerTest}'s filters on the names too, over the user's search scope.
   */
  private record TrackedEntityWorkingList(
      Request firstPage,
      Request firstPageAsCapture,
      OpenRequests open,
      Request withEnrollmentStatus,
      Request withEnrollmentStatusAsCapture)
      implements Step {
    static TrackedEntityWorkingList of(TrackerProgram program, Band facility, OpenRequests open) {
      String captureUrl =
          "/api/tracker/trackedEntities?order=createdAt:desc&page=1&pageSize=15&orgUnits="
              + "#{orgUnit}&orgUnitMode=SELECTED&program="
              + program.uid()
              + "&fields="
              + CAPTURE_TE_LIST_FIELDS;
      return new TrackedEntityWorkingList(
          program.request(
              facility,
              "/api/tracker/trackedEntities?order=createdAt:desc&page=1&pageSize=15&orgUnits="
                  + "#{orgUnit}&orgUnitMode=SELECTED&program="
                  + program.uid()
                  + "&fields=:all,!relationships,programOwners[orgUnit,program]",
              "Get first page of TEs"),
          program.request(facility, captureUrl, "Get first page of TEs as Capture"),
          open,
          program.request(
              facility,
              "/api/tracker/trackedEntities?order=createdAt:desc&page=1&pageSize=15"
                  + "&orgUnitMode=ACCESSIBLE&program="
                  + program.uid()
                  + program.nameFilters("ge", "A", "A")
                  + "&enrollmentStatus=ACTIVE"
                  + "&fields=:all,!relationships,programOwners[orgUnit,program]",
              "Get TEs with enrollment status"),
          program.request(
              facility,
              captureUrl + "&enrollmentStatus=ACTIVE",
              "Get TEs with enrollment status as Capture"));
    }

    @Override
    public List<Request> requests() {
      return Stream.concat(
              Stream.of(
                  firstPage,
                  firstPageAsCapture,
                  withEnrollmentStatus,
                  withEnrollmentStatusAsCapture),
              open.requests().stream())
          .toList();
    }

    @Override
    public List<String> keys() {
      return open.keys();
    }

    @Override
    public ChainBuilder chain() {
      return exec(firstPage
              .action()
              .check(jsonPath("$.trackedEntities[*]").count().gte(1))
              .check(jsonPath("$.trackedEntities[0].trackedEntity").saveAs(open.key())))
          .exec(firstPageAsCapture.action().check(jsonPath("$.trackedEntities[*]").count().gte(1)))
          .pause(1, 3) // user reads TE list, picks one
          .doIf(session -> session.contains(open.key()))
          .then(open.chain())
          .pause(1, 3) // user goes back to list
          .exec(
              withEnrollmentStatus.action().check(jsonPath("$.trackedEntities[*]").count().gte(1)))
          .exec(
              withEnrollmentStatusAsCapture
                  .action()
                  .check(jsonPath("$.trackedEntities[*]").count().gte(1)));
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
      Request relationshipsForEvent)
      implements Step {

    /**
     * {@code names} follow the order of the requests: the tracked entity, the tracked entity as
     * Capture, the enrollment, the tracked entity's relationships, the event and the event's
     * relationships.
     */
    static OpenRequests of(TrackerProgram program, String key, String group, List<String> names) {
      String trackedEntityUrl =
          "/api/tracker/trackedEntities/#{" + key + "}?program=" + program.uid();
      String event = "#{" + key + "Event}";
      return new OpenRequests(
          key,
          group,
          program.request(
              trackedEntityUrl + "&fields=programOwners[orgUnit],enrollments", names.get(0), group),
          // The heaviest of the requests Capture sends for its enrollment dashboard.
          program.request(
              trackedEntityUrl
                  + "&fields=enrollments[*,!attributes],attributes,inactive,programOwners[program,orgUnit]",
              names.get(1),
              group),
          program.request(
              "/api/tracker/enrollments/#{"
                  + key
                  + "Enrollment}?fields=enrollment,trackedEntity,program,status,orgUnit,enrolledAt,occurredAt,followUp,deleted,createdBy,updatedBy,updatedAt,geometry",
              names.get(2),
              group),
          program.request(
              "/api/tracker/relationships?trackedEntity=#{"
                  + key
                  + "}&paging=false&fields=relationship,relationshipType,createdAt,from[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]],to[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]]",
              names.get(3),
              group),
          program.request(
              "/api/tracker/events/"
                  + event
                  + "?fields=event,relationships[relationship,relationshipType,relationshipName,bidirectional,from[event[event,dataValues,occurredAt,scheduledAt,status,orgUnit,programStage,program]],to[event[event,dataValues,*,occurredAt,scheduledAt,status,orgUnit,programStage,program]]]",
              names.get(4),
              group,
              "Get one event"),
          program.request(
              "/api/tracker/relationships?event="
                  + event
                  + "&fields=from,to,relationshipType,relationship,createdAt",
              names.get(5),
              group,
              "Get one event"));
    }

    @Override
    public List<String> keys() {
      return List.of(key, key + "Enrollment", key + "Event");
    }

    @Override
    public List<Request> requests() {
      return List.of(
          trackedEntity,
          trackedEntityAsCapture,
          enrollment,
          relationshipsForTrackedEntity,
          event,
          relationshipsForEvent);
    }

    /** Capture loads the tracked entity, its enrollment and relationships together. */
    @Override
    public ChainBuilder chain() {
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
