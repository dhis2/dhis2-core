package org.hisp.dhis.program;

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

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ProgramStageDataElementStoreTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramStageDataElementStore programStageDataElementStore;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    private OrganisationUnit organisationUnit;

    private ProgramStage stageA;

    private ProgramStage stageB;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private ProgramStageDataElement stageDataElementA;

    private ProgramStageDataElement stageDataElementB;

    @Override
    public void setUpTest()
    {
        organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        Program program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        stageA = new ProgramStage( "A", program );
        stageA.setUid( "StageA" );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );

        stageB = new ProgramStage( "B", program );
        stageB.setSortOrder( 2 );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        dataElementA = createDataElement( 'A' );
        dataElementB = createDataElement( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        stageDataElementA = new ProgramStageDataElement( stageA, dataElementA, false, 1 );
        stageDataElementB = new ProgramStageDataElement( stageA, dataElementB, false, 2 );
    }

    @Test
    public void testAddProgramStageDataElement()
    {
        programStageDataElementStore.save( stageDataElementA );
        programStageDataElementStore.save( stageDataElementB );

        assertNotNull( programStageDataElementStore.get( stageA, dataElementA ) );
        assertNotNull( programStageDataElementStore.get( stageA, dataElementB ) );
    }

    @Test
    public void testUpdateProgramStageDataElement()
    {
        programStageDataElementStore.save( stageDataElementA );

        assertNotNull( programStageDataElementStore.get( stageA, dataElementA ) );

        stageDataElementA.setCompulsory( true );
        programStageDataElementStore.update( stageDataElementA );

        assertEquals( true, programStageDataElementStore.get( stageA, dataElementA ).isCompulsory() );
    }

    @Test
    public void testDeleteProgramStageDataElement()
    {
        programStageDataElementStore.save( stageDataElementA );
        programStageDataElementStore.save( stageDataElementB );

        assertNotNull( programStageDataElementStore.get( stageA, dataElementA ) );
        assertNotNull( programStageDataElementStore.get( stageA, dataElementB ) );

        programStageDataElementStore.delete( stageDataElementA );

        assertNull( programStageDataElementStore.get( stageA, dataElementA ) );
        assertNotNull( programStageDataElementStore.get( stageA, dataElementB ) );

        programStageDataElementStore.delete( stageDataElementB );

        assertNull( programStageDataElementStore.get( stageA, dataElementA ) );
        assertNull( programStageDataElementStore.get( stageA, dataElementB ) );
    }

    @Test
    public void testGetByStageElement()
    {
        programStageDataElementStore.save( stageDataElementA );
        programStageDataElementStore.save( stageDataElementB );

        assertNotNull( programStageDataElementStore.get( stageA, dataElementA ) );
        assertNotNull( programStageDataElementStore.get( stageA, dataElementB ) );
    }

    @Test
    public void testGetAllProgramStageDataElements()
    {
        programStageDataElementStore.save( stageDataElementA );
        programStageDataElementStore.save( stageDataElementB );

        assertTrue( equals( programStageDataElementStore.getAll(), stageDataElementA, stageDataElementB ) );
    }
}