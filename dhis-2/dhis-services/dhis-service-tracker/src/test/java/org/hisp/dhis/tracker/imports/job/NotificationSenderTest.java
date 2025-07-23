/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.tracker.imports.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateService;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSenderTest {
  private static final UID TEMPLATE_UID = UID.of("h4w96yEMlzO");
  private static final String ENROLLMENT_UID = "enrollmentUid";
  private static final UID EVENT_UID = UID.generate();

  @Mock private ProgramNotificationTemplateService programNotificationTemplateService;

  @Mock private NotificationLoggingService notificationLoggingService;

  @Mock private ProgramNotificationService programNotificationService;

  @Mock private ProgramNotificationInstanceService programNotificationInstanceService;

  @Mock private NotificationTemplateService notificationTemplateService;

  private NotificationSender notificationSender;

  @BeforeEach
  void setUp() {
    notificationSender =
        new NotificationSender(
            programNotificationInstanceService,
            programNotificationService,
            notificationTemplateService,
            programNotificationTemplateService,
            notificationLoggingService);
  }

  @Test
  void shouldFailValidationForEnrollmentWhenTemplateIsNotFound() {
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(null);

    notificationSender.send(scheduleMessage(), enrollment());

    verify(notificationTemplateService, never())
        .createNotificationInstance(any(), any(String.class));
    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldFailValidationForEnrollmentWhenNotRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = scheduleMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(notRepeatableNotificationLog());
    notificationSender.send(ruleEffect, enrollment());

    verify(notificationTemplateService, never())
        .createNotificationInstance(any(), any(String.class));
    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void
      shouldPassValidationForEnrollmentWhenSchedulingMessageAndRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = scheduleMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(repeatableNotificationLog());
    when(notificationTemplateService.createNotificationInstance(template, ruleEffect.scheduledAt()))
        .thenReturn(new ProgramNotificationInstance());
    notificationSender.send(ruleEffect, enrollment());

    verify(programNotificationInstanceService, times(1)).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEnrollmentWhenSendingMessageAndRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = sendMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(repeatableNotificationLog());
    notificationSender.send(ruleEffect, enrollment());

    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEnrollmentWhenSchedulingAndMessageNoNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = scheduleMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(null);
    when(notificationTemplateService.createNotificationInstance(template, ruleEffect.scheduledAt()))
        .thenReturn(new ProgramNotificationInstance());
    notificationSender.send(ruleEffect, enrollment());

    verify(programNotificationInstanceService, times(1)).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, times(1)).save(any());
  }

  @Test
  void shouldPassValidationForEnrollmentWhenSendingMessageDateAndNoNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = sendMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    notificationSender.send(ruleEffect, enrollment());

    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, times(1)).save(any());
  }

  @Test
  void shouldFailValidationForEventWhenTemplateIsNotFound() {
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(null);

    notificationSender.send(scheduleMessage(), event());

    verify(notificationTemplateService, never())
        .createNotificationInstance(any(), any(String.class));
    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldFailValidationForEventWhenSchedulingMessageAndNotRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = scheduleMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(notRepeatableNotificationLog());
    notificationSender.send(ruleEffect, event());

    verify(notificationTemplateService, never())
        .createNotificationInstance(any(), any(String.class));
    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldFailValidationForEventWhenSendingMessageAndNotRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = sendMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(notRepeatableNotificationLog());
    notificationSender.send(ruleEffect, event());

    verify(notificationTemplateService, never())
        .createNotificationInstance(any(), any(String.class));
    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void
      shouldPassValidationForSingleEventWhenSchedulingMessageAndNotRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = scheduleMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationTemplateService.createNotificationInstance(template, ruleEffect.scheduledAt()))
        .thenReturn(new ProgramNotificationInstance());
    notificationSender.send(ruleEffect, singleEvent());

    verify(programNotificationInstanceService, times(1)).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void
      shouldPassValidationForSingleEventWhenSendingMessageAndNotRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = sendMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    notificationSender.send(ruleEffect, singleEvent());

    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredEventNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEventWhenScheduleMessageAndRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = scheduleMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(repeatableNotificationLog());
    when(notificationTemplateService.createNotificationInstance(template, ruleEffect.scheduledAt()))
        .thenReturn(new ProgramNotificationInstance());
    notificationSender.send(ruleEffect, event());

    verify(programNotificationInstanceService, times(1)).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEventWhenSendingMessageAndRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = sendMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(repeatableNotificationLog());
    notificationSender.send(ruleEffect, event());

    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredEventNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEventWhenSchedulingMessageAndNoNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = scheduleMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(null);
    when(notificationTemplateService.createNotificationInstance(template, ruleEffect.scheduledAt()))
        .thenReturn(new ProgramNotificationInstance());
    notificationSender.send(ruleEffect, event());

    verify(programNotificationInstanceService, times(1)).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, times(1)).save(any());
  }

  @Test
  void shouldPassValidationForEventWhenSendingMessageAndNoNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    Notification ruleEffect = sendMessage();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(null);
    notificationSender.send(ruleEffect, event());

    verify(programNotificationInstanceService, never()).save(any());
    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, times(1)).save(any());
  }

  private ExternalNotificationLogEntry notRepeatableNotificationLog() {
    ExternalNotificationLogEntry externalNotificationLogEntry = new ExternalNotificationLogEntry();
    externalNotificationLogEntry.setAllowMultiple(false);
    return externalNotificationLogEntry;
  }

  private ExternalNotificationLogEntry repeatableNotificationLog() {
    ExternalNotificationLogEntry externalNotificationLogEntry = new ExternalNotificationLogEntry();
    externalNotificationLogEntry.setAllowMultiple(true);
    return externalNotificationLogEntry;
  }

  private ProgramNotificationTemplate template() {
    ProgramNotificationTemplate programNotificationTemplate = new ProgramNotificationTemplate();
    programNotificationTemplate.setUid(TEMPLATE_UID.getValue());
    programNotificationTemplate.setSendRepeatable(true);
    return programNotificationTemplate;
  }

  private Enrollment enrollment() {
    Program program = new Program();
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(ENROLLMENT_UID);
    enrollment.setProgram(program);
    return enrollment;
  }

  private Enrollment enrollment(Program program) {
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(ENROLLMENT_UID);
    enrollment.setProgram(program);
    return enrollment;
  }

  private TrackerEvent event() {
    Program program = new Program();
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    TrackerEvent event = new TrackerEvent();
    event.setUid(EVENT_UID.getValue());
    event.setEnrollment(enrollment());
    return event;
  }

  private SingleEvent singleEvent() {
    Program program = new Program();
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    SingleEvent event = new SingleEvent();
    event.setUid(EVENT_UID.getValue());
    event.setEnrollment(enrollment(program));
    return event;
  }

  private Notification sendMessage() {
    return new Notification(TEMPLATE_UID, null);
  }

  private Notification scheduleMessage() {
    return new Notification(TEMPLATE_UID, new Date());
  }
}
