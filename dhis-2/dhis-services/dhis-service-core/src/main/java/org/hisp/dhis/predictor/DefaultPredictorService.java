package org.hisp.dhis.predictor;

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
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ListMapMap;
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
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.system.util.MathUtils;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * @author Ken Haase
 * @author Jim Grace
 */
public class DefaultPredictorService
    implements PredictorService
{
    private static final Log log = LogFactory.getLog( DefaultPredictorService.class );

    @Autowired
    private PredictorStore predictorStore;

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
    private CurrentUserService currentUserService;

    public void setCurrentUserService( CurrentUserService currentUserService )
    {
        this.currentUserService = currentUserService;
    }

    // -------------------------------------------------------------------------
    // Predictor CRUD
    // -------------------------------------------------------------------------

    @Override
    public int addPredictor( Predictor predictor )
    {
        predictorStore.save( predictor );
        return predictor.getId();
    }

    @Override
    public void updatePredictor( Predictor predictor )
    {
        predictorStore.update( predictor );
    }

    @Override
    public void deletePredictor( Predictor predictor )
    {
        predictorStore.delete( predictor );
    }

    @Override
    public Predictor getPredictor( int id )
    {
        return predictorStore.get( id );
    }

    @Override
    public Predictor getPredictor( String uid )
    {
        return predictorStore.getByUid( uid );
    }

    @Override
    public List<Predictor> getAllPredictors()
    {
        return predictorStore.getAll();
    }

    @Override
    public List<Predictor> getPredictorsByUid( Collection<String> uids )
    {
        return predictorStore.getByUid( uids );
    }

    @Override
    public List<Predictor> getPredictorsByName( String name )
    {
        return new ArrayList<>( predictorStore.getAllEqName( name ) );
    }

    @Override
    public int getPredictorCount()
    {
        return predictorStore.getCount();
    }

    // -------------------------------------------------------------------------
    // Predictor run
    // -------------------------------------------------------------------------

    @Override
    public int predict( Predictor predictor, Date startDate, Date endDate )
    {
        log.info( "Predicting for " + predictor.getName() + " from " + startDate.toString() + " to " + endDate.toString() );

        Expression generator = predictor.getGenerator();
        Expression skipTest = predictor.getSampleSkipTest();
        DataElement outputDataElement = predictor.getOutput();

        Set<String> aggregates = expressionService.getAggregatesInExpression( generator.getExpression() );
        Map<String, Double> constantMap = constantService.getConstantMap();
        List<Period> outputPeriods = getPeriodsBetweenDates( predictor.getPeriodType(), startDate, endDate );
        ListMap<Period, Period> samplePeriodsMap = getSamplePeriodsMap( outputPeriods, predictor );
        Set<Period> allSamplePeriods = samplePeriodsMap.uniqueValues();
        Set<DataElementOperand> dataElementOperands = getDataElementOperands( aggregates, skipTest );
        User currentUser = currentUserService.getCurrentUser();

        DataElementCategoryOptionCombo outputOptionCombo = predictor.getOutputCombo() == null ?
            categoryService.getDefaultDataElementCategoryOptionCombo() : predictor.getOutputCombo();

        List<OrganisationUnit> orgUnits = organisationUnitService.getOrganisationUnitsAtOrgUnitLevels(
            predictor.getOrganisationUnitLevels(), currentUser.getOrganisationUnits() );

        int predictionCount = 0;

        for ( OrganisationUnit orgUnit : orgUnits )
        {
            MapMapMap<Period, String, DimensionalItemObject, Double> dataMap = dataElementOperands.isEmpty() ? null :
                dataValueService.getDataElementOperandValues( dataElementOperands, allSamplePeriods, orgUnit );

            applySkipTest( dataMap, skipTest, constantMap );

            for ( Period period : outputPeriods )
            {
                ListMapMap<String, String, Double> aggregateSampleMap = getAggregateSamples( dataMap,
                    aggregates, samplePeriodsMap.get( period ), constantMap );

                for ( String aoc : aggregateSampleMap.keySet() )
                {
                    ListMap<String, Double> aggregateValueMap = aggregateSampleMap.get( aoc );

                    Double value = expressionService.getExpressionValue( generator, new HashMap<>(),
                        constantMap,null, period.getDaysInPeriod(), aggregateValueMap );

                    if ( value != null && !value.isNaN() && !value.isInfinite() )
                    {
                        writeDataValue( outputDataElement, period, orgUnit, outputOptionCombo,
                            categoryService.getDataElementCategoryOptionCombo( aoc ),
                            value.toString(), currentUser.getUsername() );

                        predictionCount++;
                    }
                }
            }
        }

        log.info("Generated " + predictionCount + " predictions for " + predictor.getName()
            + " from " + startDate.toString() + " to " + endDate.toString() );

        return predictionCount;
    }

    /**
     * Gets all DataElementOperands from the aggregate expressions and skip test.
     *
     * @param aggregates set of aggregate expressions. These are subexpressions
     *                   which are passed to aggregate functions (such as AVG,
     *                   STDDEV, etc.) which generate vectors of sample values
     *                   rather than a simple, scalar value.
     * @param skipTest the skip test expression.
     * @return set of all DataElementOperands found in all expressions.
     */
    private Set<DataElementOperand> getDataElementOperands( Set<String> aggregates, Expression skipTest )
    {
        Set<DataElementOperand> operands = new HashSet<DataElementOperand>();

        for ( String aggregate : aggregates )
        {
            operands.addAll( expressionService.getOperandsInExpression( aggregate ) );
        }

        if ( skipTest != null )
        {
            operands.addAll( expressionService.getOperandsInExpression( skipTest.getExpression() ) );
        }

        return operands;
    }

    /**
     * For a given predictor, orgUnit, and outputPeriod, returns for each
     * attribute option combo and aggregate expression a list of values for
     * the various sample periods.
     *
     * If there are no aggregate expressions, then the prediction is a constant.
     * If this is the case, then return the default attribute option combo
     * into which any constant prediction value will be written.
     *
     * @param dataMap data to be used in evaluating expressions.
     * @param aggregates the aggregate expressions.
     * @param samplePeriods the periods to sample from.
     * @param constantMap any constants used in evaluating expressions.
     * @return lists of sample values by attributeOptionCombo and expression
     */
    private ListMapMap<String, String, Double> getAggregateSamples (
        MapMapMap<Period, String, DimensionalItemObject, Double> dataMap,
        Collection<String> aggregates, List<Period> samplePeriods,
        Map<String, Double> constantMap )
    {
        ListMapMap<String, String, Double> result = new ListMapMap<>();

        if ( aggregates.isEmpty() )
        {
            result.put( categoryService.getDefaultDataElementCategoryOptionCombo().getUid(), new ListMap<>() );
        }
        else if ( dataMap != null )
        {
            for ( String aggregate : aggregates )
            {
                Expression expression = new Expression( aggregate, "Aggregated" );

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
