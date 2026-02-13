/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_DENOMINATOR_PROPERTIES_COUNT;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getRoundedValue;
import static org.hisp.dhis.system.util.MathUtils.getRounded;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.db.sql.AnalyticsSqlBuilder;
import org.hisp.dhis.program.ProgramIndicator;
import org.springframework.jdbc.support.rowset.SqlRowSet;

/**
 * Builds a single row of aggregated event data from a SQL result set.
 *
 * <p>This class encapsulates the logic for extracting and formatting values from a {@link
 * SqlRowSet} based on {@link EventQueryParams}. It handles different value types including dates,
 * text, and numeric values.
 */
class AggregatedRowBuilder {

  private static final String COL_VALUE = "value";

  private final EventQueryParams params;
  private final SqlRowSet rowSet;
  private final AnalyticsSqlBuilder sqlBuilder;
  private final BiFunction<QueryItem, EventQueryParams, ColumnAndAlias> columnAliasResolver;
  private final Function<EventQueryParams, String> itemIdProvider;
  private final List<Object> row;

  private AggregatedRowBuilder(
      EventQueryParams params,
      SqlRowSet rowSet,
      AnalyticsSqlBuilder sqlBuilder,
      BiFunction<QueryItem, EventQueryParams, ColumnAndAlias> columnAliasResolver,
      Function<EventQueryParams, String> itemIdProvider) {
    this.params = params;
    this.rowSet = rowSet;
    this.sqlBuilder = sqlBuilder;
    this.columnAliasResolver = columnAliasResolver;
    this.itemIdProvider = itemIdProvider;
    this.row = new ArrayList<>();
  }

  /**
   * Creates a new builder instance.
   *
   * @param params the event query parameters
   * @param rowSet the SQL row set positioned at the current row
   * @param sqlBuilder the SQL builder for database-specific operations
   * @param columnAliasResolver function to resolve column aliases for query items
   * @param itemIdProvider function to get the item identifier
   * @return a new AggregatedRowBuilder instance
   */
  static AggregatedRowBuilder create(
      EventQueryParams params,
      SqlRowSet rowSet,
      AnalyticsSqlBuilder sqlBuilder,
      BiFunction<QueryItem, EventQueryParams, ColumnAndAlias> columnAliasResolver,
      Function<EventQueryParams, String> itemIdProvider) {
    return new AggregatedRowBuilder(
        params, rowSet, sqlBuilder, columnAliasResolver, itemIdProvider);
  }

  /**
   * Builds the complete row by adding all required data.
   *
   * @return the list of row values
   */
  List<Object> build() {
    addItemOrAggregateData();
    addDimensionData();
    addValueData();
    addNumDenPlaceholders();
    return row;
  }

  /**
   * Adds item identifiers or query item values to the row based on whether aggregate data is
   * requested. For aggregate data, adds either the value dimension item ID or program indicator
   * UID. For non-aggregate data, adds extracted values for each query item.
   */
  private void addItemOrAggregateData() {
    if (params.isAggregateData()) {
      if (params.hasValueDimension()) {
        row.add(itemIdProvider.apply(params));
      } else if (params.hasProgramIndicatorDimension()) {
        row.add(params.getProgramIndicator().getUid());
      }
    } else {
      for (QueryItem queryItem : params.getItems()) {
        addQueryItemValue(queryItem);
      }
    }
  }

  /**
   * Extracts and adds a single query item value to the row. Resolves the column alias, extracts the
   * value based on the item's value type, and applies output scheme transformations if needed.
   *
   * @param queryItem the query item to extract value for
   */
  private void addQueryItemValue(QueryItem queryItem) {
    ColumnAndAlias columnAndAlias = columnAliasResolver.apply(queryItem, params);
    String alias = columnAndAlias.getAlias();

    if (isEmpty(alias)) {
      alias =
          queryItem.getItemName()
              + (columnAndAlias.hasPostfix() ? columnAndAlias.getPostfix() : "");
    }

    String itemName = extractStringValue(alias, queryItem.getValueType());
    String itemValue =
        params.isCollapseDataDimensions()
            ? QueryItemHelper.getCollapsedDataItemValue(queryItem, itemName)
            : itemName;

    if (params.getOutputIdScheme() == null || params.getOutputIdScheme() == IdScheme.NAME) {
      row.add(itemValue);
    } else {
      row.add(resolveOutputValue(itemValue));
    }
  }

