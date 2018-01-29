package org.hisp.dhis.security.spring2fa;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.LongValidator;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.SecurityUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author Henning Håkonsen
 * @author Lars Helge Øverland
 */
@Component
public class TwoFactorAuthenticationProvider
    extends DaoAuthenticationProvider
{
    private static final Logger log = LoggerFactory.getLogger( TwoFactorAuthenticationProvider.class );

    private UserService userService;

    private SecurityService securityService;

    @Autowired
    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }

    @Autowired
    public void setSecurityService( SecurityService securityService )
    {
        this.securityService = securityService;
    }

    @Autowired
    public void setPasswordEncoder( PasswordEncoder passwordEncoder )
    {
        super.setPasswordEncoder( passwordEncoder );
    }

    @Override
    public Authentication authenticate( Authentication auth )
        throws AuthenticationException
    {
        log.debug( String.format( "Login attempt: %s", auth.getName() ) );

        TwoFactorWebAuthenticationDetails authDetails =
            (TwoFactorWebAuthenticationDetails) auth.getDetails();

        // -------------------------------------------------------------------------
        // Check whether account is locked due to multiple failed login attempts
        // -------------------------------------------------------------------------

        String username = auth.getName();
        String ip = authDetails.getIp();

        if ( securityService.isLocked( ip ) )
        {
            log.info( String.format( "Temporary lockout for user: %s and IP: %s", username, ip ) );

            throw new LockedException( String.format( "IP is temporarily locked: %s", ip ) );
        }

        // -------------------------------------------------------------------------
        // Check two-factor authentication
        // -------------------------------------------------------------------------

        String code = StringUtils.deleteWhitespace( authDetails.getCode() );

        User user = userService.getUserCredentialsByUsername( username ).getUser();

        if ( user == null )
        {
            throw new BadCredentialsException( "Invalid username or password" );
        }

        if ( user.isIs2FA() )
        {
            if ( !LongValidator.getInstance().isValid( code ) || !SecurityUtils.verify( user, code ) )
            {
                log.info( String.format( "Two-factor authentication failure for user: %s", user.getUsername() ) );

                throw new BadCredentialsException( "Invalid verification code" );
            }
        }

        // -------------------------------------------------------------------------
        // Delegate authentication downstream, using UserDetails as principal
        // -------------------------------------------------------------------------

        // HH correct?
        UserDetails principal = (UserDetails) auth.getPrincipal();

        Authentication result = super.authenticate( auth );

        return new UsernamePasswordAuthenticationToken( principal, result.getCredentials(), result.getAuthorities() );
    }

    @Override
    public boolean supports( Class<?> authentication )
    {
        return authentication.equals( UsernamePasswordAuthenticationToken.class );
    }
}