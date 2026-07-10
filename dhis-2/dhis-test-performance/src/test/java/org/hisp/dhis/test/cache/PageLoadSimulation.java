/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.test.cache;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance test measuring ETag API cache effectiveness under realistic page-load patterns.
 *
 * <p>Simulates three DHIS2 app page loads derived from real Chrome HAR captures:
 *
 * <ul>
 *   <li><b>Maintenance</b> — 13 API calls, ~2.5 MB (schemas endpoint dominates)
 *   <li><b>Dashboard</b> — 20+ API calls, ~1.3 MB (most chatty, plugins duplicate bootstrap)
 *   <li><b>Capture</b> — 12 API calls (org unit tree traversal + tracker queries)
 * </ul>
 *
 * <p>Each virtual user cycles through all three apps repeatedly. The first cycle populates the HTTP
 * cache (200 responses); subsequent cycles benefit from ETag caching (304 responses). Gatling's
 * built-in HTTP cache stores ETags and sends {@code If-None-Match} automatically.
 *
 * <p><b>A/B comparison:</b> Run once with {@code cache.api.etag.enabled=on} and once with {@code
 * off} in dhis.conf (see {@code scripts/etag-ab-benchmark.sh} or {@code scripts/etag-ab-live.sh}).
 * Compare Gatling reports for: response times, 304 ratio, throughput. Combine with {@code
 * run-simulation.sh} for server-side metrics (SQL count, CPU, memory).
 *
 * <p><b>etag.expect</b> (system property, default {@code none}): optional post-run assertion on the
 * share of cacheable API responses that returned HTTP 304. {@code on} requires a minimum 304 share
 * so a broken cache fails; {@code off} requires ~zero 304s so a label swap fails; {@code none}
 * keeps historical behavior (success-rate only). Wired by the A/B scripts per side.
 *
 * <p><b>Profiles:</b>
 *
 * <ul>
 *   <li>{@code smoke} (default) — 1 user, 5 app cycles, quick validation
 *   <li>{@code load} — N users sustained, measures steady-state cache benefits
 *   <li>{@code capacity} — staircase ramp to find breaking point
 * </ul>
 *
 * <p>Run:
 *
 * <pre>{@code
 * mvn gatling:test \
 *   -Dgatling.simulationClass=org.hisp.dhis.test.cache.PageLoadSimulation \
 *   -Dinstance=http://localhost:8080 \
 *   -Dprofile=load -DconcurrentUsers=20 -Detag.expect=on
 * }</pre>
 */
public class PageLoadSimulation extends Simulation {
  private static final Logger log = LoggerFactory.getLogger(PageLoadSimulation.class);

  /**
   * Floor for 304 share when {@code etag.expect=on}. Smoke with 5 cycles has ~4/5 cycles as
   * revalidations; load over minutes is higher. Keep conservative to avoid host flakiness.
   */
  private static final double MIN_304_SHARE_WHEN_ON = 0.25;

  /** Ceiling for 304 share when {@code etag.expect=off} (noise / odd redirects only). */
  private static final double MAX_304_SHARE_WHEN_OFF = 0.02;

  private static final AtomicLong CACHEABLE_RESPONSES = new AtomicLong();
  private static final AtomicLong NOT_MODIFIED_RESPONSES = new AtomicLong();

  // -- Configuration via system properties --
  private final String instance = prop("instance", "http://localhost:8080");
  private final String adminUser = prop("adminUser", "admin");
  private final String adminPassword = prop("adminPassword", "district");
  private final String apiVersion = prop("apiVersion", "44");
  private final Profile profile = Profile.fromString(prop("profile", "smoke"));
  private final EtagExpect etagExpect = EtagExpect.fromString(prop("etag.expect", "none"));
  private final int concurrentUsers = intProp("concurrentUsers", profile.defaultUsers);
  private final int appCycles = intProp("appCycles", profile.defaultCycles);
  private final int durationSec = intProp("durationSec", profile.defaultDurationSec);
  private final int rampDurationSec = intProp("rampDurationSec", profile.defaultRampSec);

