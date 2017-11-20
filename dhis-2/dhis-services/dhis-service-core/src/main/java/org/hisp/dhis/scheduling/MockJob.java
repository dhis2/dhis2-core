package org.hisp.dhis.scheduling;

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
        try
        {
            Thread.sleep( 10000 );
        }
        catch ( InterruptedException e )
        {
            e.printStackTrace();
        }
    }
}
