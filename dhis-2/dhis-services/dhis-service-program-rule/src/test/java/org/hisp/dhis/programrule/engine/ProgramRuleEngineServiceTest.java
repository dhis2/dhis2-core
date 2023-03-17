/*
 * Copyright (c) 2004-2021, University of Oslo
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
package org.hisp.dhis.programrule.engine;

import static org.hisp.dhis.external.conf.ConfigurationKey.SYSTEM_PROGRAM_RULE_SERVER_EXECUTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Created by zubair@dhis2.org on 04.02.18.
 */
public class ProgramRuleEngineServiceTest extends DhisConvenienceTest
{
    private static final String NOTIFICATION_UID = "abc123";

    private static final String DATA = "abc123";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    // -------------------------------------------------------------------------
    // Mocking Dependencies
    // -------------------------------------------------------------------------

    @Mock
    private ProgramInstanceService programInstanceService;

    @Mock
    private ProgramStageInstanceService programStageInstanceService;

    @Mock
    private ProgramRuleEngine programRuleEngine;

    @Mock
    private RuleActionSendMessageImplementer ruleActionSendMessage;

    @Mock
    private ProgramRuleService programRuleService;

    @Mock
    private ProgramService programService;

    @Mock
    private DhisConfigurationProvider config;

    @Spy
    private ArrayList<RuleActionImplementer> ruleActionImplementers;

    @InjectMocks
    private DefaultProgramRuleEngineService service;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private ProgramRule programRuleA;

    private Program program;

    private List<ProgramRule> programRules = new ArrayList<>();

    private ProgramRuleAction programRuleActionA;

    private List<RuleEffect> ruleEffects;

    @Before
    public void initTest()
    {
        ruleEffects = new ArrayList<>();

        setUpInstances();

        // fill up the spy
        ruleActionImplementers.add( ruleActionSendMessage );

        // stub for ruleActionSendMessage
        Mockito.lenient().when( ruleActionSendMessage.accept( any() ) ).thenReturn( true );

        Mockito.when( config.isDisabled( SYSTEM_PROGRAM_RULE_SERVER_EXECUTION ) ).thenReturn( false );
    }

    @Test
    public void testWhenNoImplementableActionExist_programInstance()
    {
        setProgramRuleActionType_ShowError();

        verify( programRuleEngine, never() ).evaluate( programInstance, Sets.newHashSet(),
            Lists.newArrayList( programRuleA ) );
        assertEquals( 0, ruleEffects.size() );
    }

