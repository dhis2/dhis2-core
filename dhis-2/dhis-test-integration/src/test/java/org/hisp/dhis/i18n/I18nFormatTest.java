/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.hisp.dhis.period.PeriodType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class I18nFormatTest {

  @ParameterizedTest
  @MethodSource("providePeriodData")
  void testFormatPeriod(String period, String expected) {
    I18nFormat i18nFormat = new I18nFormat();
    assertEquals(expected, i18nFormat.formatPeriod(PeriodType.getPeriodFromIsoString(period)));
  }

  private static Stream<Arguments> providePeriodData() {
    return Stream.of(
        Arguments.of("2024W1", "Week 1 2024-01-01 - 2024-01-07"),
        Arguments.of("2024W2", "Week 2 2024-01-08 - 2024-01-14"),
        Arguments.of("2024W3", "Week 3 2024-01-15 - 2024-01-21"),
        Arguments.of("2021W1", "Week 1 2021-01-04 - 2021-01-10"),
        Arguments.of("2021W2", "Week 2 2021-01-11 - 2021-01-17"),
        Arguments.of("2024SunW1", "Week 1 2023-12-31 - 2024-01-06"),
        Arguments.of("2024ThuW1", "Week 1 2024-01-04 - 2024-01-10"),
        Arguments.of("2024SatW1", "Week 1 2023-12-30 - 2024-01-05"),
        Arguments.of("2024WedW2", "Week 2 2024-01-10 - 2024-01-16"),
        Arguments.of("2024ThuW2", "Week 2 2024-01-11 - 2024-01-17"),
        Arguments.of("2024SatW2", "Week 2 2024-01-06 - 2024-01-12"));
  }
}
