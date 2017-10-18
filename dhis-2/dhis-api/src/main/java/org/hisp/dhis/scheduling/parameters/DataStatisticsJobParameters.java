package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author Henning Håkonsen
 */
public class DataStatisticsJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 2L;

    private JobId jobId;

    public DataStatisticsJobParameters()
    {}

    public DataStatisticsJobParameters( JobId jobId )
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
