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

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.common.AnalyticsDateFilter;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
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
  record JoinClause(String table, String alias, String condition) {
    String toSql() {
      return "left join " + table + " as " + alias + " on " + condition;
    }
  }

  record ResolvedExpression(
      String selectExpression,
      String groupByExpression,
      String sourceColumn,
      Optional<JoinClause> joinClause) {}

  private static final Set<PeriodTypeEnum> PERIOD_IDENTIFIER_BACKED_TYPES =
      EnumSet.of(
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

  private final AnalyticsSqlBuilder sqlBuilder;

  Optional<String> resolve(
      AnalyticsType analyticsType, DimensionalObject dimension, boolean isGroupByClause) {
    return resolve(analyticsType, dimension, "ax")
        .map(
            expression ->
                isGroupByClause ? expression.groupByExpression() : expression.selectExpression());
  }

  Optional<ResolvedExpression> resolve(
      AnalyticsType analyticsType, DimensionalObject dimension, String tableAlias) {
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
        sqlBuilder.quote(tableAlias, getSourceColumn(analyticsType, timeField.get()));

    return renderPeriodBucketExpression(
        dateColumn,
        periodTypes.iterator().next(),
        dimension.getDimensionName(),
        getSourceColumn(analyticsType, timeField.get()),
        tableAlias);
  }

  Optional<String> resolveSourceColumn(AnalyticsType analyticsType, DimensionalObject dimension) {
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

    return resolveTimeField(dateField).map(timeField -> getSourceColumn(analyticsType, timeField));
  }

  private Optional<ResolvedExpression> renderPeriodBucketExpression(
      String dateColumn,
      PeriodTypeEnum periodType,
      String dimensionName,
      String sourceColumn,
      String tableAlias) {
    return Optional.of(
        renderResolvedExpression(
            renderBucket(dateColumn, periodType),
            periodType,
            dimensionName,
            sourceColumn,
            tableAlias));
  }

  private ResolvedExpression renderResolvedExpression(
      String bucketExpression,
      PeriodTypeEnum periodType,
      String dimensionName,
      String sourceColumn,
      String tableAlias) {
    String expression = bucketExpression;
    Optional<JoinClause> joinClause = Optional.empty();

    if (isPeriodIdentifierBackedType(periodType)) {
      String periodTypeName = getPeriodTypeColumnName(periodType);

      if (sqlBuilder.useJoinForDatePeriodStructureLookup()) {
        String joinAlias = getJoinAlias(tableAlias, sourceColumn);
        expression = joinAlias + "." + sqlBuilder.quote(periodTypeName);
        joinClause =
            Optional.of(
                new JoinClause(
                    DATE_PERIOD_STRUCTURE_TABLE,
                    joinAlias,
                    joinAlias
                        + "."
                        + sqlBuilder.quote(DATE_PERIOD_COLUMN)
                        + " = "
                        + bucketExpression));
      } else {
        expression =
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
                + ")";
      }
    }

    return new ResolvedExpression(
        expression + " as " + sqlBuilder.quote(dimensionName),
        expression,
        sourceColumn,
        joinClause);
  }

  private boolean isPeriodIdentifierBackedType(PeriodTypeEnum periodType) {
    return PERIOD_IDENTIFIER_BACKED_TYPES.contains(periodType);
  }

  private Optional<TimeField> resolveTimeField(String dateField) {
    return AnalyticsDateFilter.of(dateField)
        .map(AnalyticsDateFilter::getTimeField)
        .or(() -> TimeField.of(dateField));
  }

  private String getSourceColumn(AnalyticsType analyticsType, TimeField timeField) {
    return analyticsType == AnalyticsType.ENROLLMENT
        ? timeField.getEnrollmentColumnName()
        : timeField.getEventColumnName();
  }

  private String getPeriodTypeColumnName(PeriodTypeEnum periodType) {
    return periodType.getName().toLowerCase();
  }

  private String renderBucket(String dateColumn, PeriodTypeEnum periodType) {
    return sqlBuilder
        .renderDateFieldPeriodBucketDate(dateColumn, periodType)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "AnalyticsSqlBuilder %s does not support period bucket date rendering for %s"
                        .formatted(sqlBuilder.getClass().getSimpleName(), periodType)));
  }

  private String getJoinAlias(String tableAlias, String sourceColumn) {
    return (DATE_PERIOD_STRUCTURE_ALIAS + "_" + tableAlias + "_" + sourceColumn)
        .replaceAll("[^A-Za-z0-9_]", "_");
  }
}
