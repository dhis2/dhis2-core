package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;

/**
 * @author Henning Håkonsen
 */
public interface Job
{
    JobType getJobType();

    void execute( JobConfiguration jobConfiguration );
}
