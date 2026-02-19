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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.annotation.CheckForNull;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppCacheConfig;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheBuilderProvider;
import org.hisp.dhis.common.event.ApplicationCacheClearedEvent;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Rewrites HTML entry points served by DHIS2 apps so that all relative asset URLs ({@code <script
 * src>}, {@code <link href>}, {@code <img src>}, {@code <source src>}) get a {@code ?v=XXXX}
 * cache-busting parameter appended. The rewritten HTML is cached in memory to avoid repeated
 * parsing.
 */
@Service
public class HtmlCacheBustingService {

  private static final long CACHE_MAX_SIZE = 500;
  private static final long CACHE_TTL_HOURS = 1;

  private final DhisConfigurationProvider config;
  private final Cache<String> rewrittenHtmlCache;

  @Autowired
  public HtmlCacheBustingService(
      DhisConfigurationProvider config, CacheBuilderProvider cacheBuilderProvider) {
    this.config = config;
    this.rewrittenHtmlCache =
        cacheBuilderProvider
            .<String>newCacheBuilder()
            .forRegion("htmlCacheBust")
            .expireAfterWrite(CACHE_TTL_HOURS, TimeUnit.HOURS)
            .withMaximumSize(CACHE_MAX_SIZE)
            .forceInMemory()
            .build();
  }

  HtmlCacheBustingService(DhisConfigurationProvider config, Cache<String> cache) {
    this.config = config;
    this.rewrittenHtmlCache = cache;
  }

  /**
   * Rewrites relative asset URLs in the given HTML stream with a cache-busting query parameter
   * derived from the app's {@code cacheBustKey}. Returns the original stream unchanged when
   * rewriting is disabled, the app has no cache-bust key, or the URI is not an HTML entry point.
   */
  public InputStream rewriteIfNeeded(InputStream original, @CheckForNull App app, String requestUri)
      throws IOException {
    if (!isEnabled()
        || !shouldRewrite(requestUri)
        || app == null
        || app.getCacheBustKey() == null
        || isAppOptedOut(app)) {
      return original;
    }

    String cacheKey = app.getKey() + ":" + app.getCacheBustKey() + ":" + requestUri;
    Optional<String> cached = rewrittenHtmlCache.getIfPresent(cacheKey);
    if (cached.isPresent()) {
      return toStream(cached.get());
    }

    String rewritten = doRewrite(original, app.getCacheBustKey());
    rewrittenHtmlCache.put(cacheKey, rewritten);
    return toStream(rewritten);
  }

  public void invalidateAll() {
    rewrittenHtmlCache.invalidateAll();
  }

  @EventListener
  public void onCacheCleared(ApplicationCacheClearedEvent event) {
    invalidateAll();
  }

  private boolean isAppOptedOut(App app) {
    AppCacheConfig cfg = app.getCacheConfig();
    return cfg != null && Boolean.FALSE.equals(cfg.getHtmlRewriteEnabled());
  }

  private boolean isEnabled() {
    return config.isEnabled(ConfigurationKey.STATIC_CACHE_HTML_REWRITE_ENABLED);
  }

  private boolean shouldRewrite(String uri) {
    return uri.endsWith(".html") || uri.endsWith("/");
  }

  private String doRewrite(InputStream stream, String bustKey) throws IOException {
    Document doc = Jsoup.parse(stream, StandardCharsets.UTF_8.name(), "");
    String param = "v=" + bustKey;

    for (Element el : doc.select("script[src], link[href], img[src], source[src]")) {
      String attr = el.hasAttr("src") ? "src" : "href";
      rewriteAttribute(el, attr, param);
    }

    return doc.outerHtml();
  }

  private void rewriteAttribute(Element el, String attr, String param) {
    String url = el.attr(attr).trim();
    if (url.isEmpty() || isExternal(url) || url.contains("?v=") || url.contains("&v=")) {
      return;
    }
    String separator = url.contains("?") ? "&" : "?";
    el.attr(attr, url + separator + param);
  }

  private boolean isExternal(String url) {
    return url.startsWith("http://")
        || url.startsWith("https://")
        || url.startsWith("//")
        || url.startsWith("data:");
  }

  private static InputStream toStream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }
}
