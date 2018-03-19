package org.hisp.dhis.configuration;
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

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.LocalCache;
import org.hisp.dhis.cache.NoOpCache;
import org.hisp.dhis.cache.RedisCache;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * A CacheFactory object that provides cache instances based on the input
 * parameters. Provides cache instances based on the configurations like Redis
 * backed cache (Shared Cache), Caffeine backed cache (local cache) or NoOp
 * cache (for system testing)
 * 
 * @author Ameen Mohamed
 *
 */
@Component
public class CacheProvider
{
    private static final Log log = LogFactory.getLog( CacheProvider.class );

    private DhisConfigurationProvider configurationProvider;

    private RedisTemplate<String, Serializable> redisTemplate;

    /**
     * Creates and returns a cacheInstance based on the system configuration and
     * the input parameters. If {@code maximumSize} is 0 then a NoOpCache
     * instance will be returned which does not cache anything. This can be used
     * during system testings where cache has to be disabled. If
     * {@code maximumSize} is greater than 0 than based on {@code redis.enabled}
     * property in dhis.conf, either Redis backed implementation will be
     * returned or a Local Caffeine backed cache implementation will be
     * returned. For Local cache, every instance created using this method will
     * be logically separate and will not share any state. However, when using
     * Redis Cache, every instance created using this method will use the same
     * redis store.
     * 
     * 
     * @param region The cache region name
     * @param refreshExpiryOnAccess Indicates whether the expiry (timeToLive)
     *        has to reset on every access
     * @param expiryInSeconds The time to live value in seconds
     * @param maximumSize The maximum size this cache instance should hold. If
     *        set to 0, then caching is disabled. If set to -1, then the cache
     *        instance will use as much as feasible.
     * @return A cache instance based on the system configuration and input
     *         parameters. Returns one of {@link RedisCache}, {@link LocalCache}
     *         or {@link NoOpCache}
     */
    public Cache createCacheInstance( String region, boolean refreshExpiryOnAccess, long expiryInSeconds,
        long maximumSize, Serializable defaultValue )
    {
        if ( maximumSize == 0 )
        {
            log.info( "NoOp Cache instance created for region=" + region );
            return new NoOpCache( defaultValue );
        }
        else if ( configurationProvider.getProperty( ConfigurationKey.REDIS_ENABLED ).equalsIgnoreCase( "true" ) )
        {
            log.info( "Redis Cache instance created for region=" + region );
            return new RedisCache( redisTemplate, region, refreshExpiryOnAccess, expiryInSeconds, defaultValue );
        }
        else
        {
            log.info( "Local Cache instance created for region=" + region );
            return new LocalCache( refreshExpiryOnAccess, expiryInSeconds, maximumSize, defaultValue );
        }
    }

    @Autowired
    public void setConfigurationProvider( DhisConfigurationProvider configurationProvider )
    {
        this.configurationProvider = configurationProvider;
    }

    @Autowired( required = false )
    public void setRedisTemplate( RedisTemplate<String, Serializable> redisTemplate )
    {
        this.redisTemplate = redisTemplate;
    }

}