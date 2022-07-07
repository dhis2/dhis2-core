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

import static org.hisp.dhis.common.CodeGenerator.generateUid;
import static org.hisp.dhis.commons.util.CronUtils.getDailyCronExpression;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.TestCache;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.system.notification.Notifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;

/**
 * Since most test setups run with a mock {@link SchedulingManager}
 * implementation this test focuses on testing the
 * {@link DefaultSchedulingManager} implementation.
 *
 * This does not run any actual {@link Job} but checks that the ceremony around
 * running them works.
 *
 * @author Jan Bernitt
 */
class SchedulingManagerTest
{
    private final TaskScheduler taskScheduler = mock( TaskScheduler.class );

    private final JobConfigurationService jobConfigurationService = mock( JobConfigurationService.class );

    private final ApplicationContext applicationContext = mock( ApplicationContext.class );

    private final Job job = mock( Job.class );

    private DefaultSchedulingManager schedulingManager;

    @BeforeEach
    void setUp()
    {
        when( applicationContext.getBeansOfType( any() ) ).thenReturn( Collections.singletonMap( "test", job ) );

        CacheProvider cacheProvider = mock( CacheProvider.class );
        when( cacheProvider.createJobCancelRequestedCache() ).thenReturn( new TestCache<>() );
        when( cacheProvider.createRunningJobsInfoCache() ).thenReturn( new TestCache<>() );
        when( cacheProvider.createCompletedJobsInfoCache() ).thenReturn( new TestCache<>() );

        schedulingManager = new DefaultSchedulingManager( new DefaultJobService( applicationContext ),
            jobConfigurationService, mock( MessageService.class ), mock( Notifier.class ),
            mock( LeaderManager.class ), taskScheduler, mock( AsyncTaskExecutor.class ), cacheProvider );
    }

    @Test
    void testScheduleRunCronJob()
    {
        JobConfiguration configuration = createCronJonConfiguration();
        when( job.getJobType() ).thenReturn( configuration.getJobType() );
        when( jobConfigurationService.getJobConfigurationByUid( configuration.getUid() ) ).thenReturn( configuration );
        ArgumentCaptor<Runnable> cronTask = ArgumentCaptor.forClass( Runnable.class );
        when( taskScheduler.schedule( cronTask.capture(), any( Trigger.class ) ) ).thenReturn( new MockFuture<>() );
        schedulingManager.schedule( configuration );
        assertScheduledJob( configuration, SchedulingManagerTest::createCronJonConfiguration, cronTask.getValue() );
    }

    @Test
    void testScheduleRunFixedDelayJob()
    {
        JobConfiguration configuration = createFixedDelayJobConfiguration();
        when( job.getJobType() ).thenReturn( configuration.getJobType() );
        when( jobConfigurationService.getJobConfigurationByUid( configuration.getUid() ) ).thenReturn( configuration );
        ArgumentCaptor<Runnable> delayTask = ArgumentCaptor.forClass( Runnable.class );
        when( taskScheduler.scheduleWithFixedDelay( delayTask.capture(), any( Instant.class ), any( Duration.class ) ) )
            .thenReturn( new MockFuture<>() );
        schedulingManager.schedule( configuration );
        assertScheduledJob( configuration, SchedulingManagerTest::createFixedDelayJobConfiguration,
            delayTask.getValue() );
    }

    @Test
    void testScheduleRunWithStartTimeJob()
    {
        JobConfiguration configuration = createStartTimeJobConfiguration();
        when( job.getJobType() ).thenReturn( configuration.getJobType() );
        when( jobConfigurationService.getJobConfigurationByUid( configuration.getUid() ) ).thenReturn( configuration );
        ArgumentCaptor<Runnable> startTimeTask = ArgumentCaptor.forClass( Runnable.class );
        when( taskScheduler.schedule( startTimeTask.capture(), any( Date.class ) ) ).thenReturn( new MockFuture<>() );
        schedulingManager.scheduleWithStartTime( configuration, new Date() );
        assertScheduledJob( configuration, SchedulingManagerTest::createStartTimeJobConfiguration,
            startTimeTask.getValue() );
    }

    private static JobConfiguration createCronJonConfiguration()
    {
        JobConfiguration configuration = new JobConfiguration( "cron", JobType.ANALYTICS_TABLE,
            getDailyCronExpression( 0, 0 ), new AnalyticsJobParameters() );
        // only jobs with ID get scheduled
        configuration.setUid( generateUid() );
        return configuration;
    }

    private static JobConfiguration createFixedDelayJobConfiguration()
    {
        JobConfiguration configuration = new JobConfiguration( "delay", JobType.CONTINUOUS_ANALYTICS_TABLE,
            getDailyCronExpression( 0, 0 ), new ContinuousAnalyticsJobParameters() );
        // only jobs with ID get scheduled
        configuration.setUid( generateUid() );
        configuration.setDelay( 1 );
        return configuration;
    }

    private static JobConfiguration createStartTimeJobConfiguration()
    {
        return new JobConfiguration( "CLUSTER_LEADER_RENEWAL", JobType.LEADER_RENEWAL, null, true );
    }

