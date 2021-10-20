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
package org.hisp.dhis.preheat;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.user.User;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class PreheatTest
{

    private final Preheat preheat = new Preheat();

    private final DataElement de1 = new DataElement( "dataElementA" );

    private final DataElement de2 = new DataElement( "dataElementB" );

    private final DataElement de3 = new DataElement( "dataElementC" );

    @Before
    public void setUp()
    {
        de1.setAutoFields();
        de2.setAutoFields();
        de3.setAutoFields();
    }

    @Test
    public void testAllEmpty()
    {
        assertTrue( preheat.isEmpty() );
        assertTrue( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.UID, User.class ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE, User.class ) );
    }

    @Test
    public void testPutUid()
    {
        preheat.put( PreheatIdentifier.UID, de1 );
        preheat.put( PreheatIdentifier.UID, de2 );
        preheat.put( PreheatIdentifier.UID, de3 );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );

        assertSame( de1, preheat.get( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertSame( de2, preheat.get( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertSame( de3, preheat.get( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );
    }

    @Test
    public void testPutCode()
    {
        de1.setCode( "Code1" );
        de2.setCode( "Code2" );
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

        assertSame( de1, preheat.get( PreheatIdentifier.CODE, DataElement.class, de1.getCode() ) );
        assertSame( de2, preheat.get( PreheatIdentifier.CODE, DataElement.class, de2.getCode() ) );
        assertSame( de3, preheat.get( PreheatIdentifier.CODE, DataElement.class, de3.getCode() ) );
    }

    @Test
    public void testPutCollectionUid()
    {
        preheat.put( PreheatIdentifier.UID, asList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );

        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertTrue( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );

        assertSame( de1, preheat.get( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertSame( de2, preheat.get( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertSame( de3, preheat.get( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );
    }

    @Test
    public void testPutCollectionRemoveOneUid()
    {
        preheat.put( PreheatIdentifier.UID, asList( de1, de2, de3 ) );

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
        preheat.put( PreheatIdentifier.UID, asList( de1, de2, de3 ) );

        assertFalse( preheat.isEmpty() );
        assertFalse( preheat.isEmpty( PreheatIdentifier.UID ) );
        assertTrue( preheat.isEmpty( PreheatIdentifier.CODE ) );

        preheat.remove( PreheatIdentifier.UID, DataElement.class, asList( de1.getUid(), de2.getUid(), de3.getUid() ) );

        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de1.getUid() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de2.getUid() ) );
        assertFalse( preheat.containsKey( PreheatIdentifier.UID, DataElement.class, de3.getUid() ) );
    }
}
