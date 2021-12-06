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
package org.hisp.dhis.tracker.preheat.cache;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class NewPreheatCacheService
    implements PreheatCacheService
{
    private Map<String, Cache<String, IdentifiableObject>> cacheMap = new ConcurrentHashMap<>();

    @Override
    public Optional<IdentifiableObject> get( String cacheKey, String id )
    {
        Cache<String, IdentifiableObject> cache = cacheMap.get( cacheKey );

        if ( cache != null )
        {
            return Optional.ofNullable( cache.get( id ) );
        }

        return Optional.empty();
    }

    @Override
    public boolean hasKey( String cacheKey )
    {
        return cacheMap.containsKey( cacheKey );
    }

    @Override
    public List<IdentifiableObject> getAll( String cacheKey )
    {
        Cache<String, IdentifiableObject> cache = cacheMap.get( cacheKey );
        List<IdentifiableObject> objects = new ArrayList<>();

        if ( cache != null )
        {
            cache.keys().forEach( key -> objects.add( cache.get( key ) ) );
        }

        return objects;
    }

    @Override
    public void put( String cacheKey, String id, IdentifiableObject object, int cacheTTL, long capacity )
    {
        if ( !cacheMap.containsKey( cacheKey ) )
        {
            Cache<String, IdentifiableObject> cache = Cache2kBuilder.of( String.class, IdentifiableObject.class )
                .expireAfterWrite( cacheTTL, TimeUnit.SECONDS )
                .permitNullValues( false )
                .build();

            cacheMap.putIfAbsent( cacheKey, cache );
        }

        cacheMap.get( cacheKey ).putIfAbsent( id, object );
    }

    @Override
    public void invalidateCache()
    {
        cacheMap.values().forEach( Cache::removeAll );
    }

    @Override
    @EventListener
    public void handleApplicationCachesCleared( ApplicationCacheClearedEvent event )
    {
        invalidateCache();
    }
}
