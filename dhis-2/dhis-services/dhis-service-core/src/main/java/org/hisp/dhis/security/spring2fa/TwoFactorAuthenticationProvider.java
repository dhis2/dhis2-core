package org.hisp.dhis.security.spring2fa;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.LongValidator;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.SecurityUtils;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedCredentialsNotFoundException;

/**
 * @author Henning Håkonsen
 */
public class TwoFactorAuthenticationProvider
    extends DaoAuthenticationProvider
{
    private static final Logger log = LoggerFactory.getLogger( TwoFactorAuthenticationProvider.class );

    private UserService userService;

    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    private SecurityService securityService;

    public void setSecurityService( SecurityService securityService )
    {
        this.securityService = securityService;
    }

    public void setPasswordEncoder( PasswordEncoder passwordEncoder )
    {
        super.setPasswordEncoder( passwordEncoder );
    }

    @Override
    public Authentication authenticate( Authentication auth )
        throws AuthenticationException
    {
        log.info( String.format( "Login attempt: %s", auth.getName() ) );

        String username = auth.getName();

        UserCredentials userCredentials = userService.getUserCredentialsByUsername( username );

        if ( userCredentials == null )
        {
            throw new BadCredentialsException( "Invalid username or password" );
        }

        // -------------------------------------------------------------------------
        // Check two-factor authentication
        // -------------------------------------------------------------------------

        if ( userCredentials.isTwoFA() )
        {
            TwoFactorWebAuthenticationDetails authDetails =
                (TwoFactorWebAuthenticationDetails) auth.getDetails();

            String code = StringUtils.deleteWhitespace( authDetails.getCode() );

            // -------------------------------------------------------------------------
            // Check whether account is locked due to multiple failed login attempts
            // -------------------------------------------------------------------------

            if ( authDetails == null )
            {
                log.info( "Missing authentication details in authentication request." );
                throw new PreAuthenticatedCredentialsNotFoundException( "Missing authentication details in authentication request." );
            }

            String ip = authDetails.getIp();

            if ( securityService.isLocked( ip ) )
            {
                log.info( String.format( "Temporary lockout for user: %s and IP: %s", username, ip ) );

                throw new LockedException( String.format( "IP is temporarily locked: %s", ip ) );
            }

            if ( !LongValidator.getInstance().isValid( code ) || !SecurityUtils.verify( userCredentials, code ) )
            {
                log.info( String.format( "Two-factor authentication failure for user: %s", userCredentials.getUsername() ) );

                throw new BadCredentialsException( "Invalid verification code" );
            }
        }

        // -------------------------------------------------------------------------
        // Delegate authentication downstream, using UserCredentials as principal
        // -------------------------------------------------------------------------

        Authentication result = super.authenticate( auth );

        return new UsernamePasswordAuthenticationToken( userCredentials, result.getCredentials(), result.getAuthorities() );
    }

    @Override
    public boolean supports( Class<?> authentication )
    {
        return authentication.equals( UsernamePasswordAuthenticationToken.class );
    }
}