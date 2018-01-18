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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private ValidationResultService validationResultService;

    private OrganisationUnit orgUnit;
    private ValidationRunContext context;

    private Set<ValidationResult> validationResults;

    private Period period; // Current period (when one is selected).
    private ValidationRule rule; // Current rule (when one is selected).

    // Data for current period and all rules being evaluated:
    private MapMap<String, DataElementOperand, Date> lastUpdatedMap;
    private MapMap<String, DimensionalItemObject, Double> dataValueMap;
    private MapMap<String, DimensionalItemObject, Double> slidingWindowEventMap;
    private MapMap<String, DimensionalItemObject, Double> eventMap;

    public void init( OrganisationUnit orgUnit, ValidationRunContext context )
    {
        this.orgUnit = orgUnit;
        this.context = context;
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
     * Breaks up the validation work for a single period type (and associated
     * longer period types which may contain baseline demographic data, etc.)
     */
    private void runInternal()
    {
        validationResults = new HashSet<>();

        if ( !context.isAnalysisComplete() )
        {
            for ( PeriodTypeExtended periodTypeX : context.getPeriodTypeExtendedMap().values() )
            {
                validatePeriodType( periodTypeX );
            }
        }

        addValidationResultsToContext();
    }

    /**
     * Evaluates rules for one (extended) period type, by getting data
     * for each period of that type (including any longer periods), and then
     * evaluating each rule within that period.
     *
     * @param periodTypeX the extended period type.
     */
    private void validatePeriodType( PeriodTypeExtended periodTypeX )
    {
        log.trace( "Validation PeriodType " + periodTypeX.getPeriodType().getName() );

        Set<ValidationRuleExtended> ruleXs = getRulesBySourceAndPeriodType( orgUnit, periodTypeX );

        if ( !ruleXs.isEmpty() )
        {
            SetMap<String, DataElementOperand> dataElementOperandsToGet = getDataElementOperands( ruleXs );

            for ( Period p : periodTypeX.getPeriods() )
            {
                period = p;

                getData( dataElementOperandsToGet, periodTypeX );

                log.trace( "OrgUnit " + orgUnit.getName() + " [" + period.getStartDate() + " - "
                    + period.getEndDate() + "]" + " currentValueMap[" + dataValueMap.size() + "]" );

                for ( ValidationRuleExtended ruleX : ruleXs )
                {
                    rule = ruleX.getRule();

                    validateRule();
                }
            }
        }
    }

    /**
     * Validates one rule / period by seeing which attribute option combos exist
     * for that data, and then iterating through those attribute option combos.
     */
    private void validateRule()
    {
        // Skip validation if org unit level does not match
        if ( !rule.getOrganisationUnitLevels().isEmpty() &&
            !rule.getOrganisationUnitLevels().contains( orgUnit.getLevel() ) )
        {
            return;
        }
        log.trace( "Validation rule " + rule.getUid() + " " + rule.getName() );

        Map<String, Double> leftSideValues = getValuesForExpression( rule.getLeftSide() );
        Map<String, Double> rightSideValues = getValuesForExpression( rule.getRightSide() );

        Set<String> attributeOptionCombos = Sets.newHashSet( leftSideValues.keySet() );
        attributeOptionCombos.addAll( rightSideValues.keySet() );

        for ( String optionCombo : attributeOptionCombos )
        {
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
        if ( context.skipValidationOfTuple( orgUnit, rule, period, optionCombo,
            periodService.getDayInPeriod( period, new Date() ) ) )
        {
            return;
        }

        log.trace( "Validation attributeOptionCombo " + optionCombo );

        boolean violation = isViolation( leftSide, rightSide );

        if ( violation )
        {
            validationResults.add( new ValidationResult(
                rule, period, orgUnit,
                categoryService.getDataElementCategoryOptionCombo( optionCombo ),
                roundSignificant( zeroIfNull( leftSide ) ),
                roundSignificant( zeroIfNull( rightSide ) ),
                periodService.getDayInPeriod( period, new Date() ) ) );
        }

        log.debug( "Evaluated " + rule.getName() + ", combo id " + optionCombo
            + ": " + (violation ? "violation" : "OK") + " "
            + (leftSide == null ? "(null)" : leftSide.toString()) + " "
            + rule.getOperator() + " "
            + (rightSide == null ? "(null)" : rightSide.toString()) + " ("
            + context.getValidationResults().size() + " results)" );
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
        if ( Operator.compulsory_pair.equals( rule.getOperator() ) )
        {
            return ( leftSide == null ) != ( rightSide == null );
        }

        if ( Operator.exclusive_pair.equals( rule.getOperator() ) )
        {
            return ( leftSide != null ) && ( rightSide != null );
        }

        if ( leftSide == null )
        {
            if ( rule.getLeftSide().getMissingValueStrategy() == NEVER_SKIP )
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
            if ( rule.getRightSide().getMissingValueStrategy() == NEVER_SKIP )
            {
                rightSide = 0d;
            }
            else
            {
                return false;
            }
        }

        return !expressionIsTrue( leftSide, rule.getOperator(), rightSide );
    }

    /**
     * Gets the data we will need for all the rules in a period.
     *
     * @param dataElementOperandsToGet Data element operands to look for.
     * @param periodTypeX The period type extended.
     */
    private void getData( SetMap<String, DataElementOperand> dataElementOperandsToGet,
        PeriodTypeExtended periodTypeX )
    {
        lastUpdatedMap = new MapMap<>();

        getDataValueMap( dataElementOperandsToGet, periodTypeX.getAllowedPeriodTypes() );

        getEventMapForSlidingWindow();

        slidingWindowEventMap.putMap( dataValueMap );

        getEventMap();

        dataValueMap.putMap( eventMap );
    }

    /**
     * For an expression (left side or right side), finds the values
     * (grouped by attribute option combo).
     *
     * @param expression left or right side expression.
     * @return the values grouped by attribute option combo.
     */
    private Map<String, Double> getValuesForExpression( Expression expression )
    {
        if ( expression == null )
        {
            return new HashMap<>();
        }
        else if ( expression.getSlidingWindow() )
        {
            return getExpressionValueMap( expression, slidingWindowEventMap );
        }
        else
        {
            return getExpressionValueMap( expression, dataValueMap );
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
     * Gets the rules that should be evaluated for a given organisation unit and
     * period type.
     *
     * @param orgUnit     The organisation unit.
     * @param periodTypeX The period type extended information.
     * @return set of rules for this org unit and period type.
     */
    private Set<ValidationRuleExtended> getRulesBySourceAndPeriodType(
        OrganisationUnit orgUnit, PeriodTypeExtended periodTypeX )
    {
        Set<DataElement> orgUnitDataElements = periodTypeX.getOrgUnitDataElements().get( orgUnit );

        Set<ValidationRuleExtended> periodTypeRuleXs = new HashSet<>();

        for ( ValidationRuleExtended ruleX : periodTypeX.getRuleXs() )
        {
            // Include only rules where the organisation collects all the data elements
            // in the rule, or rules which have no data elements.

            Set<DataElement> elements = ruleX.getDataElements();

            if ( elements.isEmpty() || orgUnitDataElements.containsAll( elements ) )
            {
                periodTypeRuleXs.add( ruleX );
            }
        }

        return periodTypeRuleXs;
    }

    /**
     * Gets the DataElementOperands from a set of Rules (extended),
     * mapped by DataElement UID.
     *
     * @param ruleXs the set of ValidationRuleExtendeds.
     * @return the combined list of DataElementOperands.
     */
    private SetMap<String, DataElementOperand> getDataElementOperands( Set<ValidationRuleExtended> ruleXs )
    {
        SetMap<String, DataElementOperand> dataElementOperands = new SetMap<>();

        for ( ValidationRuleExtended ruleX : ruleXs )
        {
            for ( DataElementOperand operand : ruleX.getDataElementOperands() )
            {
                dataElementOperands.putValue( operand.getDataElement().getUid(), operand );
            }
        }

        return dataElementOperands;
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
        MapMap<String, DimensionalItemObject, Double> valueMap )
    {
        Map<String, Double> expressionValueMap = new HashMap<>();

        for ( Map.Entry<String, Map<DimensionalItemObject, Double>> entry : valueMap.entrySet() )
        {
            Double value = expressionService.getExpressionValue( expression, entry.getValue(),
                context.getConstantMap(), null, period.getDaysInPeriod() );

            if ( MathUtils.isValidDouble( value ) )
            {
                expressionValueMap.put( entry.getKey(), value );
            }
        }

        return expressionValueMap;
    }

    /**
     * Gets data values for a given organisation unit and period, recursing if
     * necessary to sum the values from child organisation units.
     *
     * @param dataElementOperandsToGet data element operands for orgUnit and period
     * @param allowedPeriodTypes       all the periods in which we might find data values
     */
    private void getDataValueMap( SetMap<String, DataElementOperand> dataElementOperandsToGet,
        Set<PeriodType> allowedPeriodTypes )
    {
        log.trace( "getDataValueMap: orgUnit:" + orgUnit.getName()
            + " dataElementOperandsToGet[" + dataElementOperandsToGet.size()
            + "] allowedPeriodTypes[" + allowedPeriodTypes.size() + "]" );

        dataValueMap = dataValueService.getDataValueMapByAttributeCombo(
            dataElementOperandsToGet, period.getStartDate(), orgUnit, allowedPeriodTypes, context.getAttributeCombo(),
            context.getCogDimensionConstraints(), context.getCoDimensionConstraints(), lastUpdatedMap );
    }

    /**
     * Gets aggregated event data for the given parameters.
     */
    private void getEventMap()
    {
        if ( context.getEventItems().isEmpty() || period == null || orgUnit == null )
        {
            eventMap = new MapMap<>();

            return;
        }

        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataDimensionItems( Lists.newArrayList( context.getEventItems() ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .withFilterPeriods( Lists.newArrayList( period ) )
            .withFilterOrganisationUnits( Lists.newArrayList( orgUnit ) )
            .build();

        eventMap = getEventData( params );
    }

    /**
     * Gets sliding window aggregated event data for the given parameters.
     */
    private void getEventMapForSlidingWindow( )
    {
        if ( context.getEventItems().isEmpty() || period == null || orgUnit == null )
        {
            slidingWindowEventMap = new MapMap<>();

            return;
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

        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataDimensionItems( Lists.newArrayList( context.getEventItems() ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .withStartDate( startDate.getTime() )
            .withEndDate( endDate.getTime() )
            .withFilterOrganisationUnits( Lists.newArrayList( orgUnit ) )
            .build();

        slidingWindowEventMap = getEventData( params );
    }

    /**
     * Gets event data.
     *
     * @param params event data query parameters.
     * @return event data.
     */
    private MapMap<String, DimensionalItemObject, Double> getEventData( DataQueryParams params )
    {
        MapMap<String, DimensionalItemObject, Double> map = new MapMap<>();

        Grid grid = analyticsService.getAggregatedDataValues( params );

        int dxInx = grid.getIndexOfHeader( DimensionalObject.DATA_X_DIM_ID );
        int aoInx = grid.getIndexOfHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID );
        int vlInx = grid.getWidth() - 1;

        for ( List<Object> row : grid.getRows() )
        {
            String dx = (String) row.get( dxInx );
            String ao = (String) row.get( aoInx );
            Double vl = (Double) row.get( vlInx );

            map.putEntry( ao, new BaseDimensionalItemObject( dx ), vl );
        }

        return map;
    }
}
