package org.hisp.dhis.program.notification;

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

import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.ProgramNotificationMessageRenderer;
import org.hisp.dhis.notification.ProgramStageNotificationMessageRenderer;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author Zubair Asghar.
 */
@RunWith( MockitoJUnitRunner.class )
public class ProgramNotificationServiceTest extends DhisConvenienceTest
{
    @InjectMocks
    private DefaultProgramNotificationService programNotificationService;

    @Mock
    private MessageService messageService;

    @Mock
    private ProgramMessageService programMessageService;

    @Mock
    private ProgramNotificationMessageRenderer programNotificationMessageRenderer;

    @Mock
    private ProgramStageNotificationMessageRenderer programStageNotificationMessageRenderer;

    @Mock
    private IdentifiableObjectManager manager;

    @Mock
    private ProgramInstanceStore programInstanceStore;

    @Mock
    private ProgramStageInstanceStore programStageInstanceStore;

    private List<ProgramInstance> programInstances = new ArrayList<>();

    private List<ProgramStageInstance> programStageInstances = new ArrayList<>();

    private List<ProgramNotificationTemplate> programNotificationTemplates = new ArrayList<>();

    private NotificationMessage notificationMessage;

    @Before
    public void initTest()
    {
        setUpInstances();

        BatchResponseStatus status = new BatchResponseStatus( Arrays.asList() );
        when( programMessageService.sendMessages( anyList() ) ).thenReturn( status );

        when( messageService.sendMessage( any() ) ).thenReturn( 1 );

        when( programInstanceStore.getWithScheduledNotifications( any(), any()) ).thenReturn( programInstances );
        when( programStageInstanceStore.getWithScheduledNotifications( any(), any() ) ).thenReturn( programStageInstances );

        when( manager.getAll( ProgramNotificationTemplate.class ) ).thenReturn( programNotificationTemplates );

        when( programNotificationMessageRenderer.render( any(), any() ) ).thenReturn( notificationMessage );
        when( programStageNotificationMessageRenderer.render( any(), any() ) ).thenReturn( notificationMessage );
    }

    @Test
    public void testIfProgramInstanceIsNull()
    {
        ProgramInstance programInstance = null;

        programNotificationService.sendCompletionNotifications( programInstance );

        verify( manager, never() ).getAll( any() );
    }

    @Test
    public void testIfProgramStageInstanceIsNull()
    {
        ProgramStageInstance programStageInstance = null;

        programNotificationService.sendCompletionNotifications( programStageInstance );

        verify( manager, never() ).getAll( any() );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void setUpInstances()
    {
        programInstances = new ArrayList<>();
        programStageInstances = new ArrayList<>();

        programNotificationTemplates = new ArrayList<>();

        notificationMessage = null;
    }
}
