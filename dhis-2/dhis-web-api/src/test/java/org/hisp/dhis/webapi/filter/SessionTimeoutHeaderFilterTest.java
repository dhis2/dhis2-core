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
package org.hisp.dhis.webapi.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SessionTimeoutHeaderFilterTest {

  private final SessionTimeoutHeaderFilter filter = new SessionTimeoutHeaderFilter();

  @Mock private FilterChain filterChain;

  @Mock private HttpSession session;

  private MockHttpServletRequest request;

  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAddHeaderForAuthenticatedUserWithSession() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(3600);
    request.setSession(session);

    filter.doFilter(request, response, filterChain);

    assertEquals("3600", response.getHeader(SessionTimeoutHeaderFilter.HEADER_NAME));
  }

  @Test
  void shouldNotAddHeaderWhenNotAuthenticated() throws Exception {
    filter.doFilter(request, response, filterChain);

    assertFalse(response.containsHeader(SessionTimeoutHeaderFilter.HEADER_NAME));
  }

  @Test
  void shouldNotAddHeaderForAnonymousUser() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    filter.doFilter(request, response, filterChain);

    assertFalse(response.containsHeader(SessionTimeoutHeaderFilter.HEADER_NAME));
  }

  @Test
  void shouldNotAddHeaderWhenNoSession() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    filter.doFilter(request, response, filterChain);

    assertFalse(response.containsHeader(SessionTimeoutHeaderFilter.HEADER_NAME));
  }

  @Test
  void shouldNotAddHeaderWhenMaxInactiveIntervalIsZero() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(0);
    request.setSession(session);

    filter.doFilter(request, response, filterChain);

    assertFalse(response.containsHeader(SessionTimeoutHeaderFilter.HEADER_NAME));
  }

  @Test
  void shouldNotAddHeaderWhenMaxInactiveIntervalIsNegative() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(-1);
    request.setSession(session);

    filter.doFilter(request, response, filterChain);

    assertFalse(response.containsHeader(SessionTimeoutHeaderFilter.HEADER_NAME));
  }

  @Test
  void shouldAddCookieForAuthenticatedUserWithSession() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(3600);
    request.setSession(session);

    long beforeEpoch = Instant.now().plusSeconds(3600).getEpochSecond();
    filter.doFilter(request, response, filterChain);
    long afterEpoch = Instant.now().plusSeconds(3600).getEpochSecond();

    String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
    assertTrue(setCookieHeader.startsWith("SESSION_EXPIRE="));

    String cookieValue =
        setCookieHeader.substring("SESSION_EXPIRE=".length(), setCookieHeader.indexOf(';'));
    long epochValue = Long.parseLong(cookieValue);
    assertTrue(epochValue >= beforeEpoch && epochValue <= afterEpoch);
  }

  @Test
  void shouldSetCookieMaxAgeToSessionTimeout() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(3600);
    request.setSession(session);

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
    assertTrue(setCookieHeader.contains("Max-Age=3600"));
  }

  @Test
  void shouldSetCookiePathToRoot() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(3600);
    request.setSession(session);

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
    assertTrue(setCookieHeader.contains("Path=/"));
  }

  @Test
  void shouldNotSetCookieHttpOnly() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(3600);
    request.setSession(session);

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
    assertFalse(setCookieHeader.contains("HttpOnly"));
  }

  @Test
  void shouldSetCookieSecureWhenSessionCookieIsSecure() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(3600);
    request.setSession(session);
    request.getServletContext().getSessionCookieConfig().setSecure(true);

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
    assertTrue(setCookieHeader.contains("Secure"));
  }

  @Test
  void shouldNotSetCookieSecureWhenSessionCookieIsNotSecure() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(3600);
    request.setSession(session);
    request.getServletContext().getSessionCookieConfig().setSecure(false);

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
    assertFalse(setCookieHeader.contains("Secure"));
  }

  @Test
  void shouldSetCookieSameSiteFromSessionCookieConfig() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    when(session.getMaxInactiveInterval()).thenReturn(3600);
    request.setSession(session);
    request.getServletContext().getSessionCookieConfig().setAttribute("SameSite", "Lax");

    filter.doFilter(request, response, filterChain);

    String setCookieHeader = response.getHeader(HttpHeaders.SET_COOKIE);
    assertTrue(setCookieHeader.contains("SameSite=Lax"));
  }

  @Test
  void shouldNotAddCookieWhenNotAuthenticated() throws Exception {
    filter.doFilter(request, response, filterChain);

    assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
  }

  @Test
  void shouldNotAddCookieForAnonymousUser() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                Collections.singleton(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

    filter.doFilter(request, response, filterChain);

    assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
  }

  @Test
  void shouldNotAddCookieWhenNoSession() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", "password", Collections.emptyList()));

    filter.doFilter(request, response, filterChain);

    assertNull(response.getHeader(HttpHeaders.SET_COOKIE));
  }
}
