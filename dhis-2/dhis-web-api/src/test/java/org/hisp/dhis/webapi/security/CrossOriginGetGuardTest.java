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
package org.hisp.dhis.webapi.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.feedback.ForbiddenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class CrossOriginGetGuardTest {

  private static final String[] SITES = {
    null, "same-origin", "none", "same-site", "cross-site", "CROSS-SITE", "unexpected"
  };

  private static final String[] METHODS = {"GET", "HEAD", "POST", "PUT", "PATCH", "DELETE"};

  static Stream<Arguments> requests() {
    Stream.Builder<Arguments> cases = Stream.builder();
    for (String method : METHODS) {
      for (String site : SITES) {
        for (boolean withOrigin : new boolean[] {false, true}) {
          cases.add(
              Arguments.of(method, site, withOrigin, expectedAllowed(method, site, withOrigin)));
        }
      }
    }
    return cases.build();
  }

  /** The guard rule written out per input, independent of the implementation. */
  private static boolean expectedAllowed(String method, String site, boolean withOrigin) {
    boolean getOrHead = method.equals("GET") || method.equals("HEAD");
    boolean trustedSite = site == null || site.equals("same-origin") || site.equals("none");
    return !getOrHead || trustedSite || withOrigin;
  }

  @ParameterizedTest(name = "{0} Sec-Fetch-Site={1} Origin={2} -> allowed={3}")
  @MethodSource("requests")
  void isAllowed(String method, String site, boolean withOrigin, boolean expected) {
    MockHttpServletRequest request = request(method, site, withOrigin);

    assertEquals(expected, CrossOriginGetGuard.isAllowed(request));
  }

  @ParameterizedTest
  @CsvSource({"GET, cross-site", "GET, same-site", "HEAD, cross-site", "HEAD, same-site"})
  void checkRejectsCrossOriginNavigation(String method, String site) {
    MockHttpServletRequest request = request(method, site, false);
    request.addHeader("Sec-Fetch-Mode", "navigate");
    request.addHeader("Sec-Fetch-Dest", "document");

    ForbiddenException ex =
        assertThrows(ForbiddenException.class, () -> CrossOriginGetGuard.check(request));
    assertEquals(ErrorCode.E1008, ex.getCode());
    assertEquals(CrossOriginGetGuard.MESSAGE, ex.getMessage());
  }

  @ParameterizedTest
  @CsvSource({"GET, same-origin", "GET, none", "POST, cross-site"})
  void checkAcceptsTrustedRequests(String method, String site) {
    MockHttpServletRequest request = request(method, site, false);

    assertDoesNotThrow(() -> CrossOriginGetGuard.check(request));
  }

  static Stream<Arguments> crossSiteOrigins() {
    return Stream.of(
        Arguments.of("missing", null, false),
        Arguments.of("empty", "", false),
        Arguments.of("literal null", "null", true),
        Arguments.of("allowlisted", "https://allowed.example.org", true));
  }

  /**
   * The CORS origin check skips an empty Origin, so the guard must not trust one. A literal {@code
   * null} is a value that the CORS origin check has already compared to the allowlist.
   */
  @ParameterizedTest(name = "{0} Origin -> allowed={2}")
  @MethodSource("crossSiteOrigins")
  void isAllowedTreatsEmptyOriginAsMissing(String name, String origin, boolean expected) {
    MockHttpServletRequest request = request("GET", "cross-site", false);
    if (origin != null) {
      request.addHeader("Origin", origin);
    }

    assertEquals(expected, CrossOriginGetGuard.isAllowed(request));
  }

  @Test
  void checkAcceptsRequestWithoutFetchMetadata() {
    assertDoesNotThrow(() -> CrossOriginGetGuard.check(request("GET", null, false)));
  }

  @Test
  void sanitizeReplacesControlCharactersAndTruncates() {
    assertEquals("-", CrossOriginGetGuard.sanitize(null));
    assertEquals("cross-site__x", CrossOriginGetGuard.sanitize("cross-site\r\nx"));
    assertEquals(203, CrossOriginGetGuard.sanitize("a".repeat(500)).length());
  }

  @Test
  void routeIsTheHandlerTemplateNotTheRequestUri() {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/routes/abcdefghijk/run/private/path");
    // as set by Spring MVC handler mapping before interceptors and handlers run
    request.setAttribute(
        HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
        PathPatternParser.defaultInstance.parse("/api/routes/{id}/run/**"));

    assertEquals("/api/routes/{id}/run/**", CrossOriginGetGuard.route(request));
  }

  @Test
  void routeIsPlaceholderBeforeHandlerMapping() {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/routes/abcdefghijk/run/private/path");

    assertEquals("-", CrossOriginGetGuard.route(request));
  }

  private static MockHttpServletRequest request(String method, String site, boolean withOrigin) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/test");
    if (site != null) {
      request.addHeader("Sec-Fetch-Site", site);
    }
    if (withOrigin) {
      request.addHeader("Origin", "https://allowed.example.org");
    }
    return request;
  }
}
