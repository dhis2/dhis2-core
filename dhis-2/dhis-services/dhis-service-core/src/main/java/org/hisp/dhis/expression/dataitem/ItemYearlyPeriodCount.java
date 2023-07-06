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

import static java.lang.String.valueOf;
import static org.hisp.dhis.calendar.DateTimeUnit.fromJdkDate;
import static org.hisp.dhis.period.PeriodType.getPeriodFromIsoString;

import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.period.BiWeeklyAbstractPeriodType;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.WeeklyAbstractPeriodType;
import org.hisp.dhis.period.YearlyPeriodType;

/**
 * Expression item [yearlyPeriodCount]
 *
 * @author Jim Grace
 */
public class ItemYearlyPeriodCount extends ItemPeriodBase {
  private static final int WEEKS_PER_YEAR_MINIMUM = 52;

  private static final int BIWEEKS_PER_YEAR_MINIMUM = 26;

  private static final int DECEMBER = 12;

  @Override
  public Double evaluate(PeriodType periodType, Period period) {
    if (periodType instanceof WeeklyAbstractPeriodType) {
      return weeksInYearContaining(period);
    } else if (periodType instanceof BiWeeklyAbstractPeriodType) {
      return biWeeksInYearContaining(period);
    } else if (periodType instanceof CalendarPeriodType
        && periodType.getFrequencyOrder() < YearlyPeriodType.FREQUENCY_ORDER) {
      return countPeriodsWithinThisYear(period);
    }

    return 1.0;
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  /** Finds the number of weeks in this year (52 or 53). */
  private double weeksInYearContaining(Period period) {
    return testLastPeriodInYear(period, WEEKS_PER_YEAR_MINIMUM);
  }

  /** Finds the number of biweeks in this year (26 or 27). */
  private double biWeeksInYearContaining(Period period) {
    return testLastPeriodInYear(period, BIWEEKS_PER_YEAR_MINIMUM);
  }

  /**
   * Sees if the guaranteed last week (or biweek) of the year ends before December 28. If so, there
   * will be at least 4 more days in the year and DHIS2 will generate another week (or biweek) for
   * that year.
   *
   * <p>(This doesn't make complete sense for biweeks, but it's how DHIS2 currently does it.)
   */
  private double testLastPeriodInYear(Period period, int count) {
    String isoDate = period.getIsoDate();
    String testIsoString = isoDate.replaceAll("\\d+$", valueOf(count));
    Period testPeriod = getPeriodFromIsoString(testIsoString);
    DateTimeUnit testEndDate = fromJdkDate(testPeriod.getEndDate());

    if (testEndDate.getMonth() == DECEMBER && testEndDate.getDay() < 28) {
      return 1.0 + count; // An extra week (or biweek) this year
    }

    return count;
  }

  /**
   * Counts the periods that can be generated within a year.
   *
   * <p>{@link PeriodType} must be a {@link CalendarPeriodType}.
   */
  private double countPeriodsWithinThisYear(Period period) {
    return ((CalendarPeriodType) period.getPeriodType()).generatePeriods(period).size();
  }
}
