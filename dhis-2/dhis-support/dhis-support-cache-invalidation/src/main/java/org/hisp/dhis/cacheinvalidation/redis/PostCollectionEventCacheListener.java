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
package org.hisp.dhis.cacheinvalidation.redis;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.event.spi.AbstractCollectionEvent;
import org.hibernate.event.spi.PostCollectionRecreateEvent;
import org.hibernate.event.spi.PostCollectionRecreateEventListener;
import org.hibernate.event.spi.PreCollectionRemoveEvent;
import org.hibernate.event.spi.PreCollectionRemoveEventListener;
import org.hibernate.event.spi.PreCollectionUpdateEvent;
import org.hibernate.event.spi.PreCollectionUpdateEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Listens to Hibernate events and publishes a message to Redis when a
 * collection is updated.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class PostCollectionEventCacheListener implements PostCollectionRecreateEventListener,
    PreCollectionRemoveEventListener, PreCollectionUpdateEventListener
{
    @Autowired
    @Qualifier( "cacheInvalidationUid" )
    private String uid;

    @Autowired
    @Qualifier( "redisConnection" )
    private StatefulRedisConnection<String, String> redisConnection;

    @Override
    public void onPreUpdateCollection( PreCollectionUpdateEvent event )
    {
        log.info( "onPostUpdateCollection" );
        CollectionEntry collectionEntry = getCollectionEntry( event );
        onCollectionAction( event, event.getCollection(), collectionEntry.getSnapshot() );
    }

    @Override
    public void onPreRemoveCollection( PreCollectionRemoveEvent event )
    {
        log.info( "onPostRemoveCollection" );
        CollectionEntry collectionEntry = getCollectionEntry( event );
        onCollectionAction( event, null, collectionEntry.getSnapshot() );
    }

    @Override
    public void onPostRecreateCollection( PostCollectionRecreateEvent event )
    {
        log.info( "onPostRecreateCollection" );
        onCollectionAction( event, event.getCollection(), null );
    }

    protected CollectionEntry getCollectionEntry( AbstractCollectionEvent event )
    {
        return event.getSession().getPersistenceContext().getCollectionEntry( event.getCollection() );
    }

    protected void onCollectionAction( AbstractCollectionEvent event, PersistentCollection newColl,
        Serializable oldColl )
    {
        boolean b1 = newColl instanceof Collection;
        boolean b2 = oldColl instanceof Collection;
        boolean b3 = oldColl instanceof List;
        boolean b4 = oldColl instanceof Map;
        boolean b5 = oldColl instanceof Set;

        Integer nradded = null;
        Integer nrremoved = null;

        Collection<?> added = (b1 ? (Collection<?>) newColl : null);
        if ( added != null )
        {
            nradded = added.size();
        }

        if ( b2 )
        {
            Collection old = (Collection) oldColl;
            nrremoved = old.size();
        }

        if ( b3 )
        {
            List old = (List) oldColl;
            nrremoved = old.size();
        }

        if ( b4 )
        {
            Map old = (Map) oldColl;
            nrremoved = old.size();
        }

        if ( b5 )
        {
            Set old = (Set) oldColl;
            nrremoved = old.size();
        }

        if ( (nradded != null && nrremoved != null) && !Objects.equals( nradded, nrremoved ) )
        {
            String affectedOwnerEntityName = event.getAffectedOwnerEntityName();
            String role = event.getCollection().getRole();

            Serializable affectedOwnerIdOrNull = event.getAffectedOwnerIdOrNull();
            String message = uid + ":" + "collection:" + affectedOwnerEntityName + ":" + role + ":"
                + affectedOwnerIdOrNull;
            redisConnection.sync().publish( RedisCacheInvalidationConfiguration.CHANNEL_NAME, message );
        }
    }
}
