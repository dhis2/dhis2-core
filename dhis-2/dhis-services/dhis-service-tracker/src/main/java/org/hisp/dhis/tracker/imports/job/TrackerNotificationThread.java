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
package org.hisp.dhis.tracker.imports.job;

import java.util.Map;
import java.util.function.Consumer;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.notification.ProgramNotificationService;
import org.hisp.dhis.security.SecurityContextRunnable;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Class represents a thread which will be triggered as soon as tracker notification consumer
 * consumes a message from tracker notification queue.
 *
 * @author Zubair Asghar
 */
@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class TrackerNotificationThread extends SecurityContextRunnable {
  private final Notifier notifier;

  private TrackerSideEffectDataBundle sideEffectDataBundle;

  private final IdentifiableObjectManager manager;

  private final Map<Class<? extends BaseIdentifiableObject>, Consumer<Long>> serviceMapper;

  public TrackerNotificationThread(
      ProgramNotificationService programNotificationService,
      Notifier notifier,
      IdentifiableObjectManager manager) {
    this.notifier = notifier;
    this.manager = manager;
    this.serviceMapper =
        Map.of(
            Enrollment.class, programNotificationService::sendEnrollmentNotifications,
            Event.class, programNotificationService::sendEventCompletionNotifications);
  }

  @Override
  public void call() {
    if (sideEffectDataBundle == null) {
      return;
    }

    if (serviceMapper.containsKey(sideEffectDataBundle.getKlass())) {
      BaseIdentifiableObject object =
          manager.get(sideEffectDataBundle.getKlass(), sideEffectDataBundle.getObject());
      if (object != null) {
        serviceMapper.get(sideEffectDataBundle.getKlass()).accept(object.getId());
      }
    }

    notifier.notify(
        sideEffectDataBundle.getJobConfiguration(),
        NotificationLevel.DEBUG,
        "Tracker notification side effects completed");
  }

  public void setSideEffectDataBundle(TrackerSideEffectDataBundle sideEffectDataBundle) {
    this.sideEffectDataBundle = sideEffectDataBundle;
  }
}
