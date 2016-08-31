package org.hisp.dhis.commons.util;

/*
 * Copyright (c) 2004-2015, University of Oslo
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hisp.dhis.commons.collection.PaginatedList;
import org.junit.Test;

/**
 * @author Lars Helge Overland
 */
public class PaginatedListTest
{
    @Test
    public void testNextPage()
    {
        PaginatedList<String> list = new PaginatedList<>( Arrays.asList( "A", "B", "C" ) ).setPageSize( 2 );
        
        List<String> page = list.nextPage();
        
        assertNotNull( page );
        assertEquals( 2, page.size() );
        assertTrue( page.contains( "A" ) );
        assertTrue( page.contains( "B" ) );
        
        page = list.nextPage();
        
        assertNotNull( page );
        assertEquals( 1, page.size() );
        assertTrue( page.contains( "C" ) );
        
        page = list.nextPage();
        
        assertNull( page );
    }
    
    @Test
    public void testGetPageEmpty()
    {
        PaginatedList<String> list = new PaginatedList<>( new ArrayList<String>() ).setPageSize( 2 );
        
        List<String> page = list.nextPage();
        
        assertNull( page );
    }
    
    @Test
    public void testPageCount()
    {
        PaginatedList<String> list = new PaginatedList<>( Arrays.asList( "A", "B", "C" ) ).setPageSize( 2 );
        
        assertEquals( 2, list.pageCount() );
        
        list = new PaginatedList<>( Arrays.asList( "A", "B", "C", "D" ) ).setPageSize( 2 );
        
        assertEquals( 2, list.pageCount() );

        list = new PaginatedList<>( Arrays.asList( "A", "B", "C", "D", "E" ) ).setPageSize( 2 );
        
        assertEquals( 3, list.pageCount() );
    }
    
    @Test
    public void testReset()
    {
        PaginatedList<String> list = new PaginatedList<>( Arrays.asList( "A", "B", "C" ) ).setPageSize( 2 );
        
        assertTrue( list.nextPage().contains( "A" ) );
        
        list.reset();

        assertTrue( list.nextPage().contains( "A" ) );        
    }
    
    @Test
    public void testSetNumberOfPages()
    {
        PaginatedList<String> list = new PaginatedList<>( Arrays.asList( "A", "B", "C", "D", "E" ) ).setNumberOfPages( 3 );
        
        assertEquals( 3, list.pageCount() );
        
        assertEquals( 2, list.nextPage().size() );
    }
    
    @Test
    public void testNextPageNumberOfPages()
    {
        PaginatedList<String> list = new PaginatedList<>( Arrays.asList( "A", "B", "C", "D", "E" ) ).setNumberOfPages( 2 );
        
        List<String> page = list.nextPage();
        
        assertNotNull( page );
        assertEquals( 3, page.size() );
        assertTrue( page.contains( "A" ) );
        assertTrue( page.contains( "B" ) );
        assertTrue( page.contains( "C" ) );
        
        page = list.nextPage();
        
        assertNotNull( page );
        assertEquals( 2, page.size() );
        assertTrue( page.contains( "D" ) );
        assertTrue( page.contains( "E" ) );
        
        page = list.nextPage();
        
        assertNull( page );
    }
    
    @Test
    public void testGetPages()
    {
        PaginatedList<String> list = new PaginatedList<>( Arrays.asList( "A", "B", "C", "D", "E" ) ).setPageSize( 2 );
        
        List<List<String>> pages = list.getPages();

        assertNotNull( pages );
        assertEquals( 3, pages.size() );
        
        List<String> page = pages.get( 0 );
        assertNotNull( page );
        assertEquals( 2, page.size() );
        assertTrue( page.contains( "A" ) );
        assertTrue( page.contains( "B" ) );
    }
}
