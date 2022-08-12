package org.hisp.dhis.cacheinvalidation.redis;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Profile( { "!test", "!test-h2" } )
@Conditional( value = RedisCacheInvalidationEnabledCondition.class )
@Service
public class RedisCacheInvalidationSubscriptionService
{
    @Autowired
    private CacheInvalidationListener cacheInvalidationListener;

    @Autowired
    @Qualifier( "pubSubConnection" )
    private StatefulRedisPubSubConnection<String, String> pubSubConnection;

    public void start()
    {
        log.info( "RedisCacheInvalidationSubscriptionService started" );

        pubSubConnection.addListener( cacheInvalidationListener );

        RedisPubSubAsyncCommands<String, String> async = pubSubConnection.async();
        async.subscribe( RedisCacheInvalidationConfiguration.CHANNEL_NAME );
    }
}
