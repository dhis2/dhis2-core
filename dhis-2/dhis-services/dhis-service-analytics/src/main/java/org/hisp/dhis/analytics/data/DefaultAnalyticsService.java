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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AggregationType;
import org.hisp.dhis.analytics.AnalyticsManager;
import org.hisp.dhis.analytics.AnalyticsMetaDataKey;
import org.hisp.dhis.analytics.AnalyticsSecurityManager;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.AnalyticsUtils;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DataQueryService;
import org.hisp.dhis.analytics.DimensionItem;
import org.hisp.dhis.analytics.OutputFormat;
import org.hisp.dhis.analytics.ProcessingHint;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.RawAnalyticsManager;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.table.AnalyticsTableType;
import org.hisp.dhis.calendar.Calendar;
import org.hisp.dhis.common.AnalyticalObject;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.CombinationGenerator;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.IdentifiableObjectUtils;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.commons.collection.ListUtils;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dxf2.datavalueset.DataValueSet;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.reporttable.ReportTable;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.Timer;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hisp.dhis.analytics.DataQueryParams.*;
import static org.hisp.dhis.common.DataDimensionItemType.*;
import static org.hisp.dhis.common.DimensionalObject.*;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionalItemIds;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getLocalPeriodIdentifiers;
import static org.hisp.dhis.common.ReportingRateMetric.*;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentGraphMap;
import static org.hisp.dhis.organisationunit.OrganisationUnit.getParentNameGraphMap;
import static org.hisp.dhis.period.PeriodType.getPeriodTypeFromIsoString;
import static org.hisp.dhis.reporttable.ReportTable.IRT2D;
import static org.hisp.dhis.reporttable.ReportTable.addListIfEmpty;

/**
 * @author Lars Helge Overland
 */
