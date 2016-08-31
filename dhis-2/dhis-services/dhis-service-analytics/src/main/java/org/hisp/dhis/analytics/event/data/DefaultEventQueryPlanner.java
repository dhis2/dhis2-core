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

import static org.hisp.dhis.analytics.AnalyticsTableManager.EVENT_ANALYTICS_TABLE_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Lars Helge Overland
 */
public class DefaultEventQueryPlanner
    implements EventQueryPlanner
{
    private static final Log log = LogFactory.getLog( DefaultEventQueryPlanner.class );
    
    @Autowired
    private QueryPlanner queryPlanner;

    @Autowired
    private SystemSettingManager systemSettingManager;
    
    @Autowired
    private PartitionManager partitionManager;

    // -------------------------------------------------------------------------
    // EventQueryPlanner implementation
    // -------------------------------------------------------------------------

    @Override
    public void validate( EventQueryParams params )
        throws IllegalQueryException, MaintenanceModeException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }
        
        queryPlanner.validateMaintenanceMode();
        
        if ( !params.hasOrganisationUnits() )
        {
            violation = "At least one organisation unit must be specified";
        }

        if ( !params.getDuplicateDimensions().isEmpty() )
        {
            violation = "Dimensions cannot be specified more than once: " + params.getDuplicateDimensions();
        }
        
        if ( !params.getDuplicateQueryItems().isEmpty() )
        {
            violation = "Query items cannot be specified more than once: " + params.getDuplicateQueryItems();
        }
        
        if ( params.hasValueDimension() && params.getDimensionalObjectItems().contains( params.getValue() ) )
        {
            violation = "Value dimension cannot also be specified as an item or item filter";
        }
        
        if ( params.hasAggregationType() && !( params.hasValueDimension() || params.isAggregateData() ) )
        {
            violation = "Value dimension or aggregate data must be specified when aggregation type is specified";
        }
        
        if ( !params.hasPeriods() && ( params.getStartDate() == null || params.getEndDate() == null ) )
        {
            violation = "Start and end date or at least one period must be specified";
        }
        
        if ( params.getStartDate() != null && params.getEndDate() != null && params.getStartDate().after( params.getEndDate() ) )
        {
            violation = "Start date is after end date: " + params.getStartDate() + " - " + params.getEndDate();
        }

        if ( params.getPage() != null && params.getPage() <= 0 )
        {
            violation = "Page number must be positive: " + params.getPage();
        }
        
        if ( params.getPageSize() != null && params.getPageSize() < 0 )
        {
            violation = "Page size must be zero or positive: " + params.getPageSize();
        }
        
        if ( params.hasLimit() && getMaxLimit() > 0 && params.getLimit() > getMaxLimit() )
        {
            violation = "Limit of: " + params.getLimit() + " is larger than max limit: " + getMaxLimit();
        }
        
        if ( violation != null )
        {
            log.warn( "Event analytics validation failed: " + violation );
            
            throw new IllegalQueryException( violation );
        }
    }
    
    @Override
    public List<EventQueryParams> planAggregateQuery( EventQueryParams params )
    {
        Set<String> validPartitions = partitionManager.getEventAnalyticsPartitions();

        List<EventQueryParams> queries = new ArrayList<>();
        
        List<EventQueryParams> groupedByQueryItems = groupByQueryItems( params );
        
        for ( EventQueryParams byQueryItem : groupedByQueryItems )
        {        
            List<EventQueryParams> groupedByPartition = groupByPartition( byQueryItem, validPartitions );
            
            for ( EventQueryParams byPartition : groupedByPartition )
            {
                List<EventQueryParams> groupedByOrgUnitLevel = convert( queryPlanner.groupByOrgUnitLevel( byPartition ) );
                
                for ( EventQueryParams byOrgUnitLevel : groupedByOrgUnitLevel )
                {
                    queries.addAll( convert( queryPlanner.groupByPeriodType( byOrgUnitLevel ) ) );
                }
            }
        }
        
        return queries;
    }

    @Override
    public EventQueryParams planEventQuery( EventQueryParams params )
    {
        Set<String> validPartitions = partitionManager.getEventAnalyticsPartitions();

        String tableSuffix = PartitionUtils.SEP + params.getProgram().getUid();
        
        if ( params.hasStartEndDate() )
        {
            Period queryPeriod = new Period();
            queryPeriod.setStartDate( params.getStartDate() );
            queryPeriod.setEndDate( params.getEndDate() );            
            params.setPartitions( PartitionUtils.getPartitions( queryPeriod, EVENT_ANALYTICS_TABLE_NAME, tableSuffix, validPartitions ) );
        }
                
        //TODO periods, convert to start/end dates
        
        return params;
    }
    
    public void validateMaintenanceMode()
        throws MaintenanceModeException
    {
        queryPlanner.validateMaintenanceMode();
    }
    
    @Override
    public int getMaxLimit()
    {
        return (Integer) systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAX_LIMIT );
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private List<EventQueryParams> groupByPartition( EventQueryParams params, Set<String> validPartitions )
    {
        String tableSuffix = PartitionUtils.SEP + params.getProgram().getUid();
        
        if ( params.hasStartEndDate() )
        {
            List<EventQueryParams> queries = new ArrayList<>();
            
            Period queryPeriod = new Period();
            queryPeriod.setStartDate( params.getStartDate() );
            queryPeriod.setEndDate( params.getEndDate() );
            
            EventQueryParams query = params.instance();
            query.setPartitions( PartitionUtils.getPartitions( queryPeriod, EVENT_ANALYTICS_TABLE_NAME, tableSuffix, validPartitions ) );
            
            if ( query.getPartitions().hasAny() )
            {
                queries.add( query );
            }
            
            return queries;
        }
        else // Aggregate only
        {
            return convert( queryPlanner.groupByPartition( params, EVENT_ANALYTICS_TABLE_NAME, tableSuffix ) );
        }
    }
    
    /**
     * Group by items if query items are to be collapsed in order to aggregate
     * each item individually.
     */
    private List<EventQueryParams> groupByQueryItems( EventQueryParams params )
    {
        List<EventQueryParams> queries = new ArrayList<>();
        
        if ( params.isAggregateData() )
        {
            for ( QueryItem item : params.getItemsAndItemFilters() )
            {
                EventQueryParams query = params.instance();
                query.getItems().clear();
                query.getItemProgramIndicators().clear();
                query.setValue( item.getItem() );
                
                if ( item.hasProgram() )
                {
                    query.setProgram( item.getProgram() );
                }
                
                queries.add( query );
            }
            
            for ( ProgramIndicator programIndicator : params.getItemProgramIndicators() )
            {
                EventQueryParams query = params.instance();
                query.getItems().clear();
                query.getItemProgramIndicators().clear();
                query.setProgramIndicator( programIndicator );
                query.setProgram( programIndicator.getProgram() );
                queries.add( query );
            }
        }
        else if ( params.isCollapseDataDimensions() && !params.getItems().isEmpty() )
        {
            for ( QueryItem item : params.getItems() )
            {
                EventQueryParams query = params.instance();
                query.getItems().clear();
                query.getItems().add( item );
                query.setProgram( item.getProgram() );
                queries.add( query );
            }
        }
        else
        {
            queries.add( params.instance() );
        }
        
        return queries;
    }
        
    private static List<EventQueryParams> convert( List<DataQueryParams> params )
    {
        List<EventQueryParams> eventParams = new ArrayList<>();
        
        for ( DataQueryParams param : params )
        {
            eventParams.add( (EventQueryParams) param );
        }
        
        return eventParams;
    }    
}
