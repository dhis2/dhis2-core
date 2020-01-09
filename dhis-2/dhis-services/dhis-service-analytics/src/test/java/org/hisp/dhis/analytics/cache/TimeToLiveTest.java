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

import static java.util.Calendar.DATE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.analytics.cache.TimeToLive.DEFAULT_MULTIPLIER;
import static org.hisp.dhis.analytics.cache.TimeToLive.DEFAULT_TTL_FACTOR;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_FACTOR;
import static org.hisp.dhis.util.DateUtils.calculateDateFrom;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.setting.DefaultSystemSettingManager;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TimeToLiveTest
{

    @Mock
    private DefaultSystemSettingManager systemSettingManager;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void testComputeForCurrentDayWhenCacheFactorIsNull()
    {
        // Given
        final Integer aNullCachingFactor = null;
        final Date endingDate = new Date();
        final Date beginningDate = new Date();
        final DataQueryParams dataQueryParams = stubbedParams( beginningDate, endingDate );
        final long expectedTtl = DEFAULT_TTL_FACTOR;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_FACTOR ) )
            .thenReturn( aNullCachingFactor );
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForCurrentDayWhenCacheFactorIsNegative()
    {
        // Given
        final Integer aNegativeCachingFactor = -1;
        final Date endingDate = new Date();
        final Date beginningDate = new Date();
        final DataQueryParams dataQueryParams = stubbedParams( beginningDate, endingDate );
        final long expectedTtl = DEFAULT_TTL_FACTOR;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_FACTOR ) )
            .thenReturn( aNegativeCachingFactor );
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForZeroDayDiffWhenCacheFactorIsPositive()
    {
        // Given
        final Integer aPositiveCachingFactor = 3;
        final Date endingDate = new Date();
        final Date beginningDate = endingDate;
        final DataQueryParams dataQueryParams = stubbedParams( beginningDate, endingDate );
        final long expectedTtl = DEFAULT_MULTIPLIER * aPositiveCachingFactor;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_FACTOR ) )
            .thenReturn( aPositiveCachingFactor );
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForOneDayBeforeWhenCacheFactorIsPositive()
    {
        // Given
        final int oneDayDiff = 1;
        final Integer aPositiveCachingFactor = 2;
        final Date endingDate = new Date();
        final Date beginningDate = calculateDateFrom( endingDate, minus( oneDayDiff ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( beginningDate, endingDate );
        final long expectedTtl = aPositiveCachingFactor * oneDayDiff;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_FACTOR ) )
            .thenReturn( aPositiveCachingFactor );
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeEndingDateIsAheadOfNowAndCacheFactorIsPositive()
    {
        // Given
        final int tenDaysAhead = 10;
        final Integer aPositiveCachingFactor = 1;
        final Date beginningDate = new Date();
        final Date endingDate = calculateDateFrom( beginningDate, plus( tenDaysAhead ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( beginningDate, endingDate );
        final long expectedTtl = DEFAULT_MULTIPLIER * aPositiveCachingFactor;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_FACTOR ) )
            .thenReturn( aPositiveCachingFactor );
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeEndingDateIsTenDaysBeforeNowAndCacheFactorIsPositive()
    {
        // Given
        final int tenDays = 10;
        final Integer aPositiveCachingFactor = 2;
        final Date now = new Date();
        final Date beginningDate = calculateDateFrom( now, minus( tenDays ), DATE );
        final Date endingDate = calculateDateFrom( now, minus( tenDays ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( beginningDate, endingDate );
        final long expectedTtl = aPositiveCachingFactor * tenDays;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_FACTOR ) )
            .thenReturn( aPositiveCachingFactor );
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    private DataQueryParams stubbedParams( final Date beginningDate, final Date endingDate )
    {
        final DataQueryParams dataQueryParams = DataQueryParams.newBuilder().withStartDate( beginningDate )
            .withEndDate( endingDate ).withEarliestStartDateLatestEndDate().build();
        return dataQueryParams;
    }

    private int minus( final int value )
    {
        return -value;
    }

    private int plus( final int value )
    {
        return value;
    }
}
