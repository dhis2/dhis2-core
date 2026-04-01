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
import org.hisp.dhis.datastore.DatastoreNamespace;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.storage.BlobKey;
import org.hisp.dhis.storage.BlobKeyPrefix;
import org.hisp.dhis.storage.BlobStoreService;
import org.hisp.dhis.util.ZipBombException;
import org.hisp.dhis.util.ZipFileUtils;
import org.hisp.dhis.util.ZipSlipException;
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

  private final BlobStoreService blobStore;
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
    for (BlobKeyPrefix folder : blobStore.listFolders(BlobKeyPrefix.of(APPS_DIR))) {
      BlobKey manifestKey = folder.resolve(MANIFEST_WEBAPP_FILENAME);
      InputStream manifestStream = blobStore.openStream(manifestKey);
      if (manifestStream == null) {
        log.error(
            "Could not find manifest file in app folder '{}', with key '{}' skipping app.",
            folder,
            manifestKey);
        continue;
      }
      handleAppManifest(handler, folder, manifestStream);
    }
  }

  private void handleAppManifest(
      BiConsumer<App, BundledAppInfo> handler, BlobKeyPrefix folder, InputStream manifestStream) {
    try (InputStream inputStream = manifestStream) {
      App app = App.MAPPER.readValue(inputStream, App.class);
      app.setAppStorageSource(AppStorageSource.JCLOUDS);
      app.setFolderName(folder.value());
      app.setManifestTranslations(
          readAppManifestTranslations(
              blobStore.openStream(
                  folder.resolve(AppStorageService.MANIFEST_TRANSLATION_FILENAME))));

      InputStream bundledAppInfoStream =
          blobStore.openStream(folder.resolve(BUNDLED_APP_INFO_FILENAME));
      if (bundledAppInfoStream != null) {
        try (InputStream bis = bundledAppInfoStream) {
          handler.accept(app, App.MAPPER.readValue(bis, BundledAppInfo.class));
        }
      } else {
        handler.accept(app, null);
      }
    } catch (IOException ex) {
      log.error("Could not read manifest file of '{}'", folder, ex);
    }
  }

  private List<AppManifestTranslation> readAppManifestTranslations(
      @CheckForNull InputStream translationsStream) {
    if (translationsStream == null) {
      return Collections.emptyList();
    }
    try (InputStream inputStream = translationsStream) {
      return App.MAPPER.readerForListOf(AppManifestTranslation.class).readValue(inputStream);
    } catch (IOException e) {
      log.error(
          "An error occurred trying to read the app manifest translations '{}'",
          e.getLocalizedMessage());
      return Collections.emptyList();
    }
  }

  private boolean validateApp(App app, Cache<App> appCache) {
    validateAppNamespaceNotAlreadyInUse(app, appCache);
    validateAppAdditionalNamespacesAreWellDefined(app);
    return app.getAppState().ok();
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
    AppFolderName folder;
    String topLevelFolder;
    try {
      topLevelFolder = ZipFileUtils.getTopLevelFolder(file);
      app = AppManager.readAppManifest(file, this.jsonMapper, topLevelFolder);
      folder = AppFolderName.forApp(app.getKey());
      app.setFolderName(folder.value());
      app.setAppStorageSource(AppStorageSource.JCLOUDS);
    } catch (IOException e) {
      log.error("Failed to install app: Failure during reading manifest from zip file", e);
      app = new App();
      app.setAppState(AppStatus.MISSING_MANIFEST);
      return app;
    }

    if (bundledAppInfo != null) {
      try {
        writeBundledAppInfo(bundledAppInfo, folder);
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
      ZipFileUtils.validateZip(file, folder.value(), topLevelFolder);
      unzipFile(file, folder, topLevelFolder);

      removeOtherAppsWithSameKey(app);

      app.setAppState(AppStatus.OK);
      logInstallSuccess(app, folder.value());
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

  private void unzipFile(File file, AppFolderName folder, String topLevelFolder)
      throws IOException, ZipSlipException {
    try (ZipFile zipFile = new ZipFile(file)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();
        String filePath = getFilePath(folder.value(), topLevelFolder, zipEntry);
        // If it's the root folder, skip
        if (filePath == null) continue;
        try (InputStream zipInputStream = zipFile.getInputStream(zipEntry)) {
          blobStore.putBlob(
              new BlobKey(filePath), zipInputStream, zipEntry.getSize(), null, null, null);
        }
      }
    }
  }

  private void writeBundledAppInfo(BundledAppInfo bundledAppInfo, AppFolderName folder)
      throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    jsonMapper.writerWithDefaultPrettyPrinter().writeValue(baos, bundledAppInfo);
    byte[] bundledAppInfoBytes = baos.toByteArray();
    blobStore.putBlob(
        folder.resolve(BUNDLED_APP_INFO_FILENAME),
        new ByteArrayInputStream(bundledAppInfoBytes),
        bundledAppInfoBytes.length,
        null,
        null,
        null);
  }

  /**
   * Removes all other apps with the same key as the one we are installing.
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
    // Delete the manifest file first in case the system crashes during deletion
    // so the app cannot be re-discovered in a partially-deleted state.
    AppFolderName folder = new AppFolderName(app.getFolderName());
    blobStore.deleteBlob(folder.resolve(MANIFEST_WEBAPP_FILENAME));

    if (blobStore.isFilesystem()) {
      // Delete all files related to app (works for local filestore)
      blobStore.deleteDirectory(folder.asPrefix());
    } else {
      // S3: deleteDirectory is not recursive, so enumerate and delete individually
      for (BlobKey key : blobStore.listKeys(folder.asPrefix())) {
        log.debug("Deleting app file: {}", key);
        blobStore.deleteBlob(key);
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

    String normalized = resource.startsWith("/") ? resource.substring(1) : resource;
    String resolvedFileResource = useIndexHtmlIfDirCall(normalized);
    BlobKey key = new BlobKey(app.getFolderName() + "/" + resolvedFileResource);

    log.debug("Checking if blob exists {} for App {}", key, app.getName());
    if (blobStore.blobExists(key)) {
      return new ResourceFound(getResource(key));
    }
    if (keyExistsAsDirectory(key)) {
      return new Redirect(resource + "/");
    }
    log.debug("ResourceNotFound {} for App {}", key, app.getName());
    return new ResourceNotFound(resource);
  }

  private boolean keyExistsAsDirectory(BlobKey key) {
    return blobStore.listKeys(BlobKeyPrefix.of(key.value())).iterator().hasNext();
  }

  private Resource getResource(@Nonnull BlobKey key) throws MalformedURLException {
    if (blobStore.isFilesystem()) {
      return new FileSystemResource(
          locationManager.getFileForReading(blobStore.container().value() + "/" + key.value()));
    } else {
      URI uri = fileResourceContentStore.getSignedGetContentUri(key);
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
