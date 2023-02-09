/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.analytics.cache;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExpressionDimensionItemAnalyticsCache
{
    private final AnalyticsCacheSettings analyticsCacheSettings;

    // Inside the cache there is DataQueryParam.getKey()
    private final Cache<Set<String>> queryCache;

    /**
     * Default constructor. Note that a default expiration time is set, as the
     * TTL will always be overwritten during cache put operations.
     */
    public ExpressionDimensionItemAnalyticsCache( CacheProvider cacheProvider,
        AnalyticsCacheSettings analyticsCacheSettings )
    {
        checkNotNull( cacheProvider );
        checkNotNull( analyticsCacheSettings );

        this.analyticsCacheSettings = analyticsCacheSettings;
        this.queryCache = cacheProvider.createExpressionDimensionItemAnalyticsResponseCache();
    }

    /**
     * This method returns all keys (DataQueryParams::getKey)
     *
     * @param key
     * @return
     */
    public Optional<Set<String>> get( String key )
    {
        return queryCache.get( key );
    }

    /**
     * This method will cache the given DataQueryParams associated with the
     * given key.
     * <p>
     * The TTL of the cached object will be set accordingly to the cache
     * settings available at {@link AnalyticsCacheSettings}.
     *
     * @param key
     * @param params the DataQueryParams.
     */
    public void put( String key, DataQueryParams params )
    {
        if ( analyticsCacheSettings.isProgressiveCachingEnabled() )
        {
            // Uses the progressive TTL
            put( key, params,
                analyticsCacheSettings.progressiveExpirationTimeOrDefault( params.getLatestEndDate() ) );
        }
        else
        {
            // Respects the fixed (predefined) caching TTL
            put( key, params, analyticsCacheSettings.fixedExpirationTimeOrDefault() );
        }
    }

    /**
     * Will cache the given key/DataQueryParams pair respecting the TTL provided
     * through the parameter "ttlInSeconds".
     *
     * @param key the cache key associate with the Grid.
     * @param params the DataQueryParams object to be cached.
     * @param ttlInSeconds the time to live (expiration time) in seconds.
     */
    public void put( String key, DataQueryParams params, long ttlInSeconds )
    {
        Optional<Set<String>> dataQueryParamKeySet = queryCache.get( key );

        if ( dataQueryParamKeySet.isPresent() )
        {
            dataQueryParamKeySet.get().add( params.getKey() );

            queryCache.put( key, dataQueryParamKeySet.get(), ttlInSeconds );
        }
        else
        {
            Set<String> paramsSet = new HashSet<>();

            paramsSet.add( params.getKey() );

            queryCache.put( key, paramsSet, ttlInSeconds );
        }
    }

    /**
     * Removes cache entry.
     */
    public void invalidate( String key )
    {
        queryCache.invalidate( key );
    }

    /**
     * Caching enabled test
     *
     * @return flag indicated the enabling of the cache
     */
    public boolean isEnabled()
    {
        return analyticsCacheSettings.isCachingEnabled();
    }
}
