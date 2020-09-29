package org.hisp.dhis.programrule;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.hibernate.SessionFactory;
import org.hisp.dhis.IntegrationTestBase;
import org.hisp.dhis.common.DeleteNotAllowedException;
import org.hisp.dhis.deletedobject.DeletedObjectQuery;
import org.hisp.dhis.deletedobject.DeletedObjectStore;
import org.hisp.dhis.program.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.google.common.collect.Sets;

public class ProgramRuleServiceTest
    extends IntegrationTestBase
{
    private Program programA;
    private Program programB;
    private Program programC;

    private ProgramStage programStageA;

    private ProgramStage programStageB;

    private ProgramStageSection programStageSectionA;
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
    private ProgramStageSectionService programStageSectionService;

    @Autowired
    private ProgramRuleService programRuleService;

    @Autowired
    private ProgramRuleActionService programRuleActonService;

    @Autowired
    private ProgramRuleVariableService programRuleVariableService;

    @Autowired
    private DeletedObjectStore deletedObjectStore;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public boolean emptyDatabaseAfterTest()
    {
        return true;
    }

    @Override
    public void setUpTest()
    {
        programA = createProgram( 'A', null, null );
        programB = createProgram( 'B', null, null );
        programC = createProgram( 'C', null, null );

        programService.addProgram( programA );
        programService.addProgram( programB );
        programService.addProgram( programC );

        programStageSectionA = createProgramStageSection( 'A', 1 );
        programStageSectionService.saveProgramStageSection( programStageSectionA );

        programStageA = createProgramStage( 'A', 1 );
        programStageA.setProgram( programA );
        programStageService.saveProgramStage( programStageA );

        Set<ProgramStage> stagesA = new HashSet<>();
        stagesA.add( programStageA );
        programA.setProgramStages( stagesA );
        programService.updateProgram( programA );



        programStageB = createProgramStage( 'B', 1 );
        programStageB.setProgram( programA );

        programStageB.setProgramStageSections( Sets.newHashSet( programStageSectionA ) );
        programStageService.saveProgramStage( programStageB );

        programStageSectionA.setProgramStage( programStageB );
        programStageSectionService.updateProgramStageSection( programStageSectionA );

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

        long idA = programRuleService.addProgramRule( ruleA );
        long idB = programRuleService.addProgramRule( ruleB );
        long idC = programRuleService.addProgramRule( ruleC );

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

        long idH = programRuleService.addProgramRule( ruleH );

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

        long idI = programRuleService.addProgramRule( ruleI );
        long idJ = programRuleService.addProgramRule( ruleJ );

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
    public void testDeleteDeletedObjectWithCascade()
    {
        ProgramRule programRule = createProgramRule( 'A', programA );

        ProgramRuleAction programRuleAction = createProgramRuleAction( 'D' );
        programRuleAction.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleAction.setProgramRule( programRule );

        programRule.setProgramRuleActions( Sets.newHashSet(programRuleAction) );

        programRuleService.addProgramRule( programRule );

        String programRuleUID = programRule.getUid();
        String programRuleActionUID = programRuleAction.getUid();

        programRuleService.deleteProgramRule( programRule );

        ProgramRule programRule1 = createProgramRule( 'A', programA );
        programRule1.setUid( programRuleUID );

        ProgramRuleAction programRuleAction1 = createProgramRuleAction( 'D' );
        programRuleAction1.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleAction1.setProgramRule( programRule1 );
        programRuleAction1.setUid( programRuleActionUID );

        programRule1.setProgramRuleActions( Sets.newHashSet( programRuleAction1 ) );

        programRuleService.addProgramRule( programRule1 );

        programRuleService.deleteProgramRule( programRule1 );

        assertNotNull( deletedObjectStore.query( new DeletedObjectQuery( programRule1 ) ) );
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
    }
     */

    @Test
    public void testDoNotAllowDeleteProgramStageBecauseOfLinkWithProgramRule()
    {
        expectedEx.expect( DeleteNotAllowedException.class );
        expectedEx.expectMessage( "ProgramRuleA" );

        programRuleA.setProgramStage( programStageA );
        programRuleService.updateProgramRule( programRuleA );

        programStageService.deleteProgramStage( programStageA );
    }

    @Test
    public void testDoNotAllowDeleteProgramStageSectionBecauseOfLinkWithProgramRuleAction()
    {
        expectedEx.expect( DeleteNotAllowedException.class );
        expectedEx.expectMessage( "ProgramRuleA" );

        programRuleActionA.setProgramStageSection( programStageSectionA );
        programRuleActonService.updateProgramRuleAction( programRuleActionA );

        programRuleA.getProgramRuleActions().add( programRuleActionA );
        programRuleService.updateProgramRule( programRuleA );

        programStageSectionService.deleteProgramStageSection( programStageSectionA );
    }

    @Test
    public void testDoNotAllowDeleteProgramStageBecauseOfLinkWithProgramRuleActionAndSection()
    {
        expectedEx.expect( DeleteNotAllowedException.class );
        expectedEx.expectMessage( "ProgramRuleA" );

        programRuleActionA.setProgramStageSection( programStageSectionA );
        programRuleActonService.updateProgramRuleAction( programRuleActionA );

        programRuleA.getProgramRuleActions().add( programRuleActionA );
        programRuleService.updateProgramRule( programRuleA );

        programStageService.deleteProgramStage( programStageB );
    }

    @Test
    public void testDoNotAllowDeleteProgramStageBecauseOfLinkWithProgramRuleAction()
    {
        expectedEx.expect( DeleteNotAllowedException.class );
        expectedEx.expectMessage( "ProgramRuleA" );

        programRuleActionA.setProgramStage( programStageA );
        programRuleActonService.updateProgramRuleAction( programRuleActionA );

        programRuleA.getProgramRuleActions().add( programRuleActionA );
        programRuleService.updateProgramRule( programRuleA );

        programStageService.deleteProgramStage( programStageA );
    }

    @Test
    public void testDoNotAllowDeleteProgramStageBecauseOfLinkWithProgramRuleVariable()
    {
        expectedEx.expect( DeleteNotAllowedException.class );
        expectedEx.expectMessage( "ProgramRuleVariableA" );

        programRuleVariableA.setProgramStage( programStageA );
        programRuleVariableService.updateProgramRuleVariable( programRuleVariableA );

        programStageService.deleteProgramStage( programStageA );
    }
}
