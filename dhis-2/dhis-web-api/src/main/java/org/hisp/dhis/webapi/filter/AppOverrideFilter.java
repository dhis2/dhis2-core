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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.compile;

import javax.annotation.Nonnull;

import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.appmanager.ResourceResult;
import org.hisp.dhis.appmanager.ResourceResult.Redirect;
import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.appmanager.ResourceResult.ResourceNotFound;
import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.webapi.utils.AppHtmlTemplate;
import org.hisp.dhis.webapi.utils.HttpServletRequestPaths;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Austin McGee <austin@dhis2.org>
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AppOverrideFilter extends OncePerRequestFilter {
  public static final String APP_PATH_PATTERN_STRING =
      "^/"
          + AppManager.BUNDLED_APP_PREFIX
          + "("
          + String.join("|", AppManager.BUNDLED_APPS)
          + ")/(.*)";

  public static final Pattern APP_PATH_PATTERN = compile(APP_PATH_PATTERN_STRING);

  private final AppManager appManager;

  private final ObjectMapper jsonMapper;

  @Override
  protected void doFilterInternal(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull FilterChain chain)
      throws IOException, ServletException {
    String pathInfo = request.getPathInfo();
    String contextPath = HttpServletRequestPaths.getContextPath(request);

    Matcher m = APP_PATH_PATTERN.matcher(Strings.nullToEmpty(pathInfo));
    if (m.find()) {
      String appName = m.group(1);
      String resourcePath = m.group(2);

      log.info("AppOverrideFilter :: Matched for path: " + pathInfo);

      App app = appManager.getApp(appName, contextPath);
      if (app != null && app.getAppState() != AppStatus.DELETION_IN_PROGRESS) {
        log.info("AppOverrideFilter :: Overridden app " + appName + " found, serving override");
        // if resource path is blank, this means the base app dir has been requested
        // this is due to the complex regex above which has to include '/' at the end, so correct
        // app names are matched e.g. dhis-web-user v dhis-web-user-profile
        serveInstalledAppResource(
            app, resourcePath.isBlank() ? "/" : resourcePath, request, response);

        return;
      } else {
        log.info(
            "AppOverrideFilter :: App " + appName + " not found, falling back to bundled app");

        if (resourcePath.endsWith(".html")) {
          log.info("AppOverrideFilter :: HTML response detected, applying app override template {}", resourcePath);
          
          CharResponseWrapper responseWrapper = new CharResponseWrapper(response);
          chain.doFilter(request, responseWrapper);

          InputStream inputStream = new ByteArrayInputStream(responseWrapper.toString().getBytes());
          ByteArrayOutputStream bout = new ByteArrayOutputStream();

          AppHtmlTemplate template = new AppHtmlTemplate(contextPath, contextPath);
          template.apply(inputStream, bout);

          response.setContentType("text/html");
          response.setContentLength(bout.size());
          response.setHeader("Content-Encoding", StandardCharsets.UTF_8.toString());
          bout.writeTo(response.getOutputStream());
        } else {
          log.debug("AppOverrideFilter :: Non-HTML response detected, passing through");
          chain.doFilter(request, response);
        }

        return;
      }
    }

    chain.doFilter(request, response);
  }

  private void serveInstalledAppResource(
      App app, String resourcePath, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    log.debug(String.format("Serving app resource: '%s'", resourcePath));

    // Handling of 'manifest.webapp'
    if ("manifest.webapp".equals(resourcePath)) {
      handleManifestWebApp(request, response, app);
    } else if ("index.action".equals(resourcePath)) {
      response.sendRedirect(app.getLaunchUrl());
    }
    // Any other resource
    else {
      ResourceResult resourceResult = appManager.getAppResource(app, resourcePath);

      Resource resource;
      if (resourceResult instanceof ResourceFound found) {
        resource = found.resource();

        String etag = HashUtils.hashMD5(String.valueOf(resource.lastModified()).getBytes());
        if (new ServletWebRequest(request, response).checkNotModified(etag)) {
          response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
          return;
        }

        String filename = resource.getFilename();
        log.debug(String.format("App filename: '%s'", filename));

        String mimeType = request.getSession().getServletContext().getMimeType(filename);
        if (mimeType != null) {
          response.setContentType(mimeType);
        }
        response.setContentLength(appManager.getUriContentLength(resource));
        response.setHeader("ETag", etag);
        StreamUtils.copyThenCloseInputStream(resource.getInputStream(), response.getOutputStream());
      }
      if (resourceResult instanceof ResourceNotFound) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
      if (resourceResult instanceof Redirect redirect) {
        String cleanValidUrl = TextUtils.cleanUrlPathOnly(app.getBaseUrl(), redirect.path());
        log.debug("Redirecting to: {}", cleanValidUrl);
        response.sendRedirect(cleanValidUrl);
      }
    }
  }

  private void handleManifestWebApp(
      HttpServletRequest request, HttpServletResponse response, App app) throws IOException {
    // If request was for manifest.webapp, check for * and replace with host
    if (app.getActivities() != null
        && app.getActivities().getDhis() != null
        && "*".equals(app.getActivities().getDhis().getHref())) {
      String contextPath = HttpServletRequestPaths.getContextPath(request);
      log.debug(String.format("Manifest context path: '%s'", contextPath));
      app.getActivities().getDhis().setHref(contextPath);
    }
    jsonMapper.writeValue(response.getOutputStream(), app);
  }
}
