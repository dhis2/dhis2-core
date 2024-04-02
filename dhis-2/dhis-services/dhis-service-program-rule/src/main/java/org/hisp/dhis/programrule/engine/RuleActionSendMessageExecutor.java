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
package org.hisp.dhis.programrule.engine;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.notification.logging.NotificationTriggerEvent;
import org.hisp.dhis.notification.logging.NotificationValidationResult;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentService;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.EventService;
import org.hisp.dhis.program.notification.ProgramNotificationTemplate;
import org.hisp.dhis.program.notification.ProgramNotificationTemplateService;
import org.hisp.dhis.program.notification.event.ProgramRuleEnrollmentEvent;
import org.hisp.dhis.program.notification.event.ProgramRuleStageEvent;
import org.hisp.dhis.programrule.ProgramRuleActionType;
import org.hisp.dhis.user.AuthenticationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 *
 * <ol>
 *   <li>Handle notifications related to enrollment/event
 *   <li>Trigger spring event to handle notification delivery in separate thread
 *   <li/>
 *   <li>Log and entry in {@link ExternalNotificationLogEntry}
 * </ol>
 */
@Component("org.hisp.dhis.programrule.engine.RuleActionSendMessageExecutor")
@Transactional
public class RuleActionSendMessageExecutor extends NotificationRuleActionExecutor {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private final ApplicationEventPublisher publisher;

  public RuleActionSendMessageExecutor(
      ProgramNotificationTemplateService programNotificationTemplateService,
      NotificationLoggingService notificationLoggingService,
      EnrollmentService enrollmentService,
      EventService eventService,
      ApplicationEventPublisher publisher,
      AuthenticationService authenticationService) {
    super(
        programNotificationTemplateService,
        notificationLoggingService,
        enrollmentService,
        eventService,
        authenticationService);
    this.publisher = publisher;
  }

  @Override
  public boolean accept(NotificationAction ruleAction) {
    return Objects.equals(ruleAction.getActionType(), ProgramRuleActionType.SENDMESSAGE.name());
  }

  @Override
  public void implement(NotificationAction action, Enrollment enrollment) {
    NotificationValidationResult result = validate(action, enrollment);

    if (!result.isValid()) {
      return;
    }

    ProgramNotificationTemplate template = result.getTemplate();

    String key = generateKey(template, enrollment);

    publisher.publishEvent(new ProgramRuleEnrollmentEvent(this, template.getId(), enrollment));

    if (result.getLogEntry() != null) {
      return;
    }

    persistLogEntry(key, template, NotificationTriggerEvent.PROGRAM);
  }

  @Override
  public void implement(NotificationAction action, Event event) {
    checkNotNull(event, "Event cannot be null");

    NotificationValidationResult result = validate(action, event.getEnrollment());

    // For program without registration
    if (event.getProgramStage().getProgram().isWithoutRegistration()) {
      handleSingleEvent(action, event);
      return;
    }

    if (!result.isValid()) {
      return;
    }

    ProgramNotificationTemplate template = result.getTemplate();
    String key = generateKey(template, event.getEnrollment());

    publisher.publishEvent(new ProgramRuleStageEvent(this, template.getId(), event));

    if (result.getLogEntry() != null) {
      return;
    }

    persistLogEntry(key, template, NotificationTriggerEvent.PROGRAM_STAGE);
  }

  private void persistLogEntry(
      String key, ProgramNotificationTemplate template, NotificationTriggerEvent triggerEvent) {
    ExternalNotificationLogEntry entry = createLogEntry(key, template.getUid());
    entry.setNotificationTriggeredBy(triggerEvent);
    entry.setAllowMultiple(template.isSendRepeatable());

    authenticationService.obtainSystemAuthentication();
    notificationLoggingService.save(entry);
    authenticationService.clearAuthentication();
  }

  private void handleSingleEvent(NotificationAction action, Event event) {
    ProgramNotificationTemplate template = getNotificationTemplate(action);

    if (template == null) {
      return;
    }

    publisher.publishEvent(new ProgramRuleStageEvent(this, template.getId(), event));
  }
}
