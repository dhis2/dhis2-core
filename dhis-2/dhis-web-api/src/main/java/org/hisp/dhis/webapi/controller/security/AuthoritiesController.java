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
package org.hisp.dhis.webapi.controller.security;

import static java.util.Collections.singletonMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.security.SystemAuthoritiesProvider;
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
@RestController
@RequestMapping(value = "/authorities")
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class AuthoritiesController {
  @Autowired private I18nManager i18nManager;

  @Autowired private SystemAuthoritiesProvider authoritiesProvider;

  @GetMapping
  public Map<String, List<Map<String, String>>> getAuthorities(HttpServletResponse response) {
    I18n i18n = i18nManager.getI18n();
    List<String> authorities = new ArrayList<>(authoritiesProvider.getSystemAuthorities());
    Collections.sort(authorities);
    List<Map<String, String>> entries = new ArrayList<>();
    for (String auth : authorities) {
      String name = getAuthName(auth, i18n);

      Map<String, String> authority = new LinkedHashMap<>();
      authority.put("id", auth);
      authority.put("name", name);
      entries.add(authority);
    }
    return singletonMap("systemAuthorities", entries);
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
