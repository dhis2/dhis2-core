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
package org.hisp.dhis.security.utils;

import java.util.List;
import java.util.stream.Collectors;

public class CspConstants {
  private CspConstants() {}

  public static final String SCRIPT_SOURCE_DEFAULT = "script-src 'none'; ";

  public static final String CONTENT_SECURITY_POLICY_HEADER_NAME = "Content-Security-Policy";
  public static final String FRAME_ANCESTORS_DEFAULT_CSP = "frame-ancestors 'self'";

  /**
   * Directives applied to every emitted policy. Each one is a policy-level directive that does NOT
   * fall back to {@code default-src} per the CSP spec, so it has to be declared explicitly:
   *
   * <ul>
   *   <li>{@code base-uri 'self'} — locks down {@code <base href>} rewriting.
   *   <li>{@code form-action 'self'} — prevents off-origin form submission (incl. via the {@code
   *       formaction} button attribute); also a partial CSRF defense layer.
   *   <li>{@code object-src 'none'} — kills the legacy plugin attack surface (Flash / Java applet /
   *       PDF plugin) — DHIS2 doesn't use any of these.
   * </ul>
   *
   * <p>{@code upgrade-insecure-requests} is appended at runtime by {@link
   * org.hisp.dhis.webapi.security.csp.CspPolicyService} based on {@link
   * org.hisp.dhis.external.conf.ConfigurationKey#CSP_UPGRADE_INSECURE_ENABLED} (default ON), only
   * when {@code server.https} is enabled.
   */
  private static final String COMMON_HARDENING =
      "base-uri 'self'; form-action 'self'; object-src 'none';";

  /**
   * Strict default CSP policy applied to all endpoints. This policy only allows resources from the
   * same origin.
   */
  public static final String DEFAULT_CSP_POLICY =
      "default-src 'self'; style-src 'self' 'unsafe-inline'; " + COMMON_HARDENING;

  /**
   * CSP policy for endpoints serving user-uploaded content. This policy disables all unsafe sources
   * to prevent injection attacks on potentially untrusted content.
   */
  public static final String USER_UPLOADED_CONTENT_CSP_POLICY =
      "default-src 'none'; " + COMMON_HARDENING;

  /**
   * Basemap tile-service hosts the bundled Maps app ships with, read from the app's built-in
   * basemap catalog:
   *
   * <ul>
   *   <li>{@code cartodb-basemaps-{a,b,c}.global.ssl.fastly.net} for "OSM Light", also used by the
   *       CartoDB external map layers of the demo database.
   *   <li>{@code {a,b,c}.tile.openstreetmap.org} for "OSM Detailed".
   *   <li>{@code tiles.maps.eox.at} for the "Sentinel-2 EOX" WMS basemap.
   * </ul>
   *
   * <p>Exact origins only, no wildcard host and no scheme-relative entry. Basemaps that only appear
   * once an administrator configures them (Bing and Azure via the {@code keyBingMapsApiKey} system
   * setting, Earth Engine via a Google service account) and administrator-created external map
   * layer or GeoJSON URL origins are deliberately not listed here: they are allowed explicitly
   * through {@link org.hisp.dhis.external.conf.ConfigurationKey#CSP_MAP_SOURCES}.
   */
  private static final List<String> MAPS_BASEMAP_HOSTS =
      List.of(
          "cartodb-basemaps-a.global.ssl.fastly.net",
          "cartodb-basemaps-b.global.ssl.fastly.net",
          "cartodb-basemaps-c.global.ssl.fastly.net",
          "a.tile.openstreetmap.org",
          "b.tile.openstreetmap.org",
          "c.tile.openstreetmap.org",
          "tiles.maps.eox.at");

  /** Built-in Maps basemap origins over HTTPS, always allowed in the app-host policy. */
  public static final String MAPS_BASEMAP_ORIGINS = basemapOrigins("https");

  /**
   * Built-in Maps basemap origins served over plain HTTP. To keep the bundled Maps app working in
   * dev, {@link org.hisp.dhis.webapi.security.csp.CspPolicyService#constructAppHostCspPolicy}
   * appends these origins to the app-host policy only when {@code server.https} is OFF in {@code
   * dhis.conf} (i.e. dev / non-TLS deployments). Production (HTTPS on) gets the strict https-only
   * policy.
   */
  public static final String MAPS_BASEMAP_HTTP_ORIGINS = basemapOrigins("http");

  private static String basemapOrigins(String scheme) {
    return MAPS_BASEMAP_HOSTS.stream()
        .map(host -> scheme + "://" + host)
        .collect(Collectors.joining(" "));
  }

  /** App images include generated chart exports and App Hub icons, never uploaded documents. */
  public static final String APP_HOST_CSP_POLICY =
      "default-src 'self'; style-src 'self' 'unsafe-inline'; child-src 'self' blob:;"
          + " img-src 'self' data: blob: https://apps.dhis2.org "
          + MAPS_BASEMAP_ORIGINS
          + "; connect-src 'self' "
          + MAPS_BASEMAP_ORIGINS
          + "; "
          + COMMON_HARDENING;

  /**
   * CSP policy for the rendered OpenAPI HTML documentation pages, which emit inline {@code onclick}
   * handlers from {@code OpenApiRenderer}. Allows {@code script-src 'self' 'unsafe-inline'} so the
   * interactive doc page works under the default-deny baseline. Scoped to the OpenAPI HTML
   * endpoints only via {@link org.hisp.dhis.webapi.security.csp.CspOpenApiDocs @CspOpenApiDocs}.
   */
  public static final String OPENAPI_DOCS_CSP_POLICY =
      "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
          + COMMON_HARDENING;
}
