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
package org.hisp.dhis.tracker.imports.bundle;

import static org.awaitility.Awaitility.await;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class TrackerNotificationHandlerServiceTest extends PostgresIntegrationTestBase {

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  private Program programA;

  private ProgramStage programStageA;

  private OrganisationUnit orgUnitA;

  private TrackedEntity trackedEntityA;

  private UserGroup userGroup;

  @BeforeEach
  void setUp() {
    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);

    TrackedEntityType trackedEntityTypeA = createTrackedEntityType('A');
    manager.save(trackedEntityTypeA, false);

    programA = createProgram('P', new HashSet<>(), orgUnitA);
    programA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(programA, false);

    programStageA = createProgramStage('S', programA);
    manager.save(programStageA, false);

    programA.getProgramStages().add(programStageA);
    manager.update(programA);

    trackedEntityA = createTrackedEntity('T', orgUnitA, trackedEntityTypeA);
    manager.save(trackedEntityA, false);

    User user = createAndAddUser(false, "user", Set.of(orgUnitA), Set.of(orgUnitA), "ALL");

    userGroup = createUserGroup('U', Set.of(user));
    manager.save(userGroup, false);

    user.getGroups().add(userGroup);
    manager.update(user);

    injectSecurityContextUser(user);

    ProgramNotificationTemplate templateForEnrollment =
        createProgramNotification(
            "enrollment",
            CodeGenerator.generateUid(),
            "enrollment_subject",
            NotificationTrigger.ENROLLMENT);
    ProgramNotificationTemplate templateForEnrollmentCompletion =
        createProgramNotification(
            "enrollment_completion",
            CodeGenerator.generateUid(),
            "enrollment_completion_subject",
            NotificationTrigger.COMPLETION);
    ProgramNotificationTemplate templateForEventCompletion =
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
  void shouldSendTrackerNotificationAtEnrollment() {
    Enrollment enrollment =
        Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(UID.of(trackedEntityA))
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(UID.generate())
            .build();

    ImportReport importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .build(),
            TrackerObjects.builder().enrollments(List.of(enrollment)).build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    List<MessageConversation> messageConversations = manager.getAll(MessageConversation.class);

    List<String> subjectMessages =
        messageConversations.stream().map(MessageConversation::getSubject).toList();

    assertContainsOnly(List.of("enrollment_subject"), subjectMessages);
  }

  @Test
  void shouldSendTrackerNotificationAtEnrollmentCompletionAndThenEventCompletion() {
    UID eventUid = UID.generate();

    Enrollment enrollment =
        Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(UID.of(trackedEntityA))
            .status(EnrollmentStatus.COMPLETED)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(UID.generate())
            .build();

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .enrollment(enrollment.getUid())
            .event(eventUid)
            .programStage(MetadataIdentifier.ofUid(programStageA.getUid()))
            .status(EventStatus.ACTIVE)
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .occurredAt(Instant.now())
            .build();

    ImportReport importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .build(),
            TrackerObjects.builder()
                .enrollments(List.of(enrollment))
                .events(List.of(event))
                .build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 1);

    List<MessageConversation> messageConversations = manager.getAll(MessageConversation.class);

    List<String> subjectMessages =
        messageConversations.stream().map(MessageConversation::getSubject).toList();

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject"), subjectMessages);

    org.hisp.dhis.tracker.imports.domain.TrackerEvent eventUpdated =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .enrollment(enrollment.getUid())
            .event(eventUid)
            .programStage(MetadataIdentifier.ofUid(programStageA.getUid()))
            .status(EventStatus.COMPLETED)
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .completedAt(Instant.now())
            .occurredAt(Instant.now())
            .build();

    importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.UPDATE).build(),
            TrackerObjects.builder().events(List.of(eventUpdated)).build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 2);

    messageConversations = manager.getAll(MessageConversation.class);

    subjectMessages = messageConversations.stream().map(MessageConversation::getSubject).toList();

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject", "event_completion_subject"),
        subjectMessages);
  }

  @Test
  void shouldSendEnrollmentCompletionNotificationWhenStatusIsUpdatedFromActiveToCompleted() {
    UID uid = UID.generate();
    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(UID.of(trackedEntityA))
            .status(EnrollmentStatus.ACTIVE)
            .enrollment(uid)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .build();

    ImportReport importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .build(),
            TrackerObjects.builder().enrollments(List.of(enrollment)).build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    List<MessageConversation> messageConversations = manager.getAll(MessageConversation.class);

    List<String> subjectMessages =
        messageConversations.stream().map(MessageConversation::getSubject).toList();

    assertContainsOnly(List.of("enrollment_subject"), subjectMessages);

    org.hisp.dhis.tracker.imports.domain.Enrollment enrollmentUpdated =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(UID.of(trackedEntityA))
            .status(EnrollmentStatus.COMPLETED)
            .enrollment(uid)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .build();

    importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.UPDATE).build(),
            TrackerObjects.builder().enrollments(List.of(enrollmentUpdated)).build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 1);

    messageConversations = manager.getAll(MessageConversation.class);

    subjectMessages = messageConversations.stream().map(MessageConversation::getSubject).toList();

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject"), subjectMessages);
  }

  @Test
  void shouldSendEnrollmentCompletionNotificationOnlyOnce() {
    UID uid = UID.generate();
    Enrollment enrollment =
        Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(UID.of(trackedEntityA))
            .status(EnrollmentStatus.COMPLETED)
            .enrollment(uid)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .build();

    ImportReport importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .build(),
            TrackerObjects.builder().enrollments(List.of(enrollment)).build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 1);

    List<MessageConversation> messageConversations = manager.getAll(MessageConversation.class);

    List<String> subjectMessages =
        messageConversations.stream().map(MessageConversation::getSubject).toList();

    assertContainsOnly(
        List.of("enrollment_subject", "enrollment_completion_subject"), subjectMessages);

    importReport =
        trackerImportService.importTracker(
            TrackerImportParams.builder()
                .importStrategy(TrackerImportStrategy.CREATE_AND_UPDATE)
                .build(),
            TrackerObjects.builder().enrollments(List.of(enrollment)).build());

    assertNoErrors(importReport);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() > 1);

    messageConversations = manager.getAll(MessageConversation.class);

    subjectMessages = messageConversations.stream().map(MessageConversation::getSubject).toList();

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
