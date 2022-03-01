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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.error;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.ok;
import static org.hisp.dhis.security.DefaultSecurityService.RECOVERY_LOCKOUT_MINS;
import static org.springframework.http.CacheControl.noStore;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dxf2.webmessage.WebMessage;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.RecaptchaResponse;
import org.hisp.dhis.security.RestoreOptions;
import org.hisp.dhis.security.RestoreType;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.security.spring2fa.TwoFactorAuthenticationProvider;
import org.hisp.dhis.security.spring2fa.TwoFactorWebAuthenticationDetails;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.CredentialsInfo;
import org.hisp.dhis.user.CurrentUser;
import org.hisp.dhis.user.PasswordValidationResult;
import org.hisp.dhis.user.PasswordValidationService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = "/account" )
@Slf4j
@AllArgsConstructor
@ApiVersion( { DhisApiVersion.DEFAULT, DhisApiVersion.ALL } )
public class AccountController
{
    private static final int MAX_LENGTH = 80;

    private static final int MAX_PHONE_NO_LENGTH = 30;

    private final UserService userService;

    private final TwoFactorAuthenticationProvider twoFactorAuthenticationProvider;

    private final ConfigurationService configurationService;

    private final PasswordManager passwordManager;

    private final SecurityService securityService;

    private final SystemSettingManager systemSettingManager;

    private final PasswordValidationService passwordValidationService;

    @PostMapping( "/recovery" )
    @ResponseBody
    public WebMessage recoverAccount( @RequestParam String username,
        HttpServletRequest request )
        throws WebMessageException
    {
        if ( !systemSettingManager.accountRecoveryEnabled() )
        {
            return conflict( "Account recovery is not enabled" );
        }

        handleRecoveryLock( username );

        User user = userService.getUserByUsername( username );

        if ( user == null )
        {
            return conflict( "User does not exist: " + username );
        }

        String validRestore = securityService.validateRestore( user );

        if ( validRestore != null )
        {
            return error( "Failed to validate recovery attempt" );
        }

        if ( !securityService
            .sendRestoreOrInviteMessage( user, ContextUtils.getContextPath( request ),
                RestoreOptions.RECOVER_PASSWORD_OPTION ) )
        {
            return conflict( "Account could not be recovered" );
        }

        log.info( "Recovery message sent for user: " + username );

        return ok( "Recovery message sent" );
    }

    private void handleRecoveryLock( String username )
        throws WebMessageException
    {
        if ( securityService.isRecoveryLocked( username ) )
        {
            throw new WebMessageException(
                forbidden( "The account recovery operation for the given user is temporarily locked due to too " +
                    "many calls to this endpoint in the last '" + RECOVERY_LOCKOUT_MINS + "' minutes. Username:" +
                    username ) );
        }
        else
        {
            securityService.registerRecoveryAttempt( username );
        }
    }

    @PostMapping( "/restore" )
    @ResponseBody
    public WebMessage restoreAccount( @RequestParam String token, @RequestParam String password )
    {
        String[] idAndRestoreToken = securityService.decodeEncodedTokens( token );
        String idToken = idAndRestoreToken[0];

        User user = userService.getUserByIdToken( idToken );
        if ( user == null || idAndRestoreToken.length < 2 )
        {
            return conflict( "Account recovery failed" );
        }

        String restoreToken = idAndRestoreToken[1];

        if ( !systemSettingManager.accountRecoveryEnabled() )
        {
            return conflict( "Account recovery is not enabled" );
        }

        if ( !ValidationUtils.passwordIsValid( password ) )
        {
            return badRequest( "Password is not specified or invalid" );
        }

        if ( password.trim().equals( user.getUsername() ) )
        {
            return badRequest( "Password cannot be equal to username" );
        }

        CredentialsInfo credentialsInfo = new CredentialsInfo( user.getUsername(), password,
            user.getEmail() != null ? user.getEmail() : "", false );

        PasswordValidationResult result = passwordValidationService.validate( credentialsInfo );

        if ( !result.isValid() )
        {
            return badRequest( result.getErrorMessage() );
        }

        boolean restoreSuccess = securityService.restore( user, restoreToken, password,
            RestoreType.RECOVER_PASSWORD );

        if ( !restoreSuccess )
        {
            return badRequest( "Account could not be restored" );
        }

        log.info( "Account restored for user: " + user.getUsername() );

        return ok( "Account restored" );
    }

