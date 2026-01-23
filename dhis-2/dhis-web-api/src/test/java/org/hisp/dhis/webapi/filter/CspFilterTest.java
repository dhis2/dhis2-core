/*
 * Copyright (c) 2004-2024, University of Oslo
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

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.webapi.filter.CspFilter.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.webapi.filter.CspFilter.FRAME_ANCESTORS_DEFAULT_CSP;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.hisp.dhis.configuration.Configuration;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link CspFilter}.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class CspFilterTest {

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;

  @Mock private ConfigurationService configurationService;

  @Mock private Configuration configuration;

  private CspFilter filter;

  @BeforeEach
  void setUp() {
    when(dhisConfigurationProvider.isEnabled(CSP_ENABLED)).thenReturn(true);
    filter = new CspFilter(dhisConfigurationProvider, configurationService);
  }

  @Test
  void shouldCacheCorsWhitelistAndNotCallServiceOnEveryRequest() throws Exception {
    // Given: A CORS whitelist configured
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getCorsWhitelist()).thenReturn(Set.of("https://example.com"));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    FilterChain filterChain = mockFilterChain();

    // When: Multiple requests are processed
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);

    // Then: ConfigurationService should only be called once (cached)
    verify(configurationService, times(1)).getConfiguration();
  }

  @Test
  void shouldRefreshCacheAfterIntervalExpires() throws Exception {
    // Given: A CORS whitelist configured
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getCorsWhitelist())
        .thenReturn(Set.of("https://example.com"))
        .thenReturn(Set.of("https://updated.com"));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    FilterChain filterChain = mockFilterChain();

    // First request - should call service
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    verify(configurationService, times(1)).getConfiguration();

    // Simulate cache expiration by setting lastCorsRefreshTime to past
    setLastRefreshTimeToExpired();

    // Next request after cache expiration - should call service again
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    verify(configurationService, times(2)).getConfiguration();
  }

  @Test
  void shouldSetFrameAncestorsCspWithCorsWhitelist() throws Exception {
    // Given: A CORS whitelist configured
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getCorsWhitelist())
        .thenReturn(Set.of("https://example.com", "https://other.com"));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mockFilterChain();

    // When
    filter.doFilter(request, response, filterChain);

    // Then: CSP header should include frame-ancestors with CORS origins
    String cspHeader = response.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME);
    assertNotNull(cspHeader);
    assertTrue(cspHeader.startsWith(FRAME_ANCESTORS_DEFAULT_CSP));
    assertTrue(
        cspHeader.contains("https://example.com") || cspHeader.contains("https://other.com"));
    assertTrue(cspHeader.endsWith(";"));
  }

  @Test
  void shouldSetDefaultFrameAncestorsCspWhenCorsWhitelistEmpty() throws Exception {
    // Given: Empty CORS whitelist
    when(configurationService.getConfiguration()).thenReturn(configuration);
    when(configuration.getCorsWhitelist()).thenReturn(Set.of());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mockFilterChain();

    // When
    filter.doFilter(request, response, filterChain);

    // Then: CSP header should only have default frame-ancestors
    String cspHeader = response.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME);
    assertEquals(FRAME_ANCESTORS_DEFAULT_CSP + ";", cspHeader);
  }

  @Test
  void shouldSetXFrameOptionsWhenCspDisabled() throws Exception {
    // Given: CSP is disabled
    when(dhisConfigurationProvider.isEnabled(CSP_ENABLED)).thenReturn(false);
    CspFilter disabledFilter = new CspFilter(dhisConfigurationProvider, configurationService);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mockFilterChain();

    // When
    disabledFilter.doFilter(request, response, filterChain);

    // Then: X-Frame-Options should be set, not CSP
    assertEquals("SAMEORIGIN", response.getHeader("X-Frame-Options"));
    // ConfigurationService should not be called when CSP is disabled
    verify(configurationService, never()).getConfiguration();
  }

  private FilterChain mockFilterChain() {
    return (filterRequest, filterResponse) -> {
      ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
    };
  }

  /**
   * Uses reflection to set the lastCorsRefreshTime to a time in the past, simulating cache
   * expiration.
   */
  private void setLastRefreshTimeToExpired() throws Exception {
    Field lastRefreshField = CspFilter.class.getDeclaredField("lastCorsRefreshTime");
    lastRefreshField.setAccessible(true);
    AtomicLong lastRefresh = (AtomicLong) lastRefreshField.get(filter);
    // Set to 6 minutes ago (cache interval is 5 minutes)
    lastRefresh.set(System.currentTimeMillis() - (6 * 60 * 1000L));
  }
}