    @Test
    public void testWithImplementableActionExist_programInstance()
    {
        doAnswer( invocationOnMock -> {
            ruleEffects.add( (RuleEffect) invocationOnMock.getArguments()[0] );
            return ruleEffects;
        } ).when( ruleActionSendMessage ).implement( any(), any( ProgramInstance.class ) );

        List<RuleEffect> effects = new ArrayList<>();
        effects.add( RuleEffect.create( "", RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        when( programInstanceService.getProgramInstance( anyLong() ) ).thenReturn( programInstance );
        when( programRuleEngine.evaluate( any(), any(), any() ) ).thenReturn( effects );
        when( programRuleEngine.getProgramRules( any() ) ).thenReturn( Lists.newArrayList( programRuleA ) );

        setProgramRuleActionType_SendMessage();

        ArgumentCaptor<ProgramInstance> argumentCaptor = ArgumentCaptor.forClass( ProgramInstance.class );

        List<RuleEffect> ruleEffects = service.evaluateEnrollmentAndRunEffects( programInstance.getId() );

        assertEquals( 1, ruleEffects.size() );

        RuleAction action = ruleEffects.get( 0 ).ruleAction();
        if ( action instanceof RuleActionSendMessage )
        {
            RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) action;

            assertEquals( NOTIFICATION_UID, ruleActionSendMessage.notification() );
        }

        verify( programRuleEngine, times( 1 ) ).evaluate( argumentCaptor.capture(), any(), any() );
        assertEquals( programInstance, argumentCaptor.getValue() );

        verify( ruleActionSendMessage ).accept( action );
        verify( ruleActionSendMessage ).implement( any( RuleEffect.class ), argumentCaptor.capture() );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
    }

    @Test
    public void testWithImplementableActionExist_programStageInstance()
    {
        doAnswer( invocationOnMock -> {
            ruleEffects.add( (RuleEffect) invocationOnMock.getArguments()[0] );
            return ruleEffects;
        } ).when( ruleActionSendMessage ).implement( any(), any( ProgramStageInstance.class ) );

        List<RuleEffect> effects = new ArrayList<>();
        effects.add( RuleEffect.create( "", RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        when( programStageInstanceService.getProgramStageInstance( anyString() ) ).thenReturn( programStageInstance );
        when( programInstanceService.getProgramInstance( anyLong() ) ).thenReturn( programInstance );

        when( programRuleEngine.getProgramRules( any(), any() ) ).thenReturn( Lists.newArrayList( programRuleA ) );
        when( programRuleEngine.evaluate( any(), any(), anySet(), anyList() ) ).thenReturn( effects );

        setProgramRuleActionType_SendMessage();

        List<RuleEffect> ruleEffects = service.evaluateEventAndRunEffects( programStageInstance.getUid() );

        assertEquals( 1, ruleEffects.size() );

        verify( programRuleEngine, times( 1 ) )
            .evaluate( programInstance, programStageInstance, programInstance.getProgramStageInstances(),
                Lists.newArrayList( programRuleA ) );

        verify( ruleActionSendMessage ).accept( ruleEffects.get( 0 ).ruleAction() );
        verify( ruleActionSendMessage ).implement( any( RuleEffect.class ), any( ProgramStageInstance.class ) );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
    }

    @Test
    public void shouldNotRetrieveEventsWhenEvaluatingAProgramEvent()
    {
        doAnswer( invocationOnMock -> {
            ruleEffects.add( (RuleEffect) invocationOnMock.getArguments()[0] );
            return ruleEffects;
        } ).when( ruleActionSendMessage ).implement( any(), any( ProgramStageInstance.class ) );

        List<RuleEffect> effects = new ArrayList<>();
        effects.add( RuleEffect.create( "", RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );
        Program program = createProgram( 'A' );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        ProgramStage programStage = createProgramStage( 'A', program );
        ProgramStageInstance programEvent = new ProgramStageInstance();
        programEvent.setProgramStage( programStage );
        programEvent.setProgramInstance( programInstance );

        when( programStageInstanceService.getProgramStageInstance( programEvent.getUid() ) ).thenReturn( programEvent );

        when( programRuleEngine.getProgramRules( program, Lists.newArrayList( programStage ) ) )
            .thenReturn( Lists.newArrayList( programRuleA ) );
        when( programRuleEngine.evaluateProgramEvent( programEvent, program, Lists.newArrayList( programRuleA ) ) )
            .thenReturn( effects );

        setProgramRuleActionType_SendMessage();

        List<RuleEffect> ruleEffects = service.evaluateEventAndRunEffects( programEvent.getUid() );

        assertEquals( 1, ruleEffects.size() );

        verify( programRuleEngine, times( 1 ) )
            .evaluateProgramEvent( programEvent, program, Lists.newArrayList( programRuleA ) );

        verify( programInstanceService, never() ).getProgramInstance( any() );

        verify( ruleActionSendMessage ).accept( ruleEffects.get( 0 ).ruleAction() );
        verify( ruleActionSendMessage ).implement( any( RuleEffect.class ), any( ProgramStageInstance.class ) );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
    }

    @Test
    public void shouldNotTryToEvaluateWhenThereAreNoRulesToRun()
    {
        when( programStageInstanceService.getProgramStageInstance( anyString() ) ).thenReturn( programStageInstance );
        when( programInstanceService.getProgramInstance( anyLong() ) ).thenReturn( programInstance );

        when( programRuleEngine.getProgramRules( any(), any() ) ).thenReturn( Lists.newArrayList() );

        List<RuleEffect> ruleEffects = service.evaluateEventAndRunEffects( programStageInstance.getUid() );

        assertEquals( 0, ruleEffects.size() );

        verify( programRuleEngine, never() ).evaluateProgramEvent( any(), any(), anyList() );
        verify( programRuleEngine, never() ).evaluate( any(), any(), anyList() );
        verify( programInstanceService, never() ).getProgramInstance( any() );
    }

    @Test
    public void testGetDescription()
    {
        RuleValidationResult result = RuleValidationResult.builder().isValid( true ).build();
        when( programRuleService.getProgramRule( anyString() ) ).thenReturn( programRuleA );
        when( programRuleEngine.getDescription( programRuleA.getCondition(), program ) ).thenReturn( result );

        assertNotNull( result );
        assertTrue( result.isValid() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void setUpInstances()
    {
        OrganisationUnit organisationUnitA = createOrganisationUnit( 'A' );

        Program programA = createProgram( 'A', new HashSet<>(), organisationUnitA );
        ProgramStage programStageA = createProgramStage( 'A', programA );

        programRuleA = createProgramRule( 'R', programA );
        programRuleActionA = createProgramRuleAction( 'T' );

        programRuleA.setProgram( programA );

        programInstance = new ProgramInstance();
        programInstance.setProgram( programA );

        programStageA = createProgramStage( 'S', programA );
        programA.getProgramStages().add( programStageA );

        programStageInstance = new ProgramStageInstance();
        programStageInstance.setProgramStage( programStageA );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setUid( "PSI1" );

        programRules.add( programRuleA );
    }

    private void setProgramRuleActionType_SendMessage()
    {
        programRuleActionA.setProgramRuleActionType( ProgramRuleActionType.SENDMESSAGE );
        programRuleA.getProgramRuleActions().add( programRuleActionA );
    }

    private void setProgramRuleActionType_ShowError()
    {
        programRuleActionA.setProgramRuleActionType( ProgramRuleActionType.SHOWWARNING );
        programRuleA.getProgramRuleActions().add( programRuleActionA );
    }
}
