package org.hisp.dhis.dataset;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.period.PeriodType;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kristian Nordal
 * @version $Id: DataSetStoreTest.java 3451 2007-07-09 12:28:19Z torgeilo $
 */
public class DataSetStoreTest
    extends DhisSpringTest
{
    @Autowired
    private DataSetStore dataSetStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataEntryFormService dataEntryFormService;

    private PeriodType periodType;

    @Override
    public void setUpTest()
        throws Exception
    {
        periodType = PeriodType.getAvailablePeriodTypes().iterator().next();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void assertEq( char uniqueCharacter, DataSet dataSet )
    {
        assertEquals( "DataSet" + uniqueCharacter, dataSet.getName() );
        assertEquals( "DataSetShort" + uniqueCharacter, dataSet.getShortName() );
        assertEquals( periodType, dataSet.getPeriodType() );
    }
    
    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    @Test
    @Ignore
    public void testGetDataSetsBySources()
    {
        OrganisationUnit unitA = createOrganisationUnit( 'A' );
        OrganisationUnit unitB = createOrganisationUnit( 'B' );  
        OrganisationUnit unitC = createOrganisationUnit( 'C' );  
        organisationUnitService.addOrganisationUnit( unitA );
        organisationUnitService.addOrganisationUnit( unitB );
        organisationUnitService.addOrganisationUnit( unitC );

        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );
        DataSet dataSetC = createDataSet( 'C', periodType );
        DataSet dataSetD = createDataSet( 'D', periodType );
        dataSetA.getSources().add( unitA );
        dataSetA.getSources().add( unitB );
        dataSetB.getSources().add( unitA );
        dataSetC.getSources().add( unitB );
        
        dataSetStore.save( dataSetA );
        dataSetStore.save( dataSetB );
        dataSetStore.save( dataSetC );
        dataSetStore.save( dataSetD );
        
        List<OrganisationUnit> sources = new ArrayList<>();
        sources.add( unitA );
        sources.add( unitB );
        
        List<DataSet> dataSets = dataSetStore.getDataSetsBySources( sources );

        assertEquals( 3, dataSets.size() );
        assertTrue( dataSets.contains( dataSetA ) );
        assertTrue( dataSets.contains( dataSetB ) );
        assertTrue( dataSets.contains( dataSetC ) );

        sources = new ArrayList<>();
        sources.add( unitA );
        
        dataSets = dataSetStore.getDataSetsBySources( sources );
        
        assertEquals( 2, dataSets.size() );
        assertTrue( dataSets.contains( dataSetA ) );
        assertTrue( dataSets.contains( dataSetB ) );
    }
    
    @Test
    public void testAddDataSet()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );

        int idA = dataSetStore.save( dataSetA );
        int idB = dataSetStore.save( dataSetB );

        dataSetA = dataSetStore.get( idA );
        dataSetB = dataSetStore.get( idB );

        assertEquals( idA, dataSetA.getId() );
        assertEq( 'A', dataSetA );

        assertEquals( idB, dataSetB.getId() );
        assertEq( 'B', dataSetB );
    }

    @Test
    public void testUpdateDataSet()
    {
        DataSet dataSet = createDataSet( 'A', periodType );

        int id = dataSetStore.save( dataSet );

        dataSet = dataSetStore.get( id );

        assertEq( 'A', dataSet );

        dataSet.setName( "DataSetB" );

        dataSetStore.update( dataSet );

        dataSet = dataSetStore.get( id );

        assertEquals( dataSet.getName(), "DataSetB" );
    }

    @Test
    public void testDeleteAndGetDataSet()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );

        int idA = dataSetStore.save( dataSetA );
        int idB = dataSetStore.save( dataSetB );

        assertNotNull( dataSetStore.get( idA ) );
        assertNotNull( dataSetStore.get( idB ) );

        dataSetStore.delete( dataSetStore.get( idA ) );

        assertNull( dataSetStore.get( idA ) );
        assertNotNull( dataSetStore.get( idB ) );
    }

    @Test
    public void testGetDataSetByName()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );

        int idA = dataSetStore.save( dataSetA );
        int idB = dataSetStore.save( dataSetB );

        assertEquals( dataSetStore.getByName( "DataSetA" ).getId(), idA );
        assertEquals( dataSetStore.getByName( "DataSetB" ).getId(), idB );
        assertNull( dataSetStore.getByName( "DataSetC" ) );
    }

    @Test
    public void testGetDataSetByShortName()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );

        int idA = dataSetStore.save( dataSetA );
        int idB = dataSetStore.save( dataSetB );

        assertEquals( dataSetStore.getByShortName( "DataSetShortA" ).getId(), idA );
        assertEquals( dataSetStore.getByShortName( "DataSetShortB" ).getId(), idB );
        assertNull( dataSetStore.getByShortName( "DataSetShortC" ) );
    }

    @Test
    public void testGetAllDataSets()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );

        dataSetStore.save( dataSetA );
        dataSetStore.save( dataSetB );

        List<DataSet> dataSets = dataSetStore.getAll();

        assertEquals( dataSets.size(), 2 );
        assertTrue( dataSets.contains( dataSetA ) );
        assertTrue( dataSets.contains( dataSetB ) );
    }

    @Test
    public void testGetByDataEntryForm()
    {
        DataEntryForm dataEntryFormX = createDataEntryForm( 'X' );
        DataEntryForm dataEntryFormY = createDataEntryForm( 'Y' );

        dataEntryFormService.addDataEntryForm( dataEntryFormX );
        dataEntryFormService.addDataEntryForm( dataEntryFormY );

        DataSet dataSetA = createDataSet( 'A', periodType );
        DataSet dataSetB = createDataSet( 'B', periodType );
        DataSet dataSetC = createDataSet( 'C', periodType );

        dataSetA.setDataEntryForm( dataEntryFormX );

        dataSetStore.save( dataSetA );
        dataSetStore.save( dataSetB );
        dataSetStore.save( dataSetC );

        List<DataSet> dataSetsWithForm = dataSetStore.getDataSetsByDataEntryForm( dataEntryFormX );

        assertEquals( 1, dataSetsWithForm.size() );
        assertEquals( dataSetA, dataSetsWithForm.get( 0 ) );

        dataSetC.setDataEntryForm( dataEntryFormX );

        dataSetStore.update( dataSetC );

        dataSetsWithForm = dataSetStore.getDataSetsByDataEntryForm( dataEntryFormX );

        assertEquals( 2, dataSetsWithForm.size() );
        assertTrue( dataSetsWithForm.contains( dataSetA ) );
        assertTrue( dataSetsWithForm.contains( dataSetC ) );

        dataSetB.setDataEntryForm( dataEntryFormY );
        dataSetStore.update( dataSetB );

        dataSetsWithForm = dataSetStore.getDataSetsByDataEntryForm( dataEntryFormY );
        assertEquals( 1, dataSetsWithForm.size() );
        assertTrue( dataSetsWithForm.contains( dataSetB ) );
    }
}
