/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.period;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.CalendarService;
import org.hisp.dhis.calendar.DateFormat;
import org.hisp.dhis.calendar.DateUnitPeriodTypeParser;
import org.hisp.dhis.calendar.impl.EthiopianCalendar;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link Period#of(String)} with a non-ISO system calendar.
 *
 * <p>ISO period strings are interpreted in the system calendar, so the parsed interval holds local
 * (here: Ethiopian) year/month/day fields. These must be converted to ISO before becoming the
 * {@link Period}'s start and end {@link java.util.Date}s, while the ISO period string stays in
 * local terms.
 *
 * <p>The system calendar is installed on both {@link DateUnitPeriodTypeParser} and {@link
 * PeriodType}, as {@code DefaultCalendarService} does at startup.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class PeriodNonIsoCalendarTest {

  private static final CalendarService ETHIOPIAN =
      new CalendarService() {
        @Override
        public List<Calendar> getAllCalendars() {
          return List.of(EthiopianCalendar.getInstance());
        }

        @Override
        public List<DateFormat> getAllDateFormats() {
          return List.of();
        }

        @Override
        public Calendar getSystemCalendar() {
          return EthiopianCalendar.getInstance();
        }

        @Override
        public DateFormat getSystemDateFormat() {
          return null;
        }
      };

  @BeforeEach
  void setUp() {
    DateUnitPeriodTypeParser.setCalendarService(ETHIOPIAN);
    PeriodType.setCalendarService(ETHIOPIAN);
  }

  @AfterEach
  void tearDown() {
    DateUnitPeriodTypeParser.setCalendarService(null);
    PeriodType.setCalendarService(null);
  }

  @ParameterizedTest(name = "{0} -> {1} .. {2} ({3})")
  @CsvSource({
    // Ethiopian year 2016 began on 2023-09-12 (Gregorian)
    "2016,       2023-09-12, 2024-09-05, 2016",
    "201601,     2023-09-12, 2023-10-11, 201601",
    "2016Q1,     2023-09-12, 2023-12-10, 2016Q1",
    "20160130,   2023-10-11, 2023-10-11, 20160130",
    // every Ethiopian month has 30 days, so the 30th of month 2 exists
    "20160230,   2023-11-10, 2023-11-10, 20160230",
    // non-canonical input normalises to canonical ISO
    "2016-01,    2023-09-12, 2023-10-11, 201601",
    "2016-01-30, 2023-10-11, 2023-10-11, 20160130",
  })
  void periodDatesAreConvertedFromSystemCalendarToIso(
      String input, String expectedStart, String expectedEnd, String expectedIso) {
    Period period = Period.of(input);

    assertEquals(expectedStart, period.getStartDateString());
    assertEquals(expectedEnd, period.getEndDateString());
    assertEquals(expectedIso, period.getIsoDate());
  }
}
