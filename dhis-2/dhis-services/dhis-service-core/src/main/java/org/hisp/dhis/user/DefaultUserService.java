package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.AuditLogUtil;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.commons.filter.FilterUtils;
import org.hisp.dhis.dataset.DataSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ErrorReport;
import org.hisp.dhis.period.PeriodType;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.filter.UserAuthorityGroupCanIssueFilter;
import org.hisp.dhis.util.DateUtils;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hisp.dhis.common.CodeGenerator.isValidUid;
import static org.hisp.dhis.system.util.ValidationUtils.usernameIsValid;
import static org.hisp.dhis.system.util.ValidationUtils.uuidIsValid;

/**
 * @author Chau Thu Tran
 */
@Slf4j
@Lazy
@Service( "org.hisp.dhis.user.UserService" )
public class DefaultUserService
    implements UserService
{
    private static final int EXPIRY_THRESHOLD = 14;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final UserStore userStore;

    private final UserGroupService userGroupService;

    private final UserCredentialsStore userCredentialsStore;

    private final UserAuthorityGroupStore userAuthorityGroupStore;

    private final CurrentUserService currentUserService;

    private final SystemSettingManager systemSettingManager;

    private final PasswordManager passwordManager;

    private final SessionRegistry sessionRegistry;

    public DefaultUserService( UserStore userStore, UserGroupService userGroupService,
        UserCredentialsStore userCredentialsStore, UserAuthorityGroupStore userAuthorityGroupStore,
        CurrentUserService currentUserService, SystemSettingManager systemSettingManager,
        @Lazy PasswordManager passwordManager, @Lazy SessionRegistry sessionRegistry )
    {
        checkNotNull( userStore );
        checkNotNull( userGroupService );
        checkNotNull( userCredentialsStore );
        checkNotNull( userAuthorityGroupStore );
        checkNotNull( systemSettingManager );
        checkNotNull( passwordManager );
        checkNotNull( sessionRegistry );

        this.userStore = userStore;
        this.userGroupService = userGroupService;
        this.userCredentialsStore = userCredentialsStore;
        this.userAuthorityGroupStore = userAuthorityGroupStore;
        this.currentUserService = currentUserService;
        this.systemSettingManager = systemSettingManager;
        this.passwordManager = passwordManager;
        this.sessionRegistry = sessionRegistry;
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
        AuditLogUtil.infoWrapper( log, currentUserService.getCurrentUsername(), user, AuditLogUtil.ACTION_CREATE );

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
    @Transactional(readOnly = true)
    public List<User> getAllUsers()
    {
        return userStore.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser( long userId )
    {
        return userStore.get( userId );
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser( String uid )
    {
        return userStore.getByUid( uid );
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUuid( UUID uuid )
    {
        UserCredentials userCredentials = userCredentialsStore.getUserCredentialsByUuid( uuid );

        return userCredentials != null ? userCredentials.getUserInfo() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByUsername( String username )
    {
        UserCredentials userCredentials = userCredentialsStore.getUserCredentialsByUsername( username );

        return userCredentials != null ? userCredentials.getUserInfo() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserByIdentifier( String id )
    {
        User user = null;

        if ( isValidUid( id ) && ( user = getUser( id ) ) != null )
        {
            return user;
        }

        if ( uuidIsValid( id ) && ( user = getUserByUuid( UUID.fromString( id ) ) ) != null )
        {
            return user;
        }

        if ( usernameIsValid( id ) && ( user = getUserByUsername( id ) ) != null )
        {
            return user;
        }

        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers( Collection<String> uids )
    {
        return userStore.getByUid( uids );
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsersBetweenByName( String name, int first, int max )
    {
        UserQueryParams params = new UserQueryParams();
        params.setQuery( name );
        params.setFirst( first );
        params.setMax( max );

        return userStore.getUsers( params );
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsers( UserQueryParams params )
    {
        return getUsers( params, null );
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public int getUserCount()
    {
        return userStore.getUserCount();
    }

    private void handleUserQueryParams( UserQueryParams params )
    {
        boolean canGrantOwnRoles = (Boolean) systemSettingManager.getSystemSetting( SettingKey.CAN_GRANT_OWN_USER_AUTHORITY_GROUPS );
        params.setDisjointRoles( !canGrantOwnRoles );

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
            params.setOrganisationUnits( Lists.newArrayList( params.getUser().getOrganisationUnits() ) );
        }
    }

    private boolean validateUserQueryParams(UserQueryParams params)
    {
        if ( params.isCanManage() && (params.getUser() == null || !params.getUser().hasManagedGroups()) )
        {
            log.warn( "Cannot get managed users as user does not have any managed groups" );
            return false;
        }

        if ( params.isAuthSubset() && (params.getUser() == null || !params.getUser().getUserCredentials().hasAuthorities()) )
        {
            log.warn( "Cannot get users with authority subset as user does not have any authorities" );
            return false;
        }

        if ( params.isDisjointRoles() && (params.getUser() == null || !params.getUser().getUserCredentials().hasUserAuthorityGroups()) )
        {
            log.warn( "Cannot get users with disjoint roles as user does not have any user roles" );
            return false;
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getUsersByPhoneNumber( String phoneNumber )
    {
        UserQueryParams params = new UserQueryParams();
        params.setPhoneNumber( phoneNumber );
        return getUsers( params );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLastSuperUser( UserCredentials userCredentials )
    {
        if ( !userCredentials.isSuper() )
        {
            return false; // Cannot be last if not super user
        }

        Collection<UserCredentials> users = userCredentialsStore.getAll();

        for ( UserCredentials user : users )
        {
            if ( user.isSuper() && !user.equals( userCredentials ) )
            {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLastSuperRole( UserAuthorityGroup userAuthorityGroup )
    {
        Collection<UserAuthorityGroup> groups = userAuthorityGroupStore.getAll();

        for ( UserAuthorityGroup group : groups )
        {
            if ( group.isSuper() && group.getId() != userAuthorityGroup.getId() )
            {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddOrUpdateUser( Collection<String> userGroups )
    {
        return canAddOrUpdateUser( userGroups, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canAddOrUpdateUser( Collection<String> userGroups, User currentUser )
    {
        if ( currentUser == null )
        {
            return false;
        }

        boolean canAdd = currentUser.getUserCredentials().isAuthorized( UserGroup.AUTH_USER_ADD );

        if ( canAdd )
        {
            return true;
        }

        boolean canAddInGroup = currentUser.getUserCredentials().isAuthorized( UserGroup.AUTH_USER_ADD_IN_GROUP );

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
    // UserAuthorityGroup
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
        userAuthorityGroupStore.save( userAuthorityGroup );
        return userAuthorityGroup.getId();
    }

    @Override
    @Transactional
    public void updateUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
        userAuthorityGroupStore.update( userAuthorityGroup );
    }

    @Override
    @Transactional
    public void deleteUserAuthorityGroup( UserAuthorityGroup userAuthorityGroup )
    {
        userAuthorityGroupStore.delete( userAuthorityGroup );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAuthorityGroup> getAllUserAuthorityGroups()
    {
        return userAuthorityGroupStore.getAll();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthorityGroup getUserAuthorityGroup( long id )
    {
        return userAuthorityGroupStore.get( id );
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthorityGroup getUserAuthorityGroup( String uid )
    {
        return userAuthorityGroupStore.getByUid( uid );
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuthorityGroup getUserAuthorityGroupByName( String name )
    {
        return userAuthorityGroupStore.getByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAuthorityGroup> getUserRolesByUid( Collection<String> uids )
    {
        return userAuthorityGroupStore.getByUid( uids );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAuthorityGroup> getUserRolesBetween( int first, int max )
    {
        return userAuthorityGroupStore.getAllOrderedName( first, max );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAuthorityGroup> getUserRolesBetweenByName( String name, int first, int max )
    {
        return userAuthorityGroupStore.getAllLikeName( name, first, max );
    }

    @Override
    @Transactional(readOnly = true)
    public int countDataSetUserAuthorityGroups( DataSet dataSet )
    {
        return userAuthorityGroupStore.countDataSetUserAuthorityGroups( dataSet );
    }

    @Override
    @Transactional(readOnly = true)
    public void canIssueFilter( Collection<UserAuthorityGroup> userRoles )
    {
        User user = currentUserService.getCurrentUser();

        boolean canGrantOwnUserAuthorityGroups = (Boolean) systemSettingManager.getSystemSetting( SettingKey.CAN_GRANT_OWN_USER_AUTHORITY_GROUPS );

        FilterUtils.filter( userRoles, new UserAuthorityGroupCanIssueFilter( user, canGrantOwnUserAuthorityGroups ) );
    }

    // -------------------------------------------------------------------------
    // UserCredentials
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addUserCredentials( UserCredentials userCredentials )
    {
        userCredentialsStore.save( userCredentials );
        return userCredentials.getId();
    }

    @Override
    @Transactional
    public void updateUserCredentials( UserCredentials userCredentials )
    {
        userCredentialsStore.update( userCredentials );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserCredentials> getAllUserCredentials()
    {
        return userCredentialsStore.getAll();
    }

    @Override
    @Transactional
    public void encodeAndSetPassword( User user, String rawPassword )
    {
        encodeAndSetPassword( user.getUserCredentials(), rawPassword );
    }

    @Override
    @Transactional
    public void encodeAndSetPassword( UserCredentials userCredentials, String rawPassword )
    {
        if ( StringUtils.isEmpty( rawPassword ) && !userCredentials.isExternalAuth() )
        {
            return; // Leave unchanged if internal authentication and no password supplied
        }

        if ( userCredentials.isExternalAuth() )
        {
            userCredentials.setPassword( UserService.PW_NO_INTERNAL_LOGIN );

            return; // Set unusable, not-encoded password if external authentication
        }

        boolean isNewPassword = StringUtils.isBlank( userCredentials.getPassword() ) ||
            !passwordManager.matches( rawPassword, userCredentials.getPassword() );

        if ( isNewPassword )
        {
            userCredentials.setPasswordLastUpdated( new Date() );
        }

        // Encode and set password

        userCredentials.setPassword( passwordManager.encode( rawPassword ) );
        userCredentials.getPreviousPasswords().add( passwordManager.encode( rawPassword ) );
    }

    @Override
    @Transactional(readOnly = true)
    public UserCredentials getUserCredentialsByUsername( String username )
    {
        return userCredentialsStore.getUserCredentialsByUsername( username );
    }

    @Override
    @Transactional( readOnly = true )
    public UserCredentials getUserCredentialsWithEagerFetchAuthorities( String username )
    {
        UserCredentials userCredentials = userCredentialsStore.getUserCredentialsByUsername( username );

        if ( userCredentials != null )
        {
            userCredentials.getAllAuthorities();
        }

        return userCredentials;
    }

    @Override
    @Transactional(readOnly = true)
    public UserCredentials getUserCredentialsByOpenId( String openId )
    {
        return userCredentialsStore.getUserCredentialsByOpenId( openId );
    }

    @Override
    @Transactional(readOnly = true)
    public UserCredentials getUserCredentialsByLdapId( String ldapId )
    {
        return userCredentialsStore.getUserCredentialsByLdapId( ldapId );
    }

    @Override
    @Transactional
    public void setLastLogin( String username )
    {
        UserCredentials credentials = getUserCredentialsByUsername( username );

        if ( credentials != null )
        {
            credentials.setLastLogin( new Date() );
            updateUserCredentials( credentials );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getActiveUsersCount( int days )
    {
        Calendar cal = PeriodType.createCalendarInstance();
        cal.add( Calendar.DAY_OF_YEAR, (days * -1) );

        return getActiveUsersCount( cal.getTime() );
    }

    @Override
    @Transactional(readOnly = true)
    public int getActiveUsersCount( Date since )
    {
        UserQueryParams params = new UserQueryParams();
        params.setLastLogin( since );

        return getUserCount( params );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean credentialsNonExpired( UserCredentials credentials )
    {
        int credentialsExpires = systemSettingManager.credentialsExpires();

        if ( credentialsExpires == 0 )
        {
            return true;
        }

        if ( credentials == null || credentials.getPasswordLastUpdated() == null )
        {
            return true;
        }

        int months = DateUtils.monthsBetween( credentials.getPasswordLastUpdated(), new Date() );

        return months < credentialsExpires;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ErrorReport> validateUser( User user, User currentUser )
    {
        List<ErrorReport> errors = new ArrayList<>();

        if ( currentUser == null || currentUser.getUserCredentials() == null || user == null || user.getUserCredentials() == null )
        {
            return errors;
        }

        // Validate user role

        boolean canGrantOwnUserAuthorityGroups = (Boolean) systemSettingManager.getSystemSetting( SettingKey.CAN_GRANT_OWN_USER_AUTHORITY_GROUPS );

        List<UserAuthorityGroup> roles = userAuthorityGroupStore.getByUid( user.getUserCredentials()
            .getUserAuthorityGroups().stream().map( BaseIdentifiableObject::getUid ).collect( Collectors.toList() ) );

        roles.forEach( ur ->
        {
            if ( !currentUser.getUserCredentials().canIssueUserRole( ur, canGrantOwnUserAuthorityGroups ) )
            {
                errors.add( new ErrorReport( UserAuthorityGroup.class, ErrorCode.E3003, currentUser.getUsername(), ur.getName() ) );
            }
        } );

        // Validate user group

        boolean canAdd = currentUser.getUserCredentials().isAuthorized( UserGroup.AUTH_USER_ADD );

        if ( canAdd )
        {
            return errors;
        }

        boolean canAddInGroup = currentUser.getUserCredentials().isAuthorized( UserGroup.AUTH_USER_ADD_IN_GROUP );

        if ( !canAddInGroup )
        {
            errors.add( new ErrorReport( UserGroup.class, ErrorCode.E3004, currentUser ) );
            return errors;
        }

        user.getGroups().forEach( ug ->
        {
            if ( ! ( currentUser.canManage( ug ) || userGroupService.canAddOrRemoveMember( ug.getUid() ) ) )
            {
                errors.add( new ErrorReport( UserGroup.class, ErrorCode.E3005, currentUser, ug ) );
            }
        } );

        return errors;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getExpiringUsers()
    {
        int daysBeforePasswordChangeRequired = (Integer) systemSettingManager.getSystemSetting( SettingKey.CREDENTIALS_EXPIRES ) * 30;

        Date daysPassed = new DateTime( new Date() ).minusDays( daysBeforePasswordChangeRequired - EXPIRY_THRESHOLD ).toDate();

        UserQueryParams userQueryParams = new UserQueryParams()
            .setDisabled( false )
            .setPasswordLastUpdated( daysPassed );

        return userStore.getExpiringUsers( userQueryParams );
    }

    @Override
    @Transactional
    public void set2FA( User user, Boolean twoFa )
    {
        user.getUserCredentials().setTwoFA( twoFa );

        updateUser( user );
    }

    @Override
    public void expireActiveSessions( UserCredentials credentials )
    {
        List<SessionInformation> sessions = sessionRegistry.getAllSessions( credentials, false );

        sessions.forEach( SessionInformation::expireNow );
    }
}
