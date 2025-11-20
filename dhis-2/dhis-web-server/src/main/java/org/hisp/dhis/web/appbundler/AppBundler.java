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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;
import org.hisp.dhis.appmanager.AppBundleInfo;
import org.hisp.dhis.appmanager.AppBundleInfo.BundledAppInfo;

/**
 * Handles the bundling of DHIS2 apps by downloading them as ZIP files from GitHub. This replaces
 * the previous process that used Git to clone the apps.
 */
public class AppBundler {
  // Regex to parse standard GitHub URL: https://github.com/owner/repo#ref
  private static final String DEFAULT_BRANCH = "master";
  private static final Pattern GITHUB_URL_PATTERN =
      Pattern.compile("^https://github\\.com/([^/]+)/([^/#]+)(?:#(.+))?$");
  private static final int DOWNLOAD_POOL_SIZE = 30; // Number of concurrent downloads
  private static final String ETAGS_DIR_NAME = "etags";
  private static final String BUNDLE_INFO_FILE = "apps-bundle.json";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
   * Record to hold parsed information from a standard GitHub URL.
   *
   * @param owner The repository owner.
   * @param repo The repository name.
   * @param ref The branch, tag, or commit reference.
   * @param originalUrl The original URL string from the input file.
   * @param codeloadUrl The converted URL for downloading the ZIP archive.
   */
  private record AppGithubRepo(
      String owner, String repo, String ref, String originalUrl, String codeloadUrl) {}

  /**
   * Executes the app bundling process.
   *
   * @throws IOException if there's an error reading the app list or downloading the apps
   */
  public void execute() throws IOException {
    Path downloadDirPath = Paths.get(downloadDir);
    Files.createDirectories(downloadDirPath);

    Path artifactDirPath = downloadDirPath.resolve(artifactId);
    Files.createDirectories(artifactDirPath);

    Path etagsDirPath = artifactDirPath.resolve(ETAGS_DIR_NAME);
    Files.createDirectories(etagsDirPath);

    List<AppGithubRepo> appRepoInfos = parseAppListUrls(appListFilePath);

    info("Found {} valid apps to bundle", appRepoInfos.size());

    // Download each app in parallel
    List<BundledAppInfo> downloadedApps = downloadApps(appRepoInfos, artifactDirPath, etagsDirPath);

    downloadedApps.forEach(bundleInfo::addApp);

    // Copy downloaded apps to build directory
    Path targetArtifactDir = copyAppsToBuildDir(downloadDirPath);

    enrichBundleInfo(targetArtifactDir);

    // Write bundle info file
    writeBundleFile(targetArtifactDir);
  }

  /**
   * Finds a ZIP entry by filename, searching through all entries in the ZIP file.
   *
   * @param zipFile the ZIP file to search
   * @param filename the name of the file to find
   * @return the first matching ZipEntry, or null if not found
   */
  private ZipEntry findEntryByFilename(ZipFile zipFile, String filename) {
    ZipEntry shallowestEntry = null;
    int minDepth = Integer.MAX_VALUE;

    var entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String entryName = entry.getName();

      // Check if this entry matches the filename we're looking for
      if (entryName.endsWith("/" + filename) || entryName.equals(filename)) {
        // Calculate the depth (number of directory separators)
        int depth = (int) entryName.chars().filter(ch -> ch == '/').count();

        // Prefer entries at shallower depths
        if (depth < minDepth) {
          minDepth = depth;
          shallowestEntry = entry;
        }
      }
    }

