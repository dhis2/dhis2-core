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

import static java.lang.Math.round;
import static org.hisp.dhis.parser.expression.antlr.ExpressionParser.ExprContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.QueryModifiers;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.parser.expression.CommonExpressionVisitor;
import org.hisp.dhis.parser.expression.ExpressionState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests {@link ItemYearlyPeriodCount}.
 *
 * @author Jim Grace
 */
@ExtendWith(MockitoExtension.class)
class ItemYearlyPeriodCountTest extends DhisConvenienceTest {
  @Mock private ExprContext ctx;

  @Mock private CommonExpressionVisitor visitor;

  @Mock private ExpressionParams params;

  @Mock private ExpressionState expressionState;

  @Mock private QueryModifiers queryMods;

  private final ItemYearlyPeriodCount target = new ItemYearlyPeriodCount();

  @Test
  void testEvaluateDaily() {
    assertEquals(366, evalWith("20201101"));
    assertEquals(365, evalWith("20210101"));
  }

  @Test
  void testEvaluateWeekly() {
    assertEquals(53, evalWith("2020W10"));
    assertEquals(52, evalWith("2021W11"));
    assertEquals(52, evalWith("2022W12"));
    assertEquals(52, evalWith("2023W23"));
    assertEquals(52, evalWith("2024W34"));
    assertEquals(52, evalWith("2025W45"));
    assertEquals(53, evalWith("2026W46"));
    assertEquals(52, evalWith("2027W47"));
    assertEquals(52, evalWith("2028W48"));
    assertEquals(52, evalWith("2029W49"));
  }

  @Test
  void testEvaluateWeeklyWednesday() {
    assertEquals(52, evalWith("2020WedW10"));
    assertEquals(52, evalWith("2021WedW11"));
    assertEquals(53, evalWith("2022WedW12"));
    assertEquals(52, evalWith("2023WedW23"));
    assertEquals(52, evalWith("2024WedW34"));
    assertEquals(52, evalWith("2025WedW45"));
    assertEquals(52, evalWith("2026WedW46"));
    assertEquals(52, evalWith("2027WedW47"));
    assertEquals(53, evalWith("2028WedW48"));
    assertEquals(52, evalWith("2029WedW49"));
  }

  @Test
  void testEvaluateBiWeekly() {
    assertEquals(27, evalWith("2020BiW10"));
    assertEquals(26, evalWith("2021BiW11"));
    assertEquals(26, evalWith("2022BiW12"));
    assertEquals(26, evalWith("2023BiW13"));
    assertEquals(26, evalWith("2024BiW14"));
    assertEquals(26, evalWith("2025BiW15"));
    assertEquals(27, evalWith("2026BiW16"));
    assertEquals(26, evalWith("2027BiW17"));
    assertEquals(26, evalWith("2028BiW18"));
    assertEquals(26, evalWith("2029BiW19"));
  }

  @Test
  void testEvaluateMonthly() {
    assertEquals(12, evalWith("202209"));
  }

  @Test
  void testEvaluateBiMonthly() {
    assertEquals(6, evalWith("202202B"));
  }

  @Test
  void testEvaluateQuarterly() {
    assertEquals(4, evalWith("2022Q1"));
  }

  @Test
  void testEvaluateYearly() {
    assertEquals(1, evalWith("2022"));
  }

  @Test
  void testEvaluateYearlyFinancial() {
    assertEquals(1, evalWith("2022Oct"));
  }

  @Test
  void testEvaluateWeeklyWithPeriodOffset() {
    // Offset from 2020 into 2021 changes week count from 53 to 52.
    assertEquals(53, evalWith("2020W50"));
    assertEquals(52, evalWith("2020W50", 5));

    // Offset from 2021 into 2020 changes week count from 52 to 53.
    assertEquals(52, evalWith("2021W01"));
    assertEquals(53, evalWith("2021W01", -5));
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private long evalWith(String period) {
    return evalWith(period, 0);
  }

  private long evalWith(String period, int periodOffset) {
    when(visitor.getParams()).thenReturn(params);
    when(params.getPeriods()).thenReturn(List.of(createPeriod(period)));
    when(visitor.getState()).thenReturn(expressionState);

    if (periodOffset == 0) {
      when(expressionState.getQueryMods()).thenReturn(null);
    } else {
      when(expressionState.getQueryMods()).thenReturn(queryMods);
      when(queryMods.getPeriodOffset()).thenReturn(periodOffset);
    }

    return round(target.evaluate(ctx, visitor));
  }
}
