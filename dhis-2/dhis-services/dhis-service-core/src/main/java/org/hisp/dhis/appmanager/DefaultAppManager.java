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
package org.hisp.dhis.appmanager;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType.RESTRICTED;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hisp.dhis.apphub.AppHubService;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheBuilderProvider;
import org.hisp.dhis.datastore.DatastoreNamespace;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.CurrentUserUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * @author Saptarshi Purkayastha
 */
@Slf4j
@Component("org.hisp.dhis.appmanager.AppManager")
public class DefaultAppManager implements AppManager {
  public static final String INVALID_FILTER_MSG = "Invalid filter: ";

  private static final Set<String> EXCLUSION_APPS = Set.of("Line Listing");

  private final DhisConfigurationProvider dhisConfigurationProvider;
  private final AppHubService appHubService;
  private final AppStorageService localAppStorageService;
  private final AppStorageService jCloudsAppStorageService;
  private final DatastoreService datastoreService;

  private final AppMenuManager appMenuManager;

  /**
   * In-memory storage of installed apps. Initially loaded on startup. Should not be cleared during
   * runtime.
   */
  private final Cache<App> appCache;

  public DefaultAppManager(
      DhisConfigurationProvider dhisConfigurationProvider,
      AppHubService appHubService,
      @Qualifier("org.hisp.dhis.appmanager.LocalAppStorageService")
          AppStorageService localAppStorageService,
      @Qualifier("org.hisp.dhis.appmanager.JCloudsAppStorageService")
          AppStorageService jCloudsAppStorageService,
      DatastoreService datastoreService,
      CacheBuilderProvider cacheBuilderProvider,
      AppMenuManager appMenuManager) {
    checkNotNull(dhisConfigurationProvider);
    checkNotNull(localAppStorageService);
    checkNotNull(jCloudsAppStorageService);
    checkNotNull(datastoreService);
    checkNotNull(cacheBuilderProvider);

    this.dhisConfigurationProvider = dhisConfigurationProvider;
    this.appHubService = appHubService;
    this.localAppStorageService = localAppStorageService;
    this.jCloudsAppStorageService = jCloudsAppStorageService;
    this.datastoreService = datastoreService;
    this.appCache = cacheBuilderProvider.<App>newCacheBuilder().forRegion("appCache").build();
    this.appMenuManager = appMenuManager;
  }

  // -------------------------------------------------------------------------
  // AppManagerService implementation
  // -------------------------------------------------------------------------

  private Stream<App> getAppsStream() {
    return appCache.getAll().filter(app -> app.getAppState() != AppStatus.DELETION_IN_PROGRESS);
  }

  private Stream<App> getAccessibleAppsStream() {
    return getAppsStream().filter(this::isAccessible);
  }

  @Override
  public List<App> getApps(String contextPath, int max) {
    return getAppsList(getAccessibleAppsStream(), max, contextPath);
  }

  private Boolean isDashboardPluginType(App app) {
    return app.hasPluginEntrypoint()
        && (app.getPluginType() == null
            || app.getPluginType().equalsIgnoreCase(AppManager.DASHBOARD_PLUGIN_TYPE));
  }

  @Override
  public List<App> getDashboardPlugins(String contextPath, int max) {
    Predicate<App> filter = defaultFilter();

    return getAppsList(getAccessibleAppsStream().filter(filter), max, contextPath);
  }

  @Override
  public List<App> getDashboardPlugins(String contextPath, int max, boolean skipCore) {
    Predicate<App> filter = defaultFilter();

    if (skipCore) {
      filter =
          filter.and(
              app -> !EXCLUSION_APPS.contains(trimToEmpty(app.getName())) && !app.isCoreApp());
    }

    return getAppsList(getAccessibleAppsStream().filter(filter), max, contextPath);
  }

  /**
   * Builds the default {@link Predicate} filter to be applied on a list of {@link App} objects.
   *
   * @return the filter as {@link Predicate}.
   */
  private Predicate<App> defaultFilter() {
    return app -> app.getAppType() == AppType.DASHBOARD_WIDGET || isDashboardPluginType(app);
  }

