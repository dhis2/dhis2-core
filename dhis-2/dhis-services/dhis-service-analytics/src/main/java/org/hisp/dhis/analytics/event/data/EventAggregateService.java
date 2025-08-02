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
package org.hisp.dhis.analytics.event.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.DataQueryParams.DENOMINATOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.DENOMINATOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DIVISOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.DIVISOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.FACTOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.FACTOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.MULTIPLIER_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.MULTIPLIER_ID;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.event.EventAnalyticsUtils.addValues;
import static org.hisp.dhis.analytics.event.EventAnalyticsUtils.generateEventDataPermutations;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.UNLIMITED_PAGING;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.addPaging;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.getDimensionsKeywords;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.isTableLayout;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.throwIllegalQueryEx;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_COLLAPSED_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.IdentifiableObjectUtils.join;
import static org.hisp.dhis.common.ValueType.BOOLEAN;
import static org.hisp.dhis.common.ValueType.NUMBER;
import static org.hisp.dhis.common.ValueType.TEXT;
import static org.hisp.dhis.commons.collection.ListUtils.removeEmptys;
import static org.hisp.dhis.feedback.ErrorCode.E7128;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.EventAnalyticsDimensionalItem;
import org.hisp.dhis.analytics.cache.AnalyticsCache;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.tracker.MetadataItemsHandler;
import org.hisp.dhis.analytics.tracker.SchemeIdHandler;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Service;

/** This service is responsible for retrieving aggregated event data. */
@Service
@RequiredArgsConstructor
public class EventAggregateService {

  private static final String DASH_PRETTY_SEPARATOR = " - ";

  private static final String SPACE = " ";

  private static final String TOTAL_COLUMN_PRETTY_NAME = "Total";

  private static final Map<String, String> COLUMN_NAMES =
      Map.of(
          DATA_X_DIM_ID, "data",
          CATEGORYOPTIONCOMBO_DIM_ID, "categoryoptioncombo",
          PERIOD_DIM_ID, "period",
          ORGUNIT_DIM_ID, "organisationunit");

  private static final Option OPT_TRUE = new Option("Yes", "1");

  private static final Option OPT_FALSE = new Option("No", "0");

  private final DataElementService dataElementService;

  private final TrackedEntityAttributeService trackedEntityAttributeService;

  private final EventAnalyticsManager eventAnalyticsManager;

  private final EnrollmentAnalyticsManager enrollmentAnalyticsManager;

  private final EventDataQueryService eventDataQueryService;

  private final EventQueryPlanner queryPlanner;

  private final AnalyticsCache analyticsCache;

  private final AnalyticsSecurityManager securityManager;

  private final EventQueryValidator queryValidator;

  private final MetadataItemsHandler metadataHandler;

  private final SchemeIdHandler schemeIdHandler;

  /**
   * Generates an aggregated for the given query. The grid will represent a table with dimensions
   * used as columns and rows as specified in columns and rows dimension arguments. If columns and
   * rows are null or empty, the normalized table will be returned.
   *
   * <p>If metadata is included in the query, the metadata map of the grid will contain keys
   * described in {@link AnalyticsMetaDataKey}.
   *
   * @param params the {@link EventQueryParams}.
   * @param columns the identifiers of the dimensions to use as columns.
   * @param rows the identifiers of the dimensions to use as rows.
   * @return aggregated data as a {@link Grid} object.
   */
  public Grid getAggregatedData(EventQueryParams params, List<String> columns, List<String> rows) {
    return isTableLayout(columns, rows)
        ? getAggregatedDataTableLayout(params, columns, rows)
        : getAggregatedData(params);
  }

  /**
   * Generates aggregated event data for the given analytical object.
   *
   * @param object the {@link EventAnalyticalObject}.
   * @return aggregated event data as a {@link Grid} object.
   */
  public Grid getAggregatedData(EventAnalyticalObject object) {
    EventQueryParams params = eventDataQueryService.getFromAnalyticalObject(object);

    return getAggregatedData(params);
  }

  /**
   * Generates aggregated event data for the given query, along with required validation.
   *
   * @param params the {@link EventQueryParams}.
   * @return aggregated event data as a {@link Grid} object.
   */
  public Grid getAggregatedData(EventQueryParams params) {
    securityManager.decideAccessEventQuery(params);
    params = securityManager.withUserConstraints(params);

    queryValidator.validate(params);

    if (analyticsCache.isEnabled() && !params.analyzeOnly()) {
      EventQueryParams immutableParams = new EventQueryParams.Builder(params).build();
      return analyticsCache.getOrFetch(params, p -> getAggregatedDataGrid(immutableParams));
    }

    return getAggregatedDataGrid(params);
  }

