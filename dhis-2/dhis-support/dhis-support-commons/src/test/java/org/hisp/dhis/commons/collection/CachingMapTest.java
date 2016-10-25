package org.hisp.dhis.commons.collection;

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

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

public class CachingMapTest
{
    @Test
    public void testLoad()
    {
        Set<Animal> animals = new HashSet<>();
        animals.add( new Animal( 1, "horse" ) );
        animals.add( new Animal( 2, "dog" ) );
        animals.add( new Animal( 3, "cat" ) );

        CachingMap<Integer, Animal> cache = new CachingMap<Integer, Animal>().load( animals, a -> a.getId() );

        assertEquals( 3, cache.size() );
        assertEquals( "horse", cache.get( 1 ).getName() );
        assertEquals( "dog", cache.get( 2 ).getName() );
        assertEquals( "cat", cache.get( 3 ).getName() );        
        assertFalse( cache.containsKey( "deer" ) );
    }
    
    @Test
    public void testLoadWithNull()
    {
        Set<Animal> animals = new HashSet<>();
        animals.add( new Animal( 1, "horse" ) );
        animals.add( new Animal( 2, null ) );
        animals.add( new Animal( 3, "cat" ) );

        CachingMap<String, Animal> cache = new CachingMap<String, Animal>().load( animals, a -> a.getName() );

        assertEquals( 2, cache.size() );
        assertEquals( 1, cache.get( "horse" ).getId() );       
        assertFalse( cache.containsKey( "dog" ) );
    }
    
    @Test
    public void testCacheHitMissCount()
    {
        CachingMap<Integer, Animal> cache = new CachingMap<Integer, Animal>();
        
        cache.put( 1, new Animal( 1, "horse" ) );
        cache.put( 2, new Animal( 2, "dog" ) );
        
        cache.get( 1, () -> null ); // Hit
        cache.get( 1, () -> null ); // Hit
        cache.get( 1, () -> null ); // Hit
        cache.get( 2, () -> null ); // Hit
        cache.get( 2, () -> null ); // Hit
        cache.get( 3, () -> null ); // Miss
        cache.get( 3, () -> null ); // Miss
        cache.get( 4, () -> null ); // Miss
        
        assertEquals( 5, cache.getCacheHitCount() );
        assertEquals( 3, cache.getCacheMissCount() );
    }

    @Test
    public void testCacheLoadCount()
    {
        Set<Animal> animals = new HashSet<>();
        animals.add( new Animal( 1, "horse" ) );
        animals.add( new Animal( 2, "dog" ) );
        animals.add( new Animal( 3, "cat" ) );

        CachingMap<Integer, Animal> cache = new CachingMap<Integer, Animal>();
        
        assertEquals( 0, cache.getCacheLoadCount() );
        
        cache.load( animals, a -> a.getId() );

        assertEquals( 1, cache.getCacheLoadCount() );
    }

    @Test
    public void testIsCacheLoaded()
    {
        Set<Animal> animals = new HashSet<>();
        animals.add( new Animal( 1, "horse" ) );
        animals.add( new Animal( 2, "dog" ) );
        animals.add( new Animal( 3, "cat" ) );

        CachingMap<Integer, Animal> cache = new CachingMap<Integer, Animal>();
        
        assertFalse( cache.isCacheLoaded() );
        
        cache.load( animals, a -> a.getId() );

        assertTrue( cache.isCacheLoaded() );
    }
    
    private class Animal
    {
        private int id;
        private String name;
        
        public Animal( int id, String name )
        {
            this.id = id;
            this.name = name;
        }
        
        public int getId()
        {
            return id;
        }

        public String getName()
        {
            return name;
        }
    }
}