    @PostMapping
    @ResponseBody
    public WebMessage createAccount(
        @RequestParam String username,
        @RequestParam String firstName,
        @RequestParam String surname,
        @RequestParam String password,
        @RequestParam String email,
        @RequestParam String phoneNumber,
        @RequestParam String employer,
        @RequestParam( required = false ) String inviteUsername,
        @RequestParam( required = false ) String inviteToken,
        @RequestParam( value = "g-recaptcha-response", required = false ) String recapResponse,
        HttpServletRequest request )
        throws IOException
    {
        User user = null;
        String restoreToken = null;

        boolean invitedByEmail = (inviteUsername != null && !inviteUsername.isEmpty());

        boolean canChooseUsername = true;

        if ( invitedByEmail )
        {
            String[] idAndRestoreToken = securityService.decodeEncodedTokens( inviteToken );

            String idToken = idAndRestoreToken[0];
            restoreToken = idAndRestoreToken[1];

            user = userService.getUserByIdToken( idToken );

            if ( user == null )
            {
                return badRequest( "Invitation link not valid" );
            }

            boolean canRestore = securityService.canRestore( user, restoreToken, RestoreType.INVITE );

            if ( !canRestore )
            {
                return badRequest( "Invitation code not valid" );
            }

            RestoreOptions restoreOptions = securityService.getRestoreOptions( restoreToken );

            canChooseUsername = restoreOptions.isUsernameChoice();

            if ( !email.equals( user.getEmail() ) )
            {
                return badRequest( "Email don't match invited email" );
            }
        }
        else
        {
            boolean allowed = configurationService.getConfiguration().selfRegistrationAllowed();

            if ( !allowed )
            {
                return badRequest( "User self registration is not allowed" );
            }
        }

        // ---------------------------------------------------------------------
        // Trim input
        // ---------------------------------------------------------------------

        username = StringUtils.trimToNull( username );
        firstName = StringUtils.trimToNull( firstName );
        surname = StringUtils.trimToNull( surname );
        password = StringUtils.trimToNull( password );
        email = StringUtils.trimToNull( email );
        phoneNumber = StringUtils.trimToNull( phoneNumber );
        employer = StringUtils.trimToNull( employer );
        recapResponse = StringUtils.trimToNull( recapResponse );

        CredentialsInfo credentialsInfo = new CredentialsInfo( username, password, email, true );

        // ---------------------------------------------------------------------
        // Validate input, return 400 if invalid
        // ---------------------------------------------------------------------

        if ( username == null || username.trim().length() > MAX_LENGTH )
        {
            return badRequest( "User name is not specified or invalid" );
        }

        User usernameAlreadyTakenCredentials = userService.getUserByUsername( username );

        if ( canChooseUsername && usernameAlreadyTakenCredentials != null )
        {
            return badRequest( "User name is already taken" );
        }

        if ( firstName == null || firstName.trim().length() > MAX_LENGTH )
        {
            return badRequest( "First name is not specified or invalid" );
        }

        if ( surname == null || surname.trim().length() > MAX_LENGTH )
        {
            return badRequest( "Last name is not specified or invalid" );
        }

        if ( password == null )
        {
            return badRequest( "Password is not specified" );
        }

        PasswordValidationResult result = passwordValidationService.validate( credentialsInfo );

        if ( !result.isValid() )
        {
            return badRequest( result.getErrorMessage() );
        }

        if ( email == null || !ValidationUtils.emailIsValid( email ) )
        {
            return badRequest( "Email is not specified or invalid" );
        }

        if ( phoneNumber == null || phoneNumber.trim().length() > MAX_PHONE_NO_LENGTH )
        {
            return badRequest( "Phone number is not specified or invalid" );
        }

        if ( employer == null || employer.trim().length() > MAX_LENGTH )
        {
            return badRequest( "Employer is not specified or invalid" );
        }

        if ( !systemSettingManager.selfRegistrationNoRecaptcha() )
        {
            if ( recapResponse == null )
            {
                return badRequest( "Please verify that you are not a robot" );
            }

            // ---------------------------------------------------------------------
            // Check result from API, return 500 if validation failed
            // ---------------------------------------------------------------------

            RecaptchaResponse recaptchaResponse = securityService
                .verifyRecaptcha( recapResponse, request.getRemoteAddr() );

            if ( !recaptchaResponse.success() )
            {
                log.warn( "Recaptcha validation failed: " + recaptchaResponse.getErrorCodes() );
                return badRequest( "Recaptcha validation failed: " + recaptchaResponse.getErrorCodes() );
            }
        }

        // ---------------------------------------------------------------------
        // Create and save user, return 201
        // ---------------------------------------------------------------------

        if ( invitedByEmail )
        {
            boolean restored = securityService.restore( user, restoreToken, password, RestoreType.INVITE );

            if ( !restored )
            {
                log.info( "Invite restore failed for: " + inviteUsername );

                return badRequest( "Unable to create invited user account" );
            }

            user = new User();
            user.setFirstName( firstName );
            user.setSurname( surname );
            user.setEmail( email );
            user.setPhoneNumber( phoneNumber );
            user.setEmployer( employer );

            if ( canChooseUsername )
            {
                user.setUsername( username );
            }
            else
            {
                username = user.getUsername();
            }

            userService.encodeAndSetPassword( user, password );

            userService.updateUser( user );

            log.info( "User " + username + " accepted invitation for " + inviteUsername );
        }
        else
        {
            UserRole userRole = configurationService.getConfiguration().getSelfRegistrationRole();
            OrganisationUnit orgUnit = configurationService.getConfiguration().getSelfRegistrationOrgUnit();

            user = new User();
            user.setFirstName( firstName );
            user.setSurname( surname );
            user.setEmail( email );
            user.setPhoneNumber( phoneNumber );
            user.setEmployer( employer );
            user.getOrganisationUnits().add( orgUnit );
            user.getDataViewOrganisationUnits().add( orgUnit );
            user.setUsername( username );

            userService.encodeAndSetPassword( user, password );

            user.setSelfRegistered( true );
            user.getUserRoles().add( userRole );

            userService.addUser( user );

            log.info( "Created user with username: " + username );
        }

        Set<GrantedAuthority> authorities = getAuthorities( user.getUserRoles() );

        authenticate( username, password, authorities, request );

        return ok( "Account created" );
    }

