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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.hisp.dhis.configuration.ConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit test for {@link LoginConfigCorsFilter}: the security-ignored {@code /api/**&#47;loginConfig}
 * endpoint must get CORS headers from the servlet-level filter (the Spring Security chain,
 * including its CorsFilter, is skipped for this path), while all other paths pass through
 * untouched.
 *
 * @author Morten Svanæs
 */
class LoginConfigCorsFilterTest {

  private static final String WHITELISTED_ORIGIN = "http://localhost:3000";

  private LoginConfigCorsFilter filter;

  @BeforeEach
  void setUp() {
    ConfigurationService configurationService = mock(ConfigurationService.class);
    when(configurationService.isCorsWhitelisted(WHITELISTED_ORIGIN)).thenReturn(true);
    filter = new LoginConfigCorsFilter(new DhisCorsProcessor(configurationService));
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/loginConfig", "/api/40/loginConfig", "/api/41/loginConfig"})
  void preflightOnLoginConfigShortCircuitsWithCorsHeaders(String path)
      throws ServletException, IOException {
    MockHttpServletRequest request = request("OPTIONS", path);
    request.addHeader("Origin", WHITELISTED_ORIGIN);
    request.addHeader("Access-Control-Request-Method", "GET");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(204, response.getStatus());
    assertEquals(WHITELISTED_ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
    assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
    assertEquals("GET", response.getHeader("Access-Control-Allow-Methods"));
    assertNull(chain.getRequest(), "preflight must short-circuit the filter chain");
  }

  @Test
  void getWithWhitelistedOriginAddsCorsHeadersAndContinuesChain()
      throws ServletException, IOException {
    MockHttpServletRequest request = request("GET", "/api/loginConfig");
    request.addHeader("Origin", WHITELISTED_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(WHITELISTED_ORIGIN, response.getHeader("Access-Control-Allow-Origin"));
    assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
    assertNotNull(chain.getRequest(), "actual request must continue down the filter chain");
  }

  @Test
  void nonWhitelistedOriginGetsNoCorsHeadersAndStopsChain() throws ServletException, IOException {
    MockHttpServletRequest request = request("GET", "/api/loginConfig");
    request.addHeader("Origin", "http://evil.example");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertNull(response.getHeader("Access-Control-Allow-Origin"));
    assertNull(chain.getRequest(), "rejected CORS request must not reach the endpoint");
  }

  @Test
  void nonCorsRequestWithoutOriginPassesThroughUnchanged() throws ServletException, IOException {
    MockHttpServletRequest request = request("GET", "/api/loginConfig");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertNull(response.getHeader("Access-Control-Allow-Origin"));
    assertNotNull(chain.getRequest(), "non-CORS request must continue down the filter chain");
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/auth/login", "/api/me", "/api/ping", "/api/40/auth/login"})
  void otherApiPathsAreNotFiltered(String path) throws ServletException, IOException {
    MockHttpServletRequest request = request("GET", path);
    request.addHeader("Origin", WHITELISTED_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertNull(
        response.getHeader("Access-Control-Allow-Origin"),
        "non-loginConfig paths must not get servlet-level CORS (security chain owns them)");
    assertNotNull(chain.getRequest(), "non-loginConfig request must continue down the chain");
  }

  private static MockHttpServletRequest request(String method, String path) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, path);
    request.setServletPath(path);
    request.setPathInfo(null);
    return request;
  }
}
