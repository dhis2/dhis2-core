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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hisp.dhis.configuration.ConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for {@link AllowedOriginMatcher}: how the server's own origin is computed, and which
 * {@code Origin} header values are accepted.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class AllowedOriginMatcherTest {

  private static final String ALLOWLISTED_ORIGIN = "http://app.example.org";

  private ConfigurationService configurationService;

  private AllowedOriginMatcher matcher;

  @BeforeEach
  void setUp() {
    configurationService = mock(ConfigurationService.class);
    when(configurationService.isCorsWhitelisted(ALLOWLISTED_ORIGIN)).thenReturn(true);
    matcher = new AllowedOriginMatcher(configurationService);
  }

  @ParameterizedTest
  @CsvSource(
      nullValues = "NONE",
      value = {
        "http,  localhost,        80,   NONE,  http://localhost",
        "http,  localhost,        8080, NONE,  http://localhost:8080",
        "https, dhis.example.org, 443,  NONE,  https://dhis.example.org",
        "https, dhis.example.org, 8443, NONE,  https://dhis.example.org:8443",
        "http,  dhis.example.org, 80,   https, https://dhis.example.org",
        "http,  dhis.example.org, 8080, https, https://dhis.example.org:8080",
      })
  void selfOriginUsesRequestHostAndPortAndForwardedProtoScheme(
      String scheme, String host, int port, String forwardedProto, String expected) {
    MockHttpServletRequest request = request(scheme, host, port);
    if (forwardedProto != null) {
      request.addHeader("X-Forwarded-Proto", forwardedProto);
    }

    assertEquals(expected, matcher.getSelfOrigin(request));
    assertTrue(matcher.isAllowed(request, expected));
  }

  @Test
  void selfOriginWithQueryStringIgnoresQuery() {
    MockHttpServletRequest request = request("http", "localhost", 8080);
    request.setQueryString("filter=name:eq:a b&fields=*");

    assertEquals("http://localhost:8080", matcher.getSelfOrigin(request));
  }

  @Test
  void forwardedProtoMismatchRejectsRequestSchemeOrigin() {
    MockHttpServletRequest request = request("http", "dhis.example.org", 80);
    request.addHeader("X-Forwarded-Proto", "https");

    assertFalse(matcher.isAllowed(request, "http://dhis.example.org"));
  }

  @Test
  void allowlistedOriginIsAllowed() {
    assertTrue(matcher.isAllowed(request("http", "localhost", 8080), ALLOWLISTED_ORIGIN));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://evil.example",
        "http://localhost",
        "https://localhost:8080",
        "http://localhost:8081",
        "http://sub.localhost:8080"
      })
  void foreignOriginIsNotAllowed(String origin) {
    assertFalse(matcher.isAllowed(request("http", "localhost", 8080), origin));
  }

  @ParameterizedTest
  @ValueSource(strings = {"null", "NULL"})
  void nullOriginIsNeverAllowedEvenWithWildcardAllowlist(String origin) {
    when(configurationService.isCorsWhitelisted(anyString())).thenReturn(true);

    assertFalse(matcher.isAllowed(request("http", "localhost", 8080), origin));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void missingOriginIsNotAllowed(String origin) {
    assertFalse(matcher.isAllowed(request("http", "localhost", 8080), origin));
  }

  private static MockHttpServletRequest request(String scheme, String host, int port) {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/constants");
    request.setScheme(scheme);
    request.setServerName(host);
    request.setServerPort(port);
    return request;
  }
}
