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
package org.hisp.dhis.analytics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.Category;
import org.hisp.dhis.category.CategoryCombo;
import org.hisp.dhis.category.CategoryOption;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.BaseDimensionalObject;
import org.hisp.dhis.common.DataDimensionItemType;
import org.hisp.dhis.common.DimensionType;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.DimensionalObject;
import org.hisp.dhis.common.DimensionalObjectUtils;
import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.common.ReportingRate;
import org.hisp.dhis.common.ReportingRateMetric;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.indicator.Indicator;
import org.hisp.dhis.indicator.IndicatorType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramDataElementDimensionItem;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
class DataQueryParamsTest extends DhisConvenienceTest
{
    private IndicatorType it;

    private Indicator inA;

    private Indicator inB;

    private CategoryOption coA;

    private CategoryOption coB;

    private Category caA;

    private CategoryCombo ccA;

    private CategoryOptionCombo cocA;

    private CategoryOptionCombo cocB;

    private DataElement deA;

    private DataElement deB;

    private DataElement deC;

    private DataSet dsA;

    private DataSet dsB;

    private DataSet dsC;

    private DataSet dsD;

    private ReportingRate rrA;

    private ReportingRate rrB;

    private ReportingRate rrC;

    private ReportingRate rrD;

    private Program prA;

    private Program prB;

    private DataElementGroup degA;

    private DataElementGroup degB;

    private DataElementGroupSet degsA;

    private TrackedEntityAttribute atA;

    private Period peA;

    private Period peB;

    private Period peC;

    private OrganisationUnit ouA;

    private OrganisationUnit ouB;

    @BeforeEach
    void setUpTest()
    {
        it = createIndicatorType( 'A' );
        inA = createIndicator( 'A', it );
        inB = createIndicator( 'A', it );
        coA = createCategoryOption( 'A' );
        coB = createCategoryOption( 'B' );
        caA = createCategory( 'A', coA, coB );
        ccA = createCategoryCombo( 'A', caA );
        cocA = createCategoryOptionCombo( ccA, coA );
        cocB = createCategoryOptionCombo( ccA, coB );
        ccA.getOptionCombos().add( cocA );
        ccA.getOptionCombos().add( cocB );
        deA = createDataElement( 'A', ccA );
        deB = createDataElement( 'B', ccA );
        deC = createDataElement( 'C', ccA );
        dsA = createDataSet( 'A' );
        dsB = createDataSet( 'B' );
        dsC = createDataSet( 'C' );
        dsD = createDataSet( 'D' );
        rrA = new ReportingRate( dsA, ReportingRateMetric.REPORTING_RATE );
        rrB = new ReportingRate( dsB, ReportingRateMetric.REPORTING_RATE );
        rrC = new ReportingRate( dsC, ReportingRateMetric.EXPECTED_REPORTS );
        rrD = new ReportingRate( dsD, ReportingRateMetric.ACTUAL_REPORTS );
        prA = createProgram( 'A' );
        prB = createProgram( 'B' );
        degA = createDataElementGroup( 'A' );
        degB = createDataElementGroup( 'B' );
        degsA = createDataElementGroupSet( 'A' );
        degsA.addDataElementGroup( degA );
        degsA.addDataElementGroup( degB );
        atA = createTrackedEntityAttribute( 'A' );
        peA = createPeriod( "201601" );
        peB = createPeriod( "201603" );
        peC = createPeriod( "2017July" );
        ouA = createOrganisationUnit( 'A' );
        ouB = createOrganisationUnit( 'B' );
    }