  /**
   * This method will initialize the {@link App} objects based on the given "contextPath" and return
   * the list of {@link App} respecting the "max" size requested.
   *
   * @param stream the {@link Stream} of {@link App} objects.
   * @param max the max elements to be returned.
   * @param contextPath the path used to initialize each {@link App}.
   * @return the list of {@link App}.
   */
  private List<App> getAppsList(Stream<App> stream, int max, String contextPath) {
    if (max >= 0) {
      stream = stream.limit(max);
    }

    return stream
        .map(
            app -> {
              app.init(contextPath);
              return app;
            })
        .collect(toList());
  }

  @Override
  public List<App> getApps(String contextPath) {
    return this.getApps(contextPath, -1);
  }

  @Override
  public App getApp(String appName) {
    // Checks for app.getUrlFriendlyName which is the key of AppMap

    Optional<App> appOptional = appCache.getIfPresent(appName);
    if (appOptional.isPresent() && this.isAccessible(appOptional.get())) {
      return appOptional.get();
    }

    // If no apps are found, check for original name
    return getAccessibleAppsStream()
        .filter(app -> app.getShortName().equals(appName))
        .findFirst()
        .orElse(null);
  }

  @Override
  public List<WebModule> getMenu(String contextPath) {
    return getAccessibleAppMenu(contextPath);
  }

  private List<WebModule> getAccessibleAppMenu(String contextPath) {
    // these are the bundle modules
    List<WebModule> modules = appMenuManager.getAccessibleWebModules(this);

    List<App> apps =
        getApps(contextPath).stream()
            .filter(
                app ->
                    app.getAppType() == AppType.APP && app.hasAppEntrypoint() && !app.isBundled())
            .toList();

    // map installed apps to the WebModule object
    modules.addAll(apps.stream().map(WebModule::getModule).toList());

    return modules;
  }

  private void applyFilter(Set<App> apps, String key, String operator, String value) {
    if ("appType".equalsIgnoreCase(key)) {
      String appType = value != null ? value.toUpperCase() : null;
      apps.retainAll(AppManager.filterAppsByType(AppType.valueOf(appType), apps));
    } else if ("name".equalsIgnoreCase(key)) {
      apps.retainAll(AppManager.filterAppsByName(value, apps, operator));
    } else if ("shortName".equalsIgnoreCase(key)) {
      apps.retainAll(AppManager.filterAppsByShortName(value, apps, operator));
    } else if ("pluginType".equalsIgnoreCase(key)) {
      apps.retainAll(AppManager.filterAppsByPluginType(value, apps));
    } else if ("bundled".equalsIgnoreCase(key)) {
      boolean isBundled = "true".equalsIgnoreCase(value);
      apps.retainAll(AppManager.filterAppsByIsBundled(isBundled, apps));
    }
  }

  @Override
  public List<App> filterApps(List<String> filters, String contextPath) {
    List<App> apps = getApps(contextPath);
    Set<App> returnList = new HashSet<>(apps);

    for (String filter : filters) {
      String[] split = filter.split(":");

      if (split.length != 3) {
        throw new QueryParserException(INVALID_FILTER_MSG + filter);
      }

      if (!"name".equalsIgnoreCase(split[0])
          && !"shortName".equalsIgnoreCase(split[0])
          && !"eq".equalsIgnoreCase(split[1])) {
        throw new QueryParserException(INVALID_FILTER_MSG + filter);
      }

      if ("bundled".equalsIgnoreCase(split[0])
          && !"true".equalsIgnoreCase(split[2])
          && !"false".equalsIgnoreCase(split[2])) {
        throw new QueryParserException(INVALID_FILTER_MSG + filter);
      }

      applyFilter(returnList, split[0], split[1], split[2]);
    }

    return new ArrayList<>(returnList);
  }

  @Override
  public App getApp(String key, String contextPath) {
    Collection<App> apps = getApps(contextPath);

    for (App app : apps) {
      if (key.equals(app.getKey())) {
        return app;
      }
    }

    return null;
  }

