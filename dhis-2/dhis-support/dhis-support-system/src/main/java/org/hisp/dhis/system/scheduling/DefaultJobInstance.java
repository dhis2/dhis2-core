package org.hisp.dhis.system.scheduling;

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
public class DefaultJobInstance implements JobInstance
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
