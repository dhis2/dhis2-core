/*
 *
 *  * Copyright (c) 2004-2020, University of Oslo
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, this
 *  * list of conditions and the following disclaimer.
 *  *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *  * this list of conditions and the following disclaimer in the documentation
 *  * and/or other materials provided with the distribution.
 *  * Neither the name of the HISP project nor the names of its contributors may
 *  * be used to endorse or promote products derived from this software without
 *  * specific prior written permission.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.hisp.dhis.user;

import com.google.common.base.Preconditions;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.commons.collection.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class DefaultUserGroupCacheService implements UserGroupCacheService
{
    private final SessionFactory sessionFactory;
    private Cache<UserGroup> userGroupCache;

    private final CacheProvider cacheProvider;

    public DefaultUserGroupCacheService( SessionFactory sessionFactory, CacheProvider cacheProvider )
    {
        Preconditions.checkNotNull( sessionFactory );
        Preconditions.checkNotNull( cacheProvider );
        this.sessionFactory = sessionFactory;
        this.cacheProvider = cacheProvider;
    }

    @PostConstruct
    public void init()
    {
        userGroupCache = cacheProvider.newCacheBuilder( UserGroup.class )
            .forRegion( "userGroupCache" )
            .expireAfterWrite( 12, TimeUnit.HOURS )
            .build();
    }

    @Override
    @Transactional( readOnly = true )
    public UserGroup getUserGroupFromCache( String groupId )
    {
        return userGroupCache.get( groupId, g -> getByUid( groupId ) ).orElse( null );
    }

    @Override
    @Transactional( readOnly = true )
    public void reloadUserGroupCache()
    {
        userGroupCache.invalidateAll();

        getAll().forEach( group -> userGroupCache.put( group.getUid(), group ) );
    }

    @Override
    public void updateUser( User user )
    {
        if ( CollectionUtils.isEmpty( user.getGroups() ) )
        {
            return;
        }

        user.getGroups().forEach( group -> userGroupCache.put( group.getUid(), group ) );
    }

    @Override
    @Transactional( readOnly = true )
    public void put( UserGroup userGroup )
    {
        userGroup.getMembers();
        userGroupCache.put( userGroup.getUid(), userGroup );
    }

    @Override
    public void invalidateAll()
    {
        userGroupCache.invalidateAll();
    }

    @Override
    @Transactional( readOnly = true )
    public UserGroup get( String key )
    {
        Optional<UserGroup> userGroup = userGroupCache.get( key, ug -> {
            UserGroup group = getByUid( key );
            group.getMembers();
            return group;
        } );

        return userGroup.orElse( null );
    }

    @Override
    public void invalidate( String key )
    {
        userGroupCache.invalidate( key );
    }

    private UserGroup getByUid( String uid )
    {
        String sql = "from UserGroup where uid =:uid";
        Query<UserGroup> query = sessionFactory.getCurrentSession().createQuery( sql );
        query.setParameter( "uid", uid );
        return query.getSingleResult();
    }

    private List<UserGroup> getAll()
    {
        String sql = "from UserGroup";
        Query<UserGroup> query = sessionFactory.getCurrentSession().createQuery( sql );
        return query.getResultList();
    }
}
