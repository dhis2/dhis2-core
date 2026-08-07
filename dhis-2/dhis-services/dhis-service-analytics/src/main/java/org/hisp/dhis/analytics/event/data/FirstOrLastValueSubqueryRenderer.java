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

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.time.DateUtils.addYears;
import static org.hisp.dhis.analytics.AnalyticsConstants.ANALYTICS_TBL_ALIAS;
import static org.hisp.dhis.analytics.DataType.NUMERIC;
import static org.hisp.dhis.util.DateUtils.toMediumDate;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.TimeField;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Builds the first-or-last-value sub query used by {@code JdbcEventAnalyticsManager} when the
 * aggregation type resolves to {@link AggregationType#FIRST} or {@link AggregationType#LAST}. The
 * rendered sub query ranks events by time (ascending for FIRST, descending for LAST) within a
 * partition and exposes a {@code pe_rank} column the outer query filters on.
 */
@Component
@RequiredArgsConstructor
public class FirstOrLastValueSubqueryRenderer {

  private static final int LAST_VALUE_YEARS_OFFSET = -10;

  private final AnalyticsSqlBuilder sqlBuilder;

  private final EventTimeFieldSqlRenderer timeFieldSqlRenderer;

  private final ProgramIndicatorService programIndicatorService;

  /**
   * Generates a sub query which provides a view of the data where each row is ranked by the
   * execution date, ascending or descending. The events are partitioned by org unit and attribute
   * option combo. A column {@code pe_rank} defines the rank. Only data for the last 10 years
   * relative to the period end date is included.
   *
   * @param params the {@link EventQueryParams}.
   */
  public String render(EventQueryParams params) {
    Assert.isTrue(
        params.hasValueDimension() || params.hasProgramIndicatorDimension(),
        "Last value aggregation type query must have value dimension or a program indicator");
    return params.hasProgramIndicatorDimension()
        ? renderForProgramIndicator(params)
        : renderForValueDimension(params);
  }

  private String renderForProgramIndicator(EventQueryParams params) {
    String columns = "*," + getProgramIndicatorSql(params) + " as value";
    String timeTest = timeFieldSqlRenderer.renderPeriodTimeFieldSql(params);
    return assembleSubquery(params, columns, timeTest, "");
  }

  private String renderForValueDimension(EventQueryParams params) {
    String valueItem = sqlBuilder.quoteAx(params.getValue().getDimensionItem());
    String columns =
        sqlBuilder.quote("event")
            + ","
            + valueItem
            + ","
            + getFirstOrLastValueSubqueryQuotedColumns(params);
    String timeTest = buildTenYearWindowTimeTest(params);
    String nullTest = " and " + valueItem + " is not null";
    return assembleSubquery(params, columns, timeTest, nullTest);
  }

  private String buildTenYearWindowTimeTest(EventQueryParams params) {
    String timeCol = sqlBuilder.quoteAx(params.getTimeFieldAsFieldFallback());
    Date latest = params.getLatestEndDate();
    Date earliest = addYears(latest, LAST_VALUE_YEARS_OFFSET);
    return timeCol
        + " >= '"
        + toMediumDate(earliest)
        + "' "
        + "and "
        + timeCol
        + " <= '"
        + toMediumDate(latest)
        + "'";
  }

  private String assembleSubquery(
      EventQueryParams params, String columns, String timeTest, String nullTest) {
    String timeCol = sqlBuilder.quoteAx(params.getTimeFieldAsFieldFallback());
    String createdCol = sqlBuilder.quoteAx(TimeField.CREATED.getEventColumnName());
    String order =
        params.getAggregationTypeFallback().isFirstPeriodAggregationType() ? "asc" : "desc";
    String partitionByClause = getFirstOrLastValuePartitionByClause(params);

    return "(select %s,row_number() over (%s order by %s %s, %s %s) as pe_rank from %s as %s where %s%s)"
        .formatted(
            columns,
            partitionByClause,
            timeCol,
            order,
            createdCol,
            order,
            params.getTableName(),
            ANALYTICS_TBL_ALIAS,
            timeTest,
            nullTest);
  }

  /**
   * Returns the partition by clause for the first or last event sub query. If the aggregation type
   * of the given parameters specifies "first" or "last" as the general aggregation type, the
   * partition by clause will use the dimensions from the analytics query as columns in order to
   * rank events in all dimensions. In this case, the outer query will perform no aggregation and
   * simply filter by the top ranked events.
   *
   * <p>If the aggregation type specifies another aggregation type (i.e. "sum" or "average") as the
   * general aggregation type, the partition by clause will use the "ou" and "ao" columns to rank
   * events in the time dimension only, and have the outer query perform the aggregation in the
   * other dimensions, as well as filter by the top ranked events.
   *
   * @param params the {@link EventQueryParams}.
   * @return the partition by clause.
   */
  private String getFirstOrLastValuePartitionByClause(EventQueryParams params) {
    if (params.isAnyAggregationType(AggregationType.FIRST, AggregationType.LAST)) {
      return getFirstOrLastValuePartitionByColumns(params.getNonPeriodDimensions());
    } else {
      return "partition by " + quoteAliasCommaDelimited(List.of("ou", "ao"));
    }
  }

  /**
   * Returns the partition by clause for the first or last event sub query. The columns to partition
   * by are based on the given list of dimensions.
   *
   * @param dimensions the list of {@link DimensionalObject}.
   * @return the partition by clause.
   */
  private String getFirstOrLastValuePartitionByColumns(List<DimensionalObject> dimensions) {
    String partitionColumns =
        dimensions.stream()
            .map(DimensionalObject::getDimensionName)
            .map(sqlBuilder::quoteAx)
            .collect(Collectors.joining(","));

    String sql = "";

    if (isNotEmpty(partitionColumns)) {
      sql += "partition by " + partitionColumns;
    }

    return sql;
  }

  /**
   * Returns quoted names of columns for the {@link AggregationType#FIRST} or {@link
   * AggregationType#LAST} sub query (not for program indicators).
   *
   * @param params the {@link EventQueryParams}.
   */
  private String getFirstOrLastValueSubqueryQuotedColumns(EventQueryParams params) {
    return params.getDimensionsAndFilters().stream()
        .map(dim -> sqlBuilder.quote(dim.getDimensionName()))
        .collect(joining(","));
  }

  /** Returns the program indicator SQL from the query parameters. */
  private String getProgramIndicatorSql(EventQueryParams params) {
    return programIndicatorService.getAnalyticsSql(
        params.getProgramIndicator().getExpression(),
        NUMERIC,
        params.getProgramIndicator(),
        params.getEarliestStartDate(),
        params.getLatestEndDate());
  }

  private String quoteAliasCommaDelimited(List<String> items) {
    return items.stream().map(sqlBuilder::quoteAx).collect(Collectors.joining(","));
  }
}
