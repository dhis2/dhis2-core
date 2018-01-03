package org.hisp.dhis.scheduling;

import org.hisp.dhis.pushanalysis.PushAnalysis;
import org.hisp.dhis.system.deletion.DeletionHandler;

public class JobConfigurationDeletionHandler
    extends DeletionHandler
{

    private JobConfigurationService jobConfigurationService;

    public void setJobConfigurationService( JobConfigurationService jobConfigurationService )
    {
        this.jobConfigurationService = jobConfigurationService;
    }

    @Override
    protected String getClassName()
    {
        return JobConfiguration.class.getSimpleName();
    }

    @Override
    public void deletePushAnalysis( PushAnalysis pushAnalysis )
    {
        JobConfiguration jobConfiguration = jobConfigurationService.getJobConfigurationByName( pushAnalysis.getName() );

        if ( jobConfiguration != null )
        {
            jobConfigurationService.deleteJobConfiguration( jobConfiguration );
        }
    }
}
