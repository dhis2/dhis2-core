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
package org.hisp.dhis.analytics.event.data;

import static java.util.stream.Collectors.toSet;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.common.AnalyticsDateFilter;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.period.PeriodTypeEnum;
import org.hisp.dhis.program.AnalyticsType;
import org.springframework.stereotype.Component;

/**
 * Resolves select/group-by SQL for period dimensions backed by a non-default date field.
 *
 * <p>This keeps bucket derivation logic out of the analytics manager and centralizes the mapping
 * from {@link PeriodDimension#getDateField()} to the actual analytics table date column.
 */
@Component
@RequiredArgsConstructor
public class DateFieldPeriodBucketColumnResolver {
  private static final Set<PeriodTypeEnum> PERIOD_IDENTIFIER_BACKED_TYPES =
      Set.of(
          PeriodTypeEnum.DAILY,
          PeriodTypeEnum.WEEKLY,
          PeriodTypeEnum.WEEKLY_WEDNESDAY,
          PeriodTypeEnum.WEEKLY_THURSDAY,
          PeriodTypeEnum.WEEKLY_FRIDAY,
          PeriodTypeEnum.WEEKLY_SATURDAY,
          PeriodTypeEnum.WEEKLY_SUNDAY,
          PeriodTypeEnum.BI_WEEKLY,
          PeriodTypeEnum.MONTHLY,
          PeriodTypeEnum.BI_MONTHLY,
          PeriodTypeEnum.QUARTERLY,
          PeriodTypeEnum.QUARTERLY_NOV,
          PeriodTypeEnum.SIX_MONTHLY,
          PeriodTypeEnum.SIX_MONTHLY_APRIL,
          PeriodTypeEnum.SIX_MONTHLY_NOV,
          PeriodTypeEnum.YEARLY,
          PeriodTypeEnum.FINANCIAL_FEB,
          PeriodTypeEnum.FINANCIAL_APRIL,
          PeriodTypeEnum.FINANCIAL_JULY,
          PeriodTypeEnum.FINANCIAL_AUG,
          PeriodTypeEnum.FINANCIAL_SEP,
          PeriodTypeEnum.FINANCIAL_OCT,
          PeriodTypeEnum.FINANCIAL_NOV);

  private static final String DATE_PERIOD_STRUCTURE_TABLE = "analytics_rs_dateperiodstructure";

  private static final String DATE_PERIOD_STRUCTURE_ALIAS = "dps_period";

  private static final String DATE_PERIOD_COLUMN = "dateperiod";

  private final SqlBuilder sqlBuilder;

  Optional<String> resolve(
      AnalyticsType analyticsType, DimensionalObject dimension, boolean isGroupByClause) {
    if (dimension.getDimensionType() != DimensionType.PERIOD) {
      return Optional.empty();
    }

    Set<String> dateFields =
        dimension.getItems().stream()
            .map(PeriodDimension.class::cast)
            .map(PeriodDimension::getDateField)
            .filter(Objects::nonNull)
            .collect(toSet());

    if (dateFields.size() != 1) {
      return Optional.empty();
    }

    String dateField = dateFields.iterator().next();

    if (TimeField.OCCURRED_DATE.name().equals(dateField)) {
      return Optional.empty();
    }

    Set<PeriodTypeEnum> periodTypes =
        dimension.getItems().stream()
            .map(PeriodDimension.class::cast)
            .map(period -> period.getPeriodType().getPeriodTypeEnum())
            .collect(toSet());

    if (periodTypes.size() != 1) {
      return Optional.empty();
    }

    Optional<TimeField> timeField = resolveTimeField(dateField);

    if (timeField.isEmpty()) {
      return Optional.empty();
    }

    String dateColumn =
        analyticsType == AnalyticsType.ENROLLMENT
            ? sqlBuilder.quoteAx(timeField.get().getEnrollmentColumnName())
            : sqlBuilder.quoteAx(timeField.get().getEventColumnName());

    return renderPeriodBucketExpression(
        dateColumn, periodTypes.iterator().next(), dimension.getDimensionName(), isGroupByClause);
  }

  private Optional<String> renderPeriodBucketExpression(
      String dateColumn, PeriodTypeEnum periodType, String dimensionName, boolean isGroupByClause) {
    return renderPeriodIdentifierExpression(dateColumn, periodType)
        .or(() -> renderBucket(dateColumn, periodType))
        .map(
            expression ->
                isGroupByClause
                    ? expression
                    : expression + " as " + sqlBuilder.quote(dimensionName));
  }

  private Optional<String> renderPeriodIdentifierExpression(
      String dateColumn, PeriodTypeEnum periodType) {
    if (!isPeriodIdentifierBackedType(periodType)) {
      return Optional.empty();
    }

    String periodTypeName = getPeriodTypeColumnName(periodType);

    return renderBucket(dateColumn, periodType)
        .map(
            bucketExpression ->
                "(select "
                    + sqlBuilder.quote(periodTypeName)
                    + " from "
                    + DATE_PERIOD_STRUCTURE_TABLE
                    + " as "
                    + DATE_PERIOD_STRUCTURE_ALIAS
                    + " where "
                    + DATE_PERIOD_STRUCTURE_ALIAS
                    + "."
                    + sqlBuilder.quote(DATE_PERIOD_COLUMN)
                    + " = "
                    + bucketExpression
                    + ")");
  }

