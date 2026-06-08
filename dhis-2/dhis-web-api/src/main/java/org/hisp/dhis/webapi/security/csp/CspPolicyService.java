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
package org.hisp.dhis.webapi.security.csp;

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_UPGRADE_INSECURE_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.SERVER_HTTPS;
import static org.hisp.dhis.security.utils.CspConstants.APP_HOST_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.CARTODB_BASEMAP_HTTP_ORIGINS;
import static org.hisp.dhis.security.utils.CspConstants.CARTODB_BASEMAP_ORIGINS;
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.FRAME_ANCESTORS_DEFAULT_CSP;
import static org.hisp.dhis.security.utils.CspConstants.OPENAPI_DOCS_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.USER_UPLOADED_CONTENT_CSP_POLICY;

import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Service that encapsulates CSP decision logic. All {@code Content-Security-Policy} strings are
 * referenced from {@link org.hisp.dhis.security.utils.CspConstants}; this service composes them
 * with the dynamic {@code frame-ancestors} directive derived from the configured CORS whitelist.
 *
 * @see CspBaselineFilter
 * @see CspInterceptor
 * @author Austin McGee
 * @author Morten Svanæs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CspPolicyService {
  private final DhisConfigurationProvider dhisConfig;
  private final ConfigurationService configurationService;

  public String constructDefaultCspPolicy() {
    return appendFrameAncestors(DEFAULT_CSP_POLICY);
  }

  public String constructUserUploadedContentCspPolicy() {
    return appendFrameAncestors(USER_UPLOADED_CONTENT_CSP_POLICY);
  }

  public String constructAppHostCspPolicy() {
    String policy = APP_HOST_CSP_POLICY;
    // Dev allowance: when the server isn't configured for HTTPS (i.e. server.https=off, the
    // default in dhis.conf), the bundled Maps app's CartoDB tile fetches go out as http://
    // because the browser inherits the http://localhost page scheme. The CSP source-list check
    // runs against the pre-upgrade URL on Chrome, so the strict https-only allow-list rejects
    // them and basemaps don't render. Extend the allow-list with the http variants in that
    // case. Production (server.https=on) keeps the strict https-only policy.
    if (!dhisConfig.isEnabled(SERVER_HTTPS)) {
      policy =
          policy.replace(
              CARTODB_BASEMAP_ORIGINS,
              CARTODB_BASEMAP_ORIGINS + " " + CARTODB_BASEMAP_HTTP_ORIGINS);
    }
    return appendFrameAncestors(policy);
  }

  public String constructOpenApiDocsCspPolicy() {
    return appendFrameAncestors(OPENAPI_DOCS_CSP_POLICY);
  }

  /**
   * Returns the baseline security headers using the default CSP policy. Used by {@link
   * CspBaselineFilter} to seed every response before any handler-specific override.
   *
   * @return headers to apply via {@code response.setHeader}
   */
  public HttpHeaders getDefaultSecurityHeaders() {
    return buildSecurityHeaders(constructDefaultCspPolicy());
  }

  /**
   * Builds the security headers to attach to a response: {@code Content-Security-Policy} (when
   * {@link org.hisp.dhis.external.conf.ConfigurationKey#CSP_ENABLED} is enabled) plus {@code
   * X-Content-Type-Options}. When CSP is disabled, {@code X-Frame-Options: SAMEORIGIN} is emitted
   * as a legacy fallback; when CSP is enabled the {@code frame-ancestors} directive is the source
   * of truth and XFO is omitted to avoid conflicting with whitelisted external origins.
   *
   * @param cspPolicy a pre-composed policy string; must not be {@code null} or blank — call {@link
   *     #getDefaultSecurityHeaders()} for the baseline
   * @return headers to apply via {@code response.setHeader}
   * @throws IllegalArgumentException if {@code cspPolicy} is {@code null} or blank
   */
  public HttpHeaders getSecurityHeaders(String cspPolicy) {
    if (cspPolicy == null || cspPolicy.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "cspPolicy must not be null or blank; call getDefaultSecurityHeaders() for the baseline");
    }
    return buildSecurityHeaders(cspPolicy);
  }

  private HttpHeaders buildSecurityHeaders(String cspPolicy) {
    HttpHeaders headers = new HttpHeaders();

    if (dhisConfig.isEnabled(CSP_ENABLED)) {
      String effectivePolicy = cspPolicy.endsWith(";") ? cspPolicy : cspPolicy + ";";
      headers.set(CONTENT_SECURITY_POLICY_HEADER_NAME, effectivePolicy);
      log.debug(
          "Applied CSP policy {} and standard security headers for response", effectivePolicy);
    } else {
      // X-Frame-Options is a legacy fallback. When CSP is enabled the frame-ancestors
      // directive is the source of truth (and may legitimately whitelist external origins
      // via the CORS whitelist), so emitting XFO would conflict with it.
      headers.set("X-Frame-Options", "SAMEORIGIN");
      log.debug("CSP disabled; applying only standard security headers for response");
    }

    headers.set("X-Content-Type-Options", "nosniff");

    return headers;
  }

  private String appendFrameAncestors(String basePolicy) {
    StringBuilder builder = new StringBuilder();
    if (basePolicy != null && !basePolicy.trim().isEmpty()) {
      String trimmed = basePolicy.trim();
      builder.append(trimmed);
      builder.append(trimmed.endsWith(";") ? " " : "; ");
    }
    // upgrade-insecure-requests is config-gated: default ON, opt out via
    // csp.upgrade.insecure.enabled=off in dhis.conf for deployments that genuinely serve over
    // plain HTTP and need cross-origin http sub-resources to remain reachable.
    if (dhisConfig.isEnabled(CSP_UPGRADE_INSECURE_ENABLED)) {
      builder.append("upgrade-insecure-requests; ");
    }
    builder.append(getFrameAncestorsCspDirective());
    return builder.toString();
  }

  private String getFrameAncestorsCspDirective() {
    Set<String> corsWhitelist = configurationService.getCorsWhitelist();
    if (corsWhitelist == null || corsWhitelist.isEmpty()) {
      return FRAME_ANCESTORS_DEFAULT_CSP + ";";
    }
    String sortedOrigins = corsWhitelist.stream().sorted().collect(Collectors.joining(" "));
    return FRAME_ANCESTORS_DEFAULT_CSP + " " + sortedOrigins + ";";
  }
}
