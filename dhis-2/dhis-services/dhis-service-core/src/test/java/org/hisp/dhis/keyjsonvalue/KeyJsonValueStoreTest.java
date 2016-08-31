package org.hisp.dhis.keyjsonvalue;

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

import org.hisp.dhis.DhisSpringTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Stian Sandvold.
 */
public class KeyJsonValueStoreTest extends DhisSpringTest
{

    @Autowired
    private KeyJsonValueStore keyJsonValueStore;

    @Test
    public void testAddKeyJsonValue()
    {
        KeyJsonValue keyJsonValue = new KeyJsonValue();
        keyJsonValue.setNamespace( "A" );
        keyJsonValue.setKey( "1" );

        int id = keyJsonValueStore.save( keyJsonValue );

        assertNotNull( keyJsonValue );
        assertEquals( keyJsonValue, keyJsonValueStore.get( id ) );
    }

    @Test
    public void testAddKeyJsonValuesAndGetNamespaces()
    {
        KeyJsonValue keyJsonValueA = new KeyJsonValue();
        keyJsonValueA.setNamespace( "A" );
        keyJsonValueA.setKey( "1" );
        keyJsonValueStore.save( keyJsonValueA );

        KeyJsonValue keyJsonValueB = new KeyJsonValue();
        keyJsonValueB.setNamespace( "B" );
        keyJsonValueB.setKey( "1" );
        keyJsonValueStore.save( keyJsonValueB );

        List<String> list = keyJsonValueStore.getNamespaces();

        assertTrue( list.contains( "A" ) );
        assertTrue( list.contains( "B" ) );
    }

    @Test
    public void testAddKeyJsonValuesAndGetKeysFromNamespace()
    {
        KeyJsonValue keyJsonValueA = new KeyJsonValue();
        keyJsonValueA.setNamespace( "A" );
        keyJsonValueA.setKey( "1" );
        keyJsonValueStore.save( keyJsonValueA );

        KeyJsonValue keyJsonValueB = new KeyJsonValue();
        keyJsonValueB.setNamespace( "A" );
        keyJsonValueB.setKey( "2" );
        keyJsonValueStore.save( keyJsonValueB );

        List<String> list = keyJsonValueStore.getKeysInNamespace( "A" );

        assertTrue( list.contains( "1" ) );
        assertTrue( list.contains( "2" ) );
    }

    @Test
    public void testAddKeyJsonValueAndGetKeyJsonValue()
    {
        KeyJsonValue keyJsonValueA = new KeyJsonValue();
        keyJsonValueA.setNamespace( "A" );
        keyJsonValueA.setKey( "1" );
        keyJsonValueStore.save( keyJsonValueA );

        assertEquals( keyJsonValueStore.getKeyJsonValue( "A", "1" ), keyJsonValueA );
    }

    @Test
    public void testGetKeyJsonValuesByNamespace()
    {
        KeyJsonValue keyJsonValueA = new KeyJsonValue();
        keyJsonValueA.setNamespace( "A" );
        keyJsonValueA.setKey( "1" );
        keyJsonValueStore.save( keyJsonValueA );

        KeyJsonValue keyJsonValueB = new KeyJsonValue();
        keyJsonValueB.setNamespace( "A" );
        keyJsonValueB.setKey( "2" );
        keyJsonValueStore.save( keyJsonValueB );

        KeyJsonValue keyJsonValueC = new KeyJsonValue();
        keyJsonValueC.setNamespace( "A" );
        keyJsonValueC.setKey( "3" );
        keyJsonValueStore.save( keyJsonValueC );


        List<KeyJsonValue> list = keyJsonValueStore.getKeyJsonValueByNamespace( "A" );

        assertTrue(list.contains( keyJsonValueA ));
        assertTrue(list.contains( keyJsonValueB ));
        assertTrue(list.contains( keyJsonValueC ));
    }
}
