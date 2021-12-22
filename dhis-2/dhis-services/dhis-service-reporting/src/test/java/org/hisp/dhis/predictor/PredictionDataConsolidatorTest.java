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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.analytics.AnalyticsService;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.DimensionalItemObject;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramIndicator;
import org.hisp.dhis.program.ProgramTrackedEntityAttributeDimensionItem;
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
 * Tests PredictionDataConsolidator.
 *
 * @author Jim Grace
 */
public class PredictionDataConsolidatorTest
    extends DhisConvenienceTest
{
    @Mock
    private CategoryService categoryService;

    @Mock
    private DataValueService dataValueService;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private PredictionDataValueFetcher dataValueFetcher;

    @Mock
    private PredictionAnalyticsDataFetcher analyticsFetcher;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementX;

    private CategoryOptionCombo cocA;

    private CategoryOptionCombo cocB;

    private CategoryOptionCombo aocC;

    private CategoryOptionCombo aocD;

    private DataElementOperand dataElementOperandA;

    private DataElementOperand dataElementOperandB;

    private DataElementOperand dataElementOperandX;

    private Period periodA;

    private Period periodB;

    private Period periodC;

    private OrganisationUnit orgUnitA;

    private OrganisationUnit orgUnitB;

    private OrganisationUnit orgUnitC;

    private OrganisationUnit orgUnitD;

    private OrganisationUnit orgUnitE;

    private OrganisationUnit orgUnitF;

    private OrganisationUnit orgUnitG;

    private Set<OrganisationUnit> currentUserOrgUnits;

    private List<OrganisationUnit> levelOneOrgUnits;

    private DataValue dataValueX;

    private DataValue dataValueY;

    private Program programA;

    private Program programB;

    private ProgramIndicator programIndicatorA;

    private ProgramIndicator programIndicatorB;

    private TrackedEntityAttribute trackedEntityAttributeA;

    private ProgramTrackedEntityAttributeDimensionItem programAttributeA;

    private ProgramTrackedEntityAttributeDimensionItem programAttributeB;

    private Set<DimensionalItemObject> items;

    private Set<Period> dataValueQueryPeriods;

    private Set<Period> analyticsQueryPeriods;

    private Set<Period> existingOutputPeriods;

    private DataElementOperand outputDataElementOperand;

    private FoundDimensionItemValue foundValueA;

    private FoundDimensionItemValue foundValueB;

    private FoundDimensionItemValue foundValueC;

    private FoundDimensionItemValue foundValueD;

    private FoundDimensionItemValue foundValueE;

    private FoundDimensionItemValue foundValueF;

    private FoundDimensionItemValue foundValueG;

    private FoundDimensionItemValue foundValueH;

    private FoundDimensionItemValue foundValueI;

    private FoundDimensionItemValue foundValueJ;

    private FoundDimensionItemValue foundValueK;

    private FoundDimensionItemValue foundValueL;

    private PredictionDataConsolidator consolidator;

    private final boolean INCLUDE_DESCENDANTS = true;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Before
    public void initTest()
    {
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementX = createDataElement( 'X' );

        dataElementA.setId( 1 );
        dataElementB.setId( 2 );
        dataElementX.setId( 3 );

        cocA = createCategoryOptionCombo( 'A' );
        cocB = createCategoryOptionCombo( 'B' );

        cocA.setId( 4 );
        cocB.setId( 5 );

        cocA.setUid( "CatOptCombA" );
        cocB.setUid( "CatOptCombB" );

        aocC = createCategoryOptionCombo( 'C' );
        aocD = createCategoryOptionCombo( 'D' );

        aocC.setId( 6 );
        aocD.setId( 7 );

        aocC.setUid( "AttOptionCC" );
        aocD.setUid( "AttOptionCD" );

        dataElementOperandA = new DataElementOperand( dataElementA, cocA );
        dataElementOperandB = new DataElementOperand( dataElementB, cocB );
        dataElementOperandX = new DataElementOperand( dataElementX, cocA );

        periodA = createPeriod( "202201" );
        periodB = createPeriod( "202202" );
        periodC = createPeriod( "202203" );

        periodA.setUid( "Perio202201" );
        periodB.setUid( "Perio202202" );
        periodC.setUid( "Perio202203" );

        periodA.setId( 10 );
        periodB.setId( 11 );
        periodC.setId( 12 );

        // OrgUnit hierarchy:
        //
        // Level 1 - Level 2
        // -- A
        // -- B ------ E
        // -- C ------ F
        // -- D ------ G

        orgUnitA = createOrganisationUnit( "A" );
        orgUnitB = createOrganisationUnit( "B" );
        orgUnitC = createOrganisationUnit( "C" );
        orgUnitD = createOrganisationUnit( "D" );
        orgUnitE = createOrganisationUnit( "E", orgUnitB );
        orgUnitF = createOrganisationUnit( "F", orgUnitC );
        orgUnitG = createOrganisationUnit( "G", orgUnitD );

        orgUnitA.setId( 20 );
        orgUnitB.setId( 21 );
        orgUnitC.setId( 22 );
        orgUnitD.setId( 23 );
        orgUnitE.setId( 24 );
        orgUnitF.setId( 25 );
        orgUnitG.setId( 26 );

        orgUnitA.setUid( "orgUnitAAAA" );
        orgUnitB.setUid( "orgUnitBBBB" );
        orgUnitC.setUid( "orgUnitCCCC" );
        orgUnitD.setUid( "orgUnitDDDD" );
        orgUnitE.setUid( "orgUnitEEEE" );
        orgUnitF.setUid( "orgUnitFFFF" );
        orgUnitG.setUid( "orgUnitGGGG" );

        orgUnitA.setPath( "/orgUnitAAAA" );
        orgUnitB.setPath( "/orgUnitBBBB" );
        orgUnitC.setPath( "/orgUnitCCCC" );
        orgUnitD.setPath( "/orgUnitDDDD" );
        orgUnitE.setPath( "/orgUnitBBBB/orgUnitEEEE" );
        orgUnitF.setPath( "/orgUnitCCCC/orgUnitFFFF" );
        orgUnitG.setPath( "/orgUnitDDDD/orgUnitGGGG" );

        currentUserOrgUnits = Sets.newHashSet( orgUnitA, orgUnitB, orgUnitC, orgUnitD );

        levelOneOrgUnits = Lists.newArrayList( orgUnitA, orgUnitB, orgUnitC, orgUnitD );

        // DataValue values:
        foundValueA = new FoundDimensionItemValue( orgUnitB, periodA, aocC, dataElementA, 25.0 );
        foundValueB = new FoundDimensionItemValue( orgUnitC, periodA, aocC, dataElementB, 18.0 );
        foundValueC = new FoundDimensionItemValue( orgUnitB, periodB, aocC, dataElementOperandA, 10.0 );
        foundValueD = new FoundDimensionItemValue( orgUnitB, periodB, aocD, dataElementOperandX, 30.0 );
        foundValueE = new FoundDimensionItemValue( orgUnitB, periodC, aocC, dataElementOperandX, 50.0 );

        // Analytics values with attribute option combo:
        foundValueF = new FoundDimensionItemValue( orgUnitB, periodA, aocC, programIndicatorA, 10.0 );
        foundValueG = new FoundDimensionItemValue( orgUnitB, periodB, aocC, programIndicatorB, 10.0 );
        foundValueH = new FoundDimensionItemValue( orgUnitB, periodA, aocD, trackedEntityAttributeA, 10.0 );
        foundValueI = new FoundDimensionItemValue( orgUnitD, periodA, aocC, programIndicatorA, 10.0 );

        // Analytics values without attribute option combo:
        CategoryOptionCombo noAoc = null;
        foundValueJ = new FoundDimensionItemValue( orgUnitD, periodA, noAoc, programAttributeA, 100.0 );
        foundValueK = new FoundDimensionItemValue( orgUnitD, periodA, noAoc, programAttributeB, 200.0 );
        foundValueL = new FoundDimensionItemValue( orgUnitD, periodB, noAoc, programAttributeA, 300.0 );

        // old predictor data values:
        dataValueX = new DataValue( dataElementX, periodA, orgUnitB, cocA, aocD, "30.0", "Z", null, null, null, false );
        dataValueY = new DataValue( dataElementX, periodC, orgUnitB, cocA, aocC, "40.0", "Z", null, null, null, true );

        programA = createProgram( 'A' );
        programB = createProgram( 'B' );

        programA.setUid( "ProgramAAAA" );
        programB.setUid( "ProgramBBBB" );

        programIndicatorA = createProgramIndicator( 'A', programA, "expressionA", "filterA" );
        programIndicatorB = createProgramIndicator( 'B', programB, "expressionB", "filterB" );

        programIndicatorA.setUid( "ProgramIndA" );
        programIndicatorB.setUid( "ProgramIndB" );

        trackedEntityAttributeA = createTrackedEntityAttribute( 'A' );

        trackedEntityAttributeA.setUid( "trackEntAtA" );

        programAttributeA = new ProgramTrackedEntityAttributeDimensionItem( programA, trackedEntityAttributeA );
        programAttributeB = new ProgramTrackedEntityAttributeDimensionItem( programB, trackedEntityAttributeA );

        programAttributeA.setUid( programA.getUid() + "." + trackedEntityAttributeA.getUid() );
        programAttributeA.setUid( programB.getUid() + "." + trackedEntityAttributeA.getUid() );

        items = Sets.newHashSet(
            // DataValues (always with AOC):
            dataElementA,
            dataElementOperandA,
            dataElementOperandB,
            dataElementOperandX,
            // Analytics with AOC:
            programIndicatorA,
            programIndicatorB,
            trackedEntityAttributeA,
            // Analytics without AOC:
            programAttributeA,
            programAttributeB );

        dataValueQueryPeriods = Sets.newHashSet( periodA, periodB, periodC );
        analyticsQueryPeriods = Sets.newHashSet( periodA, periodB );
        existingOutputPeriods = Sets.newHashSet( periodC );

        outputDataElementOperand = dataElementOperandX;

        when( dataValueFetcher.setIncludeDeleted( true ) ).thenReturn( dataValueFetcher );
        when( dataValueFetcher.setIncludeDescendants( INCLUDE_DESCENDANTS ) ).thenReturn( dataValueFetcher );

        consolidator = new PredictionDataConsolidator( items, INCLUDE_DESCENDANTS, dataValueFetcher, analyticsFetcher );

        consolidator.setAnalyticsBatchFetchSize( 3 );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetData()
    {
        when( categoryService.getCategoryOptionCombo( cocA.getId() ) ).thenReturn( cocA );
        when( categoryService.getCategoryOptionCombo( cocB.getId() ) ).thenReturn( cocB );
        when( categoryService.getCategoryOptionCombo( aocC.getId() ) ).thenReturn( aocC );
        when( categoryService.getCategoryOptionCombo( aocD.getId() ) ).thenReturn( aocD );

        // ---------------------------------------------------------------------
        // Test strategy
        // ---------------------------------------------------------------------

        // Test data returned by orgUnit (including descendants):
        // orgUnitA: No data
        // orgUnitB: AOC data (DataValues) + predictor values (DataValues)
        // orgUnitC: AOC data (DataValues + Analytics)
        // orgUnitD: AOC data (Analytics) + non-AOC data (Analytics)

        // ---------------------------------------------------------------------
        // Mock dataValueFetcher calls
        // ---------------------------------------------------------------------

        PredictionData mockDataValues1 = new PredictionData(
            orgUnitB,
            Lists.newArrayList( foundValueA, foundValueC, foundValueD, foundValueE ),
            Lists.newArrayList( dataValueX, dataValueY ) );

        PredictionData mockDataValues2 = new PredictionData(
            orgUnitC,
            Lists.newArrayList( foundValueB ),
            Collections.emptyList() );

        Queue<PredictionData> mockDataValues = new ArrayDeque<>(
            Lists.newArrayList( mockDataValues1, mockDataValues2 ) );

        when( dataValueFetcher.getData() ).thenAnswer( p -> {
            return mockDataValues.peek() == null ? null : mockDataValues.poll();
        } );

        // ---------------------------------------------------------------------
        // Mock analyticsFetcher calls
        // ---------------------------------------------------------------------

        List<OrganisationUnit> orgUnitsBCA = Lists.newArrayList( orgUnitB, orgUnitC, orgUnitA );
        List<OrganisationUnit> orgUnitsD = Lists.newArrayList( orgUnitD );

        List<FoundDimensionItemValue> analyticsValuesBCA = Lists.newArrayList(
            foundValueF, foundValueG, foundValueH );

        List<FoundDimensionItemValue> analyticsValuesD = Lists.newArrayList(
            foundValueI, foundValueJ, foundValueK, foundValueL );

        when( analyticsFetcher.getValues( orgUnitsBCA ) )
            .thenReturn( analyticsValuesBCA );

        when( analyticsFetcher.getValues( orgUnitsD ) )
            .thenReturn( analyticsValuesD );

        // ---------------------------------------------------------------------
        // Define expected data
        // ---------------------------------------------------------------------

        PredictionData expectedB = new PredictionData(
            orgUnitB,
            Lists.newArrayList( foundValueA, foundValueC, foundValueD, foundValueE,
                foundValueF, foundValueG, foundValueH ),
            Lists.newArrayList( dataValueX, dataValueY ) );

        PredictionData expectedC = new PredictionData(
            orgUnitC,
            Lists.newArrayList( foundValueB ),
            Collections.emptyList() );

        PredictionData expectedA = new PredictionData(
            orgUnitA,
            Collections.emptyList(),
            Collections.emptyList() );

        PredictionData expectedD = new PredictionData(
            orgUnitD,
            Lists.newArrayList( foundValueI, foundValueJ, foundValueK, foundValueL ),
            Collections.emptyList() );

        // ---------------------------------------------------------------------
        // Test the data
        // ---------------------------------------------------------------------

        consolidator.init( currentUserOrgUnits, 1, levelOneOrgUnits,
            dataValueQueryPeriods, analyticsQueryPeriods, existingOutputPeriods, outputDataElementOperand );

        // Expected to be returned in this order:
        assertEquals( expectedB, consolidator.getData() );
        assertEquals( expectedC, consolidator.getData() );
        assertEquals( expectedA, consolidator.getData() );
        assertEquals( expectedD, consolidator.getData() );
        assertNull( consolidator.getData() );
    }
}
