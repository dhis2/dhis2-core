package org.hisp.dhis.scheduling;

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
import org.hisp.dhis.system.util.Clock;

import java.util.Date;

/**
 * @author Henning Håkonsen
 */
public class DefaultJobInstance 
    implements JobInstance
{
    private static final Log log = LogFactory.getLog( DefaultJobInstance.class );

    public void execute( JobConfiguration jobConfiguration, SchedulingManager schedulingManager,
        MessageService messageService )
    {
        if ( !jobConfiguration.isEnabled() )
        {
            return;
        }

        final Clock clock = new Clock().startClock();
        try
        {
            if ( jobConfiguration.isInMemoryJob() )
            {
                executeJob( jobConfiguration, schedulingManager, clock );
            }
            else if ( !schedulingManager.isJobConfigurationRunning( jobConfiguration ) )
            {
                jobConfiguration.setJobStatus( JobStatus.RUNNING );
                schedulingManager.jobConfigurationStarted( jobConfiguration );
                jobConfiguration.setNextExecutionTime( null );

                executeJob( jobConfiguration, schedulingManager, clock );

                jobConfiguration.setLastExecutedStatus( JobStatus.COMPLETED );
            }
            else
            {
                log.error(
                    "Job '" + jobConfiguration.getName() + "' failed, jobtype '" + jobConfiguration.getJobType() +
                        "' is already running." );

                messageService.sendSystemErrorNotification(
                    "Job '" + jobConfiguration.getName() + "' failed, jobtype '" + jobConfiguration.getJobType() +
                        "' is already running.",
                    new Exception( "Job '" + jobConfiguration.getName() + "' failed" ) );

                jobConfiguration.setLastExecutedStatus( JobStatus.FAILED );
            }
        }
        catch ( Exception ex )
        {
            messageService.sendSystemErrorNotification( "Job '" + jobConfiguration.getName() + "' failed", ex );
            log.error( "Job '" + jobConfiguration.getName() + "' failed", ex );

            jobConfiguration.setLastExecutedStatus( JobStatus.FAILED );
        }
        finally
        {
            setFinishingStatus( clock, schedulingManager, jobConfiguration );
        }
    }

    /**
     * Set status properties of job after finish. If the job was executed manually and the job is disabled we want
     * to set the status back to DISABLED.
     *
     * @param clock Clock for keeping track of time usage
     * @param schedulingManager reference to scheduling manager
     * @param jobConfiguration the job configuration
     */
    private void setFinishingStatus( Clock clock, SchedulingManager schedulingManager, JobConfiguration jobConfiguration )
    {
        if ( jobConfiguration.isInMemoryJob() )
        {
            return;
        }

        if ( !jobConfiguration.isContinuousExecution() )
        {
            jobConfiguration.setJobStatus( JobStatus.SCHEDULED );
        }

        if ( !jobConfiguration.isEnabled() )
        {
            jobConfiguration.setJobStatus( JobStatus.DISABLED );
        }

        jobConfiguration.setNextExecutionTime( null );
        jobConfiguration.setLastExecuted( new Date() );
        jobConfiguration.setLastRuntimeExecution( clock.time() );

        schedulingManager.jobConfigurationFinished( jobConfiguration );
    }

    /**
     * Method which calls the execute method in the job. The job will run in this thread and finish, either with success
     * or with an exception.
     *
     * @param jobConfiguration the configuration to execute
     * @param schedulingManager a reference to the scheduling manager
     * @param clock refers to start time
     * @throws Exception if the job fails
     */
    private void executeJob( JobConfiguration jobConfiguration, SchedulingManager schedulingManager,
        Clock clock )
        throws Exception
    {
        log.info( "Job '" + jobConfiguration.getName() + "' started" );

        schedulingManager.getJob( jobConfiguration.getJobType() ).execute( jobConfiguration );

        log.info( "Job '" + jobConfiguration.getName() + "' executed successfully. Time used: " + clock.time() );
    }
}
