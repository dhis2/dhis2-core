/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Tests for {@link HttpServletRequestPaths}, especially X-Forwarded-Host handling for proxy
 * deployments.
 */
class HttpServletRequestPathsTest {

  @Test
  void getContextPath_usesXForwardedHostWhenPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme("http");
    request.setServerName("10.42.228.17");
    request.setServerPort(8080);
    request.setContextPath("/pr-23086");
    request.setRequestURI("/pr-23086/api/apps/menu");
    request.addHeader("X-Forwarded-Host", "dev.im.dhis2.org");
    request.addHeader("X-Forwarded-Proto", "https");
    request.addHeader("X-Forwarded-Port", "443");

    String result = HttpServletRequestPaths.getContextPath(request);

    assertEquals("https://dev.im.dhis2.org/pr-23086", result);
  }

  @Test
  void getContextPath_fallsBackToServerNameWhenNoXForwardedHost() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(8080);
    request.setContextPath("");
    request.setRequestURI("/api/apps/menu");

    String result = HttpServletRequestPaths.getContextPath(request);

    assertEquals("http://localhost:8080", result);
  }

  @Test
  void getContextPath_stripsPortFromXForwardedHostHostPortFormat() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme("http");
    request.setServerName("10.0.0.1");
    request.setServerPort(8080);
    request.setContextPath("/apppath");
    request.addHeader("X-Forwarded-Host", "dev.im.dhis2.org:443");
    request.addHeader("X-Forwarded-Proto", "https");
    request.addHeader("X-Forwarded-Port", "443");

    String result = HttpServletRequestPaths.getContextPath(request);

    assertTrue(result.contains("dev.im.dhis2.org"));
    assertTrue(result.startsWith("https://dev.im.dhis2.org/apppath"));
  }

  @Test
  void getContextPath_handlesEmptyXForwardedHost() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme("http");
    request.setServerName("localhost");
    request.setServerPort(8080);
    request.setContextPath("");
    request.addHeader("X-Forwarded-Host", "");

    String result = HttpServletRequestPaths.getContextPath(request);

    assertEquals("http://localhost:8080", result);
  }
}
