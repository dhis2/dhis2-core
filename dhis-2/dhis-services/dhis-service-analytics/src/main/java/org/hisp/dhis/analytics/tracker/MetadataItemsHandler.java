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
package org.hisp.dhis.analytics.tracker;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.event.data.QueryItemHelper.getItemOptions;
import static org.hisp.dhis.analytics.event.data.QueryItemHelper.getItemOptionsAsFilter;
import static org.hisp.dhis.analytics.tracker.ResponseHelper.getItemUid;
import static org.hisp.dhis.analytics.util.AnalyticsOrganisationUnitUtils.getUserOrganisationUnitItems;
import static org.hisp.dhis.common.DimensionalObject.OPTION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.common.ValueType.ORGANISATION_UNIT;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.data.OrganisationUnitResolver;
import org.hisp.dhis.analytics.orgunit.OrgUnitHelper;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.MetadataOptionSet;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetadataItemsHandler {
  private final AnalyticsSecurityManager securityManager;

  private final UserService userService;

  private final OrganisationUnitResolver organisationUnitResolver;

  /**
   * Adds meta data values to the given grid based on the given data query parameters.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @param keywords the list of {@link Keyword}.
   */
  public void addMetadata(Grid grid, EventQueryParams params, List<Keyword> keywords) {
    if (!params.isSkipMeta()) {
      Map<String, Object> metadata = new HashMap<>();
      Map<String, List<Option>> optionsPresentInGrid = getItemOptions(grid, params.getItems());
      Set<Option> optionItems = new LinkedHashSet<>();
      boolean hasResults = isNotEmpty(grid.getRows());

      if (hasResults) {
        optionItems.addAll(
            optionsPresentInGrid.values().stream().flatMap(Collection::stream).distinct().toList());
      } else {
        optionItems.addAll(getItemOptionsAsFilter(params.getItemOptions(), params.getItems()));
      }

      Map<String, Object> items = new HashMap<>();
      getUserOrganisationUnitItems(
              userService.getUserByUsername(CurrentUserUtil.getCurrentUsername()),
              params.getUserOrganisationUnitsCriteria())
          .forEach(items::putAll);

      if (params.isComingFromQuery()) {
        items.putAll(getMetadataItems(params, keywords, optionItems, grid));
      } else {
        items.putAll(getMetadataItems(params));
      }

      metadata.put(ITEMS.getKey(), items);

      if (params.isComingFromQuery()) {
        metadata.put(
            DIMENSIONS.getKey(), getDimensionItems(params, Optional.of(optionsPresentInGrid)));
      } else {
        metadata.put(DIMENSIONS.getKey(), getDimensionItems(params, empty()));
      }

      maybeAddOrgUnitHierarchyInfo(params, metadata, grid);

      grid.setMetaData(metadata);
    }
  }

  /**
   * Adds the given item to the given metadata item map.
   *
   * @param metadataItemMap the metadata item map.
   * @param item the {@link QueryItem}.
   * @param includeDetails whether to include metadata details.
   * @param displayProperty the {@link DisplayProperty}.
   */
  private void addItemToMetadata(
      Map<String, MetadataItem> metadataItemMap,
      QueryItem item,
      boolean includeDetails,
      DisplayProperty displayProperty) {
    MetadataItem metadataItem =
        new MetadataItem(
            item.getItem().getDisplayProperty(displayProperty),
            includeDetails ? item.getItem() : null);

    metadataItemMap.put(getItemIdWithProgramStageIdPrefix(item), metadataItem);

    // Done for backwards compatibility.
    metadataItemMap.put(item.getItemId(), metadataItem);
  }

  /**
   * Adds the given metadata items.
   *
   * @param metadataItemMap the metadata item map.
   * @param params the {@link EventQueryParams}.
   * @param itemOptions the list of {@link Option}.
   */
  private void addMetadataItems(
      Map<String, MetadataItem> metadataItemMap, EventQueryParams params, Set<Option> itemOptions) {
    boolean includeDetails = params.isIncludeMetadataDetails();

    itemOptions.forEach(
        option ->
            metadataItemMap.put(
                option.getUid(),
                new MetadataItem(
                    option.getDisplayProperty(params.getDisplayProperty()),
                    includeDetails ? option.getUid() : null,
                    option.getCode())));

    new org.hisp.dhis.analytics.common.processing.MetadataItemsHandler()
        .addOptionsSetIntoMap(metadataItemMap, itemOptions);
  }

  /**
   * Returns a map between dimension identifiers and lists of dimension item identifiers.
   *
   * @param params the {@link EventQueryParams}.
   * @param itemOptions the item options to be added into dimension items.
   * @return a {@link Map} of dimension items.
   */
  private Map<String, List<String>> getDimensionItems(
      EventQueryParams params, Optional<Map<String, List<Option>>> itemOptions) {
    Calendar calendar = PeriodType.getCalendar();

    List<String> periodUids =
        calendar.isIso8601()
            ? getUids(params.getDimensionOrFilterItems(PERIOD_DIM_ID))
            : getLocalPeriodIdentifiers(params.getDimensionOrFilterItems(PERIOD_DIM_ID), calendar);

    Map<String, List<String>> dimensionItems = new HashMap<>();

    dimensionItems.put(PERIOD_DIM_ID, periodUids);

    for (DimensionalObject dim : params.getDimensionsAndFilters()) {
      dimensionItems.put(dim.getDimension(), getDimensionalItemIds(dim.getItems()));
    }

    for (QueryItem item : params.getItems()) {
      String itemUid = getItemUid(item);

      if (item.getValueType().isOrganisationUnit()) {
        List<String> items = organisationUnitResolver.resolveOrgUnis(params, item);
        dimensionItems.put(itemUid, items);
      } else if (item.hasOptionSet()) {
        if (itemOptions.isPresent()) {
          Map<String, List<Option>> itemOptionsMap = itemOptions.get();

          // The call itemOptions.get( itemUid ) can return null.
          // The query item can't have both legends and options.
          dimensionItems.put(
              itemUid,
              getDimensionItemUidsFrom(
                  itemOptionsMap.get(itemUid), item.getOptionSetFilterItemsOrAll()));
        } else {
          dimensionItems.put(item.getItemId(), item.getOptionSetFilterItemsOrAll());
        }
      } else if (item.hasLegendSet()) {
        dimensionItems.put(itemUid, item.getLegendSetFilterItemsOrAll());
      } else {
        dimensionItems.put(itemUid, List.of());
      }
    }

    addItemFiltersToDimensionItems(params.getItemFilters(), dimensionItems);

    return dimensionItems;
  }

  /**
   * Based on the given arguments, this method will extract a list of UIDs of {@link Option}. If
   * itemOptions is null, it returns the default list of UIDs (defaultOptionUids). Otherwise, it
   * will return the list of UIDs from itemOptions.
   *
   * @param itemOptions a list of {@link Option} objects
   * @param defaultOptionUids a list of default {@link Option} UIDs.
   * @return a list of UIDs.
   */
  private List<String> getDimensionItemUidsFrom(
      List<Option> itemOptions, List<String> defaultOptionUids) {
    List<String> dimensionUids = new ArrayList<>();

    if (itemOptions == null) {
      dimensionUids.addAll(defaultOptionUids);
    } else {
      dimensionUids.addAll(IdentifiableObjectUtils.getUids(itemOptions));
    }

    return dimensionUids;
  }

  private static void addItemFiltersToDimensionItems(
      List<QueryItem> itemsFilter, Map<String, List<String>> dimensionItems) {
    for (QueryItem item : itemsFilter) {
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
   * Returns the query item identifier, may have a program stage prefix.
   *
   * @param item {@link QueryItem}.
   */
  private String getItemIdWithProgramStageIdPrefix(QueryItem item) {
    if (item.hasProgramStage()) {
      return item.getProgramStage().getUid() + "." + item.getItemId();
    }

    return item.getItemId();
  }

  /**
   * Depending on the params "hierarchy" metadata boolean flags, this method may append (or not)
   * Org. Unit data into the given metadata map.
   *
   * @param params the {@link EventQueryParams}.
   * @param metadata the metadata map.
   */
  private void maybeAddOrgUnitHierarchyInfo(
      EventQueryParams params, Map<String, Object> metadata, Grid grid) {
    if (params.isHierarchyMeta() || params.isShowHierarchy()) {
      User user = securityManager.getCurrentUser(params);

      List<OrganisationUnit> organisationUnits =
          asTypedList(params.getDimensionOrFilterItems(ORGUNIT_DIM_ID));

      Set<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;

      List<OrganisationUnit> activeOrgUnits =
          OrgUnitHelper.getActiveOrganisationUnits(grid, organisationUnits);

      if (params.isHierarchyMeta()) {
        metadata.put(ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap(activeOrgUnits, roots));
      }

      if (params.isShowHierarchy()) {
        metadata.put(
            ORG_UNIT_NAME_HIERARCHY.getKey(), getParentNameGraphMap(activeOrgUnits, roots, true));
      }
    }
  }

  /**
   * Creates the map of {@link MetadataItem} based on the given params and internal rules.
   *
   * @param params the {@link EventQueryParams}.
   * @return a {@link Map} of metadata item identifiers represented by {@link MetadataItem}.
   */
  private Map<String, MetadataItem> getMetadataItems(EventQueryParams params) {
    Map<String, MetadataItem> metadataItemMap = AnalyticsUtils.getDimensionMetadataItemMap(params);

    boolean includeDetails = params.isIncludeMetadataDetails();

    if (params.hasValueDimension()) {
      DimensionalItemObject value = params.getValue();
      metadataItemMap.put(
          value.getUid(),
          new MetadataItem(
              value.getDisplayProperty(params.getDisplayProperty()),
              includeDetails ? value.getUid() : null,
              value.getCode()));
    }

    params.getItemLegends().stream()
        .filter(Objects::nonNull)
        .forEach(
            legend ->
                metadataItemMap.put(
                    legend.getUid(),
                    new MetadataItem(
                        legend.getDisplayName(),
                        includeDetails ? legend.getUid() : null,
                        legend.getCode())));

    Map<String, List<MetadataItem>> groupedByOptionSet =
        params.getItemOptions().stream()
            .filter(option -> option != null && option.getOptionSet() != null)
            .collect(
                Collectors.groupingBy(
                    option -> option.getOptionSet().getUid(), // Key: OptionSet UID
                    Collectors.mapping(
                        // Create a standard MetadataItem for each option in the list.
                        option -> new MetadataItem(option.getDisplayName(), null, option.getCode()),
                        Collectors.toList())));

    groupedByOptionSet.forEach(
        (optionSetUid, optionList) ->
            metadataItemMap.put(optionSetUid, new MetadataOptionSet(optionList)));

    params.getItemsAndItemFilters().stream()
        .filter(Objects::nonNull)
        .forEach(
            item ->
                metadataItemMap.put(
                    item.getItemId(),
                    new MetadataItem(
                        item.getItem().getDisplayName(), includeDetails ? item.getItem() : null)));

    return metadataItemMap;
  }

  /**
   * Returns a map of metadata item identifiers and {@link MetadataItem}.
   *
   * @param params the {@link EventQueryParams}.
   * @param keywords the dimension keywords.
   * @param itemOptions the set of item {@link Option}.
   * @param grid the grid instance {@link Grid}.
   * @return a map.
   */
  private Map<String, MetadataItem> getMetadataItems(
      EventQueryParams params, List<Keyword> keywords, Set<Option> itemOptions, Grid grid) {
    Map<String, MetadataItem> metadataItemMap =
        AnalyticsUtils.getDimensionMetadataItemMap(params, grid);

    boolean includeDetails = params.isIncludeMetadataDetails();

    if (params.hasValueDimension()) {
      DimensionalItemObject value = params.getValue();
      metadataItemMap.put(
          value.getUid(),
          new MetadataItem(
              value.getDisplayProperty(params.getDisplayProperty()),
              includeDetails ? value.getUid() : null,
              value.getCode()));
    }

    params.getItemLegends().stream()
        .filter(Objects::nonNull)
        .forEach(
            legend ->
                metadataItemMap.put(
                    legend.getUid(),
                    new MetadataItem(
                        legend.getDisplayName(),
                        includeDetails ? legend.getUid() : null,
                        legend.getCode())));

    addMetadataItems(metadataItemMap, params, itemOptions);

    List<QueryItem> itemsAndFilters = params.getItemsAndItemFilters();

    // Add filter params.
    itemsAndFilters.stream()
        .filter(Objects::nonNull)
        .forEach(
            item ->
                addItemToMetadata(
                    metadataItemMap, item, includeDetails, params.getDisplayProperty()));

    // Add filter values (when it's a dimension).
    for (QueryItem item : itemsAndFilters) {
      List<QueryFilter> filters = item.getFilters();
      addOrgUnitDimensionFilter(params, metadataItemMap, includeDetails, item, filters);
    }

    if (isNotEmpty(keywords)) {
      for (Keyword keyword : keywords) {
        if (keyword.getMetadataItem() != null) {
          metadataItemMap.put(
              keyword.getKey(), new MetadataItem(keyword.getMetadataItem().getName()));
        }
      }
    }

    metadataItemMap.putAll(organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(params));

    return metadataItemMap;
  }

  /**
   * Adds, into the metadata, org. units used as filter in data elements of type org. unit.
   *
   * @param params the current {@link EventQueryParams}.
   * @param metadataItemMap the current metadata map.
   * @param includeDetails a boolean flag.
   * @param item the {@link QueryItem}.
   * @param filters the list of {@link QueryFilter}.
   */
  private void addOrgUnitDimensionFilter(
      @Nonnull EventQueryParams params,
      @Nonnull Map<String, MetadataItem> metadataItemMap,
      boolean includeDetails,
      @Nonnull QueryItem item,
      @Nonnull List<QueryFilter> filters) {
    for (QueryFilter filter : filters) {
      String[] filterValues = trimToEmpty(filter.getFilter()).split(OPTION_SEP);
      for (String filterValue : filterValues) {
        boolean isDataElementOfTypeOrgUnit = item.getValueType() == ORGANISATION_UNIT;

        if (isDataElementOfTypeOrgUnit) {
          DimensionalItemObject itemObject =
              organisationUnitResolver.loadOrgUnitDimensionalItem(filterValue, IdScheme.UID);
          if (itemObject != null) {
            addItemToMetadata(
                metadataItemMap,
                new QueryItem(itemObject),
                includeDetails,
                params.getDisplayProperty());
          }
        }
      }
    }
  }
}
