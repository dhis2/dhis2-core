package org.hisp.dhis.antlr.cache;

/*
 * Copyright (c) 2004-2020, University of Oslo
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

import com.google.common.base.Optional;
import org.cache2k.Cache2kBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Local cache implementation of {@link Cache}. This implementation is backed by
 * Caffeine library which uses an in memory Map implementation.
 *
 * @author Ameen Mohamed
 */
public class LocalCache<V> implements Cache<V>
{
    private org.cache2k.Cache<String, V> cache2kInstance;

    private V defaultValue;

    /**
     * Constructor to instantiate LocalCache object.
     *
     *
     * @param cacheBuilder CacheBuilder instance
     */
    @SuppressWarnings("unchecked")
    public LocalCache( final CacheBuilder<V> cacheBuilder )
    {
        Cache2kBuilder<?, ?> builder = Cache2kBuilder.forUnknownTypes();

        if ( cacheBuilder.isExpiryEnabled() )
        {
            builder.eternal( false );
            builder.expireAfterWrite( cacheBuilder.getExpiryInSeconds(), TimeUnit.SECONDS );
        }
        else
        {
            builder.eternal( true );
        }
        if ( cacheBuilder.getMaximumSize() > 0 )
        {
            builder.entryCapacity( cacheBuilder.getMaximumSize() );
        }

        //Using unknown typed keyvalue for builder and casting it this way seems to work.
        this.cache2kInstance = (org.cache2k.Cache<String, V>) builder.build();
        this.defaultValue = cacheBuilder.getDefaultValue();
    }

    @Override
    public Optional<V> getIfPresent( String key )
    {
        return Optional.fromNullable( cache2kInstance.get( key ) );
    }

    @Override
    public Optional<V> get( String key )
    {
        return Optional.fromNullable( cache2kInstance.get( key ) ).or( Optional.fromNullable( defaultValue ) );
    }

    @Override
    public Optional<V> get( String key, MappingFunction<V> mappingFunction )
    {
        if ( null == mappingFunction )
        {
            throw new IllegalArgumentException( "MappingFunction cannot be null" );
        }

        V value = cache2kInstance.get( key );
        if ( value == null )
        {
            value = mappingFunction.getValue( key );
        }

        if ( value != null )
        {
            cache2kInstance.put( key, value );
        }
        return Optional.fromNullable( value ).or( Optional.fromNullable( defaultValue ) );
    }

    @Override
    public Collection<V> getAll()
    {
        return new ArrayList<V>( cache2kInstance.asMap().values() );
    }

    @Override
    public void put( String key, V value )
    {
        if ( null == value )
        {
            throw new IllegalArgumentException( "Value cannot be null" );
        }
        cache2kInstance.put( key, value );
    }



    @Override
    public void invalidate( String key )
    {
        cache2kInstance.remove( key );
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
