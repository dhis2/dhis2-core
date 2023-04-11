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
package org.hisp.dhis.validation;

import static org.hisp.dhis.antlr.AntlrParserUtils.castDouble;
import static org.hisp.dhis.expression.MissingValueStrategy.NEVER_SKIP;
import static org.hisp.dhis.expression.ParseType.SIMPLE_TEST;
import static org.hisp.dhis.expression.ParseType.VALIDATION_RULE_EXPRESSION;
import static org.hisp.dhis.system.util.MathUtils.addDoubleObjects;
import static org.hisp.dhis.system.util.MathUtils.roundSignificant;
import static org.hisp.dhis.system.util.MathUtils.zeroIfNull;
import static org.hisp.dhis.system.util.ValidationUtils.getObjectValue;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.PersistenceException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemId;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.dataanalysis.ValidationRuleExpressionDetails;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionParams;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.system.util.MathUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Runs a validation task on a thread within a multi-threaded validation run.
 * <p>
 * Each task looks for validation results in a different organisation unit.
 *
 * @author Jim Grace (original)
 * @author Jan Bernitt (reorganisation)
 */
@Slf4j
@Component
@AllArgsConstructor
public class DataValidationRunner
{
    // String that is not an Attribute Option Combo
    private static final String NON_AOC = "";

    private final ExpressionService expressionService;

    private final DataValueService dataValueService;

    @Getter
    private final CategoryService categoryService;

    private final PeriodService periodService;

    @Setter
    private AnalyticsService analyticsService;

    /**
     * Evaluates validation rules for a single organisation unit. This is the
     * central method in validation rule evaluation.
     */
    @Transactional
    public void run( List<OrganisationUnit> orgUnits, ValidationRunContext context )
    {
        try
        {
            runInternal( orgUnits, context );
        }
        catch ( Exception ex )
        {
            log.error( DebugUtils.getStackTrace( ex ) );

            throw ex;
        }
    }

    /**
     * Get the data needed for this task, then evaluate each combination of
     * organisation unit / period / validation rule.
     */
    private void runInternal( List<OrganisationUnit> orgUnits, ValidationRunContext context )
    {
        if ( context.isAnalysisComplete() )
        {
            return;
        }

        for ( PeriodTypeExtended ptx : context.getPeriodTypeXs() )
        {
            for ( Period p : ptx.getPeriods() )
            {
                DataValidationRun run = new DataValidationRun( context, ptx, p );
                run.getData( orgUnits );

                for ( OrganisationUnit ou : orgUnits )
                {
                    for ( ValidationRuleExtended ruleX : ptx.getRuleXs() )
                    {
                        if ( context.isAnalysisComplete() )
                        {
                            return;
                        }
                        // Skip validation if org unit level does not match
                        Set<Integer> levels = ruleX.getOrganisationUnitLevels();
                        if ( levels.isEmpty() || levels.contains( ou.getLevel() ) )
                        {
                            run.addValidationResultsToContext( run.validateRule( ou, ruleX ) );
                        }
                    }
                }
            }
        }
    }

    @Getter
    @RequiredArgsConstructor
    private final class DataValidationRun
    {
        private final ValidationRunContext context;

        // Current period type extended.
        private final PeriodTypeExtended periodTypeX;

        // Current period.
        private final Period period;

        // Data for current period and all rules being evaluated:
        private final MapMapMap<Long, String, DimensionalItemObject, Object> dataMap = new MapMapMap<>();

        private final MapMapMap<Long, String, DimensionalItemObject, Object> slidingWindowDataMap = new MapMapMap<>();

