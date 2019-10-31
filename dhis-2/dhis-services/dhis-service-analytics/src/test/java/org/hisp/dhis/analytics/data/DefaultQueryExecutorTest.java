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

package org.hisp.dhis.analytics.data;

import static java.util.Calendar.DATE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.util.DateUtils.calculateDateFrom;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.analytics.cache.AnalyticsQueryCache;
import org.hisp.dhis.analytics.cache.CacheKeyBuilder;
import org.hisp.dhis.analytics.cache.Key;
import org.hisp.dhis.analytics.cache.UserWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;

public class DefaultQueryExecutorTest
{

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CacheKeyBuilder keyBuilder;

    @Mock
    private AnalyticsQueryCache analyticsQueryCache;

    @Mock
    private UserWrapper userWrapper;

    @InjectMocks
    private DefaultQueryExecutor defaultQueryExecutor;

    @Before
    public void setUp()
    {
        initMocks( this );
    }

    @Test
    public void testFetchCachedKeyWithSuccess()
    {
        // Given
        final String anySqlQuery = "select a from b;";
        final String anyUserAuthorities = "auht1-auth2-auth3";
        final DataQueryParams stubbedParams = stubbedParamsForYearlyPeriod();
        final Key anyKey = new CacheKeyBuilder().build( anySqlQuery, anyUserAuthorities );
        final List<Map<String, Object>> anyResultList = new ArrayList<>();

        // When
        when( userWrapper.getUserAuthorityGroupsName() ).thenReturn( anyUserAuthorities );
        when( keyBuilder.build( anySqlQuery, anyUserAuthorities ) ).thenReturn( anyKey );
        when( analyticsQueryCache.get( anyKey ) ).thenReturn( anyResultList );
        final List<Map<String, Object>> expectedSqlRowSet = defaultQueryExecutor.fetch( anySqlQuery, stubbedParams );

        // Then
        assertThat( anyResultList, is( equalTo( expectedSqlRowSet ) ) );
    }

    @Test
    public void testFetchNonCachedKeyWithSuccess()
    {
        // Given
        final String anySqlQuery = "select a from b;";
        final String anyUserAuthorities = "auht1-auth2-auth3";
        final DataQueryParams stubbedParams = stubbedParamsForYearlyPeriod();
        final Key anyKey = new CacheKeyBuilder().build( anySqlQuery, anyUserAuthorities );
        final List<Map<String, Object>> nonExistingResultList = null;
        final List<Map<String, Object>> resultListFromDatabse = new ArrayList<>();
        final long yearlyTimeTiLive = 60; // See TimeToLive.Periods.YEARLY and TimeToLive.EXPIRATION_TIME_TABLE

        // When
        when( userWrapper.getUserAuthorityGroupsName() ).thenReturn( anyUserAuthorities );
        when( keyBuilder.build( anySqlQuery, anyUserAuthorities ) ).thenReturn( anyKey );
        when( analyticsQueryCache.get( anyKey ) ).thenReturn( nonExistingResultList );
        when( jdbcTemplate.queryForList( anySqlQuery ) ).thenReturn( resultListFromDatabse );
        final List<Map<String, Object>> expectedSqlRowSet = defaultQueryExecutor.fetch( anySqlQuery, stubbedParams );

        // Then
        assertThat( resultListFromDatabse, is( equalTo( expectedSqlRowSet ) ) );
        verify( jdbcTemplate, times( 1 ) ).queryForList( anySqlQuery );
        verify( analyticsQueryCache, times( 1 ) ).put( anyKey, resultListFromDatabse, yearlyTimeTiLive );
    }

    @Test
    public void forceFetch()
    {
    }

    private DataQueryParams stubbedParamsForYearlyPeriod()
    {
        final int aYearlyPeriod = 365; // days
        final Date aMostRecentDate = new Date();
        final Date anOlderDate = calculateDateFrom( aMostRecentDate, -aYearlyPeriod, DATE );
        final DataQueryParams dataQueryParams = DataQueryParams.newBuilder().withStartDate( anOlderDate )
            .withEndDate( aMostRecentDate ).withEarliestStartDateLatestEndDate().build();
        return dataQueryParams;
    }
}
