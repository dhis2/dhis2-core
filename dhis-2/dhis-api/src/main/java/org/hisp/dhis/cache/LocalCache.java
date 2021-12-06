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

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.cache2k.Cache2kBuilder;
import org.cache2k.event.CacheEntryCreatedListener;
import org.cache2k.event.CacheEntryEvictedListener;
import org.cache2k.event.CacheEntryExpiredListener;
import org.cache2k.event.CacheEntryRemovedListener;
import org.cache2k.event.CacheEntryUpdatedListener;
import org.cache2k.expiry.Expiry;

/**
 * Local cache implementation of {@link Cache}. This implementation is backed by
 * Caffeine library which uses an in memory Map implementation.
 *
 * @author Ameen Mohamed
 */
public class LocalCache<T, V>
    implements Cache<T, V>
{
    private final org.cache2k.Cache<T, V> cache2kInstance;

    private boolean refreshExpiryOnAccess = false;

    private long expiryInSeconds = 0;

    private V defaultValue;

    /**
     * Constructor to instantiate LocalCache object.
     *
     * @param cacheBuilder CacheBuilder instance
     */
    @SuppressWarnings( value = "unchecked" )
    public LocalCache( final CacheBuilder<T, V> cacheBuilder )
    {
        Cache2kBuilder<T, V> builder = (Cache2kBuilder<T, V>) Cache2kBuilder.forUnknownTypes();

        if ( cacheBuilder.isExpiryEnabled() )
        {
            builder.expireAfterWrite( cacheBuilder.getExpiryInSeconds(), SECONDS );

            if ( cacheBuilder.isRefreshExpiryOnAccess() )
            {
                expiryInSeconds = cacheBuilder.getExpiryInSeconds();
                refreshExpiryOnAccess = true;
            }
        }

        if ( cacheBuilder.getLoader() != null )
        {
            builder.loader( ( key ) -> cacheBuilder.getLoader().loadEntity( key ) );
        }

        if ( cacheBuilder.getBulkLoader() != null )
        {
            builder.bulkLoader( set -> cacheBuilder.getBulkLoader().bulkLoadEntities( set ) );
        }

        builder.refreshAhead( cacheBuilder.isRefreshAhead() );

        if ( cacheBuilder.getMaximumSize() > 0 )
        {
            builder.entryCapacity( cacheBuilder.getMaximumSize() );
        }

        builder.expiryPolicy( ( t, v, l, cacheEntry ) -> v == null ? Expiry.NOW : SECONDS.toMillis( expiryInSeconds ) );

        // TODO:TEMP
        builder
            .addListener( (CacheEntryCreatedListener<T, V>) ( cache, cacheEntry ) -> System.out.println(
                "C2k CREATED " + cacheEntry.getKey() ) )
            .addListener( (CacheEntryUpdatedListener<T, V>) ( cache, cacheEntry, cacheEntry1 ) -> System.out.println(
                "C2k UPDATED " + cacheEntry.getKey() ) )
            .addListener( (CacheEntryEvictedListener<T, V>) ( cache, cacheEntry ) -> System.out.println(
                "C2k EVICTED " + cacheEntry.getKey() ) )
            .addListener( (CacheEntryExpiredListener<T, V>) ( cache, cacheEntry ) -> System.out.println(
                "C2k EXPIRED " + cacheEntry.getKey() ) )
            .addListener( (CacheEntryRemovedListener<T, V>) ( cache, cacheEntry ) -> System.out.println(
                "C2k REMOVED " + cacheEntry.getKey() ) );

        this.cache2kInstance = builder.build();
        this.defaultValue = cacheBuilder.getDefaultValue();
    }

    private V getInternal( T key )
    {
        // If a loader is configured, this call will load the value is missing
        // from the cache
        V res = cache2kInstance.get( key );

        // Is refreshExpiryOnAccess is set to true, we set the expiry time to
        // expiryInSeconds for the entry, if found.
        if ( res != null && refreshExpiryOnAccess )
        {
            cache2kInstance.expireAt( key, SECONDS.toMillis( expiryInSeconds ) );
        }

        return res;
    }

    private void putInternal( T key, V value )
    {
        cache2kInstance.putIfAbsent( key, value );
    }

    @Override
    public Optional<V> getIfPresent( T key )
    {
        return Optional.ofNullable( getInternal( key ) );
    }

    @Override
    public Optional<V> get( T key )
    {
        return Optional.ofNullable( Optional.ofNullable( getInternal( key ) ).orElse( defaultValue ) );
    }

    @Override
    public V get( T key, Function<T, V> mappingFunction )
    {
        if ( null == mappingFunction )
        {
            throw new IllegalArgumentException( "MappingFunction cannot be null" );
        }

        V value = getInternal( key );

        if ( value == null )
        {
            value = mappingFunction.apply( key );

            if ( value != null )
            {
                putInternal( key, value );
            }
        }

        return Optional.ofNullable( value ).orElse( defaultValue );
    }

    @Override
    public Stream<V> getAll()
    {
        return cache2kInstance.asMap().values().stream();
    }

    @Override
    public List<Optional<V>> getAll( Set<T> keys )
    {
        List<Optional<V>> result = new ArrayList<>();
        Map<T, V> hits = cache2kInstance.getAll( keys );

        keys.forEach( key -> result.add( Optional.ofNullable( hits.getOrDefault( key, null ) ) ) );

        return result;
    }

    @Override
    public void put( T key, V value )
    {
        if ( null == value )
        {
            throw new IllegalArgumentException( "Value cannot be null" );
        }
        putInternal( key, value );
    }

    @Override
    public void put( T key, V value, long ttlInSeconds )
    {
        cache2kInstance.invoke( key,
            e -> e.setValue( value ).setExpiryTime( currentTimeMillis() + SECONDS.toMillis( ttlInSeconds ) ) );
    }

    @Override
    public void invalidate( T key )
    {
        cache2kInstance.expireAt( key, Expiry.NOW );
    }

    @Override
    public void invalidateAll()
    {
        cache2kInstance.clear();
    }

    @Override
    public CacheType getCacheType()
    {
        return CacheType.IN_MEMORY;
    }
}
