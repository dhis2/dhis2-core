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
package org.hisp.dhis.security;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.message.MessageSender;
import org.hisp.dhis.period.Cal;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.ValidationUtils;
import org.hisp.dhis.system.velocity.VelocityManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.UserSettingKey;
import org.hisp.dhis.user.UserSettingService;
import org.hisp.dhis.util.ObjectUtils;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;

/**
 * @author Lars Helge Overland
 */
@Slf4j
@Service( "org.hisp.dhis.security.SecurityService" )
public class DefaultSecurityService
    implements SecurityService
{
    private static final String RESTORE_PATH = "/dhis-web-commons/security/";

    private static final Pattern INVITE_USERNAME_PATTERN = Pattern.compile( "^invite\\-(.+?)\\-(\\w{11})$" );

    private static final String TBD_NAME = "(TBD)";

    private static final String DEFAULT_APPLICATION_TITLE = "DHIS 2";

    private static final int INVITED_USER_PASSWORD_LENGTH_BYTES = 16;

    private static final int RESTORE_TOKEN_LENGTH_BYTES = 32;

    private static final int LOGIN_MAX_FAILED_ATTEMPTS = 4;

    public static final int RECOVERY_LOCKOUT_MINS = 15;

    private static final int RECOVER_MAX_ATTEMPTS = 5;

    private static final String RECAPTCHA_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    private final Cache<Integer> userFailedLoginAttemptCache;

    private final Cache<Integer> userAccountRecoverAttemptCache;
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final CurrentUserService currentUserService;

    private final UserSettingService userSettingService;

    private final AclService aclService;

    private final RestTemplate restTemplate;

    private final PasswordManager passwordManager;

    private final MessageSender emailMessageSender;

    private final UserService userService;

    private final SystemSettingManager systemSettingManager;

    private final I18nManager i18nManager;

    private final ObjectMapper jsonMapper;

    public DefaultSecurityService(
        CurrentUserService currentUserService,
        UserSettingService userSettingService,
        AclService aclService,
        RestTemplate restTemplate,
        CacheProvider cacheProvider,
        @Lazy PasswordManager passwordManager,
        MessageSender emailMessageSender,
        UserService userService,
        SystemSettingManager systemSettingManager,
        I18nManager i18nManager,
        ObjectMapper jsonMapper )
    {
        checkNotNull( currentUserService );
        checkNotNull( userSettingService );
        checkNotNull( aclService );
        checkNotNull( restTemplate );
        checkNotNull( cacheProvider );
        checkNotNull( passwordManager );
        checkNotNull( emailMessageSender );
        checkNotNull( userService );
        checkNotNull( systemSettingManager );
        checkNotNull( i18nManager );
        checkNotNull( jsonMapper );

        this.currentUserService = currentUserService;
        this.userSettingService = userSettingService;
        this.aclService = aclService;
        this.restTemplate = restTemplate;
        this.passwordManager = passwordManager;
        this.emailMessageSender = emailMessageSender;
        this.userService = userService;
        this.systemSettingManager = systemSettingManager;
        this.i18nManager = i18nManager;
        this.jsonMapper = jsonMapper;
        this.userFailedLoginAttemptCache = cacheProvider.createUserFailedLoginAttemptCache( 0 );
        this.userAccountRecoverAttemptCache = cacheProvider.createUserAccountRecoverAttemptCache( 0 );
    }

    // -------------------------------------------------------------------------
    // SecurityService implementation
    // -------------------------------------------------------------------------
    @Override
    public void registerRecoveryAttempt( String username )
    {
        if ( !isBlockFailedLogins() || username == null )
        {
            return;
        }

        Integer attempts = userAccountRecoverAttemptCache.get( username ).orElse( 0 );

        userAccountRecoverAttemptCache.put( username, ++attempts );
    }

    @Override
    public boolean isRecoveryLocked( String username )
    {
        if ( !isBlockFailedLogins() || username == null )
        {
            return false;
        }

        return userAccountRecoverAttemptCache.get( username ).orElse( 0 ) > RECOVER_MAX_ATTEMPTS;
    }

    @Override
    public void registerFailedLogin( String username )
    {
        if ( !isBlockFailedLogins() || username == null )
        {
            return;
        }

        Integer attempts = userFailedLoginAttemptCache.get( username ).orElse( 0 );

        attempts++;

        userFailedLoginAttemptCache.put( username, attempts );
    }

    @Override
    public void registerSuccessfulLogin( String username )
    {
        if ( !isBlockFailedLogins() || username == null )
        {
            return;
        }

        userFailedLoginAttemptCache.invalidate( username );
    }

    @Override
    public boolean isLocked( String username )
    {
        if ( !isBlockFailedLogins() || username == null )
        {
            return false;
        }

        return userFailedLoginAttemptCache.get( username ).orElse( 0 ) >= LOGIN_MAX_FAILED_ATTEMPTS;
    }

    private boolean isBlockFailedLogins()
    {
        return (Boolean) systemSettingManager.getSystemSetting( SettingKey.LOCK_MULTIPLE_FAILED_LOGINS );
    }

    @Override
    public void prepareUserForInvite( User user )
    {
        Objects.requireNonNull( user, "User object can't be null" );
        Objects.requireNonNull( user, "Credentials object can't be null" );

        if ( user.getUsername() == null || user.getUsername().isEmpty() )
        {
            String username = "invite-" + user.getEmail() + "-" + CodeGenerator.generateUid();

            user.getUserCredentials().setUsername( username );
        }

        String rawPassword = "Inv!t3#" + CodeGenerator.getRandomSecureToken( INVITED_USER_PASSWORD_LENGTH_BYTES );

        user.setSurname( StringUtils.isEmpty( user.getSurname() ) ? TBD_NAME : user.getSurname() );
        user.setFirstName( StringUtils.isEmpty( user.getFirstName() ) ? TBD_NAME : user.getFirstName() );
        user.getUserCredentials().setInvitation( true );

        userService.encodeAndSetPassword( user, rawPassword );
    }

    @Override
    public String validateRestore( UserCredentials credentials )
    {
        if ( credentials == null || credentials.getUserInfo() == null )
        {
            log.warn( "Could not send restore/invite message as user is null: " + credentials );
            return "no_user_credentials";
        }

        if ( credentials.getUserInfo().getEmail() == null ||
            !ValidationUtils.emailIsValid( credentials.getUserInfo().getEmail() ) )
        {
            log.warn( "Could not send restore/invite message as user has no email or email is invalid" );
            return "user_does_not_have_valid_email";
        }

        if ( !emailMessageSender.isConfigured() )
        {
            log.warn( "Could not send restore/invite message as email is not configured" );
            return "email_not_configured_for_system";
        }

        return null;
    }

    @Override
    public String validateInvite( UserCredentials credentials )
    {
        if ( credentials == null || credentials.getUserInfo() == null )
        {
            log.warn( "Could not send invite message as user is null" );
            return "no_user_credentials";
        }

        if ( credentials.getUsername() != null &&
            userService.getUserCredentialsByUsername( credentials.getUsername() ) != null )
        {
            log.warn( "Could not send invite message as username is already taken: " + credentials );
            return "username_taken";
        }

        if ( credentials.getUserInfo().getEmail() == null ||
            !ValidationUtils.emailIsValid( credentials.getUserInfo().getEmail() ) )
        {
            log.warn( "Could not send restore/invite message as user has no email or email is invalid" );
            return "user_does_not_have_valid_email";
        }

        if ( !emailMessageSender.isConfigured() )
        {
            log.warn( "Could not send restore/invite message as email is not configured" );
            return "email_not_configured_for_system";
        }

        return null;
    }

    @Override
    public boolean sendRestoreOrInviteMessage( UserCredentials credentials, String rootPath,
        RestoreOptions restoreOptions )
    {
        String encodedTokens = generateAndPersistTokens( credentials, restoreOptions );

        RestoreType restoreType = restoreOptions.getRestoreType();

        String applicationTitle = (String) systemSettingManager.getSystemSetting( SettingKey.APPLICATION_TITLE );

        if ( applicationTitle == null || applicationTitle.isEmpty() )
        {
            applicationTitle = DEFAULT_APPLICATION_TITLE;
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put( "applicationTitle", applicationTitle );
        vars.put( "restorePath", rootPath + RESTORE_PATH + restoreType.getAction() );
        vars.put( "token", encodedTokens );
        vars.put( "welcomeMessage", credentials.getUserInfo().getWelcomeMessage() );

        I18n i18n = i18nManager.getI18n( ObjectUtils.firstNonNull(
            (Locale) userSettingService.getUserSetting( UserSettingKey.UI_LOCALE, credentials.getUserInfo() ),
            LocaleManager.DEFAULT_LOCALE ) );

        vars.put( "i18n", i18n );

        rootPath = rootPath.replace( "http://", "" ).replace( "https://", "" );

        // -------------------------------------------------------------------------
        // Render emails
        // -------------------------------------------------------------------------

        VelocityManager vm = new VelocityManager();

        String messageBody = vm.render( vars, restoreType.getEmailTemplate() + "1" );

        String messageSubject = i18n.getString( restoreType.getEmailSubject() ) + " " + rootPath;

        // -------------------------------------------------------------------------
        // Send emails
        // -------------------------------------------------------------------------

        emailMessageSender
            .sendMessage( messageSubject, messageBody, null, null, ImmutableSet.of( credentials.getUserInfo() ), true );

        return true;
    }

    @Override
    public String generateAndPersistTokens( UserCredentials credentials, RestoreOptions restoreOptions )
    {
        RestoreType restoreType = restoreOptions.getRestoreType();

        String restoreToken = restoreOptions.getTokenPrefix()
            + CodeGenerator.getRandomSecureToken( RESTORE_TOKEN_LENGTH_BYTES );

        String hashedRestoreToken = passwordManager.encode( restoreToken );

        String idToken = CodeGenerator.getRandomSecureToken( RESTORE_TOKEN_LENGTH_BYTES );

        Date expiry = new Cal().now().add( restoreType.getExpiryIntervalType(), restoreType.getExpiryIntervalCount() )
            .time();

        // The id token is not hashed since we use it for lookup.
        credentials.setIdToken( idToken );
        credentials.setRestoreToken( hashedRestoreToken );
        credentials.setRestoreExpiry( expiry );

        userService.updateUserCredentials( credentials );

        return Base64.getUrlEncoder().withoutPadding().encodeToString( (idToken + ":" + restoreToken).getBytes() );
    }

    public String[] decodeEncodedTokens( String encodedTokens )
    {
        String decodedEmailToken = new String( Base64.getUrlDecoder().decode( encodedTokens ), StandardCharsets.UTF_8 );

        return decodedEmailToken.split( ":" );
    }

    @Override
    public RestoreOptions getRestoreOptions( String token )
    {
        return RestoreOptions.getRestoreOptions( token );
    }

    @Override
    public boolean restore( UserCredentials credentials, String token, String newPassword, RestoreType restoreType )
    {
        if ( credentials == null || token == null || newPassword == null
            || !canRestore( credentials, token, restoreType ) )
        {
            return false;
        }

        credentials.setRestoreToken( null );
        credentials.setRestoreExpiry( null );
        credentials.setIdToken( null );
        credentials.setInvitation( false );

        userService.encodeAndSetPassword( credentials, newPassword );
        userService.updateUserCredentials( credentials );

        return true;
    }

    @Override
    public boolean canRestore( UserCredentials credentials, String token, RestoreType restoreType )
    {
        String logPrefix = "Restore user: " + credentials.getUid() + ", username: " + credentials.getUsername() + " ";

        String errorMessage = verifyRestore( credentials, token, restoreType );

        if ( errorMessage != null )
        {
            log.warn( logPrefix + "Failed to verify restore: " + errorMessage );
            return false;
        }

        log.info( logPrefix + " success" );
        return true;
    }

    /**
     * Verifies all parameters needed for account restore and checks validity of
     * the user supplied token and code. If the restore cannot be verified a
     * descriptive error string is returned.
     *
     * @param credentials the user credentials.
     * @param token the user supplied token.
     * @param restoreType the restore type.
     * @return null if restore is valid, a descriptive error string otherwise.
     */
    private String verifyRestore( UserCredentials credentials, String token, RestoreType restoreType )
    {
        String errorMessage = credentials.isRestorable();

        if ( errorMessage != null )
        {
            return errorMessage;
        }

        errorMessage = verifyRestoreToken( credentials, token, restoreType );

        if ( errorMessage != null )
        {
            return errorMessage;
        }

        Date currentTime = new DateTime().toDate();
        Date restoreExpiry = credentials.getRestoreExpiry();

        if ( currentTime.after( restoreExpiry ) )
        {
            return "date_is_after_expiry";
        }

        return null; // Success;
    }

    /**
     * Verify the token given for a user invite or password restore operation.
     * <p/>
     * If error, returns one of the following strings:
     * <p/>
     * <ul>
     * <li>credentials_parameter_is_null</li>
     * <li>token_parameter_is_null</li>
     * <li>restore_type_parameter_is_null</li>
     * <li>cannot_parse_restore_options</li>
     * <li>wrong_prefix_for_restore_type</li>
     * <li>could_not_verify_token</li>
     * <li>restore_token_does_not_match_supplied_token</li>
     * </ul>
     *
     * @param credentials the user credentials.
     * @param restoreToken the token.
     * @param restoreType type of restore operation.
     * @return null if success, otherwise error string.
     */
    @Override
    public String verifyRestoreToken( UserCredentials credentials, String restoreToken, RestoreType restoreType )
    {
        if ( credentials == null )
        {
            log.warn( "Could not send verify restore token, credentials_parameter_is_null" );
            return "credentials_parameter_is_null";
        }

        if ( restoreToken == null )
        {
            log.warn( "Could not send verify restore token; error=token_parameter_is_null; username:" +
                credentials.getUsername() );
            return "token_parameter_is_null";
        }

        if ( restoreType == null )
        {
            log.warn( "Could not send verify restore token; error=restore_type_parameter_is_null; username:" +
                credentials.getUsername() );
            return "restore_type_parameter_is_null";
        }

        RestoreOptions restoreOptions = RestoreOptions.getRestoreOptions( restoreToken );

        if ( restoreOptions == null )
        {
            log.warn( "Could not send verify restore token; error=cannot_parse_restore_options; username:" +
                credentials.getUsername() );
            return "cannot_parse_restore_options";
        }

        if ( restoreType != restoreOptions.getRestoreType() )
        {
            log.warn( "Could not send verify restore token; error=wrong_prefix_for_restore_type; username:" +
                credentials.getUsername() );
            return "wrong_prefix_for_restore_type";
        }

        String hashedRestoreToken = credentials.getRestoreToken();

        if ( hashedRestoreToken == null )
        {
            log.warn( "Could not send verify restore token; error=could_not_verify_token; username:" +
                credentials.getUsername() );
            return "could_not_verify_token";
        }

        boolean validToken = passwordManager.matches( restoreToken, hashedRestoreToken );

        if ( !validToken )
        {
            log.warn(
                "Could not send verify restore token; error=restore_token_does_not_match_supplied_token; username:" +
                    credentials.getUsername() );
        }

        return validToken ? null : "restore_token_does_not_match_supplied_token";
    }

    @Override
    public boolean isInviteUsername( String username )
    {
        if ( username == null || username.isEmpty() )
        {
            return true;
        }

        return INVITE_USERNAME_PATTERN.matcher( username ).matches();
    }

    @Override
    public boolean canCreatePublic( IdentifiableObject identifiableObject )
    {
        return !aclService.isShareable( identifiableObject )
            || aclService.canMakePublic( currentUserService.getCurrentUser(), identifiableObject );
    }

    @Override
    public boolean canCreatePublic( String type )
    {
        Class<? extends IdentifiableObject> klass = aclService.classForType( type );

        return !aclService.isClassShareable( klass )
            || aclService.canMakeClassPublic( currentUserService.getCurrentUser(), klass );
    }

    @Override
    public boolean canCreatePrivate( IdentifiableObject identifiableObject )
    {
        return !aclService.isShareable( identifiableObject )
            || aclService.canMakePrivate( currentUserService.getCurrentUser(), identifiableObject );
    }

    @Override
    public boolean canView( String type )
    {
        boolean requireAddToView = (Boolean) systemSettingManager.getSystemSetting( SettingKey.REQUIRE_ADD_TO_VIEW );

        return !requireAddToView || (canCreatePrivate( type ) || canCreatePublic( type ));
    }

    @Override
    public boolean canCreatePrivate( String type )
    {
        Class<? extends IdentifiableObject> klass = aclService.classForType( type );

        return !aclService.isClassShareable( klass )
            || aclService.canMakeClassPrivate( currentUserService.getCurrentUser(), klass );
    }

    @Override
    public boolean canRead( IdentifiableObject identifiableObject )
    {
        return !aclService.isSupported( identifiableObject )
            || aclService.canRead( currentUserService.getCurrentUser(), identifiableObject );
    }

    @Override
    public boolean canWrite( IdentifiableObject identifiableObject )
    {
        return !aclService.isSupported( identifiableObject )
            || aclService.canWrite( currentUserService.getCurrentUser(), identifiableObject );
    }

    @Override
    public boolean canUpdate( IdentifiableObject identifiableObject )
    {
        return !aclService.isSupported( identifiableObject )
            || aclService.canUpdate( currentUserService.getCurrentUser(), identifiableObject );
    }

    @Override
    public boolean canDelete( IdentifiableObject identifiableObject )
    {
        return !aclService.isSupported( identifiableObject )
            || aclService.canDelete( currentUserService.getCurrentUser(), identifiableObject );
    }

    @Override
    public boolean canManage( IdentifiableObject identifiableObject )
    {
        return !aclService.isShareable( identifiableObject )
            || aclService.canManage( currentUserService.getCurrentUser(), identifiableObject );
    }

    @Override
    public boolean hasAnyAuthority( String... authorities )
    {
        User user = currentUserService.getCurrentUser();

        if ( user != null && user.getUserCredentials() != null )
        {
            UserCredentials userCredentials = user.getUserCredentials();

            for ( String authority : authorities )
            {
                if ( userCredentials.isAuthorized( authority ) )
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public RecaptchaResponse verifyRecaptcha( String key, String remoteIp )
        throws IOException
    {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        params.add( "secret", (String) systemSettingManager.getSystemSetting( SettingKey.RECAPTCHA_SECRET ) );
        params.add( "response", key );
        params.add( "remoteip", remoteIp );

        String result = restTemplate.postForObject( RECAPTCHA_VERIFY_URL, params, String.class );

        log.info( "Recaptcha result: " + result );

        return result != null ? jsonMapper.readValue( result, RecaptchaResponse.class ) : null;
    }

    @Override
    public boolean canDataWrite( IdentifiableObject identifiableObject )
    {
        return !aclService.isSupported( identifiableObject )
            || aclService.canDataWrite( currentUserService.getCurrentUser(), identifiableObject );
    }

    @Override
    public boolean canDataRead( IdentifiableObject identifiableObject )
    {
        return !aclService.isSupported( identifiableObject )
            || aclService.canDataRead( currentUserService.getCurrentUser(), identifiableObject );
    }
}
