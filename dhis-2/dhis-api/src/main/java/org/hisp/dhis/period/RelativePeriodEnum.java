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
package org.hisp.dhis.period;

import static org.hisp.dhis.period.PeriodTypeEnum.BI_MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.BI_WEEKLY;
import static org.hisp.dhis.period.PeriodTypeEnum.DAILY;
import static org.hisp.dhis.period.PeriodTypeEnum.MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.YEARLY;

import java.util.EnumSet;
import java.util.Set;
import org.apache.commons.lang3.EnumUtils;

public enum RelativePeriodEnum {
  // The order is the display order: relative periods are listed in this order
  // within their period type (e.g. in /api/periodTypes)
  TODAY(DAILY),
  YESTERDAY(DAILY),
  LAST_3_DAYS(DAILY),
  LAST_7_DAYS(DAILY),
  LAST_14_DAYS(DAILY),
  LAST_30_DAYS(DAILY),
  LAST_60_DAYS(DAILY),
  LAST_90_DAYS(DAILY),
  LAST_180_DAYS(DAILY),
  THIS_WEEK(PeriodTypeEnum.WEEKLY_TYPES),
  LAST_WEEK(PeriodTypeEnum.WEEKLY_TYPES),
  LAST_4_WEEKS(PeriodTypeEnum.WEEKLY_TYPES),
  LAST_12_WEEKS(PeriodTypeEnum.WEEKLY_TYPES),
  LAST_52_WEEKS(PeriodTypeEnum.WEEKLY_TYPES),
  WEEKS_THIS_YEAR(PeriodTypeEnum.WEEKLY_TYPES),
  THIS_BIWEEK(BI_WEEKLY),
  LAST_BIWEEK(BI_WEEKLY),
  LAST_4_BIWEEKS(BI_WEEKLY),
  THIS_MONTH(MONTHLY),
  LAST_MONTH(MONTHLY),
  LAST_3_MONTHS(MONTHLY),
  LAST_6_MONTHS(MONTHLY),
  LAST_12_MONTHS(MONTHLY),
  MONTHS_THIS_YEAR(MONTHLY),
  MONTHS_LAST_YEAR(MONTHLY),
  THIS_BIMONTH(BI_MONTHLY),
  LAST_BIMONTH(BI_MONTHLY),
  LAST_6_BIMONTHS(BI_MONTHLY),
  BIMONTHS_THIS_YEAR(BI_MONTHLY),
  THIS_QUARTER(PeriodTypeEnum.QUARTERLY_TYPES),
  LAST_QUARTER(PeriodTypeEnum.QUARTERLY_TYPES),
  LAST_4_QUARTERS(PeriodTypeEnum.QUARTERLY_TYPES),
  QUARTERS_THIS_YEAR(PeriodTypeEnum.QUARTERLY_TYPES),
  QUARTERS_LAST_YEAR(PeriodTypeEnum.QUARTERLY_TYPES),
  THIS_SIX_MONTH(PeriodTypeEnum.SIX_MONTHLY_TYPES),
  LAST_SIX_MONTH(PeriodTypeEnum.SIX_MONTHLY_TYPES),
  LAST_2_SIXMONTHS(PeriodTypeEnum.SIX_MONTHLY_TYPES),
  THIS_FINANCIAL_YEAR(PeriodTypeEnum.FINANCIAL_YEARLY_TYPES),
  LAST_FINANCIAL_YEAR(PeriodTypeEnum.FINANCIAL_YEARLY_TYPES),
  LAST_5_FINANCIAL_YEARS(PeriodTypeEnum.FINANCIAL_YEARLY_TYPES),
  LAST_10_FINANCIAL_YEARS(PeriodTypeEnum.FINANCIAL_YEARLY_TYPES),
  THIS_YEAR(YEARLY),
  LAST_YEAR(YEARLY),
  LAST_5_YEARS(YEARLY),
  LAST_10_YEARS(YEARLY);

  private final EnumSet<PeriodTypeEnum> appliesTo;

  RelativePeriodEnum(PeriodTypeEnum type) {
    this(Set.of(type));
  }

  RelativePeriodEnum(Set<PeriodTypeEnum> appliesTo) {
    this.appliesTo = EnumSet.copyOf(appliesTo);
  }

  public boolean appliesTo(PeriodTypeEnum type) {
    return appliesTo.contains(type);
  }

  public static boolean contains(String value) {
    return EnumUtils.isValidEnum(RelativePeriodEnum.class, value);
  }
}