  /**
   * Fetches aggregated event data for the given query and creates a grid containing headers and
   * metadata if applicable.
   *
   * @param params the {@link EventQueryParams}.
   * @return aggregated event data as a {@link Grid} object.
   */
  private Grid getAggregatedDataGrid(EventQueryParams params) {
    params.removeProgramIndicatorItems();

    Grid grid = new ListGrid();
    int maxLimit = queryValidator.getMaxLimit();
    List<Keyword> keywords = getDimensionsKeywords(params);

    if (!params.isSkipData() || params.analyzeOnly()) {
      addHeaders(params, grid);
      addData(grid, params, maxLimit);

      // Sort grid, done again due to potential multiple partitions
      if (params.hasSortOrder() && grid.getHeight() > 0) {
        grid.sortGrid(1, params.getSortOrderAsInt());
      }

      // Limit grid
      if (params.hasLimit() && grid.getHeight() > params.getLimit()) {
        grid.limitGrid(params.getLimit());
      }
    }

    addPaging(params, UNLIMITED_PAGING, grid);
    schemeIdHandler.applyScheme(grid, params);
    metadataHandler.addMetadata(grid, params, keywords);

    return grid;
  }

  /**
   * Adds data into the given grid, based on the given params.
   *
   * @param grid {@link Grid}.
   * @param params the {@link EventQueryParams}. @@param maxLimit the max number of records to
   *     retrieve.
   */
  private void addData(Grid grid, EventQueryParams params, int maxLimit) {
    Timer timer = new Timer().start().disablePrint();

    List<EventQueryParams> queries = queryPlanner.planAggregateQuery(params);

    timer.getSplitTime("Planned event query, got partitions: {}", params.getPartitions());

    for (EventQueryParams query : queries) {
      if (query.hasEnrollmentProgramIndicatorDimension()) {
        enrollmentAnalyticsManager.getAggregatedEventData(query, grid, maxLimit);
      } else {
        eventAnalyticsManager.getAggregatedEventData(query, grid, maxLimit);
      }
    }

    timer.getTime("Got aggregated events");

    if (maxLimit > 0 && grid.getHeight() > maxLimit) {
      throwIllegalQueryEx(E7128, maxLimit);
    }
  }

  /**
   * Add headers into the given {@link Grid}.
   *
   * @param params the {@link EventQueryParams}.
   * @param grid the {@link Grid} to add headers to.
   */
  private void addHeaders(EventQueryParams params, Grid grid) {
    if (params.isCollapseDataDimensions() || params.isAggregateData()) {
      grid.addHeader(new GridHeader(DATA_COLLAPSED_DIM_ID, DISPLAY_NAME_DATA_X, TEXT, false, true));
    } else {
      for (QueryItem item : params.getItems()) {
        String name = item.getItem().getUid();
        // attach program stage uid to the header name if it is a program stage item
        if (item.hasProgramStage()) {
          name = item.getProgramStage().getUid() + "." + name;
        }

        String displayProperty = item.getItem().getDisplayProperty(params.getDisplayProperty());

        grid.addHeader(
            new GridHeader(
                name,
                displayProperty,
                item.getValueType(),
                false,
                true,
                item.getOptionSet(),
                item.getLegendSet()));
      }
    }

    for (DimensionalObject dimension : params.getDimensions()) {
      String displayProperty = dimension.getDisplayProperty(params.getDisplayProperty());

      grid.addHeader(new GridHeader(dimension.getDimension(), displayProperty, TEXT, false, true));
    }

    grid.addHeader(new GridHeader(VALUE_ID, VALUE_HEADER_NAME, NUMBER, false, false));

    if (params.isIncludeNumDen()) {
      grid.addHeader(new GridHeader(NUMERATOR_ID, NUMERATOR_HEADER_NAME, NUMBER, false, false))
          .addHeader(new GridHeader(DENOMINATOR_ID, DENOMINATOR_HEADER_NAME, NUMBER, false, false))
          .addHeader(new GridHeader(FACTOR_ID, FACTOR_HEADER_NAME, NUMBER, false, false))
          .addHeader(new GridHeader(MULTIPLIER_ID, MULTIPLIER_HEADER_NAME, NUMBER, false, false))
          .addHeader(new GridHeader(DIVISOR_ID, DIVISOR_HEADER_NAME, NUMBER, false, false));
    }
  }

