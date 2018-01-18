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
import org.hisp.dhis.analytics.EventReportDimensionalItem;
import org.hisp.dhis.analytics.MetadataItem;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.event.EnrollmentAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryValidator;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.legend.Legend;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.option.Option;
import org.hisp.dhis.option.OptionSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.Timer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import static org.hisp.dhis.analytics.AnalyticsMetaDataKey.*;
import static org.hisp.dhis.analytics.DataQueryParams.*;
import static org.hisp.dhis.common.DimensionalObject.*;
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

    private final List<Option> booleanOptions = new ArrayList<>( );

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

    DefaultEventAnalyticsService ()
    {
        Option t = new Option( );
        t.setCode( "1" );
        t.setName( "Yes" );

        Option f = new Option( );
        f.setCode( "0" );
        f.setName( "No" );

        booleanOptions.add( t );
        booleanOptions.add( f );
    }

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
     * For event reports each option for a dimension will be an {@link EventReportDimensionalItem} and all permutations
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

        Map<String, List<EventReportDimensionalItem>> tableColumns = new LinkedHashMap<>();
        if ( columns != null )
        {
            for ( String dimension : columns )
            {
                getEventDataObjects( grid, params, tableColumns, dimension );
            }
        }

        Map<String, List<EventReportDimensionalItem>> tableRows = new LinkedHashMap<>();
        List<String> rowDimensions = new ArrayList<>();
        if ( rows != null )
        {
            for ( String dimension : rows )
            {
                rowDimensions.add( dimension );
                getEventDataObjects( grid, params, tableRows, dimension );
            }
        }

        List<Map<String, EventReportDimensionalItem>> rowPermutations = generateEventDataPermutations( tableRows );
        List<Map<String, EventReportDimensionalItem>> columnPermutations = generateEventDataPermutations( tableColumns );

        return generateOutputGrid( grid,  params, rowPermutations, columnPermutations, rowDimensions );
    }

    /**
     * Generate grid based on input parameters
     *
     * @param grid the result grid
     * @param params the event query parameters.
     * @param rowPermutations the row permutations
     * @param columnPermutations the column permutations
     * @param rowDimensions the row dimensions
     * @return grid with table layout
     */
    private Grid generateOutputGrid( Grid grid, EventQueryParams params, List<Map<String, EventReportDimensionalItem>> rowPermutations, List<Map<String, EventReportDimensionalItem>> columnPermutations, List<String> rowDimensions )
    {
        Grid outputGrid = new ListGrid();
        outputGrid.setTitle( IdentifiableObjectUtils.join( params.getFilterItems() ) );

        for ( String row : rowDimensions )
        {
            MetadataItem metadataItem = (MetadataItem) ((HashMap<String, Object>) grid.getMetaData()
                .get( ITEMS.getKey() )).get( row );

            String name = StringUtils.defaultIfEmpty( metadataItem.getName(), row );
            String col = StringUtils.defaultIfEmpty( COLUMN_NAMES.get( row ), row );

            outputGrid
                .addHeader( new GridHeader( name, col, ValueType.TEXT, String.class.getName(), false, true ) );
        }

        columnPermutations.forEach( permutation -> {
            StringBuilder builder = new StringBuilder();

            for ( Map.Entry<String, EventReportDimensionalItem> entry : permutation.entrySet() )
            {
                String key = entry.getKey();
                EventReportDimensionalItem value = entry.getValue();

                if ( !key.equals( ORGUNIT_DIM_ID ) && !key.equals( PERIOD_DIM_ID ) )
                {
                    builder.append( key ).append( SPACE );
                }
                builder.append( value.getDisplayProperty( params.getDisplayProperty() ) )
                    .append( DASH_PRETTY_SEPARATOR );
            }

            String display = builder.length() > 0 ?
                builder.substring( 0, builder.lastIndexOf( DASH_PRETTY_SEPARATOR ) ) :
                TOTAL_COLUMN_PRETTY_NAME;

            outputGrid.addHeader( new GridHeader( display, display,
                ValueType.NUMBER, Double.class.getName(), false, false ) );
        } );

        Map<String, Object> valueMap = getAggregatedEventDataMapping( grid );

        for ( Map<String, EventReportDimensionalItem> rowCombination : rowPermutations )
        {
            outputGrid.addRow();
            List<List<String>> ids = new ArrayList<>();
            Map<String, EventReportDimensionalItem> displayObjects = new HashMap<>();

            boolean fillDisplayList = true;

            for ( Map<String, EventReportDimensionalItem> columnCombination : columnPermutations )
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

            boolean hasValues = false;
            for ( List<String> idList : ids )
            {
                Collections.sort( idList );

                String key = StringUtils.join( idList, DIMENSION_SEP );
                Object value = valueMap.get( key );
                hasValues = hasValues || value != null;

                outputGrid.addValue( value );
            }

            if ( !hasValues )
            {
                outputGrid.removeCurrentWriteRow();
            }
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
        Map<String, List<EventReportDimensionalItem>> table,
        String dimension )
        throws Exception
    {
        List<EventReportDimensionalItem> objects = params.getEventReportDimensionalItemArrayExploded( dimension );

        if ( objects.size() == 0 )
        {
            ValueType valueType;
            OptionSet optionSet;
            LegendSet legendSet;
            String parentUid;
            String displayName;

            DataElement dataElement = dataElementService.getDataElement( dimension );

            if ( dataElement != null )
            {
                valueType = dataElement.getValueType();
                optionSet = dataElement.getOptionSet();
                legendSet = dataElement.getLegendSet();
                parentUid = dataElement.getUid();
                displayName = dataElement.getDisplayName();
            }
            else
            {
                TrackedEntityAttribute trackedEntityAttribute = trackedEntityAttributeService
                    .getTrackedEntityAttribute( dimension );
                valueType = trackedEntityAttribute.getValueType();
                optionSet = trackedEntityAttribute.getOptionSet();
                legendSet = trackedEntityAttribute.getLegendSet();
                parentUid = trackedEntityAttribute.getUid();
                displayName = trackedEntityAttribute.getDisplayName();
            }

            if ( valueType == null )
            {
                throw new Exception( "Supplied data dimension is invalid" );
            }

            if ( valueType == ValueType.BOOLEAN )
            {
                List<EventReportDimensionalItem> options = new ArrayList<>();

                for ( Option boolOption : booleanOptions )
                {
                    options.add( new EventReportDimensionalItem( boolOption, parentUid ) );
                }
                
                objects = options;
            }
            else if ( optionSet != null )
            {
                for ( Option option : optionSet.getOptions() )
                {
                    objects.add( new EventReportDimensionalItem( option, parentUid ) );
                }
            }
            else if ( legendSet != null )
            {
                List<String> legendOptions = (List<String>) ((HashMap<String, Object>) grid.getMetaData()
                    .get( DIMENSIONS.getKey() )).get( dimension );

                if ( legendOptions.isEmpty() )
                {
                    List<Legend> legends = legendSet.getSortedLegends();

                    for ( Legend legend : legends )
                    {
                        for ( int i = legend.getStartValue().intValue(); i < legend.getEndValue(); i++ )
                        {
                            objects.add( new EventReportDimensionalItem( new Option( String.valueOf( i ), String.valueOf( i ) ), parentUid ) );
                        }
                    }
                }
                else
                {
                    for ( String legend : legendOptions )
                    {
                        MetadataItem metadataItem = (MetadataItem) ((HashMap<String, Object>) grid.getMetaData()
                            .get( ITEMS.getKey() )).get( legend );

                        objects.add( new EventReportDimensionalItem( new Option( metadataItem.getName(), legend ), parentUid ) );
                    }
                }
            }

            table.put( displayName, objects );
        }
        else
        {
            table.put( dimension, objects );
        }
    }

    /**
     * Get all combinations from map. Fill the result into list.
     * @param map the map with all values
     * @param list the resulting list
     */
    public static void getCombinations( Map<String, List<EventReportDimensionalItem>> map,
        List<Map<String, EventReportDimensionalItem>> list )
    {
        recurse( map, new LinkedList<>( map.keySet() ).listIterator(), new TreeMap<>(), list );
    }

    /**
     * A recursive method which finds all permutations of the elements in map.
     *
     * @param map the map with all values
     * @param iter iterator with keys
     * @param cur the current map
     * @param list the resulting list
     */
    private static void recurse( Map<String, List<EventReportDimensionalItem>> map, ListIterator<String> iter,
        TreeMap<String, EventReportDimensionalItem> cur, List<Map<String, EventReportDimensionalItem>> list )
    {
        if ( !iter.hasNext() )
        {
            Map<String, EventReportDimensionalItem> entry = new HashMap<>();

            for ( String key : cur.keySet() )
            {
                entry.put( key, cur.get( key ) );
            }

            list.add( entry );
        }
        else
        {
            String key = iter.next();
            List<EventReportDimensionalItem> set = map.get( key );

            for ( EventReportDimensionalItem value : set )
            {
                cur.put( key, value );
                recurse( map, iter, cur, list );
                cur.remove( key );
            }

            iter.previous();
        }
    }

    /**
     *  Get all permutations for event report dimensions.
     *
     * @param dataOptionMap the map to generate permutations from
     * @return a list of a map with a permutations
     */
    private List<Map<String, EventReportDimensionalItem>> generateEventDataPermutations(
        Map<String, List<EventReportDimensionalItem>> dataOptionMap )
    {
        List<Map<String, EventReportDimensionalItem>> list = new LinkedList<>();
        getCombinations( dataOptionMap, list );

        return list;
    }

    /**
     * Get event data mapping for values.
     * 
     * @param grid the grid to collect data from
     * @return map with key and values
     */
    private Map<String, Object> getAggregatedEventDataMapping( Grid grid )
    {
        Map<String, Object> map = new HashMap<>();

        int metaCols = grid.getWidth() - 1;
        int valueIndex = grid.getWidth() - 1;

        for ( List<Object> row : grid.getRows() )
        {
            List<String> ids = new ArrayList<>();

            for ( int index = 0; index < metaCols; index++ )
            {
                Object id = row.get( index );

                if ( id != null )
                {
                    ids.add( (String) row.get( index ) );
                }
            }

            Collections.sort( ids );

            String key = StringUtils.join( ids, DIMENSION_SEP );
            Object value = row.get( valueIndex );

            map.put( key, value );
        }

        return map;
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
            Calendar calendar = PeriodType.getCalendar();
            
            List<String> periodUids = calendar.isIso8601() ?
                getDimensionalItemIds( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) :
                getLocalPeriodIdentifiers( params.getDimensionOrFilterItems( PERIOD_DIM_ID ), calendar );

            Map<String, Object> metaData = new HashMap<>();

            Map<String, String> uidNameMap = AnalyticsUtils.getUidNameMap( params );
            boolean includeMetadataDetails = params.isIncludeMetadataDetails();

            if ( params.getApiVersion().ge( DhisApiVersion.V26 ) )
            {
                Map<String, Object> metadataItemMap = AnalyticsUtils.getDimensionMetadataItemMap( params );

                if ( params.hasValueDimension() )
                {
                    metadataItemMap.put( params.getValue().getUid(), params.getValue().getDisplayProperty( params.getDisplayProperty() ) );
                }

                params.getLegends().forEach( legend -> {
                    metadataItemMap.put( legend.getUid(), new MetadataItem( legend.getDisplayName(), includeMetadataDetails ? legend.getUid() : null, includeMetadataDetails ? legend.getCode() : null ) );
                } );

                params.getOptions().forEach( option -> {
                    metadataItemMap.put( option.getUid(), new MetadataItem( option.getDisplayName(), includeMetadataDetails ? option.getUid() : null, includeMetadataDetails ? option.getCode() : null ) );
                } );

                params.getItems().forEach( item -> {
                    metadataItemMap.put( item.getItemId(),  new MetadataItem( item.getItem().getDisplayName(), includeMetadataDetails ? item.getItem() : null ) );
                } );

                metaData.put( ITEMS.getKey(), metadataItemMap );
            }
            else
            {
                metaData.put( NAMES.getKey(), uidNameMap );
            }

            Map<String, Object> dimensionItems = new HashMap<>();
            
            dimensionItems.put( PERIOD_DIM_ID, periodUids );

            for ( DimensionalObject dim : params.getDimensionsAndFilters() )
            {
                if ( !metaData.keySet().contains( dim.getDimension() ) )
                {
                    dimensionItems.put( dim.getDimension(), getDimensionalItemIds( dim.getItems() ) );
                }
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

            if ( params.getApiVersion().ge( DhisApiVersion.V26 ) )
            {
                metaData.put( DIMENSIONS.getKey(), dimensionItems );
            }
            else
            {
                metaData.putAll( dimensionItems );
            }

            User user = securityManager.getCurrentUser( params );

            List<OrganisationUnit> organisationUnits = asTypedList( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) );
            
            Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;
            
            if ( params.isHierarchyMeta() )
            {
                metaData.put( ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( organisationUnits, roots ) );
            }

            if ( params.isShowHierarchy() )
            {
                metaData.put( ORG_UNIT_NAME_HIERARCHY.getKey(), getParentNameGraphMap( organisationUnits, roots, true ) );
            }

            grid.setMetaData( metaData );
        }
    }
}
