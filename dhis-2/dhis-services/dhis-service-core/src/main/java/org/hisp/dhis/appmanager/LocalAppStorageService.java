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
import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.external.location.LocationManager;
import org.hisp.dhis.external.location.LocationManagerException;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * Note! This class is mostly code from pre 2.28's DefaultAppManager. This is to support apps
 * installed before 2.28. post 2.28, all installations using DHIS2 will use
 * JCloudsAppStorageService.
 *
 * @author Stian Sandvold
 */
@Slf4j
@RequiredArgsConstructor
@Service("org.hisp.dhis.appmanager.LocalAppStorageService")
public class LocalAppStorageService implements AppStorageService {
  private final ResourceLoader resourceLoader = new DefaultResourceLoader();

  private Map<String, App> apps = new HashMap<>();

  private Map<String, App> reservedNamespaces = new HashMap<>();

  private final LocationManager locationManager;

  @Override
  public Map<String, App> discoverInstalledApps() {
    Map<String, App> appMap = new HashMap<>();
    List<App> appList = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    String path = getAppFolderPath();

    apps.clear();

    // Make sure external directory is set
    if (path == null) {
      log.error(
          "Failed to discover installed apps: Could not get app folder path, external directory not set");
      return appMap;
    }

    File appFolderPath = new File(path);

    // If no apps folder exists, there is nothing to discover
    if (!appFolderPath.exists()) {
      log.info("Old apps folder does not exist, stopping discovery");
      return appMap;
    }

    if (!appFolderPath.isDirectory()) {
      log.error("Failed to discover installed apps: Path is not a directory '" + path + "'");
    } else {
      File[] listFiles = appFolderPath.listFiles();

      // Should only happen is IO error occurs
      if (listFiles == null) {
        log.error(
            "Failed to discover installed apps: Could not list contents of directory '"
                + path
                + "'");
      } else {
        for (File folder : listFiles) {

          // Found a file that is not an app in the app directory.
          if (!folder.isDirectory()) {
            log.warn(
                "Failed to discover app '"
                    + folder.getName()
                    + "': Path is not a directory '"
                    + folder.getPath()
                    + "'");
          } else {
            File appManifest = new File(folder, "manifest.webapp");

            if (!appManifest.exists()) {
              log.warn(
                  "Failed to discover app '"
                      + folder.getName()
                      + "': Missing 'manifest.webapp' in app directory");
            } else {
              try {
                App app = mapper.readValue(appManifest, App.class);
                app.setFolderName(folder.getName());
                app.setAppStorageSource(AppStorageSource.LOCAL);
                appList.add(app);
              } catch (IOException ex) {
                log.error(ex.getLocalizedMessage(), ex);
              }
            }
          }
        }
      }
    }

    appList.forEach(
        app -> {
          String namespace = app.getActivities().getDhis().getNamespace();

          if (namespace != null && !namespace.isEmpty()) {
            reservedNamespaces.put(namespace, app);
          }

          appMap.put(app.getUrlFriendlyName(), app);
          apps.put(app.getUrlFriendlyName(), app);

          log.info("Discovered app '" + app.getName() + "' from local storage ");
        });

    if (appList.isEmpty()) {
      log.info("No apps found during local discovery.");
    }

    return appMap;
  }

  @Override
  public Map<String, App> getReservedNamespaces() {
    return reservedNamespaces;
  }

  @Override
  public App installApp(File file, String fileName, Cache<App> appCache) {
    throw new UnsupportedOperationException(
        "LocalAppStorageService.installApp is deprecated and should no longer be used.");
  }

  @Override
  public void deleteApp(App app) {
    if (!apps.containsKey(app.getUrlFriendlyName())) {
      log.warn(String.format("Failed to delete app '%s': App not found", app.getName()));
    }

    try {
      String folderPath = getAppFolderPath() + File.separator + app.getFolderName();
      FileUtils.forceDelete(new File(folderPath));
    } catch (FileNotFoundException ex) {
      log.error(String.format("Failed to delete app '%s': Files not found", app.getName()), ex);
    } catch (IOException ex) {
      log.error(String.format("Failed to delete app '%s'", app.getName()), ex);
    } finally {
      discoverInstalledApps();
    }
  }

  private String getAppFolderPath() {
    try {
      return locationManager.getExternalDirectoryPath() + "/" + APPS_DIR;
    } catch (LocationManagerException ex) {
      return null;
    }
  }

  @Override
  public Resource getAppResource(App app, String pageName) throws IOException {
    List<Resource> locations =
        Lists.newArrayList(
            resourceLoader.getResource(
                "file:" + getAppFolderPath() + "/" + app.getFolderName() + "/"),
            resourceLoader.getResource("classpath*:/apps/" + app.getFolderName() + "/"));

    for (Resource location : locations) {
      Resource resource = location.createRelative(pageName);

      if (resource.exists() && resource.isReadable()) {
        File file = resource.getFile();

        // Make sure that file resolves into path app folder
        if (file != null && file.toPath().startsWith(getAppFolderPath())) {
          return resource;
        }
      }
    }

    return null;
  }
}
