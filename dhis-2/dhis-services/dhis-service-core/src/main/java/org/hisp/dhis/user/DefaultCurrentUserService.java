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
package org.hisp.dhis.user;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.spring.AbstractSpringSecurityCurrentUserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for retrieving information about the currently authenticated user.
 * <p>
 * Note that most methods are transactional, except for retrieving current
 * UserInfo.
 *
 * @author Torgeir Lorange Ostby
 */
@Service( "org.hisp.dhis.user.CurrentUserService" )
public class DefaultCurrentUserService
    extends AbstractSpringSecurityCurrentUserService
{
    /**
     * Cache for user IDs. Key is username. Disabled during test phase. Take
     * care not to cache user info which might change during runtime.
     */
    private final Cache<Long> usernameIdCache;

    /**
     * Cache contains Set of UserGroup UID for each user. Key is username. This
     * will be used for ACL check in
     * {@link org.hisp.dhis.common.hibernate.HibernateIdentifiableObjectStore}
     */
    private final Cache<CurrentUserGroupInfo> currentUserGroupInfoCache;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final UserStore userStore;

    public DefaultCurrentUserService( CacheProvider cacheProvider,
        @Lazy UserStore userStore )
    {
        checkNotNull( cacheProvider );
        checkNotNull( userStore );

        this.userStore = userStore;
        this.usernameIdCache = cacheProvider.createUserIdCacheCache( Long.class );
        this.currentUserGroupInfoCache = cacheProvider
            .createCurrentUserGroupInfoCache( CurrentUserGroupInfo.class );
    }

    // -------------------------------------------------------------------------
    // CurrentUserService implementation
    // -------------------------------------------------------------------------

    @Override
    public User getCurrentUser()
    {
        String username = getCurrentUsername();

        if ( username == null )
        {
            return null;
        }

        Long userId = usernameIdCache.get( username, this::getUserId ).orElse( null );

        if ( userId == null )
        {
            return null;
        }

        User user = userStore.getUser( userId );

        if ( user == null )
        {
            UserCredentials credentials = userStore.getUserCredentialsByUsername( username );

            user = userStore.getUser( credentials.getId() );

            if ( user == null )
            {
                throw new RuntimeException( "Could not retrieve current user!" );
            }
        }

        if ( user.getUserCredentials() == null )
        {
            throw new RuntimeException( "Could not retrieve current user credentials!" );
        }

        // TODO: this is pretty ugly way to retrieve auths
        user.getUserCredentials().getAllAuthorities();
        return user;
    }

    @Override
    @Transactional( readOnly = true )
    public User getCurrentUserInTransaction()
    {
        String username = getCurrentUsername();

        if ( username == null )
        {
            return null;
        }

        User user = null;

        Long userId = usernameIdCache.get( username, this::getUserId ).orElse( null );

        if ( userId != null )
        {
            user = userStore.getUser( userId );
        }

        if ( user == null )
        {
            UserCredentials credentials = userStore.getUserCredentialsByUsername( username );

            // Happens when user is anonymous aka. not logged in yet.
            if ( credentials == null )
            {
                return null;
            }

            user = userStore.getUser( credentials.getId() );

            if ( user == null )
            {
                throw new RuntimeException( "Could not retrieve current user!" );
            }
        }

        if ( user.getUserCredentials() == null )
        {
            throw new RuntimeException( "Could not retrieve current user credentials!" );
        }

        user.getUserCredentials().getAllAuthorities();

        return user;
    }

    @Override
    @Transactional( readOnly = true )
    public UserInfo getCurrentUserInfo()
    {
        String currentUsername = getCurrentUsername();

        if ( currentUsername == null )
        {
            return null;
        }

        Long userId = usernameIdCache.get( currentUsername, this::getUserId ).orElse( null );

        if ( userId == null )
        {
            return null;
        }

        return new UserInfo( userId, currentUsername, getCurrentUserAuthorities() );
    }

    @Override
    public Long getUserId( String username )
    {
        UserCredentials credentials = userStore.getUserCredentialsByUsername( username );

        return credentials != null ? credentials.getId() : null;
    }

    @Override
    @Transactional( readOnly = true )
    public boolean currentUserIsSuper()
    {
        User user = getCurrentUser();

        return user != null && user.isSuper();
    }

    @Override
    @Transactional( readOnly = true )
    public Set<OrganisationUnit> getCurrentUserOrganisationUnits()
    {
        User user = getCurrentUser();

        return user != null ? new HashSet<>( user.getOrganisationUnits() ) : new HashSet<>();
    }

    @Override
    @Transactional( readOnly = true )
    public boolean currentUserIsAuthorized( String auth )
    {
        User user = getCurrentUser();

        return user != null && user.getUserCredentials().isAuthorized( auth );
    }

    @Override
    public UserCredentials getCurrentUserCredentials()
    {
        return userStore.getUserCredentialsByUsername( getCurrentUsername() );
    }

    @Override
    public CurrentUserGroupInfo getCurrentUserGroupsInfo()
    {
        UserInfo currentUserInfo = getCurrentUserInfo();

        if ( currentUserInfo == null )
        {
            return null;
        }

        return currentUserGroupInfoCache
            .get( currentUserInfo.getUsername(), this::getCurrentUserGroupsInfo ).orElse( null );
    }

    @Override
    public CurrentUserGroupInfo getCurrentUserGroupsInfo( UserInfo userInfo )
    {
        if ( userInfo == null )
        {
            return null;
        }

        return currentUserGroupInfoCache
            .get( userInfo.getUsername(), this::getCurrentUserGroupsInfo ).orElse( null );
    }

    @Override
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

        Long userId = usernameIdCache.get( username, this::getUserId ).orElse( null );

        if ( userId == null )
        {
            return null;
        }

        return userStore.getCurrentUserGroupInfo( getCurrentUserInfo().getId() );
    }
}
