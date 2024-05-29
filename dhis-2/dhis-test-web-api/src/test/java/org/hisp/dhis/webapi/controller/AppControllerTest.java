/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static java.nio.file.Files.createTempDirectory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.AppControllerBaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests the {@link AppController}
 *
 * @author Jan Bernitt
 */
class AppControllerTest extends AppControllerBaseTest {

  @Autowired private AppManager appManager;

  static {
    try {
      ClassPathResource classPathResource = new ClassPathResource("AppControllerTestConfig.conf");
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
}
