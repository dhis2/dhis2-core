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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the bundling of DHIS2 apps by downloading them as ZIP files from GitHub. This replaces
 * the previous process that used Git to clone the apps.
 */
public class AppBundler {
  private static final Logger logger = LoggerFactory.getLogger(AppBundler.class);

  // Regex to parse standard GitHub URL: https://github.com/owner/repo#ref
  private static final Pattern GITHUB_URL_PATTERN =
      Pattern.compile("^https://github\\.com/([^/]+)/([^/#]+)(?:#(.+))?$");

  private static final int DOWNLOAD_POOL_SIZE = 25; // Number of concurrent downloads
  private static final String DEFAULT_BRANCH = "master";

  /**
   * Record to hold parsed information from a standard GitHub URL.
   *
   * @param owner The repository owner.
   * @param repo The repository name.
   * @param ref The branch, tag, or commit reference.
   * @param originalUrl The original URL string from the input file.
   * @param codeloadUrl The converted URL for downloading the ZIP archive.
   */
  private record AppRepoInfo(
      String owner, String repo, String ref, String originalUrl, String codeloadUrl) {}

  /**
   * Converts a standard GitHub repository URL with a ref in the fragment (e.g.,
   * https://github.com/owner/repo#branch) into the codeload.github.com URL format for downloading a
   * ZIP archive.
   *
   * @param githubUrl The standard GitHub URL (e.g.,
   *     "https://github.com/d2-ci/datastore-app#patch/2.42.0").
   * @return The corresponding codeload URL (e.g.,
   *     "https://codeload.github.com/d2-ci/datastore-app/zip/patch/2.42.0"), or null if the input
   *     URL format is invalid.
   */
  public static String convertToCodeloadUrl(String githubUrl) {
    if (githubUrl == null) {
      return null;
    }
    Matcher matcher = GITHUB_URL_PATTERN.matcher(githubUrl);
    if (matcher.matches()) {
      String owner = matcher.group(1);
      String repo = matcher.group(2);
      String ref = matcher.group(3);
      if (ref == null) {
        ref = "refs/heads/master";
      }

      return String.format("https://codeload.github.com/%s/%s/zip/%s", owner, repo, ref);
    } else {
      // Use logger if available statically or pass instance if needed
      // For simplicity here, using System.err, but logger is preferred
      logger.error("Invalid GitHub URL format for conversion: " + githubUrl);
      // Consider changing logger level if this becomes noisy
      // logger.warn("Invalid GitHub URL format for conversion: {}", githubUrl);
      return null;
    }
  }

  private static final String CHECKSUM_DIR = "checksums";
  private static final String BUNDLE_INFO_FILE = "apps-bundle.json";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final String downloadDir;
  private final String artifactId;
  private final String appListFilePath;
  private final String defaultBranch;
  private final AppBundleInfo bundleInfo;
  private final String buildDir;

  /**
   * Constructs an AppBundler with the specified parameters.
   *
   * @param downloadDir the directory where the app ZIP files will be stored
   * @param artifactId the artifact ID for the bundle
   * @param appListFilePath the path to the JSON file containing the list of apps to bundle
   * @param defaultBranch the default branch to use if none is specified in the app URL
   */
  public AppBundler(
      String downloadDir,
      String buildDir,
      String artifactId,
      String appListFilePath,
      String defaultBranch) {
    this.downloadDir = downloadDir;
    this.buildDir = buildDir;
    this.artifactId = artifactId;
    this.appListFilePath = appListFilePath;
    this.defaultBranch = defaultBranch != null ? defaultBranch : DEFAULT_BRANCH;
    this.bundleInfo = new AppBundleInfo();
  }

