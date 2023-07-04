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

import static com.google.common.base.Preconditions.*;

import java.util.*;

import org.hisp.dhis.cache.*;
import org.hisp.dhis.security.acl.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.*;

/**
 * @author Lars Helge Overland
 */
@Service( "org.hisp.dhis.user.UserGroupService" )
public class DefaultUserGroupService
    implements UserGroupService
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final UserGroupStore userGroupStore;

    private final CurrentUserService currentUserService;

    private final AclService aclService;

    private final HibernateCacheManager cacheManager;

    private Cache<String> userGroupNameCache;

    public DefaultUserGroupService( UserGroupStore userGroupStore,
        AclService aclService, HibernateCacheManager cacheManager, CurrentUserService currentUserService,
        CacheProvider cacheProvider )
    {
        checkNotNull( userGroupStore );
        checkNotNull( currentUserService );
        checkNotNull( aclService );
        checkNotNull( cacheManager );

        this.userGroupStore = userGroupStore;
        this.aclService = aclService;
        this.cacheManager = cacheManager;
        this.currentUserService = currentUserService;

        userGroupNameCache = cacheProvider.createUserGroupNameCache();
    }

    // -------------------------------------------------------------------------
    // UserGroup
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public long addUserGroup( UserGroup userGroup )
    {
        userGroupStore.save( userGroup );

        return userGroup.getId();
    }

    @Override
    @Transactional
    public void deleteUserGroup( UserGroup userGroup )
    {
        userGroupStore.delete( userGroup );
    }

    @Override
    @Transactional
    public void updateUserGroup( UserGroup userGroup )
    {
        userGroupStore.update( userGroup );

        // Clear query cache due to sharing and user group membership

        cacheManager.clearQueryCache();
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserGroup> getAllUserGroups()
    {
        return userGroupStore.getAll();
    }

    @Override
    @Transactional( readOnly = true )
    public UserGroup getUserGroup( long userGroupId )
    {
        return userGroupStore.get( userGroupId );
    }

    @Override
    @Transactional( readOnly = true )
    public UserGroup getUserGroup( String uid )
    {
        return userGroupStore.getByUid( uid );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean canAddOrRemoveMember( String uid )
    {
        return canAddOrRemoveMember( uid, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional( readOnly = true )
    public boolean canAddOrRemoveMember( String uid, User currentUser )
    {
        UserGroup userGroup = getUserGroup( uid );

        if ( userGroup == null || currentUser == null )
        {
            return false;
        }

        boolean canUpdate = aclService.canUpdate( currentUser, userGroup );
        boolean canAddMember = currentUser
            .isAuthorized( UserGroup.AUTH_ADD_MEMBERS_TO_READ_ONLY_USER_GROUPS );

        return canUpdate || canAddMember;
    }

    @Override
    @Transactional
    public void addUserToGroups( User user, Collection<String> uids )
    {
        addUserToGroups( user, uids, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional
    public void addUserToGroups( User user, Collection<String> uids, User currentUser )
    {
        for ( String uid : uids )
        {
            if ( canAddOrRemoveMember( uid, currentUser ) )
            {
                UserGroup userGroup = getUserGroup( uid );
                userGroup.addUser( user );
                userGroupStore.updateNoAcl( userGroup );
            }
        }
    }

    @Override
    @Transactional
    public void removeUserFromGroups( User user, Collection<String> uids )
    {
        for ( String uid : uids )
        {
            if ( canAddOrRemoveMember( uid ) )
            {
                UserGroup userGroup = getUserGroup( uid );
                userGroup.removeUser( user );
                userGroupStore.updateNoAcl( userGroup );
            }
        }
    }

    @Override
    @Transactional
    public void updateUserGroups( User user, Collection<String> uids )
    {
        updateUserGroups( user, uids, currentUserService.getCurrentUser() );
    }

    @Override
    @Transactional
    public void updateUserGroups( User user, Collection<String> uids, User currentUser )
    {
        Collection<UserGroup> updates = getUserGroupsByUid( uids );

        for ( UserGroup userGroup : new HashSet<>( user.getGroups() ) )
        {
            if ( !updates.contains( userGroup ) && canAddOrRemoveMember( userGroup.getUid(), currentUser ) )
            {
                userGroup.removeUser( user );
                userGroupStore.update( userGroup, currentUser );
            }
        }

        for ( UserGroup userGroup : updates )
        {
            if ( canAddOrRemoveMember( userGroup.getUid(), currentUser ) )
            {
                userGroup.addUser( user );
                userGroupStore.update( userGroup, currentUser );
            }
        }
    }

    private Collection<UserGroup> getUserGroupsByUid( Collection<String> uids )
    {
        return userGroupStore.getByUid( uids );
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserGroup> getUserGroupByName( String name )
    {
        return userGroupStore.getAllEqName( name );
    }

    @Override
    @Transactional( readOnly = true )
    public int getUserGroupCount()
    {
        return userGroupStore.getCount();
    }

    @Override
    @Transactional( readOnly = true )
    public int getUserGroupCountByName( String name )
    {
        return userGroupStore.getCountLikeName( name );
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserGroup> getUserGroupsBetween( int first, int max )
    {
        return userGroupStore.getAllOrderedName( first, max );
    }

    @Override
    @Transactional( readOnly = true )
    public List<UserGroup> getUserGroupsBetweenByName( String name, int first, int max )
    {
        return userGroupStore.getAllLikeName( name, first, max, false );
    }

    @Override
    @Transactional( readOnly = true )
    public String getDisplayName( String uid )
    {
        return userGroupNameCache.get( uid,
            n -> userGroupStore.getByUidNoAcl( uid ).getDisplayName() );
    }
}
