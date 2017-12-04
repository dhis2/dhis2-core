package org.hisp.dhis.scheduling;

import org.hisp.dhis.feedback.ErrorReport;

/**
 * This interface is used for jobs in the system which are scheduled or executed by spring scheduler.
 * The actual job will contain an execute method which performs the appropriate actions.
 * <p>
 * {@link JobInstance} is another interface connected to jobs. This interface is used for the actual execution of the job.
 * See {@link SchedulingManager} for more information about the scheduling.
 *
 * @author Henning Håkonsen
 */
public interface Job
{
    JobType getJobType();

    void execute( JobConfiguration jobConfiguration )
        throws Exception;

    ErrorReport validate( );
}
