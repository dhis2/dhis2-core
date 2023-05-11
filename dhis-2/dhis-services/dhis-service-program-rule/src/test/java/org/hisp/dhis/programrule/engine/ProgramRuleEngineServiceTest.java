/*
 * Copyright (c) 2004-2022, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Sets;

/**
 * Created by zubair@dhis2.org on 04.02.18.
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class ProgramRuleEngineServiceTest extends DhisConvenienceTest
{

    private static final String NOTIFICATION_UID = "abc123";

    private static final String DATA = "abc123";

    // -------------------------------------------------------------------------
    // Mocking Dependencies
    // -------------------------------------------------------------------------

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private EventService eventService;

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

    private Enrollment enrollment;

    private Event event;

    private ProgramRule programRuleA;

    private Program program;

    private List<ProgramRule> programRules = new ArrayList<>();

    private ProgramRuleAction programRuleActionA;

    private List<RuleEffect> ruleEffects;

    @BeforeEach
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
    void testWhenNoImplementableActionExist_enrollment()
    {
        setProgramRuleActionType_ShowError();

        verify( programRuleEngine, never() ).evaluate( enrollment, Sets.newHashSet(), List.of( programRuleA ) );
        assertEquals( 0, ruleEffects.size() );
    }

    @Test
    void testWithImplementableActionExist_enrollment()
    {
        doAnswer( invocationOnMock -> {
            ruleEffects.add( (RuleEffect) invocationOnMock.getArguments()[0] );
            return ruleEffects;
        } ).when( ruleActionSendMessage ).implement( any(), any( Enrollment.class ) );

        List<RuleEffect> effects = new ArrayList<>();
        effects.add( RuleEffect.create( "", RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        when( enrollmentService.getEnrollment( anyLong() ) ).thenReturn( enrollment );
        when( programRuleEngine.evaluate( any(), any(), any() ) ).thenReturn( effects );
        when( programRuleEngine.getProgramRules( any() ) ).thenReturn( List.of( programRuleA ) );

        setProgramRuleActionType_SendMessage();

        ArgumentCaptor<Enrollment> argumentCaptor = ArgumentCaptor.forClass( Enrollment.class );

        List<RuleEffect> ruleEffects = service.evaluateEnrollmentAndRunEffects( enrollment.getId() );

        assertEquals( 1, ruleEffects.size() );

        RuleAction action = ruleEffects.get( 0 ).ruleAction();
        if ( action instanceof RuleActionSendMessage )
        {
            RuleActionSendMessage ruleActionSendMessage = (RuleActionSendMessage) action;

            assertEquals( NOTIFICATION_UID, ruleActionSendMessage.notification() );
        }

        verify( programRuleEngine, times( 1 ) ).evaluate( argumentCaptor.capture(), any(), any() );
        assertEquals( enrollment, argumentCaptor.getValue() );

        verify( ruleActionSendMessage ).accept( action );
        verify( ruleActionSendMessage ).implement( any( RuleEffect.class ), argumentCaptor.capture() );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
    }

    @Test
    void testWithImplementableActionExist_event()
    {
        doAnswer( invocationOnMock -> {
            ruleEffects.add( (RuleEffect) invocationOnMock.getArguments()[0] );
            return ruleEffects;
        } ).when( ruleActionSendMessage ).implement( any(), any( Event.class ) );

        List<RuleEffect> effects = new ArrayList<>();
        effects.add( RuleEffect.create( "", RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        when( eventService.getEvent( anyString() ) ).thenReturn( event );
        when( enrollmentService.getEnrollment( anyLong() ) ).thenReturn( enrollment );

        when( programRuleEngine.getProgramRules( any(), any() ) ).thenReturn( List.of( programRuleA ) );
        when( programRuleEngine.evaluate( any(), any(), anySet(), anyList() ) ).thenReturn( effects );

        setProgramRuleActionType_SendMessage();

        List<RuleEffect> ruleEffects = service.evaluateEventAndRunEffects( event.getUid() );

        assertEquals( 1, ruleEffects.size() );

        verify( programRuleEngine, times( 1 ) )
            .evaluate( enrollment, event, enrollment.getEvents(),
                List.of( programRuleA ) );

        verify( ruleActionSendMessage ).accept( ruleEffects.get( 0 ).ruleAction() );
        verify( ruleActionSendMessage ).implement( any( RuleEffect.class ), any( Event.class ) );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
    }

    @Test
    void shouldNotRetrieveEventsWhenEvaluatingAProgramEvent()
    {
        doAnswer( invocationOnMock -> {
            ruleEffects.add( (RuleEffect) invocationOnMock.getArguments()[0] );
            return ruleEffects;
        } ).when( ruleActionSendMessage ).implement( any(), any( Event.class ) );

        List<RuleEffect> effects = new ArrayList<>();
        effects.add( RuleEffect.create( "", RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );
        Program program = createProgram( 'A' );
        program.setProgramType( ProgramType.WITHOUT_REGISTRATION );
        ProgramStage programStage = createProgramStage( 'A', program );
        Event programEvent = createEvent( programStage, enrollment,
            createOrganisationUnit( 'A' ) );

        when( eventService.getEvent( programEvent.getUid() ) ).thenReturn( programEvent );

        when( programRuleEngine.getProgramRules( program, List.of( programStage ) ) )
            .thenReturn( List.of( programRuleA ) );
        when( programRuleEngine.evaluateProgramEvent( programEvent, program, List.of( programRuleA ) ) )
            .thenReturn( effects );

        setProgramRuleActionType_SendMessage();

        List<RuleEffect> ruleEffects = service.evaluateEventAndRunEffects( programEvent.getUid() );

        assertEquals( 1, ruleEffects.size() );

        verify( programRuleEngine, times( 1 ) )
            .evaluateProgramEvent( programEvent, program, List.of( programRuleA ) );

        verify( enrollmentService, never() ).getEnrollment( any() );

        verify( ruleActionSendMessage ).accept( ruleEffects.get( 0 ).ruleAction() );
        verify( ruleActionSendMessage ).implement( any( RuleEffect.class ), any( Event.class ) );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
    }

    @Test
    void shouldNotTryToEvaluateWhenThereAreNoRulesToRun()
    {
        when( eventService.getEvent( anyString() ) ).thenReturn( event );
        when( enrollmentService.getEnrollment( anyLong() ) ).thenReturn( enrollment );

        when( programRuleEngine.getProgramRules( any(), any() ) ).thenReturn( List.of() );

        List<RuleEffect> ruleEffects = service.evaluateEventAndRunEffects( event.getUid() );

        assertEquals( 0, ruleEffects.size() );

        verify( programRuleEngine, never() ).evaluateProgramEvent( any(), any(), anyList() );
        verify( programRuleEngine, never() ).evaluate( any(), any(), anyList() );
        verify( enrollmentService, never() ).getEnrollment( any() );
    }

    @Test
    void testGetDescription()
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

        enrollment = new Enrollment();
        enrollment.setProgram( programA );

        programStageA = createProgramStage( 'S', programA );
        programA.getProgramStages().add( programStageA );

        event = new Event();
        event.setProgramStage( programStageA );
        event.setEnrollment( enrollment );
        event.setUid( "PSI1" );

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
