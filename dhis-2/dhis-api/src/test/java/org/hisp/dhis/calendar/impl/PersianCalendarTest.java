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
package org.hisp.dhis.calendar.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateInterval;
import org.hisp.dhis.calendar.DateIntervalType;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.calendar.exception.InvalidCalendarParametersException;
import org.hisp.dhis.period.Cal;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Hans Jacobson <jacobson.hans@gmail.com>
 */
class PersianCalendarTest {

  private Calendar calendar;

  @BeforeEach
  void init() {
    calendar = PersianCalendar.getInstance();
  }

  @Test
  void testIsoStartOfYear() {
    DateTimeUnit startOfYear = calendar.isoStartOfYear(1383);
    assertEquals(2004, startOfYear.getYear());
    assertEquals(3, startOfYear.getMonth());
    assertEquals(20, startOfYear.getDay());
    startOfYear = calendar.isoStartOfYear(1409);
    assertEquals(2030, startOfYear.getYear());
    assertEquals(3, startOfYear.getMonth());
    assertEquals(21, startOfYear.getDay());
  }

  @Test
  void testDaysInMonth13() {
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> calendar.daysInMonth(1389, 13));
  }

  @Test
  void testGetDaysFromMapEx() {
    assertThrows(InvalidCalendarParametersException.class, () -> calendar.daysInMonth(1500, 7));
  }

  public void testGetDaysFromMap() {
    assertEquals(29, calendar.daysInMonth(1408, 12));
  }

  @Test
  void testDaysInMonth() {
    assertEquals(29, calendar.daysInMonth(1389, 12));
    assertEquals(30, calendar.daysInMonth(1395, 12));
  }

  @Test
  void testDaysInYears() {
    assertEquals(365, calendar.daysInYear(1389));
    assertEquals(366, calendar.daysInYear(1395));
  }

  @Test
  void testToIso() {
    assertEquals(new DateTimeUnit(1993, 3, 21, true), calendar.toIso(new DateTimeUnit(1372, 1, 1)));
    assertEquals(new DateTimeUnit(2020, 3, 20, true), calendar.toIso(new DateTimeUnit(1399, 1, 1)));
    assertEquals(
        new DateTimeUnit(2020, 3, 20, true), calendar.toIso(new DateTimeUnit(2020, 3, 20)));
  }

  @Test
  void testFromIso() {
    assertEquals(
        new DateTimeUnit(1372, 1, 1, false), calendar.fromIso(new DateTimeUnit(1993, 3, 21, true)));
    assertEquals(
        new DateTimeUnit(1399, 1, 1, false), calendar.fromIso(new DateTimeUnit(2020, 3, 20, true)));
    assertEquals(
        new DateTimeUnit(1383, 1, 1, false), calendar.fromIso(new DateTimeUnit(2004, 3, 20, true)));
    assertEquals(
        new DateTimeUnit(1383, 1, 1, false), calendar.fromIso(new DateTimeUnit(1383, 1, 1, true)));
  }

  @Test
  void testGenerateMonthlyPeriods() {
    Date startDate = new Cal(1997, 1, 1, true).time();
    Date endDate = new Cal(1998, 1, 1, true).time();
    List<Period> monthly = new MonthlyPeriodType().generatePeriods(calendar, startDate, endDate);
    assertEquals(13, monthly.size());
  }

  @Test
  void testGenerateQuarterlyPeriods() {
    Date startDate = new Cal(2017, 3, 21, true).time();
    Date endDate = new Cal(2017, 6, 21, true).time();
    List<Period> monthly = new QuarterlyPeriodType().generatePeriods(calendar, startDate, endDate);
    assertEquals(1, monthly.size());
  }

  @Test
  void testToInterval() {
    DateTimeUnit start = new DateTimeUnit(1373, 6, 1, java.util.Calendar.FRIDAY);
    DateInterval interval = calendar.toInterval(start, DateIntervalType.ISO8601_DAY, 0, 10);
    assertEquals(1994, interval.getTo().getYear());
    assertEquals(9, interval.getTo().getMonth());
    assertEquals(2, interval.getTo().getDay());
  }

  @Test
  void testPlusDays() {
    DateTimeUnit dateTimeUnit = new DateTimeUnit(1382, 1, 1);
    DateTimeUnit testDateTimeUnit = calendar.plusDays(dateTimeUnit, 31);
    assertEquals(1382, testDateTimeUnit.getYear());
    assertEquals(2, testDateTimeUnit.getMonth());
    assertEquals(1, testDateTimeUnit.getDay());
    testDateTimeUnit = calendar.plusDays(dateTimeUnit, 366);
    assertEquals(1383, testDateTimeUnit.getYear());
    assertEquals(1, testDateTimeUnit.getMonth());
    assertEquals(2, testDateTimeUnit.getDay());
    dateTimeUnit = new DateTimeUnit(1403, 12, 29);
    testDateTimeUnit = calendar.plusDays(dateTimeUnit, 1);
    assertEquals(1403, testDateTimeUnit.getYear());
    assertEquals(12, testDateTimeUnit.getMonth());
    assertEquals(30, testDateTimeUnit.getDay());
    dateTimeUnit = new DateTimeUnit(1403, 12, 30);
    testDateTimeUnit = calendar.plusDays(dateTimeUnit, -1);
    assertEquals(1403, testDateTimeUnit.getYear());
    assertEquals(12, testDateTimeUnit.getMonth());
    assertEquals(29, testDateTimeUnit.getDay());
    dateTimeUnit = new DateTimeUnit(1371, 1, 1);
    testDateTimeUnit = calendar.plusDays(dateTimeUnit, -1);
    assertEquals(1370, testDateTimeUnit.getYear());
    assertEquals(12, testDateTimeUnit.getMonth());
    assertEquals(30, testDateTimeUnit.getDay());
  }

  @Test
  void testPlusWeeks() {
    DateTimeUnit dateTimeUnit = new DateTimeUnit(1382, 1, 20);
    DateTimeUnit testDateTimeUnit = calendar.plusWeeks(dateTimeUnit, 4);
    assertEquals(1382, testDateTimeUnit.getYear());
    assertEquals(2, testDateTimeUnit.getMonth());
    assertEquals(17, testDateTimeUnit.getDay());
  }

  @Test
  void testPlusMonths() {
    DateTimeUnit dateTimeUnit = new DateTimeUnit(1382, 1, 20);
    DateTimeUnit testDateTimeUnit = calendar.plusMonths(dateTimeUnit, 4);
    assertEquals(1382, testDateTimeUnit.getYear());
    assertEquals(5, testDateTimeUnit.getMonth());
    assertEquals(20, testDateTimeUnit.getDay());
    dateTimeUnit = new DateTimeUnit(1382, 1, 20);
    testDateTimeUnit = calendar.plusMonths(dateTimeUnit, -4);
    assertEquals(1381, testDateTimeUnit.getYear());
    assertEquals(9, testDateTimeUnit.getMonth());
    assertEquals(20, testDateTimeUnit.getDay());
  }

  @Test
  void testMinusDays() {
    DateTimeUnit dateTimeUnit = new DateTimeUnit(1371, 1, 1);
    DateTimeUnit testDateTimeUnit = calendar.minusDays(dateTimeUnit, 1);
    assertEquals(1370, testDateTimeUnit.getYear());
    assertEquals(12, testDateTimeUnit.getMonth());
    assertEquals(30, testDateTimeUnit.getDay());
    testDateTimeUnit = calendar.minusDays(dateTimeUnit, 366);
    assertEquals(1370, testDateTimeUnit.getYear());
    assertEquals(1, testDateTimeUnit.getMonth());
    assertEquals(1, testDateTimeUnit.getDay());
    dateTimeUnit = new DateTimeUnit(1371, 7, 1);
    testDateTimeUnit = calendar.minusDays(dateTimeUnit, 1);
    assertEquals(1371, testDateTimeUnit.getYear());
    assertEquals(6, testDateTimeUnit.getMonth());
    assertEquals(31, testDateTimeUnit.getDay());
    dateTimeUnit = new DateTimeUnit(1371, 8, 1);
    testDateTimeUnit = calendar.minusDays(dateTimeUnit, 1);
    assertEquals(1371, testDateTimeUnit.getYear());
    assertEquals(7, testDateTimeUnit.getMonth());
    assertEquals(30, testDateTimeUnit.getDay());
  }

  @Test
  void testMinusWeeks() {
    DateTimeUnit dateTimeUnit = new DateTimeUnit(1382, 1, 10);
    DateTimeUnit testDateTimeUnit = calendar.minusWeeks(dateTimeUnit, 2);
    assertEquals(1381, testDateTimeUnit.getYear());
    assertEquals(12, testDateTimeUnit.getMonth());
    assertEquals(25, testDateTimeUnit.getDay());
  }

  @Test
  void testMinusMonths() {
    DateTimeUnit dateTimeUnit = new DateTimeUnit(1382, 1, 20);
    DateTimeUnit testDateTimeUnit = calendar.minusMonths(dateTimeUnit, 1);
    assertEquals(1381, testDateTimeUnit.getYear());
    assertEquals(12, testDateTimeUnit.getMonth());
    assertEquals(20, testDateTimeUnit.getDay());
  }

  @Test
  void testWeekday() {
    assertEquals(2, calendar.weekday(new DateTimeUnit(1372, 1, 2)));
  }
}
