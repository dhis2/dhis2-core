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
package org.hisp.dhis.webapi;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DataStatisticsControllerTest extends PostgresControllerIntegrationTestBase {
  @Autowired private DashboardService dashboardService;

  private static final ZoneId ZONE = ZoneId.systemDefault();

  @Test
  void canGetFavoriteStats() {
    Instant base = Instant.now().atZone(ZONE).toInstant();
    Instant oneDayAgo = base.minus(1, ChronoUnit.DAYS);
    Instant tomorrow = base.plus(1, ChronoUnit.DAYS);

    Dashboard dashboardA = new Dashboard();
    dashboardA.setName("Test Dashboard");
    dashboardA.setUid(BASE_UID);
    dashboardService.saveDashboard(dashboardA);

    assertStatus(
        HttpStatus.CREATED,
        POST("/dataStatistics?eventType=DASHBOARD_VIEW&favorite=" + dashboardA.getUid()));

    // We should have at least one view now
    JsonObject response = GET("/api/dataStatistics/favorites/" + dashboardA.getUid()).content();
    String views = response.get("views").toString();
    int v = 0;
    try {
      v = Integer.parseInt(views);
    } catch (NumberFormatException nfe) {
      fail("Views is not a number: " + views);
    }
    assertTrue(v >= 1, "Expected at least one view, but got " + views);

    // Save the snapshot and verify we can query it
    assertStatus(HttpStatus.CREATED, POST("/dataStatistics/snapshot"));

    JsonArray stats =
        GET("/api/dataStatistics?startDate="
                + oneDayAgo.toString().substring(0, 10)
                + "&endDate="
                + tomorrow.toString().substring(0, 10)
                + "&interval=DAY&fields=year,month,week,day,dashboardViews,totalViews")
            .content();

    JsonObject stat = stats.get(0).asObject();

    String baseYear = String.valueOf(base.atZone(ZONE).getYear());
    String year = stat.get("year").toString();
    assertEquals(baseYear, year, "Expected year to be " + baseYear + " but was " + year);

    // Day should be the same
    String baseDay = String.valueOf(base.atZone(ZONE).getDayOfMonth());
    String day = stat.get("day").toString();
    assertEquals(baseDay, day, "Expected day to be " + baseDay + " but was " + day);

    // Month should be the same
    String baseMonth = String.valueOf(base.atZone(ZONE).getMonthValue());
    String month = stat.get("month").toString();
    assertEquals(baseMonth, month, "Expected month to be " + baseMonth + " but was " + month);

    // At least one view
    String dashboardViews = stat.get("dashboardViews").toString();
    int dv = 0;
    try {
      dv = Integer.parseInt(dashboardViews);
    } catch (NumberFormatException nfe) {
      fail("dashboardViews is not a number: " + dashboardViews);
    }
    assertTrue(dv >= 1, "Expected at least one dashboard view, but got " + dashboardViews);

    // Total views should be at least as high
    String totalViews = stat.get("totalViews").toString();
    int tv = 0;
    try {
      tv = Integer.parseInt(totalViews);
    } catch (NumberFormatException nfe) {
      fail("totalViews is not a number: " + totalViews);
    }
    assertTrue(tv >= dv, "Expected at least " + dv + " total views, but got " + totalViews);
  }
}
