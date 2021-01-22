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
package org.hisp.dhis.analytics.cache;

import static java.lang.Long.valueOf;
import static java.util.Calendar.DATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.FIXED;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.PROGRESSIVE;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_10_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_1_MINUTE;
import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TTL_MODE;
import static org.hisp.dhis.setting.SettingKey.CACHE_STRATEGY;
import static org.hisp.dhis.util.DateUtils.calculateDateFrom;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;

import java.util.Date;

import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;

public class AnalyticsCacheSettingsTest
{

    @Mock
    private SystemSettingManager systemSettingManager;

    @Rule
    public MockitoRule mockitoRule = rule();

    private AnalyticsCacheSettings analyticsCacheSettings;

    @Before
    public void setUp()
    {
        analyticsCacheSettings = new AnalyticsCacheSettings( systemSettingManager );
    }

    @Test
    public void testWhenProgressiveCachingIsEnabled()
    {
        // Given
        final AnalyticsCacheTtlMode progressiveCacheTtlMode = PROGRESSIVE;
        final CacheStrategy anyTimeoutStrategy = CACHE_1_MINUTE;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TTL_MODE ) ).thenReturn( progressiveCacheTtlMode );
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( anyTimeoutStrategy );
        final boolean progressiveCacheFlag = analyticsCacheSettings.isProgressiveCachingEnabled();

        // Then
        assertThat( progressiveCacheFlag, is( true ) );
    }

    @Test
    public void testWhenFixedCachingIsEnabled()
    {
        // Given
        final AnalyticsCacheTtlMode fixedCacheTtlMode = FIXED;
        final CacheStrategy anyTimeoutStrategy = CACHE_1_MINUTE;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TTL_MODE ) ).thenReturn( fixedCacheTtlMode );
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( anyTimeoutStrategy );
        final boolean fixedCacheFlag = analyticsCacheSettings.isFixedCachingEnabled();

        // Then
        assertThat( fixedCacheFlag, is( true ) );
    }

    @Test
    public void testWhenProgressiveCachingIsEnabledButStrategyIsNoCache()
    {
        // Given
        final AnalyticsCacheTtlMode progressiveCacheTtlMode = PROGRESSIVE;
        final CacheStrategy anyTimeoutStrategy = NO_CACHE;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TTL_MODE ) ).thenReturn( progressiveCacheTtlMode );
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( anyTimeoutStrategy );
        final boolean progressiveCacheFlag = analyticsCacheSettings.isProgressiveCachingEnabled();

        // Then
        assertThat( progressiveCacheFlag, is( true ) );
    }

    @Test
    public void testWhenFixedCachingIsEnabledButStrategyIsNoCache()
    {
        // Given
        final AnalyticsCacheTtlMode fixedCacheTtlMode = FIXED;
        final CacheStrategy anyTimeoutStrategy = NO_CACHE;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TTL_MODE ) ).thenReturn( fixedCacheTtlMode );
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( anyTimeoutStrategy );
        final boolean fixedCacheFlag = analyticsCacheSettings.isFixedCachingEnabled();

        // Then
        assertThat( fixedCacheFlag, is( false ) );
    }

    @Test
    public void testProgressiveExpirationTimeOrDefaultWhenTheTtlFactorIsSet()
    {
        // Given
        final int aTtlFactor = 20;
        final int oneDayDiff = 1;
        final long theExpectedTtl = aTtlFactor * oneDayDiff; // See
                                                             // TimeToLive.compute()
        final Date aDateBeforeToday = calculateDateFrom( new Date(), minus( oneDayDiff ), DATE );

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR ) )
            .thenReturn( aTtlFactor );
        final long expirationTime = analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDateBeforeToday );

        // Then
        assertThat( expirationTime, is( theExpectedTtl ) );
    }

    @Test
    public void testProgressiveExpirationTimeOrDefaultWhenTheTtlFactorIsNotSet()
    {
        // Given
        final int theDefaultTtlFactor = (Integer) ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR.getDefaultValue();
        final int oneDayDiff = 1;
        final long theExpectedTtl = theDefaultTtlFactor * oneDayDiff; // See
                                                                      // TimeToLive.compute()
        final Date aDateBeforeToday = calculateDateFrom( new Date(), minus( oneDayDiff ), DATE );

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR ) )
            .thenReturn( theDefaultTtlFactor );
        final long expirationTime = analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDateBeforeToday );

        // Then
        assertThat( expirationTime, is( theExpectedTtl ) );
    }

    @Test
    public void testProgressiveExpirationTimeOrDefaultWhenTheTtlFactorIsSetWithNegativeNumber()
    {
        // Given
        final int aTtlFactor = -20;
        final int oneDayDiff = 1;
        final int theExpectedTtlFactor = 1;
        final Date aDateBeforeToday = calculateDateFrom( new Date(), minus( oneDayDiff ), DATE );

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR ) )
            .thenReturn( aTtlFactor );
        final long expirationTime = analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDateBeforeToday );

        // Then
        assertThat( expirationTime, is( valueOf( theExpectedTtlFactor ) ) );
    }

    @Test
    public void testWhenFixedExpirationTimeOrDefaultIsSet()
    {
        // Given
        final CacheStrategy aCacheStrategy = CACHE_10_MINUTES;

        // When
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( aCacheStrategy );
        final long expirationTime = analyticsCacheSettings.fixedExpirationTimeOrDefault();

        // Then
        assertThat( expirationTime, is( valueOf( CACHE_10_MINUTES.toSeconds() ) ) );
    }

    @Test
    public void testWhenFixedExpirationTimeOrDefaultIsNotCache()
    {
        // Given
        final CacheStrategy aCacheStrategy = NO_CACHE;
        final long theExpectedExpirationTime = ((CacheStrategy) CACHE_STRATEGY.getDefaultValue()).toSeconds();

        // When
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( aCacheStrategy );
        final long expirationTime = analyticsCacheSettings.fixedExpirationTimeOrDefault();

        // Then
        assertThat( expirationTime, is( theExpectedExpirationTime ) );
    }

    @Test
    public void testIsCachingEnabledWhenFixedExpirationTimeIsSet()
    {
        // Given
        final AnalyticsCacheTtlMode fixedCacheTtlMode = FIXED;
        final CacheStrategy aCacheStrategy = CACHE_10_MINUTES;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TTL_MODE ) ).thenReturn( fixedCacheTtlMode );
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( aCacheStrategy );
        final boolean actualReturn = analyticsCacheSettings.isCachingEnabled();

        // Then
        assertThat( actualReturn, is( true ) );
    }

    @Test
    public void testIsCachingEnabledWhenProgressiveExpirationTimeIsSet()
    {
        // Given
        final AnalyticsCacheTtlMode progressiveCacheTtlMode = PROGRESSIVE;
        final CacheStrategy anyCacheStrategyButNoCache = CACHE_10_MINUTES;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TTL_MODE ) ).thenReturn( progressiveCacheTtlMode );
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( anyCacheStrategyButNoCache );
        final boolean actualReturn = analyticsCacheSettings.isCachingEnabled();

        // Then
        assertThat( actualReturn, is( true ) );
    }

    @Test
    public void testIsCachingEnabledWhenFixedExpirationTimeIsSetAndStrategyIsNoCache()
    {
        // Given
        final AnalyticsCacheTtlMode fixedCacheTtlMode = FIXED;
        final CacheStrategy cacheStrategyNoCache = NO_CACHE;

        // When
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TTL_MODE ) ).thenReturn( fixedCacheTtlMode );
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY ) ).thenReturn( cacheStrategyNoCache );
        final boolean actualReturn = analyticsCacheSettings.isCachingEnabled();

        // Then
        assertThat( actualReturn, is( false ) );
    }

    private int minus( final int value )
    {
        return -value;
    }
}
