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
import static io.gatling.javaapi.core.CoreDsl.during;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.feed;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.foreach;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.incrementConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.listFeeder;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.rampConcurrentUsers;
import static io.gatling.javaapi.core.CoreDsl.repeat;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.flushCookieJar;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.httpConcurrentRequests;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.Assertion;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import io.gatling.javaapi.core.ClosedInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracker exports of the WHO Electronic Immunization Registry (EIR) on the synthetic Perf Land MNCH
 * database (DHIS2-21406), as data clerks of district QP1613 send them: they search children by name
 * and by their unique identifier, list them (first page, enrollment status, Birth events), and open
 * one with its enrollment, event and relationships. The same clerks send these requests for women
 * of the WHO Antenatal Care Registry (ANC), with its repeatable Quick Check stage as the event
 * working list's stage. Each program is searched as its WHO package sets it up: EIR by names and by
 * its unique EPI, ANC by the ANC ID only, which is not unique ({@link TrackerProgram}).
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
 *   <li>A virtual user is a data clerk of district QP1613, one of dbgen's users of {@code
 *       perfland-mnch/clerks.csv}, instead of a replica of one user. She logs in once, then works
 *       on the children of {@code perfland-mnch/children.csv} that are hers, a child of her
 *       facility each iteration, as clerks work on different children all day. A clerk captures in
 *       her facility and searches in her district. She searches the child by name and EPI and opens
 *       it. Her children are picked at random with a fixed seed by {@code
 *       perfland-mnch/clerks.sql}, 60 each, and no two clerks share one, so every run, baseline and
 *       candidate, works on the same children in the same order.
 *   <li>On ANC a clerk works on her women of {@code perfland-mnch/women.csv} instead, picked as her
 *       children by {@code perfland-mnch/women.sql}, up to 60: a small facility has fewer. She
 *       searches the woman by her ANC ID and opens her.
 *   <li>Smoke runs one program, EIR or ANC ({@code ancShare} 0 or 1): it has the first clerk of
 *       each band work, one after another, on {@code repeat} of her children. Load and capacity
 *       split {@code concurrentUsers} between the programs, {@code ancShare} of them on ANC (0.5):
 *       the first clerks of each program work at the same time, a virtual user each, until the
 *       injection ends. The rows of {@code clerks.csv} alternate the bands, so the clerks of a run
 *       spread over them. With both programs, a band's clerks alternate between them, so a clerk
 *       works on one program and is never logged in twice, as a person has one session.
 *   <li>The clerks' facilities differ by their number of EIR children, as the query plans differ
 *       between them: a clerk's band is the range her facility's falls into (small, median, large,
 *       largest; large is the first page's slowest plan). Each range keeps away from the sizes at
 *       which an EIR request switches to another plan, so the statistics that ANALYZE samples
 *       before each run don't flip it. ANC switches at other sizes and some of a band's facilities
 *       flip, but not those of smoke's clerks: each keeps one plan in both programs. In smoke, a
 *       request whose plan depends on the facility is named after its band, such as "First page
 *       [small]", and can have a p95 threshold per band: one p95 over all bands is about the
 *       slowest band's p80, so a faster band could get several times slower unnoticed. Load names
 *       and asserts these requests over all bands, for what concurrency adds. How many records a
 *       name matches changes the time of a search, not its plan, so the names don't get bands.
 *   <li>EIR needs two attributes to search outside the capture scope, so every name search filters
 *       on the given and the family name, where {@link TrackerTest} filters on the first name. The
 *       like search takes the first 3 letters of each name, as a user types them
 *       (minCharactersToSearch is 3).
 *   <li>A child is linked to its mother by a Mother and child relationship when she is enrolled in
 *       ANC, so the relationships of a tracked entity are not always empty. Events have none.
 *   <li>Every EIR child has a Unique System Identifier (EPI), a unique attribute. The test also
 *       searches tracked entities and Birth events by the EPI of the child, which {@link
 *       TrackerTest} does not. The tracked entity search is Capture's. ANC's ANC ID is not unique:
 *       Capture has no search by it of its own, and filters ANC's Quick Check events by it with
 *       like.
 *   <li>The test opens the child it searched by its EPI, which can be of any age, or the woman its
 *       like search found by her ANC ID, and the first person of the working list's first page, as
 *       {@link TrackerTest} does: the enrollment dashboard of "Search and open" and of "Tracked
 *       entity working list".
 *   <li>{@link TrackerTest}'s lists are those of Capture before v106.2.8 ({@code fields=*} and
 *       {@code :all}), which its CSV download still sends: the variants "..., all fields". They
 *       stay, as they read relationships and notes that Capture's lists no longer do, and fixes
 *       such as #26186 were measured with them. Next to them the test sends what Capture 107.9.1
 *       and the android-sdk 1.15.0 send: the working lists, the dashboard's tracked entity, and a
 *       page of a first sync, which reads the relationships of every tracked entity, enrollment and
 *       event.
 * </ul>
 *
 * <p>The clerks and children are bound to the records of the {@code perfland-mnch} dumps, which are
 * the same for every DHIS2 version: re-generate them with {@code clerks.sql} and {@code women.sql}
 * when the dumps change. The eq pair and the like letters of a child match a child of the clerk's
 * facility among the first 5 matches in the district, the page of these searches, so DHIS2 2.42,
 * which drops what the clerk does not own after paging, returns her too. {@code -Dclerks}, {@code
 * -Dchildren} and {@code -Dwomen} read other files.
 *
 * <p>The thresholds are those of the same requests in {@link TrackerTest}, calibrated on Sierra
 * Leone, for EIR and for ANC but its like searches by the ANC ID. The other requests have none yet.
 *
 * <p>The journeys are built for a {@link TrackerProgram}, {@link #EIR} or {@link #ANC}, with its
 * UIDs: the values of {@code children.csv} belong to EIR's attributes, those of {@code women.csv}
 * to ANC's. The profiles' injections and parameters are those of {@link TrackerTest}, except who
 * the virtual users are.
 */
public class PerfLandMnchTrackerTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(PerfLandMnchTrackerTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  // The screen of a working list.
  private static final String WORKING_LIST = "Working list";

  // EIR searched by both names, as it needs two attributes to search, and by its unique EPI.
  private static final TrackerProgram EIR =
      new TrackerProgram(
          "SSLpOM0r1U7", // Electronic Immunization Registry
          "EIR",
          "children",
          "RcbCl5ww8XY", // Birth details
          "Birth",
          "names",
          List.of(
              // GEN - Given name
              new SearchAttribute("sB1IHYu2xQT", "#{givenEq}", "#{givenLike}"),
              // GEN - Family name
              new SearchAttribute("ENRjVGxVL6l", "#{familyEq}", "#{familyLike}")),
          // Unique System Identifier (EPI)
          new Identifier("KSr2yTdu1AI", "unique ID", true, "#{epi}"),
          thresholds(
              "Birth",
              p95(25, 116, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "No match, like"),
              p95(51, 172, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "Names, like"),
              p95(42, 102, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "Names, eq"),
              p95(29, 151, AndroidClient.JOURNEY, "Search, like")));

  // ANC as its WHO package (1.0.0) sets it up: searchable by the ANC ID and the phone number, not
  // by names. The test searches by the ANC ID, the woman's whole ID for like as for eq, as one ID
  // matches one woman. The ANC ID is not unique, so it has no search of its own and the event
  // working list filters it with like. Repeatable Quick Check as the event working list's stage.
  // EIR's thresholds of the requests ANC shares with it, but not of its like searches: without a
  // trigram index on the ANC ID they read every ANC ID, 2 to 5 times over EIR's smoke thresholds.
  private static final TrackerProgram ANC =
      new TrackerProgram(
          "WSGAb5XwJ3Y", // Antenatal care registry (ANC)
          "ANC",
          "women",
          "XznDErihed9", // Quick Check
          "Quick Check",
          "ANC ID",
          // ANC.A4. Unique identification
          List.of(new SearchAttribute("PpEGiQurAll", "#{ancId}", "#{ancId}")),
          new Identifier("PpEGiQurAll", "ANC ID", false, "#{ancId}"),
          thresholds(
              "Quick Check",
              // EIR's Names, eq
              p95(42, 102, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "ANC ID, eq")));

  // Of every dbgen user. Not a property: run-simulation.sh passes the admin's as -Dpassword.
  private static final String PASSWORD = "Perf-Test-2026!";

  // Session keys: a clerk of clerks.csv, her people (children of EIR, women of ANC), the number of
  // the iteration, which picks her person.
  private static final String CLERK = "clerk";
  private static final String PEOPLE = "people";
  private static final String ITERATION = "iteration";

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
  private final int concurrentUsers;
  private final int repeat;
  private final int durationSec;
  private final int rampDurationSec;
  private final int steps;

  public PerfLandMnchTrackerTest() {
    this.profile = Profile.fromString(System.getProperty("profile", "smoke"));
    this.instance = System.getProperty("instance", "http://localhost:8080");

    record ProfileDefaults(
        int concurrentUsers,
        int repeat,
        int rampDurationSec,
        int durationSec,
        int steps,
        double ancShare) {}
    ProfileDefaults defaults =
        switch (this.profile) {
          // 60 children per clerk, one clerk per facility band: a band's p95 sits below its 3
          // slowest samples, so one slow sample moves it less than with 30. One program per run.
          case SMOKE -> new ProfileDefaults(1, 60, 1, 1, 1, 0);
          // 32 clerks: about 1.3 requests in flight on the perf runner, 4 kept it idle 87% of the
          // time (0.13 in flight), and a p95 rests on about 225 samples instead of 28. 48 users
          // still ran without errors and with the same medians.
          case LOAD -> new ProfileDefaults(32, 1, 15, 180, 1, 0.5);
          case CAPACITY -> new ProfileDefaults(8, 1, 10, 30, 4, 0.5);
        };
    this.concurrentUsers = Integer.getInteger("concurrentUsers", defaults.concurrentUsers());
    this.repeat = Integer.getInteger("repeat", defaults.repeat());
    this.rampDurationSec = Integer.getInteger("rampDurationSec", defaults.rampDurationSec());
    this.durationSec = Integer.getInteger("durationSec", defaults.durationSec());
    this.steps = Integer.getInteger("steps", defaults.steps());

    // Load and capacity split concurrentUsers between the programs: ancShare of them work on
    // ANC, the others on EIR, each program's clerks at the same time. Locally, 32 users in total,
    // 16 on each, kept the db at 88% of its 4 CPUs, 48 at 129%. How many clerks work on each
    // program is unknown, hence 50/50. 0 leaves ANC out, 1 EIR.
    // Smoke runs one program, EIR (0) or ANC (1): each run has its own warmup and no program
    // runs after the other.
    double ancShare = Double.parseDouble(System.getProperty("ancShare", "" + defaults.ancShare()));
    if (ancShare < 0 || ancShare > 1) {
      throw new IllegalArgumentException("ancShare %s is not within 0 and 1".formatted(ancShare));
    }
    if (this.profile == Profile.SMOKE && ancShare != 0 && ancShare != 1) {
      throw new IllegalArgumentException(
          "smoke runs one program: ancShare 0 for EIR or 1 for ANC, not %s".formatted(ancShare));
    }
    int ancUsers = (int) Math.round(this.concurrentUsers * ancShare);
    int eirUsers = this.concurrentUsers - ancUsers;
    // The same clerks work on both programs, but with both at the same time each on one.
    List<Map<String, Object>> clerks =
        csv(System.getProperty("clerks", "perfland-mnch/clerks.csv")).readRecords();
    ProgramClerks programClerks =
        eirUsers > 0 && ancUsers > 0 ? alternateByBand(clerks) : new ProgramClerks(clerks, clerks);

    requireRequestsOf(EIR);
    requireRequestsOf(ANC);
    List<ScenarioWithRequests> scenarios = new ArrayList<>();
    if (eirUsers > 0) {
      scenarios.add(trackerProgramScenario(EIR, programClerks.eir(), eirUsers));
    }
    if (ancUsers > 0) {
      scenarios.add(trackerProgramScenario(ANC, programClerks.anc(), ancUsers));
    }

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
        setUp(
                scenarios.stream()
                    .map(s -> s.scenario().injectClosed(buildClosedInjectionProfile(s.users())))
                    .toList())
            .protocols(httpProtocolBuilder)
            .assertions(getAssertions(this.profile, scenarios));
    // Pauses model a user reading the screen between requests, see TrackerTest.
    if (this.profile == Profile.SMOKE || Boolean.getBoolean("disablePauses")) {
      setUp.disablePauses();
    }
  }

  /**
   * Splits the clerks between the programs, as a clerk works on one: a band's clerks alternate
   * between them, starting with EIR, so both get every band and EIR the first clerk of each.
   */
  private static ProgramClerks alternateByBand(List<Map<String, Object>> clerks) {
    Map<Object, Integer> ofBand = new HashMap<>();
    List<Map<String, Object>> eir = new ArrayList<>();
    List<Map<String, Object>> anc = new ArrayList<>();
    for (Map<String, Object> clerk : clerks) {
      if (ofBand.merge(clerk.get(Band.FACILITY.column), 1, Integer::sum) % 2 == 1) {
        eir.add(clerk);
      } else {
        anc.add(clerk);
      }
    }
    return new ProgramClerks(eir, anc);
  }

  /** The clerks of {@code clerks.csv} that work on EIR and on ANC. */
  private record ProgramClerks(List<Map<String, Object>> eir, List<Map<String, Object>> anc) {}

  /** Fails if {@code users} exceed the program's clerks: a clerk would be logged in twice. */
  private static void requireClerks(
      TrackerProgram program, int users, List<Map<String, Object>> clerks) {
    if (users > clerks.size()) {
      throw new IllegalArgumentException(
          "%d %s users exceed its %d clerks: a clerk would be logged in twice"
              .formatted(users, program.group(), clerks.size()));
    }
  }

  /**
   * Fails if a program's {@code users} are not a multiple of {@code steps}: capacity adds {@code
   * users / steps} at each step, so the remainder would never run, and fewer users than steps none.
   */
  private void requireStepsOf(TrackerProgram program, int users) {
    if (users % this.steps != 0) {
      throw new IllegalArgumentException(
          "%d %s users are not a multiple of %d steps: capacity would run %d of them"
              .formatted(users, program.group(), this.steps, users / this.steps * this.steps));
    }
  }

  /**
   * Fails if a threshold of {@code program} is under the path of none of its requests, in any
   * profile: it would assert nothing, as after renaming a request.
   */
  private static void requireRequestsOf(TrackerProgram program) {
    Set<String> keys =
        journeys(program, Band.FACILITY).stream()
            .flatMap(journey -> journey.requests().stream())
            .flatMap(Request::keys)
            .collect(Collectors.toSet());
    List<String> unmatched =
        program.p95Thresholds().keySet().stream()
            .filter(key -> !keys.contains(key))
            .sorted()
            .toList();
    if (!unmatched.isEmpty()) {
      throw new IllegalArgumentException(
          "%s has thresholds of no request: %s".formatted(program.group(), unmatched));
    }
  }

  /** The journeys of an iteration on {@code program}, in their order. */
  private static List<Journey> journeys(TrackerProgram program, Band facility) {
    return List.of(
        SearchAndOpen.of(program),
        EventWorkingList.of(program, facility),
        TrackedEntityWorkingList.of(program, facility),
        AndroidClient.of(program, facility));
  }

  /** The scenario of {@code users} of {@code clerks} at work on {@code program}. */
  private ScenarioWithRequests trackerProgramScenario(
      TrackerProgram program, List<Map<String, Object>> clerks, int users) {
    if (this.profile != Profile.SMOKE) {
      requireClerks(program, users, clerks);
    }
    if (this.profile == Profile.CAPACITY) {
      requireStepsOf(program, users);
    }
    // Bands only in smoke, 60 iterations each. Load's clerks run about 8 iterations each, too
    // few for a p95 per band unless a run has many clerks in each.
    Band facility = this.profile == Profile.SMOKE ? Band.FACILITY : Band.NONE;
    List<Journey> journeys = journeys(program, facility);
    String[] keys =
        journeys.stream().flatMap(journey -> journey.keys().stream()).toArray(String[]::new);

    ChainBuilder inOrder = exec(journeys.get(0).chain());
    for (Journey journey : journeys.subList(1, journeys.size())) {
      inOrder = inOrder.pause(1, 3).exec(journey.chain()); // user goes to the next task
    }
    ChainBuilder iteration =
        exec(session -> session.removeAll(keys)).exec(group(program.group()).on(inOrder));

    ScenarioBuilder scenarioBuilder =
        scenario(program.group() + " export")
            .exec(clerksAtWork(iteration, withPeople(clerks, program), users));

    return new ScenarioWithRequests(
        scenarioBuilder,
        journeys.stream().flatMap(journey -> journey.requests().stream()).toList(),
        users);
  }

  /**
   * The clerks of the profile at work. A clerk logs in once, then runs {@code iteration} on her
   * people, the next one each time. Smoke has the first clerk of each band work, one after another,
   * on {@code repeat} of her people; load and capacity have the first {@code users} clerks work at
   * the same time, a virtual user each, until the injection ends.
   */
  private ChainBuilder clerksAtWork(
      ChainBuilder iteration, List<Map<String, Object>> clerks, int users) {
    ChainBuilder nextPerson =
        exec(session -> {
              List<Map<String, Object>> people = session.getList(PEOPLE);
              return session.setAll(people.get(session.getInt(ITERATION) % people.size()));
            })
            .exec(iteration);

    if (this.profile == Profile.SMOKE) {
      List<Map<String, Object>> firstOfEachBand =
          List.copyOf(
              clerks.stream()
                  .collect(
                      Collectors.toMap(
                          clerk -> clerk.get(Band.FACILITY.column),
                          clerk -> clerk,
                          (first, next) -> first,
                          LinkedHashMap::new))
                  .values());
      return foreach(firstOfEachBand, CLERK)
          .on(
              exec(session -> session.setAll(session.getMap(CLERK)))
                  .exec(login())
                  .exec(repeat(this.repeat, ITERATION).on(nextPerson)));
    }

    // A clerk each: a virtual user that ends early is not replaced by another clerk, it empties
    // the feeder, which stops the run.
    return feed(listFeeder(clerks.subList(0, users)))
        .exec(login())
        .exec(during(Duration.ofSeconds(injectionDurationSec()), ITERATION).on(nextPerson));
  }

  /**
   * The clerks, each with her people of {@code program} in their order, as a list under {@link
   * #PEOPLE}: her children of {@code children.csv} on EIR, her women of {@code women.csv} on ANC.
   * {@code -Dchildren} and {@code -Dwomen} read other files.
   */
  private static List<Map<String, Object>> withPeople(
      List<Map<String, Object>> clerks, TrackerProgram program) {
    String file =
        System.getProperty(program.people(), "perfland-mnch/" + program.people() + ".csv");
    Map<Object, List<Map<String, Object>>> people =
        csv(file).readRecords().stream()
            .collect(Collectors.groupingBy(person -> person.get("username")));
    return clerks.stream()
        .map(
            clerk -> {
              List<Map<String, Object>> hers = people.get(clerk.get("username"));
              if (hers == null) {
                throw new IllegalArgumentException(
                    "%s has no %s in %s".formatted(clerk.get("username"), program.people(), file));
              }
              Map<String, Object> withPeople = new HashMap<>(clerk);
              withPeople.put(PEOPLE, hers);
              return withPeople;
            })
        .toList();
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
   * Logs in as the session's clerk, without the cookies of a clerk before her. A failed login
   * crashes the run: the clerks don't match the dump or DHIS2 can't log in, and the run would
   * measure fewer clerks than asked for. The session is marked succeeded first: in smoke, a KO of a
   * clerk before her is in the report and fails its assertions, but must not count as her login
   * failing.
   */
  private ChainBuilder login() {
    return exec(Session::markAsSucceeded)
        .exec(flushCookieJar())
        .exec(
            http("Login")
                .post("/api/auth/login")
                .header("Content-Type", "application/json")
                .header("X-Request-ID", session -> nextRequestId("Login"))
                .body(
                    StringBody(
                        "{\"username\":\"#{username}\",\"password\":\"%s\"}".formatted(PASSWORD)))
                .check(status().is(200)))
        .crashLoadGeneratorIf(
            session -> "login of %s failed".formatted(session.getString("username")),
            Session::isFailed);
  }

  /** The closed injection profiles of TrackerTest. */
  private List<ClosedInjectionStep> buildClosedInjectionProfile(int users) {
    return switch (this.profile) {
      case SMOKE -> List.of(constantConcurrentUsers(1).during(1));
      case LOAD ->
          List.of(
              rampConcurrentUsers(0).to(users).during(Duration.ofSeconds(this.rampDurationSec)),
              constantConcurrentUsers(users).during(Duration.ofSeconds(this.durationSec)));
      case CAPACITY ->
          List.of(
              incrementConcurrentUsers(users / this.steps)
                  .times(this.steps)
                  .eachLevelLasting(Duration.ofSeconds(this.durationSec))
                  .separatedByRampsLasting(Duration.ofSeconds(this.rampDurationSec))
                  .startingFrom(users / this.steps));
    };
  }

  private List<Assertion> getAssertions(Profile profile, List<ScenarioWithRequests> scenarios) {
    return Stream.concat(
            Stream.of(forAll().successfulRequests().percent().gte(100d)),
            scenarios.stream()
                .flatMap(scenario -> scenario.requests().stream())
                .flatMap(r -> r.assertions(profile)))
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

  /** The column of {@code clerks.csv} a request's plan depends on, and its values. */
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
   * The thresholds of a program: {@link TrackerTest}'s of the same requests, calibrated on Sierra
   * Leone, of the requests whose names are the same in every program, with its event working list
   * of the stage {@code stageName}, and its {@code own}.
   */
  @SafeVarargs
  private static Map<String, Map<Profile, Integer>> thresholds(
      String stageName, Map.Entry<String, Map<Profile, Integer>>... own) {
    return Stream.concat(
            Stream.of(
                p95(25, 35, SearchAndOpen.JOURNEY, SearchAndOpen.SEARCH, "No match, eq"),
                p95(
                    25,
                    28,
                    EventWorkingList.journey(stageName),
                    WORKING_LIST,
                    "Tracked entities of events, enrollment details"),
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
                p95(
                    41,
                    107,
                    TrackedEntityWorkingList.JOURNEY,
                    OpenTrackedEntity.EVENT_PAGE,
                    "Event"),
                p95(
                    25,
                    28,
                    TrackedEntityWorkingList.JOURNEY,
                    OpenTrackedEntity.EVENT_PAGE,
                    "Relationships of event")),
            Arrays.stream(own))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * A tracker program, the attributes its search filters on, named {@code searchedBy} in the names
   * of the requests that search by them, the {@link Identifier} a person is looked up by, and the
   * stage of its event working list. Its requests are grouped under {@code group}. A clerk works on
   * her {@code people} of {@code perfland-mnch/<people>.csv}. {@code p95Thresholds} maps the path
   * of a request below {@code group}, with its band such as {@code "Tracked entity working list /
   * First page [small]"}, to its p95 threshold per profile. A request without a threshold asserts
   * nothing, a threshold without a request fails the run.
   */
  private record TrackerProgram(
      String uid,
      String group,
      String people,
      String stage,
      String stageName,
      String searchedBy,
      List<SearchAttribute> searchAttributes,
      Identifier id,
      Map<String, Map<Profile, Integer>> p95Thresholds) {

    /** A filter with {@code operator} on each search attribute, with the value {@code value}. */
    String searchFilters(String operator, Function<SearchAttribute, String> value) {
      return searchAttributes.stream()
          .map(
              attribute ->
                  "&filter=%s:%s:%s".formatted(attribute.uid(), operator, value.apply(attribute)))
          .collect(Collectors.joining());
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
   * An attribute of a program's search, and the clerk's values for it as session expressions: the
   * whole value for eq, what she types for like.
   */
  private record SearchAttribute(String uid, String eq, String like) {}

  /**
   * The attribute a person is looked up by, named {@code name} in the requests' names, and the
   * clerk's value for it as a session expression. Capture and the Android Capture app search a
   * unique attribute with eq, Capture in a form of its own, and an attribute that is neither unique
   * nor has an option set or a preferred operator, such as a TEXT ID, with like: in the search form
   * and in a working list's filter (capture-app getSearchOperator, android-capture-app
   * SearchRepositoryImpl).
   */
  private record Identifier(String uid, String name, boolean unique, String value) {
    String operator() {
      return unique ? "eq" : "like";
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
      return bandValues().stream()
          .map(value -> path(nameOf(value)))
          .flatMap(
              path -> {
                Integer threshold = p95Thresholds.getOrDefault(key(path), Map.of()).get(profile);
                if (threshold == null) {
                  return Stream.empty();
                }
                return Stream.of(details(path).responseTime().percentile(95).lte(threshold));
              });
    }

    /**
     * The keys of {@code p95Thresholds} this request is asserted under in any profile: with its
     * band in smoke, without in load.
     */
    Stream<String> keys() {
      return Stream.concat(Stream.of(name), bandValues().stream().map(this::nameOf))
          .distinct()
          .map(requestName -> key(path(requestName)));
    }

    private List<String> bandValues() {
      return band == Band.NONE ? List.of("") : band.values;
    }

    /** The groups and the name of the request, as {@code details} takes them. */
    private String[] path(String requestName) {
      String[] path = Arrays.copyOf(groups, groups.length + 1);
      path[groups.length] = requestName;
      return path;
    }

    /** The path below the program's group. */
    private static String key(String[] path) {
      return String.join(" / ", Arrays.asList(path).subList(1, path.length));
    }
  }

  /**
   * A user's journey: a task such as searching a person and opening her, its requests grouped by
   * the screens it goes through. The scenario puts the journeys in the order of an iteration, and
   * the pauses of moving from one to the next.
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
   * The user searches a person on Capture's search screen by the program's search attributes:
   * values that match no one, with like and eq, then hers with like, as she types them, and with
   * eq. Capture sends like for these attributes; eq is {@link TrackerTest}'s. Then she searches by
   * her unique ID, which Capture does in a form of its own that opens the enrollment dashboard of
   * its match. A program whose ID is not unique has no such search: the user opens the person her
   * like search found, by her whole ID.
   */
  private record SearchAndOpen(
      Request noMatchLike,
      Request noMatchEq,
      Request like,
      Request eq,
      Optional<Request> uniqueId,
      OpenTrackedEntity open)
      implements Journey {
    static final String JOURNEY = "Search and open";
    static final String SEARCH = "Search";

    static SearchAndOpen of(TrackerProgram program) {
      String searchedBy =
          program.searchedBy().substring(0, 1).toUpperCase() + program.searchedBy().substring(1);
      Identifier id = program.id();
      return new SearchAndOpen(
          program.request(
              searchUrl(program, program.searchFilters("like", attribute -> "notfoundname"), ""),
              "No match, like",
              JOURNEY,
              SEARCH),
          program.request(
              searchUrl(program, program.searchFilters("eq", attribute -> "notfoundname"), ""),
              "No match, eq",
              JOURNEY,
              SEARCH),
          // Capture 107.9.1's fields
          program.request(
              searchUrl(program, program.searchFilters("like", SearchAttribute::like), ",inactive"),
              searchedBy + ", like",
              JOURNEY,
              SEARCH),
          program.request(
              searchUrl(program, program.searchFilters("eq", SearchAttribute::eq), ""),
              searchedBy + ", eq",
              JOURNEY,
              SEARCH),
          // Capture's search by a unique attribute, without fields.
          id.unique()
              ? Optional.of(
                  program.request(
                      "/api/tracker/trackedEntities?filter="
                          + id.uid()
                          + ":eq:"
                          + id.value()
                          + "&program="
                          + program.uid()
                          + "&orgUnitMode=ACCESSIBLE",
                      "Unique ID",
                      JOURNEY,
                      SEARCH))
              : Optional.empty(),
          OpenTrackedEntity.of(program, "searched", JOURNEY));
    }

    private static String searchUrl(TrackerProgram program, String filters, String moreFields) {
      return "/api/tracker/trackedEntities?"
          + filters
          + "&fields=attributes,enrollments,trackedEntity,orgUnit"
          + moreFields
          + "&program="
          + program.uid()
          + "&page=1&pageSize=5&orgUnitMode=ACCESSIBLE";
    }

    @Override
    public List<Request> requests() {
      return Stream.of(
              Stream.of(noMatchLike, noMatchEq, like, eq),
              uniqueId.stream(),
              open.requests().stream())
          .flatMap(requests -> requests)
          .toList();
    }

    @Override
    public List<String> keys() {
      return open.keys();
    }

    @Override
    public ChainBuilder chain() {
      CheckBuilder[] found = {jsonPath("$.trackedEntities[*]").count().gte(1)};
      // The search whose one match is the person, saved to open her.
      CheckBuilder[] hers = {
        jsonPath("$.trackedEntities[*]").count().is(1),
        jsonPath("$.trackedEntities[0].trackedEntity").isEL("#{trackedEntity}").saveAs(open.key())
      };
      ChainBuilder search =
          exec(noMatchLike.action().check(jsonPath("$.trackedEntities[*]").count().is(0)))
              .pause(1, 3) // user reads "no results", tries exact match
              .exec(noMatchEq.action().check(jsonPath("$.trackedEntities[*]").count().is(0)))
              .pause(1, 3) // user reads "no results", tries the person's values
              .exec(like.action().check(uniqueId.isPresent() ? found : hers))
              .pause(1, 3) // user reads results, refines search
              .exec(eq.action().check(found));
      if (uniqueId.isPresent()) {
        search =
            search
                .pause(1, 3) // user reads results, searches by the person's unique ID
                .exec(uniqueId.get().action().check(hers));
      }
      // Capture's search by a unique ID opens her dashboard itself. Otherwise the user reads the
      // results and opens her.
      ChainBuilder openHer = uniqueId.isPresent() ? open.chain() : pause(1, 3).exec(open.chain());
      return exec(
          group(JOURNEY)
              .on(
                  group(SEARCH)
                      .on(search)
                      .doIf(session -> session.contains(open.key()))
                      .then(openHer)));
    }
  }

  /**
   * The user opens the event working list of the program's stage in her facility: the events, then
   * the tracked entities of the page's events (sequential). Then she filters it by the person's ID,
   * with Capture's operator for it ({@link Identifier}).
   */
  private record EventWorkingList(
      String journey,
      String byId,
      boolean unique,
      Request eventsAllFields,
      Request trackedEntitiesWithEnrollmentDetails,
      Request events,
      Request trackedEntities,
      Request eventsByIdAllFields,
      Request eventsById)
      implements Journey {
    private static final String ALL_FIELDS_TRACKED_ENTITIES = "allFieldsEventTrackedEntities";
    private static final String TRACKED_ENTITIES = "eventTrackedEntities";

    /** The journey of the event working list of the stage {@code stageName}. */
    static String journey(String stageName) {
      return stageName + " event working list";
    }

    static EventWorkingList of(TrackerProgram program, Band facility) {
      String journey = journey(program.stageName());
      Identifier id = program.id();
      String byId = "Working list, by " + id.name();
      String idFilter = "&filterAttributes=" + id.uid() + ":" + id.operator() + ":" + id.value();
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
          byId,
          id.unique(),
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
                  + idFilter
                  + "&fields=*",
              "Events by " + id.name() + ", search scope, all fields",
              journey,
              byId),
          program.request(eventsUrl + idFilter, "Events by " + id.name(), journey, byId));
    }

    @Override
    public List<Request> requests() {
      return List.of(
          eventsAllFields,
          trackedEntitiesWithEnrollmentDetails,
          events,
          trackedEntities,
          eventsByIdAllFields,
          eventsById);
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
                      .pause(1, 3) // user reads the list, filters it by the person's ID
                      .exec(
                          group(byId)
                              .on(
                                  exec(eventsByIdAllFields.action().check(hers()))
                                      .exec(eventsById.action().check(hers()))))));
    }

    /**
     * The events filtered by the person's ID are hers only: one for a unique ID, as the stage of
     * such a program is not repeatable, at least one otherwise. Fails if the filter is ignored,
     * which returns the events of other people.
     */
    private CheckBuilder[] hers() {
      return new CheckBuilder[] {
        unique ? jsonPath("$.events[*]").count().is(1) : jsonPath("$.events[*]").count().gte(1),
        jsonPath("$.events[*].trackedEntity")
            .findAll()
            .transform(list -> String.join(",", list.stream().distinct().toList()))
            .isEL("#{trackedEntity}")
      };
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
   * the first person of its first page. Then she filters the list by enrollment status: the variant
   * with all fields filters on the program's search attributes too, over the user's search scope.
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
                  + program.searchFilters("ge", attribute -> "A")
                  + "&enrollmentStatus=ACTIVE"
                  + "&fields=:all,!relationships,programOwners[orgUnit,program]",
              "Active, " + program.searchedBy() + " from A, search scope, all fields",
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
                      .pause(1, 3) // user reads the list, opens the first person
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
   * The Android client: an online search by the program's search attributes with like, as the
   * Android Capture app sends it for them (attributes and program owners, no enrollments, the
   * android-sdk 1.15.0's NewTrackedEntityInstanceFields.asSearchFields), and a page of its first
   * sync of the clerk's facility (TrackerExporterNetworkHandlerImpl with
   * NewTrackedEntityInstanceFields.allFields).
   */
  private record AndroidClient(Request search, Request firstSyncPage) implements Journey {
    static final String JOURNEY = "Android";

    static AndroidClient of(TrackerProgram program, Band facility) {
      return new AndroidClient(
          program.request(
              "/api/tracker/trackedEntities?page=1&pageSize=50&orgUnitMode=ACCESSIBLE&program="
                  + program.uid()
                  + program.searchFilters("like", SearchAttribute::like)
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
   * The screens of opening a person: her enrollment dashboard (the tracked entity, the enrollment
   * Capture opens and relationships), then the event page of the enrollment's first event (the
   * event and its relationships). The session holds the tracked entity's UID under {@code key},
   * {@link #chain()} saves the enrollment's and the event's under {@code key + "Enrollment"} and
   * {@code key + "Event"}.
   *
   * <p>Capture sends a screen's requests concurrently, in waves: a request waits for the response
   * that holds the UIDs it needs. Recorded with Chrome's resource timings (Capture 107.9.1, 2
   * children): the dashboard reads the tracked entity's enrollments first, then sends its other
   * requests in waves of 1 to 3 that varied between the 2 openings, the relationships last. The
   * event page reads the event first, then sends the event's relationships, the tracked entity, the
   * enrollment and the event's relationship items together. The test sends the tracked entity with
   * its enrollments first, for the UIDs, then the dashboard's tracked entity and enrollment
   * concurrently, then the relationships; and the event page's two requests concurrently.
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

    /** The session keys of the opened person, to remove at the start of an iteration. */
    List<String> keys() {
      return List.of(key, key + "EnrollmentIndex", key + "Enrollment", key + "Event");
    }

    /**
     * The index of the enrollment to open among a tracked entity's {@code enrollments} in the
     * program: her only one, else her first ACTIVE one, as Capture opens them (capture-app
     * getSuitableEnrollmentId), else her latest by {@code enrolledAt}. In that last case Capture
     * opens none and the user picks one, presumably her latest.
     */
    private static int opened(List<Map<String, Object>> enrollments) {
      if (enrollments.size() == 1) {
        return 0;
      }
      for (int i = 0; i < enrollments.size(); i++) {
        if ("ACTIVE".equals(enrollments.get(i).get("status"))) {
          return i;
        }
      }
      return IntStream.range(0, enrollments.size())
          .boxed()
          .max(Comparator.comparing(i -> String.valueOf(enrollments.get(i).get("enrolledAt"))))
          .orElseThrow();
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
      String indexKey = key + "EnrollmentIndex";
      String enrollmentKey = key + "Enrollment";
      String eventKey = key + "Event";
      String opened = "$.enrollments[#{" + indexKey + "}]";
      return exec(group(DASHBOARD)
              .on(
                  exec(trackedEntityWithEvents
                          .action()
                          .check(
                              jsonPath("$.enrollments[*]")
                                  .ofMap()
                                  .findAll()
                                  .transform(OpenTrackedEntity::opened)
                                  .saveAs(indexKey))
                          .check(jsonPath(opened + ".enrollment").saveAs(enrollmentKey))
                          .check(jsonPath(opened + ".events[*]").count().gte(1))
                          .check(jsonPath(opened + ".events[0].event").saveAs(eventKey)))
                      .exec(
                          httpConcurrentRequests(
                              trackedEntity
                                  .action()
                                  .check(jsonPath("$.enrollments[*]").count().gte(1)),
                              enrollment.action().check(jsonPath("$.enrollment").exists())))
                      // A child is linked to its mother when she is enrolled in ANC: a child has
                      // none or one, a woman none or one per child.
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
                          httpConcurrentRequests(
                              event.action().check(jsonPath("$.event").exists()),
                              relationshipsForEvent
                                  .action()
                                  .check(jsonPath("$.relationships[*]").count().is(0))))));
    }
  }

  private record ScenarioWithRequests(
      ScenarioBuilder scenario, List<Request> requests, int users) {}
}
