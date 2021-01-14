package org.hisp.dhis.analytics.cache;

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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.hisp.dhis.commons.util.SystemUtils.isTestRun;

import java.util.Optional;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.Grid;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * This is just a wrapper class responsible for keeping and isolating all
 * caching definition related to the analytics caching, decoupling it from the
 * service layer.
 */
@Component
public class AnalyticsCache
{
    private static final Log log = getLog( AnalyticsCache.class );

    private Cache<Grid> queryCache;

    private final CacheProvider cacheProvider;

    private final Environment environment;

    private final AnalyticsCacheSettings analyticsCacheSettings;

    private static final int MAX_CACHE_ENTRIES = 20000;

    private static final String CACHE_REGION = "analyticsResponse";

    public AnalyticsCache( final CacheProvider cacheProvider, final Environment environment,
        final AnalyticsCacheSettings analyticsCacheSettings )
    {
        checkNotNull( cacheProvider );
        checkNotNull( environment );
        checkNotNull( analyticsCacheSettings );
        this.cacheProvider = cacheProvider;
        this.environment = environment;
        this.analyticsCacheSettings = analyticsCacheSettings;
    }

    public Optional<Grid> get( final String key )
    {
        return queryCache.get( key );
    }

    /**
     * This method tries to retrieve, from the cache, the Grid related to the given
     * DataQueryParams. If the Grid is not found in the cache, the Grid will be
     * fetched by the function provided. In this case, the fetched Grid will be
     * cached, so the next consumers can hit the cache only.
     * 
     * The TTL of the cached object will be set accordingly to the cache settings
     * available at {@link org.hisp.dhis.analytics.cache.AnalyticsCacheSettings}.
     * 
     * @param params the current DataQueryParams.
     * @param function that fetches a grid based on the given DataQueryParams.
     * @return the cached or fetched Grid.
     */
    public Grid getOrFetch( final DataQueryParams params, final Function<DataQueryParams, Grid> function )
    {
        final Optional<Grid> cachedGrid = get( params.getKey() );

        if ( cachedGrid.isPresent() )
        {
            return cachedGrid.get();
        }
        else
        {
            final Grid grid = function.apply( params );

            put( params, grid );

            return grid;
        }
    }

    /**
     * This method will cache the given Grid associated with the given
     * DataQueryParams.
     * 
     * The TTL of the cached object will be set accordingly to the cache settings
     * available at {@link org.hisp.dhis.analytics.cache.AnalyticsCacheSettings}.
     * 
     * @param params the DataQueryParams.
     * @param grid the associated Grid.
     */
    public void put( final DataQueryParams params, final Grid grid )
    {
        if ( analyticsCacheSettings.isProgressiveCachingEnabled() )
        {
            // Uses the progressive TTL
            put( params.getKey(), grid, analyticsCacheSettings.progressiveExpirationTimeOrDefault( params.getLatestEndDate() ) );
        }
        else
        {
            // Respects the fixed (predefined) caching TTL
            put( params.getKey(), grid, analyticsCacheSettings.fixedExpirationTimeOrDefault() );
        }
    }

    /**
     * Will cache the given key/Grid pair respecting the TTL provided through the
     * parameter "ttlInSeconds".
     * 
     * @param key the cache key associate with the Grid.
     * @param grid the Grid object to be cached.
     * @param ttlInSeconds the custom time to live (expiration time).
     */
    public void put( final String key, final Grid grid, final long ttlInSeconds )
    {
        queryCache.put( key, grid, ttlInSeconds );
    }

    /**
     * Clean the current cache by removing all existing entries.
     */
    public void invalidateAll()
    {
        queryCache.invalidateAll();
        log.info( "Analytics cache cleared" );
    }

    public boolean isEnabled()
    {
        return analyticsCacheSettings.isCachingEnabled();
    }

    @PostConstruct
    public void init()
    {
        // Set a default expiration time to always expire, as the TTL will be
        // always overwritten during "put" operations.
        final long initialExpirationTime = analyticsCacheSettings.fixedExpirationTimeOrDefault();

        final boolean nonTestEnv = !isTestRun( this.environment.getActiveProfiles() );

        queryCache = cacheProvider.newCacheBuilder( Grid.class ).forRegion( CACHE_REGION )
            .expireAfterWrite( initialExpirationTime, SECONDS ).withMaximumSize( nonTestEnv ? MAX_CACHE_ENTRIES : 0 )
            .build();

        log.info( format( "Analytics server-side cache is enabled with expiration time (in seconds): %d",
            initialExpirationTime ) );
    }
}
