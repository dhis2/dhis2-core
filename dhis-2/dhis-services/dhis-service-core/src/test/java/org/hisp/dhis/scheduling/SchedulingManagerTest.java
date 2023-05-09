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
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.cache.TestCache;
import org.hisp.dhis.common.AsyncTaskExecutor;
import org.hisp.dhis.eventhook.EventHookPublisher;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.parameters.AnalyticsJobParameters;
import org.hisp.dhis.scheduling.parameters.ContinuousAnalyticsJobParameters;
import org.hisp.dhis.system.notification.Notifier;
import org.hisp.dhis.user.AuthenticationService;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
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
 * This does not run any actual {@link Job}s but checks that the ceremony around
 * running them works.
 *
 * @author Jan Bernitt
 */
class SchedulingManagerTest
{
    private final TaskScheduler taskScheduler = mock( TaskScheduler.class );

    private final JobConfigurationService jobConfigurationService = mock( JobConfigurationService.class );

    private final ApplicationContext applicationContext = mock( ApplicationContext.class );

    private DefaultSchedulingManager schedulingManager;

    private final Map<String, Job> jobBeans = new LinkedHashMap<>();

    @BeforeEach
    void setUp()
    {
        when( applicationContext.getBeansOfType( Job.class ) ).thenReturn( jobBeans );

        CacheProvider cacheProvider = mock( CacheProvider.class );
        when( cacheProvider.createJobCancelRequestedCache() ).thenReturn( new TestCache<>() );
        when( cacheProvider.createRunningJobsInfoCache() ).thenReturn( new TestCache<>() );
        when( cacheProvider.createCompletedJobsInfoCache() ).thenReturn( new TestCache<>() );

        schedulingManager = new DefaultSchedulingManager( new SchedulingManagerSupport(
            mock( UserService.class ), mock( AuthenticationService.class ), new DefaultJobService( applicationContext ),
            jobConfigurationService, mock( MessageService.class ), mock( LeaderManager.class ), mock( Notifier.class ),
            mock( EventHookPublisher.class ), cacheProvider, mock( AsyncTaskExecutor.class ), taskScheduler ) );
    }

    @TestFactory
    Stream<DynamicTest> testScheduleRunCronJob()
    {
        JobConfiguration conf = createCronJobConfiguration();
        when( jobConfigurationService.getJobConfigurationByUid( conf.getUid() ) ).thenReturn( conf );
        ArgumentCaptor<Runnable> cronTask = ArgumentCaptor.forClass( Runnable.class );
        when( taskScheduler.schedule( cronTask.capture(), any( Trigger.class ) ) ).thenReturn( new MockFuture<>() );
        schedulingManager.schedule( conf );
        return createTestCases( conf, this::createCronJobConfiguration, cronTask.getValue() );
    }

    @TestFactory
    Stream<DynamicTest> testScheduleRunFixedDelayJob()
    {
        JobConfiguration conf = createFixedDelayJobConfiguration();
        when( jobConfigurationService.getJobConfigurationByUid( conf.getUid() ) ).thenReturn( conf );
        ArgumentCaptor<Runnable> delayTask = ArgumentCaptor.forClass( Runnable.class );
        when( taskScheduler.scheduleWithFixedDelay( delayTask.capture(), any( Instant.class ), any( Duration.class ) ) )
            .thenReturn( new MockFuture<>() );
        schedulingManager.schedule( conf );
        return createTestCases( conf, this::createFixedDelayJobConfiguration, delayTask.getValue() );
    }

    @TestFactory
    Stream<DynamicTest> testScheduleRunWithStartTimeJob()
    {
        JobConfiguration conf = createStartTimeJobConfiguration();
        when( jobConfigurationService.getJobConfigurationByUid( conf.getUid() ) ).thenReturn( conf );
        ArgumentCaptor<Runnable> startTimeTask = ArgumentCaptor.forClass( Runnable.class );
        when( taskScheduler.schedule( startTimeTask.capture(), any( Date.class ) ) ).thenReturn( new MockFuture<>() );
        schedulingManager.scheduleWithStartTime( conf, new Date() );
        return createTestCases( conf, this::createStartTimeJobConfiguration, startTimeTask.getValue() );
    }