  /**
   * Puts elements into the mapping table. The elements are fetched from the query parameters.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @param table the map to add elements to.
   * @param dimension the dimension identifier.
   */
  private void addEventDataObjects(
      Grid grid,
      EventQueryParams params,
      Map<String, List<EventAnalyticsDimensionalItem>> table,
      String dimension) {
    List<EventAnalyticsDimensionalItem> objects =
        params.getEventReportDimensionalItemArrayExploded(dimension);

    if (objects.isEmpty()) {
      ValueTypedDimensionalItemObject eventDimensionalItemObject =
          dataElementService.getDataElement(dimension);

      if (eventDimensionalItemObject == null) {
        eventDimensionalItemObject =
            trackedEntityAttributeService.getTrackedEntityAttribute(dimension);
      }

      addEventReportDimensionalItems(eventDimensionalItemObject, objects, grid, dimension);

      table.put(
          eventDimensionalItemObject.getDisplayProperty(params.getDisplayProperty()), objects);
    } else {
      table.put(dimension, objects);
    }
  }

  /**
   * Adds dimensional items to the given list of objects. Send in a list of {@link
   * EventAnalyticsDimensionalItem} and add properties from {@link ValueTypedDimensionalItemObject}
   * parameter.
   *
   * @param eventDimensionalItemObject the {@link ValueTypedDimensionalItemObject} object to get
   *     properties from.
   * @param dimensionalItems the list of {@link EventAnalyticsDimensionalItem} objects.
   * @param grid the {@link Grid} from the event analytics request.
   * @param dimension the dimension identifier.
   */
  @SuppressWarnings("unchecked")
  private void addEventReportDimensionalItems(
      ValueTypedDimensionalItemObject eventDimensionalItemObject,
      List<EventAnalyticsDimensionalItem> dimensionalItems,
      Grid grid,
      String dimension) {
    checkNotNull(
        eventDimensionalItemObject, String.format("Data dimension '%s' is invalid", dimension));

    String parentUid = eventDimensionalItemObject.getUid();

    if (eventDimensionalItemObject.getValueType() == BOOLEAN) {
      dimensionalItems.add(new EventAnalyticsDimensionalItem(OPT_TRUE, parentUid));
      dimensionalItems.add(new EventAnalyticsDimensionalItem(OPT_FALSE, parentUid));
    }

    if (eventDimensionalItemObject.hasOptionSet()) {
      for (Option option : eventDimensionalItemObject.getOptionSet().getOptions()) {
        dimensionalItems.add(new EventAnalyticsDimensionalItem(option, parentUid));
      }
    } else if (eventDimensionalItemObject.hasLegendSet()) {
      List<String> legendOptions =
          (List<String>)
              ((Map<String, Object>) grid.getMetaData().get(DIMENSIONS.getKey())).get(dimension);

      if (legendOptions.isEmpty()) {
        List<Legend> legends = eventDimensionalItemObject.getLegendSet().getSortedLegends();
        addLegends(dimensionalItems, parentUid, legends);
      } else {
        addLegendOptions(dimensionalItems, grid, parentUid, legendOptions);
      }
    }
  }

  /**
   * Adds the given legends into the list of dimensionalItems.
   *
   * @param dimensionalItems
   * @param parentUid
   * @param legends
   */
  private static void addLegends(
      List<EventAnalyticsDimensionalItem> dimensionalItems,
      String parentUid,
      List<Legend> legends) {
    for (Legend legend : legends) {
      for (int i = legend.getStartValue().intValue(); i < legend.getEndValue().intValue(); i++) {
        dimensionalItems.add(
            new EventAnalyticsDimensionalItem(
                new Option(String.valueOf(i), String.valueOf(i)), parentUid));
      }
    }
  }

  /**
   * Adds the given legendOptions into the list of dimensionalItems.
   *
   * @param dimensionalItems
   * @param grid
   * @param parentUid
   * @param legendOptions
   */
  @SuppressWarnings("unchecked")
  private static void addLegendOptions(
      List<EventAnalyticsDimensionalItem> dimensionalItems,
      Grid grid,
      String parentUid,
      List<String> legendOptions) {
    for (String legend : legendOptions) {
      MetadataItem metadataItem =
          (MetadataItem) ((Map<String, Object>) grid.getMetaData().get(ITEMS.getKey())).get(legend);

      dimensionalItems.add(
          new EventAnalyticsDimensionalItem(new Option(metadataItem.getName(), legend), parentUid));
    }
  }

