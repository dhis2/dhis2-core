package org.hisp.dhis.commons.collection;

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
        
        assertEquals( "horse", cache.get( 1 ).getName() );
        assertEquals( "dog", cache.get( 2 ).getName() );
        assertEquals( "cat", cache.get( 3 ).getName() );        
        assertFalse( cache.containsKey( "deer" ) );
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
