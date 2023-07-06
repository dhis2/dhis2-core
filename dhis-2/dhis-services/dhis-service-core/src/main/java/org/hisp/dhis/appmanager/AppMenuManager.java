/*
 * Copyright (c) 2004-2023, University of Oslo
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

import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.stereotype.Service;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppMenuManager {
  private static final Set<String> MENU_MODULE_EXCLUSIONS = Set.of("dhis-web-user-profile");

  private final I18nManager i18nManager;

  private final LocaleManager localeManager;

  private final List<WebModule> menuModules;

  private Locale currentLocale;

  private void generateModules() {
    Set<String> bundledApps = AppManager.BUNDLED_APPS;

    for (String app : bundledApps) {
      String key = "dhis-web-" + app;
      String displayName = i18nManager.getI18n().getString(key);

      WebModule module = new WebModule(key, "/" + key, "../" + key + "/index.html");
      module.setDisplayName(displayName);
      module.setIcon("../icons/" + key + ".png");

      if (!MENU_MODULE_EXCLUSIONS.contains(key)) menuModules.add(module);
    }

    currentLocale = localeManager.getCurrentLocale();
  }

  private void detectLocaleChange() {
    if (localeManager.getCurrentLocale().equals(currentLocale)) {
      return;
    }

    menuModules.forEach(m -> m.setDisplayName(i18nManager.getI18n().getString(m.getName())));

    currentLocale = localeManager.getCurrentLocale();
  }

  public List<WebModule> getAccessibleWebModules() {
    if (menuModules.isEmpty()) {
      generateModules();
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
        List.of("ALL", AppManager.WEB_MAINTENANCE_APPMANAGER_AUTHORITY, "M_" + module));
  }
}
