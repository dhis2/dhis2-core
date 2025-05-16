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
package org.hisp.dhis.tracker.imports.programrule.executor.event;

import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.notification.logging.ExternalNotificationLogEntry;
import org.hisp.dhis.notification.logging.NotificationLoggingService;
import org.hisp.dhis.notification.logging.NotificationTriggerEvent;
import org.hisp.dhis.notification.logging.NotificationValidationResult;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobExecutionService;
import org.hisp.dhis.scheduling.JobType;
import org.hisp.dhis.tracker.imports.TrackerEventScheduleParams;
import org.hisp.dhis.tracker.imports.bundle.TrackerBundle;
import org.hisp.dhis.tracker.imports.domain.Event;
import org.hisp.dhis.tracker.imports.programrule.ProgramRuleIssue;
import org.hisp.dhis.tracker.imports.programrule.executor.RuleActionExecutor;

/**
 * @author Zubair Asghar
 */
@RequiredArgsConstructor
public class CreateEventExecutor implements RuleActionExecutor<Event> {
  private final JobExecutionService jobExecutionService;
  private final NotificationLoggingService notificationLoggingService;
  private final UID programStage;
  private final String scheduledAt;

  @Override
  public Optional<ProgramRuleIssue> executeRuleAction(TrackerBundle bundle, Event event) {
    ProgramStage stage = bundle.getPreheat().getProgramStage(programStage.getValue());
    Enrollment enrollment = bundle.getPreheat().getEnrollment(event.getEnrollment());

    NotificationValidationResult result = validate(stage, enrollment);

    if (!result.isValid()) {
      return Optional.empty();
    }

    TrackerEventScheduleParams params = new TrackerEventScheduleParams();
    params.setEnrollment(event.getEnrollment().getValue());
    params.setOrgUnit(event.getOrgUnit().getIdentifier());
    params.setAttributeOptionCombo(
        bundle.getPreheat().getDefault(CategoryOptionCombo.class).getUid());
    params.setProgramStage(programStage.getValue());
    params.setScheduledAt(scheduledAt);
    params.setUserName(bundle.getUser().getUsername());

    JobConfiguration jobConfiguration =
        new JobConfiguration(JobType.TRACKER_IMPORT_EVENT_SCHEDULE_JOB);
    jobConfiguration.setExecutedBy(bundle.getUser().getUid());
    jobConfiguration.setJobParameters(params);

    try {
      jobExecutionService.executeOnceNow(jobConfiguration);
    } catch (ConflictException e) {
      throw new RuntimeException(e);
    }

    if (result.needsToCreateLogEntry()) {
      createLogEntry(stage, enrollment);
    }

    return Optional.empty();
  }

  private NotificationValidationResult validate(ProgramStage programStage, Enrollment enrollment) {
    if (programStage == null) {
      return NotificationValidationResult.invalid();
    }

    if (enrollment == null || enrollment.getProgram().isWithoutRegistration()) {
      return NotificationValidationResult.validAndNoNeedForLogEntries();
    }

    ExternalNotificationLogEntry logEntry =
        notificationLoggingService.getByKey(generateKey(programStage, enrollment));

    if (logEntry != null && !logEntry.isAllowMultiple()) {
      return NotificationValidationResult.invalid();
    }

    return logEntry == null
        ? NotificationValidationResult.validAndNeedsLogEntries()
        : NotificationValidationResult.validAndNoNeedForLogEntries();
  }

  private void createLogEntry(ProgramStage programStage, Enrollment enrollment) {
    String key = generateKey(programStage, enrollment);
    ExternalNotificationLogEntry entry = new ExternalNotificationLogEntry();
    entry.setLastSentAt(new Date());
    entry.setKey(key);
    entry.setNotificationTemplateUid(programStage.getUid());
    entry.setNotificationTriggeredBy(NotificationTriggerEvent.PROGRAM_STAGE);
    entry.setAllowMultiple(programStage.getRepeatable());

    notificationLoggingService.save(entry);
  }

  private String generateKey(ProgramStage programStage, Enrollment enrollment) {
    return programStage.getUid() + enrollment.getUid();
  }
}
