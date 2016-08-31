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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageStore;
import org.hisp.dhis.program.ProgramStore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ProgramRuleStoreTest
    extends DhisSpringTest
{
    private Program programA;
    
    private ProgramStage programStageA;
    
    @Autowired
    private ProgramStore programStore;
    
    @Autowired
    private ProgramStageStore programStageStore;
    
    @Autowired
    private ProgramRuleStore variableStore;
    
    @Override
    public void setUpTest()
    {
        programA = createProgram('A', null, null );
        programStageA = createProgramStage( 'A', 0 );
        
        programStore.save( programA );
        programStageStore.save( programStageA );        
    }
    
    @Test
    public void testGetByProgram()
    {
        ProgramRule ruleA = new ProgramRule( "RuleA", "descriptionA", programA, programStageA, null, "true", null );
        ProgramRule ruleB = new ProgramRule( "RuleA", "descriptionA", programA, null, null, "$a < 1", 1 );
        ProgramRule ruleC = new ProgramRule( "RuleA", "descriptionA", programA, null, null, "($a < 1 && $a > -10) && !$b", 0 );
        
        variableStore.save( ruleA );
        variableStore.save( ruleB );
        variableStore.save( ruleC );
        
        List<ProgramRule> vars = variableStore.get( programA );
        
        assertEquals( 3, vars.size() );
        assertTrue( vars.contains( ruleA ) );
        assertTrue( vars.contains( ruleB ) );
        assertTrue( vars.contains( ruleC ) );
    }
}
