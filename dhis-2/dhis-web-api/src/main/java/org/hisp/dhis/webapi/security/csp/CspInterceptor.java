/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.security.csp;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.webapi.security.csp.app.CspAppPolicyService;
import org.hisp.dhis.webapi.security.csp.app.CspEntrypoints;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Interceptor that resolves the {@code Content-Security-Policy} for a controller handler method
 * from marker annotations and delegates header construction to {@link CspPolicyService}. Handler
 * methods without a marker are left untouched here — they keep the baseline headers already set by
 * {@link CspBaselineFilter}.
 *
 * <p>For {@link CspAppHost} handlers the HTML entrypoints and the worker script of an app get the
 * per-app policy composed by {@link CspAppPolicyService} (base overrides and administrator
 * overrides of that app and entrypoint); other app resources get the plain app-host baseline
 * without any per-app lookup.
 *
 * @see CspBaselineFilter for the unconditional baseline applied to every response
 * @author Austin McGee
 * @author Morten Svanæs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CspInterceptor implements HandlerInterceptor {
  /** Path variable of {@code AppController.renderApp} holding the app name or key. */
  static final String APP_PATH_VARIABLE = "app";

  private static final String APPS_PATH = "/" + AppManager.INSTALLED_APP_PREFIX;

  private final CspPolicyService cspPolicyService;
  private final CspAppPolicyService cspAppPolicyService;
  private final AppManager appManager;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    HttpHeaders securityHeaders = resolveSecurityHeaders(handlerMethod, request);
    if (securityHeaders == null) {
      return true;
    }
    securityHeaders.forEach(
        (headerName, headerValues) -> {
          if (!headerValues.isEmpty()) {
            response.setHeader(headerName, headerValues.get(0));
          }
        });
    return true;
  }

  private HttpHeaders resolveSecurityHeaders(
      HandlerMethod handlerMethod, HttpServletRequest request) {
    Method method = handlerMethod.getMethod();
    Class<?> controllerClass = handlerMethod.getBeanType();

    if (hasMarker(method, controllerClass, CspUserUploadedContent.class)) {
      log.debug("Applying user-uploaded-content CSP policy for {}", method.getName());
      return cspPolicyService.getUserUploadedContentSecurityHeaders();
    }
    if (hasMarker(method, controllerClass, CspAppHost.class)) {
      return resolveAppHostHeaders(request);
    }
    if (hasMarker(method, controllerClass, CspOpenApiDocs.class)) {
      log.debug("Applying openapi-docs CSP policy for {}", method.getName());
      return cspPolicyService.getSecurityHeaders(cspPolicyService.constructOpenApiDocsCspPolicy());
    }
    return null;
  }

  private HttpHeaders resolveAppHostHeaders(HttpServletRequest request) {
    String appName = appPathVariable(request);
    if (appName != null) {
      App app = appManager.getApp(appName, request.getContextPath());
      if (app != null) {
        String path = relativePath(request, appName);
        String entrypoint = path == null ? null : CspEntrypoints.resolve(app, path);
        if (entrypoint != null) {
          log.debug(
              "Applying per-app CSP policy for app {} entrypoint {}", app.getKey(), entrypoint);
          return cspAppPolicyService.getAppSecurityHeaders(
              app.getKey(), entrypoint, request.getContextPath());
        }
      }
    }
    log.debug("Applying app-host CSP policy");
    return cspPolicyService.getSecurityHeaders(cspPolicyService.constructAppHostCspPolicy());
  }

  @CheckForNull
  private static String appPathVariable(HttpServletRequest request) {
    Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    if (variables instanceof Map<?, ?> map && map.get(APP_PATH_VARIABLE) instanceof String name) {
      return name;
    }
    return null;
  }

  /**
   * The resource path below {@code /api/apps/{app}}, e.g. {@code /index.html}, or {@code null} when
   * the request is not below that prefix.
   */
  @CheckForNull
  private static String relativePath(HttpServletRequest request, String appName) {
    String uri = request.getRequestURI();
    String contextPath = request.getContextPath();
    if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
      uri = uri.substring(contextPath.length());
    }
    String prefix = APPS_PATH + appName;
    return uri.startsWith(prefix) ? uri.substring(prefix.length()) : null;
  }

  private static boolean hasMarker(
      Method method, Class<?> controllerClass, Class<? extends Annotation> marker) {
    return AnnotatedElementUtils.hasAnnotation(method, marker)
        || AnnotatedElementUtils.hasAnnotation(controllerClass, marker);
  }
}