  @Override
  public App installApp(File file, String fileName) {
    App app = jCloudsAppStorageService.installApp(file, fileName, appCache);

    log.info(
        String.format(
            "Installed App with ID %s (status: %s)", app.getAppHubId(), app.getAppState()));

    if (app.getAppState().ok()) {
      appCache.put(app.getKey(), app);
      registerDatastoreProtection(app);
    }

    return app;
  }

  @Override
  public App installApp(UUID appHubId) {

    App installedApp = new App();
    if (appHubId == null) {
      installedApp.setAppState(AppStatus.NOT_FOUND);
      return installedApp;
    }

    try {
      String versionJson = appHubService.getAppHubApiResponse("v2", "appVersions/" + appHubId);
      if (versionJson == null || versionJson.isEmpty()) {
        log.info(String.format("No version found for id %s", appHubId));
        installedApp.setAppState(AppStatus.NOT_FOUND);
        return installedApp;
      }
      JsonString downloadUrlNode = JsonMixed.of(versionJson).getString("downloadUrl");
      if (downloadUrlNode.isUndefined()) {
        log.info(
            String.format(
                "No download URL property found in response for id %s: %s", appHubId, versionJson));
        installedApp.setAppState(AppStatus.NOT_FOUND);
        return installedApp;
      }
      String downloadUrl = downloadUrlNode.string();
      URL url = new URL(downloadUrl);

      String filename = FilenameUtils.getName(downloadUrl);

      return installApp(getFile(url), filename);

    } catch (IOException ex) {
      log.info(String.format("No version found for id %s", appHubId));
      installedApp.setAppState(AppStatus.NOT_FOUND);
    } catch (ConflictException | URISyntaxException e) {
      log.error("Failed to install app with id " + appHubId, e);
      installedApp.setAppState(AppStatus.INSTALLATION_FAILED);
    }

    return installedApp;
  }

  @Override
  public boolean exists(String appName) {
    return getApp(appName) != null;
  }

  @Override
  public void deleteApp(App app, boolean deleteAppData) {
    if (app != null) {
      getAppStorageServiceByApp(app).deleteApp(app);
      App otherVersionApp = getApp(app.getName());
      if (otherVersionApp == null) {
        unregisterDatastoreProtection(app);
      }
      if (deleteAppData) {
        deleteAppData(app);
      }
      appCache.invalidate(app.getKey());
    }
  }

  @Override
  public boolean markAppToDelete(App app) {
    Optional<App> appOpt = appCache.get(app.getKey());
    if (appOpt.isEmpty()) return false;
    App appFromCache = appOpt.get();
    appFromCache.setAppState(AppStatus.DELETION_IN_PROGRESS);
    appCache.put(app.getKey(), appFromCache);
    return true;
  }

  @Override
  public String getAppHubUrl() {
    String baseUrl =
        StringUtils.trimToNull(
            dhisConfigurationProvider.getProperty(ConfigurationKey.APPHUB_BASE_URL));
    String apiUrl =
        StringUtils.trimToNull(
            dhisConfigurationProvider.getProperty(ConfigurationKey.APPHUB_API_URL));

    return "{" + "\"baseUrl\": \"" + baseUrl + "\", " + "\"apiUrl\": \"" + apiUrl + "\"" + "}";
  }

  /**
   * Reloads apps by triggering the process to discover apps from local filesystem and remote cloud
   * storage and installing all detected apps. This method is invoked automatically on startup.
   */
  @Override
  @PostConstruct
  public void reloadApps() {
    localAppStorageService.discoverInstalledApps().values().stream()
        .filter(app -> !exists(app.getKey()))
        .forEach(this::installApp);

    jCloudsAppStorageService.discoverInstalledApps().values().forEach(this::installApp);
  }

  private void installApp(App app) {
    appCache.put(app.getKey(), app);
    registerDatastoreProtection(app);
  }

  @Override
  public boolean isAccessible(App app) {
    return app.getKey().equals("login")
        || CurrentUserUtil.hasAnyAuthority(
            List.of(
                Authorities.ALL.toString(),
                Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString(),
                app.getSeeAppAuthority()));
  }

