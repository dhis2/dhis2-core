/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Jim Grace
 */
public class MapMapMap<S, T, U, V>
    extends HashMap<S, MapMap<T, U, V>>
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 4505153475282323148L;

    public MapMap<T, U, V> putEntry( S key1, T key2, U key3, V value )
    {
        MapMap<T, U, V> map = this.computeIfAbsent( key1, k -> new MapMap<>() );
        map.putEntry( key2, key3, value );
        return map;
    }

    public void putEntries( S key1, MapMap<T, U, V> m )
    {
        MapMap<T, U, V> map = this.computeIfAbsent( key1, k -> new MapMap<>() );
        map.putMap( m );
    }

    public void putMap( MapMapMap<S, T, U, V> map )
    {
        for ( Entry<S, MapMap<T, U, V>> entry : map.entrySet() )
        {
            this.putEntries( entry.getKey(), entry.getValue() );
        }
    }

    public V getValue( S key1, T key2, U key3 )
    {
        MapMap<T, U, V> map = this.get( key1 );
        return map == null ? null : map.getValue( key2, key3 );
    }

    @SafeVarargs
    public static <S, T, U, V> MapMapMap<S, T, U, V> ofEntries( Map.Entry<S, MapMap<T, U, V>>... entries )
    {
        MapMapMap<S, T, U, V> map = new MapMapMap<>();

        for ( Map.Entry<S, MapMap<T, U, V>> entry : entries )
        {
            map.put( entry.getKey(), entry.getValue() );
        }

        return map;
    }
}
