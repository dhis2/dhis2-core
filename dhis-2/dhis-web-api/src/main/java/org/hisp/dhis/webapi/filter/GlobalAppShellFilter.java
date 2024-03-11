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
package org.hisp.dhis.webapi.filter;

import static java.util.regex.Pattern.compile;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.AppManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Austin McGee <austin@dhis2.org>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class GlobalAppShellFilter extends OncePerRequestFilter {
  @Autowired AppManager appManager;

  public static final String GLOBAL_APP_SHELL_PATH_PREFIX = "/apps/";
  public static final String GLOBAL_APP_SHELL_APP_NAME = "global-app-shell";

  private static final Pattern APP_PATH_PATTERN =
      compile("^/" + "(?:" + AppManager.BUNDLED_APP_PREFIX + "|api/apps/)" + "(\\S+)/(.*)");

  private static final Pattern GLOBAL_APP_SHELL_PATTERN =
      compile("^" + GLOBAL_APP_SHELL_PATH_PREFIX + "([^/.]+)/?$");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if (!appManager.exists(GLOBAL_APP_SHELL_APP_NAME)) {
      chain.doFilter(request, response);
      return;
    }

    if (redirectLegacyAppPaths(request, response)) {
      return;
    }

    if (request.getRequestURI().startsWith(GLOBAL_APP_SHELL_PATH_PREFIX)) {
      serveGlobalAppShell(request, response);
      return;
    }

    chain.doFilter(request, response);
  }

  private boolean redirectLegacyAppPaths(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String path = request.getRequestURI();
    String queryString = request.getQueryString();
    Matcher m = APP_PATH_PATTERN.matcher(path);

    boolean matchesPattern = m.find();
    boolean isIndexPath = path.endsWith("/") || path.endsWith("/index.html");
    boolean hasRedirectFalse = queryString == null || !queryString.contains("redirect=false");
    if (matchesPattern && isIndexPath && hasRedirectFalse) {
      String appName = m.group(1);
      response.sendRedirect(GLOBAL_APP_SHELL_PATH_PREFIX + appName);
      log.debug("Redirecting to global shell");
      return true;
    }
    return false;
  }

  private boolean serveGlobalAppShell(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    String path = request.getRequestURI();
    if (!path.startsWith(GLOBAL_APP_SHELL_PATH_PREFIX)) {
      return false;
    }

    if (GLOBAL_APP_SHELL_PATTERN.matcher(path).matches()) {
      if (path.endsWith("/")) {
        response.sendRedirect(path.substring(0, path.length() - 1));
        return true;
      }
      // Return index.html for all index.html or directory root requests
      log.debug("Serving global shell");
      serveGlobalAppShellResource(request, response, "index.html");
      return true;
    } else {
      // Serve global app shell resources
      serveGlobalAppShellResource(
          request, response, path.substring(GLOBAL_APP_SHELL_PATH_PREFIX.length()));
      return true;
    }
  }

  private void serveGlobalAppShellResource(
      HttpServletRequest request, HttpServletResponse response, String resource)
      throws IOException, ServletException {
    RequestDispatcher dispatcher =
        getServletContext()
            .getRequestDispatcher(
                String.format("/api/apps/%s/%s", GLOBAL_APP_SHELL_APP_NAME, resource));
    dispatcher.forward(request, response);
  }
}