  /**
   * Executes the app bundling process.
   *
   * @throws IOException if there's an error reading the app list or downloading the apps
   */
  public void execute() throws IOException {
    logger.error("Starting app bundling process");
    logger.error("Download directory: {}", downloadDir);
    logger.error("Build directory: {}", buildDir);
    logger.error("Artifact ID: {}", artifactId);
    logger.error("App list path: {}", appListFilePath);
    logger.error("Default branch: {}", defaultBranch);

    // Create download directory if it doesn't exist
    Path downloadDirPath = Paths.get(downloadDir);
    Files.createDirectories(downloadDirPath);

    // Create download artifact directory
    Path artifactDirPath = downloadDirPath.resolve(artifactId);
    Files.createDirectories(artifactDirPath);

    // Create download checksums directory
    Path checksumDirPath = artifactDirPath.resolve(CHECKSUM_DIR);
    Files.createDirectories(checksumDirPath);

    // Read app list - now returns List<AppRepoInfo>
    List<AppRepoInfo> appRepoInfos =
        parseAppListUrls(appListFilePath); // Changed variable name and type
    logger.error("Found {} valid apps to bundle", appRepoInfos.size()); // Use the new list

    // Download each app in parallel - pass the new list type
    downloadApps(appRepoInfos, artifactDirPath, checksumDirPath); // Pass AppRepoInfo list

    // Copy downloaded apps to build directory
    Path targetArtifactDir = copyApps(downloadDirPath);

    // Write bundle info file
    writeBundleFile(targetArtifactDir);
  }

  private void writeBundleFile(Path targetArtifactDir) throws IOException {
    Path bundleInfoPath = targetArtifactDir.resolve(BUNDLE_INFO_FILE);
    objectMapper.writerWithDefaultPrettyPrinter().writeValue(bundleInfoPath.toFile(), bundleInfo);
    logger.error("Wrote bundle info to {}", bundleInfoPath);
    logger.error("App bundling process completed successfully");
  }

  private @Nonnull Path copyApps(Path downloadDirPath) throws IOException {
    Path targetArtifactDir = Paths.get(buildDir).resolve(artifactId);
    try {
      Files.createDirectories(targetArtifactDir);
      logger.error("Ensured target artifact directory exists: {}", targetArtifactDir);

      for (AppBundleInfo.AppInfo app : bundleInfo.getApps()) {
        Path sourceZipPath = downloadDirPath.resolve(artifactId).resolve(app.getName() + ".zip");
        Path destZipPath = targetArtifactDir.resolve(app.getName() + ".zip");

        if (Files.exists(sourceZipPath)) {
          Files.copy(sourceZipPath, destZipPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
          logger.error("Copied {} to {}", sourceZipPath, destZipPath);
        } else {
          logger.warn(
              "Source zip file not found, skipping copy for app {}: {}",
              app.getName(),
              sourceZipPath);
        }
      }
      logger.error("Finished copying app bundles to build directory.");

    } catch (IOException e) {
      logger.error("Error copying app bundles to build directory: {}", e.getMessage(), e);
      throw new IOException("Failed to copy app bundles to build directory", e);
    }
    // --- End Copying ---
    return targetArtifactDir;
  }

  // Update method signature to accept List<AppRepoInfo>
  private void downloadApps(
      List<AppRepoInfo> appRepoInfos, Path artifactDirPath, Path checksumDirPath) {
    ExecutorService executor = Executors.newFixedThreadPool(DOWNLOAD_POOL_SIZE);
    List<Future<AppBundleInfo.AppInfo>> futures = new ArrayList<>();

    logger.error(
        "Submitting download tasks for {} apps...", appRepoInfos.size()); // Use the new list size
    for (AppRepoInfo repoInfo : appRepoInfos) { // Iterate over AppRepoInfo
      // Pass the repoInfo object to the downloadApp task
      Callable<AppBundleInfo.AppInfo> task =
          () -> downloadApp(repoInfo, artifactDirPath, checksumDirPath);
      Future<AppBundleInfo.AppInfo> future = executor.submit(task);
      futures.add(future);
    }

    logger.error("Collecting download results...");
    for (Future<AppBundleInfo.AppInfo> future : futures) {
      try {
        AppBundleInfo.AppInfo appInfo = future.get(); // Blocks until completion
        if (appInfo != null) {
          bundleInfo.addApp(appInfo);
          logger.error("Successfully processed app: {}", appInfo.getName());
        }
      } catch (InterruptedException | ExecutionException e) {
        // Log the error and continue with other apps
        // The specific failing app URL isn't directly available here without more
        // complex tracking
        logger.error(
            "Error retrieving result for an app download task: {} - {}",
            e.getClass().getSimpleName(),
            e.getMessage(),
            e.getCause());
      }
    }

    logger.error("Shutting down download executor...");
    executor.shutdown();
    try {
      // Wait a reasonable time for existing tasks to terminate
      if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
        logger.warn(
            "Download executor did not terminate gracefully after 5 minutes, forcing shutdown.");
        executor.shutdownNow();
        // Wait again for tasks to respond to being cancelled
        if (!executor.awaitTermination(60, TimeUnit.SECONDS))
          logger.error("Download executor did not terminate even after forced shutdown.");
      }
    } catch (InterruptedException ie) {
      logger.error("Interrupted while waiting for executor termination, forcing shutdown.", ie);
      executor.shutdownNow();
      Thread.currentThread().interrupt(); // Preserve interrupt status
    }
    logger.error("Download executor shut down.");
  }

