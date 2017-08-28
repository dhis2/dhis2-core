package org.hisp.dhis.scheduling;

import org.hisp.dhis.common.IdentifiableObject;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by henninghakonsen on 25/08/2017.
 * Project: dhis-2.
 */
public interface Job
    extends IdentifiableObject
{
    void setDelay( long delay );

    /**
     * Set status of job. Default status is scheduled (Send 'null' as parameter to the method).
     * @param status the status you want the job to have
     */
    void setStatus( JobStatus status );

    /**
     * Set key of job. If no key is given, a key is generated.
     * @param key key you want for the job
     */
    void setKey( String key );

    void setNextExecutionTime();

    String getKey( );

    JobType getJobType( );

    String getCronExpression( );

    Date getNextExecutionTime();

    JobStatus getStatus( );

    DateTime getStartTime( );

    DateTime getEndTime();

    long getDelay();

    Runnable getRunnable();

    @Override
    int compareTo( IdentifiableObject o );
}
