/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.webapi.security.csp.app;

import java.util.List;
import javax.annotation.CheckForNull;
import org.hisp.dhis.appmanager.App;

/**
 * The entrypoints a per-app CSP override can address. {@link #ALL} applies to every entrypoint of
 * the app; a specific entrypoint merges onto it.
 *
 * @author Morten Svanæs
 */
public final class CspEntrypoints {
  private CspEntrypoints() {}

  /** All entrypoints of an app, the default. */
  public static final String ALL = "*";

  /** The app launch page, {@link App#getLaunchPath()} ({@code index.html}). */
  public static final String APP = "app";

  /** The plugin page, {@link App#getPluginLaunchPath()} ({@code plugin.html}). */
  public static final String PLUGIN = "plugin";

  /** The PWA service worker script, {@code service-worker.js}. */
  public static final String WORKER = "worker";

  public static final List<String> KEYS = List.of(ALL, APP, PLUGIN, WORKER);

  public static final List<String> SPECIFIC = List.of(APP, PLUGIN, WORKER);

  public static final String SERVICE_WORKER_FILE = "service-worker.js";

  private static final String DEFAULT_LAUNCH_PATH = "index.html";

  public static boolean isValid(@CheckForNull String entrypoint) {
    return entrypoint != null && KEYS.contains(entrypoint);
  }

  /**
   * Resolves the entrypoint of an app resource.
   *
   * @param app the app
   * @param relativePath the resource path relative to the app root, with or without a leading slash
   * @return the entrypoint, {@link #ALL} for other HTML documents, or {@code null} when the
   *     resource is neither an HTML document nor the worker script and so needs no per-app policy
   */
  @CheckForNull
  public static String resolve(App app, @CheckForNull String relativePath) {
    String path = stripLeadingSlash(relativePath == null ? "" : relativePath);
    if (path.isEmpty()) {
      return APP;
    }
    String launchPath = stripLeadingSlash(app.getLaunchPath());
    if (path.equals(launchPath.isEmpty() ? DEFAULT_LAUNCH_PATH : launchPath)) {
      return APP;
    }
    String pluginPath = stripLeadingSlash(app.getPluginLaunchPath());
    if (!pluginPath.isEmpty() && path.equals(pluginPath)) {
      return PLUGIN;
    }
    if (path.equals(SERVICE_WORKER_FILE)) {
      return WORKER;
    }
    if (path.endsWith(".html") || path.endsWith(".htm") || path.endsWith("/")) {
      return ALL;
    }
    return null;
  }

  private static String stripLeadingSlash(@CheckForNull String path) {
    if (path == null) {
      return "";
    }
    String result = path;
    while (result.startsWith("/")) {
      result = result.substring(1);
    }
    return result;
  }
}
