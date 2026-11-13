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
package org.hisp.dhis.webapi.security.csp.app;

import static org.hisp.dhis.security.utils.CspConstants.APP_HOST_CSP_POLICY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.CheckForNull;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheProvider;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.webapi.security.csp.CspPolicyService;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.Draft;
import org.hisp.dhis.webapi.security.csp.app.CspAppOverride.EntrypointOverride;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Composes the effective CSP of an app entrypoint:
 *
 * <ol>
 *   <li>the app-host baseline ({@link
 *       org.hisp.dhis.security.utils.CspConstants#APP_HOST_CSP_POLICY}),
 *   <li>the core base overrides of the app ({@link CspBaseOverrides}), minus the base sources the
 *       administrator disabled for the entrypoint,
 *   <li>the administrator additions of {@code "*"} and the specific entrypoint (stored overrides
 *       are re-validated on read; values that fail now are dropped),
 *   <li>the mandatory footer ({@code upgrade-insecure-requests}, {@code frame-ancestors}, {@code
 *       report-uri}) from {@link CspPolicyService}.
 * </ol>
 *
 * <p>A {@code REPORT_ONLY_EXEMPT} entrypoint enforces only {@code frame-ancestors} and reports the
 * composed policy. An enabled report-only draft adds a {@code Content-Security-Policy-Report-Only}
 * header with the candidate; while the draft test window ({@code expiresAt}) is open its additions
 * are enforced instead.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Component
public class CspAppPolicyService {
  private static final ObjectMapper MAPPER = JacksonObjectMapperConfig.staticJsonMapper();

  private final CspPolicyService cspPolicyService;
  private final CspAppOverrideStore store;
  private final CspReportUriProvider reportUriProvider;
  private final Cache<String> overrideCache;
  private final Set<String> warnedDrops = ConcurrentHashMap.newKeySet();

  public CspAppPolicyService(
      CspPolicyService cspPolicyService,
      CspAppOverrideStore store,
      CspReportUriProvider reportUriProvider,
      CacheProvider cacheProvider) {
    this.cspPolicyService = cspPolicyService;
    this.store = store;
    this.reportUriProvider = reportUriProvider;
    this.overrideCache = cacheProvider.createCspAppOverrideCache();
  }

  /** The effective policy headers of an app entrypoint, for {@code CspInterceptor}. */
  public HttpHeaders getAppSecurityHeaders(String appKey, String entrypoint, String contextPath) {
    Effective effective =
        compose(appKey, entrypoint, getOverride(appKey), Instant.now(), contextPath);
    return cspPolicyService.getSecurityHeaders(
        effective.contentSecurityPolicy(), effective.contentSecurityPolicyReportOnly());
  }

  /** The stored override of an app (cached), or {@code null}. */
  @CheckForNull
  public CspAppOverride getOverride(String appKey) {
    String json = overrideCache.get(appKey, key -> store.get(key).orElse(""));
    return parse(appKey, json);
  }

  @CheckForNull
  static CspAppOverride parse(String appKey, @CheckForNull String json) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return MAPPER.readValue(json, CspAppOverride.class);
    } catch (JsonProcessingException ex) {
      log.warn("Ignoring unreadable CSP override of app {}", appKey);
      return null;
    }
  }

  static String toJson(CspAppOverride override) {
    try {
      return MAPPER.writeValueAsString(override);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Failed to serialize CSP override", ex);
    }
  }

  public void evict(String appKey) {
    overrideCache.invalidate(appKey);
  }

  /**
   * The composed headers of one entrypoint.
   *
   * @param enforcementMode the effective mode
   * @param contentSecurityPolicy the enforced header value
   * @param contentSecurityPolicyReportOnly the report-only header value, or {@code null}
   * @param draftActive whether a report-only draft is enabled
   * @param draftTestActive whether the draft additions are currently enforced
   */
  public record Effective(
      CspEnforcementMode enforcementMode,
      String contentSecurityPolicy,
      String contentSecurityPolicyReportOnly,
      boolean draftActive,
      boolean draftTestActive) {}

  public Effective compose(
      String appKey,
      String entrypoint,
      @CheckForNull CspAppOverride override,
      Instant now,
      String contextPath) {
    List<EntrypointOverride> layers = layers(override, entrypoint);
    CspPolicy policy = enforcedPolicy(appKey, entrypoint, layers);
    String reportUri = reportUriProvider.reportUri(contextPath, appKey, entrypoint);
    CspEnforcementMode mode = mode(layers);

    if (mode == CspEnforcementMode.REPORT_ONLY_EXEMPT) {
      return new Effective(
          mode,
          cspPolicyService.constructFrameAncestorsOnlyPolicy(),
          reportOnly(policy, reportUri),
          false,
          false);
    }

    List<Draft> drafts =
        layers.stream().map(EntrypointOverride::draftOrEmpty).filter(Draft::enabled).toList();
    if (drafts.isEmpty()) {
      return new Effective(
          mode, cspPolicyService.appendFooter(policy.toString(), reportUri), null, false, false);
    }

    boolean https = cspPolicyService.isHttpsEnabled();
    boolean testActive = drafts.stream().anyMatch(d -> isFuture(d.expiresAt(), now));
    if (testActive) {
      CspPolicy tested = policy.copy();
      drafts.forEach(d -> addSources(tested, appKey, d.addOrEmpty(), https));
      return new Effective(
          mode, cspPolicyService.appendFooter(tested.toString(), reportUri), null, true, true);
    }
    CspPolicy candidate = policy.copy();
    drafts.forEach(d -> addSources(candidate, appKey, d.addOrEmpty(), https));
    Map<String, Set<String>> base = CspBaseOverrides.forEntrypoint(appKey, entrypoint, https);
    drafts.forEach(
        d ->
            d.disabledOrEmpty()
                .forEach(
                    entry -> {
                      String[] parts = entry.split(" ", 2);
                      if (parts.length == 2
                          && base.getOrDefault(parts[0], Set.of()).contains(parts[1])) {
                        candidate.remove(parts[0], parts[1]);
                      }
                    }));
    return new Effective(
        mode,
        cspPolicyService.appendFooter(policy.toString(), reportUri),
        reportOnly(candidate, reportUri),
        true,
        false);
  }

  /** The enforced policy body (baseline + base overrides + admin additions), without footer. */
  public CspPolicy enforcedPolicy(
      String appKey, String entrypoint, @CheckForNull CspAppOverride override) {
    return enforcedPolicy(appKey, entrypoint, layers(override, entrypoint));
  }

  private CspPolicy enforcedPolicy(
      String appKey, String entrypoint, List<EntrypointOverride> layers) {
    boolean https = cspPolicyService.isHttpsEnabled();
    CspPolicy policy = CspPolicy.parse(APP_HOST_CSP_POLICY);
    Set<String> disabled = new LinkedHashSet<>();
    layers.forEach(l -> disabled.addAll(l.enforcedOrEmpty().disabledOrEmpty()));
    CspBaseOverrides.forEntrypoint(appKey, entrypoint, https)
        .forEach(
            (directive, sources) ->
                policy.add(
                    directive,
                    sources.stream()
                        .filter(s -> !disabled.contains(directive + " " + s))
                        .toList()));
    layers.forEach(l -> addSources(policy, appKey, l.enforcedOrEmpty().addOrEmpty(), https));
    return policy;
  }

  private static List<EntrypointOverride> layers(
      @CheckForNull CspAppOverride override, String entrypoint) {
    if (override == null || !override.enabled()) {
      return List.of();
    }
    List<EntrypointOverride> layers = new ArrayList<>(2);
    EntrypointOverride all = override.entrypoint(CspEntrypoints.ALL);
    if (all != null) {
      layers.add(all);
    }
    if (!CspEntrypoints.ALL.equals(entrypoint)) {
      EntrypointOverride specific = override.entrypoint(entrypoint);
      if (specific != null) {
        layers.add(specific);
      }
    }
    return layers;
  }

  /** The most specific explicitly set mode wins; the default is {@code ENFORCE}. */
  private static CspEnforcementMode mode(List<EntrypointOverride> layers) {
    CspEnforcementMode mode = CspEnforcementMode.ENFORCE;
    for (EntrypointOverride layer : layers) {
      if (layer.enforcementMode() != null) {
        mode = layer.enforcementMode();
      }
    }
    return mode;
  }

  private void addSources(
      CspPolicy policy, String appKey, Map<String, List<String>> add, boolean https) {
    add.forEach(
        (directive, values) -> {
          List<String> accepted = new ArrayList<>();
          for (String value : values == null ? List.<String>of() : values) {
            if (value != null && CspOverrideValidator.isAcceptableOnRead(directive, value, https)) {
              accepted.add(value);
            } else if (warnedDrops.add(appKey + "|" + directive)) {
              // The value itself is not logged: stored text may be arbitrary.
              log.warn(
                  "Dropping a stored CSP override value of app {} in {} that is not valid (anymore)",
                  appKey,
                  directive);
            }
          }
          policy.add(directive, accepted);
        });
  }

  private static String reportOnly(CspPolicy policy, @CheckForNull String reportUri) {
    String body = policy.toString();
    return reportUri == null ? body : body + " report-uri " + reportUri + ";";
  }

  private static boolean isFuture(@CheckForNull String instant, Instant now) {
    if (instant == null) {
      return false;
    }
    try {
      return Instant.parse(instant).isAfter(now);
    } catch (DateTimeParseException e) {
      return false;
    }
  }
}
