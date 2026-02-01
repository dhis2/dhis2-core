/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics;

import org.hisp.dhis.period.WeeklyAbstractPeriodType;
import org.hisp.dhis.period.WeeklyPeriodType;
import org.hisp.dhis.period.WeeklySaturdayPeriodType;
import org.hisp.dhis.period.WeeklySundayPeriodType;
import org.hisp.dhis.period.WeeklyThursdayPeriodType;
import org.hisp.dhis.period.WeeklyWednesdayPeriodType;

/**
 * Enum representing the start day of the week for analytics relative weekly periods.
 *
 * @author Jan Bernitt
 */
public enum AnalyticsWeekStartKey {
  WEEKLY("WEEKLY", new WeeklyPeriodType()),
  WEEKLY_WEDNESDAY("WEEKLY_WEDNESDAY", new WeeklyWednesdayPeriodType()),
  WEEKLY_THURSDAY("WEEKLY_THURSDAY", new WeeklyThursdayPeriodType()),
  WEEKLY_SATURDAY("WEEKLY_SATURDAY", new WeeklySaturdayPeriodType()),
  WEEKLY_SUNDAY("WEEKLY_SUNDAY", new WeeklySundayPeriodType());

  private final String name;

  private final WeeklyAbstractPeriodType weeklyPeriodType;

  AnalyticsWeekStartKey(String name, WeeklyAbstractPeriodType weeklyPeriodType) {
    this.name = name;
    this.weeklyPeriodType = weeklyPeriodType;
  }

  public String getName() {
    return name;
  }

  public WeeklyAbstractPeriodType getWeeklyPeriodType() {
    return weeklyPeriodType;
  }
}
