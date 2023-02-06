/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.common.processing;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
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
import static org.hisp.dhis.analytics.orgunit.OrgUnitHelper.getActiveOrganisationUnits;
import static org.hisp.dhis.common.DimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.PERIOD;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifiers;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.hisp.dhis.analytics.common.AnalyticsPagingParams;
import org.hisp.dhis.analytics.common.CommonParams;
import org.hisp.dhis.analytics.common.MetadataInfo;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionItemKeywords.Keyword;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.SlimPager;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

/**
 * Component that, internally, handles all data structure and maps required by
 * the metadata object. It works on top of common objects, so it can be reused
 * by different analytics services/endpoints.
 *
 * This class and methods were pulled from other part of the code, so we could
 * have a centralized way to generate and keep the logic related to analytics
 * metadata elements. Light changes were applied to make the code slightly
 * cleaner. Major structural changes were not applied to reduce the risk of
 * bugs.
 */
@Component
public class MetadataParamsHandler
{
    private static final String DOT = ".";

    /**
     * Appends the metadata to the given {@link Grid} based on the given
     * arguments.
     *
     * @param grid the current {@link Grid}.
     * @param commonParams the {@link CommonParams}.
     * @param rowsCount the total of rows found for the current query.
     */
    public void handle( Grid grid, CommonParams commonParams, User user, long rowsCount )
    {
        if ( !commonParams.isSkipMeta() )
        {
            MetadataInfo metadataInfo = new MetadataInfo();

            // Dimensions.
            metadataInfo.put( ITEMS.getKey(), new MetadataItemsHandler().handle( grid, commonParams ) );
            metadataInfo.put( DIMENSIONS.getKey(),
                new MetadataDimensionsHandler().handle( grid, commonParams ) );

            // Org. Units.
            boolean hierarchyMeta = commonParams.isHierarchyMeta();
            boolean showHierarchy = commonParams.isShowHierarchy();

            if ( hierarchyMeta || showHierarchy )
            {
                List<OrganisationUnit> activeOrgUnits = getActiveOrgUnits( grid, commonParams );
                Set<OrganisationUnit> roots = getUserOrgUnits( user );

                if ( hierarchyMeta )
                {
                    metadataInfo.put( ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( activeOrgUnits, roots ) );
                }

                if ( showHierarchy )
                {
                    metadataInfo.put( ORG_UNIT_NAME_HIERARCHY.getKey(),
                        getParentNameGraphMap( activeOrgUnits, roots, true ) );
                }
            }

            // Paging.
            AnalyticsPagingParams pagingParams = commonParams.getPagingParams();

            if ( pagingParams.isPaging() )
            {
                metadataInfo.put( PAGER.getKey(), new MetadataPagingHandler().handle( grid, pagingParams, rowsCount ) );
            }

            grid.setMetaData( metadataInfo.getMap() );
        }
    }

    private Set<OrganisationUnit> getUserOrgUnits( User user )
    {
        return user != null ? user.getOrganisationUnits() : emptySet();
    }

    /**
     * Returns only the Org. Units currently present in the current grid rows.
     *
     * @param grid the current {@link Grid} object.
     * @param commonParams the {@link CommonParams}.
     */
    private List<OrganisationUnit> getActiveOrgUnits( Grid grid, CommonParams commonParams )
    {
        List<DimensionalItemObject> orgUnitDimensionOrFilterItems = commonParams.delegate()
            .getOrgUnitDimensionOrFilterItems();

        List<OrganisationUnit> organisationUnits = asTypedList( orgUnitDimensionOrFilterItems );

        return getActiveOrganisationUnits( grid, organisationUnits );
    }

    /**
     * Returns the query {@link QueryItem} identifier. It may be prefixed with
     * its program stage identifier (if one exists).
     *
     * @param item the {@link QueryItem}.
     *
     * @return the {@link QueryItem} uid with a prefix (if applicable).
     */
    private String getItemUid( @Nonnull QueryItem item )
    {
        String uid = item.getItem().getUid();

        if ( item.hasProgramStage() )
        {
            uid = joinWith( DOT, item.getProgramStage().getUid(), uid );
        }

        return uid;
    }