  @Override
  public ResourceResult getAppResource(App app, String pageName) throws IOException {
    return getAppStorageServiceByApp(app).getAppResource(app, pageName);
  }

  /**
   * We need to handle scenarios when the Resource is a File (knowing the content length) or when
   * it's URL (not knowing the content length and having to make a call, e.g. remote web link in AWS
   * S3/MinIO) - otherwise content length can be set to 0 which causes issues at the front-end,
   * returning an empty body. If it's a URL resource, an underlying HEAD request is made to get the
   * content length.
   *
   * @param resource resource to check content length
   * @return the content length or -1 (unknown size) if exception caught
   */
  @Override
  public int getUriContentLength(@Nonnull Resource resource) {
    try {
      if (resource.isFile()) {
        return (int) resource.contentLength();
      } else {
        URLConnection urlConnection = resource.getURL().openConnection();
        return urlConnection.getContentLength();
      }
    } catch (IOException e) {
      log.error("Error trying to retrieve content length of Resource: {}", e.getMessage());
      e.printStackTrace();
      return -1;
    }
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

  private AppStorageService getAppStorageServiceByApp(App app) {
    if (app != null && app.getAppStorageSource() == AppStorageSource.LOCAL) {
      return localAppStorageService;
    }
    return jCloudsAppStorageService;
  }

  private void deleteAppData(App app) {
    String namespace = app.getActivities().getDhis().getNamespace();
    if (namespace != null && !namespace.isEmpty()) {
      datastoreService.deleteNamespace(namespace);
      log.info(String.format("Deleted app namespace '%s'", namespace));
    }
  }

  private void registerDatastoreProtection(App app) {
    registerMainNamespaceProtection(app);
    registerAdditionalNamespaceProtection(app);
  }

  private void registerMainNamespaceProtection(App app) {
    String namespace = app.getActivities().getDhis().getNamespace();
    if (namespace == null || namespace.isEmpty()) return;
    String adminAuthority = Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString();
    String[] authorities =
        app.getShortName() == null
            ? new String[] {adminAuthority}
            : new String[] {adminAuthority, app.getSeeAppAuthority()};
    datastoreService.addProtection(
        new DatastoreNamespaceProtection(namespace, RESTRICTED, authorities));
  }

  private void registerAdditionalNamespaceProtection(App app) {
    List<DatastoreNamespace> additionalNamespaces =
        app.getActivities().getDhis().getAdditionalNamespaces();
    if (additionalNamespaces == null || additionalNamespaces.isEmpty()) return;
    for (DatastoreNamespace ns : additionalNamespaces) {
      Set<String> readAuthorities = ns.getAllAuthorities();
      Set<String> writeAuthorities = ns.getAuthorities();
      if (writeAuthorities == null || writeAuthorities.isEmpty()) writeAuthorities = Set.of();
      String namespace = requireNonNull(ns.getNamespace());
      datastoreService.addProtection(
          new DatastoreNamespaceProtection(
              namespace, RESTRICTED, readAuthorities, RESTRICTED, writeAuthorities));
    }
  }

  private void unregisterDatastoreProtection(App app) {
    AppDhis dhis = app.getActivities().getDhis();
    String namespace = dhis.getNamespace();
    if (namespace != null && !namespace.isEmpty()) {
      datastoreService.removeProtection(namespace);
    }
    List<DatastoreNamespace> additionalNamespaces = dhis.getAdditionalNamespaces();
    if (additionalNamespaces != null && !additionalNamespaces.isEmpty()) {
      additionalNamespaces.forEach(ns -> datastoreService.removeProtection(ns.getNamespace()));
    }
  }

  private static File getFile(URL url) throws IOException {
    URLConnection connection = url.openConnection();

    File tempFile = File.createTempFile("dhis", null);

    tempFile.deleteOnExit();

    try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
        FileOutputStream out = new FileOutputStream(tempFile)) {
      IOUtils.copy(in, out);
    }
    return tempFile;
  }
}
