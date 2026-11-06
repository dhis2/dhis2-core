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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.nimbusds.jose.jwk.source.JWKSetBasedJWKSource;
import com.nimbusds.jose.jwk.source.JWKSetSource;
import com.nimbusds.jose.jwk.source.JWKSetSourceWrapper;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RefreshAheadCachingJWKSetSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;

/**
 * @author Morten (Morty) <netroms@gmail.com>
 */
class JwkSourceCacheTest {

  private static final String JWKS_URL = "https://idp.example.org/.well-known/jwks.json";

  private final JwkSourceCache cache = new JwkSourceCache();

  @Test
  void closeShutsDownTheRefreshAheadExecutorOfEveryCachedSource() {
    ExecutorService first = refreshAheadExecutor(cache.get("esignet", JWKS_URL));
    ExecutorService second = refreshAheadExecutor(cache.get("other", JWKS_URL));
    assertFalse(first.isShutdown());
    assertFalse(second.isShutdown());

    cache.close();

    assertTrue(first.isShutdown());
    assertTrue(second.isShutdown());
  }

  @Test
  void getAfterCloseBuildsAFreshSource() {
    JWKSource<SecurityContext> before = cache.get("esignet", JWKS_URL);

    cache.close();

    JWKSource<SecurityContext> after = cache.get("esignet", JWKS_URL);
    assertNotSame(before, after);
    assertFalse(refreshAheadExecutor(after).isShutdown());
  }

  private static ExecutorService refreshAheadExecutor(JWKSource<SecurityContext> source) {
    JWKSetSource<SecurityContext> current =
        ((JWKSetBasedJWKSource<SecurityContext>) source).getJWKSetSource();
    while (current instanceof JWKSetSourceWrapper<SecurityContext> wrapper) {
      if (current instanceof RefreshAheadCachingJWKSetSource<SecurityContext> refreshAhead) {
        return refreshAhead.getExecutorService();
      }
      current = wrapper.getSource();
    }
    return fail("No refresh-ahead cache in the JWK source chain");
  }
}
