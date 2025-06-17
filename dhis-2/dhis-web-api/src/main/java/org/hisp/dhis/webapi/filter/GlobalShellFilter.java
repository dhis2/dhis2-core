/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.filter;

import static java.util.regex.Pattern.compile;

import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.webapi.utils.HttpServletRequestPaths;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * @author Austin McGee <austin@dhis2.org>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class GlobalShellFilter extends OncePerRequestFilter {
  public static final String BUNDLED_GLOBAL_SHELL_NAME = "global-shell";
  public static final String BUNDLED_GLOBAL_SHELL_PATH = "dhis-web-" + BUNDLED_GLOBAL_SHELL_NAME;
  public static final String GLOBAL_SHELL_PATH_PREFIX = "/apps/";
  public static final String REFERER_HEADER = "Referer";
  public static final String SERVICE_WORKER_JS = "/service-worker.js";
  public static final String REDIRECT_FALSE = "redirect=false";
  public static final String SHELL_FALSE = "shell=false";

  private static final Pattern LEGACY_APP_PATH_PATTERN =
      compile(
          "^/"
              + "(?:"
              + AppManager.BUNDLED_APP_PREFIX
              + "|"
              + AppManager.INSTALLED_APP_PREFIX
              + ")(\\S+)/(.*)");

  private static final Pattern APP_IN_GLOBAL_SHELL_PATTERN =
      compile("^" + GLOBAL_SHELL_PATH_PREFIX + "([^/.]+)/?$");

  private final AppManager appManager;
  private final SystemSettingsProvider settingsProvider;

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain chain)
      throws IOException, ServletException {

    boolean globalShellEnabled = settingsProvider.getCurrentSettings().getGlobalShellEnabled();
    String path = getContextRelativePath(request);

    if (!globalShellEnabled) {
      boolean redirected = redirectDisabledGlobalShell(request, response, path);
      log.debug("GlobalShellFilter.doFilterInternal: redirectDisabledGlobalShell = {}", redirected);
      if (!redirected) {
        chain.doFilter(request, response);
      }
      return;
    }

    if (redirectLegacyAppPaths(request, response, path)) {
      log.debug("GlobalShellFilter.doFilterInternal: redirectLegacyAppPaths = true");
      return;
    }

    if (path.startsWith(GLOBAL_SHELL_PATH_PREFIX)) {
      log.debug("GlobalShellFilter.doFilterInternal: path starts with GLOBAL_SHELL_PATH_PREFIX");
      serveGlobalShell(request, response, path);
      return;
    }

    chain.doFilter(request, response);
  }

  private boolean redirectDisabledGlobalShell(
      HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    Matcher m = APP_IN_GLOBAL_SHELL_PATTERN.matcher(path);
    String baseUrl = HttpServletRequestPaths.getContextPath(request);

    if (m.matches()) {
      String appName = m.group(1);
      App app = appManager.getApp(appName, baseUrl);

      String targetPath;
      if (app != null) {
        log.debug("Installed app {} found", appName);
        targetPath = app.getLaunchUrl();
      } else {
        log.debug("App {} not found", appName);
        targetPath = baseUrl;
      }

      targetPath = withQueryString(targetPath, request.getQueryString());

      log.debug("Redirecting to {}", targetPath);
      response.sendRedirect(targetPath);
      return true;
    } else if (path.startsWith(GLOBAL_SHELL_PATH_PREFIX)) {
      log.debug("Redirecting to instance root");
      response.sendRedirect(baseUrl);
      return true;
    }
    return false;
  }

  private boolean redirectLegacyAppPaths(
      HttpServletRequest request, HttpServletResponse response, String path) throws IOException {
    String baseUrl = HttpServletRequestPaths.getContextPath(request);
    String queryString = request.getQueryString();
    Matcher m = LEGACY_APP_PATH_PATTERN.matcher(path);

    boolean matchesPattern = m.find();
    if (!matchesPattern) {
      return false;
    }

    // Only redirect index.html or directory root requests
    boolean isIndexPath = path.endsWith("/") || path.endsWith("/index.html");

    // Skip redirect if explicitly requested with ?redirect=false
    boolean hasRedirectFalse =
        queryString != null
            && (queryString.contains(REDIRECT_FALSE) || queryString.contains(SHELL_FALSE));

    String referer = request.getHeader(REFERER_HEADER);
    boolean isServiceWorkerRequest = referer != null && referer.endsWith(SERVICE_WORKER_JS);

    log.debug(
        "redirectLegacyAppPaths: path = {}, queryString = {}, referer = {}",
        path,
        queryString,
        referer);

    if (isIndexPath && !isServiceWorkerRequest && !hasRedirectFalse) {
      String appName = m.group(1);
      String targetPath = baseUrl + GLOBAL_SHELL_PATH_PREFIX + appName;
      targetPath = withQueryString(targetPath, queryString);
      response.sendRedirect(targetPath);
      log.debug("Redirecting to global shell {}", targetPath);
      return true;
    }
    return false;
  }

  private void serveGlobalShell(
      HttpServletRequest request, HttpServletResponse response, String path)
      throws IOException, ServletException {

    if (APP_IN_GLOBAL_SHELL_PATTERN.matcher(path).matches()) {
      if (request.getQueryString() != null && request.getQueryString().contains(SHELL_FALSE)) {
        log.debug("Redirecting to raw app because global shell was requested with shell=false");
        redirectDisabledGlobalShell(request, response, path);
        return;
      }

      if (path.endsWith("/")) {
        String targetPath = path.substring(0, path.length() - 1);
        targetPath = withQueryString(targetPath, request.getQueryString());
        response.sendRedirect(targetPath);
        return;
      }
      // Return index.html for all index.html or directory root requests
      log.debug("Serving global shell index.  Original path: {}", path);
      serveGlobalShellResource(request, response, "index.html");
    } else {
      String resource = path.substring(GLOBAL_SHELL_PATH_PREFIX.length());
      if (resource.isEmpty()) {
        resource = "index.html";
      }

      log.debug("Serving global shell resource. Path {}, resolved resource {}", path, resource);
      // Serve global app shell resources
      serveGlobalShellResource(request, response, resource);
    }
    if (!response.isCommitted()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void serveGlobalShellResource(
      HttpServletRequest request, HttpServletResponse response, String resource)
      throws IOException, ServletException {

    log.debug("Serving global shell resource {}", resource);

    String globalShellAppName = settingsProvider.getCurrentSettings().getGlobalShellAppName();
    App globalShellApp = appManager.getApp(globalShellAppName);

    if (globalShellApp != null) {
      log.debug("Serving global shell resource {}", resource);
      RequestDispatcher dispatcher =
          getServletContext()
              .getRequestDispatcher(
                  "/" + AppManager.INSTALLED_APP_PREFIX + globalShellAppName + "/" + resource);
      dispatcher.forward(request, response);
    }
  }

  private String withQueryString(@Nonnull String path, String queryString) {
    String result = path;

    if (queryString != null && !queryString.isEmpty()) {
      result += "?" + queryString;
    }

    return result;
  }

  private String getContextRelativePath(HttpServletRequest request) {
    return request.getRequestURI().substring(request.getContextPath().length());
  }
}
