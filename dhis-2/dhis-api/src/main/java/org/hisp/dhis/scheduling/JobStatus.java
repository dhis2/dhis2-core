package org.hisp.dhis.scheduling;

/**
 * Created by henninghakonsen on 23/08/2017.
 * Project: dhis-2.
 */
public enum JobStatus
{
    RUNNING( "running" ),
    COMPLETED( "done" ),
    STOPPED( "stopped" ),
    SCHEDULED( "scheduled" ),
    FAILED( "failed" );

    private final String key;

    JobStatus( String key )
    {
        this.key = key;
    }

    public String getKey()
    {
        return key;
    }
}
