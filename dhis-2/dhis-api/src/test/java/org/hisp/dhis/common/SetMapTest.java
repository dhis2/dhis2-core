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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Set;

import org.hisp.dhis.dataelement.DataElementGroup;
import org.hisp.dhis.dataelement.DataElementGroupSet;

import org.junit.Test;

import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class SetMapTest
{
    @Test
    public void testPutValue()
    {
        SetMap<String, Integer> map = new SetMap<>();
        
        map.putValue( "a", 1 );
        map.putValue( "a", 2 );
        map.putValue( "a", 3 );
        map.putValue( "b", 4 );
        map.putValue( "b", 5 );
        map.putValue( "c", 6 );
        
        assertEquals( Sets.newHashSet( 1, 2, 3 ), map.get( "a" ) );
        assertEquals( Sets.newHashSet( 4, 5 ), map.get( "b" ) );
        assertEquals( Sets.newHashSet( 6 ), map.get( "c" ) );        
    }

    @Test
    public void testPutValues()
    {
        SetMap<String, Integer> map = new SetMap<>();

        map.putValues( "a", Sets.newHashSet( 1, 2, 3 ) );
        map.putValues( "a", Sets.newHashSet( 3, 4 ) );
        map.putValues( "b", Sets.newHashSet( 5, 6 ) );
        
        assertEquals( Sets.newHashSet( 1, 2, 3, 4 ), map.get( "a" ) );
        assertEquals( Sets.newHashSet( 5, 6 ), map.get( "b" ) );
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
        
        groupA.getGroupSets().add( groupSetA );
        groupB.getGroupSets().add( groupSetB );
        groupC.getGroupSets().add( groupSetC );
        groupD.getGroupSets().add( groupSetA );
        groupE.getGroupSets().add( groupSetB );
        groupF.getGroupSets().add( groupSetA );
        
        Set<DataElementGroup> groups = Sets.newHashSet( groupA, groupB, groupC, groupD, groupE, groupF );
                        
        SetMap<DataElementGroupSet, DataElementGroup> map = SetMap.getSetMap( groups, group -> group.getGroupSets().iterator().next() );
        
        assertEquals( Sets.newHashSet( groupA, groupD, groupF ), map.get( groupSetA ) );
        assertEquals( Sets.newHashSet( groupB, groupE ), map.get( groupSetB ) );
        assertEquals( Sets.newHashSet( groupC ), map.get( groupSetC ) );
        assertNull( map.get( groupSetZ ) );
    }
}
