package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;

/**
 * Created by henninghakonsen on 30/08/2017.
 * Project: dhis-2.
 */
public interface Job
{
    void execute( JobConfiguration jobConfiguration );
}
