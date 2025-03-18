/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
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
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_1_HOUR;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_1_MINUTE;
import static org.hisp.dhis.common.cache.CacheStrategy.CACHE_6AM_TOMORROW;
import static org.hisp.dhis.common.cache.CacheStrategy.NO_CACHE;
import static org.hisp.dhis.common.cache.CacheStrategy.RESPECT_SYSTEM_SETTING;
import static org.hisp.dhis.common.cache.Cacheability.PRIVATE;
import static org.hisp.dhis.common.cache.Cacheability.PUBLIC;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.test.utils.Assertions.assertWithinRange;
import static org.hisp.dhis.webapi.utils.ContextUtils.getAttachmentFileName;
import static org.hisp.dhis.webapi.utils.ContextUtils.stripFormatCompressionExtension;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Calendar;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.analytics.DataQueryParams;
import org.hisp.dhis.common.cache.CacheStrategy;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.test.webapi.WebSpringTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Stian Sandvold
 */
class ContextUtilsTest extends WebSpringTestBase {
  @Autowired private ContextUtils contextUtils;

  @Autowired private SystemSettingsService settingsService;

  private HttpServletResponse response;

  @BeforeEach
  void init() {
    response = new MockHttpServletResponse();
  }

  @AfterEach
  void afterEach() {
    response.reset();
  }

  @Test
  void testConfigureResponseReturnsCorrectTypeAndNumberOfHeaders() {
    contextUtils.configureResponse(response, null, NO_CACHE, null, false);
    String cacheControl = response.getHeader("Cache-Control");
    // Make sure we just have 1 header: Cache-Control
    assertEquals(1, response.getHeaderNames().size());
    assertNotNull(cacheControl);
  }

  @Test
  void testConfigureResponseReturnsConfiguredNoCacheStrategy() {
    contextUtils.configureResponse(response, null, NO_CACHE, null, false);

    assertEquals("no-cache", response.getHeader("Cache-Control"));
  }

  @Test
  void testConfigureResponseReturnsConfiguredCacheStrategy() {
    contextUtils.configureResponse(response, null, CACHE_1_MINUTE, null, false);

    assertEquals("max-age=60, public", response.getHeader("Cache-Control"));
  }

  @Test
  void testConfigureResponseReturnsConfiguredCacheStrategy6AMTomorrow() {
    contextUtils.configureResponse(response, null, CACHE_6AM_TOMORROW, null, false);

    String header = response.getHeader("Cache-Control");
    assertAll(
        () -> assertStartsWith("max-age", header),
        () -> {
          long maxAgeSeconds = Long.parseLong(StringUtils.getDigits(header));
          Long expected = CACHE_6AM_TOMORROW.toSeconds();
          long delta = 60; // 1 minute
          assertWithinRange(expected - delta, expected + delta, maxAgeSeconds);
        });
  }

  @Test
  void testConfigureResponseRespectsCacheStrategyInSystemSetting() {
    settingsService.put("keyCacheStrategy", CACHE_1_HOUR);
    settingsService.clearCurrentSettings();

    contextUtils.configureResponse(response, null, RESPECT_SYSTEM_SETTING, null, false);

    assertEquals("max-age=3600, public", response.getHeader("Cache-Control"));
  }

  @Test
  void testConfigureResponseReturnsCorrectCacheabilityInHeader() {
    // Set to public; is default
    settingsService.put("keyCacheability", PUBLIC);
    contextUtils.configureResponse(response, null, CACHE_1_HOUR, null, false);
    assertEquals("max-age=3600, public", response.getHeader("Cache-Control"));
    // Set to private
    settingsService.put("keyCacheability", PRIVATE);
    settingsService.clearCurrentSettings();
    response.reset();
    contextUtils.configureResponse(response, null, CACHE_1_HOUR, null, false);
    assertEquals("max-age=3600, private", response.getHeader("Cache-Control"));
  }

