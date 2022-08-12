package org.hisp.dhis.cacheinvalidation.redis;

import java.io.Serializable;
import java.util.Objects;

import org.hisp.dhis.cache.PaginationCacheManager;
import org.hisp.dhis.cache.QueryCacheManager;
import org.hisp.dhis.cacheinvalidation.KnownTransactionsService;
import org.hisp.dhis.cacheinvalidation.TableNameToEntityMapping;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.period.PeriodService;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;

import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CacheInvalidationListener implements RedisPubSubListener<String, String>
{
    @Autowired
    @Qualifier( "cacheInvalidationUid" )
    private String cacheInvalidationUid;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private KnownTransactionsService knownTransactionsService;

    @Autowired
    private PaginationCacheManager paginationCacheManager;

    @Autowired
    private QueryCacheManager queryCacheManager;

    @Autowired
    private TableNameToEntityMapping tableNameToEntityMapping;

    @Autowired
    private IdentifiableObjectManager idObjectManager;

    @Autowired
    private TrackedEntityAttributeService trackedEntityAttributeService;

    @Autowired
    private TrackedEntityInstanceService trackedEntityInstanceService;

    @Autowired
    private PeriodService periodService;

    public enum Operation
    {
        READ( "read" ),
        CREATE( "insert" ),
        UPDATE( "update" ),
        DELETE( "delete" ),
        COLLECTION( "collection" );

        private final String code;

        Operation( String code )
        {
            this.code = code;
        }

        public static Operation forCode( String code )
        {
            Operation[] var1 = values();
            int var2 = var1.length;

            for ( int var3 = 0; var3 < var2; ++var3 )
            {
                Operation op = var1[var3];
                if ( op.code().equalsIgnoreCase( code ) )
                {
                    return op;
                }
            }

            return null;
        }

        public String code()
        {
            return this.code;
        }
    }

    @Override public void message( String pattern, String channel, String message )
    {
        log.debug( "Got {} on channel {}", message, channel );
    }

    @Override public void subscribed( String channel, long count )
    {
        log.debug( "Subscribed to {}", channel );
    }

    @Override public void psubscribed( String pattern, long count )
    {
        log.debug( "Subscribed to pattern {}", pattern );
    }

    @Override public void unsubscribed( String channel, long count )
    {
        log.debug( "Unsubscribed from {}", channel );
    }

    @Override public void punsubscribed( String pattern, long count )
    {
        log.debug( "Unsubscribed from pattern {}", pattern );
    }

    @Override
    public void message( String channel, String message )
    {
        log.debug( "Got {} on channel {}", message, channel );

        try
        {
            handleMessage( message );
        }
        catch ( Exception e )
        {
            log.error( "Error handling message: " + message, e );
        }
    }

    private void handleMessage( String message )
        throws Exception
    {
        String[] parts = message.split( ":" );
        String uid = parts[0];
        // If the UID is the same, it means the event is coming from this server.
        if ( cacheInvalidationUid.equals( uid ) )
        {
            return;
        }

        Operation type = Operation.forCode( parts[1] );

        if ( Operation.COLLECTION == type )
        {
            String role = parts[3];
            Long entityId = Long.parseLong( parts[4] );
            sessionFactory.getCache().evictCollectionData( role, entityId );
            return;
        }

        Long entityId = Long.parseLong( parts[3] );
        Class<?> entityClass = Class.forName( parts[2] );
        Objects.requireNonNull( entityClass, "Entity class can't be null" );

        if ( Operation.CREATE == type )
        {
            // Make sure queries will re-fetch to capture the new object.
            queryCacheManager.evictQueryCache( sessionFactory.getCache(), entityClass );
            paginationCacheManager.evictCache( entityClass.getName() );
            // Try to fetch the new entity, so it might get cached.
            tryFetchNewEntity( entityId, entityClass );
        }
        else if ( Operation.UPDATE == type )
        {
            sessionFactory.getCache().evict( entityClass, entityId );
        }
        else if ( Operation.DELETE == type )
        {
            queryCacheManager.evictQueryCache( sessionFactory.getCache(), entityClass );
            paginationCacheManager.evictCache( entityClass.getName() );
            sessionFactory.getCache().evict( entityClass, entityId );
        }
    }

    private void tryFetchNewEntity( Serializable entityId, Class<?> entityClass )
    {
        try (Session session = sessionFactory.openSession())
        {
            session.get( entityClass, entityId );
        }
        catch ( Exception e )
        {
            log.warn(
                String.format( "Fetching new entity failed, failed to execute get query. "
                        + "entityId=%s, entityClass=%s",
                    entityId, entityClass ),
                e );
            if ( e instanceof HibernateException )
            {
                log.info( "tryFetchNewEntity. HibernateException: {}", e.getMessage() );
                return;
            }

            throw e;
        }
    }

}