    return shallowestEntry;
  }

  private void enrichBundleInfo(Path targetPath) {
    for (BundledAppInfo app : bundleInfo.getApps()) {
      Path appPath = targetPath.resolve(app.getName() + ".zip");
      if (Files.exists(appPath)) {
        try (ZipFile zipFile = new ZipFile(appPath.toFile())) {
          ZipEntry buildInfoEntry = findEntryByFilename(zipFile, "BUILD_INFO");
          ZipEntry manifestEntry = findEntryByFilename(zipFile, "package.json");

          if (manifestEntry != null) {
            String manifestContent =
                new String(
                    zipFile.getInputStream(manifestEntry).readAllBytes(), StandardCharsets.UTF_8);
            JsonNode manifestNode = OBJECT_MAPPER.readTree(manifestContent);
            String version = manifestNode.get("version").asText();
            app.setVersion(version);
          }

          if (buildInfoEntry != null) {
            String buildInfoContent =
                new String(
                    zipFile.getInputStream(buildInfoEntry).readAllBytes(), StandardCharsets.UTF_8);
            String[] info = buildInfoContent.split("\\R");
            if (info.length > 2) {
              app.setBuildDate(info[0].trim());
              app.setCommitUrl(info[2].trim());
            }
          }
        } catch (IOException e) {
          error("Error opening zip file for app {}: {}", app.getName(), e, e.getMessage());
        }
      }
    }
  }

  private List<BundledAppInfo> downloadApps(
      List<AppGithubRepo> appRepoInfos, Path artifactDirPath, Path etagDirPath) {
    ForkJoinPool customThreadPool = new ForkJoinPool(DOWNLOAD_POOL_SIZE);
    try {
      return customThreadPool
          .submit(
              () ->
                  appRepoInfos.parallelStream()
                      .map(
                          repoInfo -> {
                            try {
                              return downloadApp(repoInfo, artifactDirPath, etagDirPath);
                            } catch (IOException e) {
                              // error is logged in downloadApp()
                              return null;
                            }
                          })
                      .filter(Objects::nonNull)
                      .toList())
          .get();

    } catch (InterruptedException | ExecutionException e) {
      error("Error during parallel app download", e);
      // Restore interrupt status
      Thread.currentThread().interrupt();
    } finally {
      customThreadPool.shutdown();
    }
    throw new IllegalStateException("Failed to download apps");
  }

  /**
   * Downloads a file if it has changed since the last download.
   *
   * @param fileUrl the URL of the file to download
   * @param outputPath the path where the file will be saved
   * @param etagPath the path to the etag file
   * @return the ETag of the downloaded file, or the previous ETag if the file hasn't changed
   * @throws IOException if there's an error downloading the file
   */
  private String downloadIfChanged(String fileUrl, Path outputPath, Path etagPath)
      throws IOException {
    HttpURLConnection connection = getHttpURLConnection(fileUrl);

    // Check if we have a previous ETag
    if (Files.exists(etagPath)) {
      String previousEtag = Files.readString(etagPath);
      connection.setRequestProperty("If-None-Match", "\"" + previousEtag + "\"");
    }

    connection.connect();

    int responseCode = connection.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
      return null;
    }
    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new IOException("HTTP error code: " + responseCode + " for URL: " + fileUrl);
    }

    // Get the ETag from the response header
    String etag = connection.getHeaderField("ETag");
    if (etag != null) {
      etag = etag.replace("\"", ""); // Remove quotes from ETag
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
      Files.writeString(etagPath, etag);
    }
    return etag;
  }

  private void writeBundleFile(Path targetArtifactDir) throws IOException {
    Path bundleInfoPath = targetArtifactDir.resolve(BUNDLE_INFO_FILE);
    OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(bundleInfoPath.toFile(), bundleInfo);
    info("Wrote bundle info to: {}", bundleInfoPath);
  }

  private @Nonnull Path copyAppsToBuildDir(Path downloadDirPath) throws IOException {
    Path targetArtifactDir = Paths.get(buildDir).resolve(artifactId);
    try {
      Files.createDirectories(targetArtifactDir);

      for (BundledAppInfo app : bundleInfo.getApps()) {
        Path sourceZipPath = downloadDirPath.resolve(artifactId).resolve(app.getName() + ".zip");
        Path destZipPath = targetArtifactDir.resolve(app.getName() + ".zip");

        if (Files.exists(sourceZipPath)) {
          Files.copy(sourceZipPath, destZipPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } else {
          error(
              "Source zip file not found, skipping copy for app {}: {}",
              app.getName(),
              sourceZipPath);
        }
      }
      info("Finished copying app bundles to build directory.");

    } catch (IOException e) {
      error("Error copying app bundles to build directory: {}", e, e.getMessage());
      throw new IOException("Failed to copy app bundles to build directory", e);
    }
    return targetArtifactDir;
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
  private List<AppGithubRepo> parseAppListUrls(String appListPath) throws IOException {
    List<String> rawUrls =
        OBJECT_MAPPER.readValue(
            new File(appListPath),
            OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));

    List<AppGithubRepo> appInfos = new ArrayList<>();

    for (String rawUrl : rawUrls) {
      Matcher matcher = GITHUB_URL_PATTERN.matcher(rawUrl);
      if (matcher.matches()) {
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String ref = matcher.group(3);

        String codeloadUrl = convertToCodeloadUrl(rawUrl);
        if (codeloadUrl != null) {
          appInfos.add(new AppGithubRepo(owner, repo, ref, rawUrl, codeloadUrl));
        } else {
          error("Skipping app due to failed conversion from URL: {}", rawUrl);
        }
      } else {
        error("Skipping app due to invalid GitHub URL format: {}", rawUrl);
      }
    }
    info("Successfully parsed {} app URLs out of {}", appInfos.size(), rawUrls.size());
    return appInfos;
  }

  /**
   * Downloads an app from GitHub as a ZIP file.
   *
   * <p>Downloads an app from GitHub as a ZIP file using pre-parsed info.
   *
   * @param repoInfo the parsed repository information including the codeload URL
   * @param targetDir the directory where the app ZIP file will be stored
   * @param etagsDir the directory where the etags files will be stored
   * @return the downloaded AppBundleInfo.AppInfo object containing ETag etc.
   * @throws IOException if there's an error downloading the app
   */
  private BundledAppInfo downloadApp(AppGithubRepo repoInfo, Path targetDir, Path etagsDir)
      throws IOException {
    BundledAppInfo appBundleResultInfo = new BundledAppInfo();
    appBundleResultInfo.setName(repoInfo.repo());
    appBundleResultInfo.setUrl(repoInfo.originalUrl());
    appBundleResultInfo.setBranch(repoInfo.ref());

    try {
      String zipName = repoInfo.repo() + ".zip";
      Path zipFilePath = targetDir.resolve(zipName);
      Path etagFile = etagsDir.resolve(repoInfo.repo() + ".etag");

      String etag = downloadIfChanged(repoInfo.codeloadUrl(), zipFilePath, etagFile);
      if (etag == null) { // Not modified or download failed ETag retrieval
        if (Files.exists(etagFile)) {
          appBundleResultInfo.setEtag(Files.readString(etagFile, StandardCharsets.UTF_8));
          info("App {} is already up to date (ETag: {})", zipName, appBundleResultInfo.getEtag());
        } else {
          // This case might happen if download failed before ETag was written,
          // or if ETag header was missing. Log a warning.
          error("Could not determine ETag for app {}, etag file missing.", zipName);
        }
      } else {
        appBundleResultInfo.setEtag(etag);
        info("Downloaded app: {} (ETag: {})", zipName, etag);
      }
    } catch (IOException e) {
      error("Failed to download or process app {}: {}", e, repoInfo.originalUrl(), e.getMessage());
      throw e;
    }

    return appBundleResultInfo;
  }

  private static @Nonnull HttpURLConnection getHttpURLConnection(String fileUrl)
      throws IOException {
    URL url = new URL(fileUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    return connection;
  }

  /**
   * Converts a standard GitHub repository URL format "https://github.com/owner/repo#branch" into
   * "codeload.github.com/owner/repo/zip/branch" URL format for downloading a ZIP archive.
   *
   * @param githubUrl The standard GitHub URL (e.g.,
   *     "https://github.com/d2-ci/datastore-app#patch/2.42.0").
   * @return The corresponding codeload URL (e.g.,
   *     "https://codeload.github.com/d2-ci/datastore-app/zip/patch/2.42.0"), or null if the input
   *     URL format is invalid.
   */
  private String convertToCodeloadUrl(String githubUrl) {
    if (githubUrl == null) {
      return null;
    }
    Matcher matcher = GITHUB_URL_PATTERN.matcher(githubUrl);
    if (matcher.matches()) {
      String owner = matcher.group(1);
      String repo = matcher.group(2);
      String ref = matcher.group(3);
      if (ref == null) {
        ref = "refs/heads/" + defaultBranch;
      }
      return String.format("https://codeload.github.com/%s/%s/zip/%s", owner, repo, ref);
    } else {
      error("Invalid GitHub URL format for conversion: {}", githubUrl);
      return null;
    }
  }

  /**
   * Main method to run the app bundler as a standalone program.
   *
   * @param args command-line arguments
   */
  public static void main(String[] args) {
    try {
      String downloadDir = System.getProperty("DOWNLOAD_DIR");
      String buildDir = System.getProperty("BUILD_DIR");
      String artifactId = System.getProperty("ARTIFACT_ID");
      String appListPath = System.getProperty("APP_LIST");
      String defaultBranch = System.getProperty("DEFAULT_BRANCH");
      info("DOWNLOAD_DIR: {}", downloadDir);
      info("BUILD_DIR: {}", buildDir);
      info("ARTIFACT_ID: {}", artifactId);
      info("APP_LIST: {}", appListPath);
      info("DEFAULT_BRANCH: {}", defaultBranch);

      if (downloadDir == null || buildDir == null || artifactId == null || appListPath == null) {
        error("System properties DOWNLOAD_DIR, BUILD_DIR, ARTIFACT_ID, and APP_LIST must be set");
        System.exit(1);
      }
      AppBundler bundler =
          new AppBundler(downloadDir, buildDir, artifactId, appListPath, defaultBranch);
      bundler.execute();
    } catch (Exception e) {
      error("Error executing app bundler: {}", e, e.getMessage());
      System.exit(1);
    }
  }

  private static void error(String content) {
    System.err.println(content);
  }

  private static void error(String content, Object... args) {
    error(content, null, args);
  }

  private static void error(String content, Throwable error, Object... args) {
    StringWriter sWriter = new StringWriter();
    if (error != null) {
      PrintWriter pWriter = new PrintWriter(sWriter);
      error.printStackTrace(pWriter);
    }
    content = replaceArgs(content, args);

    if (error != null) {
      System.err.println(content + System.lineSeparator() + sWriter);
    } else {
      System.err.println(content);
    }
  }

  private static void info(String content, Object... args) {
    content = replaceArgs(content, args);
    System.out.println(content);
  }

  private static String replaceArgs(String content, Object[] args) {
    if (args == null) {
      return content;
    }
    for (Object arg : args) {
      String replacement = (arg == null) ? "null" : Matcher.quoteReplacement(arg.toString());
      content = content.replaceFirst(Pattern.quote("{}"), replacement);
    }
    return content;
  }
}
