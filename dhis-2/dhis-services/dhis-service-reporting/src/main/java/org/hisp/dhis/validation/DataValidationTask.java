package org.hisp.dhis.validation;

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
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.MapMap;
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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.system.util.MathUtils.expressionIsTrue;
import static org.hisp.dhis.system.util.MathUtils.roundSignificant;
import static org.hisp.dhis.system.util.MathUtils.zeroIfNull;
import static org.hisp.dhis.validation.ValidationService.MAX_INTERACTIVE_ALERTS;
import static org.hisp.dhis.validation.ValidationService.MAX_SCHEDULED_ALERTS;

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
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );

            throw ex;
        }
    }

    private void runInternal()
    {
        int maxAlerts = ValidationRunType.INTERACTIVE == context.getRunType() ? MAX_INTERACTIVE_ALERTS : MAX_SCHEDULED_ALERTS;
        
        if ( context.getValidationResults().size() < maxAlerts )
        {
            for ( PeriodTypeExtended periodTypeX : context.getPeriodTypeExtendedMap().values() )
            {
                Set<DataElement> sourceDataElements = periodTypeX.getSourceDataElements().get( sourceX.getSource() );
                
                Set<ValidationRule> rules = getRulesBySourceAndPeriodType( periodTypeX, sourceDataElements );
                
                expressionService.explodeValidationRuleExpressions( rules );

                if ( !rules.isEmpty() )
                {
                    for ( Period period : periodTypeX.getPeriods() )
                    {
                        MapMap<String, DataElementOperand, Date> lastUpdatedMap = new MapMap<>();

                        MapMap<String, DimensionalItemObject, Double> dataValueMap = getDataValueMap( 
                            periodTypeX.getDataElements(), sourceDataElements, periodTypeX.getAllowedPeriodTypes(), 
                            period, sourceX.getSource(), lastUpdatedMap );
                        
                        MapMap<String, DimensionalItemObject, Double> eventMap = getEventMap( context.getDimensionItems(), period, sourceX.getSource() );
                        
                        dataValueMap.putMap( eventMap );

                        log.trace( "Source " + sourceX.getSource().getName() + " [" + period.getStartDate() + " - "
                            + period.getEndDate() + "]" + " currentValueMap[" + dataValueMap.size() + "]" );

                        for ( ValidationRule rule : rules )
                        {
                            if ( evaluateValidationCheck( dataValueMap, lastUpdatedMap, rule ) || !eventMap.isEmpty() )
                            {
                                Map<String, Double> leftSideValues =
                                    getExpressionValueMap( rule.getLeftSide(), dataValueMap );

                                Map<String, Double> rightSideValues =
                                    getExpressionValueMap( rule.getRightSide(), dataValueMap );

                                Set<String> attributeOptionCombos = Sets.newHashSet( leftSideValues.keySet() );
                                attributeOptionCombos.addAll( rightSideValues.keySet() );

                                for ( String optionCombo : attributeOptionCombos )
                                {
                                    Double leftSide = leftSideValues.get( optionCombo );
                                    Double rightSide = rightSideValues.get( optionCombo );
                                    boolean violation = false;

                                    if ( Operator.compulsory_pair.equals( rule.getOperator() ) )
                                    {
                                        violation = ( leftSide != null && rightSide == null )
                                            || ( leftSide == null && rightSide != null );
                                    }
                                    else if ( Operator.exclusive_pair.equals( rule.getOperator() ) )
                                    {
                                        violation = ( leftSide != null && rightSide != null );
                                    }
                                    else
                                    {
                                        if ( leftSide == null && rule.getLeftSide().getMissingValueStrategy() == NEVER_SKIP )
                                        {
                                            leftSide = 0d;
                                        }

                                        if ( rightSide == null && rule.getRightSide().getMissingValueStrategy() == NEVER_SKIP )
                                        {
                                            rightSide = 0d;
                                        }

                                        if ( leftSide != null && rightSide != null )
                                        {
                                            violation = !expressionIsTrue( leftSide, rule.getOperator(), rightSide );
                                        }
                                    }

                                    if ( violation )
                                    {
                                        context.getValidationResults().add( new ValidationResult( 
                                            period, sourceX.getSource(),
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

    /**
     * Gets the rules that should be evaluated for a given organisation unit and
     * period type.
     *
     * @param periodTypeX        the period type extended information
     * @param sourceDataElements all data elements collected for this
     *                           organisation unit
     * @return set of rules for this org unit and period type
     */
    private Set<ValidationRule> getRulesBySourceAndPeriodType( PeriodTypeExtended periodTypeX, 
        Set<DataElement> sourceDataElements )
    {
        Set<ValidationRule> periodTypeRules = new HashSet<>();

        for ( ValidationRule rule : periodTypeX.getRules() )
        {
            // Include only rules where the organisation collects all the data elements
            // in the rule, or rules which have no data elements.
        
            Set<DataElement> elements = getDataElements( rule );

            if ( elements.isEmpty() || sourceDataElements.containsAll( elements ) )
            {
                periodTypeRules.add( rule );
            }
        }

        return periodTypeRules;
    }

    /**
     * Gets data elements part of left side and right side expressions of the
     * given validation rule.
     * 
     * @param validationRule the validation rule.
     */
    private Set<DataElement> getDataElements( ValidationRule validationRule )
    {
        Set<DataElement> elements = new HashSet<>();
        elements.addAll( expressionService.getDataElementsInExpression( validationRule.getLeftSide().getExpression() ) );
        elements.addAll( expressionService.getDataElementsInExpression( validationRule.getRightSide().getExpression() ) );
        return elements;
    }
    
    /**
     * Checks to see if the evaluation should go further for this
     * evaluationRule, after the "current" data to evaluate has been fetched.
     * For INTERACTIVE runs, we always go further (always return true.) For
     * SCHEDULED runs, we go further only if something has changed since the
     * last successful scheduled run. Either the rule definition or one of the
     * "current" data element / option values on the left or right sides.
     * <p>
     * For scheduled runs, remove all values for any attribute option combinations
     * where nothing has changed since the last run.
     *
     * @param lastUpdatedMapMap when each data value was last updated
     * @param rule              the rule that may be evaluated
     * @return true if the rule should be evaluated with this data, false if not
     */
    private boolean evaluateValidationCheck( MapMap<String, DimensionalItemObject, Double> currentValueMapMap,
        MapMap<String, DataElementOperand, Date> lastUpdatedMapMap, ValidationRule rule )
    {
        boolean evaluate = true; // Assume true for now

        if ( ValidationRunType.SCHEDULED == context.getRunType() )
        {
            if ( context.getLastScheduledRun() != null ) // True if no previous scheduled run
            {
                if ( rule.getLastUpdated().before( context.getLastScheduledRun() ) )
                {
                    Set<DataElementOperand> deos = expressionService
                        .getOperandsInExpression( rule.getLeftSide().getExpression() );

                    // Return true if any data is more recent than the last
                    // scheduled run, otherwise return false
                    evaluate = false;

                    for ( Map.Entry<String, Map<DataElementOperand, Date>> entry : lastUpdatedMapMap.entrySet() )
                    {
                        boolean saveCombo = false;

                        for ( DataElementOperand deo : deos )
                        {
                            Date lastUpdated = entry.getValue().get( deo );

                            if ( lastUpdated != null && lastUpdated.after( context.getLastScheduledRun() ) )
                            {
                                saveCombo = true; // True if new/updated data
                                evaluate = true;
                                break;
                            }
                        }

                        if ( !saveCombo )
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
     * @return map of values.
     */
    private Map<String, Double> getExpressionValueMap( Expression expression,
        MapMap<String, DimensionalItemObject, Double> valueMap )
    {
        Map<String, Double> expressionValueMap = new HashMap<>();

        for ( Map.Entry<String, Map<DimensionalItemObject, Double>> entry : valueMap.entrySet() )
        {
            Double value = expressionService.getExpressionValue( expression, entry.getValue(),
                context.getConstantMap(), null, null );

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
     * @param ruleDataElements      data elements configured for the rule
     * @param sourceDataElements    data elements configured for the organisation unit
     * @param allowedPeriodTypes    all the periods in which we might find data values
     * @param period                period in which we are looking for values
     * @param source                organisation unit for which we are looking for values
     * @param lastUpdatedMap        map showing when each data values was last updated
     * @return map of attribute option combo to map of values found.
     */
    private MapMap<String, DimensionalItemObject, Double> getDataValueMap( 
        Set<DataElement> ruleDataElements, Set<DataElement> sourceDataElements,
        Set<PeriodType> allowedPeriodTypes, Period period,
        OrganisationUnit source, MapMap<String, DataElementOperand, Date> lastUpdatedMap )
    {
        Set<DataElement> dataElementsToGet = new HashSet<>( ruleDataElements );
        dataElementsToGet.retainAll( sourceDataElements );

        log.trace( "getDataValueMapRecursive: source:" + source.getName() + " ruleDataElements["
            + ruleDataElements.size() + "] sourceDataElements[" + sourceDataElements.size() + "] elementsToGet["
            + dataElementsToGet.size() + "] allowedPeriodTypes[" + allowedPeriodTypes.size() + "]" );

        return dataValueService.getDataValueMapByAttributeCombo( dataElementsToGet,
            period.getStartDate(), source, allowedPeriodTypes, context.getAttributeCombo(),
            context.getCogDimensionConstraints(), context.getCoDimensionConstraints(), lastUpdatedMap );
    }

    /**
     * Returns aggregated event data for the given parameters.
     * 
     * @param dimensionItems the data dimension items.
     * @param period the period.
     * @param organisationUnit the organisation unit.
     * @return a map mapping of attribute option combo identifier to data element operand
     *         and value.
     */
    private MapMap<String, DimensionalItemObject, Double> getEventMap( Set<DimensionalItemObject> dimensionItems, Period period, OrganisationUnit organisationUnit )
    {
        MapMap<String, DimensionalItemObject, Double> map = new MapMap<>();
        
        if ( dimensionItems.isEmpty() || period == null || organisationUnit == null )
        {
            return map;
        }
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataDimensionItems( Lists.newArrayList( dimensionItems ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .withFilterPeriods( Lists.newArrayList( period ) )
            .withFilterOrganisationUnits( Lists.newArrayList( organisationUnit ) )
            .build();
        
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
