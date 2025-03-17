/*
 * Copyright (c) 2004-2025, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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
package org.hisp.dhis.appmanager;

import static org.hisp.dhis.appmanager.AppManager.BUNDLED_APPS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.hisp.dhis.apphub.AppHubService;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheBuilder;
import org.hisp.dhis.cache.DefaultCacheBuilderProvider;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AppMenuManagerTest {
  @Mock private I18nManager i18nManager;

  @Mock private I18n i18n;

  @Mock private LocaleManager localeManager;

  @Mock private DhisConfigurationProvider dhisConfigurationProvider;
  @Mock private AppHubService appHubService;
  @Mock private AppStorageService localAppStorageService;
  @Mock private AppStorageService jCloudsAppStorageService;
  @Mock private BundledAppStorageService bundledAppStorageService;
  @Mock private DatastoreService datastoreService;
  @Mock private Cache<App> appCache;
  @Mock private DefaultCacheBuilderProvider cacheBuilderProvider;
  @Mock private CacheBuilder cacheBuilder;

  @Spy private ResourceLoader resourceLoader;

  private AppManager appManager;

  String mockFile =
      """
                  {
                    "app_hub_id": "4a5b87dc-015c-47db-ae77-f2f42e3bbb5a",
                    "appType": "APP",
                    "short_name": "aggregate-data-entry",
                    "name": "Data Entry",
                    "description": "",
                    "version": "101.0.1",
                    "core_app": true,
                    "launch_path": "index.html",
                    "default_locale": "en",
                    "icons": {
                      "48": "dhis2-app-icon.png"
                    },
                    "shortcuts": [
                      {
                        "name": "Aggregate form",
                        "url": "#/aggregate-form"
                      },{
                        "name": "Custom form",
                        "url": "#/custom-form"
                      }
                    ]
                  }
                  """;
  InputStream inputStream = new ByteArrayInputStream(mockFile.getBytes());

  @BeforeEach
  void setUp() throws Exception {
    mockBundledApps();

    doReturn(cacheBuilder).when(cacheBuilderProvider).newCacheBuilder();
    doReturn(cacheBuilder).when(cacheBuilder).forRegion("appCache");
    doReturn(appCache).when(cacheBuilder).build();

    appManager =
        new DefaultAppManager(
            dhisConfigurationProvider,
            appHubService,
            localAppStorageService,
            jCloudsAppStorageService,
            bundledAppStorageService,
            datastoreService,
            cacheBuilderProvider);

    appManager.reloadApps();

    Mockito.when(localeManager.getCurrentLocale()).thenReturn(new Locale("en"));
    when(i18nManager.getI18n()).thenReturn(i18n);

    Resource mockResource = Mockito.mock(Resource.class);
    Mockito.when(mockResource.getInputStream()).thenReturn(inputStream);

    Mockito.when(resourceLoader.getResource(Mockito.anyString())).thenReturn(mockResource);
  }

  @Disabled(
      "Needs to be updated to mock ResourcePatternResolver and ResourceLoader in BundledAppStorageService")
  @Test
  void testGetMenu_BundledApps() {
    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(() -> CurrentUserUtil.hasAnyAuthority(Mockito.anyList()))
          .thenReturn(Boolean.TRUE);

      List<WebModule> accessibleWebModules = appManager.getMenu("/context");
      assertEquals(BUNDLED_APPS.size(), accessibleWebModules.size());

      WebModule webModule = accessibleWebModules.get(0);

      AppShortcut firstShortcut = webModule.getShortcuts().get(0);
      AppShortcut secondShortcut = webModule.getShortcuts().get(1);

      assertNotNull(webModule.getName());
      assertEquals("101.0.1", webModule.getVersion());
      String expectedRegex = "\\.\\./dhis.+/index.html$";
      assertTrue(webModule.getDefaultAction().matches(expectedRegex));

      assertEquals("Aggregate form", firstShortcut.getName());
      assertEquals("#/aggregate-form", firstShortcut.getUrl());

      assertEquals("Custom form", secondShortcut.getName());
      assertEquals("#/custom-form", secondShortcut.getUrl());
    }
  }

  private void mockBundledApps() {
    Map<String, App> apps =
        AppManager.BUNDLED_APPS.stream()
            .map(app -> stubApp(app, true))
            .collect(Collectors.toMap(App::getKey, app -> app));

    when(bundledAppStorageService.discoverInstalledApps()).thenReturn(apps);
  }

  private App stubApp(String key, boolean bundled) {
    App app = new App();
    app.setShortName(key);
    app.setIsBundled(bundled);
    app.setShortcuts(List.of(new AppShortcut(), new AppShortcut()));
    return app;
  }
}
