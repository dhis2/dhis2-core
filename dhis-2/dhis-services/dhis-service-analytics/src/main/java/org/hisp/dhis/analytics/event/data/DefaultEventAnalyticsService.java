package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import static org.hisp.dhis.analytics.AnalyticsService.NAMES_META_KEY;
import static org.hisp.dhis.analytics.AnalyticsService.OU_HIERARCHY_KEY;
import static org.hisp.dhis.analytics.AnalyticsService.OU_NAME_HIERARCHY_KEY;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_NAME_SEP;
import static org.hisp.dhis.common.DimensionalObject.ITEM_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionFromParam;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItemsFromParam;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.common.NameableObjectUtils.asTypedList;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.EventOutputType;
import org.hisp.dhis.analytics.SortOrder;
import org.hisp.dhis.analytics.event.EventAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DisplayProperty;
import org.hisp.dhis.common.EventAnalyticalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.NameableObject;
import org.hisp.dhis.common.NameableObjectUtils;
import org.hisp.dhis.common.Pager;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.i18n.I18nFormat;
import org.hisp.dhis.legend.LegendService;
import org.hisp.dhis.legend.LegendSet;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramIndicatorService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.Timer;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class DefaultEventAnalyticsService
    implements EventAnalyticsService
{
    private static final String ITEM_EVENT = "psi";
    private static final String ITEM_PROGRAM_STAGE = "ps";
    private static final String ITEM_EXECUTION_DATE = "eventdate";
    private static final String ITEM_LONGITUDE = "longitude";
    private static final String ITEM_LATITUDE = "latitude";
    private static final String ITEM_ORG_UNIT_NAME = "ouname";
    private static final String ITEM_ORG_UNIT_CODE = "oucode";
    private static final String COL_NAME_EVENTDATE = "executiondate";

    private static final List<String> SORTABLE_ITEMS = Lists.newArrayList( 
        ITEM_EXECUTION_DATE, ITEM_ORG_UNIT_NAME, ITEM_ORG_UNIT_CODE );

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;
    
    @Autowired
    private ProgramIndicatorService programIndicatorService;
    
    @Autowired
    private LegendService legendService;

    @Autowired
    private EventAnalyticsManager analyticsManager;

    @Autowired
    private AnalyticsSecurityManager securityManager;
    
    @Autowired
    private EventQueryPlanner queryPlanner;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private CurrentUserService currentUserService;
    
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
    
            grid.addHeader( new GridHeader( "value", "Value", Double.class.getName(), false, false ) );

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
    
            metaData.put( NAMES_META_KEY, uidNameMap );
            metaData.put( PERIOD_DIM_ID, getUids( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) );
            metaData.put( ORGUNIT_DIM_ID, getUids( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) ) );

            User user = currentUserService.getCurrentUser();

            List<OrganisationUnit> organisationUnits = asTypedList( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ), OrganisationUnit.class );
            Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;
            
            if ( params.isHierarchyMeta() )
            {
                metaData.put( OU_HIERARCHY_KEY, getParentGraphMap( organisationUnits, roots ) );
            }

            if ( params.isShowHierarchy() )
            {
                metaData.put( OU_NAME_HIERARCHY_KEY, getParentNameGraphMap( organisationUnits, roots, true, params.getDisplayProperty() ) );
            }

            grid.setMetaData( metaData );
        }

        return grid;
    }
    
    @Override
    public Grid getAggregatedEventData( AnalyticalObject object, I18nFormat format )
    {
        EventQueryParams params = getFromAnalyticalObject( (EventAnalyticalObject) object, format );
        
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

        int count = 0;

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

        metaData.put( NAMES_META_KEY, uidNameMap );

        User user = currentUserService.getCurrentUser();

        Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;
        
        if ( params.isHierarchyMeta() )
        {
            metaData.put( OU_HIERARCHY_KEY, getParentGraphMap( asTypedList( 
                params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ), OrganisationUnit.class ), roots ) );
        }

        if ( params.isPaging() )
        {
            Pager pager = new Pager( params.getPageWithDefault(), count, params.getPageSizeWithDefault() );
            metaData.put( AnalyticsService.PAGER_META_KEY, pager );
        }

        grid.setMetaData( metaData );

        return grid;
    }

    @Override
    public EventQueryParams getFromUrl( String program, String stage, String startDate, String endDate,
        Set<String> dimension, Set<String> filter, String value, AggregationType aggregationType, boolean skipMeta, boolean skipData, boolean skipRounding, 
        boolean completedOnly, boolean hierarchyMeta, boolean showHierarchy, SortOrder sortOrder, Integer limit, EventOutputType outputType, boolean collapseDataDimensions, 
        boolean aggregateData, DisplayProperty displayProperty, String userOrgUnit, I18nFormat format )
    {
        EventQueryParams params = getFromUrl( program, stage, startDate, endDate, dimension, filter, null, null, null,
            skipMeta, skipData, completedOnly, hierarchyMeta, false, displayProperty, userOrgUnit, null, null, format );
                
        params.setValue( getValueDimension( value ) );
        params.setAggregationType( aggregationType );
        params.setSkipRounding( skipRounding );
        params.setShowHierarchy( showHierarchy );
        params.setSortOrder( sortOrder );
        params.setLimit( limit );
        params.setOutputType( MoreObjects.firstNonNull( outputType, EventOutputType.EVENT ) );
        params.setCollapseDataDimensions( collapseDataDimensions );
        params.setAggregateData( aggregateData );

        return params;
    }

    @Override
    public EventQueryParams getFromUrl( String program, String stage, String startDate, String endDate,
        Set<String> dimension, Set<String> filter, String ouMode, Set<String> asc, Set<String> desc,
        boolean skipMeta, boolean skipData, boolean completedOnly, boolean hierarchyMeta, boolean coordinatesOnly, 
        DisplayProperty displayProperty, String userOrgUnit, Integer page, Integer pageSize, I18nFormat format )
    {
        EventQueryParams params = new EventQueryParams();
        
        List<OrganisationUnit> userOrgUnits = analyticsService.getUserOrgUnits( userOrgUnit );

        Program pr = programService.getProgram( program );

        if ( pr == null )
        {
            throw new IllegalQueryException( "Program does not exist: " + program );
        }

        ProgramStage ps = programStageService.getProgramStage( stage );

        if ( StringUtils.isNotEmpty( stage ) && ps == null )
        {
            throw new IllegalQueryException( "Program stage is specified but does not exist: " + stage );
        }

        Date start = null;
        Date end = null;

        if ( startDate != null && endDate != null )
        {
            try
            {
                start = DateUtils.getMediumDate( startDate );
                end = DateUtils.getMediumDate( endDate );
            }
            catch ( RuntimeException ex )
            {
                throw new IllegalQueryException( "Start date or end date is invalid: " + startDate + " - " + endDate );
            }
        }

        if ( dimension != null )
        {
            for ( String dim : dimension )
            {
                String dimensionId = getDimensionFromParam( dim );
                List<String> items = getDimensionItemsFromParam( dim );                
                DimensionalObject dimObj = analyticsService.getDimension( dimensionId, items, null, userOrgUnits, format, true );
                
                if ( dimObj != null )
                {
                    params.getDimensions().add( dimObj );
                }
                else
                {
                    params.getItems().add( getQueryItem( dim ) );
                }
            }
        }

        if ( filter != null )
        {
            for ( String dim : filter )
            {
                String dimensionId = getDimensionFromParam( dim );
                List<String> items = getDimensionItemsFromParam( dim );                
                DimensionalObject dimObj = analyticsService.getDimension( dimensionId, items, null, userOrgUnits, format, true );
                
                if ( dimObj != null )
                {
                    params.getFilters().add( dimObj );
                }
                else
                {
                    params.getItemFilters().add( getQueryItem( dim ) );
                }
            }
        }

        if ( asc != null )
        {
            for ( String sort : asc )
            {
                params.getAsc().add( getSortItem( sort ) );
            }
        }

        if ( desc != null )
        {
            for ( String sort : desc )
            {
                params.getDesc().add( getSortItem( sort ) );
            }
        }

        params.setProgram( pr );
        params.setProgramStage( ps );
        params.setStartDate( start );
        params.setEndDate( end );
        params.setOrganisationUnitMode( ouMode );
        params.setSkipMeta( skipMeta );
        params.setSkipData( skipData );
        params.setCompletedOnly( completedOnly );
        params.setHierarchyMeta( hierarchyMeta );
        params.setCoordinatesOnly( coordinatesOnly );
        params.setDisplayProperty( displayProperty );
        params.setPage( page );
        params.setPageSize( pageSize );
        
        return params;
    }

    @Override
    public EventQueryParams getFromAnalyticalObject( EventAnalyticalObject object, I18nFormat format )
    {        
        EventQueryParams params = new EventQueryParams();

        if ( object != null )
        {
            Date date = object.getRelativePeriodDate();
            
            object.populateAnalyticalProperties();

            for ( DimensionalObject dimension : ListUtils.union( object.getColumns(), object.getRows() ) )
            {
                DimensionalObject dimObj = analyticsService.
                    getDimension( dimension.getDimension(), getUids( dimension.getItems() ), date, null, format, true );
                
                if ( dimObj != null )
                {
                    params.getDimensions().add( dimObj );
                }
                else
                {
                    params.getItems().add( getQueryItem( dimension.getDimension(), dimension.getFilter() ) );
                }
            }
            
            for ( DimensionalObject filter : object.getFilters() )
            {
                DimensionalObject dimObj = analyticsService.
                    getDimension( filter.getDimension(), getUids( filter.getItems() ), date, null, format, true );
                
                if ( dimObj != null )
                {
                    params.getFilters().add( dimObj );
                }
                else
                {
                    params.getItemFilters().add( getQueryItem( filter.getDimension(), filter.getFilter() ) );
                }
            }

            params.setProgram( object.getProgram() );
            params.setProgramStage( object.getProgramStage() );
            params.setStartDate( object.getStartDate() );
            params.setEndDate( object.getEndDate() );
            params.setValue( object.getValue() );
            params.setOutputType( object.getOutputType() );
        }
        
        return params;
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private QueryItem getQueryItem( String dimension, String filter )
    {
        if ( filter != null )
        {
            dimension += DIMENSION_NAME_SEP + filter;
        }
                
        return getQueryItem( dimension );
    }
    
    private QueryItem getQueryItem( String dimensionString )
    {
        String[] split = dimensionString.split( DIMENSION_NAME_SEP );

        if ( split == null || ( split.length % 2 != 1 ) )
        {
            throw new IllegalQueryException( "Query item or filter is invalid: " + dimensionString );
        }
        
        QueryItem queryItem = getQueryItemFromDimension( split[0] );
        
        if ( split.length > 1 ) // Filters specified
        {
            for ( int i = 1; i < split.length; i += 2 )
            {
                QueryOperator operator = QueryOperator.fromString( split[i] );
                QueryFilter filter = new QueryFilter( operator, split[i+1] );
                queryItem.getFilters().add( filter );
            }
        }
        
        return queryItem;
    }
    
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
            boolean hierarchy = hierarchyMeta && DimensionType.ORGANISATIONUNIT.equals( dimension.getDimensionType() );

            for ( NameableObject object : dimension.getItems() )
            {
                Set<NameableObject> objects = new HashSet<>();
                objects.add( object );
                
                if ( hierarchy )
                {
                    OrganisationUnit unit = (OrganisationUnit) object;
                    objects.addAll( unit.getParents() );
                }
                
                map.putAll( NameableObjectUtils.getUidDisplayPropertyMap( objects, displayProperty ) );
            }
            
            map.put( dimension.getDimension(), dimension.getDisplayProperty( displayProperty ) );
        }

        return map;
    }

    private String getSortItem( String item )
    {
        if ( !SORTABLE_ITEMS.contains( item.toLowerCase() ) && getQueryItem( item ) == null )
        {
            throw new IllegalQueryException( "Descending sort item is invalid: " + item );
        }

        item = ITEM_EXECUTION_DATE.equalsIgnoreCase( item ) ? COL_NAME_EVENTDATE : item;

        return item;
    }

    private QueryItem getQueryItemFromDimension( String dimension )
    {
        String[] split = dimension.split( ITEM_SEP );

        String item = split[0];

        LegendSet legendSet = split.length > 1 && split[1] != null ? legendService.getLegendSet( split[1] ) : null;
        
        DataElement de = dataElementService.getDataElement( item );

        if ( de != null ) //TODO check if part of program
        {
            return new QueryItem( de, legendSet, de.getValueType(), de.getAggregationType(), de.getOptionSet() );
        }

        TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute( item );

        if ( at != null )
        {
            return new QueryItem( at, legendSet, at.getValueType(), at.getAggregationType(), at.getOptionSet() );
        }
        
        ProgramIndicator pi = programIndicatorService.getProgramIndicatorByUid( item );
        
        if ( pi != null )
        {
            return new QueryItem( pi, legendSet, ValueType.NUMBER, pi.getAggregationType(), null );
        }

        throw new IllegalQueryException( "Item identifier does not reference any data element or attribute part of the program: " + item );
    }
    
    private DimensionalObject getValueDimension( String value )
    {
        if ( value == null )
        {
            return null;
        }
        
        DataElement de = dataElementService.getDataElement( value );
        
        if ( de != null && de.isNumericType() )
        {
            return de;
        }
        
        TrackedEntityAttribute at = attributeService.getTrackedEntityAttribute( value );
        
        if ( at != null && at.isNumericType() )
        {
            return at;
        }
        
        throw new IllegalQueryException( "Value identifier does not reference any data element or attribute which are numeric type and part of the program: " + value );        
    }
}
