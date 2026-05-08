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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Permanently redirects legacy {@code /dhis-web-login/...} requests to {@code /login/...}.
 *
 * <p>The login app moved from {@code /dhis-web-login/} to {@code /login/} in the new app-bundling
 * setup. Bookmarked links and password-restore emails still point at the old path; this filter
 * keeps them working with a 301 so the URL is updated client-side.
 */
@Component
public class LegacyDhisWebLoginRedirectFilter extends OncePerRequestFilter {

  static final String LEGACY_PREFIX = "/dhis-web-login";
  static final String NEW_PREFIX = "/login";

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain chain)
      throws IOException, ServletException {
    String contextPath = request.getContextPath();
    String requestUri = request.getRequestURI();
    String legacyBase = contextPath + LEGACY_PREFIX;

    if (requestUri == null || !requestUri.startsWith(legacyBase)) {
      chain.doFilter(request, response);
      return;
    }

    String tail = requestUri.substring(legacyBase.length());
    if (!tail.isEmpty() && tail.charAt(0) != '/') {
      chain.doFilter(request, response);
      return;
    }

    StringBuilder target = new StringBuilder(contextPath).append(NEW_PREFIX).append(tail);
    String query = request.getQueryString();
    if (query != null && !query.isEmpty()) {
      target.append('?').append(query);
    }

    response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    response.setHeader(HttpHeaders.LOCATION, target.toString());
  }
}
