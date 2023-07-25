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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.JobProgress.Progress;
import org.hisp.dhis.system.notification.Notifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultJobSchedulerService implements JobSchedulerService {

  private final CacheProvider cacheProvider;
  private final Notifier notifier;
  private final MessageService messages;
  private final JobConfigurationStore jobConfigurationStore;
  private final ObjectMapper jsonMapper;

  /**
   * Set of currently running jobs. We use a map to use CAS operation {@link Map#putIfAbsent(Object,
   * Object)} to "atomically" start or abort execution.
   */
  private final Map<String, RecordingJobProgress> recordingsById = new ConcurrentHashMap<>();

  private Cache<Progress> clusterProgressById;
  private Cache<Boolean> clusterCancellationById;

  @PostConstruct
  public void init() {
    this.clusterProgressById = cacheProvider.createRunningJobsInfoCache();
    this.clusterCancellationById = cacheProvider.createJobCancelRequestedCache();
  }

  @Override
  public void syncCluster() {
    for (Entry<String, RecordingJobProgress> info : recordingsById.entrySet()) {
      String jobId = info.getKey();
      if (clusterCancellationById.getIfPresent(jobId).isPresent()) {
        requestCancel(jobId);
        clusterCancellationById.invalidate(jobId);
      } else {
        // update progress in case this is the leader
        clusterProgressById.put(jobId, info.getValue().getProgress());
      }
    }
  }

  @Override
  public void requestCancel(@Nonnull String jobId) {
    RecordingJobProgress progress = recordingsById.get(jobId);
    if (progress != null) {
      progress.requestCancellation();
    } else {
      // no matter which node received the cancel this is shared
      clusterCancellationById.put(jobId, true);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public void requestCancel(@Nonnull JobType type) {
    String jobId = jobConfigurationStore.getLastRunningId(type);
    if (jobId != null) requestCancel(jobId);
  }

  @Override
  @Transactional
  public void executeNow(@Nonnull String jobId) throws NotFoundException, ConflictException {
    if (!jobConfigurationStore.tryScheduleToRunOutOfOrder(jobId)) {
      JobConfiguration job = jobConfigurationStore.getByUid(jobId);
      if (job == null) throw new NotFoundException(JobConfiguration.class, jobId);
      if (job.getJobStatus() == JobStatus.RUNNING)
        throw new ConflictException("Job is already running.");
      if (job.getSchedulingType() == SchedulingType.ONCE_ASAP)
        throw new ConflictException("Job is already scheduled for immediate execution.");
    }
  }

  @Override
  public boolean isRunning(@Nonnull JobType type) {
    return jobConfigurationStore.getRunningTypes().contains(type);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Set<JobType> getRunningTypes() {
    return jobConfigurationStore.getRunningTypes();
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public Set<JobType> getCompletedTypes() {
    return jobConfigurationStore.getCompletedTypes();
  }

  @Override
  public Progress getRunningProgress(@Nonnull String jobId) {
    RecordingJobProgress progress = recordingsById.get(jobId);
    return progress == null ? null : progress.getProgress();
  }

  @Override
  @Transactional(readOnly = true)
  public Progress getRunningProgress(@Nonnull JobType type) {
    String jobId = jobConfigurationStore.getLastRunningId(type);
    return jobId == null ? null : getRunningProgress(jobId);
  }

  @Override
  @Transactional(readOnly = true)
  public Progress getCompletedProgress(@Nonnull String jobId) {
    String json = jobConfigurationStore.getCompletedProgress(jobId);
    if (json == null) return null;
    try {
      return jsonMapper.readValue(json, Progress.class);
    } catch (JsonProcessingException ex) {
      log.error("Failed to map job progress", ex);
      return null;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Progress getCompletedProgress(@Nonnull JobType type) {
    String jobId = jobConfigurationStore.getLastCompletedId(type);
    return jobId == null ? null : getCompletedProgress(jobId);
  }

  @Override
  public JobProgress startRecording(@Nonnull JobConfiguration job, @Nonnull Runnable observer) {
    JobProgress tracker =
        job.getJobType().isUsingNotifications()
            ? new NotifierJobProgress(notifier, job)
            : NoopJobProgress.INSTANCE;
    RecordingJobProgress progress =
        new RecordingJobProgress(messages, job, tracker, true, observer);
    recordingsById.put(job.getUid(), progress);
    return progress;
  }

  @Override
  public void stopRecording(String jobId) {
    RecordingJobProgress job = recordingsById.remove(jobId);
    if (job != null) {
      clusterProgressById.invalidate(jobId);
      clusterCancellationById.invalidate(jobId);
      job.autoComplete();
      try {
        jobConfigurationStore.attachProgress(
            jobId, jsonMapper.writeValueAsString(job.getProgress()));
      } catch (JsonProcessingException ex) {
        jobConfigurationStore.attachProgress(jobId, null);
        log.error("Failed to attach progress json", ex);
      }
    }
  }
}
