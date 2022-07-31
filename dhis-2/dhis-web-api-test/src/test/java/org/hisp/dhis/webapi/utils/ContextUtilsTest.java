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
package org.hisp.dhis.webapi.utils;

import static java.time.ZonedDateTime.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Calendar.YEAR;
import static java.util.Calendar.getInstance;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.FIXED;
import static org.hisp.dhis.analytics.AnalyticsCacheTtlMode.PROGRESSIVE;
import static org.hisp.dhis.analytics.DataQueryParams.newBuilder;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_10_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_15_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_1_HOUR;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_1_MINUTE;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_30_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_5_MINUTES;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_6AM_TOMORROW;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_TWO_WEEKS;
import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.common.cache.Cacheability.PRIVATE;
import static org.hisp.dhis.common.cache.Cacheability.PUBLIC;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR;
import static org.hisp.dhis.setting.SettingKey.ANALYTICS_CACHE_TTL_MODE;
import static org.hisp.dhis.setting.SettingKey.CACHEABILITY;
import static org.hisp.dhis.setting.SettingKey.CACHE_STRATEGY;
import static org.hisp.dhis.setting.SettingKey.getAsRealClass;
import static org.hisp.dhis.webapi.utils.ContextUtils.getAttachmentFileName;
import static org.hisp.dhis.webapi.utils.ContextUtils.stripFormatCompressionExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Calendar;

import javax.servlet.http.HttpServletResponse;

import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Stian Sandvold
 */
class ContextUtilsTest extends DhisWebSpringTest
{

    @Autowired
    private ContextUtils contextUtils;

    @Autowired
    private SystemSettingManager systemSettingManager;

    private HttpServletResponse response;

    @BeforeEach
    void init()
    {
        response = new MockHttpServletResponse();
    }

    @Test
    void testConfigureResponseReturnsCorrectTypeAndNumberOfHeaders()
    {
        contextUtils.configureResponse( response, null, NO_CACHE, null, false );
        String cacheControl = response.getHeader( "Cache-Control" );
        // Make sure we just have 1 header: Cache-Control
        assertEquals( 1, response.getHeaderNames().size() );
        assertNotNull( cacheControl );
    }

