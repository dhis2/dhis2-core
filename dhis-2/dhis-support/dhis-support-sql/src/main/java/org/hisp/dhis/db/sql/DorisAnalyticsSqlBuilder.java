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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.period.PeriodTypeEnum;

public class DorisAnalyticsSqlBuilder extends DorisSqlBuilder implements AnalyticsSqlBuilder {
  private static final DateTimeFormatter SQL_DATETIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS][.SS][.S]");

  public DorisAnalyticsSqlBuilder(String catalog, String driverFilename) {
    super(catalog, driverFilename);
  }

  @Override
  public String getEventDataValues() {
    return "ev.eventdatavalues";
  }

  @Override
  public String renderTimestamp(String timestampAsString) {
    if (StringUtils.isBlank(timestampAsString)) return null;
    LocalDateTime dateTime = tryParseDateTime(timestampAsString);
    if (dateTime == null) {
      // Keep non-timestamp values (for example period buckets like "202104") as-is.
      return timestampAsString;
    }

    String formattedDate = dateTime.format(TIMESTAMP_FORMATTER);

    // Find the position of the decimal point
    int decimalPoint = formattedDate.lastIndexOf('.');
    if (decimalPoint != -1) {
      // Remove trailing zeros after decimal point
      String millisPart = formattedDate.substring(decimalPoint + 1);
      millisPart = millisPart.replaceAll("0+$", ""); // Remove all trailing zeros

      // If all digits were zeros, use "0" instead of empty string
      if (millisPart.isEmpty()) {
        millisPart = "0";
      }

      formattedDate = formattedDate.substring(0, decimalPoint + 1) + millisPart;
    }

    return formattedDate;
  }

  @Override
  public Optional<String> renderStageDatePeriodBucket(
      String stageDateColumn, String periodBucketColumn) {
    String dateExpr = "cast(" + stageDateColumn + " as date)";

    return Optional.ofNullable(
        switch (periodBucketColumn) {
          case "yearly" -> "date_format(" + dateExpr + ", '%Y')";
          case "monthly" -> "date_format(" + dateExpr + ", '%Y%m')";
          case "daily" -> "date_format(" + dateExpr + ", '%Y%m%d')";
          case "quarterly" ->
              "concat(date_format("
                  + dateExpr
                  + ", '%Y'), 'Q', cast(quarter("
                  + dateExpr
                  + ") as char))";
          default -> null;
        });
  }

  @Override
  public Optional<String> renderDateFieldPeriodBucketDate(
      String dateColumn, PeriodTypeEnum periodType) {
    String dateExpr = castToDate(dateColumn);

    return Optional.ofNullable(
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
              dateShift(weekStart(dateExpr), "-((weekofyear(" + dateExpr + ") - 1) % 2) * 7");
          case MONTHLY -> castToDate(dateTrunc("month", dateExpr));
          case QUARTERLY -> castToDate(dateTrunc("quarter", dateExpr));
          case QUARTERLY_NOV ->
              dateShift(
                  castToDate(dateTrunc("quarter", dateShift(dateExpr, -1, "MONTH"))), 1, "MONTH");
          case BI_MONTHLY ->
              firstDayOfMonth(year(dateExpr), "((month(" + dateExpr + ") - 1) / 2) * 2 + 1");
          case SIX_MONTHLY ->
              firstDayOfMonth(
                  year(dateExpr), "case when month(" + dateExpr + ") <= 6 then 1 else 7 end");
          case SIX_MONTHLY_APRIL ->
              "case when month("
                  + dateExpr
                  + ") between 4 and 9 then "
                  + firstDayOfMonth(year(dateExpr), "4")
                  + " when month("
                  + dateExpr
                  + ") >= 10 then "
                  + firstDayOfMonth(year(dateExpr), "10")
                  + " else "
                  + firstDayOfMonth(year(dateExpr) + " - 1", "10")
                  + " end";
          case SIX_MONTHLY_NOV ->
              "case when month("
                  + dateExpr
                  + ") between 5 and 10 then "
                  + firstDayOfMonth(year(dateExpr), "5")
                  + " when month("
                  + dateExpr
                  + ") >= 11 then "
                  + firstDayOfMonth(year(dateExpr), "11")
                  + " else "
                  + firstDayOfMonth(year(dateExpr) + " - 1", "11")
                  + " end";
          case FINANCIAL_FEB -> financialYearStart(dateExpr, 2);
          case FINANCIAL_APRIL -> financialYearStart(dateExpr, 4);
          case FINANCIAL_JULY -> financialYearStart(dateExpr, 7);
          case FINANCIAL_AUG -> financialYearStart(dateExpr, 8);
          case FINANCIAL_SEP -> financialYearStart(dateExpr, 9);
          case FINANCIAL_OCT -> financialYearStart(dateExpr, 10);
          case FINANCIAL_NOV -> financialYearStart(dateExpr, 11);
          default -> null;
        });
  }

  @Override
  public boolean useJoinForDatePeriodStructureLookup() {
    return true;
  }

  private String castToDate(String expression) {
    return "cast(" + expression + " as date)";
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
    return "date_add(" + expression + ", interval " + amount + " " + unit + ")";
  }

  private String year(String dateExpr) {
    return "year(" + dateExpr + ")";
  }

  private String firstDayOfMonth(String yearExpression, String monthExpression) {
    return "date_add(makedate("
        + yearExpression
        + ", 1), interval ("
        + monthExpression
        + " - 1) month)";
  }

  private String financialYearStart(String dateExpr, int startMonth) {
    return firstDayOfMonth(
        "case when month("
            + dateExpr
            + ") >= "
            + startMonth
            + " then year("
            + dateExpr
            + ") else year("
            + dateExpr
            + ") - 1 end",
        Integer.toString(startMonth));
  }

  private LocalDateTime tryParseDateTime(String value) {
    try {
      return LocalDateTime.parse(value);
    } catch (DateTimeParseException ignored) {
      // Try SQL-like datetime format used by some JDBC result values.
    }

    try {
      return LocalDateTime.parse(value, SQL_DATETIME_FORMATTER);
    } catch (DateTimeParseException ignored) {
      return null;
    }
  }
}
