package org.hisp.dhis.programrule;

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
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @Author Zubair Asghar.
 */
public class ProgramRuleAuditServiceTest extends DhisSpringTest
{
    private static final String RULE_VARIABLE_NAME_A = "RuleVariableOne";  // ProgramRuleVariable linked to TrackedEntityAttribute
    private static final String RULE_VARIABLE_NAME_B = "RuleVariableTwo";  // ProgramRuleVariable linked to DataElement

    private static final String PROGRAM_RULE_EXPRESSION_A = "A{"+ RULE_VARIABLE_NAME_A +"} == 1 && #{"+ RULE_VARIABLE_NAME_B +"} > 0";
    private static final String PROGRAM_RULE_EXPRESSION_B = "#{"+ RULE_VARIABLE_NAME_B +"} > 0 && V{event_status} == 1";

    private Program programA;

    private ProgramStage programStageA;
    private ProgramRule programRuleA;
    private ProgramRuleAction programRuleActionA;
    private ProgramRuleAction programRuleActionB;
    private ProgramRuleVariable programRuleVariableA;
    private ProgramRuleVariable programRuleVariableB;

    private DataElement dataElement;
    private TrackedEntityAttribute attribute;

    @Autowired
    private ProgramService programService;

    @Autowired
    private ProgramStageService programStageService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActonService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private DataElementService dataElementService;

    @Autowired
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private ProgramRuleAuditService programRuleAuditService;

    @Override
    public void setUpTest()
    {
        dataElement = createDataElement( 'D' );
        attribute = createTrackedEntityAttribute( 'A' );

        dataElementService.addDataElement( dataElement );
        attributeService.addTrackedEntityAttribute( attribute );

        programA = createProgram( 'A', null, null );

        programStageA = createProgramStage( 'A', 1 );
        programStageA.setProgram( programA );
        Set<ProgramStage> stagesA = new HashSet<>();
        stagesA.add( programStageA );
        programA.setProgramStages( stagesA );

        programService.addProgram( programA );

        programStageService.saveProgramStage( programStageA );

        //Add a tree of variables, rules and actions to programA:
        programRuleA = createProgramRule( 'A', programA );
        programRuleService.addProgramRule( programRuleA );

        programRuleActionA = createProgramRuleAction( 'A', programRuleA );
        programRuleActionB = createProgramRuleAction( 'B', programRuleA );
        programRuleActonService.addProgramRuleAction( programRuleActionA );
        programRuleActonService.addProgramRuleAction( programRuleActionB );

        programRuleVariableA = createProgramRuleVariable( 'A', programA );
        programRuleVariableA.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );
        programRuleVariableA.setAttribute( attribute );
        programRuleVariableA.setName( RULE_VARIABLE_NAME_A );

        programRuleVariableB = createProgramRuleVariable( 'B', programA );
        programRuleVariableB.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );
        programRuleVariableB.setName( RULE_VARIABLE_NAME_B );
        programRuleVariableB.setDataElement( dataElement );

        programRuleVariableService.addProgramRuleVariable( programRuleVariableA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableB );
    }

    @Test
    public void testSaveProgramRuleAudit()
    {
        ProgramRule ruleA = new ProgramRule( "RuleA", "descriptionA", programA, programStageA, null, PROGRAM_RULE_EXPRESSION_A, null );
        ProgramRule ruleB = new ProgramRule( "RuleA", "descriptionA", programA, null, null, PROGRAM_RULE_EXPRESSION_B, 1 );

        int idA = programRuleService.addProgramRule( ruleA );
        int idB = programRuleService.addProgramRule( ruleB );

        ProgramRuleAudit auditA = programRuleAuditService.getProgramRuleAudit( ruleA );

        assertNotNull( auditA );
    }
}
