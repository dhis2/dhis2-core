package org.hisp.dhis.common;

/*
 * Copyright (c) 2004-2015, University of Oslo
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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lars Helge Overland
 */
public class MapMap<T, U, V>
    extends HashMap<T, Map<U, V>>
{
    public Map<U, V> putEntry( T key, U valueKey, V value )
    {
        Map<U, V> map = this.get( key );
        map = map == null ? new HashMap<>() : map;
        map.put( valueKey, value );
        return this.put( key, map );
    }
    
    public void putEntries( T key, Map<U, V> m )
    {
        Map<U, V> map = this.get( key );
        map = map == null ? new HashMap<>() : map;
        map.putAll( m );
        this.put( key, map );
    }

    public V getValue( T key, U valueKey )
    {
        return this.get( key ) == null ? null : this.get( key ).get( valueKey );
    }
    
    @SafeVarargs
    public static final <T, U, V> MapMap<T, U, V> asMapMap( final AbstractMap.SimpleEntry<T, Map<U, V>>... entries )
    {
        MapMap<T, U, V> map = new MapMap<>();

        for ( AbstractMap.SimpleEntry<T, Map<U, V>> entry : entries )
        {
            map.put( entry.getKey(), entry.getValue() );
        }

        return map;
    }
}