  /**
   * Reads the list of app URLs from the JSON file.
   *
   * <p>Reads the list of standard app URLs from the JSON file, parses them, converts them to
   * codeload format, and returns structured info.
   *
   * @return a list of AppRepoInfo objects
   * @throws IOException if there's an error reading the file
   */
  private List<AppRepoInfo> parseAppListUrls(String appListPath) throws IOException {
    File appListFile = new File(appListPath);
    CollectionType urlListType =
        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
    List<String> rawUrls = objectMapper.readValue(appListFile, urlListType);

    List<AppRepoInfo> appInfos = new ArrayList<>();
    for (String rawUrl : rawUrls) {
      Matcher matcher = GITHUB_URL_PATTERN.matcher(rawUrl);
      if (matcher.matches()) {
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String ref = matcher.group(3);
        String codeloadUrl = convertToCodeloadUrl(rawUrl); // Use the static method

        if (codeloadUrl != null) {
          appInfos.add(new AppRepoInfo(owner, repo, ref, rawUrl, codeloadUrl));
          logger.debug(
              "Parsed app URL: owner={}, repo={}, ref={}, codeload={}",
              owner,
              repo,
              ref,
              codeloadUrl);
        } else {
          // Log error from convertToCodeloadUrl is sufficient
          logger.error("Skipping app due to failed conversion from URL: {}", rawUrl);
        }
      } else {
        logger.error("Skipping app due to invalid GitHub URL format: {}", rawUrl);
      }
    }
    logger.info("Successfully parsed {} app URLs out of {}", appInfos.size(), rawUrls.size());
    return appInfos;
  }

  /**
   * Downloads an app from GitHub as a ZIP file.
   *
   * <p>Downloads an app from GitHub as a ZIP file using pre-parsed info.
   *
   * @param repoInfo the parsed repository information including the codeload URL
   * @param targetDir the directory where the app ZIP file will be stored
   * @param checksumDir the directory where the checksum files will be stored
   * @return the downloaded AppBundleInfo.AppInfo object containing ETag etc.
   * @throws IOException if there's an error downloading the app
   */
  private AppBundleInfo.AppInfo downloadApp(AppRepoInfo repoInfo, Path targetDir, Path checksumDir)
      throws IOException {
    // No need to match pattern here, info is already parsed from AppRepoInfo

    logger.error(
        "Processing app: {}/{} (ref: {}) from {}",
        repoInfo.owner(),
        repoInfo.repo(),
        repoInfo.ref(),
        repoInfo.codeloadUrl());

    // Generate a filename for the ZIP file using info from the record
    String zipName = repoInfo.repo() + ".zip";
    Path zipPath = targetDir.resolve(zipName);

    // Create app info object to store results (ETag etc.)
    AppBundleInfo.AppInfo appBundleResultInfo = new AppBundleInfo.AppInfo();
    appBundleResultInfo.setName(repoInfo.repo());
    appBundleResultInfo.setUrl(repoInfo.originalUrl()); // Store original URL
    appBundleResultInfo.setBranch(repoInfo.ref()); // Store the ref (branch/tag)

    // Check if we already have the latest version using the codeload URL
    String etag = null;
    // Use repo name from record for checksum file
    Path checksumFile = checksumDir.resolve(repoInfo.repo() + ".checksum");
    try {
      // Use the pre-converted codeloadUrl for download check
      etag = downloadIfChanged(repoInfo.codeloadUrl(), zipPath, checksumFile);

      if (etag == null) { // Not modified or download failed ETag retrieval
        if (Files.exists(checksumFile)) {
          appBundleResultInfo.setEtag(Files.readString(checksumFile, StandardCharsets.UTF_8));
          logger.error(
              "App {} is already up to date (ETag: {})", zipName, appBundleResultInfo.getEtag());
        } else {
          // This case might happen if download failed before ETag was written,
          // or if ETag header was missing. Log a warning.
          logger.warn("Could not determine ETag for app {}, checksum file missing.", zipName);
        }
      } else {
        // Store the new ETag in the result object
        appBundleResultInfo.setEtag(etag);
        logger.error("Downloaded app: {} (ETag: {})", zipName, etag);
      }
    } catch (IOException e) {
      // Log using the original URL for better context
      logger.error(
          "Failed to download or process app {}: {}", repoInfo.originalUrl(), e.getMessage(), e);
      // Propagate the exception so the Future captures it
      throw e;
    }

    return appBundleResultInfo; // Return the result info object
  }

