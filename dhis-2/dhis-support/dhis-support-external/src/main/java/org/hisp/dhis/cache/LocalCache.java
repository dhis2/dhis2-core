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

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Local cache implementation of {@link Cache}. This implementation is backed by
 * Caffeine library which uses an in memory Map implementation.
 * 
 * @author Ameen Mohamed
 *
 */
public class LocalCache<V> implements Cache<V>
{

    private com.github.benmanes.caffeine.cache.Cache<String, V> caffeineCache;

    private V defaultValue;

    /**
     * Constructor to instantiate LocalCache object.
     * 
     * 
     * @param refreshExpiryOnAccess Indicates whether the expiry (timeToLive)
     *        has to reset on every access
     * @param expiryInSeconds The time to live value in seconds
     * @param maximumSize The maximum size this cache instance should hold. If
     *        set to 0, then caching is disabled.
     * @param defaultValue Default value to be returned if no associated value
     *        for a key is found in the cache. The defaultValue will not be
     *        stored in the cache, but should be used as an indicator that the
     *        key did not have an associated value. By default the defaultValue
     *        is null
     */
    public LocalCache( final CacheBuilder<V> cacheBuilder )
    {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();
        if ( cacheBuilder.isRefreshExpiryOnAccess() )
        {
            builder.expireAfterAccess( cacheBuilder.getExpiryInSeconds(), TimeUnit.SECONDS );
        }
        else
        {
            builder.expireAfterWrite( cacheBuilder.getExpiryInSeconds(), TimeUnit.SECONDS );

        }

        if ( cacheBuilder.getMaximumSize() > 0 )
        {
            builder.maximumSize( cacheBuilder.getMaximumSize() );
        }
        this.caffeineCache = builder.build();
    }

    @Override
    public Optional<V> getIfPresent( String key )
    {
        if ( null == key)
        {
            throw new IllegalArgumentException( "Key cannot be null" );
        }
        return Optional.ofNullable( caffeineCache.getIfPresent( key ) );
    }

    @Override
    public Optional<V> get( String key )
    {
        if ( null == key)
        {
            throw new IllegalArgumentException( "Key cannot be null" );
        }
        return Optional.ofNullable( Optional.ofNullable( caffeineCache.getIfPresent( key ) ).orElse( defaultValue ) );
    }

    @Override
    public Optional<V> get( String key, Function<String, V> mappingFunction )
    {
        if ( null == key || null == mappingFunction)
        {
            throw new IllegalArgumentException( "Key and MappingFunction cannot be null" );
        }
        return Optional
            .ofNullable( Optional.ofNullable( caffeineCache.get( key, mappingFunction ) ).orElse( defaultValue ) );
    }

    @Override
    public void put( String key, V value )
    {
        if ( null == key || null == value )
        {
            throw new IllegalArgumentException( "Key and Value cannot be null" );
        }
        caffeineCache.put( key, value );
    }

    @Override
    public void invalidate( String key )
    {
        if ( null == key)
        {
            throw new IllegalArgumentException( "Key cannot be null" );
        }
        caffeineCache.invalidate( key );
    }

    @Override
    public void invalidateAll()
    {
        caffeineCache.invalidateAll();
    }

}
