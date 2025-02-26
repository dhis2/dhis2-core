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
package org.hisp.dhis.appmanager;

import static org.jclouds.blobstore.options.ListContainerOptions.Builder.prefix;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.ResourceResult.Redirect;
import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.appmanager.ResourceResult.ResourceNotFound;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.datastore.DatastoreNamespace;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.fileresource.FileResourceContentStore;
import org.hisp.dhis.jclouds.JCloudsStore;
import org.hisp.dhis.util.ZipFileUtils;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.joda.time.Minutes;
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
  private static final long FIVE_MINUTES_IN_SECONDS =
      Minutes.minutes(5).toStandardDuration().getStandardSeconds();

  private final JCloudsStore jCloudsStore;

  private final LocationManager locationManager;

  private final ObjectMapper jsonMapper;
  private final FileResourceContentStore fileResourceContentStore;

  private void discoverInstalledApps(Consumer<App> handler) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    log.info("Starting JClouds discovery");
    for (StorageMetadata resource :
        jCloudsStore.getBlobList(prefix(APPS_DIR + "/").delimiter("/"))) {
      log.info("Found potential app: {}", resource.getName());

      // Found potential app
      Blob manifest = jCloudsStore.getBlob(resource.getName() + "manifest.webapp");

      if (manifest == null) {
        log.warn("Could not find manifest file of {}", resource.getName());
        continue;
      }

      try {
        InputStream inputStream = manifest.getPayload().openStream();
        App app = mapper.readValue(inputStream, App.class);
        inputStream.close();

        app.setAppStorageSource(AppStorageSource.JCLOUDS);
        app.setFolderName(resource.getName());

        handler.accept(app);
      } catch (IOException ex) {
        log.error("Could not read manifest file of " + resource.getName(), ex);
      }
    }
  }

  @Override
  public Map<String, App> discoverInstalledApps() {
    Map<String, App> apps = new HashMap<>();
    discoverInstalledApps(app -> apps.put(app.getUrlFriendlyName(), app));

    if (apps.isEmpty()) {
      log.info("No apps found during JClouds discovery.");
    } else {
      apps.values()
          .forEach(app -> log.info("Discovered app '{}' from JClouds storage ", app.getName()));
    }

    return apps;
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
  public App installApp(File file, String filename, Cache<App> appCache) {
    App app = new App();
    log.info("Installing new app: {}", filename);

    try (ZipFile zip = new ZipFile(file)) {
      // -----------------------------------------------------------------
      // Determine top-level directory name, if the zip file contains one
      // -----------------------------------------------------------------

      String prefix = ZipFileUtils.getTopLevelDirectory(zip.entries().asIterator());
      log.debug("Detected top-level directory '{}' in zip", prefix);

      // -----------------------------------------------------------------
      // Parse manifest.webapp file from ZIP archive.
      // -----------------------------------------------------------------

      ZipEntry entry = zip.getEntry(prefix + MANIFEST_FILENAME);

      if (entry == null) {
        log.error("Failed to install app: Missing manifest.webapp in zip");

        app.setAppState(AppStatus.MISSING_MANIFEST);
        return app;
      }

      InputStream inputStream = zip.getInputStream(entry);

      app = jsonMapper.readValue(inputStream, App.class);

      app.setFolderName(
          APPS_DIR + File.separator + filename.substring(0, filename.lastIndexOf('.')));
      app.setAppStorageSource(AppStorageSource.JCLOUDS);

      if (!this.validateApp(app, appCache)) {
        return app;
      }

      // -----------------------------------------------------------------
      // Unzip the app
      // -----------------------------------------------------------------

      String dest = APPS_DIR + File.separator + filename.substring(0, filename.lastIndexOf('.'));

      zip.stream()
          .forEach(
              (Consumer<ZipEntry>)
                  zipEntry -> {
                    log.debug("Uploading zipEntry: {}", zipEntry);
                    String name = zipEntry.getName().substring(prefix.length());

                    try {
                      InputStream input = zip.getInputStream(zipEntry);

                      Blob blob =
                          jCloudsStore
                              .getBlobStore()
                              .blobBuilder(dest + File.separator + name)
                              .payload(input)
                              .contentLength(zipEntry.getSize())
                              .build();
                      jCloudsStore.putBlob(blob);

                      input.close();
                    } catch (IOException e) {
                      log.error("Unable to store app file '" + name + "'", e);
                    }
                  });

      // make sure any other version of same app is removed
      List<App> otherVersions = new ArrayList<>();
      String key = app.getKey();
      String version = app.getVersion();
      discoverInstalledApps(
          other -> {
            if (key.equals(other.getKey()) && !version.equals(other.getVersion()))
              otherVersions.add(other);
          });
      otherVersions.forEach(this::deleteApp);

      String namespace = app.getActivities().getDhis().getNamespace();

      log.info(
          "New app {} installed, Install path: {}, Namespace reserved: {}",
          app.getName(),
          dest,
          (namespace != null && !namespace.isEmpty() ? namespace : "no namespace reserved"));

      // -----------------------------------------------------------------
      // Installation complete.
      // -----------------------------------------------------------------

      app.setAppState(AppStatus.OK);

      return app;
    } catch (ZipException e) {
      log.error("Failed to install app: Invalid ZIP format", e);
      app.setAppState(AppStatus.INVALID_ZIP_FORMAT);
    } catch (JsonParseException e) {
      log.error("Failed to install app: Invalid manifest.webapp", e);
      app.setAppState(AppStatus.INVALID_MANIFEST_JSON);
    } catch (IOException e) {
      log.error("Failed to install app: Could not save app", e);
      app.setAppState(AppStatus.INSTALLATION_FAILED);
    }

    return app;
  }

  @Override
  public void deleteApp(App app) {
    log.info("Deleting app {}", app.getName());

    if (jCloudsStore.isUsingFileSystem()) {
      // Delete all files related to app (works for local filestore):
      jCloudsStore.deleteDirectory(app.getFolderName());
    } else {
      // slower but works for S3:
      // delete the manifest file first in case the system crashes during deletion
      // and the manifest file is not deleted, resulting in an app that can't be installed
      jCloudsStore.removeBlob(app.getFolderName() + "manifest.webapp");
      // Delete all files related to app
      ListContainerOptions options = prefix(app.getFolderName()).recursive();
      for (StorageMetadata resource : jCloudsStore.getBlobList(options)) {
        log.debug("Deleting app file: {}", resource.getName());
        jCloudsStore.removeBlob(resource.getName());
      }
    }
    log.info("Deleted app {}", app.getName());
  }

  @Override
  public ResourceResult getAppResource(App app, @Nonnull String resource) throws IOException {
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
    String key = app.getFolderName() + ("/" + resolvedFileResource);
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
}
