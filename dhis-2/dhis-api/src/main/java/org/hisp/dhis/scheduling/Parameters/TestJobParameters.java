package org.hisp.dhis.scheduling.Parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;

/**
 * Created by henninghakonsen on 04/09/2017.
 * Project: dhis-2.
 */
public class TestJobParameters
    implements JobParameters
{
    public TestJobParameters()
    {}

    private String message;

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
}
