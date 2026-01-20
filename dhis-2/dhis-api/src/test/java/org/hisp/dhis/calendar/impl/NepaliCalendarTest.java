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
package org.hisp.dhis.calendar.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.period.Cal;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
class NepaliCalendarTest {

  private Calendar calendar;

  @BeforeEach
  void init() {
    calendar = NepaliCalendar.getInstance();
  }

  @Test
  void testToIso() {
    Assertions.assertEquals(
        new DateTimeUnit(2014, 4, 14, true), calendar.toIso(new DateTimeUnit(2071, 1, 1)));
    Assertions.assertEquals(new DateTimeUnit(2014, 4, 14, true), calendar.toIso(2071, 1, 1));
  }

  @Test
  void testFromIso() {
    Assertions.assertEquals(
        new DateTimeUnit(2071, 1, 1, false), calendar.fromIso(new DateTimeUnit(2014, 4, 14, true)));
    Assertions.assertEquals(new DateTimeUnit(2071, 1, 1, false), calendar.fromIso(2014, 4, 14));
  }

  @Test
  void testPlusDays() {
    DateTimeUnit dateTimeUnit = new DateTimeUnit(2014, 12, 30);
    DateTimeUnit testDateTimeUnit = calendar.plusDays(dateTimeUnit, -1);
    assertEquals(2014, testDateTimeUnit.getYear());
    assertEquals(12, testDateTimeUnit.getMonth());
    assertEquals(29, testDateTimeUnit.getDay());
    dateTimeUnit = new DateTimeUnit(2014, 1, 1);
    testDateTimeUnit = calendar.plusDays(dateTimeUnit, -1);
    assertEquals(2013, testDateTimeUnit.getYear());
    assertEquals(12, testDateTimeUnit.getMonth());
    assertEquals(30, testDateTimeUnit.getDay());
  }

  @Test
  void testGenerateWeeklyPeriods() {
    Date startDate = new Cal(2021, 4, 12, true).time();
    Date endDate = new Cal(2022, 4, 10, true).time();

    List<Period> weeks = new WeeklyPeriodType().generatePeriods(calendar, startDate, endDate);
    assertEquals(52, weeks.size());
  }

  @Test
  void testIsoDates() {
    DateTimeUnit dateTimeUnit = new DateTimeUnit(2081, 1, 3, false);
    DateTime startDate = new DateTime(2024, 4, 15, 0, 0);
    DateTime endDate = new DateTime(2024, 4, 21, 0, 0);

    WeeklyPeriodType periodType = new WeeklyPeriodType();
    Period period = periodType.createPeriod(dateTimeUnit, calendar);

    assertEquals("2081W1", period.getIsoDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());

    dateTimeUnit = new DateTimeUnit(2080, 12, 26, false);
    startDate = new DateTime(2024, 4, 8, 0, 0);
    endDate = new DateTime(2024, 4, 14, 0, 0);

    period = periodType.createPeriod(dateTimeUnit, calendar);
    assertEquals("2080W52", period.getIsoDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());

    dateTimeUnit = new DateTimeUnit(2080, 1, 11, false);
    period = periodType.createPeriod(dateTimeUnit, calendar);
    startDate = new DateTime(2023, 4, 24, 0, 0);
    endDate = new DateTime(2023, 4, 30, 0, 0);

    assertEquals("2080W2", period.getIsoDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());

    dateTimeUnit = new DateTimeUnit(2081, 1, 10, false);
    period = periodType.createPeriod(dateTimeUnit, calendar);
    startDate = new DateTime(2024, 4, 22, 0, 0);
    endDate = new DateTime(2024, 4, 28, 0, 0);

    assertEquals("2081W2", period.getIsoDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());

    dateTimeUnit = new DateTimeUnit(2082, 1, 1, false);
    period = periodType.createPeriod(dateTimeUnit, calendar);
    startDate = new DateTime(2025, 4, 14, 0, 0);
    endDate = new DateTime(2025, 4, 20, 0, 0);

    assertEquals("2082W1", period.getIsoDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());

    dateTimeUnit = new DateTimeUnit(2025, 1, 4, true);
    period = periodType.createPeriod(dateTimeUnit, calendar);
    startDate = new DateTime(2024, 12, 30, 0, 0);
    endDate = new DateTime(2025, 1, 5, 0, 0);

    assertEquals("2025W1", period.getIsoDate());
    assertEquals(startDate.toDate(), period.getStartDate());
    assertEquals(endDate.toDate(), period.getEndDate());
  }
}
