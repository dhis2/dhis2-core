package org.hisp.dhis.scheduling;


import java.io.Serializable;

/**
 * Interface for job specific parameters. Serializable so that we can store the object in the database.
 *
 * @author Henning Håkonsen
 */
public interface JobParameters
    extends Serializable
{
    JobId getJobId();

    void setJobId( JobId jobId );
}
