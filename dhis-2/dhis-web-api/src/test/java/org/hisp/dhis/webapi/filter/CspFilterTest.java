/*
 * Copyright (c) 2004-2026, University of Oslo
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
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

  @Mock(lenient = true)
  private DhisConfigurationProvider dhisConfigurationProvider;

  @Mock(lenient = true)
  private SystemSettingsProvider settingsProvider;

  @Mock private SystemSettings settings;

  private CspFilter filter;

  @BeforeEach
  void setUp() {
    when(dhisConfigurationProvider.isEnabled(CSP_ENABLED)).thenReturn(true);
    when(settingsProvider.getCurrentSettings()).thenReturn(settings);

    filter = new CspFilter(dhisConfigurationProvider, settingsProvider);
  }

  @Test
  void shouldReadCorsWhitelistFromSystemSettingsOnEachRequest() throws Exception {
    when(settings.getCorsWhitelist()).thenReturn(Set.of("https://example.com"));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    FilterChain filterChain = mockFilterChain();

    for (int i = 0; i < 5; i++) {
      filter.doFilter(request, new MockHttpServletResponse(), filterChain);
    }

    // SystemSettingsProvider snapshot is in-memory; the filter reads it on every request
    verify(settingsProvider, times(5)).getCurrentSettings();
  }

  @Test
  void shouldReflectUpdatedWhitelistAcrossRequests() throws Exception {
    when(settings.getCorsWhitelist())
        .thenReturn(Set.of("https://example.com"))
        .thenReturn(Set.of("https://updated.com"));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    FilterChain filterChain = mockFilterChain();

    MockHttpServletResponse response1 = new MockHttpServletResponse();
    filter.doFilter(request, response1, filterChain);

    MockHttpServletResponse response2 = new MockHttpServletResponse();
    filter.doFilter(request, response2, filterChain);

    assertTrue(
        response1.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME).contains("https://example.com"));
    assertTrue(
        response2.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME).contains("https://updated.com"));
  }

  @Test
  void shouldSetFrameAncestorsCspWithCorsWhitelist() throws Exception {
    when(settings.getCorsWhitelist())
        .thenReturn(Set.of("https://example.com", "https://other.com"));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mockFilterChain();

    filter.doFilter(request, response, filterChain);

    String cspHeader = response.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME);
    assertNotNull(cspHeader);
    assertTrue(cspHeader.startsWith(FRAME_ANCESTORS_DEFAULT_CSP));
    assertTrue(
        cspHeader.contains("https://example.com") || cspHeader.contains("https://other.com"));
    assertTrue(cspHeader.endsWith(";"));
  }

  @Test
  void shouldSetDefaultFrameAncestorsCspWhenCorsWhitelistEmpty() throws Exception {
    when(settings.getCorsWhitelist()).thenReturn(Set.of());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mockFilterChain();

    filter.doFilter(request, response, filterChain);

    String cspHeader = response.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME);
    assertEquals(FRAME_ANCESTORS_DEFAULT_CSP + ";", cspHeader);
  }

  @Test
  void shouldSetXFrameOptionsWhenCspDisabled() throws Exception {
    when(dhisConfigurationProvider.isEnabled(CSP_ENABLED)).thenReturn(false);
    CspFilter disabledFilter = new CspFilter(dhisConfigurationProvider, settingsProvider);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mockFilterChain();

    disabledFilter.doFilter(request, response, filterChain);

    assertEquals("SAMEORIGIN", response.getHeader("X-Frame-Options"));
  }

  private FilterChain mockFilterChain() {
    return (filterRequest, filterResponse) -> {
      ((HttpServletResponse) filterResponse).setStatus(HttpServletResponse.SC_OK);
    };
  }
}
