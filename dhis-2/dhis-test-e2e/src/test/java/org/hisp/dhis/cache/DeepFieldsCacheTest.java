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
package org.hisp.dhis.cache;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Verifies the field-filter hop gate: {@code fields=} expressions that traverse more than one
 * reference hop bypass ETag caching entirely (always-fresh full responses), while shallow
 * expressions keep the conditional-caching contract. Covers both the regular metadata collection
 * endpoints and their {@code /gist} variants, which share URL resolution and the fields parser.
 *
 * <p>Before the gate, a deep request was served bounded-stale 304s: a change to the two-hop entity
 * (for example a {@code Category} rename under {@code
 * /dataElements?fields=categoryCombo[categories[name]]}) did not rotate the ETag until the TTL
 * window flipped.
 *
 * @author Morten Svanaes
 */
class DeepFieldsCacheTest extends CacheApiTest {

  private static final String DEEP_FIELDS_PATH =
      "/dataElements?fields=categoryCombo[categories[name]]&page=1&pageSize=1";

  private static final String DEEP_FIELDS_GIST_PATH =
      "/dataElements/gist?fields=categoryCombo.categories.name&page=1&pageSize=1";

  private static final String SHALLOW_FIELDS_PATH =
      "/dataElements?fields=id,name,categoryCombo[id]&page=1&pageSize=1";

  @BeforeAll
  void beforeDeepFieldsCacheTests() {
    loginActions.loginAsSuperUser();
  }

  @Test
  void deepFieldsRequestBypassesEtagCaching() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheAssertions.assertNoAutomaticCacheHeaders(probe.get(DEEP_FIELDS_PATH));
  }

  @Test
  void deepFieldsGistRequestBypassesEtagCaching() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheAssertions.assertNoAutomaticCacheHeaders(probe.get(DEEP_FIELDS_GIST_PATH));
  }

  @Test
  void shallowFieldsRequestKeepsEtagCaching() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse initialResponse = probe.get(SHALLOW_FIELDS_PATH);
    CacheAssertions.assertCacheHeaders(initialResponse);

    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheAssertions.assertNotModified(
        probe.getIfNoneMatch(SHALLOW_FIELDS_PATH, initialResponse.etag()), initialResponse.etag());
  }
}
