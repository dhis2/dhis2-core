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
package org.hisp.dhis.user;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.time.ZoneId.systemDefault;
import static java.time.ZonedDateTime.now;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.system.util.ValidationUtils.usernameIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.uuidIsValid;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.security.SecurityService;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.filter.UserRoleCanIssueFilter;
import org.hisp.dhis.util.DateUtils;
import org.hisp.dhis.util.ObjectUtils;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

/**
 * @author Chau Thu Tran
 */
@Slf4j
@Lazy
@Service( "org.hisp.dhis.user.UserService" )
public class DefaultUserService
    implements UserService
{
    private Pattern BCRYPT_PATTERN = Pattern.compile( "\\A\\$2(a|y|b)?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}" );

    private static final int EXPIRY_THRESHOLD = 14;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final UserStore userStore;

    private final UserGroupService userGroupService;

    private final UserRoleStore userRoleStore;

    private final CurrentUserService currentUserService;

    private final SystemSettingManager systemSettingManager;

    private final PasswordManager passwordManager;

    private final SessionRegistry sessionRegistry;

    private final SecurityService securityService;

    private final Cache<String> userDisplayNameCache;

    public DefaultUserService( UserStore userStore, UserGroupService userGroupService,
        UserRoleStore userRoleStore,
        CurrentUserService currentUserService, SystemSettingManager systemSettingManager,
        CacheProvider cacheProvider,
        @Lazy PasswordManager passwordManager, @Lazy SessionRegistry sessionRegistry,
        @Lazy SecurityService securityService )
    {
        checkNotNull( userStore );
        checkNotNull( userGroupService );
        checkNotNull( userRoleStore );
        checkNotNull( systemSettingManager );
        checkNotNull( passwordManager );
        checkNotNull( sessionRegistry );
        checkNotNull( securityService );

        this.userStore = userStore;
        this.userGroupService = userGroupService;
        this.userRoleStore = userRoleStore;
        this.currentUserService = currentUserService;
        this.systemSettingManager = systemSettingManager;
        this.passwordManager = passwordManager;
        this.sessionRegistry = sessionRegistry;
        this.securityService = securityService;
        this.userDisplayNameCache = cacheProvider.createUserDisplayNameCache();
    }

    // -------------------------------------------------------------------------
    // UserService implementation
    // -------------------------------------------------------------------------

    // -------------------------------------------------------------------------
    // User
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addUser( User user )
    {
        String currentUsername = currentUserService.getCurrentUsername();
        AuditLogUtil.infoWrapper( log, currentUsername, user, AuditLogUtil.ACTION_CREATE );

        userStore.save( user );

        return user.getId();
    }

    @Override
    @Transactional
    public void updateUser( User user )
    {
        userStore.update( user );

        AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), user, AuditLogUtil.ACTION_UPDATE );
    }

    @Override
    @Transactional
    public void deleteUser( User user )
    {
        AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), user, AuditLogUtil.ACTION_DELETE );

        userStore.delete( user );
    }

    @Override
    @Transactional( readOnly = true )
    public List<User> getAllUsers()
    {
        return userStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public User getUser( long userId )
    {
        return userStore.get( userId );
    }

    @Override
    @Transactional( readOnly = true )
    public User getUser( String uid )
    {
        return userStore.getByUidNoAcl( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public User getUserByUuid( UUID uuid )
    {
        return userStore.getUserByUuid( uuid );
    }

    @Override
    @Transactional( readOnly = true )
    public User getUserByUsername( String username )
    {
        return userStore.getUserByUsername( username );
    }

    @Override
    @Transactional( readOnly = true )
    public User getUserByIdentifier( String id )
    {
        User user = null;

        if ( isValidUid( id ) && (user = getUser( id )) != null )
        {
            return user;
        }

        if ( uuidIsValid( id ) && (user = getUserByUuid( UUID.fromString( id ) )) != null )
        {
            return user;
        }

        if ( usernameIsValid( id, false ) && (user = getUserByUsername( id )) != null )
        {
            return user;
        }

        return user;
    }

    @Override
    @Transactional( readOnly = true )
    public List<User> getUsers( Collection<String> uids )
    {
        return userStore.getByUid( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public List<User> getAllUsersBetweenByName( String name, int first, int max )
    {
        UserQueryParams params = new UserQueryParams();
        params.setQuery( name );
        params.setFirst( first );
        params.setMax( max );

        return userStore.getUsers( params );
    }

    @Override
    @Transactional( readOnly = true )
    public List<User> getUsers( UserQueryParams params )
    {
        return getUsers( params, null );
    }

    @Override
    @Transactional( readOnly = true )
    public List<User> getUsers( UserQueryParams params, @Nullable List<String> orders )
    {
        handleUserQueryParams( params );

        if ( !validateUserQueryParams( params ) )
        {
            return Lists.newArrayList();
        }

        return userStore.getUsers( params, orders );
    }

    @Override
    @Transactional( readOnly = true )
    public int getUserCount( UserQueryParams params )
    {
        handleUserQueryParams( params );

        if ( !validateUserQueryParams( params ) )
        {
            return 0;
        }

        return userStore.getUserCount( params );
    }

    @Override
    @Transactional( readOnly = true )
    public int getUserCount()
    {
        return userStore.getUserCount();
    }

    private void handleUserQueryParams( UserQueryParams params )
    {
        boolean canSeeOwnRoles = params.isCanSeeOwnRoles()
            || systemSettingManager.getBoolSetting( SettingKey.CAN_GRANT_OWN_USER_ROLES );
        params.setDisjointRoles( !canSeeOwnRoles );

        if ( !params.hasUser() )
        {
            params.setUser( currentUserService.getCurrentUser() );
        }

        if ( params.hasUser() && params.getUser().isSuper() )
        {
            params.setCanManage( false );
            params.setAuthSubset( false );
            params.setDisjointRoles( false );
        }

        if ( params.getInactiveMonths() != null )
        {
            Calendar cal = PeriodType.createCalendarInstance();
            cal.add( Calendar.MONTH, (params.getInactiveMonths() * -1) );
            params.setInactiveSince( cal.getTime() );
        }

        if ( params.isUserOrgUnits() && params.hasUser() )
        {
            params.setOrganisationUnits( params.getUser().getOrganisationUnits() );
        }
    }

    private boolean validateUserQueryParams( UserQueryParams params )
    {
        if ( params.isCanManage() && (params.getUser() == null || !params.getUser().hasManagedGroups()) )
        {
            log.warn( "Cannot get managed users as user does not have any managed groups" );
            return false;
        }

        if ( params.isAuthSubset()
            && (params.getUser() == null || !params.getUser().hasAuthorities()) )
        {
            log.warn( "Cannot get users with authority subset as user does not have any authorities" );
            return false;
        }

        if ( params.isDisjointRoles()
            && (params.getUser() == null || !params.getUser().hasUserRoles()) )
        {
            log.warn( "Cannot get users with disjoint roles as user does not have any user roles" );
            return false;
        }

        return true;
    }

    @Override
    @Transactional( readOnly = true )
    public List<User> getUsersByPhoneNumber( String phoneNumber )
    {
        UserQueryParams params = new UserQueryParams();
        params.setPhoneNumber( phoneNumber );
        return getUsers( params );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean isLastSuperUser( User user )
    {
        if ( !user.isSuper() )
        {
            return false; // Cannot be last if not superuser
        }

        Collection<User> allUsers = userStore.getAll();

        for ( User u : allUsers )
        {
            if ( u.isSuper() && !u.equals( user ) )
            {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional( readOnly = true )
    public boolean canAddOrUpdateUser( Collection<String> userGroups )
    {
        return canAddOrUpdateUser( userGroups, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean canAddOrUpdateUser( Collection<String> userGroups, User currentUser )
    {
        if ( currentUser == null )
        {
            return false;
        }

        boolean canAdd = currentUser.isAuthorized( UserGroup.AUTH_USER_ADD );

        if ( canAdd )
        {
            return true;
        }

        boolean canAddInGroup = currentUser.isAuthorized( UserGroup.AUTH_USER_ADD_IN_GROUP );

        if ( !canAddInGroup )
        {
            return false;
        }

        boolean canManageAnyGroup = false;

        for ( String uid : userGroups )
        {
            UserGroup userGroup = userGroupService.getUserGroup( uid );

            if ( currentUser.canManage( userGroup ) )
            {
                canManageAnyGroup = true;
                break;
            }
        }

        return canManageAnyGroup;
    }

    // -------------------------------------------------------------------------
    // UserRole
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addUserRole( UserRole userRole )
    {
        userRoleStore.save( userRole );
        return userRole.getId();
    }

    @Override
    @Transactional
    public void updateUserRole( UserRole userRole )
    {
        userRoleStore.update( userRole );
    }

    @Override
    @Transactional
    public void deleteUserRole( UserRole userRole )
    {
        userRoleStore.delete( userRole );
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserRole> getAllUserRoles()
    {
        return userRoleStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public UserRole getUserRole( long id )
    {
        return userRoleStore.get( id );
    }

    @Override
    @Transactional( readOnly = true )
    public UserRole getUserRole( String uid )
    {
        return userRoleStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public UserRole getUserRoleByName( String name )
    {
        return userRoleStore.getByName( name );
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserRole> getUserRolesByUid( Collection<String> uids )
    {
        return userRoleStore.getByUid( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserRole> getUserRolesBetween( int first, int max )
    {
        return userRoleStore.getAllOrderedName( first, max );
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserRole> getUserRolesBetweenByName( String name, int first, int max )
    {
        return userRoleStore.getAllLikeName( name, first, max );
    }

    @Override
    @Transactional( readOnly = true )
    public int countDataSetUserRoles( DataSet dataSet )
    {
        return userRoleStore.countDataSetUserRoles( dataSet );
    }

    @Override
    @Transactional( readOnly = true )
    public void canIssueFilter( Collection<UserRole> userRoles )
    {
        User user = currentUserService.getCurrentUser();

        boolean canGrantOwnUserRoles = systemSettingManager
            .getBoolSetting( SettingKey.CAN_GRANT_OWN_USER_ROLES );

        FilterUtils.filter( userRoles, new UserRoleCanIssueFilter( user, canGrantOwnUserRoles ) );
    }

    @Override
    @Transactional( readOnly = true )
    public List<User> getUsersByUsernames( Collection<String> usernames )
    {
        return userStore.getUserByUsernames( usernames );
    }

    @Override
    @Transactional
    public void encodeAndSetPassword( User user, String rawPassword )
    {
        if ( StringUtils.isEmpty( rawPassword ) && !user.isExternalAuth() )
        {
            return; // Leave unchanged if internal authentication and no
            // password supplied
        }

        if ( user.isExternalAuth() )
        {
            user.setPassword( UserService.PW_NO_INTERNAL_LOGIN );

            return; // Set unusable, not-encoded password if external
            // authentication
        }

        boolean isNewPassword = StringUtils.isBlank( user.getPassword() ) ||
            !passwordManager.matches( rawPassword, user.getPassword() );

        if ( isNewPassword )
        {
            user.setPasswordLastUpdated( new Date() );
        }

        // Encode and set password
        Matcher matcher = this.BCRYPT_PATTERN.matcher( rawPassword );
        if ( matcher.matches() )
        {
            throw new IllegalArgumentException( "Raw password look like BCrypt: " + rawPassword );
        }

        String encode = passwordManager.encode( rawPassword );
        user.setPassword( encode );
        user.getPreviousPasswords().add( encode );
    }

    @Override
    @Transactional( readOnly = true )
    public User getUserByIdToken( String token )
    {
        return userStore.getUserByIdToken( token );
    }

    @Override
    @Transactional( readOnly = true )
    public User getUserWithEagerFetchAuthorities( String username )
    {
        User user = userStore.getUserByUsername( username );

        if ( user != null )
        {
            user.getAllAuthorities();
        }

        return user;
    }

    @Override
    @Transactional( readOnly = true )
    public User getUserByOpenId( String openId )
    {
        User user = userStore.getUserByOpenId( openId );

        if ( user != null )
        {
            user.getAllAuthorities();
        }

        return user;
    }

    @Override
    @Transactional( readOnly = true )
    public User getUserByLdapId( String ldapId )
    {
        return userStore.getUserByLdapId( ldapId );
    }

    @Override
    @Transactional
    public void setLastLogin( String username )
    {
        User user = getUserByUsername( username );

        if ( user != null )
        {
            user.setLastLogin( new Date() );
            updateUser( user );
        }
    }

    @Override
    @Transactional( readOnly = true )
    public int getActiveUsersCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return getActiveUsersCount( cal.getTime() );
    }

    @Override
    @Transactional( readOnly = true )
    public int getActiveUsersCount( Date since )
    {
        UserQueryParams params = new UserQueryParams();
        params.setLastLogin( since );

        return getUserCount( params );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean userNonExpired( User user )
    {
        int credentialsExpires = systemSettingManager.credentialsExpires();

        if ( credentialsExpires == 0 )
        {
            return true;
        }

        if ( user == null || user.getPasswordLastUpdated() == null )
        {
            return true;
        }

        int months = DateUtils.monthsBetween( user.getPasswordLastUpdated(), new Date() );

        return months < credentialsExpires;
    }

    @Override
    public boolean isAccountExpired( User user )
    {
        return !user.isAccountNonExpired();
    }

    @Override
    @Transactional( readOnly = true )
    public List<ErrorReport> validateUser( User user, User currentUser )
    {
        List<ErrorReport> errors = new ArrayList<>();

        if ( currentUser == null || user == null )
        {
            return errors;
        }

        // Validate user role

        boolean canGrantOwnUserRoles = systemSettingManager
            .getBoolSetting( SettingKey.CAN_GRANT_OWN_USER_ROLES );

        Set<UserRole> userRoles = user.getUserRoles();

        if ( userRoles != null )
        {
            List<UserRole> roles = userRoleStore.getByUid(
                userRoles.stream().map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() ) );
            roles.forEach( ur -> {
                if ( !currentUser.canIssueUserRole( ur, canGrantOwnUserRoles ) )
                {
                    errors.add( new ErrorReport( UserRole.class, ErrorCode.E3003, currentUser.getUsername(),
                        ur.getName() ) );
                }
            } );
        }

        // Validate user group
        boolean canAdd = currentUser.isAuthorized( UserGroup.AUTH_USER_ADD );

        if ( canAdd )
        {
            return errors;
        }

        boolean canAddInGroup = currentUser.isAuthorized( UserGroup.AUTH_USER_ADD_IN_GROUP );

        if ( !canAddInGroup )
        {
            errors.add( new ErrorReport( UserGroup.class, ErrorCode.E3004, currentUser ) );
            return errors;
        }

        user.getGroups().forEach( ug -> {
            if ( !(currentUser.canManage( ug ) || userGroupService.canAddOrRemoveMember( ug.getUid() )) )
            {
                errors.add( new ErrorReport( UserGroup.class, ErrorCode.E3005, currentUser, ug ) );
            }
        } );

        return errors;
    }

    @Override
    @Transactional( readOnly = true )
    public List<User> getExpiringUsers()
    {
        int daysBeforePasswordChangeRequired = systemSettingManager
            .getIntSetting( SettingKey.CREDENTIALS_EXPIRES ) * 30;

        Date daysPassed = new DateTime( new Date() ).minusDays( daysBeforePasswordChangeRequired - EXPIRY_THRESHOLD )
            .toDate();

        UserQueryParams userQueryParams = new UserQueryParams()
            .setDisabled( false )
            .setPasswordLastUpdated( daysPassed );

        return userStore.getExpiringUsers( userQueryParams );
    }

    @Override
    public List<UserAccountExpiryInfo> getExpiringUserAccounts( int inDays )
    {
        return userStore.getExpiringUserAccounts( inDays );
    }

    @Override
    @Transactional
    public void set2FA( User user, Boolean twoFa )
    {
        user.setTwoFA( twoFa );

        updateUser( user );
    }

    @Override
    public void expireActiveSessions( User user )
    {
        List<SessionInformation> sessions = sessionRegistry.getAllSessions( user, false );

        sessions.forEach( SessionInformation::expireNow );
    }

    @Override
    @Transactional
    public int disableUsersInactiveSince( Date inactiveSince )
    {
        if ( ZonedDateTime.ofInstant( inactiveSince.toInstant(), systemDefault() ).plusMonths( 1 ).isAfter( now() ) )
        {
            // we never disable users that have been active during last month
            return 0;
        }
        return userStore.disableUsersInactiveSince( inactiveSince );
    }

    @Override
    @Transactional( readOnly = true )
    public Map<String, Optional<Locale>> findNotifiableUsersWithLastLoginBetween( Date from, Date to )
    {
        return userStore.findNotifiableUsersWithLastLoginBetween( from, to );
    }

    @Override
    public String getDisplayName( String userUid )
    {
        return userDisplayNameCache.get( userUid, c -> userStore.getDisplayName( userUid ) );
    }

    @Override

    public List<User> getUsersWithAuthority( String authority )
    {
        return userStore.getHasAuthority( authority );
    }

    @Override
    @Transactional( readOnly = true )
    public CurrentUserDetails validateAndCreateUserDetails( User user, String password )
    {
        Objects.requireNonNull( user );

        String username = user.getUsername();
        boolean enabled = !user.isDisabled();
        boolean credentialsNonExpired = userNonExpired( user );
        boolean accountNonLocked = !securityService.isLocked( user.getUsername() );
        boolean accountNonExpired = !isAccountExpired( user );

        if ( ObjectUtils.anyIsFalse( enabled, credentialsNonExpired, accountNonLocked, accountNonExpired ) )
        {
            log.info( String.format(
                "Login attempt for disabled/locked user: '%s', enabled: %b, account non-expired: %b, user non-expired: %b, account non-locked: %b",
                username, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked ) );
        }

        return createUserDetails( user, password, accountNonLocked, credentialsNonExpired );
    }

    @Override
    public CurrentUserDetailsImpl createUserDetails( User user, String password, boolean accountNonLocked,
        boolean credentialsNonExpired )
    {
        return CurrentUserDetailsImpl.builder()
            .uid( user.getUid() )
            .username( user.getUsername() )
            .password( user.getPassword() )
            .enabled( user.isEnabled() )
            .accountNonExpired( user.isAccountNonExpired() )
            .accountNonLocked( accountNonLocked )
            .credentialsNonExpired( credentialsNonExpired )
            .authorities( user.getAuthorities() )
            .userSettings( new HashMap<>() )
            .userGroupIds( currentUserService.getCurrentUserGroupsInfo( user.getUid() ).getUserGroupUIDs() )
            .isSuper( user.isSuper() )
            .build();
    }
}
