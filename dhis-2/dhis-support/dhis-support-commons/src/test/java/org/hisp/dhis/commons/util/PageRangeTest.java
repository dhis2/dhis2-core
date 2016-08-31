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

import java.util.List;

import org.hisp.dhis.commons.util.PageRange;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Lars Helge Overland
 */
public class PageRangeTest
{
    @Test
    public void testPageSize()
    {
        PageRange range = new PageRange( 12 ).setPageSize( 5 );
        
        assertTrue( range.nextPage() );        
        assertEquals( 0, range.getFromIndex() );
        assertEquals( 5, range.getToIndex() );

        assertTrue( range.nextPage() );        
        assertEquals( 5, range.getFromIndex() );
        assertEquals( 10, range.getToIndex() );
        
        assertTrue( range.nextPage() );        
        assertEquals( 10, range.getFromIndex() );
        assertEquals( 12, range.getToIndex() );
        
        assertFalse( range.nextPage() );
    }

    @Test
    public void testPages()
    {
        PageRange range = new PageRange( 11 ).setPages( 3 );
        
        assertTrue( range.nextPage() );        
        assertEquals( 0, range.getFromIndex() );
        assertEquals( 4, range.getToIndex() );

        assertTrue( range.nextPage() );        
        assertEquals( 4, range.getFromIndex() );
        assertEquals( 8, range.getToIndex() );
        
        assertTrue( range.nextPage() );        
        assertEquals( 8, range.getFromIndex() );
        assertEquals( 11, range.getToIndex() );
        
        assertFalse( range.nextPage() );
    }
    
    @Test
    public void testGetPages()
    {
        PageRange range = new PageRange( 12 ).setPageSize( 5 );
        
        List<int[]> pages = range.getPages();
        
        assertEquals( 3, pages.size() );
        assertEquals( 0, pages.get( 0 )[0] );
        assertEquals( 5, pages.get( 0 )[1] );
        assertEquals( 5, pages.get( 1 )[0] );
        assertEquals( 10, pages.get( 1 )[1] );
        assertEquals( 10, pages.get( 2 )[0] );
        assertEquals( 12, pages.get( 2 )[1] );
    }
}
