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

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.commons.util.DebugUtils;
import org.hisp.dhis.leader.election.LeaderManager;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.system.util.Clock;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

/**
 * @author Henning Håkonsen
 */
@Slf4j
@Component( "org.hisp.dhis.scheduling.JobInstance" )
public class DefaultJobInstance
    implements JobInstance
{
    private static final String NOT_LEADER_SKIP_LOG = "Not a leader, skipping job with jobType:%s and name:%s";

    private SchedulingManager schedulingManager;

    private MessageService messageService;

    private LeaderManager leaderManager;

    @SuppressWarnings( "unused" )
    private DefaultJobInstance()
    {
    }

    public DefaultJobInstance( SchedulingManager schedulingManager, MessageService messageService,
        LeaderManager leaderManager )
    {
        this.schedulingManager = schedulingManager;
        this.messageService = messageService;
        this.leaderManager = leaderManager;

        Preconditions.checkNotNull( schedulingManager );
        Preconditions.checkNotNull( messageService );
        Preconditions.checkNotNull( leaderManager );
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isEnabled() )
        {
            return;
        }

        if ( jobConfiguration.isLeaderOnlyJob() && !leaderManager.isLeader() )
        {
            log.debug(
                String.format( NOT_LEADER_SKIP_LOG, jobConfiguration.getJobType(), jobConfiguration.getName() ) );
            return;
        }

        final Clock clock = new Clock().startClock();

        try
        {
            if ( jobConfiguration.isInMemoryJob() )
            {
                executeJob( jobConfiguration, clock );
            }
            else if ( !schedulingManager.isJobConfigurationRunning( jobConfiguration ) )
            {
                jobConfiguration.setJobStatus( JobStatus.RUNNING );
                schedulingManager.jobConfigurationStarted( jobConfiguration );
                jobConfiguration.setNextExecutionTime( null );

                executeJob( jobConfiguration, clock );

                jobConfiguration.setLastExecutedStatus( JobStatus.COMPLETED );
            }
            else
            {
                String message = String.format( "Job failed: '%s', job type already running: '%s'",
                    jobConfiguration.getName(), jobConfiguration.getJobType() );
                log.error( message );
                messageService.sendSystemErrorNotification( message, new RuntimeException( message ) );

                jobConfiguration.setLastExecutedStatus( JobStatus.FAILED );
            }
        }
        catch ( Exception ex )
        {
            String message = String.format( "Job failed: '%s'", jobConfiguration.getName() );
            messageService.sendSystemErrorNotification( message, ex );
            log.error( message, ex );
            log.error( DebugUtils.getStackTrace( ex ) );

            jobConfiguration.setLastExecutedStatus( JobStatus.FAILED );
        }
        finally
        {
            setFinishingStatus( clock, jobConfiguration );
        }
    }

    /**
     * Set status properties of job after finish. If the job was executed
     * manually and the job is disabled we want to set the status back to
     * DISABLED.
     *
     * @param clock Clock for keeping track of time usage.
     * @param jobConfiguration the job configuration.
     */
    private void setFinishingStatus( Clock clock, JobConfiguration jobConfiguration )
    {
        if ( jobConfiguration.isInMemoryJob() )
        {
            return;
        }

        if ( !jobConfiguration.isEnabled() )
        {
            jobConfiguration.setJobStatus( JobStatus.DISABLED );
        }
        else
        {
            jobConfiguration.setJobStatus( JobStatus.SCHEDULED );
        }

        jobConfiguration.setNextExecutionTime( null );
        jobConfiguration.setLastExecuted( new Date() );
        jobConfiguration.setLastRuntimeExecution( clock.time() );

        schedulingManager.jobConfigurationFinished( jobConfiguration );
    }

    /**
     * Method which calls the execute method in the job. The job will run in
     * this thread and finish, either with success or with an exception.
     *
     * @param jobConfiguration the configuration to execute.
     * @param clock refers to start time.
     */
    private void executeJob( JobConfiguration jobConfiguration, Clock clock )
    {
        log.debug( String.format( "Job started: '%s'", jobConfiguration.getName() ) );

        schedulingManager.getJob( jobConfiguration.getJobType() ).execute( jobConfiguration );

        log.debug( String.format( "Job executed successfully: '%s'. Time used: '%s'", jobConfiguration.getName(),
            clock.time() ) );
    }
}
