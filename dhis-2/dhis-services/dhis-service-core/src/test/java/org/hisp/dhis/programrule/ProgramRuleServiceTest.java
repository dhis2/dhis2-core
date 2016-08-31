package org.hisp.dhis.programrule;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
    
    @Override
    public void setUpTest()
    {
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
        programRuleVariableB = createProgramRuleVariable( 'B', programA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableA );
        programRuleVariableService.addProgramRuleVariable( programRuleVariableB );  
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
    
    /*TODO: Fix the functionality for 2 level cascading deletes.
        
    @Test
    public void testCascadingDeleteProgram()
    {
        programService.deleteProgram( programA );
        
        assertNull( programRuleService.getProgramRule( programRuleA.getId() ) );
        assertNull( programRuleActonService.getProgramRuleAction( programRuleActionA.getId() ) );
        assertNull( programRuleActonService.getProgramRuleAction( programRuleActionB.getId() ) );
        assertNull( programRuleVariableService.getProgramRuleVariable( programRuleVariableA.getId() ) );
        assertNull( programRuleVariableService.getProgramRuleVariable( programRuleVariableB.getId() ) );
    }*/
}
