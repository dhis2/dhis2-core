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

    private void runInternal()
    {
        Set<ValidationResult> validationResults = new HashSet<>();

        if ( !context.isAnalysisComplete() )
        {
            for ( PeriodTypeExtended periodTypeX : context.getPeriodTypeExtendedMap().values() )
            {
                log.trace("Validation PeriodType " + periodTypeX.getPeriodType().getName() );

                Set<ValidationRuleExtended> ruleXs = getRulesBySourceAndPeriodType( orgUnit, periodTypeX );

                SetMap<String, DataElementOperand> dataElementOperandsToGet = getDataElementOperands( ruleXs );

                if ( !ruleXs.isEmpty() )
                {
                    for ( Period period : periodTypeX.getPeriods() )
                    {
                        MapMap<String, DataElementOperand, Date> lastUpdatedMap = new MapMap<>();

                        MapMap<String, DimensionalItemObject, Double> dataValueMap = getDataValueMap(
                            dataElementOperandsToGet, periodTypeX.getAllowedPeriodTypes(),
                            period, orgUnit, lastUpdatedMap );

                        MapMap<String, DimensionalItemObject, Double> slidingWindowEventMap = getEventMapForSlidingWindow(
                            context.getEventItems(), period, orgUnit );

                        slidingWindowEventMap.putMap( dataValueMap );

                        MapMap<String, DimensionalItemObject, Double> eventMap = getEventMap(
                            context.getEventItems(), period, orgUnit );

                        dataValueMap.putMap( eventMap );

                        log.trace( "OrgUnit " + orgUnit.getName() + " [" + period.getStartDate() + " - "
                            + period.getEndDate() + "]" + " currentValueMap[" + dataValueMap.size() + "]" );

                        for ( ValidationRuleExtended ruleX : ruleXs )
                        {
                            ValidationRule rule = ruleX.getRule();

                            log.trace("Validation rule " + rule.getUid() + " " + rule.getName() );

                            Map<String, Double> leftSideValues;

                            if ( rule.getLeftSide() != null && rule.getLeftSide().getSlidingWindow() )
                            {
                                leftSideValues = getExpressionValueMap( rule.getLeftSide(), slidingWindowEventMap,
                                    period );
                            }
                            else
                            {
                                leftSideValues = getExpressionValueMap( rule.getLeftSide(), dataValueMap, period );
                            }

                            Map<String, Double> rightSideValues;

                            if ( rule.getRightSide() != null && rule.getRightSide().getSlidingWindow() )
                            {
                                rightSideValues = getExpressionValueMap( rule.getRightSide(), slidingWindowEventMap,
                                    period );
                            }
                            else
                            {
                                rightSideValues = getExpressionValueMap( rule.getRightSide(), dataValueMap, period );
                            }

                            Set<String> attributeOptionCombos = Sets.newHashSet( leftSideValues.keySet() );
                            attributeOptionCombos.addAll( rightSideValues.keySet() );

                            for ( String optionCombo : attributeOptionCombos )
                            {
                                log.trace("Validation attributeOptionCombo " + optionCombo );

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
                                else
                                {
                                    if ( leftSide == null &&
                                        rule.getLeftSide().getMissingValueStrategy() == NEVER_SKIP )
                                    {
                                        leftSide = 0d;
                                    }

                                    if ( rightSide == null &&
                                        rule.getRightSide().getMissingValueStrategy() == NEVER_SKIP )
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
                        }
                    }
                }
            }
        }

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
     * @param period     Period for evaluating the expression.
     * @return map of values.
     */
    private Map<String, Double> getExpressionValueMap( Expression expression,
        MapMap<String, DimensionalItemObject, Double> valueMap, Period period )
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
     * @param period                   period in which we are looking for values
     * @param orgUnit                  organisation unit for which we are looking for values
     * @param lastUpdatedMap           map showing when each data value was last updated
     * @return map of attribute option combo to map of values found.
     */
    private MapMap<String, DimensionalItemObject, Double> getDataValueMap(
        SetMap<String, DataElementOperand> dataElementOperandsToGet,
        Set<PeriodType> allowedPeriodTypes, Period period,
        OrganisationUnit orgUnit, MapMap<String, DataElementOperand, Date> lastUpdatedMap )
    {
        log.trace( "getDataValueMap: orgUnit:" + orgUnit.getName()
            + " dataElementOperandsToGet[" + dataElementOperandsToGet.size()
            + "] allowedPeriodTypes[" + allowedPeriodTypes.size() + "]" );

        MapMap<String, DimensionalItemObject, Double> map = dataValueService.getDataValueMapByAttributeCombo(
            dataElementOperandsToGet, period.getStartDate(), orgUnit, allowedPeriodTypes, context.getAttributeCombo(),
            context.getCogDimensionConstraints(), context.getCoDimensionConstraints(), lastUpdatedMap );

        return map;
    }

    /**
     * Returns aggregated event data for the given parameters.
     *
     * @param dimensionItems   the data dimension items.
     * @param period           the period.
     * @param organisationUnit the organisation unit.
     * @return a map mapping of attribute option combo identifier to data element operand
     * and value.
     */
    private MapMap<String, DimensionalItemObject, Double> getEventMap( Set<DimensionalItemObject> dimensionItems,
        Period period, OrganisationUnit organisationUnit )
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

        return getEventData( params );
    }

    private MapMap<String, DimensionalItemObject, Double> getEventMapForSlidingWindow(
        Set<DimensionalItemObject> dimensionItems,
        Period period, OrganisationUnit organisationUnit )
    {
        MapMap<String, DimensionalItemObject, Double> map = new MapMap<>();

        if ( dimensionItems.isEmpty() || period == null || organisationUnit == null )
        {
            return map;
        }

        // We want to position the sliding window over the most recent data. To achieve this, we need to satisfy the
        // following criteria:
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
            .withDataDimensionItems( Lists.newArrayList( dimensionItems ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .withStartDate( startDate.getTime() )
            .withEndDate( endDate.getTime() )
            .withFilterOrganisationUnits( Lists.newArrayList( organisationUnit ) )
            .build();

        return getEventData( params );

    }

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
