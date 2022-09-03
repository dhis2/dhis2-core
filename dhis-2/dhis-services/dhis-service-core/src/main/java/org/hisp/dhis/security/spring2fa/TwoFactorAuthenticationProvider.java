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
import org.apache.commons.validator.routines.LongValidator;
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

        log.debug( String.format( "Login attempt: %s", username ) );

        User user = userService.getUserWithEagerFetchAuthorities( username );
        if ( user == null )
        {
            throw new BadCredentialsException( "Invalid username or password" );
        }

        TwoFactorWebAuthenticationDetails authDetails = (TwoFactorWebAuthenticationDetails) auth.getDetails();
        if ( authDetails == null )
        {
            log.info( "Missing authentication details in authentication request." );
            throw new PreAuthenticatedCredentialsNotFoundException(
                "Missing authentication details in authentication request." );
        }

        if ( securityService.isLocked( username ) )
        {
            String ip = ((TwoFactorWebAuthenticationDetails) auth.getDetails()).getIp();
            log.debug( String.format( "Temporary lockout for user: %s and IP: %s", username, ip ) );
            throw new LockedException( String.format( "IP is temporarily locked: %s", ip ) );
        }

        // Redirect to enrolment page if user has not enrolled in 2FA
        if ( !user.hasTwoFAEnabled() && userService.hasTwoFactorRequirementRole( user ) )
        {
            throw new TwoFactorAuthenticationEnrolmentException( "User must setup two factor authentication" );
        }

        boolean resetApprovalCodeIfPasswordIsCorrect = false;

        // Check 2FA code if user has enrolled in 2FA
        if ( user.hasTwoFAEnabled() )
        {
            String code = StringUtils.deleteWhitespace( authDetails.getCode() );
            boolean validCode = validateTwoFactorCode( code, user );

            if ( !validCode )
            {
                if ( UserService.hasTwoFactorSecretForApproval( user ) )
                {
                    resetApprovalCodeIfPasswordIsCorrect = true;
                }
                else
                {
                    log.info( String.format( "Invalid two factor code for user: %s", username ) );
                    throw new TwoFactorAuthenticationException( "Invalid verification code" );
                }
            }
            else if ( UserService.hasTwoFactorSecretForApproval( user ) )
            {
                // Change from approval to normal 2FA secret
                userService.approveTwoFactorCode( user );
            }
        }

        // -------------------------------------------------------------------------
        // Delegate authentication downstream
        // -------------------------------------------------------------------------
        Authentication result = super.authenticate( auth );

        if ( resetApprovalCodeIfPasswordIsCorrect )
        {
            userService.resetTwoFA( user );
            log.info( String.format( "Invalid two factor code for user: %s", username ) );
            throw new TwoFactorAuthenticationEnrolmentException( "Invalid verification code" );
        }

        return new UsernamePasswordAuthenticationToken( userService.createUserDetails( user ),
            result.getCredentials(),
            result.getAuthorities() );
    }

    private boolean validateTwoFactorCode( String code, User user )
    {
        if ( !user.hasTwoFAEnabled() )
        {
            throw new IllegalStateException( "User has not enrolled in 2FA" );
        }

        if ( !LongValidator.getInstance().isValid( code ) ||
            !TwoFactoryAuthenticationUtils.verify( code,
                user.getSecret() ) )
        {
            log.debug(
                String.format( "Two-factor authentication failure for user: %s", user.getUsername() ) );

            // Failed the 2FA approval, reset so we can restart the process

            return false;
        }

        return true;
    }

    @Override
    public boolean supports( Class<?> authentication )
    {
        return authentication.equals( UsernamePasswordAuthenticationToken.class );
    }
}
