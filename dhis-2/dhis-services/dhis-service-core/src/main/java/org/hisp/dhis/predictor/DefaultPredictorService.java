package org.hisp.dhis.predictor;

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

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.common.BaseDimensionalItemObject;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.constant.ConstantService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryService;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.expression.Expression;
import org.hisp.dhis.expression.ExpressionService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.period.PeriodType;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by haase on 6/12/16.
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

    // -------------------------------------------------------------------------
    // Predictor
    // -------------------------------------------------------------------------

    @Override
    public int addPredictor( Predictor predictor )
    {
        return predictorStore.save( predictor );
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

    private Set<BaseDimensionalItemObject> getExpressionInputs( String exprString )
    {
        return expressionService.getDataInputsInExpression( exprString );
    }

    @Override
    public Collection<DataValue> getPredictions( Predictor p, Date start, Date end )
    {
        List<OrganisationUnit> sources = new ArrayList<OrganisationUnit>();

        for ( OrganisationUnitLevel level : p.getOrganisationUnitLevels() )
        {
            sources.addAll( organisationUnitService.getOrganisationUnitsAtLevel( level.getLevel() ) );
        }

        Collection<Period> basePeriods = getPeriodsBetween( p.getPeriodType(), start, end );

        return getPredictions( p, sources, basePeriods );
    }

    @Override
    public Collection<DataValue> getPredictions( Predictor p, Collection<OrganisationUnit> sources, Date start, Date end )
    {
        Collection<Period> basePeriods = getPeriodsBetween( p.getPeriodType(), start, end );

        return getPredictions( p, sources, basePeriods );
    }

    @Override
    public Collection<DataValue> getPredictions( Predictor predictor, Collection<OrganisationUnit> sources, Collection<Period> periods )
    {
        // Is end inclusive or exclusive? And what if end is in the middle of a
        // period? Does the period get included?
        List<DataValue> results = new ArrayList<DataValue>();
        Expression generator = predictor.getGenerator();
        Expression skipTest = predictor.getSampleSkipTest();
        DataElement output = predictor.getOutput();
        Set<BaseDimensionalItemObject> datarefs = getExpressionInputs( generator.getExpression() );
        Set<BaseDimensionalItemObject> skiprefs = skipTest == null ? new HashSet<BaseDimensionalItemObject>()
            : getExpressionInputs( skipTest.getExpression() );
        Set<BaseDimensionalItemObject> samplerefs = new HashSet<BaseDimensionalItemObject>();
        Set<String> aggregates = expressionService.getAggregatesInExpression( generator.getExpression() );
        Map<String, Double> constantMap = constantService.getConstantMap();

        ListMap<Period, Period> periodMaps = getSamplePeriods( periods, predictor.getPeriodType(),
            predictor.getSequentialSkipCount(), predictor.getSequentialSampleCount(),
                predictor.getAnnualSampleCount() );

        Set<Period> basePeriods = periodMaps.keySet();
        Set<Period> samplePeriods = periodMaps.uniqueValues();

        for ( String aggregate : aggregates )
        {
            samplerefs.addAll( expressionService.getDataInputsInExpression( aggregate ) );
        }

        for ( OrganisationUnit source : sources )
        {
            MapMap<OrganisationUnit, Period, MapMap<Integer, BaseDimensionalItemObject, Double>> valueMaps = getDataValues(
                datarefs, sourceList( source ), basePeriods );
            Map<Period, MapMap<Integer, BaseDimensionalItemObject, Double>> skipdata = skipTest == null ? null :
                getDataValues( skiprefs, sourceList( source ), samplePeriods ).get( source );

            for ( Period period : basePeriods )
            {
                MapMap<Integer, BaseDimensionalItemObject, Double> valueMap = valueMaps.getValue( source, period );
                
                Map<Integer, ListMap<String, Double>> sampleMap = getSampleMaps( aggregates, samplerefs, source,
                    periodMaps.get( period ), skipTest, skipdata, constantMap );

                Set<Integer> allAoc = new HashSet<Integer>();
                
                if ( valueMap != null )
                {
                    allAoc.addAll( valueMap.keySet() );
                }
                
                if ( sampleMap != null )
                {
                    allAoc.addAll( sampleMap.keySet() );
                }

                if ( allAoc.size() > 0 )
                {
                    for ( Integer aoc : allAoc )
                    {
                        Map<? extends BaseDimensionalItemObject, Double> bindings = valueMap == null ? 
                            emptyBindings() : valueMap.get( aoc );
                        
                        Double value = null;

                        if ( sampleMap == null )
                        {
                            value = expressionService.getExpressionValue( generator, bindings, constantMap, null, 0, null );
                        }
                        else
                        {
                            value = expressionService.getExpressionValue( generator, bindings, constantMap, null, 0, null, sampleMap.get( aoc ) );
                        }

                        if ( value != null && !value.isNaN() && !value.isInfinite() )
                        {
                            DataValue dv = new DataValue( output, period, source,
                                categoryService.getDefaultDataElementCategoryOptionCombo(),
                                categoryService.getDataElementCategoryOptionCombo( aoc ) );

                            dv.setValue( value.toString() );
                            
                            results.add( dv );
                        }
                    }
                }
            }
        }

        return results;
    }

    private Map<? extends BaseDimensionalItemObject, Double> emptyBindings()
    {
        return new HashMap<BaseDimensionalItemObject, Double>();
    }

    private Map<Integer, ListMap<String, Double>> getSampleMaps( Collection<String> aggregateExpressions,
        Set<BaseDimensionalItemObject> samplerefs, OrganisationUnit source, Collection<Period> periods,
        Expression skipTest, Map<Period, MapMap<Integer, BaseDimensionalItemObject, Double>> skipData,
        Map<String, Double> constantMap )
    {
        Map<Integer, ListMap<String, Double>> result = new HashMap<>();
        
        MapMap<OrganisationUnit, Period, MapMap<Integer, BaseDimensionalItemObject, Double>> dataMaps = getDataValues(
            samplerefs, sourceList( source ), periods );
        
        Map<Period, MapMap<Integer, BaseDimensionalItemObject, Double>> dataMap = dataMaps.get( source );

        if ( dataMap != null )
        {
            if ( ( skipTest != null ) && (skipData != null) )
            {
                for ( Period period : periods )
                {
                    MapMap<Integer, BaseDimensionalItemObject, Double> periodData = skipData.get( period );
                    
                    if ( periodData != null )
                    {
                        for ( Integer aoc : periodData.keySet() )
                        {
                            Map<BaseDimensionalItemObject, Double> bindings = periodData.get( aoc );
                            Double testValue = expressionService.getExpressionValue( skipTest, bindings, constantMap, null, 0 );
                            
                            log.debug( "skipTest " + skipTest.getExpression() + " yielded " + testValue );
                            
                            if ( testValue != null && !testValue.equals( 0 ) )
                            {
                                MapMap<Integer, BaseDimensionalItemObject, Double> inPeriod = dataMap.get( period );
                                
                                log.debug( "Removing sample for aoc=" + aoc + " at " + period + " from " + source );
                                inPeriod.remove( aoc );
                            }
                        }
                    }
                }
            }
            
            for ( String aggregate : aggregateExpressions )
            {
                Expression exp = new Expression( aggregate, "aggregated",
                    expressionService.getDataElementsInExpression( aggregate ) );
                
                for ( Period period : periods )
                {
                    MapMap<Integer, BaseDimensionalItemObject, Double> inperiod = dataMap.get( period );
                    
                    if ( inperiod != null )
                    {
                        for ( Integer aoc : inperiod.keySet() )
                        {
                            Double value = expressionService.getExpressionValue( exp, inperiod.get( aoc ), constantMap, null, 0 );
                            
                            ListMap<String, Double> samplemap = result.get( aoc );
                            
                            if ( samplemap == null )
                            {
                                samplemap = new ListMap<String, Double>();
                                result.put( aoc, samplemap );
                            }
                            
                            samplemap.putValue( aggregate, value );
                        }
                    }
                }
            }
        }



        return result;
    }

    private List<DataValue> readDataValues( BaseDimensionalItemObject input, Collection<OrganisationUnit> sources,
        Collection<Period> periods )
    {
        List<DataValue> result;
        
        if ( input instanceof DataElement )
        {
            DataElement de = (DataElement) input;
            result = dataValueService.getRecursiveDeflatedDataValues( de, null, periods, sources );
        }
        else if ( input instanceof DataElementOperand )
        {
            DataElementOperand deo = (DataElementOperand) input;
            result = dataValueService.getRecursiveDeflatedDataValues(
                    deo.getDataElement(), deo.getCategoryOptionCombo(), periods, sources );
        }
        else
        {
            result = new ArrayList<>();
        }
        
        return result;
    }

    private void gatherDataValues( BaseDimensionalItemObject input, Collection<OrganisationUnit> sources,
            Collection<Period> periods, MapMap<OrganisationUnit, Period,
            MapMap<Integer, BaseDimensionalItemObject, Double>> result )
    {
        List<DataValue> values = readDataValues( input, sources, periods );

        for ( DataValue value : values )
        {
            Period pe = value.getPeriod();
            OrganisationUnit source = value.getSource();
            Integer aoc = value.getAttributeOptionCombo().getId();
            MapMap<Integer, BaseDimensionalItemObject, Double> valueMap =
                    result.getValue( source, pe );
            Map<BaseDimensionalItemObject, Double> deoMap =
                    valueMap == null ? null : valueMap.get( aoc );

            if ( valueMap == null )
            {
                Map<Period, MapMap<Integer, BaseDimensionalItemObject, Double>>  periodSubMap = result.get( source );
                
                if ( periodSubMap == null )
                {
                    periodSubMap = new HashMap<Period, MapMap<Integer, BaseDimensionalItemObject, Double>>();
                    result.put( source, periodSubMap );
                }

                valueMap = new MapMap<Integer, BaseDimensionalItemObject, Double>();

                periodSubMap.put( pe, valueMap );
            }
            
            if ( deoMap == null )
            {
                deoMap = new HashMap<BaseDimensionalItemObject, Double>();
                valueMap.put( aoc, deoMap );
            }

            deoMap.put( input, Double.valueOf( value.getValue() ) );
        }
    }

    private MapMap<OrganisationUnit, Period, MapMap<Integer, BaseDimensionalItemObject, Double>> getDataValues(
        Collection<BaseDimensionalItemObject> inputs,
        Collection<OrganisationUnit> sources,
        Collection<Period> periods )
    {
        MapMap<OrganisationUnit, Period, MapMap<Integer, BaseDimensionalItemObject, Double>> result = 
            new MapMap<OrganisationUnit, Period, MapMap<Integer, BaseDimensionalItemObject, Double>>();
        
        for ( BaseDimensionalItemObject input : inputs )
        {
            gatherDataValues( input, sources, periods, result );
        }
        
        return result;
    }

    private ArrayList<OrganisationUnit> sourceList( OrganisationUnit source )
    {
        return Lists.newArrayList( source );
    }

    private Set<Period> getPeriodsBetween( PeriodType ptype, Date first, Date last )
    {
        Set<Period> periods=new HashSet<Period>();

        Period period = ptype.createPeriod( first );

        if ( last == null )
        {
            periods.add( period );
            return periods;
        }

        while ( ( period != null ) && ( !(period.getEndDate().after( last ) ) ) )
        {
            periods.add( period );
            period = ptype.getNextPeriod( period );
        }

        return periods;
    }
    
    private ListMap<Period, Period> getSamplePeriods( Collection<Period> periods, PeriodType ptype,
        int skipCount, int sequentialCount, int annualCount )
    {
        // Is end inclusive or exclusive? And what if end is in the middle of a
        // period? Does the period get included?

        ListMap<Period, Period> results = new ListMap<Period, Period>();

        for ( Period period: periods ) 
        {
            results.put( period, new ArrayList<Period>() );
            
            if ( sequentialCount > 0 )
            {
                Period samplePeriod = ptype.getPreviousPeriod( period );
                int i = 0;
                
                while ( i < skipCount )
                {
                    samplePeriod = ptype.getPreviousPeriod( samplePeriod );
                    i++;
                }

                // We try to get a 'known period' for two reasons:
                //  1. It will have an id (which lets us do direct sql queries against the datavalue table)
                //  2. If there isn't a known period, there won't be any samples!
                Period knownPeriod = getPeriod( samplePeriod );

                while ( i < sequentialCount )
                {
                    if ( knownPeriod != null )
                    {
                        results.putValue( period, knownPeriod );
                    }
                    else
                    {
                        log.debug( "Ignoring unregistered period " + samplePeriod );
                    }
                    
                    samplePeriod = ptype.getPreviousPeriod( samplePeriod );
                    knownPeriod = getPeriod( samplePeriod );
                    i++;
                }
            }

            if ( annualCount > 0 )
            {
                int yearCount = 0;
                Calendar yearlyCalendar = PeriodType.createCalendarInstance( period.getStartDate() );

                // Move to the previous year
                yearlyCalendar.set( Calendar.YEAR, yearlyCalendar.get( Calendar.YEAR ) - 1 );

                while ( yearCount < annualCount )
                {
                    // Defensive copy because createPeriod mutates Calendar
                    Calendar pastYear = PeriodType.createCalendarInstance( yearlyCalendar.getTime() );
                    
                    Period pastPeriod = ptype.createPeriod( pastYear );
                    
                    if ( sequentialCount == 0 )
                    {
                        Period knownPeriod = getPeriod( pastPeriod );
                        
                        if ( knownPeriod != null )
                        {
                            results.putValue( period, knownPeriod );
                        }
                        else
                        {
                            log.debug( "Ignoring unregistered period " + pastPeriod );
                        }
                    }
                    else
                    {
                        Period samplePeriod = pastPeriod;
                        Period knownPeriod = getPeriod( samplePeriod );
                        int j = 0;
                        while ( j < sequentialCount+1 ) // The +1 includes the identical past year period
                        {
                            if ( knownPeriod != null )
                            {
                                results.putValue( period, knownPeriod );
                            }
                            else
                            {
                                log.debug( "Ignoring unregistered period " + samplePeriod );
                            }
                            
                            samplePeriod = ptype.getNextPeriod( samplePeriod );
                            knownPeriod = getPeriod( samplePeriod );
                            j++;
                        }

                        // Reset past year, because createPeriod may have mutated it
                        pastYear = PeriodType.createCalendarInstance( yearlyCalendar.getTime() );

                        j=0; 
                        samplePeriod = ptype.getPreviousPeriod( pastPeriod );
                        knownPeriod = getPeriod( samplePeriod );
                        
                        while ( j < sequentialCount )
                        {
                            if ( knownPeriod != null )
                            {
                                results.putValue( period, knownPeriod );
                            }
                            else
                            {
                                log.debug( "Ignoring unregistered period " + samplePeriod );
                            }
                            
                            samplePeriod = ptype.getPreviousPeriod( samplePeriod );
                            knownPeriod = getPeriod( samplePeriod );
                            j++;
                        }
                    }
                    
                    // Move to the previous year
                    yearlyCalendar.set( Calendar.YEAR, yearlyCalendar.get( Calendar.YEAR ) - 1 );
                    yearCount++;
                }
            }
        }
        return results;
    }

    private Period getPeriod( Period predictor )
    {
        return predictor.getId() != 0 ? predictor : periodService.getPeriod( predictor.getStartDate(), predictor.getEndDate(), predictor.getPeriodType() );
    }

    @Override
    public int predict( Predictor predictor, Date start, Date end )
    {
        Collection<DataValue> values = getPredictions( predictor, start, end );

        for ( DataValue value : values )
        {
            dataValueService.addDataValue( value );
        }

        return values.size();
    }

    @Override
    public int predict( Predictor predictor, Collection<OrganisationUnit> sources, Collection<Period> basePeriods )
    {
        Collection<DataValue> values = getPredictions( predictor, sources, basePeriods );

        for ( DataValue value : values )
        {
            dataValueService.addDataValue( value );
        }

        return values.size();
    }
}
