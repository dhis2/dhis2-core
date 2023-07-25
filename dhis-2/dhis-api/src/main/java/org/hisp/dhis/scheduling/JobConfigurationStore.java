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
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.hisp.dhis.common.GenericDimensionalObjectStore;

/**
 * Special methods for handling the scheduler main loop execution.
 *
 * @author Jan Bernitt
 */
public interface JobConfigurationStore extends GenericDimensionalObjectStore<JobConfiguration> {

  /**
   * The UID of the job for the given type that most recently started execution
   *
   * @param type the type of job to find
   * @return the UID of the most recently started job configuration or null if none exists
   */
  @CheckForNull
  String getLastRunningId(@Nonnull JobType type);

  /**
   * The UID of the job for the given type that most recently finished with any outcome
   *
   * @param type the type of job to find
   * @return the UID of the most recently finished job configuration or null if none exists
   */
  @CheckForNull
  String getLastCompletedId(@Nonnull JobType type);

  /**
   * @param jobId of the job whose last complected progress to fetch
   * @return the last completed progress raw JSON of the given job or null if it is not available
   */
  @CheckForNull
  String getCompletedProgress(@Nonnull String jobId);

  /**
   * @return All UIDs of all existing job configurations.
   */
  Set<String> getAllIds();

  /**
   * Lists all jobs of a specific type.
   *
   * @param type the type to list
   * @return all jobs of the given type
   */
  List<JobConfiguration> getJobConfigurations(JobType type);

  /**
   * Finds stale jobs.
   *
   * @param timeoutSeconds the duration for which the job has not been updated (alive).
   * @return all jobs that appear to be stale (hanging) considering the given timeout
   */
  List<JobConfiguration> getStaleConfigurations(int timeoutSeconds);

  /**
   * This list contains any trigger for a {@link JobConfiguration} that wants to run and matches the
   * basic preconditions. Such a trigger might still be blocked by another job of the same type
   * currently running. In such a case the run is simply postponed until a start attempt that
   * succeeds. For this there is no explicit change needed as the {@link
   * JobConfiguration#getLastExecuted()} will not update so once "now" got past the point where it
   * is time to trigger a new run the trigger stays active until it actually can trigger.
   *
   * <p>OBS! The {@link JobConfiguration}s returned are not fully initialized.
   *
   * @return all job configurations that could potentially be started based on their cron expression
   *     or delay time
   */
  List<JobConfiguration> getAllTriggers();

  /**
   * @return A list of all job types that are currently in {@link JobStatus#RUNNING} state.
   */
  Set<JobType> getRunningTypes();

  /**
   * @return A list of all job types that are currently not {@link JobStatus#RUNNING} and that have
   *     a {@link org.hisp.dhis.scheduling.JobProgress.Progress} information attached for the last
   *     completed run.
   */
  Set<JobType> getCompletedTypes();

  /**
   * @param queue name of the queue
   * @param fromPosition current position in the queue (executed last)
   * @return the job next in line if exists or null otherwise
   */
  @CheckForNull
  JobConfiguration getNextInQueue(@Nonnull String queue, int fromPosition);

  /**
   * Change the {@link SchedulingType} so the job runs {@link SchedulingType#ONCE_ASAP}.
   *
   * <p>After that run it changes back to another {@link SchedulingType} based on if a cron
   * expression or delay is defined.
   *
   * @param jobId of the job to switch to {@link SchedulingType#ONCE_ASAP}
   * @return true, if the update was successful, otherwise false
   */
  boolean tryScheduleToRunOutOfOrder(@Nonnull String jobId);

  /**
   * A successful update means the DB state flipped from {@link JobStatus#SCHEDULED} to {@link
   * JobStatus#RUNNING}.
   *
   * <p>If the update is successful the {@link JobConfiguration#getLastExecuted()} is also updated
   * to "now".
   *
   * @param jobId of the job to switch to {@link JobStatus#RUNNING} state
   * @return true, if update was successful, otherwise false
   */
  boolean tryRun(@Nonnull String jobId);

  /**
   * A successful update means the DB state flipped from {@link JobStatus#RUNNING} to {@link
   * JobStatus#SCHEDULED} (enabled) or {@link JobStatus#DISABLED} (not enabled).
   *
   * @param jobId of the job to switch to {@link JobStatus#SCHEDULED} or {@link JobStatus#DISABLED}
   *     based on the {@link JobConfiguration#isEnabled()} state
   * @param lastExecutedStatus the result of the execution to remember as {@link
   *     JobConfiguration#getLastExecutedStatus()}
   * @return true, if the update was successful, otherwise false
   */
  boolean tryStop(@Nonnull String jobId, JobStatus lastExecutedStatus);

  /**
   * If this has no effect there either were no further items in the queue or the items were not in
   * an inconsistent execution state, that is some had been started or completed while others were
   * not done (yet).
   *
   * @param queue name of the queue that should be skipped
   * @return true, if any further items in the given queue were skipped successful, else false.
   */
  boolean trySkip(@Nonnull String queue);

  /**
   * If an update is not successful this means the job was no longer in a running state.
   *
   * @param jobId of the job that is currently running (it worker calls this as a heart-beat)
   * @return true, if the update was successful, else false.
   */
  boolean assureRunning(@Nonnull String jobId);

  void attachProgress(@Nonnull String jobId, @CheckForNull String progressJson);

  /**
   * Switches {@link JobConfiguration#getJobStatus()} to {@link JobStatus#DISABLED} for any job that
   * currently is still {@link JobStatus#SCHEDULED} while it has been set to {@link
   * JobConfiguration#isEnabled()} {@code false}.
   *
   * @return number of job configurations that were affected
   */
  int updateDisabledJobs();

  /**
   * Clears the DB from finished jobs that only should run once. The TTL can be used to keep them
   * for a certain duration after they were finished for observation purposes.
   *
   * @param ttlSeconds duration after the job finished before it gets cleared
   * @return the number of finished {@link SchedulingType#ONCE_ASAP} that got removed
   */
  int deleteFinishedJobs(int ttlSeconds);

  /**
   * Jobs that apparently are stuck in {@link JobStatus#RUNNING} are "force reset" to {@link
   * JobStatus#SCHEDULED}. Such run then counts as {@link JobStatus#FAILED}.
   *
   * <p>This will only affect jobs which had updated the {@link JobConfiguration#getLastAlive()} at
   * least once. This is to protect against aborting a job that does not support alive signals as it
   * does not yet use the {@link JobProgress} tracking.
   *
   * @param timeoutMinutes duration in minutes for which the job has not been updated for it to be
   *     considered stale and changed back to {@link JobStatus#SCHEDULED}.
   * @return number of job configurations that were affected
   */
  int rescheduleStaleJobs(int timeoutMinutes);
}
