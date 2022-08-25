package org.hisp.dhis.security.authtentication;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationEnrolException;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationException;

import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthHandler extends SimpleUrlAuthenticationFailureHandler implements AuthenticationFailureHandler
{

    @Override
    public void onAuthenticationFailure( HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception )
        throws
        IOException,
        ServletException
    {
        setUseForward( true );
        saveException( request, exception );

        if ( exception.getClass().equals( CredentialsExpiredException.class ) )
        {
            setDefaultFailureUrl( "/dhis-web-commons/security/expired.action" );
        }
        else if ( exception.getClass().equals( TwoFactorAuthenticationException.class ) )
        {
//???
        }
        else if ( exception.getClass().equals( TwoFactorAuthenticationEnrolException.class ) )
        {
            setDefaultFailureUrl( "/dhis-web-commons/security/enrolTwoFa.action" );

        }
        else
        {
            setDefaultFailureUrl( "/dhis-web-commons/security/login.action?failed=true" );
        }

        super.onAuthenticationFailure( request, response, exception );
    }

}
