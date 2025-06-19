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
package org.hisp.dhis.webapi.controller;

import static java.nio.file.Files.createTempDirectory;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppShortcut;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.test.config.TestDhisConfigurationProvider;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.webapi.controller.AppControllerTest.DhisConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link AppController}
 *
 * @author Jan Bernitt
 */
@ContextConfiguration(
    classes = {
      DhisConfig.class,
    })
@Transactional
class AppControllerTest extends H2ControllerIntegrationTestBase {
  static class DhisConfig {
    @Bean
    public DhisConfigurationProvider dhisConfigurationProvider() {
      return new TestDhisConfigurationProvider("appControllerBaseTestDhis.conf");
    }
  }

  @Autowired private AppManager appManager;

  static {
    try {
      ClassPathResource classPathResource = new ClassPathResource("appControllerBaseTestDhis.conf");
      Path tempDir = createTempDirectory("appFiles").toAbsolutePath();
      try (InputStream inputStream = classPathResource.getInputStream()) {
        Path destFile = tempDir.resolve("dhis.conf");
        Files.copy(inputStream, destFile);
      }
      String filePath = tempDir.toString();
      System.setProperty("dhis2.home", filePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void cleanUp() {
    // make sure we reset the UI locale to default in case a test changes it
    DELETE("/systemSettings/keyUiLocale");
    DELETE("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID);
  }

  @Test
  void testGetInstalledAppIndexHtml() throws IOException {
    appManager.installApp(
        new ClassPathResource("app/test-app-with-index-html.zip").getFile(),
        "test-app-with-index-html.zip");

    HttpResponse response = GET("/apps/myapp/index.html");
    assertTrue(response.hasBody());
    String content = response.content("text/html");
    assertTrue(content.contains("<!doctype html>"));
  }

  @Test
  void testInstallReturnsAppInfo() throws IOException {
    var result =
        appManager.installApp(
            new ClassPathResource("app/test-app-with-index-html.zip").getFile(),
            "test-app-with-index-html.zip");

    assertEquals(
        "31.0.0",
        result.getVersion(),
        "the version returned should match the version in the installed zip file");
  }

  @Test
  void testGetApps() {
    HttpResponse response = GET("/apps");
    JsonArray apps = response.content(HttpStatus.OK);
    assertTrue(apps.isArray());
  }

  @Test
  void testGetApps_KeyNotFound() {
    HttpResponse response = GET("/apps?key=xyz");
    assertEquals(HttpStatus.NOT_FOUND, response.status());
    assertFalse(response.hasBody());
  }

  @Test
  @DisplayName(
      "Requesting to reload the apps while missing the required auth results in an exception")
  void testReloadAppsNoAuth() {
    switchToNewUser("noAuth", "NoAuth");
    JsonMixed mergeResponse = PUT("/apps").content(HttpStatus.FORBIDDEN);
    assertEquals("Forbidden", mergeResponse.getString("httpStatus").string());
    assertEquals("ERROR", mergeResponse.getString("status").string());
    assertEquals(
        "Access is denied, requires one Authority from [M_dhis-web-app-management]",
        mergeResponse.getString("message").string());
  }

  @Test
  @DisplayName("Requesting to reload the apps with the required auth results in success")
  void testReloadAppsWithAuth() {
    switchToNewUser("hasAuth", Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString());
    assertEquals(HttpStatus.NO_CONTENT, PUT("/apps").status());
  }

  @Disabled(
      "Deprecated, will be reintroduced if bundled app overrides are again served at root paths")
  @Test
  @DisplayName("Redirect for bundled app has correct location header")
  void redirectLocationTest() throws IOException {
    appManager.installApp(
        new ClassPathResource("app/test-bundled-app.zip").getFile(), "test-bundled-app.zip");

    HttpResponse get = GET("/api/apps/cache-cleaner/index.html");
    assertEquals("http://localhost/dhis-web-cache-cleaner/index.html", get.location());
  }

  @Test
  void testInstalledAppReturnsShortcuts() throws IOException {
    appManager.installApp(
        new ClassPathResource("app/test-app-with-shortcuts.zip").getFile(),
        "test-app-with-shortcuts.zip");

    HttpResponse response = GET("/apps/menu");
    assertEquals(HttpStatus.OK, response.status());

    List<App> modules =
        App.MAPPER.readValue(response.content().get("modules").toJson(), new TypeReference<>() {});

    App installedApp = modules.get(modules.size() - 1);
    AppShortcut firstShortcut = installedApp.getShortcuts().get(0);
    AppShortcut secondShortcut = installedApp.getShortcuts().get(1);

    assertEquals(2, installedApp.getShortcuts().size());
    assertEquals("Category section", firstShortcut.getName());
    assertEquals("Category section", firstShortcut.getDisplayName());
    assertEquals("#/overview/categories", firstShortcut.getUrl());

    assertEquals("Category", secondShortcut.getName());
    assertEquals("Category section", firstShortcut.getDisplayName());
    assertEquals("#/categories", secondShortcut.getUrl());
  }

  @Test
  void testInstalledAppReturnsTranslatedShortcuts() throws IOException {
    POST("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID + "&value=es");

    appManager.installApp(
        new ClassPathResource("app/test-app-with-translated-manifest.zip").getFile(),
        "test-app-with-translated-manifest.zip");

    HttpResponse response = GET("/apps/menu");
    assertEquals(HttpStatus.OK, response.status());

    List<App> modules =
        App.MAPPER.readValue(response.content().get("modules").toJson(), new TypeReference<>() {});

    App installedApp = modules.get(modules.size() - 1);
    AppShortcut firstShortcut = installedApp.getShortcuts().get(0);
    AppShortcut secondShortcut = installedApp.getShortcuts().get(1);

    assertEquals("Seccion de categorías", firstShortcut.getDisplayName());
    assertEquals("Categoría", secondShortcut.getDisplayName());
  }

  @Test
  @DisplayName(
      "Should fallback to the language if the language + country combination does not match")
  void testInstalledAppReturnsTranslatedShortcuts_WithFallbackToRootLanguage() throws IOException {
    POST("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID + "&value=es_CO");

    appManager.installApp(
        new ClassPathResource("app/test-app-with-translated-manifest.zip").getFile(),
        "test-app-with-translated-manifest.zip");

    HttpResponse response = GET("/apps/menu");
    assertEquals(HttpStatus.OK, response.status());

    List<App> modules =
        App.MAPPER.readValue(response.content().get("modules").toJson(), new TypeReference<>() {});

    Optional<App> installedApp =
        modules.stream().filter(a -> Objects.equals(a.getName(), "test")).findFirst();
    AppShortcut firstShortcut = installedApp.get().getShortcuts().get(0);
    AppShortcut secondShortcut = installedApp.get().getShortcuts().get(1);

    assertEquals("Seccion de categorías", firstShortcut.getDisplayName());
    assertEquals("Categoría", secondShortcut.getDisplayName());
  }

  @Test
  @DisplayName(
      "Should return the most specific language match, i.e. if the user has ar_IQ as locale, we should retrieve ar_IQ first, then ar then default (english) in this order")
  void testInstalledAppReturnsTranslatedShortcuts_WithLanguageFallback() throws IOException {
    POST("/userSettings/keyUiLocale/?userId=" + ADMIN_USER_UID + "&value=ar_IQ");

    appManager.installApp(
        new ClassPathResource("app/test-app-with-translated-manifest.zip").getFile(),
        "test-app-with-translated-manifest.zip");

    HttpResponse response = GET("/apps/menu");
    assertEquals(HttpStatus.OK, response.status());

    List<App> modules =
        App.MAPPER.readValue(response.content().get("modules").toJson(), new TypeReference<>() {});

    Optional<App> installedApp =
        modules.stream().filter(a -> Objects.equals(a.getName(), "test")).findFirst();
    AppShortcut firstShortcut = installedApp.get().getShortcuts().get(0);
    AppShortcut secondShortcut = installedApp.get().getShortcuts().get(1);

    assertEquals("فسم اﻷنواع", firstShortcut.getDisplayName());
    assertEquals("Iraqi Arabic نوع", secondShortcut.getDisplayName());
  }
}
