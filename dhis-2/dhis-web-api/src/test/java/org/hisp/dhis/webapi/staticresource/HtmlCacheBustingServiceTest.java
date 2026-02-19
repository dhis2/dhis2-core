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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppCacheConfig;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HtmlCacheBustingServiceTest {

  @Mock private DhisConfigurationProvider config;
  @Mock private Cache<String> cache;

  private HtmlCacheBustingService service;

  @BeforeEach
  void setUp() {
    service = new HtmlCacheBustingService(config, cache);
    lenient()
        .when(config.isEnabled(ConfigurationKey.STATIC_CACHE_HTML_REWRITE_ENABLED))
        .thenReturn(true);
    lenient().when(cache.getIfPresent(anyString())).thenReturn(Optional.empty());
  }

  @Test
  @DisplayName("Rewrites script src with cache-bust parameter")
  void rewritesScriptSrc() throws IOException {
    String html = "<html><head><script src=\"app.js\"></script></head><body></body></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"app.js?v=abc123\""));
  }

  @Test
  @DisplayName("Rewrites link href with cache-bust parameter")
  void rewritesLinkHref() throws IOException {
    String html =
        "<html><head><link href=\"style.css\" rel=\"stylesheet\"></head><body></body></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("href=\"style.css?v=abc123\""));
  }

  @Test
  @DisplayName("Rewrites img src with cache-bust parameter")
  void rewritesImgSrc() throws IOException {
    String html = "<html><body><img src=\"logo.png\"></body></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"logo.png?v=abc123\""));
  }

  @Test
  @DisplayName("Rewrites source src with cache-bust parameter")
  void rewritesSourceSrc() throws IOException {
    String html = "<html><body><video><source src=\"video.mp4\"></video></body></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"video.mp4?v=abc123\""));
  }

  @Test
  @DisplayName("Rewrites multiple asset types in one document")
  void rewritesMultipleAssets() throws IOException {
    String html =
        "<html><head>"
            + "<script src=\"app.js\"></script>"
            + "<link href=\"style.css\" rel=\"stylesheet\">"
            + "</head><body>"
            + "<img src=\"logo.png\">"
            + "</body></html>";
    App app = appWithCacheBustKey("xyz789");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"app.js?v=xyz789\""));
    assertThat(result, containsString("href=\"style.css?v=xyz789\""));
    assertThat(result, containsString("src=\"logo.png?v=xyz789\""));
  }

  @Test
  @DisplayName("Skips external HTTP URLs")
  void skipsExternalHttpUrls() throws IOException {
    String html =
        "<html><head><script src=\"https://cdn.example.com/lib.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"https://cdn.example.com/lib.js\""));
    assertThat(result, not(containsString("?v=")));
  }

  @Test
  @DisplayName("Skips protocol-relative URLs")
  void skipsProtocolRelativeUrls() throws IOException {
    String html = "<html><head><script src=\"//cdn.example.com/lib.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"//cdn.example.com/lib.js\""));
    assertThat(result, not(containsString("?v=")));
  }

  @Test
  @DisplayName("Skips data URIs")
  void skipsDataUris() throws IOException {
    String html = "<html><body><img src=\"data:image/png;base64,AAAA\"></body></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, not(containsString("?v=")));
  }

  @Test
  @DisplayName("Skips URLs that already have v= parameter")
  void skipsAlreadyBustedUrls() throws IOException {
    String html = "<html><head><script src=\"app.js?v=existing\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"app.js?v=existing\""));
    assertThat(result, not(containsString("v=abc123")));
  }

  @Test
  @DisplayName("Appends v= with & when URL already has query parameters")
  void appendsWithAmpersandWhenQueryExists() throws IOException {
    String html = "<html><head><script src=\"app.js?ts=123\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"app.js?ts=123&amp;v=abc123\""));
  }

  @Test
  @DisplayName("Returns original stream when cacheBustKey is null")
  void returnsOriginalWhenNoCacheBustKey() throws IOException {
    String html = "<html><head><script src=\"app.js\"></script></head></html>";
    App app = new App();
    app.setName("my-app");
    app.setShortName("my-app");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"app.js\""));
    assertThat(result, not(containsString("?v=")));
  }

  @Test
  @DisplayName("Returns original stream when app is null")
  void returnsOriginalWhenAppIsNull() throws IOException {
    String html = "<html><head><script src=\"app.js\"></script></head></html>";

    String result = rewrite(html, null, "/apps/my-app/index.html");

    assertEquals(html, result);
  }

  @Test
  @DisplayName("Returns original stream when URI is not HTML")
  void returnsOriginalForNonHtmlUri() throws IOException {
    String content = "var x = 1;";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(content, app, "/apps/my-app/main.js");

    assertEquals(content, result);
  }

  @Test
  @DisplayName("Returns original stream when rewriting is disabled")
  void returnsOriginalWhenDisabled() throws IOException {
    when(config.isEnabled(ConfigurationKey.STATIC_CACHE_HTML_REWRITE_ENABLED)).thenReturn(false);

    String html = "<html><head><script src=\"app.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertEquals(html, result);
  }

  @Test
  @DisplayName("Returns cached HTML on second call")
  void returnsCachedHtmlOnSecondCall() throws IOException {
    String cachedHtml = "<html><head><script src=\"app.js?v=abc123\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");
    String cacheKey = app.getKey() + ":" + app.getCacheBustKey() + ":/apps/my-app/index.html";

    when(cache.getIfPresent(cacheKey)).thenReturn(Optional.of(cachedHtml));

    String html = "<html><head><script src=\"app.js\"></script></head></html>";
    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertEquals(cachedHtml, result);
    verify(cache, never()).put(anyString(), any());
  }

  @Test
  @DisplayName("Caches rewritten HTML on first call")
  void cachesRewrittenHtmlOnFirstCall() throws IOException {
    String html = "<html><head><script src=\"app.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");
    String cacheKey = app.getKey() + ":" + app.getCacheBustKey() + ":/apps/my-app/index.html";

    when(cache.getIfPresent(cacheKey)).thenReturn(Optional.empty());

    rewrite(html, app, "/apps/my-app/index.html");

    verify(cache).put(anyString(), any());
  }

  @Test
  @DisplayName("Handles plugin.html URI")
  void handlesPluginHtmlUri() throws IOException {
    String html = "<html><head><script src=\"plugin.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/plugin.html");

    assertThat(result, containsString("src=\"plugin.js?v=abc123\""));
  }

  @Test
  @DisplayName("Handles URI ending with slash")
  void handlesUriEndingWithSlash() throws IOException {
    String html = "<html><head><script src=\"app.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");

    String result = rewrite(html, app, "/apps/my-app/");

    assertThat(result, containsString("src=\"app.js?v=abc123\""));
  }

  @Test
  @DisplayName("Skips rewriting when app opts out via dhis2-cache.json")
  void skipsRewriteWhenAppOptsOut() throws IOException {
    String html = "<html><head><script src=\"app.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");
    app.setCacheConfig(new AppCacheConfig(List.of(), null, false));

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"app.js\""));
    assertThat(result, not(containsString("?v=")));
  }

  @Test
  @DisplayName("Rewrites when app cache config has htmlRewriteEnabled=true")
  void rewritesWhenAppExplicitlyEnabled() throws IOException {
    String html = "<html><head><script src=\"app.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");
    app.setCacheConfig(new AppCacheConfig(List.of(), null, true));

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"app.js?v=abc123\""));
  }

  @Test
  @DisplayName("Rewrites when app cache config has htmlRewriteEnabled=null (default)")
  void rewritesWhenHtmlRewriteEnabledIsNull() throws IOException {
    String html = "<html><head><script src=\"app.js\"></script></head></html>";
    App app = appWithCacheBustKey("abc123");
    app.setCacheConfig(new AppCacheConfig(List.of(), null, null));

    String result = rewrite(html, app, "/apps/my-app/index.html");

    assertThat(result, containsString("src=\"app.js?v=abc123\""));
  }

  @Test
  @DisplayName("invalidateAll clears the cache")
  void invalidateAllClearsCache() {
    service.invalidateAll();

    verify(cache).invalidateAll();
  }

  private App appWithCacheBustKey(String key) {
    App app = new App();
    app.setName("my-app");
    app.setShortName("my-app");
    app.setCacheBustKey(key);
    return app;
  }

  private String rewrite(String content, App app, String requestUri) throws IOException {
    InputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    InputStream result = service.rewriteIfNeeded(input, app, requestUri);
    return new String(result.readAllBytes(), StandardCharsets.UTF_8);
  }
}
