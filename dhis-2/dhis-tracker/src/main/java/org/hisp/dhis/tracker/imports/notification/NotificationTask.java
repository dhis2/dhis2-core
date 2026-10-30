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

import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.security.SecurityContextRunnable;
import org.hisp.dhis.tracker.imports.programrule.engine.Notification;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.tracker.model.SingleEvent;
import org.hisp.dhis.tracker.model.TrackerEvent;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Async task that sends all notifications (lifecycle and rule engine) for a single tracker entity.
 * Iterates the deduplicated notification list and delegates to {@link NotificationSender}.
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class NotificationTask extends SecurityContextRunnable {
  private final NotificationSender notificationSender;

  @Setter private EntityNotifications entityNotifications;
  @Setter private Map<Long, Set<GroupMemberInfo>> groupMembers;

  @Override
  public void call() {
    IdentifiableObject entity = entityNotifications.entity();
    for (Notification notification : entityNotifications.notifications()) {
      if (entity instanceof Enrollment enrollment) {
        notificationSender.send(notification, enrollment, groupMembers);
      } else if (entity instanceof TrackerEvent event) {
        notificationSender.send(notification, event, groupMembers);
      } else if (entity instanceof SingleEvent singleEvent) {
        notificationSender.send(notification, singleEvent, groupMembers);
      }
    }
  }
}
