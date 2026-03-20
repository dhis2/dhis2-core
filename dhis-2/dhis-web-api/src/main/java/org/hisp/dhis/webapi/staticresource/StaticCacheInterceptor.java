/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.staticresource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that sets {@code Cache-Control} headers for core DHIS2 static resources
 * served from classpath JARs ({@code /dhis-web-commons/**}, {@code /icons/**}, {@code /images/**},
 * etc.). These resources are handled by Spring's {@code ResourceHttpRequestHandler} and this
 * interceptor works in both embedded Tomcat and WAR deployment modes.
 *
 * <p>App resources ({@code /apps/**}) are handled directly by {@code AppController} which calls
 * {@code StaticCacheControlService} itself, so this interceptor does not need to cover those paths.
 */
@Component
@RequiredArgsConstructor
public class StaticCacheInterceptor implements HandlerInterceptor {

  private final StaticCacheControlService staticCacheControlService;

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String uri = request.getRequestURI();
    if (isCoreStaticPath(uri)) {
      staticCacheControlService.setHeaders(response, uri, null);
    }
    return true;
  }

  private boolean isCoreStaticPath(String uri) {
    return uri.startsWith("/dhis-web-")
        || uri.startsWith("/icons/")
        || uri.startsWith("/images/")
        || uri.equals("/favicon.ico");
  }
}
