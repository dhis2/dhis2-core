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
package org.hisp.dhis.webapi.staticresource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link StaticCacheInterceptor}, in particular that core static paths are matched
 * relative to the servlet context path so cache headers are also set on deployments under a
 * non-root context path (DHIS2-21880).
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@ExtendWith(MockitoExtension.class)
class StaticCacheInterceptorTest {

  @Mock private StaticCacheControlService staticCacheControlService;

  private StaticCacheInterceptor interceptor;

  private final Object handler = new Object();

  @BeforeEach
  void setUp() {
    interceptor = new StaticCacheInterceptor(staticCacheControlService);
  }

  @ParameterizedTest(
      name = "Core static URI {1} under context path \"{0}\" gets cache headers for {2}")
  @CsvSource({
    "'', /dhis-web-commons/css/style.css, /dhis-web-commons/css/style.css",
    "/dhis, /dhis/dhis-web-commons/css/style.css, /dhis-web-commons/css/style.css",
    "/dhis, /dhis/favicon.ico, /favicon.ico",
    "/dhis, /dhis/icons/star.png, /icons/star.png"
  })
  void coreStaticPath_setsHeaders(String contextPath, String requestUri, String expectedPath) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath(contextPath);
    request.setRequestURI(requestUri);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(interceptor.preHandle(request, response, handler));

    verify(staticCacheControlService).setHeaders(response, expectedPath, null, null);
  }

  @Test
  @DisplayName("Non-static path does not get cache headers")
  void nonStaticPath_doesNotSetHeaders() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContextPath("/dhis");
    request.setRequestURI("/dhis/api/dataElements");
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertTrue(interceptor.preHandle(request, response, handler));

    verify(staticCacheControlService, never())
        .setHeaders(any(MockHttpServletResponse.class), anyString(), any(), any());
  }
}
