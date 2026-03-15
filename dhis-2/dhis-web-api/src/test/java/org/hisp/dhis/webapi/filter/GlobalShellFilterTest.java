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
package org.hisp.dhis.webapi.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.system.SystemService;
import org.hisp.dhis.webapi.staticresource.StaticCacheControlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for the service-worker dispatch logic in {@link GlobalShellFilter}.
 *
 * <p>When canonical app paths are enabled:
 *
 * <ul>
 *   <li>Root SW request ({@code /apps/service-worker.js}) → canonical-service-worker.js
 *   <li>Per-app SW request ({@code /apps/{name}/service-worker.js}) →
 *       unregistering-service-worker.js
 * </ul>
 *
 * <p>Uses a real {@link StaticCacheControlService} (with mocked dependencies) instead of a Mockito
 * mock, since Mockito's ByteBuddy-based inline mock maker cannot instrument concrete classes on
 * newer JDKs (22+). The service worker tests don't exercise {@code HtmlCacheBustingService}, so
 * {@code null} is passed safely.
 */
@ExtendWith(MockitoExtension.class)
class GlobalShellFilterTest {

  @Mock private AppManager appManager;
  @Mock private SystemSettingsProvider settingsProvider;
  @Mock private SystemSettings settings;
  @Mock private DhisConfigurationProvider dhisConfig;
  @Mock private SystemService systemService;
  @Mock private FilterChain filterChain;

  private StaticCacheControlService staticCacheControlService;
  private GlobalShellFilter filter;

  @BeforeEach
  void setUp() {
    lenient()
        .when(dhisConfig.isEnabled(ConfigurationKey.STATIC_CACHE_ENABLED))
        .thenReturn(true);
    lenient()
        .when(dhisConfig.isEnabled(ConfigurationKey.STATIC_CACHE_DEV_MODE_FORCE_NO_CACHE))
        .thenReturn(false);
    lenient()
        .when(dhisConfig.getProperty(ConfigurationKey.STATIC_CACHE_DEFAULT_MAX_AGE))
        .thenReturn("3600");
    lenient()
        .when(dhisConfig.getProperty(ConfigurationKey.STATIC_CACHE_HTML_MAX_AGE))
        .thenReturn("300");
    lenient()
        .when(dhisConfig.getProperty(ConfigurationKey.STATIC_CACHE_IMMUTABLE_MAX_AGE))
        .thenReturn("31536000");
    lenient()
        .when(dhisConfig.getProperty(ConfigurationKey.STATIC_CACHE_ALWAYS_NO_CACHE_PATTERNS))
        .thenReturn("**/*.html,**/index.*,**/manifest.*,**/config.*,**/plugin.html");

    staticCacheControlService =
        new StaticCacheControlService(dhisConfig, appManager, systemService);

    // HtmlCacheBustingService is not exercised in service worker tests — passing null is safe
    filter =
        new GlobalShellFilter(appManager, settingsProvider, staticCacheControlService, null);
    lenient().when(settingsProvider.getCurrentSettings()).thenReturn(settings);
    lenient().when(settings.getGlobalShellAppName()).thenReturn("global-shell");
  }

  // --- canonical paths ON ---

  @Test
  void rootSwRequest_canonicalOn_servesCanonicalServiceWorker() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    MockHttpServletRequest request = swRequest("/apps/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertEquals(200, response.getStatus());
    assertEquals("application/javascript", response.getContentType());
    assertEquals("/", response.getHeader("Service-Worker-Allowed"));
    assertNotNull(response.getHeader("ETag"), "Service worker response should include an ETag");

    String cacheControl = response.getHeader("Cache-Control");
    assertNotNull(cacheControl, "Should have Cache-Control header");
    assertFalse(cacheControl.contains("no-store"), "Service worker should be cacheable");
    assertTrue(cacheControl.contains("max-age=3600"), "Should use default max-age for JS");

    String body = response.getContentAsString();
    assertTrue(
        body.contains("SHELL_CACHE"), "Should contain canonical SW content (SHELL_CACHE constant)");
    assertFalse(body.contains("unregister"), "Root SW should NOT self-unregister");
  }

  @Test
  void perAppSwRequest_canonicalOn_servesUnregisteringSW() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    MockHttpServletRequest request = swRequest("/apps/maps/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertEquals(200, response.getStatus());
    assertEquals("application/javascript", response.getContentType());
    String body = response.getContentAsString();
    assertTrue(body.contains("unregister"), "Per-app SW must self-unregister");
    assertFalse(body.contains("SHELL_CACHE"), "Per-app SW must NOT contain canonical content");
  }

