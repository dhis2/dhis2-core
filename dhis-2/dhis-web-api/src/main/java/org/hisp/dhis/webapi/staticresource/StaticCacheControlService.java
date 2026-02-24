/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.staticresource;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppCacheConfig;
import org.hisp.dhis.appmanager.AppCacheConfig.CacheRule;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.system.SystemService;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

/**
 * Central service for computing and setting {@code Cache-Control} headers on all static resources
 * served by DHIS2 (apps, core JAR resources, logos). Every static-serving code path must delegate
 * to this service instead of setting headers directly.
 */
@Service
@RequiredArgsConstructor
public class StaticCacheControlService {

  private static final AntPathMatcher ANT = new AntPathMatcher();
  private static final Pattern HASHED_FILENAME =
      Pattern.compile(
          "\\.[0-9a-f]{8,}\\." // Webpack: name.abc12345.ext
              + "|-(?=[a-zA-Z0-9]*[0-9])[a-zA-Z0-9]{7,}\\."); // Vite/Rollup: name-Dhu2pmiS.ext

  private final DhisConfigurationProvider config;
  private final AppManager appManager;
  private final SystemService systemService;

  /**
   * Sets appropriate {@code Cache-Control} headers on the response for the given static resource
   * URI.
   *
   * @param response the HTTP response
   * @param requestUri the request URI (e.g. {@code /apps/dashboard/main.abc123.js})
   * @param appKey the app key if the resource belongs to an app, or {@code null} for core resources
   */
  public void setHeaders(
      HttpServletResponse response, String requestUri, @CheckForNull String appKey) {
    if (!isCacheEnabled() || isDevModeForceNoCache()) {
      response.setHeader("Cache-Control", CacheControl.noStore().getHeaderValue());
      return;
    }

    AppCacheConfig appConfig = resolveAppConfig(appKey);
    CacheControl cc = computeCacheControl(requestUri, appConfig);
    response.setHeader("Cache-Control", cc.getHeaderValue());
  }

  /**
   * Generates an ETag suitable for cache busting on upgrades. Includes the DHIS2 server version so
   * that any patch/release automatically invalidates browser caches.
   */
  public String generateETag(@CheckForNull App app, long lastModified, String uri) {
    String version = app != null ? app.getVersion() : getDhis2Version();
    AppCacheConfig cfg = app != null ? app.getCacheConfig() : null;
    String suffix = isImmutable(uri, cfg) ? "-immutable" : "";
    String source = version + "-" + lastModified + "-" + getDhis2Version() + suffix;
    return HashUtils.hashMD5(source.getBytes());
  }

  private CacheControl computeCacheControl(String uri, AppCacheConfig config) {
    if (matchesAnyNoCachePattern(uri) || matchesNoCacheRule(uri, config)) {
      return CacheControl.noStore();
    }

    if (isImmutable(uri, config)) {
      long immutableSeconds = getImmutableMaxAgeSeconds();
      return CacheControl.maxAge(immutableSeconds, TimeUnit.SECONDS).cachePublic();
    }

    Duration maxAge = resolveMaxAge(uri, config);
    CacheControl cc = CacheControl.maxAge(maxAge.getSeconds(), TimeUnit.SECONDS).cachePublic();
    if (shouldMustRevalidate(uri, config)) {
      cc = cc.mustRevalidate();
    }
    return cc;
  }

  private AppCacheConfig resolveAppConfig(@CheckForNull String appKey) {
    if (appKey == null) return null;
    App app = appManager.getApp(appKey);
    return app != null ? app.getCacheConfig() : null;
  }

  private boolean matchesAnyNoCachePattern(String uri) {
    String patterns = config.getProperty(ConfigurationKey.STATIC_CACHE_ALWAYS_NO_CACHE_PATTERNS);
    if (patterns == null || patterns.isBlank()) return false;
    for (String pattern : patterns.split(",")) {
      String trimmed = pattern.trim();
      if (!trimmed.isEmpty() && matchesPattern(trimmed, uri)) {
        return true;
      }
    }
    return false;
  }

  private boolean matchesNoCacheRule(String uri, @CheckForNull AppCacheConfig config) {
    if (config == null) return false;
    return config.getRules().stream()
        .anyMatch(
            r ->
                r.getMaxAgeSeconds() != null
                    && r.getMaxAgeSeconds() == 0
                    && matchesPattern(r.getPattern(), uri));
  }

  private boolean isImmutable(String uri, @CheckForNull AppCacheConfig config) {
    if (config != null) {
      boolean ruleMatch =
          config.getRules().stream()
              .anyMatch(
                  r ->
                      Boolean.TRUE.equals(r.getImmutable()) && matchesPattern(r.getPattern(), uri));
      if (ruleMatch) return true;
    }
    return HASHED_FILENAME.matcher(uri).find();
  }

  private Duration resolveMaxAge(String uri, @CheckForNull AppCacheConfig config) {
    if (config != null) {
      List<CacheRule> rules = config.getRules();
      for (CacheRule rule : rules) {
        if (rule.getMaxAgeSeconds() != null && matchesPattern(rule.getPattern(), uri)) {
          return Duration.ofSeconds(rule.getMaxAgeSeconds());
        }
      }
      if (config.getDefaultMaxAgeSeconds() != null) {
        return Duration.ofSeconds(config.getDefaultMaxAgeSeconds());
      }
    }
    if (isHtmlPath(uri)) {
      return Duration.ofSeconds(getHtmlMaxAgeSeconds());
    }
    return Duration.ofSeconds(getDefaultMaxAgeSeconds());
  }

  private boolean shouldMustRevalidate(String uri, @CheckForNull AppCacheConfig config) {
    if (isHtmlPath(uri)) return true;
    if (config == null) return false;
    return config.getRules().stream()
        .anyMatch(
            r -> Boolean.TRUE.equals(r.getMustRevalidate()) && matchesPattern(r.getPattern(), uri));
  }

  /**
   * Matches an Ant-style glob against a request URI. Handles the common case where glob patterns
   * (e.g. {@code ** /*.html}) lack a leading {@code /} but request URIs always start with one.
   */
  private static boolean matchesPattern(String pattern, String uri) {
    if (ANT.match(pattern, uri)) return true;
    if (uri.startsWith("/")) return ANT.match(pattern, uri.substring(1));
    return false;
  }

  private boolean isHtmlPath(String uri) {
    return uri.endsWith(".html") || uri.endsWith("/");
  }

  private boolean isCacheEnabled() {
    return config.isEnabled(ConfigurationKey.STATIC_CACHE_ENABLED);
  }

  private boolean isDevModeForceNoCache() {
    return config.isEnabled(ConfigurationKey.STATIC_CACHE_DEV_MODE_FORCE_NO_CACHE);
  }

  private long getDefaultMaxAgeSeconds() {
    return Long.parseLong(config.getProperty(ConfigurationKey.STATIC_CACHE_DEFAULT_MAX_AGE));
  }

  private long getHtmlMaxAgeSeconds() {
    return Long.parseLong(config.getProperty(ConfigurationKey.STATIC_CACHE_HTML_MAX_AGE));
  }

  private long getImmutableMaxAgeSeconds() {
    return Long.parseLong(config.getProperty(ConfigurationKey.STATIC_CACHE_IMMUTABLE_MAX_AGE));
  }

  private String getDhis2Version() {
    String version = systemService.getSystemInfoVersion();
    return version != null ? version : "unknown";
  }
}
