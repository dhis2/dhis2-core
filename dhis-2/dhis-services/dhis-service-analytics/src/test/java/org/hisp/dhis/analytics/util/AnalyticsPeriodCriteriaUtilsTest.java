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
package org.hisp.dhis.analytics.util;

import static java.time.LocalDate.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.ZoneId;
import java.util.Set;
import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.period.PeriodDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AnalyticsPeriodCriteriaUtilsTest {
  @Test
  void testDefineDefaultPeriodForCriteria_without_period() {
    // given
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setDimension(Set.of("dimension"));
    PeriodDataProvider periodDataProvider = new PeriodDataProvider(new JdbcTemplate());

    // when
    AnalyticsPeriodCriteriaUtils.defineDefaultPeriodForCriteria(
        criteria, periodDataProvider, PeriodDataProvider.PeriodSource.SYSTEM_DEFINED);

    // then
    assertEquals(
        1975,
        criteria.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear());
    assertEquals(
        now().plusYears(25).getYear(),
        criteria.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().getYear());
  }

  @Test
  void testDefineDefaultPeriodForCriteria_with_period() {
    // given
    final String period = "pe:LAST_YEAR";
    EnrollmentAnalyticsQueryCriteria criteria = new EnrollmentAnalyticsQueryCriteria();
    criteria.setDimension(Set.of(period));
    PeriodDataProvider periodDataProvider = new PeriodDataProvider(new JdbcTemplate());

    // when
    AnalyticsPeriodCriteriaUtils.defineDefaultPeriodForCriteria(
        criteria, periodDataProvider, PeriodDataProvider.PeriodSource.SYSTEM_DEFINED);

    // then
    assertNull(criteria.getStartDate());
    assertNull(criteria.getEndDate());
    assertEquals(criteria.getDimension(), Set.of(period));
  }
}
