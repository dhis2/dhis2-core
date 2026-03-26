/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.db.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClickHouseAnalyticsSqlBuilderTest {
  private final ClickHouseAnalyticsSqlBuilder sqlBuilder =
      new ClickHouseAnalyticsSqlBuilder("dhis2");

  @ParameterizedTest
  @MethodSource("bucketDateCases")
  void shouldRenderDateFieldPeriodBucketDate(PeriodTypeEnum periodType, String expected) {
    assertEquals(
        expected,
        sqlBuilder.renderDateFieldPeriodBucketDate("ax.\"created\"", periodType).orElseThrow());
  }

  private static Stream<Arguments> bucketDateCases() {
    String dateExpr = "toDate(ax.\"created\")";
    String yearStart = "toDate(concat(toString(toYear(" + dateExpr + ")), '-01-01'))";

    return Stream.of(
        Arguments.of(PeriodTypeEnum.DAILY, dateExpr),
        Arguments.of(PeriodTypeEnum.YEARLY, "toDate(date_trunc('year', " + dateExpr + "))"),
        Arguments.of(PeriodTypeEnum.WEEKLY, "toDate(date_trunc('week', " + dateExpr + "))"),
        Arguments.of(
            PeriodTypeEnum.WEEKLY_WEDNESDAY,
            "addDays(toDate(date_trunc('week', addDays(" + dateExpr + ", 5))), -5)"),
        Arguments.of(
            PeriodTypeEnum.WEEKLY_THURSDAY,
            "addDays(toDate(date_trunc('week', addDays(" + dateExpr + ", 4))), -4)"),
        Arguments.of(
            PeriodTypeEnum.WEEKLY_FRIDAY,
            "addDays(toDate(date_trunc('week', addDays(" + dateExpr + ", 3))), -3)"),
        Arguments.of(
            PeriodTypeEnum.WEEKLY_SATURDAY,
            "addDays(toDate(date_trunc('week', addDays(" + dateExpr + ", 2))), -2)"),
        Arguments.of(
            PeriodTypeEnum.WEEKLY_SUNDAY,
            "addDays(toDate(date_trunc('week', addDays(" + dateExpr + ", 1))), -1)"),
        Arguments.of(
            PeriodTypeEnum.BI_WEEKLY,
            "addDays(toDate(date_trunc('week', "
                + dateExpr
                + ")), -((toISOWeek("
                + dateExpr
                + ") - 1) % 2) * 7)"),
        Arguments.of(PeriodTypeEnum.MONTHLY, "toDate(date_trunc('month', " + dateExpr + "))"),
        Arguments.of(
            PeriodTypeEnum.BI_MONTHLY,
            "addMonths(" + yearStart + ", (((toMonth(" + dateExpr + ") - 1) / 2) * 2 + 1) - 1)"),
        Arguments.of(PeriodTypeEnum.QUARTERLY, "toDate(date_trunc('quarter', " + dateExpr + "))"),
        Arguments.of(
            PeriodTypeEnum.QUARTERLY_NOV,
            "addMonths(toDate(date_trunc('quarter', addMonths(" + dateExpr + ", -1))), 1)"),
        Arguments.of(
            PeriodTypeEnum.SIX_MONTHLY,
            "addMonths(" + yearStart + ", (if(toMonth(" + dateExpr + ") <= 6, 1, 7)) - 1)"),
        Arguments.of(
            PeriodTypeEnum.SIX_MONTHLY_APRIL,
            "if(toMonth("
                + dateExpr
                + ") between 4 and 9, addMonths("
                + yearStart
                + ", (4) - 1), if(toMonth("
                + dateExpr
                + ") >= 10, addMonths("
                + yearStart
                + ", (10) - 1), addMonths(toDate(concat(toString(toYear("
                + dateExpr
                + ") - 1), '-01-01')), (10) - 1)))"),
        Arguments.of(
            PeriodTypeEnum.SIX_MONTHLY_NOV,
            "if(toMonth("
                + dateExpr
                + ") between 5 and 10, addMonths("
                + yearStart
                + ", (5) - 1), if(toMonth("
                + dateExpr
                + ") >= 11, addMonths("
                + yearStart
                + ", (11) - 1), addMonths(toDate(concat(toString(toYear("
                + dateExpr
                + ") - 1), '-01-01')), (11) - 1)))"),
        Arguments.of(
            PeriodTypeEnum.FINANCIAL_FEB,
            "addMonths(toDate(concat(toString(if(toMonth("
                + dateExpr
                + ") >= 2, toYear("
                + dateExpr
                + "), toYear("
                + dateExpr
                + ") - 1)), '-01-01')), (2) - 1)"),
        Arguments.of(
            PeriodTypeEnum.FINANCIAL_APRIL,
            "addMonths(toDate(concat(toString(if(toMonth("
                + dateExpr
                + ") >= 4, toYear("
                + dateExpr
                + "), toYear("
                + dateExpr
                + ") - 1)), '-01-01')), (4) - 1)"),
        Arguments.of(
            PeriodTypeEnum.FINANCIAL_JULY,
            "addMonths(toDate(concat(toString(if(toMonth("
                + dateExpr
                + ") >= 7, toYear("
                + dateExpr
                + "), toYear("
                + dateExpr
                + ") - 1)), '-01-01')), (7) - 1)"),
        Arguments.of(
            PeriodTypeEnum.FINANCIAL_AUG,
            "addMonths(toDate(concat(toString(if(toMonth("
                + dateExpr
                + ") >= 8, toYear("
                + dateExpr
                + "), toYear("
                + dateExpr
                + ") - 1)), '-01-01')), (8) - 1)"),
        Arguments.of(
            PeriodTypeEnum.FINANCIAL_SEP,
            "addMonths(toDate(concat(toString(if(toMonth("
                + dateExpr
                + ") >= 9, toYear("
                + dateExpr
                + "), toYear("
                + dateExpr
                + ") - 1)), '-01-01')), (9) - 1)"),
        Arguments.of(
            PeriodTypeEnum.FINANCIAL_OCT,
            "addMonths(toDate(concat(toString(if(toMonth("
                + dateExpr
                + ") >= 10, toYear("
                + dateExpr
                + "), toYear("
                + dateExpr
                + ") - 1)), '-01-01')), (10) - 1)"),
        Arguments.of(
            PeriodTypeEnum.FINANCIAL_NOV,
            "addMonths(toDate(concat(toString(if(toMonth("
                + dateExpr
                + ") >= 11, toYear("
                + dateExpr
                + "), toYear("
                + dateExpr
                + ") - 1)), '-01-01')), (11) - 1)"));
  }
}
