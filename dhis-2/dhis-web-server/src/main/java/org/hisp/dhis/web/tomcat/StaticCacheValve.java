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
package org.hisp.dhis.web.tomcat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.hisp.dhis.webapi.staticresource.StaticCacheControlService;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Tomcat Valve that sets {@code Cache-Control} headers for core static resources served from JAR
 * files ({@code /dhis-web-commons/**} etc.). These resources bypass Spring MVC and are served
 * directly by Tomcat's default servlet, so a Valve is the only hook point.
 *
 * <p>Skips {@code /api/**} (handled by Spring controllers) and {@code /apps/**} (handled by {@code
 * AppController}).
 */
public class StaticCacheValve extends ValveBase {

  private volatile StaticCacheControlService service;

  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {
    String uri = request.getRequestURI();

    if (isStaticCorePath(uri)) {
      StaticCacheControlService svc = getService(request);
      if (svc != null) {
        svc.setHeaders(response.getResponse(), uri, null);
      }
    }

    getNext().invoke(request, response);
  }

  private boolean isStaticCorePath(String uri) {
    return uri.startsWith("/dhis-web-") && !uri.startsWith("/api/") && !uri.startsWith("/apps/");
  }

  private StaticCacheControlService getService(Request request) {
    StaticCacheControlService svc = this.service;
    if (svc != null) return svc;

    WebApplicationContext ctx =
        WebApplicationContextUtils.getWebApplicationContext(request.getServletContext());
    if (ctx != null && ctx.containsBean("staticCacheControlService")) {
      svc = ctx.getBean(StaticCacheControlService.class);
      this.service = svc;
    }
    return svc;
  }
}
