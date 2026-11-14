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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.id.enhanced.NoopOptimizer;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.LongType;
import org.hibernate.type.ManyToOneType;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@Transactional
class DataStatisticsStoreTest extends PostgresIntegrationTestBase {
  @Autowired private DataStatisticsStore dataStatisticsStore;

  @Autowired private DbmsManager dbmsManager;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  /** The count properties with their columns and getters. */
  private static final List<CountColumn> COUNT_COLUMNS =
      List.of(
          new CountColumn("mapViews", "mapviews", DataStatistics::getMapViews),
          new CountColumn(
              "visualizationViews", "visualizationviews", DataStatistics::getVisualizationViews),
          new CountColumn(
              "eventReportViews", "eventreportviews", DataStatistics::getEventReportViews),
          new CountColumn("eventChartViews", "eventchartviews", DataStatistics::getEventChartViews),
          new CountColumn(
              "eventVisualizationViews",
              "eventvisualizationviews",
              DataStatistics::getEventVisualizationViews),
          new CountColumn("dashboardViews", "dashboardviews", DataStatistics::getDashboardViews),
          new CountColumn(
              "passiveDashboardViews",
              "passivedashboardviews",
              DataStatistics::getPassiveDashboardViews),
          new CountColumn(
              "dataSetReportViews", "datasetreportviews", DataStatistics::getDataSetReportViews),
          new CountColumn("totalViews", "totalviews", DataStatistics::getTotalViews),
          new CountColumn("savedMaps", "maps", DataStatistics::getSavedMaps),
          new CountColumn(
              "savedVisualizations", "visualizations", DataStatistics::getSavedVisualizations),
          new CountColumn(
              "savedEventReports", "eventreports", DataStatistics::getSavedEventReports),
          new CountColumn("savedEventCharts", "eventcharts", DataStatistics::getSavedEventCharts),
          new CountColumn(
              "savedEventVisualizations",
              "eventvisualizations",
              DataStatistics::getSavedEventVisualizations),
          new CountColumn("savedDashboards", "dashboards", DataStatistics::getSavedDashboards),
          new CountColumn("savedIndicators", "indicators", DataStatistics::getSavedIndicators),
          new CountColumn("savedDataValues", "datavalues", DataStatistics::getSavedDataValues),
          new CountColumn("activeUsers", "active_users", DataStatistics::getActiveUsers),
          new CountColumn("users", "users", DataStatistics::getUsers));

  private record CountColumn(
      String property, String column, Function<DataStatistics, Long> getter) {}

  private DataStatistics ds1;

  private DataStatistics ds2;

  private DataStatistics ds3;

  private DataStatistics ds4;

  private DataStatistics ds5;

  private long ds1Id;

  private long ds2Id;

  private Date date;

  @BeforeEach
  void setUp() {
    ds1 = new DataStatistics();
    ds2 = new DataStatistics(1, 3, 4, 5, 3, 6, 17, 10, 8, 11, 14, 11, 15, 20, 16, 17, 11, 1, 18);
    ds3 = new DataStatistics(1, 4, 5, 6, 4, 7, 17, 8, 11, 12, 15, 12, 16, 21, 17, 18, 11, 2, 19);
    ds4 = new DataStatistics(1, 1, 6, 5, 5, 4, 16, 8, 10, 4, 9, 7, 14, 22, 6, 4, 12, 3, 2);
    ds5 = new DataStatistics(3, 4, 3, 5, 6, 7, 16, 8, 10, 2, 8, 8, 16, 23, 9, 10, 11, 2, 9);
    ds1Id = 0;
    ds2Id = 0;
    date = getDate(2016, 3, 21);
    ds1.setCreated(date);
    ds2.setCreated(date);
    ds3.setCreated(date);
    ds4.setCreated(date);
    ds5.setCreated(date);
  }

  @Test
  void saveSnapshotTest() {
    dataStatisticsStore.save(ds1);
    ds1Id = ds1.getId();
    dataStatisticsStore.save(ds2);
    ds2Id = ds2.getId();
    assertNotEquals(0L, ds1Id);
    assertNotEquals(0L, ds2Id);
  }

  @Test
  void getSnapshotsInIntervalGetInDAYTest() {
    dataStatisticsStore.save(ds2);
    dataStatisticsStore.save(ds3);
    dataStatisticsStore.save(ds4);
    dataStatisticsStore.save(ds5);
    dbmsManager.flushSession();
    List<AggregatedStatistics> asList =
        dataStatisticsStore.getSnapshotsInInterval(
            EventInterval.DAY, getDate(2015, 3, 21), getDate(2016, 3, 21));
    assertEquals(1, asList.size());
  }

