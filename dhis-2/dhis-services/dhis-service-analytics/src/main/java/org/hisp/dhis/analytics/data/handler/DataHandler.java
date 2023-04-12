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
package org.hisp.dhis.analytics.data.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Math.min;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.remove;
import static org.apache.commons.lang3.StringUtils.join;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.COUNT;
import static org.hisp.dhis.analytics.AnalyticsAggregationType.SUM;
import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS;
import static org.hisp.dhis.analytics.AnalyticsTableType.COMPLETENESS_TARGET;
import static org.hisp.dhis.analytics.AnalyticsTableType.DATA_VALUE;
import static org.hisp.dhis.analytics.AnalyticsTableType.ORG_UNIT_TARGET;
import static org.hisp.dhis.analytics.AnalyticsTableType.VALIDATION_RESULT;
import static org.hisp.dhis.analytics.DataQueryParams.COMPLETENESS_DIMENSION_TYPES;
import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DX_INDEX;
import static org.hisp.dhis.analytics.DataQueryParams.NUMERATOR_DENOMINATOR_PROPERTIES_COUNT;
import static org.hisp.dhis.analytics.DataQueryParams.getPermutationDimensionalItemValueMap;
import static org.hisp.dhis.analytics.DataQueryParams.getPermutationOrgUnitGroupCountMap;
import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.analytics.DimensionItem.asItemKey;
import static org.hisp.dhis.analytics.DimensionItem.getItemIdentifiers;
import static org.hisp.dhis.analytics.DimensionItem.getOrganisationUnitItem;
import static org.hisp.dhis.analytics.DimensionItem.getPeriodItem;
import static org.hisp.dhis.analytics.OutputFormat.ANALYTICS;
import static org.hisp.dhis.analytics.event.EventQueryParams.fromDataQueryParams;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.convertDxToOperand;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.findDimensionalItems;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getDoubleMap;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getRoundedValue;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.getRoundedValueObject;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.hasPeriod;
import static org.hisp.dhis.analytics.util.AnalyticsUtils.isPeriodInPeriods;
import static org.hisp.dhis.analytics.util.PeriodOffsetUtils.buildYearToDateRows;
import static org.hisp.dhis.analytics.util.PeriodOffsetUtils.getPeriodOffsetRow;
import static org.hisp.dhis.analytics.util.PeriodOffsetUtils.isYearToDate;
import static org.hisp.dhis.analytics.util.ReportRatesHelper.getCalculatedTarget;
import static org.hisp.dhis.common.DataDimensionItemType.DATA_ELEMENT;
import static org.hisp.dhis.common.DataDimensionItemType.DATA_ELEMENT_OPERAND;
import static org.hisp.dhis.common.DataDimensionItemType.INDICATOR;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_ATTRIBUTE;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_DATA_ELEMENT;
import static org.hisp.dhis.common.DataDimensionItemType.PROGRAM_INDICATOR;
import static org.hisp.dhis.common.DataDimensionItemType.VALIDATION_RULE;
import static org.hisp.dhis.common.DimensionType.ATTRIBUTE_OPTION_COMBO;
import static org.hisp.dhis.common.DimensionType.CATEGORY_OPTION_COMBO;
import static org.hisp.dhis.common.DimensionType.DATA_X;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT;
import static org.hisp.dhis.common.DimensionType.ORGANISATION_UNIT_GROUP;
import static org.hisp.dhis.common.DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_GROUP_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;
import static org.hisp.dhis.common.DimensionalObjectUtils.convertToDimItemValueMap;
import static org.hisp.dhis.common.DimensionalObjectUtils.getAttributeOptionCombos;
import static org.hisp.dhis.common.DimensionalObjectUtils.getCategoryOptionCombos;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDataElements;
import static org.hisp.dhis.common.DimensionalObjectUtils.getDimensionItem;
import static org.hisp.dhis.common.ReportingRateMetric.ACTUAL_REPORTS;
import static org.hisp.dhis.common.ReportingRateMetric.ACTUAL_REPORTS_ON_TIME;
import static org.hisp.dhis.common.ReportingRateMetric.EXPECTED_REPORTS;
import static org.hisp.dhis.common.ReportingRateMetric.REPORTING_RATE_ON_TIME;
import static org.hisp.dhis.commons.util.DebugUtils.getStackTrace;
import static org.hisp.dhis.commons.util.SystemUtils.getCpuCores;
import static org.hisp.dhis.dataelement.DataElementOperand.TotalType.values;
import static org.hisp.dhis.period.PeriodType.getPeriodTypeFromIsoString;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_MAX_LIMIT;
import static org.hisp.dhis.setting.SettingKey.DATABASE_SERVER_CPUS;
import static org.hisp.dhis.system.grid.GridUtils.getGridIndexByDimensionItem;
import static org.hisp.dhis.system.util.MathUtils.getWithin;
import static org.hisp.dhis.system.util.MathUtils.isZero;
import static org.hisp.dhis.util.ObjectUtils.firstNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.hisp.dhis.analytics.AnalyticsManager;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryGroups;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.DimensionItem;
import org.hisp.dhis.analytics.QueryPlanner;
import org.hisp.dhis.analytics.QueryPlannerParams;
import org.hisp.dhis.analytics.QueryValidator;
import org.hisp.dhis.analytics.RawAnalyticsManager;
import org.hisp.dhis.analytics.analyze.ExecutionPlanStore;
import org.hisp.dhis.analytics.event.EventAnalyticsService;
import org.hisp.dhis.analytics.event.EventQueryParams;
import org.hisp.dhis.analytics.resolver.ExpressionResolver;
import org.hisp.dhis.analytics.resolver.ExpressionResolvers;
import org.hisp.dhis.analytics.util.PeriodOffsetUtils;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DimensionItemObjectValue;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.ExecutionPlan;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.dataelement.DataElementOperand.TotalType;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.util.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * This component is responsible for handling and retrieving data based on the
 * input provided to the public methods. The main goal is to correctly populate
 * the data into the Grid object.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataHandler
{
    private static final int MAX_QUERIES = 8;

    private static final int PERCENT = 100;

    private final EventAnalyticsService eventAnalyticsService;

    private final RawAnalyticsManager rawAnalyticsManager;

    private final ExpressionResolvers resolvers;

    private final ExpressionService expressionService;

    private final QueryPlanner queryPlanner;

    private final QueryValidator queryValidator;

    private final SystemSettingManager systemSettingManager;

    private final AnalyticsManager analyticsManager;

    private final OrganisationUnitService organisationUnitService;

    private DataAggregator dataAggregator;

    private final ExecutionPlanStore executionPlanStore;

    /**
     * Adds performance metrics.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the {@link Grid} to add performance metrics to.
     */
    void addPerformanceMetrics( DataQueryParams params, Grid grid )
    {
        if ( params.analyzeOnly() )
        {
            String key = params.getExplainOrderId();

            List<ExecutionPlan> plans = executionPlanStore.getExecutionPlans( key );

            grid.addPerformanceMetrics( plans );

            executionPlanStore.removeExecutionPlans( key );
        }
    }

    /**
     * Adds indicator values to the given grid based on the given data query
     * parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the {@link Grid}.
     */
    @Transactional( readOnly = true )
    public void addIndicatorValues( DataQueryParams params, Grid grid )
    {
        if ( !params.getIndicators().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = newBuilder( params )
                .retainDataDimension( INDICATOR )
                .withIncludeNumDen( false ).build();

            List<Indicator> indicators = resolveIndicatorExpressions( dataSourceParams );

            // Try to get filters periods from dimension (pe), or else fall back
            // to "startDate/endDate" periods

            List<Period> filterPeriods = isNotEmpty( dataSourceParams.getTypedFilterPeriods() )
                ? dataSourceParams.getTypedFilterPeriods()
                : dataSourceParams.getStartEndDatesToSingleList();

            // -----------------------------------------------------------------
            // Get indicator values
            // -----------------------------------------------------------------

            Map<String, Map<String, Integer>> permutationOrgUnitTargetMap = getOrgUnitTargetMap( dataSourceParams,
                indicators );

            List<List<DimensionItem>> dimensionItemPermutations = dataSourceParams.getDimensionItemPermutations();

            Map<DimensionalItemId, DimensionalItemObject> itemMap = expressionService
                .getIndicatorDimensionalItemMap( indicators );

            Map<String, List<DimensionItemObjectValue>> permutationDimensionItemValueMap = getPermutationDimensionItemValueMap(
                params, new ArrayList<>( itemMap.values() ) );

            handleEmptyDimensionItemPermutations( dimensionItemPermutations );

            for ( Indicator indicator : indicators )
            {
                for ( List<DimensionItem> dimensionItems : dimensionItemPermutations )
                {
                    IndicatorValue value = getIndicatorValue( filterPeriods, itemMap,
                        permutationOrgUnitTargetMap, permutationDimensionItemValueMap, indicator, dimensionItems );

                    addIndicatorValuesToGrid( params, grid, dataSourceParams, indicator, dimensionItems, value );
                }
            }
        }
    }

    /**
     * Based on the given indicator plus additional parameters, this method will
     * find the respective IndicatorValue.
     *
     * @param filterPeriods the filter periods. See
     *        {@link ConstantService#getConstantMap()}.
     * @param permutationOrgUnitTargetMap the org unit permutation map. See
     *        {@link #getOrgUnitTargetMap(DataQueryParams, Collection)}.
     * @param itemMap Every dimensional item to process.
     * @param permutationDimensionItemValueMap the dimension item permutation
     *        map. See
     *        {@link #getPermutationDimensionItemValueMap(DataQueryParams,
     *        List<DimensionalItemObject>)}.
     * @param indicator the input Indicator where the IndicatorValue will be
     *        based.
     * @param dimensionItems the dimensional items permutation map. See
     *        {@link DataQueryParams#getDimensionItemPermutations()}.
     * @return the IndicatorValue
     */
    private IndicatorValue getIndicatorValue( List<Period> filterPeriods,
        Map<DimensionalItemId, DimensionalItemObject> itemMap,
        Map<String, Map<String, Integer>> permutationOrgUnitTargetMap,
        Map<String, List<DimensionItemObjectValue>> permutationDimensionItemValueMap,
        Indicator indicator, List<DimensionItem> dimensionItems )
    {
        String permKey = asItemKey( dimensionItems );

        final List<DimensionItemObjectValue> values = permutationDimensionItemValueMap
            .getOrDefault( permKey, new ArrayList<>() );

        List<Period> periods = !filterPeriods.isEmpty() ? filterPeriods
            : singletonList( (Period) getPeriodItem( dimensionItems ) );

        OrganisationUnit unit = (OrganisationUnit) getOrganisationUnitItem( dimensionItems );

        String ou = unit != null ? unit.getUid() : null;

        Map<String, Integer> orgUnitCountMap = permutationOrgUnitTargetMap != null
            ? permutationOrgUnitTargetMap.get( ou )
            : null;

        return expressionService.getIndicatorValueObject( indicator, periods, itemMap,
            convertToDimItemValueMap( values ), orgUnitCountMap );
    }

    /**
     * Adds data element values to the given grid based on the given data query
     * parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    @Transactional( readOnly = true )
    public void addDataElementValues( DataQueryParams params, Grid grid )
    {
        if ( !params.getAllDataElements().isEmpty() && (!params.isSkipData() || params.analyzeOnly()) )
        {
            DataQueryParams dataSourceParams = newBuilder( params )
                .retainDataDimension( DATA_ELEMENT )
                .withIncludeNumDen( false ).build();

            Map<String, Object> aggregatedDataMap = getAggregatedDataValueMapObjectTyped( dataSourceParams );

            for ( Map.Entry<String, Object> entry : aggregatedDataMap.entrySet() )
            {
                Object value = getRoundedValueObject( params, entry.getValue() );

                grid.addRow()
                    .addValues( entry.getKey().split( DIMENSION_SEP ) )
                    .addValue( value );

                if ( params.isIncludeNumDen() )
                {
                    grid.addNullValues( NUMERATOR_DENOMINATOR_PROPERTIES_COUNT );
                }
            }
        }
    }

    /**
     * Adds program data element values to the given grid based on the given
     * data query parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    @Transactional( readOnly = true )
    public void addProgramDataElementAttributeIndicatorValues( DataQueryParams params, Grid grid )
    {
        if ( (!params.getAllProgramDataElementsAndAttributes().isEmpty() || !params.getProgramIndicators().isEmpty())
            && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = newBuilder( params )
                .retainDataDimensions( PROGRAM_DATA_ELEMENT, PROGRAM_ATTRIBUTE, PROGRAM_INDICATOR ).build();

            EventQueryParams eventQueryParams = new EventQueryParams.Builder( fromDataQueryParams( dataSourceParams ) )
                .withSkipMeta( true ).build();

            Grid eventGrid = eventAnalyticsService.getAggregatedEventData( eventQueryParams );

            grid.addRows( eventGrid );

            replaceGridIfNeeded( grid, eventGrid );
        }
    }

    /**
     * This method will replace the headers in the current grid by the event
     * grid IF, and only IF, there is a mismatch between the current grid and
     * the event grid headers.
     *
     * @param grid the current/actual grid
     * @param eventGrid the event grid
     */
    private void replaceGridIfNeeded( final Grid grid, final Grid eventGrid )
    {
        final boolean eventGridHasAdditionalHeaders = grid.getHeaderWidth() < eventGrid.getHeaderWidth();
        final boolean eventHeaderSizeIsSameAsGridColumns = eventGrid.getHeaderWidth() == eventGrid.getWidth();

        // Replacing the current grid headers by the actual event grid headers.
        if ( eventGridHasAdditionalHeaders && eventHeaderSizeIsSameAsGridColumns )
        {
            grid.replaceHeaders( eventGrid.getHeaders() );
        }
    }

    /**
     * Adds reporting rates to the given grid based on the given data query
     * parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    @Transactional( readOnly = true )
    public void addReportingRates( DataQueryParams params, Grid grid )
    {
        if ( !params.getReportingRates().isEmpty() && !params.isSkipData() )
        {
            for ( ReportingRateMetric metric : ReportingRateMetric.values() )
            {
                DataQueryParams dataSourceParams = newBuilder( params )
                    .retainDataDimensionReportingRates( metric )
                    .ignoreDataApproval() // No approval for reporting rates
                    .withAggregationType( COUNT )
                    .withTimely( (REPORTING_RATE_ON_TIME == metric || ACTUAL_REPORTS_ON_TIME == metric) ).build();

                addReportingRates( dataSourceParams, grid, metric );
            }
        }
    }

    /**
     * Adds data element operand values to the given grid based on the given
     * data query parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    @Transactional( readOnly = true )
    public void addDataElementOperandValues( DataQueryParams params, Grid grid )
    {
        if ( !params.getDataElementOperands().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = newBuilder( params )
                .retainDataDimension( DATA_ELEMENT_OPERAND ).build();

            for ( TotalType type : values() )
            {
                addDataElementOperandValues( dataSourceParams, grid, type );
            }
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
    @Transactional( readOnly = true )
    public void addDynamicDimensionValues( DataQueryParams params, Grid grid )
    {
        if ( params.getDataDimensionAndFilterOptions().isEmpty() && !params.isSkipData() )
        {
            Map<String, Double> aggregatedDataMap = getAggregatedDataValueMap( newBuilder( params )
                .withIncludeNumDen( false ).build() );

            fillGridWithAggregatedDataMap( params, grid, aggregatedDataMap );
        }
    }

    /**
     * Adds validation results to the given grid based on the given data query
     * parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    @Transactional( readOnly = true )
    public void addValidationResultValues( DataQueryParams params, Grid grid )
    {
        if ( !params.getAllValidationResults().isEmpty() && !params.isSkipData() )
        {
            DataQueryParams dataSourceParams = newBuilder( params )
                .retainDataDimension( VALIDATION_RULE )
                .withAggregationType( COUNT )
                .withIncludeNumDen( false ).build();

            Map<String, Double> aggregatedDataMap = getAggregatedValidationResultMapObjectTyped( dataSourceParams );

            fillGridWithAggregatedDataMap( params, grid, aggregatedDataMap );
        }
    }

    /**
     * Adds raw data to the grid for the given data query parameters.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid.
     */
    @Transactional( readOnly = true )
    public void addRawData( DataQueryParams params, Grid grid )
    {
        if ( !params.isSkipData() )
        {
            QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder()
                .withTableType( DATA_VALUE ).build();

            params = queryPlanner.withTableNameAndPartitions( params, plannerParams );

            rawAnalyticsManager.getRawDataValues( params, grid );
        }
    }

    /**
     * Prepares the given data query parameters.
     *
     * @param params the {@link DataQueryParams}.
     */
    @Transactional( readOnly = true )
    public DataQueryParams prepareForRawDataQuery( DataQueryParams params )
    {
        DataQueryParams.Builder builder = newBuilder( params )
            .withEarliestStartDateLatestEndDate()
            .withPeriodDimensionWithoutOptions()
            .withIncludePeriodStartEndDates( true );

        if ( params.isShowHierarchy() )
        {
            builder.withOrgUnitLevels( organisationUnitService.getFilledOrganisationUnitLevels() );
        }

        return builder.build();
    }

    /**
     * Generates a mapping between the count of a validation result.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between validation results and counts of them
     */
    private Map<String, Double> getAggregatedValidationResultMapObjectTyped( DataQueryParams params )
    {
        return getDoubleMap( getAggregatedValueMap( params, VALIDATION_RESULT, newArrayList() ) );
    }

    /**
     * Fill grid with aggregated data map with key and value
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the grid
     * @param aggregatedDataMap the aggregated data map
     */
    private void fillGridWithAggregatedDataMap( DataQueryParams params, Grid grid,
        Map<String, Double> aggregatedDataMap )
    {
        for ( Map.Entry<String, Double> entry : aggregatedDataMap.entrySet() )
        {
            Number value = params.isSkipRounding() ? entry.getValue()
                : (Number) getRoundedValueObject( params, entry.getValue() );

            grid.addRow()
                .addValues( entry.getKey().split( DIMENSION_SEP ) )
                .addValue( value );

            if ( params.isIncludeNumDen() )
            {
                grid.addNullValues( NUMERATOR_DENOMINATOR_PROPERTIES_COUNT );
            }
        }
    }

    /**
     * Adds reporting rates to the given grid based on the given data query
     * parameters and reporting rate metric.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the {@link Grid}.
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

            DataQueryParams targetParams = newBuilder( params )
                .withSkipPartitioning( true )
                .withTimely( false )
                .withRestrictByOrgUnitOpeningClosedDate( true )
                .withRestrictByCategoryOptionStartEndDate( true )
                .withAggregationType( SUM ).build();

            Map<String, Double> targetMap = getAggregatedCompletenessTargetMap( targetParams );

            Map<String, Double> dataMap = metric != EXPECTED_REPORTS
                ? getAggregatedCompletenessValueMap( params )
                : new HashMap<>();

            Integer periodIndex = params.getPeriodDimensionIndex();
            Integer dataSetIndex = DX_INDEX;
            Map<String, PeriodType> dsPtMap = params.getDataSetPeriodTypeMap();
            PeriodType filterPeriodType = params.getFilterPeriodType();

            int timeUnits = getTimeUnits( params );

            for ( Map.Entry<String, Double> entry : targetMap.entrySet() )
            {
                List<String> dataRow = newArrayList( entry.getKey().split( DIMENSION_SEP ) );

                Double target = entry.getValue();
                Double actual = firstNonNull( dataMap.get( entry.getKey() ), 0d );

                if ( target != null )
                {
                    // ---------------------------------------------------------
                    // Multiply target value by number of periods in time span
                    // ---------------------------------------------------------

                    PeriodType queryPt = filterPeriodType != null ? filterPeriodType
                        : getPeriodTypeFromIsoString( dataRow.get( periodIndex ) );
                    PeriodType dataSetPt = dsPtMap.get( dataRow.get( dataSetIndex ) );

                    target = getCalculatedTarget( periodIndex, timeUnits, dataRow, target, queryPt, dataSetPt,
                        params.getFilterPeriods() );

                    addReportRateToGrid( params, grid, metric, dataRow, target, actual );
                }
            }
        }
    }

    /**
     * Returns the number of filter periods, or 1 if no filter periods exist.
     *
     * @param params the {@link DataQueryParams}.
     * @return the number of filter periods, or 1 if no filter periods exist.
     */
    private int getTimeUnits( DataQueryParams params )
    {
        return params.hasFilter( PERIOD_DIM_ID ) ? params.getFilterPeriods().size() : 1;
    }

    /**
     * Calculates reporting rate and replace data set with rate and add the rate
     * to the Grid.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the {@link Grid} to be manipulated.
     * @param metric the object to assist with the report rate calculation.
     * @param dataRow the current dataRow, based on the key map built by
     *        {@link #getAggregatedCompletenessTargetMap(DataQueryParams)).
     * @param target the current value of the respective key ("dataRow"). See
     * @param actual the current actual value from
     *        {@link #getAggregatedCompletenessValueMap(DataQueryParams)} or
     *        zero (default).
     */
    private void addReportRateToGrid( DataQueryParams params, Grid grid, ReportingRateMetric metric,
        List<String> dataRow, Double target, Double actual )
    {
        Double value = getReportingRate( metric, target, actual );

        String reportingRate = getDimensionItem( dataRow.get( DX_INDEX ), metric );
        dataRow.set( DX_INDEX, reportingRate );

        grid.addRow()
            .addValues( dataRow.toArray() )
            .addValue( params.isSkipRounding() ? value : getRoundedValueObject( params, value ) );

        if ( params.isIncludeNumDen() )
        {
            grid.addValue( actual )
                .addValue( target )
                .addValue( PERCENT )
                .addNullValues( 2 );
        }
    }

    /**
     * Calculates the reporting rate based on the given parameters.
     *
     * @param metric the {@link ReportingRateMetric}.
     * @param target the target value.
     * @param actual the actual value.
     * @return the reporting rate.
     */
    private Double getReportingRate( ReportingRateMetric metric, Double target, Double actual )
    {
        Double value = 0d;

        if ( EXPECTED_REPORTS == metric )
        {
            value = target;
        }
        else if ( ACTUAL_REPORTS == metric || ACTUAL_REPORTS_ON_TIME == metric )
        {
            value = actual;
        }
        else if ( !isZero( target ) )
        {
            // REPORTING_RATE or REPORTING_RATE_ON_TIME
            value = min( ((actual * PERCENT) / target), 100d );
        }

        return value;
    }

    /**
     * Generates aggregated values for the given query. Creates a mapping
     * between a dimension key and the aggregated value. The dimension key is a
     * concatenation of the identifiers of the dimension items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between a dimension key and the aggregated value.
     */
    private Map<String, Double> getAggregatedCompletenessValueMap( DataQueryParams params )
    {
        return getDoubleMap( getAggregatedValueMap( params, COMPLETENESS, newArrayList() ) );
    }

    /**
     * Generates a mapping between the data set dimension key and the count of
     * expected data sets to report.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between the data set dimension key and the count of
     *         expected data sets to report.
     */
    private Map<String, Double> getAggregatedCompletenessTargetMap( DataQueryParams params )
    {
        List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers = newArrayList();
        queryGroupers.add( q -> queryPlanner.groupByStartEndDateRestriction( q ) );

        return getDoubleMap( getAggregatedValueMap( params, COMPLETENESS_TARGET, queryGroupers ) );
    }

    /**
     * Adds data element operand values to the given grid.
     *
     * @param params the {@link DataQueryParams}.
     * @param grid the {@link Grid}.
     * @param totalType the operand {@link TotalType}.
     */
    private void addDataElementOperandValues( DataQueryParams params, Grid grid, TotalType totalType )
    {
        List<DataElementOperand> operands = asTypedList( params.getDataElementOperands() );
        operands = operands.stream().filter( o -> totalType.equals( o.getTotalType() ) ).collect( Collectors.toList() );

        if ( operands.isEmpty() )
        {
            return;
        }

        List<DimensionalItemObject> dataElements = newArrayList( getDataElements( operands ) );
        List<DimensionalItemObject> categoryOptionCombos = newArrayList( getCategoryOptionCombos( operands ) );
        List<DimensionalItemObject> attributeOptionCombos = newArrayList( getAttributeOptionCombos( operands ) );

        // TODO Check if data was dim or filter

        DataQueryParams.Builder builder = newBuilder( params )
            .removeDimension( DATA_X_DIM_ID )
            .addDimension( new BaseDimensionalObject( DATA_X_DIM_ID, DATA_X, dataElements ) );

        if ( totalType.isCategoryOptionCombo() )
        {
            builder.addDimension( new BaseDimensionalObject( CATEGORYOPTIONCOMBO_DIM_ID,
                CATEGORY_OPTION_COMBO, categoryOptionCombos ) );
        }

        if ( totalType.isAttributeOptionCombo() )
        {
            builder.addDimension( new BaseDimensionalObject( ATTRIBUTEOPTIONCOMBO_DIM_ID,
                ATTRIBUTE_OPTION_COMBO, attributeOptionCombos ) );
        }

        DataQueryParams operandParams = builder.build();

        Map<String, Object> aggregatedDataMap = getAggregatedDataValueMapObjectTyped( operandParams );

        aggregatedDataMap = convertDxToOperand( aggregatedDataMap, totalType );

        for ( Map.Entry<String, Object> entry : aggregatedDataMap.entrySet() )
        {
            Object value = getRoundedValueObject( operandParams, entry.getValue() );

            grid.addRow()
                .addValues( entry.getKey().split( DIMENSION_SEP ) )
                .addValue( value );

            if ( params.isIncludeNumDen() )
            {
                grid.addNullValues( NUMERATOR_DENOMINATOR_PROPERTIES_COUNT );
            }
        }
    }

    /**
     * Generates aggregated values for the given query. Creates a mapping
     * between a dimension key and the aggregated value. The dimension key is a
     * concatenation of the identifiers of the dimension items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between a dimension key and the aggregated value.
     */
    private Map<String, Object> getAggregatedDataValueMapObjectTyped( DataQueryParams params )
    {
        return getAggregatedValueMap( params, DATA_VALUE, newArrayList() );
    }

    /**
     * Returns a mapping of permutation keys and mappings of data element
     * operands and values based on the given query.
     *
     * @param params the {@link DataQueryParams}.
     */
    private Map<String, List<DimensionItemObjectValue>> getPermutationDimensionItemValueMap(
        DataQueryParams params, List<DimensionalItemObject> items )
    {
        MultiValuedMap<String, DimensionItemObjectValue> aggregatedDataMap = getAggregatedDataValueMap( params, items );

        return getPermutationDimensionalItemValueMap( aggregatedDataMap );
    }

    /**
     * Checks whether the measure criteria in query parameters is satisfied for
     * the given indicator value.
     *
     * @param params the {@link DataQueryParams}.
     * @param value the {@link IndicatorValue}.
     * @param indicator the {@link Indicator}.
     * @return true if all the measure criteria are satisfied for this indicator
     *         value, false otherwise.
     */
    private boolean satisfiesMeasureCriteria( DataQueryParams params, IndicatorValue value, Indicator indicator )
    {
        if ( !params.hasMeasureCriteria() || value == null )
        {
            return true;
        }

        Double indicatorRoundedValue = getRoundedValue( params, indicator.getDecimals(), value.getValue() )
            .doubleValue();

        return !params.getMeasureCriteria().entrySet().stream()
            .anyMatch( measureValue -> !measureValue.getKey()
                .measureIsValid( indicatorRoundedValue, measureValue.getValue() ) );
    }

    /**
     * Handles the case where there are no dimension item permutations by adding
     * an empty dimension item list to the permutations list. This state occurs
     * where there are only data or category option combo dimensions specified.
     *
     * @param dimensionItemPermutations list of {@link DimensionItem}
     *        permutations.
     */
    private void handleEmptyDimensionItemPermutations( List<List<DimensionItem>> dimensionItemPermutations )
    {
        if ( dimensionItemPermutations.isEmpty() )
        {
            dimensionItemPermutations.add( new ArrayList<>() );
        }
    }

    /**
     * Generates a mapping of permutations keys and mappings of organisation
     * unit group and counts.
     *
     * @param params the {@link DataQueryParams}.
     * @param indicators the indicators for which formulas to scan for
     *        organisation unit groups.
     * @return a map of maps.
     */
    private Map<String, Map<String, Integer>> getOrgUnitTargetMap( DataQueryParams params,
        Collection<Indicator> indicators )
    {
        List<OrganisationUnitGroup> orgUnitGroups = expressionService.getOrgUnitGroupCountGroups( indicators );

        if ( orgUnitGroups.isEmpty() )
        {
            return null;
        }

        DataQueryParams orgUnitTargetParams = newBuilder( params )
            .pruneToDimensionType( ORGANISATION_UNIT )
            .addDimension( new BaseDimensionalObject( ORGUNIT_GROUP_DIM_ID, ORGANISATION_UNIT_GROUP, orgUnitGroups ) )
            .withOutputFormat( ANALYTICS )
            .withSkipPartitioning( true )
            .withSkipDataDimensionValidation( true )
            .build();

        Map<String, Double> orgUnitCountMap = getAggregatedOrganisationUnitTargetMap( orgUnitTargetParams );

        return getPermutationOrgUnitGroupCountMap( orgUnitCountMap );
    }

    /**
     * Generates a mapping between the organisation unit dimension key and the
     * count of organisation units inside the subtree of the given organisation
     * units and members of the given organisation unit groups.
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between the the data set dimension key and the count of
     *         expected data sets to report.
     */
    private Map<String, Double> getAggregatedOrganisationUnitTargetMap( DataQueryParams params )
    {
        return getDoubleMap( getAggregatedValueMap( params, ORG_UNIT_TARGET, newArrayList() ) );
    }

    /**
     * Resolves the numerator and denominator expressions of indicators in the
     * data query.
     *
     * @param params the {@link DataQueryParams}.
     * @return the resolved list of indicators.
     */
    private List<Indicator> resolveIndicatorExpressions( DataQueryParams params )
    {
        List<Indicator> indicators = asTypedList( params.getIndicators() );

        for ( Indicator indicator : indicators )
        {
            for ( ExpressionResolver resolver : resolvers.getExpressionResolvers() )
            {
                indicator.setNumerator( resolver.resolve( indicator.getNumerator() ) );

                indicator.setDenominator( resolver.resolve( indicator.getDenominator() ) );
            }
        }

        return indicators;
    }

    /**
     * Returns a mapping between dimension items and values for the given data
     * query and list of indicators. The dimensional items part of the indicator
     * numerators and denominators are used as dimensional item for the
     * aggregated values being retrieved. In case of circular references between
     * Indicators, an exception is thrown.
     *
     * @param params the {@link DataQueryParams}.
     * @param items the list of {@link DimensionalItemObject}.
     * @return a dimensional items to aggregate values map.
     */
    private MultiValuedMap<String, DimensionItemObjectValue> getAggregatedDataValueMap(
        DataQueryParams params, List<DimensionalItemObject> items )
    {
        if ( items.isEmpty() )
        {
            return new ArrayListValuedHashMap<>();
        }

        DimensionalObject dimension = new BaseDimensionalObject(
            DATA_X_DIM_ID, DATA_X, null, DISPLAY_NAME_DATA_X, items );

        DataQueryParams dataSourceParams = newBuilder( params )
            .replaceDimension( dimension )
            .withMeasureCriteria( new HashMap<>() )
            .withIncludeNumDen( false )
            .withSkipHeaders( true )
            .withOutputFormat( ANALYTICS )
            .withSkipMeta( true )
            .build();

        Grid grid = dataAggregator.getAggregatedDataValueGrid( dataSourceParams );

        if ( isEmpty( grid.getRows() ) )
        {
            return new ArrayListValuedHashMap<>();
        }

        return getAggregatedValueMapFromGrid( params, items, grid );
    }

    /**
     * Gets a mapping between dimension items and values from a grid.
     *
     * @param params the {@link DataQueryParams}.
     * @param items the list of {@link DimensionalItemObject}.
     * @param grid the {@link Grid}.
     * @return a dimensional items to aggregate values map.
     */
    private MultiValuedMap<String, DimensionItemObjectValue> getAggregatedValueMapFromGrid(
        DataQueryParams params, List<DimensionalItemObject> items, Grid grid )
    {
        // Derive the Grid indexes for data, value and period based on the first
        // row of the Grid

        final int dataIndex = getGridIndexByDimensionItem( grid.getRow( 0 ), items, 0 );
        final int periodIndex = getGridIndexByDimensionItem( grid.getRow( 0 ), params.getPeriods(), 1 );
        final int valueIndex = grid.getWidth() - 1;

        final List<DimensionalItemObject> basePeriods = params.getPeriods();

        MultiValuedMap<String, DimensionItemObjectValue> valueMap = new ArrayListValuedHashMap<>();

        Map<String, List<Object>> yearToDateRows = new HashMap<>();

        // Process the grid rows. If yearToDate, build any yearToDate rows for
        // adding later. Otherwise, add the row to the result.

        List<DimensionalItemObject> nonYearToDateItems = items.stream()
            .filter( i -> !isYearToDate( i ) )
            .collect( toList() );

        Map<String, List<DimensionalItemObject>> yearToDateItemsById = items.stream()
            .filter( PeriodOffsetUtils::isYearToDate )
            .collect( groupingBy( DimensionalItemObject::getDimensionItemWithQueryModsId ) );

        for ( List<Object> row : grid.getRows() )
        {
            for ( DimensionalItemObject dimensionalItem : findDimensionalItems(
                (String) row.get( dataIndex ), nonYearToDateItems ) )
            {
                addRowToValueMap( periodIndex, valueIndex, row, dimensionalItem, basePeriods, valueMap );
            }

            List<DimensionalItemObject> yearToDateItems = yearToDateItemsById.get( row.get( dataIndex ) );

            if ( yearToDateItems != null )
            {
                buildYearToDateRows( periodIndex, valueIndex, row, yearToDateItems, basePeriods, yearToDateRows );
            }
        }

        if ( !yearToDateRows.isEmpty() )
        {
            addYearToDateRowsToValueMap( dataIndex, periodIndex, valueIndex, items, basePeriods, yearToDateRows,
                valueMap );
        }

        return valueMap;
    }

    private void addYearToDateRowsToValueMap( int dataIndex, int periodIndex, int valueIndex,
        List<DimensionalItemObject> items, List<DimensionalItemObject> basePeriods,
        Map<String, List<Object>> yearToDateRows, MultiValuedMap<String, DimensionItemObjectValue> valueMap )
    {
        List<DimensionalItemObject> yearToDateItems = items.stream()
            .filter( PeriodOffsetUtils::isYearToDate )
            .collect( toList() );

        for ( List<Object> row : yearToDateRows.values() )
        {
            for ( DimensionalItemObject dimensionalItem : findDimensionalItems(
                (String) row.get( dataIndex ), yearToDateItems ) )
            {
                addRowToValueMap( periodIndex, valueIndex, row, dimensionalItem, basePeriods, valueMap );
            }
        }
    }

    private void addRowToValueMap( int periodIndex, int valueIndex, List<Object> row,
        DimensionalItemObject dimensionalItem, List<DimensionalItemObject> basePeriods,
        MultiValuedMap<String, DimensionItemObjectValue> valueMap )
    {
        Optional<List<Object>> adjustedRow = getAdjustedRow( periodIndex, valueIndex, row, dimensionalItem,
            basePeriods );

        if ( adjustedRow.isPresent() )
        {
            List<Object> aRow = adjustedRow.get();
            String key = join( remove( aRow.toArray( new Object[0] ), valueIndex ), DIMENSION_SEP );
            Double value = ((Number) aRow.get( valueIndex )).doubleValue();

            valueMap.put( key, new DimensionItemObjectValue( dimensionalItem, value ) );
        }
    }

    /**
     * Add the given Indicator values to the given grid.
     *
     * @param params the current DataQueryParams.
     * @param grid the current Grid.
     * @param dataSourceParams the DataQueryParams built for Indicators.
     * @param indicator the Indicator which the values will be extracted from,
     *        and added to be added to the Grid.
     * @param dimensionItems the dimensional items permutation. See
     *        {@link DataQueryParams#getDimensionItemPermutations()}.
     * @param value the IndicatorValue which the values will be extracted from.
     */
    private void addIndicatorValuesToGrid( DataQueryParams params, Grid grid, DataQueryParams dataSourceParams,
        Indicator indicator, List<DimensionItem> dimensionItems, IndicatorValue value )
    {
        if ( value != null && satisfiesMeasureCriteria( params, value, indicator ) )
        {
            List<DimensionItem> row = new ArrayList<>( dimensionItems );

            row.add( DX_INDEX, new DimensionItem( DATA_X_DIM_ID, indicator ) );

            grid.addRow()
                .addValues( getItemIdentifiers( row ) )
                .addValue( getRoundedValue( dataSourceParams, indicator.getDecimals(),
                    value.getValue() ) );

            if ( params.isIncludeNumDen() )
            {
                grid.addValue( getRoundedValue( dataSourceParams, indicator.getDecimals(),
                    value.getNumeratorValue() ) )
                    .addValue( getRoundedValue( dataSourceParams, indicator.getDecimals(),
                        value.getDenominatorValue() ) )
                    .addValue( getRoundedValue( dataSourceParams, indicator.getDecimals(),
                        value.getFactor() ) )
                    .addValue( value.getMultiplier() )
                    .addValue( value.getDivisor() );
            }
        }
    }

    private Optional<List<Object>> getAdjustedRow( int periodIndex, int valueIndex, List<Object> row,
        DimensionalItemObject dimensionalItemObject, List<DimensionalItemObject> basePeriods )
    {
        if ( !hasPeriod( row, periodIndex ) )
        {
            return Optional.of( row );
        }

        if ( row.get( valueIndex ) == null )
        {
            return Optional.empty();
        }

        int periodOffset = (dimensionalItemObject.getQueryMods() == null)
            ? 0
            : dimensionalItemObject.getQueryMods().getPeriodOffset();

        final List<Object> adjustedRow = (periodOffset != 0)
            ? getPeriodOffsetRow( row, periodIndex, periodOffset )
            : row;

        // Check if the current row's Period belongs to the list of
        // periods from the original Analytics request. The row may
        // not have a Period if Period is used as filter.

        if ( !isPeriodInPeriods( (String) adjustedRow.get( periodIndex ), basePeriods ) )
        {
            return Optional.empty();
        }

        return Optional.of( adjustedRow );
    }

    /**
     * Generates a mapping between a dimension key and the aggregated value. The
     * dimension key is a concatenation of the identifiers of the dimension
     * items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @param tableType the {@link AnalyticsTableType}.
     * @param queryGroupers the list of additional query groupers to use for
     *        query planning, use empty list for none.
     * @return a mapping between a dimension key and aggregated values.
     */
    private Map<String, Object> getAggregatedValueMap( DataQueryParams params, AnalyticsTableType tableType,
        List<Function<DataQueryParams, List<DataQueryParams>>> queryGroupers )
    {
        queryValidator.validateMaintenanceMode();

        int optimalQueries = getWithin( getProcessNo(), 1, MAX_QUERIES );

        int maxLimit = params.isIgnoreLimit() ? 0
            : systemSettingManager.getIntSetting( ANALYTICS_MAX_LIMIT );

        Timer timer = new Timer().start().disablePrint();

        QueryPlannerParams plannerParams = QueryPlannerParams.newBuilder()
            .withOptimalQueries( optimalQueries )
            .withTableType( tableType )
            .withQueryGroupers( queryGroupers ).build();

        DataQueryGroups queryGroups = queryPlanner.planQuery( params, plannerParams );

        timer.getSplitTime(
            "Planned analytics query, got: " + queryGroups.getLargestGroupSize() + " for optimal: " + optimalQueries );

        Map<String, Object> map = new HashMap<>();

        for ( List<DataQueryParams> queries : queryGroups.getSequentialQueries() )
        {
            executeQueries( tableType, maxLimit, map, queries );
        }

        timer.getTime( "Got analytics values" );

        return map;
    }

    /**
     * Executes the given list of queries in parallel.
     *
     * @param tableType the {@link AnalyticsTableType}.
     * @param maxLimit the max limit of records to retrieve.
     * @param map the map of metadata identifiers to data values.
     * @param queries the list of {@link DataQueryParams} to execute.
     */
    private void executeQueries( AnalyticsTableType tableType, int maxLimit, Map<String, Object> map,
        List<DataQueryParams> queries )
    {
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        for ( DataQueryParams query : queries )
        {
            futures.add( analyticsManager.getAggregatedDataValues( query, tableType, maxLimit ) );
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
                log.error( getStackTrace( ex ) );
                log.error( getStackTrace( ex.getCause() ) );

                if ( ex.getCause() instanceof RuntimeException )
                {
                    // Throw the real exception
                    throw (RuntimeException) ex.getCause();
                }
                else
                {
                    throw new RuntimeException( "Error during execution of aggregation query task", ex );
                }
            }
        }
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
        Integer cores = systemSettingManager.getIntegerSetting( DATABASE_SERVER_CPUS );

        return (cores == null || cores == 0) ? getCpuCores() : cores;
    }

    /**
     * Generates aggregated values for the given query. Creates a mapping
     * between a dimension key and the aggregated value. The dimension key is a
     * concatenation of the identifiers of the dimension items separated by "-".
     *
     * @param params the {@link DataQueryParams}.
     * @return a mapping between a dimension key and the aggregated value.
     */
    private Map<String, Double> getAggregatedDataValueMap( DataQueryParams params )
    {
        return getDoubleMap( getAggregatedValueMap( params, DATA_VALUE, newArrayList() ) );
    }

    void require( DataAggregator dataAggregator )
    {
        this.dataAggregator = dataAggregator;
    }
}
