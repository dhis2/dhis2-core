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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.category.CategoryService;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementOperand;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.datavalue.DeflatedDataValue;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.Period;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Tests PredictionDataValueFetcher.
 * <p>
 * There are two threads calling PredictionDataValueFetcher: the producer thread
 * "from below" calling handle() to supply data values from the database, and
 * the consumer thread "from above" calling getDataValues() to retrieve those
 * values.
 * <p>
 * To test that the synchronization inside PredictionDataValueFetcher is working
 * between these two threads, there are two tests. One simulates a "slow"
 * database, where the producer calls to handle() wait until the consumer calls
 * getDataValues(). The other simulates a "fast" database, where the consumer
 * calls to getDataValues() wait until the producer has called handle().
 * <p>
 * Both of these tests call getDataValues() for organisation units before,
 * between, and after organisation units where data actually exists.
 *
 * @author Jim Grace
 */
public class PredictionDataValueFetcherTest
    extends DhisConvenienceTest
{
    @Mock
    private DataValueService dataValueService;

    @Mock
    private CategoryService categoryService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DataElement dataElementA;

    private DataElement dataElementB;

    private Set<DataElement> dataElements;

    private CategoryOptionCombo cocA;

    private CategoryOptionCombo cocB;

    private CategoryOptionCombo aocC;

    private CategoryOptionCombo aocD;

    private DataElementOperand dataElementOperandA;

    private DataElementOperand dataElementOperandB;

    private Set<DataElementOperand> dataElementOperands;

    private OrganisationUnit orgUnitA;

    private OrganisationUnit orgUnitB;

    private OrganisationUnit orgUnitC;

    private OrganisationUnit orgUnitD;

    private OrganisationUnit orgUnitE;

    private List<OrganisationUnit> orgUnits;

    private Period periodA;

    private Period periodB;

    private Set<Period> periods;

    private DataValue dataValueA;

    private DataValue dataValueB;

    private DataValue dataValueC;

    private Semaphore semaphore;

    PredictionDataValueFetcher fetcher;

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    @Before
    public void initTest()
    {
        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );

        dataElementA.setId( 1 );
        dataElementB.setId( 2 );

        dataElements = Sets.newHashSet( dataElementA, dataElementB );

        cocA = createCategoryOptionCombo( 'A' );
        cocB = createCategoryOptionCombo( 'B' );

        cocA.setId( 3 );
        cocB.setId( 4 );

        aocC = createCategoryOptionCombo( 'C' );
        aocD = createCategoryOptionCombo( 'D' );

        aocC.setId( 5 );
        aocD.setId( 6 );

        dataElementOperandA = new DataElementOperand( dataElementA, cocA );
        dataElementOperandB = new DataElementOperand( dataElementB, cocA );
        dataElementOperands = Sets.newHashSet( dataElementOperandA, dataElementOperandB );

        orgUnitA = createOrganisationUnit( "A" );
        orgUnitB = createOrganisationUnit( "B" );
        orgUnitC = createOrganisationUnit( "C" );
        orgUnitD = createOrganisationUnit( "D" );
        orgUnitE = createOrganisationUnit( "E" );

        orgUnitA.setId( 7 );
        orgUnitB.setId( 8 );
        orgUnitC.setId( 9 );
        orgUnitD.setId( 10 );
        orgUnitE.setId( 11 );

        orgUnitA.setUid( "orgUnitAuid" );
        orgUnitB.setUid( "orgUnitBuid" );
        orgUnitC.setUid( "orgUnitCuid" );
        orgUnitD.setUid( "orgUnitDuid" );
        orgUnitE.setUid( "orgUnitEuid" );

        orgUnits = Lists.newArrayList( orgUnitA, orgUnitB, orgUnitC, orgUnitD, orgUnitE );

        periodA = createPeriod( "202001" );
        periodB = createPeriod( "202002" );

        periods = Sets.newHashSet( periodA, periodB );

        periodA.setId( 12 );
        periodB.setId( 13 );

        dataValueA = new DataValue( dataElementA, periodA, orgUnitB, cocA, aocC, "10", "X", null, null, null, false );
        dataValueB = new DataValue( dataElementA, periodA, orgUnitD, cocA, aocD, "20", "Y", null, null, null, true );
        dataValueC = new DataValue( dataElementB, periodB, orgUnitD, cocB, aocC, "30", "Y", null, null, null, false );

        semaphore = new Semaphore( 1 );

        semaphore.acquireUninterruptibly();

        fetcher = new PredictionDataValueFetcher( dataValueService, categoryService );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test( expected = RuntimeException.class )
    public void testOrgUnitsOutOfOrder()
    {
        when( dataValueService.getDeflatedDataValues( any() ) ).thenAnswer( p -> unsynchronizedDb() );

        fetcher.init( new HashSet<>(), 1, orgUnits, periods, dataElements, dataElementOperands );

        fetcher.getDataValues( orgUnitC );
    }

    @Test
    public void testNoDataValues()
    {
        when( dataValueService.getDeflatedDataValues( any() ) ).thenAnswer( p -> emptyDb() );

        fetcher.init( new HashSet<>(), 1, orgUnits, periods, dataElements, dataElementOperands );

        assertEquals( 0, fetcher.getDataValues( orgUnitA ).size() );
        assertEquals( 0, fetcher.getDataValues( orgUnitB ).size() );
        assertEquals( 0, fetcher.getDataValues( orgUnitC ).size() );
        assertEquals( 0, fetcher.getDataValues( orgUnitD ).size() );
        assertEquals( 0, fetcher.getDataValues( orgUnitE ).size() );
    }

    @Test( expected = NullPointerException.class )
    public void testProducerException()
    {
        try
        {
            when( dataValueService.getDeflatedDataValues( any() ) ).thenAnswer( p -> {
                throw new NullPointerException();
            } );

            fetcher.init( new HashSet<>(), 1, new ArrayList<>(), new HashSet<>(), new HashSet<>(), new HashSet<>() );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Exception happened too soon: " + ex.toString() );
        }

        fetcher.getDataValues( orgUnitA );
    }

    @Test( expected = RuntimeException.class )
    public void testTimeoutWaitingForProducer()
    {
        try
        {
            fetcher.setSemaphoreTimeout( 2, TimeUnit.MILLISECONDS );

            when( dataValueService.getDeflatedDataValues( any() ) ).thenAnswer( p -> {
                Thread.sleep( 100 );
                return null;
            } );

            fetcher.init( new HashSet<>(), 1, new ArrayList<>(), new HashSet<>(), new HashSet<>(), new HashSet<>() );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Exception happened too soon: " + ex.toString() );
        }

        fetcher.getDataValues( orgUnitA );
    }

    @Test( expected = RuntimeException.class )
    public void testTimeoutWaitingForConsumer()
    {
        try
        {
            fetcher.setSemaphoreTimeout( 2, TimeUnit.MILLISECONDS );

            when( dataValueService.getDeflatedDataValues( any() ) ).thenAnswer( p -> unsynchronizedDb() );

            fetcher.init( new HashSet<>(), 1, orgUnits, periods, dataElements, dataElementOperands );

            fetcher.getDataValues( orgUnitA );

            Thread.sleep( 100 );
        }
        catch ( Exception ex )
        {
            throw new RuntimeException( "Exception happened too soon: " + ex.toString() );
        }

        fetcher.getDataValues( orgUnitB );
    }

    // -------------------------------------------------------------------------
    // Supportive Methods
    // -------------------------------------------------------------------------

    /**
     * Returns the database values without trying to synchronize with the test
     * routine.
     *
     * @return null (return value is not needed when callback is used)
     */
    private List<DeflatedDataValue> unsynchronizedDb()
    {
        fetcher.consume( makeDeflatedDataValue( dataValueA ) );
        fetcher.consume( makeDeflatedDataValue( dataValueB ) );
        fetcher.consume( makeDeflatedDataValue( dataValueC ) );

        return null;
    }

    private DeflatedDataValue makeDeflatedDataValue( DataValue dv )
    {
        DeflatedDataValue ddv = new DeflatedDataValue( dv );

        ddv.setSourcePath( dv.getSource().getPath() );

        return ddv;
    }

    /**
     * Simulates an empty database where no values are returned.
     *
     * @return null (return value is not needed when callback is used)
     */
    private List<DeflatedDataValue> emptyDb()
    {
        return null;
    }
}
