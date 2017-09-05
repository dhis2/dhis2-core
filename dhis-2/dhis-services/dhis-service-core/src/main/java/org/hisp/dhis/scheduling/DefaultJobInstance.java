package org.hisp.dhis.scheduling;

/**
 * Created by henninghakonsen on 05/09/2017.
 * Project: dhis-2.
 */
public class DefaultJobInstance implements JobInstance
{
    public void execute( JobConfiguration jobConfiguration, SchedulingManager schedulingManager )
    {
        if(!schedulingManager.isJobInProgress( jobConfiguration.getUid() ))
        {
            schedulingManager.runJobConfiguration( jobConfiguration );
            schedulingManager.getJob( jobConfiguration.getJobType() ).execute( jobConfiguration.getJobParameters() );
        }
    }
}
