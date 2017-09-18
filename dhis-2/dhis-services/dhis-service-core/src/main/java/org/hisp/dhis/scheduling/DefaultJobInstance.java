package org.hisp.dhis.scheduling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.system.scheduling.SpringScheduler;
import org.hisp.dhis.system.util.Clock;

import java.util.Date;

/**
 * @author Henning Håkonsen
 */
public class DefaultJobInstance implements JobInstance
{
    private static final Log log = LogFactory.getLog( SpringScheduler.class );
    private static final Long oneDayInSeconds = (long)24*24*60;

    private void setFinishingStatus( SchedulingManager schedulingManager, JobConfiguration jobConfiguration )
    {
        jobConfiguration.setJobStatus( JobStatus.SCHEDULED );
        jobConfiguration.setNextExecutionTime( null );
        jobConfiguration.setLastExecuted( new Date(  ) );

        schedulingManager.jobConfigurationFinished( jobConfiguration );
    }

    public void execute( JobConfiguration jobConfiguration, SchedulingManager schedulingManager, MessageService messageService )
    {
        final Clock clock = new Clock().startClock();

        if(!jobConfiguration.getEnabled()) return;

        if(!schedulingManager.isJobConfigurationRunning( jobConfiguration.getJobType() ))
        {
            jobConfiguration.setJobStatus( JobStatus.RUNNING );
            schedulingManager.runJobConfiguration( jobConfiguration );

            try
            {
                log.info( "Job '" + jobConfiguration.getName() + "' started");

                schedulingManager.getJob( jobConfiguration.getJobType() ).execute( jobConfiguration.getJobParameters() );

                log.info( "Job '" + jobConfiguration.getName() + "' executed successfully");
            }
            catch ( RuntimeException ex )
            {
                messageService.sendSystemErrorNotification( "Job '" + jobConfiguration.getName() + "' failed", ex );
                jobConfiguration.setLastExecutedStatus( JobStatus.FAILED );

                setFinishingStatus( schedulingManager, jobConfiguration );

                throw ex;
            }

            jobConfiguration.setLastExecutedStatus( JobStatus.COMPLETED );
        } else {
            messageService.sendSystemErrorNotification( "Job '" + jobConfiguration.getName() + "' failed, jobtype '" + jobConfiguration.getJobType() + "' is already running [" + clock.time() + "]", new JobFailureException(jobConfiguration) );
            jobConfiguration.setLastExecutedStatus( JobStatus.FAILED );

            // Simple solution for rescheduling jobs. If the next execution time is more than a day away, we wait the configured amount of minimum interval time and reschedule the job.
            Long duration = jobConfiguration.getNextExecutionTime().getTime() - new Date().getTime();
            if( duration > oneDayInSeconds ) {
                try
                {
                    Thread.sleep( jobConfiguration.getJobType().getMinimumFrequencyInSeconds() );
                }
                catch ( InterruptedException e )
                {
                    e.printStackTrace();
                }
            }
            schedulingManager.executeJob( jobConfiguration );
        }

        setFinishingStatus( schedulingManager, jobConfiguration );
    }
}
