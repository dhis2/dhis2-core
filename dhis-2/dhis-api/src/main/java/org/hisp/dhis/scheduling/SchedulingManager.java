package org.hisp.dhis.scheduling;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

/**
 * Interface for scheduling jobs.
 * <p>
 * <p>
 * The main steps of the scheduling:
 * <p>
 * <ul>
 * <li>Create a job configuration {@link JobConfiguration}</li>
 * <li>This job configuration needs a job specific parameters object {@link JobParameters}, ie {@link org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters}.</li>
 * <li>Call scheduleJob with the job configuration.</li>
 * <li>The schedulingManager calls the spring scheduler with a runnable object {@link JobInstance}.</li>
 * <li>When the cron expression ocours the job will try to execute from the runnable object, job instance.</li>
 * </ul>
 *
 * @author Henning Håkonsen
 */
public interface SchedulingManager
{
    /**
     * Check if this jobconfiguration is currently running
     *
     * @param jobConfiguration the job to check
     * @return true/false
     */
    boolean isJobConfigurationRunning( JobConfiguration jobConfiguration );

    /**
     * Set up default behavior for a started job.
     * @param jobConfiguration the job which started
     */
    void jobConfigurationStarted( JobConfiguration jobConfiguration );

    /**
     * Set up default behavior for a finished job.
     *
     * A special case is if a job is disabled when running, but the job does not stop. The job wil run normally one last time and
     * try to set finished status. Since the job is disabled we manually set these parameters in this method so that the
     * job is not automatically rescheduled.
     *
     * Also we dont want to update a job configuration of the job is deleted.
     *
     * @param jobConfiguration the job which started
     */
    void jobConfigurationFinished( JobConfiguration jobConfiguration );

    /**
     * Get a job based on the job type.
     *
     * @param jobType the job type for the job we want to collect
     * @return the job
     */
    Job getJob( JobType jobType );

    /**
     * Schedules a job with the given job configuration.
     *
     * @param jobConfiguration the job to schedule.
     */
    void scheduleJob( JobConfiguration jobConfiguration );

    /**
     * Stops one job.
     */
    void stopJob( JobConfiguration jobConfiguration );

    /**
     * Execute the job.
     *
     * @param jobConfiguration The configuration of the job to be executed
     */
    boolean executeJob( JobConfiguration jobConfiguration );

    /**
     * Execute an actual job without validation
     *
     * @param job The job to be executed
     */
    void executeJob( Runnable job );

    /**
     * Execute the given job immediately and return a ListenableFuture.
     *
     * @param callable the job to execute.
     * @param <T> return type of the supplied callable.
     * @return a ListenableFuture representing the result of the job.
     */
    <T> ListenableFuture<T> executeJob( Callable<T> callable );

    /**
     * Returns a list of all scheduled jobs sorted based on cron expression and the current time.
     *
     * @return list of jobs
     */
    Map<String, ScheduledFuture<?>> getAllFutureJobs();
}
