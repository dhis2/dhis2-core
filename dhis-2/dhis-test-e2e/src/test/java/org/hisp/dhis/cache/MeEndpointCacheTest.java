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

import com.google.gson.JsonObject;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * E2E tests for /api/me endpoint ETag caching. The /api/me endpoint is a composite endpoint
 * depending on User, UserRole, UserGroup, UserSetting, and OrganisationUnit entity types.
 */
class MeEndpointCacheTest extends CacheApiTest {

  @BeforeAll
  void setUp() {
    loginActions.loginAsSuperUser();
  }

  @Test
  void meEndpointReturnsCacheHeaders() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse response = probe.get("/me");
    CacheAssertions.assertCacheHeaders(response);
  }

  @Test
  void meEndpointReturns304OnSecondRequest() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse first = probe.get("/me");
    CacheAssertions.assertCacheHeaders(first);

    CacheProbe.CacheResponse conditional = probe.getIfNoneMatch("/me", first.etag());
    CacheAssertions.assertNotModified(conditional, first.etag());
  }

  @Test
  void differentUsersGetDifferentETags_me() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse superuserResponse = probe.get("/me");
    String superuserEtag = superuserResponse.etag();
    assertNotNull(superuserEtag);

    CacheProbeUser.ADMIN.login(loginActions);
    CacheProbe.CacheResponse adminResponse = probe.get("/me");
    String adminEtag = adminResponse.etag();
    assertNotNull(adminEtag);

    assertNotEquals(superuserEtag, adminEtag, "Different users should get different ETags for /me");
  }

  @Test
  void meWithQueryStringProducesDifferentETag() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse baseResponse = probe.get("/me");
    CacheProbe.CacheResponse fieldsResponse = probe.get("/me?fields=id,displayName");

    String baseEtag = baseResponse.etag();
    String fieldsEtag = fieldsResponse.etag();

    assertNotNull(baseEtag);
    assertNotNull(fieldsEtag);
    assertNotEquals(baseEtag, fieldsEtag, "Different query params should produce different ETags");
  }

  @Test
  void userAEtagDoesNotWorkForUserB_me() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse superuserResponse = probe.get("/me");
    String superuserEtag = superuserResponse.etag();
    assertNotNull(superuserEtag);

    CacheProbeUser.ADMIN.login(loginActions);
    CacheProbe.CacheResponse adminConditional = probe.getIfNoneMatch("/me", superuserEtag);
    assertEquals(200, adminConditional.statusCode(), "User A's ETag should not 304 for User B");
  }

  @Test
  void selfEditInvalidatesMe() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse initial = probe.get("/me");
    CacheAssertions.assertCacheHeaders(initial);

    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheAssertions.assertNotModified(probe.getIfNoneMatch("/me", initial.etag()), initial.etag());

    editOwnFirstName();

    CacheProbe.CacheResponse invalidated =
        CacheAwait.awaitInvalidation(
            probe, CacheProbeUser.SUPERUSER, loginActions, "/me", initial.etag());
    assertNotEquals(
        initial.etag(), invalidated.etag(), "Editing own user should invalidate the /me ETag");
  }

  @Test
  void userRoleMutationInvalidatesMe() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse initial = probe.get("/me");
    CacheAssertions.assertCacheHeaders(initial);

    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheAssertions.assertNotModified(probe.getIfNoneMatch("/me", initial.etag()), initial.etag());

    mutators.mutate(CacheDependency.USER_ROLE);

    CacheProbe.CacheResponse invalidated =
        CacheAwait.awaitInvalidation(
            probe, CacheProbeUser.SUPERUSER, loginActions, "/me", initial.etag());
    assertNotEquals(
        initial.etag(), invalidated.etag(), "A UserRole mutation should invalidate the /me ETag");
  }

  @Test
  void userDataStoreMutationDoesNotInvalidateMe() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse initial = probe.get("/me");
    CacheAssertions.assertCacheHeaders(initial);

    // Advance the async invalidation pipeline with a real dependency mutation first, so any
    // pending processing has flushed before we assert the negative control below.
    mutators.mutate(CacheDependency.USER_ROLE);
    CacheProbe.CacheResponse invalidated =
        CacheAwait.awaitInvalidation(
            probe, CacheProbeUser.SUPERUSER, loginActions, "/me", initial.etag());
    assertNotEquals(initial.etag(), invalidated.etag());

    // A user datastore entry is not part of the /me dependency set, so it must not invalidate.
    mutators.mutate(CacheDependency.USER_DATASTORE_ENTRY);
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheAssertions.assertNotModified(
        probe.getIfNoneMatch("/me", invalidated.etag()), invalidated.etag());
  }

  private void editOwnFirstName() {
    CacheProbeUser.SUPERUSER.login(loginActions);
    UserActions userActions = new UserActions();
    String userId = loginActions.getLoggedInUserId();
    JsonObject user = userActions.get(userId).getBody();
    user.addProperty("firstName", "Cache" + DataGenerator.randomString());
    userActions.update(userId, user).validate().statusCode(200);
  }
}
