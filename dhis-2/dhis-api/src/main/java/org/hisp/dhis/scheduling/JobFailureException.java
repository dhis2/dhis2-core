package org.hisp.dhis.scheduling;

/**
 * Simple exception for job failure
 *
 * @author Henning Håkonsen
 */
class JobFailureException extends RuntimeException
{
    JobFailureException( JobConfiguration jobConfiguration )
    {
        super( "Job '" + jobConfiguration.getName() + "' failed" );
    }
}
