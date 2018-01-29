package org.hisp.dhis.security;

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