  @Test
  void deepPerAppSwRequest_canonicalOn_servesUnregisteringSW() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    MockHttpServletRequest request = swRequest("/apps/data-visualizer/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertEquals(200, response.getStatus());
    assertEquals("application/javascript", response.getContentType());
    String body = response.getContentAsString();
    assertTrue(body.contains("unregister"), "Per-app SW must self-unregister");
  }

  @Test
  void globalShellSwRequest_canonicalOn_servesCanonicalServiceWorker() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    MockHttpServletRequest request = swRequest("/apps/global-shell/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertEquals(200, response.getStatus());
    assertEquals("application/javascript", response.getContentType());

    String body = response.getContentAsString();
    assertTrue(body.contains("SHELL_CACHE"), "Global shell SW should get canonical content");
  }

  // --- conditional request (ETag / 304) ---

  @Test
  void swRequest_withMatchingETag_returns304() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    // First request to get the ETag
    MockHttpServletRequest request1 = swRequest("/apps/service-worker.js");
    MockHttpServletResponse response1 = new MockHttpServletResponse();
    filter.doFilter(request1, response1, filterChain);
    String etag = response1.getHeader("ETag");
    assertNotNull(etag, "First response must include an ETag");

    // Second request with If-None-Match
    MockHttpServletRequest request2 = swRequest("/apps/service-worker.js");
    request2.addHeader("If-None-Match", etag);
    MockHttpServletResponse response2 = new MockHttpServletResponse();
    filter.doFilter(request2, response2, filterChain);

    assertEquals(304, response2.getStatus());
    assertEquals(etag, response2.getHeader("ETag"));
    assertEquals(0, response2.getContentLength(), "304 response must have no body");
  }

  @Test
  void swRequest_withStaleETag_returns200() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    MockHttpServletRequest request = swRequest("/apps/service-worker.js");
    request.addHeader("If-None-Match", "\"stale-etag-value\"");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, filterChain);

    assertEquals(200, response.getStatus());
    assertTrue(
        response.getContentAsString().contains("SHELL_CACHE"),
        "Stale ETag should return full content");
  }

  @Test
  void swETag_isContentBased() throws Exception {
    byte[] canonicalSwBytes;
    try (InputStream stream =
        getClass().getClassLoader().getResourceAsStream("canonical-service-worker.js")) {
      canonicalSwBytes = stream.readAllBytes();
    }
    String expectedEtag = "\"" + HashUtils.hashMD5(canonicalSwBytes) + "\"";

    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    MockHttpServletRequest request = swRequest("/apps/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, filterChain);

    assertEquals(expectedEtag, response.getHeader("ETag"));
  }

  // --- canonical paths OFF ---

  @Test
  void rootSwRequest_canonicalOff_passesThrough() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(false);

    MockHttpServletRequest request = swRequest("/apps/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertFalse(
        "application/javascript".equals(response.getContentType()),
        "Should not serve a SW when canonical paths are off");
  }

  @Test
  void perAppSwRequest_canonicalOff_passesThrough() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(false);

    MockHttpServletRequest request = swRequest("/apps/maps/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertFalse(
        "application/javascript".equals(response.getContentType()),
        "Should not serve a SW when canonical paths are off");
  }

  // --- classpath resources exist ---

  @Test
  void canonicalServiceWorkerResourceExistsOnClasspath() {
    try (InputStream stream =
        getClass().getClassLoader().getResourceAsStream("canonical-service-worker.js")) {
      assertTrue(stream != null, "canonical-service-worker.js must be on the classpath");
      String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(content.contains("SHELL_CACHE"), "Canonical SW must define SHELL_CACHE");
    } catch (Exception e) {
      throw new AssertionError("Failed to read canonical-service-worker.js", e);
    }
  }

  @Test
  void unregisteringServiceWorkerResourceExistsOnClasspath() {
    try (InputStream stream =
        getClass().getClassLoader().getResourceAsStream("unregistering-service-worker.js")) {
      assertTrue(stream != null, "unregistering-service-worker.js must be on the classpath");
      String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(
          content.contains("unregister"), "Unregistering SW must contain self-unregister call");
      assertFalse(
          content.contains("SHELL_CACHE"), "Unregistering SW must NOT contain canonical content");
    } catch (Exception e) {
      throw new AssertionError("Failed to read unregistering-service-worker.js", e);
    }
  }

  private MockHttpServletRequest swRequest(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setContextPath("");
    return request;
  }
}
