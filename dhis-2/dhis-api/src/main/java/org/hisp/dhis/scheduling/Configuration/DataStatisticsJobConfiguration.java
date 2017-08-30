package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.JobType;

/**
 * Created by henninghakonsen on 30/08/2017.
 * Project: dhis-2.
 */
public class DataStatisticsJobConfiguration
    extends JobConfiguration
{
    public DataStatisticsJobConfiguration( String name, JobType jobType, String cronExpression )
    {
        super( name, jobType, cronExpression );
    }
}