  /**
   * Creates a grid with table layout for downloading event reports. The grid is dynamically made
   * from rows and columns input, which refers to the dimensions requested.
   *
   * <p>For event reports each option for a dimension will be an {@link
   * EventAnalyticsDimensionalItem} and all permutations will be added to the grid.
   *
   * @param params the {@link EventQueryParams}.
   * @param columns the identifiers of the dimensions to use as columns.
   * @param rows the identifiers of the dimensions to use as rows.
   * @return aggregated data as a Grid object.
   */
  private Grid getAggregatedDataTableLayout(
      EventQueryParams params, List<String> columns, List<String> rows) {
    params.removeProgramIndicatorItems();

    Grid grid = getAggregatedData(params);

    removeEmptys(columns);
    removeEmptys(rows);

    Map<String, List<EventAnalyticsDimensionalItem>> tableColumns = new LinkedHashMap<>();

    if (columns != null) {
      for (String dimension : columns) {
        addEventDataObjects(grid, params, tableColumns, dimension);
      }
    }

    Map<String, List<EventAnalyticsDimensionalItem>> tableRows = new LinkedHashMap<>();
    List<String> rowDimensions = new ArrayList<>();

    if (rows != null) {
      for (String dimension : rows) {
        rowDimensions.add(dimension);
        addEventDataObjects(grid, params, tableRows, dimension);
      }
    }

    List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations =
        generateEventDataPermutations(tableRows);

    List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations =
        generateEventDataPermutations(tableColumns);

    return generateOutputGrid(grid, params, rowPermutations, columnPermutations, rowDimensions);
  }

  /**
   * Generates an output grid for event analytics download based on input parameters.
   *
   * @param grid the result grid.
   * @param params the {@link EventQueryParams}.
   * @param rowPermutations the row permutations
   * @param columnPermutations the column permutations.
   * @param rowDimensions the row dimensions.
   * @return grid with table layout.
   */
  @SuppressWarnings("unchecked")
  private Grid generateOutputGrid(
      Grid grid,
      EventQueryParams params,
      List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations,
      List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations,
      List<String> rowDimensions) {
    Grid outputGrid = new ListGrid();
    outputGrid.setTitle(join(params.getFilterItems()));

    for (String row : rowDimensions) {
      MetadataItem metadataItem =
          (MetadataItem) ((Map<String, Object>) grid.getMetaData().get(ITEMS.getKey())).get(row);

      String name = defaultIfEmpty(metadataItem.getName(), row);
      String col = defaultIfEmpty(COLUMN_NAMES.get(row), row);

      outputGrid.addHeader(new GridHeader(name, col, TEXT, false, true));
    }

    columnPermutations.forEach(
        permutation -> {
          StringBuilder builder = new StringBuilder();

          permutation.forEach(
              (key, value) -> {
                if (!key.equals(ORGUNIT_DIM_ID) && !key.equals(PERIOD_DIM_ID)) {
                  builder.append(key).append(SPACE);
                }
                builder
                    .append(value.getDisplayProperty(params.getDisplayProperty()))
                    .append(DASH_PRETTY_SEPARATOR);
              });

          String display =
              builder.length() > 0
                  ? builder.substring(0, builder.lastIndexOf(DASH_PRETTY_SEPARATOR))
                  : TOTAL_COLUMN_PRETTY_NAME;

          outputGrid.addHeader(new GridHeader(display, display, NUMBER, false, false));
        });

    for (Map<String, EventAnalyticsDimensionalItem> rowCombination : rowPermutations) {
      outputGrid.addRow();
      List<List<String>> ids = new ArrayList<>();
      Map<String, EventAnalyticsDimensionalItem> displayObjects = new HashMap<>();

      boolean fillDisplayList = true;

      for (Map<String, EventAnalyticsDimensionalItem> columnCombination : columnPermutations) {
        List<String> idList = new ArrayList<>();

        boolean finalFillDisplayList = fillDisplayList;
        rowCombination.forEach(
            (key, value) -> {
              idList.add(value.toString());

              if (finalFillDisplayList) {
                displayObjects.put(value.getParentUid(), value);
              }
            });

        columnCombination.forEach((key, value) -> idList.add(value.toString()));

        ids.add(idList);
        fillDisplayList = false;
      }

      addValuesInOutputGrid(rowDimensions, outputGrid, displayObjects, params);
      addValues(ids, grid, outputGrid);
    }

    return getGridWithRows(grid, outputGrid);
  }

  /**
   * Returns a valid grid.
   *
   * @param grid the {@link Grid}.
   * @param outputGrid the output {@link Grid}.
   */
  private static Grid getGridWithRows(Grid grid, Grid outputGrid) {
    return outputGrid.getRows().isEmpty() ? grid : outputGrid;
  }

  /**
   * Adds values to the given output grid. Display objects are not empty if columns and rows are not
   * empty.
   *
   * @param rowDimensions the list of row dimensions.
   * @param grid the {@link Grid}.
   * @param displayObjects the map of display objects.
   * @param params the {@link EventQueryParams}.
   */
  private static void addValuesInOutputGrid(
      List<String> rowDimensions,
      Grid grid,
      Map<String, EventAnalyticsDimensionalItem> displayObjects,
      EventQueryParams params) {
    if (!displayObjects.isEmpty()) {
      rowDimensions.forEach(
          dimension ->
              grid.addValue(
                  displayObjects.get(dimension).getDisplayProperty(params.getDisplayProperty())));
    }
  }
}
