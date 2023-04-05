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
package org.hisp.dhis.predictor;

import static org.hisp.dhis.datavalue.DataValueStore.END_OF_DDV_DATA;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.common.FoundDimensionItemValue;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataExportParams;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Tests PredictionDataValueFetcher.
 *
 * @author Jim Grace
 */
@ExtendWith( MockitoExtension.class )
class PredictionDataValueFetcherTest
    extends DhisConvenienceTest
{
    @Mock
    private DataValueService dataValueService;

    @Mock
    private CategoryService categoryService;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private DataElement dataElementX;

    private Set<DataElement> dataElements;

    private CategoryOptionCombo cocA;

    private CategoryOptionCombo cocB;

    private CategoryOptionCombo aocC;

    private CategoryOptionCombo aocD;

    private DataElementOperand dataElementOperandA;

    private DataElementOperand dataElementOperandB;

    private DataElementOperand dataElementOperandAB;

    private DataElementOperand dataElementOperandX;

    private DataElementOperand dataElementOperandZ;

    private Set<DataElementOperand> dataElementOperands;

    private Period periodA;

    private Period periodB;

    private Period periodC;

    private Set<Period> queryPeriods;

    private Set<Period> outputPeriods;

    private OrganisationUnit orgUnitA;

    private OrganisationUnit orgUnitB;

    private OrganisationUnit orgUnitC;

    private OrganisationUnit orgUnitD;

    private OrganisationUnit orgUnitE;

    private OrganisationUnit orgUnitF;

    private OrganisationUnit orgUnitG;

    private Set<OrganisationUnit> currentUserOrgUnits;

    private List<OrganisationUnit> levelOneOrgUnits;

    private DataValue dataValueA;

    private DataValue dataValueB;

    private DataValue dataValueAB;

    private DataValue dataValueC;

    private DataValue dataValueD;

    private DataValue dataValueW;

    private DataValue dataValueX;

    private DataValue dataValueY;

    private DataValue dataValueZ;

    private DeflatedDataValue deflatedDataValueA;

    private DeflatedDataValue deflatedDataValueB;

    private DeflatedDataValue deflatedDataValueAB;

    private DeflatedDataValue deflatedDataValueC;

    private DeflatedDataValue deflatedDataValueD;

    private DeflatedDataValue deflatedDataValueW;

    private DeflatedDataValue deflatedDataValueX;

    private DeflatedDataValue deflatedDataValueY;

    private DeflatedDataValue deflatedDataValueZ;

    private FoundDimensionItemValue foundValueA;

    private FoundDimensionItemValue foundValueB;

    private FoundDimensionItemValue foundValueAB;

    private FoundDimensionItemValue foundValueC;

    private FoundDimensionItemValue foundValueD;

    private FoundDimensionItemValue foundValueE;

    private PredictionDataValueFetcher fetcher;

    private final static int ORG_UNIT_LEVEl = 1;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @BeforeEach
    void initTest()
    {
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );
        dataElementX = createDataElement( 'X' );

        dataElementA.setId( 1 );
        dataElementB.setId( 2 );
        dataElementX.setId( 3 );

        dataElements = Sets.newHashSet( dataElementA, dataElementB );

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
        dataElementOperandAB = new DataElementOperand( dataElementA, cocB );
        dataElementOperandX = new DataElementOperand( dataElementX, cocA );
        dataElementOperandZ = new DataElementOperand( dataElementX, null );

        dataElementOperands = Sets.newHashSet( dataElementOperandA, dataElementOperandB, dataElementOperandAB,
            dataElementOperandX );

        periodA = createPeriod( "202201" );
        periodB = createPeriod( "202202" );
        periodC = createPeriod( "202203" );

        periodA.setUid( "Perio202201" );
        periodB.setUid( "Perio202202" );
        periodC.setUid( "Perio202203" );

        periodA.setId( 10 );
        periodB.setId( 11 );
        periodC.setId( 12 );

        queryPeriods = Sets.newHashSet( periodA, periodB, periodC );

        outputPeriods = Sets.newHashSet( periodC );

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

        dataValueA = new DataValue( dataElementA, periodA, orgUnitB, cocA, aocC, "10.0", "Y", null, null, null, false );
        dataValueB = new DataValue( dataElementA, periodA, orgUnitB, cocB, aocC, "15.0", "Y", null, null, null, false );
        dataValueW = new DataValue( dataElementX, periodC, orgUnitB, cocB, aocD, "17.0", "Z", null, null, null, false );
        dataValueX = new DataValue( dataElementX, periodA, orgUnitB, cocA, aocD, "30.0", "Z", null, null, null, false );
        dataValueY = new DataValue( dataElementX, periodC, orgUnitB, cocA, aocC, "40.0", "Z", null, null, null, true );
        dataValueZ = new DataValue( dataElementX, periodC, orgUnitE, cocA, aocC, "50.0", "Z", null, null, null, false );
        dataValueC = new DataValue( dataElementB, periodB, orgUnitC, cocA, aocC, "18.0", "Y", null, null, null, false );
        dataValueD = new DataValue( dataElementB, periodB, orgUnitC, cocB, aocC, "20.0", "Y", null, null, null, true );
        dataValueAB = new DataValue( dataElementA, periodA, orgUnitB, cocB, aocC, "60.0", "Y", null, null, null,
            false );

        deflatedDataValueA = new DeflatedDataValue( dataValueA );
        deflatedDataValueB = new DeflatedDataValue( dataValueB );
        deflatedDataValueW = new DeflatedDataValue( dataValueW );
        deflatedDataValueX = new DeflatedDataValue( dataValueX );
        deflatedDataValueY = new DeflatedDataValue( dataValueY );
        deflatedDataValueZ = new DeflatedDataValue( dataValueZ );
        deflatedDataValueC = new DeflatedDataValue( dataValueC );
        deflatedDataValueD = new DeflatedDataValue( dataValueD );
        deflatedDataValueAB = new DeflatedDataValue( dataValueAB );

        foundValueA = new FoundDimensionItemValue( orgUnitB, periodA, aocC, dataElementA, 85.0 );
        foundValueB = new FoundDimensionItemValue( orgUnitC, periodB, aocC, dataElementB, 18.0 );
        foundValueC = new FoundDimensionItemValue( orgUnitB, periodA, aocC, dataElementOperandA, 10.0 );
        foundValueD = new FoundDimensionItemValue( orgUnitB, periodA, aocD, dataElementOperandX, 30.0 );
        foundValueE = new FoundDimensionItemValue( orgUnitB, periodC, aocC, dataElementOperandX, 50.0 );
        foundValueAB = new FoundDimensionItemValue( orgUnitB, periodA, aocC, dataElementOperandAB, 75.0 );

        fetcher = new PredictionDataValueFetcher( dataValueService, categoryService, currentUserOrgUnits );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void testGetDataValues()
    {
        when( categoryService.getCategoryOptionCombo( cocA.getId() ) ).thenReturn( cocA );
        when( categoryService.getCategoryOptionCombo( cocB.getId() ) ).thenReturn( cocB );
        when( categoryService.getCategoryOptionCombo( aocC.getId() ) ).thenReturn( aocC );
        when( categoryService.getCategoryOptionCombo( aocD.getId() ) ).thenReturn( aocD );

        when( dataValueService.getDeflatedDataValues( any( DataExportParams.class ) ) ).thenAnswer( p -> {
            BlockingQueue<DeflatedDataValue> blockingQueue = ((DataExportParams) p.getArgument( 0 )).getBlockingQueue();
            blockingQueue.put( deflatedDataValueA );
            blockingQueue.put( deflatedDataValueAB );
            blockingQueue.put( deflatedDataValueB );
            blockingQueue.put( deflatedDataValueX );
            blockingQueue.put( deflatedDataValueY );
            blockingQueue.put( deflatedDataValueZ );
            blockingQueue.put( deflatedDataValueC );
            blockingQueue.put( deflatedDataValueD );
            blockingQueue.put( END_OF_DDV_DATA );
            return new ArrayList<>();
        } );

        fetcher.init( ORG_UNIT_LEVEl, levelOneOrgUnits, queryPeriods, outputPeriods,
            dataElements, dataElementOperands, dataElementOperandX );

        PredictionData data1 = fetcher.getData();
        assertNotNull( data1 );
        assertEquals( orgUnitB, data1.getOrgUnit() );
        assertContainsOnly( List.of( foundValueA, foundValueAB, foundValueC, foundValueD, foundValueE ),
            data1.getValues() );
        assertContainsOnly( List.of( dataValueY ), data1.getOldPredictions() );

        PredictionData data2 = fetcher.getData();
        assertNotNull( data2 );
        assertEquals( orgUnitC, data2.getOrgUnit() );
        assertContainsOnly( List.of( foundValueB ), data2.getValues() );
        assertTrue( data2.getOldPredictions().isEmpty() );

        PredictionData data3 = fetcher.getData();
        assertNull( data3 );

        PredictionData data4 = fetcher.getData();
        assertNull( data4 );
    }

    @Test
    void testGetDataValuesWithAllDisaggregations()
    {
        when( categoryService.getCategoryOptionCombo( cocA.getId() ) ).thenReturn( cocA );
        when( categoryService.getCategoryOptionCombo( cocB.getId() ) ).thenReturn( cocB );
        when( categoryService.getCategoryOptionCombo( aocC.getId() ) ).thenReturn( aocC );
        when( categoryService.getCategoryOptionCombo( aocD.getId() ) ).thenReturn( aocD );

        when( dataValueService.getDeflatedDataValues( any( DataExportParams.class ) ) ).thenAnswer( p -> {
            BlockingQueue<DeflatedDataValue> blockingQueue = ((DataExportParams) p.getArgument( 0 )).getBlockingQueue();
            blockingQueue.put( deflatedDataValueA );
            blockingQueue.put( deflatedDataValueAB );
            blockingQueue.put( deflatedDataValueB );
            blockingQueue.put( deflatedDataValueW );
            blockingQueue.put( deflatedDataValueX );
            blockingQueue.put( deflatedDataValueY );
            blockingQueue.put( deflatedDataValueZ );
            blockingQueue.put( deflatedDataValueC );
            blockingQueue.put( deflatedDataValueD );
            blockingQueue.put( END_OF_DDV_DATA );
            return new ArrayList<>();
        } );

        fetcher.init( ORG_UNIT_LEVEl, levelOneOrgUnits, queryPeriods, outputPeriods,
            dataElements, dataElementOperands, dataElementOperandZ );

        PredictionData data1 = fetcher.getData();
        assertNotNull( data1 );
        assertEquals( orgUnitB, data1.getOrgUnit() );
        assertContainsOnly( List.of( foundValueA, foundValueAB, foundValueC, foundValueD, foundValueE ),
            data1.getValues() );
        assertContainsOnly( List.of( dataValueW, dataValueY ), data1.getOldPredictions() );

        PredictionData data2 = fetcher.getData();
        assertNotNull( data2 );
        assertEquals( orgUnitC, data2.getOrgUnit() );
        assertContainsOnly( List.of( foundValueB ), data2.getValues() );
        assertTrue( data2.getOldPredictions().isEmpty() );

        PredictionData data3 = fetcher.getData();
        assertNull( data3 );
    }

    @Test
    void testNoDataValues()
    {
        when( dataValueService.getDeflatedDataValues( any( DataExportParams.class ) ) ).thenAnswer( p -> {
            BlockingQueue<DeflatedDataValue> blockingQueue = ((DataExportParams) p.getArgument( 0 )).getBlockingQueue();
            blockingQueue.put( END_OF_DDV_DATA );
            return new ArrayList<>();
        } );

        fetcher.init( ORG_UNIT_LEVEl, levelOneOrgUnits, queryPeriods, outputPeriods,
            dataElements, dataElementOperands, dataElementOperandX );

        assertNull( fetcher.getData() );
    }

    @Test
    void testProducerException()
    {
        when( dataValueService.getDeflatedDataValues( any() ) ).thenAnswer( p -> {
            throw new ArithmeticException();
        } );
        assertThrows( ArithmeticException.class, () -> fetcher.init( ORG_UNIT_LEVEl, levelOneOrgUnits,
            queryPeriods, outputPeriods, dataElements, dataElementOperands, dataElementOperandX ) );
    }
}
