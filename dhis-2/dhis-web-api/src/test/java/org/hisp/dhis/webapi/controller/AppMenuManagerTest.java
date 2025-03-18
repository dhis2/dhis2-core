/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.appmanager.AppManager.BUNDLED_APPS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppMenuManager;
import org.hisp.dhis.appmanager.AppShortcut;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.web.servlet.MockMvc;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AppMenuManagerTest {
  @Autowired private MockMvc mockMvc;

  @Mock private I18nManager i18nManager;

  @Mock I18n i18n;

  @Mock AppManager appManager;

  @Mock LocaleManager localeManager;

  @Spy private ResourceLoader resourceLoader;

  @InjectMocks private AppMenuManager appMenuManager;

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
    Mockito.when(localeManager.getCurrentLocale()).thenReturn(new Locale("en"));
    when(i18nManager.getI18n()).thenReturn(i18n);

    Resource mockResource = Mockito.mock(Resource.class);
    Mockito.when(mockResource.getInputStream()).thenReturn(inputStream);

    Mockito.when(resourceLoader.getResource(Mockito.anyString())).thenReturn(mockResource);
  }

  @Test
  void testGetMenu_BundledApps() {
    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(() -> CurrentUserUtil.hasAnyAuthority(Mockito.anyList()))
          .thenReturn(Boolean.TRUE);

      List<WebModule> accessibleWebModules = appMenuManager.getAccessibleWebModules(appManager);
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
}
