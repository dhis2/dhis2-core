package org.hisp.dhis.scheduling;

/**
 * Created by henninghakonsen on 05/09/2017.
 * Project: dhis-2.
 */
public class DefaultJobInstance implements JobInstance
{
    public void execute( JobConfiguration jobConfiguration, SchedulingManager schedulingManager )
    {
        if(!schedulingManager.isJobConfigurationRunning( jobConfiguration.getJobType() ))
        {
            schedulingManager.runJobConfiguration( jobConfiguration );
            jobConfiguration.setJobStatus( JobStatus.RUNNING );
            schedulingManager.getJob( jobConfiguration.getJobType() ).execute( jobConfiguration.getJobParameters() );
            jobConfiguration.setJobStatus( JobStatus.COMPLETED );
        } else {
            System.out.println("Job '" + jobConfiguration.getName() + "' failed, jobtype '" + jobConfiguration.getJobType() + "' is already running");
            jobConfiguration.setJobStatus( JobStatus.FAILED );
        }

        schedulingManager.jobConfigurationFinished( jobConfiguration );
    }
}
