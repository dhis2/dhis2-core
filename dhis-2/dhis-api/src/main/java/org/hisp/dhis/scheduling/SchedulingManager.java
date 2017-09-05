package org.hisp.dhis.scheduling;

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

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * @author Henning Håkonsen
 */
public interface SchedulingManager
{
    /**
     * Schedules a job with the given job configuration.
     *
     * @param jobConfiguration the job to schedule.
     */
    void scheduleJob( JobConfiguration jobConfiguration );

    /**
     * Stops one job.
     */
    void stopJob( String jobKey );

    /**
     * Stops all jobs.
     */
    void stopAllJobs( );

    /**
     * Refreshes the given job
     */
    void refreshJob( JobConfiguration jobConfiguration );

    /**
     * Execute the job.
     *
     * @param jobConfiguration The configuration of the job to be executed
     */
    void executeJob( JobConfiguration jobConfiguration );

    /**
     * Resolve the cron expression mapped for the given task key, or null if none.
     *
     * @param jobKey the key of the job, not null.
     * @return the cron for the job or null.
     */
    String getCronForJob( final String jobKey );

    /**
     * Returns a list of all scheduled jobs sorted based on cron expression and the current time.
     * @return list of jobs
     */
    Map<String, ScheduledFuture<?>> getAllFutureJobs();
    
    /**
     * Gets the job status.
     */
    JobStatus getJobStatus( String jobKey );

    /**
     *
     * Returns the status of the currently executing job.
     */
    boolean isJobInProgress(String jobKey);


}
