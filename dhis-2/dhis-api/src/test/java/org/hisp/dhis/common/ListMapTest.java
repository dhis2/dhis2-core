package org.hisp.dhis.common;

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

import org.hisp.dhis.common.ListMap;
import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;
import org.junit.Test;

import com.google.common.collect.Lists;

import static org.junit.Assert.*;

import java.util.List;

/**
 * @author Lars Helge Overland
 */
public class ListMapTest
{
    @Test
    public void testPutValue()
    {
        ListMap<String, Integer> map = new ListMap<>();
        
        map.putValue( "A", 1 );
        map.putValue( "B", 2 );
        map.putValue( "C", 3 );
        map.putValue( "A", 4 );
        map.putValue( "B", 5 );
        map.putValue( "A", 6 );
        
        assertEquals( Lists.newArrayList( 1, 4, 6 ), map.get( "A" ) );
        assertEquals( Lists.newArrayList( 2, 5 ), map.get( "B" ) );
        assertEquals( Lists.newArrayList( 3 ), map.get( "C" ) );
        assertNull( map.get( "Z" ) );
    }
    
    @Test
    public void testGetListMapValueMapper()
    {
        DataElementGroupSet groupSetA = new DataElementGroupSet( "GroupSetA" );
        DataElementGroupSet groupSetB = new DataElementGroupSet( "GroupSetB" );
        DataElementGroupSet groupSetC = new DataElementGroupSet( "GroupSetC" );
        DataElementGroupSet groupSetZ = new DataElementGroupSet( "GroupSetZ" );
        
        DataElementGroup groupA = new DataElementGroup( "GroupA" );
        DataElementGroup groupB = new DataElementGroup( "GroupB" );
        DataElementGroup groupC = new DataElementGroup( "GroupC" );
        DataElementGroup groupD = new DataElementGroup( "GroupD" );
        DataElementGroup groupE = new DataElementGroup( "GroupE" );
        DataElementGroup groupF = new DataElementGroup( "GroupF" );
        
        groupA.setGroupSet( groupSetA );
        groupB.setGroupSet( groupSetB );
        groupC.setGroupSet( groupSetC );
        groupD.setGroupSet( groupSetA );
        groupE.setGroupSet( groupSetB );
        groupF.setGroupSet( groupSetA );
        
        List<DataElementGroup> groups = Lists.newArrayList( groupA, groupB, groupC, groupD, groupE, groupF );
                        
        ListMap<DataElementGroupSet, DataElementGroup> map = ListMap.getListMap( groups, group -> group.getGroupSet() );
        
        assertEquals( Lists.newArrayList( groupA, groupD, groupF ), map.get( groupSetA ) );
        assertEquals( Lists.newArrayList( groupB, groupE ), map.get( groupSetB ) );
        assertEquals( Lists.newArrayList( groupC ), map.get( groupSetC ) );
        assertNull( map.get( groupSetZ ) );
    }

    @Test
    public void testGetListMapKeyValueMapper()
    {
        DataElementGroupSet groupSetA = new DataElementGroupSet( "GroupSetA" );
        DataElementGroupSet groupSetB = new DataElementGroupSet( "GroupSetB" );
        DataElementGroupSet groupSetC = new DataElementGroupSet( "GroupSetC" );
        DataElementGroupSet groupSetZ = new DataElementGroupSet( "GroupSetZ" );
        
        DataElementGroup groupA = new DataElementGroup( "GroupA" );
        DataElementGroup groupB = new DataElementGroup( "GroupB" );
        DataElementGroup groupC = new DataElementGroup( "GroupC" );
        DataElementGroup groupD = new DataElementGroup( "GroupD" );
        DataElementGroup groupE = new DataElementGroup( "GroupE" );
        DataElementGroup groupF = new DataElementGroup( "GroupF" );
        
        groupA.setGroupSet( groupSetA );
        groupB.setGroupSet( groupSetB );
        groupC.setGroupSet( groupSetC );
        groupD.setGroupSet( groupSetA );
        groupE.setGroupSet( groupSetB );
        groupF.setGroupSet( groupSetA );
        
        List<DataElementGroup> groups = Lists.newArrayList( groupA, groupB, groupC, groupD, groupE, groupF );
                        
        ListMap<DataElementGroupSet, Integer> map = ListMap.getListMap( groups, group -> group.getGroupSet(), group -> group.getId() );
        
        assertEquals( Lists.newArrayList( groupA.getId(), groupD.getId(), groupF.getId() ), map.get( groupSetA ) );
        assertEquals( Lists.newArrayList( groupB.getId(), groupE.getId() ), map.get( groupSetB ) );
        assertEquals( Lists.newArrayList( groupC.getId() ), map.get( groupSetC ) );
        assertNull( map.get( groupSetZ ) );
    }
}
