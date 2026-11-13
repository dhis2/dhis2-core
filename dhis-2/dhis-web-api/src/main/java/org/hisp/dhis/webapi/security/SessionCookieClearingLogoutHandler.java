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

import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hisp.dhis.webapi.filter.SessionTimeoutHeaderFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

/**
 * Clears the session cookie and the {@link SessionTimeoutHeaderFilter#COOKIE_NAME} cookie on
 * logout.
 *
 * <p>The expired cookies carry the same {@code Path}, {@code SameSite}, {@code HttpOnly} and {@code
 * Secure} attributes as the cookies they replace. Spring's {@code CookieClearingLogoutHandler}
 * writes them without {@code SameSite}, which browsers may treat differently from the cookie that
 * was set.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
public class SessionCookieClearingLogoutHandler implements LogoutHandler {
  private static final String DEFAULT_SESSION_COOKIE_NAME = "JSESSIONID";

  private static final String SAME_SITE = "SameSite";

  @Override
  public void logout(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
    SessionCookieConfig config = request.getServletContext().getSessionCookieConfig();
    response.addHeader(HttpHeaders.SET_COOKIE, expiredSessionCookie(request, config).toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE, SessionTimeoutHeaderFilter.createCookie(config, "", 0).toString());
  }

  private static ResponseCookie expiredSessionCookie(
      HttpServletRequest request, SessionCookieConfig config) {
    String name =
        StringUtils.hasText(config.getName()) ? config.getName() : DEFAULT_SESSION_COOKIE_NAME;
    return ResponseCookie.from(name, "")
        .maxAge(0)
        .path(sessionCookiePath(request, config))
        .httpOnly(config.isHttpOnly())
        // the servlet container also marks the session cookie secure on secure requests
        .secure(config.isSecure() || request.isSecure())
        .sameSite(config.getAttribute(SAME_SITE))
        .build();
  }

  private static String sessionCookiePath(HttpServletRequest request, SessionCookieConfig config) {
    if (StringUtils.hasText(config.getPath())) {
      return config.getPath();
    }
    String contextPath = request.getContextPath();
    return StringUtils.hasText(contextPath) ? contextPath : "/";
  }
}
