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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValueService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ProgramValidationServiceTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramValidationService programValidationService;

    @Autowired
    private ProgramStageInstanceService programStageInstanceService;

    @Autowired
    private ProgramStageDataElementService programStageDataElementService;

    @Autowired
    private OrganisationUnitService organisationUnitService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private TrackedEntityInstanceService entityInstanceService;

    @Autowired
    private ProgramInstanceService programInstanceService;

    @Autowired
    private TrackedEntityDataValueService dataValueService;

    private Program program;

    private ProgramStage stageA;

    private ProgramStage stageB;

    private ProgramStageDataElement stageDataElementA;

    private ProgramStageDataElement stageDataElementC;

    private ProgramStageInstance stageInstanceA;

    private ProgramStageInstance stageInstanceB;

    private ProgramValidation validationA;

    private ProgramValidation validationB;

    @Override
    public void setUpTest()
    {
        OrganisationUnit organisationUnit = createOrganisationUnit( 'A' );
        organisationUnitService.addOrganisationUnit( organisationUnit );

        program = createProgram( 'A', new HashSet<>(), organisationUnit );
        programService.addProgram( program );

        stageA = new ProgramStage( "A", program );
        programStageService.saveProgramStage( stageA );

        stageB = new ProgramStage( "B", program );
        programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );

        dataElementService.addDataElement( dataElementA );
        dataElementService.addDataElement( dataElementB );

        stageDataElementA = new ProgramStageDataElement( stageA, dataElementA, false, 1 );
        ProgramStageDataElement stageDataElementB = new ProgramStageDataElement( stageA, dataElementB, false, 2 );
        stageDataElementC = new ProgramStageDataElement( stageB, dataElementA, false, 1 );
        ProgramStageDataElement stageDataElementD = new ProgramStageDataElement( stageB, dataElementB, false, 2 );

        programStageDataElementService.addProgramStageDataElement( stageDataElementA );
        programStageDataElementService.addProgramStageDataElement( stageDataElementB );
        programStageDataElementService.addProgramStageDataElement( stageDataElementC );
        programStageDataElementService.addProgramStageDataElement( stageDataElementD );

        TrackedEntityInstance entityInstance = createTrackedEntityInstance( 'A', organisationUnit );
        entityInstanceService.addTrackedEntityInstance( entityInstance );

        ProgramInstance programInstance = programInstanceService.enrollTrackedEntityInstance( entityInstance, program,
            new Date(), new Date(), organisationUnit );

        stageInstanceA = programStageInstanceService.createProgramStageInstance( programInstance, stageA, new Date(),
            new Date(), organisationUnit );
        stageInstanceB = programStageInstanceService.createProgramStageInstance( programInstance, stageB, new Date(),
            new Date(), organisationUnit );

        Set<ProgramStageInstance> programStageInstances = new HashSet<>();
        programStageInstances.add( stageInstanceA );
        programStageInstances.add( stageInstanceB );
        programInstance.setProgramStageInstances( programStageInstances );

        TrackedEntityDataValue dataValueA = new TrackedEntityDataValue( stageInstanceA, dataElementA, "1" );
        TrackedEntityDataValue dataValueB = new TrackedEntityDataValue( stageInstanceA, dataElementB, "1" );
        TrackedEntityDataValue dataValueC = new TrackedEntityDataValue( stageInstanceB, dataElementA, "2" );
        TrackedEntityDataValue dataValueD = new TrackedEntityDataValue( stageInstanceB, dataElementB, "3" );

        dataValueService.saveTrackedEntityDataValue( dataValueA );
        dataValueService.saveTrackedEntityDataValue( dataValueB );
        dataValueService.saveTrackedEntityDataValue( dataValueC );
        dataValueService.saveTrackedEntityDataValue( dataValueD );

        ProgramExpression programExpressionA = new ProgramExpression( "["
            + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT + stageA.getUid()
            + "." + dataElementA.getUid() + "]", "A" );
        ProgramExpression programExpressionB = new ProgramExpression( "["
            + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT + stageA.getUid()
            + "." + dataElementB.getUid() + "]", "B" );

        ProgramExpression programExpressionC = new ProgramExpression( "["
            + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT + stageB.getUid()
            + "." + dataElementA.getUid() + "]", "C" );
        ProgramExpression programExpressionD = new ProgramExpression( "["
            + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT + stageB.getUid()
            + "." + dataElementB.getUid() + "]", "D" );

        validationA = new ProgramValidation( "A", programExpressionA, programExpressionB, program );
        validationA.setOperator( Operator.valueOf( "equal_to" ) );

        validationB = new ProgramValidation( "B", programExpressionC, programExpressionD, program );
        validationB.setOperator( Operator.valueOf( "greater_than" ) );
    }

    @Test
    public void testAddProgramValidation()
    {
        int idA = programValidationService.addProgramValidation( validationA );
        int idB = programValidationService.addProgramValidation( validationB );

        assertNotNull( programValidationService.getProgramValidation( idA ) );
        assertNotNull( programValidationService.getProgramValidation( idB ) );
    }

    @Test
    public void testDeleteProgramValidation()
    {
        int idA = programValidationService.addProgramValidation( validationA );
        int idB = programValidationService.addProgramValidation( validationB );

        assertNotNull( programValidationService.getProgramValidation( idA ) );
        assertNotNull( programValidationService.getProgramValidation( idB ) );

        programValidationService.deleteProgramValidation( validationA );

        assertNull( programValidationService.getProgramValidation( idA ) );
        assertNotNull( programValidationService.getProgramValidation( idB ) );

        programValidationService.deleteProgramValidation( validationB );

        assertNull( programValidationService.getProgramValidation( idA ) );
        assertNull( programValidationService.getProgramValidation( idB ) );
    }

    @Test
    public void testUpdateProgramValidation()
    {
        int idA = programValidationService.addProgramValidation( validationA );

        assertNotNull( programValidationService.getProgramValidation( idA ) );

        validationA.setName( "B" );
        programValidationService.updateProgramValidation( validationA );

        assertEquals( "B", programValidationService.getProgramValidation( idA ).getName() );
    }

    @Test
    public void testGetEntityInstancevalidationById()
    {
        int idA = programValidationService.addProgramValidation( validationA );
        int idB = programValidationService.addProgramValidation( validationB );

        assertEquals( validationA, programValidationService.getProgramValidation( idA ) );
        assertEquals( validationB, programValidationService.getProgramValidation( idB ) );
    }

    @Test
    public void testGetAllProgramValidations()
    {
        programValidationService.addProgramValidation( validationA );
        programValidationService.addProgramValidation( validationB );

        assertTrue( equals( programValidationService.getAllProgramValidation(), validationA, validationB ) );
    }

    @Test
    public void testGetProgramValidationByStage()
    {
        programValidationService.addProgramValidation( validationA );
        programValidationService.addProgramValidation( validationB );

        assertTrue( equals( programValidationService.getProgramValidation( stageA ), validationA ) );
        assertTrue( equals( programValidationService.getProgramValidation( stageB ), validationB ) );
    }

    @Test
    public void testValidate()
    {
        programValidationService.addProgramValidation( validationA );
        programValidationService.addProgramValidation( validationB );

        List<ProgramValidation> validationList = new ArrayList<>();
        validationList.add( validationA );
        validationList.add( validationB );

        List<ProgramValidationResult> result = programValidationService.validate( validationList, stageInstanceA );
        assertEquals( 1, result.size() );
        assertEquals( result.iterator().next().getProgramValidation(), validationB );
    }
}