    private void assertScheduledJob( JobConfiguration configuration, Supplier<JobConfiguration> copy, Runnable task )
    {
        assertNotNull( task, "job was not scheduled" );
        assertScheduledJobCompletes( configuration, task );
        assertScheduledJobDoesNotStartWhenAlreadyRunning( configuration, task );
        assertScheduledJobStops( configuration, task );
        assertScheduledJobStopWhenInterrupted( configuration, task );
        assertScheduledJobFailsGraceful( configuration, task );
        assertScheduledJobStaysDisabled( configuration, copy, task );
    }

    private void assertScheduledJobCompletes( JobConfiguration configuration, Runnable task )
    {
        setUpJobExecute( this::assertIsRunning );
        // synchronously
        task.run();
        verify( applicationContext, atLeastOnce() ).getBeansOfType( any() );
        if ( !configuration.isInMemoryJob() )
        {
            assertEquals( JobStatus.COMPLETED, configuration.getLastExecutedStatus() );
            assertNotNull( configuration.getLastExecuted(), "job did not complete" );
        }
        assertFalse( schedulingManager.isRunning( configuration.getJobType() ),
            "job is still considered running when it is actually finished" );
    }

    private void assertIsRunning( JobConfiguration configuration )
    {
        assertTrue( schedulingManager.isRunning( configuration.getJobType() ) );
        assertEquals( JobStatus.RUNNING, configuration.getJobStatus() );
    }

    private void assertScheduledJobDoesNotStartWhenAlreadyRunning( JobConfiguration configuration, Runnable task )
    {
        setUpJobExecute( jobConfiguration -> assertFalse( schedulingManager.executeNow( configuration ) ) );
        // synchronously
        task.run();
        assertTrue( schedulingManager.executeNow( configuration ) );
    }

    private void assertScheduledJobStops( JobConfiguration configuration, Runnable task )
    {
        // once running the job stops itself
        setUpJobExecute( jobConfiguration -> schedulingManager.stop( jobConfiguration ) );
        // synchronously
        task.run();
        assertEquals( JobStatus.STOPPED, configuration.getLastExecutedStatus() );
        assertFalse( schedulingManager.isRunning( configuration.getJobType() ) );
    }

    private void assertScheduledJobStopWhenInterrupted( JobConfiguration configuration, Runnable task )
    {
        setUpJobExecute( jobConfiguration -> Thread.currentThread().interrupt() );
        // synchronously
        task.run();
        assertEquals( JobStatus.STOPPED, configuration.getLastExecutedStatus() );
        assertFalse( schedulingManager.isRunning( configuration.getJobType() ) );
    }

    private void assertScheduledJobFailsGraceful( JobConfiguration configuration, Runnable task )
    {
        setUpJobExecute( jobConfiguration -> {
            throw new IllegalStateException( "Something goes wrong while doing the work..." );
        } );
        // synchronously
        task.run();
        assertEquals( JobStatus.FAILED, configuration.getLastExecutedStatus() );
        assertFalse( schedulingManager.isRunning( configuration.getJobType() ) );
    }

    private void assertScheduledJobStaysDisabled( JobConfiguration configuration, Supplier<JobConfiguration> copy,
        Runnable task )
    {
        if ( !configuration.isInMemoryJob() )
        {
            setUpJobExecute( jobConfiguration -> {
                // pretend the job has been disabled in database
                JobConfiguration persistent = copy.get();
                persistent.setJobStatus( JobStatus.DISABLED );
                when( jobConfigurationService.getJobConfigurationByUid( anyString() ) ).thenReturn( persistent );
            } );
            // synchronously
            task.run();
            assertEquals( JobStatus.DISABLED, configuration.getJobStatus() );
            assertFalse( configuration.isEnabled() );
        }
    }

    private void setUpJobExecute( Consumer<JobConfiguration> execute )
    {
        doAnswer( invocation -> {
            execute.accept( invocation.getArgument( 0, JobConfiguration.class ) );
            return null;
        } ).when( job ).execute( any( JobConfiguration.class ), any( JobProgress.class ) );
    }

    /**
     * Because {@link SchedulingManager} internally works with
     * {@link ScheduledFuture} and {@link ListenableFuture} we cannot use a
     * stock {@link CompletableFuture} to mock results.
     */
    static final class MockFuture<T> extends CompletableFuture<T> implements ScheduledFuture<T>, ListenableFuture<T>
    {

        @Override
        public long getDelay( TimeUnit timeUnit )
        {
            return 0;
        }

        @Override
        public int compareTo( Delayed delayed )
        {
            return 0;
        }

        @Override
        public void addCallback( ListenableFutureCallback<? super T> listenableFutureCallback )
        {
            addCallback( listenableFutureCallback::onSuccess, listenableFutureCallback::onFailure );
        }

        @Override
        public void addCallback( SuccessCallback<? super T> successCallback, FailureCallback failureCallback )
        {
            whenComplete( ( success, error ) -> {
                if ( error != null )
                {
                    failureCallback.onFailure( error );
                }
                else
                {
                    successCallback.onSuccess( success );
                }
            } );
        }
    }
}
