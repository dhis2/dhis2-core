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
import static java.util.Calendar.YEAR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.analytics.cache.TimeToLive.DEFAULT_TO_30_SECONDS;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.MONTHLY;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.QUARTERLY;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.SIX_MONTHS;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.WEEKLY;
import static org.hisp.dhis.analytics.cache.TimeToLive.Periods.YEARLY;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_MONTHLY_PERIOD_IN_SECONDS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_QUARTERLY_PERIOD_IN_SECONDS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_SIX_MONTHS_PERIOD_IN_SECONDS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TIMEOUT_FACTOR_YEARLY_OR_OVER_PERIOD_IN_SECONDS;
import static org.hisp.dhis.util.DateUtils.calculateDateFrom;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.setting.DefaultSystemSettingManager;
import org.junit.Before;
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

    @Before
	public void setUp() {
	    // Setting up the default timeout for each period.
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS ) )
            .thenReturn( ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS.getDefaultValue() );
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TIMEOUT_FACTOR_MONTHLY_PERIOD_IN_SECONDS ) )
            .thenReturn( ANALYTICS_CACHE_TIMEOUT_FACTOR_MONTHLY_PERIOD_IN_SECONDS.getDefaultValue() );
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TIMEOUT_FACTOR_QUARTERLY_PERIOD_IN_SECONDS ) )
            .thenReturn( ANALYTICS_CACHE_TIMEOUT_FACTOR_QUARTERLY_PERIOD_IN_SECONDS.getDefaultValue() );
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TIMEOUT_FACTOR_SIX_MONTHS_PERIOD_IN_SECONDS ) )
            .thenReturn( ANALYTICS_CACHE_TIMEOUT_FACTOR_SIX_MONTHS_PERIOD_IN_SECONDS.getDefaultValue() );
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TIMEOUT_FACTOR_YEARLY_OR_OVER_PERIOD_IN_SECONDS ) )
            .thenReturn( ANALYTICS_CACHE_TIMEOUT_FACTOR_YEARLY_OR_OVER_PERIOD_IN_SECONDS.getDefaultValue() );
	}

    @Test
    public void testComputeForWeeklyPeriodDuringTheCurrentYearWhenCacheFactorIsNull()
    {
        // Given
        final Long aNullCachingFactor = null;
        final int aWeeklyPeriod = WEEKLY.value();
        final Date aMostRecentDate = new Date();
        final Date anOlderDate = calculateDateFrom( aMostRecentDate, minus( aWeeklyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aMostRecentDate, anOlderDate );
        final long expectedTtl = DEFAULT_TO_30_SECONDS;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS ) )
            .thenReturn( aNullCachingFactor );
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForWeeklyPeriodDuringTheCurrentYearWhenCacheFactorIsNegative()
    {
        // Given
        final Long aNegativeCachingFactor = -1l;
        final int aWeeklyPeriod = WEEKLY.value();
        final Date aMostRecentDate = new Date();
        final Date anOlderDate = calculateDateFrom( aMostRecentDate, minus( aWeeklyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aMostRecentDate, anOlderDate );
        final long expectedTtl = DEFAULT_TO_30_SECONDS;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS ) )
            .thenReturn( aNegativeCachingFactor );
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForWeeklyPeriodDuringTheCurrentYear()
    {
        // Given
        final int aWeeklyPeriod = WEEKLY.value();
        final Date aMostRecentDate = new Date();
        final Date anOlderDate = calculateDateFrom( aMostRecentDate, minus( aWeeklyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aMostRecentDate, anOlderDate );
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS.getDefaultValue();

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForWeeklyPeriodThreeYearsAgo()
    {
        // Given
        final int threeYearsAgo = 3;
        final int aWeeklyPeriod = WEEKLY.value();
        final Date aDateThreeYearsAgo = calculateDateFrom( new Date(), minus( threeYearsAgo ), YEAR );
        final Date anOlderDate = calculateDateFrom( aDateThreeYearsAgo, minus( aWeeklyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aDateThreeYearsAgo, anOlderDate );
        final int expectedMultiplier = threeYearsAgo + 1;
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_WEEKLY_PERIOD_IN_SECONDS.getDefaultValue()
            * expectedMultiplier;

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForMonthlyPeriodDuringTheCurrentYear()
    {
        // Given
        final int aMonthlyPeriod = MONTHLY.value();
        final Date aMostRecentDate = new Date();
        final Date anOlderDate = calculateDateFrom( aMostRecentDate, minus( aMonthlyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aMostRecentDate, anOlderDate );
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_MONTHLY_PERIOD_IN_SECONDS.getDefaultValue();

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForMonthlyPeriodInAPastYear()
    {
        // Given
        final int threeYearsAgo = 3;
        final int expectedMultiplier = threeYearsAgo + 1;
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_MONTHLY_PERIOD_IN_SECONDS.getDefaultValue()
            * expectedMultiplier;
        final int aWeeklyPeriod = MONTHLY.value();
        final Date aDateThreeYearsAgo = calculateDateFrom( new Date(), minus( threeYearsAgo ), YEAR );
        final Date anOlderDate = calculateDateFrom( aDateThreeYearsAgo, minus( aWeeklyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aDateThreeYearsAgo, anOlderDate );

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForQuarterlyPeriodDuringTheCurrentYear()
    {
        // Given
        final int aQuarterlyPeriod = QUARTERLY.value();
        final Date aMostRecentDate = new Date();
        final Date anOlderDate = calculateDateFrom( aMostRecentDate, minus( aQuarterlyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aMostRecentDate, anOlderDate );
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_QUARTERLY_PERIOD_IN_SECONDS.getDefaultValue();

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForQuarterlyPeriodInAPastYear()
    {
        // Given
        final int threeYearsAgo = 3;
        final int expectedMultiplier = threeYearsAgo + 1;
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_QUARTERLY_PERIOD_IN_SECONDS.getDefaultValue()
            * expectedMultiplier;
        final int aWeeklyPeriod = QUARTERLY.value();
        final Date aDateThreeYearsAgo = calculateDateFrom( new Date(), minus( threeYearsAgo ), YEAR );
        final Date anOlderDate = calculateDateFrom( aDateThreeYearsAgo, minus( aWeeklyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aDateThreeYearsAgo, anOlderDate );

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForSixMonthsPeriodDuringTheCurrentYear()
    {
        // Given
        final int aSixMonthsPeriod = SIX_MONTHS.value();
        final Date aMostRecentDate = new Date();
        final Date anOlderDate = calculateDateFrom( aMostRecentDate, minus( aSixMonthsPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aMostRecentDate, anOlderDate );
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_SIX_MONTHS_PERIOD_IN_SECONDS.getDefaultValue();

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForSixMonthsPeriodInAPastYear()
    {
        // Given
        final int threeYearsAgo = 3;
        final int expectedMultiplier = threeYearsAgo + 1;
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_SIX_MONTHS_PERIOD_IN_SECONDS.getDefaultValue()
            * expectedMultiplier;
        final int aWeeklyPeriod = SIX_MONTHS.value();
        final Date aDateThreeYearsAgo = calculateDateFrom( new Date(), minus( threeYearsAgo ), YEAR );
        final Date anOlderDate = calculateDateFrom( aDateThreeYearsAgo, minus( aWeeklyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aDateThreeYearsAgo, anOlderDate );

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForYearlyPeriodDuringTheCurrentYear()
    {
        // Given
        final int aYearlyPeriod = YEARLY.value();
        final Date aMostRecentDate = new Date();
        final Date anOlderDate = calculateDateFrom( aMostRecentDate, minus( aYearlyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aMostRecentDate, anOlderDate );
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_YEARLY_OR_OVER_PERIOD_IN_SECONDS.getDefaultValue();

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    @Test
    public void testComputeForYearlyPeriodInAPastYear()
    {
        // Given
        final int threeYearsAgo = 3;
        final int expectedMultiplier = threeYearsAgo + 1;
        final Long expectedTtl = (Long) ANALYTICS_CACHE_TIMEOUT_FACTOR_YEARLY_OR_OVER_PERIOD_IN_SECONDS.getDefaultValue()
            * expectedMultiplier;
        final int aWeeklyPeriod = YEARLY.value();
        final Date aDateThreeYearsAgo = calculateDateFrom( new Date(), minus( threeYearsAgo ), YEAR );
        final Date anOlderDate = calculateDateFrom( aDateThreeYearsAgo, minus( aWeeklyPeriod ), DATE );
        final DataQueryParams dataQueryParams = stubbedParams( aDateThreeYearsAgo, anOlderDate );

        // When
        final long actualTtl = new TimeToLive( dataQueryParams, systemSettingManager ).compute();

        // Then
        assertThat( actualTtl, is( equalTo( expectedTtl ) ) );
    }

    private DataQueryParams stubbedParams( final Date mostRecentDate, final Date oldestDate )
    {
        final DataQueryParams dataQueryParams = DataQueryParams.newBuilder().withStartDate( oldestDate )
            .withEndDate( mostRecentDate ).withEarliestStartDateLatestEndDate().build();
        return dataQueryParams;
    }

    private int minus( final int positiveValue )
    {
        return -positiveValue;
    }
}
