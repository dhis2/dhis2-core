package org.hisp.dhis.cacheinvalidation.redis;

import io.lettuce.core.pubsub.RedisPubSubListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CacheInvalidationListener implements RedisPubSubListener<String, String>
{
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
    }
}