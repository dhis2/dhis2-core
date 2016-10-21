package org.hisp.dhis.webapi.utils;
/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.common.cache.Cacheability;
import org.hisp.dhis.setting.SettingKey;
import org.hisp.dhis.setting.SystemSettingManager;
import org.hisp.dhis.system.util.DateUtils;
import org.hisp.dhis.webapi.DhisWebSpringTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Stian Sandvold
 */
public class ContextUtilsTest
    extends DhisWebSpringTest
{

    @Autowired
    ContextUtils contextUtils;

    @Autowired
    SystemSettingManager systemSettingManager;

    HttpServletResponse response;

    @Before
    public void init()
    {
        response = new MockHttpServletResponse();
    }

    @Test
    public void testConfigureResponseReturnsCorrectTypeAndNumberOfHeaders()
    {
        contextUtils.configureResponse( response, null, CacheStrategy.NO_CACHE, null, false );
        String cacheControl = response.getHeader( "Cache-Control" );

        // Make sure we just have 1 header: Cache-Control
        assertEquals( 1, response.getHeaderNames().size() );
        assertNotNull( cacheControl );
    }

    @Test
    public void testConfigureResponseReturnsCorrectHeaderValueForAllCacheStrategies()
    {
        contextUtils.configureResponse( response, null, CacheStrategy.NO_CACHE, null, false );
        assertEquals( "no-cache", response.getHeader( "Cache-Control" ) );

        response.reset();
        contextUtils.configureResponse( response, null, CacheStrategy.CACHE_1_HOUR, null, false );
        assertEquals( "max-age=3600, public", response.getHeader( "Cache-Control" ) );

        response.reset();
        contextUtils.configureResponse( response, null, CacheStrategy.CACHE_15_MINUTES, null, false );
        assertEquals( "max-age=900, public", response.getHeader( "Cache-Control" ) );

        response.reset();
        contextUtils.configureResponse( response, null, CacheStrategy.CACHE_TWO_WEEKS, null, false );
        assertEquals( "max-age=1209600, public", response.getHeader( "Cache-Control" ) );

        long seconds = DateUtils.getSecondsUntilTomorrow( 6 );

        response.reset();
        contextUtils.configureResponse( response, null, CacheStrategy.CACHE_6AM_TOMORROW, null, false );
        assertEquals( "max-age=" + seconds + ", public", response.getHeader( "Cache-Control" ) );

        systemSettingManager.saveSystemSetting( SettingKey.CACHE_STRATEGY, CacheStrategy.CACHE_1_HOUR.toString() );

        response.reset();
        contextUtils.configureResponse( response, null, CacheStrategy.RESPECT_SYSTEM_SETTING, null, false );
        assertEquals( "max-age=3600, public", response.getHeader( "Cache-Control" ) );
    }

    @Test
    public void testConfigureResponseReturnsCorrectCacheabilityInHeader()
    {
        // Set to public; is default
        systemSettingManager.saveSystemSetting( SettingKey.CACHEABILITY, Cacheability.PUBLIC );

        contextUtils.configureResponse( response, null, CacheStrategy.CACHE_1_HOUR, null, false );
        assertEquals( "max-age=3600, public", response.getHeader( "Cache-Control" ) );

        // Set to private
        systemSettingManager.saveSystemSetting( SettingKey.CACHEABILITY, Cacheability.PRIVATE );

        response.reset();
        contextUtils.configureResponse( response, null, CacheStrategy.CACHE_1_HOUR, null, false );
        assertEquals( "max-age=3600, private", response.getHeader( "Cache-Control" ) );
    }
}
