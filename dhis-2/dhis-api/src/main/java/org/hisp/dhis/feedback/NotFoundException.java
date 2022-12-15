package org.hisp.dhis.feedback;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.webmessage.WebMessageResponse;

import java.text.MessageFormat;

import static org.hisp.dhis.common.OpenApi.Response.Status.NOT_FOUND;

@OpenApi.Response( status = NOT_FOUND, value = WebMessageResponse.class )
public final class NotFoundException extends Exception implements Error
{
    private final ErrorCode code;

    public NotFoundException(String message) {
        super(message);
        this.code = ErrorCode.E1005;
    }

    public NotFoundException( ErrorCode code, Object...args )
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
