package org.hisp.dhis.common;

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

import java.util.*;
import java.util.function.Function;

/**
 * @author Lars Helge Overland
 */
public class ListMap<T, V>
    extends HashMap<T, List<V>>
{
    /**
     * Determines if a de-serialized file is compatible with this class.
     */
    private static final long serialVersionUID = 4880664228933342003L;

    public ListMap()
    {
        super();
    }
    
    public ListMap( ListMap<T, V> listMap )
    {
        super( listMap );
    }

    public List<V> putValue( T key, V value )
    {
        List<V> list = this.get( key );
        list = list == null ? new ArrayList<>() : list;
        list.add( value );
        super.put( key, list );
        return null;
    }

    public List<V> putValues( T key, Collection<V> values )
    {
        for ( V value : values )
        {
            putValue( key, value );
        }

        return null;
    }

    public void putValueMap( Map<T, V> map )
    {
        for ( Map.Entry<T, V> entry : map.entrySet() )
        {
            putValue( entry.getKey(), entry.getValue() );
        }
    }

    public void putAll( ListMap<T, V> map )
    {
        for ( T key : map.keySet() )
        {
            putValues( key, map.get( key ) );
        }
    }

    public Collection<V> allValues()
    {
        Collection<V> results = new ArrayList<>();
        
        for ( Map.Entry<T, List<V>> entry: entrySet() )
        {
            results.addAll( entry.getValue() );
        }
        
        return results;
    }

    public Set<V> uniqueValues( )
    {
        Set<V> results = new HashSet<>();
        
        for ( Map.Entry<T, List<V>> entry: entrySet() )
        {
            results.addAll( entry.getValue() );
        }
        
        return results;
    }

    public boolean containsValue( T key, V value )
    {
        List<V> list = this.get( key );
        
        if ( list == null )
        {
            return false;
        }
        
        if ( list.contains( value ) )
        {
            return true;
        }
        
        return false;
    }

    /**
     * Produces a ListMap based on the given list of values. The key for
     * each entry is produced by applying the given keyMapper function.
     * 
     * @param values the values of the map.
     * @param keyMapper the function producing the key for each entry.
     * @return a ListMap.
     */
    public static <T, V> ListMap<T, V> getListMap( List<V> values, Function<V, T> keyMapper )
    {
        ListMap<T, V> map = new ListMap<>();
        
        for ( V value : values )
        {
            T key = keyMapper.apply( value );
            
            map.putValue( key, value );
        }
        
        return map;
    }

    /**
     * Produces a ListMap based on the given list of values. The key for
     * each entry is produced by applying the given keyMapper function. The value
     * for each entry is produced by applying the given valueMapper function. 
     * 
     * @param values the values of the map.
     * @param keyMapper the function producing the key for each entry.
     * @param valueMapper the function producing the value for each entry.
     * @return a ListMap.
     */
    public static <T, U, V> ListMap<T, U> getListMap( List<V> values, Function<V, T> keyMapper, Function<V, U> valueMapper )
    {
        ListMap<T, U> map = new ListMap<>();
        
        for ( V value : values )
        {
            T key = keyMapper.apply( value );
            U val = valueMapper.apply( value );
            
            map.putValue( key, val );
        }
        
        return map;
    }

    /**
     * Returns a union of two same-type ListMaps. Either or both of the input
     * ListMaps may be null. The returned ListMap never is.
     *
     * @param a one ListMap.
     * @param b the other ListMap.
     * @return union of the two ListMaps.
     */
    public static <T, V> ListMap<T, V> union( ListMap<T, V> a, ListMap<T, V> b )
    {
        if ( a == null || a.isEmpty() )
        {
            if ( b == null || b.isEmpty() )
            {
                return new ListMap<T, V>();
            }

            return b;
        }
        else if ( b == null || b.isEmpty() )
        {
            return a;
        }

        ListMap<T, V> c = new ListMap<T, V>( a );

        for ( Map.Entry<T, List<V>> entry : b.entrySet() )
        {
            for ( V value : entry.getValue() )
            {
                c.putValue( entry.getKey(), value );
            }
        }

        return c;
    }
}
