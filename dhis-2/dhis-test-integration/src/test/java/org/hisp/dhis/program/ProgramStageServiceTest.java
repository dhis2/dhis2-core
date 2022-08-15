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
package org.hisp.dhis.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class ProgramStageServiceTest extends TransactionalIntegrationTest
{

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    private Program program;

    private ProgramStage stageA;

    private ProgramStage stageB;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );
        stageA = new ProgramStage( "A", program );
        stageA.setUid( "UID-A" );
        stageA.setReferral( true );
        stageB = new ProgramStage( "B", program );
        stageB.setUid( "UID-B" );
    }

    @Test
    void testSaveProgramStage()
    {
        long idA = programStageService.saveProgramStage( stageA );
        long idB = programStageService.saveProgramStage( stageB );

        assertNotNull( programStageService.getProgramStage( idA ) );

        assertNotNull( programStageService.getProgramStage( idB ) );

        ProgramStage persistedProgramStage = programStageService.getProgramStage( idA );
        assertEquals( persistedProgramStage, stageA );
        assertTrue( persistedProgramStage.isReferral() );
    }

    @Test
    void testDeleteProgramStage()
    {
        long idA = programStageService.saveProgramStage( stageA );
        long idB = programStageService.saveProgramStage( stageB );
        assertNotNull( programStageService.getProgramStage( idA ) );
        assertNotNull( programStageService.getProgramStage( idB ) );
        programStageService.deleteProgramStage( stageA );
        assertNull( programStageService.getProgramStage( idA ) );
        assertNotNull( programStageService.getProgramStage( idB ) );
        programStageService.deleteProgramStage( stageB );
        assertNull( programStageService.getProgramStage( idA ) );
        assertNull( programStageService.getProgramStage( idB ) );
    }

    @Test
    void testUpdateProgramStage()
    {
        long idA = programStageService.saveProgramStage( stageA );
        assertNotNull( programStageService.getProgramStage( idA ) );
        stageA.setName( "B" );
        programStageService.updateProgramStage( stageA );
        assertEquals( "B", programStageService.getProgramStage( idA ).getName() );
    }

    @Test
    void testGetProgramStageById()
    {
        long idA = programStageService.saveProgramStage( stageA );
        long idB = programStageService.saveProgramStage( stageB );
        assertEquals( stageA, programStageService.getProgramStage( idA ) );
        assertEquals( stageB, programStageService.getProgramStage( idB ) );
    }

    @Test
    void testGetProgramStageByUid()
    {
        programStageService.saveProgramStage( stageA );
        programStageService.saveProgramStage( stageB );
        assertEquals( stageA, programStageService.getProgramStage( "UID-A" ) );
        assertEquals( stageB, programStageService.getProgramStage( "UID-B" ) );
    }
}
