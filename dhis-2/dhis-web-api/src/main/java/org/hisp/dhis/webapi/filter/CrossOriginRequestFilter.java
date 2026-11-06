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

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.forbidden;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.render.RenderService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects browser requests that come from a web origin other than this server or an origin on the
 * CORS allowlist, with HTTP 403 and a {@code WebMessage} body. It runs in the Spring Security
 * filter chains before the CORS filter and before any authentication filter, so a rejected request
 * is never authenticated or executed.
 *
 * <p>Rules, evaluated per request:
 *
 * <ol>
 *   <li>If the {@code Origin} header is present, the origin must be accepted by {@link
 *       AllowedOriginMatcher}: the server's own origin or an allowlisted one. {@code Origin: null}
 *       is never accepted. This applies to all HTTP methods.
 *   <li>Without an {@code Origin} header, a state-changing request (any method except GET, HEAD,
 *       OPTIONS and TRACE) is rejected if its {@code Sec-Fetch-Site} header is present and is
 *       neither {@code same-origin} nor {@code none}.
 *   <li>Requests without both headers pass: non-browser clients and plain HTTP deployments, where
 *       browsers do not send {@code Sec-Fetch-*} headers.
 * </ol>
 *
 * <p>Configured in {@code dhis.conf} by {@link ConfigurationKey#CROSS_ORIGIN_PROTECTION_ENABLED}
 * and {@link ConfigurationKey#CROSS_ORIGIN_PROTECTION_LOG_ONLY}. In log-only mode requests that
 * would be rejected are logged and let through.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class CrossOriginRequestFilter extends OncePerRequestFilter {

  static final String REJECTED_MESSAGE = "Cross-origin request rejected: origin is not allowed";

  static final String SEC_FETCH_SITE = "Sec-Fetch-Site";

  /** Methods that must not change state, so a cross-site request using them is not rejected. */
  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  /** {@code Sec-Fetch-Site} values of requests the user or this server's own pages initiated. */
  private static final Set<String> TRUSTED_FETCH_SITES = Set.of("same-origin", "none");

  private enum Verdict {
    ALLOWED,
    ORIGIN_NOT_ALLOWED,
    CROSS_SITE_WITHOUT_ORIGIN
  }

  private final AllowedOriginMatcher allowedOriginMatcher;

  private final RenderService renderService;

  private final boolean enabled;

  private final boolean logOnly;

  public CrossOriginRequestFilter(
      DhisConfigurationProvider config,
      AllowedOriginMatcher allowedOriginMatcher,
      RenderService renderService) {
    this.allowedOriginMatcher = allowedOriginMatcher;
    this.renderService = renderService;
    this.enabled = config.isEnabled(ConfigurationKey.CROSS_ORIGIN_PROTECTION_ENABLED);
    this.logOnly = config.isEnabled(ConfigurationKey.CROSS_ORIGIN_PROTECTION_LOG_ONLY);
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !enabled;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Verdict verdict = evaluate(request);

    if (verdict == Verdict.ALLOWED) {
      filterChain.doFilter(request, response);
      return;
    }

    logRejection(request, verdict);

    if (logOnly) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    renderService.toJson(response.getOutputStream(), forbidden(REJECTED_MESSAGE));
  }

  private Verdict evaluate(HttpServletRequest request) {
    String origin = request.getHeader(HttpHeaders.ORIGIN);

    if (!StringUtils.isEmpty(origin)) {
      return allowedOriginMatcher.isAllowed(request, origin)
          ? Verdict.ALLOWED
          : Verdict.ORIGIN_NOT_ALLOWED;
    }

    String fetchSite = request.getHeader(SEC_FETCH_SITE);

    if (fetchSite == null
        || SAFE_METHODS.contains(request.getMethod())
        || TRUSTED_FETCH_SITES.contains(fetchSite.toLowerCase(Locale.ROOT))) {
      return Verdict.ALLOWED;
    }

    return Verdict.CROSS_SITE_WITHOUT_ORIGIN;
  }

  private void logRejection(HttpServletRequest request, Verdict verdict) {
    log.warn(
        "{} cross-origin request: reason={}, method={}, path={}, origin={}, sec-fetch-site={},"
            + " self-origin={}. If this was a same-origin request behind a reverse proxy, check"
            + " that the proxy forwards the original Host header and X-Forwarded-Proto.",
        logOnly ? "Would reject (log-only mode)" : "Rejected",
        verdict.name().toLowerCase(Locale.ROOT),
        request.getMethod(),
        request.getRequestURI(),
        headerForLog(request, HttpHeaders.ORIGIN),
        headerForLog(request, SEC_FETCH_SITE),
        allowedOriginMatcher.getSelfOrigin(request));
  }

  /** Absent headers are logged as {@code (absent)}, so they differ from {@code Origin: null}. */
  private static String headerForLog(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    return value == null ? "(absent)" : value;
  }
}
