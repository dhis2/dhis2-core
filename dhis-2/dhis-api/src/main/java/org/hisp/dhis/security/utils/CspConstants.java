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

public class CspConstants {
  private CspConstants() {}

  public static final String SCRIPT_SOURCE_DEFAULT = "script-src 'none'; ";

  public static final String CONTENT_SECURITY_POLICY_HEADER_NAME = "Content-Security-Policy";
  public static final String FRAME_ANCESTORS_DEFAULT_CSP = "frame-ancestors 'self'";

  /**
   * Defense-in-depth directives applied to every emitted policy. Each one is a policy-level
   * directive that does NOT fall back to {@code default-src} per the CSP spec, so it has to be
   * declared explicitly:
   *
   * <ul>
   *   <li>{@code base-uri 'self'} — locks down {@code <base href>} rewriting.
   *   <li>{@code form-action 'self'} — prevents off-origin form submission (incl. via the {@code
   *       formaction} button attribute); also a partial CSRF defense layer.
   *   <li>{@code object-src 'none'} — kills the legacy plugin attack surface (Flash / Java applet /
   *       PDF plugin) — DHIS2 doesn't use any of these.
   * </ul>
   *
   * <p>{@code upgrade-insecure-requests} is intentionally NOT in this list. It was tried but caused
   * the e2e {@code OAuth2Test} to time out: the OAuth2 flow uses a Selenium-driven redirect to
   * {@code http://localhost:9090/oauth2/code/dhis2-client} and Chromium upgrades that navigation
   * under the directive in some configurations, hitting a non-existent https endpoint. Deferred to
   * a Phase 2 follow-up that scopes it more narrowly or wires it via the existing {@code
   * CSP_UPGRADE_INSECURE_ENABLED} config key.
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
   * CartoDB tile-server origins (Fastly CDN, round-robin across {@code a/b/c} subdomains) used by
   * the bundled Maps app. HTTPS variant — always allowed in the app-host policy.
   *
   * <p>Apps that need to call other external services (analytics, third-party APIs, etc.) must be
   * granted an explicit override via an admin-controlled mechanism — see the per-app CSP follow-up.
   * When that lands, these CartoDB origins should move into the Maps app's manifest and an
   * admin-approved override row, and the constant should return to strictly same-origin.
   */
  public static final String CARTODB_BASEMAP_ORIGINS =
      "https://cartodb-basemaps-a.global.ssl.fastly.net"
          + " https://cartodb-basemaps-b.global.ssl.fastly.net"
          + " https://cartodb-basemaps-c.global.ssl.fastly.net";

  /**
   * CartoDB origins served over plain HTTP. Browsers don't reliably apply {@code
   * upgrade-insecure-requests} to cross-origin sub-resource fetches when the parent page is on
   * {@code http://localhost} (Chromium quirk: the CSP source-list check runs against the
   * pre-upgrade URL on some Chrome versions, so an https-only allow-list rejects the http fetch
   * before the upgrade fires). To keep the bundled Maps app working in dev, {@link
   * org.hisp.dhis.webapi.security.csp.CspPolicyService#constructAppHostCspPolicy} appends these
   * origins to the app-host policy only when {@code server.https} is OFF in {@code dhis.conf} (i.e.
   * dev / non-TLS deployments). Production (HTTPS on) gets the strict https-only policy.
   */
  public static final String CARTODB_BASEMAP_HTTP_ORIGINS =
      "http://cartodb-basemaps-a.global.ssl.fastly.net"
          + " http://cartodb-basemaps-b.global.ssl.fastly.net"
          + " http://cartodb-basemaps-c.global.ssl.fastly.net";

  public static final String APP_HOST_CSP_POLICY =
      "default-src 'self'; style-src 'self' 'unsafe-inline'; child-src 'self' blob:;"
          + " img-src 'self' data: "
          + CARTODB_BASEMAP_ORIGINS
          + "; connect-src 'self' "
          + CARTODB_BASEMAP_ORIGINS
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
