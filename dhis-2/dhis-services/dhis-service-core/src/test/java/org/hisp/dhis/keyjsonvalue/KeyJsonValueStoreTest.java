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
package org.hisp.dhis.keyjsonvalue;

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Stian Sandvold.
 */
public class KeyJsonValueStoreTest extends DhisSpringTest
{

    @Autowired
    private KeyJsonValueStore store;

    @Test
    public void testAddKeyJsonValue()
    {
        KeyJsonValue entry = addKeyJsonValue( "A", "1" );

        assertNotNull( entry );
        assertEquals( entry, store.get( entry.getId() ) );
    }

    @Test
    public void testAddKeyJsonValuesAndGetNamespaces()
    {
        addKeyJsonValue( "A", "1" );
        addKeyJsonValue( "B", "1" );

        assertContainsOnly( store.getNamespaces(), "A", "B" );
    }

    @Test
    public void testAddKeyJsonValuesAndGetKeysFromNamespace()
    {
        addKeyJsonValue( "A", "1" );
        addKeyJsonValue( "A", "2" );
        addKeyJsonValue( "B", "1" );

        assertContainsOnly( store.getKeysInNamespace( "A" ), "1", "2" );
    }

    @Test
    public void testAddKeyJsonValueAndGetKeyJsonValue()
    {
        KeyJsonValue entryA = addKeyJsonValue( "A", "1" );

        assertEquals( store.getKeyJsonValue( "A", "1" ), entryA );
    }

    @Test
    public void testGetKeyJsonValuesByNamespace()
    {
        KeyJsonValue entryA1 = addKeyJsonValue( "A", "1" );
        KeyJsonValue entryA2 = addKeyJsonValue( "A", "2" );
        KeyJsonValue entryA3 = addKeyJsonValue( "A", "3" );
        KeyJsonValue entryB1 = addKeyJsonValue( "B", "1" );

        assertContainsOnly( store.getKeyJsonValueByNamespace( "A" ), entryA1, entryA2, entryA3 );
        assertFalse( store.getKeyJsonValueByNamespace( "A" ).contains( entryB1 ) );
    }

    @Test
    public void testCountKeysInNamespace()
    {
        addKeyJsonValue( "A", "1" );
        addKeyJsonValue( "A", "2" );
        addKeyJsonValue( "A", "3" );
        addKeyJsonValue( "B", "1" );

        assertEquals( 3, store.countKeysInNamespace( "A" ) );
        assertEquals( 1, store.countKeysInNamespace( "B" ) );
        assertEquals( 0, store.countKeysInNamespace( "C" ) );
    }

    @Test
    public void deleteNamespace()
    {
        addKeyJsonValue( "A", "1" );
        addKeyJsonValue( "A", "3" );
        addKeyJsonValue( "B", "1" );
        addKeyJsonValue( "C", "1" );

        store.deleteNamespace( "A" );

        assertContainsOnly( store.getNamespaces(), "B", "C" );
    }

    private KeyJsonValue addKeyJsonValue( String ns, String key )
    {
        KeyJsonValue entry = createKeyJsonValue( ns, key );
        store.save( entry );
        return entry;
    }

    private static KeyJsonValue createKeyJsonValue( String ns, String key )
    {
        return new KeyJsonValue( ns, key );
    }
}
