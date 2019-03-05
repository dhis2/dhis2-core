/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.data.pipeline;

import com.google.common.collect.Lists;
import org.hisp.dhis.analytics.AnalyticsAggregationType;
import org.hisp.dhis.analytics.AnalyticsTableType;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.MathUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hisp.dhis.analytics.DataQueryParams.COMPLETENESS_DIMENSION_TYPES;
import static org.hisp.dhis.analytics.DataQueryParams.DX_INDEX;
import static org.hisp.dhis.common.DimensionalObject.DIMENSION_SEP;
import static org.hisp.dhis.common.ReportingRateMetric.*;
import static org.hisp.dhis.period.PeriodType.getPeriodTypeFromIsoString;

/**
 * Adds reporting rates to the given grid based on the given data query
 * parameters.
 *
 */
public class addReportingRatesStep extends BaseStep {
    private static final int PERCENT = 100;

    @Override
    public void execute( DataQueryParams params, Grid grid )
    {

        if ( !params.getReportingRates().isEmpty() && !params.isSkipData() )
        {
            for ( ReportingRateMetric metric : ReportingRateMetric.values() )
            {
                DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                        .retainDataDimensionReportingRates( metric )
                        .ignoreDataApproval() // No approval for reporting rates
                        .withAggregationType( AnalyticsAggregationType.COUNT )
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
                    .withAggregationType( AnalyticsAggregationType.SUM ).build();

            Map<String, Double> targetMap = getAggregatedCompletenessTargetMap( targetParams );

            Map<String, Double> dataMap = metric != EXPECTED_REPORTS ? getAggregatedCompletenessValueMap( params ) : new HashMap<>();

            Integer periodIndex = params.getPeriodDimensionIndex();
            Integer dataSetIndex = DataQueryParams.DX_INDEX;
            Map<String, PeriodType> dsPtMap = params.getDataSetPeriodTypeMap();
            PeriodType filterPeriodType = params.getFilterPeriodType();

            DimensionalObject filterPeriod = params.getFilter("pe");
            int timeUnits = 1;
            if (filterPeriod != null) {
                timeUnits = filterPeriod.getItems().size();
            }

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
                    target = target * queryPt.getPeriodSpan( dataSetPt ) * timeUnits;

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
                        value = Math.min( ( ( actual * PERCENT ) / target ), 100d );
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
                                .addValue( PERCENT )
                                .addNullValues( 2 );
                    }
                }
            }
        }
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
        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.COMPLETENESS, Lists.newArrayList() ) );
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
        queryGroupers.add( q -> queryPlanner.groupByStartEndDateRestriction( q ) );

        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.COMPLETENESS_TARGET, queryGroupers ) );
    }
}
