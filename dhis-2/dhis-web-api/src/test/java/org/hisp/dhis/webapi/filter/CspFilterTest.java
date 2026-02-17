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
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.FRAME_ANCESTORS_DEFAULT_CSP;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.function.Function;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
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

  @Mock private CacheProvider cacheProvider;

  @Mock private Cache<Set<String>> cache;

  private CspFilter filter;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    when(dhisConfigurationProvider.isEnabled(CSP_ENABLED)).thenReturn(true);
    when(cacheProvider.<Set<String>>createCorsWhitelistCache()).thenReturn(cache);

    filter = new CspFilter(dhisConfigurationProvider, configurationService, cacheProvider);
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldCacheCorsWhitelistAndNotCallServiceOnEveryRequest() throws Exception {
    // Given: A CORS whitelist configured via cache
    Set<String> corsWhitelist = Set.of("https://example.com");
    when(cache.get(anyString(), any(Function.class))).thenReturn(corsWhitelist);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    FilterChain filterChain = mockFilterChain();

    // When: Multiple requests are processed
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    filter.doFilter(request, new MockHttpServletResponse(), filterChain);

    // Then: Cache.get() should be called for each request (cache handles expiration internally)
    verify(cache, times(5)).get(anyString(), any(Function.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldUseCacheForCorsWhitelistLookup() throws Exception {
    // Given: Cache returns the CORS whitelist (simulates cache behavior)
    when(cache.get(anyString(), any(Function.class)))
        .thenReturn(Set.of("https://example.com"))
        .thenReturn(Set.of("https://updated.com"));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    FilterChain filterChain = mockFilterChain();

    // When: Two requests are processed
    MockHttpServletResponse response1 = new MockHttpServletResponse();
    filter.doFilter(request, response1, filterChain);

    MockHttpServletResponse response2 = new MockHttpServletResponse();
    filter.doFilter(request, response2, filterChain);

    // Then: Cache.get() is invoked for each request
    verify(cache, times(2)).get(anyString(), any(Function.class));

    // And responses contain the appropriate CORS origins
    assertTrue(
        response1.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME).contains("https://example.com"));
    assertTrue(
        response2.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME).contains("https://updated.com"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldSetFrameAncestorsCspWithCorsWhitelist() throws Exception {
    // Given: A CORS whitelist configured via cache
    when(cache.get(anyString(), any(Function.class)))
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
    assertTrue(
        cspHeader.contains(FRAME_ANCESTORS_DEFAULT_CSP + " https://example.com https://other.com;")
            || cspHeader.contains(
                FRAME_ANCESTORS_DEFAULT_CSP + " https://other.com https://example.com;"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldSetDefaultFrameAncestorsCspWhenCorsWhitelistEmpty() throws Exception {
    // Given: Empty CORS whitelist from cache
    when(cache.get(anyString(), any(Function.class))).thenReturn(Set.of());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mockFilterChain();

    // When
    filter.doFilter(request, response, filterChain);

    // Then: CSP header should only have default frame-ancestors
    String cspHeader = response.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME);
    assertTrue(cspHeader.contains(FRAME_ANCESTORS_DEFAULT_CSP + ";"));
  }

  @SuppressWarnings("unchecked")
  @Test
  void shouldSetXFrameOptionsWhenCspDisabled() throws Exception {
    // Given: CSP is disabled
    when(dhisConfigurationProvider.isEnabled(CSP_ENABLED)).thenReturn(false);
    CspFilter disabledFilter =
        new CspFilter(dhisConfigurationProvider, configurationService, cacheProvider);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mockFilterChain();

    // When
    disabledFilter.doFilter(request, response, filterChain);

    // Then: X-Frame-Options should be set, not CSP
    assertEquals("SAMEORIGIN", response.getHeader("X-Frame-Options"));
    // Cache should not be accessed when CSP is disabled
    verify(cache, never()).get(anyString(), any(Function.class));
  }

  private FilterChain mockFilterChain() {
    return (filterRequest, filterResponse) -> {
      ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
    };
  }
}
