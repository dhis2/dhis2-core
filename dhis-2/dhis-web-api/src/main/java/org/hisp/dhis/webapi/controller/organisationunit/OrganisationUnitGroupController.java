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
package org.hisp.dhis.webapi.controller.organisationunit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Pattern;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.organisationunit.OrganisationUnitGroup;
import org.hisp.dhis.query.GetObjectListParams;
import org.hisp.dhis.webapi.controller.AbstractCrudController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
@Controller
@RequestMapping("/api/organisationUnitGroups")
@OpenApi.Document(classifiers = {"team:platform", "purpose:metadata"})
public class OrganisationUnitGroupController
    extends AbstractCrudController<OrganisationUnitGroup, GetObjectListParams> {

  private static final Pattern ORG_UNIT_GROUP_SINGLE_PATH =
      Pattern.compile("^/api(?:/\\d+)?/organisationUnitGroups/[^/]+(?:\\.[^/]+)?$");

  @Value("${dhis.cache.organisationunitgroup.max-age:3600}")
  private long cacheMaxAgeSeconds;

  @Override
  protected void applyCacheHeaders(HttpServletRequest request, HttpServletResponse response) {
    if (request != null && "GET".equalsIgnoreCase(request.getMethod())) {
      String path = request.getRequestURI();
      String contextPath = request.getContextPath();
      if (!contextPath.isEmpty() && path.startsWith(contextPath)) {
        path = path.substring(contextPath.length());
      }
      if (ORG_UNIT_GROUP_SINGLE_PATH.matcher(path).matches()) {
        response.setHeader("Cache-Control", "private, max-age=" + cacheMaxAgeSeconds);
        response.setHeader("Vary", "Accept-Encoding");
        response.setDateHeader(
            "Expires", System.currentTimeMillis() + (cacheMaxAgeSeconds * 1000L));
        return;
      }
    }
    super.applyCacheHeaders(request, response);
  }
}
