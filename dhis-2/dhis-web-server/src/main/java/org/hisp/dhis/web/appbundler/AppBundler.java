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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static final Pattern REPO_PATTERN =
      Pattern.compile("https://codeload.github.com/([^/]+)/([^/]+)/zip/refs/heads/([^/]+)");

  private static final String DEFAULT_BRANCH = "master";
  private static final String CHECKSUM_DIR = "checksums";
  private static final String BUNDLE_INFO_FILE = "apps-bundle.json";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final String downloadDir;
  private final String artifactId;
  private final String appListPath;
  private final String defaultBranch;
  private final AppBundleInfo bundleInfo;
  private final String buildDir;

  /**
   * Constructs an AppBundler with the specified parameters.
   *
   * @param downloadDir the directory where the app ZIP files will be stored
   * @param artifactId the artifact ID for the bundle
   * @param appListPath the path to the JSON file containing the list of apps to bundle
   * @param defaultBranch the default branch to use if none is specified in the app URL
   */
  public AppBundler(String downloadDir, String buildDir, String artifactId, String appListPath, String defaultBranch) {
    this.downloadDir = downloadDir;
    this.buildDir = buildDir;
    this.artifactId = artifactId;
    this.appListPath = appListPath;
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
    logger.error("App list path: {}", appListPath);
    logger.error("Default branch: {}", defaultBranch);

    // Create download directory if it doesn't exist
    Path downloadDirPath = Paths.get(downloadDir);
    Files.createDirectories(downloadDirPath);

    // Create download artifact directory
    Path artifactDir = downloadDirPath.resolve(artifactId);
    Files.createDirectories(artifactDir);

    // Create download checksums directory
    Path checksumDir = artifactDir.resolve(CHECKSUM_DIR);
    Files.createDirectories(checksumDir);

    // Read app list
    List<String> appUrls = readAppList();
    logger.error("Found {} apps to bundle", appUrls.size());

    // Download each app
    for (String appUrl : appUrls) {
      try {
        downloadApp(appUrl, artifactDir, checksumDir);
      } catch (Exception e) {
        logger.error("Error downloading app {}: {}", appUrl, e.getMessage(), e);
      }
    }

    // --- Start Copying downloaded apps to build directory ---
    Path targetArtifactDir = Paths.get(buildDir).resolve(artifactId);
    try {
      Files.createDirectories(targetArtifactDir);
      logger.error("Ensured target artifact directory exists: {}", targetArtifactDir);

      for (AppBundleInfo.AppInfo app : bundleInfo.getApps()) {
        Path sourceZipPath = downloadDirPath.resolve(artifactId).resolve(app.getName() + ".zip");
        Path destZipPath = targetArtifactDir.resolve(app.getName() + ".zip");

        if (Files.exists(sourceZipPath)) {
          Files.copy(
              sourceZipPath, destZipPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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

    // Write bundle info file
    Path bundleInfoPath = targetArtifactDir.resolve(BUNDLE_INFO_FILE);
    objectMapper.writerWithDefaultPrettyPrinter().writeValue(bundleInfoPath.toFile(), bundleInfo);
    logger.error("Wrote bundle info to {}", bundleInfoPath);
    logger.error("App bundling process completed successfully");
  }

  /**
   * Reads the list of app URLs from the JSON file.
   *
   * @return a list of app URLs
   * @throws IOException if there's an error reading the file
   */
  private List<String> readAppList() throws IOException {
    File appListFile = new File(appListPath);
    CollectionType type =
        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
    return objectMapper.readValue(appListFile, type);
  }

  /**
   * Downloads an app from GitHub as a ZIP file.
   *
   * @param appUrl the GitHub URL of the app
   * @param targetDir the directory where the app ZIP file will be stored
   * @param checksumDir the directory where the checksum files will be stored
   * @throws IOException if there's an error downloading the app
   */
  private void downloadApp(String appUrl, Path targetDir, Path checksumDir) throws IOException {
    Matcher matcher = REPO_PATTERN.matcher(appUrl);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid GitHub URL: " + appUrl);
    }

    String owner = matcher.group(1);
    String repo = matcher.group(2);
    String branch = matcher.group(3) != null ? matcher.group(3) : defaultBranch;

    logger.error("Processing app: {}/{} (branch: {})", owner, repo, branch);

    // Generate a filename for the ZIP file
    String zipName = repo + ".zip";
    Path zipPath = targetDir.resolve(zipName);

    // Create app info object
    AppBundleInfo.AppInfo appInfo = new AppBundleInfo.AppInfo();
    appInfo.setName(repo);
    appInfo.setUrl(appUrl);
    appInfo.setBranch(branch);

    // Check if we already have the latest version
    String etag = downloadIfChanged(appUrl, zipPath, checksumDir.resolve(repo + ".checksum"));
    if (etag == null) {
      appInfo.setEtag(Files.readString(checksumDir.resolve(repo + ".checksum")));
    } else {
      appInfo.setEtag(etag);
    }
    // Add app info to bundle info
    bundleInfo.addApp(appInfo);

    if (etag != null) {
      logger.error("Downloaded app: {} (ETag: {})", zipName, etag);
    } else {
      logger.error("App {} is already up to date", zipName);
    }
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

    logger.error("Downloading {} to {}", fileUrl, outputPath);
    logger.error("Checksum file: {}", checksumPath);

    // Check if we have a previous ETag
    if (Files.exists(checksumPath)) {
      String previousEtag = new String(Files.readAllBytes(checksumPath), StandardCharsets.UTF_8);
      logger.error("Previous ETag: {}", previousEtag);
      connection.setRequestProperty("If-None-Match", "\"" + previousEtag + "\"");
    }

    connection.connect();
    Map<String, List<String>> headers = connection.getHeaderFields();
    for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
      logger.error("Response header: {} = {}", entry.getKey(), String.join(", ", entry.getValue()));
    }
    int responseCode = connection.getResponseCode();

    logger.error("Response code: {}", responseCode);

    if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
      // File hasn't changed
      logger.error("File not modified: {}", fileUrl);
      return null;
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
        logger.error("System properties DOWNLOAD_DIR, BUILD_DIR, ARTIFACT_ID, and APPS must be set");
        System.exit(1);
      }

      AppBundler bundler = new AppBundler(downloadDir, buildDir, artifactId, appListPath, defaultBranch);
      bundler.execute();
    } catch (Exception e) {
      logger.error("Error executing app bundler: {}", e.getMessage(), e);
      System.exit(1);
    }
  }
}
