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
package org.hisp.dhis.webapi.mvc;

import java.lang.reflect.Method;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * This class used to create ~50k API mappings for all versions of the App for each controller
 * method e.g. <br>
 * <code>/api/39/icons</code> <br>
 * <code>/api/39/icons/{key}</code> <br>
 * <code>/api/40/icons</code> <br>
 * <code>/api/40/icons/{key}</code> <br>
 * <br>
 * It now provides a mapping for any endpoint starting with `/api/`, allowing any version from 28 to
 * 43. It also still allows the endpoint without any version e.g. <br>
 * <code>/api/icons/{key}</code> <br>
 * which is what we want clients to use exclusively in the future.
 */
public class CustomRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
  @Override
  protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

    if (info == null) {
      return null;
    }

    // allow API calls with versions 28-43 in the path e.g. `/api/42/icons`
    List<String> versionedApiEndpoints =
        new java.util.ArrayList<>(
            info.getPatternValues().stream()
                .map(pv -> pv.replace("/api/", "/api/{apiVersion:^[2][8-9]|^[3][0-9]|^[4][0-3]$}/"))
                .toList());
    // allow original API path with no version in it e.g. `/api/icons
    versionedApiEndpoints.addAll(info.getPatternValues());

    RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();

    if (methodsCondition.getMethods().isEmpty()) {
      methodsCondition = new RequestMethodsRequestCondition(RequestMethod.GET);
    }

    PatternsRequestCondition patternsRequestCondition =
        new PatternsRequestCondition(
            versionedApiEndpoints.toArray(new String[] {}), null, null, true, true, null);
    return new RequestMappingInfo(
        null,
        patternsRequestCondition,
        methodsCondition,
        info.getParamsCondition(),
        info.getHeadersCondition(),
        info.getConsumesCondition(),
        info.getProducesCondition(),
        info.getCustomCondition());
  }
}
