/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.appmanager;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Saptarshi
 */
class AppTest {
  private App app;

  @BeforeEach
  void setUp() throws IOException {
    String appJson =
        FileUtils.readFileToString(
            new File(this.getClass().getResource("/manifest.webapp").getFile()),
            StandardCharsets.UTF_8);
    this.app = App.MAPPER.readValue(appJson, App.class);
    this.app.init("https://example.com");

    List<AppShortcut> shortcuts =
        List.of(
            new AppShortcut("help", "#/help"),
            new AppShortcut("info", "#/info"),
            new AppShortcut("exit", "#/exit"));
    this.app.setShortcuts(shortcuts);
  }

  @AfterEach
  void tearDown() {}

  // TODO: Verify missing property
  @Test
  void testRequiredProperties() {
    assertEquals("0.1", app.getVersion());
    assertEquals("Test App", app.getName());
    assertEquals("/index.html", app.getLaunchPath());
    assertEquals("/plugin.html", app.getPluginLaunchPath());
    assertEquals("*", app.getInstallsAllowedFrom()[0]);
    assertEquals("en", app.getDefaultLocale());
  }

  @Test
  void testGetKey() {
    assertEquals("Test-App", app.getKey());
  }

  @Test
  void testGetBasePath() {
    assertEquals("/api/apps/Test-App", app.getBasePath());
  }

  @Test
  void testGetBaseUrl() {
    assertEquals("https://example.com/api/apps/Test-App", app.getBaseUrl());
  }

  // TODO: Complete test for skipped optional properties
  @Test
  void testOptionalProperties() {
    assertEquals("Test Description", app.getDescription());
    assertFalse(app.getSettings().getDashboardWidget().getHideTitle());
  }

  @Test
  void testIcons() {
    assertEquals("/img/icons/mortar-16.png", app.getIcons().getIcon16());
    assertEquals("/img/icons/mortar-48.png", app.getIcons().getIcon48());
    assertEquals("/img/icons/mortar-128.png", app.getIcons().getIcon128());
  }

  @Test
  void testDeveloper() {
    assertEquals("Test Developer", app.getDeveloper().getName());
    assertEquals("http://test", app.getDeveloper().getUrl());
    assertNull(app.getDeveloper().getEmail());
    assertNull(app.getDeveloper().getCompany());
  }

  @Test
  void testActivities() {
    AppDhis dhisActivity = app.getActivities().getDhis();
    assertEquals("http://localhost:8080/dhis", dhisActivity.getHref());
    dhisActivity.setHref("ALL TEST");
    assertEquals("ALL TEST", dhisActivity.getHref());
  }

  @Test
  void testGetAuthorities() {
    Set<String> authorities = app.getAuthorities();
    assertNotNull(authorities);
    assertEquals(4, authorities.size());
  }

  @Test
  void testGetSeeAppAuthority() {
    assertEquals("M_Test_App", app.getSeeAppAuthority());
  }

  @Test
  void testGetUrlFriendlyName() {
    App appA = new App();
    appA.setName("Org [Facility] &Registry@");
    App appB = new App();
    appB.setName(null);
    assertEquals("Org-Facility-Registry", appA.getUrlFriendlyName());
    assertNull(appB.getUrlFriendlyName());
  }

  @Test
  void testGetLaunchUrl() {
    assertEquals("https://example.com/api/apps/Test-App/index.html", app.getLaunchUrl());
  }

  @Test
  void testGetPluginLaunchUrl() {
    assertEquals("https://example.com/api/apps/Test-App/plugin.html", app.getPluginLaunchUrl());

    App appWithoutPlugin = new App();
    appWithoutPlugin.setName("Test App");
    appWithoutPlugin.setLaunchPath("/index.html");
    appWithoutPlugin.init("https://example.com");
    assertEquals(
        "https://example.com/api/apps/Test-App/index.html", appWithoutPlugin.getLaunchUrl());
    assertNull(appWithoutPlugin.getPluginLaunchUrl());
  }

