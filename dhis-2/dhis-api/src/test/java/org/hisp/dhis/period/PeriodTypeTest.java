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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.hisp.dhis.calendar.Calendar;
import org.junit.jupiter.api.Test;

/**
 * @author Lars Helge Overland
 */
class PeriodTypeTest {

  @Test
  void testGetByIndex() {
    assertNull(PeriodType.getByIndex(-1));
    PeriodType yearly = PeriodType.getByNameIgnoreCase("Yearly");
    assertNotNull(yearly);
    int yearlyIndex = PeriodType.getAvailablePeriodTypes().indexOf(yearly) + 1;
    assertEquals(new YearlyPeriodType(), PeriodType.getByIndex(yearlyIndex));
    assertNull(PeriodType.getByIndex(999));
  }

  @Test
  void testGetPeriodTypeFromIsoString() {
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011").getName(), "Yearly");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("201101").getName(), "Monthly");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011W1").getName(), "Weekly");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011W32").getName(), "Weekly");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("20110101").getName(), "Daily");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011Q3").getName(), "Quarterly");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("201101B").getName(), "BiMonthly");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011S1").getName(), "SixMonthly");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011AprilS1").getName(), "SixMonthlyApril");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011April").getName(), "FinancialApril");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011July").getName(), "FinancialJuly");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011Oct").getName(), "FinancialOct");
    assertNull(PeriodType.getPeriodTypeFromIsoString("201"));
    assertNull(PeriodType.getPeriodTypeFromIsoString("20111"));
    assertNull(PeriodType.getPeriodTypeFromIsoString("201W2"));
    assertNull(PeriodType.getPeriodTypeFromIsoString("2011Q12"));
    assertNull(PeriodType.getPeriodTypeFromIsoString("2011W234"));
    assertNull(PeriodType.getPeriodTypeFromIsoString("201er2345566"));
    assertNull(PeriodType.getPeriodTypeFromIsoString("2011Q10"));
  }

  @Test
  void testGetIsoDurationFromIsoString() {
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011").getIso8601Duration(), "P1Y");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("201101").getIso8601Duration(), "P1M");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011W1").getIso8601Duration(), "P7D");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("20110101").getIso8601Duration(), "P1D");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011Q3").getIso8601Duration(), "P3M");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("201101B").getIso8601Duration(), "P2M");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011S1").getIso8601Duration(), "P6M");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011AprilS1").getIso8601Duration(), "P6M");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011April").getIso8601Duration(), "P1Y");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011July").getIso8601Duration(), "P1Y");
    assertEquals(PeriodType.getPeriodTypeFromIsoString("2011Oct").getIso8601Duration(), "P1Y");
  }

  @Test
  void testGetPeriodTypePeriods() {
    Calendar calendar = PeriodType.getCalendar();
    Period jan2018 = PeriodType.getPeriodFromIsoString("201801");
    Period q12018 = PeriodType.getPeriodFromIsoString("2018Q1");
    Period y2018 = PeriodType.getPeriodFromIsoString("2018");
    Period fyApril2018 = PeriodType.getPeriodFromIsoString("2018April");
    int inxMonthly = PeriodType.PERIOD_TYPES.indexOf(new MonthlyPeriodType());
    int inxQuarterly = PeriodType.PERIOD_TYPES.indexOf(new QuarterlyPeriodType());
    int inxYearly = PeriodType.PERIOD_TYPES.indexOf(new YearlyPeriodType());
    int inxFinancialApril = PeriodType.PERIOD_TYPES.indexOf(new FinancialAprilPeriodType());
    List<Period> periods = PeriodType.getPeriodTypePeriods(jan2018, calendar);
    assertEquals(jan2018, periods.get(inxMonthly));
    assertEquals(q12018, periods.get(inxQuarterly));
    assertEquals(y2018, periods.get(inxYearly));
    periods = PeriodType.getPeriodTypePeriods(y2018, calendar);
    assertNull(periods.get(inxMonthly));
    assertNull(periods.get(inxQuarterly));
    assertEquals(y2018, periods.get(inxYearly));
    assertNull(periods.get(inxFinancialApril));
    periods = PeriodType.getPeriodTypePeriods(fyApril2018, calendar);
    assertNull(periods.get(inxMonthly));
    assertNull(periods.get(inxQuarterly));
    assertNull(periods.get(inxYearly));
    assertEquals(fyApril2018, periods.get(inxFinancialApril));
  }
}
