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

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProgramRuleVariableServiceTest
    extends DhisSpringTest
{
    private Program programA;
    private Program programB;
    private Program programC;
    
    private DataElement dataElementA;
    private TrackedEntityAttribute attributeA;
    
    @Autowired
    private ProgramService programService;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private TrackedEntityAttributeService attributeService;
    
    @Autowired
    private ProgramRuleVariableService variableService;
    
    @Override
    public void setUpTest()
    {
        programA = createProgram( 'A', null, null );
        programB = createProgram( 'B', null, null );
        programC = createProgram( 'C', null, null );
        
        dataElementA = createDataElement( 'A' );
        attributeA = createTrackedEntityAttribute( 'A' );
        
        programService.addProgram( programA );
        programService.addProgram( programB );
        programService.addProgram( programC );
        dataElementService.addDataElement( dataElementA );
        attributeService.addTrackedEntityAttribute( attributeA );
    }
    
    @Test
    public void testAddGet()
    {
        ProgramRuleVariable variableA = new ProgramRuleVariable( "RuleVariableA", programA, ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, null, dataElementA, false, null );
        ProgramRuleVariable variableB = new ProgramRuleVariable( "RuleVariableB", programA, ProgramRuleVariableSourceType.TEI_ATTRIBUTE, attributeA, null, true, null );
        ProgramRuleVariable variableC = new ProgramRuleVariable( "RuleVariableC", programA, ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, null, dataElementA, false, null );
        
        int idA = variableService.addProgramRuleVariable( variableA );
        int idB = variableService.addProgramRuleVariable( variableB );
        int idC = variableService.addProgramRuleVariable( variableC );
        assertEquals( variableA, variableService.getProgramRuleVariable( idA ) );
        assertEquals( variableB, variableService.getProgramRuleVariable( idB ) );
        assertEquals( variableC, variableService.getProgramRuleVariable( idC ) );
    }
    
    @Test
    public void testGetByProgram()
    {
        ProgramRuleVariable variableD = new ProgramRuleVariable( "RuleVariableD", programB, ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, null, dataElementA, false, null );
        ProgramRuleVariable variableE = new ProgramRuleVariable( "RuleVariableE", programB, ProgramRuleVariableSourceType.TEI_ATTRIBUTE, attributeA, null, null, null );
        ProgramRuleVariable variableF = new ProgramRuleVariable( "RuleVariableF", programB, ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, null, dataElementA, null, null );
         //Add a var that is not part of programB....
        ProgramRuleVariable variableG = new ProgramRuleVariable( "RuleVariableG", programA, ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, null, dataElementA, null, null );
        
        variableService.addProgramRuleVariable( variableD );
        variableService.addProgramRuleVariable( variableE );
        variableService.addProgramRuleVariable( variableF );
        variableService.addProgramRuleVariable( variableG );

        //Get all the 3 rules for programB
        List<ProgramRuleVariable> vars = variableService.getProgramRuleVariable( programB );
        assertEquals( 3, vars.size() );
        assertTrue( vars.contains( variableD ) );
        assertTrue( vars.contains( variableE ) );
        assertTrue( vars.contains( variableF ) );
        //Make sure that the var connected to program A is not returned as part of list of vars in program B.
        assertFalse( vars.contains( variableG ) );
        
    }
    
    @Test
    public void testUpdate()
    {
        ProgramRuleVariable variableH = new ProgramRuleVariable( "RuleVariableH", programA, ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM_STAGE, null, dataElementA, false, null );
        
        int idH = variableService.addProgramRuleVariable( variableH );
        
        variableH.setAttribute( attributeA );
        variableH.setDataElement( dataElementA );
        variableH.setName( "newname" );
        variableH.setProgram( programC );
        variableH.setSourceType( ProgramRuleVariableSourceType.DATAELEMENT_PREVIOUS_EVENT );
                
        variableService.updateProgramRuleVariable( variableH );
        
        assertEquals( variableH, variableService.getProgramRuleVariable( idH ) );
    }
    
    @Test
    public void testDeleteProgramRuleVariable()
    {
        ProgramRuleVariable ruleVariableI = new ProgramRuleVariable( "RuleVariableI", programA, ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, null, dataElementA, null, null );
        ProgramRuleVariable ruleVariableJ = new ProgramRuleVariable( "RuleVariableJ", programA, ProgramRuleVariableSourceType.TEI_ATTRIBUTE, attributeA, null, false, null );

        int idI = variableService.addProgramRuleVariable( ruleVariableI );
        int idJ = variableService.addProgramRuleVariable( ruleVariableJ );
        
        assertNotNull( variableService.getProgramRuleVariable( idI ) );
        assertNotNull( variableService.getProgramRuleVariable( idJ ) );

        variableService.deleteProgramRuleVariable( ruleVariableI );

        assertNull( variableService.getProgramRuleVariable( idI ) );
        assertNotNull( variableService.getProgramRuleVariable( idJ ) );

        variableService.deleteProgramRuleVariable( ruleVariableJ );

        assertNull( variableService.getProgramRuleVariable( idI ) );
        assertNull( variableService.getProgramRuleVariable( idJ ) );
    }
}