  // Sierra Leone demo dashboard UID
  private final String dashboardUid = prop("dashboardUid", "nghVC4wtyzi");

  public PageLoadSimulation() {
    // Fresh counters each simulation instance (Gatling may reuse the classloader).
    CACHEABLE_RESPONSES.set(0);
    NOT_MODIFIED_RESPONSES.set(0);

    log.info(
        "ETag Cache Performance Test: profile={}, users={}, cycles={}, etag.expect={}, instance={}",
        profile,
        concurrentUsers,
        appCycles,
        etagExpect,
        instance);

    String api = "/api/" + apiVersion;

    // -- HTTP protocol: caching ENABLED (critical for ETag testing) --
    // NO .basicAuth() — we login once via POST and reuse the session cookie.
    // Basic Auth re-authenticates on every request (loads user + org units from DB).
    // Session cookies restore SecurityContext from session (fast, no DB).
    HttpProtocolBuilder httpProtocol =
        http.baseUrl(instance)
            .acceptHeader("application/json")
            .header("X-Requested-With", "XMLHttpRequest");

    // -- Login once per virtual user, establish session cookie --
    ChainBuilder login =
        exec(
            http("login")
                .post("/api/auth/login")
                .header("Content-Type", "application/json")
                .body(
                    StringBody(
                        "{\"username\":\""
                            + adminUser
                            + "\",\"password\":\""
                            + adminPassword
                            + "\"}"))
                .check(status().in(200, 302)));

    // -- Shared bootstrap requests (common across all apps) --
    ChainBuilder bootstrap =
        group("bootstrap")
            .on(
                exec(
                    http("me")
                        .get(api + "/me?fields=authorities,avatar,name,settings,username")
                        .check(cacheableStatus()),
                    http("systemSettings").get(api + "/systemSettings").check(cacheableStatus()),
                    http("userSettings").get(api + "/userSettings").check(cacheableStatus()),
                    http("systemSettings/applicationTitle")
                        .get(api + "/systemSettings/applicationTitle")
                        .check(cacheableStatus()),
                    http("systemSettings/helpPageLink")
                        .get(api + "/systemSettings/helpPageLink")
                        .check(cacheableStatus()),
                    http("apps").get(api + "/apps").check(cacheableStatus()),
                    http("apps/menu").get(api + "/apps/menu").check(cacheableStatus()),
                    http("me/dashboard").get(api + "/me/dashboard").check(cacheableStatus()),
                    http("system/info").get("/api/system/info").check(cacheableStatus())));

    // -- Maintenance app requests --
    ChainBuilder maintenanceApp =
        group("maintenance")
            .on(
                exec(bootstrap)
                    .pause(Duration.ofMillis(200))
                    .exec(
                        http("schemas")
                            .get(
                                api
                                    + "/schemas?fields=authorities,displayName,name,plural,singular,translatable,properties,shareable,dataShareable")
                            .check(cacheableStatus()),
                        http("me/authorities")
                            .get(api + "/me?fields=authorities,avatar,email,name,settings,username")
                            .check(cacheableStatus()),
                        http("organisationUnits")
                            .get(
                                api
                                    + "/organisationUnits?fields=id,access,displayName,level,path&paging=false")
                            .check(cacheableStatus())));

    // -- Dashboard app requests --
    ChainBuilder dashboardApp =
        group("dashboard")
            .on(
                exec(bootstrap)
                    .pause(Duration.ofMillis(200))
                    .exec(
                        http("dashboards")
                            .get(api + "/dashboards?fields=id,displayName,favorite&paging=false")
                            .check(cacheableStatus()),
                        http("dashboard/" + dashboardUid)
                            .get(
                                api
                                    + "/dashboards/"
                                    + dashboardUid
                                    + "?fields=id,displayName,dashboardItems[*]")
                            .check(cacheableStatus()),
                        http("organisationUnitLevels")
                            .get(api + "/organisationUnitLevels")
                            .check(cacheableStatus()),
                        http("systemSettings/analyticsRelativePeriod")
                            .get(api + "/systemSettings/keyAnalysisRelativePeriod")
                            .check(cacheableStatus()),
                        http("systemSettings/financialYearStart")
                            .get(api + "/systemSettings/analyticsFinancialYearStart")
                            .check(cacheableStatus()),
                        http("dataStore/custom-translations")
                            .get(api + "/dataStore/custom-translations/controller")
                            .check(status().in(200, 304, 404)),
                        http("locales/db").get(api + "/locales/db").check(cacheableStatus()),
                        http("periodTypes").get(api + "/periodTypes").check(cacheableStatus())));

    // -- Capture app requests --
    ChainBuilder captureApp =
        group("capture")
            .on(
                exec(bootstrap)
                    .pause(Duration.ofMillis(200))
                    .exec(
                        http("me/settings")
                            .get(api + "/me?fields=settings[keyUiLocale]")
                            .check(cacheableStatus()),
                        http("trackedEntityInstanceFilters")
                            .get(
                                api
                                    + "/trackedEntityInstanceFilters?filter=program.id:eq:IpHINAT79UW")
                            .check(cacheableStatus()),
                        http("programStageWorkingLists")
                            .get(api + "/programStageWorkingLists?filter=program.id:eq:IpHINAT79UW")
                            .check(cacheableStatus())));

    // -- Full user session: cycle through all three apps --
    // Think-time configurable: "fast" for stress testing, "realistic" for real-world simulation
    boolean fastMode = Boolean.parseBoolean(prop("fast", "false"));
    ChainBuilder appCycle;
    if (fastMode) {
      appCycle =
          exec(maintenanceApp)
              .pause(Duration.ofMillis(200), Duration.ofMillis(500))
              .exec(dashboardApp)
              .pause(Duration.ofMillis(200), Duration.ofMillis(500))
              .exec(captureApp)
              .pause(Duration.ofMillis(200), Duration.ofMillis(500));
    } else {
      appCycle =
          exec(maintenanceApp)
              .pause(Duration.ofSeconds(3), Duration.ofSeconds(5))
              .exec(dashboardApp)
              .pause(Duration.ofSeconds(5), Duration.ofSeconds(10))
              .exec(captureApp)
              .pause(Duration.ofSeconds(3), Duration.ofSeconds(5));
    }

    // -- Scenario: repeated app navigation --
    // SMOKE uses fixed repeat count; LOAD/CAPACITY loop for the injection duration
    ScenarioBuilder pageLoadScenario;
    if (profile == Profile.SMOKE) {
      pageLoadScenario =
          scenario("Page Load with ETag Cache").exec(login).exec(repeat(appCycles).on(appCycle));
    } else {
      pageLoadScenario =
          scenario("Page Load with ETag Cache")
              .exec(login)
              .during(Duration.ofSeconds(durationSec + rampDurationSec))
              .on(appCycle);
    }

    // -- Injection profile --
    List<Assertion> assertions = new ArrayList<>();
    assertions.add(forAll().successfulRequests().percent().gte(95.0));

    PopulationBuilder population;
    switch (profile) {
      case SMOKE:
        population =
            pageLoadScenario
                .injectClosed(
                    constantConcurrentUsers(concurrentUsers)
                        .during(Duration.ofSeconds(durationSec)))
                .protocols(httpProtocol);
        break;
      case LOAD:
        population =
            pageLoadScenario
                .injectClosed(
                    rampConcurrentUsers(0)
                        .to(concurrentUsers)
                        .during(Duration.ofSeconds(rampDurationSec)),
                    constantConcurrentUsers(concurrentUsers)
                        .during(Duration.ofSeconds(durationSec)))
                .protocols(httpProtocol);
        break;
      case CAPACITY:
        population =
            pageLoadScenario
                .injectClosed(
                    incrementConcurrentUsers(concurrentUsers / 5)
                        .times(5)
                        .eachLevelLasting(Duration.ofSeconds(durationSec))
                        .separatedByRampsLasting(Duration.ofSeconds(rampDurationSec)))
                .protocols(httpProtocol);
        break;
      default:
        throw new IllegalArgumentException("Unknown profile: " + profile);
    }

    setUp(population).assertions(assertions);
  }

