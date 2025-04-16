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
    artifactId = "dhis-web-apps";
  }

  @Test
  void testAppBundlerCreatesCorrectStructure() throws IOException {
    // Execute the app bundler
    AppBundler bundler =
        new AppBundler(buildDir, artifactId, appListFile.getAbsolutePath(), "master");
    bundler.execute();

    // Verify the directory structure
    Path artifactDir = Path.of(buildDir).resolve(artifactId);
    assertTrue(Files.exists(artifactDir), "Artifact directory should exist");

    Path checksumDir = artifactDir.resolve("checksums");
    assertTrue(Files.exists(checksumDir), "Checksums directory should exist");

    Path bundleInfoFile = artifactDir.resolve("apps-bundle.json");
    assertTrue(Files.exists(bundleInfoFile), "Bundle info file should exist");

    // Settings app should be downloaded
    Path settingsAppZip = artifactDir.resolve("settings-app.zip");
    assertTrue(Files.exists(settingsAppZip), "Settings app ZIP should exist");

    // Dashboard app should be downloaded
    Path dashboardAppZip = artifactDir.resolve("dashboard-app.zip");
    assertTrue(Files.exists(dashboardAppZip), "Dashboard app ZIP should exist");
  }
}