    class MetadataDimensionsHandler
    {
        /**
         * Handles all required logic/rules in order to return a map of metadata
         * item identifiers.
         *
         * @param grid the {@link Grid}.
         * @param commonParams the {@link CommonParams}.
         *
         * @return the map of {@link MetadataItem}.
         */
        private Map<String, List<String>> handle( Grid grid, CommonParams commonParams )
        {
            List<QueryItem> items = commonParams.delegate().getAllItems();
            List<DimensionalObject> allDimensionalObjects = commonParams.delegate().getAllDimensionalObjects();
            List<DimensionalItemObject> periodDimensionOrFilterItems = commonParams.delegate()
                .getPeriodDimensionOrFilterItems();
            List<QueryItem> itemFilters = commonParams.delegate().getItemsAsFilters();

            Calendar calendar = PeriodType.getCalendar();

            List<String> periodUids = calendar.isIso8601()
                ? getUids( periodDimensionOrFilterItems )
                : getLocalPeriodIdentifiers( periodDimensionOrFilterItems, calendar );

            Map<String, List<String>> dimensionItems = new HashMap<>();
            dimensionItems.put( PERIOD_DIM_ID, periodUids );

            for ( DimensionalObject dim : allDimensionalObjects )
            {
                dimensionItems.put( dim.getDimension(), getDimensionalItemIds( dim.getItems() ) );
            }

            putQueryItemsIntoMap( dimensionItems, items, grid );
            putFilterItemsIntoMap( dimensionItems, itemFilters );

            return dimensionItems;
        }

        /**
         * Adds the list of given {@link QueryItem} into the given dimension
         * items map.
         *
         * @param dimensionItems the map of items uid.
         * @param filterItems the list of {@link QueryItem}.
         */
        private void putFilterItemsIntoMap( Map<String, List<String>> dimensionItems, List<QueryItem> filterItems )
        {
            for ( QueryItem item : filterItems )
            {
                if ( item.hasOptionSet() )
                {
                    dimensionItems.put( item.getItemId(), item.getOptionSetFilterItemsOrAll() );
                }
                else if ( item.hasLegendSet() )
                {
                    dimensionItems.put( item.getItemId(), item.getLegendSetFilterItemsOrAll() );
                }
                else
                {
                    dimensionItems.put( item.getItemId(),
                        item.getFiltersAsString() != null
                            ? List.of( item.getFiltersAsString() )
                            : emptyList() );
                }
            }
        }

        /**
         * Adds the list of given {@link QueryItem} into the given dimension
         * items map. It also takes into consideration elements present in the
         * {@link Grid}.
         *
         * @param dimensionItems the map of items uid.
         * @param items the list of {@link QueryItem}.
         * @param grid the {@link Grid}.
         */
        private void putQueryItemsIntoMap( Map<String, List<String>> dimensionItems, List<QueryItem> items, Grid grid )
        {
            Map<String, List<Option>> optionsPresentInGrid = getItemOptions( grid, items );

            for ( QueryItem item : items )
            {
                String itemUid = getItemUid( item );

                if ( item.hasOptionSet() )
                {
                    // Query items can't have both legends and options.
                    dimensionItems.put( itemUid,
                        getDimensionItemUids( optionsPresentInGrid.get( itemUid ),
                            item.getOptionSetFilterItemsOrAll() ) );
                }
                else if ( item.hasLegendSet() )
                {
                    dimensionItems.put( itemUid, item.getLegendSetFilterItemsOrAll() );
                }
                else
                {
                    dimensionItems.put( itemUid, List.of() );
                }
            }
        }

