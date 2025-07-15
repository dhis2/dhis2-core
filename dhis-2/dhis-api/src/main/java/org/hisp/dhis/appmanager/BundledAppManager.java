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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.appmanager.AppBundleInfo.BundledAppInfo;
import org.hisp.dhis.util.ZipFileUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Component that installs bundled app ZIP files during server startup.
 *
 * <p>It detects .zip files in the bundled apps directory and installs them using the AppManager.
 *
 * <p>It also caches the the AppBundleInfo file and the list of apps names it contains for fast
 * lookup.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BundledAppManager {
  private static final String CLASSPATH_DHIS_WEB_APPS = "classpath:/static/dhis-web-apps";
  private static final String ZIPPED_APPS_PATH = CLASSPATH_DHIS_WEB_APPS + "/*.zip";
  private static final String APPS_BUNDLE_INFO_PATH = CLASSPATH_DHIS_WEB_APPS + "/apps-bundle.json";

  private final ObjectMapper jsonMapper;

  @Getter private AppBundleInfo cachedBundleInfo;
  @Getter private Set<String> cachedBundledAppNames;
  @Getter private final Set<String> bundledAppsKeys = new HashSet<>();
  @Getter private final Map<String, Pair<App, BundledAppInfo>> bundledApps = new HashMap<>();

  @PostConstruct
  private void initializeCache() {
    InputStream appBundleInfoInputStream = getAppBundleInfoInputStream();
    if (appBundleInfoInputStream == null) {
      cachedBundleInfo = null;
      cachedBundledAppNames = Set.of();
      return;
    }
    try (appBundleInfoInputStream) {
      cachedBundleInfo = jsonMapper.readValue(appBundleInfoInputStream, AppBundleInfo.class);
      if (cachedBundleInfo == null || cachedBundleInfo.getApps() == null) {
        cachedBundledAppNames = Set.of();
      } else {
        cachedBundledAppNames =
            cachedBundleInfo.getApps().stream()
                .map(BundledAppInfo::getName)
                .collect(Collectors.toSet());
      }
    } catch (IOException e) {
      log.error("Failed to read bundled apps info file from disk", e);
      throw new RuntimeException(e);
    }

    cacheAppManifestsAndBundleInfo();
  }

  public void installBundledApps(@Nonnull TriConsumer<App, BundledAppInfo, Resource> consumer) {
    AppBundleInfo appBundleInfo = getCachedBundleInfo();
    if (appBundleInfo == null) {
      return;
    }

    Map<String, BundledAppInfo> bundledAppsInfo =
        appBundleInfo.getApps().stream()
            .collect(Collectors.toMap(BundledAppInfo::getName, Function.identity()));

    bundledAppsInfo.forEach(
        (fileName, bundledAppInfo) -> {
          Resource zipFileResource = getBundledAppsResources().get(fileName + ".zip");
          consumer.accept(
              getBundledApps().get(fileName).getLeft(), bundledAppInfo, zipFileResource);
        });
  }

  private void cacheAppManifestsAndBundleInfo() {
    AppBundleInfo appBundleInfo = getCachedBundleInfo();
    if (appBundleInfo == null) {
      return;
    }

    Map<String, BundledAppInfo> bundledAppsInfo =
        appBundleInfo.getApps().stream()
            .collect(Collectors.toMap(BundledAppInfo::getName, Function.identity()));

    bundledAppsInfo.forEach(
        (fileName, bundledAppInfo) -> {
          Resource zipFileResource = getBundledAppsResources().get(fileName + ".zip");
          try (InputStream inputStream = zipFileResource.getInputStream()) {
            Path tempFile = Files.createTempFile("bundled-app-" + fileName, ".zip");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            File zipFile = tempFile.toFile();
            String topLevelFolder = ZipFileUtils.getTopLevelFolder(zipFile);
            App app = AppManager.readAppManifest(zipFile, jsonMapper, topLevelFolder);

            bundledAppsKeys.add(app.getKey());
            bundledApps.put(fileName, Pair.of(app, bundledAppInfo));

            Files.deleteIfExists(tempFile);
          } catch (IOException e) {
            log.error(
                "Fail to read app manifest from bundled app zip file, appName: '{}'", fileName, e);
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

  @CheckForNull
  public static InputStream getAppBundleInfoInputStream() {
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources(APPS_BUNDLE_INFO_PATH);
      // Ignore if not exists, this is the case when running tests
      if (resources.length == 0 || resources[0] == null || !resources[0].exists()) {
        log.error("Bundled apps info file not found at: {}", APPS_BUNDLE_INFO_PATH);
        return null;
      }
      return resources[0].getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
