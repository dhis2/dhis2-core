package org.hisp.dhis.feedback;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.webmessage.WebMessageResponse;

import java.text.MessageFormat;

import static org.hisp.dhis.common.OpenApi.Response.Status.BAD_REQUEST;

@OpenApi.Response( status = BAD_REQUEST, value = WebMessageResponse.class )
public final class BadRequestException extends Exception implements Error
{
    private final ErrorCode code;

    public BadRequestException( String message )
    {
        super(message);
        this.code = ErrorCode.E1003;
    }

    public BadRequestException( ErrorCode code, Object...args )
    {
        super( MessageFormat.format( code.getMessage(), args ));
        this.code = code;
    }

    @Override
    public ErrorCode getCode()
    {
        return code;
    }
}
