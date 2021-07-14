package org.hisp.dhis.webapi.json.domain;

import org.hisp.dhis.webapi.json.JsonObject;

/**
 * Web API equivalent of a {@code WebMessage} or {@code DescriptiveWebMessage}
 *
 * @author Jan Bernitt
 */
public interface JsonWebMessage extends JsonObject
{
    default String getHttpStatus()
    {
        return getString( "httpStatus" ).string();
    }

    default int getHttpStatusCode()
    {
        return getNumber( "httpStatusCode" ).intValue();
    }

    default String getStatus()
    {
        return getString( "status" ).string();
    }

    default String getMessage()
    {
        return getString( "message" ).string();
    }

    default String getDescription()
    {
        return getString( "description" ).string();
    }
}
