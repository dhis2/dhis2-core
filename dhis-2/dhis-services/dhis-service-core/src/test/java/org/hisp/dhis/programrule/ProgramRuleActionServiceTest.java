package org.hisp.dhis.programrule;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class ProgramRuleActionServiceTest
    extends DhisSpringTest
{
    private ProgramRule programRuleA;
    private ProgramRule programRuleB;
    private ProgramRule programRuleC;
    private TrackedEntityAttribute attributeA;
    private DataElement dataElementA;
    private Program programA;
    private ProgramStage programStageA;
    
    @Autowired
    private ProgramRuleService programRuleService;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private TrackedEntityAttributeService attributeService;
    
    @Autowired
    private ProgramRuleActionService actionService;

    @Autowired
    private ProgramService programService;
    
    @Autowired
    private ProgramStageService programStageService;
    
    @Override
    public void setUpTest()
    {
        
        programStageA = createProgramStage( 'A', 0 );
        Set<ProgramStage> programStages = new HashSet<>();
        programStages.add( programStageA );
        programA = createProgram( 'A', programStages, null );
        programRuleA = createProgramRule( 'A', programA );
        programRuleB = createProgramRule( 'B', programA );
        programRuleC = createProgramRule( 'C', programA );
        dataElementA = createDataElement( 'A' );
        attributeA = createTrackedEntityAttribute('a');
        
        programService.addProgram( programA );
        programStageService.saveProgramStage(programStageA);
        programRuleService.addProgramRule( programRuleA );
        programRuleService.addProgramRule( programRuleB );
        programRuleService.addProgramRule( programRuleC );
        dataElementService.addDataElement( dataElementA );
        attributeService.addTrackedEntityAttribute(attributeA);
    }
    
    @Test
    public void testAddGet()
    {
        ProgramRuleAction actionA = new ProgramRuleAction( "ActionA", programRuleA, ProgramRuleActionType.ASSIGN, null, null, null, null, null, null, "$myvar", "true");
        ProgramRuleAction actionB = new ProgramRuleAction( "ActionB", programRuleA, ProgramRuleActionType.DISPLAYTEXT, null, null, null, null, null, "con","Hello", "$placeofliving");
        ProgramRuleAction actionC = new ProgramRuleAction( "ActionC", programRuleA, ProgramRuleActionType.HIDEFIELD, dataElementA, null, null, null, null, null, null, null);
        ProgramRuleAction actionD = new ProgramRuleAction( "ActionD", programRuleA, ProgramRuleActionType.HIDEFIELD, null, attributeA, null, null, null, null, null, null);
        ProgramRuleAction actionE = new ProgramRuleAction( "ActionE", programRuleA, ProgramRuleActionType.CREATEEVENT, null, null, null, programStageA, null, null, null, "{wqpUVEeJR3D:30,mrVkW9h2Rdp:'live'}");
        
        int idA = actionService.addProgramRuleAction( actionA );
        int idB = actionService.addProgramRuleAction( actionB );
        int idC = actionService.addProgramRuleAction( actionC );
        int idD = actionService.addProgramRuleAction( actionD );
        int idE = actionService.addProgramRuleAction( actionE );
        
        assertEquals( actionA, actionService.getProgramRuleAction( idA ) );
        assertEquals( actionB, actionService.getProgramRuleAction( idB ) );
        assertEquals( actionC, actionService.getProgramRuleAction( idC ) );
        assertEquals( actionD, actionService.getProgramRuleAction( idD ) );
        assertEquals( actionE, actionService.getProgramRuleAction( idE ) );
    }
    
    @Test
    public void testGetByProgram()
    {
        ProgramRuleAction actionD = new ProgramRuleAction( "ActionD", programRuleB, ProgramRuleActionType.ASSIGN, null, null, null, null, null, null, "$myvar", "true");
        ProgramRuleAction actionE = new ProgramRuleAction( "ActionE", programRuleB, ProgramRuleActionType.DISPLAYTEXT, null, null, null, null, null, "con","Hello", "$placeofliving");
        ProgramRuleAction actionF = new ProgramRuleAction( "ActionF", programRuleB, ProgramRuleActionType.HIDEFIELD, dataElementA, null, null, null, null, null, null, null);
        //Add an action that is not part of programRuleB....
        ProgramRuleAction actionG = new ProgramRuleAction( "ActionG", programRuleC, ProgramRuleActionType.HIDEFIELD, dataElementA, null, null, null, null, null, null, null);
        
        actionService.addProgramRuleAction( actionD );
        actionService.addProgramRuleAction( actionE );
        actionService.addProgramRuleAction( actionF );
        actionService.addProgramRuleAction( actionG );
        
        //Get all the 3 rules for programB
        List<ProgramRuleAction> rules = actionService.getProgramRuleAction( programRuleB );
        assertEquals( 3, rules.size() );
        assertTrue( rules.contains( actionD ) );
        assertTrue( rules.contains( actionE ) );
        assertTrue( rules.contains( actionF ) );
        //Make sure that the action connected to rule A is not returned as part of list of actions in rule B.
        assertFalse( rules.contains( actionG ) );
        
    }
    
    @Test
    public void testUpdate()
    {
        ProgramRuleAction actionH = new ProgramRuleAction( "ActionH", programRuleB, ProgramRuleActionType.ASSIGN, null, null, null, null, null, null, "$myvar", "true");
        
        int idH = actionService.addProgramRuleAction( actionH );
        
        actionH.setName( "new name" );
        actionH.setData( "$newdata" );
        actionH.setLocation( "newlocation" );
        actionH.setDataElement( dataElementA );
        actionH.setProgramRule( programRuleC );
        actionH.setProgramRuleActionType( ProgramRuleActionType.HIDEFIELD );
        
        actionService.updateProgramRuleAction( actionH );
        
        assertEquals( actionH, actionService.getProgramRuleAction( idH ) );
    }
    
    @Test
    public void testDeleteProgramRuleVariable()
    {
        ProgramRuleAction actionI = new ProgramRuleAction( "ActionI", programRuleA, ProgramRuleActionType.ASSIGN, null, null, null, null, null, null, "$myvar", "true");
        ProgramRuleAction actionJ = new ProgramRuleAction( "ActionJ", programRuleA, ProgramRuleActionType.DISPLAYTEXT, null, null, null, null, null, "con","Hello", "$placeofliving");
        
        int idI = actionService.addProgramRuleAction( actionI );
        int idJ = actionService.addProgramRuleAction( actionJ );
        
        assertNotNull( actionService.getProgramRuleAction( idI ) );
        assertNotNull( actionService.getProgramRuleAction( idJ ) );

        actionService.deleteProgramRuleAction( actionI );

        assertNull( actionService.getProgramRuleAction( idI ) );
        assertNotNull( actionService.getProgramRuleAction( idJ ) );

        actionService.deleteProgramRuleAction( actionJ );

        assertNull( actionService.getProgramRuleAction( idI ) );
        assertNull( actionService.getProgramRuleAction( idJ ) );
    }
}
