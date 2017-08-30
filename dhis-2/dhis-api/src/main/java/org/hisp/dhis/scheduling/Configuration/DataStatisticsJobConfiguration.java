package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.JobType;

/**
 * @author Henning Håkonsen
 */
public class DataStatisticsJobConfiguration
    extends JobConfiguration
{
    public DataStatisticsJobConfiguration( String name, JobType jobType, String cronExpression )
    {
        super( name, jobType, cronExpression );
    }
}
