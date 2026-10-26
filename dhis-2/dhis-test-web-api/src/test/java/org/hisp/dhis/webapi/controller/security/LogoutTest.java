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
package org.hisp.dhis.webapi.controller.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.SessionCookieConfig;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.test.webapi.AuthenticationApiTestBase;
import org.hisp.dhis.webapi.security.CrossOriginGetGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Tests logout through the security filter chain: POST always logs out, GET only when it passes the
 * cross-origin GET guard, and the cookie-clearing headers keep the attributes the cookies were set
 * with.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class LogoutTest extends AuthenticationApiTestBase {
  private static final String LOGOUT_URL = "/dhis-web-commons-security/logout.action";

  private SessionCookieConfig cookieConfig;

  private String previousSameSite;

  private boolean previousSecure;

  private boolean previousHttpOnly;

  @BeforeEach
  void configureSessionCookie() {
    cookieConfig = webApplicationContext.getServletContext().getSessionCookieConfig();
    previousSameSite = cookieConfig.getAttribute("SameSite");
    previousSecure = cookieConfig.isSecure();
    previousHttpOnly = cookieConfig.isHttpOnly();
    // as configured by DhisWebApiWebAppInitializer with server.https=on
    cookieConfig.setAttribute("SameSite", "Strict");
    cookieConfig.setSecure(true);
    cookieConfig.setHttpOnly(true);
  }

  @AfterEach
  void restoreSessionCookie() {
    cookieConfig.setAttribute("SameSite", previousSameSite);
    cookieConfig.setSecure(previousSecure);
    cookieConfig.setHttpOnly(previousHttpOnly);
  }

  static Stream<Arguments> acceptedGetHeaders() {
    return Stream.of(
        Arguments.of("no Fetch Metadata", List.of()),
        Arguments.of("same-origin navigation", List.of("Sec-Fetch-Site", "same-origin")),
        Arguments.of("typed URL or bookmark", List.of("Sec-Fetch-Site", "none")),
        Arguments.of(
            "fetch with an Origin that passed the CORS check",
            List.of("Sec-Fetch-Site", "same-site", "Origin", "http://localhost")));
  }

  static Stream<Arguments> rejectedGetHeaders() {
    return Stream.of(
        Arguments.of(
            "cross-site navigation",
            List.of("Sec-Fetch-Site", "cross-site", "Sec-Fetch-Mode", "navigate")),
        Arguments.of(
            "same-site image load",
            List.of("Sec-Fetch-Site", "same-site", "Sec-Fetch-Dest", "image")));
  }

  @Test
  void testLogoutWithPost() throws Exception {
    MockHttpServletResponse response = perform(post(LOGOUT_URL), List.of());

    assertLoggedOut(response);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("acceptedGetHeaders")
  void testLogoutWithGetAccepted(String name, List<String> headers) throws Exception {
    MockHttpServletResponse response = perform(get(LOGOUT_URL), headers);

    assertLoggedOut(response);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedGetHeaders")
  void testLogoutWithGetRejected(String name, List<String> headers) throws Exception {
    MockHttpServletResponse response = perform(get(LOGOUT_URL), headers);

    assertEquals(403, response.getStatus());
    assertTrue(
        response.getContentAsString().contains(CrossOriginGetGuard.MESSAGE),
        response.getContentAsString());
    assertFalse(session.isInvalid());
    assertTrue(response.getHeaders("Set-Cookie").isEmpty());
  }

  @Test
  void testLogoutWithGetWithoutSessionIsNotRejected() throws Exception {
    MockHttpServletResponse response =
        mvc.perform(get(LOGOUT_URL).header("Sec-Fetch-Site", "cross-site"))
            .andReturn()
            .getResponse();

    assertEquals(302, response.getStatus());
  }

  private MockHttpServletResponse perform(
      MockHttpServletRequestBuilder request, List<String> headers) throws Exception {
    request.session(session);
    for (int i = 0; i < headers.size(); i += 2) {
      request.header(headers.get(i), headers.get(i + 1));
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private void assertLoggedOut(MockHttpServletResponse response) {
    assertEquals(302, response.getStatus());
    assertTrue(session.isInvalid());

    List<String> cookies = response.getHeaders("Set-Cookie");
    String sessionCookie = findCookie(cookies, "JSESSIONID=");
    assertTrue(sessionCookie.contains("Max-Age=0"), sessionCookie);
    assertTrue(sessionCookie.contains("Path=/"), sessionCookie);
    assertTrue(sessionCookie.contains("HttpOnly"), sessionCookie);
    assertTrue(sessionCookie.contains("Secure"), sessionCookie);
    assertTrue(sessionCookie.contains("SameSite=Strict"), sessionCookie);

    String expireCookie = findCookie(cookies, "SESSION_EXPIRE=");
    assertTrue(expireCookie.contains("Max-Age=0"), expireCookie);
    assertTrue(expireCookie.contains("Path=/"), expireCookie);
    assertFalse(expireCookie.contains("HttpOnly"), expireCookie);
    assertTrue(expireCookie.contains("Secure"), expireCookie);
    assertTrue(expireCookie.contains("SameSite=Strict"), expireCookie);
  }

  private static String findCookie(List<String> cookies, String prefix) {
    return cookies.stream()
        .filter(cookie -> cookie.startsWith(prefix))
        .findFirst()
        .orElseThrow(() -> new AssertionError(prefix + " not cleared in " + cookies));
  }
}