  @Override
  public void after() {
    long total = CACHEABLE_RESPONSES.get();
    long n304 = NOT_MODIFIED_RESPONSES.get();
    double share = total == 0 ? 0.0 : (double) n304 / (double) total;
    log.info(
        "etag.expect={} cacheableResponses={} notModified={} share={}%",
        etagExpect, total, n304, String.format("%.1f", share * 100.0));

    switch (etagExpect) {
      case NONE -> {
        // Historical behavior: success-rate assertion only.
      }
      case ON -> {
        if (total < 20) {
          throw new AssertionError(
              "etag.expect=on: too few cacheable responses to judge 304 share (n="
                  + total
                  + "). Need a multi-cycle or load profile.");
        }
        if (share < MIN_304_SHARE_WHEN_ON) {
          throw new AssertionError(
              String.format(
                  "etag.expect=on: 304 share %.1f%% (n=%d/ %d) is below minimum %.0f%% — cache may be broken or disabled",
                  share * 100.0, n304, total, MIN_304_SHARE_WHEN_ON * 100.0));
        }
      }
      case OFF -> {
        if (total < 20) {
          throw new AssertionError(
              "etag.expect=off: too few cacheable responses to judge 304 share (n=" + total + ")");
        }
        if (share > MAX_304_SHARE_WHEN_OFF) {
          throw new AssertionError(
              String.format(
                  "etag.expect=off: 304 share %.1f%% (n=%d / %d) exceeds %.0f%% — labels may be swapped or ETag still on",
                  share * 100.0, n304, total, MAX_304_SHARE_WHEN_OFF * 100.0));
        }
      }
    }
  }

