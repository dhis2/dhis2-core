/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.scheduling.JobStatus.DISABLED;
import static org.hisp.dhis.util.DateUtils.getMediumDateString;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BiFunction;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.AsyncListenableTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Cron refers to the cron expression used for scheduling. Key refers to the key
 * identifying the scheduled jobs.
 *
 * @author Henning Håkonsen
 */
@Slf4j
@Service( "org.hisp.dhis.scheduling.SchedulingManager" )
public class DefaultSchedulingManager
    implements SchedulingManager
{
    private static final int DEFAULT_INITIAL_DELAY_S = 10;

    private final Map<String, Future<?>> futures = new ConcurrentHashMap<>();

    private final Map<String, Future<?>> currentTasks = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final JobConfigurationService jobConfigurationService;

    private final MessageService messageService;

    private final LeaderManager leaderManager;

    private final TaskScheduler jobScheduler;

    private final AsyncListenableTaskExecutor jobExecutor;

    private final ApplicationContext applicationContext;

    public DefaultSchedulingManager( JobConfigurationService jobConfigurationService, MessageService messageService,
        LeaderManager leaderManager, @Qualifier( "taskScheduler" ) TaskScheduler jobScheduler,
        @Qualifier( "taskScheduler" ) AsyncListenableTaskExecutor jobExecutor, ApplicationContext applicationContext )
    {
        checkNotNull( jobConfigurationService );
        checkNotNull( messageService );
        checkNotNull( leaderManager );
        checkNotNull( jobScheduler );
        checkNotNull( jobExecutor );
        checkNotNull( applicationContext );

        this.jobConfigurationService = jobConfigurationService;
        this.messageService = messageService;
        this.leaderManager = leaderManager;
        this.jobScheduler = jobScheduler;
        this.jobExecutor = jobExecutor;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void init()
    {
        leaderManager.setSchedulingManager( this );
    }

    // -------------------------------------------------------------------------
    // Queue
    // -------------------------------------------------------------------------

    /**
     * List of currently running jobs.
     */
    private Set<JobType> runningJobTypes = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isJobConfigurationRunning( JobType type )
    {
        return runningJobTypes.contains( type );
    }

    @Override
    public void jobConfigurationStarted( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isInMemoryJob() )
        {
            runningJobTypes.add( jobConfiguration.getJobType() );
            jobConfigurationService.updateJobConfiguration( jobConfiguration );
        }
    }

    @Override
    public void jobConfigurationFinished( JobConfiguration jobConfiguration )
    {
        runningJobTypes.remove( jobConfiguration.getJobType() );

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
        scheduleJob( jobConfiguration, this::scheduleCronOrDelayJob );
    }

    private ScheduledFuture<?> scheduleCronOrDelayJob( JobConfiguration jobConfiguration, JobInstance jobInstance )
    {
        log.info( String.format( "Scheduling job: %s", jobConfiguration ) );

        ScheduledFuture<?> future = jobConfiguration.getJobType().getSchedulingType() == SchedulingType.CRON
            ? createCronJob( jobConfiguration, jobInstance )
            : createDelayJob( jobConfiguration, jobInstance );

        log.info( String.format( "Scheduled job: %s", jobConfiguration ) );
        return future;
    }

    private ScheduledFuture<?> createDelayJob( JobConfiguration jobConfiguration, JobInstance jobInstance )
    {
        return jobScheduler.scheduleWithFixedDelay( () -> jobInstance.execute( jobConfiguration ),
            Instant.now().plusSeconds( DEFAULT_INITIAL_DELAY_S ),
            Duration.of( jobConfiguration.getDelay(), ChronoUnit.SECONDS ) );
    }

    private ScheduledFuture<?> createCronJob( JobConfiguration jobConfiguration, JobInstance jobInstance )
    {
        return jobScheduler.schedule( () -> jobInstance.execute( jobConfiguration ),
            new CronTrigger( jobConfiguration.getCronExpression() ) );
    }

    @Override
    public void scheduleJobWithStartTime( JobConfiguration jobConfiguration, Date startTime )
    {
        scheduleJob( jobConfiguration,
            ( configuration, jobInstance ) -> scheduleJobWithStartTime( configuration, jobInstance, startTime ) );
    }

    private ScheduledFuture<?> scheduleJobWithStartTime( JobConfiguration jobConfiguration, JobInstance jobInstance,
        Date startTime )
    {
        ScheduledFuture<?> future = jobScheduler.schedule( () -> jobInstance.execute( jobConfiguration ), startTime );

        log.info( String.format( "Scheduled job: %s with start time: %s", jobConfiguration,
            getMediumDateString( startTime ) ) );
        return future;
    }

    private void scheduleJob( JobConfiguration jobConfiguration,
        BiFunction<JobConfiguration, JobInstance, ScheduledFuture<?>> task )
    {
        String uid = jobConfiguration.getUid();
        if ( ifJobInSystemStop( uid ) && uid != null )
        {
            futures.computeIfAbsent( uid,
                key -> task.apply( jobConfiguration, new DefaultJobInstance( this, messageService, leaderManager ) ) );
        }
    }

    @Override
    public void stopJob( JobConfiguration jobConfiguration )
    {
        if ( isJobInSystem( jobConfiguration.getUid() ) )
        {
            jobConfiguration.setLastExecutedStatus( JobStatus.STOPPED );
            jobConfigurationService.updateJobConfiguration( jobConfiguration );

            internalStopJob( jobConfiguration.getUid() );
        }
    }

    @Override
    public boolean executeJob( JobConfiguration jobConfiguration )
    {
        if ( jobConfiguration != null && !isJobConfigurationRunning( jobConfiguration.getJobType() ) )
        {
            internalExecuteJobConfiguration( jobConfiguration );
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    public void executeJob( Runnable job )
    {
        jobExecutor.execute( job );
    }

    @Override
    public Map<String, Future<?>> getAllFutureJobs()
    {
        return futures;
    }

    @Override
    public Job getJob( JobType jobType )
    {
        return (Job) applicationContext.getBean( jobType.getKey() );
    }

    @Override
    public <T> ListenableFuture<T> executeJob( Callable<T> callable )
    {
        return jobExecutor.submitListenable( callable );
    }

    // -------------------------------------------------------------------------
    // Supportive methods
    // -------------------------------------------------------------------------

    private void internalExecuteJobConfiguration( JobConfiguration jobConfiguration )
    {
        JobInstance jobInstance = new DefaultJobInstance( this, messageService, leaderManager );

        ListenableFuture<?> future = jobExecutor.submitListenable( () -> jobInstance.execute( jobConfiguration ) );

        String uid = jobConfiguration.getUid();
        currentTasks.put( uid, future );
        future.completable().whenComplete( ( res, ex ) -> currentTasks.remove( uid ) );

        log.info( String.format( "Scheduler initiated execution of job: %s", jobConfiguration ) );
    }

    private boolean internalStopJob( String uid )
    {
        if ( uid != null )
        {
            Future<?> future = futures.get( uid );

            if ( future == null )
            {
                log.info( String.format( "Tried to stop job but job was not found with key: '%s'", uid ) );

                return true;
            }
            else
            {
                boolean result = future.cancel( true );

                futures.remove( uid );

                log.info( String.format( "Stopped job with key: '%s' with successful result: '%b'", uid, result ) );

                return result;
            }
        }

        return false;
    }

    private boolean ifJobInSystemStop( String jobKey )
    {
        return !isJobInSystem( jobKey ) || internalStopJob( jobKey );
    }

    private boolean isJobInSystem( String jobKey )
    {
        return jobKey != null && (futures.get( jobKey ) != null || currentTasks.get( jobKey ) != null);
    }
}
