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
package org.hisp.dhis.tracker.preheat.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import lombok.RequiredArgsConstructor;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * Pre-heat cache implementation for metadata objects.
 *
 * @author Luciano Fiandesio
 */
@RequiredArgsConstructor
@Service
public class DefaultPreheatCacheService implements PreheatCacheService
{
    private final DhisConfigurationProvider config;

    private final Environment environment;

    /**
     * Data structure to hold the metadata cache:
     *
     * - the key is the full class name of the metadata class getting cached
     * (e.g. "org.hisp.dhis.program.Program")
     *
     * - the value is a Cache2K cache holding the objects to cache
     *
     * Caveat: this data structure may reference multiple times the same
     * objects, if different {@link TrackerIdScheme} are used during different
     * imports.
     */
    private static final Map<String, Cache<String, IdentifiableObject>> cache = new HashMap<>();

    @Override
    public Optional<IdentifiableObject> get( final String cacheKey, final String id )
    {
        if ( isCacheEnabled() && cache.containsKey( cacheKey ) )
        {
            return Optional.ofNullable( cache.get( cacheKey ).get( id ) );
        }

        return Optional.empty();
    }

    @Override
    public Optional<IdentifiableObject> get( String cacheKey, String id,
        BiFunction<String, String, Optional<IdentifiableObject>> mappingFunction, int cacheTTL, long capacity )
    {
        if ( mappingFunction == null )
        {
            throw new IllegalArgumentException( "MappingFunction cannot be null" );
        }

        Optional<IdentifiableObject> value = get( cacheKey, id );
        if ( value.isPresent() )
        {
            return value;
        }

        value = mappingFunction.apply( cacheKey, id );
        if ( value.isPresent() )
        {
            put( cacheKey, id, value.get(), cacheTTL, capacity );
        }

        return value;
    }

    @Override
    public boolean hasKey( String cacheKey )
    {
        return cache.containsKey( cacheKey );
    }

    public List<IdentifiableObject> getAll( String cacheKey )
    {
        List<IdentifiableObject> res = new ArrayList<>();
        if ( hasKey( cacheKey ) )
        {
            cache.get( cacheKey ).keys().forEach( k -> {
                res.add( cache.get( cacheKey ).get( k ) );
            } );
        }
        return res;

    }

    @Override
    public void put( final String cacheKey, final String id, IdentifiableObject object,
        final int cacheTTL, final long capacity )
    {
        if ( cacheKey == null || id == null || object == null )
            return;

        if ( isCacheEnabled() )
        {
            if ( cache.containsKey( cacheKey ) )
            {
                cache.get( cacheKey ).put( id, object );
            }
            else
            {
                Cache<String, IdentifiableObject> c = new Cache2kBuilder<String, IdentifiableObject>()
                {
                }
                    .expireAfterWrite( cacheTTL, TimeUnit.MINUTES )
                    .name( cacheKey )
                    .permitNullValues( false )
                    .entryCapacity( capacity == -1 ? Long.MAX_VALUE : capacity )
                    .resilienceDuration( 30, TimeUnit.SECONDS ) // cope with at
                    // most 30
                    // seconds
                    // outage before propagating exceptions
                    .build();

                c.put( id, object );

                cache.put( cacheKey, c );
            }
        }
    }

    @EventListener
    @Override
    public void handleApplicationCachesCleared( ApplicationCacheClearedEvent event )
    {
        invalidateCache();
    }

    @Override
    public void invalidateCache()
    {
        cache.values().forEach( Cache::removeAll );
    }

    private boolean isCacheEnabled()
    {
        return false;

        // Due to concerns and issues with the current cache implementation, we
        // decided to
        // deactivate the cache in the preheat completely for now.
        // return !isTestRun( this.environment.getActiveProfiles() )
        // && config.isEnabled(
        // ConfigurationKey.TRACKER_IMPORT_PREHEAT_CACHE_ENABLED );
    }
}
