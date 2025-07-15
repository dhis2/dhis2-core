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

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.security.SecurityContextRunnable;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Class represents a thread which will be triggered as soon as tracker rule engine consumer
 * consumes a message from tracker rule engine queue. It loops through the list of notifications and
 * implement it if it has an associated rule implementer class.
 *
 * @author Zubair Asghar
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class TrackerRuleEngineThread extends SecurityContextRunnable {
  private final NotificationSender notificationSender;
  private final Notifier notifier;

  @Setter private TrackerNotificationDataBundle notificationDataBundle;

  @Override
  public void call() {
    if (notificationDataBundle == null) {
      return;
    }

    for (Notification effect : notificationDataBundle.getEnrollmentNotifications()) {
      Enrollment enrollment = notificationDataBundle.getEnrollment();
      enrollment.setProgram(notificationDataBundle.getProgram());
      this.notificationSender.send(effect, enrollment);
    }

    for (Notification effect : notificationDataBundle.getEventNotifications()) {
      Event event = notificationDataBundle.getEvent();
      event.getProgramStage().setProgram(notificationDataBundle.getProgram());
      this.notificationSender.send(effect, event);
    }

    notifier.notify(
        notificationDataBundle.getJobConfiguration(),
        "Tracker Rule-engine notifications completed");
  }
}
