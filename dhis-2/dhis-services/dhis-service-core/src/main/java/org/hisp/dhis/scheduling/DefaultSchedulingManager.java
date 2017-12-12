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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static org.hisp.dhis.scheduling.JobStatus.DISABLED;

/**
 * Cron refers to the cron expression used for scheduling. Key refers to the key
 * identifying the scheduled jobs.
 *
 * @author Henning Håkonsen
 */
public class DefaultSchedulingManager
    implements SchedulingManager
{
    @Autowired
    private JobConfigurationService jobConfigurationService;

    @Autowired
    private List<Job> jobs;

    private Map<JobType, Job> jobMap = new HashMap<>();

    private static final Log log = LogFactory.getLog( DefaultSchedulingManager.class );

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private Scheduler scheduler;

    @Autowired
    public void setScheduler( Scheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void init()
    {
        jobs.forEach( job -> {
            if ( job == null )
            {
                log.fatal( "Scheduling manager tried to add job, but it was null" );
            }
            else
            {
                jobMap.put( job.getJobType(), job );
            }
        } );
    }

    // -------------------------------------------------------------------------
    // Queue
    // -------------------------------------------------------------------------

    private List<JobConfiguration> runningJobConfigurations = new ArrayList<>();

    public boolean isJobConfigurationRunning( JobConfiguration jobConfiguration )
    {
        return !jobConfiguration.isContinuousExecution() && runningJobConfigurations.stream().anyMatch(
            jobConfig -> jobConfig.getJobType().equals( jobConfiguration.getJobType() ) &&
                !jobConfig.isContinuousExecution() );
    }

    public void jobConfigurationStarted( JobConfiguration jobConfiguration )
    {
        runningJobConfigurations.add( jobConfiguration );
        jobConfigurationService.updateJobConfiguration( jobConfiguration );
    }

    public void jobConfigurationFinished( JobConfiguration jobConfiguration )
    {
        runningJobConfigurations.remove( jobConfiguration );

        JobConfiguration tempJobConfiguration = jobConfigurationService
            .getJobConfigurationByUid( jobConfiguration.getUid() );

        if ( tempJobConfiguration != null )
        {
            if ( tempJobConfiguration.getJobStatus() == DISABLED )
            {
                jobConfiguration.setJobStatus( DISABLED );
                jobConfiguration.setEnabled( false );
            }

            jobConfigurationService.updateJobConfiguration( jobConfiguration );
        }
    }

    // -------------------------------------------------------------------------
    // SchedulingManager implementation
    // -------------------------------------------------------------------------

    @Override
    public void scheduleJob( JobConfiguration jobConfiguration )
    {
        if ( !scheduler.isJobInSystem( jobConfiguration.getUid() ) )
        {
            scheduler.scheduleJob( jobConfiguration );
        }
    }

    @Override
    public void scheduleJob( Date date, JobConfiguration jobConfiguration )
    {
        if ( !scheduler.isJobInSystem( jobConfiguration.getUid() ) )
        {
            scheduler.scheduleJob( date, jobConfiguration );
        }
    }

    @Override
    public void scheduleJobs( List<JobConfiguration> jobConfigurations )
    {
        jobConfigurations.forEach( this::scheduleJob );
    }

    @Override
    public void scheduleJobWithFixedDelay( JobConfiguration jobConfiguration, Date delay, int interval )
    {
        if ( !scheduler.isJobInSystem( jobConfiguration.getUid() ) )
        {
            scheduler.scheduleJobWithFixedDelay( jobConfiguration, delay, interval );
        }
    }

    @Override
    public void scheduleJobAtFixedRate( JobConfiguration jobConfiguration, int interval )
    {
        if ( !scheduler.isJobInSystem( jobConfiguration.getUid() ) )
        {
            scheduler.scheduleJobAtFixedRate( jobConfiguration, interval );
        }
    }

    @Override
    public void stopJob( JobConfiguration jobConfiguration )
    {
        jobConfiguration.setLastExecutedStatus( JobStatus.STOPPED );
        jobConfigurationService.updateJobConfiguration( jobConfiguration );

        scheduler.stopJob( jobConfiguration.getUid() );
    }

    @Override
    public void stopAllJobs()
    {
        scheduler.stopAllJobs();
    }

    @Override
    public void refreshJob( JobConfiguration jobConfiguration )
    {
        scheduler.stopJob( jobConfiguration.getUid() );
        scheduleJob( jobConfiguration );
    }

    @Override
    public void executeJob( JobConfiguration jobConfiguration )
    {
        if ( jobConfiguration != null && !isJobInProgress( jobConfiguration.getUid() ) )
        {
            scheduler.executeJob( jobConfiguration );
        }
    }

    @Override
    public void executeJob( Runnable job )
    {
        scheduler.executeJob( job );
    }

    @Override
    public String getCronForJob( String jobKey )
    {
        return null;
    }

    public Map<String, ScheduledFuture<?>> getAllFutureJobs()
    {
        return scheduler.getAllFutureJobs();
    }

    @Override
    public JobStatus getJobStatus( String jobKey )
    {
        return scheduler.getJobStatus( jobKey );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    @Override
    public boolean isJobInProgress( String jobKey )
    {
        return JobStatus.RUNNING == scheduler.getCurrentJobStatus( jobKey );
    }

    public Job getJob( JobType jobType )
    {
        return jobMap.get( jobType );
    }
}
