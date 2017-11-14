package org.hisp.dhis.scheduling;

/**
 * Enum for job status. This is used for current status of a job(RUNNING or SCHEDULED) and for last executed status, for which the
 * status can be COMPLETED, STOPPED or FAILED.
 *
 * @author Henning Håkonsen
 */
public enum JobStatus
{
    RUNNING( "running" ),
    COMPLETED( "done" ),
    STOPPED( "stopped" ),
    SCHEDULED( "scheduled" ),
    DISABLED( "disabled" ),
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
