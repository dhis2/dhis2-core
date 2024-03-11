package org.hisp.dhis.sqlview;

/*
 * Copyright (c) 2004-2018, University of Oslo
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * @author Lars Helge Overland
 */
public class SqlViewTest
{
    @Test
    public void testIsValidQueryValue()
    {
        assertTrue( SqlView.isValidQueryValue( "east" ) );
        assertTrue( SqlView.isValidQueryValue( "NUMBER" ) );
        assertTrue( SqlView.isValidQueryValue( "2015-03-01" ) );
        assertTrue( SqlView.isValidQueryValue( "John Doe" ) );
        assertTrue( SqlView.isValidQueryValue( "anc_1" ) );
        
        assertFalse( SqlView.isValidQueryValue( "../var/dir" ) );
        assertFalse( SqlView.isValidQueryValue( "delete from table;" ) );
    }
    
    @Test
    public void testGetCriteria()
    {
        Set<String> params = Sets.newHashSet( "type:NUMBER", "aggregationType:AVERAGE" );
        
        Map<String, String> expected = ImmutableMap.of( "type", "NUMBER", "aggregationType", "AVERAGE" );
        
        assertEquals( expected, SqlView.getCriteria( params ) );
    }

    @Test
    public void testCompareTo()
    {
        SqlView svA = new SqlView( "SqlViewA", "", SqlViewType.QUERY );
        SqlView svB = new SqlView( "SqlViewB", "", SqlViewType.QUERY );
        SqlView svC = new SqlView( "SqlViewC", "", SqlViewType.QUERY );
        SqlView svD = new SqlView( "SqlViewD", "", SqlViewType.QUERY );
        SqlView svE = new SqlView( "SqlViewE", "", SqlViewType.QUERY );
        SqlView svF = new SqlView( null, null, SqlViewType.QUERY );
        
        svA.setUid( "UidA" );
        svB.setUid( "UidB" );
        svC.setUid( "UidC" );
        svD.setUid( "UidD" );
        svE.setUid( "UidE" );
        svF.setUid( "UidF" );
        
        List<SqlView> list = Lists.newArrayList( svB, svE, svF, svC, svA, svD );
        
        Collections.sort( list );
        
        assertEquals( svA, list.get( 0 ) );
        assertEquals( svB, list.get( 1 ) );
        assertEquals( svC, list.get( 2 ) );
        assertEquals( svD, list.get( 3 ) );
        assertEquals( svE, list.get( 4 ) );
        assertEquals( svF, list.get( 5 ) );
    }
}
