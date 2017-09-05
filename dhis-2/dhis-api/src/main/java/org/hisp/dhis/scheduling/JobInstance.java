package org.hisp.dhis.scheduling;

/**
 * Created by henninghakonsen on 05/09/2017.
 * Project: dhis-2.
 */
public interface JobInstance
{
    void execute( JobConfiguration jobConfiguration, SchedulingManager schedulingManager );
}