        /**
         * Based on the given arguments, this method will extract a list of UIDs
         * of {@link Option}. If itemOptions is null, it returns the default
         * list of UIDs (defaultOptionUids). Otherwise, it will return the list
         * of UIDs from itemOptions.
         *
         * @param itemOptions a list of {@link Option} objects.
         * @param defaultOptionUids a list of default {@link Option} UIDs.
         *
         * @return a list of UIDs.
         */
        private List<String> getDimensionItemUids( List<Option> itemOptions, List<String> defaultOptionUids )
        {
            List<String> dimensionUids = new ArrayList<>();

            if ( itemOptions == null )
            {
                dimensionUids.addAll( defaultOptionUids );
            }
            else
            {
                dimensionUids.addAll( getUids( itemOptions ) );
            }

            return dimensionUids;
        }
    }

    class MetadataItemsHandler
    {
        /**
         * Handles all required logic/rules in order to return a map of metadata
         * item identifiers and its respective {@link MetadataItem}.
         *
         * @param grid the {@link Grid}.
         * @param commonParams the {@link CommonParams}.
         *
         * @return the map of {@link MetadataItem}.
         */
        Map<String, MetadataItem> handle( Grid grid, CommonParams commonParams )
        {
            List<QueryItem> items = commonParams.delegate().getAllItems();
            Set<Option> itemOptions = commonParams.delegate().getItemsOptions();
            Map<String, List<Option>> optionsPresentInGrid = getItemOptions( grid, items );
            Set<Option> optionItems = getOptionItems( grid, itemOptions, items, optionsPresentInGrid );
            List<DimensionalObject> allDimensionalObjects = commonParams.delegate().getAllDimensionalObjects();
            List<Keyword> periodKeywords = commonParams.delegate().getPeriodKeywords();
            Set<Legend> itemLegends = commonParams.delegate().getItemsLegends();
            List<Program> programs = commonParams.getPrograms();
            Set<ProgramStage> programStages = commonParams.delegate().getProgramStages();
            boolean includeMetadataDetails = commonParams.isIncludeMetadataDetails();
            DisplayProperty displayProperty = commonParams.getDisplayProperty();
            DimensionalItemObject value = commonParams.getValue();
            Map<String, MetadataItem> metadataItemMap = getDimensionMetadataItemMap( grid, allDimensionalObjects,
                displayProperty, programs, programStages, includeMetadataDetails );

            if ( value != null )
            {
                metadataItemMap.put( value.getUid(),
                    new MetadataItem( value.getDisplayProperty( displayProperty ),
                        includeMetadataDetails ? value.getUid() : null, value.getCode() ) );
            }

            itemLegends.stream()
                .filter( Objects::nonNull )
                .forEach( legend -> metadataItemMap.put( legend.getUid(),
                    new MetadataItem( legend.getDisplayName(), includeMetadataDetails ? legend.getUid() : null,
                        legend.getCode() ) ) );

            putItemOptionsIntoMap( metadataItemMap, optionItems, displayProperty, includeMetadataDetails );

            items.stream()
                .filter( Objects::nonNull )
                .forEach( item -> addItemToMap(
                    metadataItemMap, item, includeMetadataDetails, displayProperty ) );

            boolean hasPeriodKeywords = periodKeywords != null && !periodKeywords.isEmpty();

            if ( hasPeriodKeywords )
            {
                for ( Keyword keyword : periodKeywords )
                {
                    if ( keyword.getMetadataItem() != null )
                    {
                        metadataItemMap.put( keyword.getKey(),
                            new MetadataItem( keyword.getMetadataItem().getName() ) );
                    }
                }
            }

            return metadataItemMap;
        }

        /**
         * Gets the set of {@link Option} based on the given items and options.
         *
         * @param grid the {@link Grid}.
         * @param itemOptions the set of {@link Option}.
         * @param items the list of {@link QueryItem}.
         * @param optionsPresentInGrid the map of {@link Option} present in the
         *        grid.
         *
         * @return the set of relevant {@link Option}.
         */
        private Set<Option> getOptionItems( Grid grid, Set<Option> itemOptions, List<QueryItem> items,
            Map<String, List<Option>> optionsPresentInGrid )
        {
            Set<Option> optionItems = new LinkedHashSet<>();

            boolean hasResults = isNotEmpty( grid.getRows() );

            if ( hasResults )
            {
                optionItems.addAll( optionsPresentInGrid.values().stream()
                    .flatMap( List::stream ).distinct().collect( toList() ) );
            }
            else
            {
                optionItems.addAll( getItemOptionsAsFilter( itemOptions, items ) );
            }

            return optionItems;
        }

