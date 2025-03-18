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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.badRequest;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.conflict;
import static org.hisp.dhis.dxf2.webmessage.WebMessageUtils.notFound;
import static org.hisp.dhis.security.Authorities.M_DHIS_WEB_APP_MANAGEMENT;

import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.hisp.dhis.appmanager.App;
import org.hisp.dhis.appmanager.AppManager;
import org.hisp.dhis.appmanager.AppStatus;
import org.hisp.dhis.appmanager.AppStorageSource;
import org.hisp.dhis.appmanager.ResourceResult;
import org.hisp.dhis.appmanager.ResourceResult.Redirect;
import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.appmanager.ResourceResult.ResourceNotFound;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.common.DhisApiVersion;
import org.hisp.dhis.common.OpenApi;
import org.hisp.dhis.commons.util.StreamUtils;
import org.hisp.dhis.commons.util.TextUtils;
import org.hisp.dhis.dxf2.webmessage.WebMessageException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.render.RenderService;
import org.hisp.dhis.security.RequiresAuthority;
import org.hisp.dhis.webapi.mvc.annotation.ApiVersion;
import org.hisp.dhis.webapi.service.ContextService;
import org.hisp.dhis.webapi.utils.ContextUtils;
import org.hisp.dhis.webapi.utils.HttpServletRequestPaths;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Lars Helge Overland
 */
@OpenApi.Document(
    entity = App.class,
    classifiers = {"team:extensibility", "purpose:support"})
@Controller
@RequestMapping("/api/apps")
@Slf4j
@ApiVersion({DhisApiVersion.DEFAULT, DhisApiVersion.ALL})
public class AppController {

  public static final Pattern REGEX_REMOVE_PROTOCOL = Pattern.compile(".+:/+");

  @Autowired private AppManager appManager;

  @Autowired private RenderService renderService;

  @Autowired private I18nManager i18nManager;

  @Autowired private ContextService contextService;

  @GetMapping(value = "/menu", produces = ContextUtils.CONTENT_TYPE_JSON)
  public @ResponseBody Map<String, List<WebModule>> getWebModules(HttpServletRequest request) {
    String contextPath = HttpServletRequestPaths.getContextPath(request);

    List<WebModule> modules = appManager.getMenu(contextPath);
    return Map.of("modules", modules);
  }

  @GetMapping(produces = ContextUtils.CONTENT_TYPE_JSON)
  public ResponseEntity<List<App>> getApps(@RequestParam(required = false) String key) {
    List<String> filters = Lists.newArrayList(contextService.getParameterValues("filter"));
    String contextPath = contextService.getContextPath();

    List<App> apps = new ArrayList<>();

    if (key != null) {
      App app = appManager.getApp(key, contextPath);

      if (app == null) {
        return ResponseEntity.notFound().build();
      }

      apps.add(app);
    } else if (!filters.isEmpty()) {
      apps = appManager.filterApps(filters, contextPath);
    } else {
      apps = appManager.getApps(contextPath);
    }
    return ResponseEntity.ok(apps);
  }

  @PostMapping(produces = ContextUtils.CONTENT_TYPE_JSON)
  @RequiresAuthority(anyOf = M_DHIS_WEB_APP_MANAGEMENT)
  public ResponseEntity<App> installApp(@RequestParam("file") MultipartFile file)
      throws IOException, WebMessageException {
    File tempFile = File.createTempFile("IMPORT_", "_ZIP");
    file.transferTo(tempFile);

    App installedApp = appManager.installApp(tempFile, file.getOriginalFilename());
    AppStatus appStatus = installedApp.getAppState();

    if (!appStatus.ok()) {
      String message = i18nManager.getI18n().getString(installedApp.getAppState().getMessage());

      throw new WebMessageException(conflict(message));
    }

    return new ResponseEntity<>(installedApp, HttpStatus.CREATED);
  }

  @PutMapping
  @RequiresAuthority(anyOf = M_DHIS_WEB_APP_MANAGEMENT)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reloadApps() {
    appManager.reloadApps();
  }

