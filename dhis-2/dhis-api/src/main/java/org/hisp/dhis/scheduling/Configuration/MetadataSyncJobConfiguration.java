package org.hisp.dhis.scheduling.Configuration;

import org.hisp.dhis.scheduling.JobType;

/**
 * @author Henning Håkonsen
 */
public class MetadataSyncJobConfiguration extends JobConfiguration
{
    // HH configuration options?

    public MetadataSyncJobConfiguration( String name, JobType jobType, String cronExpression )
    {
        super( name, jobType, cronExpression );
    }
}
