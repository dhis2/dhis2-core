package org.hisp.dhis.validation;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.SetMap;
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
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    public static final String NAME = "validationTask";

    private static final Log log = LogFactory.getLog( DataValidationTask.class );

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataElementCategoryService categoryService;

    private OrganisationUnitExtended sourceX;

    private ValidationRunContext context;

    public void init( OrganisationUnitExtended sourceX, ValidationRunContext context )
    {
        this.sourceX = sourceX;
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
        catch ( RuntimeException ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );

            throw ex;
        }
    }

    private void runInternal()
    {
        if ( context.getValidationResults().size() < (ValidationRunType.INTERACTIVE == context.getRunType() ? 
            ValidationRuleService.MAX_INTERACTIVE_ALERTS : ValidationRuleService.MAX_SCHEDULED_ALERTS) )
        {
            for ( PeriodTypeExtended periodTypeX : context.getPeriodTypeExtendedMap().values() )
            {
                Collection<DataElement> sourceDataElements = periodTypeX.getSourceDataElements()
                    .get( sourceX.getSource() );
                Set<ValidationRule> rules = getRulesBySourceAndPeriodType( sourceX, periodTypeX, sourceDataElements, context );
                expressionService.explodeValidationRuleExpressions( rules );

                if ( !rules.isEmpty() )
                {
                    for ( Period period : periodTypeX.getPeriods() )
                    {
                        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap = new MapMap<>();
                        SetMap<Integer, DataElementOperand> incompleteValuesMap = new SetMap<>();
                        MapMap<Integer, DataElementOperand, Double> currentValueMap = getValueMap( periodTypeX,
                            periodTypeX.getDataElements(), sourceDataElements,
                            periodTypeX.getAllowedPeriodTypes(), period, sourceX.getSource(), lastUpdatedMap,
                            incompleteValuesMap );

                        log.trace( "Source " + sourceX.getSource().getName() + " [" + period.getStartDate() + " - "
                            + period.getEndDate() + "]" + " currentValueMap[" + currentValueMap.size() + "]" );

                        for ( ValidationRule rule : rules )
                        {
                            if ( evaluateValidationCheck( currentValueMap, lastUpdatedMap, rule ) )
                            {
                                Map<Integer, Double> leftSideValues =
                                    getExpressionValueMap( rule.getLeftSide(), currentValueMap, incompleteValuesMap );

                                if ( !leftSideValues.isEmpty()
                                    || Operator.compulsory_pair.equals( rule.getOperator() )
                                    || Operator.exclusive_pair.equals( rule.getOperator() ) )
                                {
                                    Map<Integer, Double> rightSideValues =
                                        getExpressionValueMap( rule.getRightSide(), currentValueMap, incompleteValuesMap );
                                    if ( !rightSideValues.isEmpty()
                                        || Operator.compulsory_pair.equals( rule.getOperator() )
                                        || Operator.exclusive_pair.equals( rule.getOperator() ) )
                                    {
                                        Set<Integer> attributeOptionCombos = leftSideValues.keySet();

                                        if ( Operator.compulsory_pair.equals( rule.getOperator() ) ||
                                            Operator.exclusive_pair.equals( rule.getOperator() ) )
                                        {
                                            attributeOptionCombos = new HashSet<>( attributeOptionCombos );
                                            attributeOptionCombos.addAll( rightSideValues.keySet() );
                                        }

                                        for ( int optionCombo : attributeOptionCombos )
                                        {
                                            Double leftSide = leftSideValues.get( optionCombo );
                                            Double rightSide = rightSideValues.get( optionCombo );
                                            boolean violation = false;

                                            if ( Operator.compulsory_pair.equals( rule.getOperator() ) )
                                            {
                                                violation = (leftSide != null && rightSide == null)
                                                    || (leftSide == null && rightSide != null);
                                            }
                                            else if ( Operator.exclusive_pair.equals( rule.getOperator() ) )
                                            {
                                                violation = (leftSide != null && rightSide != null);
                                            }
                                            else if ( leftSide != null && rightSide != null )
                                            {
                                                violation = !expressionIsTrue( leftSide, rule.getOperator(),
                                                    rightSide );
                                            }

                                            if ( violation )
                                            {
                                                context.getValidationResults()
                                                    .add( new ValidationResult( period, sourceX.getSource(),
                                                        categoryService.getDataElementCategoryOptionCombo( optionCombo ),
                                                        rule, roundSignificant( zeroIfNull( leftSide ) ),
                                                        roundSignificant( zeroIfNull( rightSide ) ) ) );
                                            }

                                            log.debug( "Evaluated " + rule.getName() + ", combo id " + optionCombo
                                                + ": " + (violation ? "violation" : "OK") + " "
                                                + (leftSide == null ? "(null)" : leftSide.toString()) + " "
                                                + rule.getOperator() + " "
                                                + (rightSide == null ? "(null)" : rightSide.toString()) + " ("
                                                + context.getValidationResults().size() + " results)" );

                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gets the rules that should be evaluated for a given organisation unit and
     * period type.
     *
     * @param sourceX            the organisation unit extended information
     * @param periodTypeX        the period type extended information
     * @param sourceDataElements all data elements collected for this
     *                           organisation unit
     * @return set of rules for this org unit and period type
     */
    private Set<ValidationRule> getRulesBySourceAndPeriodType( OrganisationUnitExtended sourceX,
        PeriodTypeExtended periodTypeX, Collection<DataElement> sourceDataElements,
        ValidationRunContext context )
    {
        Set<ValidationRule> periodTypeRules = new HashSet<>();

        for ( ValidationRule rule : periodTypeX.getRules() )
        {
                // For validation-type rules, include only rules where the
                // organisation collects all the data elements in the rule.
                // But if this is some funny kind of rule with no elements
                // (like for testing), include it also.
                Collection<DataElement> elements = rule.getCurrentDataElements();

                if ( elements == null || elements.size() == 0 || sourceDataElements.containsAll( elements ) )
                {
                    periodTypeRules.add( rule );
                }
        }

        return periodTypeRules;
    }

    /**
     * Checks to see if the evaluation should go further for this
     * evaluationRule, after the "current" data to evaluate has been fetched.
     * For INTERACTIVE runs, we always go further (always return true.) For
     * SCHEDULED runs, we go further only if something has changed since the
     * last successful scheduled run -- either the rule definition or one of the
     * "current" data element / option values on the left or right sides.
     * <p>
     * For scheduled runs, remove all values for any attribute option combos
     * where nothing has changed since the last run.
     *
     * @param lastUpdatedMapMap when each data value was last updated
     * @param rule              the rule that may be evaluated
     * @return true if the rule should be evaluated with this data, false if not
     */
    private boolean evaluateValidationCheck( MapMap<Integer, DataElementOperand, Double> currentValueMapMap,
        MapMap<Integer, DataElementOperand, Date> lastUpdatedMapMap, ValidationRule rule )
    {
        boolean evaluate = true; // Assume true for now.

        if ( ValidationRunType.SCHEDULED == context.getRunType() )
        {
            if ( context.getLastScheduledRun() != null ) // True if no previous scheduled run
            {
                if ( rule.getLastUpdated().before( context.getLastScheduledRun() ) )
                {
                    // Get the "current" DataElementOperands from this rule:
                    // Left+Right sides for VALIDATION, Left side only for
                    // SURVEILLANCE.
                    Collection<DataElementOperand> deos = expressionService
                        .getOperandsInExpression( rule.getLeftSide().getExpression() );

                    // Return true if any data is more recent than the last
                    // scheduled run, otherwise return false.
                    evaluate = false;

                    for ( Map.Entry<Integer, Map<DataElementOperand, Date>> entry : lastUpdatedMapMap.entrySet() )
                    {
                        boolean saveThisCombo = false;

                        for ( DataElementOperand deo : deos )
                        {
                            Date lastUpdated = entry.getValue().get( deo );

                            if ( lastUpdated != null && lastUpdated.after( context.getLastScheduledRun() ) )
                            {
                                saveThisCombo = true; // True if new/updated data
                                evaluate = true;
                                break;
                            }
                        }

                        if ( !saveThisCombo )
                        {
                            currentValueMapMap.remove( entry.getKey() );
                        }
                    }
                }
            }
        }
        
        return evaluate;
    }

    /**
     * Evaluates an expression, returning a map of values by attribute option
     * combo.
     *
     * @param expression          expression to evaluate.
     * @param valueMap            Map of value maps, by attribute option combo.
     * @param incompleteValuesMap map of values that were incomplete.
     * @return map of values.
     */
    private Map<Integer, Double> getExpressionValueMap( Expression expression,
        MapMap<Integer, DataElementOperand, Double> valueMap,
        SetMap<Integer, DataElementOperand> incompleteValuesMap )
    {
        Map<Integer, Double> expressionValueMap = new HashMap<>();

        for ( Map.Entry<Integer, Map<DataElementOperand, Double>> entry : valueMap.entrySet() )
        {
            Double value = expressionService.getExpressionValue( expression, entry.getValue(),
                    context.getConstantMap(), null, null, incompleteValuesMap.getSet( entry.getKey() ), null );

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
     * @param periodTypeX           period type which we are evaluating
     * @param ruleDataElements      data elements configured for the rule
     * @param sourceDataElements    data elements configured for the organisation
     *                              unit
     * @param allowedPeriodTypes    all the periods in which we might find the data
     *                              values
     * @param period                period in which we are looking for values
     * @param source                organisation unit for which we are looking for values
     * @param lastUpdatedMap        map showing when each data values was last updated
     * @param incompleteValuesMap   ongoing set showing which values were found
     *                              but not from all children, mapped by attribute option combo.
     * @return map of attribute option combo to map of values found.
     */
    private MapMap<Integer, DataElementOperand, Double> getValueMap( PeriodTypeExtended periodTypeX,
        Collection<DataElement> ruleDataElements, Collection<DataElement> sourceDataElements,
        Collection<PeriodType> allowedPeriodTypes, Period period,
        OrganisationUnit source, MapMap<Integer, DataElementOperand, Date> lastUpdatedMap,
        SetMap<Integer, DataElementOperand> incompleteValuesMap )
    {
        Set<DataElement> dataElementsToGet = new HashSet<>( ruleDataElements );
        dataElementsToGet.retainAll( sourceDataElements );

        log.trace( "getDataValueMapRecursive: source:" + source.getName() + " ruleDataElements["
            + ruleDataElements.size() + "] sourceDataElements[" + sourceDataElements.size() + "] elementsToGet["
            + dataElementsToGet.size() + "] allowedPeriodTypes[" + allowedPeriodTypes.size() + "]" );

        if ( dataElementsToGet.isEmpty() )
        {
            return new MapMap<>();
        }
        else
        {
            return dataValueService.getDataValueMapByAttributeCombo( dataElementsToGet,
                period.getStartDate(), source, allowedPeriodTypes, context.getAttributeCombo(),
                context.getCogDimensionConstraints(), context.getCoDimensionConstraints(), lastUpdatedMap );
        }
    }
}
