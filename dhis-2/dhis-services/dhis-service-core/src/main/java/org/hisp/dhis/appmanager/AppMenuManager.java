/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.hisp.dhis.security.Authorities.ALL;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppMenuManager {
  private final I18nManager i18nManager;

  private final LocaleManager localeManager;

  private final List<WebModule> menuModules = new ArrayList<>();

  private Locale currentLocale;

  private final ResourceLoader resourceLoader;

  private String readFromInputStream(InputStream inputStream) throws IOException {
    StringBuilder resultStringBuilder = new StringBuilder();
    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
      String line;
      while ((line = br.readLine()) != null) {
        resultStringBuilder.append(line).append("\n");
      }
    }
    return resultStringBuilder.toString();
  }

  private void generateModules(AppManager appManager) {
    Set<String> bundledApps = AppManager.BUNDLED_APPS;

    for (String app : bundledApps) {
      String key = "dhis-web-" + app;
      String displayName = i18nManager.getI18n().getString(key);

      WebModule module = new WebModule(key, "/" + key, "../" + key + "/index.html");
      module.setDisplayName(displayName);
      module.setIcon("../icons/" + key + ".png");

      // Retrieve the manifest file from the manifest.web file in the bundled file path
      // This contains other information we need like shortcuts and
      // ToDo: consolidate these extra steps for bundled apps so that they are done on discovery and
      // bundled apps are
      //  also added to the AppManager cache - this means this generateModules would not to care
      // about these differences
      try {
        // 1. read the manifest file for bundled apps
        InputStream appManifestResource =
            resourceLoader
                .getResource("classpath:static/" + key + "/manifest.webapp")
                .getInputStream();
        String appManifest = readFromInputStream(appManifestResource);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        App bundledApp = mapper.readValue(appManifest, App.class);
        module.setShortcuts(bundledApp.getShortcuts());
        module.setVersion(bundledApp.getVersion());

        // 2. check if this is a bundled app that was updated (so its information should be in the
        // cache)
        App installedApp = appManager.getApp(app);

        if (installedApp != null && !installedApp.getVersion().equals(module.getVersion())) {
          module.setShortcuts(installedApp.getShortcuts());
          module.setVersion(installedApp.getVersion());
        }
      } catch (IOException ex) {
        log.error(ex.getLocalizedMessage(), ex);
      }
      menuModules.add(module);
    }
  }

  private void detectLocaleChange() {
    if (localeManager.getCurrentLocale().equals(currentLocale)) {
      return;
    }

    menuModules.forEach(m -> m.setDisplayName(i18nManager.getI18n().getString(m.getName())));

    currentLocale = localeManager.getCurrentLocale();
  }

  public List<WebModule> getAccessibleWebModules(AppManager appManager) {
    if (menuModules.isEmpty()) {
      generateModules(appManager);
    }

    detectLocaleChange();

    return getAccessibleModules(menuModules);
  }

  private List<WebModule> getAccessibleModules(List<WebModule> modules) {
    return modules.stream()
        .filter(module -> module != null && hasAccess(module.getName()))
        .collect(Collectors.toList());
  }

  private boolean hasAccess(String module) {
    return CurrentUserUtil.hasAnyAuthority(
        List.of(ALL.toString(), Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString(), "M_" + module));
  }
}
