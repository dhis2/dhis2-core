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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.ValidationNotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.validation.notification.DefaultValidationNotificationService;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for the business logic implemented in ValidationNotificationService.
 *
 * The actual rendering of the messages is not tested here, only the logic
 * responsible for generating and sending the messages/summaries for each recipient.
 *
 * See {@link org.hisp.dhis.notification.BaseNotificationMessageRendererTest}.
 *
 * TODO
 *  - Add test for intersecting the recipients in case of template.notifyUsersInHierarchyOnly
 *  - Add test which asserts messages are split/summarized correctly for multiple recipient groups.
 *
 * @author Halvdan Hoem Grelland
 */
@RunWith( MockitoJUnitRunner.class )
public class ValidationNotificationServiceTest
    extends DhisConvenienceTest
{
    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private static final String STATIC_MOCK_SUBJECT = "Subject goes here";
    private static final String STATIC_MOCK_MESSAGE = "Message goes here";

    @Mock
    private ValidationNotificationMessageRenderer renderer;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private DefaultValidationNotificationService service;

    private List<MockMessage> sentMessages;

    @Before
    public void setUpTest()
    {
        setUpMocks();
    }

    /**
     * We mock the sending of messages to write to a local List (which we can inspect).
     * Also, the renderer is replaced with a mock which returns a static subject/message-pair.
     */
    private void setUpMocks()
    {
        resetState();

        // Stub MessageService.sendMessage(..) so that it appends any outgoing messages to our List
        when(
            messageService.sendMessage(
                anyString(),
                anyString(),
                anyString(),
                anySetOf( User.class ),
                any( User.class ),
                anyBoolean(),
                anyBoolean()
            )
        ).then(
            invocation -> {
                sentMessages.add( new MockMessage( invocation.getArguments() ) );
                return RandomUtils.nextInt( 100, 2000 );
            }
        );

        // Stub renderer
        when(
            renderer.render( any(), any() )
        ).thenReturn(
            new NotificationMessage( STATIC_MOCK_SUBJECT, STATIC_MOCK_MESSAGE )
        );
    }

    private void resetState()
    {
        sentMessages = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testTestSetupWorksAsExpected() throws Exception // TODO Remove
    {
        messageService.sendMessage( "test", "test", null, new HashSet<>(), createUser( 'A' ), false, false );
        assertEquals( 1, sentMessages.size() );

        messageService.sendMessage( "test", "test", null, new HashSet<>(), createUser( 'A' ), false, false );
        assertEquals( 2, sentMessages.size() );
    }

    @Test
    public void testNoValidationResultsCausesNoNotificationsSent()
        throws Exception
    {
        Set<ValidationResult> emptyResultsSet = Collections.emptySet();

        assertTrue( sentMessages.isEmpty() );

        service.sendNotifications( emptyResultsSet );

        assertTrue( "No messages should have been sent but was " + sentMessages.size(), sentMessages.isEmpty() );
    }

    @Test
    public void testValidationResultGeneratesNotification()
        throws Exception
    {
        setUpEntitiesA();
        ValidationResult validationResult = createValidationResultA();

        service.sendNotifications( Sets.newHashSet( validationResult ) );

        assertEquals( "A single message should have been sent", 1, sentMessages.size() );
    }

    @Test
    public void testValidationResultGeneratesSingleNotificationForMultipleUsers()
        throws Exception
    {
        setUpEntitiesA();
        userB = createUser( 'B' );
        userGroupA.addUser( userB );

        ValidationResult validationResult = createValidationResultA();

        service.sendNotifications( Sets.newHashSet( validationResult ) );

        assertEquals( 1, sentMessages.size() );
        assertEquals( 2, sentMessages.get( 0 ).users.size() );
    }

    @Test
    public void testMultipleValidationResultsAreSummarized()
    {
        setUpEntitiesA();

        Set<ValidationResult> results = IntStream.iterate( 0, i -> i + 1 ).limit( 10 )
            .boxed()
            .map( i -> createValidationResultA() )
            .collect( Collectors.toSet() );

        service.sendNotifications( results );

        assertEquals( "The validation results should form a single summarized message", 1, sentMessages.size() );

        String text = sentMessages.iterator().next().text;

        assertEquals(
            "Wrong number of messages in the summarized message", 10, StringUtils.countMatches( text, STATIC_MOCK_SUBJECT ) );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private OrganisationUnit orgUnitA, orgUnitB;
    private User userA, userB;
    private DataElementCategoryOptionCombo catOptCombo = createCategoryOptionCombo( 'A', 'x', 'y', 'z' );
    private ValidationRule valRuleA, valRuleB;
    private ValidationNotificationTemplate templateA, templateB;
    private UserGroup userGroupA, userGroupB;

    private void setUpEntitiesA()
    {
        userA = createUser( 'A' );

        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitA.addUser( userA );

        userGroupA = createUserGroup( 'A', Sets.newHashSet( userA ) );
        userA.setGroups( Sets.newHashSet( userGroupA ) );

        valRuleA = createValidationRule(
            'A',
            Operator.equal_to,
            createExpression2( 'A', "X" ),
            createExpression2( 'B', "Y" ),
            PeriodType.getPeriodTypeByName( QuarterlyPeriodType.NAME )
        );

        templateA = createValidationNotificationTemplate( "Template A" );
        templateA.addValidationRule( valRuleA );
        templateA.setRecipientUserGroups( Sets.newHashSet( userGroupA ) );
    }

    private void setUpEntitiesB()
    {
        userB = createUser( 'B' );

        orgUnitB = createOrganisationUnit( 'B' );
        orgUnitB.addUser( userB );

        userGroupB = createUserGroup( 'B', Sets.newHashSet( userB ) );
        userB.setGroups( Sets.newHashSet( userGroupB ) );

        valRuleB = createValidationRule(
            'B',
            Operator.equal_to,
            createExpression2( 'A', "X" ),
            createExpression2( 'B', "Y" ),
            PeriodType.getPeriodTypeByName( QuarterlyPeriodType.NAME )
        );

        templateB = createValidationNotificationTemplate( "Template B" );
        templateB.addValidationRule( valRuleB );
        templateB.setRecipientUserGroups( Sets.newHashSet( userGroupB ) );
    }

    private ValidationResult createValidationResultA() {

        return new ValidationResult(
            createPeriod( "2017Q1" ),
            orgUnitA,
            catOptCombo,
            valRuleA,
            RandomUtils.nextDouble( 10, 1000 ),
            RandomUtils.nextDouble( 10, 1000 )
        );
    }

    // -------------------------------------------------------------------------
    // Mock classes
    // -------------------------------------------------------------------------

    /**
     * Mocks the input to MessageService.sendMessage(..)
     */
    static class MockMessage
    {
        final String subject, text, metaData;
        final Set<User> users;
        final User sender;
        final boolean includeFeedbackRecipients, forceNotifications;

        /**
         * Danger danger! Will break if MessageService API changes.
         */
        @SuppressWarnings( "unchecked" )
        MockMessage( Object[] args )
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

        MockMessage(
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

        @Override
        public String toString()
        {
            return MoreObjects.toStringHelper( this )
                .add( "subject", subject )
                .add( "text", text )
                .add( "metaData", metaData )
                .add( "users", users )
                .add( "sender", sender )
                .add( "includeFeedbackRecipients", includeFeedbackRecipients )
                .add( "forceNotifications", forceNotifications )
                .toString();
        }
    }
}
