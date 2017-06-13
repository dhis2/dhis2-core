package org.hisp.dhis.analytics.event.data;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.data.QueryPlannerUtils;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.event.EventQueryPlanner;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.AnalyticsTableType;
import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.common.QueryItem;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
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
            violation = "Page number must be a positive number: " + params.getPage();
        }
        
        if ( params.getPageSize() != null && params.getPageSize() < 0 )
        {
            violation = "Page size must be zero or a positive number: " + params.getPageSize();
        }
        
        if ( params.hasLimit() && getMaxLimit() > 0 && params.getLimit() > getMaxLimit() )
        {
            violation = "Limit of: " + params.getLimit() + " is larger than max limit: " + getMaxLimit();
        }
        
        if ( params.hasClusterSize() && params.getClusterSize() <= 0 )
        {
            violation = "Cluster size must be a positive number: " + params.getClusterSize();
        }
        
        if ( params.hasBbox() && !ValidationUtils.bboxIsValid( params.getBbox() ) )
        {
            violation = "Bbox is invalid: " + params.getBbox() + ", must be on format: 'min-lng,min-lat,max-lng,max-lat'";
        }
        
        if ( ( params.hasBbox() || params.hasClusterSize() ) && params.getCoordinateField() == null )
        {
            violation = "Cluster field must be specified when bbox or cluster size are specified";
        }

        for ( QueryItem item : params.getItemsAndItemFilters() )
        {
            if ( item.hasLegendSet() && item.hasOptionSet() )
            {
                violation = "Query item cannot specify both legend set and option set: " + item.getItemId();
            }
            
            if ( params.isAggregateData() && !item.getAggregationType().isAggregateable() )
            {
                violation = "Query item must be aggregateable when used in aggregate query: " + item.getItemId();
            }
        }
        
        if ( violation != null )
        {
            log.warn( String.format( "Event analytics validation failed: %s", violation ) );
            
            throw new IllegalQueryException( violation );
        }
    }
    
    // TODO use list of functional groupers and single loop
    
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
                List<EventQueryParams> groupedByOrgUnitLevel = QueryPlannerUtils.convert( queryPlanner.groupByOrgUnitLevel( byPartition ) );
                
                for ( EventQueryParams byOrgUnitLevel : groupedByOrgUnitLevel )
                {
                    queries.addAll( QueryPlannerUtils.convert( queryPlanner.groupByPeriodType( byOrgUnitLevel ) ) );
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
            params.setPartitions( PartitionUtils.getPartitions( queryPeriod, AnalyticsTableType.EVENT.getTableName(), tableSuffix, validPartitions ) );
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

    /**
     * Group by partition.
     * 
     * @param params the event query parameters.
     * @param validPartitions the set of valid partitions.
     * @return a list of {@link EventQueryParams}.
     */
    private List<EventQueryParams> groupByPartition( EventQueryParams params, Set<String> validPartitions )
    {
        String tableSuffix = PartitionUtils.SEP + params.getProgram().getUid();
        
        if ( params.hasEnrollmentProgramIndicatorDimension() ) 
        {
            List<EventQueryParams> indicatorQueries = new ArrayList<>();
            
            EventQueryParams query = new EventQueryParams.Builder( params )
                .withPartitions( PartitionUtils.getPartitions( AnalyticsTableType.ENROLLMENT.getTableName(), tableSuffix, validPartitions ) )
                .build();
        
            if ( query.getPartitions().hasAny() )
            {
                indicatorQueries.add( query );
            }
            
            return indicatorQueries;
        }
        else if ( params.hasStartEndDate() )
        {
            List<EventQueryParams> queries = new ArrayList<>();
            
            Period queryPeriod = new Period();
            queryPeriod.setStartDate( params.getStartDate() );
            queryPeriod.setEndDate( params.getEndDate() );
            
            EventQueryParams query = new EventQueryParams.Builder( params )
                .withPartitions( PartitionUtils.getPartitions( queryPeriod, AnalyticsTableType.EVENT.getTableName(), tableSuffix, validPartitions ) )
                .build();
            
            if ( query.getPartitions().hasAny() )
            {
                queries.add( query );
            }
            
            return queries;
        }
        else // Aggregate only
        {
            QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder().
                withTableName( AnalyticsTableType.EVENT.getTableName() ).
                withTableSuffix( tableSuffix ).build();

            return QueryPlannerUtils.convert( queryPlanner.groupByPartition( params, plannerParams ) );
        }
    }
    
    /**
     * Group by items if query items are to be collapsed in order to aggregate
     * each item individually.
     * 
     * @param params the event query parameters.
     * @return a list of {@link EventQueryParams}.
     */
    private List<EventQueryParams> groupByQueryItems( EventQueryParams params )
    {
        List<EventQueryParams> queries = new ArrayList<>();
        
        if ( params.isAggregateData() )
        {
            for ( QueryItem item : params.getItemsAndItemFilters() )
            {
                EventQueryParams.Builder query = new EventQueryParams.Builder( params )
                    .removeItems()
                    .removeItemProgramIndicators()
                    .withValue( item.getItem() );
                
                if ( item.hasProgram() )
                {
                    query.withProgram( item.getProgram() );
                }
                
                queries.add( query.build() );
            }
            
            for ( ProgramIndicator programIndicator : params.getItemProgramIndicators() )
            {
                EventQueryParams query = new EventQueryParams.Builder( params )
                    .removeItems()
                    .removeItemProgramIndicators()
                    .withProgramIndicator( programIndicator )
                    .withProgram( programIndicator.getProgram() )
                    .build();
                
                queries.add( query );
            }
        }
        else if ( params.isCollapseDataDimensions() && !params.getItems().isEmpty() )
        {
            for ( QueryItem item : params.getItems() )
            {
                EventQueryParams.Builder query = new EventQueryParams.Builder( params )
                    .removeItems()
                    .addItem( item );
                
                if ( item.hasProgram() )
                {
                    query.withProgram( item.getProgram() );
                }
                
                queries.add( query.build() );
            }
        }
        else
        {
            queries.add( new EventQueryParams.Builder( params ).build() );
        }
        
        return queries;
    }
}