  private boolean isPeriodIdentifierBackedType(PeriodTypeEnum periodType) {
    return PERIOD_IDENTIFIER_BACKED_TYPES.contains(periodType);
  }

  private Optional<TimeField> resolveTimeField(String dateField) {
    return AnalyticsDateFilter.of(dateField)
        .map(AnalyticsDateFilter::getTimeField)
        .or(() -> TimeField.of(dateField));
  }

  private String getPeriodTypeColumnName(PeriodTypeEnum periodType) {
    return periodType.getName().toLowerCase();
  }

  private Optional<String> renderBucket(String dateColumn, PeriodTypeEnum periodType) {
    return Optional.ofNullable(
        switch (periodType) {
          case DAILY -> dateColumn + "::date";
          case YEARLY -> "date_trunc('year', " + dateColumn + ")::date";
          case WEEKLY -> "date_trunc('week', " + dateColumn + ")::date";
          case WEEKLY_WEDNESDAY ->
              "date_trunc('week', "
                  + dateColumn
                  + " + interval '5 days')::date - interval '5 days'";
          case WEEKLY_THURSDAY ->
              "date_trunc('week', "
                  + dateColumn
                  + " + interval '4 days')::date - interval '4 days'";
          case WEEKLY_FRIDAY ->
              "date_trunc('week', "
                  + dateColumn
                  + " + interval '3 days')::date - interval '3 days'";
          case WEEKLY_SATURDAY ->
              "date_trunc('week', "
                  + dateColumn
                  + " + interval '2 days')::date - interval '2 days'";
          case WEEKLY_SUNDAY ->
              "date_trunc('week', " + dateColumn + " + interval '1 day')::date - interval '1 day'";
          case BI_WEEKLY ->
              "date_trunc('week', "
                  + dateColumn
                  + ")::date - ((extract(week from "
                  + dateColumn
                  + ")::int - 1) % 2) * interval '7 days'";
          case MONTHLY -> "date_trunc('month', " + dateColumn + ")::date";
          case QUARTERLY -> "date_trunc('quarter', " + dateColumn + ")::date";
          case QUARTERLY_NOV ->
              "date_trunc('quarter', "
                  + dateColumn
                  + " - interval '1 month')::date + interval '1 month'";
          case BI_MONTHLY ->
              """
              make_date(
                extract(year from %1$s)::int,
                ((extract(month from %1$s)::int - 1) / 2) * 2 + 1,
                1
              )
              """
                  .formatted(dateColumn)
                  .replace('\n', ' ')
                  .replaceAll("\\s+", " ")
                  .trim();
          case SIX_MONTHLY ->
              """
              make_date(
                extract(year from %1$s)::int,
                case when extract(month from %1$s) <= 6 then 1 else 7 end,
                1
              )
              """
                  .formatted(dateColumn)
                  .replace('\n', ' ')
                  .replaceAll("\\s+", " ")
                  .trim();
          case SIX_MONTHLY_APRIL ->
              """
              case
                when extract(month from %1$s) between 4 and 9
                  then make_date(extract(year from %1$s)::int, 4, 1)
                when extract(month from %1$s) >= 10
                  then make_date(extract(year from %1$s)::int, 10, 1)
                else make_date(extract(year from %1$s)::int - 1, 10, 1)
              end
              """
                  .formatted(dateColumn)
                  .replace('\n', ' ')
                  .replaceAll("\\s+", " ")
                  .trim();
          case SIX_MONTHLY_NOV ->
              """
              case
                when extract(month from %1$s) between 5 and 10
                  then make_date(extract(year from %1$s)::int, 5, 1)
                when extract(month from %1$s) >= 11
                  then make_date(extract(year from %1$s)::int, 11, 1)
                else make_date(extract(year from %1$s)::int - 1, 11, 1)
              end
              """
                  .formatted(dateColumn)
                  .replace('\n', ' ')
                  .replaceAll("\\s+", " ")
                  .trim();
          case FINANCIAL_FEB -> renderFinancialYearStart(dateColumn, 2);
          case FINANCIAL_APRIL -> renderFinancialYearStart(dateColumn, 4);
          case FINANCIAL_JULY -> renderFinancialYearStart(dateColumn, 7);
          case FINANCIAL_AUG -> renderFinancialYearStart(dateColumn, 8);
          case FINANCIAL_SEP -> renderFinancialYearStart(dateColumn, 9);
          case FINANCIAL_OCT -> renderFinancialYearStart(dateColumn, 10);
          case FINANCIAL_NOV -> renderFinancialYearStart(dateColumn, 11);
          default -> null;
        });
  }

  private String renderFinancialYearStart(String dateColumn, int startMonth) {
    return """
        make_date(
          case when extract(month from %1$s) >= %2$d
               then extract(year from %1$s)::int
               else extract(year from %1$s)::int - 1
          end, %2$d, 1
        )
        """
        .formatted(dateColumn, startMonth)
        .replace('\n', ' ')
        .replaceAll("\\s+", " ")
        .trim();
  }
}
