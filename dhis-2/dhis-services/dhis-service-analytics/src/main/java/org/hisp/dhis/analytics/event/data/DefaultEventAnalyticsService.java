package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.AnalyticsUtils;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.EventAnalyticsDimensionalItem;
import org.hisp.dhis.analytics.MetadataItem;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventAnalyticsUtils;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.ValueTypedDimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.Timer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.*;
import static org.hisp.dhis.analytics.DataQueryParams.*;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifiers;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;
import static org.hisp.dhis.reporttable.ReportTable.*;

/**
 * @author Lars Helge Overland
 */
public class DefaultEventAnalyticsService
    implements EventAnalyticsService
{
    private static final String NAME_EVENT = "Event";
    private static final String NAME_PROGRAM_STAGE = "Program stage";
    private static final String NAME_EVENT_DATE = "Event date";
    private static final String NAME_LONGITUDE = "Longitude";
    private static final String NAME_LATITUDE = "Latitude";
    private static final String NAME_ORG_UNIT_NAME = "Organisation unit name";
    private static final String NAME_ORG_UNIT_CODE = "Organisation unit code";    
    private static final String NAME_COUNT = "Count";
    private static final String NAME_CENTER = "Center";
    private static final String NAME_EXTENT = "Extent";
    private static final String NAME_POINTS = "Points";
    
    private static final Option OPT_TRUE = new Option( "Yes", "1" );
    private static final Option OPT_FALSE = new Option( "No", "0" );

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private EventAnalyticsManager eventAnalyticsManager;

    @Autowired
    private EnrollmentAnalyticsManager enrollmentAnalyticsManager;
    
    @Autowired
    private EventDataQueryService eventDataQueryService;

    @Autowired
    private AnalyticsSecurityManager securityManager;
    
    @Autowired
    private EventQueryPlanner queryPlanner;
    
    @Autowired
    private EventQueryValidator queryValidator;

    @Autowired
    private DatabaseInfo databaseInfo;

    // -------------------------------------------------------------------------
    // EventAnalyticsService implementation
    // -------------------------------------------------------------------------

    // TODO use [longitude/latitude] format for event points
    // TODO order event analytics tables on execution date to avoid default sort
    // TODO sorting in queries

    @Override
    public Grid getAggregatedEventData( EventQueryParams params, List<String> columns, List<String> rows )
        throws Exception
    {
        boolean tableLayout = (columns != null && !columns.isEmpty()) || (rows != null && !rows.isEmpty());

        return tableLayout ?
            getAggregatedEventDataTableLayout( params, columns, rows ) :
            getAggregatedEventData( params );
    }

    /**
     * Create a grid with table layout for downloading event reports.
     * The grid is dynamically made from rows and columns input, which refers to the dimensions requested.
     *
     * For event reports each option for a dimension will be an {@link EventAnalyticsDimensionalItem} and all permutations
     * will be added to the grid.
     *
     * @param params the event query parameters.
     * @param columns the identifiers of the dimensions to use as columns.
     * @param rows the identifiers of the dimensions to use as rows.
     * @return aggregated data as a Grid object.
     */
    private Grid getAggregatedEventDataTableLayout( EventQueryParams params, List<String> columns, List<String> rows )
        throws Exception
    {
        params.removeProgramIndicatorItems(); // Not supported as items for aggregate

        Grid grid = getAggregatedEventData( params );

        ListUtils.removeEmptys( columns );
        ListUtils.removeEmptys( rows );

        Map<String, List<EventAnalyticsDimensionalItem>> tableColumns = new LinkedHashMap<>();
        if ( columns != null )
        {
            for ( String dimension : columns )
            {
                getEventDataObjects( grid, params, tableColumns, dimension );
            }
        }

        Map<String, List<EventAnalyticsDimensionalItem>> tableRows = new LinkedHashMap<>();
        List<String> rowDimensions = new ArrayList<>();
        if ( rows != null )
        {
            for ( String dimension : rows )
            {
                rowDimensions.add( dimension );
                getEventDataObjects( grid, params, tableRows, dimension );
            }
        }

        List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations = EventAnalyticsUtils.generateEventDataPermutations( tableRows );
        List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations = EventAnalyticsUtils.generateEventDataPermutations( tableColumns );

        return generateOutputGrid( grid, params, rowPermutations, columnPermutations, rowDimensions );
    }

    /**
     * Generate output grid for event analytics download based on input parameters.
     *
     * @param grid the result grid
     * @param params the event query parameters.
     * @param rowPermutations the row permutations
     * @param columnPermutations the column permutations
     * @param rowDimensions the row dimensions
     * @return grid with table layout
     */
    @SuppressWarnings( "unchecked" )
    private Grid generateOutputGrid( Grid grid, EventQueryParams params, List<Map<String, EventAnalyticsDimensionalItem>> rowPermutations, List<Map<String, EventAnalyticsDimensionalItem>> columnPermutations, List<String> rowDimensions )
    {
        Grid outputGrid = new ListGrid();
        outputGrid.setTitle( IdentifiableObjectUtils.join( params.getFilterItems() ) );

        for ( String row : rowDimensions )
        {
            MetadataItem metadataItem = (MetadataItem) ((Map<String, Object>) grid.getMetaData()
                .get( ITEMS.getKey() )).get( row );

            String name = StringUtils.defaultIfEmpty( metadataItem.getName(), row );
            String col = StringUtils.defaultIfEmpty( COLUMN_NAMES.get( row ), row );

            outputGrid
                .addHeader( new GridHeader( name, col, ValueType.TEXT, String.class.getName(), false, true ) );
        }

        columnPermutations.forEach( permutation -> {
            StringBuilder builder = new StringBuilder();

            permutation.forEach( ( key, value ) -> {
                if ( !key.equals( ORGUNIT_DIM_ID ) && !key.equals( PERIOD_DIM_ID ) )
                {
                    builder.append( key ).append( SPACE );
                }
                builder.append( value.getDisplayProperty( params.getDisplayProperty() ) )
                    .append( DASH_PRETTY_SEPARATOR );
            } );

            String display = builder.length() > 0 ?
                builder.substring( 0, builder.lastIndexOf( DASH_PRETTY_SEPARATOR ) ) :
                TOTAL_COLUMN_PRETTY_NAME;

            outputGrid.addHeader( new GridHeader( display, display,
                ValueType.NUMBER, Double.class.getName(), false, false ) );
        } );

        for ( Map<String, EventAnalyticsDimensionalItem> rowCombination : rowPermutations )
        {
            outputGrid.addRow();
            List<List<String>> ids = new ArrayList<>();
            Map<String, EventAnalyticsDimensionalItem> displayObjects = new HashMap<>();

            boolean fillDisplayList = true;

            for ( Map<String, EventAnalyticsDimensionalItem> columnCombination : columnPermutations )
            {
                List<String> idList = new ArrayList<>();

                boolean finalFillDisplayList = fillDisplayList;
                rowCombination.forEach( ( key, value ) -> {
                    idList.add( value.toString() );

                    if ( finalFillDisplayList )
                    {
                        displayObjects.put( value.getParentUid(), value );
                    }
                } );

                columnCombination.forEach( ( key, value ) -> idList.add( value.toString() ) );

                ids.add( idList );
                fillDisplayList = false;
            }

            rowDimensions.forEach( dimension -> outputGrid
                .addValue( displayObjects.get( dimension ).getDisplayProperty( params.getDisplayProperty() ) ) );

            EventAnalyticsUtils.addValues( ids, grid, outputGrid );
        }

        return outputGrid;
    }

    /**
     * Put elements into the map "table". The elements are fetched from the query parameters.
     *
     * @param params the event query parameters.
     * @param table the map to add elements to
     * @param dimension the requested dimension
     */
    private void getEventDataObjects( Grid grid, EventQueryParams params,
        Map<String, List<EventAnalyticsDimensionalItem>> table, String dimension ) throws Exception
    {
        List<EventAnalyticsDimensionalItem> objects = params.getEventReportDimensionalItemArrayExploded( dimension );

        if ( objects.size() == 0 )
        {
            ValueTypedDimensionalItemObject eventDimensionalItemObject = dataElementService.getDataElement( dimension );

            if ( eventDimensionalItemObject == null )
            {
                eventDimensionalItemObject = trackedEntityAttributeService
                    .getTrackedEntityAttribute( dimension );
            }

            addEventReportDimensionalItems( eventDimensionalItemObject, objects, grid, dimension );

            table.put( eventDimensionalItemObject.getDisplayProperty( params.getDisplayProperty() ), objects );
        }
        else
        {
            table.put( dimension, objects );
        }
    }

    /**
     * Send in a list of {@link EventAnalyticsDimensionalItem} and add properties from {@link EventAnalyticsDimensionalItem} parameter.
     *
     * @param eventDimensionalItemObject object to get properties from
     * @param objects the list with objects. We are adding objects to this list as well.
     * @param grid the grid from the event analytics request
     * @param dimension the dimension we are looking at
     * @throws Exception throws exception if the given dimension is invalid
     */
    @SuppressWarnings( "unchecked" )
    private void addEventReportDimensionalItems( ValueTypedDimensionalItemObject eventDimensionalItemObject, 
        List<EventAnalyticsDimensionalItem> objects, Grid grid, String dimension ) throws Exception
    {
        if ( eventDimensionalItemObject == null )
        {
            throw new IllegalStateException( String.format( "Data dimension '%s' is invalid", dimension ) );
        }

        String parentUid = eventDimensionalItemObject.getUid();

        if ( eventDimensionalItemObject.getValueType() == ValueType.BOOLEAN )
        {
            objects.add( new EventAnalyticsDimensionalItem( OPT_TRUE, parentUid ) );
            objects.add( new EventAnalyticsDimensionalItem( OPT_FALSE, parentUid ) );
        }

        if ( eventDimensionalItemObject.hasOptionSet() )
        {
            for ( Option option : eventDimensionalItemObject.getOptionSet().getOptions() )
            {
                objects.add( new EventAnalyticsDimensionalItem( option, parentUid ) );
            }
        }
        else if ( eventDimensionalItemObject.hasLegendSet() )
        {
            List<String> legendOptions = (List<String>) ((Map<String, Object>) grid.getMetaData()
                .get( DIMENSIONS.getKey() ))
                .get( dimension );

            if ( legendOptions.isEmpty() )
            {
                List<Legend> legends = eventDimensionalItemObject.getLegendSet().getSortedLegends();

                for ( Legend legend : legends )
                {
                    for ( int i = legend.getStartValue().intValue(); i < legend.getEndValue(); i++ )
                    {
                        objects.add( new EventAnalyticsDimensionalItem( new Option( 
                            String.valueOf( i ), String.valueOf( i ) ), parentUid ) );
                    }
                }
            }
            else
            {
                for ( String legend : legendOptions )
                {
                    MetadataItem metadataItem = (MetadataItem) ((Map<String, Object>) grid.getMetaData()
                        .get( ITEMS.getKey() ))
                        .get( legend );

                    objects.add( new EventAnalyticsDimensionalItem( 
                        new Option( metadataItem.getName(), legend ), parentUid ) );
                }
            }
        }
    }

    @Override
    public Grid getAggregatedEventData( EventQueryParams params )
    {
        securityManager.decideAccess( params );
        
        queryValidator.validate( params );
        
        params.removeProgramIndicatorItems(); // Not supported as items for aggregate
        
        Grid grid = new ListGrid();

        int maxLimit = queryValidator.getMaxLimit();
        
        // ---------------------------------------------------------------------
        // Headers and data
        // ---------------------------------------------------------------------

        if ( !params.isSkipData() )
        {
            // -----------------------------------------------------------------
            // Headers
            // -----------------------------------------------------------------

            if ( params.isCollapseDataDimensions() || params.isAggregateData() )
            {
                grid.addHeader( new GridHeader( DimensionalObject.DATA_COLLAPSED_DIM_ID, DataQueryParams.DISPLAY_NAME_DATA_X, ValueType.TEXT, String.class.getName(), false, true ) );
            }
            else
            {
                for ( QueryItem item : params.getItems() )
                {
                    String legendSet = item.hasLegendSet() ? item.getLegendSet().getUid() : null;
    
                    grid.addHeader( new GridHeader( item.getItem().getUid(), item.getItem().getName(), item.getValueType(), item.getTypeAsString(), false, true, item.getOptionSetUid(), legendSet ) );
                }
            }
            
            for ( DimensionalObject dimension : params.getDimensions() )
            {
                grid.addHeader( new GridHeader( dimension.getDimension(), dimension.getDisplayName(), ValueType.TEXT, String.class.getName(), false, true ) );
            }
    
            grid.addHeader( new GridHeader( VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) );

            if ( params.isIncludeNumDen() )
            {
                grid.addHeader( new GridHeader( NUMERATOR_ID, NUMERATOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) )
                    .addHeader( new GridHeader( DENOMINATOR_ID, DENOMINATOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) )
                    .addHeader( new GridHeader( FACTOR_ID, FACTOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) );
            }

            // -----------------------------------------------------------------
            // Data
            // -----------------------------------------------------------------

            Timer timer = new Timer().start().disablePrint();
    
            List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
    
            timer.getSplitTime( "Planned event query, got partitions: " + params.getPartitions() );

            for ( EventQueryParams query : queries )
            {
                //TODO: As we make support for event reports with enrollment listings, we will have to change this.
                if ( query.hasEnrollmentProgramIndicatorDimension() )
                {
                    enrollmentAnalyticsManager.getAggregatedEventData( query, grid, maxLimit );
                }
                else
                {
                    eventAnalyticsManager.getAggregatedEventData( query, grid, maxLimit );
                }
            }
            
            timer.getTime( "Got aggregated events" );
            
            if ( maxLimit > 0 && grid.getHeight() > maxLimit )
            {
                throw new IllegalQueryException( "Number of rows produced by query is larger than the max limit: " + maxLimit );
            }

            // -----------------------------------------------------------------
            // Limit and sort, done again due to potential multiple partitions
            // -----------------------------------------------------------------

            if ( params.hasSortOrder() && grid.getHeight() > 0 )
            {
                grid.sortGrid( 1, params.getSortOrderAsInt() );
            }
            
            if ( params.hasLimit() && grid.getHeight() > params.getLimit() )
            {
                grid.limitGrid( params.getLimit() );
            }
        }

        // ---------------------------------------------------------------------
        // Meta-data
        // ---------------------------------------------------------------------

        addMetadata( params, grid );
        
        return grid;
    }

    @Override
    public Grid getAggregatedEventData( AnalyticalObject object )
    {
        EventQueryParams params = eventDataQueryService.getFromAnalyticalObject( (EventAnalyticalObject) object );
        
        return getAggregatedEventData( params );
    }

    @Override
    public Grid getEvents( EventQueryParams params )
    {
        securityManager.decideAccessEventQuery( params );
        
        queryValidator.validate( params );
        
        params = new EventQueryParams.Builder( params )
            .withStartEndDatesForPeriods()
            .build();

        Grid grid = new ListGrid();
        
        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        grid.addHeader( new GridHeader( ITEM_EVENT, NAME_EVENT, ValueType.TEXT, String.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_PROGRAM_STAGE, NAME_PROGRAM_STAGE, ValueType.TEXT, String.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_EVENT_DATE, NAME_EVENT_DATE, ValueType.DATE, Date.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_LONGITUDE, NAME_LONGITUDE, ValueType.NUMBER, Double.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_LATITUDE, NAME_LATITUDE, ValueType.NUMBER, Double.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_ORG_UNIT_NAME, NAME_ORG_UNIT_NAME, ValueType.TEXT, String.class.getName(), false, true ) )
            .addHeader( new GridHeader( ITEM_ORG_UNIT_CODE, NAME_ORG_UNIT_CODE, ValueType.TEXT, String.class.getName(), false, true ) );

        for ( DimensionalObject dimension : params.getDimensions() )
        {
            grid.addHeader( new GridHeader( dimension.getDimension(), dimension.getDisplayName(), ValueType.TEXT, String.class.getName(), false, true ) );
        }

        for ( QueryItem item : params.getItems() )
        {
            grid.addHeader( new GridHeader( item.getItem().getUid(), item.getItem().getName(), item.getValueType(), item.getTypeAsString(), false, true, item.getOptionSetUid(), item.getLegendSetUid() ) );
        }

        // ---------------------------------------------------------------------
        // Data
        // ---------------------------------------------------------------------

        Timer timer = new Timer().start().disablePrint();

        params = queryPlanner.planEventQuery( params );

        timer.getSplitTime( "Planned event query, got partitions: " + params.getPartitions() );

        long count = 0;

        if ( params.getPartitions().hasAny() )
        {
            if ( params.isPaging() )
            {
                count += eventAnalyticsManager.getEventCount( params );
            }
    
            eventAnalyticsManager.getEvents( params, grid, queryValidator.getMaxLimit() );
    
            timer.getTime( "Got events " + grid.getHeight() );
        }

        // ---------------------------------------------------------------------
        // Meta-data
        // ---------------------------------------------------------------------

        addMetadata( params, grid );

        // ---------------------------------------------------------------------
        // Paging
        // ---------------------------------------------------------------------

        if ( params.isPaging() )
        {
            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            
            grid.getMetaData().put( PAGER.getKey(), pager );
        }

        return grid;
    }

    @Override
    public Grid getEventClusters( EventQueryParams params )
    {
        if ( !databaseInfo.isSpatialSupport() )
        {
            throw new IllegalQueryException( "Spatial database support is not enabled" );
        }
        
        params = new EventQueryParams.Builder( params )
            .withGeometryOnly( true )
            .withStartEndDatesForPeriods()
            .build();
        
        securityManager.decideAccess( params );
        
        queryValidator.validate( params );
        
        Grid grid = new ListGrid();
        
        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        grid.addHeader( new GridHeader( ITEM_COUNT, NAME_COUNT, ValueType.NUMBER, Long.class.getName(), false, false ) )
            .addHeader( new GridHeader( ITEM_CENTER, NAME_CENTER, ValueType.TEXT, String.class.getName(), false, false ) )
            .addHeader( new GridHeader( ITEM_EXTENT, NAME_EXTENT, ValueType.TEXT, String.class.getName(), false, false ) )
            .addHeader( new GridHeader( ITEM_POINTS, NAME_POINTS, ValueType.TEXT, String.class.getName(), false, false ) );

        // ---------------------------------------------------------------------
        // Data
        // ---------------------------------------------------------------------

        params = queryPlanner.planEventQuery( params );

        eventAnalyticsManager.getEventClusters( params, grid, queryValidator.getMaxLimit() );
        
        return grid;
    }
    
    @Override
    public Rectangle getRectangle( EventQueryParams params )
    {
        if ( !databaseInfo.isSpatialSupport() )
        {
            throw new IllegalQueryException( "Spatial database support is not enabled" );
        }

        params = new EventQueryParams.Builder( params )
            .withGeometryOnly( true )
            .withStartEndDatesForPeriods()
            .build();
        
        securityManager.decideAccess( params );
        
        queryValidator.validate( params );

        params = queryPlanner.planEventQuery( params );

        return eventAnalyticsManager.getRectangle( params );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Adds meta data values to the given grid based on the given data query
     * parameters.
     * 
     * @param params the data query parameters.
     * @param grid the grid.
     */
    private void addMetadata( EventQueryParams params, Grid grid )
    {
        if ( !params.isSkipMeta() )
        {
            final Map<String, Object> metadata = new HashMap<>();
            
            metadata.put( ITEMS.getKey(), getMetadataItems( params ) );
            metadata.put( DIMENSIONS.getKey(), getDimensionItems( params ) );

            if ( params.isHierarchyMeta() || params.isShowHierarchy() )
            {
                User user = securityManager.getCurrentUser( params );    
                List<OrganisationUnit> organisationUnits = asTypedList( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) );                
                Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;
                
                if ( params.isHierarchyMeta() )
                {
                    metadata.put( ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( organisationUnits, roots ) );
                }
    
                if ( params.isShowHierarchy() )
                {
                    metadata.put( ORG_UNIT_NAME_HIERARCHY.getKey(), getParentNameGraphMap( organisationUnits, roots, true ) );
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
    private Map<String, MetadataItem> getMetadataItems( EventQueryParams params )
    {
        Map<String, MetadataItem> metadataItemMap = AnalyticsUtils.getDimensionMetadataItemMap( params );

        boolean includeDetails = params.isIncludeMetadataDetails();

        if ( params.hasValueDimension() )
        {
            DimensionalItemObject value = params.getValue();            
            metadataItemMap.put( value.getUid(), new MetadataItem( value.getDisplayProperty( params.getDisplayProperty() ), includeDetails ? value.getUid() : null, value.getCode() ) );
        }

        params.getItemLegends().forEach( legend -> {
            metadataItemMap.put( legend.getUid(), new MetadataItem( legend.getDisplayName(), includeDetails ? legend.getUid() : null, legend.getCode() ) );
        } );

        params.getItemOptions().forEach( option -> {
            metadataItemMap.put( option.getUid(), new MetadataItem( option.getDisplayName(), includeDetails ? option.getUid() : null, option.getCode() ) );
        } );

        params.getItemsAndItemFilters().forEach( item -> {
            metadataItemMap.put( item.getItemId(), new MetadataItem( item.getItem().getDisplayName(), includeDetails ? item.getItem() : null ) );
        } );

        return metadataItemMap;
    }
    
    /**
     * Returns a map between dimension identifiers and lists of dimension item
     * identifiers.
     * 
     * @param params the data query parameters.
     * @return a map.
     */
    private Map<String, List<String>> getDimensionItems( EventQueryParams params )
    {
        Calendar calendar = PeriodType.getCalendar();
        
        List<String> periodUids = calendar.isIso8601() ?
            getDimensionalItemIds( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) :
            getLocalPeriodIdentifiers( params.getDimensionOrFilterItems( PERIOD_DIM_ID ), calendar );        

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
                dimensionItems.put( item.getItemId(), item.getOptionSetFilterItemsOrAll() );
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
}
