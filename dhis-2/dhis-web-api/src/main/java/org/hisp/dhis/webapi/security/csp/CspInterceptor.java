/*
 * Copyright (c) 2004-2025, University of Oslo
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
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** Interceptor that delegates CSP logic to {@link CspPolicyService}. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CspInterceptor implements HandlerInterceptor {
  @Autowired private final CspPolicyService cspPolicyService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!(handler instanceof HandlerMethod)) {
      return true;
    }

    HandlerMethod handlerMethod = (HandlerMethod) handler;
    Method method = handlerMethod.getMethod();
    Class<?> controllerClass = handlerMethod.getBeanType();

    // Determine the CSP policy to apply
    String cspPolicy = null;

    // Check for @CustomCsp and @CspUserUploadedContent annotations on the method first, then on the
    // class
    CustomCsp methodAnnotation = method.getAnnotation(CustomCsp.class);
    if (methodAnnotation != null) {
      cspPolicy = cspPolicyService.constructCustomCspPolicy(methodAnnotation.value());
      log.info("Setting custom CSP policy for method {} to: {}", method.getName(), cspPolicy);
    } else if (method.getAnnotation(CspUserUploadedContent.class) != null) {
      cspPolicy = cspPolicyService.constructUserUploadedContentCspPolicy();
      log.info("Setting user-uploaded-content CSP policy for method {}", method.getName());
    } else {
      CustomCsp classAnnotation = controllerClass.getAnnotation(CustomCsp.class);
      if (classAnnotation != null) {
        cspPolicy = cspPolicyService.constructCustomCspPolicy(classAnnotation.value());
        log.info(
            "Setting custom CSP policy for class {} to: {}", controllerClass.getName(), cspPolicy);
      } else if (controllerClass.getAnnotation(CspUserUploadedContent.class) != null) {
        cspPolicy = cspPolicyService.constructUserUploadedContentCspPolicy();
        log.info(
            "Setting user-uploaded-content CSP policy for class {}", controllerClass.getName());
      }
    }

    // Add all security headers including the determined CSP policy
    HttpHeaders securityHeaders = cspPolicyService.getSecurityHeaders(cspPolicy);

    securityHeaders.forEach(
        (headerName, headerValues) -> {
          for (String headerValue : headerValues) {
            response.addHeader(headerName, headerValue);
          }
        });
    return true;
  }
}
