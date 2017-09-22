package org.hisp.dhis.scheduling.Parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.JobId;

/**
 * @author Henning Håkonsen
 */
public class TestJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 10L;

    private JobId jobId;
    private String message;

    public TestJobParameters()
    {}

    public TestJobParameters( JobId jobId )
    {
        this.jobId = jobId;
    }

    @JacksonXmlProperty
    @JsonProperty
    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
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
