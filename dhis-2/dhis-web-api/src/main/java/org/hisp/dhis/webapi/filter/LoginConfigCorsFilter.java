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
import org.hisp.dhis.webapi.security.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet-level CORS for the security-ignored login bootstrap endpoint {@code
 * /api/**&#47;loginConfig}.
 *
 * <p>The endpoint is excluded from the Spring Security filter chain via {@code web.ignoring()}
 * (DHIS2-21909) so public login bootstrap never builds {@code UserDetails}. That also means the
 * security-level {@link CorsFilter} never runs for it, and browser apps on foreign origins (e.g. a
 * local app on {@code http://localhost:3000}) lose the {@code Access-Control-Allow-Origin} header
 * even when whitelisted.
 *
 * <p>This filter fills that gap outside the security chain, scoped to loginConfig only via {@link
 * #shouldNotFilter(HttpServletRequest)}. It delegates to a {@link CorsFilter} driven by the same
 * {@link DhisCorsProcessor} and system CORS whitelist as the rest of the API, so preflight requests
 * short-circuit with 204 and whitelist rules stay identical. All other paths are untouched,
 * avoiding double CORS headers on endpoints served by the security chain.
 *
 * <p>Registered as a servlet filter before the security chain in {@code
 * DhisWebApiWebAppInitializer#setupServlets}.
 *
 * @author Morten Svanæs
 */
@Component("loginConfigCorsFilter")
public class LoginConfigCorsFilter extends OncePerRequestFilter {

  /** Same Ant mid-pattern as the {@code web.ignoring()} matcher for loginConfig. */
  private static final AntPathRequestMatcher LOGIN_CONFIG =
      new AntPathRequestMatcher("/api/**/loginConfig");

  private final CorsFilter corsFilter;

  public LoginConfigCorsFilter(DhisCorsProcessor corsProcessor) {
    CorsFilter delegate = new CorsFilter(new UrlBasedCorsConfigurationSource());
    delegate.setCorsProcessor(corsProcessor);
    this.corsFilter = delegate;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !LOGIN_CONFIG.matches(request);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    corsFilter.doFilter(request, response, filterChain);
  }
}
