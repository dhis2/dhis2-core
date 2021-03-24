/*
 * Copyright (c) 2004-2004-2020, University of Oslo
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
package org.hisp.dhis.commons.jsonfiltering.metric.source;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;

/**
 * A source that provides metrics from a Guava {@link Cache}.
 */
public class GuavaCacheJsonFilteringMetricsSource implements JsonFilteringMetricsSource
{

    private final String prefix;

    private final Cache cache;

    public GuavaCacheJsonFilteringMetricsSource( String prefix, Cache cache )
    {
        checkNotNull( prefix );
        checkNotNull( cache );
        this.prefix = prefix;
        this.cache = cache;
    }

    @Override
    public void applyMetrics( Map<String, Object> map )
    {
        CacheStats stats = cache.stats();
        map.put( prefix + "averageLoadPenalty", stats.averageLoadPenalty() );
        map.put( prefix + "evictionCount", stats.evictionCount() );
        map.put( prefix + "hitCount", stats.hitCount() );
        map.put( prefix + "hitRate", stats.hitRate() );
        map.put( prefix + "hitCount", stats.hitCount() );
        map.put( prefix + "loadExceptionCount", stats.loadExceptionCount() );
        map.put( prefix + "loadExceptionRate", stats.loadExceptionRate() );
        map.put( prefix + "loadSuccessCount", stats.loadSuccessCount() );
        map.put( prefix + "missCount", stats.missCount() );
        map.put( prefix + "missRate", stats.missRate() );
        map.put( prefix + "requestCount", stats.requestCount() );
        map.put( prefix + "totalLoadTime", stats.totalLoadTime() );
    }
}
