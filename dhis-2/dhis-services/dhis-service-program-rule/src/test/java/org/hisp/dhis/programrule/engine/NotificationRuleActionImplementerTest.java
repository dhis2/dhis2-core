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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;

import javax.annotation.Nonnull;

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.notification.ProgramNotificationEventType;
import org.hisp.dhis.program.notification.ProgramNotificationPublisher;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateStore;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.rules.models.RuleAction;
import org.hisp.dhis.rules.models.RuleActionSendMessage;
import org.hisp.dhis.rules.models.RuleActionSetMandatoryField;
import org.hisp.dhis.rules.models.RuleEffect;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

/**
 * Created by zubair@dhis2.org on 05.02.18.
 */
public class NotificationRuleActionImplementerTest extends DhisConvenienceTest
{
    private static final String NOTIFICATION_UID = "123abc";

    private static final String MANDATORY_FIELD = "fname";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    // -------------------------------------------------------------------------
    // Mocking Dependencies
    // -------------------------------------------------------------------------

    @Mock
    private ProgramNotificationTemplateStore templateStore;

    @Mock
    private ProgramNotificationPublisher publisher;

    @Mock
    private NotificationLoggingService loggingService;

    @InjectMocks
    private RuleActionSendMessageImplementer implementer;

    private ProgramNotificationTemplate template;

    private ProgramNotificationEventType eventType;

    private ExternalNotificationLogEntry logEntry;

    private RuleEffect ruleEffectWithActionSendMessage;

    private RuleAction ruleActionSendMessage;

    private RuleAction setMandatoryFieldFalse;

    private ProgramInstance programInstance;

    private ProgramStageInstance programStageInstance;

    private ProgramRule programRuleA;

    @Before
    public void initTest()
    {
        setUpInstances();
    }

    @Test
    public void test_acceptBehaviorForActionAssign()
    {
        assertFalse( implementer.accept( setMandatoryFieldFalse ) );
    }

    @Test
    public void test_acceptBehaviorForActionSendMessage()
    {
        assertTrue( implementer.accept( ruleActionSendMessage ) );
    }

    @Test
    public void test_implementWithProgramInstanceWithTemplate()
    {

        when( templateStore.getByUid( anyString() ) ).thenReturn( template );

        doAnswer( invocationOnMock ->
        {
            eventType = (ProgramNotificationEventType) invocationOnMock.getArguments()[2];
            return eventType;
        }).when( publisher ).publishEnrollment( Mockito.any( ProgramNotificationTemplate.class ) ,Mockito.any( ProgramInstance.class ), any( ProgramNotificationEventType.class ) );

        doAnswer( invocationOnMock ->
        {
            logEntry = ( ExternalNotificationLogEntry ) invocationOnMock.getArguments()[0];
            return logEntry;
        }).when( loggingService ).save( any() );

        when( loggingService.isValidForSending( anyString() ) ).thenReturn( true );

        ArgumentCaptor<ProgramNotificationEventType> argumentEventCaptor = ArgumentCaptor.forClass( ProgramNotificationEventType.class );
        ArgumentCaptor<ProgramInstance> argumentInstanceCaptor = ArgumentCaptor.forClass( ProgramInstance.class );

        implementer.implement( ruleEffectWithActionSendMessage, programInstance );

        verify( templateStore, times( 2 ) ).getByUid( anyString() );
        verify( loggingService, times( 1 ) ).isValidForSending( anyString() );

        verify( publisher ).publishEnrollment( any( ProgramNotificationTemplate.class ), argumentInstanceCaptor.capture(), argumentEventCaptor.capture() );
        assertEquals( eventType, argumentEventCaptor.getValue() );
        assertEquals( programInstance, argumentInstanceCaptor.getValue() );
    }

