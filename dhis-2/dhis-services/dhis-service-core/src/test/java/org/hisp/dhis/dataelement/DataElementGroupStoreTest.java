package org.hisp.dhis.dataelement;

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
import org.hisp.dhis.common.GenericIdentifiableObjectStore;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 * @version $Id$
 */
public class DataElementGroupStoreTest
    extends DhisSpringTest
{
    @Resource(name="org.hisp.dhis.dataelement.DataElementGroupStore")
    private GenericIdentifiableObjectStore<DataElementGroup> dataElementGroupStore;
    
    @Test
    public void testAddDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );
        
        dataElementGroupStore.save( dataElementGroupA );
        int idA = dataElementGroupA.getId();
        dataElementGroupStore.save( dataElementGroupB );
        int idB = dataElementGroupB.getId();
        dataElementGroupStore.save( dataElementGroupC );
        int idC = dataElementGroupC.getId();

        dataElementGroupA = dataElementGroupStore.get( idA );
        assertNotNull( dataElementGroupA );
        assertEquals( idA, dataElementGroupA.getId() );
        assertEquals( "DataElementGroupA", dataElementGroupA.getName() );

        dataElementGroupB = dataElementGroupStore.get( idB );
        assertNotNull( dataElementGroupB );
        assertEquals( idB, dataElementGroupB.getId() );
        assertEquals( "DataElementGroupB", dataElementGroupB.getName() );

        dataElementGroupC = dataElementGroupStore.get( idC );
        assertNotNull( dataElementGroupC );
        assertEquals( idC, dataElementGroupC.getId() );
        assertEquals( "DataElementGroupC", dataElementGroupC.getName() );
    }

    @Test
    public void testUpdateDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );

        dataElementGroupStore.save( dataElementGroupA );
        int idA = dataElementGroupA.getId();
        dataElementGroupStore.save( dataElementGroupB );
        int idB = dataElementGroupB.getId();
        dataElementGroupStore.save( dataElementGroupC );
        int idC = dataElementGroupC.getId();

        dataElementGroupA = dataElementGroupStore.get( idA );
        assertNotNull( dataElementGroupA );
        assertEquals( idA, dataElementGroupA.getId() );
        assertEquals( "DataElementGroupA", dataElementGroupA.getName() );

        dataElementGroupA.setName( "DataElementGroupAA" );
        dataElementGroupStore.update( dataElementGroupA );

        dataElementGroupA = dataElementGroupStore.get( idA );
        assertNotNull( dataElementGroupA );
        assertEquals( idA, dataElementGroupA.getId() );
        assertEquals( "DataElementGroupAA", dataElementGroupA.getName() );

        dataElementGroupB = dataElementGroupStore.get( idB );
        assertNotNull( dataElementGroupB );
        assertEquals( idB, dataElementGroupB.getId() );
        assertEquals( "DataElementGroupB", dataElementGroupB.getName() );

        dataElementGroupC = dataElementGroupStore.get( idC );
        assertNotNull( dataElementGroupC );
        assertEquals( idC, dataElementGroupC.getId() );
        assertEquals( "DataElementGroupC", dataElementGroupC.getName() );
    }

    @Test
    public void testDeleteAndGetDataElementGroup()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        DataElementGroup dataElementGroupC = new DataElementGroup( "DataElementGroupC" );
        DataElementGroup dataElementGroupD = new DataElementGroup( "DataElementGroupD" );

        dataElementGroupStore.save( dataElementGroupA );
        int idA = dataElementGroupA.getId();
        dataElementGroupStore.save( dataElementGroupB );
        int idB = dataElementGroupB.getId();
        dataElementGroupStore.save( dataElementGroupC );
        int idC = dataElementGroupC.getId();
        dataElementGroupStore.save( dataElementGroupD );
        int idD = dataElementGroupD.getId();

        assertNotNull( dataElementGroupStore.get( idA ) );
        assertNotNull( dataElementGroupStore.get( idB ) );
        assertNotNull( dataElementGroupStore.get( idC ) );
        assertNotNull( dataElementGroupStore.get( idD ) );

        dataElementGroupStore.delete( dataElementGroupA );
        assertNull( dataElementGroupStore.get( idA ) );
        assertNotNull( dataElementGroupStore.get( idB ) );
        assertNotNull( dataElementGroupStore.get( idC ) );
        assertNotNull( dataElementGroupStore.get( idD ) );

        dataElementGroupStore.delete( dataElementGroupB );
        assertNull( dataElementGroupStore.get( idA ) );
        assertNull( dataElementGroupStore.get( idB ) );
        assertNotNull( dataElementGroupStore.get( idC ) );
        assertNotNull( dataElementGroupStore.get( idD ) );

        dataElementGroupStore.delete( dataElementGroupC );
        assertNull( dataElementGroupStore.get( idA ) );
        assertNull( dataElementGroupStore.get( idB ) );
        assertNull( dataElementGroupStore.get( idC ) );
        assertNotNull( dataElementGroupStore.get( idD ) );

        dataElementGroupStore.delete( dataElementGroupD );
        assertNull( dataElementGroupStore.get( idA ) );
        assertNull( dataElementGroupStore.get( idB ) );
        assertNull( dataElementGroupStore.get( idC ) );
        assertNull( dataElementGroupStore.get( idD ) );
    }

    @Test
    public void testgetByName()
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        dataElementGroupStore.save( dataElementGroupA );
        int idA = dataElementGroupA.getId();
        dataElementGroupStore.save( dataElementGroupB );
        int idB = dataElementGroupB.getId();

        assertNotNull( dataElementGroupStore.get( idA ) );
        assertNotNull( dataElementGroupStore.get( idB ) );

        dataElementGroupA = dataElementGroupStore.getByName( "DataElementGroupA" );
        assertNotNull( dataElementGroupA );
        assertEquals( idA, dataElementGroupA.getId() );
        assertEquals( "DataElementGroupA", dataElementGroupA.getName() );

        dataElementGroupB = dataElementGroupStore.getByName( "DataElementGroupB" );
        assertNotNull( dataElementGroupB );
        assertEquals( idB, dataElementGroupB.getId() );
        assertEquals( "DataElementGroupB", dataElementGroupB.getName() );

        DataElementGroup dataElementGroupC = dataElementGroupStore.getByName( "DataElementGroupC" );
        assertNull( dataElementGroupC );
    }

    @Test
    public void testGetAllDataElementGroups()
        throws Exception
    {
        DataElementGroup dataElementGroupA = new DataElementGroup( "DataElementGroupA" );
        DataElementGroup dataElementGroupB = new DataElementGroup( "DataElementGroupB" );
        dataElementGroupStore.save( dataElementGroupA );
        dataElementGroupStore.save( dataElementGroupB );

        List<DataElementGroup> groups = dataElementGroupStore.getAll();
        
        assertTrue( groups.size() == 2 );
        assertTrue( groups.contains( dataElementGroupA ) );
        assertTrue( groups.contains( dataElementGroupB ) );
    }
}
