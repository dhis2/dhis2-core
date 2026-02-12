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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds an {@code X-Session-Timeout} response header and a {@code SESSION_EXPIRE} cookie to every
 * authenticated API response that has a server-side session.
 *
 * <p>The header value is the session's {@code maxInactiveInterval} in seconds. The cookie value is
 * the epoch-second timestamp when the session will expire, so any JavaScript code can compare
 * {@code Date.now()/1000} against it without needing to intercept response headers.
 *
 * <p>Neither the header nor the cookie is added for stateless authentication (PAT, JWT) since there
 * is no session to expire.
 */
public class SessionTimeoutHeaderFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Session-Timeout";

  public static final String COOKIE_NAME = "SESSION_EXPIRE";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    filterChain.doFilter(request, response);

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      return;
    }

    HttpSession session = request.getSession(false);
    if (session != null && session.getMaxInactiveInterval() > 0) {
      int maxInactiveInterval = session.getMaxInactiveInterval();
      response.setIntHeader(HEADER_NAME, maxInactiveInterval);

      long expiresEpochSecond = Instant.now().plusSeconds(maxInactiveInterval).getEpochSecond();
      SessionCookieConfig sessionCookieConfig =
          request.getServletContext().getSessionCookieConfig();

      ResponseCookie cookie =
          ResponseCookie.from(COOKIE_NAME, String.valueOf(expiresEpochSecond))
              .maxAge(maxInactiveInterval)
              .path("/")
              .httpOnly(false)
              .secure(sessionCookieConfig.isSecure())
              .sameSite(sessionCookieConfig.getAttribute("SameSite"))
              .build();

      response.addHeader("Set-Cookie", cookie.toString());
    }
  }
}
