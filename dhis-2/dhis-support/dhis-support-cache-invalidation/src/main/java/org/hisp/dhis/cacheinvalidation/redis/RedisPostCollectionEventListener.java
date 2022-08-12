package org.hisp.dhis.cacheinvalidation.redis;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.lettuce.core.api.StatefulRedisConnection;
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

@Slf4j
@Component
public class RedisPostCollectionEventListener implements PostCollectionRecreateEventListener,
    PreCollectionRemoveEventListener, PreCollectionUpdateEventListener
{

    @Autowired
    @Qualifier( "cacheInvalidationUid" )
    private String uid;

    @Autowired
    @Qualifier( "redisConnection" )
    private StatefulRedisConnection<String, String> redisConnection;

    @Override public void onPreUpdateCollection( PreCollectionUpdateEvent event )
    {
        log.info( "onPostUpdateCollection" );
        CollectionEntry collectionEntry = getCollectionEntry( event );
        onCollectionAction( event, event.getCollection(), collectionEntry.getSnapshot(), collectionEntry );
    }

    @Override public void onPreRemoveCollection( PreCollectionRemoveEvent event )
    {
        log.info( "onPostRemoveCollection" );
        CollectionEntry collectionEntry = getCollectionEntry( event );
        onCollectionAction( event, null, collectionEntry.getSnapshot(), collectionEntry );
    }

    @Override public void onPostRecreateCollection( PostCollectionRecreateEvent event )
    {
        log.info( "onPostRecreateCollection" );
        CollectionEntry collectionEntry = getCollectionEntry( event );
        onCollectionAction( event, event.getCollection(), null, collectionEntry );
    }

    protected CollectionEntry getCollectionEntry( AbstractCollectionEvent event )
    {
        return event.getSession().getPersistenceContext().getCollectionEntry( event.getCollection() );
    }

    protected void onCollectionAction( AbstractCollectionEvent event, PersistentCollection newColl,
        Serializable oldColl,
        CollectionEntry collectionEntry )
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
            String message =
                uid + ":" + "collection:" + affectedOwnerEntityName + ":" + role + ":" + affectedOwnerIdOrNull;
            redisConnection.sync().publish( RedisCacheInvalidationConfiguration.CHANNEL_NAME, message );
        }

    }

}
