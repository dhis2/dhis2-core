package org.hisp.dhis.cache;

/*
 * Copyright (c) 2004-2018, University of Oslo
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

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.Sets;

/**
 * A No operation implementation of {@link Cache}. The implementation will not
 * cache anything and can be used during system testing when caching has to be
 * disabled.
 * 
 * @author Ameen Mohamed
 */
public class NoOpCache<V> implements Cache<V>
{
    private V defaultValue;

    public NoOpCache( SimpleCacheBuilder<V> cacheBuilder )
    {
        this.defaultValue = cacheBuilder.getDefaultValue();
    }

    @Override
    public Optional<V> getIfPresent( String key )
    {
        return Optional.empty();
    }

    @Override
    public Optional<V> get( String key )
    {
        return Optional.ofNullable( defaultValue );
    }

    @Override
    public Optional<V> get( String key, Function<String, V> mappingFunction )
    {
        if ( null == mappingFunction )
        {
            throw new IllegalArgumentException( "MappingFunction cannot be null" );
        }
        return Optional.ofNullable( Optional.ofNullable( mappingFunction.apply( key ) ).orElse( defaultValue ) );
    }

    @Override
    public Collection<V> getAll()
    {
        return Sets.newHashSet();
    }

    @Override
    public void put( String key, V value )
    {
        if ( null == value )
        {
            throw new IllegalArgumentException( "Value cannot be null" );
        }
        // No operation
    }

    @Override
    public void invalidate( String key )
    {
        // No operation
    }

    @Override
    public void invalidateAll()
    {
        // No operation
    }
    
    @Override
    public CacheType getCacheType()
    {
        return CacheType.NONE;
    }
}