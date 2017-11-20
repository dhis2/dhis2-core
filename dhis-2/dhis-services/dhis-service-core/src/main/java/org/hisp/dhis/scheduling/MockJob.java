package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.parameters.MockJobParameters;

/**
 * @author Henning Håkonsen
 */
public class MockJob
    implements Job
{
    @Override
    public JobType getJobType()
    {
        return JobType.MOCK;
    }

    @Override
    public void execute( JobConfiguration jobConfiguration )
    {
        MockJobParameters parameters = (MockJobParameters) jobConfiguration.getJobParameters();

        System.out.println( "job configuration message: " + parameters.getMessage() + ", sleep for 10 seconds" );
        try
        {
            Thread.sleep( 10000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }

        System.out.println( "Slept like a child - " + parameters.getMessage() );
    }
}
