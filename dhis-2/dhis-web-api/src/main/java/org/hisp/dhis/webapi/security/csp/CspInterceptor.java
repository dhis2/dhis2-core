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
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that detects @CustomCsp and @CspUserUploadedContent annotations on controller
 * methods and sets the appropriate CSP policy in the CspPolicyHolder for use by the CspFilter.
 *
 * <p>This interceptor checks both the method and the class level for CSP annotations, with
 * method-level annotations taking precedence over class-level ones.
 *
 * <p>Precedence order: 1. Method-level @CustomCsp (custom policy) 2.
 * Method-level @CspUserUploadedContent (default-src 'none';) 3.
 * Class-level @CustomCsp (custom policy) 4.
 * Class-level @CspUserUploadedContent (default-src 'none';) 5. No annotation
 * (CspFilter applies default)
 *
 * @author DHIS2 Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CspInterceptor implements HandlerInterceptor {
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    // Clear any previous CSP policy before processing the request
    CspPolicyHolder.clear();

    if (!(handler instanceof HandlerMethod)) {
      return true;
    }

    HandlerMethod handlerMethod = (HandlerMethod) handler;
    Method method = handlerMethod.getMethod();
    Class<?> controllerClass = handlerMethod.getBeanType();

    // Check for @CustomCsp annotation on the method first (takes precedence)
    CustomCsp methodAnnotation = method.getAnnotation(CustomCsp.class);
    if (methodAnnotation != null) {
      String cspPolicy = methodAnnotation.value();
      log.debug("Setting custom CSP policy for method {} to: {}", method.getName(), cspPolicy);
      CspPolicyHolder.setCspPolicy(cspPolicy);
      return true;
    }

    // Check for @CspUserUploadedContent annotation on the method
    if (method.getAnnotation(CspUserUploadedContent.class) != null) {
      String cspPolicy = "default-src 'none'; ";
      log.debug("Setting user-uploaded-content CSP policy for method {}", method.getName());
      CspPolicyHolder.setCspPolicy(cspPolicy);
      return true;
    }

    // Check for @CustomCsp annotation on the class
    CustomCsp classAnnotation = controllerClass.getAnnotation(CustomCsp.class);
    if (classAnnotation != null) {
      String cspPolicy = classAnnotation.value();
      log.debug(
          "Setting custom CSP policy for class {} to: {}", controllerClass.getName(), cspPolicy);
      CspPolicyHolder.setCspPolicy(cspPolicy);
      return true;
    }

    // Check for @CspUserUploadedContent annotation on the class
    if (controllerClass.getAnnotation(CspUserUploadedContent.class) != null) {
      String cspPolicy = "default-src 'none'; ";
      log.debug("Setting user-uploaded-content CSP policy for class {}", controllerClass.getName());
      CspPolicyHolder.setCspPolicy(cspPolicy);
      return true;
    }

    return true;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    // Clean up ThreadLocal after request processing to prevent memory leaks
    CspPolicyHolder.clear();
  }
}
