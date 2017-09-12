package org.hisp.dhis.scheduling;

import org.hisp.dhis.message.MessageService;
import org.hisp.dhis.system.util.Clock;

/**
 * @author Henning Håkonsen
 */
public class DefaultJobInstance implements JobInstance
{
    public void execute( JobConfiguration jobConfiguration, SchedulingManager schedulingManager, MessageService messageService )
    {
        final Clock clock = new Clock().startClock();

        if(!jobConfiguration.getEnabled()) return;

        if(!schedulingManager.isJobConfigurationRunning( jobConfiguration.getJobType() ))
        {
            schedulingManager.runJobConfiguration( jobConfiguration );
            jobConfiguration.setJobStatus( JobStatus.RUNNING );
            schedulingManager.getJob( jobConfiguration.getJobType() ).execute( jobConfiguration.getJobParameters() );
            jobConfiguration.setJobStatus( JobStatus.COMPLETED );
        } else {
            messageService.sendSystemErrorNotification( "Job '" + jobConfiguration.getName() + "' failed, jobtype '" + jobConfiguration.getJobType() + "' is already running [" + clock.time() + "]", new JobFailureException(jobConfiguration) );

            jobConfiguration.setJobStatus( JobStatus.FAILED );
        }

        schedulingManager.jobConfigurationFinished( jobConfiguration );
    }
}
