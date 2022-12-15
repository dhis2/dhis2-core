package org.hisp.dhis.feedback;

import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.webmessage.WebMessageResponse;

import java.text.MessageFormat;

import static org.hisp.dhis.common.OpenApi.Response.Status.CONFLICT;

@OpenApi.Response( status = CONFLICT, value = WebMessageResponse.class )
public final class ConflictException extends Exception implements Error
{
    private final ErrorCode code;

    public ConflictException(String message) {
        super(message);
        this.code = ErrorCode.E1004;
    }

    public ConflictException( ErrorCode code, Object...args )
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