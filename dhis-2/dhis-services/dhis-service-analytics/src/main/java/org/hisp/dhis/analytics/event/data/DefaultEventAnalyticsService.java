package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;

import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.DENOMINATOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.FACTOR_ID;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_ID;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.DENOMINATOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.FACTOR_HEADER_NAME;
import static org.hisp.dhis.analytics.DataQueryParams.VALUE_HEADER_NAME;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.Rectangle;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventDataQueryService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.NameableObjectUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.system.database.DatabaseInfo;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.Timer;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DefaultEventAnalyticsService
    implements EventAnalyticsService
{
    @Autowired
    private EventAnalyticsManager analyticsManager;
    
    @Autowired
    private EventDataQueryService eventDataQueryService;

    @Autowired
    private AnalyticsSecurityManager securityManager;
    
    @Autowired
    private EventQueryPlanner queryPlanner;

    @Autowired
    private DatabaseInfo databaseInfo;
    
    // -------------------------------------------------------------------------
    // EventAnalyticsService implementation
    // -------------------------------------------------------------------------

    // TODO use ValueType for type in grid headers
    // TODO order event analytics tables on execution date to avoid default
    // TODO sorting in queries

    @Override
    public Grid getAggregatedEventData( EventQueryParams params )
    {
        securityManager.decideAccess( params );
        
        queryPlanner.validate( params );
        
        params.removeProgramIndicatorItems(); // Not supported as items for aggregate
        
        Grid grid = new ListGrid();

        int maxLimit = queryPlanner.getMaxLimit();
        
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
                grid.addHeader( new GridHeader( DimensionalObject.DATA_COLLAPSED_DIM_ID, DataQueryParams.DISPLAY_NAME_DATA_X, String.class.getName(), false, true ) );
            }
            else
            {
                for ( QueryItem item : params.getItems() )
                {
                    String legendSet = item.hasLegendSet() ? item.getLegendSet().getUid() : null;
    
                    grid.addHeader( new GridHeader( item.getItem().getUid(), item.getItem().getName(), item.getTypeAsString(), false, true, item.getOptionSetUid(), legendSet ) );
                }
            }
            
            for ( DimensionalObject dimension : params.getDimensions() )
            {
                grid.addHeader( new GridHeader( dimension.getDimension(), dimension.getDisplayName(), String.class.getName(), false, true ) );
            }
    
            grid.addHeader( new GridHeader( VALUE_ID, VALUE_HEADER_NAME, Double.class.getName(), false, false ) );

            if ( params.isIncludeNumDen() )
            {
                grid.addHeader( new GridHeader( NUMERATOR_ID, NUMERATOR_HEADER_NAME, Double.class.getName(), false, false ) );
                grid.addHeader( new GridHeader( DENOMINATOR_ID, DENOMINATOR_HEADER_NAME, Double.class.getName(), false, false ) );
                grid.addHeader( new GridHeader( FACTOR_ID, FACTOR_HEADER_NAME, Double.class.getName(), false, false ) );
            }

            // -----------------------------------------------------------------
            // Data
            // -----------------------------------------------------------------

            Timer timer = new Timer().start().disablePrint();
    
            List<EventQueryParams> queries = queryPlanner.planAggregateQuery( params );
    
            timer.getSplitTime( "Planned event query, got partitions: " + params.getPartitions() );
    
            for ( EventQueryParams query : queries )
            {
                analyticsManager.getAggregatedEventData( query, grid, maxLimit );
            }
            
            timer.getTime( "Got aggregated events" );
            
            if ( maxLimit > 0 && grid.getHeight() > maxLimit )
            {
                throw new IllegalQueryException( "Number of rows produced by query is larger than the max limit: " + maxLimit );
            }

            // -----------------------------------------------------------------
            // Limit and sort, done again due to potential multiple partitions
            // -----------------------------------------------------------------

            if ( params.hasSortOrder() )
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
        
        if ( !params.isSkipMeta() )
        {
            Map<Object, Object> metaData = new HashMap<>();

            Map<String, String> uidNameMap = getUidNameMap( params );
            
            metaData.put( AnalyticsMetaDataKey.NAMES.getKey(), uidNameMap );
            metaData.put( PERIOD_DIM_ID, getDimensionalItemIds( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) );
            metaData.put( ORGUNIT_DIM_ID, getDimensionalItemIds( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) ) );

            User user = securityManager.getCurrentUser( params );

            List<OrganisationUnit> organisationUnits = asTypedList( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) );
            Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;
            
            if ( params.isHierarchyMeta() )
            {
                metaData.put( AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( organisationUnits, roots ) );
            }

            if ( params.isShowHierarchy() )
            {
                metaData.put( AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY.getKey(), getParentNameGraphMap( organisationUnits, roots, true ) );
            }

            grid.setMetaData( metaData );
        }

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
        securityManager.decideAccess( params );
        
        queryPlanner.validate( params );

        params.replacePeriodsWithStartEndDates();
        
        Grid grid = new ListGrid();
        
        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        grid.addHeader( new GridHeader( ITEM_EVENT, "Event", String.class.getName(), false, true ) );
        grid.addHeader( new GridHeader( ITEM_PROGRAM_STAGE, "Program stage", String.class.getName(), false, true ) );
        grid.addHeader( new GridHeader( ITEM_EXECUTION_DATE, "Event date", String.class.getName(), false, true ) );
        grid.addHeader( new GridHeader( ITEM_LONGITUDE, "Longitude", String.class.getName(), false, true ) );
        grid.addHeader( new GridHeader( ITEM_LATITUDE, "Latitude", String.class.getName(), false, true ) );
        grid.addHeader( new GridHeader( ITEM_ORG_UNIT_NAME, "Organisation unit name", String.class.getName(), false, true ) );
        grid.addHeader( new GridHeader( ITEM_ORG_UNIT_CODE, "Organisation unit code", String.class.getName(), false, true ) );

        for ( DimensionalObject dimension : params.getDimensions() )
        {
            grid.addHeader( new GridHeader( dimension.getDimension(), dimension.getDisplayName(), String.class.getName(), false, true ) );
        }

        for ( QueryItem item : params.getItems() )
        {
            grid.addHeader( new GridHeader( item.getItem().getUid(), item.getItem().getName(), item.getTypeAsString(), false, true, item.getOptionSetUid(), item.getLegendSetUid() ) );
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
                count += analyticsManager.getEventCount( params );
            }
    
            analyticsManager.getEvents( params, grid, queryPlanner.getMaxLimit() );
    
            timer.getTime( "Got events " + grid.getHeight() );
        }
        
        // ---------------------------------------------------------------------
        // Meta-data
        // ---------------------------------------------------------------------

        Map<Object, Object> metaData = new HashMap<>();

        Map<String, String> uidNameMap = getUidNameMap( params );

        metaData.put( AnalyticsMetaDataKey.NAMES.getKey(), uidNameMap );

        User user = securityManager.getCurrentUser( params );

        Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;
        
        if ( params.isHierarchyMeta() )
        {
            metaData.put( AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( asTypedList( 
                params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) ), roots ) );
        }

        if ( params.isPaging() )
        {
            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            metaData.put( AnalyticsMetaDataKey.PAGER.getKey(), pager );
        }

        grid.setMetaData( metaData );

        return grid;
    }

    @Override
    public Grid getEventClusters( EventQueryParams params )
    {
        if ( !databaseInfo.isSpatialSupport() )
        {
            throw new IllegalQueryException( "Spatial database support is not enabled" );
        }
        
        securityManager.decideAccess( params );
        
        queryPlanner.validate( params );

        params.replacePeriodsWithStartEndDates();
        
        Grid grid = new ListGrid();
        
        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        grid.addHeader( new GridHeader( ITEM_COUNT, "Count", Long.class.getName(), false, false ) );
        grid.addHeader( new GridHeader( ITEM_CENTER, "Center", String.class.getName(), false, false ) );
        grid.addHeader( new GridHeader( ITEM_EXTENT, "Extent", String.class.getName(), false, false ) );
        grid.addHeader( new GridHeader( ITEM_POINTS, "Points", String.class.getName(), false, false ) );

        // ---------------------------------------------------------------------
        // Data
        // ---------------------------------------------------------------------

        params = queryPlanner.planEventQuery( params );

        analyticsManager.getEventClusters( params, grid, queryPlanner.getMaxLimit() );
        
        return grid;
    }
    
    @Override
    public Rectangle getRectangle( EventQueryParams params )
    {
        if ( !databaseInfo.isSpatialSupport() )
        {
            throw new IllegalQueryException( "Spatial database support is not enabled" );
        }
        
        securityManager.decideAccess( params );
        
        queryPlanner.validate( params );

        params.replacePeriodsWithStartEndDates();
        
        params = queryPlanner.planEventQuery( params );

        return analyticsManager.getRectangle( params );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private Map<String, String> getUidNameMap( EventQueryParams params )
    {
        Map<String, String> map = new HashMap<>();

        Program program = params.getProgram();
        ProgramStage stage = params.getProgramStage();

        map.put( program.getUid(), program.getName() );

        if ( stage != null )
        {
            map.put( stage.getUid(), stage.getName() );
        }
        else
        {
            for ( ProgramStage st : program.getProgramStages() )
            {
                map.put( st.getUid(), st.getName() );
            }
        }

        if ( params.hasValueDimension() )
        {
            map.put( params.getValue().getUid(), params.getValue().getDisplayProperty( params.getDisplayProperty() ) );
        }
        
        map.putAll( getUidNameMap( params.getItems(), params.getDisplayProperty() ) );
        map.putAll( getUidNameMap( params.getItemFilters(), params.getDisplayProperty() ) );
        map.putAll( getUidNameMap( params.getDimensions(), params.isHierarchyMeta(), params.getDisplayProperty() ) );
        map.putAll( getUidNameMap( params.getFilters(), params.isHierarchyMeta(), params.getDisplayProperty() ) );
        map.putAll( IdentifiableObjectUtils.getUidNameMap( params.getLegends() ) );
        
        return map;
    }
    
    private Map<String, String> getUidNameMap( List<QueryItem> queryItems, DisplayProperty displayProperty )
    {
        Map<String, String> map = new HashMap<>();
        
        for ( QueryItem item : queryItems )
        {
            map.put( item.getItem().getUid(), item.getItem().getDisplayProperty( displayProperty ) );
        }
        
        return map;
    }

    private Map<String, String> getUidNameMap( List<DimensionalObject> dimensions, boolean hierarchyMeta, DisplayProperty displayProperty )
    {
        Map<String, String> map = new HashMap<>();

        for ( DimensionalObject dimension : dimensions )
        {
            boolean hierarchy = hierarchyMeta && DimensionType.ORGANISATION_UNIT.equals( dimension.getDimensionType() );

            for ( DimensionalItemObject object : dimension.getItems() )
            {
                Set<DimensionalItemObject> objects = new HashSet<>();
                objects.add( object );
                
                if ( hierarchy )
                {
                    OrganisationUnit unit = (OrganisationUnit) object;
                    objects.addAll( unit.getAncestors() );
                }
                
                map.putAll( NameableObjectUtils.getUidDisplayPropertyMap( objects, displayProperty ) );
            }
            
            map.put( dimension.getDimension(), dimension.getDisplayProperty( displayProperty ) );
        }

        return map;
    }
}
