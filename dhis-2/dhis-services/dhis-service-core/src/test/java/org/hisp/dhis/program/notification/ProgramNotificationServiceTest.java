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

import com.google.api.client.util.Lists;
import com.google.common.collect.Sets;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.ProgramNotificationMessageRenderer;
import org.hisp.dhis.notification.ProgramStageNotificationMessageRenderer;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.*;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.trackedentitydatavalue.TrackedEntityDataValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.*;

/**
 * @Author Zubair Asghar.
 */
@RunWith( MockitoJUnitRunner.class )
public class ProgramNotificationServiceTest extends DhisConvenienceTest
{
    private static final String SUBJECT = "subject";
    private static final String MESSAGE = "message";
    private static final String TEMPLATE_NAME = "message";
    private static final String OU_PHONE_NUMBER = "471000000";
    private static final String DE_PHONE_NUMBER = "47200000";
    private static final String ATT_PHONE_NUMBER = "473000000";
    private static final String USERA_PHONE_NUMBER = "47400000";
    private static final String USERB_PHONE_NUMBER = "47500000";
    private static final String ATT_EMAIL = "attr@test.org";
    private static final String DE_EMAIL = "de@test.org";

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

    @InjectMocks
    private DefaultProgramNotificationService programNotificationService;

    private Set<ProgramInstance> programInstances = new HashSet<>();
    private Set<ProgramStageInstance> programStageInstances = new HashSet<>();
    private List<ProgramMessage> sentProgramMessages = new ArrayList<>();
    private List<MockMessage> sentInternalMessages = new ArrayList<>();
    private User userA;
    private User userB;
    private UserGroup userGroup;

    private User userLvlTwoLeftLeft;
    private User userLvlTwoLeftRight;
    private User userLvlOneLeft;
    private User userLvlOneRight;
    private User userRoot;
    private UserGroup userGroupBasedOnHierarchy;
    private UserGroup userGroupBasedOnParent;

    private OrganisationUnit root;
    private OrganisationUnit lvlOneLeft;
    private OrganisationUnit lvlOneRight;
    private OrganisationUnit lvlTwoLeftLeft;
    private OrganisationUnit lvlTwoLeftRight;

    private TrackedEntityInstance tei;

    private DataElement dataElement;
    private DataElement dataElementEmail;
    private TrackedEntityDataValue dataValue;
    private TrackedEntityDataValue dataValueEmail;

    private TrackedEntityAttribute trackedEntityAttribute;
    private TrackedEntityAttribute trackedEntityAttributeEmail;
    private ProgramTrackedEntityAttribute programTrackedEntityAttribute;
    private ProgramTrackedEntityAttribute programTrackedEntityAttributeEmail;
    private TrackedEntityAttributeValue attributeValue;
    private TrackedEntityAttributeValue attributeValueEmail;

    private NotificationMessage notificationMessage;

    private ProgramNotificationTemplate programNotificationTemplate;
    private ProgramNotificationTemplate programNotificationTemplateForToday;
    private ProgramNotificationInstance programNotificationInstaceForToday;
    private ProgramNotificationTemplate programNotificationTemplateForYesterday;