    @TestFactory
    Stream<DynamicTest> testScheduleRunWithQueue()
    {
        JobConfiguration conf = createCronJobConfiguration();
        conf.setQueueName( "test" );
        conf.setQueuePosition( 0 );
        List<JobConfiguration> queue = List.of( conf,
            createQueueJobConfiguration( JobType.SEND_SCHEDULED_MESSAGE, 1 ),
            createQueueJobConfiguration( JobType.PROGRAM_NOTIFICATIONS, 2 ) );
        when( jobConfigurationService.getAllJobConfigurations() ).thenReturn( queue );
        queue.forEach( config -> when( jobConfigurationService.getJobConfigurationByUid( config.getUid() ) )
            .thenReturn( config ) );
        ArgumentCaptor<Runnable> queueStart = ArgumentCaptor.forClass( Runnable.class );
        when( taskScheduler.schedule( queueStart.capture(), any( Trigger.class ) ) ).thenReturn( new MockFuture<>() );
        schedulingManager.schedule( conf );
        return createTestCases( conf, this::createCronJobConfiguration, queueStart.getValue() );
    }

    private JobConfiguration createQueueJobConfiguration( JobType type, int position )
    {
        JobConfiguration conf = new JobConfiguration();
        conf.setName( "queue" + position );
        conf.setUid( generateUid() );
        conf.setJobType( type );
        conf.setCronExpression( null );
        conf.setQueueName( "test" );
        conf.setQueuePosition( position );
        addJob( conf );
        return conf;
    }

    private JobConfiguration createCronJobConfiguration()
    {
        JobConfiguration conf = new JobConfiguration( "cron", JobType.ANALYTICS_TABLE,
            getDailyCronExpression( 0, 0 ), new AnalyticsJobParameters() );
        // only jobs with ID get scheduled
        conf.setUid( generateUid() );
        addJob( conf );
        return conf;
    }

    private JobConfiguration createFixedDelayJobConfiguration()
    {
        JobConfiguration conf = new JobConfiguration( "delay", JobType.CONTINUOUS_ANALYTICS_TABLE,
            getDailyCronExpression( 0, 0 ), new ContinuousAnalyticsJobParameters() );
        // only jobs with ID get scheduled
        conf.setUid( generateUid() );
        conf.setDelay( 1 );
        addJob( conf );
        return conf;
    }

    private JobConfiguration createStartTimeJobConfiguration()
    {
        JobConfiguration conf = new JobConfiguration( "CLUSTER_LEADER_RENEWAL",
            JobType.LEADER_RENEWAL, null, true );
        addJob( conf );
        return conf;
    }

    private void addJob( JobConfiguration conf )
    {
        Job job = mock( Job.class );
        when( job.getJobType() ).thenReturn( conf.getJobType() );
        jobBeans.put( jobBeans.size() + "", job );
    }

    private Stream<DynamicTest> createTestCases( JobConfiguration configuration, Supplier<JobConfiguration> copy,
        Runnable task )
    {
        assertNotNull( task, "job was not scheduled" );
        return Stream.of(
            //OBS! the order here is important because these are not isolated from each other
            dynamicTest( "assertScheduledJobCompletesSuccessful",
                () -> assertScheduledJobCompletesSuccessful( configuration, task ) ),
            dynamicTest( "assertJobDoesNotStartWhenAlreadyRunning",
                () -> assertJobDoesNotStartWhenAlreadyRunning( configuration, task ) ),
            dynamicTest( "assertScheduledJobCanBeCancelled",
                () -> assertScheduledJobCanBeCancelled( configuration, task ) ),
            dynamicTest( "assertScheduledJobFailsGracefulWhenInterrupted",
                () -> assertScheduledJobFailsGracefulWhenInterrupted( configuration, task ) ),
            dynamicTest( "assertScheduledJobFailsGraceful",
                () -> assertScheduledJobFailsGraceful( configuration, task ) ),
            dynamicTest( "assertStopRemovesJobFromSchedule",
                () -> assertStopRemovesJobFromSchedule( configuration, task ) ),
            dynamicTest( "assertScheduledJobStaysDisabled",
                () -> assertScheduledJobStaysDisabled( configuration, copy, task ) ) );
    }

