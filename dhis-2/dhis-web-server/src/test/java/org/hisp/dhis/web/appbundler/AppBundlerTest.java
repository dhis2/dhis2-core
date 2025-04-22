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
package org.hisp.dhis.web.appbundler;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for the AppBundler class. */
class AppBundlerTest {

  @TempDir Path tempDir;

  private File appListFile;
  private String buildDir;
  private String downloadDir;
  private String artifactId;

  @BeforeEach
  void setUp() throws IOException {
    // Create a temporary app list file
    appListFile = tempDir.resolve("test-apps.json").toFile();
    new ObjectMapper()
        .writeValue(
            appListFile,
            Arrays.asList(
                "https://codeload.github.com/d2-ci/settings-app/zip/refs/heads/master",
                "https://codeload.github.com/d2-ci/dashboard-app/zip/refs/heads/master"));

    // Set up build directory
    buildDir = tempDir.resolve("build").toString();
    downloadDir = tempDir.resolve("download").toString();
    artifactId = "dhis-web-apps";
  }

  @Test
  void testAppBundlerCreatesCorrectStructure() throws IOException {
    // Execute the app bundler
    AppBundler bundler =
        new AppBundler(downloadDir, buildDir, artifactId, appListFile.getAbsolutePath(), "master");
    bundler.execute();

    // Verify the directory structure
    Path buildDirPath = Path.of(buildDir).resolve(artifactId);
    assertTrue(Files.exists(buildDirPath), "Build directory should exist");

    Path downloadArtifactDir = Path.of(downloadDir).resolve(artifactId);
    assertTrue(Files.exists(downloadArtifactDir), "Artifact download directory should exist");

    Path checksumDir = downloadArtifactDir.resolve("checksums");
    assertTrue(Files.exists(checksumDir), "Checksums directory should exist");

    // Settings app should be downloaded
    Path settingsAppZip = downloadArtifactDir.resolve("settings-app.zip");
    assertTrue(Files.exists(settingsAppZip), "Settings app ZIP should exist");

    // Dashboard app should be downloaded
    Path dashboardAppZip = downloadArtifactDir.resolve("dashboard-app.zip");
    assertTrue(Files.exists(dashboardAppZip), "Dashboard app ZIP should exist");

    // Check apps are copied to the build dir.
    Path dashboardAppZipInBuild = buildDirPath.resolve("dashboard-app.zip");
    assertTrue(Files.exists(dashboardAppZipInBuild), "Copied dashboard app ZIP should exist");

    // Check bundle file is made
    Path bundleInfoFile = buildDirPath.resolve("apps-bundle.json");
    assertTrue(Files.exists(bundleInfoFile), "Bundle info file should exist");
  }
}
