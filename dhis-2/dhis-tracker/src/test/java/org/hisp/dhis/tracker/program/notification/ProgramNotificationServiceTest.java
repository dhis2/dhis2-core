/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.tracker.program.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.DeliveryChannel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.eventdatavalue.EventDataValue;
import org.hisp.dhis.message.MessageConversationParams;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.notification.NotificationMessage;
import org.hisp.dhis.notification.NotificationMessageRenderer;
import org.hisp.dhis.notification.NotificationTemplate;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.outboundmessage.BatchResponseStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramTrackedEntityAttribute;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.program.notification.template.NotificationTemplateMapper;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.model.TrackedEntityAttributeValue;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.tracker.program.message.ProgramMessage;
import org.hisp.dhis.tracker.program.message.ProgramMessageService;
import org.hisp.dhis.tracker.test.TrackerTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Zubair Asghar.
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ProgramNotificationServiceTest extends TrackerTestBase {

  private static final String SUBJECT = "subject";

  private static final String MESSAGE = "message";

  private static final String TEMPLATE_NAME = "message";

  private static final String OU_PHONE_NUMBER = "471000000";

  private static final String ATT_PHONE_NUMBER = "473000000";

  private static final String USERA_PHONE_NUMBER = "47400000";

  private static final String USERB_PHONE_NUMBER = "47500000";

  private static final String ATT_EMAIL = "attr@test.org";

  private final String notificationTemplate = CodeGenerator.generateUid();

  @Mock private ProgramMessageService programMessageService;

  @Mock private MessageService messageService;

  @Mock private IdentifiableObjectManager manager;

  @Mock private NotificationMessageRenderer<Enrollment> programNotificationRenderer;

  @Mock private NotificationMessageRenderer<TrackerEvent> programStageNotificationRenderer;

  @Mock private NotificationMessageRenderer<SingleEvent> singleEventNotificationRenderer;

  @Mock private ProgramNotificationTemplateService notificationTemplateService;

  @Mock private EntityManager entityManager;

  @Mock private JdbcTemplate jdbcTemplate;

  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @Mock private ProgramNotificationInstanceService programNotificationInstanceService;

  private DefaultProgramNotificationService programNotificationService;

  private final Set<Enrollment> enrollments = new HashSet<>();

  private final List<ProgramMessage> sentProgramMessages = new ArrayList<>();

  private final List<MockMessage> sentInternalMessages = new ArrayList<>();

  private User userA;

  private User userB;

  private UserGroup userGroup;

  private OrganisationUnit lvlTwoLeftLeft;

  private TrackedEntity te;

  private DataElement dataElement;

  private DataElement dataElementEmail;

  private TrackedEntityAttribute trackedEntityAttribute;

  private ProgramTrackedEntityAttribute programTrackedEntityAttribute;

  private TrackerEvent trackerEvent;

  private SingleEvent singleEvent;

  private NotificationMessage notificationMessage;

  private ProgramNotificationTemplate programNotificationTemplate;

  private ProgramNotificationInstance programNotificationInstaceForToday;

  @BeforeEach
  public void initTest() {
    programNotificationService =
        new DefaultProgramNotificationService(
            this.programMessageService,
            this.messageService,
            this.manager,
            this.programNotificationRenderer,
            this.programStageNotificationRenderer,
            this.singleEventNotificationRenderer,
            notificationTemplateService,
            entityManager,
            jdbcTemplate,
            applicationEventPublisher,
            programNotificationInstanceService);

    setUpInstances();
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  // -------------------------------------------------------------------------

  @Test
  void testSendEnrollmentNotification() {
    when(manager.get(eq(Enrollment.class), any(Long.class)))
        .thenReturn(enrollments.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(any(Enrollment.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationTrigger(NotificationTrigger.ENROLLMENT);

    programNotificationService.sendEnrollmentNotifications(enrollments.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertEquals(TrackedEntity.class, programMessage.getRecipients().getTrackedEntity().getClass());
    assertEquals(te, programMessage.getRecipients().getTrackedEntity());
  }

  @Test
  void testUserGroupRecipientWithDisabledUser() {
    when(manager.get(eq(Enrollment.class), any(Long.class)))
        .thenReturn(enrollments.iterator().next());

    when(messageService.sendMessage(any()))
        .thenAnswer(
            invocation -> {
              sentInternalMessages.add(new MockMessage(invocation.getArguments()));
              return 40L;
            });

    when(programNotificationRenderer.render(any(Enrollment.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    programNotificationTemplate.setRecipientUserGroup(userGroup);

    programNotificationService.sendEnrollmentNotifications(enrollments.iterator().next().getId());

    assertEquals(1, sentInternalMessages.size());

    MockMessage mockMessage = sentInternalMessages.iterator().next();

    assertTrue(mockMessage.users.contains(userB));
  }

  @Test
  void testOuContactRecipient() {
    when(manager.get(eq(Enrollment.class), any(Long.class)))
        .thenReturn(enrollments.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(any(Enrollment.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(
        ProgramNotificationRecipient.ORGANISATION_UNIT_CONTACT);

    programNotificationService.sendEnrollmentNotifications(enrollments.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertEquals(
        OrganisationUnit.class, programMessage.getRecipients().getOrganisationUnit().getClass());
    assertEquals(lvlTwoLeftLeft, programMessage.getRecipients().getOrganisationUnit());
    assertEquals(programMessage.getNotificationTemplate(), notificationTemplate);
  }

  @Test
  void testProgramAttributeRecipientWithSMS() {
    when(manager.get(eq(Enrollment.class), any(Long.class)))
        .thenReturn(enrollments.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(any(Enrollment.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(
        ProgramNotificationRecipient.PROGRAM_ATTRIBUTE);
    programNotificationTemplate.setRecipientProgramAttribute(trackedEntityAttribute);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.SMS));

    programNotificationService.sendEnrollmentNotifications(enrollments.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertTrue(programMessage.getRecipients().getPhoneNumbers().contains(ATT_PHONE_NUMBER));
    assertTrue(programMessage.getDeliveryChannels().contains(DeliveryChannel.SMS));
    assertEquals(programMessage.getNotificationTemplate(), notificationTemplate);
  }

  @Test
  void testProgramAttributeRecipientWithEMAIL() {
    when(manager.get(eq(Enrollment.class), any(Long.class)))
        .thenReturn(enrollments.iterator().next());

    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programNotificationRenderer.render(any(Enrollment.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(
        ProgramNotificationRecipient.PROGRAM_ATTRIBUTE);
    programNotificationTemplate.setRecipientProgramAttribute(trackedEntityAttribute);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL));

    programNotificationService.sendEnrollmentNotifications(enrollments.iterator().next().getId());

    assertEquals(1, sentProgramMessages.size());

    ProgramMessage programMessage = sentProgramMessages.iterator().next();

    assertTrue(programMessage.getRecipients().getEmailAddresses().contains(ATT_EMAIL));
    assertTrue(programMessage.getDeliveryChannels().contains(DeliveryChannel.EMAIL));
    assertEquals(programMessage.getNotificationTemplate(), notificationTemplate);
  }

  @Test
  void testDataElementRecipientWithSMSForTrackerEvent() {
    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programStageNotificationRenderer.render(
            any(TrackerEvent.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.DATA_ELEMENT);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.SMS));
    programNotificationTemplate.setRecipientDataElement(dataElement);

    programNotificationService.sendNotification(programNotificationTemplate, trackerEvent);

    assertEquals(1, sentProgramMessages.size());
  }

  @Test
  void testDataElementRecipientWithEmailForTrackerEvent() {
    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(programStageNotificationRenderer.render(
            any(TrackerEvent.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.DATA_ELEMENT);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL));
    programNotificationTemplate.setRecipientDataElement(dataElementEmail);

    programNotificationService.sendNotification(programNotificationTemplate, trackerEvent);

    assertEquals(1, sentProgramMessages.size());
  }

  @Test
  void testDataElementRecipientWithSMSForSingleEvent() {
    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(singleEventNotificationRenderer.render(
            any(SingleEvent.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.DATA_ELEMENT);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.SMS));
    programNotificationTemplate.setRecipientDataElement(dataElement);

    programNotificationService.sendNotification(programNotificationTemplate, singleEvent);

    assertEquals(1, sentProgramMessages.size());
  }

  @Test
  void testDataElementRecipientWithEmailForSingleEvent() {
    when(programMessageService.sendMessages(anyList()))
        .thenAnswer(
            invocation -> {
              sentProgramMessages.addAll((List<ProgramMessage>) invocation.getArguments()[0]);
              return new BatchResponseStatus(Collections.emptyList());
            });

    when(singleEventNotificationRenderer.render(
            any(SingleEvent.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.DATA_ELEMENT);
    programNotificationTemplate.setDeliveryChannels(Sets.newHashSet(DeliveryChannel.EMAIL));
    programNotificationTemplate.setRecipientDataElement(dataElementEmail);

    programNotificationService.sendNotification(programNotificationTemplate, singleEvent);

    assertEquals(1, sentProgramMessages.size());
  }

  @Test
  void testUserGroupRecipientForTrackerEvent() {
    when(messageService.sendMessage(any()))
        .thenAnswer(
            invocation -> {
              sentInternalMessages.add(new MockMessage(invocation.getArguments()));
              return 40L;
            });

    when(programStageNotificationRenderer.render(
            any(TrackerEvent.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    programNotificationTemplate.setRecipientUserGroup(userGroup);

    programNotificationService.sendNotification(programNotificationTemplate, trackerEvent);

    assertEquals(1, sentInternalMessages.size());
    assertTrue(sentInternalMessages.iterator().next().users.contains(userB));
  }

  @Test
  void testUserGroupRecipientForSingleEvent() {
    when(messageService.sendMessage(any()))
        .thenAnswer(
            invocation -> {
              sentInternalMessages.add(new MockMessage(invocation.getArguments()));
              return 40L;
            });

    when(singleEventNotificationRenderer.render(
            any(SingleEvent.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationTemplate.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    programNotificationTemplate.setRecipientUserGroup(userGroup);

    programNotificationService.sendNotification(programNotificationTemplate, singleEvent);

    assertEquals(1, sentInternalMessages.size());
    assertTrue(sentInternalMessages.iterator().next().users.contains(userB));
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

    when(programNotificationInstanceService.getProgramNotificationInstances(any()))
        .thenReturn(Collections.singletonList(programNotificationInstaceForToday));

    when(programNotificationRenderer.render(any(Enrollment.class), any(NotificationTemplate.class)))
        .thenReturn(notificationMessage);

    programNotificationService.sendScheduledNotifications(JobProgress.noop());

    assertEquals(1, sentProgramMessages.size());
  }

  @Test
  void testScheduledNotificationsWithDateInPast() {
    sentInternalMessages.clear();

    programNotificationService.sendScheduledNotifications(JobProgress.noop());

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

    ProgramNotificationTemplate programNotificationTemplateForToday =
        createProgramNotificationTemplate(
            TEMPLATE_NAME,
            0,
            NotificationTrigger.PROGRAM_RULE,
            ProgramNotificationRecipient.TRACKED_ENTITY_INSTANCE,
            today);

    programNotificationInstaceForToday = new ProgramNotificationInstance();
    programNotificationInstaceForToday.setProgramNotificationTemplateSnapshot(
        NotificationTemplateMapper.toProgramNotificationTemplateSnapshot(
            programNotificationTemplateForToday));
    programNotificationInstaceForToday.setName(programNotificationTemplateForToday.getName());
    programNotificationInstaceForToday.setAutoFields();
    programNotificationInstaceForToday.setScheduledAt(today);

    lvlTwoLeftLeft = createOrganisationUnit('3');
    lvlTwoLeftLeft.setPhoneNumber(OU_PHONE_NUMBER);

    userA = makeUser("U");
    userA.setPhoneNumber(USERA_PHONE_NUMBER);
    userA.getOrganisationUnits().add(lvlTwoLeftLeft);
    userA.setDisabled(true);

    userB = makeUser("V");
    userB.setPhoneNumber(USERB_PHONE_NUMBER);
    userB.getOrganisationUnits().add(lvlTwoLeftLeft);

    userGroup = createUserGroup('G', Sets.newHashSet(userA, userB));

    Program programA = createProgram('A');
    programA.setAutoFields();
    programA.setOrganisationUnits(Sets.newHashSet(lvlTwoLeftLeft));
    programA.setNotificationTemplates(
        Sets.newHashSet(programNotificationTemplate, programNotificationTemplateForToday));
    programA.getProgramAttributes().add(programTrackedEntityAttribute);

    trackedEntityAttribute = createTrackedEntityAttribute('T');
    trackedEntityAttribute.setValueType(ValueType.EMAIL);
    programTrackedEntityAttribute =
        createProgramTrackedEntityAttribute(programA, trackedEntityAttribute);
    programTrackedEntityAttribute.setAttribute(trackedEntityAttribute);

    te = new TrackedEntity();
    te.setAutoFields();
    te.setOrganisationUnit(lvlTwoLeftLeft);

    TrackedEntityAttributeValue attributeValue =
        createTrackedEntityAttributeValue('P', te, trackedEntityAttribute);
    TrackedEntityAttributeValue attributeValueEmail =
        createTrackedEntityAttributeValue('E', te, trackedEntityAttribute);
    attributeValue.setValue(ATT_PHONE_NUMBER);
    attributeValueEmail.setValue(ATT_EMAIL);
    te.getTrackedEntityAttributeValues().add(attributeValue);
    te.getTrackedEntityAttributeValues().add(attributeValueEmail);

    // ProgramStage
    ProgramStage programStage = createProgramStage('S', programA);

    dataElement = createDataElement('D');
    dataElementEmail = createDataElement('E');
    dataElement.setValueType(ValueType.PHONE_NUMBER);
    dataElementEmail.setValueType(ValueType.EMAIL);

    Enrollment enrollment = new Enrollment();
    enrollment.setAutoFields();
    enrollment.setProgram(programA);
    enrollment.setOrganisationUnit(lvlTwoLeftLeft);
    enrollment.setTrackedEntity(te);

    trackerEvent = new TrackerEvent();
    trackerEvent.setAutoFields();
    trackerEvent.setEnrollment(enrollment);
    trackerEvent.setOrganisationUnit(lvlTwoLeftLeft);
    trackerEvent.setProgramStage(programStage);
    trackerEvent.setEventDataValues(
        Set.of(
            new EventDataValue(dataElement.getUid(), ATT_PHONE_NUMBER),
            new EventDataValue(dataElementEmail.getUid(), ATT_EMAIL)));

    singleEvent = new SingleEvent();
    singleEvent.setAutoFields();
    singleEvent.setOrganisationUnit(lvlTwoLeftLeft);
    singleEvent.setProgramStage(programStage);
    singleEvent.setEventDataValues(
        Set.of(
            new EventDataValue(dataElement.getUid(), ATT_PHONE_NUMBER),
            new EventDataValue(dataElementEmail.getUid(), ATT_EMAIL)));

    enrollments.add(enrollment);

    programNotificationInstaceForToday.setEnrollment(enrollment);

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
