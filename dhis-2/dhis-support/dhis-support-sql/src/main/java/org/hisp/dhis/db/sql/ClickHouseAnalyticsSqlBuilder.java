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
package org.hisp.dhis.db.sql;

import java.util.Optional;
import java.util.regex.Pattern;
import org.hisp.dhis.period.PeriodTypeEnum;

public class ClickHouseAnalyticsSqlBuilder extends ClickHouseSqlBuilder
    implements AnalyticsSqlBuilder {
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");

  public ClickHouseAnalyticsSqlBuilder(String database) {
    super(database);
  }

  @Override
  public String getEventDataValues() {
    return "ev.eventdatavalues";
  }

  @Override
  public String renderTimestamp(String timestampAsString) {
    return timestampAsString;
  }

  @Override
  public Optional<String> renderStageDatePeriodBucket(
      String stageDateColumn, String periodBucketColumn) {
    String dateExpr = "toDate(" + stageDateColumn + ")";

    return Optional.ofNullable(
        switch (periodBucketColumn) {
          case "yearly" -> "formatDateTime(" + dateExpr + ", '%Y')";
          case "monthly" -> "formatDateTime(" + dateExpr + ", '%Y%m')";
          case "daily" -> "formatDateTime(" + dateExpr + ", '%Y%m%d')";
          case "quarterly" ->
              "concat(formatDateTime("
                  + dateExpr
                  + ", '%Y'), 'Q', toString(toQuarter("
                  + dateExpr
                  + ")))";
          default -> null;
        });
  }

  @Override
  public Optional<String> renderDateFieldPeriodBucketDate(
      String dateColumn, PeriodTypeEnum periodType) {
    String dateExpr = castToDate(dateColumn);
    String expression =
        switch (periodType) {
          case DAILY -> dateExpr;
          case YEARLY -> castToDate(dateTrunc("year", dateExpr));
          case WEEKLY -> castToDate(dateTrunc("week", dateExpr));
          case WEEKLY_WEDNESDAY -> dateShift(weekStart(dateShift(dateExpr, 5)), -5);
          case WEEKLY_THURSDAY -> dateShift(weekStart(dateShift(dateExpr, 4)), -4);
          case WEEKLY_FRIDAY -> dateShift(weekStart(dateShift(dateExpr, 3)), -3);
          case WEEKLY_SATURDAY -> dateShift(weekStart(dateShift(dateExpr, 2)), -2);
          case WEEKLY_SUNDAY -> dateShift(weekStart(dateShift(dateExpr, 1)), -1);
          case BI_WEEKLY ->
              dateShift(weekStart(dateExpr), "-((toISOWeek(" + dateExpr + ") - 1) % 2) * 7");
          case MONTHLY -> castToDate(dateTrunc("month", dateExpr));
          case QUARTERLY -> castToDate(dateTrunc("quarter", dateExpr));
          case QUARTERLY_NOV ->
              dateShift(
                  castToDate(dateTrunc("quarter", dateShift(dateExpr, -1, "MONTH"))), 1, "MONTH");
          case BI_MONTHLY ->
              firstDayOfMonth(year(dateExpr), "((toMonth(" + dateExpr + ") - 1) / 2) * 2 + 1");
          case SIX_MONTHLY ->
              firstDayOfMonth(year(dateExpr), "if(toMonth(" + dateExpr + ") <= 6, 1, 7)");
          case SIX_MONTHLY_APRIL ->
              "if(toMonth("
                  + dateExpr
                  + ") between 4 and 9, "
                  + firstDayOfMonth(year(dateExpr), "4")
                  + ", if(toMonth("
                  + dateExpr
                  + ") >= 10, "
                  + firstDayOfMonth(year(dateExpr), "10")
                  + ", "
                  + firstDayOfMonth(year(dateExpr) + " - 1", "10")
                  + "))";
          case SIX_MONTHLY_NOV ->
              "if(toMonth("
                  + dateExpr
                  + ") between 5 and 10, "
                  + firstDayOfMonth(year(dateExpr), "5")
                  + ", if(toMonth("
                  + dateExpr
                  + ") >= 11, "
                  + firstDayOfMonth(year(dateExpr), "11")
                  + ", "
                  + firstDayOfMonth(year(dateExpr) + " - 1", "11")
                  + "))";
          case FINANCIAL_FEB -> financialYearStart(dateExpr, 2);
          case FINANCIAL_APRIL -> financialYearStart(dateExpr, 4);
          case FINANCIAL_JULY -> financialYearStart(dateExpr, 7);
          case FINANCIAL_AUG -> financialYearStart(dateExpr, 8);
          case FINANCIAL_SEP -> financialYearStart(dateExpr, 9);
          case FINANCIAL_OCT -> financialYearStart(dateExpr, 10);
          case FINANCIAL_NOV -> financialYearStart(dateExpr, 11);
          default -> null;
        };

    return Optional.ofNullable(expression).map(ClickHouseAnalyticsSqlBuilder::collapseWhitespace);
  }

  private String castToDate(String expression) {
    return "toDate(" + expression + ")";
  }

  private String weekStart(String expression) {
    return castToDate(dateTrunc("week", expression));
  }

  private String dateShift(String expression, int amount) {
    return dateShift(expression, Integer.toString(amount), "DAY");
  }

  private String dateShift(String expression, String amount) {
    return dateShift(expression, amount, "DAY");
  }

  private String dateShift(String expression, int amount, String unit) {
    return dateShift(expression, Integer.toString(amount), unit);
  }

  private String dateShift(String expression, String amount, String unit) {
    return switch (unit) {
      case "DAY" -> "addDays(" + expression + ", " + amount + ")";
      case "MONTH" -> "addMonths(" + expression + ", " + amount + ")";
      default ->
          throw new IllegalArgumentException("Unsupported ClickHouse date shift unit: " + unit);
    };
  }

  private String year(String dateExpr) {
    return "toYear(" + dateExpr + ")";
  }

  private String firstDayOfMonth(String yearExpression, String monthExpression) {
    return "addMonths(toDate(concat(toString("
        + yearExpression
        + "), '-01-01')), ("
        + monthExpression
        + ") - 1)";
  }

  private String financialYearStart(String dateExpr, int startMonth) {
    return firstDayOfMonth(
        "if(toMonth("
            + dateExpr
            + ") >= "
            + startMonth
            + ", toYear("
            + dateExpr
            + "), toYear("
            + dateExpr
            + ") - 1)",
        Integer.toString(startMonth));
  }

  private static String collapseWhitespace(String input) {
    return WHITESPACE.matcher(input).replaceAll(" ").trim();
  }
}
