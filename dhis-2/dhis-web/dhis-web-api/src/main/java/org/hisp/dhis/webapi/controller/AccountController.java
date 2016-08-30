package org.hisp.dhis.webapi.controller;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.RestoreOptions;
import org.hisp.dhis.security.RestoreType;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.WebMessageService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.WebMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Lars Helge Overland
 */
@Controller
@RequestMapping( value = "/account" )
@ApiVersion( { ApiVersion.Version.DEFAULT, ApiVersion.Version.ALL } )
public class AccountController
{
    private static final Log log = LogFactory.getLog( AccountController.class );

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/verify";
    protected static final String PUB_KEY = "6LcM6tcSAAAAANwYsFp--0SYtcnze_WdYn8XwMMk";
    private static final String KEY = "6LcM6tcSAAAAAFnHo1f3lLstk3rZv3EVinNROfRq";
    private static final String TRUE = "true";
    private static final String SPLIT = "\n";
    private static final int MAX_LENGTH = 80;
    private static final int MAX_PHONE_NO_LENGTH = 30;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private PasswordManager passwordManager;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private SystemSettingManager systemSettingManager;

    @Autowired
    private WebMessageService webMessageService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @RequestMapping( value = "/recovery", method = RequestMethod.POST )
    public void recoverAccount(
        @RequestParam String username,
        HttpServletRequest request,
        HttpServletResponse response ) throws WebMessageException
    {
        String rootPath = ContextUtils.getContextPath( request );

        if ( !systemSettingManager.accountRecoveryEnabled() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Account recovery is not enabled" ) );
        }

        UserCredentials credentials = userService.getUserCredentialsByUsername( username );

        if ( credentials == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User does not exist: " + username ) );
        }

        boolean recover = securityService.sendRestoreMessage( credentials, rootPath, RestoreOptions.RECOVER_PASSWORD_OPTION );

        if ( !recover )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Account could not be recovered" ) );
        }

        log.info( "Recovery message sent for user: " + username );

