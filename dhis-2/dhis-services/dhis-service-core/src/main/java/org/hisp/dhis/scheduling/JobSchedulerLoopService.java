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

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.feedback.NotFoundException;

/**
 * The service abstraction with all support functions the {@link JobScheduler} needs. This is used
 * to abstract away other services in the system as well as all DB access.
 *
 * <p>This is an internal service that should only be called by the scheduler. It is not meant to be
 * called by any user triggered actions (from controllers).
 *
 * @author Jan Bernitt
 * @since 2.41
 */
public interface JobSchedulerLoopService {

  /**
   * @return true, if node is or become the leader, else false
   */
  boolean tryBecomeLeader(int ttlSeconds);

  /**
   * Called to stay being the leader. Being the leader times out if not renewed this way. By
   * renewing in a regular interval a leader stays the leader until he is unable to renew.
   */
  void assureAsLeader(int ttlSeconds);

  /**
   * Get all job configurations that should start within the next n seconds.
   *
   * @param dueInNextSeconds number of seconds from now the job should start
   * @return only jobs that should start soon within the given number of seconds
   */
  List<JobConfiguration> getDueJobConfigurations(int dueInNextSeconds);

  @CheckForNull
  JobConfiguration getNextInQueue(String queue, int fromPosition);

  /**
   * A successful update means the DB was updated and the state flipped from {@link
   * JobStatus#SCHEDULED} to {@link JobStatus#RUNNING}. This also sets the {@link
   * JobConfiguration#getLastExecuted()} to "now" and the {@link
   * JobConfiguration#getLastExecutedStatus()} to {@link JobStatus#RUNNING}.
   *
   * @param jobId of the job to switch to a {@link JobStatus#RUNNING} state
   * @return true, if update was successful and the execution should begin, otherwise false
   */
  boolean tryRun(@Nonnull String jobId);

  /**
   * Called when a run of the provided job is about to be processed.
   *
   * @param jobId the job that will be executed
   * @return the progress tracker to use
   */
  JobProgress startRun(@Nonnull String jobId, @CheckForNull String user, Runnable observer)
      throws NotFoundException;

  /**
   * Heartbeat signal while processing a job so observers can identify a hanging or stale run. In
   * such a case the methods gets no longer called which stops updating the alive timestamp in the
   * database for the provided job configuration. If the alive timestamp is too old the job is
   * considered hanging or stale.
   *
   * <p>There is no exact timeout as the update to this is done cooperative as part of the
   * processing. Some steps in the processing may take seconds or few minutes making it not send a
   * heartbeat during that longer step time.
   *
   * @param jobId of the job being executed at the moment
   */
  void assureAsRunning(@Nonnull String jobId);

  /**
   * Called on a successful completion of the job process.
   *
   * <p>Switches back the status from {@link JobStatus#RUNNING} to {@link JobStatus#SCHEDULED} and
   * records the {@link JobConfiguration#getLastRuntimeExecution()} as the time between the {@link
   * JobConfiguration#getLastExecuted()} and "now".
   *
   * <p>The {@link JobConfiguration#getLastExecutedStatus()} is set to {@link JobStatus#COMPLETED}.
   *
   * <p>Disabled status is preserved. This means if the {@link JobConfiguration#isEnabled()} is
   * false the status is updated to {@link JobStatus#DISABLED} when this method is called.
   *
   * <p>Idempotent: If the status is not {@link JobStatus#RUNNING} when called the call has no
   * effect.
   *
   * @param jobId the job that finished running
   * @return true, of the status change was successful, else false
   */
  boolean completeRun(@Nonnull String jobId);

  /**
   * Adjusts a job after it failed before completion. The {@link JobConfiguration#getJobStatus()} is
   * changed from {@link JobStatus#RUNNING} to {@link JobStatus#SCHEDULED}.
   *
   * <p>Records the {@link JobConfiguration#getLastRuntimeExecution()} as the time between the
   * {@link JobConfiguration#getLastExecuted()} and "now" and updates the {@link
   * JobConfiguration#getLastExecutedStatus()} to {@link JobStatus#FAILED}.
   *
   * <p>Disabled status is preserved. This means if the {@link JobConfiguration#isEnabled()} is
   * false the status is updated to {@link JobStatus#DISABLED} when this method is called.
   *
   * <p>Idempotent: If the status is not {@link JobStatus#RUNNING} when called the call has no
   * effect.
   *
   * @param jobId the job that failed before it could complete
   */
  void failRun(@Nonnull String jobId, @CheckForNull Exception ex);

  /**
   * Adjusts a job after it has been cancelled. The {@link JobConfiguration#getJobStatus()} is
   * changed from {@link JobStatus#RUNNING} to {@link JobStatus#SCHEDULED}.
   *
   * <p>Records the {@link JobConfiguration#getLastRuntimeExecution()} as the time between the
   * {@link JobConfiguration#getLastExecuted()} and "now" and updates the {@link
   * JobConfiguration#getLastExecutedStatus()} to {@link JobStatus#STOPPED}.
   *
   * <p>Disabled status is preserved. This means if the {@link JobConfiguration#isEnabled()} is
   * false the status is updated to {@link JobStatus#DISABLED} when this method is called.
   *
   * <p>Idempotent: If the status is not {@link JobStatus#RUNNING} when called the call has no
   * effect.
   *
   * @param jobId the job that got cancelled
   */
  void cancelRun(@Nonnull String jobId);
}