    @Test
    public void test_implementWithProgramStageInstanceWithTemplate()
    {
        when( templateStore.getByUid( anyString() ) ).thenReturn( template );

        doAnswer( invocationOnMock -> {
            eventType = (ProgramNotificationEventType) invocationOnMock.getArguments()[2];
            return eventType;
        } ).when( publisher ).publishEvent( any( ProgramNotificationTemplate.class ),
            Mockito.any( ProgramStageInstance.class ), any( ProgramNotificationEventType.class ) );

        doAnswer( invocationOnMock ->
        {
            logEntry = ( ExternalNotificationLogEntry ) invocationOnMock.getArguments()[0];
            return logEntry;
        }).when( loggingService ).save( any() );

        when( loggingService.isValidForSending( anyString() ) ).thenReturn( true );

        ArgumentCaptor<ProgramNotificationEventType> argumentEventCaptor = ArgumentCaptor.forClass( ProgramNotificationEventType.class );
        ArgumentCaptor<ProgramStageInstance> argumentStageInstanceCaptor = ArgumentCaptor.forClass( ProgramStageInstance.class );

        implementer.implement( ruleEffectWithActionSendMessage, programStageInstance );

        verify( templateStore, times( 2 ) ).getByUid( anyString() );
        verify( loggingService, times( 1 ) ).isValidForSending( anyString() );

        verify( publisher ).publishEvent( any( ProgramNotificationTemplate.class ), argumentStageInstanceCaptor.capture(), argumentEventCaptor.capture() );
        assertEquals( eventType, argumentEventCaptor.getValue() );
        assertEquals( programStageInstance, argumentStageInstanceCaptor.getValue() );
    }

    @Test
    public void test_loggingServiceKey()
    {

        when( templateStore.getByUid( anyString() ) ).thenReturn( template );


        doAnswer( invocationOnMock -> {
            eventType = (ProgramNotificationEventType) invocationOnMock.getArguments()[2];
            return eventType;
        } ).when( publisher ).publishEnrollment( any( ProgramNotificationTemplate.class ), any( ProgramInstance.class ),
            any( ProgramNotificationEventType.class ) );

        doAnswer( invocationOnMock ->
        {
            logEntry = ( ExternalNotificationLogEntry ) invocationOnMock.getArguments()[0];
            return logEntry;
        }).when( loggingService ).save( any() );

        when( loggingService.isValidForSending( anyString() ) ).thenReturn( true );

        String key = template.getUid() + programInstance.getUid();

        implementer.implement( ruleEffectWithActionSendMessage, programInstance );

        assertEquals( key, logEntry.getKey() );
    }

    @Test
    public void test_NothingHappensIfTemplateDoesNotExist()
    {
        // overriding stub to check null templates
        when( templateStore.getByUid( anyString() ) ).thenReturn( null );

        implementer.implement( ruleEffectWithActionSendMessage, programInstance );

        verify( templateStore, times( 1 ) ).getByUid( anyString() );
        verify( loggingService, never() ).isValidForSending( anyString() );
        verify( loggingService, never() ).save( any() );
    }

    @Test
    public void test_NothingHappensIfTemplateDoesNotExistForPSI()
    {
        when( templateStore.getByUid( anyString() ) ).thenReturn( null );

        implementer.implement( ruleEffectWithActionSendMessage, programStageInstance );

        verify( templateStore, times( 1 ) ).getByUid( anyString() );
        verify( loggingService, never() ).isValidForSending( anyString() );
    }

    @Test
    public void test_NothingHappensIfActionIsNull()
    {
        implementer.implement( null, programInstance );

        verify( templateStore, never() ).getByUid( anyString() );
        verify( loggingService, never() ).isValidForSending( anyString() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void setUpInstances()
    {
        template = new ProgramNotificationTemplate();
        template.setUid( NOTIFICATION_UID );

        ruleActionSendMessage = new RuleActionSendMessage()
        {
            @Nonnull
            @Override
            public String notification()
            {
                return NOTIFICATION_UID;
            }

            @Nonnull
            @Override
            public String data()
            {
                return null;
            }
        };

        ruleEffectWithActionSendMessage = RuleEffect.create( ruleActionSendMessage );

        setMandatoryFieldFalse = new RuleActionSetMandatoryField()
        {
            @Nonnull
            @Override
            public String field()
            {
                return MANDATORY_FIELD;
            }
        };

        OrganisationUnit organisationUnitA = createOrganisationUnit( 'A' );

        Program programA = createProgram('A', new HashSet<>(), organisationUnitA );
        ProgramStage programStageA = createProgramStage( 'A', programA );

        programRuleA = createProgramRule( 'R', programA );

        programRuleA.setProgram( programA );

        programInstance = new ProgramInstance();
        programInstance.setProgram( programA );
        programInstance.setAutoFields();

        programStageA = createProgramStage( 'S', programA );
        programA.getProgramStages().add( programStageA );

        programStageInstance = new ProgramStageInstance();
        programStageInstance.setProgramStage( programStageA );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setAutoFields();
    }
}
