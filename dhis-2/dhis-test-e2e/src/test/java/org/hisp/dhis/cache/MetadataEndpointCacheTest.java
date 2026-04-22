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
    CacheAssertions.assertNotModified(
        probe.getIfNoneMatch(path, initialResponse.etag()), initialResponse.etag());

    mutators.mutateMetadataSchema(schema);
    CacheProbe.CacheResponse invalidatedResponse =
        CacheAwait.awaitInvalidation(
            probe, CacheProbeUser.SUPERUSER, loginActions, path, initialResponse.etag());

    assertNotEquals(initialResponse.etag(), invalidatedResponse.etag());
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
