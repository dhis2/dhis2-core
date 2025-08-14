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

  public static Period parse(String periodString) throws IllegalArgumentException {
    String[] tokens = periodString.split("/");

    if (tokens.length != 2) {
      throw new IllegalArgumentException(periodString + " not in valid <date>/<duration> format");
    }

    try {
      Date startDate = DateUtils.toMediumDate(tokens[0]);
      Calendar cal = Calendar.getInstance();
      cal.setTime(startDate);
      Duration duration = Duration.valueOf(tokens[1]);

      PeriodType periodType =
          switch (duration) {
            case P1D -> new DailyPeriodType();
            case P7D ->
                switch (cal.get(Calendar.DAY_OF_WEEK)) {
                  case MONDAY -> new WeeklyPeriodType();
                  case WEDNESDAY -> new WeeklyWednesdayPeriodType();
                  case THURSDAY -> new WeeklyThursdayPeriodType();
                  case SATURDAY -> new WeeklySaturdayPeriodType();
                  case SUNDAY -> new WeeklySundayPeriodType();
                  default ->
                      throw new IllegalArgumentException(periodString + " is invalid weekly type");
                };
            case P14D -> new BiWeeklyPeriodType();
            case P1M -> new MonthlyPeriodType();
            case P2M -> new BiMonthlyPeriodType();
            case P3M -> new QuarterlyPeriodType();
            case P6M ->
                switch (cal.get(Calendar.MONTH)) {
                  case JANUARY, JULY -> new SixMonthlyPeriodType();
                  case APRIL, OCTOBER -> new SixMonthlyAprilPeriodType();
                  case NOVEMBER, MAY -> new SixMonthlyNovemberPeriodType();
                  default ->
                      throw new IllegalArgumentException(
                          periodString + " is invalid sixmonthly type");
                };
            case P1Y ->
                switch (cal.get(Calendar.MONTH)) {
                  case JANUARY -> new YearlyPeriodType();
                  case APRIL -> new FinancialAprilPeriodType();
                  case JULY -> new FinancialJulyPeriodType();
                  case OCTOBER -> new FinancialOctoberPeriodType();
                  case NOVEMBER -> new FinancialNovemberPeriodType();
                  default ->
                      throw new IllegalArgumentException(periodString + " is invalid yearly type");
                };
          };

      return periodType.createPeriod(startDate);

    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(tokens[1] + " is not a supported duration type");
    }
  }

  public static String serialize(Period period) {
    return DateUtils.toMediumDate(period.getStartDate())
        + "/"
        + period.getPeriodType().getIso8601Duration();
  }
}
