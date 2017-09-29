package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author Henning Håkonsen
 */
public class FileResourceCleanUpJobParameters
    implements JobParameters
{
    JobId jobId;

    FileResourceCleanUpJobParameters()
    {}

    FileResourceCleanUpJobParameters( JobId jobId )
    {
        this.jobId = jobId;
    }

    @Override
    public JobId getJobId()
    {
        return null;
    }

    @Override
    public void setJobId( JobId jobId )
    {
        this.jobId = jobId;
    }
}
