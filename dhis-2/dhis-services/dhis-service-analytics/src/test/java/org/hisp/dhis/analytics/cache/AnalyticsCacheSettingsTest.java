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
package org.hisp.dhis.analytics.cache;

import static java.util.Calendar.DATE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.FIXED;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.PROGRESSIVE;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_10_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_1_MINUTE;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_TWO_WEEKS;
import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TTL_MODE;
import static org.hisp.dhis.setting.SettingKey.CACHE_STRATEGY;
import static org.hisp.dhis.util.DateUtils.calculateDateFrom;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.hisp.dhis.analytics.AnalyticsCacheTtlMode;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings( strictness = Strictness.LENIENT )
@ExtendWith( MockitoExtension.class )
class AnalyticsCacheSettingsTest
{
    @Mock
    private SystemSettingManager systemSettingManager;

    private AnalyticsCacheSettings analyticsCacheSettings;

    @BeforeEach
    public void setUp()
    {
        analyticsCacheSettings = new AnalyticsCacheSettings( systemSettingManager );
    }

    @Test
    void testWhenProgressiveCachingIsEnabled()
    {
        given( PROGRESSIVE, CACHE_1_MINUTE );

        assertTrue( analyticsCacheSettings.isProgressiveCachingEnabled() );
    }

    @Test
    void testWhenFixedCachingIsEnabled()
    {
        given( FIXED, CACHE_1_MINUTE );

        assertTrue( analyticsCacheSettings.isFixedCachingEnabled() );
    }

    @Test
    void testWhenProgressiveCachingIsEnabledButStrategyIsNoCache()
    {
        given( PROGRESSIVE, NO_CACHE );

        assertTrue( analyticsCacheSettings.isProgressiveCachingEnabled() );
    }

    @Test
    void testWhenFixedCachingIsEnabledButStrategyIsNoCache()
    {
        given( FIXED, NO_CACHE );

        assertFalse( analyticsCacheSettings.isFixedCachingEnabled() );
    }

    @Test
    void testProgressiveExpirationTimeOrDefaultWhenTheTtlFactorIsSet()
    {
        // Given
        int aTtlFactor = 20;
        int oneDayDiff = 1;
        long theExpectedTtl = aTtlFactor * oneDayDiff; // See
                                                       // TimeToLive.compute()
        Date aDateBeforeToday = calculateDateFrom( new Date(), minus( oneDayDiff ), DATE );

        // When
        when( systemSettingManager.getIntegerSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR ) )
            .thenReturn( aTtlFactor );
        long expirationTime = analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDateBeforeToday );

        // Then
        assertThat( expirationTime, is( theExpectedTtl ) );
    }

    @Test
    void testProgressiveExpirationTimeOrDefaultWhenTheTtlFactorIsNotSet()
    {
        // Given
        int theDefaultTtlFactor = (Integer) ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR.getDefaultValue();
        int oneDayDiff = 1;
        long theExpectedTtl = theDefaultTtlFactor * oneDayDiff; // See
                                                                // TimeToLive.compute()
        Date aDateBeforeToday = calculateDateFrom( new Date(), minus( oneDayDiff ), DATE );

        // When
        when( systemSettingManager.getIntegerSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR ) )
            .thenReturn( theDefaultTtlFactor );
        long expirationTime = analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDateBeforeToday );

        // Then
        assertThat( expirationTime, is( theExpectedTtl ) );
    }

    @Test
    void testProgressiveExpirationTimeOrDefaultWhenTheTtlFactorIsSetWithNegativeNumber()
    {
        // Given
        int aTtlFactor = -20;
        int oneDayDiff = 1;
        Date aDateBeforeToday = calculateDateFrom( new Date(), minus( oneDayDiff ), DATE );

        // When
        when( systemSettingManager.getIntegerSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR ) )
            .thenReturn( aTtlFactor );

        // Then
        assertEquals( 1, analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDateBeforeToday ) );
    }

    @Test
    void testWhenFixedExpirationTimeOrDefaultIsSet()
    {
        given( CACHE_10_MINUTES );

        assertEquals( CACHE_TWO_WEEKS.toSeconds().longValue(), analyticsCacheSettings.fixedExpirationTimeOrDefault() );
    }

    @Test
    void testWhenFixedExpirationTimeOrDefaultIsNotCache()
    {
        given( NO_CACHE );

        assertEquals( 0L, analyticsCacheSettings.fixedExpirationTimeOrDefault() );
    }

    @Test
    void testIsCachingEnabledWhenFixedExpirationTimeIsSet()
    {
        given( FIXED, CACHE_10_MINUTES );

        assertTrue( analyticsCacheSettings.isCachingEnabled() );
    }

    @Test
    void testIsCachingEnabledWhenProgressiveExpirationTimeIsSet()
    {
        given( PROGRESSIVE, CACHE_10_MINUTES );

        assertTrue( analyticsCacheSettings.isCachingEnabled() );
    }

    @Test
    void testIsCachingEnabledWhenFixedExpirationTimeIsSetAndStrategyIsNoCache()
    {
        given( FIXED, NO_CACHE );

        assertFalse( analyticsCacheSettings.isCachingEnabled() );
    }

    private void given( CacheStrategy strategy )
    {
        given( null, strategy );
    }

    private void given( AnalyticsCacheTtlMode mode, CacheStrategy strategy )
    {
        when( systemSettingManager.getSystemSetting( ANALYTICS_CACHE_TTL_MODE, AnalyticsCacheTtlMode.class ) )
            .thenReturn( mode );
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY, CacheStrategy.class ) )
            .thenReturn( strategy );
    }

    private int minus( int value )
    {
        return -value;
    }
}
