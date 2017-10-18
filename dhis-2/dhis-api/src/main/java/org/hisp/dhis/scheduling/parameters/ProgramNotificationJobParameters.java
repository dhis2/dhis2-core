package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobId;

/**
 * @author Henning Håkonsen
 */
public class ProgramNotificationJobParameters
   implements JobParameters
{
    private static final long serialVersionUID = 6L;

    private JobId jobId;

    public ProgramNotificationJobParameters()
    {}

    public ProgramNotificationJobParameters( JobId jobId )
    {
        this.jobId = jobId;
    }

    public JobId getJobId()
    {
        return jobId;
    }

    public void setJobId( JobId jobId )
    {
        this.jobId = jobId;
    }
}
