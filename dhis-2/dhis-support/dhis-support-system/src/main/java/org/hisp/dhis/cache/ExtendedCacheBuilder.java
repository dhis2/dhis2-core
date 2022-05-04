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
package org.hisp.dhis.cache;

import static java.lang.Integer.parseInt;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * A Builder class that helps in building Cache instances. Sensible defaults are
 * in place which can be modified with a fluent builder api.
 *
 * @author Ameen Mohamed
 *
 * @param <V> The Value type to be stored in cache
 */
@Slf4j
public class ExtendedCacheBuilder<V> extends SimpleCacheBuilder<V>
{
    private final DhisConfigurationProvider configuration;

    private final RedisTemplate<String, ?> redisTemplate;

    private boolean forceInMemory;

    private final Function<CacheBuilder<V>, Cache<V>> cappedLocalCacheFactory;

    public ExtendedCacheBuilder( RedisTemplate<String, ?> redisTemplate,
        DhisConfigurationProvider configuration, Function<CacheBuilder<V>, Cache<V>> cappedLocalCacheFactory )
    {
        this.configuration = configuration;
        this.redisTemplate = redisTemplate;
        this.forceInMemory = false;
        this.cappedLocalCacheFactory = cappedLocalCacheFactory;
    }

    /**
     * Configure the cache instance to use local inmemory storage even in
     * clustered or standalone environment. Ideally used in scenarios where
     * stale data is not critical and faster lookup is preferred.
     *
     * @return The builder instance.
     */
    @Override
    public CacheBuilder<V> forceInMemory()
    {
        this.forceInMemory = true;
        return this;
    }

    /**
     * Creates and returns a cacheInstance based on the system configuration and
     * the cache builder parameters. If {@code maximumSize} is 0 then a
     * NoOpCache instance will be returned which does not cache anything. This
     * can be used during system testings where cache has to be disabled. If
     * {@code maximumSize} is greater than 0 than based on {@code redis.enabled}
     * property in dhis.conf, either Redis backed implementation
     * {@link RedisCache} will be returned or a Local Caffeine backed cache
     * implementation {@link LocalCache} will be returned. For Local cache,
     * every instance created using this method will be logically separate and
     * will not share any state. However, when using Redis Cache, every instance
     * created using this method will use the same redis store.
     *
     * @return A cache instance based on the system configuration and input
     *         parameters. Returns one of {@link RedisCache}, {@link LocalCache}
     *         or {@link NoOpCache}
     */
    @Override
    public Cache<V> build()
    {
        if ( getMaximumSize() == 0 || isDisabled() )
        {
            log.debug( String.format( "NoOp Cache instance created for region:'%s'", getRegion() ) );
            return new NoOpCache<>( this );
        }
        if ( forceInMemory )
        {
            int capPercentage = parseInt( configuration.getProperty( ConfigurationKey.SYSTEM_CACHE_CAP_PERCENTAGE ) );
            if ( capPercentage > 0 )
            {
                return cappedLocalCacheFactory.apply( this );
            }
            log.debug( String.format( "Local Cache (forced) instance created for region:'%s'", getRegion() ) );
            return new LocalCache<>( this );
        }
        if ( configuration.isEnabled( ConfigurationKey.REDIS_ENABLED ) )
        {
            log.debug( String.format( "Redis Cache instance created for region:'%s'", getRegion() ) );
            return new RedisCache<>( this );
        }
        int capPercentage = parseInt( configuration.getProperty( ConfigurationKey.SYSTEM_CACHE_CAP_PERCENTAGE ) );
        if ( capPercentage > 0 )
        {
            return cappedLocalCacheFactory.apply( this );
        }
        log.debug( String.format( "Local Cache instance created for region:'%s'", getRegion() ) );
        return new LocalCache<>( this );
    }

    public RedisTemplate<String, ?> getRedisTemplate()
    {
        return redisTemplate;
    }
}
