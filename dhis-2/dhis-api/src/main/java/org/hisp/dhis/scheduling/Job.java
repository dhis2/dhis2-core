package org.hisp.dhis.scheduling;

import org.hisp.dhis.common.IdentifiableObject;
import org.joda.time.DateTime;

/**
 * Created by henninghakonsen on 25/08/2017.
 * Project: dhis-2.
 */
public interface Job
    extends IdentifiableObject
{
    String getKey( );

    JobType getJobType( );

    String getCronExpression( );

    JobStatus getStatus( );

    DateTime getStartTime( );

    DateTime getEndTime();

    Runnable getRunnable();
}
