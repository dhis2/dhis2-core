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
package org.hisp.dhis.period;

import org.apache.commons.lang3.EnumUtils;

public enum RelativePeriodEnum {
  TODAY,
  YESTERDAY,
  LAST_3_DAYS,
  LAST_7_DAYS,
  LAST_14_DAYS,
  LAST_30_DAYS,
  LAST_60_DAYS,
  LAST_90_DAYS,
  LAST_180_DAYS,
  THIS_MONTH,
  LAST_MONTH,
  THIS_BIMONTH,
  LAST_BIMONTH,
  THIS_QUARTER,
  LAST_QUARTER,
  THIS_SIX_MONTH,
  LAST_SIX_MONTH,
  WEEKS_THIS_YEAR,
  MONTHS_THIS_YEAR,
  BIMONTHS_THIS_YEAR,
  QUARTERS_THIS_YEAR,
  THIS_YEAR,
  MONTHS_LAST_YEAR,
  QUARTERS_LAST_YEAR,
  LAST_YEAR,
  LAST_5_YEARS,
  LAST_10_YEARS,
  LAST_12_MONTHS,
  LAST_6_MONTHS,
  LAST_3_MONTHS,
  LAST_6_BIMONTHS,
  LAST_4_QUARTERS,
  LAST_2_SIXMONTHS,
  THIS_FINANCIAL_YEAR,
  LAST_FINANCIAL_YEAR,
  LAST_5_FINANCIAL_YEARS,
  LAST_10_FINANCIAL_YEARS,
  THIS_WEEK,
  LAST_WEEK,
  THIS_BIWEEK,
  LAST_BIWEEK,
  LAST_4_WEEKS,
  LAST_4_BIWEEKS,
  LAST_12_WEEKS,
  LAST_52_WEEKS;

  public static boolean contains(String value) {
    return EnumUtils.isValidEnum(RelativePeriodEnum.class, value);
  }
}
