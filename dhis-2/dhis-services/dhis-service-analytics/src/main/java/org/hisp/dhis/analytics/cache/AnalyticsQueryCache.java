/*
 * Copyright (c) 2004-2019, University of Oslo
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

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.apache.commons.logging.LogFactory.getLog;

import org.apache.commons.logging.Log;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

/**
 * Component responsible to cache and retrieve the result object from a give sql query.
 * It will also create and manage an internal cache for caching only analytics queries.
 */
@Component
public class AnalyticsQueryCache
    implements
    QueryCache
{
    private static final Log log = getLog( AnalyticsQueryCache.class );

    private static final String ANALYTICS_QUERY_CACHE = "analyticsQueryCache";

    private static final int MAX_EXPIRATION_TIME = 60;

    private boolean usingTimeToLiveInMillis = false;

    private static Cache<Key, SqlRowSet> cache = null;

    public AnalyticsQueryCache()
    {
        try {
            if ( cache == null )
            {
                cache = Cache2kBuilder.of( Key.class, SqlRowSet.class )
                        .name( ANALYTICS_QUERY_CACHE )
                        .eternal( false )
                        .expireAfterWrite( MAX_EXPIRATION_TIME, MINUTES )
                        .build();
            }
        }
        catch ( IllegalStateException e )
        {
            log.warn( "Trying to initialize the same Cache2K instance twice. Ignoring..." );
        }
        catch ( IllegalArgumentException e )
        {
            log.error( "Cache2K instance cannot be created. Missing arguments.", e );
        }
    }

    /**
     * Add a given key-value pair into the cache respecting the TTL in minutes (default).
     * If you need a support for TTL in milliseconds, you can enable it by invoking
     * "usingTimeToLiveInMillis()"
     *
     * @param key the key to be cached.
     * @param sqlRowSet the object containing the results.
     * @param ttl a time to live for the given key. Minutes is the default, unless
     *            usingTimeToLiveInMillis() was previously invoked.
     */
    @Override
    public void put( final Key key, final SqlRowSet sqlRowSet, final long ttl )
    {
        final long expirationTime = currentTimeMillis() + getTimeToLive( ttl );
        cache.invoke( key, e -> e.setValue( sqlRowSet ).setExpiryTime( expirationTime ) );
    }

    @Override
    public SqlRowSet get( final Key key )
    {
        return cache.get( key );
    }

    public void usingTimeToLiveInMillis() {
        usingTimeToLiveInMillis = true;
    }
    
    private long getTimeToLive( final long ttl )
    {
        if ( !usingTimeToLiveInMillis )
        {
            return MINUTES.toMillis( ttl );
        }
        return ttl;
    }
}
