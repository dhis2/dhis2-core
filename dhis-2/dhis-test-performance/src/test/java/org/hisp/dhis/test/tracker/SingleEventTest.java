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
import static io.gatling.javaapi.core.CoreDsl.details;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.forAll;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test for single-event-program tracker imports -- event programs without tracked
 * entities, imported one event per HTTP request from many independent concurrent user sessions.
 *
 * <p>This is deliberately separate from {@link TrackerTest}, which tests tracked-entity programs
 * (MNCH, Child) and ANC by batching multiple entities per request via a shared {@code
 * importEntitiesPerRequest}/{@code importUsers} configuration. Single-event programs are a
 * fundamentally different production traffic shape -- one event per request, from many separate
 * logged-in sessions submitting data through the web client one entry at a time -- and forcing that
 * shape out of TrackerTest's shared knobs either crashes the whole simulation (a tracked-entity
 * program's {@code linesPerRequest} floors to 0 at batch size 1) or contaminates the other
 * programs' own concurrency (since {@code importUsers} is shared across all of them). This class
 * exists so single-event programs never have to share configuration with tracked-entity ones again.
 *
 * <p>Currently exercises one program:
 *
 * <ul>
 *   <li><b>Inpatient morbidity and mortality</b> ({@code eBAyeGv0exc}) -- event program; exercises
 *       the ~14,000-option ICD-10 diagnosis option set ({@code eUZ79clX7y1}, data element {@code
 *       K6uUAvq500H})
 * </ul>
 *
 * <p>The import payload is a single event template embedded directly in this class -- the goal is
 * exercising the large ICD-10 option set, not per-patient data variety, so no Synthea generation or
 * S3 fetch is needed. DHIS2 generates a new event UID server-side on every request, so every
 * request still creates a genuinely new entity even though the payload template itself never
 * changes. To test a different single-event program, add a new {@code PAYLOAD_TEMPLATE} constant
 * alongside corresponding {@code -Dprogram}/{@code -DprogramStage} overrides, since the hardcoded
 * data element UIDs are specific to Inpatient.
 *
 * <p><b>Properties</b> (all optional, settable via {@code -D} flags):
 *
 * <ul>
 *   <li>{@code -Dprofile} -- {@code smoke} (default) or {@code load}
 *   <li>{@code -Dinstance} -- base URL (default: {@code http://localhost:8080})
 *   <li>{@code -DadminUser} / {@code -DadminPassword} -- login credentials (default: {@code admin}
 *       / {@code district})
 *   <li>{@code -Dprogram} -- program UID (default: Inpatient's {@code eBAyeGv0exc})
 *   <li>{@code -DprogramStage} -- program stage UID (default: Inpatient's {@code Zj7UnCAulEk})
 *   <li>{@code -Dusers} -- concurrent import users (default: 1 for smoke, 20 for load)
 *   <li>{@code -DrequestsPerUser} -- import requests per user (default: 10 for smoke, 50 for load)
 * </ul>
 *
 * <p><b>Profiles:</b>
 *
 * <ul>
 *   <li><b>smoke</b> (default) -- single user, 10 requests. Example: {@code -Dprofile=smoke}
 *   <li><b>load</b> -- 20 concurrent users, 50 requests each. Example: {@code -Dprofile=load}
 * </ul>
 *
 * <p>Every virtual user logs in exactly once ({@code POST /api/auth/login}) before its import loop
 * starts; Gatling's cookie jar carries that session for every subsequent request in the loop, so
 * none of the measured import requests re-authenticate.
 */
public class SingleEventTest extends Simulation {
  private static final Logger logger = LoggerFactory.getLogger(SingleEventTest.class);

  private static final AtomicLong REQUEST_COUNTER = new AtomicLong();

  // Every event in every run lands on the same date (2026-08-02) and org unit (DiszpKrYNg8) by
  // design -- this minimizes per-event variance to isolate ICD-10 option set lookup behavior.
  private static final String SINGLE_EVENT_PAYLOAD_TEMPLATE =
      """
      {"orgUnit":"DiszpKrYNg8","occurredAt":"2026-08-02","geometry":null,"status":"COMPLETED","notes":[],"program":"%s","programStage":"%s","dataValues":[{"dataElement":"oZg33kd9taw","value":"Male"},{"dataElement":"qrur9Dvnyt5","value":"56"},{"dataElement":"GieVkTxp4HH","value":"176"},{"dataElement":"vV9UWAZohSf","value":"102"},{"dataElement":"eMyVanycQSC","value":"2026-08-01"},{"dataElement":"K6uUAvq500H","value":"A011"},{"dataElement":"msodh3rEMJa","value":"2026-08-06"},{"dataElement":"fWIAEtYVEGk","value":"MODDISCH"}]}
      """
          .trim();

  private enum Profile {
    SMOKE,
    LOAD;

    static Profile fromString(String profile) {
      try {
        return valueOf(profile.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Unknown profile: " + profile + ". Valid options: smoke, load");
      }
    }
  }

  // Provisional -- not yet calibrated from a real performance-tests-compare.yml run. Replace
  // with real numbers once this scenario has been dispatched baseline-vs-candidate.
  private static final EnumMap<Profile, Integer> SINGLE_EVENT_IMPORT_P95 =
      new EnumMap<>(Map.of(Profile.SMOKE, 500, Profile.LOAD, 1000));

  public SingleEventTest() {
    record ProfileDefaults(int users, int requestsPerUser) {}

    Profile profile = Profile.fromString(System.getProperty("profile", "smoke"));
    ProfileDefaults defaults =
        switch (profile) {
          case SMOKE -> new ProfileDefaults(1, 10);
          case LOAD -> new ProfileDefaults(20, 50);
        };

    String instance = System.getProperty("instance", "http://localhost:8080");
    String adminUser = System.getProperty("adminUser", "admin");
    String adminPassword = System.getProperty("adminPassword", "district");
    String program =
        System.getProperty("program", "eBAyeGv0exc"); // Inpatient morbidity and mortality
    String programStage = System.getProperty("programStage", "Zj7UnCAulEk");
    int users = Integer.getInteger("users", defaults.users());
    int requestsPerUser = Integer.getInteger("requestsPerUser", defaults.requestsPerUser());

    logger.debug(
        "Single event import: {} users, {} requests/user, program={}",
        users,
        requestsPerUser,
        program);

    String eventPayload = SINGLE_EVENT_PAYLOAD_TEMPLATE.formatted(program, programStage);
    String payload = "{\"events\":[" + eventPayload + "]}";

    ScenarioBuilder singleEventScenario =
        scenario("Single event program")
            .exec(session -> session.set("username", adminUser).set("password", adminPassword))
            .exec(login())
            .exitHereIfFailed()
            .repeat(requestsPerUser)
            .on(
                exec(
                    http("Import event")
                        .post("/api/tracker?async=false")
                        .header("Content-Type", "application/json")
                        .header("X-Request-ID", session -> nextRequestId("Import event"))
                        .body(StringBody(payload))
                        .check(status().is(200))
                        .check(jsonPath("$.stats.created").ofInt().is(1))));

    HttpProtocolBuilder httpProtocol =
        http.baseUrl(instance)
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/Performance Test")
            .disableFollowRedirect() // we don't expect redirects
            .warmUp(instance + "/api/ping")
            .disableCaching() // to repeat the same request without HTTP cache influence (304)
            .check(status().is(200)); // global check for all requests

    setUp(singleEventScenario.injectOpen(atOnceUsers(users)))
        .protocols(httpProtocol)
        .assertions(
            forAll().successfulRequests().percent().gte(100d),
            details("Import event")
                .responseTime()
                .percentile(95)
                .lte(SINGLE_EVENT_IMPORT_P95.get(profile)));
  }

  private HttpRequestActionBuilder login() {
    return http("Login")
        .post("/api/auth/login")
        .header("Content-Type", "application/json")
        .header("X-Request-ID", session -> nextRequestId("Login"))
        .body(StringBody("{\"username\":\"#{username}\",\"password\":\"#{password}\"}"))
        .check(status().is(200));
  }

  /**
   * Tags every request with a short correlation ID logged via SLF4J and sent as {@code
   * X-Request-ID}, echoed back by the server per {@code
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
