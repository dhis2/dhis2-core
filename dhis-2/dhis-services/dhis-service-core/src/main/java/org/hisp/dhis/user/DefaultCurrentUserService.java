package org.hisp.dhis.user;

/*
 * Copyright (c) 2004-2017, University of Oslo
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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.hisp.dhis.commons.util.SystemUtils;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.security.spring.AbstractSpringSecurityCurrentUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Service for retrieving information about the currently
 * authenticated user.
 * 
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
    private static final Cache<String, Integer> USERNAME_ID_CACHE = Caffeine.newBuilder()
        .expireAfterAccess( 1, TimeUnit.HOURS )
        .initialCapacity( 200 )
        .maximumSize( SystemUtils.isTestRun() ? 0 : 2000 )
        .build();
    
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    @Autowired
    private CurrentUserStore currentUserStore;

    // -------------------------------------------------------------------------
    // CurrentUserService implementation
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public User getCurrentUser()
    {
        String username = getCurrentUsername();

        if ( username == null )
        {
            return null;
        }

        UserCredentials userCredentials = currentUserStore.getUserCredentialsByUsername( username );

        if ( userCredentials == null )
        {
            return null;
        }

        return userCredentials.getUserInfo();
    }

    @Override
    public UserInfo getCurrentUserInfo()
    {
        UserDetails userDetails = getCurrentUserDetails();
        
        if ( userDetails == null )
        {
            return null;
        }
        
        Integer userId = USERNAME_ID_CACHE.get( userDetails.getUsername(), un -> getUserId( un ) );
        
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
    @Transactional
    public boolean currentUserIsSuper()
    {
        User user = getCurrentUser();

        return user != null && user.isSuper();
    }

    @Override
    @Transactional
    public Set<OrganisationUnit> getCurrentUserOrganisationUnits()
    {
        User user = getCurrentUser();
        
        return user != null ? new HashSet<>( user.getOrganisationUnits() ) : new HashSet<>();
    }
    
    @Override
    @Transactional
    public boolean currentUserIsAuthorized( String auth )
    {
        User user = getCurrentUser();
        
        return user != null && user.getUserCredentials().isAuthorized( auth );
    }
}
