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

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Calendar-specific tests for {@link Period#of(String)}: exact date ranges, canonical forms, and
 * week-boundary semantics. These assertions were derived by hand from:
 *
 * <ul>
 *   <li>{@link org.hisp.dhis.calendar.DateUnitPeriodTypeParser#parseInternal}
 *   <li>each {@code *PeriodType#getIsoDate(DateTimeUnit, Calendar)}
 * </ul>
 *
 * <p>They will need updating if either side of that contract changes.
 *
 * @implNote AI generated (if corner case assumptions later turn out to be wrong that may be why)
 */
class PeriodCalendarTest {

  // ---------------------------------------------------------------------
  // Exact date ranges, one representative per period type
  // ---------------------------------------------------------------------

  @ParameterizedTest(name = "{0} -> {1} .. {2}")
  @CsvSource({
    // --- pure calendar types --------------------------------------------
    "2023,        2023-01-01, 2023-12-31",
    "202301,      2023-01-01, 2023-01-31",
    "202302,      2023-02-01, 2023-02-28",
    "20230115,    2023-01-15, 2023-01-15",
    "2023Q1,      2023-01-01, 2023-03-31",
    "2023Q2,      2023-04-01, 2023-06-30",
    "2023Q3,      2023-07-01, 2023-09-30",
    "2023Q4,      2023-10-01, 2023-12-31",
    "2023S1,      2023-01-01, 2023-06-30",
    "2023S2,      2023-07-01, 2023-12-31",
    "202301B,     2023-01-01, 2023-02-28",
    "202306B,     2023-11-01, 2023-12-31",

    // --- ISO weeks / bi-weeks -------------------------------------------
    "2023W1,      2023-01-02, 2023-01-08",
    "2023W52,     2023-12-25, 2023-12-31",
    "2020W53,     2020-12-28, 2021-01-03", // 2020 is a 53-week ISO year
    "2023BiW1,    2023-01-02, 2023-01-15",
    "2023BiW26,   2023-12-18, 2023-12-31",

    // --- April six-monthly (starts Apr/Oct) -----------------------------
    "2023AprilS1, 2023-04-01, 2023-09-30",
    "2023AprilS2, 2023-10-01, 2024-03-31",

    // --- November six-monthly (Q1 starts Nov of prior year) -------------
    "2023NovS1,   2022-11-01, 2023-04-30",
    "2023NovS2,   2023-05-01, 2023-10-31",

    // --- November quarterly (Q1 starts Nov of prior year) ---------------
    "2023NovQ1,   2022-11-01, 2023-01-31",
    "2023NovQ2,   2023-02-01, 2023-04-30",
    "2023NovQ3,   2023-05-01, 2023-07-31",
    "2023NovQ4,   2023-08-01, 2023-10-31",

    // --- Financial years (input year labels the fiscal year) ------------
    "2023Feb,     2023-02-01, 2024-01-31",
    "2023April,   2023-04-01, 2024-03-31",
    "2023July,    2023-07-01, 2024-06-30",
    "2023Aug,     2023-08-01, 2024-07-31",
    "2023Sep,     2023-09-01, 2024-08-31",
    "2023Oct,     2023-10-01, 2024-09-30",
    "2023Nov,     2022-11-01, 2023-10-31", // NOTE: fiscal label is END year
  })
  void parsesToExpectedDateRange(String iso, String expectedStart, String expectedEnd) {
    Period p = Period.of(iso);
    assertEquals(expectedStart, p.getStartDateString());
    assertEquals(expectedEnd, p.getEndDateString());
    assertFalse(p.getStartDate().after(p.getEndDate()));
  }

  // ---------------------------------------------------------------------
  // Canonical form is preserved exactly
  // ---------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2023",
        "202301",
        "20230115",
        "2023Q1",
        "2023Q4",
        "2023S1",
        "2023S2",
        "2023NovQ1",
        "2023NovQ4",
        "202301B",
        "202306B",
        "2023W1",
        "2023W52",
        "2023WedW1",
        "2023ThuW1",
        "2023FriW1",
        "2023SatW1",
        "2023SunW1",
        "2023BiW1",
        "2023Feb",
        "2023April",
        "2023July",
        "2023Aug",
        "2023Sep",
        "2023Oct",
        "2023Nov",
        "2023AprilS1",
        "2023AprilS2",
        "2023NovS1",
        "2023NovS2",
      })
  void canonicalFormIsPreservedExactly(String canonical) {
    assertEquals(canonical, Period.of(canonical).getIsoDate());
  }

  // ---------------------------------------------------------------------
  // Non-canonical inputs normalise
  // ---------------------------------------------------------------------

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "2023-01,    202301", // dashed monthly -> compact
    "2023-01-15, 20230115", // dashed daily -> compact
    "2023W01,    2023W1", // zero-padded week -> unpadded
    "2023BiW01,  2023BiW1", // zero-padded bi-week -> unpadded
  })
  void nonCanonicalInputNormalisesToCanonical(String input, String expectedCanonical) {
    assertEquals(expectedCanonical, Period.of(input).getIsoDate());
  }

  // ---------------------------------------------------------------------
  // Week-boundary semantics (this is where DateUnitPeriodTypeParser's
  // overflow check matters: 2023W53 must not silently roll into 2024W1)
  // ---------------------------------------------------------------------

  @Test
  void isoWeek53ExistsIn2020() {
    assertEquals("2020-12-28", Period.of("2020W53").getStartDateString());
    assertEquals("2021-01-03", Period.of("2020W53").getEndDateString());
  }

  @Test
  void isoWeek53DoesNotExistIn2023() {
    // 2023 has 52 ISO weeks; the parser must reject rather than roll to 2024W1
    assertThrows(IllegalArgumentException.class, () -> Period.of("2023W53"));
  }

  // ---------------------------------------------------------------------
  // Non-Monday weekly types start on the expected weekday
  // ---------------------------------------------------------------------

  @ParameterizedTest(name = "{0} starts on {1}")
  @CsvSource({
    "2023W1,     MONDAY",
    "2023WedW1,  WEDNESDAY",
    "2023ThuW1,  THURSDAY",
    "2023FriW1,  FRIDAY",
    "2023SatW1,  SATURDAY",
    "2023SunW1,  SUNDAY",
  })
  void weeklyTypesStartOnExpectedDayOfWeek(String iso, DayOfWeek expected) {
    Period p = Period.of(iso);
    assertEquals(expected, toLocalDate(p.getStartDate()).getDayOfWeek());
    assertEquals(7, p.getDaysInPeriod()); // WEEKLY / WEEKLY_* types are 7 days
  }

  @Test
  void biWeeklyStartsOnMondayAndIsFourteenDays() {
    Period p = Period.of("2023BiW1");
    assertEquals(DayOfWeek.MONDAY, toLocalDate(p.getStartDate()).getDayOfWeek());
    assertEquals(14, p.getDaysInPeriod());
    // 2023-01-02 .. 2023-01-15
    assertEquals("2023-01-02", p.getStartDateString());
    assertEquals("2023-01-15", p.getEndDateString());
  }

  // ---------------------------------------------------------------------
  // Invalid calendar dates must be rejected (not silently stored)
  //
  // The 4-arg DateTimeUnit constructor does no validation, so these depend
  // on Period.of wrapping the eventual DateTimeException as
  // IllegalArgumentException — see note in the accompanying message.
  // ---------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {
        "20230230", // Feb 30 (compact daily)
        "2023-02-30", // Feb 30 (dashed daily)
        "202313", // month 13 (compact monthly)
        "2023-13", // month 13 (dashed monthly)
      })
  void rejectsImpossibleCalendarDates(String bad) {
    assertThrows(IllegalArgumentException.class, () -> Period.of(bad));
  }

  // ---------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------

  private static LocalDate toLocalDate(Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
