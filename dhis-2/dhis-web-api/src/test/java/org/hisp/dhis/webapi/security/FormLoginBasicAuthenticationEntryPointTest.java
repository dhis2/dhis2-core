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
package org.hisp.dhis.webapi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletResponse;
import org.hisp.dhis.render.RenderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

@ExtendWith(MockitoExtension.class)
class FormLoginBasicAuthenticationEntryPointTest {

  @Mock private RenderService renderService;

  @InjectMocks
  private FormLoginBasicAuthenticationEntryPoint entryPoint =
      new FormLoginBasicAuthenticationEntryPoint("/login/");

  private final AuthenticationException authException = new BadCredentialsException("Unauthorized");

  @Test
  void shouldReturn401ForApiPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/dataElements");
    request.setContextPath("");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, authException);

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  void shouldReturn401ForBasicAuth() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/some/page");
    request.setContextPath("");
    request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, authException);

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
  }

  @Test
  void shouldReturn401ForJsonAcceptOnApiPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/me");
    request.setContextPath("");
    request.addHeader("Accept", "application/json");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, authException);

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    assertEquals("application/json", response.getContentType());
  }

  @Test
  void shouldReturnXmlFor401WhenXmlAcceptOnApiPath() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/me");
    request.setContextPath("");
    request.addHeader("Accept", "application/xml");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, authException);

    assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    assertEquals("application/xml", response.getContentType());
  }

  @Test
  void shouldRedirectForBrowserNavigation() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/dhis-web-dashboard/");
    request.setContextPath("");
    request.addHeader("Accept", "text/html,application/xhtml+xml");
    MockHttpServletResponse response = new MockHttpServletResponse();

    entryPoint.commence(request, response, authException);

    assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, response.getStatus());
  }
}
