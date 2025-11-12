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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
@TestInstance(Lifecycle.PER_CLASS)
@Transactional
class DataStatisticsServiceTest extends PostgresIntegrationTestBase {

  @Autowired private DataStatisticsService dataStatisticsService;

  @Autowired private DataStatisticsStore hibernateDataStatisticsStore;

  @Autowired private JdbcTemplate jdbc;

  @Autowired private TestSetup testSetup;

  private DataStatisticsEvent dse1;

  private DataStatisticsEvent dse2;

  private long snapId1;

  private DateTimeFormatter fmt;

  @BeforeAll
  void setUp() throws IOException {
    DateTime formatdate;
    fmt = DateTimeFormat.forPattern("yyyy-mm-dd");
    formatdate = fmt.parseDateTime("2016-03-22");
    Date now = formatdate.toDate();
    dse1 = new DataStatisticsEvent();
    dse2 = new DataStatisticsEvent(DataStatisticsEventType.VISUALIZATION_VIEW, now, "TestUser");
    DataStatistics ds =
        new DataStatistics(
            1.0, 1.5, 4.0, 5.0, 3.0, 6.0, 7.0, 8.0, 11.0, 10.0, 12.0, 11.0, 13.0, 20.0, 14.0, 17.0,
            11.0, 10, 18);
    hibernateDataStatisticsStore.save(ds);
    snapId1 = ds.getId();

    testSetup.importMetadata();
    injectSecurityContextUser(userService.getUser("tTgjgobT1oS"));
    testSetup.importTrackerData();
    var eventIds =
        jdbc.queryForList(
            "select eventid from singleevent order by eventid asc limit 2", Long.class);

    // backdate: 7 days
    jdbc.update(
        "update singleevent set lastupdated = now() - interval '7 days' where eventid = ?",
        eventIds.get(0));

    // backdate: 30 days
    jdbc.update(
        "update singleevent set lastupdated = now() - interval '30 days' where eventid = ?",
        eventIds.get(1));

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void testAddEvent() {
    int id = dataStatisticsService.addEvent(dse1);
    assertNotEquals(0, id);
  }

  @Test
  void testAddEventWithParams() {
    int id = dataStatisticsService.addEvent(dse2);
    assertNotEquals(0, id);
  }

  @Test
  void testSaveSnapshot() {
    Calendar c = Calendar.getInstance();
    DateTime formatdate;
    fmt = DateTimeFormat.forPattern("yyyy-mm-dd");
    c.add(Calendar.DAY_OF_MONTH, -2);
    formatdate = fmt.parseDateTime("2016-03-21");
    Date startDate = formatdate.toDate();
    dse1 =
        new DataStatisticsEvent(DataStatisticsEventType.VISUALIZATION_VIEW, startDate, "TestUser");
    dataStatisticsService.addEvent(dse1);
    dataStatisticsService.addEvent(dse2);
    long snapId2 = dataStatisticsService.saveDataStatisticsSnapshot(JobProgress.noop());
    assertTrue(snapId2 != 0);
    assertTrue(snapId1 != snapId2);
  }

  @Test
  void testGetSystemStatisticsSummary() {
    DataSummary summary = dataStatisticsService.getSystemStatisticsSummary();
    assertAll(
        () -> assertEquals(15, summary.getEventCount().get(0)),
        () -> assertEquals(15, summary.getEventCount().get(1)),
        () -> assertEquals(16, summary.getEventCount().get(7)),
        () -> assertEquals(17, summary.getEventCount().get(30)),
        () -> assertEquals(10, summary.getTrackerEventCount().get(0)),
        () -> assertEquals(10, summary.getTrackerEventCount().get(1)),
        () -> assertEquals(10, summary.getTrackerEventCount().get(7)),
        () -> assertEquals(10, summary.getTrackerEventCount().get(30)),
        () -> assertEquals(5, summary.getSingleEventCount().get(0)),
        () -> assertEquals(5, summary.getSingleEventCount().get(1)),
        () -> assertEquals(6, summary.getSingleEventCount().get(7)),
        () -> assertEquals(7, summary.getSingleEventCount().get(30)),
        () -> assertEquals(12, summary.getEnrollmentCount().get(0)),
        () -> assertEquals(12, summary.getEnrollmentCount().get(1)),
        () -> assertEquals(12, summary.getEnrollmentCount().get(7)),
        () -> assertEquals(12, summary.getEnrollmentCount().get(30)));
  }
}
