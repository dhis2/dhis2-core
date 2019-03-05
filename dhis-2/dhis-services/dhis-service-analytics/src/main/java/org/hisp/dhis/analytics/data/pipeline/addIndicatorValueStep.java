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
import org.hisp.dhis.analytics.*;
import org.hisp.dhis.analytics.util.AnalyticsUtils;
import org.hisp.dhis.common.*;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.expressionparser.ExpressionParserService;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.setting.SystemSettingManager;

import java.util.*;
import java.util.function.Function;

import static org.hisp.dhis.analytics.DataQueryParams.DISPLAY_NAME_DATA_X;
import static org.hisp.dhis.analytics.DataQueryParams.DX_INDEX;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObjectUtils.asTypedList;

/**
 * Adds indicator values to the given grid based on the given data query
 * parameters.
 *
 */
public class addIndicatorValueStep
    extends
    BaseStep
{

    private Function<DataQueryParams, Grid> function;

    public addIndicatorValueStep(Function<DataQueryParams, Grid> function) {
        this.function = function;
    }

    @Override
    public void execute( DataQueryParams params, Grid grid )
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

            handleEmptyDimensionItemPermutations( dimensionItemPermutations );

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

                    IndicatorValue value = expressionParserService.getIndicatorValueObject( indicator, period, valueMap, constantMap, orgUnitCountMap );

                    if ( value != null && satisfiesMeasureCriteria( params, value, indicator ) )
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
                                    .addValue( AnalyticsUtils.getRoundedValue( dataSourceParams, indicator.getDecimals(), value.getFactor() ) )
                                    .addValue( value.getMultiplier() )
                                    .addValue( value.getDivisor() );
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates a mapping of permutations keys (organisation unit id or null)
     * and mappings of organisation unit group and counts.
     *
     * @param params the {@link DataQueryParams}.
     * @param indicators the indicators for which formulas to scan for organisation
     *         unit groups.
     * @return a map of maps.
     */
    private Map<String, Map<String, Integer>> getOrgUnitTargetMap( DataQueryParams params, Collection<Indicator> indicators )
    {
        Set<OrganisationUnitGroup> orgUnitGroups = expressionParserService.getIndicatorOrgUnitGroups( indicators );

        if ( orgUnitGroups.isEmpty() )
        {
            return null;
        }

        DataQueryParams orgUnitTargetParams = DataQueryParams.newBuilder( params )
                .pruneToDimensionType( DimensionType.ORGANISATION_UNIT )
                .addDimension( new BaseDimensionalObject( DimensionalObject.ORGUNIT_GROUP_DIM_ID, DimensionType.ORGANISATION_UNIT_GROUP, new ArrayList<DimensionalItemObject>( orgUnitGroups ) ) )
                .withSkipPartitioning( true ).build();

        Map<String, Double> orgUnitCountMap = getAggregatedOrganisationUnitTargetMap( orgUnitTargetParams );

        return DataQueryParams.getPermutationOrgUnitGroupCountMap( orgUnitCountMap );
    }

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
     * Handles the case where there are no dimension item permutations by adding an
     * empty dimension item list to the permutations list. This state occurs where
     * there are only data or category option combo dimensions specified.
     *
     * @param dimensionItemPermutations list of dimension item permutations.
     */
    private void handleEmptyDimensionItemPermutations( List<List<DimensionItem>> dimensionItemPermutations )
    {
        if ( dimensionItemPermutations.isEmpty() )
        {
            dimensionItemPermutations.add( new ArrayList<>() );
        }
    }

    /**
     * Checks whether the measure criteria in query parameters is satisfied for the given indicator value.
     *
     * @param params the query parameters.
     * @param value the indicator value.
     * @param indicator the indicator.
     * @return true if all the measure criteria are satisfied for this indicator value, false otherwise.
     */
    private boolean satisfiesMeasureCriteria( DataQueryParams params, IndicatorValue value, Indicator indicator )
    {
        if ( !params.hasMeasureCriteria() )
        {
            return true;
        }

        Double indicatorRoundedValue = AnalyticsUtils.getRoundedValue( params, indicator.getDecimals(), value.getValue() );

        return !params.getMeasureCriteria().entrySet().stream()
                .anyMatch( measureValue -> !measureValue.getKey()
                        .measureIsValid( indicatorRoundedValue, measureValue.getValue() ) );
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
        return AnalyticsUtils.getDoubleMap( getAggregatedValueMap( params, AnalyticsTableType.ORG_UNIT_TARGET, Lists.newArrayList() ) );
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
    private Map<String, Double> getAggregatedDataValueMap(DataQueryParams params, List<Indicator> indicators)
    {
        List<DimensionalItemObject> items = Lists.newArrayList( expressionParserService.getIndicatorDimensionalItemObjects( indicators ) );

        items = DimensionalObjectUtils.replaceOperandTotalsWithDataElements( items );

        DimensionalObject dimension = new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, DISPLAY_NAME_DATA_X, items );

        DataQueryParams dataSourceParams = DataQueryParams.newBuilder( params )
                .replaceDimension( dimension )
                .withMeasureCriteria( new HashMap<>() )
                .withIncludeNumDen( false )
                .withSkipHeaders( true )
                .withSkipMeta( true ).build();

        Grid grid = function.apply( dataSourceParams );

        return grid.getAsMap( grid.getWidth() - 1, DimensionalObject.DIMENSION_SEP );
    }
}
