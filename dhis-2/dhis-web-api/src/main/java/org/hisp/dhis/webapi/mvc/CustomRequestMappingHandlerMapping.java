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
package org.hisp.dhis.webapi.mvc;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * This class is a custom implementation of the RequestMappingHandlerMapping class. It is used to
 * handle the versioning of the API endpoints. The class overrides the getMappingForMethod method to
 * add the versioning to the API endpoints.
 *
 * <p>During startup of the application, the Spring framework will create a
 * RequestMappingHandlerMapping bean. This bean is responsible for mapping the request to the
 * appropriate controller method. By creating a custom implementation of the
 * RequestMappingHandlerMapping class, we can add the versioning to the API endpoints.
 *
 * <p>
 *
 * @author Morten Olav Hansen <mortenoh@gmail.com>
 */
public class CustomRequestMappingHandlerMapping extends RequestMappingHandlerMapping {
  @Override
  protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
    RequestMappingInfo info = super.getMappingForMethod(method, handlerType);

    if (info == null) {
      return null;
    }

    ApiVersion typeApiVersion = AnnotationUtils.findAnnotation(handlerType, ApiVersion.class);
    ApiVersion methodApiVersion = AnnotationUtils.findAnnotation(method, ApiVersion.class);

    if (typeApiVersion == null && methodApiVersion == null) {
      return info;
    }

    RequestMethodsRequestCondition methodsCondition = info.getMethodsCondition();

    if (methodsCondition.getMethods().isEmpty()) {
      methodsCondition = new RequestMethodsRequestCondition(RequestMethod.GET);
    }

    Set<String> rqmPatterns = info.getPatternsCondition().getPatterns();
    Set<String> allPaths = new HashSet<>();

    Set<DhisApiVersion> versions = getVersions(typeApiVersion, methodApiVersion);

    for (String path : rqmPatterns) {
      versions.stream()
          .filter(version -> !version.isIgnore())
          .forEach(version -> addVersionedPath(version, path, allPaths));
    }

    PatternsRequestCondition patternsRequestCondition =
        new PatternsRequestCondition(
            allPaths.toArray(new String[] {}), null, null, true, true, null);

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

  private static void addVersionedPath(DhisApiVersion version, String path, Set<String> allPaths) {
    // Normalize path to start with "/api/"
    String normalizedPath = path.startsWith("/api/") ? path : path.replaceFirst("^api/", "/api/");

    // Skip path that don't start with "/api/"
    if (!normalizedPath.startsWith("/api/")) {
      return;
    }

    // Check if the path corresponds directly to a versioned API endpoint
    if (normalizedPath.startsWith("/api/" + version.getVersionString())) {
      allPaths.add(normalizedPath);
      return;
    }

    // Remove the leading "/api/" for further processing
    String pathWithoutApi = normalizedPath.substring(5); // Skip "/api/"

    // Add the versioned API path
    allPaths.add("/api/" + version.getVersionString() + "/" + pathWithoutApi);
  }

  private Set<DhisApiVersion> getVersions(ApiVersion typeApiVersion, ApiVersion methodApiVersion) {
    Set<DhisApiVersion> includes = new HashSet<>();
    Set<DhisApiVersion> excludes = new HashSet<>();

    if (typeApiVersion != null) {
      includes.addAll(Arrays.asList(typeApiVersion.include()));
      excludes.addAll(Arrays.asList(typeApiVersion.exclude()));
    }

    if (methodApiVersion != null) {
      includes.addAll(Arrays.asList(methodApiVersion.include()));
      excludes.addAll(Arrays.asList(methodApiVersion.exclude()));
    }

    if (includes.contains(DhisApiVersion.ALL)) {
      boolean includeDefault = includes.contains(DhisApiVersion.DEFAULT);
      includes = new HashSet<>(Arrays.asList(DhisApiVersion.values()));

      if (!includeDefault) {
        includes.remove(DhisApiVersion.DEFAULT);
      }
    }

    includes.removeAll(excludes);

    return includes;
  }
}
