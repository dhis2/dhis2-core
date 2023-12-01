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
package org.hisp.dhis.expression.dataitem;

import static java.lang.Integer.parseInt;
import static java.util.Calendar.DAY_OF_YEAR;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hisp.dhis.period.BiMonthlyPeriodType;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.YearlyPeriodType;

/**
 * Expression item [periodInYear]
 *
 * @author Jim Grace
 */
public class ItemPeriodInYear extends ItemPeriodBase {
  private static final Pattern TRAILING_DIGITS = Pattern.compile("\\d+$");

  @Override
  public Double evaluate(PeriodType periodType, Period period) {
    if (periodType instanceof DailyPeriodType) {
      return dayOfYear(period);
    } else if (periodType instanceof MonthlyPeriodType
        || periodType instanceof BiMonthlyPeriodType) {
      return monthOrBiMonthOfYear(period);
    } else if (periodType.getFrequencyOrder() >= YearlyPeriodType.FREQUENCY_ORDER) {
      return 1.0;
    }

    return trailingDigits(period);
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Finds the day of the year for a daily period type. */
  private double dayOfYear(Period period) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(period.getStartDate());

    return cal.get(DAY_OF_YEAR);
  }

  /**
   * For Monthly yyyyMM and BiMonthly yyyyMMB types, the period number is in the fourth and fifth
   * characters of the period's ISO Date.
   */
  private double monthOrBiMonthOfYear(Period period) {
    return parseInt(period.getIsoDate().substring(4, 6));
  }

  /**
   * For many period types, the period number can be found in the trailing digits of the period's
   * ISO Date, for example Weekly yyyWn, Quarterly yyyyQn, SixMonthly yyyySn, etc.
   */
  private double trailingDigits(Period period) {
    Matcher m = TRAILING_DIGITS.matcher(period.getIsoDate());

    if (m.find()) {
      return parseInt(m.group());
    }

    return 0; // Unexpected
  }
}
