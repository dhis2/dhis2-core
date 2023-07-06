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

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;

/**
 * @author Lars Helge Overland
 */
public class BiMonthlyPeriodType extends CalendarPeriodType {
  private static final String ISO_FORMAT = "yyyyMMB";

  private static final String ISO8601_DURATION = "P2M";

  /** The name of the BiMonthlyPeriodType, which is "BiMonthly". */
  public static final String NAME = "BiMonthly";

  public static final int FREQUENCY_ORDER = 61;

  public static final String SQL_INTERVAL = "2 months";

  // -------------------------------------------------------------------------
  // PeriodType functionality
  // -------------------------------------------------------------------------

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Period createPeriod(DateTimeUnit dateTimeUnit, Calendar calendar) {
    DateTimeUnit start = new DateTimeUnit(dateTimeUnit);
    start.setMonth(((start.getMonth() - 1) - ((start.getMonth() - 1) % 2)) + 1);
    start.setDay(1);

    DateTimeUnit end = new DateTimeUnit(start);

    end = calendar.plusMonths(end, 1);
    end.setDay(calendar.daysInMonth(end.getYear(), end.getMonth()));

    return toIsoPeriod(start, end, calendar);
  }

  @Override
  public int getFrequencyOrder() {
    return FREQUENCY_ORDER;
  }

  @Override
  public String getSqlInterval() {
    return SQL_INTERVAL;
  }

  // -------------------------------------------------------------------------
  // CalendarPeriodType functionality
  // -------------------------------------------------------------------------
  @Override
  public DateTimeUnit getDateWithOffset(DateTimeUnit dateTimeUnit, int offset, Calendar calendar) {
    return calendar.plusMonths(dateTimeUnit, 2 * offset);
  }

  /**
   * Generates bimonthly Periods for the whole year in which the start date of the given Period
   * exists.
   */
  @Override
  public List<Period> generatePeriods(DateTimeUnit dateTimeUnit) {
    Calendar cal = getCalendar();

    dateTimeUnit.setMonth(1);
    dateTimeUnit.setDay(1);

    List<Period> periods = Lists.newArrayList();

    int year = dateTimeUnit.getYear();

    while (dateTimeUnit.getYear() == year) {
      periods.add(createPeriod(dateTimeUnit, cal));
      dateTimeUnit = cal.plusMonths(dateTimeUnit, 2);
    }

    return periods;
  }

  /**
   * Generates the last 6 bi-months where the last one is the bi-month which the given date is
   * inside.
   */
  @Override
  public List<Period> generateRollingPeriods(DateTimeUnit dateTimeUnit, Calendar calendar) {
    dateTimeUnit.setDay(1);
    DateTimeUnit iterationDateTimeUnit =
        calendar.minusMonths(dateTimeUnit, (dateTimeUnit.getMonth() % 2) + 10);

    List<Period> periods = Lists.newArrayList();

    for (int i = 0; i < 6; i++) {
      periods.add(createPeriod(iterationDateTimeUnit, calendar));
      iterationDateTimeUnit = calendar.plusMonths(iterationDateTimeUnit, 2);
    }

    return periods;
  }

  @Override
  public String getIsoDate(DateTimeUnit dateTimeUnit, Calendar calendar) {
    return String.format("%d%02dB", dateTimeUnit.getYear(), (dateTimeUnit.getMonth() + 1) / 2);
  }

  @Override
  public String getIsoFormat() {
    return ISO_FORMAT;
  }

  @Override
  public Date getRewindedDate(Date date, Integer rewindedPeriods) {
    Calendar cal = getCalendar();

    date = date != null ? date : new Date();
    rewindedPeriods = rewindedPeriods != null ? rewindedPeriods : 1;

    DateTimeUnit dateTimeUnit = cal.fromIso(DateTimeUnit.fromJdkDate(date));
    dateTimeUnit = cal.minusMonths(dateTimeUnit, rewindedPeriods * 2);

    return cal.toIso(dateTimeUnit).toJdkDate();
  }

  @Override
  public String getIso8601Duration() {
    return ISO8601_DURATION;
  }
}
