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

import com.google.common.collect.Sets;
import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.expression.ExpressionValidationOutcome;
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

import static org.hisp.dhis.program.ProgramIndicator.KEY_ATTRIBUTE;
import static org.hisp.dhis.program.ProgramIndicator.KEY_DATAELEMENT;
import static org.hisp.dhis.program.ProgramIndicator.KEY_PROGRAM_VARIABLE;
import static org.junit.Assert.*;

public class ProgramRuleServiceTest
    extends DhisSpringTest
{
    private Program programA;
    private Program programB;
    private Program programC;
    
    private ProgramStage programStageA;
    private ProgramRule programRuleA;
    private ProgramRuleAction programRuleActionA;
    private ProgramRuleAction programRuleActionB;
    private ProgramRuleVariable programRuleVariableA;
    private ProgramRuleVariable programRuleVariableB;
    private ProgramRuleVariable programRuleVariableC;

    private TrackedEntityAttribute attribute;
    private DataElement dataElement;
    
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
    private TrackedEntityAttributeService attributeService;

    @Autowired
    private DataElementService dataElementService;
    
    @Override
    public void setUpTest()
    {
        attribute = createTrackedEntityAttribute( 'A' );
        attribute.setValueType( ValueType.INTEGER );

        dataElement = createDataElement( 'D' );
        dataElement.setValueType( ValueType.INTEGER );

        attributeService.addTrackedEntityAttribute( attribute );
        dataElementService.addDataElement( dataElement );

        programA = createProgram( 'A', null, null );
        programB = createProgram( 'B', null, null );
        programC = createProgram( 'C', null, null );
        
        programStageA = createProgramStage( 'A', 1 );
        programStageA.setProgram( programA );
        Set<ProgramStage> stagesA = new HashSet<>();
        stagesA.add( programStageA );
        programA.setProgramStages( stagesA );
        
        programService.addProgram( programA );
        programService.addProgram( programB );
        programService.addProgram( programC );
        
        programStageService.saveProgramStage( programStageA );
        
        //Add a tree of variables, rules and actions to programA:
        programRuleA = createProgramRule( 'A', programA );
        programRuleService.addProgramRule( programRuleA );
        
        programRuleActionA = createProgramRuleAction( 'A', programRuleA );
        programRuleActionB = createProgramRuleAction( 'B', programRuleA );
        programRuleActonService.addProgramRuleAction( programRuleActionA );
        programRuleActonService.addProgramRuleAction( programRuleActionB );
        
        programRuleVariableA = createProgramRuleVariable( 'A', programA );
        programRuleVariableA.setAttribute( attribute );
        programRuleVariableA.setSourceType( ProgramRuleVariableSourceType.TEI_ATTRIBUTE );

        programRuleVariableB = createProgramRuleVariable( 'B', programA );
        programRuleVariableB.setDataElement( dataElement );
        programRuleVariableB.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT );

        programRuleVariableC = createProgramRuleVariable( 'C', programA );
        programRuleVariableC.setSourceType( ProgramRuleVariableSourceType.CALCULATED_VALUE );

        programRuleVariableService.addProgramRuleVariable( programRuleVariableA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableB );  
        programRuleVariableService.addProgramRuleVariable( programRuleVariableC );
    }
    
    @Test
    public void testAddGet()
    {
        ProgramRule ruleA = new ProgramRule( "RuleA", "descriptionA", programA, programStageA, null, "true", null );
        ProgramRule ruleB = new ProgramRule( "RuleA", "descriptionA", programA, null, null, "$a < 1", 1 );
        ProgramRule ruleC = new ProgramRule( "RuleA", "descriptionA", programA, null, null, "($a < 1 && $a > -10) && !$b", 0 );
        
        int idA = programRuleService.addProgramRule( ruleA );
        int idB = programRuleService.addProgramRule( ruleB );
        int idC = programRuleService.addProgramRule( ruleC );
        
        assertEquals( ruleA, programRuleService.getProgramRule( idA ) );
        assertEquals( ruleB, programRuleService.getProgramRule( idB ) );
        assertEquals( ruleC, programRuleService.getProgramRule( idC ) );
    }
    
    @Test
    public void testGetByProgram()
    {
        ProgramRule ruleD = new ProgramRule( "RuleD", "descriptionD", programB, null, null, "true", null );
        ProgramRule ruleE = new ProgramRule( "RuleE", "descriptionE", programB, null, null, "$a < 1", 1 );
        ProgramRule ruleF = new ProgramRule( "RuleF", "descriptionF", programB, null, null, "($a < 1 && $a > -10) && !$b", 0 );
        //Add a rule that is not part of programB....
        ProgramRule ruleG = new ProgramRule( "RuleG", "descriptionG", programA, null, null, "!false", 0 );
        
        programRuleService.addProgramRule( ruleD );
        programRuleService.addProgramRule( ruleE );
        programRuleService.addProgramRule( ruleF );
        programRuleService.addProgramRule( ruleG );
        
        //Get all the 3 rules for programB
        List<ProgramRule> rules = programRuleService.getProgramRule( programB );
        assertEquals( 3, rules.size() );
        assertTrue( rules.contains( ruleD ) );
        assertTrue( rules.contains( ruleE ) );
        assertTrue( rules.contains( ruleF ) );
        //Make sure that the rule connected to program A is not returned as part of list of rules in program B.
        assertFalse( rules.contains( ruleG ) );


        assertEquals( ruleD.getId(), programRuleService.getProgramRuleByName( "RuleD", programB ).getId() );

        assertEquals( 3, programRuleService.getProgramRules( programB ,"rule" ).size() );
    }

    @Test
    public void testGetImplementableProgramRules()
    {
        ProgramRule ruleD = new ProgramRule( "RuleD", "descriptionD", programB, null, null, "true", null );
        ProgramRule ruleE = new ProgramRule( "RuleE", "descriptionE", programB, null, null, "$a < 1", 1 );
        //Add a rule that is not part of programB....
        ProgramRule ruleG = new ProgramRule( "RuleG", "descriptionG", programA, null, null, "!false", 0 );

        programRuleService.addProgramRule( ruleD );
        programRuleService.addProgramRule( ruleE );
        programRuleService.addProgramRule( ruleG );

        ProgramRuleAction actionD = createProgramRuleAction( 'D' );
        actionD.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        actionD.setProgramRule( ruleD );

        programRuleActonService.addProgramRuleAction( actionD );
        ruleD.setProgramRuleActions( Sets.newHashSet( actionD ) );
        programRuleService.updateProgramRule( ruleD );


        //Get all the 3 rules for programB
        List<ProgramRule> rules = programRuleService.getImplementableProgramRules( programB, ProgramRuleActionType.getImplementedActions() );
        assertEquals( 1, rules.size() );
        assertTrue( rules.contains( ruleD ) );
        assertFalse( rules.contains( ruleG ) );
    }
    
    @Test
    public void testUpdate()
    {
        ProgramRule ruleH = new ProgramRule( "RuleA", "descriptionA", programA, programStageA, null, "true", null );
        
        int idH = programRuleService.addProgramRule( ruleH );
        
        ruleH.setCondition( "$newcondition == true" );
        ruleH.setName( "new name" );
        ruleH.setDescription( "new desc" );
        ruleH.setPriority( 99 );
        ruleH.setProgram( programC );
        
        programRuleService.updateProgramRule( ruleH );
        
        assertEquals( ruleH, programRuleService.getProgramRule( idH ) );
    }

    @Test
    public void testDeleteProgramRule()
    {
        ProgramRule ruleI = new ProgramRule( "RuleI", "descriptionI", programB, null, null, "true", null );
        ProgramRule ruleJ = new ProgramRule( "RuleJ", "descriptionJ", programB, null, null, "$a < 1", 1 );

        int idI = programRuleService.addProgramRule( ruleI );
        int idJ = programRuleService.addProgramRule( ruleJ );
        
        assertNotNull( programRuleService.getProgramRule( idI ) );
        assertNotNull( programRuleService.getProgramRule( idJ ) );

        programRuleService.deleteProgramRule( ruleI );

        assertNull( programRuleService.getProgramRule( idI ) );
        assertNotNull( programRuleService.getProgramRule( idJ ) );

        programRuleService.deleteProgramRule( ruleJ );

        assertNull( programRuleService.getProgramRule( idI ) );
        assertNull( programRuleService.getProgramRule( idJ ) );
    }

    @Test
    public void testSimpleExpression()
    {
        String expression = KEY_ATTRIBUTE + "{" + programRuleVariableA.getName() + "} == " + KEY_DATAELEMENT + "{" + programRuleVariableB.getName() + "}";
        assertEquals( ExpressionValidationOutcome.VALID, programRuleService.expressionIsValid( expression ) );
    }

    @Test
    public void testIfProgramRuleVariableIsCalculatedValue()
    {
        String expression = KEY_ATTRIBUTE + "{" + programRuleVariableA.getName() + "} == " + KEY_DATAELEMENT + "{" + programRuleVariableC.getName() + "}";
        assertEquals( ExpressionValidationOutcome.VALID, programRuleService.expressionIsValid( expression ) );
    }

    @Test
    public void testIfDataElementDoesNotExist()
    {
        programRuleVariableB.setDataElement( null );
        String expression = KEY_ATTRIBUTE + "{" + programRuleVariableA.getName() + "} == " + KEY_DATAELEMENT + "{" + programRuleVariableB.getName() + "}";
        assertEquals( ExpressionValidationOutcome.NO_DE_IN_PROGRAM_RULE_VARIABLE, programRuleService.expressionIsValid( expression ) );
    }

    @Test
    public void testIfAttributeDoesNotExist()
    {
        programRuleVariableA.setAttribute( null );
        String expression = KEY_ATTRIBUTE + "{" + programRuleVariableA.getName() + "} == " + KEY_DATAELEMENT + "{" + programRuleVariableB.getName() + "}";
        assertEquals( ExpressionValidationOutcome.NO_ATTR_IN_PROGRAM_RULE_VARIABLE, programRuleService.expressionIsValid( expression ) );
    }

    @Test
    public void testIfProgramRuleVariableDoesNotExist()
    {
        String expression = KEY_ATTRIBUTE + "{ghostVariable} == " + KEY_DATAELEMENT + "{" + programRuleVariableC.getName() + "}";
        assertEquals( ExpressionValidationOutcome.INVALID_IDENTIFIERS_IN_EXPRESSION, programRuleService.expressionIsValid( expression ) );
    }

    @Test
    public void testFunctionExpression()
    {
        String functionExpression = "d2:length(#{" + programRuleVariableC.getName() + "}) > 1";
        assertEquals( ExpressionValidationOutcome.VALID, programRuleService.expressionIsValid( functionExpression ) );
    }

    @Test
    public void testCascadedExpression()
    {
        String functionExpression = "d2:ceil(d2:length(#{" + programRuleVariableC.getName() + "})) > 1";
        assertEquals( ExpressionValidationOutcome.VALID, programRuleService.expressionIsValid( functionExpression ) );
    }

    @Test
    public void testExpressionWhichDoesNotResultInBoolean()
    {
        String functionExpression = "d2:length(#{" + programRuleVariableC.getName() + "}) + " + KEY_DATAELEMENT + "{" + programRuleVariableB.getName() + "}";
        assertEquals( ExpressionValidationOutcome.FILTER_NOT_EVALUATING_TO_TRUE_OR_FALSE, programRuleService.expressionIsValid( functionExpression ) );
    }

    @Test
    public void testExpressionWithVariables()
    {
        String expression = KEY_PROGRAM_VARIABLE + "{enrollment_id} == " + KEY_ATTRIBUTE + "{" + programRuleVariableA.getName() + "}";
        assertEquals( ExpressionValidationOutcome.VALID, programRuleService.expressionIsValid( expression ) );
    }

    @Test
    public void testExpressionWithNullVariables()
    {
        String expression = KEY_PROGRAM_VARIABLE + "{enrollment_idd} == " + KEY_ATTRIBUTE + "{" + programRuleVariableA.getName() + "}";
        assertEquals( ExpressionValidationOutcome.UNKNOWN_VARIABLE, programRuleService.expressionIsValid( expression ) );
    }

    @Test
    public void testExpressionLiteralValues()
    {
        String expression = " 1 < 2 + 1";
        assertEquals( ExpressionValidationOutcome.VALID, programRuleService.expressionIsValid( expression ) );

        expression = " 1 > 2 + 1";
        assertEquals( ExpressionValidationOutcome.VALID, programRuleService.expressionIsValid( expression ) );

        expression = " 1 + 2";
        assertEquals( ExpressionValidationOutcome.FILTER_NOT_EVALUATING_TO_TRUE_OR_FALSE, programRuleService.expressionIsValid( expression ) );
    }

    /*TODO: Fix the functionality for 2 level cascading deletes.
        
    @Test
    public void testCascadingDeleteProgram()
    {
        programService.deleteProgram( programA );
        
        assertNull( programRuleService.getProgramRule( programRuleA.getId() ) );
        assertNull( programRuleActonService.getProgramRuleAction( assignAction.getId() ) );
        assertNull( programRuleActonService.getProgramRuleAction( sendMessageAction.getId() ) );
        assertNull( programRuleVariableService.getProgramRuleVariable( programRuleVariableA.getId() ) );
        assertNull( programRuleVariableService.getProgramRuleVariable( programRuleVariableB.getId() ) );
    }*/
}
