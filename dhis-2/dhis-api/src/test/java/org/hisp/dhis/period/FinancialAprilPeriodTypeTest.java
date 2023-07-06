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
import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class FinancialAprilPeriodTypeTest {

  private LocalDateTime startDate;

  private LocalDateTime endDate;

  private LocalDateTime testDate;

  private CalendarPeriodType periodType;

  @BeforeEach
  void before() {
    periodType = new FinancialAprilPeriodType();
  }

  @Test
  void testCreatePeriod() {
    testDate = new LocalDateTime(2009, 2, 15, 0, 0);
    startDate = new LocalDateTime(2008, 4, 1, 0, 0);
    endDate = new LocalDateTime(2009, 3, 31, 0, 0);
    Period period = periodType.createPeriod(testDate.toDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
    testDate = new LocalDateTime(2009, 9, 12, 0, 0);
    period = periodType.createPeriod(testDate.toDate());
    startDate = new LocalDateTime(2009, 4, 1, 0, 0);
    endDate = new LocalDateTime(2010, 3, 31, 0, 0);
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
  }

  @Test
  void testGetNextPeriod() {
    testDate = new LocalDateTime(2009, 2, 15, 0, 0);
    Period period = periodType.createPeriod(testDate.toDate());
    period = periodType.getNextPeriod(period);
    startDate = new LocalDateTime(2009, 4, 1, 0, 0);
    endDate = new LocalDateTime(2010, 3, 31, 0, 0);
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
  }

  @Test
  void testGetPreviousPeriod() {
    testDate = new LocalDateTime(2009, 2, 15, 0, 0);
    Period period = periodType.createPeriod(testDate.toDate());
    period = periodType.getPreviousPeriod(period);
    startDate = new LocalDateTime(2007, 4, 1, 0, 0);
    endDate = new LocalDateTime(2008, 3, 31, 0, 0);
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
  }

  @Test
  void testGeneratePeriods() {
    testDate = new LocalDateTime(2009, 2, 15, 0, 0);
    List<Period> periods = periodType.generatePeriods(testDate.toDate());
    assertEquals(11, periods.size());
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2003, 4, 1, 0, 0).toDate()), periods.get(0));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2004, 4, 1, 0, 0).toDate()), periods.get(1));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2005, 4, 1, 0, 0).toDate()), periods.get(2));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2006, 4, 1, 0, 0).toDate()), periods.get(3));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2007, 4, 1, 0, 0).toDate()), periods.get(4));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2008, 4, 1, 0, 0).toDate()), periods.get(5));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2009, 4, 1, 0, 0).toDate()), periods.get(6));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2010, 4, 1, 0, 0).toDate()), periods.get(7));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2011, 4, 1, 0, 0).toDate()), periods.get(8));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2012, 4, 1, 0, 0).toDate()), periods.get(9));
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2013, 4, 1, 0, 0).toDate()), periods.get(10));
    testDate = new LocalDateTime(2009, 9, 12, 0, 0);
    periods = periodType.generatePeriods(testDate.toDate());
    assertEquals(11, periods.size());
    assertEquals(
        periodType.createPeriod(new LocalDateTime(2004, 4, 1, 0, 0).toDate()), periods.get(0));
  }

  @Test
  void testGetRewindedDate() {
    assertEquals(
        new DateTime(2020, 1, 15, 0, 0).toDate(),
        periodType.getRewindedDate(new DateTime(2023, 1, 15, 0, 0).toDate(), 3));
    assertEquals(
        new DateTime(2022, 1, 1, 0, 0).toDate(),
        periodType.getRewindedDate(new DateTime(2020, 1, 1, 0, 0).toDate(), -2));
  }
}
