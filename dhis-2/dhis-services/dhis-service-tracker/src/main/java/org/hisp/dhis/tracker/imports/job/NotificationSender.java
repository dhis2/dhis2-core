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
package org.hisp.dhis.tracker.imports.job;

import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.notification.logging.NotificationTriggerEvent;
import org.hisp.dhis.notification.logging.NotificationValidationResult;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.SingleEvent;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.program.notification.ProgramNotificationInstance;
import org.hisp.dhis.program.notification.ProgramNotificationInstanceService;
import org.hisp.dhis.program.notification.ProgramNotificationService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.program.notification.template.snapshot.NotificationTemplateService;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Sends or schedules a notification to be sent as a result of a rule-engine evaluation. */
@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationSender {
  public static final String LOG_MESSAGE = "Notification with id:%s has been scheduled";
  private final ProgramNotificationInstanceService programNotificationInstanceService;
  private final ProgramNotificationService programNotificationService;
  private final NotificationTemplateService notificationTemplateService;
  private final ProgramNotificationTemplateService programNotificationTemplateService;
  private final NotificationLoggingService notificationLoggingService;

  @Transactional
  public void send(Notification notification, Enrollment enrollment) {
    ProgramNotificationTemplate template = getNotificationTemplate(notification);

    NotificationValidationResult result = validate(template, enrollment);

    if (!result.isValid()) {
      return;
    }

    if (notification.scheduledAt() != null) {
      ProgramNotificationInstance notificationInstance =
          notificationTemplateService.createNotificationInstance(
              template, notification.scheduledAt());
      notificationInstance.setEnrollment(enrollment);

      programNotificationInstanceService.save(notificationInstance);
    } else {
      programNotificationService.sendProgramRuleTriggeredNotifications(template, enrollment);
    }
    if (result.needsToCreateLogEntry()) {
      createLogEntry(template, enrollment);
    }
  }

  @Transactional
  public void send(Notification notification, TrackerEvent event) {
    ProgramNotificationTemplate template = getNotificationTemplate(notification);

    NotificationValidationResult result = validate(template, event.getEnrollment());

    if (!result.isValid()) {
      return;
    }

    if (notification.scheduledAt() != null) {
      ProgramNotificationInstance notificationInstance =
          notificationTemplateService.createNotificationInstance(
              template, notification.scheduledAt());
      notificationInstance.setTrackerEvent(event);

      programNotificationInstanceService.save(notificationInstance);
    } else {
      programNotificationService.sendProgramRuleTriggeredEventNotifications(template, event);
    }

    if (result.needsToCreateLogEntry()) {
      createLogEntry(template, event.getEnrollment());
    }
  }

  @Transactional
  public void send(Notification notification, SingleEvent singleEvent) {
    ProgramNotificationTemplate template = getNotificationTemplate(notification);

    if (notification.scheduledAt() != null) {
      ProgramNotificationInstance notificationInstance =
          notificationTemplateService.createNotificationInstance(
              template, notification.scheduledAt());

      notificationInstance.setSingleEvent(singleEvent);

      programNotificationInstanceService.save(notificationInstance);
    } else {
      programNotificationService.sendProgramRuleTriggeredEventNotifications(template, singleEvent);
    }
  }

  private void createLogEntry(ProgramNotificationTemplate template, Enrollment enrollment) {
    String key = generateKey(template, enrollment);
    ExternalNotificationLogEntry entry = new ExternalNotificationLogEntry();
    entry.setLastSentAt(new Date());
    entry.setKey(key);
    entry.setNotificationTemplateUid(template.getUid());
    entry.setNotificationTriggeredBy(NotificationTriggerEvent.PROGRAM);
    entry.setAllowMultiple(template.isSendRepeatable());

    notificationLoggingService.save(entry);
  }

  private ProgramNotificationTemplate getNotificationTemplate(Notification notification) {
    String uid = notification.template().getValue();
    return programNotificationTemplateService.getByUid(uid);
  }

  private NotificationValidationResult validate(
      ProgramNotificationTemplate template, Enrollment enrollment) {
    if (template == null) {
      return NotificationValidationResult.invalid();
    }

    if (enrollment == null || enrollment.getProgram().isWithoutRegistration()) {
      return NotificationValidationResult.validAndNoNeedForLogEntries();
    }

    ExternalNotificationLogEntry logEntry =
        notificationLoggingService.getByKey(generateKey(template, enrollment));

    // template has already been delivered and repeated delivery not allowed
    if (logEntry != null && !logEntry.isAllowMultiple()) {
      return NotificationValidationResult.invalid();
    }

    return logEntry == null
        ? NotificationValidationResult.validAndNeedsLogEntries()
        : NotificationValidationResult.validAndNoNeedForLogEntries();
  }

  private String generateKey(ProgramNotificationTemplate template, Enrollment enrollment) {
    return template.getUid() + enrollment.getUid();
  }
}
