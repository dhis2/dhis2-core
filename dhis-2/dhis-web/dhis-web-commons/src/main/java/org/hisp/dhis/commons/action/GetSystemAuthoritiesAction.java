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
package org.hisp.dhis.commons.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.i18n.I18n;
import org.hisp.dhis.paging.ActionPagingSupport;
import org.hisp.dhis.security.SystemAuthoritiesProvider;

/**
 * @author mortenoh
 */
public class GetSystemAuthoritiesAction extends ActionPagingSupport<String> {
  // -------------------------------------------------------------------------
  // Dependencies
  // -------------------------------------------------------------------------

  private SystemAuthoritiesProvider authoritiesProvider;

  public void setAuthoritiesProvider(SystemAuthoritiesProvider authoritiesProvider) {
    this.authoritiesProvider = authoritiesProvider;
  }

  private I18n i18n;

  public void setI18n(I18n i18n) {
    this.i18n = i18n;
  }

  // -------------------------------------------------------------------------
  // Input & Output
  // -------------------------------------------------------------------------

  private String systemAuthorities;

  public String getSystemAuthorities() {
    return systemAuthorities;
  }

  // -------------------------------------------------------------------------
  // Action implementation
  // -------------------------------------------------------------------------

  @Override
  public String execute() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    ArrayNode authNodes = mapper.createArrayNode();

    List<String> listAuthorities = new ArrayList<>(authoritiesProvider.getSystemAuthorities());
    Collections.sort(listAuthorities);

    if (usePaging) {
      this.paging = createPaging(listAuthorities.size());

      listAuthorities = listAuthorities.subList(paging.getStartPos(), paging.getEndPos());
    }

    listAuthorities.forEach(
        auth -> {
          String name = getAuthName(auth);

          authNodes.add(mapper.createObjectNode().put("id", auth).put("name", name));
        });

    root.set("systemAuthorities", authNodes);

    systemAuthorities = mapper.writeValueAsString(root);

    return SUCCESS;
  }

  private String getAuthName(String auth) {
    auth = i18n.getString(auth);

    // Custom App doesn't have translation for See App authority
    if (auth.startsWith(App.SEE_APP_AUTHORITY_PREFIX)) {
      auth = auth.replaceFirst(App.SEE_APP_AUTHORITY_PREFIX, "").replaceAll("_", " ") + " app";
    }

    return auth;
  }
}
