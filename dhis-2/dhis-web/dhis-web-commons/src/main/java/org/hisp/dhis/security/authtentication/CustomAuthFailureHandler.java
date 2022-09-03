package org.hisp.dhis.security.authtentication;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationEnrolmentException;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler
    implements AuthenticationFailureHandler
{
    @Autowired
    private I18nManager i18nManager;

    @Override
    public void onAuthenticationFailure( HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception )
        throws
        IOException
    {
        final String username = request.getParameter( "j_username" );
        final String password = request.getParameter( "j_password" );

        request.getSession().setAttribute( "username", username );

        I18n i18n = i18nManager.getI18n();

        if ( ExceptionUtils.indexOfThrowable( exception, LockedException.class ) != -1 )
        {
            request.getSession()
                .setAttribute( "LOGIN_FAILED_MESSAGE", i18n.getString( "authentication.message.account.locked" ) );
        }
        else
        {
            request.getSession()
                .setAttribute( "LOGIN_FAILED_MESSAGE", i18n.getString( "wrong_username_or_password" ) );
        }

        if ( CredentialsExpiredException.class.equals( exception.getClass() ) )
        {
            getRedirectStrategy().sendRedirect( request, response, "/dhis-web-commons/security/expired.action" );
        }
        else if ( TwoFactorAuthenticationEnrolmentException.class.equals( exception.getClass() ) )
        {
            getRedirectStrategy().sendRedirect( request, response, "/dhis-web-commons/security/enrolTwoFa.action" );
        }
        else if ( TwoFactorAuthenticationException.class.equals( exception.getClass() ) )
        {
            getRedirectStrategy().sendRedirect( request, response, "/dhis-web-commons/security/login.action?twoFactor=true" );
        }
        else
        {
            getRedirectStrategy().sendRedirect( request, response,
                "/dhis-web-commons/security/login.action?failed=true" );
        }
    }
}
