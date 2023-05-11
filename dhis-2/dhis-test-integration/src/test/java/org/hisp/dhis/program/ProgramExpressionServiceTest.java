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

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.test.integration.SingleSetupIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
class ProgramExpressionServiceTest extends SingleSetupIntegrationTestBase
{

    @Autowired
    private ProgramExpressionService programExpressionService;

    @Autowired
    private TrackedEntityService entityInstanceService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    private ProgramExpression programExpressionA;

    private ProgramExpression programExpressionB;

    private DataElement dataElementA;

    private DataElement dataElementB;

    private ProgramStage stageA;

    private ProgramStage stageB;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );
        Program program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );
        stageA = new ProgramStage( "StageA", program );
        stageA.setSortOrder( 1 );
        programStageService.saveProgramStage( stageA );
        stageB = new ProgramStage( "StageB", program );
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
        TrackedEntity entityInstance = createTrackedEntity( organisationUnit );
        entityInstanceService.addTrackedEntity( entityInstance );
        programExpressionA = new ProgramExpression( "[" + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT
            + ProgramExpression.SEPARATOR_OBJECT + stageA.getUid() + "." + dataElementA.getUid() + "]", "A" );
        programExpressionB = new ProgramExpression( "[" + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT
            + ProgramExpression.SEPARATOR_OBJECT + stageA.getUid() + "." + dataElementB.getUid() + "]", "B" );
    }

    @Test
    void testAddProgramExpression()
    {
        long idA = programExpressionService.addProgramExpression( programExpressionA );
        long idB = programExpressionService.addProgramExpression( programExpressionB );
        assertNotNull( programExpressionService.getProgramExpression( idA ) );
        assertNotNull( programExpressionService.getProgramExpression( idB ) );
    }

    @Test
    void testUpdateProgramExpression()
    {
        long idA = programExpressionService.addProgramExpression( programExpressionA );
        assertNotNull( programExpressionService.getProgramExpression( idA ) );
        programExpressionA.setDescription( "B" );
        programExpressionService.updateProgramExpression( programExpressionA );
        assertEquals( "B", programExpressionService.getProgramExpression( idA ).getDescription() );
    }

    @Test
    void testDeleteProgramExpression()
    {
        long idA = programExpressionService.addProgramExpression( programExpressionA );
        long idB = programExpressionService.addProgramExpression( programExpressionB );
        assertNotNull( programExpressionService.getProgramExpression( idA ) );
        assertNotNull( programExpressionService.getProgramExpression( idB ) );
        programExpressionService.deleteProgramExpression( programExpressionA );
        assertNull( programExpressionService.getProgramExpression( idA ) );
        assertNotNull( programExpressionService.getProgramExpression( idB ) );
        programExpressionService.deleteProgramExpression( programExpressionB );
        assertNull( programExpressionService.getProgramExpression( idA ) );
        assertNull( programExpressionService.getProgramExpression( idB ) );
    }

    @Test
    void testGetProgramExpression()
    {
        long idA = programExpressionService.addProgramExpression( programExpressionA );
        long idB = programExpressionService.addProgramExpression( programExpressionB );
        assertEquals( programExpressionA, programExpressionService.getProgramExpression( idA ) );
        assertEquals( programExpressionB, programExpressionService.getProgramExpression( idB ) );
    }

    @Test
    void testGetExpressionDescription()
    {
        programExpressionService.addProgramExpression( programExpressionA );
        String actual = programExpressionService.getExpressionDescription( programExpressionA.getExpression() );
        String expected = "[" + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT
            + "StageA.DataElementA]";
        assertEquals( expected, actual );
    }
}
