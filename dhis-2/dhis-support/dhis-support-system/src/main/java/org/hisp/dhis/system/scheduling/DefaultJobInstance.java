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
import org.hisp.dhis.scheduling.JobConfiguration;
import org.hisp.dhis.scheduling.JobInstance;
import org.hisp.dhis.scheduling.JobStatus;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.system.util.Clock;

import java.util.Date;

/**
 * @author Henning Håkonsen
 */
public class DefaultJobInstance 
    implements JobInstance
{
    private static final Log log = LogFactory.getLog( SpringScheduler.class );

    private void setFinishingStatus( Clock clock, SchedulingManager schedulingManager, JobConfiguration jobConfiguration )
    {
        if ( !jobConfiguration.isContinuousExecution() )
        {
            jobConfiguration.setJobStatus( JobStatus.SCHEDULED );
        }
        jobConfiguration.setNextExecutionTime( null );
        jobConfiguration.setLastExecuted( new Date() );
        jobConfiguration.setLastRuntimeExecution( clock.time() );

        schedulingManager.jobConfigurationFinished( jobConfiguration );
    }

    public void execute( JobConfiguration jobConfiguration, SchedulingManager schedulingManager,
        MessageService messageService )
        throws Exception
    {
        final Clock clock = new Clock().startClock();

        if ( !schedulingManager.isJobConfigurationRunning( jobConfiguration ) )
        {
            jobConfiguration.setJobStatus( JobStatus.RUNNING );
            schedulingManager.jobConfigurationStarted( jobConfiguration );
            jobConfiguration.setNextExecutionTime( null );

            try
            {
                log.info( "Job '" + jobConfiguration.getName() + "' started" );

                schedulingManager.getJob( jobConfiguration.getJobType() ).execute( jobConfiguration );

                log.info( "Job '" + jobConfiguration.getName() + "' executed successfully" );
            }
            catch ( RuntimeException ex )
            {
                messageService.sendSystemErrorNotification( "Job '" + jobConfiguration.getName() + "' failed", ex );
                log.error( "Job '" + jobConfiguration.getName() + "' failed", ex );

                jobConfiguration.setLastExecutedStatus( JobStatus.FAILED );

                setFinishingStatus( clock, schedulingManager, jobConfiguration );
                throw ex;
            }

            jobConfiguration.setLastExecutedStatus( JobStatus.COMPLETED );
        }
        else
        {
            log.error( "Job '" + jobConfiguration.getName() + "' failed, jobtype '" + jobConfiguration.getJobType() +
                "' is already running." );

            messageService.sendSystemErrorNotification(
                "Job '" + jobConfiguration.getName() + "' failed, jobtype '" + jobConfiguration.getJobType() +
                    "' is already running [" + clock.time() + "]",
                new Exception( "Job '" + jobConfiguration.getName() + "' failed" ) );
            jobConfiguration.setLastExecutedStatus( JobStatus.FAILED );
        }

        setFinishingStatus( clock, schedulingManager, jobConfiguration );
    }
}
