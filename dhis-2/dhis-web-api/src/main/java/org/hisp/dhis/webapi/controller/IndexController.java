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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.schema.Schema;
import org.hisp.dhis.schema.SchemaService;
import org.hisp.dhis.setting.SystemSetting;
import org.hisp.dhis.setting.SystemSettings;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.webdomain.IndexResource;
import org.hisp.dhis.webapi.webdomain.IndexResources;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@OpenApi.Document(
    entity = SystemSetting.class,
    classifiers = {"team:platform", "purpose:support"})
@Controller
@RequiredArgsConstructor
public class IndexController {

  private final SchemaService schemaService;
  private final ContextService contextService;

  @GetMapping("/")
  public void getIndexWithSlash(
      HttpServletRequest request, HttpServletResponse response, SystemSettings settings)
      throws IOException {
    String redirectUrl = request.getContextPath() + "/" + settings.getStartModule();
    // Let the GlobalShellFilter redirect to apps
    redirectUrl = redirectUrl.replaceFirst("/apps", "");

    if (!redirectUrl.endsWith("/")) {
      redirectUrl += "/";
    }
    String location = response.encodeRedirectURL(redirectUrl);
    response.sendRedirect(location);
  }

  @GetMapping("/api")
  public void getIndex(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String location = response.encodeRedirectURL("/api/resources");
    response.sendRedirect(ContextUtils.getRootPath(request) + location);
  }

  @GetMapping("/api/resources")
  public @ResponseBody IndexResources getResources() {
    return createIndexResources();
  }

  private IndexResources createIndexResources() {
    IndexResources indexResources = new IndexResources();
    for (Schema schema : schemaService.getSchemas()) {
      if (schema.hasApiEndpoint()) {
        indexResources
            .getResources()
            .add(
                new IndexResource(
                    beautify(schema.getPlural()),
                    schema.getSingular(),
                    schema.getPlural(),
                    contextService.getApiPath() + schema.getRelativeApiEndpoint()));
      }
    }
    return indexResources;
  }

  private String beautify(String name) {
    String[] camelCaseWords = StringUtils.capitalize(name).split("(?=[A-Z])");
    return StringUtils.join(camelCaseWords, " ").trim();
  }
}
