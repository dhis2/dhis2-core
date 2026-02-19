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

import static org.hisp.dhis.external.conf.ConfigurationKey.CSP_ENABLED;
import static org.hisp.dhis.security.utils.CspConstants.CONTENT_SECURITY_POLICY_HEADER_NAME;
import static org.hisp.dhis.security.utils.CspConstants.DEFAULT_CSP_POLICY;
import static org.hisp.dhis.security.utils.CspConstants.FRAME_ANCESTORS_DEFAULT_CSP;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.configuration.ConfigurationService;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that detects @CustomCsp and @CspUserUploadedContent annotations on controller methods
 * and applies the appropriate CSP headers to the response.
 *
 * <p>This interceptor checks both the method and the class level for CSP annotations, with
 * method-level annotations taking precedence over class-level ones. Headers are applied in
 * postHandle, which runs after the handler method but before the response is committed.
 *
 * <p>Precedence order: 1. Method-level @CustomCsp (custom policy) 2.
 * Method-level @CspUserUploadedContent (default-src 'none';) 3. Class-level @CustomCsp (custom
 * policy) 4. Class-level @CspUserUploadedContent (default-src 'none';) 5. Default CSP policy
 *
 * @author DHIS2 Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CspInterceptor implements HandlerInterceptor {
  private final DhisConfigurationProvider dhisConfig;

  private final ConfigurationService configurationService;

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

    // Check for @CustomCsp annotation on the method first (takes precedence)
    CustomCsp methodAnnotation = method.getAnnotation(CustomCsp.class);
    if (methodAnnotation != null) {
      cspPolicy = methodAnnotation.value();
      log.info("Setting custom CSP policy for method {} to: {}", method.getName(), cspPolicy);
    } else if (method.getAnnotation(CspUserUploadedContent.class) != null) {
      // Check for @CspUserUploadedContent annotation on the method
      cspPolicy = "default-src 'none'; ";
      log.info("Setting user-uploaded-content CSP policy for method {}", method.getName());
    } else {
      // Check for @CustomCsp annotation on the class
      CustomCsp classAnnotation = controllerClass.getAnnotation(CustomCsp.class);
      if (classAnnotation != null) {
        cspPolicy = classAnnotation.value();
        log.info(
            "Setting custom CSP policy for class {} to: {}", controllerClass.getName(), cspPolicy);
      } else if (controllerClass.getAnnotation(CspUserUploadedContent.class) != null) {
        // Check for @CspUserUploadedContent annotation on the class
        cspPolicy = "default-src 'none'; ";
        log.info(
            "Setting user-uploaded-content CSP policy for class {}", controllerClass.getName());
      }
    }

    // If no CSP annotation found, use the default
    if (cspPolicy == null) {
      cspPolicy = DEFAULT_CSP_POLICY;
    }

    // Apply CSP headers
    applySecurityHeaders(response, cspPolicy);
    return true;
  }

  private void applySecurityHeaders(HttpServletResponse response, String cspPolicy) {
    if (!dhisConfig.isEnabled(CSP_ENABLED)) {
      // If CSP is not enabled, just set X-Frame-Options to SAMEORIGIN for clickjacking
      // protection
      response.addHeader("X-Frame-Options", "SAMEORIGIN");
      return;
    }

    // Add frame-ancestors directive based on CORS whitelist
    cspPolicy += getFrameAncestorsCspPolicy();

    // Set the CSP policy and additional security headers
    response.addHeader(CONTENT_SECURITY_POLICY_HEADER_NAME, cspPolicy);
    response.addHeader("X-Content-Type-Options", "nosniff");
    response.addHeader("X-Frame-Options", "SAMEORIGIN");

    log.info("Applied CSP policy and security headers for response");
  }

  private String getFrameAncestorsCspPolicy() {
    Set<String> corsWhitelist = configurationService.getConfiguration().getCorsWhitelist();
    String corsAllowedOrigins = "";
    if (!corsWhitelist.isEmpty()) {
      corsAllowedOrigins = String.join(" ", corsWhitelist);
      return FRAME_ANCESTORS_DEFAULT_CSP + " " + corsAllowedOrigins + ";";
    } else {
      return FRAME_ANCESTORS_DEFAULT_CSP + ";";
    }
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    // No cleanup needed
  }
}
