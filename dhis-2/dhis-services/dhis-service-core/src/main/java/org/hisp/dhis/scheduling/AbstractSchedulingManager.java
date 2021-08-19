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

import static org.hisp.dhis.scheduling.JobStatus.COMPLETED;
import static org.hisp.dhis.scheduling.JobStatus.DISABLED;
import static org.hisp.dhis.scheduling.JobStatus.RUNNING;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.system.util.Clock;

/**
 * Base for synchronous or asynchronous {@link SchedulingManager} implementation
 * that keeps track of running/executing jobs and makes sure only one per
 * {@link JobType} can run at the same time.
 *
 * @author Jan Bernitt
 */
@Slf4j
@AllArgsConstructor
public abstract class AbstractSchedulingManager implements SchedulingManager
{
    private final JobService jobService;

    private final JobConfigurationService jobConfigurationService;

    private final MessageService messageService;

    private final LeaderManager leaderManager;

    @PostConstruct
    public void init()
    {
        leaderManager.setSchedulingManager( this );
    }

    /**
     * Set of currently running jobs. We use a map to use CAS operation
     * {@link Map#putIfAbsent(Object, Object)} to "atomically" start or abort
     * execution.
     */
    private final Map<JobType, Boolean> runningJobTypes = new ConcurrentHashMap<>();

    /**
     * Check if this job configuration is currently running
     *
     * @param type type of job to check
     * @return true/false
     */
    boolean isRunning( JobType type )
    {
        return runningJobTypes.containsKey( type );
    }

    protected final void execute( String jobId )
    {
        execute( jobConfigurationService.getJobConfigurationByUid( jobId ) );
    }

    protected final void execute( JobConfiguration configuration )
    {
        if ( !configuration.isEnabled() )
        {
            return;
        }
        JobType type = configuration.getJobType();
        if ( configuration.isLeaderOnlyJob() && !leaderManager.isLeader() )
        {
            whenLeaderOnlyOnNonLeader( configuration );
            return;
        }
        if ( runningJobTypes.putIfAbsent( type, true ) != null )
        {
            whenAlreadyRunning( configuration );
            return;
        }

        Clock clock = new Clock().startClock();
        try
        {
            log.debug( String.format( "Job started: '%s'", configuration.getName() ) );
            configuration.setJobStatus( JobStatus.RUNNING );
            if ( !configuration.isInMemoryJob() )
            {
                jobConfigurationService.updateJobConfiguration( configuration );
            }
            // in memory effect only: mark running (dirty)
            configuration.setLastExecutedStatus( JobStatus.RUNNING );

            // run the actual job
            jobService.getJob( type ).execute( configuration );

            if ( configuration.getLastExecutedStatus() == RUNNING )
            {
                configuration.setLastExecutedStatus( JobStatus.COMPLETED );
            }
        }
        catch ( Exception ex )
        {
            whenRunThrewException( configuration, ex );
        }
        finally
        {
            runningJobTypes.remove( configuration.getJobType() );

            whenRunIsDone( configuration, clock );
        }
    }

    private void whenRunIsDone( JobConfiguration configuration, Clock clock )
    {
        String duration = clock.time();
        if ( configuration.getLastExecutedStatus() == COMPLETED )
        {
            log.debug( "Job executed successfully: '{}'. Time used: '{}'", configuration.getName(), duration );
        }
        if ( configuration.isInMemoryJob() )
        {
            return;
        }

        configuration.setJobStatus( JobStatus.SCHEDULED );
        configuration.setNextExecutionTime( null );
        configuration.setLastExecuted( new Date() );
        configuration.setLastRuntimeExecution( duration );

        JobConfiguration persistentConfiguration = jobConfigurationService
            .getJobConfigurationByUid( configuration.getUid() );

        if ( persistentConfiguration != null )
        {
            if ( persistentConfiguration.getJobStatus() == DISABLED || !persistentConfiguration.isEnabled() )
            {
                // keep disabled status change from DB
                configuration.setJobStatus( DISABLED );
                configuration.setEnabled( false );
            }
            // only if it still exists: update
            jobConfigurationService.updateJobConfiguration( configuration );
        }
    }

    private void whenRunThrewException( JobConfiguration configuration, Exception ex )
    {
        String message = String.format( "Job failed: '%s'", configuration.getName() );
        log.error( message, ex );
        log.error( DebugUtils.getStackTrace( ex ) );
        if ( !configuration.isInMemoryJob() )
        {
            messageService.sendSystemErrorNotification( message, ex );
        }
        configuration.setLastExecutedStatus( JobStatus.FAILED );
        if ( ex instanceof InterruptedException )
        {
            Thread.currentThread().interrupt();
        }
    }

    private void whenLeaderOnlyOnNonLeader( JobConfiguration configuration )
    {
        log.debug( "Not a leader, skipping job of type: '{}' and name: '{}'",
            configuration.getJobType(), configuration.getName() );
    }

    private void whenAlreadyRunning( JobConfiguration configuration )
    {
        String message = String.format( "Job aborted: '%s', job of type: '%s' is already running",
            configuration.getName(), configuration.getJobType() );
        log.error( message );
        if ( !configuration.isInMemoryJob() )
        {
            messageService.sendSystemErrorNotification( message, new RuntimeException( message ) );
        }
    }
}
