/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Test the {@link PeriodTypeEnum} parsing of ISO periods to a type.
 *
 * @author Jan Bernitt
 */
class PeriodTypeEnumTest {

  @Test
  void testOfIsoPeriod() {
    assertEquals(PeriodTypeEnum.YEARLY, PeriodTypeEnum.ofIsoPeriod("2011"));
    assertEquals(PeriodTypeEnum.MONTHLY, PeriodTypeEnum.ofIsoPeriod("201101"));
    assertEquals(PeriodTypeEnum.MONTHLY, PeriodTypeEnum.ofIsoPeriod("2011-01"));
    assertEquals(PeriodTypeEnum.WEEKLY, PeriodTypeEnum.ofIsoPeriod("2011W1"));
    assertEquals(PeriodTypeEnum.WEEKLY, PeriodTypeEnum.ofIsoPeriod("2011W32"));
    assertEquals(PeriodTypeEnum.BI_WEEKLY, PeriodTypeEnum.ofIsoPeriod("2011BiW2"));
    assertEquals(PeriodTypeEnum.BI_WEEKLY, PeriodTypeEnum.ofIsoPeriod("2011BiW12"));
    assertEquals(PeriodTypeEnum.DAILY, PeriodTypeEnum.ofIsoPeriod("20110101"));
    assertEquals(PeriodTypeEnum.DAILY, PeriodTypeEnum.ofIsoPeriod("2011-01-01"));
    assertEquals(PeriodTypeEnum.QUARTERLY, PeriodTypeEnum.ofIsoPeriod("2011Q3"));
    assertEquals(PeriodTypeEnum.QUARTERLY_NOV, PeriodTypeEnum.ofIsoPeriod("2011NovQ3"));
    assertEquals(PeriodTypeEnum.BI_MONTHLY, PeriodTypeEnum.ofIsoPeriod("201101B"));
    assertEquals(PeriodTypeEnum.SIX_MONTHLY, PeriodTypeEnum.ofIsoPeriod("2011S1"));
    assertEquals(PeriodTypeEnum.SIX_MONTHLY_APRIL, PeriodTypeEnum.ofIsoPeriod("2011AprilS1"));
    assertEquals(PeriodTypeEnum.SIX_MONTHLY_NOV, PeriodTypeEnum.ofIsoPeriod("2011NovS1"));
    assertEquals(PeriodTypeEnum.FINANCIAL_APRIL, PeriodTypeEnum.ofIsoPeriod("2011April"));
    assertEquals(PeriodTypeEnum.FINANCIAL_JULY, PeriodTypeEnum.ofIsoPeriod("2011July"));
    assertEquals(PeriodTypeEnum.FINANCIAL_SEP, PeriodTypeEnum.ofIsoPeriod("2011Sep"));
    assertEquals(PeriodTypeEnum.FINANCIAL_OCT, PeriodTypeEnum.ofIsoPeriod("2011Oct"));
    assertEquals(PeriodTypeEnum.FINANCIAL_NOV, PeriodTypeEnum.ofIsoPeriod("2011Nov"));

    assertNull(PeriodTypeEnum.ofIsoPeriod("201"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("20111"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("201W2"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("2011Q12"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("2011W234"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("201er2345566"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("2011Q10"));
  }
}
