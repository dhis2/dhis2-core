package org.hisp.dhis.feedback;

public interface Error
{
    ErrorCode getCode();

    String getMessage();
}