        /**
         * Adds the given item to the given {@link MetadataItem} item map. It
         * respects the boolean "includeItemDetails".
         *
         * @param metadataItemMap the {@link MetadataItem} item map.
         * @param item the {@link QueryItem}.
         * @param includeItemDetails whether to include metadata item details.
         * @param displayProperty the {@link DisplayProperty}.
         */
        private void addItemToMap( @Nonnull Map<String, MetadataItem> metadataItemMap, @Nonnull QueryItem item,
            boolean includeItemDetails,
            @Nonnull DisplayProperty displayProperty )
        {
            MetadataItem metadataItem = new MetadataItem( item.getItem().getDisplayProperty( displayProperty ),
                returnSameOrNull( includeItemDetails, item.getItem() ) );

            metadataItemMap.put( getItemUid( item ), metadataItem );

            // Done for backwards compatibility.
            metadataItemMap.put( item.getItemId(), metadataItem );
        }

        /**
         * Returns a map of dimensions and related items identifiers, including
         * org. units, periods, data elements, programs, stages, etc.
         *
         * @param grid the {@link Grid}.
         * @param dimensionsAndFilters the list of {@link DimensionalObject}.
         * @param displayProperty the {@link DisplayProperty}.
         * @param programs the list of {@link Program}.
         * @param programStages the list of {@link ProgramStage}.
         * @param includeMetadataDetails whether to include item details.
         *
         * @return a map of {@link MetadataItem} and its respective key.
         */
        private Map<String, MetadataItem> getDimensionMetadataItemMap( @Nonnull Grid grid,
            List<DimensionalObject> dimensionsAndFilters, @Nonnull DisplayProperty displayProperty,
            @CheckForNull List<Program> programs,
            @CheckForNull Set<ProgramStage> programStages, boolean includeMetadataDetails )
        {
            Map<String, MetadataItem> dimensionItemMap = new HashMap<>();

            putDimensionItemsIntoMap( dimensionItemMap, grid, dimensionsAndFilters, displayProperty,
                includeMetadataDetails );

            if ( isNotEmpty( programs ) )
            {
                programs.forEach(
                    program -> dimensionItemMap.put( program.getUid(),
                        new MetadataItem( program.getDisplayProperty( displayProperty ),
                            returnSameOrNull( includeMetadataDetails, program ) ) ) );
            }

            if ( isNotEmpty( programStages ) )
            {
                programStages.forEach( programStage -> dimensionItemMap.put( programStage.getUid(),
                    new MetadataItem( programStage.getDisplayProperty( displayProperty ),
                        returnSameOrNull( includeMetadataDetails, programStage ) ) ) );
            }
            else if ( isNotEmpty( programs ) )
            {
                programs.forEach(
                    program -> {
                        for ( ProgramStage ps : program.getProgramStages() )
                        {
                            dimensionItemMap.put( ps.getUid(),
                                new MetadataItem( ps.getDisplayProperty( displayProperty ),
                                    returnSameOrNull( includeMetadataDetails, ps ) ) );
                        }
                    } );
            }

            return dimensionItemMap;
        }

