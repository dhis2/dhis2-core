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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@Transactional
class DataStatisticsStoreTest extends PostgresIntegrationTestBase {
  @Autowired private DataStatisticsStore dataStatisticsStore;

  @Autowired private DbmsManager dbmsManager;

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

    assertEquals(2016, as.year());
    assertEquals(6L, as.mapViews());
    assertEquals(12L, as.visualizationViews());
    assertEquals(18L, as.eventReportViews());
    assertEquals(21L, as.eventChartViews());
    assertEquals(18L, as.eventVisualizationViews());
    assertEquals(24L, as.dashboardViews());
    assertEquals(66L, as.passiveDashboardViews());
    assertEquals(34L, as.dataSetReportViews());
    assertEquals(39L, as.totalViews());
    assertEquals(13.0, as.averageViews(), 0.0001);
    assertEquals(2.0, as.averageMapViews(), 0.0001);
    assertEquals(4.0, as.averageVisualizationViews(), 0.0001);
    assertEquals(6.0, as.averageEventReportViews(), 0.0001);
    assertEquals(7.0, as.averageEventChartViews(), 0.0001);
    assertEquals(6.0, as.averageEventVisualizationViews(), 0.0001);
    assertEquals(8.0, as.averageDashboardViews(), 0.0001);
    assertEquals(22.0, as.averagePassiveDashboardViews(), 0.0001);
    assertEquals(29L, as.savedMaps());
    assertEquals(46L, as.savedVisualizations());
    assertEquals(38L, as.savedEventReports());
    assertEquals(61L, as.savedEventCharts());
    assertEquals(86L, as.savedEventVisualizations());
    assertEquals(48L, as.savedDashboards());
    assertEquals(49L, as.savedIndicators());
    assertEquals(45L, as.savedDataValues());
    assertEquals(3L, as.activeUsers());
    assertEquals(19L, as.users());
  }
}