  /**
   * Resolves the output value by checking for option set or legend mappings. Returns the mapped
   * value if found, otherwise returns the original value.
   *
   * @param itemValue the raw item value to resolve
   * @return the resolved value (option value, legend value, or original)
   */
  private String resolveOutputValue(String itemValue) {
    String itemOptionValue = QueryItemHelper.getItemOptionValue(itemValue, params);

    if (itemOptionValue != null && !itemOptionValue.trim().isEmpty()) {
      return itemOptionValue;
    }

    String legendItemValue = QueryItemHelper.getItemLegendValue(itemValue, params);

    if (legendItemValue != null && !legendItemValue.trim().isEmpty()) {
      return legendItemValue;
    }

    return itemValue;
  }

  /**
   * Adds dimension values to the row for all dimensions in the query parameters. Extracts each
   * dimension value using the dimension name as the column identifier.
   */
  private void addDimensionData() {
    for (DimensionalObject dimension : params.getDimensions()) {
      String dimensionValue =
          extractStringValue(dimension.getDimensionName(), dimension.getValueType());
      row.add(dimensionValue);
    }

    if (params.hasEnrollmentOuDimension()) {
      row.add(extractStringValue("enrollmentou", ValueType.TEXT));
    }
  }

  /**
   * Adds the aggregated value to the row. Delegates to the appropriate handler based on whether the
   * query has a value dimension, program indicator, or is a simple count aggregation.
   */
  private void addValueData() {
    if (params.hasValueDimension()) {
      addValueDimensionData();
    } else if (params.hasProgramIndicatorDimension()) {
      addProgramIndicatorData();
    } else {
      row.add(rowSet.getInt(COL_VALUE));
    }
  }

  /**
   * Adds the value dimension data to the row, handling different value types appropriately. Text
   * values are added as strings, date values are rendered as timestamps, and numeric values are
   * optionally rounded based on query parameters.
   */
  private void addValueDimensionData() {
    if (params.hasTextValueDimension()) {
      row.add(rowSet.getString(COL_VALUE));
    } else if (params.hasDateValueDimension()) {
      row.add(sqlBuilder.renderTimestamp(rowSet.getString(COL_VALUE)));
    } else {
      double value = rowSet.getDouble(COL_VALUE);
      row.add(params.isSkipRounding() ? value : getRounded(value));
    }
  }

  /**
   * Adds program indicator value to the row. Applies rounding based on the indicator's configured
   * decimal places and query parameters.
   */
  private void addProgramIndicatorData() {
    double value = rowSet.getDouble(COL_VALUE);
    ProgramIndicator indicator = params.getProgramIndicator();
    row.add(getRoundedValue(params, indicator.getDecimals(), value));
  }

  /**
   * Adds null placeholders for numerator and denominator properties when includeNumDen is enabled.
   * These placeholders maintain grid column alignment for indicator calculations.
   */
  private void addNumDenPlaceholders() {
    if (params.isIncludeNumDen()) {
      for (int i = 0; i < NUMERATOR_DENOMINATOR_PROPERTIES_COUNT; i++) {
        row.add(null);
      }
    }
  }

  /**
   * Extracts a string value from the row set, applying timestamp rendering for date types.
   *
   * @param column the column name
   * @param valueType the value type of the column
   * @return the extracted and potentially formatted value
   */
  private String extractStringValue(String column, ValueType valueType) {
    String value = rowSet.getString(column);
    if (valueType == ValueType.DATETIME || valueType == ValueType.DATE) {
      return sqlBuilder.renderTimestamp(value);
    }
    return value;
  }
}
