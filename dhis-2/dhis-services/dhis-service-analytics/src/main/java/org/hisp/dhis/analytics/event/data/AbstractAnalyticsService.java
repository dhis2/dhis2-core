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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.toList;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.data.handler.SchemaIdResponseMapper;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionItemKeywords;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
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
public abstract class AbstractAnalyticsService
{
    final AnalyticsSecurityManager securityManager;

    final EventQueryValidator queryValidator;

    final SchemaIdResponseMapper schemaIdResponseMapper;

    public AbstractAnalyticsService( AnalyticsSecurityManager securityManager, EventQueryValidator queryValidator,
        SchemaIdResponseMapper schemaIdResponseMapper )
    {
        checkNotNull( securityManager );
        checkNotNull( queryValidator );
        checkNotNull( schemaIdResponseMapper );

        this.securityManager = securityManager;
        this.queryValidator = queryValidator;
        this.schemaIdResponseMapper = schemaIdResponseMapper;
    }

    protected Grid getGrid( EventQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Decide access, add constraints and validate
        // ---------------------------------------------------------------------

        securityManager.decideAccessEventQuery( params );

        params = securityManager.withUserConstraints( params );

        queryValidator.validate( params );

        // keywords as well as their periods are removed in the next step,
        // params object is modified
        List<DimensionItemKeywords.Keyword> periodKeywords = params.getDimensions().stream().map(
            DimensionalObject::getDimensionItemKeywords )
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
             * Special case: If the request contains an item of Org Unit value
             * type and the item UID is linked to coordinates (coordinateField),
             * then create an Header of ValueType COORDINATE and type "Point"
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
        // Meta-data
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
        if ( !params.isSkipMeta() )
        {
            if ( params.hasCustomIdSchemaSet() )
            {
                // Apply all schemas set/mapped to the grid.
                grid.substituteMetaData( schemaIdResponseMapper.getSchemeIdResponseMap( params ) );
            }
        }
    }

    private static void maybeApplyPaging( EventQueryParams params, long count, Grid grid )
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
     * Based on the given item this method returns the correct uid based on
     * internal rules/requirements.
     *
     * @param item the current QueryItem
     * @return the correct uid based on the item type
     */
    private String getItemUid( final QueryItem item )
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

    private void maybeApplyHeaders( final EventQueryParams params, final Grid grid )
    {
        if ( params.hasHeaders() )
        {
            grid.keepOnlyThese( params.getHeaders() );
            grid.repositionColumns( grid.repositionHeaders( params.getHeaders() ) );
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
            final Map<String, Object> metadata = new HashMap<>();

            Map<String, List<Option>> options = getItemOptions( grid, params );

            metadata.put( ITEMS.getKey(), getMetadataItems( params, periodKeywords, options.values().stream()
                .flatMap( Collection::stream ).distinct().collect( toList() ) ) );

            metadata.put( DIMENSIONS.getKey(), getDimensionItems( params, options ) );

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

            grid.setMetaData( metadata );
        }
    }

    /**
     * Returns a map of metadata item identifiers and {@link MetadataItem}.
     *
     * @param params the data query parameters.
     * @return a map.
     */
    private Map<String, MetadataItem> getMetadataItems( EventQueryParams params,
        List<DimensionItemKeywords.Keyword> periodKeywords, List<Option> itemOptions )
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
            .forEach(
                item -> addItemIntoMetadata( metadataItemMap, item, includeDetails, params.getDisplayProperty() ) );

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

    private void addItemIntoMetadata( final Map<String, MetadataItem> metadataItemMap, final QueryItem item,
        final boolean includeDetails, final DisplayProperty displayProperty )
    {
        final MetadataItem metadataItem = new MetadataItem( item.getItem().getDisplayProperty( displayProperty ),
            includeDetails ? item.getItem() : null );

        metadataItemMap.put( getItemIdMaybeWithProgramStageIdPrefix( item ), metadataItem );

        // This is done for backward compatibility reason. It should remain here
        // while the New Event Report is living along with its "classic"
        // version.
        metadataItemMap.put( item.getItemId(), metadataItem );
    }

    /**
     * Program Stage id prefix for meta items
     *
     * @param item QueryItem.
     */
    private String getItemIdMaybeWithProgramStageIdPrefix( QueryItem item )
    {
        if ( item.hasProgramStage() )
        {
            return item.getProgramStage().getUid() + "." + item.getItemId();
        }

        return item.getItemId();
    }

    /**
     * check the period dimension keywords
     *
     * @param periodKeywords PeriodKeywords.
     */
    private boolean hasPeriodKeywords( List<DimensionItemKeywords.Keyword> periodKeywords )
    {
        return periodKeywords != null && !periodKeywords.isEmpty();
    }

    /**
     * Add into the MetadataItemMap itemOptions
     *
     * @param metadataItemMap MetadataItemMap.
     * @param params EventQueryParams.
     * @param itemOptions itemOtion list.
     */
    private void addMetadataItems( final Map<String, MetadataItem> metadataItemMap, final EventQueryParams params,
        final List<Option> itemOptions )
    {
        boolean includeDetails = params.isIncludeMetadataDetails();

        if ( !params.isSkipData() )
        {
            // filtering if the rows in grid are there (skipData = false)
            itemOptions.forEach( option -> metadataItemMap.put( option.getUid(),
                new MetadataItem( option.getDisplayProperty( params.getDisplayProperty() ),
                    includeDetails ? option.getUid() : null, option.getCode() ) ) );
        }
        else
        {
            // filtering if the rows in grid are not there (skipData = true
            // only)
            // dimension=Zj7UnCAulEk.K6uUAvq500H:IN:A00;A60;A01 -> IN indicates
            // there is a filter
            // the stream contains all options if no filter or only options fit
            // to the filter
            // options can be divided by separator <<;>>
            params.getItemOptions().stream()
                .filter( option -> option != null &&
                    (params.getItems().stream().noneMatch( QueryItem::hasFilter ) ||
                        params.getItems().stream().filter( QueryItem::hasFilter )
                            .anyMatch( qi -> qi.getFilters().stream()
                                .anyMatch( f -> Arrays.stream( f.getFilter().split( ";" ) )
                                    .anyMatch( ft -> ft.equalsIgnoreCase( option.getCode() ) ) ) )) )
                .forEach( option -> metadataItemMap.put( option.getUid(),
                    new MetadataItem( option.getDisplayProperty( params.getDisplayProperty() ),
                        includeDetails ? option.getUid() : null, option.getCode() ) ) );
        }
    }

    /**
     * Returns a map between dimension identifiers and lists of dimension item
     * identifiers.
     *
     * @param params the data query parameters.
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
                dimensionItems.put( itemUid,
                    getDimensionItemUidList( params, item, itemOptions.get( item.getItem().getUid() ) ) );
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
     * Return list of dimension item uids
     *
     * @param params EventQueryParams.
     * @param item QueryItem
     * @param itemOptions itemOtion list.
     * @return a list of uids.
     */
    private List<String> getDimensionItemUidList( EventQueryParams params, QueryItem item, List<Option> itemOptions )
    {
        if ( params.isSkipData() )
        {
            return item.getOptionSetFilterItemsOrAll();
        }
        else
        {
            return itemOptions.stream()
                .map( BaseIdentifiableObject::getUid )
                .collect( toList() );
        }
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
