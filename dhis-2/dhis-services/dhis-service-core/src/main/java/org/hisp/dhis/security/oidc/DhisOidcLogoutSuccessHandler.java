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
package org.hisp.dhis.security.oidc;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.hisp.dhis.external.conf.ConfigurationKey.LINKED_ACCOUNTS_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.LINKED_ACCOUNTS_RELOGIN_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_LOGOUT_REDIRECT_URL;
import static org.hisp.dhis.external.conf.ConfigurationKey.OIDC_OAUTH2_LOGIN_ENABLED;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
@RequiredArgsConstructor
public class DhisOidcLogoutSuccessHandler implements LogoutSuccessHandler
{
    private final DhisOidcProviderRepository dhisOidcProviderRepository;

    private final DhisConfigurationProvider config;

    private final UserService userService;

    private SimpleUrlLogoutSuccessHandler handler;

    @PostConstruct
    public void init()
    {
        if ( config.isEnabled( OIDC_OAUTH2_LOGIN_ENABLED ) )
        {
            setOidcLogoutUrl();
        }
    }

    private void setOidcLogoutUrl()
    {
        String logoutUri = config.getProperty( OIDC_LOGOUT_REDIRECT_URL );

        if ( config.isEnabled( LINKED_ACCOUNTS_ENABLED ) )
        {
            this.handler = new SimpleUrlLogoutSuccessHandler();
            if ( !isNullOrEmpty( logoutUri ) )
            {
                this.handler.setDefaultTargetUrl( logoutUri );
            }
        }
        else
        {
            OidcClientInitiatedLogoutSuccessHandler oidcHandler = new OidcClientInitiatedLogoutSuccessHandler(
                dhisOidcProviderRepository );
            oidcHandler.setPostLogoutRedirectUri( logoutUri );
            this.handler = oidcHandler;
            this.handler.setDefaultTargetUrl( logoutUri );
        }
    }

    @Override
    public void onLogoutSuccess( HttpServletRequest request, HttpServletResponse response,
        Authentication authentication )
        throws IOException,
        ServletException
    {
        if ( config.isEnabled( OIDC_OAUTH2_LOGIN_ENABLED ) )
        {
            boolean linkedAccountEnabled = config.isEnabled( LINKED_ACCOUNTS_ENABLED );
            if ( linkedAccountEnabled )
            {
                handleLinkedAccountsLogout( request, response, authentication );
            }
            else
            {
                handler.onLogoutSuccess( request, response, authentication );
            }
        }
    }

    private void handleLinkedAccountsLogout( HttpServletRequest request, HttpServletResponse response,
        Authentication authentication )
        throws IOException,
        ServletException
    {
        String currentUsername = request.getParameter( "current" );
        String usernameToSwitchTo = request.getParameter( "switch" );

        if ( isNullOrEmpty( currentUsername ) || isNullOrEmpty( usernameToSwitchTo ) )
        {
            setOidcLogoutUrl();
        }
        else
        {
            User currentUser = userService.getUserByUsername( currentUsername );
            userService.setActiveLinkedAccounts( currentUser, usernameToSwitchTo );

            this.handler.setDefaultTargetUrl( config.getProperty( LINKED_ACCOUNTS_RELOGIN_URL ) );
        }

        handler.onLogoutSuccess( request, response, authentication );
    }
}
