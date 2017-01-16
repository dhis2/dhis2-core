package org.hisp.dhis.validation;

/*
 * Copyright (c) 2004-2017, University of Oslo
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
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.ValidationNotificationMessageRenderer;
import org.hisp.dhis.user.User;
import org.hisp.dhis.validation.notification.DefaultValidationNotificationService;
import org.hisp.dhis.validation.notification.ValidationNotificationService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Halvdan Hoem Grelland
 */
public class ValidationNotificationServiceTest
    extends DhisConvenienceTest
{
    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    @InjectMocks
    private ValidationNotificationService service;

    @Mock
    private ValidationNotificationMessageRenderer renderer;

    @Mock
    private MessageService messageService;

    private List<Message> sentMessages;

    @Before
    public void setUpTest()
    {
        service = new DefaultValidationNotificationService();
        setUpMocks();
    }

    private void setUpMocks()
    {
        MockitoAnnotations.initMocks( this );

        sentMessages = new ArrayList<>();

        when( messageService.sendMessage(
            anyString(),
            anyString(),
            anyString(),
            anySetOf( User.class ),
            any( User.class ),
            anyBoolean(),
            anyBoolean()
        ) ).then(
            invocation -> {
                sentMessages.add( new Message( invocation.getArguments() ) );
                return anyInt();
            }
        );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testTestSetupWorksAsExpected() // TODO Remove
    {
        messageService.sendMessage( "test", "test", null, Sets.newHashSet(), createUser( 'A' ), false, false );
        Assert.assertEquals( 1, sentMessages.size() );
    }

    // -------------------------------------------------------------------------
    // Mocking classes
    // -------------------------------------------------------------------------

    /**
     * Mocks the input to MessageService.sendMessage(..)
     */
    static class Message
    {
        final String subject, text, metaData;
        final Set<User> users;
        final User sender;
        final boolean includeFeedbackRecipients, forceNotifications;

        @SuppressWarnings( "unchecked" )
        Message( Object[] args )
        {
            this(
                (String) args[0],
                (String) args[1],
                (String) args[2],
                (Set<User>) args[3],
                (User) args[4],
                (boolean) args[5],
                (boolean) args[6]
            );
        }

        Message(
            String subject,
            String text,
            String metaData,
            Set<User> users,
            User sender,
            boolean includeFeedbackRecipients,
            boolean forceNotifications )
        {
            this.subject = subject;
            this.text = text;
            this.metaData = metaData;
            this.users = users;
            this.sender = sender;
            this.includeFeedbackRecipients = includeFeedbackRecipients;
            this.forceNotifications = forceNotifications;
        }
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------
}
