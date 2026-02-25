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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppCacheConfig;
import org.hisp.dhis.appmanager.AppCacheConfig.CacheRule;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.system.SystemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class StaticCacheControlServiceTest {

  @Mock private DhisConfigurationProvider config;
  @Mock private AppManager appManager;
  @Mock private SystemService systemService;

  private StaticCacheControlService service;

  @BeforeEach
  void setUp() {
    service = new StaticCacheControlService(config, appManager, systemService);

    lenient().when(config.isEnabled(ConfigurationKey.STATIC_CACHE_ENABLED)).thenReturn(true);
    lenient()
        .when(config.isEnabled(ConfigurationKey.STATIC_CACHE_DEV_MODE_FORCE_NO_CACHE))
        .thenReturn(false);
    lenient()
        .when(config.getProperty(ConfigurationKey.STATIC_CACHE_DEFAULT_MAX_AGE))
        .thenReturn("3600");
    lenient()
        .when(config.getProperty(ConfigurationKey.STATIC_CACHE_HTML_MAX_AGE))
        .thenReturn("300");
    lenient()
        .when(config.getProperty(ConfigurationKey.STATIC_CACHE_IMMUTABLE_MAX_AGE))
        .thenReturn("31536000");
    lenient()
        .when(config.getProperty(ConfigurationKey.STATIC_CACHE_ALWAYS_NO_CACHE_PATTERNS))
        .thenReturn("**/*.html,**/index.*,**/manifest.*,**/config.*,**/plugin.html");
  }

  @Test
  @DisplayName("Cache disabled returns no-store")
  void cacheDisabled_returnsNoStore() {
    when(config.isEnabled(ConfigurationKey.STATIC_CACHE_ENABLED)).thenReturn(false);

    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/main.js", "dashboard");

    assertThat(response.getHeader("Cache-Control"), containsString("no-store"));
  }

  @Test
  @DisplayName("Dev mode force no-cache returns no-store")
  void devMode_returnsNoStore() {
    when(config.isEnabled(ConfigurationKey.STATIC_CACHE_DEV_MODE_FORCE_NO_CACHE)).thenReturn(true);

    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/main.js", "dashboard");

    assertThat(response.getHeader("Cache-Control"), containsString("no-store"));
  }

  @Test
  @DisplayName("HTML file matches always-no-cache pattern and returns no-store")
  void htmlFile_matchesNoCachePattern() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/index.html", null);

    assertThat(response.getHeader("Cache-Control"), containsString("no-store"));
  }

  @Test
  @DisplayName("Regular JS file gets default max-age with public caching")
  void regularJsFile_getsDefaultMaxAge() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/main.js", null);

    String cc = response.getHeader("Cache-Control");
    assertThat(cc, containsString("max-age=3600"));
    assertThat(cc, containsString("public"));
  }

  @Test
  @DisplayName("Hashed filename gets immutable treatment")
  void hashedFilename_getsImmutable() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/main.abc12345.js", null);

    String cc = response.getHeader("Cache-Control");
    assertThat(cc, containsString("max-age=31536000"));
    assertThat(cc, containsString("public"));
  }

  @Test
  @DisplayName("Vite hash with mixed case and digits gets immutable")
  void viteHash_mixedCaseDigits() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/dhis-web-messaging/assets/main-Dhu2pmiS.js", null);

    assertThat(response.getHeader("Cache-Control"), containsString("max-age=31536000"));
  }

  @Test
  @DisplayName("Vite hash with all-letter mixed case gets immutable")
  void viteHash_allLetterMixedCase() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/dhis-web-dashboard/assets/main-DHOiLmwl.js", null);

    assertThat(response.getHeader("Cache-Control"), containsString("max-age=31536000"));
  }

  @Test
  @DisplayName("Vite hash containing a dash (base64url) gets immutable")
  void viteHash_containsDash() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/dhis-web-dashboard/assets/main-D-tfNpnx.js", null);

    assertThat(response.getHeader("Cache-Control"), containsString("max-age=31536000"));
  }

  @Test
  @DisplayName("Vite hash containing underscore gets immutable")
  void viteHash_containsUnderscore() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/assets/App-B6NG_BIY.js", null);

    assertThat(response.getHeader("Cache-Control"), containsString("max-age=31536000"));
  }

  @Test
  @DisplayName("Vite hash with woff2 extension gets immutable")
  void viteHash_woff2Extension() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/assets/fonts/roboto-latin-500-DRg8azjQ.woff2", null);

    assertThat(response.getHeader("Cache-Control"), containsString("max-age=31536000"));
  }

  @Test
  @DisplayName("Normal dash-separated filename (all lowercase) is NOT treated as hashed")
  void dashSeparated_allLowercase_notHashed() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/main-component.js", null);

    assertThat(response.getHeader("Cache-Control"), containsString("max-age=3600"));
  }

  @Test
  @DisplayName("Normal multi-dash filename is NOT treated as hashed")
  void multiDash_normalFilename_notHashed() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/app-dashboard-plugin.js", null);

    assertThat(response.getHeader("Cache-Control"), containsString("max-age=3600"));
  }

  @Test
  @DisplayName("App cache config rule overrides default max-age")
  void appCacheConfig_overridesMaxAge() {
    App app = new App();
    app.setName("my-app");
    app.setShortName("my-app");
    CacheRule rule = new CacheRule("**/assets/**", 86400, null, null);
    app.setCacheConfig(new AppCacheConfig(List.of(rule), null, null));

    when(appManager.getApp("my-app")).thenReturn(app);

    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/my-app/assets/logo.png", "my-app");

    String cc = response.getHeader("Cache-Control");
    assertThat(cc, containsString("max-age=86400"));
  }

  @Test
  @DisplayName("App cache config immutable rule is honored")
  void appCacheConfig_immutableRule() {
    App app = new App();
    app.setName("my-app");
    app.setShortName("my-app");
    CacheRule rule = new CacheRule("**/static/**", null, true, null);
    app.setCacheConfig(new AppCacheConfig(List.of(rule), null, null));

    when(appManager.getApp("my-app")).thenReturn(app);

    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/my-app/static/bundle.js", "my-app");

    String cc = response.getHeader("Cache-Control");
    assertThat(cc, containsString("max-age=31536000"));
  }

  @Test
  @DisplayName("App cache config no-cache rule (maxAge=0) returns no-store")
  void appCacheConfig_noCacheRule() {
    App app = new App();
    app.setName("my-app");
    app.setShortName("my-app");
    CacheRule rule = new CacheRule("**/config.json", 0, null, null);
    app.setCacheConfig(new AppCacheConfig(List.of(rule), null, null));

    when(appManager.getApp("my-app")).thenReturn(app);

    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/my-app/config.json", "my-app");

    assertThat(response.getHeader("Cache-Control"), containsString("no-store"));
  }

  @Test
  @DisplayName("Core static resource (no app) gets default max-age")
  void coreStaticResource_getsDefaultMaxAge() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/dhis-web-commons/css/style.css", null);

    String cc = response.getHeader("Cache-Control");
    assertThat(cc, containsString("max-age=3600"));
    assertThat(cc, containsString("public"));
  }

  @Test
  @DisplayName("ETag includes DHIS2 version for cache busting")
  void eTag_includesDhis2Version() {
    when(systemService.getSystemInfoVersion()).thenReturn("2.42.0");

    App app = new App();
    app.setVersion("1.0.0");

    String etag1 = service.generateETag(app, 12345L, "/apps/myapp/main.js");

    when(systemService.getSystemInfoVersion()).thenReturn("2.42.1");
    String etag2 = service.generateETag(app, 12345L, "/apps/myapp/main.js");

    assertThat(
        "ETag must change when DHIS2 version changes", etag1, not(org.hamcrest.Matchers.is(etag2)));
  }

  @Test
  @DisplayName("ETag for null app uses DHIS2 version")
  void eTag_nullApp_usesDhis2Version() {
    when(systemService.getSystemInfoVersion()).thenReturn("2.42.0");

    String etag = service.generateETag(null, 99999L, "/dhis-web-commons/css/style.css");
    assertThat("ETag should not be null or empty", etag, not(org.hamcrest.Matchers.emptyString()));
  }

  @Test
  @DisplayName("App with defaultMaxAgeSeconds uses that as fallback")
  void appCacheConfig_defaultMaxAgeOverride() {
    App app = new App();
    app.setName("my-app");
    app.setShortName("my-app");
    app.setCacheConfig(new AppCacheConfig(List.of(), 7200, null));

    when(appManager.getApp("my-app")).thenReturn(app);

    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/my-app/logo.png", "my-app");

    String cc = response.getHeader("Cache-Control");
    assertThat(cc, containsString("max-age=7200"));
  }

  @Test
  @DisplayName("Must-revalidate is set for HTML paths")
  void htmlPath_getsMustRevalidate() {
    when(config.getProperty(ConfigurationKey.STATIC_CACHE_ALWAYS_NO_CACHE_PATTERNS)).thenReturn("");

    MockHttpServletResponse response = new MockHttpServletResponse();
    service.setHeaders(response, "/apps/dashboard/page.html", null);

    String cc = response.getHeader("Cache-Control");
    assertThat(cc, containsString("must-revalidate"));
  }
}
