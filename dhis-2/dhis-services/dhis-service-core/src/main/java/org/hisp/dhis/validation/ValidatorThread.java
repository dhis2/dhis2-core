package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.SetMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.CalendarPeriodType;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.MathUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
public class ValidatorThread
    implements Runnable
{
    private static final Log log = LogFactory.getLog( ValidatorThread.class );

    private OrganisationUnitExtended sourceX;

    private ValidationRunContext context;

    public ValidatorThread( OrganisationUnitExtended sourceX, ValidationRunContext context )
    {
        this.sourceX = sourceX;
        this.context = context;
    }

    /**
     * Evaluates validation rules for a single organisation unit. This is the
     * central method in validation rule evaluation.
     */
    @Override
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
                Collection<DataElement> sourceDataElements = periodTypeX.getSourceDataElements().get( sourceX.getSource() );
                Set<ValidationRule> rules = getRulesBySourceAndPeriodType( sourceX, periodTypeX, sourceDataElements );
                context.getExpressionService().explodeValidationRuleExpressions( rules );

                if ( !rules.isEmpty() )
                {
                    Set<DataElement> recursiveCurrentDataElements = getRecursiveCurrentDataElements( rules );

                    for ( Period period : periodTypeX.getPeriods() )
                    {
                        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap = new MapMap<>();
                        SetMap<Integer, DataElementOperand> incompleteValuesMap = new SetMap<>();
                        MapMap<Integer, DataElementOperand, Double> currentValueMap = getValueMap( periodTypeX,
                            periodTypeX.getDataElements(), sourceDataElements, recursiveCurrentDataElements,
                            periodTypeX.getAllowedPeriodTypes(), period, sourceX.getSource(), lastUpdatedMap, incompleteValuesMap );

                        log.trace( "Source " + sourceX.getSource().getName()
                            + " [" + period.getStartDate() + " - " + period.getEndDate() + "]"
                            + " currentValueMap[" + currentValueMap.size() + "]" );

                        for ( ValidationRule rule : rules )
                        {
                            if ( evaluateValidationCheck( currentValueMap, lastUpdatedMap, rule ) )
                            {
                                Map<Integer, Double> leftSideValues = getExpressionValueMap( rule.getLeftSide(),
                                    currentValueMap, incompleteValuesMap );

                                if ( !leftSideValues.isEmpty()
                                    || Operator.compulsory_pair.equals( rule.getOperator() )
                                    || Operator.exclusive_pair.equals( rule.getOperator() ))
                                {
                                    Map<Integer, Double> rightSideValues = getRightSideValue( sourceX.getSource(), periodTypeX, period, rule,
                                        currentValueMap, sourceDataElements );

                                    if ( !rightSideValues.isEmpty()
                                        || Operator.compulsory_pair.equals( rule.getOperator() )
                                        || Operator.compulsory_pair.equals( rule.getOperator() ))
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
                                                violation = ( leftSide != null && rightSide != null );
                                            }
                                            else if ( leftSide != null && rightSide != null )
                                            {
                                                violation = !expressionIsTrue( leftSide, rule.getOperator(), rightSide );
                                            }

                                            if ( violation )
                                            {
                                                context.getValidationResults().add( new ValidationResult(
                                                    period, sourceX.getSource(),
                                                    context.getDataElementCategoryService().getDataElementCategoryOptionCombo( optionCombo ), rule,
                                                    roundSignificant( zeroIfNull( leftSide ) ),
                                                    roundSignificant( zeroIfNull( rightSide ) ) ) );
                                            }

                                            log.trace( "Evaluated " + rule.getName()
                                                + ", combo id " + optionCombo + ": "
                                                + (violation ? "violation" : "OK") + " " + (leftSide == null ? "(null)" : leftSide.toString())
                                                + " " + rule.getOperator() + " " + (rightSide == null ? "(null)" : rightSide.toString())
                                                + " (" + context.getValidationResults().size() + " results)" );
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
        PeriodTypeExtended periodTypeX, Collection<DataElement> sourceDataElements )
    {
        Set<ValidationRule> periodTypeRules = new HashSet<>();

        for ( ValidationRule rule : periodTypeX.getRules() )
        {
            if ( rule.getRuleType() == RuleType.VALIDATION )
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
            else
            {
                // For surveillance-type rules, include only rules for this
                // organisation's unit level.
                // The organisation may not be configured for the data elements
                // because they could be aggregated from a lower level.
                if ( rule.getOrganisationUnitLevel() == sourceX.getLevel() )
                {
                    periodTypeRules.add( rule );
                }
            }
        }

        return periodTypeRules;
    }

    /**
     * Checks to see if the evaluation should go further for this
     * evaluationRule, after the "current" data to evaluate has been fetched.
     * For INTERACTIVE runs, we always go further (always return true.) For
     * SCHEDULED runs, we go further only if something has changed since the
     * last successful scheduled run -- either the rule definition or one of
     * the "current" data element / option values on the left or right sides.
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
                    Collection<DataElementOperand> deos = context.getExpressionService().getOperandsInExpression(
                        rule.getLeftSide().getExpression() );

                    if ( rule.getRuleType() == RuleType.VALIDATION )
                    {
                        // Make a copy so we can add to it.
                        deos = new HashSet<>( deos );
                        deos.addAll( context.getExpressionService().getOperandsInExpression( rule.getRightSide().getExpression() ) );
                    }

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
                                saveThisCombo = true; // True if new/updated data.
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
     * Gets the data elements for which values should be fetched recursively if
     * they are not collected for an organisation unit.
     *
     * @param rules ValidationRules to be evaluated
     * @return the data elements to fetch recursively
     */
    private Set<DataElement> getRecursiveCurrentDataElements( Set<ValidationRule> rules )
    {
        Set<DataElement> recursiveCurrentDataElements = new HashSet<>();

        for ( ValidationRule rule : rules )
        {
            if ( rule.getRuleType() == RuleType.SURVEILLANCE && rule.getCurrentDataElements() != null )
            {
                recursiveCurrentDataElements.addAll( rule.getCurrentDataElements() );
            }
        }

        return recursiveCurrentDataElements;
    }

    /**
     * Returns the right-side evaluated value of the validation rule.
     *
     * @param source             organisation unit being evaluated
     * @param periodTypeX        period type being evaluated
     * @param period             period being evaluated
     * @param rule               ValidationRule being evaluated
     * @param currentValueMap    current values already fetched
     * @param sourceDataElements the data elements collected by the organisation
     *                           unit
     * @return the right-side values, map by attribute category combo
     */
    private Map<Integer, Double> getRightSideValue( OrganisationUnit source, PeriodTypeExtended periodTypeX, Period period,
        ValidationRule rule, MapMap<Integer, DataElementOperand, Double> currentValueMap,
        Collection<DataElement> sourceDataElements )
    {
        Map<Integer, Double> rightSideValues = null;

        // If ruleType is VALIDATION, the right side is evaluated using the same
        // (current) data values. If ruleType is SURVEILLANCE but there are no
        // data elements in the right side, then it doesn't matter what data
        // values we use, so just supply the current data values in order to
        // evaluate the (constant) expression.

        if ( rule.getRuleType() == RuleType.VALIDATION || rule.getRightSide().getDataElementsInExpression().isEmpty() )
        {
            rightSideValues = getExpressionValueMap( rule.getRightSide(), currentValueMap, new SetMap<>() );
        }
        else
        // ruleType equals SURVEILLANCE, and there are some data elements in the
        // right side expression
        {
            CalendarPeriodType calendarPeriodType = (CalendarPeriodType) period.getPeriodType();
            Collection<PeriodType> rightSidePeriodTypes = context.getRuleXMap().get( rule ).getAllowedPastPeriodTypes();
            ListMap<Integer, Double> sampleValuesMap = new ListMap<>();
            Calendar yearlyCalendar = PeriodType.createCalendarInstance( period.getStartDate() );
            int annualSampleCount = rule.getAnnualSampleCount() == null ? 0 : rule.getAnnualSampleCount();
            int sequentialSampleCount = rule.getSequentialSampleCount() == null ? 0 : rule
                .getSequentialSampleCount();

            for ( int annualCount = 0; annualCount <= annualSampleCount; annualCount++ )
            {
                // Defensive copy because createPeriod mutates Calendar.
                Calendar calCopy = PeriodType.createCalendarInstance( yearlyCalendar.getTime() );

                // To track the period at the same time in preceding years.
                Period yearlyPeriod = calendarPeriodType.createPeriod( calCopy );

                // For past years, fetch the period at the same time of year
                // as this period, and any periods after this period within the
                // sequentialPeriod limit. For the year of the stating period,
                // we will only fetch previous sequential periods.

                if ( annualCount > 0 )
                {
                    // Fetch the period at the same time of year as the
                    // starting period.
                    evaluateRightSidePeriod( periodTypeX, sampleValuesMap, source, rightSidePeriodTypes, yearlyPeriod,
                        rule, sourceDataElements );

                    // Fetch the sequential periods after this prior-year
                    // period.
                    Period sequentialPeriod = new Period( yearlyPeriod );

                    for ( int sequentialCount = 0; sequentialCount < sequentialSampleCount; sequentialCount++ )
                    {
                        sequentialPeriod = calendarPeriodType.getNextPeriod( sequentialPeriod );
                        evaluateRightSidePeriod( periodTypeX, sampleValuesMap, source, rightSidePeriodTypes,
                            sequentialPeriod, rule, sourceDataElements );
                    }
                }

                // Fetch the sequential periods before this period (both this
                // year and past years).
                Period sequentialPeriod = new Period( yearlyPeriod );

                for ( int sequentialCount = 0; sequentialCount < sequentialSampleCount; sequentialCount++ )
                {
                    sequentialPeriod = calendarPeriodType.getPreviousPeriod( sequentialPeriod );
                    evaluateRightSidePeriod( periodTypeX, sampleValuesMap, source, rightSidePeriodTypes,
                        sequentialPeriod, rule, sourceDataElements );
                }

                // Move to the previous year.
                yearlyCalendar.set( Calendar.YEAR, yearlyCalendar.get( Calendar.YEAR ) - 1 );
            }

            rightSideValues = new HashMap<>();

            for ( Map.Entry<Integer, List<Double>> e : sampleValuesMap.entrySet() )
            {
                rightSideValues.put( e.getKey(), rightSideAverage( rule, e.getValue(), annualSampleCount, sequentialSampleCount ) );
            }

        }
        return rightSideValues;
    }

    /**
     * Evaluates the right side of a surveillance-type validation rule for
     * a given organisation unit and period, and adds the value to a list
     * of sample values.
     * <p>
     * Note that for a surveillance-type rule, evaluating the right side
     * expression can result in sampling multiple periods and/or child
     * organisation units.
     *
     * @param periodTypeX        the period type extended information
     * @param sampleValuesMap    the lists of sample values to add to
     * @param source             the organisation unit
     * @param allowedPeriodTypes the period types in which the data may exist
     * @param period             the main period for the validation rule evaluation
     * @param rule               the surveillance-type rule being evaluated
     * @param sourceDataElements the data elements configured for this
     *                           organisation unit
     */
    private void evaluateRightSidePeriod( PeriodTypeExtended periodTypeX, ListMap<Integer, Double> sampleValuesMap,
        OrganisationUnit source, Collection<PeriodType> allowedPeriodTypes, Period period, ValidationRule rule,
        Collection<DataElement> sourceDataElements )
    {
        Period periodInstance = context.getPeriodService().getPeriod( period.getStartDate(), period.getEndDate(),
            period.getPeriodType() );

        if ( periodInstance != null )
        {
            Set<DataElement> dataElements = rule.getRightSide().getDataElementsInExpression();
            SetMap<Integer, DataElementOperand> incompleteValuesMap = new SetMap<>();
            MapMap<Integer, DataElementOperand, Double> dataValueMapByAttributeCombo = getValueMap( periodTypeX, dataElements,
                sourceDataElements, dataElements, allowedPeriodTypes, period, source, null, incompleteValuesMap );
            sampleValuesMap.putValueMap( getExpressionValueMap( rule.getRightSide(), dataValueMapByAttributeCombo, incompleteValuesMap ) );
        }
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
            Double value = context.getExpressionService().getExpressionValue( expression,
                entry.getValue(), context.getConstantMap(), null, null,
                incompleteValuesMap.getSet( entry.getKey() ) );

            if ( MathUtils.isValidDouble( value ) )
            {
                expressionValueMap.put( entry.getKey(), value );
            }
        }

        return expressionValueMap;
    }

    /**
     * Finds the average right-side sample value. This is used as the right-side
     * expression value to evaluate a surveillance-type rule.
     *
     * @param rule                  surveillance-type rule being evaluated
     * @param sampleValues          sample values actually collected
     * @param annualSampleCount     number of annual samples tried for
     * @param sequentialSampleCount number of sequential samples tried for
     * @return average right-side sample value
     */
    private Double rightSideAverage( ValidationRule rule, List<Double> sampleValues,
        int annualSampleCount, int sequentialSampleCount )
    {
        // Find the expected sample count for the last period of its type in the
        // database: sequentialSampleCount for the immediately preceding periods 
        // in this year and for every past year: one sample for the same period 
        // in that year, plus sequentialSampleCounts before and after.
        Double average = null;

        if ( !sampleValues.isEmpty() )
        {
            int expectedSampleCount = sequentialSampleCount + annualSampleCount * (1 + 2 * sequentialSampleCount);
            int highOutliers = rule.getHighOutliers() == null ? 0 : rule.getHighOutliers();
            int lowOutliers = rule.getLowOutliers() == null ? 0 : rule.getLowOutliers();

            // If fewer than the expected number of samples, then scale back
            if ( sampleValues.size() < expectedSampleCount )
            {
                highOutliers = (highOutliers * sampleValues.size()) / expectedSampleCount;
                lowOutliers = (lowOutliers * sampleValues.size()) / expectedSampleCount;
            }

            // If we still have any high and/or low outliers to remove, then
            // sort the sample values and remove the high and/or low outliers
            if ( highOutliers + lowOutliers > 0 )
            {
                Collections.sort( sampleValues );
                log.trace( "Removing " + highOutliers + " high and " + lowOutliers + " low outliers from "
                    + Arrays.toString( sampleValues.toArray() ) );
                sampleValues = sampleValues.subList( lowOutliers, sampleValues.size() - highOutliers );
                log.trace( "Result: " + Arrays.toString( sampleValues.toArray() ) );
            }

            Double sum = 0.0;

            for ( Double sample : sampleValues )
            {
                sum += sample;
            }

            average = sum / sampleValues.size();
        }

        return average;
    }

    /**
     * Gets data values for a given organisation unit and period, recursing if
     * necessary to sum the values from child organisation units.
     *
     * @param periodTypeX           period type which we are evaluating
     * @param ruleDataElements      data elements configured for the rule
     * @param sourceDataElements    data elements configured for the organisation
     *                              unit
     * @param recursiveDataElements data elements for which we will recurse if
     *                              necessary
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
        Set<DataElement> recursiveDataElements, Collection<PeriodType> allowedPeriodTypes, Period period,
        OrganisationUnit source, MapMap<Integer, DataElementOperand, Date> lastUpdatedMap,
        SetMap<Integer, DataElementOperand> incompleteValuesMap )
    {
        Set<DataElement> dataElementsToGet = new HashSet<>( ruleDataElements );
        dataElementsToGet.retainAll( sourceDataElements );

        log.trace( "getDataValueMapRecursive: source:" + source.getName()
            + " ruleDataElements[" + ruleDataElements.size()
            + "] sourceDataElements[" + sourceDataElements.size()
            + "] elementsToGet[" + dataElementsToGet.size()
            + "] recursiveDataElements[" + recursiveDataElements.size()
            + "] allowedPeriodTypes[" + allowedPeriodTypes.size() + "]" );

        MapMap<Integer, DataElementOperand, Double> dataValueMap = null;

        if ( dataElementsToGet.isEmpty() )
        {
            // We still might get something recursively
            dataValueMap = new MapMap<>();
        }
        else
        {
            dataValueMap = context.getDataValueService().getDataValueMapByAttributeCombo( dataElementsToGet,
                period.getStartDate(), source, allowedPeriodTypes, context.getAttributeCombo(),
                context.getCogDimensionConstraints(), context.getCoDimensionConstraints(), lastUpdatedMap );
        }

        // See if there are any data elements we need to get recursively:
        Set<DataElement> recursiveDataElementsNeeded = new HashSet<>( recursiveDataElements );
        recursiveDataElementsNeeded.removeAll( dataElementsToGet );

        if ( !recursiveDataElementsNeeded.isEmpty() )
        {
            int childCount = 0;
            MapMap<Integer, DataElementOperand, Integer> childValueCounts = new MapMap<>();

            for ( OrganisationUnit child : source.getChildren() )
            {
                Collection<DataElement> childDataElements = periodTypeX.getSourceDataElements().get( child );
                MapMap<Integer, DataElementOperand, Double> childMap = getValueMap( periodTypeX,
                    recursiveDataElementsNeeded, childDataElements, recursiveDataElementsNeeded, allowedPeriodTypes,
                    period, child, lastUpdatedMap, incompleteValuesMap );

                for ( Map.Entry<Integer, Map<DataElementOperand, Double>> entry : childMap.entrySet() )
                {
                    int combo = entry.getKey();

                    for ( Map.Entry<DataElementOperand, Double> e : entry.getValue().entrySet() )
                    {
                        DataElementOperand deo = e.getKey();
                        Double childValue = e.getValue();

                        Double baseValue = dataValueMap.getValue( combo, deo );
                        dataValueMap.putEntry( combo, deo, baseValue == null ? childValue : baseValue + childValue );

                        Integer childValueCount = childValueCounts.getValue( combo, deo );
                        childValueCounts.putEntry( combo, deo, childValueCount == null ? 1 : childValueCount + 1 );
                    }
                }

                childCount++;
            }

            for ( Map.Entry<Integer, Map<DataElementOperand, Integer>> entry : childValueCounts.entrySet() )
            {
                int combo = entry.getKey();

                for ( Map.Entry<DataElementOperand, Integer> e : entry.getValue().entrySet() )
                {
                    DataElementOperand deo = e.getKey();
                    Integer childValueCount = e.getValue();

                    if ( childValueCount != childCount )
                    {
                        // Remember that we found this DataElementOperand value
                        // in some but not all children
                        incompleteValuesMap.putValue( combo, deo );
                    }
                }
            }
        }

        return dataValueMap;
    }
}
