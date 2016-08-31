package org.hisp.dhis.commons.util;

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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hisp.dhis.commons.collection.ListUtils;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class ListUtilsTest
{
    @Test
    public void testRemoveAll()
    {
        List<String> list = new ArrayList<>( Arrays.asList( "a", "b", "c", "d", "e", "f", "g", "h" ) );
        
        Integer[] indexes = { 0, 2, 5, 7, -1, 78 };

        assertEquals( 8, list.size() );
        
        ListUtils.removeAll( list, indexes );
        
        assertEquals( 4, list.size() );
        assertTrue( list.contains( "b" ) );
        assertTrue( list.contains( "d" ) );
        assertTrue( list.contains( "e" ) );
        assertTrue( list.contains( "g" ) );
    }
    
    @Test
    public void testGetDuplicates()
    {
        List<String> list = new ArrayList<>( Arrays.asList( "a", "b", "c", "c", "d", "e", "e", "e", "f" ) );
        Set<String> expected = new HashSet<>( Arrays.asList( "c", "e" ) );
        assertEquals( expected, ListUtils.getDuplicates( list ) );
        
        list = new ArrayList<>( Arrays.asList( "a", "b", "c", "d", "e", "f", "g", "h" ) );
        assertEquals( 0, ListUtils.getDuplicates( list ).size() );
    }
}
