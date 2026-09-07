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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link LegacyDhisWebLoginRedirectFilter}.
 *
 * @author Morten Svanæs
 */
@ExtendWith(MockitoExtension.class)
class LegacyDhisWebLoginRedirectFilterTest {

  private final LegacyDhisWebLoginRedirectFilter filter = new LegacyDhisWebLoginRedirectFilter();

  @Mock private FilterChain chain;

  @Test
  void redirectsBareLegacyPath() throws Exception {
    MockHttpServletResponse response = doFilter("/dhis-web-login", null, "");
    assertRedirect(response, "/login");
    verify(chain, never())
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void redirectsLegacyRoot() throws Exception {
    MockHttpServletResponse response = doFilter("/dhis-web-login/", null, "");
    assertRedirect(response, "/login/");
  }

  @Test
  void redirectsIndexHtmlPreservingHash() throws Exception {
    // The hash fragment is client-side and never sent to the server, so the
    // filter only needs to redirect /index.html — the browser keeps the hash.
    MockHttpServletResponse response = doFilter("/dhis-web-login/index.html", null, "");
    assertRedirect(response, "/login/index.html");
  }

  @Test
  void redirectsNestedPath() throws Exception {
    MockHttpServletResponse response = doFilter("/dhis-web-login/static/main.js", null, "");
    assertRedirect(response, "/login/static/main.js");
  }

  @Test
  void preservesQueryString() throws Exception {
    MockHttpServletResponse response = doFilter("/dhis-web-login/", "oidcFailure=true", "");
    assertRedirect(response, "/login/?oidcFailure=true");
  }

  @Test
  void preservesContextPath() throws Exception {
    MockHttpServletResponse response = doFilter("/dhis/dhis-web-login/index.html", null, "/dhis");
    assertRedirect(response, "/dhis/login/index.html");
  }

  @Test
  void doesNotRedirectFalsePositivePrefix() throws Exception {
    // /dhis-web-loginabc starts with /dhis-web-login but isn't the legacy login app.
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/dhis-web-loginabc");
    request.setRequestURI("/dhis-web-loginabc");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    verify(chain).doFilter(request, response);
  }

  @Test
  void doesNotRedirectUnrelatedPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/me");
    request.setRequestURI("/api/me");
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    verify(chain).doFilter(request, response);
  }

  private MockHttpServletResponse doFilter(
      String requestUri, String queryString, String contextPath) throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
    request.setRequestURI(requestUri);
    request.setContextPath(contextPath);
    if (queryString != null) {
      request.setQueryString(queryString);
    }
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(request, response, chain);
    return response;
  }

  private static void assertRedirect(MockHttpServletResponse response, String expectedLocation) {
    assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, response.getStatus());
    assertEquals(expectedLocation, response.getHeader("Location"));
  }
}
