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
import static org.hisp.dhis.security.utils.CspConstants.LOCKED_DIRECTIVES;
import static org.hisp.dhis.security.utils.CspConstants.OVERRIDABLE_DIRECTIVES;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.ResourceResult;
import org.hisp.dhis.artemis.audit.Audit;
import org.hisp.dhis.artemis.audit.AuditManager;
import org.hisp.dhis.audit.AuditScope;
import org.hisp.dhis.audit.AuditType;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.commons.jackson.config.JacksonObjectMapperConfig;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.mapping.ExternalMapLayer;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.webapi.security.csp.CspPolicyService;
import org.springframework.stereotype.Service;

/**
 * Reads, validates, stores and audits per-app CSP overrides, and builds the views of the {@code
 * /api/security/csp} API.
 *
 * @author Morten Svanæs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CspAppOverrideService {
  private static final ObjectMapper MAPPER = JacksonObjectMapperConfig.staticJsonMapper();

  private final CspAppOverrideStore store;
  private final CspAppPolicyService appPolicyService;
  private final CspPolicyService cspPolicyService;
  private final CspViolationService violationService;
  private final AppManager appManager;
  private final IdentifiableObjectManager idObjectManager;
  private final SystemSettingsProvider settingsProvider;
  private final AuditManager auditManager;

  // -------------------------------------------------------------------------
  // Views
  // -------------------------------------------------------------------------

  public record Summary(
      @JsonProperty boolean reportingEnabled,
      @JsonProperty boolean httpsOnly,
      @JsonProperty List<AppSummary> apps) {}

  public record AppSummary(
      @JsonProperty String appKey,
      @JsonProperty String name,
      @JsonProperty boolean bundled,
      @JsonProperty boolean hasBaseOverride,
      @JsonProperty boolean hasOverride,
      @JsonProperty boolean enabled,
      @JsonProperty List<String> overriddenEntrypoints,
      @JsonProperty boolean hasExemptEntrypoint,
      @JsonProperty boolean hasUnsafeDirectives,
      @JsonProperty List<String> unsafeTokens,
      @JsonProperty boolean hasActiveDraft,
      @JsonProperty String updatedAt,
      @JsonProperty String updatedBy,
      @JsonProperty CspViolationService.Counts violations24h) {}

  public record Suggestion(
      @JsonProperty String directive,
      @JsonProperty String source,
      @JsonProperty String reason,
      @JsonProperty String detail,
      @JsonProperty boolean alreadyAllowed) {}

  public record EffectiveView(
      @JsonProperty CspEnforcementMode enforcementMode,
      @JsonProperty String contentSecurityPolicy,
      @JsonProperty String contentSecurityPolicyReportOnly,
      @JsonProperty boolean draftActive,
      @JsonProperty boolean draftTestActive) {
    static EffectiveView of(CspAppPolicyService.Effective e) {
      return new EffectiveView(
          e.enforcementMode(),
          e.contentSecurityPolicy(),
          e.contentSecurityPolicyReportOnly(),
          e.draftActive(),
          e.draftTestActive());
    }
  }

  public record Detail(
      @JsonProperty String appKey,
      @JsonProperty String name,
      @JsonProperty boolean bundled,
      @JsonProperty Map<String, String> entrypoints,
      @JsonProperty List<String> overridableDirectives,
      @JsonProperty List<String> lockedDirectives,
      @JsonProperty Map<String, Set<String>> baseline,
      @JsonProperty Map<String, Map<String, List<String>>> baseOverrides,
      @JsonProperty List<Suggestion> suggestions,
      @JsonProperty CspAppOverride override,
      @JsonProperty Map<String, EffectiveView> effective,
      @JsonProperty List<CspViolationService.Violation> recentViolations) {}

  public record ValidateResponse(
      @JsonProperty boolean valid,
      @JsonProperty List<CspValidationError> errors,
      @JsonProperty List<String> warnings,
      @JsonProperty boolean requiresConfirmUnsafe,
      @JsonProperty boolean requiresConfirmExempt,
      @JsonProperty List<String> unsafeTokens,
      @JsonProperty Map<String, EffectiveView> effective) {}

  /** A rejected save: HTTP status plus the validation response body. */
  @Getter
  public static class RejectedException extends Exception {
    private final int status;
    private final ValidateResponse body;

    RejectedException(int status, ValidateResponse body) {
      super("CSP override rejected");
      this.status = status;
      this.body = body;
    }
  }

  public Summary summary(String contextPath) {
    boolean https = cspPolicyService.isHttpsEnabled();
    Map<String, String> stored = store.getAll();
    Map<String, CspViolationService.Counts> counts = violationService.counts24h();
    List<AppSummary> apps = new ArrayList<>();
    List<App> installed = new ArrayList<>(appManager.getAllApps(contextPath));
    installed.sort(Comparator.comparing(App::getKey));
    for (App app : installed) {
      String key = app.getKey();
      CspAppOverride o = CspAppPolicyService.parse(key, stored.get(key));
      List<String> overridden =
          o == null ? List.of() : new ArrayList<>(o.entrypointsOrEmpty().keySet());
      boolean activeDraft =
          o != null
              && o.entrypointsOrEmpty().values().stream()
                  .anyMatch(ep -> ep != null && ep.draftOrEmpty().enabled());
      apps.add(
          new AppSummary(
              key,
              app.getDisplayName() != null ? app.getDisplayName() : app.getName(),
              app.isBundled(),
              !CspBaseOverrides.forApp(key, https).isEmpty(),
              o != null,
              o != null && o.enabled(),
              overridden,
              o != null && o.hasExemptEntrypoint(),
              o != null && o.hasUnsafeDirectives(),
              o == null || o.unsafeTokens() == null ? List.of() : o.unsafeTokens(),
              activeDraft,
              o == null ? null : o.updatedAt(),
              o == null ? null : o.updatedBy(),
              counts.getOrDefault(key, CspViolationService.Counts.ZERO)));
    }
    return new Summary(violationService.isReportingEnabled(), https, apps);
  }

  public Detail detail(String appKey, String contextPath) throws NotFoundException {
    App app = requireApp(appKey, contextPath);
    String key = app.getKey();
    boolean https = cspPolicyService.isHttpsEnabled();
    CspAppOverride override = CspAppPolicyService.parse(key, store.get(key).orElse(null));
    Instant now = Instant.now();
    return new Detail(
        key,
        app.getDisplayName() != null ? app.getDisplayName() : app.getName(),
        app.isBundled(),
        entrypointPaths(app, contextPath),
        OVERRIDABLE_DIRECTIVES,
        LOCKED_DIRECTIVES,
        policyMap(CspPolicy.parse(APP_HOST_CSP_POLICY)),
        CspBaseOverrides.forApp(key, https),
        suggestions(key, override),
        override,
        effective(key, override, now, contextPath),
        violationService.recent(key, 20));
  }

  public ValidateResponse validate(
      String appKey, CspAppOverride.Request request, String contextPath) throws NotFoundException {
    App app = requireApp(appKey, contextPath);
    Instant now = Instant.now();
    CspOverrideValidator.Result result = runValidation(app.getKey(), request, now);
    Map<String, EffectiveView> effective =
        result.valid()
            ? effective(
                app.getKey(), toDocument(app.getKey(), request, result, now), now, contextPath)
            : Map.of();
    return response(result, request, effective, List.of());
  }

  /**
   * Validates and stores an override, evicts the cache so the next request uses it, and audits the
   * change.
   */
  public Map<String, Object> save(String appKey, CspAppOverride.Request request, String contextPath)
      throws NotFoundException, RejectedException {
    App app = requireApp(appKey, contextPath);
    String key = app.getKey();
    Instant now = Instant.now();
    CspOverrideValidator.Result result = runValidation(key, request, now);
    if (!result.valid()) {
      throw new RejectedException(400, response(result, request, Map.of(), List.of()));
    }
    List<CspValidationError> gates = new ArrayList<>();
    if (!result.unsafeTokens().isEmpty() && !request.confirmUnsafe()) {
      gates.add(
          new CspValidationError(
              null,
              null,
              String.join(" | ", result.unsafeTokens()),
              CspOverrideValidator.CONFIRM_UNSAFE_REQUIRED,
              "Unsafe sources need confirmUnsafe: true"));
    }
    if (result.exempt() && !request.confirmExempt()) {
      gates.add(
          new CspValidationError(
              null,
              null,
              null,
              CspOverrideValidator.CONFIRM_EXEMPT_REQUIRED,
              "REPORT_ONLY_EXEMPT needs confirmExempt: true"));
    }
    if (!gates.isEmpty()) {
      throw new RejectedException(409, response(result, request, Map.of(), gates));
    }
    CspAppOverride document = toDocument(key, request, result, now);
    String previous = store.get(key).orElse(null);
    store.put(key, CspAppPolicyService.toJson(document));
    appPolicyService.evict(key);
    audit(AuditType.UPDATE, key, document, previous != null);

    Map<String, Object> body =
        MAPPER.convertValue(document, new TypeReference<LinkedHashMap<String, Object>>() {});
    body.put("warnings", result.warnings());
    return body;
  }

  public boolean delete(String appKey, String contextPath) throws NotFoundException {
    App app = requireApp(appKey, contextPath);
    String key = app.getKey();
    boolean deleted = store.delete(key);
    appPolicyService.evict(key);
    if (deleted) {
      audit(AuditType.DELETE, key, null, true);
    }
    return deleted;
  }

  public App requireApp(String appKey, String contextPath) throws NotFoundException {
    for (App app : appManager.getAllApps(contextPath)) {
      if (appKey.equals(app.getKey())) {
        return app;
      }
    }
    throw new NotFoundException("App '" + appKey + "' not found.");
  }

  // -------------------------------------------------------------------------
  // Internals
  // -------------------------------------------------------------------------

  private CspOverrideValidator.Result runValidation(
      String key, CspAppOverride.Request request, Instant now) {
    boolean https = cspPolicyService.isHttpsEnabled();
    return CspOverrideValidator.validate(
        request.entrypointsOrEmpty(),
        request.approvalNote(),
        CspBaseOverrides.sourceKeys(key, https),
        https,
        now);
  }

  private static CspAppOverride toDocument(
      String key, CspAppOverride.Request request, CspOverrideValidator.Result result, Instant now) {
    String note = request.approvalNote() == null ? null : request.approvalNote().trim();
    return new CspAppOverride(
        CspAppOverride.SCHEMA_VERSION,
        key,
        request.enabled() == null || request.enabled(),
        now.toString(),
        CurrentUserUtil.getCurrentUsername(),
        null,
        note == null || note.isEmpty() ? null : note,
        !result.unsafeTokens().isEmpty(),
        result.exempt(),
        result.unsafeTokens(),
        result.normalized());
  }

  private static ValidateResponse response(
      CspOverrideValidator.Result result,
      CspAppOverride.Request request,
      Map<String, EffectiveView> effective,
      List<CspValidationError> gates) {
    List<CspValidationError> errors = new ArrayList<>(result.errors());
    errors.addAll(gates);
    return new ValidateResponse(
        errors.isEmpty(),
        errors,
        result.warnings(),
        !result.unsafeTokens().isEmpty() && !request.confirmUnsafe(),
        result.exempt() && !request.confirmExempt(),
        result.unsafeTokens(),
        effective);
  }

  private Map<String, EffectiveView> effective(
      String key, @CheckForNull CspAppOverride override, Instant now, String contextPath) {
    Map<String, EffectiveView> result = new LinkedHashMap<>();
    for (String ep : CspEntrypoints.KEYS) {
      result.put(
          ep, EffectiveView.of(appPolicyService.compose(key, ep, override, now, contextPath)));
    }
    return result;
  }

  private Map<String, String> entrypointPaths(App app, String contextPath) {
    String base = (contextPath == null ? "" : contextPath) + "/api/apps/" + app.getKey() + "/";
    Map<String, String> paths = new LinkedHashMap<>();
    String launch = app.getLaunchPath();
    paths.put(
        CspEntrypoints.APP,
        base + (launch == null || launch.isBlank() ? "index.html" : strip(launch)));
    if (app.getPluginLaunchPath() != null && !app.getPluginLaunchPath().isBlank()) {
      paths.put(CspEntrypoints.PLUGIN, base + strip(app.getPluginLaunchPath()));
    }
    if (hasServiceWorker(app)) {
      paths.put(CspEntrypoints.WORKER, base + CspEntrypoints.SERVICE_WORKER_FILE);
    }
    return paths;
  }

  private boolean hasServiceWorker(App app) {
    try {
      return appManager.getRawAppResource(app, CspEntrypoints.SERVICE_WORKER_FILE)
          instanceof ResourceResult.ResourceFound;
    } catch (IOException | RuntimeException e) {
      return false;
    }
  }

  private static String strip(String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }

  private static Map<String, Set<String>> policyMap(CspPolicy policy) {
    Map<String, Set<String>> map = new LinkedHashMap<>();
    for (String part : policy.toString().split(";")) {
      String[] tokens = part.trim().split("\\s+");
      if (tokens.length > 0 && !tokens[0].isEmpty()) {
        Set<String> values = new LinkedHashSet<>();
        for (int i = 1; i < tokens.length; i++) {
          values.add(tokens[i]);
        }
        map.put(tokens[0], values);
      }
    }
    return map;
  }

  /**
   * Origins the app is likely to need, from data the server already has. For Maps: the URL and
   * legend URL of stored external map layers and the provider hosts of configured Bing and Azure
   * keys. Suggestions never grant anything; an administrator has to add them.
   */
  List<Suggestion> suggestions(String key, @CheckForNull CspAppOverride override) {
    if (!CspBaseOverrides.MAPS_APP_KEY.equals(key)) {
      return List.of();
    }
    boolean https = cspPolicyService.isHttpsEnabled();
    CspPolicy app = appPolicyService.enforcedPolicy(key, CspEntrypoints.APP, override);
    Map<String, Suggestion> result = new LinkedHashMap<>();
    for (ExternalMapLayer layer : idObjectManager.getAll(ExternalMapLayer.class)) {
      addLayerSuggestions(result, app, layer, layer.getUrl(), "url", https);
      addLayerSuggestions(result, app, layer, layer.getLegendSetUrl(), "legendSetUrl", https);
    }
    SystemSettings settings = settingsProvider.getCurrentSettings();
    if (!settings.getBingMapsApiKey().isBlank()) {
      String detail = "Bing Maps API key is configured";
      addSuggestion(
          result, app, "connect-src", "https://dev.virtualearth.net", "BING_MAPS_KEY", detail);
      for (String d : List.of("img-src", "connect-src")) {
        addSuggestion(result, app, d, "https://*.tiles.virtualearth.net", "BING_MAPS_KEY", detail);
      }
    }
    if (!settings.getAzureMapsApiKey().isBlank()) {
      for (String d : List.of("img-src", "connect-src")) {
        addSuggestion(
            result,
            app,
            d,
            "https://atlas.microsoft.com",
            "AZURE_MAPS_KEY",
            "Azure Maps API key is configured");
      }
    }
    return new ArrayList<>(result.values());
  }

  private static void addLayerSuggestions(
      Map<String, Suggestion> result,
      CspPolicy app,
      ExternalMapLayer layer,
      @CheckForNull String url,
      String field,
      boolean https) {
    for (String origin : origins(url)) {
      if (!CspOverrideValidator.isAcceptableOnRead("img-src", origin, https)) {
        continue;
      }
      String detail = "External map layer '" + layer.getName() + "' (" + field + ")";
      for (String d : List.of("img-src", "connect-src")) {
        addSuggestion(result, app, d, origin, "EXTERNAL_MAP_LAYER", detail);
      }
    }
  }

  private static void addSuggestion(
      Map<String, Suggestion> result,
      CspPolicy app,
      String directive,
      String source,
      String reason,
      String detail) {
    result.putIfAbsent(
        directive + " " + source,
        new Suggestion(directive, source, reason, detail, app.allows(directive, source)));
  }

  /**
   * The origins of a (possibly templated) layer URL. A {@code {s}} subdomain placeholder expands to
   * the conventional {@code a}, {@code b}, {@code c}; other templates make the URL unusable.
   */
  static List<String> origins(@CheckForNull String url) {
    if (url == null || url.isBlank()) {
      return List.of();
    }
    String trimmed = url.trim();
    int schemeEnd = trimmed.indexOf("://");
    if (schemeEnd < 0) {
      return List.of();
    }
    String scheme = trimmed.substring(0, schemeEnd).toLowerCase();
    String rest = trimmed.substring(schemeEnd + 3);
    int end = rest.length();
    for (char c : new char[] {'/', '?', '#'}) {
      int i = rest.indexOf(c);
      if (i >= 0 && i < end) {
        end = i;
      }
    }
    String authority = rest.substring(0, end).toLowerCase();
    if (authority.contains("@")) {
      return List.of();
    }
    List<String> hosts =
        authority.contains("{s}")
            ? List.of(
                authority.replace("{s}", "a"),
                authority.replace("{s}", "b"),
                authority.replace("{s}", "c"))
            : List.of(authority);
    List<String> origins = new ArrayList<>();
    for (String host : hosts) {
      if (!host.contains("{")) {
        origins.add(scheme + "://" + host);
      }
    }
    return origins;
  }

  private void audit(
      AuditType type, String key, @CheckForNull CspAppOverride document, boolean existed) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("appKey", key);
    data.put("action", type == AuditType.DELETE ? "delete" : existed ? "update" : "create");
    if (document != null) {
      data.put("enabled", document.enabled());
      data.put("hasUnsafeDirectives", document.hasUnsafeDirectives());
      data.put("unsafeTokens", document.unsafeTokens());
      data.put("hasExemptEntrypoint", document.hasExemptEntrypoint());
      data.put("approvalNote", document.approvalNote());
      data.put("override", document.entrypointsOrEmpty());
    }
    String json;
    try {
      json = MAPPER.writeValueAsString(data);
    } catch (Exception e) {
      json = String.valueOf(data);
    }
    String username = CurrentUserUtil.getCurrentUsername();
    log.info(
        "CSP override of app '{}' {} by {}: unsafeTokens={}, exempt={}",
        key,
        data.get("action"),
        username,
        document == null ? List.of() : document.unsafeTokens(),
        document != null && document.hasExemptEntrypoint());
    auditManager.send(
        Audit.builder()
            .auditType(type)
            .auditScope(AuditScope.METADATA)
            .createdAt(LocalDateTime.now())
            .createdBy(username)
            .klass(CspAppOverride.class.getName())
            .code(key)
            .data(json)
            .build());
  }
}
