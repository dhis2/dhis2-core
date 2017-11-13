package org.hisp.dhis.scheduling.parameters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.schema.annotation.Property;

/**
 * @author Henning Håkonsen
 */
public class TestJobParameters
    implements JobParameters
{
    private static final long serialVersionUID = 3600315605964091689L;

    @Property
    private String message;

    public TestJobParameters()
    {
    }

    public TestJobParameters( String message )
    {
        this.message = message;
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
}
