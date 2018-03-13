package org.hisp.dhis.appmanager;
/*
 * Copyright (c) 2004-2018, University of Oslo
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

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @author Stian Sandvold
 */
public interface AppStorageService
{

    String MANIFEST_FILENAME = "manifest.webapp";
    String APPS_DIR = "apps";

    /**
     * Looks trough the appropriate directory of apps to find all installed apps
     * using the AppStorageService
     */
    void discoverInstalledApps();

    /**
     * Returns a map of all apps found using the AppStorageService, referenced by
     * their app names.
     * 
     * @return A map of all app names and apps found using the AppStorageService
     */
    Map<String, App> getApps();

    /**
     * Returns a map of namespaces and the apps reserving them.
     * 
     * @return a map of namespaces and the apps reserving them
     */
    Map<String,App> getReservedNamespaces();

    /**
     * Installs an app using the AppServiceStore.
     * 
     * @param file the zip file containing the app
     * @param filename The name of the file
     * @return The status of the installation
     */
    AppStatus installApp( File file, String filename );

    /**
     * Deletes an app from the AppStoreService.
     * 
     * @param app the app to delete
     * @return true if app is deleted, false if something fails
     */
    boolean deleteApp( App app );

    /**
     * Looks up and returns a resource representing the page for the app requested. If
     * the resource is not found, return null.
     * 
     * @param app the app to look up
     * @param pageName the name of the page to look up
     * @return The resource representing the page, or null if not found
     */
    Resource getAppResource( App app, String pageName )
        throws IOException;
}
