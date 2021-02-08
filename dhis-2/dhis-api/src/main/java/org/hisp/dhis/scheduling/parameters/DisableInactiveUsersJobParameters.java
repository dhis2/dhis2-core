package org.hisp.dhis.scheduling.parameters;

import java.util.Optional;

import org.hisp.dhis.common.DxfNamespaces;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.scheduling.JobParameters;
import org.hisp.dhis.scheduling.parameters.jackson.DisableInactiveUsersJobParametersDeserializer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement( localName = "jobParameters", namespace = DxfNamespaces.DXF_2_0 )
@JsonDeserialize( using = DisableInactiveUsersJobParametersDeserializer.class )
public class DisableInactiveUsersJobParameters implements JobParameters
{
    private static final long serialVersionUID = -5877578172615705990L;

    private int inactiveMonths;

    public DisableInactiveUsersJobParameters()
    {

    }

    public DisableInactiveUsersJobParameters( int inactiveMonths )
    {
        this.inactiveMonths = inactiveMonths;
    }

    @JsonProperty
    @JacksonXmlProperty( namespace = DxfNamespaces.DXF_2_0 )
    public int getInactiveMonths()
    {
        return inactiveMonths;
    }

    public void setInactiveMonths( int inactiveMonths )
    {
        this.inactiveMonths = inactiveMonths;
    }

    @Override
    public Optional<ErrorReport> validate()
    {
        if ( inactiveMonths < 1 || inactiveMonths > 24 )
        {
            return Optional.of(
                new ErrorReport( getClass(), ErrorCode.E4008, "inactiveMonths", 1, 24, inactiveMonths ) );
        }
        return Optional.empty();
    }
}
