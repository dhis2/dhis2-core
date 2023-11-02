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
import java.util.*;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.scheduling.JobProgress.Progress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jan Bernitt
 * @since 2.41
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultJobSchedulerService implements JobSchedulerService {

  private final JobConfigurationStore jobConfigurationStore;
  private final JobRunner jobRunner;
  private final ObjectMapper jsonMapper;

  @Override
  @Transactional
  public boolean requestCancel(@Nonnull String jobId) {
    return jobConfigurationStore.tryCancel(jobId);
  }

  @Override
  @Transactional
  public boolean requestCancel(@Nonnull JobType type) {
    String jobId = jobConfigurationStore.getLastRunningId(type);
    return jobId != null && requestCancel(jobId);
  }

  @Override
  @Transactional
  public void executeNow(@Nonnull String jobId) throws NotFoundException, ConflictException {
    if (!jobConfigurationStore.tryExecuteNow(jobId)) {
      JobConfiguration job = jobConfigurationStore.getByUid(jobId);
      if (job == null) throw new NotFoundException(JobConfiguration.class, jobId);
      if (job.getJobStatus() == JobStatus.RUNNING)
        throw new ConflictException("Job is already running.");
      if (job.getSchedulingType() == SchedulingType.ONCE_ASAP && job.getLastFinished() != null)
        throw new ConflictException("Job did already run once.");
      throw new ConflictException("Failed to transition job into ONCE_ASAP state.");
    }
    if (!jobRunner.isScheduling()) {
      JobConfiguration job = jobConfigurationStore.getByUid(jobId);
      if (job == null) throw new NotFoundException(JobConfiguration.class, jobId);
      // run "execute now" request directly when scheduling is not active (tests)
      jobRunner.runDueJob(job);
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
  @Transactional(readOnly = true)
  public Progress getProgress(@Nonnull String jobId) {
    String json = jobConfigurationStore.getProgress(jobId);
    return json == null ? null : mapToProgress(json);
  }

  @Nonnull
  @Override
  @Transactional(readOnly = true)
  public List<JobProgress.Error> getErrors(@Nonnull String jobId) {
    String json = jobConfigurationStore.getErrors(jobId);
    if (json == null) return List.of();
    Progress progress = mapToProgress("{\"sequence\":[],\"errors\":" + json + "}");
    if (progress == null) return List.of();
    Map<String, Map<ErrorCode, Queue<JobProgress.Error>>> map = progress.getErrors();
    return map.isEmpty()
        ? List.of()
        : map.values().stream()
            .flatMap(errors -> errors.values().stream().flatMap(Collection::stream))
            .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Progress getRunningProgress(@Nonnull JobType type) {
    String jobId = jobConfigurationStore.getLastRunningId(type);
    return jobId == null ? null : getProgress(jobId);
  }

  @Override
  @Transactional(readOnly = true)
  public Progress getCompletedProgress(@Nonnull JobType type) {
    String jobId = jobConfigurationStore.getLastCompletedId(type);
    return jobId == null ? null : getProgress(jobId);
  }

  private Progress mapToProgress(@Nonnull String json) {
    try {
      return jsonMapper.readValue(json, Progress.class);
    } catch (JsonProcessingException ex) {
      log.error("Failed to map job progress", ex);
      return null;
    }
  }
}
