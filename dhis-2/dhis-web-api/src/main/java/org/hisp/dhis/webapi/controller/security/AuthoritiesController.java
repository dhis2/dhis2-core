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
package org.hisp.dhis.webapi.controller.security;

import static java.util.Collections.singletonMap;

import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.appmanager.AndroidSettingsApp;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.security.SystemAuthoritiesProvider;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spring version of GetSystemAuthoritiesAction.
 *
 * @author Jan Bernitt
 */
@OpenApi.Document(
    entity = User.class,
    classifiers = {"team:platform", "purpose:support"})
@RestController
@RequestMapping("/api/authorities")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class AuthoritiesController {

  @Autowired private I18nManager i18nManager;
  @Autowired private SystemAuthoritiesProvider authoritiesProvider;
  @Autowired private SchemaService schemaService;
  @Autowired private AppManager appManager;

  @GetMapping
  public Map<String, List<Map<String, String>>> getAuthorities(HttpServletResponse response) {
    I18n i18n = i18nManager.getI18n();

    TreeSet<String> authorities = new TreeSet<>();

    Collection<String> systemAuthorities = authoritiesProvider.getSystemAuthorities();
    authorities.addAll(systemAuthorities);

    Collection<String> schemaAuthorities = schemaService.collectAuthorities();
    authorities.addAll(schemaAuthorities);

    Collection<String> appAuthorities = getAppAuthorities();
    authorities.addAll(appAuthorities);

    List<Map<String, String>> entries = new ArrayList<>();
    for (String auth : authorities) {
      Map<String, String> authority = new LinkedHashMap<>();
      String name = getAuthName(auth, i18n);
      authority.put("id", auth);
      authority.put("name", name);
      entries.add(authority);
    }
    entries.sort(Comparator.comparing(e -> e.get("name").toLowerCase()));
    return singletonMap("systemAuthorities", entries);
  }

  public Collection<String> getAppAuthorities() {
    Set<String> authorities = new HashSet<>();
    appManager.getApps(null).stream()
        .filter(app -> !StringUtils.isEmpty(app.getShortName()))
        .filter(app -> !AppManager.ALWAYS_ACCESSIBLE_APPS.contains(app.getShortName()))
        .forEach(
            app -> {
              authorities.add(app.getSeeAppAuthority());
              authorities.addAll(app.getAuthorities());
              authorities.addAll(app.getAdditionalAuthorities());
            });
    authorities.add(AndroidSettingsApp.AUTHORITY);
    return authorities;
  }

  private static String getAuthName(String auth, I18n i18n) {
    auth = i18n.getString(auth);
    // Custom App doesn't have translation for See App authority
    if (auth.startsWith(App.SEE_APP_AUTHORITY_PREFIX)) {
      auth = auth.replaceFirst(App.SEE_APP_AUTHORITY_PREFIX, "").replace("_", " ") + " app";
    }
    return auth;
  }
}
