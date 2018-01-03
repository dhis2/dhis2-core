package org.hisp.dhis.dataentryform;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.dataset.DataSetService;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Bharath
 */
public class DataEntryFormStoreTest
    extends DhisSpringTest
{
    @Autowired
    private PeriodStore periodStore;

    @Autowired
    private DataSetService dataSetService;

    @Autowired
    private DataEntryFormStore dataEntryFormStore;

    @Autowired
    private IdentifiableObjectManager manager;

    private PeriodType periodType;

    @Override
    public void setUpTest()
        throws Exception
    {
        periodType = periodStore.getAllPeriodTypes().iterator().next();
    }

    // -------------------------------------------------------------------------
    // DataEntryForm
    // -------------------------------------------------------------------------

    @Test
    public void testAddDataEntryForm()
    {
        DataSet dataSetA = createDataSet( 'A', periodType );

        dataSetService.addDataSet( dataSetA );

        DataEntryForm dataEntryFormA = createDataEntryForm( 'A' );

        dataEntryFormStore.save( dataEntryFormA );
        int dataEntryFormAid = dataEntryFormA.getId();

        dataEntryFormA = dataEntryFormStore.get( dataEntryFormAid );

        assertEquals( dataEntryFormAid, dataEntryFormA.getId() );
        assertEquals( "DataEntryFormA", dataEntryFormA.getName() );
    }

    @Test
    public void testUpdateDataEntryForm()
    {
        DataEntryForm dataEntryForm = createDataEntryForm( 'A' );

        dataEntryFormStore.save( dataEntryForm );
        int id = dataEntryForm.getId();

        dataEntryForm = dataEntryFormStore.get( id );

        assertEquals( "DataEntryFormA", dataEntryForm.getName() );

        dataEntryForm.setName( "DataEntryFormX" );

        dataEntryFormStore.update( dataEntryForm );

        dataEntryForm = dataEntryFormStore.get( id );

        assertEquals( dataEntryForm.getName(), "DataEntryFormX" );
    }

    @Test
    public void testDeleteAndGetDataEntryForm()
    {
        DataEntryForm dataEntryForm = createDataEntryForm( 'A' );

        dataEntryFormStore.save( dataEntryForm );
        int id = dataEntryForm.getId();

        dataEntryForm = dataEntryFormStore.get( id );

        assertNotNull( dataEntryFormStore.get( id ) );

        dataEntryFormStore.delete( dataEntryFormStore.get( id ) );

        assertNull( dataEntryFormStore.get( id ) );
    }

    @Test
    public void testGetDataEntryFormByName()
    {
        DataEntryForm dataEntryForm = createDataEntryForm( 'A' );

        dataEntryFormStore.save( dataEntryForm );

        assertNotNull( dataEntryForm );

        int id = dataEntryForm.getId();

        dataEntryForm = dataEntryFormStore.get( id );


        assertEquals( dataEntryFormStore.getDataEntryFormByName( "DataEntryFormA" ), dataEntryForm );
        assertNull( dataEntryFormStore.getDataEntryFormByName( "DataEntryFormX" ) );
    }

    @Test
    public void testGetAllDataEntryForms()
    {
        DataEntryForm dataEntryFormA = createDataEntryForm( 'A' );
        DataEntryForm dataEntryFormB = createDataEntryForm( 'B' );

        dataEntryFormStore.save( dataEntryFormA );
        dataEntryFormStore.save( dataEntryFormB );

        List<DataEntryForm> dataEntryForms = dataEntryFormStore.getAll();

        assertEquals( dataEntryForms.size(), 2 );
        assertTrue( dataEntryForms.contains( dataEntryFormA ) );
        assertTrue( dataEntryForms.contains( dataEntryFormB ) );
    }

    @Test
    public void testListDistinctDataEntryFormByProgramStageIds()
    {
        DataEntryForm dataEntryFormA = createDataEntryForm( 'A' );
        DataEntryForm dataEntryFormB = createDataEntryForm( 'B' );

        dataEntryFormStore.save( dataEntryFormA );
        dataEntryFormStore.save( dataEntryFormB );

        List<Integer> programStageIds = new ArrayList<>();

        Program program = createProgram( 'A' );
        manager.save( program );

        ProgramStage programStageA = createProgramStage( 'A', program );
        programStageA.setDataEntryForm( dataEntryFormA );
        manager.save( programStageA );
        programStageIds.add( programStageA.getId() );

        ProgramStage programStageB = createProgramStage( 'B', program );
        programStageB.setDataEntryForm( dataEntryFormB );
        manager.save( programStageB );
        programStageIds.add( programStageB.getId() );

        ProgramStage programStageC = createProgramStage( 'C', program );
        programStageC.setDataEntryForm( dataEntryFormB );
        manager.save( programStageC );
        programStageIds.add( programStageC.getId() );

        assertEquals( 2, dataEntryFormStore.listDistinctDataEntryFormByProgramStageIds( programStageIds ).size() );
    }
}