public class DefaultAnalyticsService
    implements AnalyticsService
{
    private static final Log log = LogFactory.getLog( DefaultAnalyticsService.class );

    private static final int PERCENT = 100;
    private static final int MAX_QUERIES = 8;

    @Autowired
    private AnalyticsManager analyticsManager;
    
    @Autowired
    private RawAnalyticsManager rawAnalyticsManager;

    @Autowired
    private AnalyticsSecurityManager securityManager;

    @Autowired
    private QueryPlanner queryPlanner;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private EventAnalyticsService eventAnalyticsService;

    @Autowired
    private DataQueryService dataQueryService;

    // -------------------------------------------------------------------------
    // AnalyticsService implementation
    // -------------------------------------------------------------------------
    
    @Override
    public Grid getAggregatedDataValues( DataQueryParams params )
    {
        // ---------------------------------------------------------------------
        // Security and validation
        // ---------------------------------------------------------------------

        securityManager.decideAccess( params );

        params = securityManager.withDataApprovalConstraints( params );
        params = securityManager.withDimensionConstraints( params );

        queryPlanner.validate( params );

        return getAggregatedDataValueGridInternal( params );
    }

    @Override
    public Grid getAggregatedDataValues( DataQueryParams params, List<String> columns, List<String> rows )
    {
        boolean tableLayout = (columns != null && !columns.isEmpty()) || (rows != null && !rows.isEmpty());

        return tableLayout ?
            getAggregatedDataValuesTableLayout( params, columns, rows ) :
            getAggregatedDataValues( params );
    }

    @Override
    public Grid getRawDataValues( DataQueryParams params )
    {
        securityManager.decideAccess( params );

        params = securityManager.withDataApprovalConstraints( params );
        params = securityManager.withDimensionConstraints( params );

        queryPlanner.validate( params );
        
        return getRawDataGrid( params );
    }

    @Override
    public DataValueSet getAggregatedDataValueSet( DataQueryParams params )
    {
        DataQueryParams query = DataQueryParams.newBuilder( params )
            .withSkipMeta( false )
            .withSkipData( false )
            .withIncludeNumDen( false )
            .withOutputFormat( OutputFormat.DATA_VALUE_SET )
            .build();
        
        Grid grid = getAggregatedDataValueGridInternal( query );
                
        return AnalyticsUtils.getDataValueSetFromGrid( params, grid );
    }
    
    @Override
    public Grid getAggregatedDataValues( AnalyticalObject object )
    {
        DataQueryParams params = dataQueryService.getFromAnalyticalObject( object );

        return getAggregatedDataValues( params );
    }

    @Override
    public Map<String, Object> getAggregatedDataValueMapping( DataQueryParams params )
    {
        Grid grid = getAggregatedDataValues( DataQueryParams.newBuilder( params )
            .withIncludeNumDen( false ).build() );

        return AnalyticsUtils.getAggregatedDataValueMapping( grid );
    }

    @Override
    public Map<String, Object> getAggregatedDataValueMapping( AnalyticalObject object )
    {
        DataQueryParams params = dataQueryService.getFromAnalyticalObject( object );

        return getAggregatedDataValueMapping( params );
    }

    // -------------------------------------------------------------------------
    // Private business logic methods
    // -------------------------------------------------------------------------

    /**
     * Returns a grid with aggregated data.
     *
     * @param params the {@link DataQueryParams}.
     * @return a grid with aggregated data.
     */
    private Grid getAggregatedDataValueGridInternal( DataQueryParams params )
    {
        params = preHandleQuery( params );

        // ---------------------------------------------------------------------
        // Headers
        // ---------------------------------------------------------------------

        Grid grid = new ListGrid();

        addHeaders( params, grid );

        // ---------------------------------------------------------------------
        // Data
        // ---------------------------------------------------------------------

        addIndicatorValues( params, grid );

        addDataElementValues( params, grid );

        addDataElementOperandValues( params, grid );

        addReportingRates( params, grid );

        addProgramDataElementAttributeIndicatorValues( params, grid );

        addDynamicDimensionValues( params, grid );

        // ---------------------------------------------------------------------
        // Meta-data
        // ---------------------------------------------------------------------

        addMetaData( params, grid );
        
        handleDataValueSet( params, grid );

        applyIdScheme( params, grid );

        postHandleGrid( params, grid );

        return grid;
    }

    /**
     * Performs pre-handling of the given query and returns the immutable,
     * handled query. If the query has a single indicator as item for the data
     * filter, the filter is set as a dimension and removed as a filter.
     *
     * @param params the {@link DataQueryParams}.
     * @return a {@link DataQueryParams}.
     */
    private DataQueryParams preHandleQuery( DataQueryParams params )
    {
        if ( params.hasSingleIndicatorAsDataFilter() || params.hasSingleReportingRateAsDataFilter() )
        {
            DimensionalObject dx = params.getFilter( DATA_X_DIM_ID );

            return DataQueryParams.newBuilder( params )
                .addDimension( dx )
                .removeFilter( DATA_X_DIM_ID )
                .addProcessingHint( ProcessingHint.SINGLE_INDICATOR_REPORTING_RATE_FILTER_ITEM ).build();
        }

        return params;
    }

    /**
     * Performs post-handling of the given grid. If the query has the single
     * indicator as data filter item, the column at the data dimension index is
     * removed.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void postHandleGrid( DataQueryParams params, Grid grid )
    {
        if ( params.hasProcessingHint( ProcessingHint.SINGLE_INDICATOR_REPORTING_RATE_FILTER_ITEM ) )
        {
            grid.removeColumn( DataQueryParams.DX_INDEX );
        }
    }

    /**
     * Adds headers to the given grid based on the given data query parameters.
     * 
     * @param params the {@link DataQueryParams}.
     * @return the grid.
     */
    private void addHeaders( DataQueryParams params, Grid grid )
    {
        if ( !params.isSkipData() && !params.isSkipHeaders() )
        {
            for ( DimensionalObject col : params.getDimensions() )
            {
                grid.addHeader( new GridHeader( col.getDimension(), col.getDisplayName(), ValueType.TEXT, String.class.getName(), false, true ) );
            }

            grid.addHeader( new GridHeader( VALUE_ID, VALUE_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) );

            if ( params.isIncludeNumDen() )
            {
                grid.addHeader( new GridHeader( NUMERATOR_ID, NUMERATOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) )
                    .addHeader( new GridHeader( DENOMINATOR_ID, DENOMINATOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) )
                    .addHeader( new GridHeader( FACTOR_ID, FACTOR_HEADER_NAME, ValueType.NUMBER, Double.class.getName(), false, false ) );
            }
        }
    }

    /**
     * Adds indicator values to the given grid based on the given data query
     * parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void addIndicatorValues( DataQueryParams params, Grid grid )
    {
        if ( !params.getIndicators().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                .retainDataDimension( DataDimensionItemType.INDICATOR )
                .withIncludeNumDen( false ).build();

            List<Indicator> indicators = asTypedList( dataSourceParams.getIndicators() );

            Period filterPeriod = dataSourceParams.getFilterPeriod();

            Map<String, Double> constantMap = constantService.getConstantMap();

            // -----------------------------------------------------------------
            // Get indicator values
            // -----------------------------------------------------------------

            Map<String, Map<String, Integer>> permutationOrgUnitTargetMap = getOrgUnitTargetMap( dataSourceParams, indicators );

            List<List<DimensionItem>> dimensionItemPermutations = dataSourceParams.getDimensionItemPermutations();

            Map<String, Map<DimensionalItemObject, Double>> permutationDimensionItemValueMap = getPermutationDimensionItemValueMap( dataSourceParams );

            for ( Indicator indicator : indicators )
            {
                for ( List<DimensionItem> dimensionItems : dimensionItemPermutations )
                {
                    String permKey = DimensionItem.asItemKey( dimensionItems );

                    Map<DimensionalItemObject, Double> valueMap = permutationDimensionItemValueMap.get( permKey );

                    if ( valueMap == null )
                    {
                        continue;
                    }

                    Period period = filterPeriod != null ? filterPeriod : (Period) DimensionItem.getPeriodItem( dimensionItems );

                    OrganisationUnit unit = (OrganisationUnit) DimensionItem.getOrganisationUnitItem( dimensionItems );

                    String ou = unit != null ? unit.getUid() : null;

                    Map<String, Integer> orgUnitCountMap = permutationOrgUnitTargetMap != null ? permutationOrgUnitTargetMap.get( ou ) : null;

                    IndicatorValue value = expressionService.getIndicatorValueObject( indicator, period, valueMap, constantMap, orgUnitCountMap );

                    if ( value != null )
                    {
                        List<DimensionItem> row = new ArrayList<>( dimensionItems );

                        row.add( DX_INDEX, new DimensionItem( DATA_X_DIM_ID, indicator ) );

                        grid.addRow()
                            .addValues( DimensionItem.getItemIdentifiers( row ) )
                            .addValue( AnalyticsUtils.getRoundedValue( dataSourceParams, indicator.getDecimals(), value.getValue() ) );

                        if ( params.isIncludeNumDen() )
                        {
                            grid.addValue( AnalyticsUtils.getRoundedValue( dataSourceParams, indicator.getDecimals(), value.getNumeratorValue() ) )
                                .addValue( AnalyticsUtils.getRoundedValue( dataSourceParams, indicator.getDecimals(), value.getDenominatorValue() ) )
                                .addValue( AnalyticsUtils.getRoundedValue( dataSourceParams, indicator.getDecimals(), value.getFactorAnnualizedValue() ) );
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds data element values to the given grid based on the given data query
     * parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void addDataElementValues( DataQueryParams params, Grid grid )
    {
        if ( !params.getAllDataElements().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                .retainDataDimension( DataDimensionItemType.DATA_ELEMENT )
                .withIncludeNumDen( false ).build();

            Map<String, Object> aggregatedDataMap = getAggregatedDataValueMapObjectTyped( dataSourceParams );

            for ( Map.Entry<String, Object> entry : aggregatedDataMap.entrySet() )
            {
                Object value = AnalyticsUtils.getRoundedValueObject( params, entry.getValue() );

                grid.addRow()
                    .addValues( entry.getKey().split( DIMENSION_SEP ) )
                    .addValue( value );

                if ( params.isIncludeNumDen() )
                {
                    grid.addNullValues( 3 );
                }
            }
        }
    }
    
    /**
     * Adds data element operand values to the given grid based on the given data
     * query parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void addDataElementOperandValues( DataQueryParams params, Grid grid )
    {
        if ( !params.getDataElementOperands().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                .retainDataDimension( DataDimensionItemType.DATA_ELEMENT_OPERAND )
                .withIncludeNumDen( false ).build();
            
            for ( DataElementOperand.TotalType type : DataElementOperand.TotalType.values() )
            {
                addDataElementOperandValues( dataSourceParams, grid, type );
            }            
        }
    }
    
    /**
     * Adds data element operand values to the given grid.
     * 
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     * @param totalType the operand {@link DataElementOperand.TotalType}.
     */
    private void addDataElementOperandValues( DataQueryParams params, Grid grid, DataElementOperand.TotalType totalType )
    {        
        List<DataElementOperand> operands = asTypedList( params.getDataElementOperands() );
        operands = operands.stream().filter( o -> totalType.equals( o.getTotalType() ) ).collect( Collectors.toList() );
        
        if ( operands.isEmpty() )
        {
            return;
        }
        
        List<DimensionalItemObject> dataElements = Lists.newArrayList( DimensionalObjectUtils.getDataElements( operands ) );
        List<DimensionalItemObject> categoryOptionCombos = Lists.newArrayList( DimensionalObjectUtils.getCategoryOptionCombos( operands ) );
        List<DimensionalItemObject> attributeOptionCobos = Lists.newArrayList( DimensionalObjectUtils.getAttributeOptionCombos( operands ) );

        //TODO check if data was dim or filter

        DataQueryParams.Builder builder = DataQueryParams.newBuilder( params )
            .removeDimension( DATA_X_DIM_ID )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DimensionType.DATA_X, dataElements ) );
        
        if ( totalType.isCategoryOptionCombo() )
        {
            builder.addDimension( new BaseDimensionalObject( CATEGORYOPTIONCOMBO_DIM_ID, DimensionType.CATEGORY_OPTION_COMBO, categoryOptionCombos ) );
        }
        
        if ( totalType.isAttributeOptionCombo() )
        {
            builder.addDimension( new BaseDimensionalObject( ATTRIBUTEOPTIONCOMBO_DIM_ID, DimensionType.ATTRIBUTE_OPTION_COMBO, attributeOptionCobos ) );
        }
        
        DataQueryParams operandParams = builder.build();

        Map<String, Object> aggregatedDataMap = getAggregatedDataValueMapObjectTyped( operandParams );
        
        aggregatedDataMap = AnalyticsUtils.convertDxToOperand( aggregatedDataMap, totalType );
        
        for ( Map.Entry<String, Object> entry : aggregatedDataMap.entrySet() )
        {
            Object value = AnalyticsUtils.getRoundedValueObject( operandParams, entry.getValue() );

            grid.addRow()
                .addValues( entry.getKey().split( DIMENSION_SEP ) )
                .addValue( value );

            if ( params.isIncludeNumDen() )
            {
                grid.addNullValues( 3 );
            }
        }
    }

    /**
     * Adds reporting rates to the given grid based on the given data query
     * parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void addReportingRates( DataQueryParams params, Grid grid )
    {
        if ( !params.getReportingRates().isEmpty() && !params.isSkipData() )
        {
            for ( ReportingRateMetric metric : ReportingRateMetric.values() )
            {
                DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                    .retainDataDimensionReportingRates( metric )
                    .ignoreDataApproval() // No approval for reporting rates
                    .withAggregationType( AggregationType.COUNT )
                    .withTimely( ( REPORTING_RATE_ON_TIME == metric || ACTUAL_REPORTS_ON_TIME == metric ) ).build();

                addReportingRates( dataSourceParams, grid, metric );
            }
        }
    }

    /**
     * Adds reporting rates to the given grid based on the given data query
     * parameters and reporting rate metric.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     * @param metric the reporting rate metric.
     */
    private void addReportingRates( DataQueryParams params, Grid grid, ReportingRateMetric metric )
    {
        if ( !params.getReportingRates().isEmpty() && !params.isSkipData() )
        {
            if ( !COMPLETENESS_DIMENSION_TYPES.containsAll( params.getDimensionTypes() ) )
            {
                return;
            }

            DataQueryParams targetParams = DataQueryParams.newBuilder( params )
                .withSkipPartitioning( true )
                .withTimely( false )
                .withRestrictByOrgUnitOpeningClosedDate( true )
                .withRestrictByCategoryOptionStartEndDate( true )
                .withAggregationType( AggregationType.SUM ).build();

            Map<String, Double> targetMap = getAggregatedCompletenessTargetMap( targetParams );

            Map<String, Double> dataMap = metric != EXPECTED_REPORTS ? getAggregatedCompletenessValueMap( params ) : new HashMap<>();

            Integer periodIndex = params.getPeriodDimensionIndex();
            Integer dataSetIndex = DataQueryParams.DX_INDEX;
            Map<String, PeriodType> dsPtMap = params.getDataSetPeriodTypeMap();
            PeriodType filterPeriodType = params.getFilterPeriodType();

            for ( Map.Entry<String, Double> entry : targetMap.entrySet() )
            {
                List<String> dataRow = Lists.newArrayList( entry.getKey().split( DIMENSION_SEP ) );

                Double target = entry.getValue();
                Double actual = dataMap.get( entry.getKey() );

                if ( target != null && ( actual != null || metric == EXPECTED_REPORTS ) )
                {
                    // ---------------------------------------------------------
                    // Multiply target value by number of periods in time span
                    // ---------------------------------------------------------

                    PeriodType queryPt = filterPeriodType != null ? filterPeriodType : getPeriodTypeFromIsoString( dataRow.get( periodIndex ) );
                    PeriodType dataSetPt = dsPtMap.get( dataRow.get( dataSetIndex ) );
                    target = target * queryPt.getPeriodSpan( dataSetPt );

                    // ---------------------------------------------------------
                    // Calculate reporting rate and replace data set with rate
                    // ---------------------------------------------------------

                    Double value = 0d;

                    if ( EXPECTED_REPORTS == metric )
                    {
                        value = target;
                    }
                    else if ( ACTUAL_REPORTS == metric || ACTUAL_REPORTS_ON_TIME == metric )
                    {
                        value = actual;
                    }
                    else if ( !MathUtils.isZero( target) ) // REPORTING_RATE or REPORTING_RATE_ON_TIME
                    {
                        value = ( actual * PERCENT ) / target;
                    }

                    String reportingRate = DimensionalObjectUtils.getDimensionItem( dataRow.get( DX_INDEX ), metric );
                    dataRow.set( DX_INDEX, reportingRate );

                    grid.addRow()
                        .addValues( dataRow.toArray() )
                        .addValue( params.isSkipRounding() ? value : MathUtils.getRounded( value ) );

                    if ( params.isIncludeNumDen() )
                    {
                        grid.addValue( actual )
                            .addValue( target )
                            .addValue( PERCENT );
                    }
                }
            }
        }
    }

    /**
     * Adds program data element values to the given grid based on the given data
     * query parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void addProgramDataElementAttributeIndicatorValues( DataQueryParams params, Grid grid )
    {
        if ( ( !params.getAllProgramDataElementsAndAttributes().isEmpty() || !params.getProgramIndicators().isEmpty() ) && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                .retainDataDimensions( PROGRAM_DATA_ELEMENT, PROGRAM_ATTRIBUTE, PROGRAM_INDICATOR ).build();

            EventQueryParams eventQueryParams = new EventQueryParams.Builder( EventQueryParams.fromDataQueryParams( dataSourceParams ) )
                .withSkipMeta( true ).build();

            Grid eventGrid = eventAnalyticsService.getAggregatedEventData( eventQueryParams );

            grid.addRows( eventGrid );
        }
    }

    /**
     * Adds values to the given grid based on dynamic dimensions from the given
     * data query parameters. This assumes that no fixed dimensions are part of
     * the query.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void addDynamicDimensionValues( DataQueryParams params, Grid grid )
    {
        if ( params.getDataDimensionAndFilterOptions().isEmpty() && !params.isSkipData() )
        {
            Map<String, Double> aggregatedDataMap = getAggregatedDataValueMap( DataQueryParams.newBuilder( params )
                .withIncludeNumDen( false ).build() );

            for ( Map.Entry<String, Double> entry : aggregatedDataMap.entrySet() )
            {
                Double value = params.isSkipRounding() ? entry.getValue() : MathUtils.getRounded( entry.getValue() );

                grid.addRow()
                    .addValues( entry.getKey().split( DIMENSION_SEP ) )
                    .addValue( value );

                if ( params.isIncludeNumDen() )
                {
                    grid.addNullValues( 3 );
                }
            }
        }
    }

    /**
     * Adds meta data values to the given grid based on the given data query
     * parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void addMetaData( DataQueryParams params, Grid grid )
    {
        if ( !params.isSkipMeta() )
        {
            Map<String, Object> metaData = new HashMap<>();
            Map<String, Object> internalMetaData = new HashMap<>();

            // -----------------------------------------------------------------
            // Items / names element
            // -----------------------------------------------------------------

            Map<String, String> cocNameMap = AnalyticsUtils.getCocNameMap( params );
            
            if ( params.getApiVersion().ge( DhisApiVersion.V26 ) )
            {
                metaData.put( AnalyticsMetaDataKey.ITEMS.getKey(), AnalyticsUtils.getDimensionMetadataItemMap( params ) );
            }
            else
            {
                Map<String, String> uidNameMap = AnalyticsUtils.getDimensionItemNameMap( params );
                uidNameMap.putAll( cocNameMap );
                uidNameMap.put( DATA_X_DIM_ID, DISPLAY_NAME_DATA_X );
                metaData.put( AnalyticsMetaDataKey.NAMES.getKey(), uidNameMap );
            }

            // -----------------------------------------------------------------
            // Item order elements
            // -----------------------------------------------------------------

            Map<String, Object> dimensionItems = new HashMap<>();
            
            Calendar calendar = PeriodType.getCalendar();

            List<String> periodUids = calendar.isIso8601() ?
                getDimensionalItemIds( params.getDimensionOrFilterItems( PERIOD_DIM_ID ) ) :
                getLocalPeriodIdentifiers( params.getDimensionOrFilterItems( PERIOD_DIM_ID ), calendar );

            dimensionItems.put( PERIOD_DIM_ID, periodUids );
            dimensionItems.put( CATEGORYOPTIONCOMBO_DIM_ID, cocNameMap.keySet() );

            for ( DimensionalObject dim : params.getDimensionsAndFilters() )
            {
                if ( !dimensionItems.keySet().contains( dim.getDimension() ) )
                {
                    dimensionItems.put( dim.getDimension(), getDimensionalItemIds( dim.getItems() ) );
                }
            }
            
            if ( params.getApiVersion().ge( DhisApiVersion.V26 ) )
            {
                metaData.put( AnalyticsMetaDataKey.DIMENSIONS.getKey(), dimensionItems );
            }
            else
            {
                metaData.putAll( dimensionItems );
            }

            // -----------------------------------------------------------------
            // Organisation unit hierarchy
            // -----------------------------------------------------------------

            User user = securityManager.getCurrentUser( params );

            List<OrganisationUnit> organisationUnits = asTypedList( params.getDimensionOrFilterItems( ORGUNIT_DIM_ID ) );
            Collection<OrganisationUnit> roots = user != null ? user.getOrganisationUnits() : null;

            if ( params.isHierarchyMeta() )
            {
                metaData.put( AnalyticsMetaDataKey.ORG_UNIT_HIERARCHY.getKey(), getParentGraphMap( organisationUnits, roots ) );
            }

            if ( params.isShowHierarchy() )
            {
                Map<Object, List<?>> ancestorMap = organisationUnits
                    .stream().collect( Collectors.toMap( OrganisationUnit::getUid, ou -> ou.getAncestorNames( roots, true ) ) );
                
                internalMetaData.put( AnalyticsMetaDataKey.ORG_UNIT_ANCESTORS.getKey(), ancestorMap );
                
                metaData.put( AnalyticsMetaDataKey.ORG_UNIT_NAME_HIERARCHY.getKey(), getParentNameGraphMap( organisationUnits, roots, true ) );
            }

            grid.setMetaData( ImmutableMap.copyOf( metaData ) );
            grid.setInternalMetaData( ImmutableMap.copyOf( internalMetaData ) );
        }
    }

    /**
     * Prepares the given grid to be converted to a data value set, given
     * that the output format is of type DATA_VALUE_SET.
     * 
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void handleDataValueSet( DataQueryParams params, Grid grid )
    {
        if ( params.isOutputFormat( OutputFormat.DATA_VALUE_SET ) && !params.isSkipHeaders() )
        {
            AnalyticsUtils.handleGridForDataValueSet( params, grid );
        }
    }

    /**
     * Substitutes the meta data of the grid with the identifier scheme meta data
     * property indicated in the query.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void applyIdScheme( DataQueryParams params, Grid grid )
    {
        if ( !params.isSkipMeta() && params.hasNonUidOutputIdScheme() )
        {
            Map<String, String> map = DimensionalObjectUtils.getDimensionItemIdSchemeMap( params.getAllDimensionItems(), params.getOutputIdScheme() );

            if ( params.isOutputFormat( OutputFormat.DATA_VALUE_SET ) && !params.getDataElementOperands().isEmpty() )
            {
                map.putAll( DimensionalObjectUtils.getDataElementOperandIdSchemeMap( asTypedList( params.getDataElementOperands() ), params.getOutputIdScheme() ) );
            }

            grid.substituteMetaData( map );
        }
    }

    /**
     * Returns a Grid with aggregated data in table layout.
     *
     * @param params the {@link DataQueryParams}.
     * @param columns the column dimensions.
     * @param rows the row dimensions.
     * @return a Grid with aggregated data in table layout.
     */
    private Grid getAggregatedDataValuesTableLayout( DataQueryParams params, List<String> columns, List<String> rows )
    {
        params.setOutputIdScheme( null );

        Grid grid = getAggregatedDataValues( params );

        ListUtils.removeEmptys( columns );
        ListUtils.removeEmptys( rows );

        queryPlanner.validateTableLayout( params, columns, rows );

        ReportTable reportTable = new ReportTable();

        List<DimensionalItemObject[]> tableColumns = new ArrayList<>();
        List<DimensionalItemObject[]> tableRows = new ArrayList<>();

        if ( columns != null )
        {
            for ( String dimension : columns )
            {
                reportTable.getColumnDimensions().add( dimension );
                tableColumns.add( params.getDimensionItemArrayExplodeCoc( dimension ) );
            }
        }

        if ( rows != null )
        {
            for ( String dimension : rows )
            {
                reportTable.getRowDimensions().add( dimension );
                tableRows.add( params.getDimensionItemArrayExplodeCoc( dimension ) );
            }
        }

        reportTable
            .setGridTitle( IdentifiableObjectUtils.join( params.getFilterItems() ) )
            .setGridColumns( new CombinationGenerator<>( tableColumns.toArray( IRT2D ) ).getCombinations() )
            .setGridRows( new CombinationGenerator<>( tableRows.toArray( IRT2D ) ).getCombinations() );

        addListIfEmpty( reportTable.getGridColumns() );
        addListIfEmpty( reportTable.getGridRows() );
        
        reportTable.setHideEmptyRows( params.isHideEmptyRows() );
        reportTable.setHideEmptyColumns( params.isHideEmptyColumns() );
        reportTable.setShowHierarchy( params.isShowHierarchy() );

        Map<String, Object> valueMap = AnalyticsUtils.getAggregatedDataValueMapping( grid );

        return reportTable.getGrid( new ListGrid( grid.getMetaData(), grid.getInternalMetaData() ), valueMap, params.getDisplayProperty(), false );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Generates a mapping of permutations keys (organisation unit id or null)
     * and mappings of organisation unit group and counts.
     *
     * @param params the {@link DataQueryParams}.
     * @param indicators the indicators for which formulas to scan for organisation
     *        unit groups.
     * @return a map of maps.
     */
    private Map<String, Map<String, Integer>> getOrgUnitTargetMap( DataQueryParams params, Collection<Indicator> indicators )
    {
        Set<OrganisationUnitGroup> orgUnitGroups = expressionService.getOrganisationUnitGroupsInIndicators( indicators );

        if ( orgUnitGroups.isEmpty() )
        {
            return null;
        }

        DataQueryParams orgUnitTargetParams = DataQueryParams.newBuilder( params )
            .pruneToDimensionType( DimensionType.ORGANISATION_UNIT )
            .addDimension( new BaseDimensionalObject( DimensionalObject.ORGUNIT_GROUP_DIM_ID, null, new ArrayList<DimensionalItemObject>( orgUnitGroups ) ) )
            .withSkipPartitioning( true ).build();

        Map<String, Double> orgUnitCountMap = getAggregatedOrganisationUnitTargetMap( orgUnitTargetParams );

        return DataQueryParams.getPermutationOrgUnitGroupCountMap( orgUnitCountMap );
    }

    /**
     * Generates aggregated values for the given query. Creates a mapping between
     * a dimension key and the aggregated value. The dimension key is a
     * concatenation of the identifiers of the dimension items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between a dimension key and the aggregated value.
     */
    private Map<String, Double> getAggregatedDataValueMap( DataQueryParams params )
    {
        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.DATA_VALUE.getTableName(), Lists.newArrayList() ) );
    }

    /**
     * Generates aggregated values for the given query. Creates a mapping between
     * a dimension key and the aggregated value. The dimension key is a
     * concatenation of the identifiers of the dimension items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between a dimension key and the aggregated value.
     */
    private Map<String, Object> getAggregatedDataValueMapObjectTyped( DataQueryParams params )
    {
        return getAggregatedValueMap( params, AnalyticsTableType.DATA_VALUE.getTableName(), Lists.newArrayList() );
    }

    /**
     * Generates aggregated values for the given query. Creates a mapping between
     * a dimension key and the aggregated value. The dimension key is a
     * concatenation of the identifiers of the dimension items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between a dimension key and the aggregated value.
     */
    private Map<String, Double> getAggregatedCompletenessValueMap( DataQueryParams params )
    {
        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.COMPLETENESS.getTableName(), Lists.newArrayList() ) );
    }

    /**
     * Generates a mapping between the the data set dimension key and the count
     * of expected data sets to report.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between the the data set dimension key and the count of
     *         expected data sets to report.
     */
    private Map<String, Double> getAggregatedCompletenessTargetMap( DataQueryParams params )
    {
        List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers = Lists.newArrayList();
        queryGroupers.add( q -> queryPlanner.groupByStartEndDate( q ) );

        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.COMPLETENESS_TARGET.getTableName(), queryGroupers ) );
    }

    /**
     * Generates a mapping between the the organisation unit dimension key and the 
     * count of organisation units inside the subtree of the given organisation units and
     * members of the given organisation unit groups.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between the the data set dimension key and the count of
     *         expected data sets to report.
     */
    private Map<String, Double> getAggregatedOrganisationUnitTargetMap( DataQueryParams params )
    {
        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.ORG_UNIT_TARGET.getTableName(), Lists.newArrayList() ) );
    }

    /**
     * Generates a mapping between a dimension key and the aggregated value. The
     * dimension key is a concatenation of the identifiers of the dimension items
     * separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @param tableName the table name to use for the query.
     * @param queryGroupers the list of additional query groupers to use for
     *        query planning, use empty list for none.
     * @return a mapping between a dimension key and aggregated values.
     */
    private Map<String, Object> getAggregatedValueMap( DataQueryParams params, String tableName, List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers )
    {
        queryPlanner.validateMaintenanceMode();

        int optimalQueries = MathUtils.getWithin( getProcessNo(), 1, MAX_QUERIES );

        int maxLimit = params.isIgnoreLimit() ? 0 : (Integer) systemSettingManager.getSystemSetting( SettingKey.ANALYTICS_MAX_LIMIT );

        Timer timer = new Timer().start().disablePrint();

        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder()
            .withOptimalQueries( optimalQueries )
            .withTableName( tableName )
            .withQueryGroupers( queryGroupers ).build();

        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );

        timer.getSplitTime( "Planned analytics query, got: " + queryGroups.getLargestGroupSize() + " for optimal: " + optimalQueries );

        Map<String, Object> map = new HashMap<>();

        for ( List<DataQueryParams> queries : queryGroups.getSequentialQueries() )
        {
            List<Future<Map<String, Object>>> futures = new ArrayList<>();

            for ( DataQueryParams query : queries )
            {
                futures.add( analyticsManager.getAggregatedDataValues( query, maxLimit ) );
            }

            for ( Future<Map<String, Object>> future : futures )
            {
                try
                {
                    Map<String, Object> taskValues = future.get();

                    if ( taskValues != null )
                    {
                        map.putAll( taskValues );
                    }
                }
                catch ( Exception ex )
                {
                    log.error( DebugUtils.getStackTrace( ex ) );
                    log.error( DebugUtils.getStackTrace( ex.getCause() ) );

                    throw new RuntimeException( "Error during execution of aggregation query task", ex );
                }
            }
        }

        timer.getTime( "Got analytics values" );

        return map;
    }
    
    /**
     * Returns headers, raw data and meta data as a grid.
     * 
     * @param params the {@link DataQueryParams}.
     * @return a grid.
     */
    private Grid getRawDataGrid( DataQueryParams params )
    {
        Grid grid = new ListGrid();
        
        addHeaders( params, grid );
        
        addRawData( params, grid );
        
        addMetaData( params, grid );
        
        applyIdScheme( params, grid );
        
        return grid;
    }
    
    /**
     * Adds raw data to the grid for the given data query parameters.
     * 
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    private void addRawData( DataQueryParams params, Grid grid )
    {
        if ( !params.isSkipData() )
        {
            QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder()
                .withTableName( AnalyticsTableType.DATA_VALUE.getTableName() ).build();
            
            List<DataQueryParams> queries = queryPlanner.groupByPartition( params, plannerParams );
            
            queries.forEach( query -> rawAnalyticsManager.getRawDataValues( query, grid ) );
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a mapping of permutation keys and mappings of data element operands
     * and values based on the given query.
     *
     * @param params the {@link DataQueryParams}.
     */
    private Map<String, Map<DimensionalItemObject, Double>> getPermutationDimensionItemValueMap( DataQueryParams params )
    {
        List<Indicator> indicators = asTypedList( params.getIndicators() );

        Map<String, Double> valueMap = getAggregatedDataValueMap( params, indicators );

        return DataQueryParams.getPermutationDimensionalItemValueMap( valueMap );
    }

    /**
     * Returns a mapping between dimension items and values for the given data
     * query and list of indicators. The dimensional items part of the indicator
     * numerators and denominators are used as dimensional item for the aggregated
     * values being retrieved.
     *
     * @param params the {@link DataQueryParams}.
     * @param indicators the list of indicators.
     * @return a dimensional items to aggregate values map.
     */
    private Map<String, Double> getAggregatedDataValueMap( DataQueryParams params, List<Indicator> indicators )
    {
        List<DimensionalItemObject> items = Lists.newArrayList( expressionService.getDimensionalItemObjectsInIndicators( indicators ) );

        items = DimensionalObjectUtils.replaceOperandTotalsWithDataElements( items );

        DimensionalObject dimension = new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, DISPLAY_NAME_DATA_X, items );

        DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
            .replaceDimension( dimension )
            .withIncludeNumDen( false )
            .withSkipHeaders( true )
            .withSkipMeta( true ).build();

        Grid grid = getAggregatedDataValueGridInternal( dataSourceParams );

        return grid.getAsMap( grid.getWidth() - 1, DimensionalObject.DIMENSION_SEP );
    }
    
    /**
     * Gets the number of available cores. Uses explicit number from system
     * setting if available. Detects number of cores from current server runtime
     * if not.
     *
     * @return the number of available cores.
     */
    private int getProcessNo()
    {
        Integer cores = (Integer) systemSettingManager.getSystemSetting( SettingKey.DATABASE_SERVER_CPUS );

        return (cores == null || cores == 0) ? SystemUtils.getCpuCores() : cores;
    }
}
