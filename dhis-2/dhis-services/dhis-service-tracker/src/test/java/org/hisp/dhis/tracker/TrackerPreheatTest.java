package org.hisp.dhis.tracker;

/*
 * Copyright (c) 2004-2019, University of Oslo
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
public class TrackerPreheatTest
{
    @Test
    public void testAllEmpty()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        assertTrue( preheat.isEmpty() );
        assertTrue( preheat.isEmpty( TrackerPreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( TrackerPreheatIdentifier.CODE ) );
    }

    @Test
    public void testPutUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( TrackerPreheatIdentifier.UID, de1 );
        preheat.put( TrackerPreheatIdentifier.UID, de2 );
        preheat.put( TrackerPreheatIdentifier.UID, de3 );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerPreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( TrackerPreheatIdentifier.CODE ) );

        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( TrackerPreheatIdentifier.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de2.getUid(), preheat.get( TrackerPreheatIdentifier.UID, DataElement.class, de2.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( TrackerPreheatIdentifier.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCode()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de1.setCode( "Code1" );
        de2.setAutoFields();
        de2.setCode( "Code2" );
        de3.setAutoFields();
        de3.setCode( "Code3" );

        preheat.put( TrackerPreheatIdentifier.CODE, de1 );
        preheat.put( TrackerPreheatIdentifier.CODE, de2 );
        preheat.put( TrackerPreheatIdentifier.CODE, de3 );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerPreheatIdentifier.CODE ) );
        assertTrue( preheat.isEmpty( TrackerPreheatIdentifier.UID ) );

        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.CODE, DataElement.class, de1.getCode() ) );
        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.CODE, DataElement.class, de2.getCode() ) );
        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.CODE, DataElement.class, de3.getCode() ) );

        assertEquals( de1.getCode(), preheat.get( TrackerPreheatIdentifier.CODE, DataElement.class, de1.getCode() ).getCode() );
        assertEquals( de2.getCode(), preheat.get( TrackerPreheatIdentifier.CODE, DataElement.class, de2.getCode() ).getCode() );
        assertEquals( de3.getCode(), preheat.get( TrackerPreheatIdentifier.CODE, DataElement.class, de3.getCode() ).getCode() );
    }

    @Test
    public void testPutCollectionUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( TrackerPreheatIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerPreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( TrackerPreheatIdentifier.CODE ) );

        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( TrackerPreheatIdentifier.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de2.getUid(), preheat.get( TrackerPreheatIdentifier.UID, DataElement.class, de2.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( TrackerPreheatIdentifier.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCollectionRemoveOneUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( TrackerPreheatIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerPreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( TrackerPreheatIdentifier.CODE ) );

        preheat.remove( TrackerPreheatIdentifier.UID, DataElement.class, de2.getUid() );

        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertFalse( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de3.getUid() ) );

        assertEquals( de1.getUid(), preheat.get( TrackerPreheatIdentifier.UID, DataElement.class, de1.getUid() ).getUid() );
        assertEquals( de3.getUid(), preheat.get( TrackerPreheatIdentifier.UID, DataElement.class, de3.getUid() ).getUid() );
    }

    @Test
    public void testPutCollectionRemoveAllUid()
    {
        TrackerPreheat preheat = new TrackerPreheat();

        DataElement de1 = new DataElement( "dataElementA" );
        DataElement de2 = new DataElement( "dataElementB" );
        DataElement de3 = new DataElement( "dataElementC" );

        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();

        preheat.put( TrackerPreheatIdentifier.UID, Lists.newArrayList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( TrackerPreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( TrackerPreheatIdentifier.CODE ) );

        preheat.remove( TrackerPreheatIdentifier.UID, DataElement.class, Lists.newArrayList( de1.getUid(), de2.getUid(), de3.getUid() ) );

        assertFalse( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertFalse( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertFalse( preheat.containsKey( TrackerPreheatIdentifier.UID, DataElement.class, de3.getUid() ) );
    }
}
