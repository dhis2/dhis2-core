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

import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.scheduling.JobProgress.Progress;

/**
 * This is the external API (called by users via controller API) for the scheduling.
 *
 * @author Jan Bernitt
 * @since 2.41
 */
public interface JobSchedulerService {

  /*
  API used by REST API and other services
   */

  /**
   * Request cancellation for job of given type. If no job of that type is currently running the
   * operation has no effect.
   *
   * <p>Cancellation is cooperative abort. The job will abort at the next possible "safe-point".
   * This is the next step or item in the overall process which checks for cancellation.
   *
   * @param jobId of the job to issue a cluster wide cancel request
   * @return if cancellation state was accepted
   */
  boolean requestCancel(@Nonnull String jobId);

  boolean requestCancel(@Nonnull JobType type);

  /**
   * Attempts to switch the {@link JobConfiguration#getSchedulingType()} to {@link
   * SchedulingType#ONCE_ASAP} for the given job.
   *
   * <p>A job with a {@link JobConfiguration#getCronExpression()} switches back to {@link
   * SchedulingType#CRON} once it finished. A job with a {@link JobConfiguration#getDelay()}
   * switches back to {@link SchedulingType#FIXED_DELAY} once it finished.
   *
   * @param jobId if the job that should run now
   * @throws NotFoundException when no such job configuration exists
   * @throws ConflictException when the status change cannot be performed, for example because the
   *     job is already running or is disabled
   */
  void executeNow(@Nonnull String jobId) throws ConflictException, NotFoundException;

  /**
   * Check if this job configuration is currently running
   *
   * @param type type of job to check
   * @return true/false
   */
  boolean isRunning(@Nonnull JobType type);

  /**
   * @return a set of job types for which a job is running currently
   */
  @Nonnull
  Set<JobType> getRunningTypes();

  @Nonnull
  Set<JobType> getCompletedTypes();

  @CheckForNull
  Progress getRunningProgress(@Nonnull String jobId);

  @CheckForNull
  Progress getRunningProgress(@Nonnull JobType type);

  @CheckForNull
  Progress getCompletedProgress(@Nonnull String jobId);

  @CheckForNull
  Progress getCompletedProgress(@Nonnull JobType type);

  /*
  API used by the leader node only
  */

  /**
   * Apply cancellation for jobs running on this node that have been marked as cancelled in the DB.
   *
   * @return number of jobs that were cancelled as a result (which had not been cancelled before)
   */
  int applyCancellation();

  JobProgress startRecording(@Nonnull JobConfiguration job, @Nonnull Runnable observer);

  void stopRecording(@Nonnull String jobId);

  void updateProgress(@Nonnull String jobId);
}
