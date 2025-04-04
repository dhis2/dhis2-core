/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.datastatistics;

import static org.hisp.dhis.datastatistics.DataStatisticsEventType.DASHBOARD_VIEW;
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.EVENT_CHART_VIEW;
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.PASSIVE_DASHBOARD_VIEW;
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.VISUALIZATION_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
class DataStatisticsEventStoreTest extends PostgresIntegrationTestBase {
  @Autowired private DataStatisticsEventStore dataStatisticsEventStore;

  @Autowired private DashboardService dashboardService;

  @Autowired private SystemSettingsService settingsService;

  @Autowired private DataStatisticsService dataStatisticsService;

  private static final Date start = getDate(2016, 3, 19);

  private static final Date end = getDate(2016, 3, 21);

  private static final Date eventDate = getDate(2016, 3, 20);

  private static final String DASHBOARD_UID = "anyUid12345";

  @BeforeEach
  void setUp() {
    // Stub objects.
    final Dashboard dashboard = new Dashboard("anyName");
    dashboard.setUid(DASHBOARD_UID);

    final DataStatisticsEvent visualizationEvent =
        new DataStatisticsEvent(VISUALIZATION_VIEW, eventDate, "anyUser");

    final DataStatisticsEvent chartEvent =
        new DataStatisticsEvent(EVENT_CHART_VIEW, eventDate, "anyUser");

    final DataStatisticsEvent dashboardEvent =
        new DataStatisticsEvent(DASHBOARD_VIEW, eventDate, "anyUser", DASHBOARD_UID);

    final DataStatisticsEvent passiveDashEvent =
        new DataStatisticsEvent(PASSIVE_DASHBOARD_VIEW, eventDate, "anyUser", DASHBOARD_UID);

    // Add four events.
    assertTrue(dataStatisticsService.addEvent(visualizationEvent) != 0);
    assertTrue(dataStatisticsService.addEvent(dashboardEvent) != 0);
    assertTrue(dataStatisticsService.addEvent(chartEvent) != 0);
    assertTrue(dataStatisticsService.addEvent(passiveDashEvent) != 0);

    // Add one dashboard.
    assertTrue(dashboardService.saveDashboard(dashboard) != 0);
  }

  @Test
  void getDataStatisticsEventCountTest() {
    // When
    final Map<DataStatisticsEventType, Double> eventsMap =
        dataStatisticsEventStore.getDataStatisticsEventCount(start, end);

    // Then
    // Test for 6 objects because TOTAL_VIEW and ACTIVE_USERS is always present.
    assertEquals(6, eventsMap.size());
  }

  @Test
  void getDataStatisticsEventCountCorrectContentTest() {
    // Given
    final double expectedSize = 1.0;

    // When
    final Map<DataStatisticsEventType, Double> eventsMap =
        dataStatisticsEventStore.getDataStatisticsEventCount(start, end);

    // Then
    assertEquals(expectedSize, eventsMap.get(VISUALIZATION_VIEW));
    assertEquals(expectedSize, eventsMap.get(EVENT_CHART_VIEW));
    assertEquals(expectedSize, eventsMap.get(DASHBOARD_VIEW));
    assertEquals(expectedSize, eventsMap.get(PASSIVE_DASHBOARD_VIEW));
  }

  @Test
  void getDataStatisticsEventCountNonExistingDatesTest() {
    // When
    final Map<DataStatisticsEventType, Double> eventsMap =
        dataStatisticsEventStore.getDataStatisticsEventCount(new Date(), new Date());

    // Test that the map contains the TOTAL_VIEW and ACTIVE_USERS keys
    assertTrue(eventsMap.containsKey(DataStatisticsEventType.TOTAL_VIEW));
    assertTrue(eventsMap.containsKey(DataStatisticsEventType.ACTIVE_USERS));
    assertEquals(2, eventsMap.size());
  }

  @Test
  void getTopFavoritesDataTest() {
    // When
    final List<FavoriteStatistics> activeDashboardStats =
        dataStatisticsService.getTopFavorites(DASHBOARD_VIEW, 100, SortOrder.ASC, null);

    final List<FavoriteStatistics> passiveDashboardStats =
        dataStatisticsService.getTopFavorites(PASSIVE_DASHBOARD_VIEW, 100, SortOrder.ASC, null);

    // Then
    assertEquals(1, activeDashboardStats.size());
    assertEquals(1, passiveDashboardStats.size());
  }

  @Test
  void getFavoriteStatisticsTest() {
    // When
    final FavoriteStatistics activeDashboardStats =
        dataStatisticsService.getFavoriteStatistics(DASHBOARD_UID);

    settingsService.put("keyCountPassiveDashboardViewsInUsageAnalytics", true);
    settingsService.clearCurrentSettings();
    final FavoriteStatistics activePlusPassiveDashboardStats =
        dataStatisticsService.getFavoriteStatistics(DASHBOARD_UID);

    // Then
    assertEquals(1, activeDashboardStats.getViews());
    assertEquals(2, activePlusPassiveDashboardStats.getViews());
  }
}
