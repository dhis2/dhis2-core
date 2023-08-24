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
package org.hisp.dhis.scheduling;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.schema.Property;
import org.springframework.util.MimeType;

/**
 * Simple service for {@link JobConfiguration} objects.
 *
 * @author Henning Håkonsen (original)
 * @author Jan Bernitt (rework with scheduling based on DB)
 */
public interface JobConfigurationService {

  String create(JobConfiguration config) throws ConflictException;

  String create(JobConfiguration config, MimeType contentType, InputStream content)
      throws ConflictException;

  /**
   * Creates the default {@link JobConfiguration} for all {@link JobType}s which have a {@link
   * JobType.Defaults} configuration and which do not yet exist in the DB.
   *
   * @return number of created entries
   */
  int createDefaultJobs();

  /**
   * Make sure the {@link JobType#HEARTBEAT} entry exists as it is responsible for spawning the
   * other system jobs when needed using {@link #createDefaultJobs()}.
   */
  void createHeartbeatJob();

  /**
   * Updates all {@link JobConfiguration}s that are not {@link JobConfiguration#isEnabled()} to
   * state {@link JobStatus#DISABLED} in case they are in state {@link JobStatus#SCHEDULED}.
   *
   * @return number of updated entries
   */
  int updateDisabledJobs();

  /**
   * Removes {@link JobConfiguration}s of {@link SchedulingType#ONCE_ASAP} when they have been
   * finished for at least the given time.
   *
   * @param ttlMinutes minimum duration in minutes since matches finished running
   * @return number of deleted entries
   */
  int deleteFinishedJobs(int ttlMinutes);

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

  /**
   * Add a job configuration
   *
   * @param jobConfiguration the job configuration to be added
   * @return id
   */
  long addJobConfiguration(JobConfiguration jobConfiguration);

  /**
   * Update an existing job configuration
   *
   * @param jobConfiguration the job configuration to be added
   * @return id
   */
  long updateJobConfiguration(JobConfiguration jobConfiguration);

  /**
   * Delete a job configuration
   *
   * @param jobConfiguration the id of the job configuration to be deleted
   */
  void deleteJobConfiguration(JobConfiguration jobConfiguration);

  /**
   * Get job configuration for given id
   *
   * @param jobId id for job configuration
   * @return Job configuration
   */
  JobConfiguration getJobConfiguration(long jobId);

  /**
   * Get a job configuration for given uid
   *
   * @param uid uid to search for
   * @return job configuration
   */
  JobConfiguration getJobConfigurationByUid(String uid);

  /**
   * Get all job configurations
   *
   * @return list of all job configurations in the system
   */
  List<JobConfiguration> getAllJobConfigurations();

  /**
   * Get all job configurations for a specific {@link JobType}.
   *
   * @param type to select
   * @return all configuration for the given {@link JobType}
   */
  List<JobConfiguration> getJobConfigurations(JobType type);

  /**
   * Get all job configurations that should start within the next n seconds.
   *
   * @param dueInNextSeconds number of seconds from now the job should start
   * @param includeWaiting true to also list jobs that cannot run because another job of the same
   *     type is already running
   * @return only jobs that should start soon within the given number of seconds
   */
  Stream<JobConfiguration> getDueJobConfigurations(int dueInNextSeconds, boolean includeWaiting);

  /**
   * Finds stale jobs.
   *
   * @param staleForSeconds the duration for which the job has not been updated (alive).
   * @return all jobs that appear to be stale (hanging) considering the given timeout
   */
  List<JobConfiguration> getStaleConfigurations(int staleForSeconds);

  /**
   * Get a map of parameter classes with appropriate properties This can be used for a frontend app
   * or for other appropriate applications which needs information about the jobs in the system.
   *
   * <p>It uses {@link JobType}.
   *
   * @return map with parameters classes
   */
  Map<String, Map<String, Property>> getJobParametersSchema();

  /**
   * Returns a list of all configurable and available job types.
   *
   * @return a list of {@link JobTypeInfo}.
   */
  List<JobTypeInfo> getJobTypeInfo();
}