    @Test
    void testConfigureResponseReturnsCorrectHeaderValueForAllCacheStrategies()
    {
        contextUtils.configureResponse( response, null, NO_CACHE, null, false );
        assertEquals( "no-cache", response.getHeader( "Cache-Control" ) );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_1_MINUTE, null, false );
        assertEquals( "max-age=60, public", response.getHeader( "Cache-Control" ) );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_5_MINUTES, null, false );
        assertEquals( "max-age=300, public", response.getHeader( "Cache-Control" ) );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_10_MINUTES, null, false );
        assertEquals( "max-age=600, public", response.getHeader( "Cache-Control" ) );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_15_MINUTES, null, false );
        assertEquals( "max-age=900, public", response.getHeader( "Cache-Control" ) );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_30_MINUTES, null, false );
        assertEquals( "max-age=1800, public", response.getHeader( "Cache-Control" ) );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_1_HOUR, null, false );
        assertEquals( "max-age=3600, public", response.getHeader( "Cache-Control" ) );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_TWO_WEEKS, null, false );
        assertEquals( "max-age=1209600, public", response.getHeader( "Cache-Control" ) );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_6AM_TOMORROW, null, false );
        assertEquals( "max-age=" + CACHE_6AM_TOMORROW.toSeconds() + ", public", response.getHeader( "Cache-Control" ) );
        response.reset();
        systemSettingManager.saveSystemSetting( CACHE_STRATEGY,
            getAsRealClass( CACHE_STRATEGY.getName(), CACHE_1_HOUR.toString() ) );
        contextUtils.configureResponse( response, null, RESPECT_SYSTEM_SETTING, null, false );
        assertEquals( "max-age=3600, public", response.getHeader( "Cache-Control" ) );
    }

    @Test
    void testConfigureResponseReturnsCorrectCacheabilityInHeader()
    {
        // Set to public; is default
        systemSettingManager.saveSystemSetting( CACHEABILITY, PUBLIC );
        contextUtils.configureResponse( response, null, CACHE_1_HOUR, null, false );
        assertEquals( "max-age=3600, public", response.getHeader( "Cache-Control" ) );
        // Set to private
        systemSettingManager.saveSystemSetting( CACHEABILITY, PRIVATE );
        response.reset();
        contextUtils.configureResponse( response, null, CACHE_1_HOUR, null, false );
        assertEquals( "max-age=3600, private", response.getHeader( "Cache-Control" ) );
    }

    @Test
    void testConfigureAnalyticsResponseWhenProgressiveIsDisabled()
    {
        Calendar dateBeforeToday = getInstance();
        dateBeforeToday.add( YEAR, -5 );
        DataQueryParams params = newBuilder().withEndDate( dateBeforeToday.getTime() ).build();
        // Progressive caching is not enabled
        systemSettingManager.saveSystemSetting( ANALYTICS_CACHE_TTL_MODE, FIXED );
        response.reset();
        contextUtils.configureAnalyticsResponse( response, null, CACHE_1_HOUR, null, false, params.getLatestEndDate() );
        assertEquals( "max-age=3600, public", response.getHeader( "Cache-Control" ) );
    }

    @Test
    void testConfigureAnalyticsResponseWhenProgressiveIsEnabledAndCacheStrategyIsOverridden()
    {
        // Cache strategy overridden
        CacheStrategy overriddenCacheStrategy = CACHE_1_HOUR;
        Calendar dateBeforeToday = getInstance();
        dateBeforeToday.add( YEAR, -5 );
        DataQueryParams params = newBuilder().withEndDate( dateBeforeToday.getTime() ).build();
        // Progressive caching is not enabled
        systemSettingManager.saveSystemSetting( ANALYTICS_CACHE_TTL_MODE, PROGRESSIVE );
        systemSettingManager.saveSystemSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR, 10 );
        response.reset();
        contextUtils.configureAnalyticsResponse( response, null, overriddenCacheStrategy, null, false,
            params.getLatestEndDate() );
        assertEquals( "max-age=3600, public", response.getHeader( "Cache-Control" ) );
    }

    @Test
    void testConfigureAnalyticsResponseWhenProgressiveIsEnabledAndCacheStrategyIsRespectSystemSetting()
    {
        Calendar dateBeforeToday = getInstance();
        dateBeforeToday.add( YEAR, -5 );
        // Cache strategy set to respect system settings
        CacheStrategy respectSystemSetting = RESPECT_SYSTEM_SETTING;
        // Defined TTL Factor
        int ttlFactor = 10;
        // Expected timeToLive. See {@link TimeToLive.compute()}
        long timeToLive = DAYS.between( dateBeforeToday.toInstant(), now() ) * ttlFactor;
        DataQueryParams params = newBuilder().withEndDate( dateBeforeToday.getTime() ).build();
        // Progressive caching is not enabled
        systemSettingManager.saveSystemSetting( ANALYTICS_CACHE_TTL_MODE, PROGRESSIVE );
        systemSettingManager.saveSystemSetting( ANALYTICS_CACHE_PROGRESSIVE_TTL_FACTOR, ttlFactor );
        response.reset();
        contextUtils.configureAnalyticsResponse( response, null, respectSystemSetting, null, false,
            params.getLatestEndDate() );
        assertEquals( "max-age=" + timeToLive + ", public", response.getHeader( "Cache-Control" ) );
    }

    @Test
    void testGetAttachmentFileNameNull()
    {
        assertNull( getAttachmentFileName( null ) );
    }

    @Test
    void testGetAttachmentFileNameInline()
    {
        assertNull( getAttachmentFileName( "inline; filename=test.txt" ) );
    }

    @Test
    void testGetAttachmentFileName()
    {
        assertEquals( "test.txt", getAttachmentFileName( "attachment; filename=test.txt" ) );
    }

    @Test
    void testStripFormatCompressionExtension()
    {
        assertEquals( "data", stripFormatCompressionExtension( "data.xml.zip", "xml", "zip" ) );
        assertEquals( "data", stripFormatCompressionExtension( "data.json.zip", "json", "zip" ) );
        assertEquals( "data.json", stripFormatCompressionExtension( "data.json.json.zip", "json", "zip" ) );
        assertEquals( "data.json", stripFormatCompressionExtension( "data.json.json.gz", "json", "gz" ) );
        assertEquals( "data", stripFormatCompressionExtension( "data.json.gz", "json", "gz" ) );
        assertEquals( "data.....", stripFormatCompressionExtension( "data.....", "xml", "zip" ) );
        assertEquals( "", stripFormatCompressionExtension( null, "xml", "zip" ) );
    }
}
