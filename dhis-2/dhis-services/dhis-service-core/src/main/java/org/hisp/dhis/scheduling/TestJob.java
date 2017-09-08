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
        System.out.println( "job configuration message: " + testJobConfigurationParameters.getMessage() + ", sleep for 60 seconds");
        try
        {
            Thread.sleep( 60000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

        System.out.println("Slept like a child - " + testJobConfigurationParameters.getMessage());
    }
}
