package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Parameters.TestJobParameters;

/**
 * Created by henninghakonsen on 04/09/2017.
 * Project: dhis-2.
 */
public class TestJob implements Job
{
    @Override
    public JobType getJobType()
    {
        return JobType.TEST;
    }

    @Override
    public void execute( JobParameters jobParameters )
    {
        TestJobParameters testJobConfigurationParameters = (TestJobParameters) jobParameters;
        System.out.println( "job configuration message: " + testJobConfigurationParameters.getMessage() );
    }
}
