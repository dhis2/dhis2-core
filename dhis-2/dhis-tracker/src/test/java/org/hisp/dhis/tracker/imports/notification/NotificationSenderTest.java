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
package org.hisp.dhis.tracker.imports.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.hisp.dhis.tracker.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.tracker.program.notification.ProgramNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSenderTest {
  private static final String TEMPLATE_UID = "h4w96yEMlzO";
  private static final String ENROLLMENT_UID = "enrollmentUid";

  @Mock private ProgramNotificationTemplateService programNotificationTemplateService;
  @Mock private NotificationLoggingService notificationLoggingService;
  @Mock private ProgramNotificationService programNotificationService;
  @Mock private ProgramNotificationInstanceService programNotificationInstanceService;

  private NotificationSender notificationSender;

  @BeforeEach
  void setUp() {
    notificationSender =
        new NotificationSender(
            programNotificationInstanceService,
            programNotificationService,
            programNotificationTemplateService,
            notificationLoggingService);
  }

  @Test
  void shouldNotSendWhenTemplateIsNull() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(null);

    notificationSender.send(sendMessage(), enrollment(), Map.of());

    verify(programNotificationService, never())
        .sendNotification(any(), any(Enrollment.class), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldSendAndCreateLogEntryForNonRepeatableTemplate() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(template());

    notificationSender.send(sendMessage(), enrollment(), Map.of());

    verify(programNotificationService, times(1))
        .sendNotification(any(), any(Enrollment.class), any());
    verify(notificationLoggingService, times(1)).save(any());
  }

  @Test
  void shouldScheduleAndCreateLogEntryForNonRepeatableTemplate() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(template());

    notificationSender.send(scheduleMessage(), enrollment(), Map.of());

    verify(programNotificationInstanceService, times(1)).save(any());
    verify(programNotificationService, never())
        .sendNotification(any(), any(Enrollment.class), any());
    verify(notificationLoggingService, times(1)).save(any());
  }

  @Test
  void shouldNotSendWhenLogEntryExistsAndNotRepeatable() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(template());
    org.hisp.dhis.notification.logging.ExternalNotificationLogEntry logEntry =
        new org.hisp.dhis.notification.logging.ExternalNotificationLogEntry();
    logEntry.setAllowMultiple(false);
    when(notificationLoggingService.getByKey(TEMPLATE_UID + ENROLLMENT_UID)).thenReturn(logEntry);

    notificationSender.send(sendMessage(), enrollment(), Map.of());

    verify(programNotificationService, never())
        .sendNotification(any(), any(Enrollment.class), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldSendWithoutLogEntryForRepeatableTemplate() {
    ProgramNotificationTemplate t = template();
    t.setSendRepeatable(true);
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(t);

    notificationSender.send(sendMessage(), enrollment(), Map.of());

    verify(programNotificationService, times(1))
        .sendNotification(any(), any(Enrollment.class), any());
    verify(notificationLoggingService, never()).save(any());
  }

  @Test
  void shouldSendEventNotification() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(template());

    notificationSender.send(sendMessage(), event(), Map.of());

    verify(programNotificationService, times(1))
        .sendNotification(any(), any(TrackerEvent.class), any());
    verify(notificationLoggingService, times(1)).save(any());
  }

  @Test
  void shouldScheduleEventNotification() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(template());

    notificationSender.send(scheduleMessage(), event(), Map.of());

    verify(programNotificationInstanceService, times(1)).save(any());
    verify(programNotificationService, never())
        .sendNotification(any(), any(TrackerEvent.class), any());
  }

  @Test
  void shouldNotSendEventWhenLogEntryExistsAndNotRepeatable() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(template());
    org.hisp.dhis.notification.logging.ExternalNotificationLogEntry logEntry =
        new org.hisp.dhis.notification.logging.ExternalNotificationLogEntry();
    logEntry.setAllowMultiple(false);
    when(notificationLoggingService.getByKey(TEMPLATE_UID + ENROLLMENT_UID)).thenReturn(logEntry);

    notificationSender.send(sendMessage(), event(), Map.of());

    verify(programNotificationService, never())
        .sendNotification(any(), any(TrackerEvent.class), any());
  }

  @Test
  void shouldSendSingleEventNotification() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(template());

    notificationSender.send(sendMessage(), singleEvent(), Map.of());

    verify(programNotificationService, times(1))
        .sendNotification(any(), any(SingleEvent.class), any());
  }

  @Test
  void shouldScheduleSingleEventNotification() {
    when(programNotificationTemplateService.getByUidCached(TEMPLATE_UID)).thenReturn(template());

    notificationSender.send(scheduleMessage(), singleEvent(), Map.of());

    verify(programNotificationInstanceService, times(1)).save(any());
    verify(programNotificationService, never())
        .sendNotification(any(), any(SingleEvent.class), any());
  }

  private ProgramNotificationTemplate template() {
    ProgramNotificationTemplate t = new ProgramNotificationTemplate();
    t.setUid(TEMPLATE_UID);
    t.setSendRepeatable(false);
    return t;
  }

  private Enrollment enrollment() {
    Program program = new Program();
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    Enrollment enrollment = new Enrollment();
    enrollment.setUid(ENROLLMENT_UID);
    enrollment.setProgram(program);
    return enrollment;
  }

  private TrackerEvent event() {
    TrackerEvent event = new TrackerEvent();
    event.setUid(UID.generate().getValue());
    event.setEnrollment(enrollment());
    return event;
  }

  private SingleEvent singleEvent() {
    SingleEvent event = new SingleEvent();
    event.setUid(UID.generate().getValue());
    return event;
  }

  private Notification sendMessage() {
    return new Notification(UID.of(TEMPLATE_UID), null);
  }

  private Notification scheduleMessage() {
    return new Notification(UID.of(TEMPLATE_UID), new Date());
  }
}
