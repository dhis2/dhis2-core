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
package org.hisp.dhis.dataset;

import static java.util.Arrays.asList;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataentryform.DataEntryForm;
import org.hisp.dhis.dataentryform.DataEntryFormService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitStore;
import org.hisp.dhis.period.PeriodType;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Kristian Nordal
 * @version $Id: DataSetStoreTest.java 3451 2007-07-09 12:28:19Z torgeilo $
 */
public class DataSetStoreTest
    extends DhisSpringTest
{
    private static final PeriodType PERIOD_TYPE = PeriodType.getAvailablePeriodTypes().iterator().next();

    @Autowired
    private DataSetStore dataSetStore;

    @Autowired
    private DataEntryFormService dataEntryFormService;

    @Autowired
    protected OrganisationUnitStore unitStore;

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void assertEq( char uniqueCharacter, DataSet dataSet )
    {
        assertEquals( "DataSet" + uniqueCharacter, dataSet.getName() );
        assertEquals( "DataSetShort" + uniqueCharacter, dataSet.getShortName() );
        assertEquals( PERIOD_TYPE, dataSet.getPeriodType() );
    }

    // -------------------------------------------------------------------------
    // DataSet
    // -------------------------------------------------------------------------

    @Test
    public void testAddDataSet()
    {
        DataSet dataSetA = addDataSet( 'A' );
        DataSet dataSetB = addDataSet( 'B' );

        assertEq( 'A', dataSetStore.get( dataSetA.getId() ) );
        assertEq( 'B', dataSetStore.get( dataSetB.getId() ) );
    }

    @Test
    public void testUpdateDataSet()
    {
        DataSet dataSetA = addDataSet( 'A' );
        assertEq( 'A', dataSetStore.get( dataSetA.getId() ) );

        dataSetA.setName( "DataSetB" );
        dataSetStore.update( dataSetA );

        assertEquals( "DataSetB", dataSetStore.get( dataSetA.getId() ).getName() );
    }

    @Test
    public void testDeleteAndGetDataSet()
    {
        DataSet dataSetA = addDataSet( 'A' );
        DataSet dataSetB = addDataSet( 'B' );

        assertNotNull( dataSetStore.get( dataSetA.getId() ) );
        assertNotNull( dataSetStore.get( dataSetB.getId() ) );

        dataSetStore.delete( dataSetA );

        assertNull( dataSetStore.get( dataSetA.getId() ) );
        assertNotNull( dataSetStore.get( dataSetB.getId() ) );
    }

    @Test
    public void testGetDataSetByName()
    {
        DataSet dataSetA = addDataSet( 'A' );
        DataSet dataSetB = addDataSet( 'B' );

        assertEquals( dataSetA.getId(), dataSetStore.getByName( "DataSetA" ).getId() );
        assertEquals( dataSetB.getId(), dataSetStore.getByName( "DataSetB" ).getId() );
        assertNull( dataSetStore.getByName( "DataSetC" ) );
    }

    @Test
    public void testGetAllDataSets()
    {
        DataSet dataSetA = addDataSet( 'A' );
        DataSet dataSetB = addDataSet( 'B' );

        assertContainsOnly( dataSetStore.getAll(), dataSetA, dataSetB );
    }

    @Test
    public void testGetDataSetByPeriodType()
    {
        List<PeriodType> types = PeriodType.getAvailablePeriodTypes();
        PeriodType periodType1 = types.get( 0 );
        PeriodType periodType2 = types.get( 1 );
        DataSet dataSetA = addDataSet( 'A', periodType1 );
        DataSet dataSetB = addDataSet( 'B', periodType2 );

        assertContainsOnly( dataSetStore.getDataSetsByPeriodType( periodType1 ), dataSetA );
        assertContainsOnly( dataSetStore.getDataSetsByPeriodType( periodType2 ), dataSetB );
    }

    @Test
    public void testGetByDataEntryForm()
    {
        DataSet dataSetA = addDataSet( 'A' );
        DataSet dataSetB = addDataSet( 'B' );
        DataSet dataSetC = addDataSet( 'C' );

        DataEntryForm dataEntryFormX = addDataEntryForm( 'X', dataSetA );
        DataEntryForm dataEntryFormY = addDataEntryForm( 'Y' );

        assertContainsOnly( dataSetStore.getDataSetsByDataEntryForm( dataEntryFormX ), dataSetA );

        dataSetC.setDataEntryForm( dataEntryFormX );
        dataSetStore.update( dataSetC );

        assertContainsOnly( dataSetStore.getDataSetsByDataEntryForm( dataEntryFormX ), dataSetA, dataSetC );

        dataSetB.setDataEntryForm( dataEntryFormY );
        dataSetStore.update( dataSetB );

        assertContainsOnly( dataSetStore.getDataSetsByDataEntryForm( dataEntryFormY ), dataSetB );
    }

    @Test
    public void testGetDataSetsNotAssignedToOrganisationUnits()
    {
        OrganisationUnit unitX = addOrganisationUnit( 'X' );
        DataSet dataSetA = addDataSet( 'A' );
        addDataSet( 'B', unitX );
        DataSet dataSetC = addDataSet( 'C' );

        assertContainsOnly( dataSetStore.getDataSetsNotAssignedToOrganisationUnits(), dataSetA, dataSetC );
    }

    private OrganisationUnit addOrganisationUnit( char uniqueCharacter )
    {
        OrganisationUnit unit = createOrganisationUnit( uniqueCharacter );
        unitStore.save( unit );
        return unit;
    }

    private DataSet addDataSet( char uniqueCharacter, OrganisationUnit... sources )
    {
        return addDataSet( uniqueCharacter, PERIOD_TYPE, sources );
    }

    private DataSet addDataSet( char uniqueCharacter, PeriodType periodType, OrganisationUnit... sources )
    {
        DataSet dataSet = createDataSet( uniqueCharacter, periodType );
        if ( sources.length > 0 )
        {
            dataSet.setSources( new HashSet<>( asList( sources ) ) );
        }
        dataSetStore.save( dataSet );
        return dataSet;
    }

    private DataEntryForm addDataEntryForm( char uniqueCharacter )
    {
        return addDataEntryForm( uniqueCharacter, null );
    }

    private DataEntryForm addDataEntryForm( char uniqueCharacter, DataSet dataSet )
    {
        DataEntryForm form = createDataEntryForm( uniqueCharacter );
        dataEntryFormService.addDataEntryForm( form );
        if ( dataSet != null )
        {
            dataSet.setDataEntryForm( form );
            dataSetStore.update( dataSet );
        }
        return form;
    }
}
