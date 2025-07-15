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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.security.Authorities.M_DHIS_WEB_APP_MANAGEMENT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.hisp.dhis.apphub.AppHubService;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Zubair Asghar
 */
@OpenApi.Document(
    entity = App.class,
    classifiers = {"team:extensibility", "purpose:support"})
@RestController
@RequestMapping("/api/appHub")
@RequiredArgsConstructor
public class AppHubController {

  private final AppManager appManager;
  private final AppHubService appHubService;
  private final I18nManager i18nManager;

  /** Deprecated as of version 2.35 and should be removed eventually. */
  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public String listAppHub() throws URISyntaxException, ConflictException {
    return appHubService.getAppHubApiResponse("", "apps");
  }

  @GetMapping(value = "/{apiVersion}/**", produces = APPLICATION_JSON_VALUE)
  public String getAppHubApiResponse(@PathVariable String apiVersion, HttpServletRequest request)
      throws URISyntaxException, ConflictException {
    String query = ContextUtils.getWildcardPathValue(request);

    return appHubService.getAppHubApiResponse(apiVersion, query);
  }

  @PostMapping(value = "/{versionId}", produces = APPLICATION_JSON_VALUE)
  @RequiresAuthority(anyOf = M_DHIS_WEB_APP_MANAGEMENT)
  public ResponseEntity<App> installAppFromAppHub(@PathVariable UUID versionId)
      throws ConflictException {
    App app = appManager.installAppByHubId(versionId);
    AppStatus status = app.getAppState();

    if (!status.ok()) {
      String message = i18nManager.getI18n().getString(status.getMessage());

      throw new ConflictException(message);
    }

    return new ResponseEntity<>(app, HttpStatus.CREATED);
  }
}