    @Before
    @SuppressWarnings("unchecked")
    public void initTest()
    {
        programNotificationService.setProgramStageNotificationRenderer( programStageNotificationMessageRenderer );
        programNotificationService.setProgramNotificationRenderer( programNotificationMessageRenderer );

        setUpInstances();

        BatchResponseStatus status = new BatchResponseStatus(Collections.emptyList());
        when( programMessageService.sendMessages( anyList() ) )
            .thenAnswer( invocation -> {
                sentProgramMessages.addAll( (List<ProgramMessage>) invocation.getArguments()[0] );
                return status;
            } );

        when( messageService.sendMessage( any() ) )
            .thenAnswer( invocation -> {
                sentInternalMessages.add( new MockMessage( invocation.getArguments() ) );
                return 40;
            } );

        when( programInstanceStore.getWithScheduledNotifications( any(), any()) )
            .thenReturn( Lists.newArrayList( programInstances ) );
        when( programStageInstanceStore.getWithScheduledNotifications( any(), any() ) )
            .thenReturn( Lists.newArrayList( programStageInstances ) );

        when( manager.getAll( ProgramNotificationTemplate.class ) )
            .thenReturn( Collections.singletonList( programNotificationTemplate ) );

        when( programNotificationMessageRenderer.render( any(), any() ) )
            .thenReturn( notificationMessage );

        when( programStageNotificationMessageRenderer.render( any(), any() ) )
            .thenReturn( notificationMessage );
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

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

    @Test
    public void testSendCompletionNotification()
    {
        programNotificationService.sendCompletionNotifications( programInstances.iterator().next() );

        assertEquals( 1, sentProgramMessages.size() );

        ProgramMessage programMessage = sentProgramMessages.iterator().next();

        assertEquals( TrackedEntityInstance.class, programMessage.getRecipients().getTrackedEntityInstance().getClass() );
        assertEquals( tei, programMessage.getRecipients().getTrackedEntityInstance() );
    }

    @Test
    public void testSendEnrollmentNotification()
    {
        programNotificationTemplate.setNotificationTrigger( NotificationTrigger.ENROLLMENT );

        programNotificationService.sendEnrollmentNotifications( programInstances.iterator().next() );

        assertEquals( 1, sentProgramMessages.size() );

        ProgramMessage programMessage = sentProgramMessages.iterator().next();

        assertEquals( TrackedEntityInstance.class, programMessage.getRecipients().getTrackedEntityInstance().getClass() );
        assertEquals( tei, programMessage.getRecipients().getTrackedEntityInstance() );
    }

    @Test
    public void testUserGroupRecipient()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.USER_GROUP );
        programNotificationTemplate.setRecipientUserGroup( userGroup );

        programNotificationService.sendCompletionNotifications( programInstances.iterator().next() );

        assertEquals( 1, sentInternalMessages.size() );

        MockMessage mockMessage = sentInternalMessages.iterator().next();

