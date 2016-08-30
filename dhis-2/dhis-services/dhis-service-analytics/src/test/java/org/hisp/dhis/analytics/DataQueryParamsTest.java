package org.hisp.dhis.analytics;

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

import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.period.Period;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author Lars Helge Overland
 */
public class DataQueryParamsTest
    extends DhisConvenienceTest
{
    private IndicatorType it;
    
    private Indicator inA;
    private Indicator inB;
    
    private DataElement deA;
    private DataElement deB;
    private DataElement deC;
    
    private ReportingRate rrA;
    private ReportingRate rrB;
    private ReportingRate rrC;
    private ReportingRate rrD;

    @Before
    public void setUpTest()
    {
        it = createIndicatorType( 'A' );
        
        inA = createIndicator( 'A', it );
        inB = createIndicator( 'A', it );
        
        deA = createDataElement( 'A', new DataElementCategoryCombo() );
        deB = createDataElement( 'B', new DataElementCategoryCombo() );
        deC = createDataElement( 'C', new DataElementCategoryCombo() );
        
        rrA = new ReportingRate( createDataSet( 'A', null ), ReportingRateMetric.REPORTING_RATE );
        rrB = new ReportingRate( createDataSet( 'B', null ), ReportingRateMetric.REPORTING_RATE );
        rrC = new ReportingRate( createDataSet( 'C', null ), ReportingRateMetric.EXPECTED_REPORTS );
        rrD = new ReportingRate( createDataSet( 'D', null ), ReportingRateMetric.ACTUAL_REPORTS );
    }
    
    @Test
    public void testSetGetDataElementsReportingRates()
    {
        List<? extends DimensionalItemObject> dataElements = Lists.newArrayList( deA, deB, deC );
        List<? extends DimensionalItemObject> reportingRates = Lists.newArrayList( rrA, rrB );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( dataElements )
            .withReportingRates( reportingRates ).build();
        
        assertEquals( 3, params.getDataElements().size() );
        assertTrue( params.getDataElements().containsAll( dataElements ) );

        assertEquals( 2, params.getReportingRates().size() );
        assertTrue( params.getReportingRates().containsAll( reportingRates ) );
    }
    
    @Test
    public void testGetDimensionFromParam()
    {
        assertEquals( DATA_X_DIM_ID, DimensionalObjectUtils.getDimensionFromParam( "dx:D348asd782j;kj78HnH6hgT;9ds9dS98s2" ) );
    }
    
    @Test
    public void testGetDimensionItemsFromParam()
    {
        List<String> expected = new ArrayList<>( Lists.newArrayList( "D348asd782j", "kj78HnH6hgT", "9ds9dS98s2" ) );
        
        assertEquals( expected, DimensionalObjectUtils.getDimensionItemsFromParam( "de:D348asd782j;kj78HnH6hgT;9ds9dS98s2" ) );        
    }
    
    @Test
    public void testGetLevelFromLevelParam()
    {
        assertEquals( 4, DimensionalObjectUtils.getLevelFromLevelParam( "LEVEL-4-dFsdfejdf2" ) );
        assertEquals( 0, DimensionalObjectUtils.getLevelFromLevelParam( "LEVEL" ) );
        assertEquals( 0, DimensionalObjectUtils.getLevelFromLevelParam( "LEVEL-gFd" ) );        
    }
        
    @Test
    public void testGetMeasureCriteriaFromParam()
    {
        Map<MeasureFilter, Double> expected = new HashMap<>();
        expected.put( MeasureFilter.GT, 100d );
        expected.put( MeasureFilter.LT, 200d );
        
        assertEquals( expected, DataQueryParams.getMeasureCriteriaFromParam( "GT:100;LT:200" ) );
    }
    
    @Test
    public void testHasPeriods()
    {
        DataQueryParams params = DataQueryParams.newBuilder().build();
        
        assertFalse( params.hasPeriods() );
        
        List<DimensionalItemObject> periods = new ArrayList<>();
        
        params = DataQueryParams.newBuilder( params )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, periods ) ).build();
        
        assertFalse( params.hasPeriods() );
        
        params = DataQueryParams.newBuilder()
            .removeDimension( PERIOD_DIM_ID ).build();

        assertFalse( params.hasPeriods() );
        
        periods.add( new Period() );
        
        params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, periods ) ).build();
        
        assertTrue( params.hasPeriods() );
    }

    @Test
    public void testPruneToDimensionType()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, null, 
                Lists.newArrayList( createIndicator( 'A', null ), createIndicator( 'B', null ) ) ) )
            .addDimension( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, null, null,
                Lists.newArrayList( createOrganisationUnit( 'A' ), createOrganisationUnit( 'B' ) ) ) )
            .addFilter( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, null, null,
                Lists.newArrayList( createPeriod( "201201" ), createPeriod( "201202" ) ) ) ).build();

        assertEquals( 2, params.getDimensions().size() );
        assertEquals( 1, params.getFilters().size() );
        
        params = DataQueryParams.newBuilder( params )
            .pruneToDimensionType( DimensionType.ORGANISATION_UNIT ).build();
        
        assertEquals( 1, params.getDimensions().size() );
        assertEquals( DimensionType.ORGANISATION_UNIT, params.getDimensions().get( 0 ).getDimensionType() );
        assertEquals( 0, params.getFilters().size() );
    }
    
    @Test
    public void testRetainDataDimension()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( inA, inB, deA, deB, deC, rrA, rrB );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .addOrSetDimensionOptions( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, items ).build();
        
        assertEquals( 7, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        
        params = DataQueryParams.newBuilder( params )
            .retainDataDimension( DataDimensionItemType.DATA_ELEMENT ).build();
        
        assertEquals( 3, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deA ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deB ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deC ) );
    }
    
    @Test
    public void testRetainDataDimensions()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( inA, inB, deA, deB, deC, rrA, rrB );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .addOrSetDimensionOptions( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, items ).build();
        
        assertEquals( 7, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        
        params = DataQueryParams.newBuilder( params )
            .retainDataDimensions( DataDimensionItemType.DATA_ELEMENT, DataDimensionItemType.REPORTING_RATE ).build();
        
        assertEquals( 5, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deA ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deB ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deC ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( rrA ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( rrB ) );
    }

    @Test
    public void testRetainDataDimensionReportingRates()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( inA, inB, deA, deB, deC, rrA, rrB, rrC, rrD );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .addOrSetDimensionOptions( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, items ).build();
        
        assertEquals( 9, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        
        params = DataQueryParams.newBuilder( params )
            .retainDataDimensionReportingRates( ReportingRateMetric.REPORTING_RATE ).build();
        
        assertEquals( 2, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( rrA ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( rrB ) );
    }
    
    @Test
    public void testSetDimensionOptions()
    {
        List<DimensionalItemObject> itemsBefore = Lists.newArrayList( createIndicator( 'A', null ), 
            createIndicator( 'B', null ), createIndicator( 'C', null ), createIndicator( 'D', null ) );
        
        List<DimensionalItemObject> itemsAfter = Lists.newArrayList( createIndicator( 'A', null ), createIndicator( 'B', null ) );

        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, null, itemsBefore ) ).build();
        
        assertEquals( itemsBefore, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems() );
        
        params = DataQueryParams.newBuilder( params )
            .withDimensionOptions( DimensionalObject.DATA_X_DIM_ID, itemsAfter ).build();
        
        assertEquals( itemsAfter, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems() );        
    }
}
