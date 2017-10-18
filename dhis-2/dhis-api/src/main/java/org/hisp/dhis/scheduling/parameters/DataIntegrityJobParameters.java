package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author Henning Håkonsen
 */
public class DataIntegrityJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 12L;

    private JobId jobId;

    DataIntegrityJobParameters()
    {}

    public DataIntegrityJobParameters( JobId jobId ) {
        this.jobId = jobId;
    }

    public JobId getJobId()
    {
        return jobId;
    }

    @Override
    public void setJobId( JobId jobId )
    {
        this.jobId = jobId;
    }
}
