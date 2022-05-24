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

import java.util.HashSet;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This interface defined methods for getting access to the currently logged in
 * user and clearing the logged in state. If no user is logged in or the auto
 * access admin is active, all user access methods will return null.
 *
 * @author Torgeir Lorange Ostby
 * @version $Id: CurrentUserService.java 5708 2008-09-16 14:28:32Z larshelg $
 */
@Service( "org.hisp.dhis.user.CurrentUserService" )
@Slf4j
public class CurrentUserService
{
    private final UserStore userStore;

    private final Cache<CurrentUserGroupInfo> currentUserGroupInfoCache;

    public CurrentUserService( @Lazy UserStore userStore, CacheProvider cacheProvider )
    {
        checkNotNull( userStore );

        this.userStore = userStore;
        this.currentUserGroupInfoCache = cacheProvider.createCurrentUserGroupInfoCache();
    }

    /**
     * @return the username of the currently logged in user. If no user is
     *         logged in or the auto access admin is active, null is returned.
     */
    public String getCurrentUsername()
    {
        return CurrentUserUtil.getCurrentUsername();
    }

    @Transactional( readOnly = true )
    public User getCurrentUser()
    {
        String username = CurrentUserUtil.getCurrentUsername();

        return userStore.getUserByUsername( username );
    }

    @Transactional( readOnly = true )
    public boolean currentUserIsSuper()
    {
        User user = getCurrentUser();

        return user != null && user.isSuper();
    }

    @Transactional( readOnly = true )
    public Set<OrganisationUnit> getCurrentUserOrganisationUnits()
    {
        User user = getCurrentUser();

        return user != null ? new HashSet<>( user.getOrganisationUnits() ) : new HashSet<>();
    }

    @Transactional( readOnly = true )
    public boolean currentUserIsAuthorized( String auth )
    {
        User user = getCurrentUser();

        return user != null && user.isAuthorized( auth );
    }

    @Transactional( readOnly = true )
    public CurrentUserGroupInfo getCurrentUserGroupsInfo()
    {
        return getCurrentUserGroupsInfo( getCurrentUser() );
    }

    @Transactional( readOnly = true )
    public CurrentUserGroupInfo getCurrentUserGroupsInfo( User user )
    {
        if ( user == null )
        {
            return null;
        }

        return currentUserGroupInfoCache
            .get( user.getUsername(), this::getCurrentUserGroupsInfo );
    }

    @Transactional( readOnly = true )
    public void invalidateUserGroupCache( String username )
    {
        try
        {
            currentUserGroupInfoCache.invalidate( username );
        }
        catch ( NullPointerException exception )
        {
            // Ignore if key doesn't exist
        }
    }

    private CurrentUserGroupInfo getCurrentUserGroupsInfo( String username )
    {
        if ( username == null )
        {
            return null;
        }

        User currentUser = getCurrentUser();
        if ( currentUser == null )
        {
            log.warn( "User is null, this should only happen at startup!" );
            return null;
        }
        return userStore.getCurrentUserGroupInfo( currentUser );
    }
}
