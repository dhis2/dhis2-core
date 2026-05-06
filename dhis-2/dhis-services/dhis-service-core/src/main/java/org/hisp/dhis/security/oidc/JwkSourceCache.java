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
package org.hisp.dhis.security.oidc;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Caches one Nimbus {@link JWKSource} per OIDC registration id. Building a {@link JWKSource}
 * triggers an HTTPS fetch of the IdP's JWKS document; reusing the source preserves Nimbus's
 * built-in remote-key cache and refresh policy across logins.
 *
 * <p>Sources are constructed lazily on first call to {@link #get(String, String)}.
 *
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Component
public class JwkSourceCache {

  private static final int CONNECT_TIMEOUT_MS = 5_000;
  private static final int READ_TIMEOUT_MS = 5_000;

  private final ConcurrentMap<String, JWKSource<SecurityContext>> sources =
      new ConcurrentHashMap<>();

  /**
   * @param registrationId the OIDC registration id to cache under
   * @param jwkSetUri the IdP's JWKS endpoint URL
   * @return a cached or freshly built {@link JWKSource}
   * @throws IllegalArgumentException when {@code jwkSetUri} is malformed
   */
  public JWKSource<SecurityContext> get(String registrationId, String jwkSetUri) {
    return sources.computeIfAbsent(registrationId, id -> build(jwkSetUri));
  }

  private JWKSource<SecurityContext> build(String jwkSetUri) {
    try {
      DefaultResourceRetriever retriever =
          new DefaultResourceRetriever(
              CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS, JWKSourceBuilder.DEFAULT_HTTP_SIZE_LIMIT);
      return JWKSourceBuilder.create(new URL(jwkSetUri), retriever).build();
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Invalid JWKS URL: " + jwkSetUri, ex);
    }
  }
}
