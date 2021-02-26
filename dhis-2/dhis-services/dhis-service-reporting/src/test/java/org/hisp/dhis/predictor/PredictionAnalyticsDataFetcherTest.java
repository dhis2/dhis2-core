/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.predictor;

import static org.hisp.dhis.common.DimensionalObject.ATTRIBUTEOPTIONCOMBO_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.DATA_X_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.ORGUNIT_DIM_ID;
import static org.hisp.dhis.common.DimensionalObject.PERIOD_DIM_ID;
import static org.hisp.dhis.predictor.PredictionAnalyticsDataFetcher.PARTITION_SIZE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.MapMap;
import org.hisp.dhis.common.MapMapMap;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

/**
 * @author Jim Grace
 */
public class PredictionAnalyticsDataFetcherTest
    extends DhisConvenienceTest
{
    @Autowired
    private PredictionAnalyticsDataFetcher predictionAnalyticsDataFetcher;

    @Mock
    private AnalyticsService analyticsService;;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final int TEST_A = 1;

    private static final int TEST_B = 3;

    private static final int TEST_C = PARTITION_SIZE + 1;

    private static final int TEST_D = PARTITION_SIZE * 2 - 1;

    private static final int TEST_E = PARTITION_SIZE * 2 + 3;

    private static final int TEST_SIZE = PARTITION_SIZE * 2 + 5;

    private static final long BASE_ORG_UNIT_ID = 1000;

    private CategoryOptionCombo attributeOptionComboA;

    private CategoryOptionCombo attributeOptionComboB;

    private List<OrganisationUnit> orgUnits;

    private List<List<OrganisationUnit>> partitions;

    private Period periodA;

    private Period periodB;

    private List<Period> periods;

    private Program programA;

    private Program programB;

    private ProgramIndicator programIndicatorA;

    private ProgramIndicator programIndicatorB;

    private TrackedEntityAttribute trackedEntityAttributeA;

    private ProgramTrackedEntityAttributeDimensionItem programAttributeA;

    private String programAttributeAUid;

    List<DimensionalItemObject> attributeOptionItems;

    List<DimensionalItemObject> nonAttributeOptionItems;

    DataQueryParams paramsWithAttributeOptionsA;

    DataQueryParams paramsWithAttributeOptionsB;

    DataQueryParams paramsWithAttributeOptionsC;

    DataQueryParams paramsWithoutAttributeOptionsA;

    DataQueryParams paramsWithoutAttributeOptionsB;

    DataQueryParams paramsWithoutAttributeOptionsC;

    Grid gridWithAttributeOptionsA;

    Grid gridWithAttributeOptionsB;

    Grid gridWithAttributeOptionsC;

    Grid gridWithoutAttributeOptionsA;

    Grid gridWithoutAttributeOptionsB;

    Grid gridWithoutAttributeOptionsC;

    PredictionAnalyticsDataFetcher fetcher;

    MapMapMap<String, Period, DimensionalItemObject, Double> aocExpected;

    MapMapMap<String, Period, DimensionalItemObject, Double> aocData;

    MapMap<Period, DimensionalItemObject, Double> nonAocExpected;

    MapMap<Period, DimensionalItemObject, Double> nonAocData;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Before
    public void initTest()
    {
        attributeOptionComboA = createCategoryOptionCombo( 'C' );
        attributeOptionComboB = createCategoryOptionCombo( 'D' );

        attributeOptionComboA.setId( 1 );
        attributeOptionComboB.setId( 2 );

        periodA = createPeriod( "202101" );
        periodB = createPeriod( "202102" );

        periodA.setId( 3 );
        periodB.setId( 4 );

        periods = Lists.newArrayList( periodA, periodB );

        orgUnits = new ArrayList<>();

        for ( int i = 0; i < TEST_SIZE; i++ )
        {
            OrganisationUnit orgUnit = createOrganisationUnit( "OrgUnit " + i );

            orgUnit.setId( BASE_ORG_UNIT_ID + i );

            orgUnit.setUid( "OuUID" + (100000 + i) );

            orgUnits.add( orgUnit );
        }

        partitions = Lists.partition( orgUnits, PARTITION_SIZE );

        programA = createProgram( 'A' );
        programB = createProgram( 'B' );

        programIndicatorA = createProgramIndicator( 'A', programA, "expressionA", "filterA" );
        programIndicatorB = createProgramIndicator( 'B', programB, "expressionB", "filterB" );

        trackedEntityAttributeA = createTrackedEntityAttribute( 'A' );

        programAttributeA = new ProgramTrackedEntityAttributeDimensionItem( programA, trackedEntityAttributeA );
        programAttributeAUid = programA.getUid() + "." + trackedEntityAttributeA.getUid();

        attributeOptionItems = Lists.newArrayList(
            programIndicatorA,
            programIndicatorB,
            trackedEntityAttributeA );

        nonAttributeOptionItems = Lists.newArrayList(
            programAttributeA );

        paramsWithAttributeOptionsA = DataQueryParams.newBuilder()
            .withPeriods( periods )
            .withDataDimensionItems( attributeOptionItems )
            .withOrganisationUnits( partitions.get( 0 ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .build();

        paramsWithAttributeOptionsB = DataQueryParams.newBuilder()
            .withPeriods( periods )
            .withDataDimensionItems( attributeOptionItems )
            .withOrganisationUnits( partitions.get( 1 ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .build();

        paramsWithAttributeOptionsC = DataQueryParams.newBuilder()
            .withPeriods( periods )
            .withDataDimensionItems( attributeOptionItems )
            .withOrganisationUnits( partitions.get( 2 ) )
            .withAttributeOptionCombos( Lists.newArrayList() )
            .build();

        paramsWithoutAttributeOptionsA = DataQueryParams.newBuilder()
            .withPeriods( periods )
            .withDataDimensionItems( attributeOptionItems )
            .withOrganisationUnits( partitions.get( 0 ) )
            .build();

        paramsWithoutAttributeOptionsB = DataQueryParams.newBuilder()
            .withPeriods( periods )
            .withDataDimensionItems( attributeOptionItems )
            .withOrganisationUnits( partitions.get( 1 ) )
            .build();

        paramsWithoutAttributeOptionsC = DataQueryParams.newBuilder()
            .withPeriods( periods )
            .withDataDimensionItems( attributeOptionItems )
            .withOrganisationUnits( partitions.get( 2 ) )
            .build();

        gridWithAttributeOptionsA = newGridWithAttributeOptions();

        gridWithAttributeOptionsA.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( programIndicatorA.getUid() )
            .addValue( orgUnits.get( TEST_A ).getUid() )
            .addValue( attributeOptionComboA.getUid() )
            .addValue( (Double) 10.0 );

        gridWithAttributeOptionsA.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( programIndicatorB.getUid() )
            .addValue( orgUnits.get( TEST_A ).getUid() )
            .addValue( attributeOptionComboA.getUid() )
            .addValue( (Double) 20.0 );

        gridWithAttributeOptionsA.addRow()
            .addValue( periodB.getIsoDate() )
            .addValue( programIndicatorB.getUid() )
            .addValue( orgUnits.get( TEST_B ).getUid() )
            .addValue( attributeOptionComboA.getUid() )
            .addValue( (Double) 30.0 );

        gridWithAttributeOptionsB = newGridWithAttributeOptions();

        gridWithAttributeOptionsB.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( trackedEntityAttributeA.getUid() )
            .addValue( orgUnits.get( TEST_C ).getUid() )
            .addValue( attributeOptionComboA.getUid() )
            .addValue( (Double) 40.0 );

        gridWithAttributeOptionsB.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( trackedEntityAttributeA.getUid() )
            .addValue( orgUnits.get( TEST_D ).getUid() )
            .addValue( attributeOptionComboA.getUid() )
            .addValue( (Double) 50.0 );

        gridWithAttributeOptionsC = newGridWithAttributeOptions();

        gridWithAttributeOptionsC.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( trackedEntityAttributeA.getUid() )
            .addValue( orgUnits.get( TEST_E ).getUid() )
            .addValue( attributeOptionComboA.getUid() )
            .addValue( (Double) 60.0 );

        gridWithoutAttributeOptionsA = newGridWithoutAttributeOptions();

        gridWithoutAttributeOptionsA.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( programAttributeAUid )
            .addValue( orgUnits.get( TEST_B ).getUid() )
            .addValue( (Double) 70.0 );

        gridWithoutAttributeOptionsB = newGridWithoutAttributeOptions();

        gridWithoutAttributeOptionsC = newGridWithoutAttributeOptions();

        gridWithoutAttributeOptionsC.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( programAttributeAUid )
            .addValue( orgUnits.get( TEST_E ).getUid() )
            .addValue( (Double) 80.0 );

        fetcher = new PredictionAnalyticsDataFetcher( analyticsService );

        when( analyticsService.getAggregatedDataValues( any( DataQueryParams.class ) ) )
            .thenAnswer( p -> getMockGrid( p ) );

        fetcher.init( orgUnits, new HashSet<>( periods ), new HashSet<>( attributeOptionItems ),
            new HashSet<>( nonAttributeOptionItems ) );

        aocExpected = new MapMapMap<>();

        nonAocExpected = new MapMap<>();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetAocData()
    {
        for ( int i = 0; i < TEST_SIZE; i++ )
        {
            MapMapMap<String, Period, DimensionalItemObject, Double> aocData = fetcher.getAocData( orgUnits.get( i ) );

            MapMapMap<String, Period, DimensionalItemObject, Double> expected = new MapMapMap<>();

            switch ( i )
            {
            case TEST_A:
                expected.putEntry( attributeOptionComboA.getUid(), periodA, programIndicatorA, 10.0 );
                expected.putEntry( attributeOptionComboA.getUid(), periodA, programIndicatorB, 20.0 );
                assertEquals( expected, aocData );
                break;

            case TEST_B:
                expected.putEntry( attributeOptionComboA.getUid(), periodB, programIndicatorB, 30.0 );
                assertEquals( expected, aocData );
                break;

            case TEST_C:
                expected.putEntry( attributeOptionComboA.getUid(), periodA, trackedEntityAttributeA, 40.0 );
                assertEquals( expected, aocData );
                break;

            case TEST_D:
                expected.putEntry( attributeOptionComboA.getUid(), periodA, trackedEntityAttributeA, 50.0 );
                assertEquals( expected, aocData );
                break;

            case TEST_E:
                expected.putEntry( attributeOptionComboA.getUid(), periodA, trackedEntityAttributeA, 60.0 );
                assertEquals( expected, aocData );
                break;

            default:
                assertEquals( "index " + i + ": size 0", "index " + i + ": size " + aocData.size() );
                break;
            }
        }
    }

    @Test
    public void testGetNonAocData()
    {
        when( analyticsService.getAggregatedDataValues( any( DataQueryParams.class ) ) )
            .thenAnswer( p -> getMockGrid( p ) );

        fetcher.init( orgUnits, new HashSet<>( periods ), new HashSet<>( attributeOptionItems ),
            new HashSet<>( nonAttributeOptionItems ) );

        for ( int i = 0; i < TEST_SIZE; i++ )
        {
            MapMap<Period, DimensionalItemObject, Double> nonAocData = fetcher.getNonAocData( orgUnits.get( i ) );

            MapMap<Period, DimensionalItemObject, Double> expected = new MapMap<>();

            switch ( i )
            {
            case TEST_B:
                expected.putEntry( periodA, programAttributeA, 70.0 );
                assertEquals( expected, nonAocData );
                break;

            case TEST_E:
                expected.putEntry( periodA, programAttributeA, 80.0 );
                assertEquals( expected, nonAocData );
                break;

            default:
                assertEquals( "index " + i + ": size 0", "index " + i + ": size " + nonAocData.size() );
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    private Grid newGridWithAttributeOptions()
    {
        Grid grid = new ListGrid();

        grid.addHeader( new GridHeader( PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true ) );
        grid.addHeader( new GridHeader( DATA_X_DIM_ID, "DimensionItem", ValueType.TEXT, false, true ) );
        grid.addHeader( new GridHeader( ORGUNIT_DIM_ID, "OrganisationUnit", ValueType.TEXT, false, true ) );
        grid.addHeader( new GridHeader( ATTRIBUTEOPTIONCOMBO_DIM_ID, "AOC", ValueType.TEXT, false, true ) );
        grid.addHeader( new GridHeader( "value", "Value", ValueType.NUMBER, false, true ) );

        return grid;
    }

    private Grid newGridWithoutAttributeOptions()
    {
        Grid grid = new ListGrid();

        grid.addHeader( new GridHeader( PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true ) );
        grid.addHeader( new GridHeader( DATA_X_DIM_ID, "DimensionItem", ValueType.TEXT, false, true ) );
        grid.addHeader( new GridHeader( ORGUNIT_DIM_ID, "OrganisationUnit", ValueType.TEXT, false, true ) );
        grid.addHeader( new GridHeader( "value", "Value", ValueType.NUMBER, false, true ) );

        return grid;
    }

    private Grid getMockGrid( InvocationOnMock invocation )
    {
        DataQueryParams params = invocation.getArgument( 0 );

        OrganisationUnit firstOrgUnitParam = (OrganisationUnit) params.getDimensionOptions( ORGUNIT_DIM_ID ).get( 0 );

        long firstOrgUnitIndex = firstOrgUnitParam.getId() - BASE_ORG_UNIT_ID;

        if ( params.getDimension( ATTRIBUTEOPTIONCOMBO_DIM_ID ) != null )
        {
            if ( firstOrgUnitIndex == 0 )
            {
                return gridWithAttributeOptionsA;
            }
            else if ( firstOrgUnitIndex == PARTITION_SIZE )
            {
                return gridWithAttributeOptionsB;
            }
            else
            {
                return gridWithAttributeOptionsC;
            }
        }
        else
        {
            if ( firstOrgUnitIndex == 0 )
            {
                return gridWithoutAttributeOptionsA;
            }
            else if ( firstOrgUnitIndex == PARTITION_SIZE )
            {
                return gridWithoutAttributeOptionsB;
            }
            else
            {
                return gridWithoutAttributeOptionsC;
            }
        }
    }
}