  /**
   * Downloads a file if it has changed since the last download.
   *
   * @param fileUrl the URL of the file to download
   * @param outputPath the path where the file will be saved
   * @param checksumPath the path to the checksum file
   * @return the ETag of the downloaded file, or the previous ETag if the file hasn't changed
   * @throws IOException if there's an error downloading the file
   */
  private String downloadIfChanged(String fileUrl, Path outputPath, Path checksumPath)
      throws IOException {
    URL url = new URL(fileUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    TrustManager[] trustAllCerts =
        new TrustManager[] {
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {}

            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
          }
        };
    SSLContext sc;
    try {
      sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    } catch (KeyManagementException e) {
      throw new RuntimeException(e);
    }
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

    connection.setRequestMethod("GET");

    logger.debug("Checking/Downloading {} to {}", fileUrl, outputPath);
    logger.debug("Checksum file: {}", checksumPath);

    // Check if we have a previous ETag
    if (Files.exists(checksumPath)) {
      String previousEtag = new String(Files.readAllBytes(checksumPath), StandardCharsets.UTF_8);
      logger.debug("Previous ETag: {}", previousEtag);
      connection.setRequestProperty("If-None-Match", "\"" + previousEtag + "\"");
    }

    connection.connect();
    int responseCode = connection.getResponseCode();

    logger.debug("Response code for {}: {}", fileUrl, responseCode);

    if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
      // File hasn't changed
      logger.debug("File not modified (HTTP 304): {}", fileUrl);
      return null; // Indicate no download occurred, use existing checksum
    }

    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new IOException("HTTP error code: " + responseCode + " for URL: " + fileUrl);
    }

    // Get the ETag from the response
    String etag = connection.getHeaderField("ETag");
    if (etag != null) {
      etag = etag.replaceAll("\"", ""); // Remove quotes from ETag
    }

    // Download the file
    try (InputStream in = connection.getInputStream();
        FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
      byte[] buffer = new byte[8192];
      int bytesRead;
      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
      }
    }

    // Save the ETag for future comparisons
    if (etag != null) {
      Files.write(checksumPath, etag.getBytes(StandardCharsets.UTF_8));
    }

    return etag;
  }

  /**
   * Main method to run the app bundler as a standalone program.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    try {
      // Read properties using System.getProperty
      String downloadDir = System.getProperty("DOWNLOAD_DIR");
      String buildDir = System.getProperty("BUILD_DIR");
      String artifactId = System.getProperty("ARTIFACT_ID");
      String appListPath = System.getProperty("APP_LIST");
      String defaultBranch = System.getProperty("DEFAULT_BRANCH");
      // log all the properties
      logger.error("DOWNLOAD_DIR: {}", downloadDir);
      logger.error("BUILD_DIR: {}", buildDir);
      logger.error("ARTIFACT_ID: {}", artifactId);
      logger.error("APP_LIST: {}", appListPath);
      logger.error("DEFAULT_BRANCH: {}", defaultBranch);
      if (downloadDir == null || buildDir == null || artifactId == null || appListPath == null) {
        logger.error(
            "System properties DOWNLOAD_DIR, BUILD_DIR, ARTIFACT_ID, and APPS must be set");
        System.exit(1);
      }

      AppBundler bundler =
          new AppBundler(downloadDir, buildDir, artifactId, appListPath, defaultBranch);
      bundler.execute();
    } catch (Exception e) {
      logger.error("Error executing app bundler: {}", e.getMessage(), e);
      System.exit(1);
    }
  }
}
