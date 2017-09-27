package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobParameters;

import java.util.HashMap;

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
}
