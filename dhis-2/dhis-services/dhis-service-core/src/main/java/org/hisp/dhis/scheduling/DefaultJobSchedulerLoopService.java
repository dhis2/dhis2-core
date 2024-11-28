/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.scheduling;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.joining;
import static org.hisp.dhis.eventhook.EventUtils.schedulerCompleted;
import static org.hisp.dhis.eventhook.EventUtils.schedulerFailed;
import static org.hisp.dhis.eventhook.EventUtils.schedulerStart;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.eventhook.EventHookPublisher;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.AuthenticationService;
import org.hisp.dhis.user.UserDetails;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Is responsible for the actual execution of the steps that together perform the scheduling.
 *
 * @author Jan Bernitt
 * @since 2.41
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultJobSchedulerLoopService implements JobSchedulerLoopService {

  private final SystemSettingsProvider settingsProvider;
  private final LeaderManager leaderManager;
  private final JobConfigurationStore jobConfigurationStore;
  private final JobConfigurationService jobConfigurationService;
  private final EventHookPublisher events;
  private final MessageService messages;
  private final Notifier notifier;
  private final AuthenticationService authenticationService;
  private final ObjectMapper jsonMapper;

  /**
   * Set of currently running jobs on this node. We use a map to use CAS operation {@link
   * Map#putIfAbsent(Object, Object)} to "atomically" start or abort execution.
   */
  private final Map<String, RecordingJobProgress> recordingsById = new ConcurrentHashMap<>();

  @Override
  @Transactional
  public void createHousekeepingJob(UserDetails actingUser) {
    JobType.Defaults defaults = JobType.HOUSEKEEPING.getDefaults();
    if (defaults == null) return;
    JobConfiguration config = jobConfigurationStore.getByUid(defaults.uid());
    if (config == null) {
      jobConfigurationService.createDefaultJob(JobType.HOUSEKEEPING, actingUser);
    } else if (config.getJobStatus() != JobStatus.SCHEDULED
        && (config.getLastAlive() == null
            || currentTimeMillis() - config.getLastAlive().getTime() > 60_000)) {
      finishRunCancel(config.getUid());
    }
  }

  @Override
  @Transactional(readOnly = true)
  public int applyCancellation() {
    int c = 0;
    for (String jobId : jobConfigurationStore.getAllCancelledIds()) {
      RecordingJobProgress progress = recordingsById.get(jobId);
      if (progress != null && !progress.isCancelled()) {
        progress.requestCancellation();
        c++;
      }
    }
    return c;
  }

  @Override
  public boolean tryBecomeLeader(int ttlSeconds) {
    leaderManager.electLeader(ttlSeconds);
    return leaderManager.isLeader();
  }

  @Override
  public void assureAsLeader(int ttlSeconds) {
    leaderManager.renewLeader(ttlSeconds);
  }

  @Override
  @Transactional(readOnly = true)
  public List<JobConfiguration> getDueJobConfigurations(int dueInNextSeconds) {
    return jobConfigurationService.getDueJobConfigurations(dueInNextSeconds, false);
  }

  @Override
  @CheckForNull
  @Transactional(readOnly = true)
  public JobConfiguration getNextInQueue(String queue, int fromPosition) {
    return jobConfigurationStore.getNextInQueue(queue, fromPosition);
  }

  @CheckForNull
  @Override
  @Transactional(readOnly = true)
  public JobConfiguration getJobConfiguration(String jobId) {
    return jobConfigurationStore.getByUid(jobId);
  }

  @Override
  @Transactional
  public boolean tryRun(@Nonnull String jobId) {
    if (!jobConfigurationStore.tryStart(jobId)) return false;
    JobConfiguration job = jobConfigurationStore.getByUid(jobId);
    if (job == null) return false;
    doSafely("start", "MDC.put", () -> MDC.put("sessionId", getSessionId(job)));
    doSafely("start", "publishEvent", () -> events.publishEvent(schedulerStart(job)));
    return true;
  }

  private static String getSessionId(JobConfiguration job) {
    return job.getUid() != null ? "UID:" + job.getUid() : "TYPE:" + job.getJobType().name();
  }

  @Override
  @Transactional(readOnly = true)
  public JobProgress startRun(@Nonnull String jobId, String user, Runnable observer)
      throws NotFoundException {
    if (user != null) {
      authenticationService.obtainAuthentication(user);
    } else {
      authenticationService.obtainSystemAuthentication();
    }
    JobConfiguration job = jobConfigurationStore.getByUid(jobId);
    if (job == null) return JobProgress.noop();
    return startRecording(job, observer);
  }

  @Override
  @Transactional
  public void updateAsRunning(@Nonnull String jobId) {
    updateProgress(jobId);
  }

  @Override
  @Transactional
  public boolean finishRunSuccess(@Nonnull String jobId) {
    if (!jobConfigurationStore.tryFinish(jobId, JobStatus.COMPLETED)) return false;
    JobConfiguration job = jobConfigurationStore.getByUid(jobId);
    if (job == null) return false;
    doSafely("complete", "stop recording", () -> stopRecording(jobId));
    doSafely("complete", "publishEvent", () -> events.publishEvent(schedulerCompleted(job)));
    return true;
  }

  @Override
  @Transactional
  public void finishRunFail(@Nonnull String jobId, @CheckForNull Exception ex) {
    if (jobConfigurationStore.tryFinish(jobId, JobStatus.FAILED)) {
      JobConfiguration job = jobConfigurationStore.getByUid(jobId);
      if (job == null) return;
      String message = String.format("Job failed: '%s'", job.getName());
      doSafely("fail", "stop recording", () -> stopRecording(jobId));
      doSafely("fail", "log.error", () -> logError(message, ex));
      doSafely("fail", "MDC.remove", () -> MDC.remove("sessionId"));
      doSafely("fail", "publishEvent", () -> events.publishEvent(schedulerFailed(job)));
      Exception cause = ex;
      if (cause == null) {
        RecordingJobProgress progress = recordingsById.get(jobId);
        if (progress != null) cause = progress.getCause();
      }
      if (cause != null) {
        Exception causeF = cause;
        doSafely(
            "fail",
            "sendSystemErrorNotification",
            () -> messages.asyncSendSystemErrorNotification(message, causeF));
      }
      skipRestOfQueue(job);
    }
  }

  @Override
  @Transactional
  public void finishRunCancel(@Nonnull String jobId) {
    if (jobConfigurationStore.tryFinish(jobId, JobStatus.STOPPED)) {
      JobConfiguration job = jobConfigurationStore.getByUid(jobId);
      if (job == null) return;
      String message = String.format("Job cancelled: '%s'", job.getName());
      doSafely("cancel", "stop recording", () -> stopRecording(jobId));
      doSafely("cancel", "log.error", () -> logError(message, null));
      doSafely("cancel", "MDC.remove", () -> MDC.remove("sessionId"));
      doSafely("cancel", "publishEvent", () -> events.publishEvent(schedulerFailed(job)));
      skipRestOfQueue(job);
    }
  }

  /** Skip further items in the same queue with {@link JobStatus#NOT_STARTED}. */
  private void skipRestOfQueue(JobConfiguration job) {
    String queue = job.getQueueName();
    if (queue != null) jobConfigurationStore.trySkip(queue);
  }

  private void doSafely(String phase, String step, Runnable action) {
    try {
      action.run();
    } catch (Exception ex) {
      log.error(format("Exception while running job %s post action: %s", phase, step), ex);
    }
  }

  private static void logError(String message, Exception ex) {
    if (ex != null) {
      log.error(message, ex);
      log.error(DebugUtils.getStackTrace(ex));
    } else {
      log.error(message);
    }
  }

  private JobProgress startRecording(@Nonnull JobConfiguration job, @Nonnull Runnable observer) {
    JobProgress tracker =
        job.getJobType().isUsingNotifications()
            ? new NotifierJobProgress(notifier, job)
            : JobProgress.noop();
    boolean logInfoOnDebug =
        job.getSchedulingType() != SchedulingType.ONCE_ASAP
            && job.getLastExecuted() != null
            && Duration.between(job.getLastExecuted().toInstant(), Instant.now()).getSeconds()
                < settingsProvider.getCurrentSettings().getJobsLogDebugBelowSeconds();
    RecordingJobProgress progress =
        new RecordingJobProgress(messages, job, tracker, true, observer, logInfoOnDebug, false);
    recordingsById.put(job.getUid(), progress);
    return progress;
  }

  private void stopRecording(@Nonnull String jobId) {
    RecordingJobProgress job = recordingsById.get(jobId);
    if (job != null) {
      job.autoComplete();
      updateProgress(jobId);
      recordingsById.remove(jobId);
    }
  }

  private void updateProgress(@Nonnull String jobId) {
    RecordingJobProgress job = recordingsById.get(jobId);
    if (job == null) return;
    try {
      JobProgress.Progress progress = job.getProgress();
      String errorCodes = progress.getErrorCodes().stream().sorted().collect(joining(" "));
      jobConfigurationStore.updateProgress(
          jobId, jsonMapper.writeValueAsString(progress), errorCodes);
    } catch (JsonProcessingException ex) {
      jobConfigurationStore.updateProgress(jobId, null, null);
      log.error("Failed to attach progress json", ex);
    }
  }
}
