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
 * <p>Each iteration goes through the {@link Journey}s of a user, each a task such as searching a
 * child and opening it, grouped by journey and by the screens within it, as Capture shows them:
 * "EIR / Search and open / Enrollment dashboard / Tracked entity". A request is named after what it
 * reads, not after the client that sends it: where a screen has more than one variant of a request,
 * the name says how they differ, such as "First page" and "First page, all fields".
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
 *       is named after its band, such as "First page [small]", and has a p95 threshold per band:
 *       one p95 over all bands is about the slowest band's p80, so a faster band could get several
 *       times slower unnoticed. Load has about 9 iterations per band, too few for a p95 each: it
 *       names and asserts these requests over all bands, for what concurrency adds. How many
 *       records a name matches changes the time of a search, not its plan, so the names don't get
 *       bands.
 *   <li>EIR needs two attributes to search outside the capture scope, so every name search filters
 *       on the given and the family name, where {@link TrackerTest} filters on the first name. The
 *       like search takes the first 3 letters of each name, as a user types them
 *       (minCharactersToSearch is 3).
 *   <li>A child is linked to its mother by a Mother and child relationship when she is enrolled in
 *       ANC, so the relationships of a tracked entity are not always empty. Events have none.
 *   <li>Every EIR child has a Unique System Identifier (EPI), a unique attribute. The test also
 *       searches tracked entities and Birth events by the EPI of the child, which {@link
 *       TrackerTest} does not. The tracked entity search is Capture's.
 *   <li>The test opens the child it searched by its EPI, which can be of any age, and the first
 *       child of the working list's first page, as {@link TrackerTest} does: the enrollment
 *       dashboard of "Search and open" and of "Tracked entity working list".
 *   <li>{@link TrackerTest}'s lists are those of Capture before v106.2.8 ({@code fields=*} and
 *       {@code :all}), which its CSV download still sends: the variants "..., all fields". They
 *       stay, as they read relationships and notes that Capture's lists no longer do, and fixes
 *       such as #26186 were measured with them. Next to them the test sends what Capture 107.9.1
 *       and the android-sdk 1.15.0 send: the working lists, the dashboard's tracked entity, and a
 *       page of a first sync, which reads the relationships of every tracked entity, enrollment and
 *       event.
 * </ul>
 *
 * <p>The children are bound to the records of the {@code perfland-mnch} dumps, which are the same
 * for every DHIS2 version: re-generate them with {@code children.sql} when the dumps change. The eq
 * pair and the like letters of a child match a child of the clerk's facility among the first 5
 * matches in the district, the page of these searches, so DHIS2 2.42, which drops what the clerk
 * does not own after paging, returns her too. {@code -Dchildren} reads another file.
 *
 * <p>The thresholds are those of the same requests in {@link TrackerTest}, calibrated on Sierra
 * Leone. The other requests have none yet.
 *
 * <p>The journeys are built for a {@link TrackerProgram}. The UIDs of the program, stage and
 * attributes are those of {@link #EIR}, as {@link TrackerTest}'s attributes are constants: the
 * values of {@code children.csv} belong to them. Profiles and their parameters are those of {@link
 * TrackerTest}.
 */
public class PerfLandMnchTrackerTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(PerfLandMnchTrackerTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  // The screen of a working list.
  private static final String WORKING_LIST = "Working list";

  private static final TrackerProgram EIR =
      new TrackerProgram(
          "SSLpOM0r1U7", // Electronic Immunization Registry
          "EIR",
          "RcbCl5ww8XY", // Birth details
          "Birth",
          "sB1IHYu2xQT", // GEN - Given name
          "ENRjVGxVL6l", // GEN - Family name
          "KSr2yTdu1AI", // Unique System Identifier (EPI)
          // TrackerTest's thresholds of the same requests
          Map.ofEntries(
              p95(25, 116, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "No match, like"),
              p95(25, 35, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "No match, eq"),
              p95(51, 172, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "Names, like"),
              p95(42, 102, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "Names, eq"),
              p95(
                  25,
                  28,
                  "Birth event working list",
                  WORKING_LIST,
                  "Tracked entities of events, enrollment details"),
              p95(29, 151, AndroidClient.JOURNEY, "Search, like"),
              p95(
                  41,
                  96,
                  TrackedEntityWorkingList.JOURNEY,
                  OpenTrackedEntity.DASHBOARD,
                  "Tracked entity, enrollments with events"),
              p95(
                  25,
                  46,
                  TrackedEntityWorkingList.JOURNEY,
                  OpenTrackedEntity.DASHBOARD,
                  "Enrollment"),
              p95(
                  25,
                  25,
                  TrackedEntityWorkingList.JOURNEY,
                  OpenTrackedEntity.DASHBOARD,
                  "Relationships of tracked entity"),
              p95(41, 107, TrackedEntityWorkingList.JOURNEY, OpenTrackedEntity.EVENT_PAGE, "Event"),
              p95(
                  25,
                  28,
                  TrackedEntityWorkingList.JOURNEY,
                  OpenTrackedEntity.EVENT_PAGE,
                  "Relationships of event")));

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
    SearchAndOpen searchAndOpen = SearchAndOpen.of(program);
    EventWorkingList eventList = EventWorkingList.of(program, facility);
    TrackedEntityWorkingList trackedEntityList = TrackedEntityWorkingList.of(program, facility);
    AndroidClient android = AndroidClient.of(program, facility);
    List<Journey> journeys = List.of(searchAndOpen, eventList, trackedEntityList, android);
    String[] keys =
        journeys.stream().flatMap(journey -> journey.keys().stream()).toArray(String[]::new);

    ChainBuilder iteration =
        exec(session -> session.removeAll(keys))
            .exec(
                group(program.group())
                    .on(
                        exec(searchAndOpen.chain())
                            .pause(1, 3) // user goes to the event working list
                            .exec(eventList.chain())
                            .pause(1, 3) // user goes to the tracked entity working list
                            .exec(trackedEntityList.chain())
                            .pause(1, 3)
                            .exec(android.chain())));

    ScenarioBuilder scenarioBuilder =
        scenario("EIR export")
            .exec(loopForProfile(feed(this.children).exec(loginAsClerk()).exec(iteration)));

    return new ScenarioWithRequests(
        scenarioBuilder,
        journeys.stream().flatMap(journey -> journey.requests().stream()).toList());
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

  /** A p95 threshold per profile of the request at {@code path}, below the program's group. */
  private static Map.Entry<String, Map<Profile, Integer>> p95(int smoke, int load, String... path) {
    return Map.entry(String.join(" / ", path), Map.of(Profile.SMOKE, smoke, Profile.LOAD, load));
  }

  /**
   * A tracker program, the attributes its tracked entities are searched by and the stage of its
   * event working list. Its requests are grouped under {@code group}. {@code p95Thresholds} maps
   * the path of a request below {@code group}, with its band such as {@code "Tracked entity working
   * list / First page [small]"}, to its p95 threshold per profile. A missing threshold asserts
   * nothing.
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
   * A request, named {@code name [band]} if it has a band. {@code p95Thresholds} maps its path
   * below the program's group, with such a name, to the p95 thresholds of each profile: a banded
   * request has no band in load, so its load threshold is under {@code name}. A missing threshold
   * asserts nothing.
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
                String[] path = Arrays.copyOf(groups, groups.length + 1);
                path[groups.length] = nameOf(value);
                String key = String.join(" / ", Arrays.asList(path).subList(1, path.length));
                Integer threshold = p95Thresholds.getOrDefault(key, Map.of()).get(profile);
                if (threshold == null) {
                  return Stream.empty();
                }
                return Stream.of(details(path).responseTime().percentile(95).lte(threshold));
              });
    }
  }

  /**
   * A user's journey: a task such as searching a child and opening it, its requests grouped by the
   * screens it goes through. The scenario puts the journeys in the order of an iteration, and the
   * pauses of moving from one to the next.
   */
  private interface Journey {
    List<Request> requests();

    /** The requests in the journey's group. */
    ChainBuilder chain();

    /** The session keys the chain saves, removed at the start of each iteration. */
    default List<String> keys() {
      return List.of();
    }
  }

  /**
   * The user searches a child on Capture's search screen: by names that match no one, with like and
   * eq, then by the child's first 3 letters with like and its names with eq, then by its EPI.
   * Capture opens the enrollment dashboard of a unique match itself.
   */
  private record SearchAndOpen(
      Request noMatchLike,
      Request noMatchEq,
      Request like,
      Request eq,
      Request uniqueId,
      OpenTrackedEntity open)
      implements Journey {
    static final String JOURNEY = "Search and open";
    static final String SEARCH = "Search";

    static SearchAndOpen of(TrackerProgram program) {
      return new SearchAndOpen(
          program.request(
              searchUrl(program, "like", "notfoundname", "notfoundname", ""),
              "No match, like",
              JOURNEY,
              SEARCH),
          program.request(
              searchUrl(program, "eq", "notfoundname", "notfoundname", ""),
              "No match, eq",
              JOURNEY,
              SEARCH),
          // Capture 107.9.1's fields
          program.request(
              searchUrl(program, "like", "#{givenLike}", "#{familyLike}", ",inactive"),
              "Names, like",
              JOURNEY,
              SEARCH),
          program.request(
              searchUrl(program, "eq", "#{givenEq}", "#{familyEq}", ""),
              "Names, eq",
              JOURNEY,
              SEARCH),
          // Capture's search by a unique attribute, without fields.
          program.request(
              "/api/tracker/trackedEntities?filter="
                  + program.uniqueId()
                  + ":eq:#{epi}&program="
                  + program.uid()
                  + "&orgUnitMode=ACCESSIBLE",
              "Unique ID",
              JOURNEY,
              SEARCH),
          OpenTrackedEntity.of(program, "searched", JOURNEY));
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
      return Stream.concat(
              Stream.of(noMatchLike, noMatchEq, like, eq, uniqueId), open.requests().stream())
          .toList();
    }

    @Override
    public List<String> keys() {
      return open.keys();
    }

    @Override
    public ChainBuilder chain() {
      return exec(
          group(JOURNEY)
              .on(
                  group(SEARCH)
                      .on(
                          exec(noMatchLike
                                  .action()
                                  .check(jsonPath("$.trackedEntities[*]").count().is(0)))
                              .pause(1, 3) // user reads "no results", tries exact match
                              .exec(
                                  noMatchEq
                                      .action()
                                      .check(jsonPath("$.trackedEntities[*]").count().is(0)))
                              .pause(1, 3) // user reads "no results", tries different name
                              .exec(
                                  like.action()
                                      .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                              .pause(1, 3) // user reads results, refines search
                              .exec(
                                  eq.action()
                                      .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                              .pause(1, 3) // user reads results, searches by the child's EPI
                              .exec(
                                  uniqueId
                                      .action()
                                      .check(jsonPath("$.trackedEntities[*]").count().is(1))
                                      .check(
                                          jsonPath("$.trackedEntities[0].trackedEntity")
                                              .isEL("#{trackedEntity}")
                                              .saveAs(open.key()))))
                      .doIf(session -> session.contains(open.key()))
                      .then(open.chain())));
    }
  }

  /**
   * The user opens the event working list of the program's stage in her facility: the events, then
   * the tracked entities of the page's events (sequential). Then she filters it by the child's EPI.
   */
  private record EventWorkingList(
      String journey,
      Request eventsAllFields,
      Request trackedEntitiesWithEnrollmentDetails,
      Request events,
      Request trackedEntities,
      Request eventsByUniqueIdAllFields,
      Request eventsByUniqueId)
      implements Journey {
    private static final String BY_UNIQUE_ID = "Working list, by unique ID";
    private static final String ALL_FIELDS_TRACKED_ENTITIES = "allFieldsEventTrackedEntities";
    private static final String TRACKED_ENTITIES = "eventTrackedEntities";

    static EventWorkingList of(TrackerProgram program, Band facility) {
      String journey = program.stageName() + " event working list";
      String eventsUrl =
          "/api/tracker/events?enrollmentStatus=ACTIVE&order=createdAt:desc&page=1&pageSize=15"
              + "&orgUnit=#{orgUnit}&orgUnitMode=SELECTED&program="
              + program.uid()
              + "&programStage="
              + program.stage()
              + "&fields="
              + CAPTURE_EVENT_LIST_FIELDS;
      return new EventWorkingList(
          journey,
          program.request(
              facility,
              "/api/tracker/events?order=createdAt:desc&page=1&pageSize=15&orgUnit=#{orgUnit}"
                  + "&orgUnitMode=SELECTED&program="
                  + program.uid()
                  + "&programStage="
                  + program.stage()
                  + "&fields=*",
              "Events, all fields",
              journey,
              WORKING_LIST),
          // The enrollments' status, org unit and enrollment date, where Capture's reads their UID
          program.request(
              "/api/tracker/trackedEntities?pageSize=15&program="
                  + program.uid()
                  + "&trackedEntities=#{"
                  + ALL_FIELDS_TRACKED_ENTITIES
                  + "}&fields=trackedEntity,createdAt,attributes[attribute,value],"
                  + "programOwners[orgUnit],enrollments[enrollment,status,orgUnit,enrolledAt]",
              "Tracked entities of events, enrollment details",
              journey,
              WORKING_LIST),
          program.request(facility, eventsUrl, "Events", journey, WORKING_LIST),
          program.request(
              "/api/tracker/trackedEntities?program="
                  + program.uid()
                  + "&pageSize=15&trackedEntities=#{"
                  + TRACKED_ENTITIES
                  + "}&fields=trackedEntity,createdAt,attributes[attribute,value],"
                  + "programOwners[orgUnit],enrollments[enrollment]",
              "Tracked entities of events",
              journey,
              WORKING_LIST),
          // In the user's search scope, not her facility, as #26109 measured it
          program.request(
              "/api/tracker/events?program="
                  + program.uid()
                  + "&programStage="
                  + program.stage()
                  + "&filterAttributes="
                  + program.uniqueId()
                  + ":eq:#{epi}&fields=*",
              "Events by unique ID, search scope, all fields",
              journey,
              BY_UNIQUE_ID),
          program.request(
              eventsUrl + "&filterAttributes=" + program.uniqueId() + ":eq:#{epi}",
              "Events by unique ID",
              journey,
              BY_UNIQUE_ID));
    }

    @Override
    public List<Request> requests() {
      return List.of(
          eventsAllFields,
          trackedEntitiesWithEnrollmentDetails,
          events,
          trackedEntities,
          eventsByUniqueIdAllFields,
          eventsByUniqueId);
    }

    @Override
    public List<String> keys() {
      return List.of(ALL_FIELDS_TRACKED_ENTITIES, TRACKED_ENTITIES);
    }

    @Override
    public ChainBuilder chain() {
      return exec(
          group(journey)
              .on(
                  group(WORKING_LIST)
                      .on(
                          exec(eventsAllFields
                                  .action()
                                  .check(jsonPath("$.events[*]").count().gte(1))
                                  .check(saveTrackedEntities(ALL_FIELDS_TRACKED_ENTITIES)))
                              .doIf(session -> session.contains(ALL_FIELDS_TRACKED_ENTITIES))
                              .then(
                                  exec(
                                      trackedEntitiesWithEnrollmentDetails
                                          .action()
                                          .check(jsonPath("$.trackedEntities[*]").count().gte(1))))
                              .exec(
                                  events
                                      .action()
                                      .check(jsonPath("$.events[*]").count().gte(1))
                                      .check(saveTrackedEntities(TRACKED_ENTITIES)))
                              .doIf(session -> session.contains(TRACKED_ENTITIES))
                              .then(
                                  exec(
                                      trackedEntities
                                          .action()
                                          .check(jsonPath("$.trackedEntities[*]").count().gte(1)))))
                      .pause(1, 3) // user reads the list, filters it by the child's EPI
                      .exec(
                          group(BY_UNIQUE_ID)
                              .on(
                                  exec(eventsByUniqueIdAllFields
                                          .action()
                                          .check(jsonPath("$.events[*]").count().is(1)))
                                      .exec(
                                          eventsByUniqueId
                                              .action()
                                              .check(jsonPath("$.events[*]").count().is(1)))))));
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
   * The user opens the tracked entity working list of her facility and the enrollment dashboard of
   * the first child of its first page. Then she filters the list by enrollment status: the variant
   * with all fields filters on the names too, over the user's search scope.
   */
  private record TrackedEntityWorkingList(
      Request firstPageAllFields,
      Request firstPage,
      OpenTrackedEntity open,
      Request activeAllFields,
      Request active)
      implements Journey {
    static final String JOURNEY = "Tracked entity working list";
    static final String ACTIVE = "Working list, active";

    static TrackedEntityWorkingList of(TrackerProgram program, Band facility) {
      String firstPageUrl =
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
              "First page, all fields",
              JOURNEY,
              WORKING_LIST),
          program.request(facility, firstPageUrl, "First page", JOURNEY, WORKING_LIST),
          OpenTrackedEntity.of(program, "listed", JOURNEY),
          program.request(
              facility,
              "/api/tracker/trackedEntities?order=createdAt:desc&page=1&pageSize=15"
                  + "&orgUnitMode=ACCESSIBLE&program="
                  + program.uid()
                  + program.nameFilters("ge", "A", "A")
                  + "&enrollmentStatus=ACTIVE"
                  + "&fields=:all,!relationships,programOwners[orgUnit,program]",
              "Active, names from A, search scope, all fields",
              JOURNEY,
              ACTIVE),
          program.request(
              facility, firstPageUrl + "&enrollmentStatus=ACTIVE", "Active", JOURNEY, ACTIVE));
    }

    @Override
    public List<Request> requests() {
      return Stream.concat(
              Stream.of(firstPageAllFields, firstPage, activeAllFields, active),
              open.requests().stream())
          .toList();
    }

    @Override
    public List<String> keys() {
      return open.keys();
    }

    @Override
    public ChainBuilder chain() {
      return exec(
          group(JOURNEY)
              .on(
                  group(WORKING_LIST)
                      .on(
                          exec(firstPageAllFields
                                  .action()
                                  .check(jsonPath("$.trackedEntities[*]").count().gte(1))
                                  .check(
                                      jsonPath("$.trackedEntities[0].trackedEntity")
                                          .saveAs(open.key())))
                              .exec(
                                  firstPage
                                      .action()
                                      .check(jsonPath("$.trackedEntities[*]").count().gte(1))))
                      .pause(1, 3) // user reads the list, opens the first child
                      .doIf(session -> session.contains(open.key()))
                      .then(open.chain())
                      .pause(1, 3) // user goes back to the list, filters it by enrollment status
                      .exec(
                          group(ACTIVE)
                              .on(
                                  exec(activeAllFields
                                          .action()
                                          .check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                                      .exec(
                                          active
                                              .action()
                                              .check(
                                                  jsonPath("$.trackedEntities[*]")
                                                      .count()
                                                      .gte(1)))))));
    }
  }

  /**
   * The Android client: an online search by names (attributes and program owners, no enrollments,
   * the android-sdk 1.15.0's NewTrackedEntityInstanceFields.asSearchFields), and a page of its
   * first sync of the clerk's facility (TrackerExporterNetworkHandlerImpl with
   * NewTrackedEntityInstanceFields.allFields).
   */
  private record AndroidClient(Request search, Request firstSyncPage) implements Journey {
    static final String JOURNEY = "Android";

    static AndroidClient of(TrackerProgram program, Band facility) {
      return new AndroidClient(
          program.request(
              "/api/tracker/trackedEntities?page=1&pageSize=50&orgUnitMode=ACCESSIBLE&program="
                  + program.uid()
                  + program.nameFilters("like", "#{givenLike}", "#{familyLike}")
                  + "&fields="
                  + ANDROID_TE_SEARCH_FIELDS,
              "Search, like",
              JOURNEY),
          program.request(
              facility,
              "/api/tracker/trackedEntities?orgUnits=#{orgUnit}&orgUnitMode=SELECTED&program="
                  + program.uid()
                  + "&order=createdAt:desc&paging=true&page=1&pageSize=50&includeDeleted=true"
                  + "&fields="
                  + ANDROID_TE_ALL_FIELDS,
              "First sync page",
              JOURNEY));
    }

    @Override
    public List<Request> requests() {
      return List.of(search, firstSyncPage);
    }

    @Override
    public ChainBuilder chain() {
      return exec(
          group(JOURNEY)
              .on(
                  exec(search.action().check(jsonPath("$.trackedEntities[*]").count().gte(1)))
                      .pause(1, 3) // user reads results
                      .exec(
                          firstSyncPage
                              .action()
                              .check(jsonPath("$.trackedEntities[*]").count().gte(1)))));
    }
  }

  /**
   * The screens of opening a child: its enrollment dashboard (the tracked entity, its enrollment
   * and relationships), then the event page of the enrollment's first event (the event and its
   * relationships). The session holds the tracked entity's UID under {@code key}, {@link #chain()}
   * saves the enrollment's and the event's under {@code key + "Enrollment"} and {@code key +
   * "Event"}.
   *
   * <p>Capture sends a screen's requests concurrently, in waves: a request waits for the response
   * that holds the UIDs it needs. Recorded with Chrome's resource timings (Capture of {@code
   * dhis2/core-dev:latest}, 2 children): the dashboard reads the tracked entity's enrollments
   * first, then sends its other requests in waves of 1 to 3 that varied between the 2 openings, the
   * relationships last. The event page reads the event first, then sends the event's relationships,
   * the tracked entity, the enrollment and the event's relationship items together. The test sends
   * the first request of a screen, then the others concurrently as its {@code resources}: Gatling
   * fetches them once the first completes.
   */
  private record OpenTrackedEntity(
      String key,
      Request trackedEntityWithEvents,
      Request trackedEntity,
      Request enrollment,
      Request relationshipsForTrackedEntity,
      Request event,
      Request relationshipsForEvent) {
    static final String DASHBOARD = "Enrollment dashboard";
    static final String EVENT_PAGE = "Event page";

    static OpenTrackedEntity of(TrackerProgram program, String key, String journey) {
      String trackedEntityUrl =
          "/api/tracker/trackedEntities/#{" + key + "}?program=" + program.uid();
      String event = "#{" + key + "Event}";
      return new OpenTrackedEntity(
          key,
          // Every enrollment field and its events, as #26186 measured it
          program.request(
              trackedEntityUrl + "&fields=programOwners[orgUnit],enrollments",
              "Tracked entity, enrollments with events",
              journey,
              DASHBOARD),
          // The heaviest of the requests Capture sends for its enrollment dashboard.
          program.request(
              trackedEntityUrl
                  + "&fields=enrollments[*,!attributes],attributes,inactive,programOwners[program,orgUnit]",
              "Tracked entity",
              journey,
              DASHBOARD),
          program.request(
              "/api/tracker/enrollments/#{"
                  + key
                  + "Enrollment}?fields=enrollment,trackedEntity,program,status,orgUnit,enrolledAt,occurredAt,followUp,deleted,createdBy,updatedBy,updatedAt,geometry",
              "Enrollment",
              journey,
              DASHBOARD),
          program.request(
              "/api/tracker/relationships?trackedEntity=#{"
                  + key
                  + "}&paging=false&fields=relationship,relationshipType,createdAt,from[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]],to[trackedEntity[trackedEntity,attributes,program,orgUnit,trackedEntityType],event[event,dataValues,program,orgUnit,orgUnitName,status,createdAt]]",
              "Relationships of tracked entity",
              journey,
              DASHBOARD),
          program.request(
              "/api/tracker/events/"
                  + event
                  + "?fields=event,relationships[relationship,relationshipType,relationshipName,bidirectional,from[event[event,dataValues,occurredAt,scheduledAt,status,orgUnit,programStage,program]],to[event[event,dataValues,*,occurredAt,scheduledAt,status,orgUnit,programStage,program]]]",
              "Event",
              journey,
              EVENT_PAGE),
          program.request(
              "/api/tracker/relationships?event="
                  + event
                  + "&fields=from,to,relationshipType,relationship,createdAt",
              "Relationships of event",
              journey,
              EVENT_PAGE));
    }

    /** The session keys of the opened child, to remove at the start of an iteration. */
    List<String> keys() {
      return List.of(key, key + "Enrollment", key + "Event");
    }

    List<Request> requests() {
      return List.of(
          trackedEntityWithEvents,
          trackedEntity,
          enrollment,
          relationshipsForTrackedEntity,
          event,
          relationshipsForEvent);
    }

    ChainBuilder chain() {
      String enrollmentKey = key + "Enrollment";
      String eventKey = key + "Event";
      return exec(group(DASHBOARD)
              .on(
                  exec(trackedEntityWithEvents
                          .action()
                          .check(jsonPath("$.enrollments[*]").count().gte(1))
                          .check(jsonPath("$.enrollments[0].enrollment").saveAs(enrollmentKey))
                          .check(jsonPath("$.enrollments[0].events[*]").count().gte(1))
                          .check(jsonPath("$.enrollments[0].events[0].event").saveAs(eventKey))
                          .resources(
                              trackedEntity
                                  .action()
                                  .check(jsonPath("$.enrollments[*]").count().gte(1)),
                              enrollment.action().check(jsonPath("$.enrollment").exists())))
                      // A child is linked to its mother when she is enrolled in ANC, so it has
                      // none or one.
                      .exec(
                          relationshipsForTrackedEntity
                              .action()
                              .check(jsonPath("$.relationships").exists()))))
          .pause(1, 3) // user reads enrollment details, opens the event
          .doIf(session -> session.contains(eventKey))
          .then(
              group(EVENT_PAGE)
                  .on(
                      exec(
                          event
                              .action()
                              .check(jsonPath("$.event").exists())
                              .resources(
                                  relationshipsForEvent
                                      .action()
                                      .check(jsonPath("$.relationships[*]").count().is(0))))));
    }
  }

  private record ScenarioWithRequests(ScenarioBuilder scenario, List<Request> requests) {}
}
