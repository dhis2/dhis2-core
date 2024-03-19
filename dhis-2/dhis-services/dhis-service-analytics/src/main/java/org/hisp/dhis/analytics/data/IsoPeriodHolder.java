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
package org.hisp.dhis.analytics.data;

import static java.util.Optional.empty;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_FREE_RANGE_SEPARATOR;
import static org.hisp.dhis.util.DateUtils.safeParseDate;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.Period;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class IsoPeriodHolder {
  private final String isoPeriod;

  private final String dateField;

  /**
   * If there is a {@link org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP}, it splits the
   * given argument and return a {@link IsoPeriodHolder} instance with the resulting "isoPeriod" and
   * "dateField". Otherwise, it simply returns a {@link IsoPeriodHolder} with the "isoPeriod" set to
   * the same value of the given argument.
   *
   * @param isoPeriodWithDateField in the format "isoPeriod:theDateField" ie: <code>
   *     LAST_YEAR:LAST_UPDATED</code>, <code>LAST_5_YEARS:SCHEDULED_DATE</code>
   * @return a populated instance of IsoPeriodHolder
   */
  static IsoPeriodHolder of(String isoPeriodWithDateField) {
    if (isoPeriodWithDateField.contains(DIMENSION_NAME_SEP)) {
      String[] split = isoPeriodWithDateField.split(DIMENSION_NAME_SEP);
      return new IsoPeriodHolder(split[0], split[1]);
    }
    return new IsoPeriodHolder(isoPeriodWithDateField, null);
  }

  public boolean hasDateField() {
    return Objects.nonNull(dateField);
  }

  /**
   * Parses periods in <code>YYYYMMDD_YYYYMMDD</code> or <code>YYYY-MM-DD_YYYY-MM-DD</code> format
   * into a {@link Period} of type {@link DailyPeriodType} with the respective start and end dates
   * properly set.
   *
   * @return the Period object
   */
  public Optional<Period> toDailyPeriod() {
    String[] dates = getIsoPeriod().split(PERIOD_FREE_RANGE_SEPARATOR);

    if (dates.length == 2) {
      Date start = safeParseDate(dates[0]);
      Date end = safeParseDate(dates[1]);

      if (start != null && end != null) {
        Period period = new Period();
        period.setPeriodType(new DailyPeriodType());
        period.setStartDate(start);
        period.setEndDate(end);
        period.setDateField(getDateField());

        return Optional.of(period);
      }
    }
    return empty();
  }
}
