package org.hisp.dhis.preheat;

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

import com.google.common.collect.Lists;
import org.hisp.dhis.dataelement.DataElement;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PreheatTest
{
    @Test
    public void testAllEmpty()
    {
        Preheat preheat = new Preheat();

        assertTrue( preheat.isEmpty() );
        assertTrue( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );
    }

    @Test
    public void testPutUid()
    {
        Preheat preheat = new Preheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( PreheatIdentifier.UID, de1 );
        preheat.put( PreheatIdentifier.UID, de2 );
        preheat.put( PreheatIdentifier.UID, de3 );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( PreheatIdentifier.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de2.getUid(), preheat.get( PreheatIdentifier.UID, DataElement.class, de2.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( PreheatIdentifier.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCode()
    {
        Preheat preheat = new Preheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de1.setCode( "Code1" );
        de2.setAutoFields();
        de2.setCode( "Code2" );
        de3.setAutoFields();
        de3.setCode( "Code3" );

        preheat.put( PreheatIdentifier.CODE, de1 );
        preheat.put( PreheatIdentifier.CODE, de2 );
        preheat.put( PreheatIdentifier.CODE, de3 );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.CODE ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.UID ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, de1.getCode() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, de2.getCode() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.CODE, DataElement.class, de3.getCode() ) );

        assertEquals( de1.getCode(), preheat.get( PreheatIdentifier.CODE, DataElement.class, de1.getCode() ).getCode() );
        assertEquals( de2.getCode(), preheat.get( PreheatIdentifier.CODE, DataElement.class, de2.getCode() ).getCode() );
        assertEquals( de3.getCode(), preheat.get( PreheatIdentifier.CODE, DataElement.class, de3.getCode() ).getCode() );
    }

    @Test
    public void testPutCollectionUid()
    {
        Preheat preheat = new Preheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( PreheatIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( PreheatIdentifier.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de2.getUid(), preheat.get( PreheatIdentifier.UID, DataElement.class, de2.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( PreheatIdentifier.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCollectionRemoveOneUid()
    {
        Preheat preheat = new Preheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( PreheatIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );

        preheat.remove( PreheatIdentifier.UID, DataElement.class, de2.getUid() );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( PreheatIdentifier.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( PreheatIdentifier.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCollectionRemoveAllUid()
    {
        Preheat preheat = new Preheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( PreheatIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );

        preheat.remove( PreheatIdentifier.UID, DataElement.class, Lists.newArrayList( de1.getUid(), de2.getUid(), de3.getUid() ) );

        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );
    }
}