        /**
         * Validates one rule / period by seeing which attribute option combos
         * exist for that data, and then iterating through those attribute
         * option combos.
         */
        private Set<ValidationResult> validateRule( OrganisationUnit orgUnit, ValidationRuleExtended ruleX )
        {
            MapMap<String, DimensionalItemObject, Object> leftValueMap = getValueMap( orgUnit,
                ruleX.getLeftSlidingWindow() );
            MapMap<String, DimensionalItemObject, Object> rightValueMap = getValueMap( orgUnit,
                ruleX.getRightSlidingWindow() );

            Map<String, Double> leftSideValues = getExpressionValueMap( orgUnit, ruleX.getRule().getLeftSide(),
                leftValueMap );
            Map<String, Double> rightSideValues = getExpressionValueMap( orgUnit, ruleX.getRule().getRightSide(),
                rightValueMap );

            Set<String> attributeOptionCombos = Sets.union( leftSideValues.keySet(), rightSideValues.keySet() );

            if ( context.isAnalysisComplete() )
            {
                return Set.of();
            }

            Set<ValidationResult> results = new HashSet<>();
            for ( String optionCombo : attributeOptionCombos )
            {
                if ( NON_AOC.compareTo( optionCombo ) == 0 )
                {
                    continue;
                }

                if ( context.processExpressionDetails() )
                {
                    setExpressionDetails(
                        leftValueMap.get( optionCombo ),
                        rightValueMap.get( optionCombo ) );
                }
                else
                {
                    validateOptionCombo( orgUnit, ruleX.getRule(), optionCombo,
                        leftSideValues.get( optionCombo ),
                        rightSideValues.get( optionCombo ),
                        results::add );
                }
            }
            return results;
        }

        private void setExpressionDetails( Map<DimensionalItemObject, Object> leftSideValues,
            Map<DimensionalItemObject, Object> rightSideValues )
        {
            setExpressionSideDetails( context.getValidationRuleExpressionDetails().getLeftSide(),
                periodTypeX.getLeftSideItemIds(), leftSideValues );

            setExpressionSideDetails( context.getValidationRuleExpressionDetails().getRightSide(),
                periodTypeX.getRightSideItemIds(), rightSideValues );
        }

        private void setExpressionSideDetails( List<Map<String, String>> detailsSide, Set<DimensionalItemId> sideIds,
            Map<DimensionalItemObject, Object> valueMap )
        {
            for ( DimensionalItemId itemId : sideIds )
            {
                DimensionalItemObject itemObject = context.getItemMap().get( itemId );

                Object itemValue = valueMap.get( itemObject );

                String itemValueString = itemValue == null
                    ? null
                    : itemValue.toString();

                ValidationRuleExpressionDetails.addDetailTo( itemObject.getName(), itemValueString, detailsSide );
            }
        }

