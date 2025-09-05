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
package org.hisp.dhis.tracker.bundle;

import static org.awaitility.Awaitility.await;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleVariable;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class TrackerSideEffectHandlerServiceTest extends IntegrationTestBase {

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired protected UserService _userService;

  private Program programA;

  private ProgramStage programStageA;

  private OrganisationUnit orgUnitA;

  private ProgramRule programRuleA;

  private ProgramRuleVariable programRuleVariableA;

  private DataElement dataElementA;

  private TrackedEntityType trackedEntityTypeA;

  private TrackedEntityInstance trackedEntityA;

  private ProgramNotificationTemplate templateForEnrollmentCompletion;
  private ProgramNotificationTemplate templateForEnrollment;
  private ProgramNotificationTemplate templateForEventCompletion;

  private User user;
  private UserGroup userGroup;

  @Override
  protected void setUpTest() throws IOException {
    userService = _userService;

    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);

    dataElementA = createDataElement('A');
    dataElementA.setValueType(ValueType.NUMBER);
    manager.save(dataElementA, false);

    trackedEntityTypeA = createTrackedEntityType('A');
    manager.save(trackedEntityTypeA, false);

    programA = createProgram('P', new HashSet<>(), orgUnitA);
    programA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(programA, false);

    programStageA = createProgramStage('S', programA);
    manager.save(programStageA, false);

    ProgramStageDataElement programStageDataElementA =
        createProgramStageDataElement(programStageA, dataElementA, 1);
    manager.save(programStageDataElementA, false);
    programStageA.getProgramStageDataElements().add(programStageDataElementA);
    manager.update(programStageA);

    programA.getProgramStages().add(programStageA);
    manager.update(programA);

    programRuleVariableA = createProgramRuleVariable('A', programA);
    programRuleVariableA.setDataElement(dataElementA);
    programRuleVariableA.setValueType(ValueType.NUMBER);
    manager.save(programRuleVariableA, false);

    programRuleA = createProgramRule('R', programA);
    programRuleA.setCondition(
        "d2:hasValue(#{ProgramRuleVariableA}) && #{ProgramRuleVariableA} > 10");
    manager.save(programRuleA, false);

    trackedEntityA = createTrackedEntityInstance('T', orgUnitA);
    trackedEntityA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityA, false);

    user = createAndAddUser(false, "user", Set.of(orgUnitA), Set.of(orgUnitA), "ALL");

    userGroup = createUserGroup('U', Set.of(user));
    manager.save(userGroup, false);

    user.getGroups().add(userGroup);
    manager.update(user);

    injectSecurityContext(user);

    templateForEnrollment =
        createProgramNotification(
            "enrollment",
            CodeGenerator.generateUid(),
            "enrollment_subject",
            NotificationTrigger.ENROLLMENT);
    templateForEnrollmentCompletion =
        createProgramNotification(
            "enrollment_completion",
            CodeGenerator.generateUid(),
            "enrollment_completion_subject",
            NotificationTrigger.COMPLETION);
    templateForEventCompletion =
        createProgramNotification(
            "event_completion",
            CodeGenerator.generateUid(),
            "event_completion_subject",
            NotificationTrigger.COMPLETION);

    manager.save(templateForEnrollmentCompletion);
    manager.save(templateForEnrollment);
    manager.save(templateForEventCompletion);

    programA.getNotificationTemplates().add(templateForEnrollmentCompletion);
    programA.getNotificationTemplates().add(templateForEnrollment);
    programStageA.getNotificationTemplates().add(templateForEventCompletion);

    manager.update(programA);
    manager.update(programStageA);
  }

  @Test
  void shouldSendTrackerNotificationAtEnrollmentCompletionAndThenEventCompletion() {
    org.hisp.dhis.tracker.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.domain.Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(trackedEntityA.getUid())
            .status(EnrollmentStatus.COMPLETED)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(CodeGenerator.generateUid())
            .build();

    ImportReport importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .userId(user.getUid())
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .enrollments(List.of(enrollment))
                .build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 1);

    List<MessageConversation> messageConversations = manager.getAll(MessageConversation.class);

    List<String> subjectMessages = new ArrayList<>();
    for (MessageConversation messageConversation : messageConversations) {
      String subject = messageConversation.getSubject();
      subjectMessages.add(subject);
    }

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject"), subjectMessages);

    org.hisp.dhis.tracker.domain.Event event =
        org.hisp.dhis.tracker.domain.Event.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .enrollment(enrollment.getEnrollment())
            .event(CodeGenerator.generateUid())
            .programStage(MetadataIdentifier.ofUid(programStageA.getUid()))
            .status(EventStatus.COMPLETED)
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .completedAt(Instant.now())
            .occurredAt(Instant.now())
            .build();

    importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .userId(user.getUid())
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .events(List.of(event))
                .build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 2);

    messageConversations = manager.getAll(MessageConversation.class);

    List<String> list = new ArrayList<>();
    for (MessageConversation messageConversation : messageConversations) {
      String subject = messageConversation.getSubject();
      list.add(subject);
    }
    subjectMessages = list;

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject", "event_completion_subject"),
        subjectMessages);
  }

  @Test
  void shouldSendTrackerNotificationAtEnrollmentCompletionAndThenEventStatusChangedToCompletion() {
    String eventUid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.domain.Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(trackedEntityA.getUid())
            .status(EnrollmentStatus.COMPLETED)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(CodeGenerator.generateUid())
            .build();

    org.hisp.dhis.tracker.domain.Event event =
        org.hisp.dhis.tracker.domain.Event.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .enrollment(enrollment.getEnrollment())
            .event(eventUid)
            .programStage(MetadataIdentifier.ofUid(programStageA.getUid()))
            .status(EventStatus.ACTIVE)
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .completedAt(Instant.now())
            .occurredAt(Instant.now())
            .build();

    ImportReport importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .userId(user.getUid())
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .enrollments(List.of(enrollment))
                .events(List.of(event))
                .build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 1);

    List<MessageConversation> messageConversations = manager.getAll(MessageConversation.class);

    List<String> subjectMessages = new ArrayList<>();
    for (MessageConversation messageConversation : messageConversations) {
      String subject = messageConversation.getSubject();
      subjectMessages.add(subject);
    }

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject"), subjectMessages);

    org.hisp.dhis.tracker.domain.Event updatedEvent =
        org.hisp.dhis.tracker.domain.Event.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .enrollment(enrollment.getEnrollment())
            .event(eventUid)
            .programStage(MetadataIdentifier.ofUid(programStageA.getUid()))
            .status(EventStatus.COMPLETED)
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .completedAt(Instant.now())
            .occurredAt(Instant.now())
            .build();

    importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .userId(user.getUid())
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .events(List.of(updatedEvent))
                .build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 2);

    messageConversations = manager.getAll(MessageConversation.class);

    List<String> list = new ArrayList<>();
    for (MessageConversation messageConversation : messageConversations) {
      String subject = messageConversation.getSubject();
      list.add(subject);
    }
    subjectMessages = list;

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject", "event_completion_subject"),
        subjectMessages);
  }

  @Test
  void shouldSendEnrollmentCompletionNotificationOnlyOnce() {
    String uid = CodeGenerator.generateUid();
    org.hisp.dhis.tracker.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.domain.Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(trackedEntityA.getUid())
            .status(EnrollmentStatus.COMPLETED)
            .enrollment(uid)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(CodeGenerator.generateUid())
            .build();

    ImportReport importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .userId(user.getUid())
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .enrollments(List.of(enrollment))
                .build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 1);

    List<MessageConversation> messageConversations = manager.getAll(MessageConversation.class);

    List<String> subjectMessages = new ArrayList<>();
    for (MessageConversation messageConversation : messageConversations) {
      String subject = messageConversation.getSubject();
      subjectMessages.add(subject);
    }

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject"), subjectMessages);

    importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .userId(user.getUid())
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .enrollments(List.of(enrollment))
                .build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 1);

    messageConversations = manager.getAll(MessageConversation.class);

    List<String> list = new ArrayList<>();
    for (MessageConversation messageConversation : messageConversations) {
      String subject = messageConversation.getSubject();
      list.add(subject);
    }
    subjectMessages = list;

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject"), subjectMessages);
  }

  private ProgramNotificationTemplate createProgramNotification(
      String name, String uid, String subject, NotificationTrigger trigger) {
    ProgramNotificationTemplate template = new ProgramNotificationTemplate();
    template.setAutoFields();
    template.setUid(uid);
    template.setName(name);
    template.setNotificationTrigger(trigger);
    template.setMessageTemplate("message_text");
    template.setSubjectTemplate(subject);
    template.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    template.setRecipientUserGroup(userGroup);

    return template;
  }
}
