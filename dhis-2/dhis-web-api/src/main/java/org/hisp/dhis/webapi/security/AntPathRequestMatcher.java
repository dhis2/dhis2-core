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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * Ant-style {@link RequestMatcher} backed by Spring's {@link AntPathMatcher}.
 *
 * <p>Spring Security 7.0 removed the framework AntPathRequestMatcher in favour of
 * PathPatternRequestMatcher. PathPattern does not support a double-star wildcard in the middle of a
 * pattern, whereas DHIS2 security rules rely on mid-pattern matches such as versioned API paths
 * under /api/{version}/loginConfig. This drop-in replacement preserves Ant matching semantics so
 * request-authorization behaviour is unchanged.
 *
 * <p>Matching uses servletPath + pathInfo (same path the removed Spring matcher used), with a
 * case-sensitive {@link AntPathMatcher} and token trimming disabled.
 *
 * @author Morten Svanæs
 */
public final class AntPathRequestMatcher implements RequestMatcher {
  /** Matches every request (the match-all pattern, as the original did). */
  public static final String MATCH_ALL = "/**";

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  static {
    PATH_MATCHER.setTrimTokens(false);
    PATH_MATCHER.setCaseSensitive(true);
  }

  private final String pattern;

  public AntPathRequestMatcher(String pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean matches(HttpServletRequest request) {
    if (MATCH_ALL.equals(pattern) || "**".equals(pattern)) {
      return true;
    }
    return PATH_MATCHER.match(pattern, getRequestPath(request));
  }

  private String getRequestPath(HttpServletRequest request) {
    String url = request.getServletPath();
    String pathInfo = request.getPathInfo();
    if (pathInfo != null) {
      url = StringUtils.hasLength(url) ? url + pathInfo : pathInfo;
    }
    return url;
  }

  @Override
  public String toString() {
    return "AntPathRequestMatcher [pattern='" + pattern + "']";
  }
}
