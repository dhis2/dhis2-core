package org.hisp.dhis.cache;

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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class CacheBuilder<V>
{
    private static final Log log = LogFactory.getLog( CacheBuilder.class );

    private DhisConfigurationProvider configurationProvider;

    private RedisTemplate<String, ?> redisTemplate;

    private long maximumSize;

    private String region;

    private boolean refreshExpiryOnAccess;

    private long expiryInSeconds;

    private V defaultValue;
    
    private boolean expiryEnabled;

    public CacheBuilder( RedisTemplate<String, ?> redisTemplate, DhisConfigurationProvider configurationProvider )
    {
        this.configurationProvider = configurationProvider;
        this.redisTemplate = redisTemplate;
        //Applying sensible defaults
        this.maximumSize = -1;
        this.region = "default";
        this.refreshExpiryOnAccess = false;
        this.expiryInSeconds = 0;
        this.defaultValue = null;
        this.expiryEnabled = false;
    }

    /**
     * Set the maximum size for the cache instance to be built. If set to 0, no
     * caching will take place. Cannot be a negative value.
     * 
     * @param maximumSize The maximum size
     * @return The builder instance
     * @throws IllegalArgumentException if specified maximumSize is a negative
     *         value.
     */
    public CacheBuilder<V> withMaximumSize( long maximumSize )
    {
        if ( maximumSize < 0 )
        {
            throw new IllegalArgumentException( "MaximumSize cannot be negative" );
        }
        this.maximumSize = maximumSize;
        return this;
    }

    /**
     * Set the cacheRegion for the cache instance to be built. If not specified
     * default is "default" region.
     * 
     * @param region The cache region name to be used.
     * @return The builder instance.
     * @throws IllegalArgumentException if specified region is null.
     */
    public CacheBuilder<V> forRegion( String region )
    {
        if ( region == null )
        {
            throw new IllegalArgumentException( "Region cannot be null" );
        }
        this.region = region;
        return this;
    }

    /**
     * Configure the cache instance to expire the keys, if the expiry duration
     * elapses after last access.
     * 
     * @param duration The duration
     * @param timeUnit The time unit of the duration
     * @return The builder instance.
     * @throws IllegalArgumentException if specified timeUnit is null.
     */
    public CacheBuilder<V> expireAfterAccess( long duration, TimeUnit timeUnit )
    {
        if ( timeUnit == null )
        {
            throw new IllegalArgumentException( "TimeUnit cannot be null" );
        }
        this.expiryInSeconds = timeUnit.toSeconds( duration );
        this.refreshExpiryOnAccess = true;
        this.expiryEnabled = true;
        return this;
    }

    /**
     * Configure the cache instance to expire the keys, if the expiry duration
     * elapses after writing. The key expires irrespective of the last access.
     * 
     * @param duration The duration
     * @param timeUnit The time unit of the duration
     * @return The builder instance.
     * @throws IllegalArgumentException if specified timeUnit is null.
     */
    public CacheBuilder<V> expireAfterWrite( long duration, TimeUnit timeUnit )
    {
        if ( timeUnit == null )
        {
            throw new IllegalArgumentException( "TimeUnit cannot be null" );
        }
        this.expiryInSeconds = timeUnit.toSeconds( duration );
        this.refreshExpiryOnAccess = false;
        this.expiryEnabled = true;
        return this;
    }

    /**
     * Configure the cache instance to have a default value if the key does not have an associated value in cache. The default value will not be stored in the cache.
     * 
     * @param defaultValue The default value
     * @return The builder instance.
     */
    public CacheBuilder<V> withDefaultValue( V defaultValue )
    {
        this.defaultValue = defaultValue;
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
    public Cache<V> build()
    {
        if ( maximumSize == 0 )
        {
            log.info( String.format( "NoOp Cache instance created for region:'%s'", region ) );
            return new NoOpCache<V>( this );
        }
        else if ( configurationProvider.getProperty( ConfigurationKey.REDIS_ENABLED ).equalsIgnoreCase( "true" ) )
        {
            log.info( String.format( "Redis Cache instance created for region:'%s'", region ) );
            return new RedisCache<V>( this );
        }
        else
        {
            log.info( String.format( "Local Cache instance created for region:'%s'", region ) );
            return new LocalCache<V>( this );
        }
    }

    public long getMaximumSize()
    {
        return maximumSize;
    }

    public String getRegion()
    {
        return region;
    }

    public boolean isRefreshExpiryOnAccess()
    {
        return refreshExpiryOnAccess;
    }
    
    public boolean isExpiryEnabled()
    {
        return expiryEnabled;
    }

    public long getExpiryInSeconds()
    {
        return expiryInSeconds;
    }

    public V getDefaultValue()
    {
        return defaultValue;
    }

    public RedisTemplate<String, ?> getRedisTemplate()
    {
        return redisTemplate;
    }
}
