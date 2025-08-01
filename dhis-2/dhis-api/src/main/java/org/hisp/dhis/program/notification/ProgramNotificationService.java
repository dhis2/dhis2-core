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
package org.hisp.dhis.program.notification;

import java.util.Date;
import java.util.List;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.scheduling.JobProgress;

/**
 * @author Halvdan Hoem Grelland
 */
public interface ProgramNotificationService {
  /**
   * Send all scheduled notifications for the given day.
   *
   * <p>Queries for any upcoming events or Enrollments which have a {@link
   * ProgramNotificationTemplate} scheduled for the given day, creates the messages and immediately
   * dispatches them.
   *
   * <p>Potentially a time consuming job, depending on the amount of configured notifications, the
   * amount of recipients, the message types (SMS, email, dhis message) and the amount of events
   * resolved by the query.
   *
   * <p>Due to the time consuming nature of the process this method should be wrapped in an
   * asynchronous task.
   *
   * @param day the Date representing the day relative to the scheduled notifications for which to
   *     send messages.
   * @param progress tracking of job progress
   */
  void sendScheduledNotificationsForDay(Date day, JobProgress progress);

  /**
   * Sends all notifications which are scheduled by program rule and having scheduledDate for today.
   *
   * @param progress tracking of job progress
   */
  void sendScheduledNotifications(JobProgress progress);

  /**
   * Send completion notifications for the Event. If the ProgramStage is not configured with
   * suitable {@link ProgramNotificationTemplate templates}, nothing will happen.
   *
   * @param eventId the event id.
   */
  void sendEventCompletionNotifications(long eventId);

  /**
   * Send completion notifications for the Enrollment triggered by ProgramRule evaluation. {@link
   * ProgramNotificationTemplate templates}, nothing will happen.
   *
   * @param template ProgramNotificationTemplate to send
   * @param enrollment the enrollment.
   */
  void sendProgramRuleTriggeredNotifications(
      ProgramNotificationTemplate template, Enrollment enrollment);

  /**
   * Send completion notifications for the Event triggered by ProgramRule evaluation. {@link
   * ProgramNotificationTemplate templates}, nothing will happen.
   *
   * @param template ProgramNotificationTemplate to send
   * @param event the event.
   */
  void sendProgramRuleTriggeredEventNotifications(
      ProgramNotificationTemplate template, TrackerEvent event);

  /**
   * Send completion notifications for the Enrollment. If the Program is not configured with
   * suitable {@link ProgramNotificationTemplate templates}, nothing will happen.
   *
   * @param enrollment the Enrollment id.
   */
  void sendEnrollmentCompletionNotifications(long enrollment);

  /**
   * Send enrollment notifications for the Enrollment. If the Program is not configured with
   * suitable {@link ProgramNotificationTemplate templates}, nothing will happen.
   *
   * @param enrollment the Enrollment id.
   */
  void sendEnrollmentNotifications(long enrollment);

  /**
   * Get all events which have notifications with the given ProgramNotificationTemplate scheduled on
   * the given date.
   *
   * @param template the template.
   * @param notificationDate the Date for which the notification is scheduled.
   * @return a list of Event.
   */
  @Deprecated
  List<TrackerEvent> getWithScheduledNotifications(
      ProgramNotificationTemplate template, Date notificationDate);

  /**
   * Get all enrollments which have notifications with the given ProgramNotificationTemplate
   * scheduled on the given date.
   *
   * @param template the template.
   * @param notificationDate the Date for which the notification is scheduled.
   * @return a list of Enrollment.
   */
  @Deprecated
  List<Enrollment> getEnrollmentsWithScheduledNotifications(
      ProgramNotificationTemplate template, Date notificationDate);
}
