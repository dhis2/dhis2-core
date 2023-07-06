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
package org.hisp.dhis.dxf2.adx;

import static java.util.Calendar.APRIL;
import static java.util.Calendar.JANUARY;
import static java.util.Calendar.JULY;
import static java.util.Calendar.MAY;
import static java.util.Calendar.MONDAY;
import static java.util.Calendar.NOVEMBER;
import static java.util.Calendar.OCTOBER;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;
import static java.util.Calendar.THURSDAY;
import static java.util.Calendar.WEDNESDAY;

import java.util.Calendar;
import java.util.Date;
import org.hisp.dhis.period.BiMonthlyPeriodType;
import org.hisp.dhis.period.BiWeeklyPeriodType;
import org.hisp.dhis.period.DailyPeriodType;
import org.hisp.dhis.period.FinancialAprilPeriodType;
import org.hisp.dhis.period.FinancialJulyPeriodType;
import org.hisp.dhis.period.FinancialNovemberPeriodType;
import org.hisp.dhis.period.FinancialOctoberPeriodType;
import org.hisp.dhis.period.MonthlyPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.period.SixMonthlyAprilPeriodType;
import org.hisp.dhis.period.SixMonthlyNovemberPeriodType;
import org.hisp.dhis.period.SixMonthlyPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.WeeklySaturdayPeriodType;
import org.hisp.dhis.period.WeeklySundayPeriodType;
import org.hisp.dhis.period.WeeklyThursdayPeriodType;
import org.hisp.dhis.period.WeeklyWednesdayPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;
import org.hisp.dhis.util.DateUtils;

/**
 * ADXPeriod
 *
 * <p>A simple wrapper class for parsing ISO 8601 <date>/<duration> period types
 *
 * @author bobj
 */
public class AdxPeriod {
  public enum Duration {
    P1D, // daily
    P7D, // weekly
    P14D, // bi-weekly
    P1M, // monthly
    P2M, // bi-monthly
    P3M, // quarterly
    P6M, // 6monthly (including 6monthlyApril)
    P1Y // yearly, financialApril, financialJuly, financialOctober
  }

  public static Period parse(String periodString) throws AdxException {
    String[] tokens = periodString.split("/");

    if (tokens.length != 2) {
      throw new AdxException(periodString + " not in valid <date>/<duration> format");
    }

    try {
      Period period;
      PeriodType periodType = null;
      Date startDate = DateUtils.getMediumDate(tokens[0]);
      Calendar cal = Calendar.getInstance();
      cal.setTime(startDate);
      Duration duration = Duration.valueOf(tokens[1]);

      switch (duration) {
        case P1D:
          periodType = new DailyPeriodType();
          break;
        case P7D:
          switch (cal.get(Calendar.DAY_OF_WEEK)) {
            case MONDAY:
              periodType = new WeeklyPeriodType();
              break;
            case WEDNESDAY:
              periodType = new WeeklyWednesdayPeriodType();
              break;
            case THURSDAY:
              periodType = new WeeklyThursdayPeriodType();
              break;
            case SATURDAY:
              periodType = new WeeklySaturdayPeriodType();
              break;
            case SUNDAY:
              periodType = new WeeklySundayPeriodType();
              break;
            default:
              throw new AdxException(periodString + " is invalid weekly type");
          }
          break;
        case P14D:
          periodType = new BiWeeklyPeriodType();
          break;
        case P1M:
          periodType = new MonthlyPeriodType();
          break;
        case P2M:
          periodType = new BiMonthlyPeriodType();
          break;
        case P3M:
          periodType = new QuarterlyPeriodType();
          break;
        case P6M:
          switch (cal.get(Calendar.MONTH)) {
            case JANUARY:
            case JULY:
              periodType = new SixMonthlyPeriodType();
              break;
            case APRIL:
            case OCTOBER:
              periodType = new SixMonthlyAprilPeriodType();
              break;
            case NOVEMBER:
            case MAY:
              periodType = new SixMonthlyNovemberPeriodType();
              break;
            default:
              throw new AdxException(periodString + " is invalid sixmonthly type");
          }
          break;
        case P1Y:
          switch (cal.get(Calendar.MONTH)) {
            case JANUARY:
              periodType = new YearlyPeriodType();
              break;
            case APRIL:
              periodType = new FinancialAprilPeriodType();
              break;
            case JULY:
              periodType = new FinancialJulyPeriodType();
              break;
            case OCTOBER:
              periodType = new FinancialOctoberPeriodType();
              break;
            case NOVEMBER:
              periodType = new FinancialNovemberPeriodType();
              break;
            default:
              throw new AdxException(periodString + " is invalid yearly type");
          }
          break;
      }

      if (periodType != null) {
        period = periodType.createPeriod(startDate);
      } else {
        throw new AdxException("Failed to create period type from " + duration);
      }

      return period;

    } catch (IllegalArgumentException ex) {
      throw new AdxException(tokens[1] + " is not a supported duration type");
    }
  }

  public static String serialize(Period period) {
    return DateUtils.getMediumDateString(period.getStartDate())
        + "/"
        + period.getPeriodType().getIso8601Duration();
  }
}
