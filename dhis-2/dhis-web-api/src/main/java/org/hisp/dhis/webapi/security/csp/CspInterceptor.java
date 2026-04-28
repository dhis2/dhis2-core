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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that resolves the {@code Content-Security-Policy} for a controller handler method
 * from marker annotations and delegates header construction to {@link CspPolicyService}. Handler
 * methods without a marker are left untouched here — they keep the baseline headers already set by
 * {@link CspBaselineFilter}.
 *
 * @see CspBaselineFilter for the unconditional baseline applied to every response
 * @author Morten Svanaes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CspInterceptor implements HandlerInterceptor {
  private final CspPolicyService cspPolicyService;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    String cspPolicy = resolvePolicy(handlerMethod);
    if (cspPolicy == null) {
      return true;
    }
    HttpHeaders securityHeaders = cspPolicyService.getSecurityHeaders(cspPolicy);
    securityHeaders.forEach(
        (headerName, headerValues) -> {
          if (!headerValues.isEmpty()) {
            response.setHeader(headerName, headerValues.get(0));
          }
        });
    return true;
  }

  private String resolvePolicy(HandlerMethod handlerMethod) {
    Method method = handlerMethod.getMethod();
    Class<?> controllerClass = handlerMethod.getBeanType();

    if (hasMarker(method, controllerClass, CspUserUploadedContent.class)) {
      log.debug("Applying user-uploaded-content CSP policy for {}", method.getName());
      return cspPolicyService.constructUserUploadedContentCspPolicy();
    }
    if (hasMarker(method, controllerClass, CspAppHost.class)) {
      log.debug("Applying app-host CSP policy for {}", method.getName());
      return cspPolicyService.constructAppHostCspPolicy();
    }
    if (hasMarker(method, controllerClass, CspLegacyLoginFallback.class)) {
      log.debug("Applying legacy-login-fallback CSP policy for {}", method.getName());
      return cspPolicyService.constructLegacyLoginFallbackCspPolicy();
    }
    return null;
  }

  private static boolean hasMarker(
      Method method, Class<?> controllerClass, Class<? extends Annotation> marker) {
    return AnnotatedElementUtils.hasAnnotation(method, marker)
        || AnnotatedElementUtils.hasAnnotation(controllerClass, marker);
  }
}
