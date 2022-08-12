package org.hisp.dhis.cacheinvalidation.redis;


import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    RedisCacheInvalidationConfiguration.RedisCacheInvalidationConnection redisCacheInvalidationConnection;

    public void start()
    {
        log.info( "RedisCacheInvalidationSubscriptionService started" );

        StatefulRedisPubSubConnection<String, String> pubSubConnection = redisCacheInvalidationConnection.getPubSubConnection();
        pubSubConnection.addListener( new CacheInvalidationListener() );

        RedisPubSubAsyncCommands<String, String> async = pubSubConnection.async();
        async.subscribe( "channel" );
    }
}
