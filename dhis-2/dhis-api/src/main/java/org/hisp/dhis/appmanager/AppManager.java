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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hisp.dhis.appmanager.webmodules.WebModule;
import org.springframework.core.io.Resource;

/**
 * @author Saptarshi Purkayastha
 */
public interface AppManager {
  static final String ID = AppManager.class.getName();

  static final String BUNDLED_APP_PREFIX = "dhis-web-";
  static final String INSTALLED_APP_PREFIX = "api/apps/";

  static final Set<String> ALWAYS_ACCESSIBLE_APPS = Set.of("login", "global-shell", "user-profile");

  static final Set<String> MENU_APP_EXCLUSIONS =
      Set.of("login", "global-shell"); // TODO: instead filter by app type

  static final String DASHBOARD_PLUGIN_TYPE = "DASHBOARD";

  /**
   * Returns a list of all installed apps.
   *
   * @param contextPath the context path of this instance.
   * @param max the maximum number of apps to return, -1 to return all
   * @return list of installed apps
   */
  List<App> getApps(String contextPath, int max);

  /**
   * Returns a list of all installed apps.
   *
   * @param contextPath the context path of this instance.
   * @return list of installed apps
   */
  List<App> getApps(String contextPath);

  /**
   * Returns a list of installed apps with plugins. Includes both DASHBOARD_WIDGET app types and APP
   * app types with a pluginLaunchPath.
   *
   * @param contextPath the context path of this instance.
   * @param max the maximum number of apps to return, -1 to return all
   * @return a list of apps.
   */
  List<App> getDashboardPlugins(String contextPath, int max);

  /**
   * Returns a list of installed apps with plugins. Includes both {@link AppType.DASHBOARD_WIDGET}
   * app types and {@link AppType.APP} app types with a pluginLaunchPath.
   *
   * @param contextPath the context path of this instance.
   * @param max the maximum number of apps to return, -1 to return all.
   * @param skipCore if true, all core apps will be removed from the result list.
   * @return a list of {@link App}.
   */
  List<App> getDashboardPlugins(String contextPath, int max, boolean skipCore);

  /**
   * Returns the installed app with a given name
   *
   * @param appName the name of the app to return.
   * @return the app with the requested name
   */
  App getApp(String appName);

  /**
   * Returns the menu items that the user has access to
   *
   * @param contextPath the context path of this instance.
   * @return a list of WebModules
   */
  List<WebModule> getMenu(String contextPath);

  /**
   * Return a list of all installed apps with given filter list Currently support filtering by
   * AppType and name
   *
   * @param filter
   * @return Return a list of all installed apps with given filter list
   */
  List<App> filterApps(List<String> filter, String contextPath);

  /**
   * Returns the app with the given key (folder name).
   *
   * @param key the app key.
   * @param contextPath the context path of this instance.
   * @return the app with the given key.
   */
  App getApp(String key, String contextPath);

  /**
   * Installs the app.
   *
   * @param file the app file.
   * @param fileName the name of the app file.
   * @return the installed app instance
   */
  App installApp(File file, String fileName);

  /**
   * Installs an app from the AppHub with the given ID.
   *
   * @param appHubId A unqiue ID for a specific app version
   * @return outcome of the installation
   */
  App installAppByHubId(UUID appHubId);

  /**
   * Indicates whether the app with the given name exist.
   *
   * @param appName the name of the app-
   * @return true if the app exists.
   */
  boolean exists(String appName);

  /** Reload list of apps. */
  void reloadApps();

  /**
   * Returns the url of the app repository
   *
   * @return url of app hub
   */
  String getAppHubUrl();

  /**
   * Indicates whether the given app is accessible to the current user.
   *
   * @param app the app.
   * @return true if app is accessible.
   */
  boolean isAccessible(App app);

  /**
   * Looks up and returns the file associated with the app and pageName, if it exists No template
   * replacement is performed, only the raw resource is returned.
   *
   * @param app the app to look up files for
   * @param pageName the page requested
   * @return the {@link ResourceResult}
   */
  ResourceResult getRawAppResource(App app, String pageName) throws IOException;

  /**
   * Looks up and returns the file associated with the app and pageName, if it exists Template
   * replacement is performed where applicable on the returned resource.
   *
   * @param app the app to look up files for
   * @param pageName the page requested
   * @param contextPath the context path of this instance.
   * @return the {@link ResourceResult}
   */
  ResourceResult getAppResource(App app, String pageName, String contextPath) throws IOException;

  /**
   * Sets the app status to DELETION_IN_PROGRESS and trigger asynchronous deletion of the app.
   *
   * @param app The app that has to be marked as deleted.
   * @param deleteAppData decide if associated data in dataStore should be deleted or not.
   * @return true if the status was changed in this method.
   */
  boolean deleteApp(App app, boolean deleteAppData);

  int getUriContentLength(Resource resource);

  // -------------------------------------------------------------------------
  // Static methods for manipulating a collection of apps
  // -------------------------------------------------------------------------

  /**
   * Returns a list of all installed apps with AppType equal the given Type
   *
   * @return list of installed apps with given AppType
   */
  public static List<App> filterAppsByType(AppType appType, Collection<App> apps) {
    return apps.stream().filter(app -> app.getAppType() == appType).collect(Collectors.toList());
  }

  private static Boolean checkAppStringProperty(
      final String propertyValue, final String testValue, final String operator) {
    return ("ilike".equalsIgnoreCase(operator)
            && propertyValue.toLowerCase().contains(testValue.toLowerCase()))
        || ("eq".equalsIgnoreCase(operator) && propertyValue.equals(testValue));
  }

  /**
   * Returns a list of all installed apps with name equal the given name and operator. Currently
   * supports eq and ilike.
   *
   * @return list of installed apps with given name
   */
  public static List<App> filterAppsByName(
      final String name, Collection<App> apps, final String operator) {
    return apps.stream()
        .filter(app -> checkAppStringProperty(app.getName(), name, operator))
        .collect(Collectors.toList());
  }

  /**
   * Returns a list of all installed apps with shortName equal the given name and operator.
   * Currently supports eq and ilike.
   *
   * @return list of installed apps with given name
   */
  public static List<App> filterAppsByShortName(
      final String name, Collection<App> apps, final String operator) {
    return apps.stream()
        .filter(app -> checkAppStringProperty(app.getShortName(), name, operator))
        .collect(Collectors.toList());
  }

  /**
   * Returns a list of all installed apps which are either bundled or not bundled operator.
   * Currently supports eq.
   *
   * @return list of installed apps with given isBundled property
   */
  public static List<App> filterAppsByIsBundled(final boolean isBundled, Collection<App> apps) {
    return apps.stream().filter(app -> app.isBundled() == isBundled).collect(Collectors.toList());
  }

  /**
   * Returns a list of all installed apps which have the given plugin type.
   *
   * @return list of installed apps with given isBundled property
   */
  static List<App> filterAppsByPluginType(String pluginType, Collection<App> apps) {
    return apps.stream()
        .filter(app -> app.getPluginType() != null)
        .filter(app -> app.getPluginType().equals(pluginType))
        .toList();
  }
}
