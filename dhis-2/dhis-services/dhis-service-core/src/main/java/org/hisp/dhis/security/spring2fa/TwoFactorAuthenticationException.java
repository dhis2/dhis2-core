package org.hisp.dhis.security.spring2fa;

import org.springframework.security.authentication.BadCredentialsException;

public class TwoFactorAuthenticationException extends BadCredentialsException
{
    public TwoFactorAuthenticationException( String msg )
    {
        super( msg );
    }
}