  List<AppManifestTranslation> getTranslation(String translationJSON) throws IOException {
    return App.MAPPER.readerForListOf(AppManifestTranslation.class).readValue(translationJSON);
  }

  @Test
  void testShortcutTranslations() throws IOException {
    String translationJSON =
        """
                        [
                        {
                              "locale": "es",
                              "title": "El App",
                              "description": "App descripcion",
                              "shortcuts": {
                                "help": "ayuda",
                                "info": "informacion"
                              }
                            }
                        ]
                        """;

    var translationManifest = getTranslation(translationJSON);
    app.setManifestTranslations(translationManifest);
    var result = app.localise(new Locale("es"));

    assertEquals("ayuda", result.getShortcuts().get(0).getDisplayName());
    assertEquals("informacion", result.getShortcuts().get(1).getDisplayName());
  }

  @Test
  @DisplayName(
      "Should match with the most specific locale if possible (country + language, i.e. es_CO) and fallback to the language if none exists (i.e. es), and to the default otherwise ")
  void testManifestTranslationsFallbackLogic() throws IOException {
    String translationJSON =
        """
                        [
                          {
                              "locale": "es",
                              "title": "El App",
                              "description": "descripcion en español",
                              "shortcuts": {
                                "help": "ayuda",
                                "info": "informacion"
                              }
                          },
                          {
                              "locale": "es_CO",
                              "shortcuts": {
                                "help": "ayuda (Colombia)"
                              }
                          }
                        ]
                        """;

    var translationManifest = getTranslation(translationJSON);
    app.setManifestTranslations(translationManifest);
    var result = app.localise(new Locale("es", "CO"));

    assertEquals("El App", result.getDisplayName());
    assertNotEquals("El App", result.getName());
    assertEquals("descripcion en español", result.getDisplayDescription());
    assertEquals("ayuda (Colombia)", result.getShortcuts().get(0).getDisplayName());
    assertEquals("informacion", result.getShortcuts().get(1).getDisplayName());
    assertEquals("exit", result.getShortcuts().get(2).getDisplayName());
  }

  @Test
  void testShouldReturnDefaultIfNoMatch() throws IOException {
    String translationJSON =
        """
                        [
                          {
                              "locale": "es",
                              "title": "El App",
                              "description": "App descripcion",
                              "shortcuts": {
                                "help": "ayuda",
                                "info": "informacion"
                              }
                          }
                        ]
                        """;

    var translationManifest = getTranslation(translationJSON);
    app.setManifestTranslations(translationManifest);
    var result = app.localise(new Locale("de"));

    assertEquals("help", result.getShortcuts().get(0).getDisplayName());
    assertEquals("info", result.getShortcuts().get(1).getDisplayName());
    assertEquals("exit", result.getShortcuts().get(2).getDisplayName());
  }

  @Test
  void testShouldRespectLanguageScript() throws IOException {
    String translationJSON =
        """
                        [
                            {
                              "locale": "uz_Latn",
                              "shortcuts": {
                                "help": "help (Uzbek Latin)"
                              }
                            },
                            {
                              "locale": "uz_UZ_Cyrl",
                               "shortcuts": {
                                "help": "help (Uzbek Cyrillic)"
                              }
                            },
                            {
                              "locale": "uz_UZ_Latn",
                               "shortcuts": {
                                "help": "help (Uzbek-Uzbekistan Latin)"
                              }
                            }
                        ]
                        """;

    var translationManifest = getTranslation(translationJSON);
    app.setManifestTranslations(translationManifest);

    Locale locale =
        new Locale.Builder().setLanguage("uz").setRegion("UZ").setScript("Cyrl").build();

    var result = app.localise(locale);
    assertEquals("help (Uzbek Cyrillic)", result.getShortcuts().get(0).getDisplayName());

    locale = new Locale.Builder().setLanguage("uz").setRegion("UZ").setScript("Latn").build();

    result = app.localise(locale);
    assertEquals("help (Uzbek-Uzbekistan Latin)", result.getShortcuts().get(0).getDisplayName());
  }
}
