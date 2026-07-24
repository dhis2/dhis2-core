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
import static org.hisp.dhis.period.PeriodTypeEnum.FINANCIAL_YEARLY;
import static org.hisp.dhis.period.PeriodTypeEnum.MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.QUARTERLY;
import static org.hisp.dhis.period.PeriodTypeEnum.SIX_MONTHLY;
import static org.hisp.dhis.period.PeriodTypeEnum.WEEKLY;
import static org.hisp.dhis.period.PeriodTypeEnum.YEARLY;

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
  THIS_QUARTER(QUARTERLY),
  LAST_QUARTER(QUARTERLY),
  THIS_SIX_MONTH(SIX_MONTHLY),
  LAST_SIX_MONTH(SIX_MONTHLY),
  WEEKS_THIS_YEAR(WEEKLY),
  MONTHS_THIS_YEAR(MONTHLY),
  BIMONTHS_THIS_YEAR(BI_MONTHLY),
  QUARTERS_THIS_YEAR(QUARTERLY),
  THIS_YEAR(YEARLY),
  MONTHS_LAST_YEAR(MONTHLY),
  QUARTERS_LAST_YEAR(QUARTERLY),
  LAST_YEAR(YEARLY),
  LAST_5_YEARS(YEARLY),
  LAST_10_YEARS(YEARLY),
  LAST_12_MONTHS(MONTHLY),
  LAST_6_MONTHS(MONTHLY),
  LAST_3_MONTHS(MONTHLY),
  LAST_6_BIMONTHS(BI_MONTHLY),
  LAST_4_QUARTERS(QUARTERLY),
  LAST_2_SIXMONTHS(SIX_MONTHLY),
  THIS_FINANCIAL_YEAR(FINANCIAL_YEARLY),
  LAST_FINANCIAL_YEAR(FINANCIAL_YEARLY),
  LAST_5_FINANCIAL_YEARS(FINANCIAL_YEARLY),
  LAST_10_FINANCIAL_YEARS(FINANCIAL_YEARLY),
  THIS_WEEK(WEEKLY),
  LAST_WEEK(WEEKLY),
  THIS_BIWEEK(BI_WEEKLY),
  LAST_BIWEEK(BI_WEEKLY),
  LAST_4_WEEKS(WEEKLY),
  LAST_4_BIWEEKS(BI_WEEKLY),
  LAST_12_WEEKS(WEEKLY),
  LAST_52_WEEKS(WEEKLY);

  PeriodTypeEnum periodType;

  RelativePeriodEnum(PeriodTypeEnum periodType) {
    this.periodType = periodType;
  }

  public PeriodTypeEnum value() {
    return periodType;
  }

  public static boolean contains(String value) {
    return EnumUtils.isValidEnum(RelativePeriodEnum.class, value);
  }
}
