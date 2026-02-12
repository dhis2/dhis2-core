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
package org.hisp.dhis.webapi.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiRequestDetectorTest {

  @Mock private HttpServletRequest request;

  @Test
  void shouldDetectXmlHttpRequestHeader() {
    when(request.getHeader("X-Requested-With")).thenReturn("XMLHttpRequest");

    assertTrue(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldDetectApiPath() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/api/me");
    when(request.getContextPath()).thenReturn("");

    assertTrue(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldDetectApiPathExact() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/api");
    when(request.getContextPath()).thenReturn("");

    assertTrue(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldDetectApiPathWithContextPath() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/dhis/api/me");
    when(request.getContextPath()).thenReturn("/dhis");

    assertTrue(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldDetectJsonAcceptHeader() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/some/endpoint");
    when(request.getContextPath()).thenReturn("");
    when(request.getHeader("Accept")).thenReturn("application/json");

    assertTrue(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldDetectXmlAcceptHeader() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/some/endpoint");
    when(request.getContextPath()).thenReturn("");
    when(request.getHeader("Accept")).thenReturn("application/xml");

    assertTrue(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldNotDetectBrowserNavigationWithMixedAccept() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/some/page");
    when(request.getContextPath()).thenReturn("");
    when(request.getHeader("Accept"))
        .thenReturn("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

    assertFalse(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldNotDetectBrowserNavigationWithHtmlAccept() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/dhis-web-dashboard/");
    when(request.getContextPath()).thenReturn("");
    when(request.getHeader("Accept")).thenReturn("text/html");

    assertFalse(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldNotDetectPlainBrowserRequest() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/dhis-web-dashboard/");
    when(request.getContextPath()).thenReturn("");
    when(request.getHeader("Accept")).thenReturn(null);

    assertFalse(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldDetectApiPathRegardlessOfAcceptHeader() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/api/dataElements");
    when(request.getContextPath()).thenReturn("");

    assertTrue(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldNotDetectNonApiPathWithNoHeaders() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/login/");
    when(request.getContextPath()).thenReturn("");
    when(request.getHeader("Accept")).thenReturn(null);

    assertFalse(ApiRequestDetector.isApiRequest(request));
  }

  @Test
  void shouldNotMatchPathLikeApiButNotApi() {
    when(request.getHeader("X-Requested-With")).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/apikeys/list");
    when(request.getContextPath()).thenReturn("");
    when(request.getHeader("Accept")).thenReturn(null);

    assertFalse(ApiRequestDetector.isApiRequest(request));
  }
}
