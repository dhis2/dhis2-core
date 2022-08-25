package org.hisp.dhis.security.spring2fa;

import org.springframework.security.authentication.BadCredentialsException;

public class TwoFactorAuthenticationEnrolException extends BadCredentialsException
{
    public TwoFactorAuthenticationEnrolException( String msg )
    {
        super( msg );
    }
}
