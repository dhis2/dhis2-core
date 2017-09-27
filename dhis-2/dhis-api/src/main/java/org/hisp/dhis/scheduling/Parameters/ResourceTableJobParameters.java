package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobId;

import java.util.HashMap;

/**
 * @author Henning Håkonsen
 */
public class ResourceTableJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 7L;

    private JobId jobId;

    public ResourceTableJobParameters()
    {}

    public ResourceTableJobParameters( JobId jobId )
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

