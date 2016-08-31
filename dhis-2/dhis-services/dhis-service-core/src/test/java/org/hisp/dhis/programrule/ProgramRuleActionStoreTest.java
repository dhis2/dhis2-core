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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProgramRuleActionStoreTest
    extends DhisSpringTest
{
    private ProgramRule programRuleA;
    private DataElement dataElementA;    
    private Program programA;
    
    @Autowired
    private ProgramRuleStore programRuleStore;
    
    @Autowired
    private DataElementService dataElementService;
    
    @Autowired
    private ProgramRuleActionStore actionStore;

    @Autowired
    private ProgramService programService;
    
    @Override
    public void setUpTest()
    {
        programA = createProgram( 'A', null, null );
        programRuleA = createProgramRule( 'A', programA );
        dataElementA = createDataElement( 'A' );
        
        programService.addProgram( programA );
        programRuleStore.save( programRuleA );
        dataElementService.addDataElement( dataElementA );
    }
    
    @Test
    public void testGetByProgram()
    {
        ProgramRuleAction actionA = new ProgramRuleAction( "ActionA", programRuleA, ProgramRuleActionType.ASSIGN, null, null, null, null, null, null, "$myvar", "true");        
        ProgramRuleAction actionB = new ProgramRuleAction( "ActionB", programRuleA, ProgramRuleActionType.DISPLAYTEXT, null, null, null, null, null, "con","Hello", "$placeofliving");
        ProgramRuleAction actionC = new ProgramRuleAction( "ActionC", programRuleA, ProgramRuleActionType.HIDEFIELD, dataElementA, null, null, null, null, null, null, null);
       
        actionStore.save( actionA );
        actionStore.save( actionB );
        actionStore.save( actionC );
        
        List<ProgramRuleAction> vars = actionStore.get( programRuleA );
        
        assertEquals( 3, vars.size() );
        assertTrue( vars.contains( actionA ) );
        assertTrue( vars.contains( actionB ) );
        assertTrue( vars.contains( actionC ) );
    }
}
