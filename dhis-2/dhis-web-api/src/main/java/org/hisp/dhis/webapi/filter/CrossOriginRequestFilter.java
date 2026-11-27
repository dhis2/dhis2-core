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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.render.RenderService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects browser requests that come from a web origin other than this server or an origin on the
 * CORS allowlist, with HTTP 403 and a {@code WebMessage} body. It runs in the Spring Security
 * filter chains before the CORS filter and before any authentication filter, so a rejected request
 * is never authenticated or executed. {@link LoginConfigCorsFilter} also runs it in front of the
 * security-ignored {@code loginConfig} endpoint.
 *
 * <p>Rules, evaluated per request:
 *
 * <ol>
 *   <li>If the {@code Origin} header is present, the origin must be accepted by {@link
 *       AllowedOriginMatcher}: the server's own origin or an allowlisted one. This applies to all
 *       HTTP methods.
 *   <li>{@code Origin: null} is accepted only together with {@code Sec-Fetch-Site: same-origin}.
 *       Browsers send this pair for a same-origin request from a page with the {@code no-referrer}
 *       referrer policy. Page scripts cannot set {@code Sec-Fetch-Site}, so the browser itself
 *       attests that the request is same-origin. Sandboxed frames and other opaque origins get
 *       {@code cross-site} and stay rejected, as does {@code Origin: null} without that header.
 *   <li>Without an {@code Origin} header, a state-changing request (any method except GET, HEAD,
 *       OPTIONS and TRACE) is rejected if its {@code Sec-Fetch-Site} header is present and is
 *       neither {@code same-origin} nor {@code none}.
 *   <li>Requests without both headers pass: non-browser clients and plain HTTP deployments, where
 *       browsers do not send {@code Sec-Fetch-*} headers.
 * </ol>
 *
 * <p>Configured in {@code dhis.conf} by {@link ConfigurationKey#CROSS_ORIGIN_PROTECTION_ENABLED}
 * and {@link ConfigurationKey#CROSS_ORIGIN_PROTECTION_LOG_ONLY}. The two settings only relax rule
 * 3: in log-only mode requests that rule 3 would reject are logged and let through, and with the
 * protection disabled rule 3 is not evaluated. Rules 1 and 2 apply in every mode, because the CORS
 * filter rejected requests from origins that are not allowed before this filter existed, and these
 * settings must never make the server weaker than that.
 *
 * <p>Each rejected request, and in log-only mode each request that would be rejected, increments
 * the counter {@value #REJECTED_COUNTER} on the {@code /api/metrics} endpoint. Its tags are {@code
 * mode} ({@code enforce} for a rejected request, {@code log_only} for a request that was only
 * logged) and {@code reason} ({@code origin_not_allowed} or {@code cross_site_without_origin}). The
 * origin, path and user are only logged, never used as tags, so the number of series stays fixed.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Component
public class CrossOriginRequestFilter extends OncePerRequestFilter {

  static final String REJECTED_MESSAGE = "Cross-origin request rejected: origin is not allowed";

  static final String REJECTED_COUNTER = "dhis2_cross_origin_rejected_requests_total";

  static final String TAG_MODE = "mode";

  static final String TAG_REASON = "reason";

  static final String MODE_ENFORCE = "enforce";

  static final String MODE_LOG_ONLY = "log_only";

  static final String SEC_FETCH_SITE = "Sec-Fetch-Site";

  /** Methods that must not change state, so a cross-site request using them is not rejected. */
  private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

  private static final String SAME_ORIGIN = "same-origin";

  /** {@code Sec-Fetch-Site} values of requests the user or this server's own pages initiated. */
  private static final Set<String> TRUSTED_FETCH_SITES = Set.of(SAME_ORIGIN, "none");

  /** Longest client-supplied value written to the log, so a client cannot flood it. */
  private static final int MAX_LOGGED_VALUE_LENGTH = 200;

  /** C0 and C1 control characters, among them CR, LF and NEL, which could start a new log line. */
  private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cc}");

  private enum Verdict {
    ALLOWED,
    ORIGIN_NOT_ALLOWED,
    CROSS_SITE_WITHOUT_ORIGIN;

    /** The reason as written to the log and to the {@code reason} tag of the counter. */
    String reason() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  private final AllowedOriginMatcher allowedOriginMatcher;

  private final RenderService renderService;

  /** Whether rule 3, the {@code Sec-Fetch-Site} rule for requests without an origin, is used. */
  private final boolean enabled;

  /** Whether rule 3 only logs instead of rejecting. */
  private final boolean logOnly;

  /** One counter per rejection verdict, empty when no meter registry is available. */
  private final Map<Verdict, Counter> rejectionCounters;

  @Autowired
  public CrossOriginRequestFilter(
      DhisConfigurationProvider config,
      AllowedOriginMatcher allowedOriginMatcher,
      RenderService renderService,
      ObjectProvider<MeterRegistry> meterRegistryProvider) {
    this(config, allowedOriginMatcher, renderService, meterRegistryProvider.getIfAvailable());
  }

  CrossOriginRequestFilter(
      DhisConfigurationProvider config,
      AllowedOriginMatcher allowedOriginMatcher,
      RenderService renderService,
      @CheckForNull MeterRegistry meterRegistry) {
    this.allowedOriginMatcher = allowedOriginMatcher;
    this.renderService = renderService;
    this.enabled = config.isEnabled(ConfigurationKey.CROSS_ORIGIN_PROTECTION_ENABLED);
    this.logOnly = config.isEnabled(ConfigurationKey.CROSS_ORIGIN_PROTECTION_LOG_ONLY);
    this.rejectionCounters = registerRejectionCounters(meterRegistry);
  }

  /**
   * Registers the counters up front, so they are exported with value 0 before the first rejection.
   * The configuration is fixed at startup, so each instance registers only the series it can
   * produce: {@code origin_not_allowed} always, {@code cross_site_without_origin} only when the
   * protection is enabled.
   */
  private Map<Verdict, Counter> registerRejectionCounters(@CheckForNull MeterRegistry registry) {
    Map<Verdict, Counter> counters = new EnumMap<>(Verdict.class);

    if (registry == null) {
      return counters;
    }

    for (Verdict verdict : Verdict.values()) {
      if (verdict == Verdict.ORIGIN_NOT_ALLOWED
          || (verdict == Verdict.CROSS_SITE_WITHOUT_ORIGIN && enabled)) {
        counters.put(
            verdict,
            Counter.builder(REJECTED_COUNTER)
                .description(
                    "Requests the cross-origin protection rejected, or in log-only mode would"
                        + " have rejected")
                .tag(TAG_MODE, rejects(verdict) ? MODE_ENFORCE : MODE_LOG_ONLY)
                .tag(TAG_REASON, verdict.reason())
                .register(registry));
      }
    }

    return counters;
  }

  /**
   * @return true if a request with this verdict is answered with 403, false if it continues down
   *     the filter chain
   */
  private boolean rejects(Verdict verdict) {
    return switch (verdict) {
      case ALLOWED -> false;
      case ORIGIN_NOT_ALLOWED -> true;
      case CROSS_SITE_WITHOUT_ORIGIN -> !logOnly;
    };
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

    boolean reject = rejects(verdict);
    logRejection(request, verdict, reject);
    countRejection(verdict);

    if (!reject) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    renderService.toJson(response.getOutputStream(), forbidden(REJECTED_MESSAGE));
  }

  private Verdict evaluate(HttpServletRequest request) {
    String origin = request.getHeader(HttpHeaders.ORIGIN);
    String fetchSite = request.getHeader(SEC_FETCH_SITE);

    if (!StringUtils.isEmpty(origin)) {
      return allowedOriginMatcher.isAllowed(request, origin)
              || isSameOriginNullOrigin(origin, fetchSite)
          ? Verdict.ALLOWED
          : Verdict.ORIGIN_NOT_ALLOWED;
    }

    if (!enabled
        || fetchSite == null
        || SAFE_METHODS.contains(request.getMethod())
        || TRUSTED_FETCH_SITES.contains(fetchSite.toLowerCase(Locale.ROOT))) {
      return Verdict.ALLOWED;
    }

    return Verdict.CROSS_SITE_WITHOUT_ORIGIN;
  }

  /**
   * @return true for {@code Origin: null} sent with {@code Sec-Fetch-Site: same-origin}, which only
   *     a browser can produce for a request from one of this server's own pages
   */
  private static boolean isSameOriginNullOrigin(String origin, @CheckForNull String fetchSite) {
    return AllowedOriginMatcher.NULL_ORIGIN.equalsIgnoreCase(origin)
        && SAME_ORIGIN.equalsIgnoreCase(fetchSite);
  }

  private void countRejection(Verdict verdict) {
    Counter counter = rejectionCounters.get(verdict);

    if (counter != null) {
      counter.increment();
    }
  }

  private void logRejection(HttpServletRequest request, Verdict verdict, boolean rejected) {
    log.warn(
        "{} cross-origin request: reason={}, method={}, path={}, origin={}, sec-fetch-site={},"
            + " self-origin={}. If this was a same-origin request behind a reverse proxy, check"
            + " that the proxy forwards the original Host header and X-Forwarded-Proto.",
        rejected ? "Rejected" : "Would reject (log-only mode)",
        verdict.reason(),
        sanitizeForLog(request.getMethod()),
        sanitizeForLog(request.getRequestURI()),
        headerForLog(request, HttpHeaders.ORIGIN),
        headerForLog(request, SEC_FETCH_SITE),
        sanitizeForLog(allowedOriginMatcher.getSelfOrigin(request)));
  }

  /** Absent headers are logged as {@code (absent)}, so they differ from {@code Origin: null}. */
  private static String headerForLog(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    return value == null ? "(absent)" : sanitizeForLog(value);
  }

  /**
   * Makes a client-supplied value safe for one log line. Any client can trigger the warning without
   * authentication, so control characters (including CR and LF) are replaced, which keeps a client
   * from forging log lines, and long values are truncated.
   */
  static String sanitizeForLog(String value) {
    String clean = CONTROL_CHARACTERS.matcher(value).replaceAll("_");
    return clean.length() > MAX_LOGGED_VALUE_LENGTH
        ? clean.substring(0, MAX_LOGGED_VALUE_LENGTH) + "..."
        : clean;
  }
}
