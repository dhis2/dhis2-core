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
package org.hisp.dhis.cache;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hisp.dhis.period.PeriodType;
import org.junit.Test;

/**
 * Tests the {@link Sizeof} and {@link GenericSizeof} implementation.
 *
 * @author Jan Bernitt
 */
public class SizeofTest
{

    public static class PrimitiveAndWrapperBean
    {
        int a;

        long b;

        Integer c;
    }

    public static class CollectionBean
    {
        List<Integer> a;

        List<List<Long>> b;

        CollectionBean()
        {
            this( null, null );
        }

        CollectionBean( List<Integer> a, List<List<Long>> b )
        {
            this.a = a;
            this.b = b;
        }
    }

    private final Sizeof sizeof = new GenericSizeof( 20L, obj -> obj );

    @Test
    public void testSizeofNull()
    {
        assertEquals( 0L, sizeof.sizeof( null ) );
    }

    @Test
    public void testSizeofPrimitives()
    {
        assertEquals( 24L, sizeof.sizeof( 3 ) );
        assertEquals( 28L, sizeof.sizeof( 3L ) );
        assertEquals( 28L, sizeof.sizeof( 3d ) );
    }

    @Test
    public void testSizeofPrimitiveArray()
    {
        assertEquals( 40L, sizeof.sizeof( new byte[20] ) );
    }

    @Test
    public void testSizeofFixedArray()
    {
        // sizeof(Integer) = 24 * 10 for each Integer object
        // + 20 for array object header
        // + 10 * 4 for the reference array itself
        assertEquals( 24L * 10 + 20L + 10 * 4L, sizeof.sizeof( new Integer[10] ) );
    }

    @Test
    public void testSizeofString()
    {
        // base costs are: 20 + 20 + 8 = 48
        assertEquals( 64L, sizeof.sizeof( "hello world!" ) );
        assertEquals( 58L, sizeof.sizeof( "hello!" ) );
    }

    @Test
    public void testSizeofRecord()
    {
        // 20 object header of BeanA
        // + 4 int
        // + 8 long
        // + 4 ref => + 24 Integer
        assertEquals( 60L, sizeof.sizeof( new PrimitiveAndWrapperBean() ) );
    }

    @Test
    public void testSizeofList()
    {
        // just the object header
        assertEquals( 20L, sizeof.sizeof( emptyList() ) );
        // dynamic list as we do not have a generic type
        // 20 object header wrapper
        // 20 object header size
        // + 3 * 24 Integer objects
        // + 3 * 8 for references and list structure
        assertEquals( 116, sizeof.sizeof( asList( 1, 2, 3 ) ) );
    }

    @Test
    public void testSizeofListFields()
    {
        // 20 object header + 4 + 4 for ref fields
        assertEquals( 28L, sizeof.sizeof( new CollectionBean() ) );
        // 20 object header + 4 + 4 ref fields
        // + 20 object header of the list
        // + 3 * 24 Integer's + 3 * 8 for list structure
        // + 20 object header empty list
        assertEquals( 164L, sizeof.sizeof( new CollectionBean( asList( 1, 2, 3 ), emptyList() ) ) );
    }

    @Test
    public void testSizeofPeriodType()
    {
        for ( PeriodType t : PeriodType.PERIOD_TYPES )
        {
            assertTrue( sizeof.sizeof( t ) > 0L );
        }
    }
}