    private void assertScheduledJobCompletesSuccessful( JobConfiguration conf, Runnable task )
    {
        assertTrue( schedulingManager.isScheduled( conf ), "job is scheduled before it runs" );

        setUpJobExecute( this::assertIsRunning );
        // synchronously
        task.run();
        verify( applicationContext, atLeastOnce() ).getBeansOfType( any() );
        if ( !conf.isInMemoryJob() )
        {
            assertEquals( JobStatus.COMPLETED, conf.getLastExecutedStatus() );
            assertNotNull( conf.getLastExecuted(), "job did not complete" );
        }
        assertFalse( schedulingManager.isRunning( conf.getJobType() ),
            "job is still considered running when it is actually finished" );
        assertTrue( schedulingManager.isScheduled( conf ), "job is still scheduled after it ran" );
        // all jobs involved in the test should have been executed once
        jobBeans.values().forEach( job -> verify( job, times( 1 ) ).execute( any(), any() ) );
    }

    private void assertIsRunning( JobConfiguration conf )
    {
        assertTrue( schedulingManager.isRunning( conf.getJobType() ) );
        assertEquals( JobStatus.RUNNING, conf.getJobStatus() );
    }

    private void assertJobDoesNotStartWhenAlreadyRunning( JobConfiguration configuration, Runnable task )
    {
        setUpJobExecute( conf -> assertFalse( schedulingManager.executeNow( configuration ) ) );
        // synchronously
        task.run();

        // but now it can run as the scheduled one is not running
        assertTrue( schedulingManager.executeNow( configuration ) );
    }

    private void assertScheduledJobCanBeCancelled( JobConfiguration configuration, Runnable task )
    {
        // once running the job cancels itself
        setUpJobExecute( conf -> schedulingManager.cancel( conf.getJobType() ) );
        // synchronously
        task.run();

        assertEquals( JobStatus.STOPPED, configuration.getLastExecutedStatus() );
        assertFalse( schedulingManager.isRunning( configuration.getJobType() ) );
    }

    private void assertStopRemovesJobFromSchedule( JobConfiguration configuration, Runnable task )
    {
        // once running the job stops itself
        setUpJobExecute( schedulingManager::stop );
        // synchronously
        task.run();

        // stop does not interrupt or abort a running job
        assertEquals( JobStatus.COMPLETED, configuration.getLastExecutedStatus() );
        // but removes it from being scheduled anymore
        assertFalse( schedulingManager.isScheduled( configuration ) );

        // also it should be valid to call stop again
        schedulingManager.stop( configuration );
    }

    private void assertScheduledJobFailsGracefulWhenInterrupted( JobConfiguration configuration, Runnable task )
    {
        setUpJobExecute( conf -> Thread.currentThread().interrupt() );
        // synchronously
        task.run();

        assertEquals( JobStatus.FAILED, configuration.getLastExecutedStatus() );
        assertFalse( schedulingManager.isRunning( configuration.getJobType() ) );
        assertTrue( schedulingManager.isScheduled( configuration ) );
    }

    private void assertScheduledJobFailsGraceful( JobConfiguration configuration, Runnable task )
    {
        setUpJobExecute( conf -> {
            throw new IllegalStateException( "Something goes wrong while doing the work..." );
        } );
        // synchronously
        task.run();

        assertEquals( JobStatus.FAILED, configuration.getLastExecutedStatus() );
        assertFalse( schedulingManager.isRunning( configuration.getJobType() ) );
        assertTrue( schedulingManager.isScheduled( configuration ) );
    }

    private void assertScheduledJobStaysDisabled( JobConfiguration configuration, Supplier<JobConfiguration> copy,
        Runnable task )
    {
        if ( !configuration.isInMemoryJob() )
        {
            setUpJobExecute( conf -> {
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
        Job tested = jobBeans.values().iterator().next();
        doAnswer( invocation -> {
            execute.accept( invocation.getArgument( 0, JobConfiguration.class ) );
            if ( Thread.currentThread().isInterrupted() )
                throw new InterruptedException();
            return null;
        } ).when( tested ).execute( any( JobConfiguration.class ), any( JobProgress.class ) );
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