  @Test
  void getSnapshotsInIntervalGetInDAY_DifferenDayesSavedTest() {
    date = getDate(2016, 3, 20);
    ds2.setCreated(date);
    dataStatisticsStore.save(ds2);
    dataStatisticsStore.save(ds3);
    dataStatisticsStore.save(ds4);
    dataStatisticsStore.save(ds5);
    dbmsManager.flushSession();
    List<AggregatedStatistics> asList =
        dataStatisticsStore.getSnapshotsInInterval(
            EventInterval.DAY, getDate(2015, 3, 19), getDate(2016, 3, 21));
    assertEquals(2, asList.size());
  }

  @Test
  void getSnapshotsInIntervalGetInDAY_GEDatesTest() {
    dataStatisticsStore.save(ds2);
    dataStatisticsStore.save(ds3);
    dataStatisticsStore.save(ds4);
    dataStatisticsStore.save(ds5);
    dbmsManager.flushSession();
    List<AggregatedStatistics> asList =
        dataStatisticsStore.getSnapshotsInInterval(
            EventInterval.DAY, getDate(2017, 3, 21), getDate(2017, 3, 22));
    assertEquals(0, asList.size());
  }

  @Test
  void getSnapshotsInIntervalGetInWEEKTest() {
    dataStatisticsStore.save(ds2);
    dataStatisticsStore.save(ds3);
    dataStatisticsStore.save(ds4);
    dataStatisticsStore.save(ds5);
    dbmsManager.flushSession();
    List<AggregatedStatistics> asList =
        dataStatisticsStore.getSnapshotsInInterval(
            EventInterval.WEEK, getDate(2015, 3, 21), getDate(2016, 3, 21));
    assertEquals(1, asList.size());
  }

  @Test
  void getSnapshotsInIntervalGetInMONTHTest() {
    dataStatisticsStore.save(ds2);
    dataStatisticsStore.save(ds3);
    dataStatisticsStore.save(ds4);
    dataStatisticsStore.save(ds5);
    dbmsManager.flushSession();
    List<AggregatedStatistics> asList =
        dataStatisticsStore.getSnapshotsInInterval(
            EventInterval.MONTH, getDate(2015, 3, 21), getDate(2016, 3, 21));
    assertEquals(1, asList.size());
  }

  @Test
  void getSnapshotsInIntervalGetInYEARTest() {
    dataStatisticsStore.save(ds2);
    dataStatisticsStore.save(ds3);
    dataStatisticsStore.save(ds4);
    dataStatisticsStore.save(ds5);
    dbmsManager.flushSession();

    List<AggregatedStatistics> asList =
        dataStatisticsStore.getSnapshotsInInterval(
            EventInterval.YEAR, getDate(2015, 3, 21), getDate(2016, 3, 21));

    assertEquals(1, asList.size());

    AggregatedStatistics as = asList.get(0);

    assertEquals(2016, as.getYear());
    assertEquals(6L, as.getMapViews());
    assertEquals(12L, as.getVisualizationViews());
    assertEquals(18L, as.getEventReportViews());
    assertEquals(21L, as.getEventChartViews());
    assertEquals(18L, as.getEventVisualizationViews());
    assertEquals(24L, as.getDashboardViews());
    assertEquals(66L, as.getPassiveDashboardViews());
    assertEquals(34L, as.getDataSetReportViews());
    assertEquals(39L, as.getTotalViews());
    assertEquals(13.0, as.getAverageViews(), 0.0001);
    assertEquals(2.0, as.getAverageMapViews(), 0.0001);
    assertEquals(4.0, as.getAverageVisualizationViews(), 0.0001);
    assertEquals(6.0, as.getAverageEventReportViews(), 0.0001);
    assertEquals(7.0, as.getAverageEventChartViews(), 0.0001);
    assertEquals(6.0, as.getAverageEventVisualizationViews(), 0.0001);
    assertEquals(8.0, as.getAverageDashboardViews(), 0.0001);
    assertEquals(22.0, as.getAveragePassiveDashboardViews(), 0.0001);
    assertEquals(29L, as.getSavedMaps());
    assertEquals(46L, as.getSavedVisualizations());
    assertEquals(38L, as.getSavedEventReports());
    assertEquals(61L, as.getSavedEventCharts());
    assertEquals(86L, as.getSavedEventVisualizations());
    assertEquals(48L, as.getSavedDashboards());
    assertEquals(49L, as.getSavedIndicators());
    assertEquals(45L, as.getSavedDataValues());
    assertEquals(3L, as.getActiveUsers());
    assertEquals(19L, as.getUsers());
  }

