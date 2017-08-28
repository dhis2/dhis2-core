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

import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.system.scheduling.ScheduledTaskStatus;
import org.hisp.dhis.system.scheduling.Scheduler;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

/**
 * Cron refers to the cron expression used for scheduling. Key refers to the key
 * identifying the scheduled tasks.
 * 
 * @author Lars Helge Overland
 */
public class DefaultSchedulingManager
    implements SchedulingManager
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private Scheduler scheduler;

    public void setScheduler( Scheduler scheduler )
    {
        this.scheduler = scheduler;
    }

    private List<Job> jobs = new ArrayList<Job>( );

    private void setJobs( List<Job> jobs )
    {
        this.jobs = jobs;
    }

    // -------------------------------------------------------------------------
    // SchedulingManager implementation
    // -------------------------------------------------------------------------

    @EventListener
    public void handleContextRefresh( ContextRefreshedEvent contextRefreshedEvent )
    {
        scheduleJobs();
    }

    @Override
    public void scheduleJob( Job job)
    {
        scheduler.scheduleJob( job );
    }

    @Override
    public void scheduleJobs()
    {
        jobs.forEach( scheduler::scheduleJob );
    }
    
    @Override
    public void scheduleJobs( List<Job> jobs )
    {
        setJobs(jobs);

        scheduleJobs();
    }

    @Override
    public void stopJob( String jobKey )
    {
        scheduler.stopJob( jobKey );
    }

    @Override
    public void stopAllJobs()
    {
        scheduler.stopAllJobs();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Job> getScheduledJobs()
    {
        // May want to call scheduler to get a list of jobs put in the que which are not running. The JobStatus may not be updated at this point.
        return jobs.stream().filter( job -> job.getStatus() == JobStatus.SCHEDULED).collect( Collectors.toList() ); //(ListMap<String, String>) systemSettingManager.getSystemSetting( SettingKey.SCHEDULED_TASKS, new ListMap<String, String>() );
    }

    public List<Job> getAllFutureJobs()
    {
        Map<String, ScheduledFuture<?>> futureMap = scheduler.getAllFutureJobs();

        List<Job> futureJobs = new ArrayList<>();
        futureMap.forEach( (k, v) -> futureJobs.add( getJob( k ) ) );

        Collections.sort( futureJobs );

        return futureJobs;
    }

    private Job getJob( String taskKey )
    {
        return jobs.stream().filter( job -> Objects.equals( job.getKey(), taskKey ) ).findFirst().orElse( null );
    }

    @Override
    public String getCronForJob( final String taskKey )
    {
        return getJob(taskKey).getCronExpression();
    }
    
    @Override
    public JobStatus getJobStatus( String taskKey )
    {
        return getJob( taskKey ).getStatus();
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    /**
     * Returns a ScheduledTasks object for the given cron expression. The
     * ScheduledTasks object contains a list of tasks.
     */
    private ScheduledJobs getScheduledTasksForCron( String cron, ListMap<String, String> cronKeyMap )
    {
        ScheduledJobs scheduledJobs = new ScheduledJobs();

        scheduledJobs.addJobs( jobs.stream().filter( job -> Objects.equals( job.getCronExpression(), cron ) ).map(
            Job::getRunnable ).collect( Collectors.toList()) );
        
        return scheduledJobs;
    }

    @Override
    public void executeJob( String jobKey )
    {
        Runnable job = getJob( jobKey ).getRunnable();

        if ( job != null && !isJobInProgress( jobKey ) )
        {
            scheduler.executeJob( jobKey, job );
        }
    }

    @Override
    public boolean isJobInProgress(String jobKey)
    {
        return ScheduledTaskStatus.RUNNING == scheduler.getCurrentJobStatus( jobKey );
    }

}