        assertTrue( mockMessage.users.contains( userA ) );
        assertTrue( mockMessage.users.contains( userB ) );
    }

    @Test
    public void testOuContactRecipient()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.ORGANISATION_UNIT_CONTACT );

        programNotificationService.sendCompletionNotifications( programInstances.iterator().next() );

        assertEquals( 1, sentProgramMessages.size() );

        ProgramMessage programMessage = sentProgramMessages.iterator().next();

        assertEquals( OrganisationUnit.class, programMessage.getRecipients().getOrganisationUnit().getClass() );
        assertEquals( lvlTwoLeftLeft, programMessage.getRecipients().getOrganisationUnit() );
    }

    @Test
    public void testProgramAttributeRecipientWithSMS()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.PROGRAM_ATTRIBUTE );
        programNotificationTemplate.setRecipientProgramAttribute( trackedEntityAttribute );
        programNotificationTemplate.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.SMS ) );

        programNotificationService.sendCompletionNotifications( programInstances.iterator().next() );

        assertEquals( 1, sentProgramMessages.size() );

        ProgramMessage programMessage = sentProgramMessages.iterator().next();

        assertTrue( programMessage.getRecipients().getPhoneNumbers().contains( ATT_PHONE_NUMBER ) );
        assertTrue( programMessage.getDeliveryChannels().contains( DeliveryChannel.SMS ) );
    }

    @Test
    public void testProgramAttributeRecipientWithEMAIL()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.PROGRAM_ATTRIBUTE );
        programNotificationTemplate.setRecipientProgramAttribute( trackedEntityAttribute );
        programNotificationTemplate.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.EMAIL ) );

        programNotificationService.sendCompletionNotifications( programInstances.iterator().next() );

        assertEquals( 1, sentProgramMessages.size() );

        ProgramMessage programMessage = sentProgramMessages.iterator().next();

        assertTrue( programMessage.getRecipients().getEmailAddresses().contains( ATT_EMAIL ) );
        assertTrue( programMessage.getDeliveryChannels().contains( DeliveryChannel.EMAIL ) );
    }

    @Test
    public void testDataElementRecipientWithSMS()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.DATA_ELEMENT );
        programNotificationTemplate.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.SMS ) );
        programNotificationTemplate.setRecipientDataElement( dataElement );

        ProgramStageInstance programStageInstance = programStageInstances.iterator().next();

        programNotificationService.sendCompletionNotifications( programStageInstance );

        // no message when no template is attached
        assertEquals( 0, sentProgramMessages.size() );

        programStageInstance.getProgramStage().getNotificationTemplates().add( programNotificationTemplate );

        programNotificationService.sendCompletionNotifications( programStageInstance );

        assertEquals( 1, sentProgramMessages.size() );

    }

    @Test
    public void testDataElementRecipientWithEmail()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.DATA_ELEMENT );
        programNotificationTemplate.setDeliveryChannels( Sets.newHashSet( DeliveryChannel.EMAIL ) );
        programNotificationTemplate.setRecipientDataElement( dataElementEmail );

        ProgramStageInstance programStageInstance = programStageInstances.iterator().next();

        programNotificationService.sendCompletionNotifications( programStageInstance );

        // no message when no template is attached
        assertEquals( 0, sentProgramMessages.size() );

        programStageInstance.getProgramStage().getNotificationTemplates().add( programNotificationTemplate );

        programNotificationService.sendCompletionNotifications( programStageInstance );

        assertEquals( 1, sentProgramMessages.size() );
    }

    @Test
    public void testDataElementRecipientWithInternalRecipients()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.USER_GROUP );

        programNotificationTemplate.setRecipientUserGroup( userGroup );

        ProgramStageInstance programStageInstance = programStageInstances.iterator().next();

        programNotificationService.sendCompletionNotifications( programStageInstance );

        // no message when no template is attached
        assertEquals( 0, sentInternalMessages.size() );

        programStageInstance.getProgramStage().getNotificationTemplates().add( programNotificationTemplate );

        programNotificationService.sendCompletionNotifications( programStageInstance );

        assertEquals( 1, sentInternalMessages.size() );

        assertTrue( sentInternalMessages.iterator().next().users.contains( userA ) );
        assertTrue( sentInternalMessages.iterator().next().users.contains( userB ) );
    }

    @Test
    public void testSendToParent()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.USER_GROUP );

        programNotificationTemplate.setRecipientUserGroup( userGroupBasedOnParent );
        programNotificationTemplate.setNotifyParentOrganisationUnitOnly( true );

        ProgramStageInstance programStageInstance = programStageInstances.iterator().next();
        programStageInstance.getProgramStage().getNotificationTemplates().add( programNotificationTemplate );

        programNotificationService.sendCompletionNotifications( programStageInstance );

        assertEquals( 1, sentInternalMessages.size() );

        Set<User> users = sentInternalMessages.iterator().next().users;

        assertEquals( 1, users.size() );
        assertTrue( users.contains( userLvlOneLeft ) );
    }

    @Test
    public void testSendToHierarchy()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.USER_GROUP );

        programNotificationTemplate.setRecipientUserGroup( userGroupBasedOnHierarchy );
        programNotificationTemplate.setNotifyUsersInHierarchyOnly( true );

        ProgramStageInstance programStageInstance = programStageInstances.iterator().next();
        programStageInstance.getProgramStage().getNotificationTemplates().add( programNotificationTemplate );

        programNotificationService.sendCompletionNotifications( programStageInstance );

        assertEquals( 1, sentInternalMessages.size() );

        Set<User> users = sentInternalMessages.iterator().next().users;

        assertEquals( 3, users.size() );
        assertTrue( users.contains( userLvlTwoLeftLeft ) );
        assertTrue( users.contains( userLvlOneLeft ) );
        assertTrue( users.contains( userRoot ) );

        assertFalse( users.contains( userLvlTwoLeftRight ) );
        assertFalse( users.contains( userLvlOneRight ) );
    }

    @Test
    public void testSendToUsersAtOu()
    {
        programNotificationTemplate.setNotificationRecipient( ProgramNotificationRecipient.USERS_AT_ORGANISATION_UNIT );

        lvlTwoLeftLeft.getUsers().add( userLvlTwoLeftRight );

        ProgramStageInstance programStageInstance = programStageInstances.iterator().next();
        programStageInstance.getProgramStage().getNotificationTemplates().add( programNotificationTemplate );

        programNotificationService.sendCompletionNotifications( programStageInstance );

        assertEquals( 1, sentInternalMessages.size() );

        Set<User> users = sentInternalMessages.iterator().next().users;

        assertEquals( 2, users.size() );
        assertTrue( users.contains( userLvlTwoLeftLeft ) );
        assertTrue( users.contains( userLvlTwoLeftRight ) );
    }

    @Test
    public void testScheduledNotifications()
    {
        sentProgramMessages.clear();

        when( manager.getAll( ProgramNotificationInstance.class ) )
            .thenReturn( Collections.singletonList( programNotificationInstaceForToday ) );

        programNotificationService.sendScheduledNotifications();

        assertEquals( 1, sentProgramMessages.size() );
    }

    @Test
    public void testScheduledNotificationsWithDateInPast()
    {
        sentInternalMessages.clear();

        when( manager.getAll( ProgramNotificationTemplate.class ) )
            .thenReturn( Collections.singletonList( programNotificationTemplateForYesterday ) );

        programNotificationService.sendScheduledNotifications();

        assertEquals( 0, sentProgramMessages.size() );
    }
    
    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void setUpInstances()
    {
        programNotificationTemplate = createProgramNotificationTemplate( TEMPLATE_NAME, 0, NotificationTrigger.COMPLETION, ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE );

        java.util.Calendar cal = java.util.Calendar.getInstance();

        Date today = cal.getTime();
        cal.add( java.util.Calendar.DATE, -1 );
        Date yesterday = cal.getTime();

        programNotificationTemplateForToday = createProgramNotificationTemplate( TEMPLATE_NAME, 0, NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE, today );
        programNotificationTemplateForYesterday = createProgramNotificationTemplate( TEMPLATE_NAME, 0, NotificationTrigger.PROGRAM_RULE, ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE, yesterday );

        programNotificationInstaceForToday = new ProgramNotificationInstance();
        programNotificationInstaceForToday.setProgramNotificationTemplate( programNotificationTemplateForToday );
        programNotificationInstaceForToday.setName( programNotificationTemplateForToday.getName() );
        programNotificationInstaceForToday.setAutoFields();
        programNotificationInstaceForToday.setScheduledAt( today );

        root = createOrganisationUnit( 'R' );
        lvlOneLeft = createOrganisationUnit( '1' );
        lvlOneRight = createOrganisationUnit( '2' );
        lvlTwoLeftLeft = createOrganisationUnit( '3' );
        lvlTwoLeftLeft.setPhoneNumber( OU_PHONE_NUMBER );
        lvlTwoLeftRight = createOrganisationUnit( '4' );

        configureHierarchy( root, lvlOneLeft, lvlOneRight, lvlTwoLeftLeft, lvlTwoLeftRight );

        // User and UserGroup

        userA = createUser( 'U' );
        userA.setPhoneNumber( USERA_PHONE_NUMBER );
        userA.getOrganisationUnits().add( lvlTwoLeftLeft );

        userB = createUser( 'V' );
        userB.setPhoneNumber( USERB_PHONE_NUMBER );
        userB.getOrganisationUnits().add( lvlTwoLeftLeft );

        userGroup = createUserGroup( 'G', Sets.newHashSet( userA, userB ) );

        // User based on hierarchy

        userLvlTwoLeftLeft = createUser( 'K' );
        userLvlTwoLeftLeft.getOrganisationUnits().add( lvlTwoLeftLeft );
        lvlTwoLeftLeft.getUsers().add( userLvlTwoLeftLeft );

        userLvlTwoLeftRight = createUser( 'L' );
        userLvlTwoLeftRight.getOrganisationUnits().add( lvlTwoLeftRight );
        lvlTwoLeftRight.getUsers().add( userLvlTwoLeftRight );

        userLvlOneLeft = createUser( 'M' );
        userLvlOneLeft.getOrganisationUnits().add( lvlOneLeft );
        lvlOneLeft.getUsers().add( userLvlOneLeft );

        userLvlOneRight = createUser( 'N' );
        userLvlOneRight.getOrganisationUnits().add( lvlOneRight );
        lvlOneRight.getUsers().add( userLvlOneLeft );

        userRoot = createUser( 'R' );
        userRoot.getOrganisationUnits().add( root );
        root.getUsers().add( userRoot );

        userGroupBasedOnHierarchy = createUserGroup( 'H', Sets.newHashSet( userLvlOneLeft, userLvlOneRight, userLvlTwoLeftLeft, userLvlTwoLeftRight, userRoot ) );
        userGroupBasedOnParent = createUserGroup( 'H', Sets.newHashSet( userLvlTwoLeftLeft, userLvlTwoLeftRight ) );

        // Program
        Program programA = createProgram( 'A' );
        programA.setAutoFields();
        programA.setOrganisationUnits( Sets.newHashSet( lvlTwoLeftLeft,lvlTwoLeftRight ) );
        programA.setNotificationTemplates( Sets.newHashSet( programNotificationTemplate, programNotificationTemplateForToday ) );
        programA.getProgramAttributes().add( programTrackedEntityAttribute );

        trackedEntityAttribute = createTrackedEntityAttribute( 'T' );
        trackedEntityAttributeEmail = createTrackedEntityAttribute( 'E' );
        trackedEntityAttribute.setValueType( ValueType.PHONE_NUMBER );
        trackedEntityAttribute.setValueType( ValueType.EMAIL );
        programTrackedEntityAttribute = createProgramTrackedEntityAttribute( 'O' );
        programTrackedEntityAttributeEmail = createProgramTrackedEntityAttribute( 'L' );
        programTrackedEntityAttribute.setAttribute( trackedEntityAttribute );
        programTrackedEntityAttributeEmail.setAttribute( trackedEntityAttributeEmail );

        // ProgramStage
        ProgramStage programStage = createProgramStage( 'S', programA );

        dataElement = createDataElement( 'D' );
        dataElementEmail = createDataElement( 'E' );
        dataElement.setValueType( ValueType.PHONE_NUMBER );
        dataElementEmail.setValueType( ValueType.EMAIL );

        // ProgramInstance & TEI
        tei = new TrackedEntityInstance();
        tei.setAutoFields();
        tei.setOrganisationUnit( lvlTwoLeftLeft );

        attributeValue = createTrackedEntityAttributeValue( 'P', tei, trackedEntityAttribute );
        attributeValueEmail = createTrackedEntityAttributeValue( 'E', tei, trackedEntityAttribute );
        attributeValue.setValue( ATT_PHONE_NUMBER );
        attributeValueEmail.setValue( ATT_EMAIL );
        tei.getTrackedEntityAttributeValues().add( attributeValue );
        tei.getTrackedEntityAttributeValues().add( attributeValueEmail );

        ProgramInstance programInstance = new ProgramInstance();
        programInstance.setAutoFields();
        programInstance.setProgram( programA );
        programInstance.setOrganisationUnit( lvlTwoLeftLeft );
        programInstance.setEntityInstance( tei );

        // ProgramStageInstance
        ProgramStageInstance programStageInstance = createProgramStageInstance();
        programStageInstance.setAutoFields();
        programStageInstance.setProgramInstance( programInstance );
        programStageInstance.setOrganisationUnit( lvlTwoLeftLeft );
        programStageInstance.setProgramStage( programStage );
        dataValue = new TrackedEntityDataValue();
        dataValue.setAutoFields();
        dataValue.setDataElement( dataElement );
        dataValue.setValue( DE_PHONE_NUMBER );

        dataValueEmail = new TrackedEntityDataValue();
        dataValueEmail.setAutoFields();
        dataValueEmail.setDataElement( dataElementEmail );
        dataValueEmail.setValue( DE_EMAIL );

        programStageInstance.getDataValues().add( dataValue );

        // lists returned by stubs
        programStageInstances.add( programStageInstance );
        programInstances.add( programInstance );

        programNotificationInstaceForToday.setProgramInstance( programInstance );

        notificationMessage = new NotificationMessage( SUBJECT, MESSAGE );
    }

    static class MockMessage
    {
        final String subject, text, metaData;

        final Set<User> users;

        final User sender;

        final boolean includeFeedbackRecipients, forceNotifications;

        /**
         * Danger danger! Will break if MessageService API changes.
         */
        MockMessage( Object[] args )
        {
            MessageConversationParams params = (MessageConversationParams) args[0];
            this.subject = params.getSubject();
            this.text = params.getText();
            this.metaData = params.getMetadata();
            this.users = params.getRecipients();
            this.sender = params.getSender();
            this.includeFeedbackRecipients = false;
            this.forceNotifications = params.isForceNotification();
        }
    }
}
