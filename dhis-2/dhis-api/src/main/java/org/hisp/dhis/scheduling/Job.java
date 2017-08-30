package org.hisp.dhis.scheduling;

import org.hisp.dhis.scheduling.Configuration.JobConfiguration;

/**
 * @author Henning Håkonsen
 */
public interface Job
{
    void execute( JobConfiguration jobConfiguration );
}
