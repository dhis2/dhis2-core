/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yrjan A. F. Fraschetti
 * @author Julie Hill Roa
 */
class DataStatisticsServiceTest extends SingleSetupIntegrationTestBase {

  @Autowired private DataStatisticsService dataStatisticsService;

  @Autowired private DataStatisticsStore hibernateDataStatisticsStore;

  private DataStatisticsEvent dse1;

  private DataStatisticsEvent dse2;

  private long snapId1;

  private DateTimeFormatter fmt;

  @Override
  public void setUpTest() throws Exception {
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
  }

  @Test
  void testAddEvent() throws Exception {
    int id = dataStatisticsService.addEvent(dse1);
    assertNotEquals(0, id);
  }

  @Test
  void testAddEventWithParams() throws Exception {
    int id = dataStatisticsService.addEvent(dse2);
    assertNotEquals(0, id);
  }

  @Test
  void testSaveSnapshot() throws Exception {
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
    long snapId2 = dataStatisticsService.saveDataStatisticsSnapshot(NoopJobProgress.INSTANCE);
    assertTrue(snapId2 != 0);
    assertTrue(snapId1 != snapId2);
  }
}
