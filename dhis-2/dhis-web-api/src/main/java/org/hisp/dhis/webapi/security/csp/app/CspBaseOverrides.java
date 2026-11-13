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

import static org.hisp.dhis.webapi.security.csp.app.CspEntrypoints.ALL;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Core base overrides: sources that bundled apps need on top of the app-host baseline, shipped with
 * the server and applied to that app only. Administrators can disable single base sources per
 * entrypoint ({@code disabledBaseSources}) but never edit this table.
 *
 * <p>Maps ({@code maps}, app, plugin and worker entrypoints) gets the basemap tile hosts of its
 * built-in basemap catalog:
 *
 * <ul>
 *   <li>{@code tiles.openfreemap.org} for the vector "OSM Light" basemap (style, tiles, glyphs).
 *   <li>{@code tile.openstreetmap.org} and {@code {a,b,c}.tile.openstreetmap.org} for "OSM
 *       Detailed".
 *   <li>{@code cartodb-basemaps-{a,b,c}.global.ssl.fastly.net} for the CartoDB basemaps, also used
 *       by the CartoDB external map layers of the demo database.
 *   <li>{@code tiles.maps.eox.at} for the "Sentinel-2 EOX" WMS basemap.
 * </ul>
 *
 * <p>Maps also gets {@code script-src 'wasm-unsafe-eval'} on the app and plugin entrypoints for its
 * WebAssembly RTL text plugin.
 *
 * <p>Exact origins only, no wildcard host. While {@code server.https} is off the plain HTTP
 * variants are added too: the browser inherits the {@code http://} page scheme for tile fetches in
 * dev setups, and Chrome checks the source list before any upgrade.
 *
 * @author Morten Svanæs
 */
public final class CspBaseOverrides {
  private CspBaseOverrides() {}

  public static final String MAPS_APP_KEY = "maps";

  static final List<String> MAPS_BASEMAP_HOSTS =
      List.of(
          "tiles.openfreemap.org",
          "tile.openstreetmap.org",
          "a.tile.openstreetmap.org",
          "b.tile.openstreetmap.org",
          "c.tile.openstreetmap.org",
          "cartodb-basemaps-a.global.ssl.fastly.net",
          "cartodb-basemaps-b.global.ssl.fastly.net",
          "cartodb-basemaps-c.global.ssl.fastly.net",
          "tiles.maps.eox.at");

  /**
   * The service worker is included because the Maps PWA worker fetches basemap tiles itself (for
   * its runtime cache), and those fetches are checked against the worker script's policy.
   */
  private static final List<String> MAPS_ENTRYPOINTS =
      List.of(CspEntrypoints.APP, CspEntrypoints.PLUGIN, CspEntrypoints.WORKER);

  private static final List<String> MAPS_DIRECTIVES = List.of("img-src", "connect-src");

  /**
   * Maps (maplibre) compiles the WebAssembly RTL text shaping plugin ({@code mapbox-gl-rtl-text})
   * in a blob worker that inherits the policy of the document that starts it, so the app and plugin
   * documents need {@code 'wasm-unsafe-eval'}. It is an unsafe token: it is reported as a base
   * unsafe token ({@link #unsafeTokens}) so it stays visible, and an administrator can disable it
   * per entrypoint with {@code disabledBaseSources: ["script-src 'wasm-unsafe-eval'"]}.
   */
  private static final List<String> MAPS_WASM_ENTRYPOINTS =
      List.of(CspEntrypoints.APP, CspEntrypoints.PLUGIN);

  private static final String WASM_UNSAFE_EVAL = "'wasm-unsafe-eval'";

  /**
   * The base override of an app per entrypoint ({@code "*"} and specific entrypoints).
   *
   * @return entrypoint to directive to sources, empty when the app has no base override
   */
  public static Map<String, Map<String, List<String>>> forApp(String appKey, boolean https) {
    if (!MAPS_APP_KEY.equals(appKey)) {
      return Map.of();
    }
    List<String> origins = new ArrayList<>();
    MAPS_BASEMAP_HOSTS.forEach(h -> origins.add("https://" + h));
    if (!https) {
      MAPS_BASEMAP_HOSTS.forEach(h -> origins.add("http://" + h));
    }
    Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();
    for (String ep : MAPS_ENTRYPOINTS) {
      Map<String, List<String>> directives = new LinkedHashMap<>();
      MAPS_DIRECTIVES.forEach(d -> directives.put(d, List.copyOf(origins)));
      if (MAPS_WASM_ENTRYPOINTS.contains(ep)) {
        directives.put("script-src", List.of(WASM_UNSAFE_EVAL));
      }
      result.put(ep, directives);
    }
    return result;
  }

  /** The base sources of one entrypoint: the {@code "*"} sources merged with the specific ones. */
  public static Map<String, Set<String>> forEntrypoint(
      String appKey, String entrypoint, boolean https) {
    Map<String, Map<String, List<String>>> app = forApp(appKey, https);
    Map<String, Set<String>> result = new LinkedHashMap<>();
    for (String ep : List.of(ALL, entrypoint)) {
      app.getOrDefault(ep, Map.of())
          .forEach((d, s) -> result.computeIfAbsent(d, k -> new LinkedHashSet<>()).addAll(s));
      if (ALL.equals(entrypoint)) {
        break;
      }
    }
    return result;
  }

  /**
   * The unsafe sources shipped in the base override of an app, as {@code "<entrypoint> <directive>
   * <token>"} like the {@code unsafeTokens} of administrator overrides, so a UI can badge them.
   */
  public static List<String> unsafeTokens(String appKey, boolean https) {
    List<String> tokens = new ArrayList<>();
    forApp(appKey, https)
        .forEach(
            (ep, directives) ->
                directives.forEach(
                    (d, sources) ->
                        sources.stream()
                            .filter(CspOverrideValidator.UNSAFE_KEYWORDS::contains)
                            .forEach(v -> tokens.add(ep + " " + d + " " + v))));
    return tokens;
  }

  /** All base sources of an app as {@code "<directive> <source>"}, for disabledBaseSources. */
  public static Set<String> sourceKeys(String appKey, boolean https) {
    Set<String> keys = new LinkedHashSet<>();
    forApp(appKey, https)
        .values()
        .forEach(directives -> directives.forEach((d, s) -> s.forEach(v -> keys.add(d + " " + v))));
    return keys;
  }
}
