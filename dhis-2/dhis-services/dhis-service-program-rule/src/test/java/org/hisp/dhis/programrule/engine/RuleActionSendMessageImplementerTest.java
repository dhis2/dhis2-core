/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.programrule.engine;

import static org.hisp.dhis.programrule.api.NotificationAction.SEND_MESSAGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.notification.ProgramNotificationService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.programrule.api.NotificationEffect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuleActionSendMessageImplementerTest {
  private static final UID TEMPLATE_UID = UID.of("h4w96yEMlzO");
  private static final UID RULE_UID = UID.of("TvctPPhpD8u");
  private static final String ENROLLMENT_UID = "enrollmentUid";
  private static final String EVENT_UID = "eventUid";

  @Mock private ProgramNotificationTemplateService programNotificationTemplateService;

  @Mock private NotificationLoggingService notificationLoggingService;

  @InjectMocks private NotificationHelper notificationHelper;

  @Mock private ProgramNotificationService programNotificationService;

  private RuleActionSendMessageImplementer ruleActionScheduleMessageImplementer;

  @BeforeEach
  void setUp() {
    ruleActionScheduleMessageImplementer =
        new RuleActionSendMessageImplementer(notificationHelper, programNotificationService);
  }

  @Test
  void shouldFailValidationForEnrollmentWhenTemplateIsNotFound() {
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(null);

    ruleActionScheduleMessageImplementer.implement(ruleEffect(), enrollment());

    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldFailValidationForEnrollmentWhenNotRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    NotificationEffect ruleEffect = ruleEffect();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(notRepeatableNotificationLog());
    ruleActionScheduleMessageImplementer.implement(ruleEffect, enrollment());

    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEnrollmentWhenRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    NotificationEffect ruleEffect = ruleEffect();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(repeatableNotificationLog());
    ruleActionScheduleMessageImplementer.implement(ruleEffect, enrollment());

    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEnrollmentWhenNoNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    NotificationEffect ruleEffect = ruleEffect();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(null);
    ruleActionScheduleMessageImplementer.implement(ruleEffect, enrollment());

    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, times(1)).save(any());
  }

  @Test
  void shouldFailValidationForEventWhenTemplateIsNotFound() {
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(null);

    ruleActionScheduleMessageImplementer.implement(ruleEffect(), event());

    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldFailValidationForEventWhenNotRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    NotificationEffect ruleEffect = ruleEffect();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(notRepeatableNotificationLog());
    ruleActionScheduleMessageImplementer.implement(ruleEffect, event());

    verify(programNotificationService, never()).sendProgramRuleTriggeredNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForProgramEventWhenNotRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    NotificationEffect ruleEffect = ruleEffect();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    ruleActionScheduleMessageImplementer.implement(ruleEffect, programEvent());

    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredEventNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEventWhenRepeatableNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    NotificationEffect ruleEffect = ruleEffect();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(repeatableNotificationLog());
    ruleActionScheduleMessageImplementer.implement(ruleEffect, event());

    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredEventNotifications(any(), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldPassValidationForEventWhenNoNotificationLogIsPresent() {
    ProgramNotificationTemplate template = template();
    NotificationEffect ruleEffect = ruleEffect();
    when(programNotificationTemplateService.getByUid(TEMPLATE_UID.getValue())).thenReturn(template);
    when(notificationLoggingService.getByKey(TEMPLATE_UID.getValue().concat(ENROLLMENT_UID)))
        .thenReturn(null);
    ruleActionScheduleMessageImplementer.implement(ruleEffect, event());

    verify(programNotificationService, times(1))
        .sendProgramRuleTriggeredEventNotifications(any(), any());
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

  private Event event() {
    Program program = new Program();
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    Event event = new Event();
    event.setUid(EVENT_UID);
    event.setEnrollment(enrollment());
    return event;
  }

  private Event programEvent() {
    Program program = new Program();
    program.setProgramType(ProgramType.WITHOUT_REGISTRATION);
    Event event = new Event();
    event.setUid(EVENT_UID);
    event.setEnrollment(enrollment(program));
    return event;
  }

  private NotificationEffect ruleEffect() {
    return new NotificationEffect(SEND_MESSAGE, RULE_UID, TEMPLATE_UID, null);
  }
}
