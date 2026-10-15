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
package org.hisp.dhis.appmanager;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType.RESTRICTED;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.apphub.AppHubService;
import org.hisp.dhis.appmanager.AppBundleInfo.BundledAppInfo;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.hisp.dhis.cache.Cache;
import org.hisp.dhis.cache.CacheBuilderProvider;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.HashUtils;
import org.hisp.dhis.common.Locale;
import org.hisp.dhis.datastore.DatastoreNamespace;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.external.conf.ConfigurationKey;
import org.hisp.dhis.external.conf.DhisConfigurationProvider;
import org.hisp.dhis.feedback.ConflictException;
import org.hisp.dhis.i18n.I18nManager;
import org.hisp.dhis.i18n.locale.LocaleManager;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonString;
import org.hisp.dhis.query.QueryParserException;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
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

  /**
   * Max number of bundled apps installed concurrently on startup. File uploads across all of these
   * share a single bounded pool in {@link BlobStoreAppStorageService}, so this only governs how
   * many app zips are read/decompressed at once, not total upload concurrency.
   */
  private static final int APP_INSTALL_PARALLELISM = 16;

  @Autowired private UserService userService;
  @Autowired private ObjectMapper jsonMapper;
  @Autowired private LocaleManager localeManager;

  private final DhisConfigurationProvider dhisConfigurationProvider;
  private final AppHubService appHubService;
  private final AppStorageService blobStoreAppStorageService;
  private final DatastoreService datastoreService;
  private final BundledAppManager bundledAppManager;
  private final I18nManager i18nManager;

  /**
   * In-memory storage of installed apps. Initially loaded on startup. Should not be cleared during
   * runtime.
   */
  private final Cache<App> appCache;

  public DefaultAppManager(
      DhisConfigurationProvider dhisConfigurationProvider,
      AppHubService appHubService,
      AppStorageService blobStoreAppStorageService,
      DatastoreService datastoreService,
      CacheBuilderProvider cacheBuilderProvider,
      I18nManager i18nManager,
      LocaleManager localeManager,
      BundledAppManager bundledAppManager) {

    checkNotNull(dhisConfigurationProvider);
    checkNotNull(blobStoreAppStorageService);
    checkNotNull(datastoreService);
    checkNotNull(cacheBuilderProvider);
    checkNotNull(i18nManager);
    checkNotNull(bundledAppManager);
    checkNotNull(localeManager);

    this.dhisConfigurationProvider = dhisConfigurationProvider;
    this.appHubService = appHubService;
    this.blobStoreAppStorageService = blobStoreAppStorageService;
    this.datastoreService = datastoreService;
    this.appCache = cacheBuilderProvider.<App>newCacheBuilder().forRegion("appCache").build();
    this.i18nManager = i18nManager;
    this.localeManager = localeManager;
    this.bundledAppManager = bundledAppManager;
  }

  /**
   * Reloads apps by triggering the process to discover apps from local filesystem and remote cloud
   * storage and installing all detected apps. This method is invoked automatically on startup.
   */
  @Override
  @PostConstruct
  public void reloadApps() {
    Map<String, Pair<App, BundledAppInfo>> installedApps =
        blobStoreAppStorageService.discoverInstalledApps();

    // When the store starts empty (typical first startup) there can be no pre-existing apps to
    // replace, so the per-app duplicate-removal scan is skipped to avoid an expensive O(n^2)
    // rescan of storage during bundled app installation.
    boolean storeStartedEmpty = installedApps.isEmpty();
    installBundledApps(installedApps, storeStartedEmpty);
    // Invalidate the previous app cache
    appCache.invalidateAll();
    // Cache all discovered apps
    installedApps.values().forEach(app -> cacheApp(app.getLeft()));
    log.info("Loaded {} apps.", installedApps.size());
  }

  /**
   * A bundled app that needs to be (re)installed, together with its zip resource and the zip's
   * compressed size in bytes (computed once; used to order installs largest-first).
   */
  private record BundledAppInstall(
      String appKey, BundledAppInfo bundledAppInfo, Resource zipFileResource, long zipSize) {}

  /**
   * Installs bundled apps, by looking in the classpath for app .zip files. If the bundled app is
   * already installed, it can be overwritten with a newer one if the Etag is different.
   *
   * <p>Apps are installed concurrently: each app is large enough that its files already upload in
   * parallel, but the apps themselves used to install one-after-another, so the many small apps
   * waited behind the few large ones. Installing apps in parallel lets the small apps upload in the
   * shadow of the large ones (each app reads its own zip, so decompression also parallelizes). The
   * shared upload pool in {@link BlobStoreAppStorageService} keeps total in-flight uploads bounded.
   *
   * @param installedApps the Map with all existing apps, we overwrite existing apps in this Map
   *     with new ones.
   * @param storeStartedEmpty whether the app store was empty before this install run; when {@code
   *     true} the per-app duplicate-removal scan is skipped as there can be nothing to replace.
   */
  private void installBundledApps(
      @Nonnull Map<String, Pair<App, BundledAppInfo>> installedApps, boolean storeStartedEmpty) {
    boolean removeExisting = !storeStartedEmpty;

    // First collect which bundled apps actually need (re)installing, without touching storage. An
    // app is (re)installed if it isn't installed yet, or if its installed Etag differs from the
    // bundled one. Apps that are already up to date keep their existing map entry untouched.
    List<BundledAppInstall> appsToInstall = new ArrayList<>();
    bundledAppManager.installBundledApps(
        (app, bundledAppInfo, zipFileResource) -> {
          Pair<App, BundledAppInfo> existing = installedApps.get(app.getKey());
          if (needsInstall(existing, bundledAppInfo)) {
            if (existing != null) {
              log.info(
                  "Bundled app '{}' has a different Etag than the installed one and will replace it",
                  bundledAppInfo.getName());
            }
            appsToInstall.add(
                new BundledAppInstall(
                    app.getKey(), bundledAppInfo, zipFileResource, zipSize(zipFileResource)));
          }
        });

    if (appsToInstall.isEmpty()) return;

    // Install the largest apps first. A few apps (e.g. Maintenance) are far bigger than the rest,
    // if they start last they end up running alone at the tail with no smaller apps left to overlap
    // them. Starting them first lets the many small apps install in their shadow, so total time
    // approaches the largest app's runtime rather than largest + the rest.
    appsToInstall.sort(Comparator.comparingLong(BundledAppInstall::zipSize).reversed());

    // Install in parallel, then merge results into the map on this thread (single-writer).
    ExecutorService appExecutor =
        Executors.newFixedThreadPool(
            Math.min(APP_INSTALL_PARALLELISM, appsToInstall.size()),
            new ThreadFactoryBuilder().setNameFormat("app-install-%d").setDaemon(true).build());
    try {
      List<CompletableFuture<Map.Entry<String, Pair<App, BundledAppInfo>>>> futures =
          new ArrayList<>(appsToInstall.size());
      for (BundledAppInstall item : appsToInstall) {
        futures.add(
            CompletableFuture.supplyAsync(
                () ->
                    Map.entry(
                        item.appKey(),
                        Pair.of(
                            installBundledAppResource(
                                item.zipFileResource(), item.bundledAppInfo(), removeExisting),
                            item.bundledAppInfo())),
                appExecutor));
      }
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
      for (CompletableFuture<Map.Entry<String, Pair<App, BundledAppInfo>>> future : futures) {
        Map.Entry<String, Pair<App, BundledAppInfo>> result = future.join();
        installedApps.put(result.getKey(), result.getValue());
      }
    } finally {
      appExecutor.shutdown();
    }
  }

  /**
   * Whether a bundled app needs to be (re)installed: {@code true} when it is not yet installed, or
   * when its installed bundled-app Etag differs from the one being offered.
   */
  private static boolean needsInstall(
      @CheckForNull Pair<App, BundledAppInfo> existing, @Nonnull BundledAppInfo bundledAppInfo) {
    if (existing == null) {
      return true;
    }
    BundledAppInfo installedAppInfo = existing.getRight();
    return installedAppInfo != null
        && installedAppInfo.getEtag() != null
        && !installedAppInfo.getEtag().equals(bundledAppInfo.getEtag());
  }

  /** Compressed size of the app's zip in bytes, or {@code 0} when it cannot be determined. */
  private static long zipSize(Resource zipFileResource) {
    try {
      return zipFileResource.contentLength();
    } catch (IOException e) {
      return 0L;
    }
  }

  private void cacheApp(@Nonnull App app) {
    if (app.getAppState() == AppStatus.OK) {
      app.setBundled(bundledAppManager.isBundledApp(app));
      computeCacheBustKey(app);
      appCache.put(app.getKey(), app);
      registerDatastoreProtection(app);
    }
  }

  private void computeCacheBustKey(@Nonnull App app) {
    if (app.getVersion() == null || app.getKey() == null) return;
    String source = app.getKey() + "|" + app.getVersion();
    app.setCacheBustKey(
        HashUtils.hashMD5(source.getBytes(StandardCharsets.UTF_8)).substring(0, 16));
  }

  private Stream<App> getAppsStream() {
    return appCache.getAll();
  }

  private Stream<App> getAccessibleAppsStream() {
    return getAppsStream().filter(this::isAccessible);
  }

  @Override
  @Nonnull
  public List<App> getApps(@CheckForNull String contextPath, int max) {
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
  @Nonnull
  private List<App> getAppsList(
      @Nonnull Stream<App> stream, int max, @CheckForNull String contextPath) {
    if (max >= 0) {
      stream = stream.limit(max);
    }

    Locale userLocale = localeManager.getCurrentLocale();

    return stream
        .map(
            app -> {
              app.init(contextPath);
              try {
                return app.localise(userLocale);
              } catch (RuntimeException e) {
                log.debug(
                    String.format("Could not localise app information for app: %s", app.getName()));
                return app;
              }
            })
        .collect(toList());
  }

  @Override
  @Nonnull
  public List<App> getApps(@CheckForNull String contextPath) {
    return this.getApps(contextPath, -1);
  }

  @Override
  public App getApp(String appName) {
    // Checks for app.getUrlFriendlyName which is the key of appCache
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
    List<WebModule> modules = getAccessibleAppMenu(contextPath);

    // Apply user-defined favorite apps order
    User currentUser = userService.getUserByUsername(CurrentUserUtil.getCurrentUsername());

    if (currentUser != null && currentUser.getApps() != null && !currentUser.getApps().isEmpty()) {
      final List<String> userApps = new ArrayList<>(currentUser.getApps());

      modules.sort(
          (m1, m2) -> {
            int i1 = userApps.indexOf(m1.getName());
            int i2 = userApps.indexOf(m2.getName());

            i1 = i1 == -1 ? 9999 : i1;
            i2 = i2 == -1 ? 9999 : i2;

            return Integer.compare(i1, i2);
          });
    }

    return modules;
  }

  private List<WebModule> getAccessibleAppMenu(@CheckForNull String contextPath) {
    List<WebModule> modules = new ArrayList<>();
    List<App> apps =
        getApps(contextPath).stream()
            .filter(app -> app.getAppType() == AppType.APP && app.hasAppEntrypoint())
            .toList();

    // map installed apps to the WebModule object
    modules.addAll(
        apps.stream()
            .filter(app -> !MENU_APP_EXCLUSIONS.contains(app.getKey()))
            .map(WebModule::getModule)
            .map(
                module -> {
                  // bundled apps in 42+ have the ability to add their name translations in the
                  // manifest
                  // so only use the bundled translations if no manifest translation exists
                  if (!module.isLocalised()) {
                    String bundledAppNameTranslation =
                        i18nManager.getI18n().getString(module.getName(), module.getDisplayName());
                    module.setDisplayName(bundledAppNameTranslation);
                    return module;
                  }
                  return module;
                })
            .toList());

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
  @CheckForNull
  public App getApp(@Nonnull String key, @Nonnull String contextPath) {
    Collection<App> apps = getApps(contextPath);
    for (App app : apps) {
      if (key.equals(app.getKey())) {
        return app;
      }
    }
    return null;
  }

  @Override
  @Nonnull
  public App installApp(@Nonnull File file) {
    // User-initiated installs may be upgrading an existing app, so always remove duplicates.
    return installAppZipFile(file, null, true);
  }

  @Nonnull
  private App installAppZipFile(
      @Nonnull File file, @CheckForNull BundledAppInfo bundledAppInfo, boolean removeExisting) {
    App app = blobStoreAppStorageService.installApp(file, appCache, bundledAppInfo, removeExisting);
    log.debug(
        String.format(
            "Installed App with AppHub ID %s (status: %s)", app.getAppHubId(), app.getAppState()));
    cacheApp(app);
    return app;
  }

  @Nonnull
  public App installBundledAppResource(
      @Nonnull Resource resource, @Nonnull BundledAppInfo bundledAppInfo, boolean removeExisting) {
    try {
      Path tempFile = Files.createTempFile("tmp-bundled-app-", CodeGenerator.generateUid());
      Files.copy(resource.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
      try {
        return installAppZipFile(tempFile.toFile(), bundledAppInfo, removeExisting);
      } finally {
        Files.deleteIfExists(tempFile);
      }
    } catch (IOException e) {
      log.error("Failed to install bundled app: '{}'", bundledAppInfo.getName(), e);
      throw new RuntimeException(e);
    }
  }

  @Override
  @Nonnull
  public App installAppByHubId(@CheckForNull UUID appHubId) {
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

      return installApp(getFile(url));

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
  public boolean deleteApp(@Nonnull App app, boolean deleteAppData) {
    Optional<App> appOpt = appCache.get(app.getKey());
    if (appOpt.isEmpty()) {
      return false;
    }

    // Originally bundled apps cannot be deleted, only overridden bundled apps can be deleted.
    if (bundledAppManager.isOriginallyBundledApp(appOpt.get())) {
      return false;
    }

    App appFromCache = appOpt.get();
    appCache.put(app.getKey(), appFromCache);

    blobStoreAppStorageService.deleteApp(app);
    reloadApps();

    // If a bundled version exists it will replace the deleted override.
    // In that case, deleting the app should not remove the namespace protection
    if (!app.isBundled()) {
      unregisterDatastoreProtection(app);
    }
    if (deleteAppData) {
      deleteAppData(app);
    }

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

  @Override
  public boolean isAccessible(App app) {
    return ALWAYS_ACCESSIBLE_APPS.contains(app.getKey())
        || CurrentUserUtil.hasAnyAuthority(
            List.of(
                Authorities.ALL.toString(),
                Authorities.M_DHIS_WEB_APP_MANAGEMENT.toString(),
                app.getSeeAppAuthority()));
  }

  @Override
  public ResourceResult getRawAppResource(App app, String pageName) throws IOException {
    return blobStoreAppStorageService.getAppResource(app, pageName);
  }

  @Override
  public ResourceResult getAppResource(App app, String pageName, String contextPath)
      throws IOException {
    ResourceResult resource = getRawAppResource(app, pageName);

    if (pageName.equals("/index.action")) {
      return new ResourceResult.Redirect(app.getLaunchPath());
    }

    if (resource instanceof ResourceResult.ResourceFound resourceFound) {
      if (pageName.equals("/manifest.webapp")) {
        app.getActivities().getDhis().setHref(contextPath);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        jsonMapper.writeValue(bout, app);
        ByteArrayResource byteArrayResource =
            toByteArrayResource(bout.toByteArray(), resourceFound.resource());
        return new ResourceResult.ResourceFound(byteArrayResource, "application/json");
      } else if (pageName.endsWith(".html")
          || (resourceFound.resource().getFilename() != null
              && resourceFound.resource().getFilename().endsWith(".html"))) {
        // Return raw HTML with correct mime type — AppHtmlTemplate is applied later
        // by AppController after cache-busting rewrite, so the cache stores only
        // request-independent content.
        return new ResourceResult.ResourceFound(
            resourceFound.resource(), "text/html;charset=UTF-8");
      } else if (pageName.endsWith(".js")
          || (resourceFound.resource().getFilename() != null
              && resourceFound.resource().getFilename().endsWith(".js"))) {
        // Read the original JS content
        String originalJsContent;
        try (InputStream inputStream = resourceFound.resource().getInputStream()) {
          originalJsContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        String modifiedJsContent =
            originalJsContent.replace(
                "REACT_APP_DHIS2_BASE_URL:\"..\"",
                "REACT_APP_DHIS2_BASE_URL:\"" + "../../.." + "\"");

        ByteArrayResource byteArrayResource =
            toByteArrayResource(
                modifiedJsContent.getBytes(StandardCharsets.UTF_8), resourceFound.resource());
        return new ResourceResult.ResourceFound(byteArrayResource, "application/javascript");
      }
    }

    return resource;
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
      }
      URLConnection urlConnection = resource.getURL().openConnection();
      return urlConnection.getContentLength();
    } catch (IOException e) {
      log.error("Error trying to retrieve content length of Resource: {}", e.getMessage());
      return -1;
    }
  }

  // -------------------------------------------------------------------------
  // Supportive methods
  // -------------------------------------------------------------------------

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

  private ByteArrayResource toByteArrayResource(byte[] bytes, Resource resource) {
    return new ByteArrayResource(bytes, resource.getDescription()) {
      @Override
      public String getFilename() {
        return resource.getFilename();
      }

      // This is necessary so that the content length is set correctly in getUriContentLength
      @Override
      public boolean isFile() {
        return true;
      }

      @Override
      public URL getURL() throws IOException {
        return resource.getURL();
      }

      @Override
      public long lastModified() throws IOException {
        return resource.lastModified();
      }
    };
  }
}