  @GetMapping("/{app}/**")
  public void renderApp(
      @PathVariable("app") String appName, HttpServletRequest request, HttpServletResponse response)
      throws IOException, WebMessageException, ForbiddenException {
    String contextPath = HttpServletRequestPaths.getContextPath(request);
    App application = appManager.getApp(appName, contextPath);

    if (application == null) {
      throw new WebMessageException(notFound("App '" + appName + "' not found."));
    }

    if (!appManager.isAccessible(application)) {
      throw new ForbiddenException("You don't have access to application " + appName + ".");
    }

    if (application.getAppState() == AppStatus.DELETION_IN_PROGRESS) {
      throw new WebMessageException(
          conflict("App '" + appName + "' deletion is still in progress."));
    }

    // Get page requested
    String resource = getResourcePath(request.getPathInfo(), application, contextPath);

    log.debug("Rendering resource {} from app {}", resource, application.getKey());

    ResourceResult resourceResult = appManager.getAppResource(application, resource, contextPath);
    if (resourceResult instanceof ResourceFound found) {
      serveResource(request, response, found.resource());
      return;
    }
    if (resourceResult instanceof Redirect redirect) {
      String redirectUrl = TextUtils.cleanUrlPathOnly(application.getBaseUrl(), redirect.path());
      String queryString = request.getQueryString();
      if (queryString != null) {
        redirectUrl += "?" + queryString;
      }
      log.debug(String.format("App resource redirected to: %s", redirectUrl));
      response.sendRedirect(redirectUrl);
    }
    if (resourceResult instanceof ResourceNotFound) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private void serveResource(
      HttpServletRequest request, HttpServletResponse response, Resource resource)
      throws IOException {
    String filename = resource.getFilename();
    log.debug("App filename: '{}'", filename);

    if (new ServletWebRequest(request, response).checkNotModified(resource.lastModified())) {
      return;
    }

    String mimeType = request.getSession().getServletContext().getMimeType(filename);

    if (mimeType != null) {
      response.setContentType(mimeType);
    }

    response.setContentLengthLong(resource.contentLength());
    StreamUtils.copyThenCloseInputStream(resource.getInputStream(), response.getOutputStream());
  }

  @DeleteMapping("/{app}")
  @RequiresAuthority(anyOf = M_DHIS_WEB_APP_MANAGEMENT)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteApp(
      @PathVariable("app") String app, @RequestParam(required = false) boolean deleteAppData)
      throws WebMessageException {
    App appToDelete = appManager.getApp(app);
    if (appToDelete == null) {
      throw new WebMessageException(notFound("App does not exist: " + app));
    }

    if (appToDelete.getAppState() == AppStatus.DELETION_IN_PROGRESS) {
      throw new WebMessageException(conflict("App is already being deleted: " + app));
    }

    if (appToDelete.getAppStorageSource() == AppStorageSource.BUNDLED) {
      throw new WebMessageException(badRequest("Bundled apps cannot be deleted."));
    }

    appManager.deleteApp(appToDelete, deleteAppData);
  }

  @SuppressWarnings("unchecked")
  @PostMapping(value = "/config", consumes = ContextUtils.CONTENT_TYPE_JSON)
  @RequiresAuthority(anyOf = M_DHIS_WEB_APP_MANAGEMENT)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void setConfig(HttpServletRequest request) throws IOException, WebMessageException {
    Map<String, String> config = renderService.fromJson(request.getInputStream(), Map.class);

    if (config == null) {
      throw new WebMessageException(conflict("No config specified"));
    }
  }

  // --------------------------------------------------------------------------
  // Helpers
  // --------------------------------------------------------------------------
  private String getResourcePath(String path, App app, String contextPath) {
    String resourcePath = path;
    String appPrefix = "/" + AppManager.INSTALLED_APP_PREFIX + app.getKey();

    if (resourcePath.startsWith(contextPath)) {
      resourcePath = resourcePath.substring(contextPath.length());
    }

    if (resourcePath.startsWith(appPrefix)) {
      resourcePath = resourcePath.substring(appPrefix.length());
    } else if (resourcePath.equals(appPrefix)) {
      resourcePath = "";
    }

    // if path is prefixed by any protocol, clear it out (this is to ensure
    // that only files inside app directory can be resolved)
    resourcePath = REGEX_REMOVE_PROTOCOL.matcher(resourcePath).replaceAll("");

    return resourcePath;
  }
}
