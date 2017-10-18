package org.hisp.dhis.scheduling.parameters;

import org.hisp.dhis.scheduling.JobId;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * @author Henning Håkonsen
 */
public class CredentialsExpiryAlertJobParameters implements JobParameters {
    private static final long serialVersionUID = 13L;

    private JobId jobId;

    CredentialsExpiryAlertJobParameters()
    {}

    public CredentialsExpiryAlertJobParameters( JobId jobId ) {
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
