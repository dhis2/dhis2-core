/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PagerTest
{
    @Test
    void testGetPageCount()
    {
        assertEquals( 5, new Pager( 1, 214, 50 ).getPageCount() );
        assertEquals( 1, new Pager( 2, 35, 50 ).getPageCount() );
        assertEquals( 4, new Pager( 3, 35, 10 ).getPageCount() );
    }

    @Test
    void testGetOffset()
    {
        assertEquals( 125, new Pager( 6, 500, 25 ).getOffset() );
        assertEquals( 45, new Pager( 10, 500, 5 ).getOffset() );
    }

    @Test
    void testGetPage()
    {
        assertEquals( 3, new Pager( 3, 240, 50 ).getPage() );
    }

    @Test
    void testGetPageWhenGreaterThanTotalPages()
    {
        assertEquals( 5, new Pager( 8, 240, 50 ).getPage() );
    }

    @Test
    void testGetPageWhenLessThanOne()
    {
        assertEquals( 1, new Pager( -1, 240, 50 ).getPage() );
    }

    @Test
    void testGetPageSize()
    {
        assertEquals( 30, new Pager( 3, 240, 30 ).getPageSize() );
    }

    @Test
    void testGetPageSizeWhenLessThan1()
    {
        assertEquals( 1, new Pager( 3, 240, 0 ).getPageSize() );
    }

    @Test
    void testGetTotal()
    {
        assertEquals( 200, new Pager( 2, 200, 50 ).getTotal() );
    }

    @Test
    void testTotalZero()
    {
        assertEquals( 0, new Pager( 1, 0, 50 ).getTotal() );
    }

    @Test
    void testTotalWhenLessThanZeroA()
    {
        assertEquals( 0, new Pager( 4, -5 ).getTotal() );
    }

    @Test
    void testTotalWhenLessThanZeroB()
    {
        assertEquals( 0, new Pager( 4, -5, 50 ).getTotal() );
    }

    @Test
    void testGetOffsetWithTotalZero()
    {
        assertEquals( 0, new Pager( 2, 0, 50 ).getOffset() );
    }
}
