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
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.DIMENSIONS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ITEMS;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY;
import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.PAGER;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdScheme;
import org.hisp.dhis.common.MetadataItem;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
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

    public AbstractAnalyticsService( AnalyticsSecurityManager securityManager, EventQueryValidator queryValidator )
    {
        checkNotNull( securityManager );
        checkNotNull( queryValidator );

        this.securityManager = securityManager;
        this.queryValidator = queryValidator;
    }

    protected Grid getGrid( EventQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Decide access, add constraints and validate
        // ---------------------------------------------------------------------

        securityManager.decideAccessEventQuery( params );

        params = securityManager.withUserConstraints( params );

        queryValidator.validate( params );

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

                String name = item.getProgramStage().getUid() + "[" + item.getProgramStageOffset() + "]." +
                    item.getItem().getUid();

                String column = item.getItem().getDisplayProperty( params.getDisplayProperty() );

                grid.addHeader( new GridHeader( name, column, item.getValueType(),
                    false, true, item.getOptionSet(), item.getLegendSet(),
                    item.getProgramStage().getUid(), item.getProgramStageOffset() ) );
            }
            else
            {
                String column = item.getItem().getDisplayProperty( params.getDisplayProperty() );

                grid.addHeader( new GridHeader( item.getItem().getUid(), column, item.getValueType(),
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

        addMetadata( params, grid );

        // ---------------------------------------------------------------------
        // Data ID scheme
        // ---------------------------------------------------------------------

        if ( params.hasDataIdScheme() )
        {
            substituteData( grid );
        }

        // ---------------------------------------------------------------------
        // Paging
        // ---------------------------------------------------------------------

        if ( params.isPaging() )
        {
            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );

            grid.getMetaData().put( PAGER.getKey(), pager );
        }

        maybeApplyHeaders( params, grid );

        return grid;
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
        if ( !params.isSkipMeta() )
        {
            final Map<String, Object> metadata = new HashMap<>();

            List<Option> options = getItemOptions( grid );

            metadata.put( ITEMS.getKey(), getMetadataItems( params, options ) );

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
     * Returns a map of metadata item options and {@link Option}.
     *
     * @param grid the Grid instance.
     * @return a list of options.
     */
    protected List<Option> getItemOptions( Grid grid )
    {
        List<Option> options = new ArrayList<>();

        for ( int i = 0; i < grid.getHeaders().size(); ++i )
        {
            GridHeader gridHeader = grid.getHeaders().get( i );

            if ( gridHeader.hasOptionSet() )
            {
                final int columnIndex = i;

                options.addAll( gridHeader
                    .getOptionSetObject()
                    .getOptions()
                    .stream()
                    .filter( opt -> grid.getRows().stream().anyMatch( r -> {
                        Object o = r.get( columnIndex );
                        if ( o instanceof String )
                        {
                            return ((String) o).equalsIgnoreCase( opt.getCode() );
                        }

                        return false;
                    } ) ).collect( Collectors.toList() ) );
            }
        }

        return options.stream().distinct().collect( Collectors.toList() );
    }

    /**
     * Returns a map of metadata item identifiers and {@link MetadataItem}.
     *
     * @param params the data query parameters.
     * @return a map.
     */
    private Map<String, MetadataItem> getMetadataItems( EventQueryParams params, List<Option> itemOptions )
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
            .forEach( item -> metadataItemMap.put( item.getItemId(),
                new MetadataItem( item.getItem().getDisplayName(), includeDetails ? item.getItem() : null ) ) );

        return metadataItemMap;
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
                new MetadataItem( option.getDisplayName(), includeDetails ? option.getUid() : null,
                    option.getCode() ) ) );
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
                    new MetadataItem( option.getDisplayName(), includeDetails ? option.getUid() : null,
                        option.getCode() ) ) );
        }
    }

    /**
     * Returns a map between dimension identifiers and lists of dimension item
     * identifiers.
     *
     * @param params the data query parameters.
     * @return a map.
     */
    private Map<String, List<String>> getDimensionItems( EventQueryParams params, List<Option> itemOptions )
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
            if ( item.hasOptionSet() )
            {
                dimensionItems.put( item.getItemId(), getDimensionItemUidList( params, item, itemOptions ) );
            }
            else if ( item.hasLegendSet() )
            {
                dimensionItems.put( item.getItemId(), item.getLegendSetFilterItemsOrAll() );
            }
            else
            {
                dimensionItems.put( item.getItemId(), Lists.newArrayList() );
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
                .collect( Collectors.toList() );
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
        return item.getProgramStage() != null && item.getProgramStageOffset() != 0;
    }
}
