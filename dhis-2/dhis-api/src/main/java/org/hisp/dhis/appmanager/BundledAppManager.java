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
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.hisp.dhis.appmanager.AppBundleInfo.BundledAppInfo;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * Component that installs bundled app ZIP files during server startup.
 *
 * <p>It detects .zip files in the bundled apps directory and installs them using the AppManager.
 */
@Slf4j
@Component
public class BundledAppManager {
  private static final String CLASSPATH_DHIS_WEB_APPS = "classpath:/static/dhis-web-apps";
  private static final String ZIPPED_APPS_PATH = CLASSPATH_DHIS_WEB_APPS + "/*.zip";
  private static final String APPS_BUNDLE_INFO_PATH = CLASSPATH_DHIS_WEB_APPS + "/apps-bundle.json";

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public void installBundledApps(TriConsumer<String, BundledAppInfo, Resource> consumer) {
    AppBundleInfo appBundleInfo = getAppBundleInfo();
    if (appBundleInfo == null) {
      return;
    }

    Map<String, BundledAppInfo> bundledAppsInfo =
        appBundleInfo.getApps().stream()
            .collect(Collectors.toMap(BundledAppInfo::getName, Function.identity()));

    Map<String, Resource> bundledAppsResources = getBundledApps();

    bundledAppsInfo.forEach(
        (k, appInfo) -> {
          String filename = k + ".zip";
          Resource resource = bundledAppsResources.get(filename);
          consumer.accept(k, appInfo, resource);
        });
  }

  private Map<String, Resource> getBundledApps() {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    try {
      Resource[] resources = resolver.getResources(ZIPPED_APPS_PATH);
      return Arrays.stream(resources)
          .collect(Collectors.toMap(Resource::getFilename, Function.identity()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static AppBundleInfo getAppBundleInfo() {
    InputStream appBundleInfoInputStream = getAppBundleInfoInputStream();
    if (appBundleInfoInputStream == null) {
      return null;
    }

    try {
      return objectMapper.readValue(appBundleInfoInputStream, AppBundleInfo.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static InputStream getAppBundleInfoInputStream() {
    try {
      PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
      Resource[] resources = resolver.getResources(APPS_BUNDLE_INFO_PATH);

      // Ignore if not exists, this is the case when running tests
      if (resources.length == 0 || resources[0] == null || !resources[0].exists()) {
        log.warn(String.format("Bundled apps info file not found at: '%s'", APPS_BUNDLE_INFO_PATH));
        return null;
      }

      return resources[0].getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
