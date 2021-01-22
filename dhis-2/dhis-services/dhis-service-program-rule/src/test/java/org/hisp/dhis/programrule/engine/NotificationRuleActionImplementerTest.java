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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.program.notification.event.ProgramRuleEnrollmentEvent;
import org.hisp.dhis.program.notification.event.ProgramRuleStageEvent;
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
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

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
    private ProgramNotificationTemplateService templateStore;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private NotificationLoggingService loggingService;

    @InjectMocks
    private RuleActionSendMessageImplementer implementer;

    private ProgramNotificationTemplate template;

    private ExternalNotificationLogEntry logEntry;

    private ApplicationEvent eventType;

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

        doAnswer( invocationOnMock -> {
            eventType = (ApplicationEvent) invocationOnMock.getArguments()[0];
            return eventType;
        } ).when( publisher ).publishEvent( any() );

        doAnswer( invocationOnMock -> {
            logEntry = (ExternalNotificationLogEntry) invocationOnMock.getArguments()[0];
            return logEntry;
        } ).when( loggingService ).save( any() );

        when( loggingService.isValidForSending( anyString() ) ).thenReturn( true );

        ArgumentCaptor<ApplicationEvent> argumentEventCaptor = ArgumentCaptor.forClass( ApplicationEvent.class );

        implementer.implement( ruleEffectWithActionSendMessage, programInstance );

        verify( templateStore, times( 2 ) ).getByUid( anyString() );
        verify( loggingService, times( 1 ) ).isValidForSending( anyString() );

        verify( publisher ).publishEvent( argumentEventCaptor.capture() );
        assertEquals( eventType, argumentEventCaptor.getValue() );
        assertEquals( programInstance.getId(), ((ProgramRuleEnrollmentEvent) eventType).getProgramInstance() );
    }

    @Test
    public void test_implementWithProgramStageInstanceWithTemplate()
    {
        when( templateStore.getByUid( anyString() ) ).thenReturn( template );

        doAnswer( invocationOnMock -> {
            eventType = (ApplicationEvent) invocationOnMock.getArguments()[0];
            return eventType;
        } ).when( publisher ).publishEvent( any() );

        doAnswer( invocationOnMock -> {
            logEntry = (ExternalNotificationLogEntry) invocationOnMock.getArguments()[0];
            return logEntry;
        } ).when( loggingService ).save( any() );

        when( loggingService.isValidForSending( anyString() ) ).thenReturn( true );

        ArgumentCaptor<ApplicationEvent> argumentEventCaptor = ArgumentCaptor.forClass( ApplicationEvent.class );

        implementer.implement( ruleEffectWithActionSendMessage, programStageInstance );

        verify( templateStore, times( 2 ) ).getByUid( anyString() );
        verify( loggingService, times( 1 ) ).isValidForSending( anyString() );

        verify( publisher ).publishEvent( argumentEventCaptor.capture() );
        assertEquals( eventType, argumentEventCaptor.getValue() );
        assertEquals( programStageInstance.getId(), ((ProgramRuleStageEvent) eventType).getProgramStageInstance() );
    }

    @Test
    public void test_loggingServiceKey()
    {
        when( templateStore.getByUid( anyString() ) ).thenReturn( template );

        doAnswer( invocationOnMock -> {
            eventType = (ApplicationEvent) invocationOnMock.getArguments()[0];
            return eventType;
        } ).when( publisher ).publishEvent( any() );

        doAnswer( invocationOnMock -> {
            logEntry = (ExternalNotificationLogEntry) invocationOnMock.getArguments()[0];
            return logEntry;
        } ).when( loggingService ).save( any() );

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
        assertThrows( "Rule Effect cannot be null", NullPointerException.class,
            () -> implementer.implement( null, programInstance ) );
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

        setMandatoryFieldFalse = RuleActionSetMandatoryField.create( MANDATORY_FIELD );

        OrganisationUnit organisationUnitA = createOrganisationUnit( 'A' );

        Program programA = createProgram( 'A', new HashSet<>(), organisationUnitA );

        programRuleA = createProgramRule( 'R', programA );

        programRuleA.setProgram( programA );

        programInstance = new ProgramInstance();
        programInstance.setProgram( programA );
        programInstance.setAutoFields();

        ProgramStage programStageA = createProgramStage( 'S', programA );
        programA.getProgramStages().add( programStageA );

        programStageInstance = new ProgramStageInstance();
        programStageInstance.setProgramStage( programStageA );
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setAutoFields();
    }
}
