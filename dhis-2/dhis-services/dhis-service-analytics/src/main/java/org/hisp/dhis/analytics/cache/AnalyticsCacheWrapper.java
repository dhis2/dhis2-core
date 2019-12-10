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

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.logging.LogFactory.getLog;
import static org.hisp.dhis.commons.util.SystemUtils.isTestRun;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.common.Grid;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * This wrapper is responsible for keeping and isolating all caching definition
 * related to the analytics caching, decoupling it from the service layer.
 */
@Component
public class AnalyticsCacheWrapper
{
    private static final Log log = getLog( AnalyticsCacheWrapper.class );

    private Cache<Grid> queryCache;

    private final CacheProvider cacheProvider;

    private final Environment environment;

    private final DhisConfigurationProvider dhisConfig;

    private static final int MAX_CACHE_ENTRIES = 20000;

    private static final String CACHE_REGION = "analyticsQueryResponse";

    public AnalyticsCacheWrapper( final CacheProvider cacheProvider, final Environment environment,
        final DhisConfigurationProvider dhisConfig )
    {
        checkNotNull( cacheProvider );
        checkNotNull( environment );
        this.cacheProvider = cacheProvider;
        this.environment = environment;
        this.dhisConfig = dhisConfig;
    }

    public Optional<Grid> get( final String key )
    {
        return queryCache.get( key );
    }

    public void put( final String key, final Grid grid, final long ttlInSeconds )
    {
        queryCache.put( key, grid, ttlInSeconds );
    }

    public void invalidateAll()
    {
        queryCache.invalidateAll();
        log.info( "Analytics cache cleared" );
    }

    @PostConstruct
    public void init()
    {
        long expiration = dhisConfig.getAnalyticsCacheExpiration();
        boolean enabled = expiration > 0 && !isTestRun( this.environment.getActiveProfiles() );

        queryCache = cacheProvider.newCacheBuilder( Grid.class ).forRegion( CACHE_REGION )
            .expireAfterWrite( expiration, SECONDS ).withMaximumSize( enabled ? MAX_CACHE_ENTRIES : 0 ).build();

        log.info( format( "Analytics server-side cache is enabled: %b with expiration: %d s", enabled, expiration ) );
    }
}
