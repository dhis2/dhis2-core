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

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;

/**
 * A No operation implementation of {@link Cache}. The implementation will not
 * cache anything and can be used during system testing when caching has to be
 * disabled.
 * 
 * @author Ameen Mohamed
 *
 */
public class NoOpCache
    implements
    Cache
{

    private Serializable defaultValue;

    public NoOpCache( Serializable defaultValue )
    {
        this.defaultValue = defaultValue;
    }

    @Override
    public Optional<Serializable> getIfPresent( String key )
    {
        return Optional.empty();
    }
    
    @Override
    public Optional<Serializable> get( String key )
    {
        return Optional.ofNullable( defaultValue );
    }

    @Override
    public Optional<Serializable> get( String key, Function<String, Serializable> mappingFunction )
    {
        return Optional.ofNullable( Optional.ofNullable( mappingFunction.apply( key ) ).orElse( defaultValue ) );
    }

    @Override
    public void put( String key, Serializable value )
    {
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

}
