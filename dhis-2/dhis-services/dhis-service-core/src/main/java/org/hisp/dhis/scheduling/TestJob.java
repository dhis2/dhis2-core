package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.parameters.TestJobParameters;

/**
 * @author Henning Håkonsen
 */
public class TestJob
    implements Job
{
    @Override
    public JobType getJobType()
    {
        return JobType.TEST;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        TestJobParameters parameters = (TestJobParameters) jobConfiguration.getJobParameters();

        System.out.println( "job configuration message: " + parameters.getMessage() + ", sleep for 60 seconds" );
        try
        {
            Thread.sleep( 60000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

        System.out.println( "Slept like a child - " + parameters.getMessage() );
    }
}