        /**
         * Validates one rule / period / attribute option combo.
         */
        private void validateOptionCombo( OrganisationUnit orgUnit, ValidationRule rule, String optionCombo,
            Double leftSide, Double rightSide, Consumer<ValidationResult> addResult )
        {
            // Skipping any results we already know
            if ( context.skipValidationOfTuple( orgUnit, rule, period, optionCombo,
                periodService.getDayInPeriod( period, new Date() ) ) )
            {
                return;
            }

            boolean violation = isViolation( rule, leftSide, rightSide );

            if ( violation && !context.isAnalysisComplete() )
            {
                addResult.accept( new ValidationResult(
                    rule, period, orgUnit,
                    getAttributeOptionCombo( optionCombo ),
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
        private boolean isViolation( ValidationRule rule, Double leftSide, Double rightSide )
        {
            Operator operator = rule.getOperator();
            if ( Operator.compulsory_pair == operator )
            {
                return (leftSide == null) != (rightSide == null);
            }

            if ( Operator.exclusive_pair == operator )
            {
                return (leftSide != null) && (rightSide != null);
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

            String test = leftSide
                + operator.getMathematicalOperator()
                + rightSide;
            return !(Boolean) expressionService.getExpressionValue( ExpressionParams.builder()
                .expression( test ).parseType( SIMPLE_TEST ).build() );
        }

        /**
         * Gets the data for this period:
         * <p/>
         * dataMap contains data for non-sliding window expressions.
         * slidingWindowDataMap contains data for sliding window expressions.
         */
        private void getData( List<OrganisationUnit> orgUnits )
        {
            getDataValueMap( orgUnits );

            dataMap.putMap( getAnalyticsMap( orgUnits, true, periodTypeX.getIndicators() ) );

            if ( periodTypeX.areSlidingWindowsNeeded() )
            {
                slidingWindowDataMap.putMap( dataMap );

                slidingWindowDataMap.putMap(
                    getEventMapForSlidingWindow( orgUnits, true, periodTypeX.getEventItems() ) );
                slidingWindowDataMap
                    .putMap( getEventMapForSlidingWindow( orgUnits, false,
                        periodTypeX.getEventItemsWithoutAttributeOptions() ) );
            }

            if ( periodTypeX.areNonSlidingWindowsNeeded() )
            {
                dataMap.putMap( getAnalyticsMap( orgUnits, true, periodTypeX.getEventItems() ) );
                dataMap.putMap(
                    getAnalyticsMap( orgUnits, false, periodTypeX.getEventItemsWithoutAttributeOptions() ) );
            }
        }

        private MapMap<String, DimensionalItemObject, Object> getValueMap( OrganisationUnit orgUnit,
            boolean slidingWindow )
        {
            return slidingWindow
                ? slidingWindowDataMap.get( orgUnit.getId() )
                : dataMap.get( orgUnit.getId() );
        }

        /**
         * Adds any validation results we found to the validation context.
         */
        private void addValidationResultsToContext( Set<ValidationResult> results )
        {
            if ( !results.isEmpty() )
            {
                // the results are collected for this thread first and now
                // copied to the concurrent "global" results list
                context.getValidationResults().addAll( results );
            }
        }

        private Period getPeriod( long id )
        {
            Period p = context.getPeriodIdMap().get( id );

            if ( p == null )
            {
                log.trace( "DataValidationTask calling getPeriod( id " + id + " )" );

                p = periodService.getPeriod( id );

                log.trace( "DataValidationTask called getPeriod( id " + id + " )" );

                context.getPeriodIdMap().put( id, p );
            }

            return p;
        }

        private CategoryOptionCombo getAttributeOptionCombo( long id )
        {
            CategoryOptionCombo aoc = context.getAocIdMap().get( id );

            if ( aoc == null )
            {
                log.trace( "DataValidationTask calling getCategoryOptionCombo( id " + id + " )" );

                aoc = categoryService.getCategoryOptionCombo( id );

                log.trace( "DataValidationTask called getCategoryOptionCombo( id " + id + ")" );

                addToAocCache( aoc );
            }

            return aoc;
        }

        private CategoryOptionCombo getAttributeOptionCombo( String uid )
        {
            CategoryOptionCombo aoc = context.getAocUidMap().get( uid );

            if ( aoc == null )
            {
                log.trace( "DataValidationTask calling getCategoryOptionCombo( uid " + uid + " )" );

                aoc = categoryService.getCategoryOptionCombo( uid );

                log.trace( "DataValidationTask called getCategoryOptionCombo( uid " + uid + ")" );

                addToAocCache( aoc );
            }

            return aoc;
        }

        private void addToAocCache( CategoryOptionCombo aoc )
        {
            context.getAocIdMap().put( aoc.getId(), aoc );
            context.getAocUidMap().put( aoc.getUid(), aoc );
        }

        /**
         * Evaluates an expression, returning a map of values by attribute
         * option combo.
         *
         * @param expression expression to evaluate.
         * @param valueMap Map of value maps, by attribute option combo.
         * @return map of values.
         */
        private Map<String, Double> getExpressionValueMap( OrganisationUnit orgUnit, Expression expression,
            MapMap<String, DimensionalItemObject, Object> valueMap )
        {
            Map<String, Double> expressionValueMap = new HashMap<>();

            if ( valueMap == null )
            {
                return expressionValueMap;
            }

            Map<DimensionalItemObject, Object> nonAocValues = valueMap.get( NON_AOC );

            for ( Map.Entry<String, Map<DimensionalItemObject, Object>> entry : valueMap.entrySet() )
            {
                Map<DimensionalItemObject, Object> values = entry.getValue();

                if ( nonAocValues != null )
                {
                    values.putAll( nonAocValues );
                }

                Double value = castDouble(
                    expressionService.getExpressionValue( context.getBaseExParams().toBuilder()
                        .expression( expression.getExpression() )
                        .parseType( VALIDATION_RULE_EXPRESSION )
                        .valueMap( values )
                        .days( period.getDaysInPeriod() )
                        .missingValueStrategy( expression.getMissingValueStrategy() )
                        .orgUnit( orgUnit )
                        .build() ) );

                if ( MathUtils.isValidDouble( value ) )
                {
                    expressionValueMap.put( entry.getKey(), value );
                }
            }

            return expressionValueMap;
        }

        /**
         * Gets data elements and data element operands from the datavalue
         * table.
         */
        private void getDataValueMap( List<OrganisationUnit> orgUnits )
        {
            DataExportParams params = new DataExportParams();
            params.setDataElements( periodTypeX.getDataElements() );
            params.setDataElementOperands( periodTypeX.getDataElementOperands() );
            params.setIncludedDate( period.getStartDate() );
            params.setOrganisationUnits( new HashSet<>( orgUnits ) );
            params.setPeriodTypes( periodTypeX.getAllowedPeriodTypes() );
            params.setCoDimensionConstraints( context.getCoDimensionConstraints() );
            params.setCogDimensionConstraints( context.getCogDimensionConstraints() );

            if ( context.getAttributeCombo() != null )
            {
                params.setAttributeOptionCombos( Sets.newHashSet( context.getAttributeCombo() ) );
            }

            List<DeflatedDataValue> dataValues = dataValueService.getDeflatedDataValues( params );

            MapMapMap<Long, String, DimensionalItemObject, Long> duplicateCheck = new MapMapMap<>();

            for ( DeflatedDataValue dv : dataValues )
            {
                DataElement dataElement = periodTypeX.getDataElementIdMap().get( dv.getDataElementId() );
                String deoIdKey = periodTypeX.getDeoIds( dv.getDataElementId(), dv.getCategoryOptionComboId() );
                DataElementOperand dataElementOperand = periodTypeX.getDataElementOperandIdMap().get( deoIdKey );
                Period p = getPeriod( dv.getPeriodId() );
                long orgUnitId = dv.getSourceId();
                String attributeOptionComboUid = getAttributeOptionCombo( dv.getAttributeOptionComboId() ).getUid();

                if ( dataElement != null )
                {
                    Object value = getObjectValue( dv.getValue(), dataElement.getValueType() );

                    addValueToDataMap( orgUnitId, attributeOptionComboUid, dataElement, value, p, duplicateCheck );
                }

                if ( dataElementOperand != null )
                {
                    Object value = getObjectValue( dv.getValue(), dataElementOperand.getDataElement().getValueType() );

                    addValueToDataMap( orgUnitId, attributeOptionComboUid, dataElementOperand, value, p,
                        duplicateCheck );
                }
            }
        }

        private void addValueToDataMap( long orgUnitId, String aocUid, DimensionalItemObject dimItemObject,
            Object value, Period p, MapMapMap<Long, String, DimensionalItemObject, Long> duplicateCheck )
        {
            Object existingValue = dataMap.getValue( orgUnitId, aocUid, dimItemObject );

            long periodInterval = p.getEndDate().getTime() - p.getStartDate().getTime();

            Long existingPeriodInterval = duplicateCheck.getValue( orgUnitId, aocUid, dimItemObject );

            if ( existingPeriodInterval != null )
            {
                if ( existingPeriodInterval < periodInterval )
                {
                    return; // Don't overwrite previous value if a shorter
                           // interval
                }
                if ( existingPeriodInterval > periodInterval )
                {
                    existingValue = null; // Overwrite if for a longer interval
                }
            }

            if ( existingValue != null )
            {
                value = addDoubleObjects( value, existingValue );
            }

            dataMap.putEntry( orgUnitId, aocUid, dimItemObject, value );

            duplicateCheck.putEntry( orgUnitId, aocUid, dimItemObject, periodInterval );
        }

        /**
         * Gets analytics data for the given parameters.
         *
         * @param hasAttributeOptions whether the event data has attribute
         *        options.
         */
        private MapMapMap<Long, String, DimensionalItemObject, Object> getAnalyticsMap( List<OrganisationUnit> orgUnits,
            boolean hasAttributeOptions, Set<DimensionalItemObject> analyticsItems )
        {
            if ( analyticsItems.isEmpty() )
            {
                return new MapMapMap<>();
            }

            DataQueryParams.Builder paramsBuilder = DataQueryParams.newBuilder()
                .withDataDimensionItems( Lists.newArrayList( analyticsItems ) )
                .withAttributeOptionCombos( Lists.newArrayList() )
                .withFilterPeriods( Lists.newArrayList( period ) )
                .withOrganisationUnits( orgUnits );

            if ( hasAttributeOptions )
            {
                paramsBuilder.withAttributeOptionCombos( Lists.newArrayList() );
            }

            return getAnalyticsData( orgUnits, paramsBuilder.build(), hasAttributeOptions );
        }

        /**
         * Gets sliding window analytics event data for the given parameters.
         *
         * @param hasAttributeOptions whether the event data has attribute
         *        options.
         */
        private MapMapMap<Long, String, DimensionalItemObject, Object> getEventMapForSlidingWindow(
            List<OrganisationUnit> orgUnits,
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

            // The window size is based on the frequencyOrder of the period's
            // periodType:
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

            return getAnalyticsData( orgUnits, paramsBuilder.build(), hasAttributeOptions );
        }

        /**
         * Gets analytics data.
         *
         * @param params event data query parameters.
         * @param hasAttributeOptions whether the event data has attribute
         *        options.
         * @return event data.
         */
        private MapMapMap<Long, String, DimensionalItemObject, Object> getAnalyticsData(
            List<OrganisationUnit> orgUnits,
            DataQueryParams params, boolean hasAttributeOptions )
        {
            MapMapMap<Long, String, DimensionalItemObject, Object> map = new MapMapMap<>();

            Grid grid;

            try
            {
                grid = analyticsService.getAggregatedDataValues( params );
            }
            catch ( PersistenceException ex ) // No data
            {
                return map;
            }
            catch ( RuntimeException ex ) // Other error
            {
                log.error( DebugUtils.getStackTrace( ex ) );

                return map;
            }

            int dxInx = grid.getIndexOfHeader( DimensionalObject.DATA_X_DIM_ID );
            int ouInx = grid.getIndexOfHeader( DimensionalObject.ORGUNIT_DIM_ID );
            int aoInx = hasAttributeOptions ? grid.getIndexOfHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID )
                : 0;
            int vlInx = grid.getWidth() - 1;

            Map<String, OrganisationUnit> ouLookup = orgUnits.stream()
                .collect( Collectors.toMap( IdentifiableObject::getUid, o -> o ) );
            Map<String, DimensionalItemObject> dxLookup = periodTypeX.getEventItems().stream()
                .collect( Collectors.toMap( DimensionalItemObject::getDimensionItem, d -> d ) );
            dxLookup.putAll( periodTypeX.getIndicators().stream()
                .collect( Collectors.toMap( DimensionalItemObject::getDimensionItem, d -> d ) ) );

            for ( List<Object> row : grid.getRows() )
            {
                String dx = (String) row.get( dxInx );
                String ao = hasAttributeOptions ? (String) row.get( aoInx ) : NON_AOC;
                String ou = (String) row.get( ouInx );
                Object vl = ((Number) row.get( vlInx )).doubleValue();

                OrganisationUnit orgUnit = ouLookup.get( ou );
                DimensionalItemObject analyticsItem = dxLookup.get( dx );

                map.putEntry( orgUnit.getId(), ao, analyticsItem, vl );
            }

            return map;
        }
    }
}
