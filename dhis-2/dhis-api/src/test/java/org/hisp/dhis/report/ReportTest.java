/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.report;

import static org.hisp.dhis.period.RelativePeriodEnum.BIMONTHS_THIS_YEAR;
import static org.hisp.dhis.period.RelativePeriodEnum.LAST_14_DAYS;
import static org.hisp.dhis.period.RelativePeriodEnum.LAST_3_MONTHS;
import static org.hisp.dhis.period.RelativePeriodEnum.LAST_7_DAYS;
import static org.hisp.dhis.period.RelativePeriodEnum.THIS_BIWEEK;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hisp.dhis.period.RelativePeriods;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Report}. */
class ReportTest {
  @Test
  void testGetRelatives() {
    // Given
    List<String> rawPeriods =
        List.of(BIMONTHS_THIS_YEAR.name(), THIS_BIWEEK.name(), LAST_7_DAYS.name());

    Report report = new Report();
    report.setRawPeriods(rawPeriods);

    // When
    RelativePeriods relativePeriods = report.getRelatives();

    // Then
    assertTrue(relativePeriods.isBiMonthsThisYear());
    assertTrue(relativePeriods.isThisBiWeek());
    assertTrue(relativePeriods.isLast7Days());
  }

  @Test
  void testGetRelativesWhenRawPeriodsIsNull() {
    // Given
    Report report = new Report();
    report.setRawPeriods(null);

    // When
    RelativePeriods relativePeriods = report.getRelatives();

    // Then
    assertTrue(relativePeriods.isEmpty());
  }

  @Test
  void testGetRelativesWhenRawPeriodsIsEmpty() {
    // Given
    Report report = new Report();
    report.setRawPeriods(List.of());

    // When
    RelativePeriods relativePeriods = report.getRelatives();

    // Then
    assertTrue(relativePeriods.isEmpty());
  }

  @Test
  void testSetRelatives() {
    // Given
    RelativePeriods relativePeriods = new RelativePeriods();
    relativePeriods.setBiMonthsThisYear(true);
    relativePeriods.setLast14Days(true);
    relativePeriods.setLast3Months(true);

    Report report = new Report();

    // When
    report.setRelatives(relativePeriods);

    // Then
    assertTrue(report.getRawPeriods().contains(BIMONTHS_THIS_YEAR.name()));
    assertTrue(report.getRawPeriods().contains(LAST_14_DAYS.name()));
    assertTrue(report.getRawPeriods().contains(LAST_3_MONTHS.name()));
  }
}