    @PostMapping( "/password" )
    public ResponseEntity<Map<String, String>> updatePassword(
        @RequestParam String oldPassword,
        @RequestParam String password,
        @CurrentUser User user,
        HttpServletRequest request )
    {
        Map<String, String> result = new HashMap<>();

        String username = user.getUsername();
        if ( username == null )
        {
            result.put( "status", "NON_EXPIRED" );
            result.put( "message", "Username is not valid, redirecting to login." );

            return ResponseEntity.badRequest().cacheControl( noStore() ).body( result );
        }

        CredentialsInfo credentialsInfo = new CredentialsInfo( user.getUsername(), password,
            user.getEmail(), false );

        if ( userService.userNonExpired( user ) )
        {
            result.put( "status", "NON_EXPIRED" );
            result.put( "message", "Account is not expired, redirecting to login." );

            return ResponseEntity.badRequest().cacheControl( noStore() ).body( result );
        }

        if ( !passwordManager.matches( oldPassword, user.getPassword() ) )
        {
            result.put( "status", "NON_MATCHING_PASSWORD" );
            result.put( "message", "Old password is wrong, please correct and try again." );

            return ResponseEntity.badRequest().cacheControl( noStore() ).body( result );
        }

        PasswordValidationResult passwordValidationResult = passwordValidationService.validate( credentialsInfo );

        if ( !passwordValidationResult.isValid() )
        {
            result.put( "status", "PASSWORD_INVALID" );
            result.put( "message", passwordValidationResult.getErrorMessage() );

            return ResponseEntity.badRequest().cacheControl( noStore() ).body( result );
        }

        if ( password.trim().equals( username.trim() ) )
        {
            result.put( "status", "PASSWORD_EQUAL_TO_USERNAME" );
            result.put( "message", "Password cannot be equal to username" );

            return ResponseEntity.badRequest().cacheControl( noStore() ).body( result );
        }

        userService.encodeAndSetPassword( user, password );
        userService.updateUser( user );

        authenticate( username, password, getAuthorities( user.getUserRoles() ), request );

        result.put( "status", "OK" );
        result.put( "message", "Account was updated." );

        return ResponseEntity.ok().cacheControl( noStore() ).body( result );
    }

