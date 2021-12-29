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
import static org.hisp.dhis.program.AnalyticsType.ENROLLMENT;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.common.GridHeader;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.system.grid.ListGrid;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Jim Grace
 */
public class PredictionAnalyticsDataFetcherTest
    extends DhisConvenienceTest
{
    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private CategoryService categoryService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private OrganisationUnit orgUnitA;

    private OrganisationUnit orgUnitB;

    private List<OrganisationUnit> orgUnits;

    private Period periodA;

    private Period periodB;

    private Set<Period> periods;

    private Program programA;

    private Program programB;

    private TrackedEntityAttribute trackedEntityAttributeA;

    private PredictionAnalyticsDataFetcher fetcher;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Before
    public void initTest()
    {
        periodA = createPeriod( "202101" );
        periodB = createPeriod( "202102" );

        periodA.setId( 3 );
        periodB.setId( 4 );

        periods = Sets.newHashSet( periodA, periodB );

        orgUnitA = createOrganisationUnit( "A" );
        orgUnitB = createOrganisationUnit( "B" );

        orgUnitA.setUid( "orgUnitAuid" );
        orgUnitB.setUid( "orgUnitBuid" );

        orgUnits = Lists.newArrayList( orgUnitA, orgUnitB );

        programA = createProgram( 'A' );
        programB = createProgram( 'B' );

        fetcher = new PredictionAnalyticsDataFetcher( analyticsService, categoryService );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetValues()
    {
        // ---------------------------------------------------------------------
        // Items with Attribute Option Combos
        // ---------------------------------------------------------------------

        CategoryOptionCombo aocC = createCategoryOptionCombo( 'C' );
        CategoryOptionCombo aocD = createCategoryOptionCombo( 'D' );

        when( categoryService.getCategoryOptionCombo( aocC.getUid() ) ).thenReturn( aocC );
        when( categoryService.getCategoryOptionCombo( aocD.getUid() ) ).thenReturn( aocD );

        ProgramIndicator programIndicatorA = createProgramIndicator( 'A', programA, "expressionA", "filterA" );
        ProgramIndicator programIndicatorB = createProgramIndicator( 'B', programB, "expressionB", "filterB" );

        trackedEntityAttributeA = createTrackedEntityAttribute( 'A' );

        Set<DimensionalItemObject> aocItems = Sets.newHashSet( programIndicatorA, programIndicatorB,
            trackedEntityAttributeA );

        DataQueryParams aocParams = DataQueryParams.newBuilder()
            .withPeriods( Lists.newArrayList( periods ) )
            .withDataDimensionItems( Lists.newArrayList( aocItems ) )
            .withOrganisationUnits( orgUnits )
            .withAttributeOptionCombos( Collections.emptyList() )
            .build();

        Grid aocGrid = new ListGrid();

        aocGrid.addHeader( new GridHeader( PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true ) );
        aocGrid.addHeader( new GridHeader( DATA_X_DIM_ID, "DimensionItem", ValueType.TEXT, false, true ) );
        aocGrid.addHeader( new GridHeader( ORGUNIT_DIM_ID, "OrganisationUnit", ValueType.TEXT, false, true ) );
        aocGrid.addHeader( new GridHeader( ATTRIBUTEOPTIONCOMBO_DIM_ID, "AOC", ValueType.TEXT, false, true ) );
        aocGrid.addHeader( new GridHeader( "value", "Value", ValueType.NUMBER, false, true ) );

        aocGrid.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( programIndicatorA.getUid() )
            .addValue( orgUnitA.getUid() )
            .addValue( aocC.getUid() )
            .addValue( 10.0 );

        aocGrid.addRow()
            .addValue( periodB.getIsoDate() )
            .addValue( programIndicatorB.getUid() )
            .addValue( orgUnitA.getUid() )
            .addValue( aocC.getUid() )
            .addValue( 20.0 );

        aocGrid.addRow()
            .addValue( periodB.getIsoDate() )
            .addValue( trackedEntityAttributeA.getUid() )
            .addValue( orgUnitA.getUid() )
            .addValue( aocD.getUid() )
            .addValue( 30.0 );

        aocGrid.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( programIndicatorA.getUid() )
            .addValue( orgUnitB.getUid() )
            .addValue( aocC.getUid() )
            .addValue( 40.0 );

        when( analyticsService.getAggregatedDataValues( aocParams ) ).thenReturn( aocGrid );

        FoundDimensionItemValue expected1;
        FoundDimensionItemValue expected2;
        FoundDimensionItemValue expected3;
        FoundDimensionItemValue expected4;

        expected1 = new FoundDimensionItemValue( orgUnitA, periodA, aocC, programIndicatorA, 10.0 );
        expected2 = new FoundDimensionItemValue( orgUnitA, periodB, aocC, programIndicatorB, 20.0 );
        expected3 = new FoundDimensionItemValue( orgUnitA, periodB, aocD, trackedEntityAttributeA, 30.0 );
        expected4 = new FoundDimensionItemValue( orgUnitB, periodA, aocC, programIndicatorA, 40.0 );

        // ---------------------------------------------------------------------
        // Items without Attribute Option Combos
        // ---------------------------------------------------------------------

        ProgramIndicator programIndicatorC = createProgramIndicator( 'C', programA, "expressionC", "filterC" );
        ProgramIndicator programIndicatorD = createProgramIndicator( 'D', programB, "expressionD", "filterD" );

        programIndicatorC.setAnalyticsType( ENROLLMENT );
        programIndicatorD.setAnalyticsType( ENROLLMENT );

        Set<DimensionalItemObject> nonAocItems = Sets.newHashSet( programIndicatorC, programIndicatorD );

        DataQueryParams nonAocParams = DataQueryParams.newBuilder()
            .withPeriods( Lists.newArrayList( periods ) )
            .withDataDimensionItems( Lists.newArrayList( nonAocItems ) )
            .withOrganisationUnits( orgUnits )
            .build();

        Grid nonAocGrid = new ListGrid();

        nonAocGrid.addHeader( new GridHeader( PERIOD_DIM_ID, "Period", ValueType.TEXT, false, true ) );
        nonAocGrid.addHeader( new GridHeader( DATA_X_DIM_ID, "DimensionItem", ValueType.TEXT, false, true ) );
        nonAocGrid.addHeader( new GridHeader( ORGUNIT_DIM_ID, "OrganisationUnit", ValueType.TEXT, false, true ) );
        nonAocGrid.addHeader( new GridHeader( "value", "Value", ValueType.NUMBER, false, true ) );

        nonAocGrid.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( programIndicatorC.getUid() )
            .addValue( orgUnitA.getUid() )
            .addValue( 100.0 );

        nonAocGrid.addRow()
            .addValue( periodA.getIsoDate() )
            .addValue( programIndicatorD.getUid() )
            .addValue( orgUnitB.getUid() )
            .addValue( 200.0 );

        nonAocGrid.addRow()
            .addValue( periodB.getIsoDate() )
            .addValue( programIndicatorC.getUid() )
            .addValue( orgUnitA.getUid() )
            .addValue( 300.0 );

        when( analyticsService.getAggregatedDataValues( nonAocParams ) ).thenReturn( nonAocGrid );

        FoundDimensionItemValue expected5;
        FoundDimensionItemValue expected6;
        FoundDimensionItemValue expected7;

        CategoryOptionCombo noAoc = null;

        expected5 = new FoundDimensionItemValue( orgUnitA, periodA, noAoc, programIndicatorC, 100.0 );
        expected6 = new FoundDimensionItemValue( orgUnitB, periodA, noAoc, programIndicatorD, 200.0 );
        expected7 = new FoundDimensionItemValue( orgUnitA, periodB, noAoc, programIndicatorC, 300.0 );

        // ---------------------------------------------------------------------
        // Do the test
        // ---------------------------------------------------------------------

        Set<DimensionalItemObject> items = Sets.union( aocItems, nonAocItems );

        fetcher.init( periods, items );

        List<FoundDimensionItemValue> actual = fetcher.getValues( orgUnits );

        assertContainsOnly( actual, expected1, expected2, expected3, expected4, expected5, expected6, expected7 );
    }

    @Test
    public void testGetValuesEmpty()
    {
        fetcher.init( periods, Collections.emptySet() );

        List<FoundDimensionItemValue> actual = fetcher.getValues( orgUnits );

        assertContainsOnly( actual );
    }
}
