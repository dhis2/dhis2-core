package org.hisp.dhis.security;

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

import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * @author Henning Håkonsen
 */
@Component
public class AuthenticationListener2FA
{
    private static final Logger log = LoggerFactory.getLogger( AuthenticationListener.class );

    @Autowired
    private SecurityService securityService;

    @EventListener
    public void handleAuthenticationFailure( AuthenticationFailureBadCredentialsEvent event )
    {
        Authentication auth = event.getAuthentication();

        TwoFactorWebAuthenticationDetails authDetails =
            (TwoFactorWebAuthenticationDetails) auth.getDetails();

        log.info( String.format( "Login attempt failed for remote IP: %s", authDetails.getIp() ) );

        securityService.registerFailedLogin( authDetails.getIp() );
    }

    @EventListener
    public void handleAuthenticationSuccess( AuthenticationSuccessEvent event )
    {
        Authentication auth = event.getAuthentication();

        TwoFactorWebAuthenticationDetails authDetails =
            (TwoFactorWebAuthenticationDetails) auth.getDetails();

        log.debug( String.format( "Login attempt succeeded for remote IP: %s", authDetails.getIp() ) );

        securityService.registerSuccessfulLogin( authDetails.getIp() );
    }
}
