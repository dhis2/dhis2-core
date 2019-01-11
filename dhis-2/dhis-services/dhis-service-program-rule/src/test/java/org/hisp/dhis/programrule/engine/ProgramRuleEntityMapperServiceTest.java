package org.hisp.dhis.programrule.engine;

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
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.*;
import org.hisp.dhis.rules.models.Rule;
import org.hisp.dhis.rules.models.RuleVariable;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Zubair Asghar.
 */

@RunWith( MockitoJUnitRunner.class )
public class ProgramRuleEntityMapperServiceTest extends DhisConvenienceTest
{
    private List<ProgramRule> programRules = new ArrayList<>();
    private List<ProgramRuleVariable> programRuleVariables = new ArrayList<>();

    private Program program;

    private ProgramRule programRuleA = null;
    private ProgramRule programRuleB = null;
    private ProgramRule programRuleC = null;
    private ProgramRule programRuleD = null;

    private ProgramRuleAction assignAction = null;
    private ProgramRuleAction sendMessageAction = null;

    private ProgramRuleVariable programRuleVariableA = null;
    private ProgramRuleVariable programRuleVariableB = null;
    private ProgramRuleVariable programRuleVariableD = null;

    @Mock
    private ProgramRuleService programRuleService;

    @Mock
    private ProgramRuleVariableService programRuleVariableService;

    @InjectMocks
    private DefaultProgramRuleEntityMapperService programRuleEntityMapperService;

    @Before
    public void initTest()
    {
        setUpProgramRules();

        when( programRuleService.getAllProgramRule() ).thenReturn( programRules );
        when( programRuleVariableService.getAllProgramRuleVariable() ).thenReturn( programRuleVariables );
        when( programRuleVariableService.getProgramRuleVariable( any( Program.class ) ) ).thenReturn( programRuleVariables );
    }

    @Test
    public void testMappedProgramRules()
    {
        List<Rule> rules = programRuleEntityMapperService.toMappedProgramRules();

        assertEquals( 3, rules.size() );
    }

    @Test
    public void testWhenProgramRuleConditionIsNull()
    {
        programRules.get( 0 ).setCondition( null );

        List<Rule> rules = programRuleEntityMapperService.toMappedProgramRules();

        programRules.get( 0 ).setCondition( "" );

        assertEquals( 2, rules.size() );
    }

    @Test
    public void testWhenProgramRuleActionIsNull()
    {
        programRules.get( 0 ).setProgramRuleActions( null );

        List<Rule> rules = programRuleEntityMapperService.toMappedProgramRules();

        programRules.get( 0 ).setProgramRuleActions( Sets.newHashSet( assignAction ) );

        assertEquals( 2, rules.size() );
    }

    @Test
    public void testMappedRuleVariableValues()
    {
        List<RuleVariable> ruleVariables = programRuleEntityMapperService.toMappedProgramRuleVariables();

        assertEquals( ruleVariables.size(), 2 );
    }

    private void setUpProgramRules()
    {
        program = createProgram('P' );

        programRuleVariableA = createProgramRuleVariable( 'V', program );
        programRuleVariableB = createProgramRuleVariable( 'W', program );
        programRuleVariableA = setProgramRuleVariable( programRuleVariableA, ProgramRuleVariableSourceType.CALCULATED_VALUE, program, null, createDataElement( 'D' ), null );
        programRuleVariableB = setProgramRuleVariable( programRuleVariableB, ProgramRuleVariableSourceType.TEI_ATTRIBUTE, program, null, null, createTrackedEntityAttribute( 'Z' ) );

        programRuleVariables.add( programRuleVariableA );
        programRuleVariables.add( programRuleVariableB );
        programRuleVariables.add( programRuleVariableD );

        programRuleA = createProgramRule( 'A', program );
        programRuleB = createProgramRule( 'B', program );
        programRuleD = createProgramRule( 'D', program );

        assignAction = createProgramRuleAction( 'I' );
        sendMessageAction = createProgramRuleAction( 'J' );

        assignAction = setProgramRuleAction( assignAction, ProgramRuleActionType.ASSIGN, "test_variable", "2+2" );
        sendMessageAction = setProgramRuleAction( sendMessageAction, ProgramRuleActionType.SENDMESSAGE, null, null );

        programRuleA = setProgramRule( programRuleA, "", assignAction, 1 );
        programRuleB = setProgramRule( programRuleB, "", sendMessageAction, 4 );
        programRuleD = setProgramRule( programRuleD, "", sendMessageAction, null );

        programRules.add( programRuleA );
        programRules.add( programRuleB );
        programRules.add( programRuleC );
        programRules.add( programRuleD );
    }

    private ProgramRule setProgramRule( ProgramRule programRule, String condition, ProgramRuleAction ruleAction, Integer priority )
    {
        programRule.setPriority( priority );
        programRule.setCondition( condition );
        programRule.setProgramRuleActions( Sets.newHashSet( ruleAction ) );

        return programRule;
    }

    private ProgramRuleAction setProgramRuleAction( ProgramRuleAction programRuleActionA, ProgramRuleActionType type, String content, String data )
    {
        programRuleActionA.setProgramRuleActionType( type );

        if( type == ProgramRuleActionType.ASSIGN )
        {
            programRuleActionA.setContent( content );
            programRuleActionA.setData( data );
        }

        if( type == ProgramRuleActionType.SENDMESSAGE )
        {
            ProgramNotificationTemplate notificationTemplate = new ProgramNotificationTemplate();
            notificationTemplate.setUid( "uid0" );
            programRuleActionA.setTemplateUid( notificationTemplate.getUid() );
        }

        return programRuleActionA;
    }

    private ProgramRuleVariable setProgramRuleVariable( ProgramRuleVariable variable,
                                                       ProgramRuleVariableSourceType sourceType, Program program, ProgramStage programStage, DataElement dataElement, TrackedEntityAttribute attribute )
    {
        variable.setSourceType( sourceType );
        variable.setProgram( program );
        variable.setProgramStage( programStage );
        variable.setDataElement( dataElement );
        variable.setAttribute( attribute );

        return variable;
    }
}
