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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for managing apps bundled as ClassPath resources.
 *
 * @author Austin McGee
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.appmanager.BundledAppStorageService")
public class BundledAppStorageService implements AppStorageService {
  private static final String CLASSPATH_PREFIX = "classpath:";
  private static final String STATIC_DIR = "static/";
  private static final String BUNDLED_APP_PREFIX = "dhis-web-";
  private final ResourceLoader resourceLoader;
  private final ResourcePatternResolver resourcePatternResolver;

  private final Map<String, App> apps = new ConcurrentHashMap<>();

  @Override
  public Map<String, App> discoverInstalledApps() {
    apps.clear();
    try {
      Resource[] resources =
          resourcePatternResolver.getResources(
              CLASSPATH_PREFIX + STATIC_DIR + BUNDLED_APP_PREFIX + "*/" + MANIFEST_FILENAME);
      for (Resource resource : resources) {
        App app = readAppManifest(resource);
        if (app != null) {
          String path =
              CLASSPATH_PREFIX
                  + STATIC_DIR
                  + BUNDLED_APP_PREFIX
                  + app.getKey()
                  + "/"
                  + MANIFEST_FILENAME;

          String regex = "/" + MANIFEST_FILENAME + "$";
          String shortName =
              path.replaceAll(regex, "")
                  .replaceAll("^" + CLASSPATH_PREFIX + STATIC_DIR + BUNDLED_APP_PREFIX, "");
          app.setBundled(true);
          app.setShortName(shortName);
          app.setAppStorageSource(AppStorageSource.BUNDLED);
          app.setFolderName(path.replaceAll(regex, ""));

          checkManifestTranslations(app);
          log.info("Discovered bundled app {} ({})", app.getKey(), app.getFolderName());
          apps.put(app.getKey(), app);
        }
      }
    } catch (IOException e) {
      log.error("Failed to discover bundled apps: {}", e.getLocalizedMessage());
    }

    log.info("Discovered {} bundled apps", apps.size());
    return apps;
  }

  private void checkManifestTranslations(App app) {
    try {
      // Read translations for possible manifest translations
      String resourceName =
          app.getFolderName() + "/" + AppStorageService.MANIFEST_TRANSLATION_FILENAME;
      Resource appManifestTranslation = resourceLoader.getResource(resourceName);

      if (appManifestTranslation.exists()) {
        List<AppManifestTranslation> manifestTranslations =
            readAppManifestTranslation(appManifestTranslation);

        app.setManifestTranslations(manifestTranslations);
      }
    } catch (Exception ex) {
      log.debug("Error reading manifest translation file for {}", app.getKey());
    }
  }

  private App readAppManifest(Resource resource) {
    try {
      InputStream inputStream = resource.getInputStream();

      return App.MAPPER.readValue(inputStream, App.class);
    } catch (IOException e) {
      log.error(e.getLocalizedMessage(), e);
    }
    return null;
  }

  private List<AppManifestTranslation> readAppManifestTranslation(Resource resource) {
    try {
      InputStream inputStream = resource.getInputStream();
      return App.MAPPER.readerForListOf(AppManifestTranslation.class).readValue(inputStream);
    } catch (IOException e) {
      log.error(e.getLocalizedMessage(), e);
      return Collections.emptyList();
    }
  }

  @Override
  public App installApp(File file, String fileName, Cache<App> appCache) {
    throw new UnsupportedOperationException("Bundled apps cannot be installed.");
  }

  @Override
  public Future<Boolean> deleteAppAsync(App app) {
    log.warn("Bundled apps cannot be deleted, skipping delete of app {}.", app.getKey());
    return CompletableFuture.completedFuture(false);
  }

  @Override
  public ResourceResult getAppResource(App app, String pageName) throws IOException {
    if (app == null || !app.getAppStorageSource().equals(AppStorageSource.BUNDLED)) {
      return new ResourceResult.ResourceNotFound(pageName);
    }
    log.debug(
        "Looking up resource for bundled app {}, page {}, folderName {}",
        app.getShortName(),
        pageName,
        app.getFolderName());
    if (pageName.isBlank()) {
      return new ResourceResult.Redirect("/");
    }
    if (!app.getFolderName().startsWith(CLASSPATH_PREFIX)) {
      return new ResourceResult.ResourceNotFound(pageName);
    }

    String resolvedFileResource = useIndexHtmlIfDirCall(pageName);
    String path = app.getFolderName() + "/" + resolvedFileResource;
    String cleanedPath = path.replaceAll("/+", "/");

    try {
      Resource resource = resourceLoader.getResource(cleanedPath);
      if (!resource.exists()) {
        log.debug("Resource not found {}", cleanedPath);
        return new ResourceResult.ResourceNotFound(pageName);
      }
      log.debug("Resource found {}", cleanedPath);
      return new ResourceResult.ResourceFound(resource);
    } catch (Exception ex) {
      log.error(ex.getLocalizedMessage(), ex);
    }

    return null;
  }

  private String useIndexHtmlIfDirCall(@Nonnull String resource) {
    if (resource.endsWith("/")) {
      log.debug("Resource ends with '/', appending 'index.html' to {}", resource);
      return resource + "index.html";
    }
    // any other resource, no special handling required, return as is
    return resource;
  }

  public Set<String> getBundledAppKeys() {
    return apps.keySet();
  }
}
