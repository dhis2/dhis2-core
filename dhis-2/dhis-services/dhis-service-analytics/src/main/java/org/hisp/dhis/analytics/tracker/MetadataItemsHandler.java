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
import static org.hisp.dhis.common.DimensionConstants.OPTION_SEP;
import static org.hisp.dhis.common.DimensionConstants.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionConstants.PERIOD_DIM_ID;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
   * Adds meta-data values to the given grid based on the given data query parameters.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @param keywords the list of {@link Keyword}.
   */
  public void addMetadata(Grid grid, EventQueryParams params, List<Keyword> keywords) {
    if (params.isSkipMeta()) {
      return;
    }

    Map<String, Object> metadata =
        MetadataBuilder.builder()
            .put(ITEMS, buildMetadataItems(grid, params, keywords))
            .put(DIMENSIONS, buildDimensionItems(grid, params))
            .putIf(
                ORG_UNIT_HIERARCHY,
                () -> buildOrgUnitHierarchy(grid, params),
                params::isHierarchyMeta)
            .putIf(
                ORG_UNIT_NAME_HIERARCHY,
                () -> buildOrgUnitNameHierarchy(grid, params),
                params::isShowHierarchy)
            .build();

    grid.setMetaData(metadata);
  }

  /**
   * Builds the metadata items map containing all item metadata.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @param keywords the list of {@link Keyword}.
   * @return a map of metadata items.
   */
  private Map<String, Object> buildMetadataItems(
      Grid grid, EventQueryParams params, List<Keyword> keywords) {
    Map<String, Object> items = new HashMap<>();

    addUserOrgUnitItems(items, params);

    if (params.isComingFromQuery()) {
      Set<Option> optionItems = collectOptionItems(grid, params);
      items.putAll(getMetadataItems(params, keywords, optionItems, grid));
    } else {
      items.putAll(getMetadataItems(params, null, null, null));
    }

    return items;
  }

  /**
   * Adds user organisation unit items to the metadata items map.
   *
   * @param items the metadata items map.
   * @param params the {@link EventQueryParams}.
   */
  private void addUserOrgUnitItems(Map<String, Object> items, EventQueryParams params) {
    getUserOrganisationUnitItems(
            userService.getUserByUsername(CurrentUserUtil.getCurrentUsername()),
            params.getUserOrganisationUnitsCriteria())
        .forEach(items::putAll);
  }

  /**
   * Collects option items from the grid based on whether there are results or not.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @return a set of options.
   */
  private Set<Option> collectOptionItems(Grid grid, EventQueryParams params) {
    Set<Option> optionItems = new LinkedHashSet<>();
    Map<String, List<Option>> optionsPresentInGrid = getItemOptions(grid, params.getItems());

    if (isNotEmpty(grid.getRows())) {
      optionItems.addAll(
          optionsPresentInGrid.values().stream().flatMap(Collection::stream).distinct().toList());
    } else {
      optionItems.addAll(getItemOptionsAsFilter(params.getItemOptions(), params.getItems()));
    }

    return optionItems;
  }

  /**
   * Builds the dimension items map.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @return a map of dimension items.
   */
  private Map<String, List<String>> buildDimensionItems(Grid grid, EventQueryParams params) {
    if (params.isComingFromQuery()) {
      Map<String, List<Option>> optionsPresentInGrid = getItemOptions(grid, params.getItems());
      return getDimensionItems(params, Optional.of(optionsPresentInGrid));
    }
    return getDimensionItems(params, empty());
  }

  /**
   * Returns a unified map of metadata item identifiers and {@link MetadataItem}. This method
   * handles both query and non-query scenarios.
   *
   * @param params the {@link EventQueryParams}.
   * @param keywords the dimension keywords (nullable, only used for query requests).
   * @param itemOptions the set of item {@link Option} (nullable, only used for query requests).
   * @param grid the grid instance {@link Grid} (nullable, only used for query requests).
   * @return a map of metadata items.
   */
  private Map<String, MetadataItem> getMetadataItems(
      EventQueryParams params,
      @Nullable List<Keyword> keywords,
      @Nullable Set<Option> itemOptions,
      @Nullable Grid grid) {

    boolean isQueryRequest = grid != null;
    Map<String, MetadataItem> metadataItemMap =
        isQueryRequest
            ? AnalyticsUtils.getDimensionMetadataItemMap(params, grid)
            : AnalyticsUtils.getDimensionMetadataItemMap(params);

    boolean includeDetails = params.isIncludeMetadataDetails();

    addValueDimensionMetadata(metadataItemMap, params, includeDetails, isQueryRequest);
    addLegendMetadata(metadataItemMap, params, includeDetails);

    if (isQueryRequest) {
      addOptionMetadataForQuery(metadataItemMap, params, itemOptions);
      addItemsAndFiltersMetadataForQuery(metadataItemMap, params, includeDetails);
      addKeywordsMetadata(metadataItemMap, keywords);
      metadataItemMap.putAll(
          organisationUnitResolver.getMetadataItemsForOrgUnitDataElements(params));
    } else {
      addOptionMetadataForNonQuery(metadataItemMap, params, includeDetails);
      addItemsAndFiltersMetadataForNonQuery(metadataItemMap, params, includeDetails);
    }

    return metadataItemMap;
  }

  /**
   * Adds value dimension metadata to the map.
   *
   * @param metadataItemMap the metadata item map.
   * @param params the {@link EventQueryParams}.
   * @param includeDetails whether to include metadata details.
   * @param isQueryRequest whether this is a query request.
   */
  private void addValueDimensionMetadata(
      Map<String, MetadataItem> metadataItemMap,
      EventQueryParams params,
      boolean includeDetails,
      boolean isQueryRequest) {
    if (!params.hasValueDimension()) {
      return;
    }

    DimensionalItemObject value = params.getValue();
    String key =
        isQueryRequest
            ? value.getUid()
            : (params.hasStageInValue() ? params.getRequestValue() : value.getUid());

    metadataItemMap.put(
        key,
        new MetadataItem(
            value.getDisplayProperty(params.getDisplayProperty()),
            includeDetails ? value.getUid() : null,
            value.getCode()));
  }

  /**
   * Adds legend metadata to the map.
   *
   * @param metadataItemMap the metadata item map.
   * @param params the {@link EventQueryParams}.
   * @param includeDetails whether to include metadata details.
   */
  private void addLegendMetadata(
      Map<String, MetadataItem> metadataItemMap, EventQueryParams params, boolean includeDetails) {
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
  }

  /**
   * Adds option metadata for query requests.
   *
   * @param metadataItemMap the metadata item map.
   * @param params the {@link EventQueryParams}.
   * @param itemOptions the set of options.
   */
  private void addOptionMetadataForQuery(
      Map<String, MetadataItem> metadataItemMap, EventQueryParams params, Set<Option> itemOptions) {
    if (itemOptions == null) {
      return;
    }

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
   * Adds option metadata for non-query requests.
   *
   * @param metadataItemMap the metadata item map.
   * @param params the {@link EventQueryParams}.
   * @param includeDetails whether to include metadata details.
   */
  private void addOptionMetadataForNonQuery(
      Map<String, MetadataItem> metadataItemMap, EventQueryParams params, boolean includeDetails) {
    params.getItemOptions().stream()
        .filter(Objects::nonNull)
        .forEach(
            option ->
                metadataItemMap.put(
                    option.getUid(),
                    new MetadataItem(
                        option.getDisplayName(),
                        includeDetails ? option.getUid() : null,
                        option.getCode())));
  }

  /**
   * Adds items and filters metadata for query requests.
   *
   * @param metadataItemMap the metadata item map.
   * @param params the {@link EventQueryParams}.
   * @param includeDetails whether to include metadata details.
   */
  private void addItemsAndFiltersMetadataForQuery(
      Map<String, MetadataItem> metadataItemMap, EventQueryParams params, boolean includeDetails) {
    List<QueryItem> itemsAndFilters = params.getItemsAndItemFilters();

    itemsAndFilters.stream()
        .filter(Objects::nonNull)
        .forEach(
            item ->
                addItemToMetadata(
                    metadataItemMap, item, includeDetails, params.getDisplayProperty()));

    for (QueryItem item : itemsAndFilters) {
      addOrgUnitDimensionFilter(params, metadataItemMap, includeDetails, item, item.getFilters());
    }
  }

  /**
   * Adds items and filters metadata for non-query requests.
   *
   * @param metadataItemMap the metadata item map.
   * @param params the {@link EventQueryParams}.
   * @param includeDetails whether to include metadata details.
   */
  private void addItemsAndFiltersMetadataForNonQuery(
      Map<String, MetadataItem> metadataItemMap, EventQueryParams params, boolean includeDetails) {
    params.getItemsAndItemFilters().stream()
        .filter(Objects::nonNull)
        .forEach(
            item ->
                metadataItemMap.put(
                    getItemIdWithProgramStageIdPrefix(item),
                    new MetadataItem(
                        item.getItem().getDisplayName(), includeDetails ? item.getItem() : null)));
  }

  /**
   * Adds keywords metadata to the map.
   *
   * @param metadataItemMap the metadata item map.
   * @param keywords the list of keywords.
   */
  private void addKeywordsMetadata(
      Map<String, MetadataItem> metadataItemMap, @Nullable List<Keyword> keywords) {
    if (!isNotEmpty(keywords)) {
      return;
    }

    for (Keyword keyword : keywords) {
      if (keyword.getMetadataItem() != null) {
        metadataItemMap.put(
            keyword.getKey(), new MetadataItem(keyword.getMetadataItem().getName()));
      }
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
   * Returns a map between dimension identifiers and lists of dimension item identifiers.
   *
   * @param params the {@link EventQueryParams}.
   * @param itemOptions the item options to be added into dimension items.
   * @return a {@link Map} of dimension items.
   */
  private Map<String, List<String>> getDimensionItems(
      EventQueryParams params, Optional<Map<String, List<Option>>> itemOptions) {
    Map<String, List<String>> dimensionItems = new HashMap<>();

    dimensionItems.put(PERIOD_DIM_ID, resolvePeriodUids(params));

    addDimensionsAndFilters(dimensionItems, params);
    addQueryItemDimensions(dimensionItems, params, itemOptions);
    addItemFiltersToDimensionItems(params.getItemFilters(), dimensionItems);

    return dimensionItems;
  }

  /**
   * Resolves period UIDs based on calendar type.
   *
   * @param params the {@link EventQueryParams}.
   * @return a list of period UIDs.
   */
  private List<String> resolvePeriodUids(EventQueryParams params) {
    Calendar calendar = PeriodType.getCalendar();
    return calendar.isIso8601()
        ? getUids(params.getDimensionOrFilterItems(PERIOD_DIM_ID))
        : getLocalPeriodIdentifiers(params.getDimensionOrFilterItems(PERIOD_DIM_ID), calendar);
  }

  /**
   * Adds dimensions and filters to the dimension items map.
   *
   * @param dimensionItems the dimension items map.
   * @param params the {@link EventQueryParams}.
   */
  private void addDimensionsAndFilters(
      Map<String, List<String>> dimensionItems, EventQueryParams params) {
    for (DimensionalObject dim : params.getDimensionsAndFilters()) {
      dimensionItems.put(dim.getDimension(), getDimensionalItemIds(dim.getItems()));
    }
  }

  /**
   * Adds query item dimensions to the dimension items map.
   *
   * @param dimensionItems the dimension items map.
   * @param params the {@link EventQueryParams}.
   * @param itemOptions the item options.
   */
  private void addQueryItemDimensions(
      Map<String, List<String>> dimensionItems,
      EventQueryParams params,
      Optional<Map<String, List<Option>>> itemOptions) {

    for (QueryItem item : params.getItems()) {
      String itemUid = getItemUid(item);
      List<String> itemDimensionValues = resolveQueryItemDimension(item, params, itemOptions);
      dimensionItems.put(itemUid, itemDimensionValues);
    }
  }

  /**
   * Resolves dimension values for a single query item.
   *
   * @param item the {@link QueryItem}.
   * @param params the {@link EventQueryParams}.
   * @param itemOptions the item options.
   * @return a list of dimension values.
   */
  private List<String> resolveQueryItemDimension(
      QueryItem item, EventQueryParams params, Optional<Map<String, List<Option>>> itemOptions) {

    if (item.getValueType().isOrganisationUnit()) {
      return organisationUnitResolver.resolveOrgUnis(params, item);
    }

    if (item.hasOptionSet()) {
      return resolveOptionSetDimension(item, itemOptions);
    }

    if (item.hasLegendSet()) {
      return item.getLegendSetFilterItemsOrAll();
    }

    return List.of();
  }

  /**
   * Resolves option set dimension values.
   *
   * @param item the {@link QueryItem}.
   * @param itemOptions the item options.
   * @return a list of option UIDs.
   */
  private List<String> resolveOptionSetDimension(
      QueryItem item, Optional<Map<String, List<Option>>> itemOptions) {

    if (itemOptions.isPresent()) {
      String itemUid = getItemUid(item);
      List<Option> options = itemOptions.get().get(itemUid);
      return getDimensionItemUidsFrom(options, item.getOptionSetFilterItemsOrAll());
    }

    return item.getOptionSetFilterItemsOrAll();
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
      @Nullable List<Option> itemOptions, List<String> defaultOptionUids) {
    if (itemOptions == null) {
      return new ArrayList<>(defaultOptionUids);
    }
    return new ArrayList<>(IdentifiableObjectUtils.getUids(itemOptions));
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
   * Builds the organisation unit hierarchy map.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @return the parent graph map for active organisation units.
   */
  private Map<String, String> buildOrgUnitHierarchy(Grid grid, EventQueryParams params) {
    return getParentGraphMap(getActiveOrgUnits(grid, params), getOrgUnitRoots(params));
  }

  /**
   * Builds the organisation unit name hierarchy map.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @return the parent name graph map for active organisation units.
   */
  private Map<String, String> buildOrgUnitNameHierarchy(Grid grid, EventQueryParams params) {
    return getParentNameGraphMap(getActiveOrgUnits(grid, params), getOrgUnitRoots(params), true);
  }

  /**
   * Gets the active organisation units from the grid.
   *
   * @param grid the {@link Grid}.
   * @param params the {@link EventQueryParams}.
   * @return the list of active organisation units.
   */
  private List<OrganisationUnit> getActiveOrgUnits(Grid grid, EventQueryParams params) {
    List<OrganisationUnit> organisationUnits =
        asTypedList(params.getDimensionOrFilterItems(ORGUNIT_DIM_ID));
    return OrgUnitHelper.getActiveOrganisationUnits(grid, organisationUnits);
  }

  /**
   * Gets the organisation unit roots for the current user.
   *
   * @param params the {@link EventQueryParams}.
   * @return the set of root organisation units, or null if no user.
   */
  private Set<OrganisationUnit> getOrgUnitRoots(EventQueryParams params) {
    User user = securityManager.getCurrentUser(params);
    return user != null ? user.getOrganisationUnits() : null;
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

    if (item.getValueType() != ORGANISATION_UNIT) {
      return;
    }

    for (QueryFilter filter : filters) {
      String[] filterValues = trimToEmpty(filter.getFilter()).split(OPTION_SEP);
      for (String filterValue : filterValues) {
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
