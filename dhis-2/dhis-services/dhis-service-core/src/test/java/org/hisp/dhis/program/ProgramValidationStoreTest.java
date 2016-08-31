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

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Chau Thu Tran
 */
public class ProgramValidationStoreTest
    extends DhisSpringTest
{
    @Autowired
    private ProgramValidationStore programValidationStore;

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

    private Program program;

    private ProgramStage stageA;

    private ProgramStage stageB;

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
        int psIdA = programStageService.saveProgramStage( stageA );

        stageB = new ProgramStage( "B", program );
        int psIdB = programStageService.saveProgramStage( stageB );

        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( stageA );
        programStages.add( stageB );
        program.setProgramStages( programStages );
        programService.updateProgram( program );

        DataElement dataElementA = createDataElement( 'A' );
        DataElement dataElementB = createDataElement( 'B' );

        int deIdA = dataElementService.addDataElement( dataElementA );
        int deIdB = dataElementService.addDataElement( dataElementB );

        ProgramStageDataElement stageDataElementA = new ProgramStageDataElement( stageA, dataElementA, false, 1 );
        ProgramStageDataElement stageDataElementB = new ProgramStageDataElement( stageA, dataElementB, false, 2 );
        ProgramStageDataElement stageDataElementC = new ProgramStageDataElement( stageB, dataElementA, false, 1 );
        ProgramStageDataElement stageDataElementD = new ProgramStageDataElement( stageB, dataElementB, false, 2 );

        programStageDataElementService.addProgramStageDataElement( stageDataElementA );
        programStageDataElementService.addProgramStageDataElement( stageDataElementB );
        programStageDataElementService.addProgramStageDataElement( stageDataElementC );
        programStageDataElementService.addProgramStageDataElement( stageDataElementD );

        ProgramExpression programExpressionA = new ProgramExpression( "["
            + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT + psIdA + "."
            + deIdA + "]", "A" );
        ProgramExpression programExpressionB = new ProgramExpression( "["
            + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT + psIdA + "."
            + deIdB + "]", "B" );

        ProgramExpression programExpressionC = new ProgramExpression( "["
            + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT + psIdB + "."
            + deIdA + "]", "C" );
        ProgramExpression programExpressionD = new ProgramExpression( "["
            + ProgramExpression.OBJECT_PROGRAM_STAGE_DATAELEMENT + ProgramExpression.SEPARATOR_OBJECT + psIdB + "."
            + deIdB + "]", "D" );

        validationA = new ProgramValidation( "A", programExpressionA, programExpressionB, program );
        validationA.setOperator( Operator.valueOf( "equal_to" ) );

        validationB = new ProgramValidation( "B", programExpressionC, programExpressionD, program );
        validationB.setOperator( Operator.valueOf( "greater_than" ) );
    }

    @Test
    public void testGetProgramValidationByProgram()
    {
        programValidationStore.save( validationA );
        programValidationStore.save( validationB );

        assertTrue( equals( programValidationStore.get( program ), validationA, validationB ) );
    }
}
