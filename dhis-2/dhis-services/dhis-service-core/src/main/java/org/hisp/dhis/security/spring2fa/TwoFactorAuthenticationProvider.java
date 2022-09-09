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
package org.hisp.dhis.security.spring2fa;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.security.ForwardedIpAwareWebAuthenticationDetails;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.TwoFactoryAuthenticationUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;
import org.springframework.stereotype.Component;

/**
 * @author Henning Håkonsen
 */
@Slf4j
@Component
public class TwoFactorAuthenticationProvider extends DaoAuthenticationProvider
{
    @Autowired
    private UserService userService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    public TwoFactorAuthenticationProvider( @Qualifier( "userDetailsService" ) UserDetailsService detailsService,
        PasswordEncoder passwordEncoder )
    {
        setUserDetailsService( detailsService );
        setPasswordEncoder( passwordEncoder );
    }

    @Override
    public Authentication authenticate( Authentication auth )
        throws AuthenticationException
    {
        String username = auth.getName();
        ForwardedIpAwareWebAuthenticationDetails details = (ForwardedIpAwareWebAuthenticationDetails) auth.getDetails();

        log.debug( String.format( "Login attempt: %s", username ) );

        // If enabled, temporarily block user with too many failed attempts
        if ( securityService.isLocked( username ) )
        {
            String ip = details.getIp();
            log.debug( String.format( "Temporary lockout for user: %s and IP: %s", username, ip ) );
            throw new LockedException( String.format( "IP is temporarily locked: %s", ip ) );
        }

        Authentication result = super.authenticate( auth );

        User user = userService.getUserWithEagerFetchAuthorities( username );
        if ( user == null )
        {
            log.info( "Invalid username; username={}", username );
            throw new BadCredentialsException( "Invalid username or password" );
        }

        validateTwoFactor( user, details );

        return new UsernamePasswordAuthenticationToken( userService.createUserDetails( user ),
            result.getCredentials(),
            result.getAuthorities() );
    }

    private void validateTwoFactor( User user, ForwardedIpAwareWebAuthenticationDetails details )
    {
        // If user has 2FA enabled and tries to authenticate with HTTP Basic
        if ( user.isTwoFactorEnabled() && !(details instanceof TwoFactorWebAuthenticationDetails) )
        {
            log.debug( "User has 2FA enabled, but tried to authenticate with HTTP Basic; username={}",
                user.getUsername() );
            throw new PreAuthenticatedCredentialsNotFoundException(
                "User has 2FA enabled, but tried to authenticate with HTTP Basic; username=" + user.getUsername() );
        }

        // If user require 2FA, and it's not enabled/provisioned, redirect to
        // the enrolment page,
        // (via the CustomAuthFailureHandler)
        if ( !user.isTwoFactorEnabled() && userService.hasTwoFactorRoleRestriction( user ) )
        {
            throw new TwoFactorAuthenticationEnrolmentException( "User must setup two factor authentication" );
        }

        if ( user.isTwoFactorEnabled() )
        {
            TwoFactorWebAuthenticationDetails authDetails = (TwoFactorWebAuthenticationDetails) details;
            if ( authDetails == null )
            {
                log.info( "Missing authentication details in authentication request." );
                throw new PreAuthenticatedCredentialsNotFoundException(
                    "Missing authentication details in authentication request." );
            }

            validateCode( StringUtils.deleteWhitespace( authDetails.getCode() ), user );
        }
    }

    private void validateCode( String code, User user )
    {
        code = StringUtils.deleteWhitespace( code );

        if ( !TwoFactoryAuthenticationUtils.verify( code, user.getSecret() ) )
        {
            log.debug( String.format( "Two-factor authentication failure for user: %s", user.getUsername() ) );

            if ( UserService.hasTwoFactorSecretForApproval( user ) )
            {
                userService.resetTwoFa( user );
                throw new TwoFactorAuthenticationEnrolmentException( "Invalid verification code" );
            }
            else
            {
                throw new TwoFactorAuthenticationException( "Invalid verification code" );
            }
        }
        else if ( UserService.hasTwoFactorSecretForApproval( user ) )
        {
            userService.approveTwoFactorSecret( user );
        }
    }

    @Override
    public boolean supports( Class<?> authentication )
    {
        return authentication.equals( UsernamePasswordAuthenticationToken.class );
    }
}
