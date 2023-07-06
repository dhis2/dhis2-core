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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.hisp.dhis.apphub.AppHubService;
import org.hisp.dhis.apphub.WebApp;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Zubair Asghar
 */
@RestController
@RequestMapping(AppHubController.RESOURCE_PATH)
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class AppHubController {
  public static final String RESOURCE_PATH = "/appHub";

  @Autowired private AppHubService appHubService;

  @Autowired private I18nManager i18nManager;

  /** Deprecated as of version 2.35 and should be removed eventually. */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public List<WebApp> listAppHub() throws IOException {
    return appHubService.getAppHub();
  }

  @GetMapping(value = "/{apiVersion}/**", produces = APPLICATION_JSON_VALUE)
  public String getAppHubApiResponse(@PathVariable String apiVersion, HttpServletRequest request)
      throws URISyntaxException {
    String query = ContextUtils.getWildcardPathValue(request);

    return appHubService.getAppHubApiResponse(apiVersion, query);
  }

  @PostMapping(value = "/{versionId}")
  @PreAuthorize("hasRole('ALL') or hasRole('M_dhis-web-maintenance-appmanager')")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void installAppFromAppHub(@PathVariable String versionId) throws WebMessageException {
    AppStatus status = appHubService.installAppFromAppHub(versionId);

    if (!status.ok()) {
      String message = i18nManager.getI18n().getString(status.getMessage());

      throw new WebMessageException(conflict(message));
    }
  }
}
