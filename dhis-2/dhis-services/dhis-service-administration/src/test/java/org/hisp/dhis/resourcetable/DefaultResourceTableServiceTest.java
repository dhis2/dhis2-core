/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.resourcetable;

import static java.time.temporal.ChronoUnit.YEARS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.Year;
import java.util.List;
import org.hisp.dhis.analytics.AnalyticsExportSettings;
import org.hisp.dhis.period.PeriodDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultResourceTableServiceTest {

  @InjectMocks private DefaultResourceTableService defaultResourceTableService;

  @Mock private PeriodDataProvider periodDataProvider;

  @Mock private AnalyticsExportSettings analyticsExportSettings;

  @Mock private ResourceTableStore resourceTableStore;

  @Test
  void generateDatePeriodTableWhenYearIsOutOfRange() {
    // Given
    List<Integer> yearsToCheck = List.of(2000, 2001, 2002, 2003, 2004);
    int defaultOffset = 22;

    // When
    when(periodDataProvider.getAvailableYears()).thenReturn(yearsToCheck);
    when(analyticsExportSettings.getMaxPeriodYearsOffset()).thenReturn(defaultOffset);
    doNothing().when(resourceTableStore).generateResourceTable(any());

    // Then
    assertDoesNotThrow(() -> defaultResourceTableService.generateDatePeriodTable());
  }

  @Test
  void generateDatePeriodTableWhenOffsetIsZeroWithPreviousYears() {
    // Given
    List<Integer> yearsToCheck = List.of(2000, 2001, 2002, 2003, 2004);
    int zeroOffset = 0;

    // When
    when(periodDataProvider.getAvailableYears()).thenReturn(yearsToCheck);
    when(analyticsExportSettings.getMaxPeriodYearsOffset()).thenReturn(zeroOffset);
    doNothing().when(resourceTableStore).generateResourceTable(any());

    // Then
    assertDoesNotThrow(() -> defaultResourceTableService.generateDatePeriodTable());
  }

  @Test
  void generateDatePeriodTableWhenOffsetIsZeroWithCurrentYear() {
    // Given
    List<Integer> yearsToCheck = List.of(Year.now().getValue());
    int zeroOffset = 0;

    // When
    when(periodDataProvider.getAvailableYears()).thenReturn(yearsToCheck);
    when(analyticsExportSettings.getMaxPeriodYearsOffset()).thenReturn(zeroOffset);
    doNothing().when(resourceTableStore).generateResourceTable(any());

    // Then
    assertDoesNotThrow(() -> defaultResourceTableService.generateDatePeriodTable());
  }

  @Test
  void generateDatePeriodTableWhenYearsAreInExpectedRange() {
    // Given
    List<Integer> yearsToCheck =
        List.of(
            Year.now().getValue(),
            Year.now().plus(1, YEARS).getValue(),
            Year.now().plus(2, YEARS).getValue());
    int defaultOffset = 2;

    // When
    when(periodDataProvider.getAvailableYears()).thenReturn(yearsToCheck);
    when(analyticsExportSettings.getMaxPeriodYearsOffset()).thenReturn(defaultOffset);
    doNothing().when(resourceTableStore).generateResourceTable(any());

    // Then
    assertDoesNotThrow(() -> defaultResourceTableService.generateDatePeriodTable());
  }
}
