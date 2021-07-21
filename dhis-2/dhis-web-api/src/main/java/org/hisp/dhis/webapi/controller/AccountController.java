/*
 * Copyright (c) 2004-2021, University of Oslo
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

import static org.hisp.dhis.security.DefaultSecurityService.RECOVERY_LOCKOUT_MINS;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.dxf2.webmessage.WebMessageUtils;
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
import org.hisp.dhis.user.PasswordValidationResult;
import org.hisp.dhis.user.PasswordValidationService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = "/account" )
@Slf4j
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

    private final WebMessageService webMessageService;

    private final PasswordValidationService passwordValidationService;

    private final ObjectMapper jsonMapper;

    public AccountController(
        UserService userService,
        TwoFactorAuthenticationProvider authenticationManager,
        ConfigurationService configurationService,
        PasswordManager passwordManager,
        SecurityService securityService,
        SystemSettingManager systemSettingManager,
        WebMessageService webMessageService,
        PasswordValidationService passwordValidationService,
        ObjectMapper jsonMapper )
    {
        this.userService = userService;
        this.twoFactorAuthenticationProvider = authenticationManager;
        this.configurationService = configurationService;
        this.passwordManager = passwordManager;
        this.securityService = securityService;
        this.systemSettingManager = systemSettingManager;
        this.webMessageService = webMessageService;
        this.passwordValidationService = passwordValidationService;
        this.jsonMapper = jsonMapper;
    }

    @PostMapping( "/recovery" )
    public void recoverAccount(
        @RequestParam String username,
        HttpServletRequest request,
        HttpServletResponse response )
        throws WebMessageException
    {
        if ( !systemSettingManager.accountRecoveryEnabled() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Account recovery is not enabled" ) );
        }

        handleRecoveryLock( username );

        UserCredentials credentials = userService.getUserCredentialsByUsername( username );

        if ( credentials == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User does not exist: " + username ) );
        }

        String validRestore = securityService.validateRestore( credentials );

        if ( validRestore != null )
        {
            throw new WebMessageException( WebMessageUtils.error( "Failed to validate recovery attempt" ) );
        }

        if ( !securityService
            .sendRestoreOrInviteMessage( credentials, ContextUtils.getContextPath( request ),
                RestoreOptions.RECOVER_PASSWORD_OPTION ) )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Account could not be recovered" ) );
        }

        log.info( "Recovery message sent for user: " + username );

        webMessageService.send( WebMessageUtils.ok( "Recovery message sent" ), response, request );
    }

    private void handleRecoveryLock( String username )
        throws WebMessageException
    {
        if ( securityService.isRecoveryLocked( username ) )
        {
            throw new WebMessageException( WebMessageUtils
                .forbidden( "The account recovery operation for the given user is temporarily locked due to too " +
                    "many calls to this endpoint in the last '" + RECOVERY_LOCKOUT_MINS + "' minutes. Username:" +
                    username ) );
        }
        else
        {
            securityService.registerRecoveryAttempt( username );
        }
    }

    @PostMapping( "/restore" )
    public void restoreAccount(
        @RequestParam String token,
        @RequestParam String password,
        HttpServletRequest request,
        HttpServletResponse response )
        throws WebMessageException
    {
        String[] idAndRestoreToken = securityService.decodeEncodedTokens( token );
        String idToken = idAndRestoreToken[0];
        String restoreToken = idAndRestoreToken[1];

        UserCredentials credentials = userService.getUserCredentialsByIdToken( idToken );

        if ( credentials == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Account recovery failed" ) );
        }

        if ( !systemSettingManager.accountRecoveryEnabled() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Account recovery is not enabled" ) );
        }

        if ( !ValidationUtils.passwordIsValid( password ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Password is not specified or invalid" ) );
        }

        if ( password.trim().equals( credentials.getUsername() ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Password cannot be equal to username" ) );
        }

        CredentialsInfo credentialsInfo;
        User user = credentials.getUserInfo();

        // if user is null then something is internally wrong and request should
        // be terminated.
        if ( user == null )
        {
            throw new WebMessageException(
                WebMessageUtils.error( String.format( "No user found for username: %s", credentials.getUsername() ) ) );
        }
        else
        {
            credentialsInfo = new CredentialsInfo( credentials.getUsername(), password,
                user.getEmail() != null ? user.getEmail() : "",
                false );
        }

        PasswordValidationResult result = passwordValidationService.validate( credentialsInfo );

        if ( !result.isValid() )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( result.getErrorMessage() ) );
        }

        boolean restoreSuccess = securityService.restore( credentials, restoreToken, password,
            RestoreType.RECOVER_PASSWORD );

        if ( !restoreSuccess )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Account could not be restored" ) );
        }

        log.info( "Account restored for user: " + credentials.getUsername() );

        webMessageService.send( WebMessageUtils.ok( "Account restored" ), response, request );
    }

    @PostMapping
    public void createAccount(
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
        HttpServletRequest request,
        HttpServletResponse response )
        throws WebMessageException,
        IOException
    {
        UserCredentials credentials = null;
        String restoreToken = null;

        boolean invitedByEmail = (inviteUsername != null && !inviteUsername.isEmpty());

        boolean canChooseUsername = true;

        if ( invitedByEmail )
        {
            String[] idAndRestoreToken = securityService.decodeEncodedTokens( inviteToken );

            String idToken = idAndRestoreToken[0];
            restoreToken = idAndRestoreToken[1];

            credentials = userService.getUserCredentialsByIdToken( idToken );

            if ( credentials == null )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Invitation link not valid" ) );
            }

            boolean canRestore = securityService.canRestore( credentials, restoreToken, RestoreType.INVITE );

            if ( !canRestore )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Invitation code not valid" ) );
            }

            RestoreOptions restoreOptions = securityService.getRestoreOptions( restoreToken );

            canChooseUsername = restoreOptions.isUsernameChoice();

            if ( !email.equals( credentials.getUserInfo().getEmail() ) )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Email don't match invited email" ) );
            }
        }
        else
        {
            boolean allowed = configurationService.getConfiguration().selfRegistrationAllowed();

            if ( !allowed )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "User self registration is not allowed" ) );
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
            throw new WebMessageException( WebMessageUtils.badRequest( "User name is not specified or invalid" ) );
        }

        UserCredentials usernameAlreadyTakenCredentials = userService.getUserCredentialsByUsername( username );

        if ( canChooseUsername && usernameAlreadyTakenCredentials != null )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "User name is already taken" ) );
        }

        if ( firstName == null || firstName.trim().length() > MAX_LENGTH )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "First name is not specified or invalid" ) );
        }

        if ( surname == null || surname.trim().length() > MAX_LENGTH )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Last name is not specified or invalid" ) );
        }

        if ( password == null )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Password is not specified" ) );
        }

        PasswordValidationResult result = passwordValidationService.validate( credentialsInfo );

        if ( !result.isValid() )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( result.getErrorMessage() ) );
        }

        if ( email == null || !ValidationUtils.emailIsValid( email ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Email is not specified or invalid" ) );
        }

        if ( phoneNumber == null || phoneNumber.trim().length() > MAX_PHONE_NO_LENGTH )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Phone number is not specified or invalid" ) );
        }

        if ( employer == null || employer.trim().length() > MAX_LENGTH )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Employer is not specified or invalid" ) );
        }

        if ( !systemSettingManager.selfRegistrationNoRecaptcha() )
        {
            if ( recapResponse == null )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Please verify that you are not a robot" ) );
            }

            // ---------------------------------------------------------------------
            // Check result from API, return 500 if validation failed
            // ---------------------------------------------------------------------

            RecaptchaResponse recaptchaResponse = securityService
                .verifyRecaptcha( recapResponse, request.getRemoteAddr() );

            if ( !recaptchaResponse.success() )
            {
                log.warn( "Recaptcha validation failed: " + recaptchaResponse.getErrorCodes() );
                throw new WebMessageException(
                    WebMessageUtils.badRequest( "Recaptcha validation failed: " + recaptchaResponse.getErrorCodes() ) );
            }
        }

        // ---------------------------------------------------------------------
        // Create and save user, return 201
        // ---------------------------------------------------------------------

        if ( invitedByEmail )
        {
            boolean restored = securityService.restore( credentials, restoreToken, password, RestoreType.INVITE );

            if ( !restored )
            {
                log.info( "Invite restore failed for: " + inviteUsername );

                throw new WebMessageException( WebMessageUtils.badRequest( "Unable to create invited user account" ) );
            }

            User user = credentials.getUserInfo();
            user.setFirstName( firstName );
            user.setSurname( surname );
            user.setEmail( email );
            user.setPhoneNumber( phoneNumber );
            user.setEmployer( employer );

            if ( canChooseUsername )
            {
                credentials.setUsername( username );
            }
            else
            {
                username = credentials.getUsername();
            }

            userService.encodeAndSetPassword( credentials, password );

            userService.updateUser( user );
            userService.updateUserCredentials( credentials );

            log.info( "User " + username + " accepted invitation for " + inviteUsername );
        }
        else
        {
            UserAuthorityGroup userRole = configurationService.getConfiguration().getSelfRegistrationRole();
            OrganisationUnit orgUnit = configurationService.getConfiguration().getSelfRegistrationOrgUnit();

            User user = new User();
            user.setFirstName( firstName );
            user.setSurname( surname );
            user.setEmail( email );
            user.setPhoneNumber( phoneNumber );
            user.setEmployer( employer );
            user.getOrganisationUnits().add( orgUnit );
            user.getDataViewOrganisationUnits().add( orgUnit );

            credentials = new UserCredentials();
            credentials.setUsername( username );
            userService.encodeAndSetPassword( credentials, password );
            credentials.setSelfRegistered( true );
            credentials.setUserInfo( user );
            credentials.getUserAuthorityGroups().add( userRole );

            user.setUserCredentials( credentials );

            userService.addUser( user );
            userService.addUserCredentials( credentials );

            log.info( "Created user with username: " + username );
        }

        Set<GrantedAuthority> authorities = getAuthorities( credentials.getUserAuthorityGroups() );

        authenticate( username, password, authorities, request );

        webMessageService.send( WebMessageUtils.ok( "Account created" ), response, request );
    }

    @PostMapping( "/password" )
    public void updatePassword(
        @RequestParam String oldPassword,
        @RequestParam String password,
        HttpServletRequest request,
        HttpServletResponse response )
        throws IOException
    {
        String username = (String) request.getSession().getAttribute( "username" );
        UserCredentials credentials = userService.getUserCredentialsByUsername( username );

        Map<String, String> result = new HashMap<>();
        result.put( "status", "OK" );

        if ( credentials == null )
        {
            result.put( "status", "NON_EXPIRED" );
            result.put( "message", "Username is not valid, redirecting to login." );

            ContextUtils.badRequestResponse( response, jsonMapper.writeValueAsString( result ) );
            return;
        }

        CredentialsInfo credentialsInfo = new CredentialsInfo( credentials.getUsername(), password,
            credentials.getUserInfo().getEmail(), false );

        if ( userService.credentialsNonExpired( credentials ) )
        {
            result.put( "status", "NON_EXPIRED" );
            result.put( "message", "Account is not expired, redirecting to login." );

            ContextUtils.badRequestResponse( response, jsonMapper.writeValueAsString( result ) );
            return;
        }

        if ( !passwordManager.matches( oldPassword, credentials.getPassword() ) )
        {
            result.put( "status", "NON_MATCHING_PASSWORD" );
            result.put( "message", "Old password is wrong, please correct and try again." );

            ContextUtils.badRequestResponse( response, jsonMapper.writeValueAsString( result ) );
            return;
        }

        PasswordValidationResult passwordValidationResult = passwordValidationService.validate( credentialsInfo );

        if ( !passwordValidationResult.isValid() )
        {
            result.put( "status", "PASSWORD_INVALID" );
            result.put( "message", passwordValidationResult.getErrorMessage() );

            ContextUtils.badRequestResponse( response, jsonMapper.writeValueAsString( result ) );
            return;
        }

        if ( password.trim().equals( username.trim() ) )
        {
            result.put( "status", "PASSWORD_EQUAL_TO_USERNAME" );
            result.put( "message", "Password cannot be equal to username" );

            ContextUtils.badRequestResponse( response, jsonMapper.writeValueAsString( result ) );
            return;
        }

        userService.encodeAndSetPassword( credentials, password );
        userService.updateUserCredentials( credentials );

        authenticate( username, password, getAuthorities( credentials.getUserAuthorityGroups() ), request );

        result.put( "message", "Account was updated." );

        ContextUtils.okResponse( response, jsonMapper.writeValueAsString( result ) );
    }

    @GetMapping( "/username" )
    public void validateUserNameGet( @RequestParam String username, HttpServletResponse response )
        throws IOException
    {
        Map<String, String> result = validateUserName( username );

        ContextUtils.okResponse( response, jsonMapper.writeValueAsString( result ) );
    }

    @PostMapping( "/validateUsername" )
    public void validateUserNameGetPost( @RequestParam String username, HttpServletResponse response )
        throws IOException
    {
        Map<String, String> result = validateUserName( username );

        ContextUtils.okResponse( response, jsonMapper.writeValueAsString( result ) );
    }

    @GetMapping( "/password" )
    public void validatePasswordGet( @RequestParam String password, HttpServletResponse response )
        throws IOException
    {
        Map<String, String> result = validatePassword( password );

        ContextUtils.okResponse( response, jsonMapper.writeValueAsString( result ) );
    }

    @PostMapping( "/validatePassword" )
    public void validatePasswordPost( @RequestParam String password, HttpServletResponse response )
        throws IOException
    {
        Map<String, String> result = validatePassword( password );

        ContextUtils.okResponse( response, jsonMapper.writeValueAsString( result ) );
    }

    // ---------------------------------------------------------------------
    // Supportive methods
    // ---------------------------------------------------------------------

    private Map<String, String> validateUserName( String username )
    {
        boolean valid = username != null && userService.getUserCredentialsByUsername( username ) == null;

        // Custom code required because of our hacked jQuery validation

        Map<String, String> result = new HashMap<>();

        result.put( "response", valid ? "success" : "error" );
        result.put( "message", valid ? "" : "Username is already taken" );

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

    private Set<GrantedAuthority> getAuthorities( Set<UserAuthorityGroup> userRoles )
    {
        Set<GrantedAuthority> auths = new HashSet<>();

        for ( UserAuthorityGroup userRole : userRoles )
        {
            auths.addAll( getAuthorities( userRole ) );
        }

        return auths;
    }

    private Set<GrantedAuthority> getAuthorities( UserAuthorityGroup userRole )
    {
        Set<GrantedAuthority> auths = new HashSet<>();

        for ( String auth : userRole.getAuthorities() )
        {
            auths.add( new SimpleGrantedAuthority( auth ) );
        }

        return auths;
    }
}
