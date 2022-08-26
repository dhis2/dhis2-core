package org.hisp.dhis.security.authtentication;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationEnrolException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthHandler extends SimpleUrlAuthenticationFailureHandler implements AuthenticationFailureHandler
{
    @Autowired
    private I18nManager i18nManager;

    @Override
    public void onAuthenticationFailure( HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception )
        throws
        IOException,
        ServletException
    {
//        setUseForward( true );
//        saveException( request, exception );

        final String username = request.getParameter( "j_username" );

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


        if ( exception.getClass().equals( CredentialsExpiredException.class ) )
        {
//            setDefaultFailureUrl( "/dhis-web-commons/security/expired.action" );
            getRedirectStrategy().sendRedirect(request, response, "/dhis-web-commons/security/expired.action");
        }
//        else if ( exception.getClass().equals( TwoFactorAuthenticationException.class ) )
//        {
////???
//        }
        else if ( exception.getClass().equals( TwoFactorAuthenticationEnrolException.class ) )
        {
//            setDefaultFailureUrl( "/dhis-web-commons/security/enrolTwoFa.action" );
            getRedirectStrategy().sendRedirect(request, response, "/dhis-web-commons/security/enrolTwoFa.action");

        }
        else
        {
//            setDefaultFailureUrl( "/dhis-web-commons/security/login.action?failed=true" );
            getRedirectStrategy().sendRedirect(request, response, "/dhis-web-commons/security/login.action?failed=true");

        }

//        super.onAuthenticationFailure( request, response, exception );
    }

}