        /**
         * Adds a list of {@link DimensionalObject} objects into the given map
         * of {@link MetadataItem}. It takes into consideration display property
         * and the metadata details flag and the {@link Grid} headers and rows.
         *
         * @param metadataItemMap the metadata item map.
         * @param grid the {@link Grid}.
         * @param dimensionsAndFilters the list of {@link DimensionalObject}.
         * @param displayProperty the {@link DisplayProperty}.
         * @param includeMetadataDetails whether to include option item details.
         */
        private void putDimensionItemsIntoMap( @Nonnull Map<String, MetadataItem> metadataItemMap, @Nonnull Grid grid,
            @Nonnull List<DimensionalObject> dimensionsAndFilters, @Nonnull DisplayProperty displayProperty,
            boolean includeMetadataDetails )
        {
            List<OrganisationUnit> organisationUnits = new ArrayList<>();

            for ( DimensionalObject dimension : dimensionsAndFilters )
            {
                for ( DimensionalItemObject item : dimension.getItems() )
                {
                    putPeriodOrDefaultItemIntoMap( metadataItemMap, displayProperty, includeMetadataDetails,
                        dimension.getDimensionType(), item );
                    putDataElementIntoMap( metadataItemMap, displayProperty, includeMetadataDetails, item );

                    if ( ORGANISATION_UNIT == dimension.getDimensionType() )
                    {
                        organisationUnits = getActiveOrganisationUnits( grid, organisationUnits );
                    }
                }

                for ( OrganisationUnit unit : organisationUnits )
                {
                    for ( OrganisationUnit ancestor : unit.getAncestors() )
                    {
                        metadataItemMap.put( ancestor.getUid(),
                            new MetadataItem( ancestor.getDisplayProperty( displayProperty ),
                                returnSameOrNull( includeMetadataDetails, ancestor ) ) );
                    }
                }

                metadataItemMap.put( dimension.getDimension(),
                    new MetadataItem( dimension.getDisplayProperty( displayProperty ),
                        returnSameOrNull( includeMetadataDetails, dimension ) ) );

                if ( dimension.getDimensionItemKeywords() != null )
                {
                    dimension.getDimensionItemKeywords().getKeywords()
                        .forEach( b -> metadataItemMap.putIfAbsent( b.getKey(), b.getMetadataItem() ) );
                }
            }
        }

        /**
         * Adds the given {@link DimensionalItemObject} object into the given
         * map of {@link MetadataItem}. It takes into consideration display
         * property and the metadata details flag.
         *
         * @param metadataItemMap the metadata item map.
         * @param displayProperty the {@link DisplayProperty}.
         * @param includeMetadataDetails whether to include option item details.
         * @param dataElementItem the {@link DimensionalItemObject} to be added.
         */
        private void putDataElementIntoMap( Map<String, MetadataItem> metadataItemMap, DisplayProperty displayProperty,
            boolean includeMetadataDetails, DimensionalItemObject dataElementItem )
        {
            if ( DATA_ELEMENT == dataElementItem.getDimensionItemType() )
            {
                DataElement dataElement = (DataElement) dataElementItem;

                for ( CategoryOptionCombo coc : dataElement.getCategoryOptionCombos() )
                {
                    metadataItemMap.put( coc.getUid(), new MetadataItem( coc.getDisplayProperty( displayProperty ),
                        returnSameOrNull( includeMetadataDetails, coc ) ) );
                }
            }
        }

        /**
         * Adds the given {@link DimensionalItemObject} object into the given
         * map of {@link MetadataItem}. It takes into consideration the
         * dimension type, display property and the metadata details flag.
         *
         * @param metadataItemMap the metadata item map.
         * @param displayProperty the {@link DisplayProperty}.
         * @param includeMetadataDetails whether to include option item details.
         * @param dimensionType the {@link DimensionType}.
         * @param periodOrDefaultItem the {@link DimensionalItemObject} to be
         *        added.
         */
        private void putPeriodOrDefaultItemIntoMap( Map<String, MetadataItem> metadataItemMap,
            DisplayProperty displayProperty,
            boolean includeMetadataDetails, DimensionType dimensionType, DimensionalItemObject periodOrDefaultItem )
        {
            Calendar calendar = PeriodType.getCalendar();

            if ( PERIOD == dimensionType && !calendar.isIso8601() )
            {
                Period period = (Period) periodOrDefaultItem;
                DateTimeUnit dateTimeUnit = calendar.fromIso( period.getStartDate() );
                String isoDate = period.getPeriodType().getIsoDate( dateTimeUnit );

                metadataItemMap.put( isoDate, new MetadataItem( period.getDisplayName(),
                    returnSameOrNull( includeMetadataDetails, period ) ) );
            }
            else
            {
                metadataItemMap.put( periodOrDefaultItem.getDimensionItem(),
                    new MetadataItem( periodOrDefaultItem.getDisplayProperty( displayProperty ),
                        returnSameOrNull( includeMetadataDetails, periodOrDefaultItem ) ) );
            }
        }