  @Test
  void testConfigureAnalyticsResponseWhenProgressiveIsDisabled() {
    Calendar dateBeforeToday = getInstance();
    dateBeforeToday.add(YEAR, -5);
    DataQueryParams params = newBuilder().withEndDate(dateBeforeToday.getTime()).build();
    // Progressive caching is not enabled
    settingsService.put("keyAnalyticsCacheTtlMode", FIXED);
    response.reset();
    contextUtils.configureAnalyticsResponse(
        response, null, CACHE_1_HOUR, null, false, params.getLatestEndDate());
    assertEquals("max-age=3600, public", response.getHeader("Cache-Control"));
  }

  @Test
  void testConfigureAnalyticsResponseWhenProgressiveIsEnabledAndCacheStrategyIsOverridden() {
    // Cache strategy overridden
    CacheStrategy overriddenCacheStrategy = CACHE_1_HOUR;
    Calendar dateBeforeToday = getInstance();
    dateBeforeToday.add(YEAR, -5);
    DataQueryParams params = newBuilder().withEndDate(dateBeforeToday.getTime()).build();
    // Progressive caching is not enabled
    settingsService.put("keyAnalyticsCacheTtlMode", PROGRESSIVE);
    settingsService.put("keyAnalyticsCacheProgressiveTtlFactor", 10);
    response.reset();
    contextUtils.configureAnalyticsResponse(
        response, null, overriddenCacheStrategy, null, false, params.getLatestEndDate());
    assertEquals("max-age=3600, public", response.getHeader("Cache-Control"));
  }

  @Test
  void
      testConfigureAnalyticsResponseWhenProgressiveIsEnabledAndCacheStrategyIsRespectSystemSetting() {
    Calendar dateBeforeToday = getInstance();
    dateBeforeToday.add(YEAR, -5);
    // Cache strategy set to respect system settings
    CacheStrategy respectSystemSetting = RESPECT_SYSTEM_SETTING;
    // Defined TTL Factor
    int ttlFactor = 10;
    // Expected timeToLive. See {@link TimeToLive.compute()}
    long timeToLive = DAYS.between(dateBeforeToday.toInstant(), now()) * ttlFactor;
    DataQueryParams params = newBuilder().withEndDate(dateBeforeToday.getTime()).build();
    // Progressive caching is not enabled
    settingsService.put("keyAnalyticsCacheTtlMode", PROGRESSIVE);
    settingsService.put("keyAnalyticsCacheProgressiveTtlFactor", ttlFactor);
    settingsService.clearCurrentSettings();
    response.reset();
    contextUtils.configureAnalyticsResponse(
        response, null, respectSystemSetting, null, false, params.getLatestEndDate());
    assertEquals("max-age=" + timeToLive + ", public", response.getHeader("Cache-Control"));
  }

  @Test
  void testGetAttachmentFileNameNull() {
    assertNull(getAttachmentFileName(null));
  }

  @Test
  void testGetAttachmentFileNameInline() {
    assertNull(getAttachmentFileName("inline; filename=test.txt"));
  }

  @Test
  void testGetAttachmentFileName() {
    assertEquals("test.txt", getAttachmentFileName("attachment; filename=test.txt"));
  }

  @Test
  void testStripFormatCompressionExtension() {
    assertEquals("data", stripFormatCompressionExtension("data.xml.zip", "xml", "zip"));
    assertEquals("data", stripFormatCompressionExtension("data.json.zip", "json", "zip"));
    assertEquals("data.json", stripFormatCompressionExtension("data.json.json.zip", "json", "zip"));
    assertEquals("data.json", stripFormatCompressionExtension("data.json.json.gz", "json", "gz"));
    assertEquals("data", stripFormatCompressionExtension("data.json.gz", "json", "gz"));
    assertEquals("data.....", stripFormatCompressionExtension("data.....", "xml", "zip"));
    assertEquals("", stripFormatCompressionExtension(null, "xml", "zip"));
  }
}
