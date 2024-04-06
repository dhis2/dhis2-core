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
package org.hisp.dhis.tracker.imports.bundle;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.message.MessageConversation;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.notification.NotificationTrigger;
import org.hisp.dhis.program.notification.ProgramNotificationRecipient;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
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
  @Autowired private MessageService messageService;

  private Program programA;

  private ProgramStage programStageA;

  private OrganisationUnit orgUnitA;

  private TrackedEntityType trackedEntityTypeA;

  private TrackedEntity trackedEntityA;

  private Enrollment enrollmentA;

  private Event eventA;

  private ProgramNotificationTemplate templateForEnrollment;
  private ProgramNotificationTemplate templateForEnrollmentCompletion;
  private ProgramNotificationTemplate templateForEventCompletion;

  private User user;
  private UserGroup userGroup;

  @Override
  protected void setUpTest() throws IOException {
    userService = _userService;

    orgUnitA = createOrganisationUnit('A');
    manager.save(orgUnitA, false);

    trackedEntityTypeA = createTrackedEntityType('A');
    manager.save(trackedEntityTypeA, false);

    programA = createProgram('P', new HashSet<>(), orgUnitA);
    programA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(programA, false);

    programStageA = createProgramStage('S', programA);
    manager.save(programStageA, false);

    programA.getProgramStages().add(programStageA);
    manager.update(programA);

    trackedEntityA = createTrackedEntity('T', orgUnitA);
    trackedEntityA.setTrackedEntityType(trackedEntityTypeA);
    manager.save(trackedEntityA, false);

    user = createAndAddUser(false, "user", Set.of(orgUnitA), Set.of(orgUnitA), "ALL");

    userGroup = createUserGroup('U', Set.of(user));
    manager.save(userGroup, false);

    user.getGroups().add(userGroup);
    manager.update(user);

    templateForEnrollment =
        createProgramNotification(
            NotificationTrigger.ENROLLMENT, "enrollment", CodeGenerator.generateUid());
    templateForEnrollmentCompletion =
        createProgramNotification(
            NotificationTrigger.COMPLETION, "enrollment_completion", CodeGenerator.generateUid());
    templateForEventCompletion =
        createProgramNotification(
            NotificationTrigger.COMPLETION, "event_completion", CodeGenerator.generateUid());

    manager.save(templateForEnrollment);
    manager.save(templateForEnrollmentCompletion);
    manager.save(templateForEventCompletion);

    programA.getNotificationTemplates().add(templateForEnrollment);
    programA.getNotificationTemplates().add(templateForEnrollmentCompletion);
    programStageA.getNotificationTemplates().add(templateForEventCompletion);

    manager.update(programA);
    manager.update(programStageA);
  }

  @Test
  void shouldSendTrackerNotificationAtEnrollment() {

    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(trackedEntityA.getUid())
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(CodeGenerator.generateUid())
            .build();

    trackerImportService.importTracker(
        TrackerImportParams.builder().userId(user.getUid()).build(),
        TrackerObjects.builder().enrollments(List.of(enrollment)).build());

    List<MessageConversation> messageConversations = messageService.getMessageConversations();
    System.out.println(messageConversations);
  }

  @Test
  void shouldSendTrackerNotificationAtEnrollmentCompletion() {

    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(trackedEntityA.getUid())
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(CodeGenerator.generateUid())
            .build();

    trackerImportService.importTracker(
        TrackerImportParams.builder().userId(user.getUid()).build(),
        TrackerObjects.builder().enrollments(List.of(enrollment)).build());

    List<MessageConversation> messageConversations = messageService.getMessageConversations();
    System.out.println(messageConversations);
  }

  @Test
  void shouldSendTrackerNotificationAtEventCompletion() {

    org.hisp.dhis.tracker.imports.domain.Enrollment enrollment =
        org.hisp.dhis.tracker.imports.domain.Enrollment.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .trackedEntity(trackedEntityA.getUid())
            .status(EnrollmentStatus.COMPLETED)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(CodeGenerator.generateUid())
            .build();

    org.hisp.dhis.tracker.imports.domain.Event event =
        org.hisp.dhis.tracker.imports.domain.Event.builder()
            .program(MetadataIdentifier.ofUid(programA.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnitA.getUid()))
            .enrollment(enrollment.getEnrollment())
            .event(CodeGenerator.generateUid())
            .status(EventStatus.COMPLETED)
            .completedAt(Instant.now())
            .occurredAt(Instant.now())
            .build();

    trackerImportService.importTracker(
        TrackerImportParams.builder().userId(user.getUid()).build(),
        TrackerObjects.builder().enrollments(List.of(enrollment)).events(List.of(event)).build());
    List<MessageConversation> messageConversations = messageService.getMessageConversations();

    System.out.println(messageConversations);
  }

  private ProgramNotificationTemplate createProgramNotification(
      NotificationTrigger trigger, String name, String uid) {
    ProgramNotificationTemplate template = new ProgramNotificationTemplate();
    template.setAutoFields();
    template.setUid(uid);
    template.setName(name);
    template.setNotificationTrigger(trigger);
    template.setMessageTemplate("message_text");
    template.setSubjectTemplate("subject_text");
    template.setNotificationRecipient(ProgramNotificationRecipient.USER_GROUP);
    template.setRecipientUserGroup(userGroup);

    return template;
  }
}
