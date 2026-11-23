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
package org.hisp.dhis.analytics.event.data.stage;

import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.analytics.DataType.TEXT;

import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.ColumnAndAlias;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.AnalyticsType;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link StageQuerySqlFacade}.
 *
 * <p>Delegates classification and SQL rendering to specialized stage services and returns empty
 * results when a query item is not stage-specific.
 */
@Component
public class DefaultStageQuerySqlFacade implements StageQuerySqlFacade {
  private final StageQueryItemClassifier classifier;
  private final StageDatePeriodBucketSqlRenderer dateRenderer;
  private final StageOrgUnitSqlService stageOrgUnitSqlService;
  private final AnalyticsSqlBuilder sqlBuilder;

  /**
   * Creates a stage SQL facade.
   *
   * @param classifier stage query item classifier
   * @param dateRenderer stage date period renderer
   * @param stageOrgUnitSqlService stage org unit SQL service
   * @param sqlBuilder SQL builder used to quote the event-path stage date column
   */
  public DefaultStageQuerySqlFacade(
      StageQueryItemClassifier classifier,
      StageDatePeriodBucketSqlRenderer dateRenderer,
      StageOrgUnitSqlService stageOrgUnitSqlService,
      AnalyticsSqlBuilder sqlBuilder) {
    this.classifier = classifier;
    this.dateRenderer = dateRenderer;
    this.stageOrgUnitSqlService = stageOrgUnitSqlService;
    this.sqlBuilder = sqlBuilder;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<ColumnAndAlias> resolveSelectColumn(
      QueryItem item,
      EventQueryParams params,
      boolean isGroupByClause,
      boolean isAggregated,
      String stageOuValueColumnTableAlias) {
    if (classifier.isStageOrgUnit(item)) {
      return Optional.of(
          stageOrgUnitSqlService.selectColumn(
              item, params, isGroupByClause, stageOuValueColumnTableAlias));
    }

    if (isAggregated && classifier.isStageDate(item)) {
      if (isFirstOrLastDateItem(item, params)) {
        String expression =
            sqlBuilder.cast("'" + params.getFirstOrLastStagePeriod().isoPeriod() + "'", TEXT);
        return Optional.of(
            isGroupByClause
                ? ColumnAndAlias.ofColumn(expression)
                : ColumnAndAlias.ofColumnAndAlias(expression, item.getItemName()));
      }
      Optional<String> periodBucket = dateRenderer.resolvePeriodBucketColumn(item);
      if (periodBucket.isPresent()) {
        String expression =
            dateRenderer.renderPeriodBucketExpression(
                sqlBuilder.quoteAx(item.getItemId()), periodBucket.get());
        return Optional.of(
            isGroupByClause
                ? ColumnAndAlias.ofColumn(expression)
                : ColumnAndAlias.ofColumnAndAlias(expression, item.getItemName()));
      }
    }

    return Optional.empty();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<String> resolveWhereClause(
      QueryItem item, EventQueryParams params, AnalyticsType analyticsType) {
    if (classifier.isStageOrgUnit(item)) {
      return Optional.of(stageOrgUnitSqlService.whereClause(item, params, analyticsType));
    }

    return Optional.empty();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<String> resolveDisjointPeriodFilter(
      QueryItem item, EventQueryParams params, String dateColumn) {
    if (!isGroupedStageDate(item, params)) {
      return Optional.empty();
    }

    return requestedDateRanges(item)
        .filter(ranges -> ranges.size() > 1)
        .map(ranges -> renderAnyRange(ranges, dateColumn));
  }

  /**
   * Returns true when the given item is a stage date dimension of the given query with several
   * values, other than the date item chosen for FIRST/LAST aggregation.
   */
  private boolean isGroupedStageDate(QueryItem item, EventQueryParams params) {
    return item.getDimensionValues().size() > 1
        && classifier.isStageDate(item)
        && isDimensionOf(item, params)
        && !isFirstOrLastDateItem(item, params);
  }

  /**
   * Returns true when the given item is one of the dimensions of the given query. Compares by
   * identity, since a filter on the same stage date is equal to the dimension.
   */
  private boolean isDimensionOf(QueryItem item, EventQueryParams params) {
    return params.getItems().stream().anyMatch(dimension -> dimension == item);
  }

  /** Returns true when the given item is the date item chosen for FIRST/LAST aggregation. */
  private boolean isFirstOrLastDateItem(QueryItem item, EventQueryParams params) {
    return params.hasFirstOrLastStagePeriod()
        && params.getFirstOrLastStagePeriod().dateItem().equals(item);
  }

  /**
   * Returns the days of the requested periods as closed-open ranges, so that adjacent periods merge
   * into one range and the whole last day is included.
   *
   * @return the merged ranges in date order, or empty when a value is not a period.
   */
  private Optional<Set<Range<LocalDate>>> requestedDateRanges(QueryItem item) {
    TreeRangeSet<LocalDate> dates = TreeRangeSet.create();
    for (String value : item.getDimensionValues()) {
      Period period = Period.ofNullable(value);
      if (period == null) {
        return Optional.empty();
      }
      dates.add(
          Range.closedOpen(
              toLocalDate(period.getStartDate()), toLocalDate(period.getEndDate()).plusDays(1)));
    }
    return Optional.of(dates.asRanges());
  }

  private LocalDate toLocalDate(Date date) {
    return LocalDate.ofInstant(date.toInstant(), ZoneId.systemDefault());
  }

  /** Renders a predicate matching a date column in any of the given closed-open ranges. */
  private String renderAnyRange(Set<Range<LocalDate>> ranges, String dateColumn) {
    return ranges.stream()
        .map(
            range ->
                "%1$s >= %2$s and %1$s < %3$s"
                    .formatted(
                        dateColumn,
                        sqlBuilder.singleQuote(range.lowerEndpoint().toString()),
                        sqlBuilder.singleQuote(range.upperEndpoint().toString())))
        .collect(joining(" or ", "(", ")"));
  }
}
