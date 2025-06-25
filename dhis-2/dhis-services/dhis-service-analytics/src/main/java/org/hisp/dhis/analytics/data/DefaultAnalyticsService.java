/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.analytics.data;

import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.analytics.OutputFormat.DATA_VALUE_SET;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getDataValueSet;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getDataValueSetAsGrid;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.isTableLayout;
import static org.hisp.dhis.commons.collection.ListUtils.removeEmptys;
import static org.hisp.dhis.feedback.ErrorCode.E7147;
import static org.hisp.dhis.visualization.Visualization.addListIfEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.data.handler.DataAggregator;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.visualization.Visualization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Lars Helge Overland
 */
@Service("org.hisp.dhis.analytics.AnalyticsService")
@RequiredArgsConstructor
public class DefaultAnalyticsService implements AnalyticsService {

  private final AnalyticsSecurityManager securityManager;

  private final QueryValidator queryValidator;

  private final DataQueryService dataQueryService;

  private final AnalyticsCache analyticsCache;

  private final DataAggregator dataAggregator;

  // -------------------------------------------------------------------------
  // AnalyticsService implementation
  // -------------------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public Grid getAggregatedDataValues(DataQueryParams params) {
    params = checkSecurityConstraints(params);

    queryValidator.validate(params);

    if (analyticsCache.isEnabled() && !params.analyzeOnly()) {
      final DataQueryParams immutableParams = newBuilder(params).build();

      return analyticsCache.getOrFetch(
          params, p -> dataAggregator.getAggregatedDataValueGrid(immutableParams));
    }