  // -- Helpers --

  /**
   * Accept 200/304 and, when {@code etag.expect} is not {@code none}, count the status for the
   * post-run 304-share assertion.
   */
  private CheckBuilder.Final cacheableStatus() {
    return status()
        .transform(
            code -> {
              if (etagExpect != EtagExpect.NONE && (code == 200 || code == 304)) {
                CACHEABLE_RESPONSES.incrementAndGet();
                if (code == 304) {
                  NOT_MODIFIED_RESPONSES.incrementAndGet();
                }
              }
              return code;
            })
        .in(200, 304);
  }

  private static String prop(String key, String defaultValue) {
    return System.getProperty(key, defaultValue);
  }

  private static int intProp(String key, int defaultValue) {
    return Integer.parseInt(System.getProperty(key, String.valueOf(defaultValue)));
  }

  private enum EtagExpect {
    NONE,
    ON,
    OFF;

    static EtagExpect fromString(String s) {
      if (s == null || s.isBlank() || "none".equalsIgnoreCase(s)) {
        return NONE;
      }
      return switch (s.toLowerCase()) {
        case "on" -> ON;
        case "off" -> OFF;
        default ->
            throw new IllegalArgumentException(
                "Unknown etag.expect: " + s + ". Valid: none, on, off");
      };
    }
  }

  private enum Profile {
    SMOKE(1, 5, 60, 1),
    LOAD(10, 0, 180, 15),
    CAPACITY(20, 0, 30, 10);

    final int defaultUsers;
    final int defaultCycles;
    final int defaultDurationSec;
    final int defaultRampSec;

    Profile(int defaultUsers, int defaultCycles, int defaultDurationSec, int defaultRampSec) {
      this.defaultUsers = defaultUsers;
      this.defaultCycles = defaultCycles;
      this.defaultDurationSec = defaultDurationSec;
      this.defaultRampSec = defaultRampSec;
    }

    static Profile fromString(String s) {
      try {
        return valueOf(s.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Unknown profile: " + s + ". Valid: smoke, load, capacity");
      }
    }
  }
}
