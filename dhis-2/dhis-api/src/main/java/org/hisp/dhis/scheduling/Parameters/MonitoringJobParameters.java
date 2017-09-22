package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobId;

/**
 * @author Henning Håkonsen
 */
public class MonitoringJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 5L;

    private JobId jobId;

    public MonitoringJobParameters()
    {}

    public MonitoringJobParameters( JobId jobId )
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
