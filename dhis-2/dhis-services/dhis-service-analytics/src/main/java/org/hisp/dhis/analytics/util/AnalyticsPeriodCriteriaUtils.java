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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hisp.dhis.common.EnrollmentAnalyticsQueryCriteria;
import org.hisp.dhis.common.EventsAnalyticsQueryCriteria;
import org.hisp.dhis.period.PeriodDataProvider;
import org.hisp.dhis.util.PeriodCriteriaUtils;

/**
 * Helper class that provides supportive methods to deal with analytics query criteria and periods.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnalyticsPeriodCriteriaUtils {

  /**
   * Defines a default period for the given criteria, if none is present.
   *
   * @param criteria {@link EventsAnalyticsQueryCriteria} query criteria.
   * @param periodDataProvider {@link EventsAnalyticsQueryCriteria} period data provider.
   * @param dataSource {@link PeriodDataProvider.PeriodSource} source of data.
   */
  public static void defineDefaultPeriodForCriteria(
      EnrollmentAnalyticsQueryCriteria criteria,
      PeriodDataProvider periodDataProvider,
      PeriodDataProvider.PeriodSource dataSource) {
    List<Integer> availableYears = periodDataProvider.getAvailableYears(dataSource);

    if (PeriodCriteriaUtils.hasPeriod(criteria)) {
      return;
    }

    Date startDate =
        Date.from(
            LocalDate.of(Collections.min(availableYears), 1, 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());

    Date endDate =
        Date.from(
            LocalDate.of(Collections.max(availableYears), 12, 31)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant());

    criteria.setStartDate(startDate);
    criteria.setEndDate(endDate);
  }
}
