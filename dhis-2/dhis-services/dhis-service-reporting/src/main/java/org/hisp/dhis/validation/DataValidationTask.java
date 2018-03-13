package org.hisp.dhis.validation;

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
import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.*;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.system.util.MathUtils.*;

/**
 * Runs a validation task on a thread within a multi-threaded validation run.
 * <p>
 * Each task looks for validation results in a different organisation unit.
 *
 * @author Jim Grace
 */
public class DataValidationTask
    implements ValidationTask
{
    private static final Log log = LogFactory.getLog( DataValidationTask.class );

    public static final String NAME = "validationTask";

    public final static String NON_AOC = ""; // String that is not an Attribute Option Combo

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private ValidationResultService validationResultService;

    // (wired through constructor)
    private AnalyticsService analyticsService;

    private List<OrganisationUnit> orgUnits;
    private ValidationRunContext context;

    private Set<ValidationResult> validationResults;

    private PeriodTypeExtended periodTypeX; // Current period type extended.
    private Period period;                  // Current period.
    private OrganisationUnit orgUnit;       // Current organisation unit.
    private int orgUnitId;                  // Current organisation unit id.
    private ValidationRuleExtended ruleX;   // Current rule extended.

    // Data for current period and all rules being evaluated:
    private MapMapMap<Integer, String, DimensionalItemObject, Double> dataMap;
    private MapMapMap<Integer, String, DimensionalItemObject, Double> eventMap;
    private MapMapMap<Integer, String, DimensionalItemObject, Double> slidingWindowEventMap;

    public void init( List<OrganisationUnit> orgUnits, ValidationRunContext context, AnalyticsService analyticsService )
    {
        this.orgUnits = orgUnits;
        this.context = context;
        this.analyticsService = analyticsService;
    }

    /**
     * Evaluates validation rules for a single organisation unit. This is the
     * central method in validation rule evaluation.
     */
    @Override
    @Transactional
    public void run()
    {
        try
        {
            runInternal();
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );

            throw ex;
        }
    }

    /**
     * Get the data needed for this task, then evaluate each combination
     * of organisation unit / period / validation rule.
     */
    private void runInternal()
    {
        if ( context.isAnalysisComplete() )
        {
            return;
        }

        validationResults = new HashSet<>();

        for ( PeriodTypeExtended ptx : context.getPeriodTypeXs() )
        {
            periodTypeX = ptx;

            for ( Period p : periodTypeX.getPeriods() )
            {
                period = p;

                getData();

                for ( OrganisationUnit ou : orgUnits )
                {
                    orgUnit = ou;
                    orgUnitId = ou.getId();

                    for ( ValidationRuleExtended r : periodTypeX.getRuleXs() )
                    {
                        ruleX = r;

                        validateRule();
                    }
                }
            }
        }

        addValidationResultsToContext();
    }

    /**
     * Validates one rule / period by seeing which attribute option combos exist
     * for that data, and then iterating through those attribute option combos.
     */
    private void validateRule()
    {
        // Skip validation if org unit level does not match
        if ( !ruleX.getOrganisationUnitLevels().isEmpty() &&
            !ruleX.getOrganisationUnitLevels().contains( orgUnit.getLevel() ) )
        {
            return;
        }

        Map<String, Double> leftSideValues = getValuesForExpression( ruleX.getRule().getLeftSide(), ruleX.getLeftSlidingWindow() );
        Map<String, Double> rightSideValues = getValuesForExpression( ruleX.getRule().getRightSide(), ruleX.getRightSlidingWindow() );

        Set<String> attributeOptionCombos = Sets.union( leftSideValues.keySet(), rightSideValues.keySet() );

        for ( String optionCombo : attributeOptionCombos )
        {
            if ( NON_AOC.compareTo( optionCombo ) == 0 )
            {
                continue;
            }

            validateOptionCombo( optionCombo,
                leftSideValues.get( optionCombo ),
                rightSideValues.get( optionCombo ) );
        }
    }

    /**
     * Validates one rule / period / attribute option combo.
     *
     * @param optionCombo the attribute option combo.
     * @param leftSide left side value.
     * @param rightSide right side value.
     */
    private void validateOptionCombo( String optionCombo, Double leftSide, Double rightSide )
    {
        // Skipping any results we already know
        if ( context.skipValidationOfTuple( orgUnit, ruleX.getRule(), period, optionCombo,
            periodService.getDayInPeriod( period, new Date() ) ) )
        {
            return;
        }

        log.trace( "Validation attributeOptionCombo " + optionCombo );

        boolean violation = isViolation( leftSide, rightSide );

        if ( violation )
        {
            validationResults.add( new ValidationResult(
                ruleX.getRule(), period, orgUnit,
                categoryService.getDataElementCategoryOptionCombo( optionCombo ),
                roundSignificant( zeroIfNull( leftSide ) ),
                roundSignificant( zeroIfNull( rightSide ) ),
                periodService.getDayInPeriod( period, new Date() ) ) );
        }
    }

    /**
     * Determines if left and right side values violate a rule.
     *
     * @param leftSide the left side value.
     * @param rightSide the right side value.
     * @return true if violation, otherwise false.
     */
    private boolean isViolation( Double leftSide, Double rightSide )
    {
        if ( Operator.compulsory_pair.equals( ruleX.getRule().getOperator() ) )
        {
            return ( leftSide == null ) != ( rightSide == null );
        }

        if ( Operator.exclusive_pair.equals( ruleX.getRule().getOperator() ) )
        {
            return ( leftSide != null ) && ( rightSide != null );
        }

        if ( leftSide == null )
        {
            if ( ruleX.getRule().getLeftSide().getMissingValueStrategy() == NEVER_SKIP )
            {
                leftSide = 0d;
            }
            else
            {
                return false;
            }
        }

        if ( rightSide == null )
        {
            if ( ruleX.getRule().getRightSide().getMissingValueStrategy() == NEVER_SKIP )
            {
                rightSide = 0d;
            }
            else
            {
                return false;
            }
        }

        return !expressionIsTrue( leftSide, ruleX.getRule().getOperator(), rightSide );
    }

    /**
     * Gets the data we will need for this task.
     */
    private void getData()
    {
        getDataMap();

        slidingWindowEventMap = getEventMapForSlidingWindow( true, periodTypeX.getEventItems() );
        slidingWindowEventMap.putMap ( getEventMapForSlidingWindow( false, periodTypeX.getEventItemsWithoutAttributeOptions() ) );

        slidingWindowEventMap.putMap( dataMap );

        eventMap = getEventMap( true, periodTypeX.getEventItems() );
        eventMap.putMap ( getEventMap( false, periodTypeX.getEventItemsWithoutAttributeOptions() ) );

        dataMap.putMap( eventMap );
    }

    /**
     * For an expression (left side or right side), finds the values
     * (grouped by attribute option combo).
     *
     * @param expression left or right side expression.
     * @param slidingWindow whether to use sliding window.
     * @return the values grouped by attribute option combo.
     */
    private Map<String, Double> getValuesForExpression( Expression expression, boolean slidingWindow )
    {
        if ( expression == null )
        {
            return new HashMap<>();
        }
        else if ( slidingWindow )
        {
            return getExpressionValueMap( expression, slidingWindowEventMap );
        }
        else
        {
            return getExpressionValueMap( expression, dataMap );
        }
    }

    /**
     * Adds any validation results we found to the validation context.
     */
    private void addValidationResultsToContext()
    {
        if ( validationResults.size() > 0 )
        {
            context.getValidationResults().addAll( validationResults );

            if ( context.isPersistResults() )
            {
                validationResultService.saveValidationResults( validationResults );
            }
        }
    }

    /**
     * Evaluates an expression, returning a map of values by attribute option
     * combo.
     *
     * @param expression expression to evaluate.
     * @param valueMap   Map of value maps, by attribute option combo.
     * @return map of values.
     */
    private Map<String, Double> getExpressionValueMap( Expression expression,
        MapMapMap<Integer, String, DimensionalItemObject, Double> valueMap )
    {
        Map<String, Double> expressionValueMap = new HashMap<>();

        Map<DimensionalItemObject, Double> nonAocValues = valueMap.get( orgUnitId ) == null
            ? null : valueMap.get( orgUnitId ).get( NON_AOC );

        MapMap<String, DimensionalItemObject, Double> aocValues = valueMap.get( orgUnitId );

        if ( aocValues == null )
        {
            if ( nonAocValues == null )
            {
                return expressionValueMap;
            }
            else
            {
                aocValues = new MapMap<>();
                aocValues.putEntries( context.getDefaultAttributeCombo().getUid(), nonAocValues );
            }
        }

        for ( Map.Entry<String, Map<DimensionalItemObject, Double>> entry : aocValues.entrySet() )
        {
            Map<DimensionalItemObject, Double> values = entry.getValue();

            if ( nonAocValues != null )
            {
                values.putAll( nonAocValues );
            }

            Double value = expressionService.getExpressionValue( expression, values,
                context.getConstantMap(), null, period.getDaysInPeriod() );

            if ( MathUtils.isValidDouble( value ) )
            {
                expressionValueMap.put( entry.getKey(), value );
            }
        }

        return expressionValueMap;
    }

    /**
     * Gets data values for this task.
     */
    private void getDataMap()
    {
        dataMap = dataValueService.getDataValueMapByAttributeCombo(
            periodTypeX.getDataItems(), period.getStartDate(), orgUnits,
            periodTypeX.getAllowedPeriodTypes(),
            context.getAttributeCombo(), context.getCogDimensionConstraints(),
            context.getCoDimensionConstraints() );
    }

    /**
     * Gets aggregated event data for the given parameters.
     *
     * @param hasAttributeOptions whether the event data has attribute options.
     */
    private MapMapMap<Integer, String, DimensionalItemObject, Double> getEventMap(
        boolean hasAttributeOptions, Set<DimensionalItemObject> eventItems )
    {
        if ( eventItems.isEmpty() )
        {
            return new MapMapMap<>();
        }

        DataQueryParams.Builder paramsBuilder = DataQueryParams.newBuilder()
            .withDataDimensionItems( Lists.newArrayList( eventItems ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .withFilterPeriods( Lists.newArrayList( period ) )
            .withOrganisationUnits( orgUnits );

        if ( hasAttributeOptions )
        {
            paramsBuilder.withAttributeOptionCombos( Lists.newArrayList() );
        }

        return getEventData( paramsBuilder.build(), hasAttributeOptions );
    }

    /**
     * Gets sliding window aggregated event data for the given parameters.
     *
     * @param hasAttributeOptions whether the event data has attribute options.
     */
    private MapMapMap<Integer, String, DimensionalItemObject, Double> getEventMapForSlidingWindow(
        boolean hasAttributeOptions, Set<DimensionalItemObject> eventItems )
    {
        if ( eventItems.isEmpty() )
        {
            return new MapMapMap<>();
        }

        // We want to position the sliding window over the most recent data.
        // To achieve this, we need to satisfy the following criteria:
        //
        // 1. Window end should not be later than the current date
        // 2. Window end should not be later than the period.endDate

        // Criteria 1
        Calendar endDate = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();

        // Criteria 2
        if ( endDate.getTime().after( period.getEndDate() ) )
        {
            endDate.setTime( period.getEndDate() );
        }

        // The window size is based on the frequencyOrder of the period's periodType:
        startDate.setTime( endDate.getTime() );
        startDate.add( Calendar.DATE, (-1 * period.frequencyOrder()) );

        DataQueryParams.Builder paramsBuilder = DataQueryParams.newBuilder()
            .withDataDimensionItems( Lists.newArrayList( eventItems ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .withStartDate( startDate.getTime() )
            .withEndDate( endDate.getTime() )
            .withOrganisationUnits( orgUnits );

        if ( hasAttributeOptions )
        {
            paramsBuilder.withAttributeOptionCombos( Lists.newArrayList() );
        }

        return getEventData( paramsBuilder.build(), hasAttributeOptions );
    }

    /**
     * Gets event data.
     *
     * @param params event data query parameters.
     * @param hasAttributeOptions whether the event data has attribute options.
     * @return event data.
     */
    private MapMapMap<Integer, String, DimensionalItemObject, Double> getEventData(
        DataQueryParams params, boolean hasAttributeOptions )
    {
        MapMapMap<Integer, String, DimensionalItemObject, Double> map = new MapMapMap<>();

        Grid grid = analyticsService.getAggregatedDataValues( params );

        int dxInx = grid.getIndexOfHeader( DimensionalObject.DATA_X_DIM_ID );
        int ouInx = grid.getIndexOfHeader( DimensionalObject.ORGUNIT_DIM_ID );
        int aoInx = hasAttributeOptions ? grid.getIndexOfHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID ) : 0;
        int vlInx = grid.getWidth() - 1;

        Map<String, OrganisationUnit> ouLookup = orgUnits.stream().collect( Collectors.toMap( o -> o.getUid(), o -> o ) );
        Map<String, DimensionalItemObject> dxLookup = periodTypeX.getEventItems().stream().collect( Collectors.toMap( d -> d.getDimensionItem(), d -> d ) );

        for ( List<Object> row : grid.getRows() )
        {
            String dx = (String) row.get( dxInx );
            String ao = hasAttributeOptions ? (String) row.get( aoInx ) : NON_AOC;
            String ou = (String) row.get( ouInx );
            Double vl = (Double) row.get( vlInx );

            OrganisationUnit orgUnit = ouLookup.get( ou );
            DimensionalItemObject eventItem = dxLookup.get( dx );

            map.putEntry( orgUnit.getId(), ao, eventItem, vl );
        }

        return map;
    }
}
