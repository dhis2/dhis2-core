package org.hisp.dhis.appmanager;

/*
 * Copyright (c) 2004-2016, University of Oslo
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

import org.hisp.dhis.user.User;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author Saptarshi Purkayastha
 */
public interface AppManager
{
    String ID = AppManager.class.getName();

    String APPS_DIR = "/apps";

    /**
     * Returns a list of all the installed apps at @see getAppFolderPath
     *
     * @param contextPath the context path of this instance.
     * @return list of installed apps
     */
    List<App> getApps( String contextPath );

    /**
     * Returns a list of all installed apps with AppType equal the given Type
     *
     * @return list of installed apps with given AppType
     */
    List<App> getAppsByType( AppType appType, Collection<App> apps );

    /**
     * Returns a list of all installed apps with name equal the given name
     * and operator. Currently supports eq and ilike.
     *
     * @return list of installed apps with given name
     */
    List<App> getAppsByName( String name, Collection<App> apps, String operator );

    /**
     * Return a list of all installed apps with given filter list
     * Currently support filtering by AppType and name
     *
     * @param filter
     * @return Return a list of all installed apps with given filter list
     */
    List<App> filterApps( List<String> filter, String contextPath );

    /**
     * Returns the app with the given key (folder name).
     *
     * @param key         the app key.
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
     * @param file     the app file.
     * @param fileName the name of the app file.
     * @throws IOException if the app manifest file could not be read.
     */
    AppStatus installApp( File file, String fileName )
        throws IOException;

    /**
     * Does the app with name appName exist?
     *
     * @param appName
     * @return
     */
    boolean exists( String appName );

    /**
     * Deletes the app with the given name.
     *
     * @param name          the app name.
     * @param deleteAppData decide if associated data in dataStore should be deleted or not.
     * @return true if the delete was successful, false if there is no app with
     * the given name or if the app could not be removed from the file
     * system.
     */
    boolean deleteApp( String name, boolean deleteAppData );

    /**
     * Reload list of apps.
     */
    void reloadApps();

    /**
     * Returns the full path to the folder where apps are extracted
     *
     * @return app folder path
     */
    String getAppFolderPath();

    /**
     * Returns the url of the app repository
     *
     * @return url of appstore
     */
    String getAppStoreUrl();

    /**
     * Saves the URL of the apps repository
     *
     * @param appStoreUrl
     */
    void setAppStoreUrl( String appStoreUrl );

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
     * @param app  the app.
     * @param user the user.
     * @return true if app is accessible.
     */
    boolean isAccessible( App app, User user );

    /**
     * Returns the app associated with the namespace, or null if no app is associated.
     *
     * @param namespace the namespace to check
     * @return App or null
     */
    App getAppByNamespace( String namespace );
}