        webMessageService.send( WebMessageUtils.ok( "Recovery message sent" ), response, request );
    }

    @RequestMapping( value = "/restore", method = RequestMethod.POST )
    public void restoreAccount(
        @RequestParam String username,
        @RequestParam String token,
        @RequestParam String code,
        @RequestParam String password,
        HttpServletRequest request,
        HttpServletResponse response ) throws WebMessageException
    {
        if ( !systemSettingManager.accountRecoveryEnabled() )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "Account recovery is not enabled" ) );
        }

        if ( password == null || !ValidationUtils.passwordIsValid( password ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Password is not specified or invalid" ) );
        }

        if ( password.trim().equals( username.trim() ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Password cannot be equal to username" ) );
        }

        UserCredentials credentials = userService.getUserCredentialsByUsername( username );

        if ( credentials == null )
        {
            throw new WebMessageException( WebMessageUtils.conflict( "User does not exist: " + username ) );
        }

        boolean restore = securityService.restore( credentials, token, code, password, RestoreType.RECOVER_PASSWORD );

        if ( !restore )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Account could not be restored" ) );
        }

        log.info( "Account restored for user: " + username );

        webMessageService.send( WebMessageUtils.ok( "Account restored" ), response, request );
    }

    @RequestMapping( method = RequestMethod.POST )
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
        @RequestParam( required = false ) String inviteCode,
        @RequestParam( value = "recaptcha_challenge_field", required = false ) String recapChallenge,
        @RequestParam( value = "recaptcha_response_field", required = false ) String recapResponse,
        HttpServletRequest request,
        HttpServletResponse response ) throws WebMessageException
    {
        UserCredentials credentials = null;

        boolean invitedByEmail = (inviteUsername != null && !inviteUsername.isEmpty());

        boolean canChooseUsername = true;

        if ( invitedByEmail )
        {
            credentials = userService.getUserCredentialsByUsername( inviteUsername );

            if ( credentials == null )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Invitation link not valid" ) );
            }

            boolean canRestore = securityService.canRestore( credentials, inviteToken, inviteCode, RestoreType.INVITE );

            if ( !canRestore )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Invitation code not valid" ) );
            }

            RestoreOptions restoreOptions = securityService.getRestoreOptions( inviteToken );

            canChooseUsername = restoreOptions.isUsernameChoice();
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
        recapChallenge = StringUtils.trimToNull( recapChallenge );
        recapResponse = StringUtils.trimToNull( recapResponse );

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

        if ( password == null || !ValidationUtils.passwordIsValid( password ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Password is not specified or invalid" ) );
        }

        if ( password.trim().equals( username != null ? username.trim() : null ) )
        {
            throw new WebMessageException( WebMessageUtils.badRequest( "Password cannot be equal to username" ) );
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
            if ( recapChallenge == null )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Recaptcha challenge must be specified" ) );
            }

            if ( recapResponse == null )
            {
                throw new WebMessageException( WebMessageUtils.badRequest( "Recaptcha response must be specified" ) );
            }

            // ---------------------------------------------------------------------
            // Check result from API, return 500 if not
            // ---------------------------------------------------------------------

            String[] results = checkRecaptcha( KEY, request.getRemoteAddr(), recapChallenge, recapResponse );

            if ( results == null || results.length == 0 )
            {
                throw new WebMessageException( WebMessageUtils.error( "Captcha could not be verified due to a server error" ) );
            }

            // ---------------------------------------------------------------------
            // Check if verification was successful, return 400 if not
            // ---------------------------------------------------------------------

            if ( !TRUE.equalsIgnoreCase( results[0] ) )
            {
                log.info( "Recaptcha failed with code: " + (results.length > 0 ? results[1] : "") );

                throw new WebMessageException( WebMessageUtils.badRequest( "The characters you entered did not match the word verification, try again" ) );
            }
        }

        // ---------------------------------------------------------------------
        // Create and save user, return 201
        // ---------------------------------------------------------------------

        if ( invitedByEmail )
        {
            boolean restored = securityService.restore( credentials, inviteToken, inviteCode, password, RestoreType.INVITE );

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

    @RequestMapping( value = "/password", method = RequestMethod.POST )
    public void updatePassword(
        @RequestParam String oldPassword,
        @RequestParam String password,
        HttpServletRequest request,
        HttpServletResponse response ) throws IOException
    {
        String username = (String) request.getSession().getAttribute( "username" );
        UserCredentials credentials = userService.getUserCredentialsByUsername( username );

        Map<String, String> result = new HashMap<>();
        result.put( "status", "OK" );

        if ( credentials == null )
        {
            result.put( "status", "NON_EXPIRED" );
            result.put( "message", "Username is not valid, redirecting to login." );

            ContextUtils.badRequestResponse( response, objectMapper.writeValueAsString( result ) );
            return;
        }

        if ( userService.credentialsNonExpired( credentials ) )
        {
            result.put( "status", "NON_EXPIRED" );
            result.put( "message", "Account is not expired, redirecting to login." );

            ContextUtils.badRequestResponse( response, objectMapper.writeValueAsString( result ) );
            return;
        }

        if ( !passwordManager.matches( oldPassword, credentials.getPassword() ) )
        {
            result.put( "status", "NON_MATCHING_PASSWORD" );
            result.put( "message", "Old password is wrong, please correct and try again." );

            ContextUtils.badRequestResponse( response, objectMapper.writeValueAsString( result ) );
            return;
        }

        if ( password == null || !ValidationUtils.passwordIsValid( password ) )
        {
            result.put( "status", "PASSWORD_INVALID" );
            result.put( "message", "Password is not specified or invalid" );

            ContextUtils.badRequestResponse( response, objectMapper.writeValueAsString( result ) );
            return;
        }

        if ( password.trim().equals( username.trim() ) )
        {
            result.put( "status", "PASSWORD_EQUAL_TO_USERNAME" );
            result.put( "message", "Password cannot be equal to username" );

            ContextUtils.badRequestResponse( response, objectMapper.writeValueAsString( result ) );
            return;
        }

        userService.encodeAndSetPassword( credentials, password );
        userService.updateUserCredentials( credentials );

        authenticate( username, password, getAuthorities( credentials.getUserAuthorityGroups() ), request );

        result.put( "message", "Account was updated." );

        ContextUtils.okResponse( response, objectMapper.writeValueAsString( result ) );
    }

    @RequestMapping( value = "/username", method = RequestMethod.GET )
    public void validateUserName( @RequestParam String username, HttpServletResponse response ) throws IOException
    {
        boolean valid = username != null && userService.getUserCredentialsByUsername( username ) == null;

        // Custom code required because of our hacked jQuery validation

        Map<String, String> result = new HashMap<>();

        result.put( "response", valid ? "success" : "error" );
        result.put( "message", valid ? "" : "Username is already taken" );

        ContextUtils.okResponse( response, objectMapper.writeValueAsString( result ) );
    }

    // ---------------------------------------------------------------------
    // Supportive methods
    // ---------------------------------------------------------------------

    private String[] checkRecaptcha( String privateKey, String remoteIp, String challenge, String response )
    {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add( "privatekey", privateKey );
        params.add( "remoteip", remoteIp );
        params.add( "challenge", challenge );
        params.add( "response", response );

        String result = restTemplate.postForObject( RECAPTCHA_VERIFY_URL, params, String.class );

        log.info( "Recaptcha result: " + result );

        return result != null ? result.split( SPLIT ) : null;
    }

    private void authenticate( String username, String rawPassword, Collection<GrantedAuthority> authorities, HttpServletRequest request )
    {
        UsernamePasswordAuthenticationToken token =
            new UsernamePasswordAuthenticationToken( username, rawPassword, authorities );

        Authentication auth = authenticationManager.authenticate( token );

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
