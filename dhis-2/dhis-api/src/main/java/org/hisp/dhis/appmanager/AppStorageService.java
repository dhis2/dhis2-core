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
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.tuple.Pair;
import org.hisp.dhis.appmanager.AppBundleInfo.BundledAppInfo;
import org.hisp.dhis.appmanager.ResourceResult.Redirect;
import org.hisp.dhis.appmanager.ResourceResult.ResourceFound;
import org.hisp.dhis.appmanager.ResourceResult.ResourceNotFound;
import org.hisp.dhis.cache.Cache;

/**
 * @author Stian Sandvold
 */
public interface AppStorageService {

  String MANIFEST_FILENAME = "manifest.webapp";
  String MANIFEST_TRANSLATION_FILENAME = "manifest.webapp.translations.json";

  String APPS_DIR = "apps";

  /**
   * Looks trough the appropriate directory of apps to find all installed apps using the
   * AppStorageService
   *
   * @return A map of all app names and apps found
   */
  @Nonnull
  Map<String, Pair<App, BundledAppInfo>> discoverInstalledApps();

  /**
   * Installs an app using the AppServiceStore.
   *
   * @param file the zip file containing the app
   * @param appCache The app cache
   * @param bundledAppInfo bundled app info, can be null
   * @return The status of the installation
   */
  @Nonnull
  App installApp(
      @Nonnull File file,
      @Nonnull Cache<App> appCache,
      @CheckForNull BundledAppInfo bundledAppInfo);

  /**
   * Deletes the app from storage.
   *
   * @param app the app to delete
   */
  void deleteApp(@Nonnull App app);

  /**
   * Try to retrieve the requested app resource. The returned {@link ResourceResult} value will be
   * one of :
   *
   * <ul>
   *   <li>{@link ResourceFound} - when the resource exists
   *   <li>{@link ResourceNotFound} - when no resource found
   *   <li>{@link Redirect} - when a directory is found without a trailing '/'
   * </ul>
   *
   * @param app the app to look up
   * @param resource the name of the resource to look up (can be directory or file)
   * @return {@link ResourceResult}
   */
  @Nonnull
  ResourceResult getAppResource(@CheckForNull App app, @Nonnull String resource) throws IOException;
}