    @GetMapping( "/username" )
    public ResponseEntity<Map<String, String>> validateUserNameGet( @RequestParam String username )
    {
        return ResponseEntity.ok().cacheControl( noStore() ).body( validateUserName( username ) );
    }

    @PostMapping( "/validateUsername" )
    public ResponseEntity<Map<String, String>> validateUserNameGetPost( @RequestParam String username )
    {
        return ResponseEntity.ok().cacheControl( noStore() ).body( validateUserName( username ) );
    }

    @GetMapping( "/password" )
    public ResponseEntity<Map<String, String>> validatePasswordGet( @RequestParam String password )
    {
        return ResponseEntity.ok().cacheControl( noStore() ).body( validatePassword( password ) );
    }

    @PostMapping( "/validatePassword" )
    public ResponseEntity<Map<String, String>> validatePasswordPost( @RequestParam String password,
        HttpServletResponse response )
    {
        return ResponseEntity.ok().cacheControl( noStore() ).body( validatePassword( password ) );
    }

    // ---------------------------------------------------------------------
    // Supportive methods
    // ---------------------------------------------------------------------

    private Map<String, String> validateUserName( String username )
    {
        boolean isNull = username == null;
        boolean usernameNotTaken = userService.getUserByUsername( username ) == null;
        boolean isValidSyntax = ValidationUtils.usernameIsValid( username );
        boolean isValid = !isNull && usernameNotTaken && isValidSyntax;

        // Custom code required because of our hacked jQuery validation
        Map<String, String> result = new HashMap<>();

        result.put( "response", isValid ? "success" : "error" );

        if ( isNull )
        {
            result.put( "message", "Username is null" );
        }
        else if ( !isValidSyntax )
        {
            result.put( "message", "Username is not valid" );
        }
        else if ( !usernameNotTaken )
        {
            result.put( "message", "Username is already taken" );
        }
        else
        {
            result.put( "message", "" );
        }

        return result;
    }

    private Map<String, String> validatePassword( String password )
    {
        CredentialsInfo credentialsInfo = new CredentialsInfo( password, true );

        PasswordValidationResult passwordValidationResult = passwordValidationService.validate( credentialsInfo );

        // Custom code required because of our hacked jQuery validation

        Map<String, String> result = new HashMap<>();

        result.put( "response", passwordValidationResult.isValid() ? "success" : "error" );
        result.put( "message", passwordValidationResult.isValid() ? "" : passwordValidationResult.getErrorMessage() );

        return result;
    }

    private void authenticate( String username, String rawPassword, Collection<GrantedAuthority> authorities,
        HttpServletRequest request )
    {
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken( username, rawPassword,
            authorities );
        token.setDetails( new TwoFactorWebAuthenticationDetails( request ) );

        Authentication auth = twoFactorAuthenticationProvider.authenticate( token );

        SecurityContextHolder.getContext().setAuthentication( auth );

        HttpSession session = request.getSession();

        session.setAttribute( "SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext() );
    }

    private Set<GrantedAuthority> getAuthorities( Set<UserRole> userRoles )
    {
        Set<GrantedAuthority> auths = new HashSet<>();

        for ( UserRole userRole : userRoles )
        {
            auths.addAll( getAuthorities( userRole ) );
        }

        return auths;
    }

    private Set<GrantedAuthority> getAuthorities( UserRole userRole )
    {
        Set<GrantedAuthority> auths = new HashSet<>();

        for ( String auth : userRole.getAuthorities() )
        {
            auths.add( new SimpleGrantedAuthority( auth ) );
        }

        return auths;
    }
}
