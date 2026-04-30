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
   * Strict default CSP policy applied to all endpoints. This policy only allows resources from the
   * same origin.
   */
  public static final String DEFAULT_CSP_POLICY =
      "default-src 'self'; style-src 'self' 'unsafe-inline';";

  /**
   * CSP policy for endpoints serving user-uploaded content. This policy disables all unsafe sources
   * to prevent injection attacks on potentially untrusted content.
   */
  public static final String USER_UPLOADED_CONTENT_CSP_POLICY = "default-src 'none';";

  /**
   * CSP policy for the app host endpoint that renders installed DHIS2 apps inside an iframe.
   * Same-origin by default: no wildcards in {@code img-src} or {@code connect-src}. The bundled
   * Maps app loads basemap tiles from CartoDB (Fastly CDN, round-robin across {@code a/b/c}
   * subdomains), so those three origins are temporarily allow-listed here for both schemes (HTTP
   * for local dev on {@code http://localhost}, HTTPS for production).
   *
   * <p>Apps that need to call other external services (analytics, third-party APIs, etc.) must be
   * granted an explicit override via an admin-controlled mechanism — see the per-app CSP follow-up.
   * When that lands, the CartoDB origins listed here should move into the Maps app's manifest and
   * an admin-approved override row, and the constant should return to strictly same-origin.
   */
  private static final String CARTODB_BASEMAP_ORIGINS =
      "https://cartodb-basemaps-a.global.ssl.fastly.net"
          + " https://cartodb-basemaps-b.global.ssl.fastly.net"
          + " https://cartodb-basemaps-c.global.ssl.fastly.net"
          + " http://cartodb-basemaps-a.global.ssl.fastly.net"
          + " http://cartodb-basemaps-b.global.ssl.fastly.net"
          + " http://cartodb-basemaps-c.global.ssl.fastly.net";

  public static final String APP_HOST_CSP_POLICY =
      "default-src 'self'; style-src 'self' 'unsafe-inline'; child-src 'self' blob:;"
          + " img-src 'self' data: "
          + CARTODB_BASEMAP_ORIGINS
          + "; connect-src 'self' "
          + CARTODB_BASEMAP_ORIGINS
          + ";";

  /**
   * CSP policy for the rendered OpenAPI HTML documentation pages, which emit inline {@code onclick}
   * handlers from {@code OpenApiRenderer}. Allows {@code script-src 'self' 'unsafe-inline'} so the
   * interactive doc page works under the default-deny baseline. Scoped to the OpenAPI HTML
   * endpoints only via {@link org.hisp.dhis.webapi.security.csp.CspOpenApiDocs @CspOpenApiDocs}.
   */
  public static final String OPENAPI_DOCS_CSP_POLICY =
      "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';";
}
