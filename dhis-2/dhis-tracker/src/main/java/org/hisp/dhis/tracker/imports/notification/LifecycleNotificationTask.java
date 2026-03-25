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
package org.hisp.dhis.tracker.imports.notification;

import java.util.Map;
import java.util.function.Consumer;
import lombok.Setter;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.security.SecurityContextRunnable;
import org.hisp.dhis.tracker.program.notification.ProgramNotificationService;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Async task that sends notifications for entity lifecycle events (enrollment creation,
 * enrollment/event completion). Loads the entity by ID and dispatches to {@link
 * org.hisp.dhis.tracker.program.notification.ProgramNotificationService} based on the notification
 * triggers in the bundle.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class LifecycleNotificationTask extends SecurityContextRunnable {
  @Setter private TrackerNotificationDataBundle notificationDataBundle;

  private final IdentifiableObjectManager manager;

  private final Map<NotificationTrigger, Consumer<Long>> serviceMapper;

  public LifecycleNotificationTask(
      ProgramNotificationService programNotificationService, IdentifiableObjectManager manager) {
    this.manager = manager;
    this.serviceMapper =
        Map.of(
            NotificationTrigger.ENROLLMENT, programNotificationService::sendEnrollmentNotifications,
            NotificationTrigger.TRACKER_EVENT_COMPLETION,
                programNotificationService::sendTrackerEventCompletionNotifications,
            NotificationTrigger.SINGLE_EVENT_COMPLETION,
                programNotificationService::sendSingleEventCompletionNotifications,
            NotificationTrigger.ENROLLMENT_COMPLETION,
                programNotificationService::sendEnrollmentCompletionNotifications);
  }

  @Override
  public void call() {
    if (notificationDataBundle == null) {
      return;
    }

    for (NotificationTrigger trigger : notificationDataBundle.getTriggers()) {
      if (serviceMapper.containsKey(trigger)) {
        IdentifiableObject object =
            manager.get(notificationDataBundle.getKlass(), notificationDataBundle.getObject());
        if (object != null) {
          serviceMapper.get(trigger).accept(object.getId());
        }
      }
    }
  }
}
