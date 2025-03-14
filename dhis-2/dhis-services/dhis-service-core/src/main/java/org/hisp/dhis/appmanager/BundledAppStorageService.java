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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.cache.Cache;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

/**
 * This class is responsible for managing apps bundled as ClassPath resources.
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
    try {
      Resource[] resources = resourcePatternResolver.getResources(CLASSPATH_PREFIX + STATIC_DIR + BUNDLED_APP_PREFIX + "*/manifest.webapp");
      for (Resource resource : resources) {
        App app = readAppManifest(resource);
        if (app != null) {
          String path = ((ClassPathResource) resource).getPath();
          String shortName = path.replaceAll("/manifest.webapp$", "").replaceAll("^" + STATIC_DIR + BUNDLED_APP_PREFIX, "");
          app.setIsBundled(true);
          app.setShortName(shortName);
          app.setAppStorageSource(AppStorageSource.BUNDLED);
          app.setFolderName(CLASSPATH_PREFIX + path.replaceAll("/manifest.webapp$", ""));
          
          log.info("Discovered bundled app {} (short name {}, name {}, path {})", app.getKey(), app.getShortName(), app.getName(), app.getFolderName());
          apps.put(app.getShortName(), app);
        }
      }
    } catch (IOException e) {
      log.error(e.getLocalizedMessage(), e);
    }
    return apps;
  }

  private App readAppManifest(Resource resource) {
    try {
      InputStream inputStream = resource.getInputStream();
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      return objectMapper.readValue(inputStream, App.class);
    } catch (IOException e) {
      log.error(e.getLocalizedMessage(), e);
    }
    return null;
  }

  @Override
  public App installApp(File file, String fileName, Cache<App> appCache) {
    throw new UnsupportedOperationException(
        "Bundled apps cannot be installed.");
  }

  @Override
  public void deleteApp(App app) {
    throw new UnsupportedOperationException(
        "Bundled apps cannot be deleted.");
  }

  @Override
  public ResourceResult getAppResource(App app, String pageName) throws IOException {
    if (app == null || !app.getAppStorageSource().equals(AppStorageSource.BUNDLED)) {
      return new ResourceResult.ResourceNotFound(pageName);
    }
    log.info("Looking up resource for bundled app {}, page {}, folderName {}", app.getShortName(), pageName, app.getFolderName());
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
          log.info("Resource not found {}", cleanedPath);
          return new ResourceResult.ResourceNotFound(pageName);
        }
        log.info("Resource found {}", cleanedPath);
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

  public Set<String> getBundledAppNames() {
    return apps.keySet();
  }
}
