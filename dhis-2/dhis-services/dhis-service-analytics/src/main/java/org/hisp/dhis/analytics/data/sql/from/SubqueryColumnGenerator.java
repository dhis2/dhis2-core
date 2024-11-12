/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.analytics.data.sql.from;

import static java.lang.String.join;
import static org.hisp.dhis.analytics.DataQueryParams.LEVEL_PREFIX;
import static org.hisp.dhis.analytics.data.sql.AnalyticsColumns.*;
import static org.hisp.dhis.common.collection.CollectionUtils.concat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.data.sql.AnalyticsColumns;
import org.hisp.dhis.analytics.data.sql.DimensionsUtils;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.db.sql.SqlBuilder;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.springframework.util.Assert;

/**
 * A specialized builder class for generating SQL columns in the context of analytics subqueries
 * This class handles complex data aggregations, organizational hierarchies, and time-based
 * analytics.
 *
 * <p>The ColumnBuilder supports various aggregation types including:
 *
 * <ul>
 *   <li>Basic statistical operations (SUM, AVERAGE, COUNT)
 *   <li>Temporal analysis (FIRST, LAST, LAST_IN_PERIOD)
 *   <li>Boundary operations (MIN, MAX)
 * </ul>
 */
public class SubqueryColumnGenerator {
  private final DataQueryParams params;
  private final SqlBuilder sqlBuilder;
  private final DimensionsUtils dimensionsUtils;

  private static final List<String> COMPOSITE_KEY_COLUMNS = AnalyticsColumns.getDimensionColumns();

  public SubqueryColumnGenerator(DataQueryParams params, SqlBuilder sqlBuilder) {
    this.params = params;
    this.sqlBuilder = sqlBuilder;
    this.dimensionsUtils = new DimensionsUtils(sqlBuilder);
  }

  /** Returns columns for first/last value subqueries */
  public String getFirstOrLastValueColumns() {
    return join(
        ",",
        concat(
            getQuotedBaseColumns(),
            getDataApprovalColumns(),
            getFirstOrLastValueDimensionColumns()));
  }

  /** Returns partition columns for first/last value subqueries */
  public String getFirstOrLastValuePartitionColumns() {
    if (params.isAnyAggregationType(AggregationType.FIRST, AggregationType.LAST)) {
      return getDimensionColumnsForNonPeriodDimensions();
    }
    return getQuotedCompositeKeyColumns();
  }

  /** Returns dimension columns for min/max subqueries */
  public String getMinMaxDimensionColumns() {
    return join(
        ",",
        concat(
            List.of(sqlBuilder.quoteAx(OU)),
            getDataApprovalColumns(),
            getQuotedDimensionColumns(params.getDimensions())));
  }

  public String getMinMaxValueColumns() {
    AggregationType periodAggregationType = params.getAggregationType().getPeriodAggregationType();
    Assert.isTrue(
        AggregationType.MIN == periodAggregationType
            || AggregationType.MAX == periodAggregationType,
        "periodAggregationType must be MIN or MAX, not " + periodAggregationType);

    String function = periodAggregationType.name().toLowerCase();
    return buildQuotedFunctionString(function, List.of(DAYSXVALUE, DAYSNO, VALUE, TEXTVALUE));
  }

  private List<String> getQuotedBaseColumns() {
    return toQuotedList(AnalyticsColumns.getFirstLastValueColumns());
  }

  private List<String> getDataApprovalColumns() {
    List<String> columns = new ArrayList<>();

    if (params.isDataApproval()) {
      columns.add(sqlBuilder.quote(APPROVALLEVEL));

      for (OrganisationUnit unit : params.getDataApprovalLevels().keySet()) {
        columns.add(sqlBuilder.quote(LEVEL_PREFIX + unit.getLevel()));
      }
    }
    return columns;
  }

  private List<String> getFirstOrLastValueDimensionColumns() {
    Period period = params.getLatestPeriod();
    return params.getDimensionsAndFilters().stream()
        .map(
            dim ->
                DimensionType.PERIOD == dim.getDimensionType() && period != null
                    ? String.format(
                        "cast('%s' as text) as %s",
                        period.getDimensionItem(), sqlBuilder.quote(dim.getDimensionName()))
                    : sqlBuilder.quote(dim.getDimensionName()))
        .toList();
  }

  private String getDimensionColumnsForNonPeriodDimensions() {
    return dimensionsUtils.getCommaDelimitedQuotedDimensionColumns(params.getNonPeriodDimensions());
  }

  private String getQuotedCompositeKeyColumns() {
    return COMPOSITE_KEY_COLUMNS.stream().map(sqlBuilder::quoteAx).collect(Collectors.joining(","));
  }

  private List<String> getQuotedDimensionColumns(Collection<DimensionalObject> dimensions) {
    return dimensions.stream()
        .filter(d -> !d.isFixed())
        .map(DimensionalObject::getDimensionName)
        .map(sqlBuilder::quote)
        .collect(Collectors.toList());
  }

  private String buildQuotedFunctionString(String function, List<String> columns) {
    return columns.stream()
        .map(
            item ->
                String.format(
                    "%s(%s) as %s", function, sqlBuilder.quote(item), sqlBuilder.quote(item)))
        .collect(Collectors.joining(","));
  }

  /**
   * Returns a list of quoted relations.
   *
   * @param relations the list of relations.
   * @return a list of quoted relations.
   */
  protected List<String> toQuotedList(List<String> relations) {
    return relations.stream().map(this::quote).toList();
  }

  /**
   * @param relation the relation to quote, e.g. a table or column name.
   * @return a double quoted relation.
   */
  private String quote(String relation) {
    return sqlBuilder.quote(relation);
  }
}
