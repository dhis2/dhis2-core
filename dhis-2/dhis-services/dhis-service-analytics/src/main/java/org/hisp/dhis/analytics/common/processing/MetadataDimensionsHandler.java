/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.Collections.emptyList;
import static org.hisp.dhis.analytics.common.processing.MetadataParamsHandler.getItemUid;
import static org.hisp.dhis.analytics.event.data.QueryItemHelper.getItemOptions;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.analytics.common.params.CommonParamsDelegator;
import org.hisp.dhis.analytics.common.params.CommonParsedParams;
import org.hisp.dhis.analytics.data.handler.MetadataHandler;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.period.PeriodType;

/**
 * This class handles the logic necessary to generate the metadata dimensions.
 *
 * <p>It builds all data structure and maps required by the metadata object. It works on top of
 * common objects, so it can be reused by different analytics services/endpoints.
 *
 * <p>This class and methods were pulled from other part of the code, so we could have a centralized
 * way to generate and keep the logic related to analytics metadata elements. Light changes were
 * applied to make the code slightly cleaner. Major structural changes were not applied to reduce
 * the risk of bugs.
 */
public class MetadataDimensionsHandler {
  /**
   * Handles all required logic/rules in order to return a map of metadata item identifiers.
   *
   * @param grid the {@link Grid}.
   * @param commonParsed the {@link CommonParsedParams}.
   * @return the map of {@link MetadataItem}.
   */
  Map<String, List<String>> handle(Grid grid, CommonParsedParams commonParsed) {
    CommonParamsDelegator delegator = commonParsed.delegate();
    List<QueryItem> items = delegator.getAllItems();
    List<DimensionalObject> allDimensionalObjects =
        delegator.getAllDimensionalObjects().stream()
            // We're adding periods separately, so we need to filter them out.
            .filter(MetadataDimensionsHandler::isNotPeriod)
            .toList();
    List<DimensionalItemObject> periodDimensionOrFilterItems =
        delegator.getPeriodDimensionOrFilterItems();
    List<QueryItem> itemFilters = delegator.getItemsAsFilters();

    Map<String, List<String>> dimensionItems = new HashMap<>();
    dimensionItems.put(PERIOD_DIM_ID, getDistinctPeriodUids(periodDimensionOrFilterItems));

    for (DimensionalObject dim : allDimensionalObjects) {
      dimensionItems.put(dim.getDimension(), getDimensionalItemIds(dim.getItems()));
    }

    putQueryItemsIntoMap(dimensionItems, items, grid);
    putFilterItemsIntoMap(dimensionItems, itemFilters);

    return dimensionItems;
  }

  private static boolean isNotPeriod(DimensionalObject dimensionalObject) {
    return !dimensionalObject.getDimensionType().equals(DimensionType.PERIOD);
  }

  /**
   * Returns a list of distinct period UIDs. This method was extracted from the original code to be
   * used in the {@link MetadataDimensionsHandler} and {@link MetadataHandler} classes.
   *
   * @param items the list of {@link DimensionalItemObject} of type period.
   * @return a list of distinct period UIDs.
   */
  public static List<String> getDistinctPeriodUids(List<DimensionalItemObject> items) {
    Calendar calendar = PeriodType.getCalendar();
    List<String> periodUids =
        calendar.isIso8601() ? getUids(items) : getLocalPeriodIdentifiers(items, calendar);
    return periodUids.stream().distinct().toList();
  }

  /**
   * Adds the list of given {@link QueryItem} into the given dimension items map.
   *
   * @param dimensionItems the map of items uid.
   * @param filterItems the list of {@link QueryItem}.
   */
  private void putFilterItemsIntoMap(
      Map<String, List<String>> dimensionItems, List<QueryItem> filterItems) {
    for (QueryItem item : filterItems) {
      if (item.hasOptionSet()) {
        dimensionItems.put(item.getItemId(), item.getOptionSetFilterItemsOrAll());
      } else if (item.hasLegendSet()) {
        dimensionItems.put(item.getItemId(), item.getLegendSetFilterItemsOrAll());
      } else {
        dimensionItems.put(
            item.getItemId(),
            item.getFiltersAsString() != null ? List.of(item.getFiltersAsString()) : emptyList());
      }
    }
  }

  /**
   * Adds the list of given {@link QueryItem} into the given dimension items map. It also takes into
   * consideration elements present in the {@link Grid}.
   *
   * @param dimensionItems the map of items uid.
   * @param items the list of {@link QueryItem}.
   * @param grid the {@link Grid}.
   */
  private void putQueryItemsIntoMap(
      Map<String, List<String>> dimensionItems, List<QueryItem> items, Grid grid) {
    Map<String, List<Option>> optionsPresentInGrid = getItemOptions(grid, items);

    for (QueryItem item : items) {
      String itemUid = getItemUid(item);

      if (item.hasOptionSet()) {
        // Query items can't have both legends and options.
        dimensionItems.put(
            itemUid,
            getDimensionItemUids(
                optionsPresentInGrid.get(itemUid), item.getOptionSetFilterItemsOrAll()));
      } else if (item.hasLegendSet()) {
        dimensionItems.put(itemUid, item.getLegendSetFilterItemsOrAll());
      } else {
        dimensionItems.put(itemUid, List.of());
      }
    }
  }

  /**
   * Based on the given arguments, this method will extract a list of UIDs of {@link Option}. If
   * itemOptions is null, it returns the default list of UIDs (defaultOptionUids). Otherwise, it
   * will return the list of UIDs from itemOptions.
   *
   * @param itemOptions a list of {@link Option} objects.
   * @param defaultOptionUids a list of default {@link Option} UIDs.
   * @return a list of UIDs.
   */
  private List<String> getDimensionItemUids(
      List<Option> itemOptions, List<String> defaultOptionUids) {
    List<String> dimensionUids = new ArrayList<>();

    if (itemOptions == null) {
      dimensionUids.addAll(defaultOptionUids);
    } else {
      dimensionUids.addAll(getUids(itemOptions));
    }

    return dimensionUids;
  }
}
