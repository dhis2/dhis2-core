package org.hisp.dhis.scheduling.parameters.jackson;

import org.hisp.dhis.scheduling.parameters.DisableInactiveUsersJobParameters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class DisableInactiveUsersJobParametersDeserializer
    extends AbstractJobParametersDeserializer<DisableInactiveUsersJobParameters>
{

    public DisableInactiveUsersJobParametersDeserializer()
    {
        super( DisableInactiveUsersJobParameters.class, CustomJobParameters.class );
    }

    @JsonDeserialize
    public static class CustomJobParameters extends DisableInactiveUsersJobParameters
    {
    }
}
