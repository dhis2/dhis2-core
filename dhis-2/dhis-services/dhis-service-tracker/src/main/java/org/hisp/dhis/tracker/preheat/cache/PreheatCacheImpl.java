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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.hisp.dhis.attribute.Attribute;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.tracker.TrackerIdScheme;
import org.hisp.dhis.tracker.TrackerIdentifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.ImmutableList;

@Service
public class PreheatCacheImpl
    implements PreheatCache
{
    private final Cache<TrackerCacheKey, IdentifiableObject> cache;

    private final IdentifiableObjectManager identifiableObjectManager;

    public PreheatCacheImpl( IdentifiableObjectManager identifiableObjectManager )
    {
        this.identifiableObjectManager = identifiableObjectManager;

        cache = Cache2kBuilder.of( TrackerCacheKey.class, IdentifiableObject.class )
            .expireAfterWrite( 5, TimeUnit.SECONDS )
            .permitNullValues( true )
            .build();

    }

    @Override
    @Transactional( readOnly = true )
    public ImmutableList<IdentifiableObject> getObjectsByIdentifier( Class<? extends IdentifiableObject> klass,
        Set<String> ids,
        TrackerIdentifier idScheme )
    {
        List<String> misses = new ArrayList<>();
        List<IdentifiableObject> hits = new ArrayList<>();

        /*
         * First we collect any cache hits, and note down any misses.
         */
        ids.forEach( id -> {
            TrackerCacheKey key = generateTrackerCacheKey( klass, id, idScheme );

            if ( cache.containsKey( key ) )
            {
                hits.add( cache.get( key ) );
            }
            else
            {
                misses.add( id );
            }

        } );

        /*
         * Second we fetch in bulk any misses we had
         */
        List<? extends IdentifiableObject> fetched = getIdentifiableObjectByIdScheme( klass, idScheme, misses );

        /*
         * Finally, we add any newly fetched objects to the cache, and add null
         * values for missing values.
         */
        fetched.forEach( obj -> {
            misses.remove( idScheme.getIdentifier( obj ) );
            hits.add( obj );
            cache.putIfAbsent( generateTrackerCacheKey( klass, idScheme.getIdentifier( obj ), idScheme ), obj );
        } );

        misses.forEach(
            miss -> {
                cache.putIfAbsent( generateTrackerCacheKey( klass, miss, idScheme ), null );
            } );

        return ImmutableList.copyOf( hits );

    }

    private List<? extends IdentifiableObject> getIdentifiableObjectByIdScheme(
        Class<? extends IdentifiableObject> klass,
        TrackerIdentifier idScheme, List<String> ids )
    {

        if ( TrackerIdScheme.ATTRIBUTE.equals( idScheme.getIdScheme() ) )
        {
            Attribute attribute = new Attribute();
            attribute.setUid( idScheme.getValue() );
            return identifiableObjectManager.getAllByAttributeAndValues( klass, attribute, ids );
        }
        else if ( TrackerIdScheme.CODE.equals( idScheme.getIdScheme() ) )
        {
            return identifiableObjectManager.getByCode( klass, ids );
        }
        else
        {
            return identifiableObjectManager.getByUid( klass, ids );
        }

    }

    private TrackerCacheKey generateTrackerCacheKey( Class<? extends IdentifiableObject> klass, String identifier,
        TrackerIdentifier idScheme )
    {
        return new TrackerCacheKey( klass, idScheme, identifier );
    }
}
