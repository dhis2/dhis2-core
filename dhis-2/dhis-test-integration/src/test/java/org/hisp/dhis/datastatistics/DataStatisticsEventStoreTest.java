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
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.MAP_VIEW;
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.PASSIVE_DASHBOARD_VIEW;
import static org.hisp.dhis.datastatistics.DataStatisticsEventType.VISUALIZATION_VIEW;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.CustomType;
import org.hibernate.type.EnumType;
import org.hibernate.type.TimestampType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.dashboard.Dashboard;
import org.hisp.dhis.dashboard.DashboardService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@Transactional
class DataStatisticsEventStoreTest extends PostgresIntegrationTestBase {
  @Autowired private DataStatisticsEventStore dataStatisticsEventStore;

  @Autowired private DashboardService dashboardService;

  @Autowired private SystemSettingsService settingsService;

  @Autowired private DataStatisticsService dataStatisticsService;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private EntityManagerFactory entityManagerFactory;

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

    // flush so the raw-SQL statistics-store reads see the session writes
    entityManager.flush();
  }

  @Test
  void getDataStatisticsEventCountTest() {
    final Map<DataStatisticsEventType, Long> eventsMap =
        dataStatisticsEventStore.getDataStatisticsEventCount(start, end);

    // Test for 6 objects because TOTAL_VIEW and ACTIVE_USERS is always present.
    assertEquals(6, eventsMap.size());
  }

  @Test
  void getDataStatisticsEventCountCorrectContentTest() {
    final Map<DataStatisticsEventType, Long> eventsMap =
        dataStatisticsEventStore.getDataStatisticsEventCount(start, end);

    assertEquals(1, eventsMap.get(VISUALIZATION_VIEW));
    assertEquals(1, eventsMap.get(EVENT_CHART_VIEW));
    assertEquals(1, eventsMap.get(DASHBOARD_VIEW));
    assertEquals(1, eventsMap.get(PASSIVE_DASHBOARD_VIEW));
  }

  @Test
  void getDataStatisticsEventCountNonExistingDatesTest() {
    final Map<DataStatisticsEventType, Long> eventsMap =
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

  @Test
  void getDistinctActiveUserCountsTest() {
    dataStatisticsEventStore.save(
        new DataStatisticsEvent(VISUALIZATION_VIEW, getDate(2017, 5, 1), "userA"));
    dataStatisticsEventStore.save(
        new DataStatisticsEvent(VISUALIZATION_VIEW, getDate(2017, 5, 10), "userB"));
    dataStatisticsEventStore.save(
        new DataStatisticsEvent(DASHBOARD_VIEW, getDate(2017, 5, 10), "userA"));
    entityManager.flush();

    List<Integer> counts =
        dataStatisticsEventStore.getDistinctActiveUserCounts(
            List.of(getDate(2017, 5, 1), getDate(2017, 5, 5)), getDate(2017, 5, 31));

    assertEquals(List.of(2, 2), counts);
    assertEquals(
        List.of(1),
        dataStatisticsEventStore.getDistinctActiveUserCounts(
            List.of(getDate(2017, 5, 1)), getDate(2017, 5, 2)));
  }

  // -------------------------------------------------------------------------
  // Mapping
  // -------------------------------------------------------------------------

  @Test
  @DisplayName(
      "the id is generated by a sequence generator on hibernate_sequence, one nextval per id")
  void idGeneratorUsesHibernateSequenceTest() {
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister().getIdentifierGenerator());

    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
    assertInstanceOf(NoopOptimizer.class, generator.getOptimizer());
  }

  @Test
  @DisplayName("every property is written to its column and read back from it")
  void columnsRoundTripTest() {
    Timestamp timestamp = Timestamp.valueOf("2017-06-15 13:45:30.123");
    DataStatisticsEvent saved =
        save(new DataStatisticsEvent(MAP_VIEW, timestamp, "mappingUser", "favoriteUid"));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            select eventtype, timestamp, username, favoriteuid
            from datastatisticsevent where eventid = ?""",
            saved.getId());
    assertEquals("MAP_VIEW", row.get("eventtype"));
    assertEquals(timestamp.getTime(), ((Date) row.get("timestamp")).getTime());
    assertEquals("mappingUser", row.get("username"));
    assertEquals("favoriteUid", row.get("favoriteuid"));

    DataStatisticsEvent reloaded = reload(saved);
    assertEquals(saved.getId(), reloaded.getId());
    assertSame(MAP_VIEW, reloaded.getEventType());
    assertEquals(timestamp.getTime(), reloaded.getTimestamp().getTime());
    assertEquals("mappingUser", reloaded.getUsername());
    assertEquals("favoriteUid", reloaded.getFavoriteUid());
  }

  @Test
  @DisplayName("an event without favorite, user, type and timestamp is stored with null columns")
  void nullColumnsRoundTripTest() {
    DataStatisticsEvent reloaded = reload(save(new DataStatisticsEvent()));

    assertNull(reloaded.getEventType());
    assertNull(reloaded.getTimestamp());
    assertNull(reloaded.getUsername());
    assertNull(reloaded.getFavoriteUid());
  }

  @Test
  @DisplayName("the timestamp is bound as SQL timestamp, keeping the time of day")
  void timestampIsBoundAsSqlTimestampTest() {
    assertInstanceOf(TimestampType.class, persister().getPropertyType("timestamp"));

    Date timestamp = Timestamp.valueOf("2017-06-16 23:59:58.5");
    DataStatisticsEvent reloaded =
        reload(save(new DataStatisticsEvent(MAP_VIEW, timestamp, "mappingUser")));
    assertSame(Timestamp.class, reloaded.getTimestamp().getClass());
    assertEquals(timestamp.getTime(), reloaded.getTimestamp().getTime());
  }

  @Test
  @DisplayName(
      "the event type is a named varchar enum, all columns are nullable, no cache, no version")
  void persisterMetadataTest() {
    AbstractEntityPersister persister = persister();

    CustomType type = assertInstanceOf(CustomType.class, persister.getPropertyType("eventType"));
    EnumType<?> enumType = assertInstanceOf(EnumType.class, type.getUserType());
    assertFalse(enumType.isOrdinal());
    assertSame(DataStatisticsEventType.class, enumType.returnedClass());
    assertArrayEquals(new int[] {Types.VARCHAR}, enumType.sqlTypes());

    boolean[] nullable = persister.getPropertyNullability();
    assertEquals(4, nullable.length);
    for (String property : List.of("eventType", "timestamp", "username", "favoriteUid")) {
      assertTrue(nullable[persister.getEntityMetamodel().getPropertyIndex(property)], property);
    }

    assertFalse(persister.canReadFromCache());
    assertFalse(persister.isVersioned());
  }

  private DataStatisticsEvent save(DataStatisticsEvent event) {
    dataStatisticsEventStore.save(event);
    entityManager.flush();
    return event;
  }

  private DataStatisticsEvent reload(DataStatisticsEvent event) {
    clearSession();
    return entityManager.find(DataStatisticsEvent.class, event.getId());
  }

  private AbstractEntityPersister persister() {
    return (AbstractEntityPersister)
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(DataStatisticsEvent.class);
  }
}
