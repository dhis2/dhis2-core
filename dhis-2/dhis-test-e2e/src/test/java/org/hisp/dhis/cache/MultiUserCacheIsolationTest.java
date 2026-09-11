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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * E2E test verifying that ETags are user-specific — different users get different ETags for the
 * same endpoint, ensuring per-user cache isolation.
 */
class MultiUserCacheIsolationTest extends CacheApiTest {

  @BeforeAll
  void setUp() {
    loginActions.loginAsSuperUser();
  }

  @Test
  void differentUsersGetDifferentETags_dataElements() {
    String path = "/dataElements?fields=id&page=1&pageSize=1";

    // User A: superuser
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse superuserResponse = probe.get(path);
    CacheAssertions.assertCacheHeaders(superuserResponse);
    String superuserEtag = superuserResponse.etag();
    assertNotNull(superuserEtag, "Superuser should receive an ETag");

    // User B: admin
    CacheProbeUser.ADMIN.login(loginActions);
    CacheProbe.CacheResponse adminResponse = probe.get(path);
    CacheAssertions.assertCacheHeaders(adminResponse);
    String adminEtag = adminResponse.etag();
    assertNotNull(adminEtag, "Admin should receive an ETag");

    // ETags should differ because they include user UID
    assertNotEquals(
        superuserEtag,
        adminEtag,
        "Different users should get different ETags for the same endpoint");
  }

  @Test
  void sameUserGetsSameETag_organisationUnits() {
    String path = "/organisationUnits?fields=id&page=1&pageSize=1";

    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse firstResponse = probe.get(path);
    CacheAssertions.assertCacheHeaders(firstResponse);

    // Same user, same request — should get 304 with same ETag
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse conditionalResponse = probe.getIfNoneMatch(path, firstResponse.etag());
    CacheAssertions.assertNotModified(conditionalResponse, firstResponse.etag());
  }

  @Test
  void userAEtagDoesNotWorkForUserB() {
    String path = "/indicators?fields=id&page=1&pageSize=1";

    // Get ETag as superuser
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse superuserResponse = probe.get(path);
    CacheAssertions.assertCacheHeaders(superuserResponse);
    String superuserEtag = superuserResponse.etag();

    // Use superuser's ETag as admin — should NOT get 304
    CacheProbeUser.ADMIN.login(loginActions);
    CacheProbe.CacheResponse adminConditionalResponse = probe.getIfNoneMatch(path, superuserEtag);
    assertEquals(
        200,
        adminConditionalResponse.statusCode(),
        "User A's ETag should not produce 304 for User B");
  }

  @Test
  void differentUsersGetDifferentETags_compositeEndpoint() {
    // me/settings is a composite endpoint
    String path = "/me/settings";

    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse superuserResponse = probe.get(path);
    String superuserEtag = superuserResponse.etag();

    CacheProbeUser.ADMIN.login(loginActions);
    CacheProbe.CacheResponse adminResponse = probe.get(path);
    String adminEtag = adminResponse.etag();

    if (superuserEtag != null && adminEtag != null) {
      assertNotEquals(
          superuserEtag,
          adminEtag,
          "Different users should get different ETags for composite endpoints");
    }
  }
}
