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
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.period.PeriodDimension;
import org.hisp.dhis.program.AnalyticsType;

/**
 * Resolves select/group-by SQL for period dimensions backed by a non-default date field.
 *
 * <p>This keeps bucket derivation logic out of the analytics manager and centralizes the mapping
 * from {@link PeriodDimension#getDateField()} to the actual analytics table date column.
 */
class DateFieldPeriodBucketColumnResolver {
  private final SqlBuilder sqlBuilder;

  DateFieldPeriodBucketColumnResolver(SqlBuilder sqlBuilder) {
    this.sqlBuilder = sqlBuilder;
  }

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

    Set<String> periodTypes =
        dimension.getItems().stream()
            .map(PeriodDimension.class::cast)
            .map(period -> period.getPeriodType().getPeriodTypeEnum().getName().toLowerCase())
            .collect(toSet());

    if (periodTypes.size() != 1) {
      return Optional.empty();
    }

    Optional<TimeField> timeField = TimeField.of(dateField);

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
      String dateColumn, String periodTypeName, String dimensionName, boolean isGroupByClause) {
    return renderBucket(dateColumn, periodTypeName)
        .map(
            expression ->
                isGroupByClause
                    ? expression
                    : expression + " as " + sqlBuilder.quote(dimensionName));
  }

  private Optional<String> renderBucket(String dateColumn, String periodTypeName) {
    return Optional.ofNullable(
        switch (periodTypeName) {
          case "yearly" -> "date_trunc('year', " + dateColumn + ")::date";
          case "weekly" -> "date_trunc('week', " + dateColumn + ")::date";
          case "monthly" -> "date_trunc('month', " + dateColumn + ")::date";
          case "sixmonthlyapril" ->
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
          default -> null;
        });
  }
}
