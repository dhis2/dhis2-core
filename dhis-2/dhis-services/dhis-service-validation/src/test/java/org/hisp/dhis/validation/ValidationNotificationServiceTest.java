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
package org.hisp.dhis.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.expression.Operator;
import org.hisp.dhis.message.MessageConversationPriority;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.ValidationNotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.period.DefaultPeriodService;
import org.hisp.dhis.period.Period;
import org.hisp.dhis.period.PeriodStore;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.period.QuarterlyPeriodType;
import org.hisp.dhis.scheduling.NoopJobProgress;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.validation.notification.DefaultValidationNotificationService;
import org.hisp.dhis.validation.notification.ValidationNotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Sets;

/**
 * Tests for the business logic implemented in ValidationNotificationService.
 * <p>
 * The actual rendering of the messages is not tested here, only the logic
 * responsible for generating and sending the messages/summaries for each
 * recipient.
 * <p>
 *
 * @author Halvdan Hoem Grelland
 */
@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class ValidationNotificationServiceTest extends DhisConvenienceTest
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

    @Mock
    private ValidationResultService validationResultService;

    @Mock
    private PeriodStore periodStore;

    private DefaultPeriodService periodService;

    private DefaultValidationNotificationService subject;

    private List<MockMessage> sentMessages;

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------
    private OrganisationUnit orgUnitA;

    private CategoryOptionCombo catOptCombo = createCategoryOptionCombo( 'A', 'r', 'i', 'b', 'a' );

    private ValidationRule valRuleA;

    private UserGroup userGroupA;

    private int idCounter = 0;

    /*
     * Configure org unit hierarchy like so:
     *
     * Root / \ lvlOneLeft lvlOneRight / \ lvlTwoLeftLeft lvlTwoLeftRight
     */
    /**
     * We mock the sending of messages to write to a local List (which we can
     * inspect). Also, the renderer is replaced with a mock which returns a
     * static subject/message-pair.
     */
    @BeforeEach
    void initTest()
    {
        subject = new DefaultValidationNotificationService( renderer, messageService, validationResultService );
        this.periodService = new DefaultPeriodService( periodStore );
        sentMessages = new ArrayList<>();
        when( messageService.sendValidationMessage( anySet(), anyString(), anyString(),
            any( MessageConversationPriority.class ) ) ).then( invocation -> {
                sentMessages.add( new MockMessage( invocation.getArguments() ) );
                return 42L;
            } );
        // Stub renderer
        when( renderer.render( any(), any() ) )
            .thenReturn( new NotificationMessage( STATIC_MOCK_SUBJECT, STATIC_MOCK_MESSAGE ) );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------
    @Test
    void testNoValidationResultsCausesNoNotificationsSent()
    {
        Set<ValidationResult> emptyResultsSet = Collections.emptySet();
        subject.sendNotifications( emptyResultsSet, NoopJobProgress.INSTANCE );
        assertTrue( sentMessages.isEmpty(), "No messages should have been sent but was " + sentMessages.size() );
    }

    @Test
    void testValidationResultGeneratesNotification()
    {
        setUpEntitiesA();
        ValidationResult validationResult = createValidationResultA();
        subject.sendNotifications( Sets.newHashSet( validationResult ), NoopJobProgress.INSTANCE );
        assertEquals( 1, sentMessages.size(), "A single message should have been sent" );
    }

    @Test
    void testValidationResultGeneratesSingleNotificationForMultipleUsers()
    {
        setUpEntitiesA();
        User userB = makeUser( "B" );
        userGroupA.addUser( userB );
        ValidationResult validationResult = createValidationResultA();
        subject.sendNotifications( Sets.newHashSet( validationResult ), NoopJobProgress.INSTANCE );
        assertEquals( 1, sentMessages.size() );
        assertEquals( 2, sentMessages.get( 0 ).recipients.size() );
    }

    @Test
    void testMultipleValidationResultsAreSummarized()
    {
        setUpEntitiesA();
        Set<ValidationResult> results = IntStream.iterate( 0, i -> i + 1 ).limit( 10 ).boxed()
            .map( i -> createValidationResultA() ).collect( Collectors.toSet() );
        subject.sendNotifications( results, NoopJobProgress.INSTANCE );
        assertEquals( 1, sentMessages.size(), "The validation results should form a single summarized message" );
        String text = sentMessages.iterator().next().text;
        assertEquals( 10, StringUtils.countMatches( text, STATIC_MOCK_SUBJECT ),
            "Wrong number of messages in the summarized message" );
    }

    @Test
    void testNotifyParentOfUserInGroup()
    {
        OrganisationUnit root = createOrganisationUnit( 'R' ), lvlOneLeft = createOrganisationUnit( '1' ),
            lvlOneRight = createOrganisationUnit( '2' ), lvlTwoLeftLeft = createOrganisationUnit( '3' ),
            lvlTwoLeftRight = createOrganisationUnit( '4' );
        configureHierarchy( root, lvlOneLeft, lvlOneRight, lvlTwoLeftLeft, lvlTwoLeftRight );
        // Users
        User uB = makeUser( "B" ), uC = makeUser( "C" ), uD = makeUser( "D" ), uE = makeUser( "E" );
        UserGroup groupA = createUserGroup( 'A', Sets.newHashSet() );
        groupA.addUser( uD );
        groupA.addUser( uE );
        lvlOneLeft.addUser( uB );
        lvlOneRight.addUser( uC );
        lvlTwoLeftLeft.addUser( uD );
        lvlTwoLeftRight.addUser( uE );
        ValidationRule rule = createValidationRule( 'V', Operator.equal_to, createExpression2( 'A', "X" ),
            createExpression2( 'B', "Y" ), PeriodType.getPeriodTypeByName( QuarterlyPeriodType.NAME ) );
        ValidationNotificationTemplate template = createValidationNotificationTemplate( "My fancy template" );
        template.setNotifyParentOrganisationUnitOnly( true );
        template.addValidationRule( rule );
        template.setRecipientUserGroups( Sets.newHashSet( groupA ) );
        final ValidationResult validationResult = createValidationResult( lvlOneLeft, rule );
        subject.sendNotifications( Sets.newHashSet( validationResult ), NoopJobProgress.INSTANCE );
        assertEquals( 1, sentMessages.size() );
        Collection<User> rcpt = sentMessages.iterator().next().recipients;
        assertEquals( 1, rcpt.size() );
    }

    @Test
    void testNotifyUsersInHierarchyLimitsRecipients()
    {
        // Complicated fixtures. Sorry to whomever has to read this...
        // Org units
        OrganisationUnit root = createOrganisationUnit( 'R' ), lvlOneLeft = createOrganisationUnit( '1' ),
            lvlOneRight = createOrganisationUnit( '2' ), lvlTwoLeftLeft = createOrganisationUnit( '3' ),
            lvlTwoLeftRight = createOrganisationUnit( '4' );
        configureHierarchy( root, lvlOneLeft, lvlOneRight, lvlTwoLeftLeft, lvlTwoLeftRight );
        // Users
        User uA = makeUser( "A" ), uB = makeUser( "B" ), uC = makeUser( "C" ), uD = makeUser( "D" ),
            uE = makeUser( "E" ), uF = makeUser( "F" ), uG = makeUser( "G" );
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
        ValidationRule rule = createValidationRule( 'V', Operator.equal_to, createExpression2( 'A', "X" ),
            createExpression2( 'B', "Y" ), PeriodType.getPeriodTypeByName( QuarterlyPeriodType.NAME ) );
        ValidationNotificationTemplate template = createValidationNotificationTemplate( "My fancy template" );
        template.setNotifyUsersInHierarchyOnly( true );
        template.addValidationRule( rule );
        template.setRecipientUserGroups( Sets.newHashSet( ugA ) );
        // Create a validationResult that emanates from the middle of the left
        // branch
        final ValidationResult resultFromMiddleLeft = createValidationResult( lvlOneLeft, rule );
        // Perform tests
        // One
        subject.sendNotifications( Sets.newHashSet( resultFromMiddleLeft ), NoopJobProgress.INSTANCE );
        assertEquals( 1, sentMessages.size() );
        Collection<User> rcpt = sentMessages.iterator().next().recipients;
        assertEquals( 2, rcpt.size() );
        assertTrue( rcpt.containsAll( Sets.newHashSet( uB, uC ) ) );
        // Two
        sentMessages = new ArrayList<>();
        // Add the second group (with user F) to the recipients
        template.getRecipientUserGroups().add( ugB );
        subject.sendNotifications( Sets.newHashSet( resultFromMiddleLeft ), NoopJobProgress.INSTANCE );
        assertEquals( 1, sentMessages.size() );
        rcpt = sentMessages.iterator().next().recipients;
        // We now expect user A, which is on the root org unit and in group B to
        // also be among the recipients
        assertEquals( 3, rcpt.size() );
        assertTrue( rcpt.containsAll( Sets.newHashSet( uA, uB, uC ) ) );
        // Three
        sentMessages = new ArrayList<>();
        // Keep the hierarchy as is, but spread out the validation result from
        // the bottom left of the tree
        final ValidationResult resultFromBottomLeft = createValidationResult( lvlTwoLeftLeft, rule );
        subject.sendNotifications( Sets.newHashSet( resultFromBottomLeft ), NoopJobProgress.INSTANCE );
        assertEquals( 1, sentMessages.size() );
        rcpt = sentMessages.iterator().next().recipients;
        assertEquals( 4, rcpt.size() );
        assertTrue( rcpt.containsAll( Sets.newHashSet( uA, uB, uC, uF ) ) );
    }

    private void setUpEntitiesA()
    {
        User userA = makeUser( "A" );
        orgUnitA = createOrganisationUnit( 'A' );
        orgUnitA.addUser( userA );
        userGroupA = createUserGroup( 'A', Sets.newHashSet( userA ) );
        userA.setGroups( Sets.newHashSet( userGroupA ) );
        valRuleA = createValidationRule( 'A', Operator.equal_to, createExpression2( 'A', "X" ),
            createExpression2( 'B', "Y" ), PeriodType.getPeriodTypeByName( QuarterlyPeriodType.NAME ) );
        ValidationNotificationTemplate templateA = createValidationNotificationTemplate( "Template A" );
        templateA.addValidationRule( valRuleA );
        templateA.setRecipientUserGroups( Sets.newHashSet( userGroupA ) );
    }

    private ValidationResult createValidationResult( OrganisationUnit ou, ValidationRule rule )
    {
        Period period = createPeriod( "2017Q1" );
        ValidationResult vr = new ValidationResult( rule, period, ou, catOptCombo, RandomUtils.nextDouble( 10, 1000 ),
            RandomUtils.nextDouble( 10, 1000 ), periodService.getDayInPeriod( period, new Date() ) );
        vr.setId( idCounter++ );
        return vr;
    }

    private ValidationResult createValidationResultA()
    {
        Period period = createPeriod( "2017Q1" );
        ValidationResult vr = new ValidationResult( valRuleA, period, orgUnitA, catOptCombo,
            RandomUtils.nextDouble( 10, 1000 ), RandomUtils.nextDouble( 10, 1000 ),
            periodService.getDayInPeriod( period, new Date() ) );
        vr.setId( idCounter++ );
        return vr;
    }

    // -------------------------------------------------------------------------
    // Mock classes
    // -------------------------------------------------------------------------
    /**
     * Mocks the input to MessageService.sendValidationResultMessage(..)
     */
    static class MockMessage
    {

        final Collection<User> recipients;

        final String subject;

        final String text;

        /**
         * Danger danger! Will break if MessageService API changes.
         */
        @SuppressWarnings( "unchecked" )
        MockMessage( Object[] args )
        {
            this.recipients = (Collection<User>) args[0];
            this.subject = (String) args[1];
            this.text = (String) args[2];
        }
    }
}
