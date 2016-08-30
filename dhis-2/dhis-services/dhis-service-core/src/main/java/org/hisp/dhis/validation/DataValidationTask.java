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

import com.google.common.collect.Sets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.ListMap;
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
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
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
    private PeriodService periodService;

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
        if ( context.getValidationResults().size() < (ValidationRunType.INTERACTIVE == context.getRunType()
            ? ValidationRuleService.MAX_INTERACTIVE_ALERTS : ValidationRuleService.MAX_SCHEDULED_ALERTS) )
        {
            for ( PeriodTypeExtended periodTypeX : context.getPeriodTypeExtendedMap().values() )
            {
                Collection<DataElement> sourceDataElements = periodTypeX.getSourceDataElements()
                    .get( sourceX.getSource() );
                Set<ValidationRule> rules = getRulesBySourceAndPeriodType( sourceX, periodTypeX, sourceDataElements );
                expressionService.explodeValidationRuleExpressions( rules );

                if ( !rules.isEmpty() )
                {
                    Set<DataElement> recursiveCurrentDataElements = getRecursiveCurrentDataElements( rules );

                    for ( Period period : periodTypeX.getPeriods() )
                    {
                        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap = new MapMap<>();
                        SetMap<Integer, DataElementOperand> incompleteValuesMap = new SetMap<>();
                        MapMap<Integer, DataElementOperand, Double> currentValueMap = getValueMap( periodTypeX,
                            periodTypeX.getDataElements(), sourceDataElements, recursiveCurrentDataElements,
                            periodTypeX.getAllowedPeriodTypes(), period, sourceX.getSource(), lastUpdatedMap,
                            incompleteValuesMap );

                        log.trace( "Source " + sourceX.getSource().getName() + " [" + period.getStartDate() + " - "
                            + period.getEndDate() + "]" + " currentValueMap[" + currentValueMap.size() + "]" );

                        for ( ValidationRule rule : rules )
                        {
                            if ( evaluateValidationCheck( currentValueMap, lastUpdatedMap, rule ) )
                            {
                                int n_years = rule.getAnnualSampleCount() == null ? 0 : rule.getAnnualSampleCount();
                                int window = rule.getSequentialSampleCount() == null ? 0
                                    : rule.getSequentialSampleCount();
                                int skip = rule.getSequentialSkipCount() == null ? 0
                                    : rule.getSequentialSkipCount();
                                Collection<PeriodType> periodTypes = context.getRuleXMap().get( rule )
                                    .getAllowedPastPeriodTypes();

                                log.debug( "Rule " + rule.getName() + " @" + period.getDisplayShortName() + " & "
                                    + sourceX.getSource() + " window=" + window + ", years=" + n_years );
                                Map<Integer, Double> leftSideValues = getRuleExpressionValueMap
                                    ( rule.getLeftSide(), rule.getSampleSkipTest(),
                                        currentValueMap, incompleteValuesMap, sourceX.getSource(),
                                        period, window, n_years, skip,
                                        periodTypeX, periodTypes, lastUpdatedMap, sourceDataElements );

                                if ( !leftSideValues.isEmpty()
                                    || Operator.compulsory_pair.equals( rule.getOperator() )
                                    || Operator.exclusive_pair.equals( rule.getOperator() ) )
                                {
                                    Map<Integer, Double> rightSideValues = getRuleExpressionValueMap
                                        ( rule.getRightSide(), rule.getSampleSkipTest(),
                                            currentValueMap, incompleteValuesMap, sourceX.getSource(),
                                            period, window, n_years, skip, periodTypeX, periodTypes, lastUpdatedMap,
                                            sourceDataElements );

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

                    if ( rule.getRuleType() == RuleType.VALIDATION )
                    {
                        // Make a copy so we can add to it.
                        deos = new HashSet<>( deos );
                        deos.addAll( expressionService
                            .getOperandsInExpression( rule.getRightSide().getExpression() ) );
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
            if ( rule.getRuleType() == RuleType.SURVEILLANCE )
            {
                Set<DataElement> cur = rule.getCurrentDataElements();
                
                if ( cur != null )
                {
                    recursiveCurrentDataElements.addAll( cur );
                }
            }
        }

        return recursiveCurrentDataElements;
    }

    private boolean falsy( Object o )
    {
        if ( o instanceof Boolean )
        {
            Boolean b = (Boolean) o;
            return b.booleanValue();
        }
        else if ( o instanceof Number )
        {
            Number n = (Number) o;
            return (n.intValue() == 0);
        }
        else if ( o instanceof String )
        {
            String s = (String) o;
            return s.isEmpty();
        }
        else
        {
            return false;
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
    private Map<Integer, Double> getExpressionValueMap
    ( Expression expression,
        Set<Integer> skipCombos,
        MapMap<Integer, DataElementOperand, Double> valueMap,
        SetMap<Integer, DataElementOperand> incompleteValuesMap )
    {
        Map<Integer, Double> expressionValueMap = new HashMap<>();

        if ( skipCombos == null )
        {
            for ( Map.Entry<Integer, Map<DataElementOperand, Double>> entry : valueMap.entrySet() )
            {
                Double value = expressionService.getExpressionValue( expression, entry.getValue(),
                    context.getConstantMap(), null, null, incompleteValuesMap.getSet( entry.getKey() ), null );

                if ( MathUtils.isValidDouble( value ) )
                {
                    expressionValueMap.put( entry.getKey(), value );
                }
            }
        }
        else
        {
            for ( Map.Entry<Integer, Map<DataElementOperand, Double>> entry : valueMap.entrySet() )
                if ( !(skipCombos.contains( entry.getKey() )) )
                {
                    Double value = expressionService.getExpressionValue( expression, entry.getValue(),
                        context.getConstantMap(), null, null, incompleteValuesMap.getSet( entry.getKey() ), null );

                    if ( MathUtils.isValidDouble( value ) )
                    {
                        expressionValueMap.put( entry.getKey(), value );
                    }
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

        log.trace( "getDataValueMapRecursive: source:" + source.getName() + " ruleDataElements["
            + ruleDataElements.size() + "] sourceDataElements[" + sourceDataElements.size() + "] elementsToGet["
            + dataElementsToGet.size() + "] recursiveDataElements[" + recursiveDataElements.size()
            + "] allowedPeriodTypes[" + allowedPeriodTypes.size() + "]" );

        MapMap<Integer, DataElementOperand, Double> dataValueMap = null;

        if ( dataElementsToGet.isEmpty() )
        {
            // We still might get something recursively
            dataValueMap = new MapMap<>();
        }
        else
        {
            dataValueMap = dataValueService.getDataValueMapByAttributeCombo( dataElementsToGet,
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

    // -------------------------------------------------------------------------
    // Generalized surveillance rules
    // -------------------------------------------------------------------------

    /**
     * Returns the aggregated values of an expression
     *
     * @param expression         the expression whose values are being fetched
     * @param skipTest           an expression (or null) specifying conditions for which values will be discarded
     * @param source             organisation unit being evaluated
     * @param period             current period being considered
     * @param window             how many annual samples (before and after the current period)
     * @param n_years            how many past years to include in the aggregate sample
     * @param skip               how many periods to skip before the current period
     * @param px
     * @param periodTypes        applicable period types
     * @param lastUpdatedMap
     * @param sourceDataElements the data elements collected by the organisation unit
     * @return the aggregated values for the expression as a map by attribute category combo
     */
    private ListMap<Integer, Double> getAggregateValueMap
    ( Expression expression, Expression skipTest, OrganisationUnit source,
        Period period, int window, int n_years, int skip, PeriodTypeExtended px, Collection<PeriodType> periodTypes,
        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap, Collection<DataElement> sourceDataElements )
    {
        ListMap<Integer, Double> results = new ListMap<Integer, Double>();
        PeriodType periodType = period.getPeriodType();
        Calendar yearly = PeriodType.createCalendarInstance( period.getStartDate() );

        for ( int years = 0; years <= n_years; years++ )
        {
            // Defensive copy because createPeriod mutates Calendar.
            Calendar each_year = PeriodType.createCalendarInstance( yearly.getTime() );
            // To track the period at the same time in preceding years.
            Period base_period = periodType.createPeriod( each_year );

            if ( years > 0 )
            {
                // For past years, fetch a window around the period at the same time of year as this period.

                // This first call gets the value for the same period in the previous year:
                getPeriodValues( results, expression, skipTest, source, base_period, 0, px,
                    periodTypes, lastUpdatedMap, sourceDataElements );
                // And if we're taking a window, this gets values for periods both before and after the same period
                if ( window != 0 )
                {
                    getPeriodValues( results, expression, skipTest, source, periodType.getNextPeriod( base_period ),
                        window - 1, px, periodTypes, lastUpdatedMap, sourceDataElements );
                    getPeriodValues( results, expression, skipTest, source, periodType.getPreviousPeriod( base_period ),
                        1 - window, px, periodTypes, lastUpdatedMap, sourceDataElements );
                }
            }
            else if ( window != 0 )
            {
                int steps = window - 1, skipping = skip;
                Period start = periodType.getPreviousPeriod( base_period );
                while ( skipping > 0 )
                {
                    start = periodType.getPreviousPeriod( start );
                    skipping--;
                    steps--;
                }
                if ( steps >= 0 )
                {
                    getPeriodValues( results, expression, skipTest, source, start,
                        -steps, px, periodTypes, lastUpdatedMap, sourceDataElements );
                }
            }

            // Move to the previous year.
            yearly.set( Calendar.YEAR, yearly.get( Calendar.YEAR ) - 1 );
        }

        return results;
    }

    /**
     * Gathers the values of an expression for a given organisation unit and
     * period, accumulating a range of values around the given period.
     * <p>
     * Note that for a surveillance-type rule, evaluating the right side
     * expression can result in sampling multiple periods and/or child
     * organisation units.
     *
     * @param results        the ListMap into which results will be stored
     * @param expression     the expression to be evaluated
     * @param source         the organisation unit
     * @param period         the main period for the validation rule evaluation
     * @param window         how many periods (before and after) to collect
     * @param px             the period type extended information
     * @param periodTypes    the period types in which the data may exist
     * @param sourceElements the data elements configured for this organisation
     *                       unit
     */
    private void getPeriodValues( ListMap<Integer, Double> results, Expression expression, Expression skipTest, OrganisationUnit source,
        Period period, int window, PeriodTypeExtended px, Collection<PeriodType> periodTypes,
        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap, Collection<DataElement> sourceElements )
    {
        PeriodType periodType = period.getPeriodType();
        Period periodInstance = periodService.getPeriod( period.getStartDate(),
            period.getEndDate(), periodType );

        if ( periodInstance == null )
        {
            return;
        }

        SetMap<Integer, DataElementOperand> incompleteValuesMap = new SetMap<Integer, DataElementOperand>();

        Set<DataElement> dataElements = getExpressionDataElements( expression );
        Set<Integer> skipCombos = (skipTest == null) ? (null) : getSkipCombos( skipTest, source, sourceElements,
            px, periodTypes, periodInstance, lastUpdatedMap, incompleteValuesMap );

        MapMap<Integer, DataElementOperand, Double> dataValueMapByAttributeCombo = getValueMap( 
            px, dataElements, sourceElements, dataElements, periodTypes, periodInstance, source, lastUpdatedMap, incompleteValuesMap );
        Map<Integer, Double> eValues = getExpressionValueMap( expression, skipCombos, dataValueMapByAttributeCombo, incompleteValuesMap );
        
        results.putValueMap( eValues );

        int direction = ((window < 0) ? (-1) : (window > 0) ? (1) : (0));
        int steps = ((direction > 0) ? (window) : (direction < 0) ? (-window) : (0));

        log.debug( "Gathering '" + expression.getExpression() + "' " + "at " + period + " (" + window + ") " + "from "
            + source.getName() + " starting with: " + results );

        if ( direction == 0 )
        {
            return;
        }

        Period scan = new Period( periodInstance );

        for ( int count = 0; count < steps; count++ )
        {
            if ( direction < 0 )
            {
                scan = periodType.getPreviousPeriod( scan );
            }
            else
            {
                scan = periodType.getNextPeriod( scan );
            }

            getPeriodValues( results, expression, skipTest, source, scan, 0, px, periodTypes, lastUpdatedMap, sourceElements );
        }
    }

    private Set<Integer> getSkipCombos( Expression skipTest, OrganisationUnit source, Collection<DataElement> sourceElements,
        PeriodTypeExtended px, Collection<PeriodType> periodTypes, Period period,
        MapMap<Integer, DataElementOperand, Date> lastUpdatedMap, SetMap<Integer, DataElementOperand> incompleteValuesMap )
    {
        Set<Integer> results = new HashSet<Integer>();
        Set<DataElement> dataElements = getExpressionDataElements( skipTest );

        MapMap<Integer, DataElementOperand, Double> skipMap = getValueMap
            ( px, dataElements, sourceElements, dataElements, periodTypes, period, source, lastUpdatedMap, incompleteValuesMap );
        
        for ( Map.Entry<Integer, Map<DataElementOperand, Double>> entry : skipMap.entrySet() )
        {
            Integer combo = entry.getKey();
            Object value = expressionService.getExpressionObjectValue( skipTest, entry.getValue(),
                context.getConstantMap(), null, null, incompleteValuesMap.getSet( entry.getKey() ), null );

            if ( !(falsy( value )) )
            {
                results.add( combo );
            }
        }

        return results.isEmpty() ? null : results;
    }

    /**
     * Returns the data elements referenced in an expression, as a set. This will
     * return an empty set if e.getPresentDataNeeded returns null.
     *
     * @param expression expression to evaluate.
     * @return a Set of DataElement(s)
     */
    private Set<DataElement> getExpressionDataElements( Expression expression )
    {
        Set<DataElement> elts = expression.getDataElementsInExpression();

        return elts != null ? elts : Sets.newHashSet();
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
    private Map<Integer, Double> getRuleExpressionValueMap
    ( Expression expression, Expression skipTest,
        MapMap<Integer, DataElementOperand, Double> valueMap, SetMap<Integer, DataElementOperand> incompleteValuesMap,
        OrganisationUnit source, Period period, int window, int n_years, int skip, PeriodTypeExtended px,
        Collection<PeriodType> periodTypes, MapMap<Integer, DataElementOperand, Date> lastUpdatedMap,
        Collection<DataElement> sourceElements )
    {
        Map<Integer, Double> expressionValueMap = new HashMap<>();
        Map<Integer, ListMap<String, Double>> aggregateValuesMap = new HashMap<>();
        Set<String> aggregates = expressionService.getAggregatesInExpression( expression.getExpression() );

        if ( aggregates.isEmpty() )
        {
            return getExpressionValueMap( expression, null, valueMap, incompleteValuesMap );
        }

        for ( String subExpression : aggregates )
        {
            Expression subexp = new Expression( subExpression, "aggregated", new HashSet<DataElement>( sourceElements ) );

            ListMap<Integer, Double> aggregateValues = getAggregateValueMap
                ( subexp, skipTest, source, period, window, n_years, skip,
                    px, periodTypes, lastUpdatedMap, sourceElements );

            for ( Integer attributeOptionCombo : aggregateValues.keySet() )
            {
                ListMap<String, Double> aggmap;

                if ( aggregateValuesMap.containsKey( attributeOptionCombo ) )
                {
                    aggmap = aggregateValuesMap.get( attributeOptionCombo );
                }
                else
                {
                    aggmap = new ListMap<>();
                    aggregateValuesMap.put( attributeOptionCombo, aggmap );
                }

                aggmap.put( subExpression, aggregateValues.get( attributeOptionCombo ) );
            }
        }

        for ( Map.Entry<Integer, Map<DataElementOperand, Double>> entry : valueMap.entrySet() )
        {
            Double value = expressionService.getExpressionValue( expression, entry.getValue(),
                context.getConstantMap(), null, null, incompleteValuesMap.getSet( entry.getKey() ),
                aggregateValuesMap.get( entry.getKey() ) );

            if ( MathUtils.isValidDouble( value ) )
            {
                expressionValueMap.put( entry.getKey(), value );
            }
        }

        return expressionValueMap;
    }
}