  @Test
  void shouldMapDataStatisticsToItsTableColumnsAndSequence() {
    AbstractEntityPersister persister = persister();

    assertEquals("datastatistics", persister.getTableName());
    assertArrayEquals(new String[] {"statisticsid"}, persister.getIdentifierColumnNames());
    SequenceStyleGenerator generator =
        assertInstanceOf(SequenceStyleGenerator.class, persister.getIdentifierGenerator());
    assertEquals(
        "hibernate_sequence",
        generator.getDatabaseStructure().getPhysicalName().getObjectName().getText());
    assertEquals(1, generator.getDatabaseStructure().getIncrementSize());
    assertInstanceOf(NoopOptimizer.class, generator.getOptimizer());

    Map<String, String> columns = new LinkedHashMap<>();
    COUNT_COLUMNS.forEach(count -> columns.put(count.property(), count.column()));
    columns.put("uid", "uid");
    columns.put("code", "code");
    columns.put("created", "created");
    columns.put("lastUpdated", "lastupdated");
    columns.put("lastUpdatedBy", "lastupdatedby");
    assertEquals(columns.size(), persister.getPropertyNames().length);
    columns.forEach(
        (property, column) ->
            assertEquals(
                column,
                persister.getPropertyColumnNames(property)[0].toLowerCase(),
                property + " column"));
  }

  @Test
  void shouldMapDataStatisticsNullabilityTypesLazinessAndNoCache() {
    AbstractEntityPersister persister = persister();

    for (String property : List.of("uid", "created", "lastUpdated")) {
      assertFalse(isNullable(persister, property), property + " is mapped nullable");
    }
    assertTrue(isNullable(persister, "code"), "code is mapped not-null");
    assertTrue(isNullable(persister, "lastUpdatedBy"), "lastUpdatedBy is mapped not-null");
    for (CountColumn count : COUNT_COLUMNS) {
      assertTrue(isNullable(persister, count.property()), count.property() + " is mapped not-null");
      assertInstanceOf(
          LongType.class, persister.getPropertyType(count.property()), count.property());
    }
    assertFalse(
        persister.getPropertyUpdateability()[persister.getPropertyIndex("created")],
        "created is updateable");
    assertFalse(((ManyToOneType) persister.getPropertyType("lastUpdatedBy")).isEager(null));
    assertFalse(persister.canReadFromCache());
  }

  @Test
  void shouldWriteAndReadEveryDataStatisticsColumn() {
    DataStatistics statistics =
        new DataStatistics(
            101,
            102,
            103,
            104,
            105,
            106,
            107,
            108,
            109,
            110,
            111,
            112,
            113,
            114,
            115,
            116,
            3_000_000_000L,
            118,
            119);
    statistics.setCode("STATS_A");
    statistics.setCreated(date);
    dataStatisticsStore.save(statistics);
    dbmsManager.flushSession();

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            "select * from datastatistics where statisticsid = ?", statistics.getId());
    assertEquals(statistics.getUid(), row.get("uid"));
    assertEquals("STATS_A", row.get("code"));
    assertEquals(date.getTime(), ((Date) row.get("created")).getTime());
    assertEquals(statistics.getLastUpdated().getTime(), ((Date) row.get("lastupdated")).getTime());
    assertEquals(getAdminUser().getId(), ((Number) row.get("lastupdatedby")).longValue());
    Map<String, Long> expected = countsOf(statistics);
    COUNT_COLUMNS.forEach(
        count ->
            assertEquals(
                expected.get(count.property()),
                ((Number) row.get(count.column())).longValue(),
                count.column()));

    dbmsManager.clearSession();
    DataStatistics loaded = dataStatisticsStore.get(statistics.getId());
    assertEquals(statistics.getUid(), loaded.getUid());
    assertEquals("STATS_A", loaded.getCode());
    assertEquals(date.getTime(), loaded.getCreated().getTime());
    assertEquals(statistics.getLastUpdated().getTime(), loaded.getLastUpdated().getTime());
    assertEquals(getAdminUser().getUid(), loaded.getLastUpdatedBy().getUid());
    assertEquals(expected, countsOf(loaded));
  }

  @Test
  void shouldEqualProxyWithSameUidAndCode() {
    ds2.setCode("STATS_B");
    dataStatisticsStore.save(ds2);
    dbmsManager.flushSession();
    dbmsManager.clearSession();

    DataStatistics proxy = entityManager.getReference(DataStatistics.class, ds2.getId());

    assertFalse(Hibernate.isInitialized(proxy));
    assertEquals(ds2, proxy);
    assertEquals(proxy, ds2);
    assertEquals(ds2.hashCode(), proxy.hashCode());
  }

  private static Map<String, Long> countsOf(DataStatistics statistics) {
    Map<String, Long> counts = new LinkedHashMap<>();
    COUNT_COLUMNS.forEach(count -> counts.put(count.property(), count.getter().apply(statistics)));
    return counts;
  }

  private AbstractEntityPersister persister() {
    return (AbstractEntityPersister)
        entityManagerFactory
            .unwrap(SessionFactoryImplementor.class)
            .getMetamodel()
            .entityPersister(DataStatistics.class);
  }

  private static boolean isNullable(AbstractEntityPersister persister, String property) {
    return persister.getPropertyNullability()[persister.getPropertyIndex(property)];
  }
}
