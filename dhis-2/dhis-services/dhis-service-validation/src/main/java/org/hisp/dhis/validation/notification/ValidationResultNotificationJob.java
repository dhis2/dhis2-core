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
package org.hisp.dhis.validation.notification;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobProgress;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.system.notification.NotificationLevel;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.system.util.Clock;
import org.springframework.stereotype.Component;

/**
 * @author Stian Sandvold
 */
@Component("validationResultNotificationJob")
public class ValidationResultNotificationJob implements Job {
  private final ValidationNotificationService notificationService;

  private final MessageService messageService;

  private final Notifier notifier;

  public ValidationResultNotificationJob(
      ValidationNotificationService notificationService,
      MessageService messageService,
      Notifier notifier) {
    checkNotNull(notificationService);
    checkNotNull(messageService);
    checkNotNull(notifier);

    this.notificationService = notificationService;
    this.messageService = messageService;
    this.notifier = notifier;
  }

  // -------------------------------------------------------------------------
  // Implementation
  // -------------------------------------------------------------------------

  @Override
  public JobType getJobType() {
    return JobType.VALIDATION_RESULTS_NOTIFICATION;
  }

  @Override
  public void execute(JobConfiguration jobConfiguration, JobProgress progress) {
    final Clock clock = new Clock().startClock();

    notifier.notify(jobConfiguration, "Sending new validation result notifications");

    try {
      runInternal();

      notifier.notify(
          jobConfiguration,
          NotificationLevel.INFO,
          "Sent validation result notifications: " + clock.time(),
          true);
    } catch (RuntimeException ex) {
      notifier.notify(
          jobConfiguration, NotificationLevel.ERROR, "Process failed: " + ex.getMessage(), true);

      messageService.sendSystemErrorNotification(
          "Sending validation result notifications failed", ex);

      throw ex;
    }
  }

  void runInternal() {
    notificationService.sendUnsentNotifications();
  }
}