        /**
         * Adds {@link Option} objects into the given map of
         * {@link MetadataItem}. It takes into consideration the display
         * property and the metadata details flag.
         *
         * @param metadataItemMap the metadata item map.
         * @param itemOptions the list of {@link Option}.
         * @param displayProperty the {@link DisplayProperty}.
         * @param includeMetadataDetails whether to include option item details.
         */
        private void putItemOptionsIntoMap( @Nonnull Map<String, MetadataItem> metadataItemMap,
            @CheckForNull Set<Option> itemOptions,
            @Nonnull DisplayProperty displayProperty, boolean includeMetadataDetails )
        {
            if ( isNotEmpty( itemOptions ) )
            {
                itemOptions.forEach( option -> metadataItemMap.put( option.getUid(),
                    new MetadataItem( option.getDisplayProperty( displayProperty ),
                        returnSameOrNull( includeMetadataDetails, option.getUid() ), option.getCode() ) ) );
            }
        }

        /**
         * Utility method that simply returns the same given object if the flag
         * is true. Otherwise, it returns null.
         *
         * @param returnSame the boolean flag.
         * @param object the object to be returned.
         *
         * @return the given object or null.
         *
         * @param <T>
         */
        private <T extends Object> T returnSameOrNull( boolean returnSame, T object )
        {
            if ( returnSame )
            {
                return object;
            }

            return null;
        }
    }

    class MetadataPagingHandler
    {
        /**
         * Handles all required logic/rules related to the paging element (if
         * applicable), and returns the correct {@link Pager} object.
         *
         * @param grid the {@link Grid}.
         * @param pagingParams the {@link AnalyticsPagingParams}.
         * @param rowsCount the total count.
         */
        Pager handle( Grid grid, AnalyticsPagingParams pagingParams, long rowsCount )
        {
            Pager pager;

            if ( pagingParams.showTotalPages() )
            {
                pager = new Pager( pagingParams.getPageWithDefault(), rowsCount,
                    pagingParams.getPageSizeWithDefault() );

                // Always try to remove last row.
                removeLastRow( pagingParams, grid );
            }
            else
            {
                boolean isLastPage = handleLastPageFlag( pagingParams, grid );

                pager = new SlimPager( pagingParams.getPageWithDefault(), pagingParams.getPageSizeWithDefault(),
                    isLastPage );
            }
            return pager;
        }

        /**
         * This method will handle the "lastPage" flag. Here, we assume that the
         * given {@Grid} might have page results + 1. We use this assumption to
         * return the correct boolean value.
         *
         * @param pagingParams the {@link AnalyticsPagingParams}.
         * @param grid the {@link Grid}.
         *
         * @return return true if this is the last page, false otherwise.
         */
        private boolean handleLastPageFlag( @Nonnull AnalyticsPagingParams pagingParams, @Nonnull Grid grid )
        {
            boolean isLastPage = grid.getHeight() > 0 && grid.getHeight() < pagingParams.getPageSizePlusOne();

            removeLastRow( pagingParams, grid );

            return isLastPage;
        }

        /**
         * As grid should have page size + 1 results, we need to remove the last
         * row if there are more pages left.
         *
         * @param pagingParams the {@link AnalyticsPagingParams}.
         * @param grid the {@link Grid}.
         */
        private void removeLastRow( @Nonnull AnalyticsPagingParams pagingParams, @Nonnull Grid grid )
        {
            boolean hasNextPageRow = grid.getHeight() == pagingParams.getPageSizePlusOne();

            if ( hasNextPageRow )
            {
                grid.removeCurrentWriteRow();
            }
        }
    }
}
