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
package org.hisp.dhis.analytics.event.data;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;
import static org.hisp.dhis.analytics.event.data.QueryItemHelper.getItemOptions;
import static org.hisp.dhis.analytics.event.data.QueryItemHelper.getItemOptionsAsFilter;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.common.ValueType.COORDINATE;
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
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.data.handler.SchemaIdResponseMapper;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.orgunit.OrgUnitHelper;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.RepeatableStageParams;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.user.User;

/**
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
public abstract class AbstractAnalyticsService {
  protected final AnalyticsSecurityManager securityManager;

  protected final EventQueryValidator queryValidator;

  protected final SchemaIdResponseMapper schemaIdResponseMapper;

  /**
   * Returns a grid based on the given query.
   *
   * @param params the {@link EventQueryParams}.
   * @return a {@link Grid}.
   */
  protected Grid getGrid(EventQueryParams params) {
    // ---------------------------------------------------------------------
    // Decide access, add constraints and validate
    // ---------------------------------------------------------------------

    securityManager.decideAccessEventQuery(params);

    params = securityManager.withUserConstraints(params);

    queryValidator.validate(params);

    // Keywords and periods are removed in the next step

    List<DimensionItemKeywords.Keyword> periodKeywords =
        params.getDimensions().stream()
            .map(DimensionalObject::getDimensionItemKeywords)
            .filter(
                dimensionItemKeywords ->
                    dimensionItemKeywords != null && !dimensionItemKeywords.isEmpty())
            .flatMap(dk -> dk.getKeywords().stream())
            .collect(toList());

    params = new EventQueryParams.Builder(params).withStartEndDatesForPeriods().build();

    // ---------------------------------------------------------------------
    // Headers
    // ---------------------------------------------------------------------

    Grid grid = createGridWithHeaders(params);

    for (DimensionalObject dimension : params.getDimensions()) {
      grid.addHeader(
          new GridHeader(
              dimension.getDimension(),
              dimension.getDimensionDisplayName(),
              ValueType.TEXT,
              false,
              true));
    }

    for (QueryItem item : params.getItems()) {
      /**
       * If the request contains an item of value type ORGANISATION_UNIT and the item UID is linked
       * to coordinates (coordinateField), then create header of value type COORDINATE and type
       * Point.
       */
      if (item.getValueType() == ValueType.ORGANISATION_UNIT
          && params.getCoordinateFields().stream()
              .anyMatch(f -> f.equals(item.getItem().getUid()))) {
        grid.addHeader(
            new GridHeader(
                item.getItem().getUid(),
                item.getItem().getDisplayProperty(params.getDisplayProperty()),
                COORDINATE,
                false,
                true,
                item.getOptionSet(),
                item.getLegendSet()));
      } else if (item.hasNonDefaultRepeatableProgramStageOffset()) {
        String column = item.getItem().getDisplayProperty(params.getDisplayProperty());

        RepeatableStageParams repeatableStageParams = item.getRepeatableStageParams();

        String name = repeatableStageParams.getDimension();

        grid.addHeader(
            new GridHeader(
                name,
                column,
                repeatableStageParams.simpleStageValueExpected()
                    ? item.getValueType()
                    : ValueType.REFERENCE,
                false,
                true,
                item.getOptionSet(),
                item.getLegendSet(),
                item.getProgramStage().getUid(),
                item.getRepeatableStageParams()));
      } else {
        String uid = getItemUid(item);

        String column = item.getItem().getDisplayProperty(params.getDisplayProperty());

        grid.addHeader(
            new GridHeader(
                uid,
                column,
                item.getValueType(),
                false,
                true,
                item.getOptionSet(),
                item.getLegendSet()));
      }
    }

    // ---------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------

    long count = 0;

    if (!params.isSkipData() || params.analyzeOnly()) {
      count = addEventData(grid, params);
    }

    // ---------------------------------------------------------------------
    // Metadata
    // ---------------------------------------------------------------------

    addMetadata(params, periodKeywords, grid);

    // ---------------------------------------------------------------------
    // ID scheme
    // ---------------------------------------------------------------------

    if (params.hasDataIdScheme()) {
      substituteData(grid);
    }

    applyIdScheme(params, grid);

    // ---------------------------------------------------------------------
    // Paging
    // ---------------------------------------------------------------------

    addPaging(params, count, grid);

    // ---------------------------------------------------------------------
    // Headers
    // ---------------------------------------------------------------------

    addHeaders(params, grid);

    return grid;
  }

  /**
   * Substitutes the meta data of the grid with the identifier scheme meta data property indicated
   * in the query. This happens only when a custom identifier scheme is specified.
   *
   * @param params the {@link EventQueryParams}.
   * @param grid the {@link Grid}.
   */
  void applyIdScheme(EventQueryParams params, Grid grid) {
    if (!params.isSkipMeta() && params.hasCustomIdSchemaSet()) {
      grid.substituteMetaData(schemaIdResponseMapper.getSchemeIdResponseMap(params));
    }
  }

  /**
   * Applies paging to the given grid if the given query specifies paging.
   *
   * @param params the {@link EventQueryParams}.
   * @param totalCount the total count.
   * @param grid the {@link Grid}.
   */
  private void addPaging(EventQueryParams params, long totalCount, Grid grid) {
    if (params.isPaging()) {
      Pager pager =
          params.isTotalPages()
              ? new Pager(params.getPageWithDefault(), totalCount, params.getPageSizeWithDefault())
              : new SlimPager(
                  params.getPageWithDefault(),
                  params.getPageSizeWithDefault(),
                  grid.hasLastDataRow());

      grid.getMetaData().put(PAGER.getKey(), pager);
    }
  }

  /**
   * Based on the given item this method returns the correct UID based on internal rules.
   *
   * @param item the current QueryItem.
   * @return the correct UID based on the item type.
   */
  private String getItemUid(QueryItem item) {
    String uid = item.getItem().getUid();

    if (item.hasProgramStage()) {
      uid = joinWith(".", item.getProgramStage().getUid(), uid);
    }

    return uid;
  }

  protected abstract Grid createGridWithHeaders(EventQueryParams params);

  protected abstract long addEventData(Grid grid, EventQueryParams params);

  /**
   * Applies headers to the given if the given query specifies headers.
   *
   * @param params the {@link EventQueryParams}.
   * @param grid the {@link Grid}.
   */
  private void addHeaders(EventQueryParams params, Grid grid) {
    if (params.hasHeaders()) {
      grid.retainColumns(params.getHeaders());
    }
  }

  /**
   * Adds meta data values to the given grid based on the given data query parameters.
   *
   * @param params the {@link EventQueryParams}.
   * @param grid the {@link Grid}.
   */
  protected void addMetadata(EventQueryParams params, Grid grid) {
    addMetadata(params, null, grid);
  }

  /**
   * Adds meta data values to the given grid based on the given data query parameters.
   *
   * @param params the {@link EventQueryParams}.
   * @param periodKeywords the list of period keywords.
   * @param grid the {@link Grid}.
   */
  protected void addMetadata(
      EventQueryParams params, List<DimensionItemKeywords.Keyword> periodKeywords, Grid grid) {
    if (!params.isSkipMeta()) {
      Map<String, Object> metadata = new HashMap<>();
      Map<String, List<Option>> optionsPresentInGrid = getItemOptions(grid, params.getItems());
      Set<Option> optionItems = new LinkedHashSet<>();
      boolean hasResults = isNotEmpty(grid.getRows());

      if (hasResults) {
        optionItems.addAll(
            optionsPresentInGrid.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .collect(toList()));
      } else {
        optionItems.addAll(getItemOptionsAsFilter(params.getItemOptions(), params.getItems()));
      }

      if (params.isComingFromQuery()) {
        metadata.put(ITEMS.getKey(), getMetadataItems(params, periodKeywords, optionItems, grid));
        metadata.put(
            DIMENSIONS.getKey(), getDimensionItems(params, Optional.of(optionsPresentInGrid)));
      } else {
        metadata.put(ITEMS.getKey(), getMetadataItems(params));
        metadata.put(DIMENSIONS.getKey(), getDimensionItems(params, empty()));
      }

      maybeAddOrgUnitHierarchyInfo(params, metadata, grid);

      grid.setMetaData(metadata);
    }
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

      Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;

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
   * @param periodKeywords the period keywords.
   * @param itemOptions the set of item {@link Option}.
   * @param grid the grid instance {@link Grid}.
   * @return a map.
   */
  private Map<String, MetadataItem> getMetadataItems(
      EventQueryParams params,
      List<DimensionItemKeywords.Keyword> periodKeywords,
      Set<Option> itemOptions,
      Grid grid) {
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

    params.getItemsAndItemFilters().stream()
        .filter(Objects::nonNull)
        .forEach(
            item ->
                addItemToMetadata(
                    metadataItemMap, item, includeDetails, params.getDisplayProperty()));

    if (hasPeriodKeywords(periodKeywords)) {
      for (DimensionItemKeywords.Keyword keyword : periodKeywords) {
        if (keyword.getMetadataItem() != null) {
          metadataItemMap.put(
              keyword.getKey(), new MetadataItem(keyword.getMetadataItem().getName()));
        }
      }
    }

    return metadataItemMap;
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

    // Done for backwards compatibility

    metadataItemMap.put(item.getItemId(), metadataItem);
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
   * Indicates whether any keywords exist.
   *
   * @param keywords the list of {@link Keyword}.
   */
  private boolean hasPeriodKeywords(List<DimensionItemKeywords.Keyword> keywords) {
    return keywords != null && !keywords.isEmpty();
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

      if (item.hasOptionSet()) {
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

  /**
   * Substitutes metadata in the given grid.
   *
   * @param grid the {@link Grid}.
   */
  private void substituteData(Grid grid) {
    for (int i = 0; i < grid.getHeaders().size(); i++) {
      GridHeader header = grid.getHeaders().get(i);

      if (header.hasOptionSet()) {
        Map<String, String> optionMap =
            header.getOptionSetObject().getOptionCodePropertyMap(IdScheme.NAME);
        grid.substituteMetaData(i, i, optionMap);
      } else if (header.hasLegendSet()) {
        Map<String, String> legendMap =
            header.getLegendSetObject().getLegendUidPropertyMap(IdScheme.NAME);
        grid.substituteMetaData(i, i, legendMap);
      }
    }
  }
}
