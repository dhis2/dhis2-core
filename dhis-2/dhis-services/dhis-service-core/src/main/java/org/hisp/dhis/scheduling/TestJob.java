package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;

import java.util.Date;

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
    public void execute( JobConfiguration jobConfiguration )
    {
        if(!jobConfiguration.getCronExpression().equals( "" )) jobConfiguration.setNextExecutionTime();

        System.out.println("Job with name " + jobConfiguration.getName() + " fired, at time: " + new Date() + ", with cron: " + jobConfiguration.getCronExpression() + ", nextEx: " + jobConfiguration.getNextExecutionTime());
    }
}
