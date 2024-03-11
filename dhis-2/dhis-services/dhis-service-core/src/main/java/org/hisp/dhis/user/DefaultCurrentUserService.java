package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.spring.AbstractSpringSecurityCurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

/**
 * Service for retrieving information about the currently
 * authenticated user.
 * <p>
 * Note that most methods are transactional, except for
 * retrieving current UserInfo.
 *
 * @author Torgeir Lorange Ostby
 */
public class DefaultCurrentUserService
    extends AbstractSpringSecurityCurrentUserService
{
    /**
     * Cache for user IDs. Key is username. Disabled during test phase.
     * Take care not to cache user info which might change during runtime.
     */
    private static Cache<Integer> USERNAME_ID_CACHE;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CurrentUserStore currentUserStore;

    @Autowired
    private SessionRegistry sessionRegistry;
    
    @Autowired
    private CacheProvider cacheProvider;
    
    @PostConstruct
    public void init()
    {
        USERNAME_ID_CACHE = cacheProvider.newCacheBuilder( Integer.class )
            .forRegion( "userIdCache" )
            .expireAfterAccess( 1, TimeUnit.HOURS )
            .withInitialCapacity( 200 )
            .forceInMemory()
            .withMaximumSize( SystemUtils.isTestRun() ? 0 : 4000 )
            .build();
    }


    // -------------------------------------------------------------------------
    // CurrentUserService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser()
    {
        String username = getCurrentUsername();

        if ( username == null )
        {
            return null;
        }

        Integer userId = USERNAME_ID_CACHE.get( username, this::getUserId ).orElse( null );

        if ( userId == null )
        {
            return null;
        }

        return currentUserStore.getUser( userId );
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getCurrentUserInfo()
    {
        UserDetails userDetails = getCurrentUserDetails();

        if ( userDetails == null )
        {
            return null;
        }

        Integer userId = USERNAME_ID_CACHE.get( userDetails.getUsername(), un -> getUserId( un ) ).orElse( null );

        if ( userId == null )
        {
            return null;
        }

        Set<String> authorities = userDetails.getAuthorities()
            .stream().map( GrantedAuthority::getAuthority )
            .collect( Collectors.toSet() );

        return new UserInfo( userId, userDetails.getUsername(), authorities );
    }

    private Integer getUserId( String username )
    {
        UserCredentials credentials = currentUserStore.getUserCredentialsByUsername( username );

        return credentials != null ? credentials.getId() : null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean currentUserIsSuper()
    {
        User user = getCurrentUser();

        return user != null && user.isSuper();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<OrganisationUnit> getCurrentUserOrganisationUnits()
    {
        User user = getCurrentUser();

        return user != null ? new HashSet<>( user.getOrganisationUnits() ) : new HashSet<>();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean currentUserIsAuthorized( String auth )
    {
        User user = getCurrentUser();

        return user != null && user.getUserCredentials().isAuthorized( auth );
    }

    @Override
    @Transactional( readOnly = true )
    public void expireUserSessions()
    {
        UserDetails userDetails = getCurrentUserDetails();

        if ( userDetails != null )
        {
            List<SessionInformation> sessions = sessionRegistry.getAllSessions( userDetails, false );
            sessions.forEach( SessionInformation::expireNow );
        }
    }
}
