package org.hisp.dhis.scheduling;

/**
 * Created by henninghakonsen on 12/09/2017.
 * Project: dhis-2.
 */
class JobFailureException extends RuntimeException
{
    JobFailureException( JobConfiguration jobConfiguration )
    {
        super( "Job '" + jobConfiguration.getName() + "' failed" );
    }
}
