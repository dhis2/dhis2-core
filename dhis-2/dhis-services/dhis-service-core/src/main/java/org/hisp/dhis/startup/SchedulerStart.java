package org.hisp.dhis.startup;

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

import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hisp.dhis.scheduling.JobStatus.FAILED;
import static org.hisp.dhis.scheduling.JobStatus.SCHEDULED;

/**
 *
 * Reschedule old jobs and execute jobs which were scheduled when the server was not running.
 *
 * @author Henning Håkonsen
 */
public class SchedulerStart
    extends AbstractStartupRoutine
{
    private JobConfigurationService jobConfigurationService;

    public void setJobConfigurationService( JobConfigurationService jobConfigurationService )
    {
        this.jobConfigurationService = jobConfigurationService;
    }

    private SchedulingManager schedulingManager;

    public void setSchedulingManager( SchedulingManager schedulingManager )
    {
        this.schedulingManager = schedulingManager;
    }

    private MessageService messageService;

    public void setMessageService( MessageService messageService )
    {
        this.messageService = messageService;
    }

    @Override
    public void execute( )
        throws Exception
    {
        Date now = new Date();
        List<String> unexecutedJobs = new ArrayList<>( );

        jobConfigurationService.getAllJobConfigurations().forEach( (jobConfig -> {
            if ( jobConfig.isEnabled() )
            {
                Date oldExecutionTime = jobConfig.getNextExecutionTime();

                jobConfig.setNextExecutionTime( null );
                jobConfig.setJobStatus( SCHEDULED );
                jobConfigurationService.updateJobConfiguration( jobConfig );

                if ( jobConfig.getLastExecutedStatus() == FAILED ||
                    ( !jobConfig.isContinuousExecution() && oldExecutionTime.compareTo( now ) < 0 ) )
                {
                    unexecutedJobs.add( "Job [" + jobConfig.getUid() + ", " + jobConfig.getName() + "] has status failed or was scheduled in server downtime. Actual execution time was supposed to be: " + oldExecutionTime );
                }

                schedulingManager.scheduleJob( jobConfig );
            }
        }) );

        if ( unexecutedJobs.size() > 0 )
        {
            StringBuilder jobs = new StringBuilder();

            for ( String unexecutedJob : unexecutedJobs )
            {
                jobs.append( unexecutedJob ).append( "\n" );
            }

            messageService.sendSystemErrorNotification( "Scheduler startup", new Exception( "Scheduler started with one or more unexecuted jobs:\n" + jobs ) );
        }
    }
}
