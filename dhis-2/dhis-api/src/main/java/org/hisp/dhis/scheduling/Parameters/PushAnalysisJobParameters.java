package org.hisp.dhis.scheduling.Parameters;

import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobId;

import java.util.HashMap;

/**
 * @author Henning Håkonsen
 */
public class PushAnalysisJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 7L;

    private int pushAnalysisId;
    private JobId jobId;

    public PushAnalysisJobParameters()
    {}

    public PushAnalysisJobParameters( JobId jobId, int pushAnalysisId )
    {
        this.pushAnalysisId = pushAnalysisId;
        this.jobId = jobId;
    }

    public int getPushAnalysisId()
    {
        return pushAnalysisId;
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
