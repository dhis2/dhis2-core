package org.hisp.dhis.predictor;

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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.DimensionItemType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ListMapMap;
import org.hisp.dhis.common.Map4;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.expression.MissingValueStrategy;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.AnalyticsType;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;
import static org.hisp.dhis.common.IdentifiableObjectUtils.getUids;
import static org.hisp.dhis.system.util.ValidationUtils.dataValueIsZeroAndInsignificant;

/**
 * @author Ken Haase
 * @author Jim Grace
 */
@Transactional
public class DefaultPredictionService
    implements PredictionService
{
    private static final Log log = LogFactory.getLog( DefaultPredictionService.class );

    @Autowired
    private PredictorService predictorService;

    @Autowired
    private ConstantService constantService;

    @Autowired
    private ExpressionService expressionService;

    @Autowired
    private DataValueService dataValueService;

    @Autowired
    private DataElementCategoryService categoryService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private PeriodService periodService;

    @Autowired
    private AnalyticsService analyticsService;

    public void setAnalyticsService( AnalyticsService analyticsService )
    {
        this.analyticsService = analyticsService;
    }

    @Autowired
    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // Prediction business logic
    // -------------------------------------------------------------------------

    public final static String NON_AOC = ""; // String that is not an Attribute Option Combo

    public int predictPredictors( List<String> predictors, Date startDate, Date endDate )
    {
        int totalCount = 0;

        if ( CollectionUtils.isEmpty( predictors ) )
        {
            predictors = getUids( predictorService.getAllPredictors() );
        }

        for ( String uid : predictors) {
            Predictor predictor = predictorService.getPredictor( uid );

            int count = predict( predictor, startDate, endDate );

            log.info( "Generated " + count + " predictions" );
            totalCount += count;
        }

        return totalCount;
    }

    @Override
    public int predict( Predictor predictor, Date startDate, Date endDate )
    {
        log.info( "Predicting for " + predictor.getName() + " from " + startDate.toString() + " to " + endDate.toString() );

        Expression generator = predictor.getGenerator();
        Expression skipTest = predictor.getSampleSkipTest();
        DataElement outputDataElement = predictor.getOutput();

        Set<String> aggregates = new HashSet<>();
        Set<String> nonAggregates = new HashSet<>();
        expressionService.getAggregatesAndNonAggregatesInExpression( generator.getExpression(), aggregates, nonAggregates );
        Map<String, Double> constantMap = constantService.getConstantMap();
        List<Period> outputPeriods = getPeriodsBetweenDates( predictor.getPeriodType(), startDate, endDate );
        Set<Period> existingOutputPeriods = getExistingPeriods( outputPeriods );
        ListMap<Period, Period> samplePeriodsMap = getSamplePeriodsMap( outputPeriods, predictor );
        Set<Period> allSamplePeriods = samplePeriodsMap.uniqueValues();
        Set<DimensionalItemObject> aggregateDimensionItems = getDimensionItems( aggregates, skipTest );
        Set<DimensionalItemObject> nonAggregateDimensionItems = getDimensionItems( nonAggregates, null );
        User currentUser = currentUserService.getCurrentUser();
        Set<String> defaultOptionComboAsSet = Sets.newHashSet( categoryService.getDefaultDataElementCategoryOptionCombo().getUid() );
        Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> emptyMap4 = new Map4<>();
        MapMapMap<Period, String, DimensionalItemObject, Double> emptyMapMapMap = new MapMapMap<>();
        boolean usingAttributeOptions = hasAttributeOptions( aggregateDimensionItems ) || hasAttributeOptions( nonAggregateDimensionItems );

        DataElementCategoryOptionCombo outputOptionCombo = predictor.getOutputCombo() == null ?
            categoryService.getDefaultDataElementCategoryOptionCombo() : predictor.getOutputCombo();

        int predictionCount = 0;

        Set<OrganisationUnit> currentUserOrgUnits = new HashSet<>();
        String currentUsername = "system-process";

        if ( currentUser != null )
        {
            currentUserOrgUnits = currentUser.getOrganisationUnits();
            currentUsername = currentUser.getUsername();
        }

        for ( OrganisationUnitLevel orgUnitLevel : predictor.getOrganisationUnitLevels() )
        {
            List<OrganisationUnit> orgUnitsAtLevel = organisationUnitService.getOrganisationUnitsAtOrgUnitLevels(
                Lists.newArrayList( orgUnitLevel ), currentUserOrgUnits );

            if ( orgUnitsAtLevel.size() == 0 )
            {
                continue;
            }

            List<List<OrganisationUnit>> orgUnitLists = Lists.partition(orgUnitsAtLevel, 500);

            for ( List<OrganisationUnit> orgUnits : orgUnitLists )
            {
                Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> aggregateDataMap4 =
                    aggregateDimensionItems.isEmpty() ? emptyMap4 :
                        getDataValues( aggregateDimensionItems, allSamplePeriods, orgUnits );

                Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> nonAggregateDataMap4 =
                    nonAggregateDimensionItems.isEmpty() ? emptyMap4 :
                        getDataValues( nonAggregateDimensionItems, existingOutputPeriods, orgUnits );

                for ( OrganisationUnit orgUnit : orgUnits )
                {
                    MapMapMap<Period, String, DimensionalItemObject, Double> aggregateDataMap = aggregateDataMap4.get( orgUnit );

                    MapMapMap<Period, String, DimensionalItemObject, Double> nonAggregateDataMap =
                        firstNonNull( nonAggregateDataMap4.get( orgUnit ), emptyMapMapMap );

                    applySkipTest( aggregateDataMap, skipTest, constantMap );

                    for ( Period period : outputPeriods )
                    {
                        ListMapMap<String, String, Double> aggregateSampleMap = getAggregateSamples( aggregateDataMap,
                            aggregates, samplePeriodsMap.get( period ), constantMap, generator.getMissingValueStrategy() );

                        MapMap<String, DimensionalItemObject, Double> nonAggregateSampleMap = firstNonNull(
                            nonAggregateDataMap.get( period ), new MapMap<>() );

                        Set<String> attributeOptionCombos = usingAttributeOptions ?
                            Sets.union( aggregateSampleMap.keySet(), nonAggregateSampleMap.keySet() ) : defaultOptionComboAsSet;

                        if ( attributeOptionCombos.isEmpty() && generator.getMissingValueStrategy() == MissingValueStrategy.NEVER_SKIP )
                        {
                            attributeOptionCombos = defaultOptionComboAsSet;
                        }

                        ListMap<String, Double> aggregateSampleMapNonAoc = aggregateSampleMap.get( NON_AOC );

                        Map<DimensionalItemObject, Double> nonAggregateSampleMapNonAoc = nonAggregateSampleMap.get( NON_AOC );

                        for ( String aoc : attributeOptionCombos )
                        {
                            if ( NON_AOC.compareTo( aoc ) == 0 )
                            {
                                continue;
                            }

                            ListMap<String, Double> aggregateValueMap = ListMap.union( aggregateSampleMap.get( aoc ), aggregateSampleMapNonAoc );

                            Map<DimensionalItemObject, Double> nonAggregateValueMap = combine( nonAggregateSampleMap.get( aoc ), nonAggregateSampleMapNonAoc );

                            Double value = expressionService.getExpressionValue( generator, nonAggregateValueMap,
                                constantMap, null, period.getDaysInPeriod(), aggregateValueMap );

                            if ( value != null && !value.isNaN() && !value.isInfinite() &&
                                !dataValueIsZeroAndInsignificant( Double.toString( value ), outputDataElement ) )
                            {
                                String valueString = outputDataElement.getValueType().isInteger() ?
                                    Long.toString( Math.round( value ) ) :
                                    Double.toString( MathUtils.roundFraction( value, 4 ) );

                                writeDataValue( outputDataElement, period, orgUnit, outputOptionCombo,
                                    categoryService.getDataElementCategoryOptionCombo( aoc ),
                                    valueString, currentUsername );

                                predictionCount++;
                            }
                        }
                    }
                }
            }
        }

        log.info("Generated " + predictionCount + " predictions for " + predictor.getName()
            + " from " + startDate.toString() + " to " + endDate.toString() );

        return predictionCount;
    }

    private Map<DimensionalItemObject, Double> combine ( Map<DimensionalItemObject, Double> a, Map<DimensionalItemObject, Double> b )
    {
        if ( a == null || a.isEmpty() )
        {
            if ( b == null || b.isEmpty() )
            {
                return new HashMap<>();
            }
            else
            {
                return b;
            }
        }
        else if ( b == null || b.isEmpty() )
        {
            return a;
        }

        Map<DimensionalItemObject, Double> c = new HashMap<>( a );

        for (Map.Entry<DimensionalItemObject, Double> entry : b.entrySet() )
        {
            c.put( (DimensionalItemObject)entry.getKey(), entry.getValue() );
        }

        return c;
    }

    /**
     * Gets all DimensionalItemObjects from the expressions and skip test.
     *
     * @param expressions set of expressions.
     * @param skipTest the skip test expression (if any).
     * @return set of all dimensional item objects found in all expressions.
     */
    private Set<DimensionalItemObject> getDimensionItems( Set<String> expressions, Expression skipTest )
    {
        Set<DimensionalItemObject> operands = new HashSet<>();

        for ( String expression : expressions )
        {
            operands.addAll( expressionService.getDimensionalItemObjectsInExpression( expression ) );
        }

        if ( skipTest != null )
        {
            operands.addAll( expressionService.getDimensionalItemObjectsInExpression( skipTest.getExpression() ) );
        }

        return operands;
    }

    /**
     * Checks to see if any dimensional item objects in a set have values
     * stored in the database by attribute option combo.
     *
     * @param oSet set of dimensional item objects
     * @return true if any are stored by attribuete option combo.
     */
    private boolean hasAttributeOptions( Set<DimensionalItemObject> oSet )
    {
        for ( DimensionalItemObject o : oSet )
        {
            if ( hasAttributeOptions( o ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if a dimensional item object has values
     * stored in the database by attribute option combo.
     *
     * @param o dimensional item object
     * @return true if values are stored by attribuete option combo.
     */
    private boolean hasAttributeOptions( DimensionalItemObject o )
    {
        return o.getDimensionItemType() != DimensionItemType.PROGRAM_INDICATOR
            || ( (ProgramIndicator)o ).getAnalyticsType() != AnalyticsType.ENROLLMENT;
    }

    /**
     * For a given predictor, orgUnit, and outputPeriod, returns for each
     * attribute option combo and aggregate expression a list of values for
     * the various sample periods.
     *
     * @param dataMap data to be used in evaluating expressions.
     * @param aggregates the aggregate expressions.
     * @param samplePeriods the periods to sample from.
     * @param constantMap any constants used in evaluating expressions.
     * @param missingValueStrategy strategy for sampled period missing values.
     * @return lists of sample values by attributeOptionCombo and expression
     */
    private ListMapMap<String, String, Double> getAggregateSamples (
        MapMapMap<Period, String, DimensionalItemObject, Double> dataMap,
        Collection<String> aggregates, List<Period> samplePeriods,
        Map<String, Double> constantMap, MissingValueStrategy missingValueStrategy )
    {
        ListMapMap<String, String, Double> result = new ListMapMap<>();

        if ( dataMap != null )
        {
            for ( String aggregate : aggregates )
            {
                Expression expression = new Expression( aggregate, "Aggregated", missingValueStrategy );

                for ( Period period : samplePeriods )
                {
                    MapMap<String, DimensionalItemObject, Double> periodValues = dataMap.get( period );

                    if ( periodValues != null )
                    {
                        for ( String aoc : periodValues.keySet() )
                        {
                            Double value = expressionService.getExpressionValue( expression,
                                periodValues.get( aoc ), constantMap, null, period.getDaysInPeriod() );

                            result.putValue( aoc, aggregate, value );
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Evaluates the skip test expression for any sample periods in which
     * skip test data occurs. For any combination of period and attribute
     * option combo where the skip test is true, removes all sample data with
     * that combination of period and attribute option combo.
     *
     * @param dataMap all data values (both skip and aggregate).
     * @param skipTest the skip test expression.
     * @param constantMap constants to use in skip expression if needed.
     */
    private void applySkipTest( MapMapMap<Period, String, DimensionalItemObject, Double> dataMap,
        Expression skipTest, Map<String, Double> constantMap )
    {
        if ( skipTest != null && dataMap != null )
        {
            for ( Period period : dataMap.keySet() )
            {
                MapMap<String, DimensionalItemObject, Double> periodData = dataMap.get( period );

                for ( String aoc : periodData.keySet() )
                {
                    Double testValue = expressionService.getExpressionValue( skipTest, periodData.get( aoc ),
                        constantMap, null, period.getDaysInPeriod() );

                    if ( testValue != null && !MathUtils.isZero( testValue ) )
                    {
                        periodData.remove( aoc );
                    }
                }
            }
        }
    }

    /**
     * Returns all Periods of the specified PeriodType with start date after or
     * equal the specified start date and end date before or equal the specified
     * end date.
     *
     * The periods returned do not need to be in the database.
     *
     * @param periodType the PeriodType.
     * @param startDate the ultimate start date.
     * @param endDate the ultimate end date.
     * @return a list of all Periods with start date after or equal the
     *         specified start date and end date before or equal the specified
     *         end date, or an empty list if no Periods match.
     */
    private List<Period> getPeriodsBetweenDates( PeriodType periodType, Date startDate, Date endDate )
    {
        List<Period> periods = new ArrayList<Period>();

        Period period = periodType.createPeriod( startDate );

        if ( !period.getStartDate().before( startDate ) && !period.getEndDate().after( endDate ) )
        {
            periods.add( period );
        }

        period = periodType.getNextPeriod( period );

        while ( !period.getEndDate().after( endDate ) )
        {
            periods.add( period );
            period = periodType.getNextPeriod( period );
        }

        return periods;
    }

    /**
     * Creates a map relating each output period to a list of sample periods
     * from which the sample data is to be drawn.
     *
     * @param outputPeriods the output periods
     * @param predictor the predictor
     * @return map from output periods to sample periods
     */
    private ListMap<Period, Period> getSamplePeriodsMap( List<Period> outputPeriods, Predictor predictor)
    {
        int sequentialCount = predictor.getSequentialSampleCount();
        int annualCount = predictor.getAnnualSampleCount();
        int skipCount = firstNonNull( predictor.getSequentialSkipCount(),  0 );
        PeriodType periodType = predictor.getPeriodType();

        ListMap<Period, Period> samplePeriodsMap = new ListMap<Period, Period>();

        for ( Period outputPeriod : outputPeriods )
        {
            samplePeriodsMap.put( outputPeriod, new ArrayList<Period>() );

            Period p = periodType.getPreviousPeriod( outputPeriod, skipCount );

            for ( int i = skipCount; i < sequentialCount; i++ )
            {
                p = periodType.getPreviousPeriod( p );

                addPeriod( samplePeriodsMap, outputPeriod, p );
            }

            for ( int year = 1; year <= annualCount; year++ )
            {
                Period pPrev = periodType.getPreviousYearsPeriod( outputPeriod, year );
                Period pNext = pPrev;

                addPeriod( samplePeriodsMap, outputPeriod, pPrev );

                for ( int i = 0; i < sequentialCount; i++ )
                {
                    pPrev = periodType.getPreviousPeriod( pPrev );
                    pNext = periodType.getNextPeriod( pNext );

                    addPeriod( samplePeriodsMap, outputPeriod, pPrev );
                    addPeriod( samplePeriodsMap, outputPeriod, pNext );
                }
            }
        }
        return samplePeriodsMap;
    }

    /**
     * Adds a period to the sample period map, for the given output period.
     *
     * Only adds the period if it is found in the database, because:
     * (a) We will need the period id, and
     * (b) If the period does not exist in the database, then
     *     there is no data in the database to look for.
     *
     * @param samplePeriodsMap the sample period map to add to
     * @param outputPeriod the output period for which we are adding the sample
     * @param samplePeriod the sample period to add
     */
    private void addPeriod( ListMap<Period, Period> samplePeriodsMap, Period outputPeriod, Period samplePeriod )
    {
        Period foundPeriod = samplePeriod.getId() != 0 ? samplePeriod :
            periodService.getPeriod( samplePeriod.getStartDate(), samplePeriod.getEndDate(), samplePeriod.getPeriodType() );

        if ( foundPeriod != null )
        {
            samplePeriodsMap.putValue( outputPeriod, foundPeriod );
        }
    }

    /**
     * Finds the set of periods that exist, from a list of periods.
     *
     * Only adds the period if it is found in the database, because:
     * (a) We will need the period id, and
     * (b) If the period does not exist in the database, then
     *     there is no data in the database to look for.
     *
     * @param periods the periods to look for
     * @return the set of periods that exist, with ids.
     */
    private Set<Period> getExistingPeriods( List<Period> periods )
    {
        Set<Period> existingPeriods = new HashSet<>();

        for ( Period period : periods )
        {
            Period existingPeriod = period.getId() != 0 ? period :
                periodService.getPeriod( period.getStartDate(), period.getEndDate(), period.getPeriodType() );

            if ( existingPeriod != null )
            {
                existingPeriods.add( existingPeriod );
            }
        }
        return existingPeriods;
    }

    /**
     * Gets data values for a set of DimensionalItemObjects over a set of
     * Periods for an organisation unit and/or any of the organisation unit's
     * descendants.
     *
     * DimensionalItemObjects may reference aggregate and/or event data.
     *
     * Returns the values mapped by Period, then attribute option combo UID,
     * then DimensionalItemObject.
     *
     * @param dimensionItems the dimensionItems.
     * @param periods the Periods of the DataValues.
     * @param orgUnits the roots of the OrganisationUnit trees to include.
     * @return the map of values
     */
    private Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> getDataValues(
        Set<DimensionalItemObject> dimensionItems, Set<Period> periods, List<OrganisationUnit> orgUnits)
    {
        Set<DataElementOperand> dataElementOperands = new HashSet<>();
        Set<DimensionalItemObject> eventAttributeOptionObjects = new HashSet<>();
        Set<DimensionalItemObject> eventNonAttributeOptionObjects = new HashSet<>();
        Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> dataValues = new Map4<>();

        for ( DimensionalItemObject o : dimensionItems )
        {
            if ( o instanceof DataElementOperand )
            {
                dataElementOperands.add( (DataElementOperand) o );
            }
            else if ( o instanceof DataElement )
            {
                dataElementOperands.add( new DataElementOperand( (DataElement) o ) );
            }
            else if ( hasAttributeOptions( o ) )
            {
                eventAttributeOptionObjects.add( o );
            }
            else
            {
                eventNonAttributeOptionObjects.add( o );
            }
        }

        if ( !dataElementOperands.isEmpty() )
        {
            dataValues = dataValueService.getDataElementOperandValues( dataElementOperands, periods, orgUnits );
        }

        if ( !eventAttributeOptionObjects.isEmpty() )
        {
            dataValues.putMap( getEventDataValues( eventAttributeOptionObjects, true, periods, orgUnits ) );
        }

        if ( !eventNonAttributeOptionObjects.isEmpty() )
        {
            dataValues.putMap( getEventDataValues( eventNonAttributeOptionObjects, false, periods, orgUnits ) );
        }

        return dataValues;
    }

    /**
     * Gets data values for a set of Event dimensionItems over a set of
     * Periods for a list of organisation units and/or any of the organisation
     * units' descendants.
     *
     * Returns the values mapped by OrganisationUnit, Period, attribute option
     * combo UID, and DimensionalItemObject.
     *
     * @param dimensionItems the dimensionItems.
     * @param periods the Periods of the DataValues.
     * @param orgUnits the roots of the OrganisationUnit trees to include.
     * @return the map of values
     */
    private Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> getEventDataValues(
        Set<DimensionalItemObject> dimensionItems, boolean hasAttributeOptions, Set<Period> periods, List<OrganisationUnit> orgUnits)
    {
        Map4<OrganisationUnit, Period, String, DimensionalItemObject, Double> eventDataValues = new Map4<>();

        DataQueryParams.Builder paramsBuilder = DataQueryParams.newBuilder()
            .withPeriods( new ArrayList<Period>( periods ) )
            .withDataDimensionItems( Lists.newArrayList( dimensionItems ) )
            .withOrganisationUnits( orgUnits );

        if ( hasAttributeOptions )
        {
            paramsBuilder.withAttributeOptionCombos( Lists.newArrayList() );
        }

        Grid grid = analyticsService.getAggregatedDataValues( paramsBuilder.build() );

        int peInx = grid.getIndexOfHeader( DimensionalObject.PERIOD_DIM_ID );
        int dxInx = grid.getIndexOfHeader( DimensionalObject.DATA_X_DIM_ID );
        int ouInx = grid.getIndexOfHeader( DimensionalObject.ORGUNIT_DIM_ID );
        int aoInx = hasAttributeOptions ? grid.getIndexOfHeader( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID ) : 0;
        int vlInx = grid.getWidth() - 1;

        Map<String, Period> periodLookup = periods.stream().collect( Collectors.toMap( p -> p.getIsoDate(), p -> p ) );
        Map<String, DimensionalItemObject> dimensionItemLookup = dimensionItems.stream().collect( Collectors.toMap( d -> d.getDimensionItem(), d -> d ) );
        Map<String, OrganisationUnit> orgUnitLookup = orgUnits.stream().collect( Collectors.toMap( o -> o.getUid(), o -> o ) );

        for ( List<Object> row : grid.getRows() )
        {
            String pe = (String) row.get( peInx );
            String dx = (String) row.get( dxInx );
            String ou = (String) row.get( ouInx );
            String ao = hasAttributeOptions ? (String) row.get( aoInx ) : NON_AOC;
            Double vl = (Double) row.get( vlInx );

            Period period = periodLookup.get( pe );
            DimensionalItemObject dimensionItem = dimensionItemLookup.get( dx );
            OrganisationUnit orgUnit = orgUnitLookup.get( ou );

            eventDataValues.putEntry( orgUnit, period, ao, dimensionItem, vl );
        }

        return eventDataValues;
    }

    /**
     * Writes (adds or updates) a predicted data value to the database.
     *
     * @param dataElement the data element.
     * @param period the period.
     * @param orgUnit the organisation unit.
     * @param categoryOptionCombo the category option combo.
     * @param attributeOptionCombo the attribute option combo.
     * @param value the value.
     * @param storedBy the user that will store this data value.
     */
    private void writeDataValue( DataElement dataElement, Period period,
        OrganisationUnit orgUnit, DataElementCategoryOptionCombo categoryOptionCombo,
        DataElementCategoryOptionCombo attributeOptionCombo, String value, String storedBy )
    {
        DataValue existingValue = dataValueService.getDataValue( dataElement, period,
            orgUnit, categoryOptionCombo, attributeOptionCombo );

        if ( existingValue != null )
        {
            existingValue.setValue( value );
            existingValue.setStoredBy( storedBy );

            dataValueService.updateDataValue( existingValue );
        }
        else
        {
            DataValue dv = new DataValue( dataElement, period, orgUnit,
                categoryOptionCombo, attributeOptionCombo, value, storedBy, null, null );

            dataValueService.addDataValue( dv );
        }
    }
}
