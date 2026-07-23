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

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.CheckBuilder;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared Gatling chains for the ETag cache performance suite: session-cookie login, the app
 * bootstrap group and the three app page-load groups (maintenance, dashboard, capture) derived from
 * real Chrome HAR captures.
 *
 * <p>Used by {@link PageLoadSimulation} (read-only page loads) and {@link
 * MetadataMutationSimulation} (page loads under concurrent metadata mutations). The {@link
 * #cacheableStatus()} check counts cacheable responses and 304s in static counters so simulations
 * can assert on the 304 share after a run; call {@link #resetCounters()} in the simulation
 * constructor (Gatling may reuse the classloader between runs).
 *
 * @author Morten Svanaes
 */
final class CacheScenarios {
  private static final AtomicLong CACHEABLE_RESPONSES = new AtomicLong();
  private static final AtomicLong NOT_MODIFIED_RESPONSES = new AtomicLong();

  private final String api;
  private final String adminUser;
  private final String adminPassword;
  private final String dashboardUid;
  private final boolean countStatuses;

  /**
   * @param api versioned API prefix, e.g. {@code /api/43}
   * @param countStatuses when true, {@link #cacheableStatus()} feeds the static 200/304 counters
   */
  CacheScenarios(
      String api,
      String adminUser,
      String adminPassword,
      String dashboardUid,
      boolean countStatuses) {
    this.api = api;
    this.adminUser = adminUser;
    this.adminPassword = adminPassword;
    this.dashboardUid = dashboardUid;
    this.countStatuses = countStatuses;
  }

  static void resetCounters() {
    CACHEABLE_RESPONSES.set(0);
    NOT_MODIFIED_RESPONSES.set(0);
  }

  static long cacheableResponses() {
    return CACHEABLE_RESPONSES.get();
  }

  static long notModifiedResponses() {
    return NOT_MODIFIED_RESPONSES.get();
  }

  /** Login once per virtual user via POST, establishing a session cookie (no Basic Auth). */
  ChainBuilder login() {
    return exec(
        http("login")
            .post("/api/auth/login")
            .header("Content-Type", "application/json")
            .body(
                StringBody(
                    "{\"username\":\"" + adminUser + "\",\"password\":\"" + adminPassword + "\"}"))
            .check(status().in(200, 302)));
  }

  /** Shared bootstrap requests (common across all apps). */
  ChainBuilder bootstrap() {
    return group("bootstrap")
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
  }

  /** Maintenance app page load. */
  ChainBuilder maintenanceApp() {
    return group("maintenance")
        .on(
            exec(bootstrap())
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
  }

  /** Dashboard app page load. */
  ChainBuilder dashboardApp() {
    return group("dashboard")
        .on(
            exec(bootstrap())
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
  }

  /** Capture app page load. */
  ChainBuilder captureApp() {
    return group("capture")
        .on(
            exec(bootstrap())
                .pause(Duration.ofMillis(200))
                .exec(
                    http("me/settings")
                        .get(api + "/me?fields=settings[keyUiLocale]")
                        .check(cacheableStatus()),
                    http("trackedEntityInstanceFilters")
                        .get(api + "/trackedEntityInstanceFilters?filter=program.id:eq:IpHINAT79UW")
                        .check(cacheableStatus()),
                    http("programStageWorkingLists")
                        .get(api + "/programStageWorkingLists?filter=program.id:eq:IpHINAT79UW")
                        .check(cacheableStatus())));
  }

  /**
   * One full user session cycling through all three apps. Think-time configurable: {@code fast} for
   * stress testing, realistic pauses for real-world simulation.
   */
  ChainBuilder appCycle(boolean fastMode) {
    if (fastMode) {
      return exec(maintenanceApp())
          .pause(Duration.ofMillis(200), Duration.ofMillis(500))
          .exec(dashboardApp())
          .pause(Duration.ofMillis(200), Duration.ofMillis(500))
          .exec(captureApp())
          .pause(Duration.ofMillis(200), Duration.ofMillis(500));
    }
    return exec(maintenanceApp())
        .pause(Duration.ofSeconds(3), Duration.ofSeconds(5))
        .exec(dashboardApp())
        .pause(Duration.ofSeconds(5), Duration.ofSeconds(10))
        .exec(captureApp())
        .pause(Duration.ofSeconds(3), Duration.ofSeconds(5));
  }

  /**
   * Accept 200/304 and, when counting is enabled, feed the static counters for the post-run
   * 304-share assertion.
   */
  CheckBuilder.Final cacheableStatus() {
    return status()
        .transform(
            code -> {
              if (countStatuses && (code == 200 || code == 304)) {
                CACHEABLE_RESPONSES.incrementAndGet();
                if (code == 304) {
                  NOT_MODIFIED_RESPONSES.incrementAndGet();
                }
              }
              return code;
            })
        .in(200, 304);
  }
}
