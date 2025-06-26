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
import static org.hisp.dhis.util.ZipFileUtils.MAX_ENTRIES;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests the {@link AppController}
 *
 * @author Jan Bernitt
 */
class AppControllerTest extends DhisControllerConvenienceTest {

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

  @Autowired private AppManager appManager;

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
  void testInstalledEvilZipSlipApp() throws IOException {
    AppStatus appStatus =
        appManager.installApp(new ClassPathResource("app/evil_app.zip").getFile(), "evil_app.zip");
    assertEquals(AppStatus.INVALID_ZIP_FORMAT, appStatus);
  }

  @Test
  void testInstalledEvilFlatZipBombApp() throws IOException {
    AppStatus appStatus =
        appManager.installApp(
            new ClassPathResource("app/flat_bomb.zip").getFile(), "flat_bomb.zip");
    assertEquals(AppStatus.INVALID_ZIP_FORMAT, appStatus);
  }

  @Test
  @DisplayName("Install app with zip slip vulnerability fails")
  void testInstallZipSlipApp() throws IOException {
    Map<String, byte[]> entries =
        Map.of(
            "manifest.webapp",
            "{\"name\":\"Evil App\",\"version\":\"1.0\"}".getBytes(StandardCharsets.UTF_8),
            "../../../../../../../../../../../../../../../../../../tmp/evil.txt",
            "evil content".getBytes(StandardCharsets.UTF_8));

    File evilZip = createTempZipFile(entries);
    AppStatus appStatus = appManager.installApp(evilZip, "evil_slip.zip");

    assertTrue(
        appStatus == AppStatus.INVALID_ZIP_FORMAT,
        "App installation should fail due to path traversal attempt");

    evilZip.delete();
  }

  @Test
  @DisplayName("Install app with zip bomb vulnerability fails")
  void testInstallZipBombWithTooManyEntriesApp() throws IOException {
    // Create a small, highly compressible data block (e.g., 1KB of zeros)
    byte[] compressibleData = new byte[1024]; // 1KB of zeros

    // Create many entries pointing to the same compressible data
    Map<String, byte[]> entries = new java.util.HashMap<>();
    entries.put(
        "manifest.webapp",
        "{\"name\":\"Bomb App\",\"version\":\"1.0\"}".getBytes(StandardCharsets.UTF_8));

    for (int i = 0; i < MAX_ENTRIES; i++) {
      entries.put("file" + i + ".txt", compressibleData);
    }

    File bombZip = createTempZipFile(entries);
    AppStatus appStatus = appManager.installApp(bombZip, "bomb.zip");

    assertTrue(
        appStatus == AppStatus.INVALID_ZIP_FORMAT,
        "App installation should fail due to zip bomb attempt");

    bombZip.delete();
  }

  /**
   * Creates a temporary zip file with the given entries.
   *
   * @throws IOException If an I/O error occurs.
   */
  private static File createTempZipFile(Map<String, byte[]> entries) throws IOException {
    File tempFile = File.createTempFile("test", ".zip");
    try (FileOutputStream fos = new FileOutputStream(tempFile);
        ZipOutputStream zos = new ZipOutputStream(fos)) {

      for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
        ZipEntry zipEntry = new ZipEntry(entry.getKey());
        zos.putNextEntry(zipEntry);
        zos.write(entry.getValue());
        zos.closeEntry();
      }
    }
    return tempFile;
  }
}
