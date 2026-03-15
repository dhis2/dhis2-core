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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.webapi.staticresource.HtmlCacheBustingService;
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
 */
@ExtendWith(MockitoExtension.class)
class GlobalShellFilterTest {

  @Mock private AppManager appManager;
  @Mock private SystemSettingsProvider settingsProvider;
  @Mock private SystemSettings settings;
  @Mock private StaticCacheControlService staticCacheControlService;
  @Mock private HtmlCacheBustingService htmlCacheBustingService;
  @Mock private FilterChain filterChain;

  private GlobalShellFilter filter;

  @BeforeEach
  void setUp() {
    filter =
        new GlobalShellFilter(
            appManager, settingsProvider, staticCacheControlService, htmlCacheBustingService);
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
    assertEquals("no-store", response.getHeader("Cache-Control"));
    assertEquals("/", response.getHeader("Service-Worker-Allowed"));

    String body = response.getContentAsString();
    assertTrue(
        body.contains("SHELL_CACHE"), "Should contain canonical SW content (SHELL_CACHE constant)");
    assertFalse(body.contains("unregister"), "Root SW should NOT self-unregister");
  }

  @Test
  void perAppSwRequest_canonicalOn_returns404() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    MockHttpServletRequest request = swRequest("/apps/maps/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertEquals(404, response.getStatus());
  }

  @Test
  void deepPerAppSwRequest_canonicalOn_returns404() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    MockHttpServletRequest request = swRequest("/apps/data-visualizer/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertEquals(404, response.getStatus());
  }

  @Test
  void globalShellSwRequest_canonicalOn_servesCanonicalServiceWorker() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(true);

    // The global shell registers its SW from its own app path, not the root.
    // It must still receive the canonical SW, not a 404.
    MockHttpServletRequest request = swRequest("/apps/global-shell/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    assertEquals(200, response.getStatus());
    assertEquals("application/javascript", response.getContentType());

    String body = response.getContentAsString();
    assertTrue(body.contains("SHELL_CACHE"), "Global shell SW should get canonical content");
  }

  // --- canonical paths OFF ---

  @Test
  void rootSwRequest_canonicalOff_passesThrough() throws Exception {
    when(settings.getGlobalShellEnabled()).thenReturn(true);
    when(settings.getCanonicalAppPaths()).thenReturn(false);

    MockHttpServletRequest request = swRequest("/apps/service-worker.js");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, filterChain);

    // Not handled by SW logic, falls through to global-shell serving
    // (which will eventually 404 since no real app is installed in this test).
    // The key assertion: it did NOT write JS content.
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

  private MockHttpServletRequest swRequest(String path) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.setContextPath("");
    return request;
  }
}
