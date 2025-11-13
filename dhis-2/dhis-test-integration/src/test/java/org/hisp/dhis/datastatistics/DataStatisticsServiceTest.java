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

import static org.junit.jupiter.api.Assertions.assertNotEquals;


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
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

  private DataStatisticsEvent dse1;

  private DataStatisticsEvent dse2;

  private long snapId1;
  private ZoneId zone;
  private Date dayStart;

  @BeforeAll
  void setUp() {
    zone = ZoneId.systemDefault();
    LocalDate fixedDate = LocalDate.of(2016, 3, 22);
    dayStart = toDate(fixedDate.atStartOfDay());

    dse1 = new DataStatisticsEvent();
    dse2 =
        new DataStatisticsEvent(DataStatisticsEventType.VISUALIZATION_VIEW, dayStart, "TestUser");
    DataStatistics ds =
        new DataStatistics(
            1.0, 1.5, 4.0, 5.0, 3.0, 6.0, 7.0, 8.0, 11.0, 10.0, 12.0, 11.0, 13.0, 20.0, 14.0, 17.0,
            11.0, 10, 18);
    hibernateDataStatisticsStore.save(ds);
    snapId1 = ds.getId();
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
    assertNotEquals(snapId1 , snapId2);
  }

  // --- Helpers ---

  private Date addDays(Date base, int days) {
    Instant instant = base.toInstant().plus(Duration.ofDays(days));
    return Date.from(instant);
  }

  private Date toDate(LocalDateTime ldt) {
    return Date.from(ldt.atZone(zone).toInstant());
  }
}
