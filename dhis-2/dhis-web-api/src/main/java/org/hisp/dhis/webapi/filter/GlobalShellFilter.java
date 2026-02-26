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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.ResourceResult;
import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.setting.SystemSettingsProvider;
import org.hisp.dhis.webapi.staticresource.HtmlCacheBustingService;
import org.hisp.dhis.webapi.staticresource.StaticCacheControlService;
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
  public static final String GLOBAL_SHELL_PATH_PREFIX = "/apps/";
  public static final String REFERER_HEADER = "Referer";
  public static final String SERVICE_WORKER_JS = "/service-worker.js";
  public static final String REDIRECT_FALSE = "redirect=false";
  public static final String SHELL_FALSE = "shell=false";
  private static final String PATH_SEP = "/";

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

  private static final Pattern APP_SUBRESOURCE_PATTERN =
      compile("^" + GLOBAL_SHELL_PATH_PREFIX + "([^/.]+)/(.+)");

  private final AppManager appManager;
  private final SystemSettingsProvider settingsProvider;
  private final StaticCacheControlService staticCacheControlService;
  private final HtmlCacheBustingService htmlCacheBustingService;

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

    if (serveCanonicalServiceWorkerIfNeeded(response, path)) {
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
    Matcher legacyPatternMatcher = LEGACY_APP_PATH_PATTERN.matcher(path);
    if (!legacyPatternMatcher.find()) {
      return false;
    }

    String referer = request.getHeader(REFERER_HEADER);
    String queryString = request.getQueryString();
    boolean useCanonicalAppPaths = settingsProvider.getCurrentSettings().getCanonicalAppPaths();

    log.debug(
        "redirectLegacyAppPaths: path = {}, queryString = {}, referer = {}, canonical = {}",
        path,
        queryString,
        referer,
        useCanonicalAppPaths);

    String appName = legacyPatternMatcher.group(1);
    String subResource = legacyPatternMatcher.group(2);

    if (useCanonicalAppPaths) {
      String baseUrl = HttpServletRequestPaths.getContextPath(request);
      App app = appManager.getApp(appName, baseUrl);
      if (app == null) {
        log.debug("redirectLegacyAppPaths: app '{}' not found, not redirecting", appName);
        return false;
      }
      String targetPath = baseUrl + GLOBAL_SHELL_PATH_PREFIX + appName + PATH_SEP + subResource;
      targetPath = withQueryString(targetPath, queryString);
      response.sendRedirect(targetPath);
      log.debug("302 redirect legacy path to canonical: {}", targetPath);
      return true;
    }

    // Original behavior when canonical paths are off
    boolean isIndexPath = path.endsWith("/") || path.endsWith("/index.html");
    boolean isServiceWorkerRequest = referer != null && referer.endsWith(SERVICE_WORKER_JS);
    boolean hasRedirectFalse =
        queryString != null
            && (queryString.contains(REDIRECT_FALSE) || queryString.contains(SHELL_FALSE));

    if (isIndexPath && !isServiceWorkerRequest && !hasRedirectFalse) {
      String targetPath =
          HttpServletRequestPaths.getContextPath(request) + GLOBAL_SHELL_PATH_PREFIX + appName;
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
      serveShellEntryPoint(request, response, path);
    } else {
      serveShellSubresource(request, response, path);
    }
    if (!response.isCommitted()) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void serveShellEntryPoint(
      HttpServletRequest request, HttpServletResponse response, String path)
      throws IOException, ServletException {
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
    log.debug("Serving global shell index.  Original path: {}", path);
    boolean canonicalAppPaths = settingsProvider.getCurrentSettings().getCanonicalAppPaths();
    if (canonicalAppPaths) {
      serveGlobalShellIndexWithRewrittenPaths(request, response);
    } else {
      serveGlobalShellResource(request, response, "index.html");
    }
  }

  private void serveShellSubresource(
      HttpServletRequest request, HttpServletResponse response, String path)
      throws IOException, ServletException {
    Matcher subMatcher = APP_SUBRESOURCE_PATTERN.matcher(path);
    if (!subMatcher.matches()) {
      String resource = path.substring(GLOBAL_SHELL_PATH_PREFIX.length());
      if (resource.isEmpty()) {
        resource = "index.html";
      }
      log.debug("Serving global shell resource. Path {}, resolved resource {}", path, resource);
      serveGlobalShellResource(request, response, resource);
      return;
    }

    String appName = subMatcher.group(1);
    String subResource = subMatcher.group(2);
    String globalShellAppName = settingsProvider.getCurrentSettings().getGlobalShellAppName();
    boolean canonicalAppPaths = settingsProvider.getCurrentSettings().getCanonicalAppPaths();

    if (globalShellAppName.equals(appName)) {
      log.debug("Serving global-shell's own resource: {}", subResource);
      serveGlobalShellResource(request, response, subResource);
    } else if (canonicalAppPaths && isAppIndexWithoutRedirect(subResource, request)) {
      redirectToShellEntry(request, response, appName);
    } else if (canonicalAppPaths && appManager.getApp(appName) != null) {
      log.debug("Canonical: serving actual app {} resource: {}", appName, subResource);
      serveAppResource(request, response, appName, subResource);
    } else {
      String resource = path.substring(GLOBAL_SHELL_PATH_PREFIX.length());
      log.debug("Falling back to global-shell resource: {}", resource);
      serveGlobalShellResource(request, response, resource);
    }
  }

  private boolean isAppIndexWithoutRedirect(String subResource, HttpServletRequest request) {
    if (!"index.html".equals(subResource)) {
      return false;
    }
    String qs = request.getQueryString();
    return qs == null || (!qs.contains(REDIRECT_FALSE) && !qs.contains(SHELL_FALSE));
  }

  private void redirectToShellEntry(
      HttpServletRequest request, HttpServletResponse response, String appName) throws IOException {
    if (appManager.getApp(appName) == null) {
      return;
    }
    String targetPath =
        HttpServletRequestPaths.getContextPath(request) + GLOBAL_SHELL_PATH_PREFIX + appName;
    log.debug("Redirecting app index to shell entry: {}", targetPath);
    response.sendRedirect(targetPath);
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

  /**
   * Serves the global shell's index.html with all relative asset paths ({@code ./assets/main.js},
   * {@code ./manifest.json}, etc.) rewritten to absolute paths pointing at the global-shell app
   * ({@code {contextPath}/apps/global-shell/assets/main.js}). This is necessary because the HTML is
   * served at {@code /apps/{appName}} but the assets live under {@code /apps/global-shell/}.
   *
   * <p>Loads the resource directly via AppManager instead of forwarding to avoid request-dispatch
   * issues that can cause hangs when the forward target re-enters the filter chain.
   */
  private void serveGlobalShellIndexWithRewrittenPaths(
      HttpServletRequest request, HttpServletResponse response) throws IOException {

    String globalShellAppName = settingsProvider.getCurrentSettings().getGlobalShellAppName();
    String contextPath = HttpServletRequestPaths.getContextPath(request);
    String shellBasePath =
        request.getContextPath() + GLOBAL_SHELL_PATH_PREFIX + globalShellAppName + PATH_SEP;

    App globalShellApp = appManager.getApp(globalShellAppName, contextPath);
    if (globalShellApp == null || !appManager.isAccessible(globalShellApp)) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    ResourceResult result = appManager.getAppResource(globalShellApp, "index.html", contextPath);
    if (!(result instanceof ResourceFound found)) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String requestUri = contextPath + GLOBAL_SHELL_PATH_PREFIX + globalShellAppName + "/index.html";
    staticCacheControlService.setHeaders(response, requestUri, globalShellApp.getKey());

    try (InputStream rawStream = found.resource().getInputStream()) {
      InputStream htmlStream =
          htmlCacheBustingService.rewriteIfNeeded(rawStream, globalShellApp, requestUri);
      String html = new String(htmlStream.readAllBytes(), StandardCharsets.UTF_8);

      html = html.replace("href=\"./", "href=\"" + shellBasePath);
      html = html.replace("src=\"./", "src=\"" + shellBasePath);

      byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
      response.setContentType("text/html;charset=UTF-8");
      response.setContentLength(bytes.length);
      response.getOutputStream().write(bytes);
    }
  }

  private void serveAppResource(
      HttpServletRequest request, HttpServletResponse response, String appName, String resource)
      throws IOException, ServletException {
    String dispatchPath =
        PATH_SEP + AppManager.INSTALLED_APP_PREFIX + appName + PATH_SEP + resource;
    log.debug("Dispatching to actual app resource: {}", dispatchPath);
    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(dispatchPath);
    if (dispatcher != null) {
      dispatcher.forward(request, response);
    }
  }

  private static final String CANONICAL_SW_RESOURCE = "canonical-service-worker.js";

  /**
   * When canonical app paths are enabled, the global shell's standard service worker (from
   * {@code @dhis2/pwa}) conflicts with the new URL scheme. This method intercepts any {@code
   * service-worker.js} request under {@code /apps/} and serves a canonical-aware replacement that:
   *
   * <ul>
   *   <li>Only caches shell HTML for exact {@code /apps/{name}} navigations
   *   <li>Lets {@code /apps/{name}/{resource}} pass through for actual app serving
   *   <li>Cleans up stale Workbox caches from the previous worker
   *   <li>Preserves the message bus for shell-app communication
   * </ul>
   */
  private boolean serveCanonicalServiceWorkerIfNeeded(HttpServletResponse response, String path)
      throws IOException {
    if (!path.startsWith(GLOBAL_SHELL_PATH_PREFIX) || !path.endsWith(SERVICE_WORKER_JS)) {
      return false;
    }
    if (!settingsProvider.getCurrentSettings().getCanonicalAppPaths()) {
      return false;
    }

    try (InputStream swStream =
        getClass().getClassLoader().getResourceAsStream(CANONICAL_SW_RESOURCE)) {
      if (swStream == null) {
        log.warn("Canonical service worker resource not found: {}", CANONICAL_SW_RESOURCE);
        return false;
      }
      byte[] bytes = swStream.readAllBytes();
      response.setContentType("application/javascript");
      response.setContentLength(bytes.length);
      response.setHeader("Cache-Control", "no-store");
      response.setHeader("Service-Worker-Allowed", "/");
      response.getOutputStream().write(bytes);
      log.debug("Served canonical service worker at: {}", path);
      return true;
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
