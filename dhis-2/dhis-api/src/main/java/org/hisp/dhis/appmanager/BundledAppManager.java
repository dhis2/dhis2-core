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
package org.hisp.dhis.appmanager;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.hisp.dhis.appmanager.AppBundleInfo.BundledAppInfo;
import org.hisp.dhis.util.ZipFileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Component that installs bundled app ZIP files during server startup.
 *
 * <p>It detects .zip files in the bundled apps directory and installs them using the AppManager.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BundledAppManager {
  private static final String CLASSPATH_DHIS_WEB_APPS = "classpath:/static/dhis-web-apps";
  private static final String ZIPPED_APPS_PATH = CLASSPATH_DHIS_WEB_APPS + "/*.zip";
  private static final String APPS_BUNDLE_INFO_PATH = CLASSPATH_DHIS_WEB_APPS + "/apps-bundle.json";

  private final ObjectMapper jsonMapper;
  private volatile AppBundleInfo cachedBundleInfo;
  private volatile Set<String> cachedBundledAppNames;

  public void installBundledApps(TriConsumer<App, BundledAppInfo, Resource> consumer) {
    AppBundleInfo appBundleInfo = getAppBundleInfo();
    if (appBundleInfo == null) {
      return;
    }

    Map<String, BundledAppInfo> bundledAppsInfo =
        appBundleInfo.getApps().stream()
            .collect(Collectors.toMap(BundledAppInfo::getName, Function.identity()));

    bundledAppsInfo.forEach(
        (appName, appInfo) -> {
          Resource zipFileResource = getBundledAppsResources().get(appName + ".zip");
          try (InputStream inputStream = zipFileResource.getInputStream()) {
            Path tempFile = Files.createTempFile("bundled-app-" + appName, ".zip");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            File zipFile = tempFile.toFile();
            String topLevelFolder = ZipFileUtils.getTopLevelFolder(zipFile);
            App app = AppManager.readAppManifest(zipFile, jsonMapper, topLevelFolder);
            consumer.accept(app, appInfo, zipFileResource);
            Files.deleteIfExists(tempFile);
          } catch (IOException e) {
            log.error(
                "Fail to read app manifest from bundled app zip file, appName: '{}'", appName, e);
            throw new RuntimeException(e);
          }
        });
  }

  private Map<String, Resource> getBundledAppsResources() {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    try {
      Resource[] resources = resolver.getResources(ZIPPED_APPS_PATH);
      return Arrays.stream(resources)
          .collect(Collectors.toMap(Resource::getFilename, Function.identity()));
    } catch (IOException e) {
      log.error("Failed to read bundled apps zip files from disk", e);
      throw new RuntimeException(e);
    }
  }

  /*
   Get the AppBundleInfo which contains a list of all the bundled apps
  */
  public AppBundleInfo getAppBundleInfo() {
    if (cachedBundleInfo == null) {
      synchronized (this) {
        if (cachedBundleInfo == null) {
          InputStream appBundleInfoInputStream = getAppBundleInfoInputStream();
          if (appBundleInfoInputStream == null) {
            return null;
          }
          try (appBundleInfoInputStream) {
            cachedBundleInfo = jsonMapper.readValue(appBundleInfoInputStream, AppBundleInfo.class);
          } catch (IOException e) {
            log.error("Failed to read bundled apps info file from disk", e);
            throw new RuntimeException(e);
          }
        }
      }
    }
    return cachedBundleInfo;
  }

  public Set<String> getBundledAppNames() {
    if (cachedBundledAppNames == null) {
      synchronized (this) {
        if (cachedBundledAppNames == null) {
          AppBundleInfo bundleInfo = getAppBundleInfo();
          if (bundleInfo == null || bundleInfo.getApps() == null) {
            cachedBundledAppNames = Set.of();
          } else {
            cachedBundledAppNames =
                bundleInfo.getApps().stream()
                    .map(BundledAppInfo::getName)
                    .collect(Collectors.toSet());
          }
        }
      }
    }
    return cachedBundledAppNames;
  }

  public static InputStream getAppBundleInfoInputStream() {
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources(APPS_BUNDLE_INFO_PATH);
      // Ignore if not exists, this is the case when running tests
      if (resources.length == 0 || resources[0] == null || !resources[0].exists()) {
        log.warn("Bundled apps info file not found at: {}", APPS_BUNDLE_INFO_PATH);
        return null;
      }
      return resources[0].getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
