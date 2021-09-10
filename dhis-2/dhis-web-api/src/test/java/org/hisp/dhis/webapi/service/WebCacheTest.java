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
package org.hisp.dhis.webapi.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_10_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_15_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_1_HOUR;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_1_MINUTE;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_30_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_5_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_TWO_WEEKS;
import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.common.cache.Cacheability.PRIVATE;
import static org.hisp.dhis.common.cache.Cacheability.PUBLIC;
import static org.hisp.dhis.setting.SettingKey.CACHEABILITY;
import static org.hisp.dhis.setting.SettingKey.CACHE_STRATEGY;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.junit.MockitoJUnit.rule;
import static org.springframework.http.CacheControl.maxAge;
import static org.springframework.http.CacheControl.noCache;

import java.util.Date;

import org.hisp.dhis.analytics.cache.AnalyticsCacheSettings;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheability;
import org.hisp.dhis.setting.SystemSettingManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoRule;
import org.springframework.http.CacheControl;

public class WebCacheTest
{

    @Mock
    private SystemSettingManager systemSettingManager;

    @Mock
    private AnalyticsCacheSettings analyticsCacheSettings;

    @Rule
    public MockitoRule mockitoRule = rule();

    private WebCache webCache;

    @Before
    public void setUp()
    {
        webCache = new WebCache( systemSettingManager, analyticsCacheSettings );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsNoCache()
    {
        // Given
        final CacheControl expectedCacheControl = noCache();

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( NO_CACHE );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsRespectSystemSetting()
    {
        // Given
        final CacheStrategy strategy = CACHE_5_MINUTES;
        final CacheControl expectedCacheControl = stubPublicCacheControl( strategy );
        givenCacheabilityPublic();
        givenCacheStartegy( strategy );

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( RESPECT_SYSTEM_SETTING );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetAnalyticsCacheControlForWhenTimeToLiveIsZero()
    {
        // Given
        final long zeroTimeToLive = 0;
        final Date aDate = new Date();
        final CacheControl expectedCacheControl = noCache();

        // When
        when( analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDate ) ).thenReturn( zeroTimeToLive );
        final CacheControl actualCacheControl = webCache.getCacheControlFor( aDate );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetAnalyticsCacheControlForWhenTimeToLiveIsNegative()
    {
        // Given
        final long zeroTimeToLive = -1;
        final Date aDate = new Date();
        final CacheControl expectedCacheControl = noCache();

        // When
        when( analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDate ) ).thenReturn( zeroTimeToLive );
        final CacheControl actualCacheControl = webCache.getCacheControlFor( aDate );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetAnalyticsCacheControlForWhenTimeToLiveIsPositive()
    {
        // Given
        final long positiveTimeToLive = 60;
        final Date aDate = new Date();
        final CacheControl expectedCacheControl = stubPublicCacheControl( positiveTimeToLive );
        givenCacheability( PUBLIC );
        when( analyticsCacheSettings.progressiveExpirationTimeOrDefault( aDate ) ).thenReturn( positiveTimeToLive );

        final CacheControl actualCacheControl = webCache.getCacheControlFor( aDate );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetCacheControlForWhenCacheStrategyIsRespectSystemSettingNotUsedInObjectBasis()
    {
        // Given
        givenCacheabilityPublic();
        givenCacheStartegy( RESPECT_SYSTEM_SETTING );

        // When
        webCache.getCacheControlFor( RESPECT_SYSTEM_SETTING );

        // Then
        fail( "Should not reach here. Exception was expected: " );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsCache1Minute()
    {
        // Given
        final CacheStrategy strategy = CACHE_1_MINUTE;
        final CacheControl expectedCacheControl = stubPublicCacheControl( strategy );
        givenCacheabilityPublic();

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( strategy );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsCache5Minutes()
    {
        // Given
        final CacheStrategy strategy = CACHE_5_MINUTES;
        final CacheControl expectedCacheControl = stubPublicCacheControl( strategy );
        givenCacheabilityPublic();

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( strategy );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsCache10Minutes()
    {
        // Given
        final CacheStrategy strategy = CACHE_10_MINUTES;
        final CacheControl expectedCacheControl = stubPublicCacheControl( strategy );
        givenCacheabilityPublic();

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( strategy );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsCache15Minutes()
    {
        // Given
        final CacheStrategy strategy = CACHE_15_MINUTES;
        final CacheControl expectedCacheControl = stubPublicCacheControl( strategy );
        givenCacheabilityPublic();

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( strategy );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsCache30Minutes()
    {
        // Given
        final CacheStrategy strategy = CACHE_30_MINUTES;
        final CacheControl expectedCacheControl = stubPublicCacheControl( strategy );
        givenCacheabilityPublic();

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( strategy );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsCache1Hour()
    {
        // Given
        final CacheStrategy strategy = CACHE_1_HOUR;
        final CacheControl expectedCacheControl = stubPublicCacheControl( strategy );
        givenCacheabilityPublic();

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( strategy );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testGetCacheControlForWhenCacheStrategyIsCache2Weeks()
    {
        // Given
        final CacheStrategy strategy = CACHE_TWO_WEEKS;
        final CacheControl expectedCacheControl = stubPublicCacheControl( strategy );
        givenCacheabilityPublic();

        // When
        final CacheControl actualCacheControl = webCache.getCacheControlFor( strategy );

        // Then
        assertThat( actualCacheControl.toString(), is( expectedCacheControl.toString() ) );
    }

    @Test
    public void testSetCacheabilityWhenCacheabilityIsSetToPublic()
    {
        // Given
        givenCacheability( PUBLIC );

        final CacheControl actualCacheControl = webCache.getCacheControlFor( CACHE_5_MINUTES );

        // Then
        assertThat( actualCacheControl.toString().toLowerCase(), containsString( "public" ) );
    }

    @Test
    public void testSetCacheabilityWhenCacheabilityIsSetToPrivate()
    {
        // Given
        givenCacheability( PRIVATE );

        final CacheControl actualCacheControl = webCache.getCacheControlFor( CACHE_5_MINUTES );

        // Then
        assertThat( actualCacheControl.toString().toLowerCase(), containsString( "private" ) );
    }

    @Test
    public void testSetCacheabilityWhenCacheabilityIsSetToNull()
    {
        // Given
        givenCacheability( null );

        final CacheControl actualCacheControl = webCache.getCacheControlFor( CACHE_5_MINUTES );

        // Then
        assertThat( actualCacheControl.toString().toLowerCase(), not( containsString( "private" ) ) );
        assertThat( actualCacheControl.toString().toLowerCase(), not( containsString( "public" ) ) );
    }

    private CacheControl stubPublicCacheControl( final CacheStrategy cacheStrategy )
    {
        return stubPublicCacheControl( cacheStrategy.toSeconds() );
    }

    private CacheControl stubPublicCacheControl( final long seconds )
    {
        return maxAge( seconds, SECONDS ).cachePublic();
    }

    private void givenCacheStartegy( CacheStrategy theCacheStrategySet )
    {
        when( systemSettingManager.getSystemSetting( CACHE_STRATEGY, CacheStrategy.class ) )
            .thenReturn( theCacheStrategySet );
    }

    private void givenCacheabilityPublic()
    {
        givenCacheability( PUBLIC );
    }

    private void givenCacheability( Cacheability cacheability )
    {
        when( systemSettingManager.getSystemSetting( CACHEABILITY, Cacheability.class ) )
            .thenReturn( cacheability );
    }
}
