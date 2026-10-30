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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.util.Set;
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
 * Unit tests for {@link CspFilter}. CORS-whitelist caching is owned by {@link
 * ConfigurationService#getCorsWhitelist()} and is exercised by {@code ConfigurationServiceTest};
 * here we just verify the filter renders the right headers given whatever the service returns.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class CspFilterTest {

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;

  @Mock private ConfigurationService configurationService;

  private CspFilter filter;

  @BeforeEach
  void setUp() {
    when(dhisConfigurationProvider.isEnabled(CSP_ENABLED)).thenReturn(true);
    filter = new CspFilter(dhisConfigurationProvider, configurationService);
  }

  @Test
  void shouldSetFrameAncestorsCspWithCorsWhitelist() throws Exception {
    when(configurationService.getCorsWhitelist())
        .thenReturn(Set.of("https://example.com", "https://other.com"));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, mockFilterChain());

    String cspHeader = response.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME);
    assertNotNull(cspHeader);
    assertTrue(cspHeader.startsWith(FRAME_ANCESTORS_DEFAULT_CSP));
    assertTrue(
        cspHeader.contains("https://example.com") || cspHeader.contains("https://other.com"));
    assertTrue(cspHeader.endsWith(";"));
  }

  @Test
  void shouldSetDefaultFrameAncestorsCspWhenCorsWhitelistEmpty() throws Exception {
    when(configurationService.getCorsWhitelist()).thenReturn(Set.of());

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, mockFilterChain());

    assertEquals(
        FRAME_ANCESTORS_DEFAULT_CSP + ";", response.getHeader(CONTENT_SECURITY_POLICY_HEADER_NAME));
  }

  @Test
  void shouldSetXFrameOptionsWhenCspDisabled() throws Exception {
    when(dhisConfigurationProvider.isEnabled(CSP_ENABLED)).thenReturn(false);
    CspFilter disabledFilter = new CspFilter(dhisConfigurationProvider, configurationService);

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
    request.setServerName("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();

    disabledFilter.doFilter(request, response, mockFilterChain());

    assertEquals("SAMEORIGIN", response.getHeader("X-Frame-Options"));
    verify(configurationService, never()).getCorsWhitelist();
  }

  private static FilterChain mockFilterChain() {
    return (req, res) -> {
      // No-op chain.
    };
  }
}
