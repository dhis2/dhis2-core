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
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_REPORT_ONLY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.FRAME_ANCESTORS_DEFAULT_CSP;
import static org.hisp.dhis.security.utils.CspConstants.OPENAPI_DOCS_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.USER_UPLOADED_CONTENT_CSP_POLICY;

import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Service that encapsulates CSP decision logic. All {@code Content-Security-Policy} strings are
 * referenced from {@link org.hisp.dhis.security.utils.CspConstants}; this service composes them
 * with the mandatory footer: {@code upgrade-insecure-requests} (HTTPS only), the dynamic {@code
 * frame-ancestors} directive derived from the configured CORS whitelist and, for app entrypoints,
 * the {@code report-uri}. Per-app policies are composed by {@link CspAppPolicyService} on top of
 * this footer.
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
    return appendFooter(DEFAULT_CSP_POLICY, null);
  }

  /**
   * Uploaded content is isolated even when general CSP is disabled. In that legacy mode, preserve
   * SAMEORIGIN framing rather than expanding access to the CORS whitelist.
   */
  public HttpHeaders getUserUploadedContentSecurityHeaders() {
    return buildSecurityHeaders(
        appendFooter(USER_UPLOADED_CONTENT_CSP_POLICY, null, dhisConfig.isEnabled(CSP_ENABLED)),
        null,
        true);
  }

  /**
   * The app-host baseline with footer, used for app resources that are neither an HTML entrypoint
   * nor a worker script (scripts, styles, images), which need no per-app lookup.
   */
  public String constructAppHostCspPolicy() {
    return appendFooter(APP_HOST_CSP_POLICY, null);
  }

  public String constructOpenApiDocsCspPolicy() {
    return appendFooter(OPENAPI_DOCS_CSP_POLICY, null);
  }

  /**
   * The enforced policy of a {@code REPORT_ONLY_EXEMPT} entrypoint: only the framing restriction
   * survives, everything else is reported but not blocked.
   */
  public String constructFrameAncestorsOnlyPolicy() {
    return getFrameAncestorsCspDirective();
  }

  public boolean isHttpsEnabled() {
    return dhisConfig.isEnabled(SERVER_HTTPS);
  }

  public boolean isCspEnabled() {
    return dhisConfig.isEnabled(CSP_ENABLED);
  }

  /**
   * Appends the mandatory footer to a policy body: {@code upgrade-insecure-requests} (HTTPS
   * deployments only), {@code frame-ancestors} and, when given, {@code report-uri}. No override can
   * change any of these.
   *
   * @param basePolicy the policy body without footer
   * @param reportUri the report endpoint, or {@code null} for none
   */
  public String appendFooter(String basePolicy, @CheckForNull String reportUri) {
    return appendFooter(basePolicy, reportUri, true);
  }

  /**
   * Returns the baseline security headers using the default CSP policy. Used by {@link
   * CspBaselineFilter} to seed every response before any handler-specific override.
   *
   * @return headers to apply via {@code response.setHeader}
   */
  public HttpHeaders getDefaultSecurityHeaders() {
    return buildSecurityHeaders(constructDefaultCspPolicy(), null, false);
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
    return getSecurityHeaders(cspPolicy, null);
  }

  /**
   * Like {@link #getSecurityHeaders(String)} and additionally emits {@code
   * Content-Security-Policy-Report-Only} when {@code reportOnlyPolicy} is given and CSP is enabled.
   */
  public HttpHeaders getSecurityHeaders(String cspPolicy, @CheckForNull String reportOnlyPolicy) {
    if (cspPolicy == null || cspPolicy.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "cspPolicy must not be null or blank; call getDefaultSecurityHeaders() for the baseline");
    }
    return buildSecurityHeaders(cspPolicy, reportOnlyPolicy, false);
  }

  private HttpHeaders buildSecurityHeaders(
      String cspPolicy, @CheckForNull String reportOnlyPolicy, boolean mandatory) {
    HttpHeaders headers = new HttpHeaders();

    boolean enabled = dhisConfig.isEnabled(CSP_ENABLED);
    if (enabled || mandatory) {
      String effectivePolicy = terminate(cspPolicy);
      headers.set(CONTENT_SECURITY_POLICY_HEADER_NAME, effectivePolicy);
      log.debug(
          "Applied CSP policy {} and standard security headers for response", effectivePolicy);
    }
    if (enabled && reportOnlyPolicy != null && !reportOnlyPolicy.isBlank()) {
      headers.set(CONTENT_SECURITY_POLICY_REPORT_ONLY_HEADER_NAME, terminate(reportOnlyPolicy));
    }
    if (!enabled) {
      // Mandatory upload CSP uses the same ancestor restriction as this legacy fallback.
      headers.set("X-Frame-Options", "SAMEORIGIN");
      log.debug("General CSP disabled; retaining SAMEORIGIN framing");
    }

    headers.set("X-Content-Type-Options", "nosniff");

    return headers;
  }

  private static String terminate(String policy) {
    return policy.endsWith(";") ? policy : policy + ";";
  }

  private String appendFooter(
      String basePolicy, @CheckForNull String reportUri, boolean allowConfiguredAncestors) {
    StringBuilder builder = new StringBuilder();
    if (basePolicy != null && !basePolicy.trim().isEmpty()) {
      String trimmed = basePolicy.trim();
      builder.append(trimmed);
      builder.append(trimmed.endsWith(";") ? " " : "; ");
    }
    // HTTP deployments must not upgrade requests to an unavailable TLS endpoint.
    if (dhisConfig.isEnabled(SERVER_HTTPS) && dhisConfig.isEnabled(CSP_UPGRADE_INSECURE_ENABLED)) {
      builder.append("upgrade-insecure-requests; ");
    }
    builder.append(
        allowConfiguredAncestors
            ? getFrameAncestorsCspDirective()
            : FRAME_ANCESTORS_DEFAULT_CSP + ";");
    if (reportUri != null) {
      builder.append(" report-uri ").append(reportUri).append(';');
    }
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
