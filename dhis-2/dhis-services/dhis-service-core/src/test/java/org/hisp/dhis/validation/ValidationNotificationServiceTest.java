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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.dataelement.DataElementCategoryOptionCombo;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.ValidationNotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DefaultPeriodService;
import org.hisp.dhis.period.Period;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Tests for the business logic implemented in ValidationNotificationService.
 *
 * The actual rendering of the messages is not tested here, only the logic
 * responsible for generating and sending the messages/summaries for each recipient.
 *
 * See {@link org.hisp.dhis.notification.BaseNotificationMessageRendererTest}.
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

    @InjectMocks
    private DefaultPeriodService periodService;

    private List<MockMessage> sentMessages;

    /**
     * We mock the sending of messages to write to a local List (which we can inspect).
     * Also, the renderer is replaced with a mock which returns a static subject/message-pair.
     */
    @Before
    public void initTest()
    {
        sentMessages = new ArrayList<>();

        // Stub MessageService.sendMessage(..) so that it appends any outgoing messages to our List
        when(
            messageService.sendValidationResultMessage(
                anyString(),
                anyString(),
                anySetOf( User.class )
            )
        ).then(
            invocation -> {
                sentMessages.add( new MockMessage( invocation.getArguments() ) );
                return 42;
            }
        );

        // Stub renderer
        when(
            renderer.render( any(), any() )
        ).thenReturn(
            new NotificationMessage( STATIC_MOCK_SUBJECT, STATIC_MOCK_MESSAGE )
        );
    }

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------

    private OrganisationUnit orgUnitA;
    private DataElementCategoryOptionCombo catOptCombo = createCategoryOptionCombo( 'A', 'r', 'i', 'b', 'a' );
    private ValidationRule valRuleA;
    private UserGroup userGroupA;

    int idCounter = 0;

    private void setUpEntitiesA()
    {
        User userA = createUser( 'A' );

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

        ValidationNotificationTemplate templateA = createValidationNotificationTemplate( "Template A" );
        templateA.addValidationRule( valRuleA );
        templateA.setRecipientUserGroups( Sets.newHashSet( userGroupA ) );
    }

    private ValidationResult createValidationResult( OrganisationUnit ou, ValidationRule rule )
    {
        Period period = createPeriod( "2017Q1" );
        ValidationResult vr = new ValidationResult(
            rule,
            period,
            ou,
            catOptCombo,
            RandomUtils.nextDouble( 10, 1000 ),
            RandomUtils.nextDouble( 10, 1000 ),
            periodService.getDayInPeriod( period, new Date() )
        );

        vr.setId( idCounter++ );

        return vr;
    }

    private ValidationResult createValidationResultA()
    {
        Period period = createPeriod( "2017Q1" );
        ValidationResult vr = new ValidationResult(
            valRuleA,
            period,
            orgUnitA,
            catOptCombo,
            RandomUtils.nextDouble( 10, 1000 ),
            RandomUtils.nextDouble( 10, 1000 ),
            periodService.getDayInPeriod( period, new Date() )
        );

        vr.setId( idCounter++ );

        return vr;
    }

    /*
     * Configure org unit hierarchy like so:
     *
     *                  Root
     *                 /   \
     *       lvlOneLeft    lvlOneRight
     *               / \
     *  lvlTwoLeftLeft  lvlTwoLeftRight
     */
    private static void configureHierarchy( OrganisationUnit root, OrganisationUnit lvlOneLeft, OrganisationUnit lvlOneRight,
        OrganisationUnit lvlTwoLeftLeft, OrganisationUnit lvlTwoLeftRight )
    {
        root.getChildren().addAll( Sets.newHashSet( lvlOneLeft, lvlOneRight ) );
        lvlOneLeft.setParent( root );
        lvlOneRight.setParent( root );

        lvlOneLeft.getChildren().addAll( Sets.newHashSet( lvlTwoLeftLeft, lvlTwoLeftRight ) );
        lvlTwoLeftLeft.setParent( lvlOneLeft );
        lvlTwoLeftRight.setParent( lvlOneLeft );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    public void testNoValidationResultsCausesNoNotificationsSent()
        throws Exception
    {
        Set<ValidationResult> emptyResultsSet = Collections.emptySet();

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
        User userB = createUser( 'B' );
        userGroupA.addUser( userB );

        ValidationResult validationResult = createValidationResultA();

        service.sendNotifications( Sets.newHashSet( validationResult ) );

        assertEquals( 1, sentMessages.size() );
        assertEquals( 2, sentMessages.get( 0 ).users.size() );
    }

    @Test
    public void testMultipleValidationResultsAreSummarized()
        throws Exception
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

    @Test
    public void testNotifyUsersInHierarchyLimitsRecipients()
        throws Exception
    {
        // Complicated fixtures. Sorry to whomever has to read this...

        // Org units
        OrganisationUnit root            = createOrganisationUnit( 'R' ),
                         lvlOneLeft      = createOrganisationUnit( '1' ),
                         lvlOneRight     = createOrganisationUnit( '2' ),
                         lvlTwoLeftLeft  = createOrganisationUnit( '3' ),
                         lvlTwoLeftRight = createOrganisationUnit( '4' );

        configureHierarchy( root, lvlOneLeft, lvlOneRight, lvlTwoLeftLeft, lvlTwoLeftRight );

        // Users
        User uA = createUser( 'A' ),
             uB = createUser( 'B' ),
             uC = createUser( 'C' ),
             uD = createUser( 'D' ),
             uE = createUser( 'E' ),
             uF = createUser( 'F' ),
             uG = createUser( 'G' );

        root.addUser( uA );

        lvlOneLeft.addUser( uB );
        lvlOneLeft.addUser( uC );

        lvlOneRight.addUser( uD );
        lvlOneRight.addUser( uE );

        lvlTwoLeftLeft.addUser( uF );
        lvlTwoLeftRight.addUser( uG );

        // User groups
        UserGroup ugA = createUserGroup( 'A', Sets.newHashSet() );
        ugA.addUser( uB );
        ugA.addUser( uC );
        ugA.addUser( uD );
        ugA.addUser( uE );
        ugA.addUser( uF );
        ugA.addUser( uG );

        UserGroup ugB = createUserGroup( 'B', Sets.newHashSet() );
        ugB.addUser( uA );

        // Validation rule and template
        ValidationRule rule = createValidationRule(
            'V',
            Operator.equal_to,
            createExpression2( 'A', "X" ),
            createExpression2( 'B', "Y" ),
            PeriodType.getPeriodTypeByName( QuarterlyPeriodType.NAME )
        );

        ValidationNotificationTemplate template = createValidationNotificationTemplate( "My fancy template" );
        template.setNotifyUsersInHierarchyOnly( true );

        template.addValidationRule( rule );
        template.setRecipientUserGroups( Sets.newHashSet( ugA ) );

        // Create a validationResult that emanates from the middle of the left branch
        final ValidationResult resultFromMiddleLeft = createValidationResult( lvlOneLeft, rule );

        // Perform tests

        // Uno

        service.sendNotifications( Sets.newHashSet( resultFromMiddleLeft ) );

        assertEquals( 1, sentMessages.size() );

        Set<User> rcpt = sentMessages.iterator().next().users;

        assertEquals( 2, rcpt.size() );
        assertTrue( rcpt.containsAll( Sets.newHashSet( uB, uC ) ) );

        // Dos

        sentMessages = new ArrayList<>();

        // Add the second group (with user F) to the recipients
        template.getRecipientUserGroups().add( ugB );

        service.sendNotifications( Sets.newHashSet( resultFromMiddleLeft ) );

        assertEquals( 1, sentMessages.size() );
        rcpt = sentMessages.iterator().next().users;

        // We now expect user A, which is on the root org unit and in group B to also be among the recipients
        assertEquals( 3, rcpt.size() );
        assertTrue( rcpt.containsAll( Sets.newHashSet( uA, uB, uC ) ) );

        // Tres

        sentMessages = new ArrayList<>();

        // Keep the hierarchy as is, but emanate the validation result from the bottom left of the tree

        final ValidationResult resultFromBottomLeft = createValidationResult( lvlTwoLeftLeft, rule );

        service.sendNotifications( Sets.newHashSet( resultFromBottomLeft ) );

        assertEquals( 1, sentMessages.size() );

        rcpt = sentMessages.iterator().next().users;

        assertEquals( 4, rcpt.size() );
        assertTrue( rcpt.containsAll( Sets.newHashSet( uA, uB, uC, uF ) ) );
    }

    // -------------------------------------------------------------------------
    // Mock classes
    // -------------------------------------------------------------------------

    /**
     * Mocks the input to MessageService.sendValidationResultMessage(..)
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
            this.subject = (String) args[0];
            this.text = (String) args[1];
            this.metaData = null;
            this.users = (Set<User>) args[2];
            this.sender = null;
            this.includeFeedbackRecipients = false;
            this.forceNotifications = false;
        }
    }
}
