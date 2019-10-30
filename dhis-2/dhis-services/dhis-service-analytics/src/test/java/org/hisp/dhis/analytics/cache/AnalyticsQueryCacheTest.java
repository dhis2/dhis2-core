/*
 * Copyright (c) 2004-2019, University of Oslo
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

package org.hisp.dhis.analytics.cache;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.hisp.dhis.analytics.util.StubOfSqlRowSet;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.support.rowset.SqlRowSet;

public class AnalyticsQueryCacheTest
{
    private static AnalyticsQueryCache analyticsQueryCache;

    @BeforeClass
    public static void setUp()
    {
        analyticsQueryCache = new AnalyticsQueryCache();
        analyticsQueryCache.usingTimeToLiveInMillis();
    }

    @Test
    public void testPutAndGetWithSuccess()
    {
        // Given
        final Key anyCacheKey = new CacheKeyBuilder().build( "anyKey" );
        final SqlRowSet anySqlRowSet = new StubOfSqlRowSet();
        final long aTtlOfOneSecond = (1) + 1000;

        // When
        analyticsQueryCache.put( anyCacheKey, anySqlRowSet, aTtlOfOneSecond );

        // Then
        final SqlRowSet sqlRowSetExpected = analyticsQueryCache.get( anyCacheKey );
        assertThat( anySqlRowSet, is( equalTo( sqlRowSetExpected ) ) );
    }

    @Test
    public void testGetWhenTheKeyIsNotCached()
    {
        // Given
        final Key anyCacheKey = new CacheKeyBuilder().build( "anyKey" );

        // When
        final SqlRowSet sqlRowSetNotCached = analyticsQueryCache.get( anyCacheKey );

        // Then
        assertThat( null, is( equalTo( sqlRowSetNotCached ) ) );
    }

    @Test
    public void testGetExpiredKey()
    {
        // Given
        final Key anyCacheKey = new CacheKeyBuilder().build( "anyKey" );
        final SqlRowSet anySqlRowSet = new StubOfSqlRowSet();
        final long aTtlOfOneMillisecond = 1;

        // When
        analyticsQueryCache.put( anyCacheKey, anySqlRowSet, aTtlOfOneMillisecond );
        waitForExpiration( aTtlOfOneMillisecond );

        // Then
        final SqlRowSet sqlRowSetExpected = analyticsQueryCache.get( anyCacheKey );
        assertThat( null, is( equalTo( sqlRowSetExpected ) ) );
    }

    private void waitForExpiration( final long millisecondsToWait )
    {
        try
        {
            Thread.sleep( millisecondsToWait );
        }
        catch ( InterruptedException e )
        {

        }
    }
}
