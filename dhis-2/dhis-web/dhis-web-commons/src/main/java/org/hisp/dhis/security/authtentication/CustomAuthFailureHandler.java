/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.security.authtentication;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationEnrolmentException;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler
    implements AuthenticationFailureHandler
{
    @Autowired
    private I18nManager i18nManager;

    @Override
    public void onAuthenticationFailure( HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception )
        throws IOException
    {
        request.getSession().setAttribute( "username", request.getParameter( "j_username" ) );

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
            getRedirectStrategy().sendRedirect( request, response,
                "/dhis-web-commons/security/login.action?twoFactor=true" );
        }
        else
        {
            getRedirectStrategy().sendRedirect( request, response,
                "/dhis-web-commons/security/login.action?failed=true" );
        }
    }
}
