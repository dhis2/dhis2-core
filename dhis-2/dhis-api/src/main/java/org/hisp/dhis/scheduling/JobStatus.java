package org.hisp.dhis.scheduling;

/**
 * @author Henning Håkonsen
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
