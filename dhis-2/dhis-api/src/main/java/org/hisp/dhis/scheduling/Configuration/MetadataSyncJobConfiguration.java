package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.JobType;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public class MetadataSyncJobConfiguration extends JobConfiguration
{
    // HH configuration options?

    public MetadataSyncJobConfiguration( String name, JobType jobType, String cronExpression )
    {
        super( name, jobType, cronExpression );
    }
}
