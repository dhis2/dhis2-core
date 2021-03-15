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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * A test for the {@link CappedLocalCache}.
 *
 * @author Jan Bernitt
 */
public class CappedLocalCacheTest
{
    private final Sizeof sizeof = new GenericSizeof( 20L, obj -> obj );

    private final CappedLocalCache cache = new CappedLocalCache( sizeof, 0 );

    private final Cache<String> testRegion = cache
        .createRegion( new SimpleCacheBuilder<String>()
            .forRegion( "test" )
            .expireAfterWrite( 1, TimeUnit.MINUTES )
            .forceInMemory() );

    @Test
    public void testSizeofCacheEntry()
    {
        // 20 object header of CacheEntry
        // + 4 ref region
        // + 4 ref key
        // + 4 ref value
        // + 8 created
        // + 8 expires
        // + 8 size
        // + 4 reads
        assertEquals( 60L, sizeof.sizeof( CappedLocalCache.EMPTY ) );
    }

    @Test
    public void testPut()
    {
        String value = "bar";
        testRegion.put( "foo", value );
        assertSame( value, testRegion.get( "foo" ).get() );
    }

    @Test
    public void testPutWithTTL()
    {
        String value = "bar";
        testRegion.put( "foo", value, 2000L );
        assertSame( value, testRegion.get( "foo" ).get() );
    }

    @Test
    public void testGetExpired()
    {
        testRegion.put( "foo", "bar", 0L );
        assertFalse( testRegion.get( "foo" ).isPresent() );
    }

    @Test
    public void testInvalidateKey()
    {
        testRegion.put( "x", "y" );
        testRegion.put( "a", "b" );
        testRegion.invalidate( "x" );
        assertFalse( testRegion.get( "x" ).isPresent() );
        assertTrue( testRegion.get( "a" ).isPresent() );
    }

    @Test
    public void testInvalidateAll()
    {
        testRegion.put( "x", "y" );
        testRegion.put( "a", "b" );
        testRegion.invalidateAll();
        assertFalse( testRegion.get( "x" ).isPresent() );
        assertFalse( testRegion.get( "a" ).isPresent() );
    }

    @Test
    public void testGetAll()
    {
        testRegion.put( "x", "y" );
        testRegion.put( "a", "b" );
        assertContainsOnly( testRegion.getAll(), "y", "b" );
    }
}
