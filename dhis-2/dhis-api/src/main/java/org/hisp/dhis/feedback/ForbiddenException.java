package org.hisp.dhis.feedback;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.webmessage.WebMessageResponse;

import java.text.MessageFormat;

import static org.hisp.dhis.common.OpenApi.Response.Status.FORBIDDEN;

@OpenApi.Response( status = FORBIDDEN, value = WebMessageResponse.class )
public final class ForbiddenException extends Exception implements Error
{
    private final ErrorCode code;

    public ForbiddenException(String message) {
        super(message);
        this.code = ErrorCode.E1006;
    }

    public ForbiddenException( ErrorCode code, Object...args )
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

