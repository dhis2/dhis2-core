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
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import lombok.AllArgsConstructor;

import org.apache.commons.lang3.ObjectUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.calendar.DateTimeUnit;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MetadataDetailsHandler
{
    @Nonnull
    private final CurrentUserService currentUserService;

    /**
     * Adds meta data values to the given grid based on the given data query
     * parameters.
     */
    public void addMetadata( Grid grid, Set<Option> itemOptions, List<QueryItem> items,
        List<DimensionalItemObject> periodDimensionOrFilterItems,
        List<DimensionalItemObject> orgUnitDimensionOrFilterItems,
        User userParam, boolean hierarchyMeta, boolean showHierarchy, boolean includeMetadataDetails,
        List<DimensionItemKeywords.Keyword> periodKeywords, DimensionalItemObject value,
        DisplayProperty displayProperty, Set<Legend> itemLegends, List<QueryItem> itemsAndItemFilters,
        List<QueryItem> queryItems, List<QueryItem> itemFilters, List<DimensionalObject> dimensionsAndFilters,
        Program program, ProgramStage programStage )
    {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, List<Option>> optionsPresentInGrid = getItemOptions( grid, items );
        Set<Option> optionItems = new LinkedHashSet<>();
        boolean hasResults = isNotEmpty( grid.getRows() );

        if ( hasResults )
        {
            optionItems.addAll( optionsPresentInGrid.values().stream()
                .flatMap( Collection::stream ).distinct().collect( toList() ) );
        }
        else
        {
            optionItems.addAll( getItemOptionsAsFilter( itemOptions, queryItems ) );
        }

        metadata.put( ITEMS.getKey(), getMetadataItems( grid, periodKeywords, optionItems, includeMetadataDetails,
            value, displayProperty, itemLegends, itemsAndItemFilters, dimensionsAndFilters, program, programStage ) );
        metadata.put( DIMENSIONS.getKey(), getDimensionItems( periodDimensionOrFilterItems, dimensionsAndFilters, items,
            itemFilters, optionsPresentInGrid ) );
        addOrgUnitHierarchyInfo( grid, metadata, orgUnitDimensionOrFilterItems, userParam, hierarchyMeta,
            showHierarchy );

        grid.setMetaData( metadata );
    }

    /**
     * Depending on the params "hierarchy" metadata boolean flags, this method
     * may append (or not) Org. Unit data into the given metadata map.
     *
     * @param grid
     * @param metadata
     * @param orgUnitDimensionOrFilterItems
     * @param userParam
     * @param hierarchyMeta
     * @param showHierarchy
     */
    private void addOrgUnitHierarchyInfo( Grid grid, Map<String, Object> metadata,
        List<DimensionalItemObject> orgUnitDimensionOrFilterItems, User userParam, boolean hierarchyMeta,
        boolean showHierarchy )
    {
        if ( hierarchyMeta || showHierarchy )
        {
            User user = userParam != null ? userParam : currentUserService.getCurrentUser();

            List<OrganisationUnit> organisationUnits = asTypedList( orgUnitDimensionOrFilterItems );

            Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;

            List<OrganisationUnit> activeOrgUnits = getActiveOrganisationUnits( grid, organisationUnits );

            if ( hierarchyMeta )
            {
                metadata.put( ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( activeOrgUnits, roots ) );
            }

            if ( showHierarchy )
            {
                metadata.put( ORG_UNIT_NAME_HIERARCHY.getKey(),
                    getParentNameGraphMap( activeOrgUnits, roots, true ) );
            }
        }
    }

    /**
     * Returns a map between dimension identifiers and lists of dimension item
     * identifiers.
     */
    private Map<String, List<String>> getDimensionItems( List<DimensionalItemObject> periodDimensionOrFilterItems,
        List<DimensionalObject> dimensionsAndFilters, List<QueryItem> items, List<QueryItem> itemFilters,
        Map<String, List<Option>> itemOptions )
    {
        Calendar calendar = PeriodType.getCalendar();

        List<String> periodUids = calendar.isIso8601()
            ? getUids( periodDimensionOrFilterItems )
            : getLocalPeriodIdentifiers( periodDimensionOrFilterItems, calendar );

        Map<String, List<String>> dimensionItems = new HashMap<>();

        dimensionItems.put( PERIOD_DIM_ID, periodUids );

        for ( DimensionalObject dim : dimensionsAndFilters )
        {
            dimensionItems.put( dim.getDimension(), getDimensionalItemIds( dim.getItems() ) );
        }

        for ( QueryItem item : items )
        {
            String itemUid = getItemUid( item );

            if ( item.hasOptionSet() )
            {
                // The call itemOptions.get( itemUid ) can return null.
                // The query item can't have both legends and options.
                dimensionItems.put( itemUid,
                    getDimensionItemUidsFrom( itemOptions.get( itemUid ), item.getOptionSetFilterItemsOrAll() ) );
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

        for ( QueryItem item : itemFilters )
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

        return dimensionItems;
    }

    /**
     * Based on the given arguments, this method will extract a list of UIDs of
     * {@link Option}. If itemOptions is null, it returns the default list of
     * UIDs (defaultOptionUids). Otherwise, it will return the list of UIDs from
     * itemOptions.
     *
     * @param itemOptions a list of {@link Option} objects
     * @param defaultOptionUids a list of default {@link Option} UIDs.
     * @return a list of UIDs.
     */
    private List<String> getDimensionItemUidsFrom( List<Option> itemOptions, List<String> defaultOptionUids )
    {
        List<String> dimensionUids = new ArrayList<>();

        if ( itemOptions == null )
        {
            dimensionUids.addAll( defaultOptionUids );
        }
        else
        {
            dimensionUids.addAll( IdentifiableObjectUtils.getUids( itemOptions ) );
        }

        return dimensionUids;
    }

    /**
     * Based on the given item this method returns the correct UID based on
     * internal rules.
     *
     * @param item the current QueryItem.
     * @return the correct UID based on the item type.
     */
    private String getItemUid( QueryItem item )
    {
        String uid = item.getItem().getUid();

        if ( item.hasProgramStage() )
        {
            uid = joinWith( ".", item.getProgramStage().getUid(), uid );
        }

        return uid;
    }

    /**
     * Returns a map of metadata item identifiers and {@link MetadataItem}.
     */
    private Map<String, MetadataItem> getMetadataItems( Grid grid,
        List<DimensionItemKeywords.Keyword> periodKeywords, Set<Option> optionItems,
        boolean includeMetadataDetails, DimensionalItemObject value, DisplayProperty displayProperty,
        Set<Legend> itemLegends, List<QueryItem> itemsAndItemFilters, List<DimensionalObject> dimensionsAndFilters,
        Program program, ProgramStage programStage )
    {
        Map<String, MetadataItem> metadataItemMap = getDimensionMetadataItemMap( grid, dimensionsAndFilters,
            displayProperty, program, programStage, includeMetadataDetails );

        boolean includeDetails = includeMetadataDetails;

        if ( value != null )
        {
            metadataItemMap.put( value.getUid(),
                new MetadataItem( value.getDisplayProperty( displayProperty ),
                    includeDetails ? value.getUid() : null, value.getCode() ) );
        }

        itemLegends.stream()
            .filter( Objects::nonNull )
            .forEach( legend -> metadataItemMap.put( legend.getUid(),
                new MetadataItem( legend.getDisplayName(), includeDetails ? legend.getUid() : null,
                    legend.getCode() ) ) );

        addMetadataItems( metadataItemMap, optionItems, displayProperty, includeMetadataDetails );

        itemsAndItemFilters.stream()
            .filter( Objects::nonNull )
            .forEach( item -> addItemToMetadata(
                metadataItemMap, item, includeDetails, displayProperty ) );

        if ( hasPeriodKeywords( periodKeywords ) )
        {
            for ( DimensionItemKeywords.Keyword keyword : periodKeywords )
            {
                if ( keyword.getMetadataItem() != null )
                {
                    metadataItemMap.put( keyword.getKey(), new MetadataItem( keyword.getMetadataItem().getName() ) );
                }
            }
        }

        return metadataItemMap;
    }

    /**
     * Returns a mapping between identifiers and metadata items for the given
     * query.
     */
    public Map<String, MetadataItem> getDimensionMetadataItemMap( Grid grid,
        List<DimensionalObject> dimensionsAndFilters, DisplayProperty displayProperty, Program program,
        ProgramStage programStage, boolean includeMetadataDetails )
    {
        Map<String, MetadataItem> map = new HashMap<>();

        Calendar calendar = PeriodType.getCalendar();

        List<OrganisationUnit> organisationUnits = new ArrayList<>();

        for ( DimensionalObject dimension : dimensionsAndFilters )
        {
            for ( DimensionalItemObject item : dimension.getItems() )
            {
                if ( PERIOD == dimension.getDimensionType() && !calendar.isIso8601() )
                {
                    Period period = (Period) item;
                    DateTimeUnit dateTimeUnit = calendar.fromIso( period.getStartDate() );
                    String isoDate = period.getPeriodType().getIsoDate( dateTimeUnit );
                    map.put( isoDate,
                        new MetadataItem( period.getDisplayName(), includeMetadataDetails ? period : null ) );
                }
                else
                {
                    if ( ORGANISATION_UNIT == dimension.getDimensionType() )
                    {
                        organisationUnits = getActiveOrganisationUnits( grid,
                            organisationUnits );
                    }

                    map.put( item.getDimensionItem(),
                        new MetadataItem( item.getDisplayProperty( displayProperty ),
                            includeMetadataDetails ? item : null ) );
                }

                if ( DATA_ELEMENT == item.getDimensionItemType() )
                {
                    DataElement dataElement = (DataElement) item;

                    for ( CategoryOptionCombo coc : dataElement.getCategoryOptionCombos() )
                    {
                        map.put( coc.getUid(), new MetadataItem( coc.getDisplayProperty( displayProperty ),
                            includeMetadataDetails ? coc : null ) );
                    }
                }
            }

            for ( OrganisationUnit unit : organisationUnits )
            {
                for ( OrganisationUnit ancestor : unit.getAncestors() )
                {
                    map.put( ancestor.getUid(),
                        new MetadataItem( ancestor.getDisplayProperty( displayProperty ),
                            includeMetadataDetails ? ancestor : null ) );
                }
            }

            map.put( dimension.getDimension(),
                new MetadataItem( dimension.getDisplayProperty( displayProperty ),
                    includeMetadataDetails ? dimension : null ) );

            if ( dimension.getDimensionItemKeywords() != null )
            {
                // if there is includeMetadataDetails flag set to true
                // MetaDataItem is put into the map
                // with all existing information.
                // DimensionItemKeyword can use the same key and overwrite the
                // value with the less information (DimensionItemKeyword can
                // contain only key, uid, code and name ).
                // The key/value should be included only if absent.
                dimension.getDimensionItemKeywords().getKeywords()
                    .forEach( b -> map.putIfAbsent( b.getKey(), b.getMetadataItem() ) );
            }
        }

        if ( ObjectUtils.allNotNull( program ) )
        {
            map.put( program.getUid(), new MetadataItem( program.getDisplayProperty( displayProperty ),
                includeMetadataDetails ? program : null ) );

            if ( programStage != null )
            {
                map.put( programStage.getUid(),
                    new MetadataItem( programStage.getDisplayProperty( displayProperty ),
                        includeMetadataDetails ? programStage : null ) );
            }
            else
            {
                for ( ProgramStage ps : program.getProgramStages() )
                {
                    map.put( ps.getUid(), new MetadataItem( ps.getDisplayProperty( displayProperty ),
                        includeMetadataDetails ? ps : null ) );
                }
            }
        }

        return map;
    }

    /**
     * Adds the given item to the given metadata item map.
     *
     * @param metadataItemMap the metadata item map.
     * @param item the {@link QueryItem}.
     * @param includeDetails whether to include metadata details.
     * @param displayProperty the {@link DisplayProperty}.
     */
    private void addItemToMetadata( Map<String, MetadataItem> metadataItemMap, QueryItem item,
        boolean includeDetails, DisplayProperty displayProperty )
    {
        MetadataItem metadataItem = new MetadataItem( item.getItem().getDisplayProperty( displayProperty ),
            includeDetails ? item.getItem() : null );

        metadataItemMap.put( getItemIdWithProgramStageIdPrefix( item ), metadataItem );

        // Done for backwards compatibility.
        metadataItemMap.put( item.getItemId(), metadataItem );
    }

    /**
     * Adds the given metadata items.
     *
     * @param metadataItemMap the metadata item map.
     * @param itemOptions the list of {@link Option}.
     */
    private void addMetadataItems( Map<String, MetadataItem> metadataItemMap, Set<Option> itemOptions,
        DisplayProperty displayProperty, boolean includeMetadataDetails )
    {
        itemOptions.forEach( option -> metadataItemMap.put( option.getUid(),
            new MetadataItem(
                option.getDisplayProperty( displayProperty ),
                includeMetadataDetails ? option.getUid() : null,
                option.getCode() ) ) );
    }

    /**
     * Indicates whether any keywords exist.
     *
     * @param keywords the list of {@link DimensionItemKeywords.Keyword}.
     */
    private boolean hasPeriodKeywords( List<DimensionItemKeywords.Keyword> keywords )
    {
        return keywords != null && !keywords.isEmpty();
    }

    /**
     * Returns the query item identifier, may have a program stage prefix.
     *
     * @param item {@link QueryItem}.
     */
    private String getItemIdWithProgramStageIdPrefix( QueryItem item )
    {
        if ( item.hasProgramStage() )
        {
            return item.getProgramStage().getUid() + "." + item.getItemId();
        }

        return item.getItemId();
    }
}
