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
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011"), PeriodTypeEnum.YEARLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("201101"), PeriodTypeEnum.MONTHLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011-01"), PeriodTypeEnum.MONTHLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011W1"), PeriodTypeEnum.WEEKLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011W32"), PeriodTypeEnum.WEEKLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011BiW2"), PeriodTypeEnum.BI_WEEKLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011BiW12"), PeriodTypeEnum.BI_WEEKLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("20110101"), PeriodTypeEnum.DAILY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011-01-01"), PeriodTypeEnum.DAILY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011Q3"), PeriodTypeEnum.QUARTERLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011NovQ3"), PeriodTypeEnum.QUARTERLY_NOV);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("201101B"), PeriodTypeEnum.BI_MONTHLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011S1"), PeriodTypeEnum.SIX_MONTHLY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011AprilS1"), PeriodTypeEnum.SIX_MONTHLY_APRIL);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011NovS1"), PeriodTypeEnum.SIX_MONTHLY_NOV);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011April"), PeriodTypeEnum.FINANCIAL_APRIL);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011July"), PeriodTypeEnum.FINANCIAL_JULY);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011Sep"), PeriodTypeEnum.FINANCIAL_SEP);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011Oct"), PeriodTypeEnum.FINANCIAL_OCT);
    assertEquals(PeriodTypeEnum.ofIsoPeriod("2011Nov"), PeriodTypeEnum.FINANCIAL_NOV);

    assertNull(PeriodTypeEnum.ofIsoPeriod("201"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("20111"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("201W2"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("2011Q12"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("2011W234"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("201er2345566"));
    assertNull(PeriodTypeEnum.ofIsoPeriod("2011Q10"));
  }
}
