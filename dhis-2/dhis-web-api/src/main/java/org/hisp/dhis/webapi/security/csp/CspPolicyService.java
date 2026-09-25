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
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_MAP_SOURCES;
import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_UPGRADE_INSECURE_ENABLED;
import static org.hisp.dhis.external.conf.ConfigurationKey.SERVER_HTTPS;
import static org.hisp.dhis.security.utils.CspConstants.APP_HOST_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.FRAME_ANCESTORS_DEFAULT_CSP;
import static org.hisp.dhis.security.utils.CspConstants.MAPS_BASEMAP_HTTP_ORIGINS;
import static org.hisp.dhis.security.utils.CspConstants.MAPS_BASEMAP_ORIGINS;
import static org.hisp.dhis.security.utils.CspConstants.OPENAPI_DOCS_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.USER_UPLOADED_CONTENT_CSP_POLICY;

import com.google.common.base.Suppliers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  /**
   * A bare origin: {@code scheme://host[:port]}. Anything else, in particular userinfo, a path,
   * query, fragment, trailing slash, wildcard host or a source-list keyword, is rejected so a
   * configured value can neither break out of its directive nor widen it beyond one origin.
   */
  private static final Pattern MAP_SOURCE_ORIGIN =
      Pattern.compile(
          "(?i)^(?<scheme>https?)://[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
              + "(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)*(?::(?<port>\\d{1,5}))?$");

  private static final int MAX_PORT = 65535;

  private final DhisConfigurationProvider dhisConfig;
  private final ConfigurationService configurationService;

  /**
   * The app-host policy without {@code frame-ancestors}. Both inputs it depends on, {@code
   * server.https} and {@code csp.map.sources}, are read from {@code dhis.conf} and fixed for the
   * lifetime of the configuration provider, so the policy is composed and the configured origins
   * validated once instead of on every response. Rejected entries are therefore also logged once,
   * and editing {@code csp.map.sources} requires a server restart to take effect.
   */
  private final Supplier<String> appHostBasePolicy =
      Suppliers.memoize(this::buildAppHostBasePolicy);

  public String constructDefaultCspPolicy() {
    return appendFrameAncestors(DEFAULT_CSP_POLICY);
  }

  /**
   * Uploaded content is isolated even when general CSP is disabled. In that legacy mode, preserve
   * SAMEORIGIN framing rather than expanding access to the CORS whitelist.
   */
  public HttpHeaders getUserUploadedContentSecurityHeaders() {
    return buildSecurityHeaders(
        appendFrameAncestors(USER_UPLOADED_CONTENT_CSP_POLICY, dhisConfig.isEnabled(CSP_ENABLED)),
        true);
  }

  public String constructAppHostCspPolicy() {
    return appendFrameAncestors(appHostBasePolicy.get());
  }

  /**
   * Extends the built-in Maps basemap origins of {@link
   * org.hisp.dhis.security.utils.CspConstants#APP_HOST_CSP_POLICY} with the plain-HTTP variants on
   * non-TLS deployments and with the administrator-configured map sources. Both lists are appended
   * to {@code img-src} and {@code connect-src} only; no directive is added or removed.
   */
  private String buildAppHostBasePolicy() {
    boolean https = dhisConfig.isEnabled(SERVER_HTTPS);
    Set<String> extraOrigins = new LinkedHashSet<>();
    // Dev allowance: when the server isn't configured for HTTPS (i.e. server.https=off, the
    // default in dhis.conf), the bundled Maps app's tile fetches go out as http:// because the
    // browser inherits the http://localhost page scheme. The CSP source-list check runs against
    // the pre-upgrade URL on Chrome, so the strict https-only allow-list rejects them and
    // basemaps don't render. Extend the allow-list with the http variants in that case.
    // Production (server.https=on) keeps the strict https-only policy.
    if (!https) {
      extraOrigins.addAll(Arrays.asList(MAPS_BASEMAP_HTTP_ORIGINS.split(" ")));
    }
    extraOrigins.addAll(getConfiguredMapSources(https));
    extraOrigins.removeAll(Arrays.asList(MAPS_BASEMAP_ORIGINS.split(" ")));
    if (extraOrigins.isEmpty()) {
      return APP_HOST_CSP_POLICY;
    }
    return APP_HOST_CSP_POLICY.replace(
        MAPS_BASEMAP_ORIGINS, MAPS_BASEMAP_ORIGINS + " " + String.join(" ", extraOrigins));
  }

  /**
   * Parses {@link org.hisp.dhis.external.conf.ConfigurationKey#CSP_MAP_SOURCES}. Entries are
   * validated individually and fail closed: a malformed entry, or a plain-HTTP entry on an HTTPS
   * deployment, which this policy allows only while {@code server.https} is off, is dropped with a
   * warning while the remaining entries are kept.
   */
  private List<String> getConfiguredMapSources(boolean https) {
    String configured = dhisConfig.getProperty(CSP_MAP_SOURCES);
    if (configured == null || configured.isBlank()) {
      return List.of();
    }
    String[] entries = configured.split(",");
    List<String> origins = new ArrayList<>(entries.length);
    for (int i = 0; i < entries.length; i++) {
      String origin = entries[i].trim();
      if (!origin.isEmpty() && isValidMapSource(origin, i + 1, https)) {
        origins.add(origin);
      }
    }
    return origins;
  }

  private boolean isValidMapSource(String origin, int position, boolean https) {
    Matcher matcher = MAP_SOURCE_ORIGIN.matcher(origin);
    if (!matcher.matches()) {
      reject(position, "expected a bare origin on the form scheme://host[:port]");
      return false;
    }
    String port = matcher.group("port");
    if (port != null) {
      int portNumber = Integer.parseInt(port);
      if (portNumber < 1 || portNumber > MAX_PORT) {
        reject(position, "port is outside the range 1-" + MAX_PORT);
        return false;
      }
    }
    if (https && "http".equalsIgnoreCase(matcher.group("scheme"))) {
      reject(position, "plain HTTP map sources are only allowed when server.https is off");
      return false;
    }
    return true;
  }

  /**
   * Identifies a rejected entry by its position in the configured list. The entry itself is never
   * logged: it is unvalidated text that can carry credentials, query tokens or line breaks.
   */
  private void reject(int position, String reason) {
    log.warn("Ignoring {} entry number {}: {}", CSP_MAP_SOURCES.getKey(), position, reason);
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
    return buildSecurityHeaders(constructDefaultCspPolicy(), false);
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
    return buildSecurityHeaders(cspPolicy, false);
  }

  private HttpHeaders buildSecurityHeaders(String cspPolicy, boolean mandatory) {
    HttpHeaders headers = new HttpHeaders();

    boolean enabled = dhisConfig.isEnabled(CSP_ENABLED);
    if (enabled || mandatory) {
      String effectivePolicy = cspPolicy.endsWith(";") ? cspPolicy : cspPolicy + ";";
      headers.set(CONTENT_SECURITY_POLICY_HEADER_NAME, effectivePolicy);
      log.debug(
          "Applied CSP policy {} and standard security headers for response", effectivePolicy);
    }
    if (!enabled) {
      // Mandatory upload CSP uses the same ancestor restriction as this legacy fallback.
      headers.set("X-Frame-Options", "SAMEORIGIN");
      log.debug("General CSP disabled; retaining SAMEORIGIN framing");
    }

    headers.set("X-Content-Type-Options", "nosniff");

    return headers;
  }

  private String appendFrameAncestors(String basePolicy) {
    return appendFrameAncestors(basePolicy, true);
  }

  private String appendFrameAncestors(String basePolicy, boolean allowConfiguredAncestors) {
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
