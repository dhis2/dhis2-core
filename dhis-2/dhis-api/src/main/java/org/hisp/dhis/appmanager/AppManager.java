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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.hisp.dhis.user.User;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableSet;

/**
 * @author Saptarshi Purkayastha
 */
public interface AppManager
{
    String ID = AppManager.class.getName();

    String BUNDLED_APP_PREFIX = "dhis-web-";

    ImmutableSet<String> BUNDLED_APPS = ImmutableSet.of(
        // Javascript apps
        "aggregate-data-entry",
        "app-management",
        "cache-cleaner",
        "capture",
        "dashboard",
        "data-administration",
        "data-visualizer",
        "data-quality",
        "datastore",
        "event-reports",
        "event-visualizer",
        "import-export",
        "interpretation",
        "maintenance",
        "maps",
        "menu-management",
        "messaging",
        "pivot",
        "reports",
        "scheduler",
        "settings",
        "sms-configuration",
        "tracker-capture",
        "translations",
        "usage-analytics",
        "user",
        "user-profile",

        // Struts apps
        "approval",
        "dataentry",
        "maintenance-mobile" );

    String WEB_MAINTENANCE_APPMANAGER_AUTHORITY = "M_dhis-web-maintenance-appmanager";

    /**
     * Returns a list of all installed apps.
     *
     * @param contextPath the context path of this instance.
     * @return list of installed apps
     */
    List<App> getApps( String contextPath );

    /**
     * Returns a list of all installed apps.
     *
     * @param contextPath the context path of this instance.
     * @param skipCore if true, core apps will be filtered out.
     * @return list of installed apps
     */
    List<App> getApps( String contextPath, boolean skipCore );

    /**
     * Returns a list of installed apps.
     *
     * @param appType the app type filter.
     * @param max the max number of apps to return.
     * @return a list of apps.
     */
    List<App> getApps( AppType appType, int max );

    /**
     * Returns a list of installed apps.
     *
     * @param appType the app type filter.
     * @param max the max number of apps to return.
     * @param skipCore if true, core apps will be filtered out.
     * @return a list of apps.
     */
    List<App> getApps( AppType appType, int max, boolean skipCore );

    App getApp( String appName );

    /**
     * Returns a list of all installed apps with AppType equal the given Type
     *
     * @return list of installed apps with given AppType
     */
    List<App> getAppsByType( AppType appType, Collection<App> apps );

    /**
     * Returns a list of all installed apps with name equal the given name and
     * operator. Currently supports eq and ilike.
     *
     * @return list of installed apps with given name
     */
    List<App> getAppsByName( String name, Collection<App> apps, String operator );

    /**
     * Returns a list of all installed apps with shortName equal the given name
     * and operator. Currently supports eq and ilike.
     *
     * @return list of installed apps with given name
     */
    List<App> getAppsByShortName( String shortName, Collection<App> apps, String operator );

    /**
     * Returns a list of all installed apps which are either bundled or not
     * bundled operator. Currently supports eq.
     *
     * @return list of installed apps with given isBundled property
     */
    List<App> getAppsByIsBundled( boolean isBundled, Collection<App> apps );

    /**
     * Return a list of all installed apps with given filter list Currently
     * support filtering by AppType and name
     *
     * @param filter
     * @return Return a list of all installed apps with given filter list
     */
    List<App> filterApps( List<String> filter, String contextPath );

    /**
     * Returns the app with the given key (folder name).
     *
     * @param key the app key.
     * @param contextPath the context path of this instance.
     * @return the app with the given key.
     */
    App getApp( String key, String contextPath );

    /**
     * Returns apps which are accessible to the current user.
     *
     * @param contextPath the context path of this instance.
     * @return apps which are accessible to the current user.
     */
    List<App> getAccessibleApps( String contextPath );

    /**
     * Installs the app.
     *
     * @param file the app file.
     * @param fileName the name of the app file.
     * @throws IOException if the app manifest file could not be read.
     */
    AppStatus installApp( File file, String fileName );

    /**
     * Indicates whether the app with the given name exist.
     *
     * @param appName the name of the app-
     * @return true if the app exists.
     */
    boolean exists( String appName );

    /**
     * Deletes the given app.
     *
     * @param app the app to delete.
     * @param deleteAppData decide if associated data in dataStore should be
     *        deleted or not.
     */
    void deleteApp( App app, boolean deleteAppData );

    /**
     * Reload list of apps.
     */
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
    boolean isAccessible( App app );

    /**
     * Indicates whether the given app is accessible to the given user.
     *
     * @param app the app.
     * @param user the user.
     * @return true if app is accessible.
     */
    boolean isAccessible( App app, User user );

    /**
     * Returns the app associated with the namespace, or null if no app is
     * associated.
     *
     * @param namespace the namespace to check
     * @return App or null
     */
    App getAppByNamespace( String namespace );

    /**
     * Looks up and returns the file associated with the app and pageName, if it
     * exists
     *
     * @param app the app to look up files for
     * @param pageName the page requested
     * @return the Resource representing the file, or null if no file was found
     */
    Resource getAppResource( App app, String pageName )
        throws IOException;

    /**
     * Sets the app status to DELETION_IN_PROGRESS.
     *
     * @param app The app that has to be marked as deleted.
     * @return true if the status was changed in this method.
     */
    boolean markAppToDelete( App app );

}
