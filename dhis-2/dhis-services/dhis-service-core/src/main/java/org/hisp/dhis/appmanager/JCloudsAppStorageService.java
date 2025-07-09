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

import static org.hisp.dhis.util.ZipFileUtils.getFilePath;
import static org.jclouds.blobstore.options.ListContainerOptions.Builder.prefix;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.appmanager.AppBundleInfo.BundledAppInfo;
import org.hisp.dhis.appmanager.ResourceResult.Redirect;
import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.appmanager.ResourceResult.ResourceNotFound;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.datastore.DatastoreNamespace;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.jclouds.JCloudsStore;
import org.hisp.dhis.util.ZipBombException;
import org.hisp.dhis.util.ZipFileUtils;
import org.hisp.dhis.util.ZipSlipException;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

/**
 * @author Stian Sandvold
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.appmanager.JCloudsAppStorageService")
public class JCloudsAppStorageService implements AppStorageService {

  private static final String BUNDLED_APP_INFO_FILENAME = "bundled-app-info.json";
  public static final String MANIFEST_WEBAPP_FILENAME = "manifest.webapp";

  private final JCloudsStore jCloudsStore;
  private final LocationManager locationManager;
  private final ObjectMapper jsonMapper;
  private final FileResourceContentStore fileResourceContentStore;

  @Override
  @Nonnull
  public Map<String, Pair<App, BundledAppInfo>> discoverInstalledApps() {
    Map<String, Pair<App, BundledAppInfo>> apps = new HashMap<>();
    discoverInstalledApps((app, appInfo) -> apps.put(app.getKey(), Pair.of(app, appInfo)));
    logDiscoveredApps(apps);
    return apps;
  }

  private void discoverInstalledApps(BiConsumer<App, BundledAppInfo> handler) {
    PageSet<? extends StorageMetadata> allAppFolders =
        jCloudsStore.getBlobList(prefix(APPS_DIR + "/").delimiter("/"));

    for (StorageMetadata resource : allAppFolders) {
      String blobKey = resource.getName() + MANIFEST_WEBAPP_FILENAME;
      Blob manifest = jCloudsStore.getBlob(blobKey);
      if (manifest == null) {
        log.error(
            "Could not find manifest file in app folder '{}', with key '{}' skipping app.",
            resource.getName(),
            blobKey);
        continue;
      }

      try (InputStream inputStream = manifest.getPayload().openStream()) {
        App app = App.MAPPER.readValue(inputStream, App.class);
        app.setAppStorageSource(AppStorageSource.JCLOUDS);
        app.setFolderName(resource.getName().replaceAll("/$", ""));

        Blob translationFile =
            jCloudsStore.getBlob(
                resource.getName() + AppStorageService.MANIFEST_TRANSLATION_FILENAME);
        List<AppManifestTranslation> translations = readAppManifestTranslations(translationFile);
        app.setManifestTranslations(translations);

        Blob bundledAppInfo = jCloudsStore.getBlob(resource.getName() + BUNDLED_APP_INFO_FILENAME);
        if (bundledAppInfo == null) {
          handler.accept(app, null);
        } else {
          try (InputStream bundledAppInfoStream = bundledAppInfo.getPayload().openStream()) {
            BundledAppInfo appInfo =
                App.MAPPER.readValue(bundledAppInfoStream, BundledAppInfo.class);
            app.setBundled(true);
            handler.accept(app, appInfo);
          }
        }
      } catch (IOException ex) {
        log.error("Could not read manifest file of '{}'", resource.getName(), ex);
      }
    }
  }

  private List<AppManifestTranslation> readAppManifestTranslations(Blob manifestTranslationsFile) {
    if (manifestTranslationsFile == null) {
      return Collections.emptyList();
    }
    try (InputStream inputStream = manifestTranslationsFile.getPayload().openStream()) {
      return App.MAPPER.readerForListOf(AppManifestTranslation.class).readValue(inputStream);
    } catch (IOException e) {
      log.error(
          "An error occurred trying to read the app manifest translations '{}'",
          e.getLocalizedMessage());
      return Collections.emptyList();
    }
  }

  private boolean validateApp(App app, Cache<App> appCache) {
    validateAppDeletionNotInProgress(app, appCache);
    validateAppNamespaceNotAlreadyInUse(app, appCache);
    validateAppAdditionalNamespacesAreWellDefined(app);
    return app.getAppState().ok();
  }

  private void validateAppDeletionNotInProgress(App app, Cache<App> appCache) {
    if (!app.getAppState().ok()) return;
    Optional<App> existingApp = appCache.getIfPresent(app.getKey());
    if (existingApp.isPresent()
        && existingApp.get().getAppState() == AppStatus.DELETION_IN_PROGRESS) {
      log.error("Failed to install app: App with same name is currently being deleted");

      app.setAppState(AppStatus.DELETION_IN_PROGRESS);
    }
  }

  private void validateAppNamespaceNotAlreadyInUse(App app, Cache<App> appCache) {
    if (!app.getAppState().ok()) return;
    AppDhis dhis = app.getActivities().getDhis();
    String namespace = dhis.getNamespace();
    Set<String> namespaces = new HashSet<>();
    if (namespace != null && !namespace.isEmpty()) namespaces.add(namespace);
    List<DatastoreNamespace> additionalNamespaces = dhis.getAdditionalNamespaces();
    if (additionalNamespaces != null)
      additionalNamespaces.forEach(ns -> namespaces.add(ns.getNamespace()));

    if (namespaces.isEmpty()) return;
    for (String ns : namespaces) {
      Optional<App> other =
          appCache.getAll().filter(a -> a.getNamespaces().contains(ns)).findFirst();
      if (other.isPresent() && !other.get().getKey().equals(app.getKey())) {
        log.error(
            "Failed to install app '{}': Namespace '{}' already taken.", app.getName(), namespace);
        app.setAppState(AppStatus.NAMESPACE_TAKEN);
        return;
      }
    }
  }

  private void validateAppAdditionalNamespacesAreWellDefined(App app) {
    if (!app.getAppState().ok()) return;
    List<DatastoreNamespace> additionalNamespaces =
        app.getActivities().getDhis().getAdditionalNamespaces();
    if (additionalNamespaces != null) {
      for (DatastoreNamespace ns : additionalNamespaces) {
        if (ns.getNamespace() == null
            || ns.getNamespace().isEmpty()
            || ns.getAllAuthorities().isEmpty()) {
          log.error(
              "Failed to install app '{}': Required property is undefined, namespace '{}', authorities '{}'.",
              app.getName(),
              ns.getNamespace(),
              ns.getAllAuthorities());
          app.setAppState(AppStatus.NAMESPACE_INVALID);
          return;
        }
      }
    }
  }

  @Override
  @Nonnull
  public App installApp(
      @Nonnull File file,
      @Nonnull Cache<App> appCache,
      @CheckForNull BundledAppInfo bundledAppInfo) {
    App app;
    String topLevelFolder;
    String installationFolder;
    try {
      topLevelFolder = ZipFileUtils.getTopLevelFolder(file);
      app = AppManager.readAppManifest(file, this.jsonMapper, topLevelFolder);
      installationFolder = getInstallationFolder(app);
      app.setFolderName(installationFolder);
      app.setAppStorageSource(AppStorageSource.JCLOUDS);
    } catch (IOException e) {
      log.error("Failed to install app: Failure during reading manifest from zip file", e);
      app = new App();
      app.setAppState(AppStatus.MISSING_MANIFEST);
      return app;
    }

    if (bundledAppInfo != null) {
      try {
        writeBundledAppInfo(bundledAppInfo, installationFolder);
      } catch (IOException e) {
        log.error("Failed to install app: Failure during writing bundled app info");
        app.setAppState(AppStatus.FAILED_TO_WRITE_BUNDLED_APP_INFO);
        return app;
      }
    }

    if (!validateApp(app, appCache)) {
      log.error("Failed to install app: App validation failed");
      return app;
    }

    try {
      ZipFileUtils.validateZip(file, installationFolder, topLevelFolder);
      unzipFile(file, installationFolder, topLevelFolder);

      removeOtherAppsWithSameKey(app);

      app.setAppState(AppStatus.OK);
      logInstallSuccess(app, installationFolder);
      return app;

    } catch (IOException e) {
      log.error("Failed to install app: IO Failure during unzipping", e);
      app.setAppState(AppStatus.INVALID_ZIP_FORMAT);
    } catch (ZipBombException e) {
      log.error("Failed to install app: Possible ZipBomb detected", e);
      app.setAppState(AppStatus.INVALID_ZIP_FORMAT);
    } catch (ZipSlipException e) {
      log.error("Failed to install app: Possible ZipSlip detected", e);
      app.setAppState(AppStatus.INVALID_ZIP_FORMAT);
    }

    if (!app.getAppState().ok()) {
      deleteApp(app);
    }

    return app;
  }

  /**
   * Add a random generated part on the installation folder to avoid collisions.
   *
   * @param app the app manifest
   * @return the name of the folder to install the app
   */
  private String getInstallationFolder(App app) {
    String appKey = app.getKey();
    String folderName =
        appKey.length() > 32
            ? appKey.substring(0, 31)
            : appKey + "_" + CodeGenerator.getRandomSecureToken();
    return APPS_DIR + File.separator + folderName;
  }

  private void unzipFile(File file, String installationFolder, String topLevelFolder)
      throws IOException, ZipSlipException {
    try (ZipFile zipFile = new ZipFile(file)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();
        String filePath = getFilePath(installationFolder, topLevelFolder, zipEntry);
        // If it's the root folder, skip
        if (filePath == null) continue;
        try (InputStream zipInputStream = zipFile.getInputStream(zipEntry)) {
          Blob blob =
              jCloudsStore
                  .getBlobStore()
                  .blobBuilder(filePath)
                  .payload(zipInputStream)
                  .contentLength(zipEntry.getSize())
                  .build();
          jCloudsStore.putBlob(blob);
        }
      }
    }
  }

  // Create the BundledAppInfo JSON file and write it to JClouds storage
  private void writeBundledAppInfo(BundledAppInfo bundledAppInfo, String installationFolder)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    jsonMapper.writerWithDefaultPrettyPrinter().writeValue(baos, bundledAppInfo);
    byte[] bundledAppInfoBytes = baos.toByteArray();
    ByteArrayInputStream bais = new ByteArrayInputStream(bundledAppInfoBytes);
    Blob bundledAppInfoBlob =
        jCloudsStore
            .getBlobStore()
            .blobBuilder(installationFolder + File.separator + BUNDLED_APP_INFO_FILENAME)
            .payload(bais)
            .contentLength(bundledAppInfoBytes.length)
            .build();
    jCloudsStore.putBlob(bundledAppInfoBlob);
    bais.close();
    baos.close();
  }

  /**
   * Simpy removes all other apps with the same key as the one we are installing.
   *
   * @param newApp the manifest of the app we are trying to install
   */
  private void removeOtherAppsWithSameKey(@Nonnull App newApp) {
    discoverInstalledApps(
        (a, bai) -> {
          if (newApp.getKey().equals(a.getKey())
              && !newApp.getFolderName().equals(a.getFolderName())) deleteApp(a);
        });
  }

  @Override
  public void deleteApp(@Nonnull App app) {
    // delete the manifest file first in case the system crashes during deletion
    // and the manifest file is not deleted, resulting in an app that can't be installed
    String folderName = app.getFolderName();
    jCloudsStore.removeBlob(folderName + File.separator + MANIFEST_WEBAPP_FILENAME);

    if (jCloudsStore.isUsingFileSystem()) {
      // Delete all files related to app (works for local filestore):
      jCloudsStore.deleteDirectory(folderName);
    } else {
      // slower but works for S3:
      // Delete all files related to app
      ListContainerOptions options = prefix(folderName).recursive();
      for (StorageMetadata resource : jCloudsStore.getBlobList(options)) {
        log.debug("Deleting app file: {}", resource.getName());
        jCloudsStore.removeBlob(resource.getName());
      }
    }
    log.info("Deleted app {}", app.getName());
  }

  @Override
  @Nonnull
  public ResourceResult getAppResource(@CheckForNull App app, @Nonnull String resource)
      throws IOException {
    if (app == null || !app.getAppStorageSource().equals(AppStorageSource.JCLOUDS)) {
      log.warn(
          "Can't look up resource {}. The specified app was not found in JClouds storage.",
          resource);
      return new ResourceNotFound(resource);
    }
    if (resource.isBlank()) {
      return new Redirect("/");
    }

    String resolvedFileResource = useIndexHtmlIfDirCall(resource);
    String key = app.getFolderName() + File.separator + resolvedFileResource;
    String cleanedKey = key.replaceAll("/+", "/");

    log.debug("Checking if blob exists {} for App {}", cleanedKey, app.getName());
    if (jCloudsStore.blobExists(cleanedKey)) {
      return new ResourceFound(getResource(cleanedKey));
    }
    if (keyExistsAsDirectory(cleanedKey)) {
      return new Redirect(resource + "/");
    }
    log.debug("ResourceNotFound {} for App {}", cleanedKey, app.getName());
    return new ResourceNotFound(resource);
  }

  private boolean keyExistsAsDirectory(String cleanedKey) {
    return !jCloudsStore.getBlobList(prefix(cleanedKey)).isEmpty();
  }

  private Resource getResource(@Nonnull String filePath) throws MalformedURLException {
    if (jCloudsStore.isUsingFileSystem()) {
      String cleanedFilepath = jCloudsStore.getBlobContainer() + "/" + filePath;
      return new FileSystemResource(
          locationManager.getFileForReading((cleanedFilepath).replaceAll("/+", "/")));
    } else {
      URI uri = fileResourceContentStore.getSignedGetContentUri(filePath);
      return new UrlResource(uri);
    }
  }

  /**
   * The server is expected to return the 'index.html' for calls made to resources ending in '/'<br>
   *
   * <p>Examples: <br>
   * <li>'' -> ''
   * <li>'index.html' ->'index.html'
   * <li>'subDir/index.html' ->'subDir/index.html'
   * <li>'baseDir/' ->'baseDir/index.html'
   * <li>'baseDir/subDir/' ->'baseDir/subDir/index.html'
   * <li>'subDir' ->'subDir'
   * <li>'static/js/138.af8b0ff6.chunk.js' ->'static/js/138.af8b0ff6.chunk.js'
   *
   * @param resource app resource
   * @return potentially-updated app resource
   */
  private String useIndexHtmlIfDirCall(@Nonnull String resource) {
    if (resource.endsWith("/")) {
      log.debug("Resource ends with '/', appending 'index.html' to {}", resource);
      return resource + "index.html";
    }
    // any other resource, no special handling required, return as is
    return resource;
  }

  private static void logInstallSuccess(App app, String appFolder) {
    String namespace = app.getActivities().getDhis().getNamespace();
    log.info(
        "New app {} installed, Install path: {}, Namespace reserved: {}",
        app.getName(),
        appFolder,
        (namespace != null && !namespace.isEmpty() ? namespace : "no namespace reserved"));
  }

  private static void logDiscoveredApps(Map<String, Pair<App, BundledAppInfo>> apps) {
    if (apps.isEmpty()) {
      log.info("No apps found during JClouds discovery.");
    } else {
      apps.values()
          .forEach(
              pair ->
                  log.info("Discovered app '{}' from JClouds storage ", pair.getLeft().getName()));
    }
  }
}
