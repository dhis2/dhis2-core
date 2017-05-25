package org.hisp.dhis.analytics.data;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataType;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.analytics.Partitions;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.partition.PartitionManager;
import org.hisp.dhis.analytics.table.PartitionUtils;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MaintenanceModeException;
import org.hisp.dhis.commons.collection.PaginatedList;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.filter.AggregatableDataElementFilter;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.hisp.dhis.analytics.AggregationType.SUM;
import static org.hisp.dhis.analytics.DataQueryParams.LEVEL_PREFIX;
import static org.hisp.dhis.analytics.DataQueryParams.COMPLETENESS_DIMENSION_TYPES;
import static org.hisp.dhis.common.DimensionalObject.*;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;

/**
 * @author Lars Helge Overland
 */
public class DefaultQueryPlanner
    implements QueryPlanner
{
    private static final Log log = LogFactory.getLog( DefaultQueryPlanner.class );

    @Autowired
    private PartitionManager partitionManager;

    @Autowired
    private SystemSettingManager systemSettingManager;

    // -------------------------------------------------------------------------
    // DefaultQueryPlanner implementation
    // -------------------------------------------------------------------------

    @Override
    public void validate( DataQueryParams params )
        throws IllegalQueryException
    {
        String violation = null;

        if ( params == null )
        {
            throw new IllegalQueryException( "Params cannot be null" );
        }

        final List<DimensionalItemObject> dataElements = Lists.newArrayList( params.getDataElements() );
        params.getProgramDataElements().stream().forEach( pde -> dataElements.add( ((ProgramDataElementDimensionItem) pde).getDataElement() ) );        
        final List<DataElement> nonAggDataElements = FilterUtils.inverseFilter( asTypedList( dataElements ), AggregatableDataElementFilter.INSTANCE );

        if ( params.getDimensions().isEmpty() )
        {
            violation = "At least one dimension must be specified";
        }

        if ( !params.getDimensionsAsFilters().isEmpty() )
        {
            violation = "Dimensions cannot be specified as dimension and filter simultaneously: " + params.getDimensionsAsFilters();
        }

        if ( !params.hasPeriods() && !params.isSkipPartitioning() && !params.hasStartEndDate() )
        {
            violation = "At least one period must be specified as dimension or filter";
        }
        
        if ( params.hasPeriods() && params.hasStartEndDate() )
        {
            violation = "Periods and start and end dates cannot be specified simultaneously";
        }

        if ( !params.getFilterIndicators().isEmpty() && params.getFilterOptions( DATA_X_DIM_ID ).size() > 1 )
        {
            violation = "Only a single indicator can be specified as filter";
        }

        if ( !params.getFilterReportingRates().isEmpty() && params.getFilterOptions( DATA_X_DIM_ID ).size() > 1 )
        {
            violation = "Only a single reporting rate can be specified as filter";
        }

        if ( params.getFilters().contains( new BaseDimensionalObject( CATEGORYOPTIONCOMBO_DIM_ID ) ) )
        {
            violation = "Category option combos cannot be specified as filter";
        }

        if ( !params.getDuplicateDimensions().isEmpty() )
        {
            violation = "Dimensions cannot be specified more than once: " + params.getDuplicateDimensions();
        }
        
        if ( !params.getAllReportingRates().isEmpty() && !params.containsOnlyDimensionsAndFilters( COMPLETENESS_DIMENSION_TYPES ) )
        {
            violation = "Reporting rates can only be specified together with dimensions of type: " + COMPLETENESS_DIMENSION_TYPES;
        }

        if ( params.hasDimensionOrFilter( CATEGORYOPTIONCOMBO_DIM_ID ) && params.getAllDataElements().isEmpty() )
        {
            violation = "Assigned categories cannot be specified when data elements are not specified";
        }

        if ( params.hasDimensionOrFilter( CATEGORYOPTIONCOMBO_DIM_ID ) && ( params.getAllDataElements().size() != params.getAllDataDimensionItems().size() ) )
        {
            violation = "Assigned categories can only be specified together with data elements, not indicators or reporting rates";
        }
        
        if ( !nonAggDataElements.isEmpty() )
        {
            violation = "Data elements must be of a value and aggregation type that allow aggregation: " + getUids( nonAggDataElements );
        }
        
        if ( params.isOutputFormat( OutputFormat.DATA_VALUE_SET ) )
        {
            if ( !params.hasDimension( DATA_X_DIM_ID ) )
            {
                violation = "A data dimension 'dx' must be specified when output format is DATA_VALUE_SET";
            }
            
            if ( !params.hasDimension( PERIOD_DIM_ID ) )
            {
                violation = "A period dimension 'pe' must be specified when output format is DATA_VALUE_SET";
            }
                        
            if ( !params.hasDimension( ORGUNIT_DIM_ID ) )
            {
                violation = "An organisation unit dimension 'ou' must be specified when output format is DATA_VALUE_SET";
            }
        }

        if ( violation != null )
        {
            log.warn( String.format( "Analytics validation failed: %s", violation ) );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public void validateTableLayout( DataQueryParams params, List<String> columns, List<String> rows )
    {
        String violation = null;

        if ( columns != null )
        {
            for ( String column : columns )
            {
                if ( !params.hasDimension( column ) )
                {
                    violation = "Column must be present as dimension in query: " + column;
                }
            }
        }

        if ( rows != null )
        {
            for ( String row : rows )
            {
                if ( !params.hasDimension( row ) )
                {
                    violation = "Row must be present as dimension in query: " + row;
                }
            }
        }

        if ( violation != null )
        {
            log.warn( String.format( "Validation failed: %s", violation ) );

            throw new IllegalQueryException( violation );
        }
    }

    @Override
    public void validateMaintenanceMode()
        throws MaintenanceModeException
    {
        boolean maintenance = (Boolean) systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAINTENANCE_MODE );

        if ( maintenance )
        {
            throw new MaintenanceModeException( "Analytics engine is in maintenance mode, try again later" );
        }
    }

    @Override
    public DataQueryGroups planQuery( DataQueryParams params, QueryPlannerParams plannerParams )
    {
        validate( params );

        // ---------------------------------------------------------------------
        // Group queries which can be executed together
        // ---------------------------------------------------------------------

        final List<DataQueryParams> queries = new ArrayList<>( groupByPartition( params, plannerParams ) );
        
        List<Function<DataQueryParams, List<DataQueryParams>>> groupers = new ImmutableList.Builder<Function<DataQueryParams, List<DataQueryParams>>>()
            .add( q -> groupByOrgUnitLevel( q ) )
            .add( q -> groupByPeriodType( q ) )
            .add( q -> groupByDataType( q ) )
            .add( q -> groupByAggregationType( q ) )
            .add( q -> groupByDaysInPeriod( q ) )
            .add( q -> groupByDataPeriodType( q ) )
            .addAll( plannerParams.getQueryGroupers() )
            .build();
        
        for ( Function<DataQueryParams, List<DataQueryParams>> grouper : groupers )
        {
            List<DataQueryParams> currentQueries = Lists.newArrayList( queries );
            queries.clear();
            
            for ( DataQueryParams query : currentQueries )
            {
                queries.addAll( grouper.apply( query ) );
            }
        }

        // ---------------------------------------------------------------------
        // Split queries until optimal number
        // ---------------------------------------------------------------------

        DataQueryGroups queryGroups = DataQueryGroups.newBuilder().withQueries( queries ).build();

        if ( queryGroups.isOptimal( plannerParams.getOptimalQueries() ) )
        {
            return queryGroups;
        }

        List<String> splitDimensions = Lists.newArrayList( DATA_X_DIM_ID, ORGUNIT_DIM_ID );
        
        for ( String dim : splitDimensions )
        {
            queryGroups = splitByDimension( queryGroups, dim, plannerParams.getOptimalQueries() );

            if ( queryGroups.isOptimal( plannerParams.getOptimalQueries() ) )
            {
                break;
            }
        }
        
        return queryGroups;
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Splits the given list of queries in sub queries on the given dimension.
     */
    private DataQueryGroups splitByDimension( DataQueryGroups queryGroups, String dimension, int optimalQueries )
    {
        int optimalForSubQuery = MathUtils.divideToFloor( optimalQueries, queryGroups.getLargestGroupSize() );

        List<DataQueryParams> subQueries = new ArrayList<>();

        for ( DataQueryParams query : queryGroups.getAllQueries() )
        {
            DimensionalObject dim = query.getDimension( dimension );

            List<DimensionalItemObject> values = null;

            if ( dim == null || (values = dim.getItems()) == null || values.isEmpty() )
            {
                subQueries.add( DataQueryParams.newBuilder( query ).build() );
                continue;
            }

            List<List<DimensionalItemObject>> valuePages = new PaginatedList<>( values ).setNumberOfPages( optimalForSubQuery ).getPages();

            for ( List<DimensionalItemObject> valuePage : valuePages )
            {
                DataQueryParams subQuery = DataQueryParams.newBuilder( query )
                    .withDimensionOptions( dim.getDimension(), valuePage ).build();
                
                subQueries.add( subQuery );
            }
        }

        if ( subQueries.size() > queryGroups.getAllQueries().size() )
        {
            log.debug( String.format( "Split on dimension %s: %d", dimension, (subQueries.size() / queryGroups.getAllQueries().size()) ) );
        }

        return DataQueryGroups.newBuilder().withQueries( subQueries ).build();
    }

    // -------------------------------------------------------------------------
    // Supportive - group by methods
    // -------------------------------------------------------------------------

    @Override
    public List<DataQueryParams> groupByPartition( DataQueryParams params, QueryPlannerParams plannerParams )
    {
        Set<String> validPartitions = partitionManager.getDataValueAnalyticsPartitions();
        
        String tableName = plannerParams.getTableName();
        String tableSuffix = plannerParams.getTableSuffix();

        List<DataQueryParams> queries = new ArrayList<>();

        if ( params.isSkipPartitioning() )
        {
            DataQueryParams query = DataQueryParams.newBuilder( params )
                .withPartitions( new Partitions().add( tableName ) ).build();
            
            queries.add( query );
        }
        else if ( !params.getPeriods().isEmpty() )
        {
            ListMap<Partitions, DimensionalItemObject> partitionPeriodMap = 
                PartitionUtils.getPartitionPeriodMap( params.getPeriods(), tableName, tableSuffix, validPartitions );

            for ( Partitions partitions : partitionPeriodMap.keySet() )
            {
                if ( partitions.hasAny() )
                {
                    DataQueryParams query = DataQueryParams.newBuilder( params )
                        .withPeriods( partitionPeriodMap.get( partitions ) )
                        .withPartitions( partitions ).build();
                    
                    queries.add( query );
                }
            }
        }
        else if ( !params.getFilterPeriods().isEmpty() )
        {
            Partitions partitions = PartitionUtils.getPartitions( params.getFilterPeriods(), tableName, tableSuffix, validPartitions );

            if ( partitions.hasAny() )
            {
                DataQueryParams query = DataQueryParams.newBuilder( params )
                    .withPartitions( partitions ).build();
                
                queries.add( query );
            }
        }
        else if ( params.hasStartEndDate() )
        {
            Partitions partitions = PartitionUtils.getPartitions( params.getStartDate(), params.getEndDate(), tableName, tableSuffix, validPartitions );
            
            if ( partitions.hasAny() )
            {
                DataQueryParams query = DataQueryParams.newBuilder( params )
                    .withPartitions( partitions ).build();
                
                queries.add( query );
            }
        }
        else
        {
            throw new IllegalQueryException( "Query does not contain any period dimension items" );
        }

        if ( queries.size() > 1 )
        {
            log.debug( String.format( "Split on partition: %d", queries.size() ) );
        }

        return queries;
    }
    
    /**
     * If periods appear as dimensions in the given query; groups the query into
     * sub queries based on the period type of the periods. Sets the period type
     * name on each query. If periods appear as filters; replaces the period filter
     * with one filter for each period type. Sets the dimension names and filter
     * names respectively.
     */
    @Override
    public List<DataQueryParams> groupByPeriodType( DataQueryParams params )
    {
        List<DataQueryParams> queries = new ArrayList<>();

        if ( params.isSkipPartitioning() )
        {
            queries.add( params );
        }
        else if ( !params.getPeriods().isEmpty() )
        {
            ListMap<String, DimensionalItemObject> periodTypePeriodMap = PartitionUtils.getPeriodTypePeriodMap( params.getPeriods() );

            for ( String periodType : periodTypePeriodMap.keySet() )
            {
                DataQueryParams query = DataQueryParams.newBuilder( params )
                    .addOrSetDimensionOptions( PERIOD_DIM_ID, DimensionType.PERIOD, periodType.toLowerCase(), periodTypePeriodMap.get( periodType ) )
                    .withPeriodType( periodType ).build();
                
                queries.add( query );
            }
        }
        else if ( !params.getFilterPeriods().isEmpty() )
        {
            DimensionalObject filter = params.getFilter( PERIOD_DIM_ID );

            ListMap<String, DimensionalItemObject> periodTypePeriodMap = PartitionUtils.getPeriodTypePeriodMap( filter.getItems() );

            DataQueryParams.Builder query = DataQueryParams.newBuilder( params )
                .removeFilter( PERIOD_DIM_ID )
                .withPeriodType( periodTypePeriodMap.keySet().iterator().next() ); // Using first period type

            for ( String periodType : periodTypePeriodMap.keySet() )
            {
                query.addFilter( new BaseDimensionalObject( filter.getDimension(), filter.getDimensionType(), 
                    periodType.toLowerCase(), filter.getDisplayName(), periodTypePeriodMap.get( periodType ) ) );
            }

            queries.add( query.build() );
        }
        else
        {
            queries.add( DataQueryParams.newBuilder( params ).build() );
            return queries;
        }

        if ( queries.size() > 1 )
        {
            log.debug( String.format( "Split on period type: %d", queries.size() ) );
        }

        return queries;
    }

    @Override
    public List<DataQueryParams> groupByOrgUnitLevel( DataQueryParams params )
    {
        List<DataQueryParams> queries = new ArrayList<>();

        if ( !params.getOrganisationUnits().isEmpty() )
        {
            ListMap<Integer, DimensionalItemObject> levelOrgUnitMap = 
                QueryPlannerUtils.getLevelOrgUnitMap( params.getOrganisationUnits() );

            for ( Integer level : levelOrgUnitMap.keySet() )
            {
                DataQueryParams query = DataQueryParams.newBuilder( params )
                    .addOrSetDimensionOptions( ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, LEVEL_PREFIX + level, levelOrgUnitMap.get( level ) ).build();
                
                queries.add( query );
            }
        }
        else if ( !params.getFilterOrganisationUnits().isEmpty() )
        {
            ListMap<Integer, DimensionalItemObject> levelOrgUnitMap = 
                QueryPlannerUtils.getLevelOrgUnitMap( params.getFilterOrganisationUnits() );

            DimensionalObject filter = params.getFilter( ORGUNIT_DIM_ID );
            
            DataQueryParams.Builder query = DataQueryParams.newBuilder( params )
                .removeFilter( ORGUNIT_DIM_ID );

            for ( Integer level : levelOrgUnitMap.keySet() )
            {
                query.addFilter( new BaseDimensionalObject( filter.getDimension(),
                    filter.getDimensionType(), LEVEL_PREFIX + level, filter.getDisplayName(), levelOrgUnitMap.get( level ) ) );
            }

            queries.add( query.build() );
        }
        else
        {
            queries.add( DataQueryParams.newBuilder( params ).build() );
            return queries;
        }

        if ( queries.size() > 1 )
        {
            log.debug( String.format( "Split on org unit level: %d", queries.size() ) );
        }

        return queries;
    }

    @Override
    public List<DataQueryParams> groupByStartEndDate( DataQueryParams params )
    {
        List<DataQueryParams> queries = new ArrayList<>();
        
        if ( !params.getPeriods().isEmpty() )
        {
            for ( DimensionalItemObject item : params.getPeriods() )
            {
                Period period = (Period) item;
                
                DataQueryParams query = DataQueryParams.newBuilder( params )
                    .withStartDate( period.getStartDate() )
                    .withEndDate( period.getEndDate() ).build();
    
                BaseDimensionalObject staticPeriod = (BaseDimensionalObject) query.getDimension( PERIOD_DIM_ID );
                staticPeriod.setDimensionName( period.getIsoDate() );
                staticPeriod.setFixed( true );
                
                queries.add( query );
            }
        }
        else if ( !params.getFilterPeriods().isEmpty() )
        {
            Period period = (Period) params.getFilterPeriods().get( 0 );

            DataQueryParams query = DataQueryParams.newBuilder( params )
                .withStartDate( period.getStartDate() )
                .withEndDate( period.getEndDate() )
                .removeFilter( PERIOD_DIM_ID ).build();
            
            queries.add( query );
        }
        else
        {
            throw new IllegalQueryException( "Query does not contain any period dimension items" );
        }

        if ( queries.size() > 1 )
        {
            log.debug( String.format( "Split on period: %d", queries.size() ) );
        }
        
        return queries;
    }
    
    /**
     * Groups queries by their data type.
     * 
     * @param params the data query parameters.
     * @return a list of {@link DataQueryParams}.
     */
    private List<DataQueryParams> groupByDataType( DataQueryParams params )
    {
        List<DataQueryParams> queries = new ArrayList<>();

        if ( !params.getDataElements().isEmpty() )
        {
            ListMap<DataType, DimensionalItemObject> dataTypeDataElementMap = 
                QueryPlannerUtils.getDataTypeDataElementMap( params.getDataElements() );

            for ( DataType dataType : dataTypeDataElementMap.keySet() )
            {
                DataQueryParams query = DataQueryParams.newBuilder( params )
                    .withDataElements( dataTypeDataElementMap.get( dataType ) )
                    .withDataType( dataType ).build();
                
                queries.add( query );
            }
        }
        else
        {
            DataQueryParams query = DataQueryParams.newBuilder( params )
                .withDataType( DataType.NUMERIC ).build();
            
            queries.add( query );
        }

        if ( queries.size() > 1 )
        {
            log.debug( String.format( "Split on data type: %d", queries.size() ) );
        }

        return queries;
    }

    /**
     * Groups the given query in sub queries based on the aggregation type of its
     * data elements. The aggregation type can be sum, average aggregation or
     * average disaggregation. Sum means that the data elements have sum aggregation
     * operator. Average aggregation means that the data elements have the average
     * aggregation operator and that the period type of the data elements have
     * higher or equal frequency than the aggregation period type. Average disaggregation
     * means that the data elements have the average aggregation operator and
     * that the period type of the data elements have lower frequency than the
     * aggregation period type. Average bool means that the data elements have the
     * average aggregation operator and the bool value type.
     * <p>
     * If no data elements are present, the aggregation type will be determined
     * based on the first data element in the first data element group in the
     * first data element group set in the query.
     * <p>
     * If the aggregation type is already set/overridden in the request, the
     * query will be returned unchanged. If there are no data elements or data
     * element group sets specified the aggregation type will fall back to sum.
     * 
     * @param params the data query parameters.
     * @return a list of {@link DataQueryParams}.
     */
    private List<DataQueryParams> groupByAggregationType( DataQueryParams params )
    {
        List<DataQueryParams> queries = new ArrayList<>();

        if ( !params.getDataElements().isEmpty() )
        {
            ListMap<AggregationType, DimensionalItemObject> aggregationTypeDataElementMap = 
                QueryPlannerUtils.getAggregationTypeDataElementMap( params );

            for ( AggregationType aggregationType : aggregationTypeDataElementMap.keySet() )
            {
                DataQueryParams query = DataQueryParams.newBuilder( params )
                    .withDataElements( aggregationTypeDataElementMap.get( aggregationType ) )
                    .withAggregationType( aggregationType ).build();
                
                queries.add( query );
            }
        }
        else if ( !params.getDataElementGroupSets().isEmpty() )
        {
            DimensionalObject degs = params.getDataElementGroupSets().get( 0 );
            DataElementGroup deg = (DataElementGroup) (degs.hasItems() ? degs.getItems().get( 0 ) : null);
            
            AggregationType aggregationType = ObjectUtils.firstNonNull( params.getAggregationType(), SUM );

            if ( deg != null && !deg.getMembers().isEmpty() )
            {
                PeriodType periodType = PeriodType.getPeriodTypeByName( params.getPeriodType() );
                aggregationType = ObjectUtils.firstNonNull( params.getAggregationType(), deg.getAggregationType() );
                aggregationType = QueryPlannerUtils.getAggregationType( 
                    deg.getValueType(), aggregationType, periodType, deg.getPeriodType() );
            }
            
            DataQueryParams query = DataQueryParams.newBuilder( params )
                .withAggregationType( aggregationType ).build();

            queries.add( query );
        }
        else
        {
            DataQueryParams query = DataQueryParams.newBuilder( params )
                .withAggregationType( ObjectUtils.firstNonNull( params.getAggregationType(), SUM ) ).build();
            
            queries.add( query );
        }

        if ( queries.size() > 1 )
        {
            log.debug( String.format( "Split on aggregation type: %d", queries.size() ) );
        }

        return queries;
    }

    /**
     * Groups the given query into sub queries based on the number of days in the
     * aggregation period. This only applies if the aggregation type is
     * {@link AggregationType#AVERAGE_SUM_INT} and the query has at least one period as 
     * dimension option. This is necessary since the number of days in the aggregation 
     * period is part of the expression for aggregating the value.
     * 
     * @param params the data query parameters.
     * @return a list of {@link DataQueryParams}.
     */
    private List<DataQueryParams> groupByDaysInPeriod( DataQueryParams params )
    {
        List<DataQueryParams> queries = new ArrayList<>();

        if ( params.getPeriods().isEmpty() || !params.isAggregationType( AggregationType.AVERAGE_SUM_INT ) )
        {
            queries.add( DataQueryParams.newBuilder( params ).build() );
            return queries;
        }

        ListMap<Integer, DimensionalItemObject> daysPeriodMap = 
            QueryPlannerUtils.getDaysPeriodMap( params.getPeriods() );

        DimensionalObject periodDim = params.getDimension( PERIOD_DIM_ID );

        for ( Integer days : daysPeriodMap.keySet() )
        {
            DataQueryParams query = DataQueryParams.newBuilder( params )
                .addOrSetDimensionOptions( periodDim.getDimension(), periodDim.getDimensionType(), periodDim.getDimensionName(), daysPeriodMap.get( days ) ).build();
            
            queries.add( query );
        }

        if ( queries.size() > 1 )
        {
            log.debug( String.format( "Split on days in period: %d", queries.size() ) );
        }

        return queries;
    }

    /**
     * Groups the given query in sub queries based on the period type of its
     * data elements. Sets the data period type on each query. This only applies
     * if the aggregation type of the query involves disaggregation.
     * 
     * @param params the data query parameters.
     * @return a list of {@link DataQueryParams}.
     */
    private List<DataQueryParams> groupByDataPeriodType( DataQueryParams params )
    {
        List<DataQueryParams> queries = new ArrayList<>();

        if ( params.getDataElements().isEmpty() || !params.isDisaggregation() )
        {
            queries.add( DataQueryParams.newBuilder( params ).build() );
            return queries;
        }

        ListMap<PeriodType, DimensionalItemObject> periodTypeDataElementMap = 
            QueryPlannerUtils.getPeriodTypeDataElementMap( params.getDataElements() );

        for ( PeriodType periodType : periodTypeDataElementMap.keySet() )
        {
            DataQueryParams query = DataQueryParams.newBuilder( params )
                .withDataElements( periodTypeDataElementMap.get( periodType ) )
                .withDataPeriodType( periodType ).build();
            
            queries.add( query );
        }

        if ( queries.size() > 1 )
        {
            log.debug( String.format( "Split on data period type: %d", queries.size() ) );
        }

        return queries;
    }
}
