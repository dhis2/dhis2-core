/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.tracker.imports.notification;

import static org.awaitility.Awaitility.await;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.test.TrackerTestBase.createTrackedEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
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
import org.hisp.dhis.programrule.ProgramRule;
import org.hisp.dhis.programrule.ProgramRuleAction;
import org.hisp.dhis.programrule.ProgramRuleActionService;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.programrule.ProgramRuleService;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.model.TrackedEntity;
import org.hisp.dhis.tracker.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the unified tracker notification dispatch. Tests lifecycle notifications
 * (enrollment creation, enrollment completion, event completion) and rule engine notifications
 * (SENDMESSAGE, SCHEDULEMESSAGE) flowing through the same path.
 *
 * <p>Not @Transactional because notifications dispatch asynchronously in a separate thread.
 */
class TrackerNotificationTest extends PostgresIntegrationTestBase {

  @Autowired private TestSetup testSetup;
  @Autowired private TrackerImportService trackerImportService;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private ProgramRuleService programRuleService;
  @Autowired private ProgramRuleActionService programRuleActionService;

  private Program program;
  private ProgramStage programStage;
  private UserGroup userGroup;

  private UID trackedEntityUid;

  @BeforeEach
  void setUp() throws Exception {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    program = manager.get(Program.class, "BFcipDERJnf");
    programStage = manager.get(ProgramStage.class, "NpsdDv6kKSO");
    userGroup = manager.get(UserGroup.class, "xfHoY6IZSWI");

    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "h4w96yEMlzO");
    TrackedEntityType teType = manager.get(TrackedEntityType.class, "ja8NY4PW7Xm");
    TrackedEntity te = createTrackedEntity(orgUnit, teType);
    manager.save(te, false);
    trackedEntityUid = UID.of(te);
  }

  @Test
  void shouldSendEnrollmentNotification() {
    addLifecycleTemplate("enrollment_subject", NotificationTrigger.ENROLLMENT, program);

    importEnrollment(EnrollmentStatus.ACTIVE);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    assertContainsOnly(List.of("enrollment_subject"), messageSubjects());
  }

  @Test
  void shouldSendEnrollmentNotificationToUsersAtOrgUnit() {
    ProgramNotificationTemplate template =
        addNotificationTemplate(
            "org_unit_users_subject",
            NotificationTrigger.ENROLLMENT,
            ProgramNotificationRecipient.USERS_AT_ORGANISATION_UNIT,
            null);
    program.getNotificationTemplates().add(template);
    manager.update(program);

    importEnrollment(EnrollmentStatus.ACTIVE);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    assertContainsOnly(List.of("org_unit_users_subject"), messageSubjects());
  }

  @Test
  void shouldSendEnrollmentCompletionNotification() {
    addLifecycleTemplate("enrollment_completion_subject", NotificationTrigger.COMPLETION, program);

    importEnrollment(EnrollmentStatus.COMPLETED);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    assertContainsOnly(List.of("enrollment_completion_subject"), messageSubjects());
  }

  @Test
  void shouldSendEventCompletionNotification() {
    addLifecycleTemplate("event_completion_subject", NotificationTrigger.COMPLETION, programStage);

    importEnrollmentWithCompletedEvent();

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    assertContainsOnly(List.of("event_completion_subject"), messageSubjects());
  }

  @Test
  void shouldSendBothLifecycleAndRuleEngineNotifications() {
    addLifecycleTemplate("enrollment_subject", NotificationTrigger.ENROLLMENT, program);
    ProgramNotificationTemplate ruleTemplate =
        addNotificationTemplate("rule_subject", NotificationTrigger.PROGRAM_RULE);
    addSendMessageRule(ruleTemplate);

    importEnrollment(EnrollmentStatus.ACTIVE);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> manager.getAll(MessageConversation.class).size() >= 2);

    assertContainsOnly(List.of("enrollment_subject", "rule_subject"), messageSubjects());
  }

  @Test
  void shouldScheduleRuleEngineNotification() {
    ProgramNotificationTemplate template =
        addNotificationTemplate("scheduled_subject", NotificationTrigger.PROGRAM_RULE);
    addScheduleMessageRule(template, "'2025-01-01'");

    importEnrollment(EnrollmentStatus.ACTIVE);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(ProgramNotificationInstance.class).isEmpty());

    List<ProgramNotificationInstance> instances = manager.getAll(ProgramNotificationInstance.class);
    assertEquals(1, instances.size());
    assertEquals(
        template.getUid(), instances.get(0).getProgramNotificationTemplateSnapshot().getUid());
  }

  @Test
  void shouldDeduplicateLifecycleAndRuleEngineNotifications() {
    ProgramNotificationTemplate template =
        addNotificationTemplate("completion_subject", NotificationTrigger.PROGRAM_RULE);
    // same template used by both lifecycle COMPLETION and rule engine SENDMESSAGE
    programStage.getNotificationTemplates().add(template);
    template.setNotificationTrigger(NotificationTrigger.COMPLETION);
    manager.update(template);
    manager.update(programStage);
    addSendMessageRule(template);

    importEnrollmentWithCompletedEvent();

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    assertContainsOnly(List.of("completion_subject"), messageSubjects());
  }

  @Test
  void shouldNotSendRuleEngineNotificationTwiceWhenNotRepeatable() {
    ProgramNotificationTemplate template =
        addNotificationTemplate("rule_subject", NotificationTrigger.PROGRAM_RULE);
    addSendMessageRule(template);

    // create enrollment -- rule engine fires SENDMESSAGE
    UID enrollmentUid = UID.generate();
    importEnrollment(enrollmentUid, EnrollmentStatus.ACTIVE);

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    assertEquals(1, messageSubjects().stream().filter("rule_subject"::equals).count());

    // update same enrollment -- rule fires again but template is not repeatable
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.UPDATE).build(),
            TrackerObjects.builder()
                .enrollments(
                    List.of(
                        Enrollment.builder()
                            .enrollment(enrollmentUid)
                            .program(MetadataIdentifier.ofUid(program.getUid()))
                            .orgUnit(MetadataIdentifier.ofUid("h4w96yEMlzO"))
                            .trackedEntity(trackedEntityUid)
                            .status(EnrollmentStatus.ACTIVE)
                            .enrolledAt(Instant.now())
                            .occurredAt(Instant.now())
                            .attributeOptionCombo(MetadataIdentifier.ofUid("HllvX50cXC0"))
                            .build()))
                .build()));

    // wait long enough for any async notification that might have been dispatched
    await().during(3, TimeUnit.SECONDS).atMost(4, TimeUnit.SECONDS).until(() -> true);

    // still only one message -- repeatable flag prevented the second send
    assertEquals(1, messageSubjects().stream().filter("rule_subject"::equals).count());
  }

  @Test
  void shouldSendSingleEventCompletionNotification() {
    Program programWithoutRegistration = manager.get(Program.class, "BFcipDERJne");
    ProgramStage singleEventStage = manager.get(ProgramStage.class, "NpsdDv6kKSe");
    addLifecycleTemplate("single_event_subject", NotificationTrigger.COMPLETION, singleEventStage);

    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(UID.generate())
            .program(MetadataIdentifier.ofUid(programWithoutRegistration.getUid()))
            .programStage(MetadataIdentifier.ofUid(singleEventStage.getUid()))
            .orgUnit(MetadataIdentifier.ofUid("h4w96yEMlzO"))
            .status(EventStatus.COMPLETED)
            .occurredAt(Instant.now())
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .build();

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build(),
            TrackerObjects.builder().events(List.of(event)).build()));

    await()
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> !manager.getAll(MessageConversation.class).isEmpty());

    assertContainsOnly(List.of("single_event_subject"), messageSubjects());
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void importEnrollment(EnrollmentStatus status) {
    importEnrollment(UID.generate(), status);
  }

  private void importEnrollment(UID enrollmentUid, EnrollmentStatus status) {
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(enrollmentUid)
            .program(MetadataIdentifier.ofUid(program.getUid()))
            .orgUnit(MetadataIdentifier.ofUid("h4w96yEMlzO"))
            .trackedEntity(trackedEntityUid)
            .status(status)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .attributeOptionCombo(MetadataIdentifier.ofUid("HllvX50cXC0"))
            .build();
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build(),
            TrackerObjects.builder().enrollments(List.of(enrollment)).build()));
  }

  private void importEnrollmentWithCompletedEvent() {
    UID enrollmentUid = UID.generate();
    Enrollment enrollment =
        Enrollment.builder()
            .enrollment(enrollmentUid)
            .program(MetadataIdentifier.ofUid(program.getUid()))
            .orgUnit(MetadataIdentifier.ofUid("h4w96yEMlzO"))
            .trackedEntity(trackedEntityUid)
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .attributeOptionCombo(MetadataIdentifier.ofUid("HllvX50cXC0"))
            .build();
    org.hisp.dhis.tracker.imports.domain.TrackerEvent event =
        org.hisp.dhis.tracker.imports.domain.TrackerEvent.builder()
            .event(UID.generate())
            .enrollment(enrollmentUid)
            .program(MetadataIdentifier.ofUid(program.getUid()))
            .programStage(MetadataIdentifier.ofUid(programStage.getUid()))
            .orgUnit(MetadataIdentifier.ofUid("h4w96yEMlzO"))
            .status(EventStatus.COMPLETED)
            .occurredAt(Instant.now())
            .attributeOptionCombo(MetadataIdentifier.EMPTY_UID)
            .build();
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build(),
            TrackerObjects.builder()
                .enrollments(List.of(enrollment))
                .events(List.of(event))
                .build()));
  }

  private List<String> messageSubjects() {
    return manager.getAll(MessageConversation.class).stream()
        .map(MessageConversation::getSubject)
        .toList();
  }

  private ProgramNotificationTemplate addNotificationTemplate(
      String subject, NotificationTrigger trigger) {
    return addNotificationTemplate(
        subject, trigger, ProgramNotificationRecipient.USER_GROUP, userGroup);
  }

  private ProgramNotificationTemplate addNotificationTemplate(
      String subject,
      NotificationTrigger trigger,
      ProgramNotificationRecipient recipient,
      UserGroup group) {
    ProgramNotificationTemplate template = new ProgramNotificationTemplate();
    template.setAutoFields();
    template.setUid(CodeGenerator.generateUid());
    template.setName(subject);
    template.setSubjectTemplate(subject);
    template.setMessageTemplate("message");
    template.setNotificationTrigger(trigger);
    template.setNotificationRecipient(recipient);
    if (group != null) {
      template.setRecipientUserGroup(group);
    }
    manager.save(template);
    return template;
  }

  private void addLifecycleTemplate(String subject, NotificationTrigger trigger, Program p) {
    ProgramNotificationTemplate template = addNotificationTemplate(subject, trigger);
    p.getNotificationTemplates().add(template);
    manager.update(p);
  }

  private void addLifecycleTemplate(String subject, NotificationTrigger trigger, ProgramStage ps) {
    ProgramNotificationTemplate template = addNotificationTemplate(subject, trigger);
    ps.getNotificationTemplates().add(template);
    manager.update(ps);
  }

  private void addSendMessageRule(ProgramNotificationTemplate template) {
    addProgramRule(template, ProgramRuleActionType.SENDMESSAGE, null);
  }

  private void addScheduleMessageRule(ProgramNotificationTemplate template, String dateData) {
    addProgramRule(template, ProgramRuleActionType.SCHEDULEMESSAGE, dateData);
  }

  private void addProgramRule(
      ProgramNotificationTemplate template, ProgramRuleActionType actionType, String data) {
    ProgramRule rule = createProgramRule('N', program);
    rule.setUid(CodeGenerator.generateUid());
    rule.setCondition("true");
    programRuleService.addProgramRule(rule);

    ProgramRuleAction action = createProgramRuleAction('N', rule);
    action.setProgramRuleActionType(actionType);
    action.setTemplateUid(template.getUid());
    action.setNotificationTemplate(template);
    action.setData(data);
    programRuleActionService.addProgramRuleAction(action);

    rule.getProgramRuleActions().add(action);
    programRuleService.updateProgramRule(rule);
  }
}
