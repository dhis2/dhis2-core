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
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_APRIL;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_AUG;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_FEB;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_JULY;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_NOV;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_OCT;
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_SEP;
import static org.hisp.dhis.period.PeriodTypeEnum.MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.QUARTERLY;
import static org.hisp.dhis.period.PeriodTypeEnum.QUARTERLY_NOV;
import static org.hisp.dhis.period.PeriodTypeEnum.SIX_MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.SIX_MONTHLY_APRIL;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_FRIDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_SATURDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_SUNDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_THURSDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY_WEDNESDAY;
import static org.hisp.dhis.period.PeriodTypeEnum.YEARLY;

import java.util.EnumSet;
import org.apache.commons.lang3.EnumUtils;

public enum RelativePeriodEnum {
  TODAY(DAILY),
  YESTERDAY(DAILY),
  LAST_3_DAYS(DAILY),
  LAST_7_DAYS(DAILY),
  LAST_14_DAYS(DAILY),
  LAST_30_DAYS(DAILY),
  LAST_60_DAYS(DAILY),
  LAST_90_DAYS(DAILY),
  LAST_180_DAYS(DAILY),
  THIS_MONTH(MONTHLY),
  LAST_MONTH(MONTHLY),
  THIS_BIMONTH(BI_MONTHLY),
  LAST_BIMONTH(BI_MONTHLY),
  THIS_QUARTER(QUARTERLY, QUARTERLY_NOV),
  LAST_QUARTER(QUARTERLY, QUARTERLY_NOV),
  THIS_SIX_MONTH(SIX_MONTHLY, SIX_MONTHLY_APRIL, SIX_MONTHLY_APRIL),
  LAST_SIX_MONTH(SIX_MONTHLY, SIX_MONTHLY_APRIL, SIX_MONTHLY_APRIL),
  WEEKS_THIS_YEAR(
      WEEKLY, WEEKLY_WEDNESDAY, WEEKLY_THURSDAY, WEEKLY_FRIDAY, WEEKLY_SATURDAY, WEEKLY_SUNDAY),
  MONTHS_THIS_YEAR(MONTHLY),
  BIMONTHS_THIS_YEAR(BI_MONTHLY),
  QUARTERS_THIS_YEAR(QUARTERLY, QUARTERLY_NOV),
  THIS_YEAR(YEARLY),
  MONTHS_LAST_YEAR(MONTHLY),
  QUARTERS_LAST_YEAR(QUARTERLY, QUARTERLY_NOV),
  LAST_YEAR(YEARLY),
  LAST_5_YEARS(YEARLY),
  LAST_10_YEARS(YEARLY),
  LAST_12_MONTHS(MONTHLY),
  LAST_6_MONTHS(MONTHLY),
  LAST_3_MONTHS(MONTHLY),
  LAST_6_BIMONTHS(BI_MONTHLY),
  LAST_4_QUARTERS(QUARTERLY, QUARTERLY_NOV),
  LAST_2_SIXMONTHS(SIX_MONTHLY, SIX_MONTHLY_APRIL, SIX_MONTHLY_APRIL),
  THIS_FINANCIAL_YEAR(
      FINANCIAL_FEB,
      FINANCIAL_APRIL,
      FINANCIAL_JULY,
      FINANCIAL_AUG,
      FINANCIAL_SEP,
      FINANCIAL_OCT,
      FINANCIAL_NOV),
  LAST_FINANCIAL_YEAR(
      FINANCIAL_FEB,
      FINANCIAL_APRIL,
      FINANCIAL_JULY,
      FINANCIAL_AUG,
      FINANCIAL_SEP,
      FINANCIAL_OCT,
      FINANCIAL_NOV),
  LAST_5_FINANCIAL_YEARS(
      FINANCIAL_FEB,
      FINANCIAL_APRIL,
      FINANCIAL_JULY,
      FINANCIAL_AUG,
      FINANCIAL_SEP,
      FINANCIAL_OCT,
      FINANCIAL_NOV),
  LAST_10_FINANCIAL_YEARS(
      FINANCIAL_FEB,
      FINANCIAL_APRIL,
      FINANCIAL_JULY,
      FINANCIAL_AUG,
      FINANCIAL_SEP,
      FINANCIAL_OCT,
      FINANCIAL_NOV),
  THIS_WEEK(
      WEEKLY, WEEKLY_WEDNESDAY, WEEKLY_THURSDAY, WEEKLY_FRIDAY, WEEKLY_SATURDAY, WEEKLY_SUNDAY),
  LAST_WEEK(
      WEEKLY, WEEKLY_WEDNESDAY, WEEKLY_THURSDAY, WEEKLY_FRIDAY, WEEKLY_SATURDAY, WEEKLY_SUNDAY),
  THIS_BIWEEK(BI_WEEKLY),
  LAST_BIWEEK(BI_WEEKLY),
  LAST_4_WEEKS(
      WEEKLY, WEEKLY_WEDNESDAY, WEEKLY_THURSDAY, WEEKLY_FRIDAY, WEEKLY_SATURDAY, WEEKLY_SUNDAY),
  LAST_4_BIWEEKS(BI_WEEKLY),
  LAST_12_WEEKS(
      WEEKLY, WEEKLY_WEDNESDAY, WEEKLY_THURSDAY, WEEKLY_FRIDAY, WEEKLY_SATURDAY, WEEKLY_SUNDAY),
  LAST_52_WEEKS(
      WEEKLY, WEEKLY_WEDNESDAY, WEEKLY_THURSDAY, WEEKLY_FRIDAY, WEEKLY_SATURDAY, WEEKLY_SUNDAY);

  private final EnumSet<PeriodTypeEnum> appliesTo;

  RelativePeriodEnum(PeriodTypeEnum... types) {
    this.appliesTo = EnumSet.of(types[0], types);
  }

  public boolean appliesTo(PeriodTypeEnum type) {
    return appliesTo.contains(type);
  }

  public static boolean contains(String value) {
    return EnumUtils.isValidEnum(RelativePeriodEnum.class, value);
  }
}
