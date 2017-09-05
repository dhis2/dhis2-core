package org.hisp.dhis.scheduling;

/**
 * @author Henning Håkonsen
 */
public interface Job
{
    JobType getJobType();

    void execute( JobParameters jobParameters );
}
