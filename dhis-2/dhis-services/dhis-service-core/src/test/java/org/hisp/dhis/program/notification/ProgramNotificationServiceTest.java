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
package org.hisp.dhis.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.DhisConvenienceTest;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.notification.NotificationTemplate;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceStore;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceStore;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.message.ProgramMessage;
import org.hisp.dhis.program.message.ProgramMessageService;
import org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateMapper;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentityattributevalue.TrackedEntityAttributeValue;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Zubair Asghar.
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ProgramNotificationServiceTest extends DhisConvenienceTest {

  private static final String SUBJECT = "subject";

  private static final String MESSAGE = "message";

  private static final String TEMPLATE_NAME = "message";

  private static final String OU_PHONE_NUMBER = "471000000";

  private static final String ATT_PHONE_NUMBER = "473000000";

  private static final String USERA_PHONE_NUMBER = "47400000";

  private static final String USERB_PHONE_NUMBER = "47500000";

  private static final String ATT_EMAIL = "attr@test.org";

  private String notificationTemplate = CodeGenerator.generateUid();

  @Mock private ProgramMessageService programMessageService;

  @Mock private MessageService messageService;

  @Mock private ProgramInstanceStore programInstanceStore;

  @Mock private ProgramStageInstanceStore programStageInstanceStore;

  @Mock private IdentifiableObjectManager manager;

  @Mock private NotificationMessageRenderer<ProgramInstance> programNotificationRenderer;

  @Mock private NotificationMessageRenderer<ProgramStageInstance> programStageNotificationRenderer;

  @Mock private ProgramNotificationTemplateService notificationTemplateService;

  private NotificationTemplateMapper notificationTemplateMapper = new NotificationTemplateMapper();

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

  @BeforeEach
  public void initTest() {
    programNotificationService =
        new DefaultProgramNotificationService(
            this.programMessageService,
            this.messageService,
            this.programInstanceStore,
            this.programStageInstanceStore,
            this.manager,
            this.programNotificationRenderer,
            this.programStageNotificationRenderer,
            notificationTemplateService,
            notificationTemplateMapper);

    setUpInstances();
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  // -------------------------------------------------------------------------

  @Test
  void testIfProgramInstanceIsNull() {
    when(programInstanceStore.get(anyLong())).thenReturn(null);

    programNotificationService.sendEnrollmentCompletionNotifications(0);

    verify(manager, never()).getAll(any());
  }

  @Test
  void testIfProgramStageInstanceIsNull() {
    when(programStageInstanceStore.get(anyLong())).thenReturn(null);

    programNotificationService.sendEventCompletionNotifications(0);

    verify(manager, never()).getAll(any());
  }

  @Test
  void testSendCompletionNotification() {
    when(programInstanceStore.get(anyLong())).thenReturn(programInstances.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(
            any(ProgramInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.COMPLETION);
    programNotificationService.sendEnrollmentCompletionNotifications(
        programInstances.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertEquals(
        TrackedEntityInstance.class,
        programMessage.getRecipients().getTrackedEntityInstance().getClass());
    assertEquals(tei, programMessage.getRecipients().getTrackedEntityInstance());
  }

  @Test
  void testSendEnrollmentNotification() {
    when(programInstanceStore.get(anyLong())).thenReturn(programInstances.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(
            any(ProgramInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.ENROLLMENT);

    programNotificationService.sendEnrollmentNotifications(
        programInstances.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertEquals(
        TrackedEntityInstance.class,
        programMessage.getRecipients().getTrackedEntityInstance().getClass());
    assertEquals(tei, programMessage.getRecipients().getTrackedEntityInstance());
  }

  @Test
  void testUserGroupRecipient() {
    when(programInstanceStore.get(anyLong())).thenReturn(programInstances.iterator().next());

    when(messageService.sendMessage(any()))
        .thenAnswer(
            invocation -> {
              sentInternalMessages.add(new MockMessage(invocation.getArguments()));
              return 40L;
            });

    when(programNotificationRenderer.render(
            any(ProgramInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    programNotificationTemplate.setRecipientUserGroup(userGroup);

    programNotificationService.sendEnrollmentNotifications(
        programInstances.iterator().next().getId());

    assertEquals(1, sentInternalMessages.size());

    MockMessage mockMessage = sentInternalMessages.iterator().next();

    assertTrue(mockMessage.users.contains(userA));
    assertTrue(mockMessage.users.contains(userB));
  }

  @Test
  void testOuContactRecipient() {
    when(programInstanceStore.get(anyLong())).thenReturn(programInstances.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(
            any(ProgramInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(
        ProgramNotificationRecipient.ORGANISATION_UNIT_CONTACT);

    programNotificationService.sendEnrollmentNotifications(
        programInstances.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertEquals(
        OrganisationUnit.class, programMessage.getRecipients().getOrganisationUnit().getClass());
    assertEquals(lvlTwoLeftLeft, programMessage.getRecipients().getOrganisationUnit());
    assertEquals(programMessage.getNotificationTemplate(), notificationTemplate);
  }

  @Test
  void testProgramAttributeRecipientWithSMS() {
    when(programInstanceStore.get(anyLong())).thenReturn(programInstances.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(
            any(ProgramInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(
        ProgramNotificationRecipient.PROGRAM_ATTRIBUTE);
    programNotificationTemplate.setRecipientProgramAttribute(trackedEntityAttribute);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.SMS));

    programNotificationService.sendEnrollmentNotifications(
        programInstances.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertTrue(programMessage.getRecipients().getPhoneNumbers().contains(ATT_PHONE_NUMBER));
    assertTrue(programMessage.getDeliveryChannels().contains(DeliveryChannel.SMS));
    assertEquals(programMessage.getNotificationTemplate(), notificationTemplate);
  }

  @Test
  void testProgramAttributeRecipientWithEMAIL() {
    when(programInstanceStore.get(anyLong())).thenReturn(programInstances.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(
            any(ProgramInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(
        ProgramNotificationRecipient.PROGRAM_ATTRIBUTE);
    programNotificationTemplate.setRecipientProgramAttribute(trackedEntityAttribute);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL));

    programNotificationService.sendEnrollmentNotifications(
        programInstances.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertTrue(programMessage.getRecipients().getEmailAddresses().contains(ATT_EMAIL));
    assertTrue(programMessage.getDeliveryChannels().contains(DeliveryChannel.EMAIL));
    assertEquals(programMessage.getNotificationTemplate(), notificationTemplate);
  }

  @Test
  void testDataElementRecipientWithSMS() {
    when(programStageInstanceStore.get(anyLong()))
        .thenReturn(programStageInstances.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programStageNotificationRenderer.render(
            any(ProgramStageInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.DATA_ELEMENT);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.SMS));
    programNotificationTemplate.setRecipientDataElement(dataElement);
    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.COMPLETION);

    ProgramStageInstance programStageInstance = programStageInstances.iterator().next();

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    // no message when no template is attached
    assertEquals(0, sentProgramMessages.size());

    programStageInstance
        .getProgramStage()
        .getNotificationTemplates()
        .add(programNotificationTemplate);

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    assertEquals(1, sentProgramMessages.size());
  }

  @Test
  void testDataElementRecipientWithEmail() {
    when(programStageInstanceStore.get(anyLong()))
        .thenReturn(programStageInstances.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programStageNotificationRenderer.render(
            any(ProgramStageInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.DATA_ELEMENT);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL));
    programNotificationTemplate.setRecipientDataElement(dataElementEmail);
    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.COMPLETION);

    ProgramStageInstance programStageInstance = programStageInstances.iterator().next();

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    // no message when no template is attached
    assertEquals(0, sentProgramMessages.size());

    programStageInstance
        .getProgramStage()
        .getNotificationTemplates()
        .add(programNotificationTemplate);

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    assertEquals(1, sentProgramMessages.size());
  }

  @Test
  void testDataElementRecipientWithInternalRecipients() {
    when(programStageInstanceStore.get(anyLong()))
        .thenReturn(programStageInstances.iterator().next());

    when(messageService.sendMessage(any()))
        .thenAnswer(
            invocation -> {
              sentInternalMessages.add(new MockMessage(invocation.getArguments()));
              return 40L;
            });

    when(programStageNotificationRenderer.render(
            any(ProgramStageInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.COMPLETION);
    programNotificationTemplate.setRecipientUserGroup(userGroup);

    ProgramStageInstance programStageInstance = programStageInstances.iterator().next();

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    // no message when no template is attached
    assertEquals(0, sentInternalMessages.size());

    programStageInstance
        .getProgramStage()
        .getNotificationTemplates()
        .add(programNotificationTemplate);

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    assertEquals(1, sentInternalMessages.size());

    assertTrue(sentInternalMessages.iterator().next().users.contains(userA));
    assertTrue(sentInternalMessages.iterator().next().users.contains(userB));
  }

  @Test
  void testSendToParent() {
    when(programStageInstanceStore.get(anyLong()))
        .thenReturn(programStageInstances.iterator().next());

    when(messageService.sendMessage(any()))
        .thenAnswer(
            invocation -> {
              sentInternalMessages.add(new MockMessage(invocation.getArguments()));
              return 40L;
            });

    when(programStageNotificationRenderer.render(
            any(ProgramStageInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.COMPLETION);
    programNotificationTemplate.setRecipientUserGroup(userGroupBasedOnParent);
    programNotificationTemplate.setNotifyParentOrganisationUnitOnly(true);

    ProgramStageInstance programStageInstance = programStageInstances.iterator().next();
    programStageInstance
        .getProgramStage()
        .getNotificationTemplates()
        .add(programNotificationTemplate);

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    assertEquals(1, sentInternalMessages.size());

    Set<User> users = sentInternalMessages.iterator().next().users;

    assertEquals(1, users.size());
    assertTrue(users.contains(userLvlOneLeft));
  }

  @Test
  void testSendToHierarchy() {
    when(programStageInstanceStore.get(anyLong()))
        .thenReturn(programStageInstances.iterator().next());

    when(messageService.sendMessage(any()))
        .thenAnswer(
            invocation -> {
              sentInternalMessages.add(new MockMessage(invocation.getArguments()));
              return 40L;
            });

    when(programStageNotificationRenderer.render(
            any(ProgramStageInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);

    programNotificationTemplate.setRecipientUserGroup(userGroupBasedOnHierarchy);
    programNotificationTemplate.setNotifyUsersInHierarchyOnly(true);
    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.COMPLETION);

    ProgramStageInstance programStageInstance = programStageInstances.iterator().next();
    programStageInstance
        .getProgramStage()
        .getNotificationTemplates()
        .add(programNotificationTemplate);

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    assertEquals(1, sentInternalMessages.size());

    Set<User> users = sentInternalMessages.iterator().next().users;

    assertEquals(3, users.size());
    assertTrue(users.contains(userLvlTwoLeftLeft));
    assertTrue(users.contains(userLvlOneLeft));
    assertTrue(users.contains(userRoot));

    assertFalse(users.contains(userLvlTwoLeftRight));
    assertFalse(users.contains(userLvlOneRight));
  }

  @Test
  void testSendToUsersAtOu() {
    when(programStageInstanceStore.get(anyLong()))
        .thenReturn(programStageInstances.iterator().next());

    when(messageService.sendMessage(any()))
        .thenAnswer(
            invocation -> {
              sentInternalMessages.add(new MockMessage(invocation.getArguments()));
              return 40L;
            });

    when(programStageNotificationRenderer.render(
            any(ProgramStageInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(
        ProgramNotificationRecipient.USERS_AT_ORGANISATION_UNIT);
    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.COMPLETION);

    lvlTwoLeftLeft.getUsers().add(userLvlTwoLeftRight);

    ProgramStageInstance programStageInstance = programStageInstances.iterator().next();
    programStageInstance
        .getProgramStage()
        .getNotificationTemplates()
        .add(programNotificationTemplate);

    programNotificationService.sendEventCompletionNotifications(programStageInstance.getId());

    assertEquals(1, sentInternalMessages.size());

    Set<User> users = sentInternalMessages.iterator().next().users;

    assertEquals(2, users.size());
    assertTrue(users.contains(userLvlTwoLeftLeft));
    assertTrue(users.contains(userLvlTwoLeftRight));
  }

  @Test
  void testScheduledNotifications() {
    sentProgramMessages.clear();

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(manager.getAll(ProgramNotificationInstance.class))
        .thenReturn(Collections.singletonList(programNotificationInstaceForToday));

    when(programNotificationRenderer.render(
            any(ProgramInstance.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationService.sendScheduledNotifications();

    assertEquals(1, sentProgramMessages.size());
  }

  @Test
  void testScheduledNotificationsWithDateInPast() {
    sentInternalMessages.clear();

    programNotificationService.sendScheduledNotifications();

    assertEquals(0, sentProgramMessages.size());
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private void setUpInstances() {
    programNotificationTemplate =
        createProgramNotificationTemplate(
            TEMPLATE_NAME,
            0,
            NotificationTrigger.ENROLLMENT,
            ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE);
    programNotificationTemplate.setUid(notificationTemplate);

    java.util.Calendar cal = java.util.Calendar.getInstance();

    Date today = cal.getTime();
    cal.add(java.util.Calendar.DATE, -1);

    programNotificationTemplateForToday =
        createProgramNotificationTemplate(
            TEMPLATE_NAME,
            0,
            NotificationTrigger.PROGRAM_RULE,
            ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE,
            today);

    programNotificationInstaceForToday = new ProgramNotificationInstance();
    programNotificationInstaceForToday.setProgramNotificationTemplateSnapshot(
        notificationTemplateMapper.toProgramNotificationTemplateSnapshot(
            programNotificationTemplateForToday));
    programNotificationInstaceForToday.setName(programNotificationTemplateForToday.getName());
    programNotificationInstaceForToday.setAutoFields();
    programNotificationInstaceForToday.setScheduledAt(today);

    root = createOrganisationUnit('R');
    lvlOneLeft = createOrganisationUnit('1');
    lvlOneRight = createOrganisationUnit('2');
    lvlTwoLeftLeft = createOrganisationUnit('3');
    lvlTwoLeftLeft.setPhoneNumber(OU_PHONE_NUMBER);
    lvlTwoLeftRight = createOrganisationUnit('4');

    configureHierarchy(root, lvlOneLeft, lvlOneRight, lvlTwoLeftLeft, lvlTwoLeftRight);

    // User and UserGroup

    userA = createUser('U');
    userA.setPhoneNumber(USERA_PHONE_NUMBER);
    userA.getOrganisationUnits().add(lvlTwoLeftLeft);

    userB = createUser('V');
    userB.setPhoneNumber(USERB_PHONE_NUMBER);
    userB.getOrganisationUnits().add(lvlTwoLeftLeft);

    userGroup = createUserGroup('G', Sets.newHashSet(userA, userB));

    // User based on hierarchy

    userLvlTwoLeftLeft = createUser('K');
    userLvlTwoLeftLeft.getOrganisationUnits().add(lvlTwoLeftLeft);
    lvlTwoLeftLeft.getUsers().add(userLvlTwoLeftLeft);

    userLvlTwoLeftRight = createUser('L');
    userLvlTwoLeftRight.getOrganisationUnits().add(lvlTwoLeftRight);
    lvlTwoLeftRight.getUsers().add(userLvlTwoLeftRight);

    userLvlOneLeft = createUser('M');
    userLvlOneLeft.getOrganisationUnits().add(lvlOneLeft);
    lvlOneLeft.getUsers().add(userLvlOneLeft);

    userLvlOneRight = createUser('N');
    userLvlOneRight.getOrganisationUnits().add(lvlOneRight);
    lvlOneRight.getUsers().add(userLvlOneLeft);

    userRoot = createUser('R');
    userRoot.getOrganisationUnits().add(root);
    root.getUsers().add(userRoot);

    userGroupBasedOnHierarchy =
        createUserGroup(
            'H',
            Sets.newHashSet(
                userLvlOneLeft,
                userLvlOneRight,
                userLvlTwoLeftLeft,
                userLvlTwoLeftRight,
                userRoot));
    userGroupBasedOnParent =
        createUserGroup('H', Sets.newHashSet(userLvlTwoLeftLeft, userLvlTwoLeftRight));

    // Program
    Program programA = createProgram('A');
    programA.setAutoFields();
    programA.setOrganisationUnits(Sets.newHashSet(lvlTwoLeftLeft, lvlTwoLeftRight));
    programA.setNotificationTemplates(
        Sets.newHashSet(programNotificationTemplate, programNotificationTemplateForToday));
    programA.getProgramAttributes().add(programTrackedEntityAttribute);

    trackedEntityAttribute = createTrackedEntityAttribute('T');
    trackedEntityAttributeEmail = createTrackedEntityAttribute('E');
    trackedEntityAttribute.setValueType(ValueType.PHONE_NUMBER);
    trackedEntityAttribute.setValueType(ValueType.EMAIL);
    programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, trackedEntityAttribute);
    programTrackedEntityAttributeEmail =
        createProgramTrackedEntityAttribute(programA, trackedEntityAttributeEmail);
    programTrackedEntityAttribute.setAttribute(trackedEntityAttribute);
    programTrackedEntityAttributeEmail.setAttribute(trackedEntityAttributeEmail);

    // ProgramStage
    ProgramStage programStage = createProgramStage('S', programA);

    dataElement = createDataElement('D');
    dataElementEmail = createDataElement('E');
    dataElement.setValueType(ValueType.PHONE_NUMBER);
    dataElementEmail.setValueType(ValueType.EMAIL);

    // ProgramInstance & TEI
    tei = new TrackedEntityInstance();
    tei.setAutoFields();
    tei.setOrganisationUnit(lvlTwoLeftLeft);

    attributeValue = createTrackedEntityAttributeValue('P', tei, trackedEntityAttribute);
    attributeValueEmail = createTrackedEntityAttributeValue('E', tei, trackedEntityAttribute);
    attributeValue.setValue(ATT_PHONE_NUMBER);
    attributeValueEmail.setValue(ATT_EMAIL);
    tei.getTrackedEntityAttributeValues().add(attributeValue);
    tei.getTrackedEntityAttributeValues().add(attributeValueEmail);

    ProgramInstance programInstance = new ProgramInstance();
    programInstance.setAutoFields();
    programInstance.setProgram(programA);
    programInstance.setOrganisationUnit(lvlTwoLeftLeft);
    programInstance.setEntityInstance(tei);

    // ProgramStageInstance
    ProgramStageInstance programStageInstance = new ProgramStageInstance();
    programStageInstance.setAutoFields();
    programStageInstance.setProgramInstance(programInstance);
    programStageInstance.setOrganisationUnit(lvlTwoLeftLeft);
    programStageInstance.setProgramStage(programStage);

    // lists returned by stubs
    programStageInstances.add(programStageInstance);
    programInstances.add(programInstance);

    programNotificationInstaceForToday.setProgramInstance(programInstance);

    notificationMessage = new NotificationMessage(SUBJECT, MESSAGE);
  }

  static class MockMessage {
    final String subject, text, metaData;

    final Set<User> users;

    final User sender;

    final boolean includeFeedbackRecipients, forceNotifications;

    /** Danger danger! Will break if MessageService API changes. */
    MockMessage(Object[] args) {
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
