/*
<<<<<<< HEAD
 * Copyright (c) 2004-2020, University of Oslo
=======
 * Copyright (c) 2004-2021, University of Oslo
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.*;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleEffect;
import org.hisp.dhis.rules.models.RuleValidationResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.*;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

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

<<<<<<< HEAD
=======
    @Mock
    private ProgramRuleService programRuleService;

    @Mock
    private ProgramService programService;

    @Mock
    private DhisConfigurationProvider config;

>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
<<<<<<< HEAD
=======

        Mockito.when( config.isDisabled( SYSTEM_PROGRAM_RULE_SERVER_EXECUTION ) ).thenReturn( false );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
    }

    @Test
    public void testWhenNoImplementableActionExist_programInstance()
    {
        setProgramRuleActionType_ShowError();

<<<<<<< HEAD
        verify( programRuleEngine, never() ).evaluate( programInstance, Optional.empty(), Sets.newHashSet() );
=======
        verify( programRuleEngine, never() ).evaluate( programInstance, Sets.newHashSet() );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
<<<<<<< HEAD
        effects.add( RuleEffect.create( RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        when( programInstanceService.getProgramInstance( anyLong() ) ).thenReturn( programInstance );
        when( programRuleEngine.evaluate( any(), any(), any() ) ).thenReturn( effects );
=======
        effects.add( RuleEffect.create( "", RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        when( programInstanceService.getProgramInstance( anyLong() ) ).thenReturn( programInstance );
        when( programRuleEngine.evaluate( any(), any() ) ).thenReturn( effects );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

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

<<<<<<< HEAD
        verify( programRuleEngine, times( 1 ) ).evaluate( argumentCaptor.capture(), any(), any() );
=======
        verify( programRuleEngine, times( 1 ) ).evaluate( argumentCaptor.capture(), any() );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za
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
<<<<<<< HEAD
        effects.add( RuleEffect.create( RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        when( programStageInstanceService.getProgramStageInstance( anyLong() ) ).thenReturn( programStageInstance );
        when( programRuleEngine.evaluate( any(), any(), any() ) ).thenReturn( effects );
=======
        effects.add( RuleEffect.create( "", RuleActionSendMessage.create( NOTIFICATION_UID, DATA ) ) );

        when( programStageInstanceService.getProgramStageInstance( anyString() ) ).thenReturn( programStageInstance );
        when( programInstanceService.getProgramInstance( anyLong() ) ).thenReturn( programInstance );

        when( programRuleEngine.evaluate( any(), any(), anySet() ) ).thenReturn( effects );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        setProgramRuleActionType_SendMessage();

        ArgumentCaptor<Optional> argumentCaptor = ArgumentCaptor.forClass( Optional.class );

<<<<<<< HEAD
        List<RuleEffect> ruleEffects = service.evaluateEventAndRunEffects( programStageInstance.getId() );
=======
        List<RuleEffect> ruleEffects = service.evaluateEventAndRunEffects( programStageInstance.getUid() );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        assertEquals( 1, ruleEffects.size() );

        verify( programRuleEngine, times( 1 ) ).evaluate( any(), argumentCaptor.capture(), any() );
<<<<<<< HEAD
        assertEquals( programStageInstance, argumentCaptor.getValue().get() );
=======
        assertEquals( programStageInstance, argumentCaptor.getValue() );
>>>>>>> refs/remotes/origin/2.35.8-EMBARGOED_za

        verify( ruleActionSendMessage ).accept( ruleEffects.get( 0 ).ruleAction() );
        verify( ruleActionSendMessage ).implement( any( RuleEffect.class ), any( ProgramStageInstance.class ) );

        assertEquals( 1, this.ruleEffects.size() );
        assertTrue( this.ruleEffects.get( 0 ).ruleAction() instanceof RuleActionSendMessage );
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