    @Test
    void testAddDimension()
    {
        DimensionalObject doA = new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID,
            DimensionType.ORGANISATION_UNIT, Lists.newArrayList() );
        DimensionalObject doB = new BaseDimensionalObject( DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID,
            DimensionType.CATEGORY_OPTION_COMBO, Lists.newArrayList() );
        DimensionalObject doC = new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD,
            Lists.newArrayList() );
        DimensionalObject doD = new BaseDimensionalObject( DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID,
            DimensionType.ATTRIBUTE_OPTION_COMBO, Lists.newArrayList() );
        DimensionalObject doE = new BaseDimensionalObject( "WpDi1seZU0Z", DimensionType.DATA_ELEMENT_GROUP_SET,
            Lists.newArrayList() );
        DimensionalObject doF = new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X,
            Lists.newArrayList() );
        DimensionalObject doG = new BaseDimensionalObject( "Cz3WQznvrCM", DimensionType.ORGANISATION_UNIT_GROUP_SET,
            Lists.newArrayList() );
        DataQueryParams params = DataQueryParams.newBuilder().addDimension( doA ).addDimension( doB )
            .addDimension( doC ).addDimension( doD ).addDimension( doE ).addDimension( doF ).addDimension( doG )
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
    void testSetGetDataElementsReportingRates()
    {
        List<? extends DimensionalItemObject> dataElements = Lists.newArrayList( deA, deB, deC );
        List<? extends DimensionalItemObject> reportingRates = Lists.newArrayList( rrA, rrB );
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( dataElements )
            .withReportingRates( reportingRates ).build();
        assertEquals( 3, params.getDataElements().size() );
        assertTrue( params.getDataElements().containsAll( dataElements ) );
        assertEquals( 2, params.getReportingRates().size() );
        assertTrue( params.getReportingRates().containsAll( reportingRates ) );
    }

    @Test
    void testGetDimensionFromParam()
    {
        assertEquals( DATA_X_DIM_ID,
            DimensionalObjectUtils.getDimensionFromParam( "dx:D348asd782j;kj78HnH6hgT;9ds9dS98s2" ) );
    }

    @Test
    void testGetDimensionItemsFromParam()
    {
        List<String> expected = new ArrayList<>( Lists.newArrayList( "D348asd782j", "kj78HnH6hgT", "9ds9dS98s2" ) );
        assertEquals( expected,
            DimensionalObjectUtils.getDimensionItemsFromParam( "de:D348asd782j;kj78HnH6hgT;9ds9dS98s2" ) );
    }

    @Test
    void testGetDimensionItemsFromParamForPeriods()
    {
        List<String> expected = new ArrayList<>(
            Lists.newArrayList( "TODAY:LAST_UPDATED", "LAST_WEEK:INCIDENT_DATE", "YESTERDAY" ) );
        assertEquals( expected,
            DimensionalObjectUtils
                .getDimensionItemsFromParam( "pe:TODAY:LAST_UPDATED;LAST_WEEK:INCIDENT_DATE;YESTERDAY" ) );
    }

    @Test
    void testGetValueFromKeywordParam()
    {
        assertEquals( "4", DimensionalObjectUtils.getValueFromKeywordParam( "LEVEL-4" ) );
        assertNull( DimensionalObjectUtils.getValueFromKeywordParam( "LEVEL" ) );
    }

    @Test
    void testGetMeasureCriteriaFromParam()
    {
        Map<MeasureFilter, Double> expected = new HashMap<>();
        expected.put( MeasureFilter.GT, 100d );
        expected.put( MeasureFilter.LT, 200d );
        assertEquals( expected, DataQueryParams.getMeasureCriteriaFromParam( "GT:100;LT:200" ) );
    }

    @Test
    void testHasPeriods()
    {
        DataQueryParams params = DataQueryParams.newBuilder().build();
        assertFalse( params.hasPeriods() );
        List<DimensionalItemObject> periods = new ArrayList<>();
        params = DataQueryParams.newBuilder( params )
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, periods ) ).build();
        assertFalse( params.hasPeriods() );
        params = DataQueryParams.newBuilder().removeDimension( PERIOD_DIM_ID ).build();
        assertFalse( params.hasPeriods() );
        periods.add( new Period() );
        params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, periods ) ).build();
        assertTrue( params.hasPeriods() );
    }

    @Test
    void testPruneToDimensionType()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, null,
                Lists.newArrayList( createIndicator( 'A', null ), createIndicator( 'B', null ) ) ) )
            .addDimension( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT,
                null, null, Lists.newArrayList( createOrganisationUnit( 'A' ), createOrganisationUnit( 'B' ) ) ) )
            .addFilter( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, null, null,
                Lists.newArrayList( createPeriod( "201201" ), createPeriod( "201202" ) ) ) )
            .build();
        assertEquals( 2, params.getDimensions().size() );
        assertEquals( 1, params.getFilters().size() );
        params = DataQueryParams.newBuilder( params ).pruneToDimensionType( DimensionType.ORGANISATION_UNIT ).build();
        assertEquals( 1, params.getDimensions().size() );
        assertEquals( DimensionType.ORGANISATION_UNIT, params.getDimensions().get( 0 ).getDimensionType() );
        assertEquals( 0, params.getFilters().size() );
    }

    @Test
    void testRetainDataDimension()
    {
        List<DimensionalItemObject> items = Lists.newArrayList( inA, inB, deA, deB, deC, rrA, rrB );
        DataQueryParams params = DataQueryParams.newBuilder()
            .addOrSetDimensionOptions( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, items ).build();
        assertEquals( 7, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        params = DataQueryParams.newBuilder( params ).retainDataDimension( DataDimensionItemType.DATA_ELEMENT ).build();
        assertEquals( 3, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().size() );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deA ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deB ) );
        assertTrue( params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems().contains( deC ) );
    }

    @Test
    void testRetainDataDimensions()
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
    void testGetDimensionItemArrayExplodeCoc()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addOrSetDimensionOptions( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null,
                Lists.newArrayList( deA, deB, deC ) )
            .addOrSetDimensionOptions( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD, null,
                Lists.newArrayList( peA, peB ) )
            .build();
        List<DimensionalItemObject> items = params
            .getDimensionItemsExplodeCoc( DimensionalObject.CATEGORYOPTIONCOMBO_DIM_ID );
        assertEquals( 2, items.size() );
        assertTrue( items.contains( cocA ) );
        assertTrue( items.contains( cocB ) );
    }

    @Test
    void testRetainDataDimensionReportingRates()
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
    void testSetDimensionOptions()
    {
        List<DimensionalItemObject> itemsBefore = Lists.newArrayList( createIndicator( 'A', null ),
            createIndicator( 'B', null ), createIndicator( 'C', null ), createIndicator( 'D', null ) );
        List<DimensionalItemObject> itemsAfter = Lists.newArrayList( createIndicator( 'A', null ),
            createIndicator( 'B', null ) );
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X, null, null,
                itemsBefore ) )
            .build();
        assertEquals( itemsBefore, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems() );
        params = DataQueryParams.newBuilder( params )
            .withDimensionOptions( DimensionalObject.DATA_X_DIM_ID, itemsAfter ).build();
        assertEquals( itemsAfter, params.getDimension( DimensionalObject.DATA_X_DIM_ID ).getItems() );
    }

    @Test
    void testGetDaysForAvgSumIntAggregation()
    {
        List<DimensionalItemObject> dataElements = Lists.newArrayList( deA, deB, deC );
        List<DimensionalItemObject> periods = Lists.newArrayList( peA, peB );
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( dataElements ).withPeriods( periods )
            .build();
        assertEquals( peA.getDaysInPeriod(), params.getDaysForAvgSumIntAggregation() );
        params = DataQueryParams.newBuilder().withDataElements( dataElements ).withFilterPeriods( periods ).build();
        int totalDays = peA.getDaysInPeriod() + peB.getDaysInPeriod();
        assertEquals( totalDays, params.getDaysForAvgSumIntAggregation() );
    }

    @Test
    void testGetDimensionsAndFiltersByDimensionTypes()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElements( Lists.newArrayList( deA, deB, deC ) )
            .withPeriods( Lists.newArrayList( peA, peB ) ).withOrganisationUnits( Lists.newArrayList( ouA, ouB ) )
            .build();
        List<DimensionalObject> dimensions = params
            .getDimensionsAndFilters( Sets.newHashSet( DimensionType.PERIOD, DimensionType.ORGANISATION_UNIT ) );
        assertEquals( 2, dimensions.size() );
        assertTrue( dimensions.contains( new BaseDimensionalObject( PERIOD_DIM_ID ) ) );
        assertTrue( dimensions.contains( new BaseDimensionalObject( ORGUNIT_DIM_ID ) ) );
    }

    @Test
    void testGetLatestPeriod()
    {
        Period jan_2016 = PeriodType.getPeriodFromIsoString( "201601" );
        Period feb_2016 = PeriodType.getPeriodFromIsoString( "201602" );
        Period mar_2016 = PeriodType.getPeriodFromIsoString( "201603" );
        DataQueryParams paramsA = DataQueryParams.newBuilder().withPeriods( Lists.newArrayList( jan_2016 ) )
            .withFilterPeriods( Lists.newArrayList( feb_2016, mar_2016 ) ).build();
        DataQueryParams paramsB = DataQueryParams.newBuilder().withPeriods( Lists.newArrayList( mar_2016 ) )
            .withFilterPeriods( Lists.newArrayList( jan_2016, feb_2016 ) ).build();
        assertEquals( mar_2016, paramsA.getLatestPeriod() );
        assertEquals( mar_2016, paramsB.getLatestPeriod() );
    }

    @Test
    void testGetLatestEndDate()
    {
        Period q1_2016 = PeriodType.getPeriodFromIsoString( "2016Q1" );
        Period q2_2016 = PeriodType.getPeriodFromIsoString( "2016Q2" );
        Calendar today = Calendar.getInstance();
        DataQueryParams paramsA = DataQueryParams.newBuilder().withEndDate( today.getTime() )
            .withPeriods( Lists.newArrayList( q1_2016 ) ).withFilterPeriods( Lists.newArrayList( q2_2016 ) ).build();
        DataQueryParams paramsB = DataQueryParams.newBuilder().withEndDate( q1_2016.getEndDate() ).build();
        DataQueryParams paramsC = DataQueryParams.newBuilder().withFilterPeriods( Lists.newArrayList( q2_2016 ) )
            .withPeriods( Lists.newArrayList( q1_2016 ) ).build();
        assertEquals( today.getTime(), paramsA.getLatestEndDate() );
        assertEquals( q1_2016.getEndDate(), paramsB.getLatestEndDate() );
        assertEquals( q2_2016.getEndDate(), paramsC.getLatestEndDate() );
    }

    @Test
    void testGetEarliestStartDate()
    {
        Period jan_2016 = PeriodType.getPeriodFromIsoString( "201601" );
        Period feb_2016 = PeriodType.getPeriodFromIsoString( "201602" );
        Period mar_2016 = PeriodType.getPeriodFromIsoString( "201603" );
        Date dec_2015 = getDate( 2015, 12, 1 );
        DataQueryParams paramsA = DataQueryParams.newBuilder().withStartDate( dec_2015 )
            .withPeriods( Lists.newArrayList( jan_2016 ) ).withFilterPeriods( Lists.newArrayList( feb_2016, mar_2016 ) )
            .build();
        DataQueryParams paramsB = DataQueryParams.newBuilder().withStartDate( jan_2016.getStartDate() ).build();
        DataQueryParams paramsC = DataQueryParams.newBuilder()
            .withFilterPeriods( Lists.newArrayList( feb_2016, mar_2016 ) ).withPeriods( Lists.newArrayList( jan_2016 ) )
            .build();
        assertEquals( dec_2015, paramsA.getEarliestStartDate() );
        assertEquals( jan_2016.getStartDate(), paramsB.getEarliestStartDate() );
        assertEquals( jan_2016.getStartDate(), paramsC.getEarliestStartDate() );
    }

    @Test
    void testSetPeriodDimensionWithoutOptionsA()
    {
        Period mar_2016 = PeriodType.getPeriodFromIsoString( "201603" );
        Period apr_2016 = PeriodType.getPeriodFromIsoString( "201604" );
        Period may_2016 = PeriodType.getPeriodFromIsoString( "201605" );
        DataQueryParams params = DataQueryParams.newBuilder()
            .withPeriods( Lists.newArrayList( mar_2016, apr_2016, may_2016 ) ).build();
        assertEquals( 3, params.getPeriods().size() );
        DataQueryParams query = DataQueryParams.newBuilder( params ).withEarliestStartDateLatestEndDate()
            .withPeriodDimensionWithoutOptions().build();
        assertNotNull( query.getDimension( DimensionalObject.PERIOD_DIM_ID ) );
        assertEquals( 0, query.getPeriods().size() );
        assertEquals( getDate( 2016, 3, 1 ), query.getStartDate() );
        assertEquals( getDate( 2016, 5, 31 ), query.getEndDate() );
    }

    @Test
    void testSetPeriodDimensionWithoutOptionsB()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withStartDate( getDate( 2017, 3, 1 ) )
            .withEndDate( getDate( 2017, 5, 31 ) ).build();
        assertEquals( 0, params.getPeriods().size() );
        DataQueryParams query = DataQueryParams.newBuilder( params ).withEarliestStartDateLatestEndDate()
            .withPeriodDimensionWithoutOptions().build();
        assertNotNull( query.getDimension( DimensionalObject.PERIOD_DIM_ID ) );
        assertEquals( 0, query.getPeriods().size() );
        assertEquals( getDate( 2017, 3, 1 ), query.getStartDate() );
        assertEquals( getDate( 2017, 5, 31 ), query.getEndDate() );
    }

    @Test
    void testGetAllTypedOrganisationUnits()
    {
        DataQueryParams paramsA = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList( ouA, ouB ) ) )
            .build();
        DataQueryParams paramsB = DataQueryParams.newBuilder()
            .addFilter( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList( ouA, ouB ) ) )
            .build();
        assertEquals( 2, paramsA.getAllTypedOrganisationUnits().size() );
        assertEquals( 2, paramsB.getAllTypedOrganisationUnits().size() );
    }

    @Test
    void testGetAllDataSets()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withPeriods( Lists.newArrayList( peA, peB ) )
            .withOrganisationUnits( Lists.newArrayList( ouA, ouB ) )
            .withReportingRates( Lists.newArrayList( rrA, rrB, rrC ) ).build();
        Set<DimensionalItemObject> expected = Sets.newHashSet( dsA, dsB, dsC );
        assertEquals( expected, params.getAllDataSets() );
    }

    @Test
    void testGetCategoryOptions()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withPeriods( Lists.newArrayList( peA, peB ) )
            .withOrganisationUnits( Lists.newArrayList( ouA, ouB ) ).withCategory( caA ).build();
        Set<DimensionalItemObject> expected = Sets.newHashSet( coA, coB );
        assertEquals( expected, params.getCategoryOptions() );
    }

    @Test
    void testGetDataElementGroups()
    {
        DataQueryParams params = DataQueryParams.newBuilder().withDataElementGroupSet( degsA )
            .withPeriods( Lists.newArrayList( peA, peB ) ).withOrganisationUnits( Lists.newArrayList( ouA, ouB ) )
            .build();
        List<DimensionalItemObject> expected = Lists.newArrayList( degA, degB );
        assertEquals( expected, params.getAllDataElementGroups() );
    }

    @Test
    void testGetAllProgramsInAttributesAndDataElements()
    {
        ProgramTrackedEntityAttributeDimensionItem ptaA = new ProgramTrackedEntityAttributeDimensionItem( prA, atA );
        ProgramDataElementDimensionItem pdeA = new ProgramDataElementDimensionItem( prB, deA );
        DataQueryParams params = DataQueryParams.newBuilder().withProgramAttributes( Lists.newArrayList( ptaA ) )
            .withProgramDataElements( Lists.newArrayList( pdeA ) ).withPeriods( Lists.newArrayList( peA, peB ) )
            .withOrganisationUnits( Lists.newArrayList( ouA, ouB ) ).build();
        Set<Program> expected = Sets.newHashSet( prA, prB );
        assertEquals( expected, params.getProgramsInAttributesAndDataElements() );
    }

    @Test
    void testGetKey()
    {
        DataQueryParams paramsA = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X,
                Lists.newArrayList( deA, deB ) ) )
            .addDimension( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList( ouA, ouB ) ) )
            .addDimension( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD,
                Lists.newArrayList( peA ) ) )
            .build();
        DataQueryParams paramsB = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( DimensionalObject.DATA_X_DIM_ID, DimensionType.DATA_X,
                Lists.newArrayList( deA ) ) )
            .addDimension( new BaseDimensionalObject( DimensionalObject.ORGUNIT_DIM_ID, DimensionType.ORGANISATION_UNIT,
                Lists.newArrayList( ouA ) ) )
            .addDimension( new BaseDimensionalObject( DimensionalObject.PERIOD_DIM_ID, DimensionType.PERIOD,
                Lists.newArrayList( peB ) ) )
            .withAggregationType( AnalyticsAggregationType.AVERAGE ).build();
        assertNotNull( paramsA.getKey() );
        assertEquals( 40, paramsA.getKey().length() );
        assertNotNull( paramsB.getKey() );
        assertEquals( 40, paramsB.getKey().length() );
        // No collision
        assertNotEquals( paramsA.getKey(), paramsB.getKey() );
    }

    @Test
    void testFinancialYearPeriodResultsInTwoAggregationYears()
    {
        DataQueryParams params = DataQueryParams.newBuilder()
            .addDimension( new BaseDimensionalObject( PERIOD_DIM_ID, DimensionType.PERIOD, Lists.newArrayList( peC ) ) )
            .withDataPeriodType( PeriodType.getPeriodTypeFromIsoString( "2017" ) ).build();
        ListMap<DimensionalItemObject, DimensionalItemObject> periodMap = params.getDataPeriodAggregationPeriodMap();
        assertThat( periodMap.entrySet(), hasSize( 2 ) );
        assertThat( periodMap.keySet(), IsIterableContainingInAnyOrder.containsInAnyOrder(
            hasProperty( "isoDate", Matchers.is( "2017" ) ), hasProperty( "isoDate", Matchers.is( "2018" ) ) ) );
        assertThat( periodMap.allValues(), hasSize( 2 ) );
        assertThat( periodMap.allValues(),
            IsIterableContainingInAnyOrder.containsInAnyOrder(
                hasProperty( "isoDate", Matchers.is( peC.getIsoDate() ) ),
                hasProperty( "isoDate", Matchers.is( peC.getIsoDate() ) ) ) );
    }
}
