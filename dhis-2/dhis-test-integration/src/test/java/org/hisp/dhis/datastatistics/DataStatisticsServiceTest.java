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
package org.hisp.dhis.datastatistics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.datasummary.DataSummary;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.junit.jupiter.api.AfterAll;
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
  private Date dayStart;
  private long snapId1;
  private static final ZoneId ZONE = DefaultDataStatisticsService.SERVER_ZONE;
  private List<Long> eventIds;

  @BeforeAll
  void setUp() throws IOException {
    LocalDate fixedDate = LocalDate.of(2016, 3, 22);
    dayStart = toDate(fixedDate.atStartOfDay());

    dse1 = new DataStatisticsEvent();
    dse2 =
        new DataStatisticsEvent(DataStatisticsEventType.VISUALIZATION_VIEW, dayStart, "TestUser");
    DataStatistics ds =
        new DataStatistics(
            1L, 2L, 4L, 5L, 3L, 6L, 7L, 8L, 11L, 10L, 12L, 11L, 13L, 20L, 14L, 17L, 11L, 10L, 18L);
    hibernateDataStatisticsStore.save(ds);
    snapId1 = ds.getId();

    try {
      testSetup.importMetadata();
    } catch (IOException e) {
      fail("Metadata import failed", e);
    }
    injectSecurityContextUser(userService.getUser("tTgjgobT1oS"));
    try {
      testSetup.importTrackerData();
    } catch (IOException e) {
      fail("Tracker import failed", e);
    }
    eventIds =
        jdbc.queryForList(
            "select eventid from singleevent order by eventid asc limit 3", long.class);
    // Be sure we have at least three
    assertTrue(eventIds.size() >= 3);

    Instant base = Instant.now().atZone(ZONE).toInstant();

    // Boundaries
    Instant sevenDaysAgo = base.minus(7, ChronoUnit.DAYS);
    Instant oneDayAgo = base.minus(1, ChronoUnit.DAYS);
    Instant oneHourAgo = base.minus(1, ChronoUnit.HOURS);

    // Avoid edge cases by subtracting extra minutes
    Instant olderThanHour = oneHourAgo.minus(5, ChronoUnit.MINUTES);
    Instant olderThanDay = oneDayAgo.minus(5, ChronoUnit.MINUTES);
    Instant olderThanSevenDays = sevenDaysAgo.minus(5, ChronoUnit.MINUTES);

    // Backdate the three events
    jdbc.update(
        "update singleevent set lastupdated = ? where eventid = ?",
        Timestamp.from(olderThanHour),
        eventIds.get(1));

    jdbc.update(
        "update singleevent set lastupdated = ? where eventid = ?",
        Timestamp.from(olderThanDay),
        eventIds.get(0));

    jdbc.update(
        "update singleevent set lastupdated = ? where eventid = ?",
        Timestamp.from(olderThanSevenDays),
        eventIds.get(2));

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
    Date twoDaysBefore = addDays(dayStart, -2);
    dse1 =
        new DataStatisticsEvent(
            DataStatisticsEventType.VISUALIZATION_VIEW, twoDaysBefore, "TestUser");
    dataStatisticsService.addEvent(dse1);
    dataStatisticsService.addEvent(dse2);
    long snapId2 = dataStatisticsService.saveDataStatisticsSnapshot(JobProgress.noop());
    assertNotEquals(0, snapId2);
    assertNotEquals(snapId1, snapId2);
  }

  @Test
  void testGetSystemStatisticsSummary() {
    DataSummary summary = dataStatisticsService.getSystemStatisticsSummary();

    assertAll(
        () -> assertEquals(15L, summary.getEventCount().get(1)),
        () -> assertEquals(16L, summary.getEventCount().get(7)),
        () -> assertEquals(17L, summary.getEventCount().get(30)),
        () -> assertEquals(10L, summary.getTrackerEventCount().get(0)),
        () -> assertEquals(10L, summary.getTrackerEventCount().get(1)),
        () -> assertEquals(10L, summary.getTrackerEventCount().get(7)),
        () -> assertEquals(10L, summary.getTrackerEventCount().get(30)),
        () -> assertEquals(4L, summary.getSingleEventCount().get(0)),
        () -> assertEquals(5L, summary.getSingleEventCount().get(1)),
        () -> assertEquals(6L, summary.getSingleEventCount().get(7)),
        () -> assertEquals(7L, summary.getSingleEventCount().get(30)),
        () -> assertEquals(12L, summary.getEnrollmentCount().get(0)),
        () -> assertEquals(12L, summary.getEnrollmentCount().get(1)),
        () -> assertEquals(12L, summary.getEnrollmentCount().get(7)),
        () -> assertEquals(12L, summary.getEnrollmentCount().get(30)));
  }

  // --- Helpers ---
  private Date addDays(Date base, int days) {
    Instant instant = base.toInstant().plus(Duration.ofDays(days));
    return Date.from(instant);
  }

  private Date toDate(LocalDateTime ldt) {
    return Date.from(ldt.atZone(ZONE).toInstant());
  }

  @AfterAll
  void tearDown() {
    // Truncate affected tables
    jdbc.execute("TRUNCATE TABLE datastatisticsevent CASCADE");
    jdbc.execute("TRUNCATE TABLE datastatistics CASCADE");
  }
}
