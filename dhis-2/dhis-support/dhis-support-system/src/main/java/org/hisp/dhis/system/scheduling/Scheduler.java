package org.hisp.dhis.system.scheduling;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobInstance;
import org.hisp.dhis.scheduling.JobStatus;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;

/**
 * Scheduler for managing the scheduling and execution of tasks.
 *
 * @author Lars Helge Overland
 */
public interface Scheduler
{
    String CRON_DAILY_11PM = "0 0 23 * * ?";
    String CRON_DAILY_0AM = "0 0 0 * * ?";
    String CRON_DAILY_2AM = "0 0 2 * * ?";
    String CRON_DAILY_5AM = "0 0 5 * * ?";
    String CRON_DAILY_6AM = "0 0 6 * * ?";
    String CRON_DAILY_7AM = "0 0 7 * * ?";
    String CRON_DAILY_8AM = "0 0 8 * * ?";

    String CRON_EVERY_MIN = "0 0/1 * * * ?";
    String CRON_EVERY_15MIN = "0 0/15 * * * ?";

    String CRON_TEST = "0 * * * * ?";

    /**
     * Execute the given job immediately.
     *
     * @job the job to execute.
     */
    void executeJob( Runnable job );

    /**
     * Execute the given job immediately. The job can be referenced
     * again through the given job key if the current job is not completed. A job cannot be scheduled if another
     * job with the same key is already scheduled.
     *
     * @job the job to execute.
     */
    void executeJob( String jobKey, Runnable job );

    /**
     * Execute the given job immediately and return a ListenableFuture.
     *
     * @param callable the job to execute.
     * @param <T> return type of the supplied callable.
     * @return a ListenableFuture representing the result of the job.
     */
    <T> ListenableFuture<T> executeJob( Callable<T> callable );

    /**
     * Schedule the given job for future execution. The job can be referenced
     * later through the given job key. A job cannot be scheduled if another
     * job with the same key is already scheduled. The job must be unique for
     * the job but can have an arbitrary value.
     *
     * @param jobConfiguration the job to schedule
     * @return true if the job was scheduled for execution as a result of this
     *         operation, false if not.
     */
    boolean scheduleJob( JobConfiguration jobConfiguration, Job job );


    void scheduleJob(JobConfiguration jobConfiguration, JobInstance jobInstance );

    /**
     * Deactivates scheduling of the job with the given key.
     *
     * @param key the job key.
     * @return true if the job was deactivated as a result of this operation,
     *         false if not.
     */
    boolean stopJob ( String key );

    /**
     * Stops and starts a job with the given key. If no key exists, still start a new job
     * @param jobConfiguration the job to schedule
     * @return true if the job was scheduled for execution as a result of this
     *         operation, false if not.
     */
    boolean refreshJob( JobConfiguration jobConfiguration );


    /**
     * Deactivates scheduling for all jobs.
     */
    void stopAllJobs();

    /**
     * Gets all future jobs
     */
    Map<String, ScheduledFuture<?>> getAllFutureJobs();

    /**
     * Gets the status for the job with the given key.
     *
     * @param key the job key.
     * @return the job status.
     */
    JobStatus getJobStatus( String key );

    /**
     * Gets the status for the current job with the given key.
     *
     * @param key the job key.
     * @return the job status.
     */
    JobStatus getCurrentJobStatus( String key );

}
