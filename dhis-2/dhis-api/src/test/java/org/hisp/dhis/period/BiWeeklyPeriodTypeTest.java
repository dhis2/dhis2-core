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
package org.hisp.dhis.period;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Kristian Wærstad
 */
class BiWeeklyPeriodTypeTest {

  private DateTime startDate;

  private DateTime endDate;

  private DateTime testDate;

  private BiWeeklyPeriodType periodType;

  @BeforeEach
  void before() {
    periodType = new BiWeeklyPeriodType();
  }

  @Test
  void testGetPeriodTypeEnum() {
    assertEquals(PeriodTypeEnum.BI_WEEKLY, periodType.getPeriodTypeEnum());
    assertEquals(PeriodTypeEnum.BI_WEEKLY.getName(), periodType.getName());
  }

  @Test
  void testCreatePeriod() {
    startDate = new DateTime(2018, 1, 1, 0, 0);
    endDate = new DateTime(2018, 1, 14, 0, 0);
    testDate = new DateTime(2018, 1, 8, 0, 0);
    Period period = periodType.createPeriod(testDate.toDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
    startDate = new DateTime(2017, 12, 18, 0, 0);
    endDate = new DateTime(2017, 12, 31, 0, 0);
    testDate = new DateTime(2017, 12, 29, 0, 0);
    period = periodType.createPeriod(testDate.toDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
  }

  @Test
  void testGetNextPeriod() {
    testDate = new DateTime(2018, 1, 3, 0, 0);
    Period period = periodType.createPeriod(testDate.toDate());
    period = periodType.getNextPeriod(period);
    startDate = new DateTime(2018, 1, 15, 0, 0);
    endDate = new DateTime(2018, 1, 28, 0, 0);
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
  }

  @Test
  void testGetPreviousPeriod() {
    testDate = new DateTime(2018, 1, 4, 0, 0);
    Period period = periodType.createPeriod(testDate.toDate());
    period = periodType.getPreviousPeriod(period);
    startDate = new DateTime(2017, 12, 18, 0, 0);
    endDate = new DateTime(2017, 12, 31, 0, 0);
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
  }

  @Test
  void testGeneratePeriods() {
    testDate = new DateTime(2018, 10, 5, 0, 0);
    List<Period> periods = periodType.generatePeriods(testDate.toDate());
    assertEquals(26, periods.size());
    assertEquals(periodType.createPeriod(new DateTime(2018, 1, 1, 0, 0).toDate()), periods.get(0));
    assertEquals(periodType.createPeriod(new DateTime(2018, 1, 15, 0, 0).toDate()), periods.get(1));
    assertEquals(periodType.createPeriod(new DateTime(2018, 1, 31, 0, 0).toDate()), periods.get(2));
  }

  @Test
  void testGenerateRollingPeriods() {
    testDate = new DateTime(2018, 1, 1, 0, 0);
    List<Period> periods = periodType.generateRollingPeriods(testDate.toDate());
    assertEquals(26, periods.size());
    assertEquals(periodType.createPeriod(new DateTime(2017, 1, 16, 0, 0).toDate()), periods.get(0));
    assertEquals(periodType.createPeriod(new DateTime(2017, 1, 30, 0, 0).toDate()), periods.get(1));
    assertEquals(periodType.createPeriod(new DateTime(2017, 2, 13, 0, 0).toDate()), periods.get(2));
    assertEquals(periodType.createPeriod(new DateTime(2017, 2, 28, 0, 0).toDate()), periods.get(3));
    assertEquals(periodType.createPeriod(new DateTime(2017, 3, 14, 0, 0).toDate()), periods.get(4));
    assertEquals(periodType.createPeriod(new DateTime(2017, 3, 29, 0, 0).toDate()), periods.get(5));
    assertEquals(periodType.createPeriod(testDate.toDate()), periods.get(periods.size() - 1));
  }

  @Test
  void testToIsoDate() {
    testDate = new DateTime(2018, 1, 1, 0, 0);
    List<Period> periods = periodType.generateRollingPeriods(testDate.toDate());
    assertEquals("2017BiW2", periodType.getIsoDate(periods.get(0)));
    assertEquals("2018BiW1", periodType.getIsoDate(periods.get(25)));
    testDate = new DateTime(2019, 1, 1, 0, 0);
    periods = periodType.generateRollingPeriods(testDate.toDate());
    assertEquals("2018BiW2", periodType.getIsoDate(periods.get(0)));
    assertEquals("2019BiW1", periodType.getIsoDate(periods.get(25)));
    testDate = new DateTime(2010, 1, 1, 0, 0);
    periods = periodType.generateRollingPeriods(testDate.toDate());
    assertEquals("2009BiW2", periodType.getIsoDate(periods.get(0)));
    assertEquals("2009BiW27", periodType.getIsoDate(periods.get(25)));
    testDate = new DateTime(2020, 1, 1, 0, 0);
    periods = periodType.generateRollingPeriods(testDate.toDate());
    assertEquals("2019BiW2", periodType.getIsoDate(periods.get(0)));
    assertEquals("2020BiW1", periodType.getIsoDate(periods.get(25)));
  }

  @Test
  void testGetRewindedDate() {
    assertEquals(
        new DateTime(2020, 1, 3, 0, 0).toDate(),
        periodType.getRewindedDate(new DateTime(2020, 2, 14, 0, 0).toDate(), 3));
    assertEquals(
        new DateTime(2020, 1, 31, 0, 0).toDate(),
        periodType.getRewindedDate(new DateTime(2020, 1, 3, 0, 0).toDate(), -2));
  }
}
