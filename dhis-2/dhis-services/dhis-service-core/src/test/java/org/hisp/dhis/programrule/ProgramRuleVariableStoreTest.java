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
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.DhisSpringTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementStore;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProgramRuleVariableStoreTest
    extends DhisSpringTest
{
    private Program programA; 
    
    private DataElement dataelementA;
    
    @Autowired
    private ProgramStore programStore;
    
    @Autowired
    private DataElementStore dataElementStore;
    
    @Autowired
    private ProgramRuleVariableStore variableStore;
    
    @Override
    public void setUpTest()
    {
        programA = createProgram('A', null, null);
        dataelementA = createDataElement('A');
        
        programStore.save( programA );
        dataElementStore.save( dataelementA );        
    }
    
    @Test
    public void testGetByProgram()
    {
        ProgramRuleVariable varA = new ProgramRuleVariable( "VarA", programA, ProgramRuleVariableSourceType.DATAELEMENT_CURRENT_EVENT, null, dataelementA, null);
        ProgramRuleVariable varB = new ProgramRuleVariable( "VarB", programA, ProgramRuleVariableSourceType.DATAELEMENT_NEWEST_EVENT_PROGRAM, null, dataelementA, null);
        ProgramRuleVariable varC = new ProgramRuleVariable( "VarC", programA, ProgramRuleVariableSourceType.TEI_ATTRIBUTE, null, dataelementA, null);
        
        variableStore.save( varA );
        variableStore.save( varB );
        variableStore.save( varC );
        
        List<ProgramRuleVariable> vars = variableStore.get( programA );
        
        assertEquals( 3, vars.size() );
        assertTrue( vars.contains( varA ) );
        assertTrue( vars.contains( varB ) );
        assertTrue( vars.contains( varC ) );
    }
}