    return dataAggregator.getAggregatedDataValueGrid(params);
  }

  @Override
  @Transactional(readOnly = true)
  public Grid getAggregatedDataValues(
      DataQueryParams params, List<String> columns, List<String> rows) {
    return isTableLayout(columns, rows)
        ? getAggregatedDataValuesTableLayout(params, columns, rows)
        : getAggregatedDataValues(params);
  }

  @Override
  @Transactional(readOnly = true)
  public Grid getAggregatedDataValues(AnalyticalObject object) {
    DataQueryParams params = dataQueryService.getFromAnalyticalObject(object);

    return getAggregatedDataValues(params);
  }

  @Override
  @Transactional(readOnly = true)
  public Grid getRawDataValues(DataQueryParams params) {
    params = checkSecurityConstraints(params);

    queryValidator.validate(params);

    return dataAggregator.getRawDataGrid(params);
  }

  @Override
  @Transactional(readOnly = true)
  public DataValueSet getAggregatedDataValueSet(DataQueryParams params) {
    params = checkSecurityConstraints(params);

    Grid grid = getAggregatedDataValueSetGrid(params);

    return getDataValueSet(params, grid);
  }

  @Override
  @Transactional(readOnly = true)
  public Grid getAggregatedDataValueSetAsGrid(DataQueryParams params) {
    params = checkSecurityConstraints(params);

    Grid grid = getAggregatedDataValueSetGrid(params);

    return getDataValueSetAsGrid(grid);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> getAggregatedDataValueMapping(DataQueryParams params) {
    Grid grid = getAggregatedDataValues(newBuilder(params).withIncludeNumDen(false).build());

    return AnalyticsUtils.getAggregatedDataValueMapping(grid);
  }

  @Override
  @Transactional(readOnly = true)
  public Map<String, Object> getAggregatedDataValueMapping(AnalyticalObject object) {
    DataQueryParams params = dataQueryService.getFromAnalyticalObject(object);

    return getAggregatedDataValueMapping(params);
  }

  // -------------------------------------------------------------------------
  // Private business logic methods
  // -------------------------------------------------------------------------

  /**
   * Returns a grid with aggregated data in data value set format.
   *
   * @param params the {@link DataQueryParams}.
   * @return a grid with aggregated data in data value set format.
   */
  private Grid getAggregatedDataValueSetGrid(DataQueryParams params) {
    DataQueryParams query =
        newBuilder(params)
            .withSkipMeta(false)
            .withSkipData(false)
            .withIncludeNumDen(false)
            .withOutputFormat(DATA_VALUE_SET)
            .build();

    return dataAggregator.getAggregatedDataValueGrid(query);
  }

  /**
   * Check the common security constraints that should be applied to the given params. Decide
   * access, add constraints and validate.
   *
   * @param params
   * @return the params after the security constraints appliance.
   */
  private DataQueryParams checkSecurityConstraints(DataQueryParams params) {
    securityManager.decideAccess(params);

    params = securityManager.withDataApprovalConstraints(params);
    params = securityManager.withUserConstraints(params);

    return params;
  }

  /**
   * Returns a Grid with aggregated data in table layout.
   *
   * @param params the {@link DataQueryParams}.
   * @param columns the column dimensions.
   * @param rows the row dimensions.
   * @return a Grid with aggregated data in table layout.
   */
  private Grid getAggregatedDataValuesTableLayout(
      DataQueryParams params, List<String> columns, List<String> rows) {
    params.setOutputIdScheme(null);

    Grid grid = getAggregatedDataValues(params);

    removeEmptys(columns);
    removeEmptys(rows);

    queryValidator.validateTableLayout(params, columns, rows);
    queryValidator.validate(params);

    final Visualization visualization = new Visualization();

    List<List<DimensionalItemObject>> tableColumns = new ArrayList<>();
    List<List<DimensionalItemObject>> tableRows = new ArrayList<>();
    Map<String, List<DimensionalItemObject>> columnsDimensionItemsByDimension = new HashMap<>();
    Map<String, List<DimensionalItemObject>> rowsDimensionItemsByDimension = new HashMap<>();

    if (columns != null) {
      for (String dimension : columns) {
        visualization.addDimensionDescriptor(
            dimension, params.getDimension(dimension).getDimensionType());

        visualization.getColumnDimensions().add(dimension);
        List<DimensionalItemObject> dimensionItemsExplodeCoc =
            params.getDimensionItemsExplodeCoc(dimension);
        columnsDimensionItemsByDimension.put(dimension, dimensionItemsExplodeCoc);
        tableColumns.add(dimensionItemsExplodeCoc);
      }
    }

    if (rows != null) {
      for (String dimension : rows) {
        visualization.addDimensionDescriptor(
            dimension, params.getDimension(dimension).getDimensionType());

        visualization.getRowDimensions().add(dimension);

        List<DimensionalItemObject> dimensionItemsExplodeCoc =
            params.getDimensionItemsExplodeCoc(dimension);
        rowsDimensionItemsByDimension.put(dimension, dimensionItemsExplodeCoc);
        tableRows.add(dimensionItemsExplodeCoc);
      }
    }

    List<List<DimensionalItemObject>> gridColumns =
        Optional.ofNullable(columns)
            .map(cols -> getGridItems(grid, columnsDimensionItemsByDimension, cols))
            .filter(CollectionUtils::isNotEmpty)
            .orElseGet(() -> CombinationGenerator.newInstance(tableColumns).getCombinations());

    List<List<DimensionalItemObject>> gridRows =
        Optional.ofNullable(rows)
            .map(rws -> getGridItems(grid, rowsDimensionItemsByDimension, rws))
            .filter(CollectionUtils::isNotEmpty)
            .orElseGet(() -> CombinationGenerator.newInstance(tableRows).getCombinations());

    visualization
        .setGridTitle(IdentifiableObjectUtils.join(params.getFilterItems()))
        .setGridColumns(gridColumns)
        .setGridRows(gridRows);

    addListIfEmpty(visualization.getGridColumns());
    addListIfEmpty(visualization.getGridRows());

    visualization.setHideEmptyRows(params.isHideEmptyRows());
    visualization.setHideEmptyColumns(params.isHideEmptyColumns());
    visualization.setShowHierarchy(params.isShowHierarchy());

    Map<String, Object> valueMap = AnalyticsUtils.getAggregatedDataValueMapping(grid);

    return visualization.getGrid(
        new ListGrid(grid.getMetaData(), grid.getInternalMetaData()),
        valueMap,
        params.getDisplayProperty(),
        false);
  }

  /**
   * Returns alternative grid dimensional items based on the given grid and all dimension items.
   * Alternative grid items are used improve the performance of the grid rendering when the
   * combination of dimension items is large. The alternative grid items are a list of lists of
   * dimension items where each list represents a possible combination of dimension items that are
   * used to render the grid.
   *
   * @param grid the grid
   * @param dimensionItemsByDimension a map of dimension items by dimension
   * @param dimensionIds the dimension ids
   * @return the alternative grid items
   */
  static List<List<DimensionalItemObject>> getGridItems(
      Grid grid,
      Map<String, List<DimensionalItemObject>> dimensionItemsByDimension,
      List<String> dimensionIds) {
    Set<List<DimensionalItemObject>> alternateItems = new HashSet<>();

    // Last column is the value column.
    int metaCount = grid.getWidth() - 1;

    for (List<Object> row : grid.getRows()) {
      DimensionalItemObject[] alternateItem = new DimensionalItemObject[dimensionIds.size()];

      for (int i = 0; i < metaCount; i++) {
        // Header name is the dimension id.
        String headerName = grid.getHeaders().get(i).getName();
        String value = row.get(i).toString();
        if (isDimension(headerName, dimensionItemsByDimension)) {
          int indexInColumn = dimensionIds.indexOf(headerName);
          alternateItem[indexInColumn] = findValueInDimensionItem(dimensionItemsByDimension, value);
        }
      }
      alternateItems.add(Arrays.stream(alternateItem).collect(Collectors.toList()));
    }
    return new ArrayList<>(alternateItems);
  }

  private static DimensionalItemObject findValueInDimensionItem(
      Map<String, List<DimensionalItemObject>> dimensionItemsByDimension, String value) {
    return dimensionItemsByDimension.values().stream()
        .flatMap(List::stream)
        .filter(dio -> dio.getDimensionItem().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalQueryException(E7147, value));
  }

  private static boolean isDimension(
      String dimensionUid, Map<String, List<DimensionalItemObject>> rowsDimensionItemsByDimension) {
    return rowsDimensionItemsByDimension.containsKey(dimensionUid);
  }
}
