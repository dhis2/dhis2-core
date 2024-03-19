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
package org.hisp.dhis.servlet.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Torgeir Lorange Ostby
 * @version $Id: HttpRedirectFilter.java 2869 2007-02-20 14:26:09Z andegje $
 */
@Slf4j
@WebFilter(
    urlPatterns = {"/"},
    initParams = {
      @WebInitParam(name = "redirectPath", value = "dhis-web-commons-about/redirect.action"),
      @WebInitParam(name = "urlPattern", value = "index\\.html|/$")
    })
public class HttpRedirectFilter implements Filter {
  private static final String REDIRECT_PATH_KEY = "redirectPath";

  private String redirectPath;

  // -------------------------------------------------------------------------
  // Filter implementation
  // -------------------------------------------------------------------------

  @Override
  public void init(FilterConfig config) {
    redirectPath = config.getInitParameter(REDIRECT_PATH_KEY);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException {
    log.debug("Redirecting to: " + redirectPath);

    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if (redirectPath == null) {
      String msg =
          "HttpRedirectFilter was not properly initialised. \""
              + REDIRECT_PATH_KEY
              + "\" must be specified.";

      httpResponse.setContentType("text/plain");
      httpResponse.getWriter().print(msg);

      log.warn(msg);

      return;
    }

    httpResponse.sendRedirect(redirectPath);
  }

  @Override
  public void destroy() {}
}
