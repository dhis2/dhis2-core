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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.*;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementCategoryCombo;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.hisp.dhis.common.DimensionalObject.*;
import static org.junit.Assert.*;

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
    
    private Period peA;
    private Period peB;
    
    private OrganisationUnit ouA;
    private OrganisationUnit ouB;

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
        
        peA = createPeriod( "201601" );
        peB = createPeriod( "201603" );
        
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
    }

    @Test
    public void addDimension()
    {
        DimensionalObject doA = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT, Lists.newArrayList() );
        DimensionalObject doB = new BaseDimensionalObject( DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID, DimensionType.CATEGORY_OPTION_COMBO, Lists.newArrayList() );
        DimensionalObject doC = new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList() );
        DimensionalObject doD = new BaseDimensionalObject( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID, DimensionType.ATTRIBUTE_OPTION_COMBO, Lists.newArrayList() );
        DimensionalObject doE = new BaseDimensionalObject( "WpDi1seZU0Z", DimensionType.DATA_ELEMENT_GROUP_SET, Lists.newArrayList() );
        DimensionalObject doF = new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, Lists.newArrayList() );
        DimensionalObject doG = new BaseDimensionalObject( "Cz3WQznvrCM", DimensionType.ORGANISATION_UNIT_GROUP_SET, Lists.newArrayList() );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( doA )
            .addDimension( doB )
            .addDimension( doC )
            .addDimension( doD )
            .addDimension( doE )
            .addDimension( doF )
            .addDimension( doG )
            .build();
        
        List<DimensionalObject> dimensions = params.getDimensions();
        
        assertEquals( 7, dimensions.size() );
        assertEquals( doF, dimensions.get( 0 ) );
        assertEquals( doB, dimensions.get( 1 ) );
        assertEquals( doD, dimensions.get( 2 ) );
        assertEquals( doA, dimensions.get( 3 ) );
        assertEquals( doC, dimensions.get( 4 ) );
        assertEquals( doE, dimensions.get( 5 ) );
        assertEquals( doG, dimensions.get( 6 ) );        
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

    @Test
    public void testGetDaysForAvgSumIntAggregation()
    {
        List<DimensionalItemObject> dataElements = Lists.newArrayList( deA, deB, deC );
        List<DimensionalItemObject> periods = Lists.newArrayList( peA, peB );
        
        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( dataElements )
            .withPeriods( periods ).build();
        
        assertEquals( peA.getDaysInPeriod(), params.getDaysForAvgSumIntAggregation() );
        
        params = DataQueryParams.newBuilder()
            .withDataElements( dataElements )
            .withFilterPeriods( periods ).build();
        
        int totalDays = peA.getDaysInPeriod() + peB.getDaysInPeriod();
        
        assertEquals( totalDays, params.getDaysForAvgSumIntAggregation() );
    }

    @Test
    public void testGetDimensionsAndFiltersByDimensionTypes()
    {        
        DataQueryParams params = DataQueryParams.newBuilder()
            .withDataElements( Lists.newArrayList( deA, deB, deC ) )
            .withPeriods( Lists.newArrayList( peA, peB ) )
            .withOrganisationUnits( Lists.newArrayList( ouA, ouB ) )
            .build();
        
        List<DimensionalObject> dimensions = params.getDimensionsAndFilters( Sets.newHashSet( DimensionType.PERIOD, DimensionType.ORGANISATION_UNIT ) );
        
        assertEquals( 2, dimensions.size() );
        assertTrue( dimensions.contains( new BaseDimensionalObject( PERIOD_DIM_ID ) ) );
        assertTrue( dimensions.contains( new BaseDimensionalObject( ORGUNIT_DIM_ID ) ) );        
    }

    @Test
    public void testGetLatestEndDate()
    {
        Period q1_2016 = PeriodType.getPeriodFromIsoString( "2016Q1");
        Period q2_2016 = PeriodType.getPeriodFromIsoString( "2016Q2");
        Calendar today = Calendar.getInstance();

        DataQueryParams dqp1 = DataQueryParams.newBuilder()
            .withEndDate( today.getTime() )
            .withPeriods( Lists.newArrayList( q1_2016 ) )
            .withFilterPeriods( Lists.newArrayList( q2_2016 ) )
            .build();

        DataQueryParams dqp2 = DataQueryParams.newBuilder()
            .withEndDate( q1_2016.getEndDate() )
            .build();

        DataQueryParams dqp3 = DataQueryParams.newBuilder()
            .withFilterPeriods( Lists.newArrayList( q2_2016 ) )
            .withPeriods( Lists.newArrayList( q1_2016 ) )
            .build();

        assertEquals( today.getTime(), dqp1.getLatestEndDate() );
        assertEquals( q1_2016.getEndDate(), dqp2.getLatestEndDate() );
        assertEquals( q2_2016.getEndDate(), dqp3.getLatestEndDate() );
    }
}
