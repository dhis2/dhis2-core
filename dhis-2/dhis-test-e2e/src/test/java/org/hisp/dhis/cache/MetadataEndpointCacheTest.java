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

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.test.e2e.actions.SchemasActions;
import org.hisp.dhis.test.e2e.dto.schemas.Schema;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class MetadataEndpointCacheTest extends CacheApiTest {
  private static final Set<String> SKIPPED_SCHEMAS =
      Set.of(
          "eventHooks",
          "routes",
          "oAuth2Authorizations",
          "oAuth2AuthorizationConsents",
          "categoryOptionCombos",
          "jobConfigurations");

  /**
   * Extra handshake re-baselines after the first conditional GET when the ETag has moved. CI has
   * shown a single refresh is insufficient for high fan-in roots (e.g. organisationUnits) when
   * suite fixture churn bumps shared observed deps mid-handshake.
   */
  private static final int MAX_HANDSHAKE_REFRESHES = 3;

  private SchemasActions schemasActions;

  @BeforeAll
  void beforeMetadataCacheTests() {
    schemasActions = new SchemasActions();
    loginActions.loginAsSuperUser();
  }

  @TestFactory
  Stream<DynamicTest> metadataRootCollectionsHonorConditionalEtags() {
    return schemasActions.getMetadataSchemas().stream()
        .filter(schema -> !SKIPPED_SCHEMAS.contains(schema.getPlural()))
        .map(
            schema ->
                dynamicTest(
                    "GET " + schema.getRelativeApiEndpoint(), () -> assertSchemaContract(schema)));
  }

  @Test
  void usersEndpointInvalidatesOnUserRoleChanges() {
    assertDependencyInvalidates("/users?fields=id&page=1&pageSize=1", CacheDependency.USER_ROLE);
  }

  @Test
  void usersEndpointInvalidatesOnUserGroupChanges() {
    assertDependencyInvalidates("/users?fields=id&page=1&pageSize=1", CacheDependency.USER_GROUP);
  }

  @Test
  void organisationUnitsEndpointInvalidatesOnFileResourceChanges() {
    assertDependencyInvalidates(
        "/organisationUnits?fields=id&page=1&pageSize=1", CacheDependency.FILE_RESOURCE);
  }

  @Test
  void mapsEndpointInvalidatesOnAttributeChanges() {
    assertDependencyInvalidates("/maps?fields=id&page=1&pageSize=1", CacheDependency.ATTRIBUTE);
  }

  @Test
  void mapsEndpointInvalidatesOnOrganisationUnitChanges() {
    assertDependencyInvalidates(
        "/maps?fields=id&page=1&pageSize=1", CacheDependency.ORGANISATION_UNIT);
  }

  @Test
  void usersEndpointIgnoresUserDatastoreChanges() {
    String path = "/users?fields=id&page=1&pageSize=1";

    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse initialResponse = probe.get(path);
    CacheAssertions.assertCacheHeaders(initialResponse);

    mutators.mutate(CacheDependency.USER_DATASTORE_ENTRY);

    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheAssertions.assertNotModified(
        probe.getIfNoneMatch(path, initialResponse.etag()), initialResponse.etag());
  }

  private void assertSchemaContract(Schema schema) {
    String path = metadataCollectionPath(schema);

    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse initialResponse = probe.get(path);
    CacheAssertions.assertCacheHeaders(initialResponse);

    CacheProbeUser.SUPERUSER.login(loginActions);
    initialResponse = assertNotModifiedAllowingRefresh(path, initialResponse);

    mutators.mutateMetadataSchema(schema);
    CacheProbe.CacheResponse invalidatedResponse =
        CacheAwait.awaitInvalidation(
            probe, CacheProbeUser.SUPERUSER, loginActions, path, initialResponse.etag());

    assertNotEquals(initialResponse.etag(), invalidatedResponse.etag());
  }

  /**
   * Asserts the conditional GET returns 304 for the current ETag. Between the initial GET and the
   * conditional GET the ETag can legitimately move without any test-driven mutation: the TTL time
   * window can roll over, a background job can write an observed dependency type, or (in this
   * suite) an earlier schema's fixture churn can bump a shared dependency such as {@code
   * FileResource} / {@code UserGroup} while the handshake is in flight. High fan-in endpoints like
   * {@code /organisationUnits} are especially exposed.
   *
   * <p>When a conditional GET returns 200 with a <em>different</em> ETag, the handshake is
   * re-baselined from that response (its ETag is already the server's current value) and retried.
   * Further refreshes are allowed only while the ETag keeps changing, up to {@link
   * #MAX_HANDSHAKE_REFRESHES}. That gate preserves the strict negative control: a broken cache that
   * returns 200 with the <em>same</em> ETag still fails immediately, and a cache that never
   * stabilizes fails after the refresh budget instead of looping forever. Never use this after a
   * negative-control mutation, where a moving ETag would mask an over-invalidation bug.
   */
  private CacheProbe.CacheResponse assertNotModifiedAllowingRefresh(
      String path, CacheProbe.CacheResponse initialResponse) {
    CacheProbe.CacheResponse current = initialResponse;
    for (int refresh = 0; ; refresh++) {
      CacheProbe.CacheResponse conditional = probe.getIfNoneMatch(path, current.etag());
      if (conditional.statusCode() == 304) {
        CacheAssertions.assertNotModified(conditional, current.etag());
        return current;
      }
      boolean etagMoved =
          conditional.statusCode() == 200
              && conditional.etag() != null
              && !conditional.etag().equals(current.etag());
      if (etagMoved && refresh < MAX_HANDSHAKE_REFRESHES) {
        // The 200 body already carries the current ETag; re-baseline without an extra GET that
        // would only widen the race window for another version bump.
        CacheAssertions.assertCacheHeaders(conditional);
        current = conditional;
        continue;
      }
      CacheAssertions.assertNotModified(conditional, current.etag());
      return current;
    }
  }

  private void assertDependencyInvalidates(String path, CacheDependency dependency) {
    CacheProbeUser.SUPERUSER.login(loginActions);
    CacheProbe.CacheResponse initialResponse = probe.get(path);
    CacheAssertions.assertCacheHeaders(initialResponse);

    mutators.mutate(dependency);
    CacheProbe.CacheResponse invalidatedResponse =
        CacheAwait.awaitInvalidation(
            probe, CacheProbeUser.SUPERUSER, loginActions, path, initialResponse.etag());

    assertNotEquals(initialResponse.etag(), invalidatedResponse.etag());
  }

  private String metadataCollectionPath(Schema schema) {
    return schema.getRelativeApiEndpoint() + "?fields=id&page=1&pageSize=1";
  }
}
