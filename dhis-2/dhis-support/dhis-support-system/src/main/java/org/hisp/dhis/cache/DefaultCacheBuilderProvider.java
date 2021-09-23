/*
 * Copyright (c) 2004-2021, University of Oslo
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

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Provides cache builder to build instances.
 *
 * @author Ameen Mohamed
 *
 */
@Component( "cacheProvider" )
public class DefaultCacheBuilderProvider implements CacheBuilderProvider
{
    private DhisConfigurationProvider configurationProvider;

    private RedisTemplateFactory redisTemplateFactory;

    private CappedLocalCache cappedLocalCache;

    @Override
    public <V> CacheBuilder<V> newCacheBuilder( Class<V> valueType )
    {
        RedisTemplate<String, V> redisTemplate = null;
        if ( redisTemplateFactory != null )
        {
            redisTemplate = redisTemplateFactory.createRedisTemplate( valueType );
        }
        return getExtendedCacheBuilder( redisTemplate );
    }

    private <V> ExtendedCacheBuilder<V> getExtendedCacheBuilder( RedisTemplate<String, V> redisTemplate )
    {
        Function<CacheBuilder<V>, Cache<V>> capCacheFactory = cappedLocalCache != null
            ? cappedLocalCache::createRegion
            : builder -> new NoOpCache<>();
        return new ExtendedCacheBuilder<>( redisTemplate, configurationProvider, capCacheFactory );
    }

    @Override
    public <V> CacheBuilder<V> newCacheBuilder( Class<? extends Collection> collectionType, Class<?> elementType )
    {
        RedisTemplate<String, V> redisTemplate = null;
        if ( redisTemplateFactory != null )
        {
            redisTemplate = redisTemplateFactory.createRedisTemplateForCollection( collectionType, elementType );
        }
        return getExtendedCacheBuilder( redisTemplate );
    }

    @Override
    public <V> CacheBuilder<V> newCacheBuilder( Class<? extends Map> mapType, Class<?> keyType, Class<?> valueType )
    {
        RedisTemplate<String, V> redisTemplate = null;
        if ( redisTemplateFactory != null )
        {
            redisTemplate = redisTemplateFactory.createRedisTemplateForMap( mapType, keyType, valueType );
        }
        return getExtendedCacheBuilder( redisTemplate );
    }

    @Autowired
    public void setConfigurationProvider( DhisConfigurationProvider configurationProvider )
    {
        this.configurationProvider = configurationProvider;
    }

    @Autowired( required = false )
    @Qualifier( "redisTemplateFactory" )
    public void setRedisTemplateFactory( RedisTemplateFactory redisTemplateFactory )
    {
        this.redisTemplateFactory = redisTemplateFactory;
    }

    @Autowired
    public void setCappedLocalCache( CappedLocalCache cappedLocalCache )
    {
        this.cappedLocalCache = cappedLocalCache;
    }
}
