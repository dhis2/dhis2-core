package org.hisp.dhis.startup;

import org.hisp.dhis.scheduling.JobConfigurationService;
import org.hisp.dhis.scheduling.SchedulingManager;
import org.hisp.dhis.system.startup.AbstractStartupRoutine;

import java.util.Date;

import static org.hisp.dhis.scheduling.JobStatus.FAILED;

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

    @Override
    public void execute( )
        throws Exception
    {
        Date now = new Date();
        jobConfigurationService.getAllJobConfigurations().forEach( (jobConfig -> {
            jobConfig.setNextExecutionTime( null );
            jobConfigurationService.updateJobConfiguration( jobConfig );

            if ( jobConfig.getLastExecutedStatus() == FAILED ||
                ( !jobConfig.isContinuousExecution() && jobConfig.getNextExecutionTime().compareTo( now ) < 0 ) )
            {
                schedulingManager.executeJob( jobConfig );
            }
            schedulingManager.scheduleJob( jobConfig );
        }) );
    }
}
