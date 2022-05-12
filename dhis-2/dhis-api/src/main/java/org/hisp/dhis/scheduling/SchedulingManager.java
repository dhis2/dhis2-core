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

import java.util.Collection;
import java.util.Date;

import org.hisp.dhis.scheduling.JobProgress.Process;

/**
 * {@link Job}s are tasks that are supposed to run asynchronously.
 *
 * A {@link JobConfiguration} is a specific configuration for running such a
 * task.
 *
 * There should be one {@link JobConfiguration} for each {@link JobType} and
 * only one task of each {@link JobType} can run at the same time.
 *
 * Usually a {@link Job}'s execution is scheduled by using
 * {@link #schedule(JobConfiguration)} or
 * {@link #scheduleWithStartTime(JobConfiguration, Date)}.
 *
 * Alternatively a execution can be ran ad-hoc using
 * {@link #executeNow(JobConfiguration)}.
 *
 * There are ways to schedule a task for future execution:
 * <dl>
 * <dt>Cron expression ({@link SchedulingType#CRON}):</dt>
 * <dd>To {@link #schedule(JobConfiguration)} a task in a reoccurring time
 * pattern</dd>
 * <dt>Fixed delay time ({@link SchedulingType#FIXED_DELAY}):</dt>
 * <dd>To {@link #schedule(JobConfiguration)} a task in a fixed interval</dd>
 * <dt>Start time based:</dt>
 * <dd>To {@link #scheduleWithStartTime(JobConfiguration, Date)} a task once at
 * a specific point in time.</dd>
 * </dl>
 *
 * @see <a href=
 *      "https://github.com/dhis2/wow-backend/blob/master/docs/job_scheduling.md">Docs</a>
 *
 * @author Henning Håkonsen (initial)
 * @author Jan Bernitt (overhaul and extension)
 */
public interface SchedulingManager
{
    /**
     * Schedules a job with the given job configuration.
     *
     * This stops running jobs of the same {@link JobType} is necessary and
     * replaces existing scheduled tasks in the future.
     *
     * The job will be scheduled based on the
     * {@link JobConfiguration#getCronExpression()} property.
     *
     * @param configuration the job to schedule.
     */
    void schedule( JobConfiguration configuration );

    /**
     * Schedule a job with the given start time.
     *
     * This stops running jobs of the same {@link JobType} is necessary and
     * replaces existing scheduled tasks in the future.
     *
     * @param configuration The configuration with job details to be scheduled
     * @param startTime The time at which the job should start
     */
    void scheduleWithStartTime( JobConfiguration configuration, Date startTime );

    /**
     * Stops the currently running task with the same {@link JobType} as the
     * provided configuration.
     */
    void stop( JobConfiguration configuration );

    /**
     * Ad-hoc execution of a {@link JobConfiguration}.
     *
     * This only starts a new new task if no job with the same {@link JobType}
     * is running.
     *
     * @param configuration The configuration of the job to be executed
     * @return true, if the job was was not already running and got started now,
     *         otherwise false
     */
    boolean executeNow( JobConfiguration configuration );

    /**
     * Request cancellation for job of given type potentially running currently
     *
     * @param type job type to cancel
     */
    void cancel( JobType type );

    /**
     * Check if this job configuration is currently running
     *
     * @param type type of job to check
     * @return true/false
     */
    boolean isRunning( JobType type );

    /**
     * @return a set of job types for which a job is running currently
     */
    Collection<JobType> getRunningTypes();

    /**
     * @return a set of job types for which a job has finished running. Each
     *         type will contain the most recent completed run. Newer runs
     *         replace older ones.
     */
    Collection<JobType> getCompletedTypes();

    /**
     * @param type job type for which to return the current running progress
     * @return the progress of the running job
     */
    Collection<Process> getRunningProgress( JobType type );

    /**
     * @param type job type for which to return completed progress
     * @return the progress of the completed job
     */
    Collection<Process> getCompletedProgress( JobType type );
}
