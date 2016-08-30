package org.hisp.dhis.commons.collection;

import java.util.Collection;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Map which allows storing a {@link java.util.concurrent.Callable}
 * and caches its return value on the first call to get(Object, Callable).
 * Subsequent calls returns the cached value.
 *
 * @author Lars Helge Overland
 */
public class CachingMap<K, V>
    extends HashMap<K, V>
{
    // -------------------------------------------------------------------------
    // Internal variables
    // -------------------------------------------------------------------------

    private long cacheHitCount;
    
    private long cacheMissCount;
    
    private long cacheLoadCount;

    // -------------------------------------------------------------------------
    // Methods
    // -------------------------------------------------------------------------

    /**
     * Returns the cached value if available or executes the Callable and returns
     * the value, which is also cached. Will not attempt to fetch values for null
     * keys, to avoid potentially expensive and pointless operations.
     *
     * @param key the key.
     * @param callable the Callable.
     * @return the return value of the Callable, either from cache or immediate execution.
     */
    public V get( K key, Callable<V> callable )
    {
        if ( key == null )
        {
            return null;
        }
        
        V value = super.get( key );

        if ( value == null )
        {
            try
            {
                value = callable.call();
                
                super.put( key, value );
                
                cacheMissCount++;
            }
            catch ( Exception ex )
            {
                throw new RuntimeException( ex );
            }
        }
        else
        {
            cacheHitCount++;
        }
        
        return value;
    }
    
    /**
     * Loads the cache with the given content. Entries for which the key is a
     * null reference are ignored.
     * 
     * @param collection the content collection.
     * @param keyMapper the function to produce the cache key for a content item.
     * @return a reference to this caching map.
     */
    public CachingMap<K, V> load( Collection<V> collection, Function<V, K> keyMapper )
    {
        for ( V item : collection )
        {
            K key = keyMapper.apply( item );
            
            if ( key != null )
            {
                super.put( key, item );
            }            
        }
        
        cacheLoadCount++;
        
        return this;
    }

    /**
     * Returns the number of cache hits from calling the {@link get} method.
     * 
     * @return the number of cache hits.
     */
    public long getCacheHitCount()
    {
        return cacheHitCount;
    }

    /**
     * Returns the number of cache misses from calling the {@link get} method.
     * 
     * @return the number of cache misses.
     */
    public long getCacheMissCount()
    {
        return cacheMissCount;
    }
    
    /**
     * Returns the ratio between cache hits and misses from calling the 
     * {@link get} method.
     * 
     * @return the cache hit versus miss ratio.
     */
    public double getCacheHitRatio()
    {
        return (double) cacheHitCount / (double) cacheMissCount;
    }

    /**
     * Returns the number of times the cache has been loaded.
     * 
     * @return the number of times the cache has been loaded.
     */
    public long getCacheLoadCount()
    {
        return cacheLoadCount;
    }
    
    /**
     * Indicates whether the cache has been loaded at least one time.
     * 
     * @return true if the cache has been loaded at least one time.
     */
    public boolean isCacheLoaded()
    {
        return cacheLoadCount > 0;
    }
}
