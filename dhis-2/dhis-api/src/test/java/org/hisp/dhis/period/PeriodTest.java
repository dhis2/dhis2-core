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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * API-contract tests for {@link Period#of(String)} / {@link Period#ofNullable(String)}.
 *
 * <p>Deliberately free of any knowledge of {@link org.hisp.dhis.calendar.DateTimeUnit}, {@link
 * org.hisp.dhis.calendar.impl.Iso8601Calendar}, or the concrete {@code *PeriodType}
 * implementations. Anything that depends on those lives in {@link PeriodCalendarTest}.
 *
 * @implNote AI generated (if corner case assumptions later turn out to be wrong that may be why)
 */
class PeriodTest {

  // ---------------------------------------------------------------------
  // Type resolution (structural only)
  // ---------------------------------------------------------------------

  @Test
  void resolvesYearlyType() {
    assertType(Period.of("2023"), PeriodTypeEnum.YEARLY);
  }

  @Test
  void resolvesMonthlyType() {
    assertType(Period.of("202301"), PeriodTypeEnum.MONTHLY);
    assertType(Period.of("2023-01"), PeriodTypeEnum.MONTHLY);
  }

  @Test
  void resolvesDailyType() {
    assertType(Period.of("20230115"), PeriodTypeEnum.DAILY);
    assertType(Period.of("2023-01-15"), PeriodTypeEnum.DAILY);
  }

  @Test
  void resolvesQuarterlyType() {
    assertType(Period.of("2023Q1"), PeriodTypeEnum.QUARTERLY);
  }

  @Test
  void resolvesQuarterlyNovemberType() {
    assertType(Period.of("2023NovQ1"), PeriodTypeEnum.QUARTERLY_NOV);
  }

  @Test
  void resolvesSixMonthlyTypes() {
    assertType(Period.of("2023S1"), PeriodTypeEnum.SIX_MONTHLY);
    assertType(Period.of("2023AprilS1"), PeriodTypeEnum.SIX_MONTHLY_APRIL);
    assertType(Period.of("2023NovS1"), PeriodTypeEnum.SIX_MONTHLY_NOV);
  }

  @Test
  void resolvesBiMonthlyType() {
    assertType(Period.of("202301B"), PeriodTypeEnum.BI_MONTHLY);
  }

  @Test
  void resolvesBiWeeklyType() {
    assertType(Period.of("2023BiW1"), PeriodTypeEnum.BI_WEEKLY);
    assertType(Period.of("2023BiW01"), PeriodTypeEnum.BI_WEEKLY);
  }

  @Test
  void resolvesAllWeeklyTypes() {
    assertType(Period.of("2023W1"), PeriodTypeEnum.WEEKLY);
    assertType(Period.of("2023WedW1"), PeriodTypeEnum.WEEKLY_WEDNESDAY);
    assertType(Period.of("2023ThuW1"), PeriodTypeEnum.WEEKLY_THURSDAY);
    assertType(Period.of("2023FriW1"), PeriodTypeEnum.WEEKLY_FRIDAY);
    assertType(Period.of("2023SatW1"), PeriodTypeEnum.WEEKLY_SATURDAY);
    assertType(Period.of("2023SunW1"), PeriodTypeEnum.WEEKLY_SUNDAY);
  }

  @Test
  void resolvesAllFinancialTypes() {
    assertType(Period.of("2023Feb"), PeriodTypeEnum.FINANCIAL_FEB);
    assertType(Period.of("2023April"), PeriodTypeEnum.FINANCIAL_APRIL);
    assertType(Period.of("2023July"), PeriodTypeEnum.FINANCIAL_JULY);
    assertType(Period.of("2023Aug"), PeriodTypeEnum.FINANCIAL_AUG);
    assertType(Period.of("2023Sep"), PeriodTypeEnum.FINANCIAL_SEP);
    assertType(Period.of("2023Oct"), PeriodTypeEnum.FINANCIAL_OCT);
    assertType(Period.of("2023Nov"), PeriodTypeEnum.FINANCIAL_NOV);
  }

  // ---------------------------------------------------------------------
  // Structure-level rejection (throws from Period.Input.of)
  // ---------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {
        " ",
        "  ",
        "202",
        "20230",
        "2023Q",
        "202301151234", // wrong lengths
        "XXXX",
        "202X",
        "XXXX-01-15", // non-digit year
        "2023X1",
        "2023X01",
        "2023-1",
        "2023-Q1", // invalid infix
        "2023W1X",
        "2023-W01",
        "2023AprilSX", // malformed tail
        "2023WedWX1",
        "2023Apr", // unsupported abbreviation
      })
  void rejectsMalformedInput(String bad) {
    assertThrows(IllegalArgumentException.class, () -> Period.of(bad));
  }

  @Test
  void rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> Period.of(null));
  }

  // ---------------------------------------------------------------------
  // Semantic rejection (parser returns null or throws, wrapped by of())
  // ---------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2023Q0",
        "2023Q5",
        "2023Q9",
        "2023S0",
        "2023S3",
        "2023S9",
        "2023AprilS0",
        "2023AprilS3",
        "2023NovS0",
        "2023NovS3",
        "2023NovQ0",
        "2023NovQ5",
        "202300B",
        "202307B",
        "202309B",
        "2023W0",
        "2023W00",
        "2023BiW0",
        // Week overflow — see PeriodCalendarTest for the boundary specifics
        "2023W53",
        "2023W54",
        "2023W99",
      })
  void rejectsSemanticallyInvalidInput(String bad) {
    assertThrows(IllegalArgumentException.class, () -> Period.of(bad));
  }

  // ---------------------------------------------------------------------
  // Roundtrip stability (no assumption about the canonical form)
  // ---------------------------------------------------------------------

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2023",
        "202301",
        "2023-01",
        "20230115",
        "2023-01-15",
        "2023Q1",
        "2023Q4",
        "2023S1",
        "2023S2",
        "2023NovQ1",
        "202301B",
        "202306B",
        "2023W1",
        "2023W01",
        "2023W52",
        "2023WedW1",
        "2023ThuW1",
        "2023FriW1",
        "2023SatW1",
        "2023SunW1",
        "2023BiW1",
        "2023BiW01",
        "2023Feb",
        "2023April",
        "2023July",
        "2023Aug",
        "2023Sep",
        "2023Oct",
        "2023Nov",
        "2023AprilS1",
        "2023NovS1",
      })
  void roundtripIsStable(String iso) {
    String canonical = Period.of(iso).getIsoDate();
    assertNotNull(canonical);
    assertFalse(canonical.isEmpty());
    assertEquals(canonical, Period.of(canonical).getIsoDate());
  }

  // ---------------------------------------------------------------------
  // Error messages mention the offending input (helps debugging)
  // ---------------------------------------------------------------------

  @Test
  void errorMessageIncludesInput() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> Period.of("2023Q5"));
    assertNotNull(ex.getMessage());
    assertTrue(ex.getMessage().contains("2023Q5"), "was: " + ex.getMessage());
  }

  // ---------------------------------------------------------------------
  // ofNullable
  // ---------------------------------------------------------------------

  @ParameterizedTest
  @NullAndEmptySource
  void ofNullableReturnsNullForNullOrEmpty(String input) {
    assertNull(Period.ofNullable(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2023Q5", "2023S0", "2023W0", "XXXX", "2023X01", "202301151234"})
  void ofNullableReturnsNullForInvalid(String input) {
    assertNull(Period.ofNullable(input));
  }

  @ParameterizedTest
  @ValueSource(strings = {"2023", "2023Q1", "202301", "2023W01", "20230115"})
  void ofNullableReturnsPeriodForValid(String input) {
    assertNotNull(Period.ofNullable(input));
  }

  // ---------------------------------------------------------------------
  // Helper
  // ---------------------------------------------------------------------

  private static void assertType(Period period, PeriodTypeEnum expected) {
    assertNotNull(period.getPeriodType());
    assertEquals(expected, period.getPeriodType().getPeriodTypeEnum());
    assertNotNull(period.getStartDate());
    assertNotNull(period.getEndDate());
    assertFalse(period.getStartDate().after(period.getEndDate()));
  }
}
