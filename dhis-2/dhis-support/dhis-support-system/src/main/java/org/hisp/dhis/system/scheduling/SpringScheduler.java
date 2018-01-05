package org.hisp.dhis.system.scheduling;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.Job;
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobInstance;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * {@link Scheduler} implementation for use within the Spring framework.
 * @author Lars Helge Overland
 */
public class SpringScheduler
    implements Scheduler
{
    private static final Log log = LogFactory.getLog( SpringScheduler.class );

    private Map<String, ScheduledFuture<?>> futures = new HashMap<>();

    private Map<String, ListenableFuture<?>> currentTasks = new HashMap<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private MessageService messageService;

    @Autowired
    private SchedulingManager schedulingManager;

    private TaskScheduler jobScheduler;

    public void setTaskScheduler( TaskScheduler JobScheduler )
    {
        this.jobScheduler = JobScheduler;
    }

    private AsyncListenableTaskExecutor jobExecutor;

    public void setTaskExecutor( AsyncListenableTaskExecutor jobExecutor )
    {
        this.jobExecutor = jobExecutor;
    }

    // -------------------------------------------------------------------------
    // Scheduler implementation
    // -------------------------------------------------------------------------

    @Override
    public void executeJob( Runnable job )
    {
        jobExecutor.execute( job );
    }

    @Override
    public void executeJob( String JobKey, Runnable Job )
    {
        ListenableFuture<?> future = jobExecutor.submitListenable( Job );
        currentTasks.put( JobKey, future );
    }

    @Override
    public void executeJob( JobConfiguration jobConfiguration )
    {
        JobInstance jobInstance = new DefaultJobInstance();
        
        ListenableFuture<?> future = jobExecutor.submitListenable( () -> {
            try
            {
                jobInstance.execute( jobConfiguration, schedulingManager, messageService );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        } );
        currentTasks.put( jobConfiguration.getUid(), future );
    }

    @Override
    public <T> ListenableFuture<T> executeJob( Callable<T> callable )
    {
        return jobExecutor.submitListenable( callable );
    }

    @Override
    public boolean scheduleJob( JobConfiguration jobConfiguration, Job job )
    {
        if ( jobConfiguration.getUid() != null && !futures.containsKey( jobConfiguration.getUid() ) )
        {
            ScheduledFuture<?> future = jobScheduler
                .schedule( () -> {
                    try
                    {
                        job.execute( jobConfiguration );
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }, new CronTrigger( jobConfiguration
                    .getCronExpression() ) );

            futures.put( jobConfiguration.getUid(), future );

            log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and cron: " + jobConfiguration.getCronExpression() );

            return true;
        }

        return false;
    }

    @Override
    public boolean scheduleJob( JobConfiguration jobConfiguration )
    {
        JobInstance jobInstance = new DefaultJobInstance();
        
        if ( jobConfiguration.getUid() != null && !futures.containsKey( jobConfiguration.getUid() ) )
        {
            ScheduledFuture<?> future = jobScheduler
                .schedule( () -> {
                    try
                    {
                        jobInstance.execute( jobConfiguration, schedulingManager, messageService );
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }, new CronTrigger( jobConfiguration.getCronExpression() ) );

            futures.put( jobConfiguration.getUid(), future );

            log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and cron: " +
                jobConfiguration.getCronExpression() );

            return true;
        }

        return false;
    }

    @Override
    public boolean scheduleJob( Date date, JobConfiguration jobConfiguration )
    {
        JobInstance jobInstance = new DefaultJobInstance();
        
        if ( jobConfiguration.getUid() != null && !futures.containsKey( jobConfiguration.getUid() ) && date != null &&
            date.getTime() > new Date().getTime() )
        {
            ScheduledFuture<?> future = jobScheduler
                .schedule( () -> {
                    try
                    {
                        jobInstance.execute( jobConfiguration, schedulingManager, messageService );
                    }
                    catch ( Exception e )
                    {
                        e.printStackTrace();
                    }
                }, date );

            futures.put( jobConfiguration.getUid(), future );

            log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and execution time: " + date );

            return true;
        }

        return false;
    }

    @Override
    public void scheduleJobWithFixedDelay( JobConfiguration jobConfiguration, Date delay, int interval )
    {
        JobInstance jobInstance = new DefaultJobInstance();
        
        ScheduledFuture<?> future = jobScheduler.scheduleWithFixedDelay( () -> {
            try
            {
                jobInstance.execute( jobConfiguration, schedulingManager, messageService );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }, delay,interval );

        futures.put( jobConfiguration.getUid(), future );

        log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and first execution time: " + delay );
    }

    @Override
    public void scheduleJobAtFixedRate( JobConfiguration jobConfiguration, int interval )
    {
        JobInstance jobInstance = new DefaultJobInstance();
        
        ScheduledFuture<?> future = jobScheduler.scheduleAtFixedRate( () -> {
            try
            {
                jobInstance.execute( jobConfiguration, schedulingManager, messageService );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }, interval );

        futures.put( jobConfiguration.getUid(), future );

        log.info( "Scheduled job with uid: " + jobConfiguration.getUid() + " and fixed rate: " + interval );
    }

    @Override
    public boolean stopJob( String uid )
    {
        if ( uid != null )
        {
            ScheduledFuture<?> future = futures.get( uid );

            if ( future == null )
            {
                log.info( "Tried to stop job with key '" + uid + "', but was not found" );
                return true;
            }
            else
            {
                boolean result = future.cancel( true );

                futures.remove( uid );

                log.info( "Stopped job with key: " + uid + " successfully: " + result );

                return result;
            }
        }

        return false;
    }

    @Override
    public boolean refreshJob( JobConfiguration jobConfiguration )
    {
        return false;
    }

    @Override
    public void stopAllJobs()
    {
        Iterator<String> keys = futures.keySet().iterator();

        while ( keys.hasNext() )
        {
            String key = keys.next();

            ScheduledFuture<?> future = futures.get( key );

            boolean result = future != null && future.cancel( true );

            keys.remove();

            log.info( "Stopped job with key: " + key + " successfully: " + result );
        }
    }

    public Map<String, ScheduledFuture<?>> getAllFutureJobs( )
    {
        return futures;
    }

    private JobStatus getStatus( Future<?> future )
    {
        if ( future == null )
        {
            return JobStatus.SCHEDULED;
        }
        else if ( future.isCancelled() )
        {
            return JobStatus.STOPPED;
        }
        else if ( future.isDone() )
        {
            return JobStatus.COMPLETED;
        }
        else
        {
            return JobStatus.RUNNING;
        }
    }

    @Override
    public JobStatus getJobStatus( String key )
    {
        ScheduledFuture<?> future = futures.get( key );

        return getStatus( future );
    }

    @Override
    public boolean ifJobInSystemStop( String key )
    {
        return !isJobInSystem( key ) || stopJob( key );
    }

    @Override
    public boolean isJobInSystem( String key )
    {
        return futures.get( key ) != null || currentTasks.get( key ) != null;
    }

    @Override
    public JobStatus getCurrentJobStatus( String key )
    {
        ListenableFuture<?> future = currentTasks.get( key );

        return getStatus( future );
    }
}
