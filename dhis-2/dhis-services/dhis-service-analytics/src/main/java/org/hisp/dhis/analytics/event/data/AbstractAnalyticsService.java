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

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;
import static org.hisp.dhis.analytics.event.data.QueryItemHelper.getItemOptions;
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
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.data.handler.SchemaIdResponseMapper;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.DimensionItemKeywords;
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

import com.google.common.collect.Lists;

/**
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
public abstract class AbstractAnalyticsService
{
    protected final AnalyticsSecurityManager securityManager;

    protected final EventQueryValidator queryValidator;

    protected final SchemaIdResponseMapper schemaIdResponseMapper;

    protected Grid getGrid( EventQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Decide access, add constraints and validate
        // ---------------------------------------------------------------------

        securityManager.decideAccessEventQuery( params );

        params = securityManager.withUserConstraints( params );

        queryValidator.validate( params );

        // Keywords and periods are removed in the next step

        List<DimensionItemKeywords.Keyword> periodKeywords = params.getDimensions().stream()
            .map( DimensionalObject::getDimensionItemKeywords )
            .filter( dimensionItemKeywords -> dimensionItemKeywords != null && !dimensionItemKeywords.isEmpty() )
            .flatMap( dk -> dk.getKeywords().stream() ).collect( toList() );

        params = new EventQueryParams.Builder( params )
            .withStartEndDatesForPeriods()
            .build();

        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        Grid grid = createGridWithHeaders( params );

        for ( DimensionalObject dimension : params.getDimensions() )
        {
            grid.addHeader( new GridHeader( dimension.getDimension(), dimension.getDimensionDisplayName(),
                ValueType.TEXT, false, true ) );
        }

        for ( QueryItem item : params.getItems() )
        {
            /**
             * If the request contains an item of value type ORGANISATION_UNIT
             * and the item UID is linked to coordinates (coordinateField), then
             * create header of value type COORDINATE and type Point.
             */
            if ( item.getValueType() == ValueType.ORGANISATION_UNIT
                && params.getCoordinateField().equals( item.getItem().getUid() ) )
            {
                grid.addHeader( new GridHeader( item.getItem().getUid(),
                    item.getItem().getDisplayProperty( params.getDisplayProperty() ), COORDINATE,
                    false, true, item.getOptionSet(), item.getLegendSet() ) );
            }
            else if ( hasNonDefaultRepeatableProgramStageOffset( item ) )
            {
                String column = item.getItem().getDisplayProperty( params.getDisplayProperty() );

                RepeatableStageParams repeatableStageParams = item.getRepeatableStageParams();

                String name = repeatableStageParams.getDimension();

                grid.addHeader( new GridHeader( name, column,
                    repeatableStageParams.simpleStageValueExpected() ? item.getValueType() : ValueType.REFERENCE,
                    false, true, item.getOptionSet(), item.getLegendSet(),
                    item.getProgramStage().getUid(), item.getRepeatableStageParams() ) );
            }
            else
            {
                String uid = getItemUid( item );

                String column = item.getItem().getDisplayProperty( params.getDisplayProperty() );

                grid.addHeader( new GridHeader( uid, column, item.getValueType(),
                    false, true, item.getOptionSet(), item.getLegendSet() ) );
            }
        }

        // ---------------------------------------------------------------------
        // Data
        // ---------------------------------------------------------------------

        long count = 0;

        if ( !params.isSkipData() || params.analyzeOnly() )
        {
            count = addEventData( grid, params );
        }

        // ---------------------------------------------------------------------
        // Metadata
        // ---------------------------------------------------------------------

        addMetadata( params, periodKeywords, grid );

        // ---------------------------------------------------------------------
        // Data ID scheme
        // ---------------------------------------------------------------------

        if ( params.hasDataIdScheme() )
        {
            substituteData( grid );
        }

        maybeApplyIdScheme( params, grid );

        // ---------------------------------------------------------------------
        // Paging
        // ---------------------------------------------------------------------

        maybeApplyPaging( params, count, grid );

        maybeApplyHeaders( params, grid );

        return grid;
    }

    /**
     * Substitutes the meta data of the grid with the identifier scheme meta
     * data property indicated in the query. This happens only when a custom ID
     * Schema is set.
     *
     * @param params the {@link EventQueryParams}.
     * @param grid the grid.
     */
    void maybeApplyIdScheme( EventQueryParams params, Grid grid )
    {
        if ( !params.isSkipMeta() && params.hasCustomIdSchemaSet() )
        {
            // Apply ID schemes mapped to the grid.
            grid.substituteMetaData( schemaIdResponseMapper.getSchemeIdResponseMap( params ) );
        }
    }

    private void maybeApplyPaging( EventQueryParams params, long count, Grid grid )
    {
        if ( params.isPaging() )
        {
            Pager pager = params.isTotalPages()
                ? new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() )
                : new SlimPager( params.getPageWithDefault(), params.getPageSizeWithDefault(), grid.hasLastDataRow() );

            grid.getMetaData().put( PAGER.getKey(), pager );
        }
    }

    /**
     * Based on the given item this method returns the correct UID based on
     * internal rules/requirements.
     *
     * @param item the current QueryItem
     * @return the correct uid based on the item type
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

    protected abstract Grid createGridWithHeaders( EventQueryParams params );

    protected abstract long addEventData( Grid grid, EventQueryParams params );

    private void maybeApplyHeaders( EventQueryParams params, Grid grid )
    {
        if ( params.hasHeaders() )
        {
            grid.retainColumns( params.getHeaders() );
            grid.repositionColumns( grid.repositionHeaders( new ArrayList<>( params.getHeaders() ) ) );
        }
    }

    /**
     * Adds meta data values to the given grid based on the given data query
     * parameters.
     *
     * @param params the data query parameters.
     * @param grid the grid.
     */
    protected void addMetadata( EventQueryParams params, Grid grid )
    {
        addMetadata( params, null, grid );
    }

    /**
     * Adds meta data values to the given grid based on the given data query
     * parameters.
     *
     * @param params the data query parameters.
     * @param grid the grid.
     */
    protected void addMetadata( EventQueryParams params, List<DimensionItemKeywords.Keyword> periodKeywords, Grid grid )
    {
        if ( !params.isSkipMeta() )
        {
            Map<String, Object> metadata = new HashMap<>();
            Map<String, List<Option>> optionsPresentInGrid = getItemOptions( grid, params );
            Set<Option> optionItems = new LinkedHashSet<>();
            boolean hasResults = isNotEmpty( grid.getRows() );

            if ( hasResults )
            {
                optionItems.addAll( optionsPresentInGrid.values().stream()
                    .flatMap( Collection::stream ).distinct().collect( toList() ) );
            }
            else
            {
                optionItems.addAll( getItemOptions( params.getItemOptions(), params.getItems() ) );
            }

            metadata.put( ITEMS.getKey(), getMetadataItems( params, periodKeywords, optionItems ) );
            metadata.put( DIMENSIONS.getKey(), getDimensionItems( params, optionsPresentInGrid ) );
            maybeAddOrgUnitHierarchyInfo( params, metadata );

            grid.setMetaData( metadata );
        }
    }

    /**
     * Depending on the params "hierarchy" metadata boolean flags, this method
     * may append (or not) Org. Unit data into the given metadata map.
     *
     * @param params the {@link EventQueryParams}.
     * @param metadata map.
     */
    private void maybeAddOrgUnitHierarchyInfo( EventQueryParams params, Map<String, Object> metadata )
    {
        if ( params.isHierarchyMeta() || params.isShowHierarchy() )
        {
            User user = securityManager.getCurrentUser( params );
            List<OrganisationUnit> organisationUnits = asTypedList(
                params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) );
            Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;

            if ( params.isHierarchyMeta() )
            {
                metadata.put( ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( organisationUnits, roots ) );
            }

            if ( params.isShowHierarchy() )
            {
                metadata.put( ORG_UNIT_NAME_HIERARCHY.getKey(),
                    getParentNameGraphMap( organisationUnits, roots, true ) );
            }
        }
    }

    /**
     * Returns a map of metadata item identifiers and {@link MetadataItem}.
     *
     * @param params the data query parameters.
     * @return a map.
     */
    private Map<String, MetadataItem> getMetadataItems( EventQueryParams params,
        List<DimensionItemKeywords.Keyword> periodKeywords, Set<Option> itemOptions )
    {
        Map<String, MetadataItem> metadataItemMap = AnalyticsUtils.getDimensionMetadataItemMap( params );

        boolean includeDetails = params.isIncludeMetadataDetails();

        if ( params.hasValueDimension() )
        {
            DimensionalItemObject value = params.getValue();
            metadataItemMap.put( value.getUid(),
                new MetadataItem( value.getDisplayProperty( params.getDisplayProperty() ),
                    includeDetails ? value.getUid() : null, value.getCode() ) );
        }

        params.getItemLegends().stream()
            .filter( Objects::nonNull )
            .forEach( legend -> metadataItemMap.put( legend.getUid(),
                new MetadataItem( legend.getDisplayName(), includeDetails ? legend.getUid() : null,
                    legend.getCode() ) ) );

        addMetadataItems( metadataItemMap, params, itemOptions );

        params.getItemsAndItemFilters().stream()
            .filter( Objects::nonNull )
            .forEach( item -> addItemToMetadata(
                metadataItemMap, item, includeDetails, params.getDisplayProperty() ) );

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

    private void addItemToMetadata( Map<String, MetadataItem> metadataItemMap, QueryItem item,
        boolean includeDetails, DisplayProperty displayProperty )
    {
        MetadataItem metadataItem = new MetadataItem( item.getItem().getDisplayProperty( displayProperty ),
            includeDetails ? item.getItem() : null );

        metadataItemMap.put( getItemIdWithProgramStageIdPrefix( item ), metadataItem );

        // Done for backwards compatibility

        metadataItemMap.put( item.getItemId(), metadataItem );
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

    /**
     * Indicates whether any keywords exist.
     *
     * @param keywords the list of {@link Keyword}.
     */
    private boolean hasPeriodKeywords( List<DimensionItemKeywords.Keyword> keywords )
    {
        return keywords != null && !keywords.isEmpty();
    }

    /**
     * Adds the given metadata items.
     *
     * @param metadataItemMap the metadata item map.
     * @param params the {@link EventQueryParams}.
     * @param itemOptions the list of {@link Option}.
     */
    private void addMetadataItems( Map<String, MetadataItem> metadataItemMap,
        EventQueryParams params, Set<Option> itemOptions )
    {
        boolean includeDetails = params.isIncludeMetadataDetails();

        itemOptions.forEach( option -> metadataItemMap.put( option.getUid(),
            new MetadataItem(
                option.getDisplayProperty( params.getDisplayProperty() ),
                includeDetails ? option.getUid() : null,
                option.getCode() ) ) );
    }

    /**
     * Returns a map between dimension identifiers and lists of dimension item
     * identifiers.
     *
     * @param params the data query parameters.
     * @param itemOptions the data query parameters.
     * @return a map.
     */
    private Map<String, List<String>> getDimensionItems( EventQueryParams params,
        Map<String, List<Option>> itemOptions )
    {
        Calendar calendar = PeriodType.getCalendar();

        List<String> periodUids = calendar.isIso8601() ? getUids( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) )
            : getLocalPeriodIdentifiers( params.getDimensionOrFilterItems( PERIOD_DIM_ID ), calendar );

        Map<String, List<String>> dimensionItems = new HashMap<>();

        dimensionItems.put( PERIOD_DIM_ID, periodUids );

        for ( DimensionalObject dim : params.getDimensionsAndFilters() )
        {
            dimensionItems.put( dim.getDimension(), getDimensionalItemIds( dim.getItems() ) );
        }

        for ( QueryItem item : params.getItems() )
        {
            final String itemUid = getItemUid( item );

            if ( item.hasOptionSet() )
            {
                // The call itemOptions.get( itemUid ) can return null.
                // This should be ok, the query item can't have both legends and
                // options.
                dimensionItems.put( itemUid,
                    getDimensionItemUidsFrom( itemOptions.get( itemUid ), item.getOptionSetFilterItemsOrAll() ) );
            }
            else if ( item.hasLegendSet() )
            {
                dimensionItems.put( itemUid, item.getLegendSetFilterItemsOrAll() );
            }
            else
            {
                dimensionItems.put( itemUid, Lists.newArrayList() );
            }
        }

        for ( QueryItem item : params.getItemFilters() )
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
                dimensionItems.put( item.getItemId(), Lists.newArrayList( item.getFiltersAsString() ) );
            }
        }

        return dimensionItems;
    }

    /**
     * Based on the given arguments, this method will extract a list of uids of
     * {@link Option}. If itemOptions is null, it returns the default list of
     * uids (defaultOptionUids). Otherwise, it will return the list of uids from
     * itemOptions.
     *
     * @param itemOptions a list of {@link Option} objects
     * @param defaultOptionUids a list of default {@link Option} uids
     * @return a list of uids
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
     * Substitutes metadata in the given grid.
     *
     * @param grid the {@link Grid}.
     */
    private void substituteData( Grid grid )
    {
        for ( int i = 0; i < grid.getHeaders().size(); i++ )
        {
            GridHeader header = grid.getHeaders().get( i );

            if ( header.hasOptionSet() )
            {
                Map<String, String> optionMap = header.getOptionSetObject().getOptionCodePropertyMap( IdScheme.NAME );
                grid.substituteMetaData( i, i, optionMap );
            }
            else if ( header.hasLegendSet() )
            {
                Map<String, String> legendMap = header.getLegendSetObject().getLegendUidPropertyMap( IdScheme.NAME );
                grid.substituteMetaData( i, i, legendMap );
            }
        }
    }

    private boolean hasNonDefaultRepeatableProgramStageOffset( QueryItem item )
    {
        return item != null && item.getProgramStage() != null && item.getRepeatableStageParams() != null
            && !item.getRepeatableStageParams().isDefaultObject();
    }
}
