/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonMetadataVersion;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MetadataVersionControllerTest extends PostgresControllerIntegrationTestBase {

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  @DisplayName("A valid metadata version should be returned from the versions endpoint")
  void getMetadataVersionsTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonObject response = GET("/metadata/versions").content().as(JsonObject.class);
    JsonMetadataVersion metadataVersion =
        response.getArray("metadataversions").get(0, JsonMetadataVersion.class);

    // then
    assertEquals("Version_1", metadataVersion.getName());
    assertTrue(CodeGenerator.isValidUid(metadataVersion.getId()), "id is valid UID");
    assertTrue(DateUtils.dateIsValid(metadataVersion.getCreated()), "date is valid date");
    assertEquals("ATOMIC", metadataVersion.getType());
    assertNotNull(metadataVersion.getHashCode());
  }

  @Test
  @DisplayName("A valid current metadata version should be returned from the version endpoint")
  void getCurrentMetadataVersionTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonMetadataVersion metadataVersion =
        GET("/metadata/version").content().as(JsonMetadataVersion.class);

    // then
    assertEquals("Version_1", metadataVersion.getName());
    assertTrue(CodeGenerator.isValidUid(metadataVersion.getId()), "id is valid UID");
    assertTrue(DateUtils.dateIsValid(metadataVersion.getCreated()), "date is valid date");
    assertEquals("ATOMIC", metadataVersion.getType());
    assertNotNull(metadataVersion.getHashCode());
  }

  @Test
  @DisplayName("A valid metadata version by name should be returned from the version endpoint")
  void getMetadataVersionByNameTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonMetadataVersion metadataVersion =
        GET("/metadata/version?versionName=Version_2").content().as(JsonMetadataVersion.class);

    // then
    assertEquals("Version_2", metadataVersion.getName());
    assertTrue(CodeGenerator.isValidUid(metadataVersion.getId()), "id is valid UID");
    assertTrue(DateUtils.dateIsValid(metadataVersion.getCreated()), "date is valid date");
    assertEquals("ATOMIC", metadataVersion.getType());
    assertNotNull(metadataVersion.getHashCode());
  }

  @Test
  @DisplayName(
      "Valid metadata version history should be returned from the version history endpoint")
  void getCurrentMetadataVersionHistoryTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonObject response = GET("/metadata/version/history").content().as(JsonObject.class);
    JsonMetadataVersion metadataVersion =
        response.getArray("metadataversions").get(0, JsonMetadataVersion.class);

    // then
    assertEquals("Version_1", metadataVersion.getName());
    assertTrue(CodeGenerator.isValidUid(metadataVersion.getId()), "id is valid UID");
    assertTrue(DateUtils.dateIsValid(metadataVersion.getCreated()), "date is valid date");
    assertEquals("ATOMIC", metadataVersion.getType());
    assertNotNull(metadataVersion.getHashCode());
  }

  @Test
  @DisplayName(
      "Valid metadata version history by name should be returned from the version history endpoint")
  void getCurrentMetadataVersionHistoryByNameTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonArray versions =
        GET("/metadata/version/history?versionName=Version_2")
            .content()
            .as(JsonObject.class)
            .getArray("metadataversions");

    // then
    assertEquals(2, versions.size());
  }

  @Test
  @DisplayName("Valid metadata version data should be returned from the version data endpoint")
  void getCurrentMetadataVersionDataTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    JsonObject systemData =
        GET("/metadata/version/Version_2/data").content().as(JsonObject.class).getObject("system");

    // then
    assertNotNull(systemData.getString("id"));
    assertTrue(DateUtils.dateIsValid(systemData.getString("date").string()), "date is valid date");
  }

  @Test
  @DisplayName("Valid metadata version data (zipped) call should be successful")
  void getCurrentMetadataVersionDataZippedTest() {
    // given
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    POST("/metadata/version/create?type=ATOMIC").success();

    // when
    HttpResponse response = GET("/metadata/version/Version_2/data.gz");

    // then
    assertTrue(response.success(), "response is successful");
    assertEquals("application/gzip", response.header("Content-Type"));
  }

  @Test
  @DisplayName("Missing version on data endpoint returns an error, not a 2xx empty body")
  void getMissingMetadataVersionDataReturnsError() {
    POST("/systemSettings/keyVersionEnabled?value=true").success();

    HttpResponse response = GET("/metadata/version/DoesNotExist/data");

    assertFalse(response.success(), "response should not be 2xx for a missing snapshot");
    assertTrue(
        response.error().getMessage().contains("DoesNotExist"),
        "error message should reference the missing version name");
  }

  @Test
  @DisplayName(
      "Missing version on data.gz endpoint returns an error, not a 2xx empty gzip (GZIPOutputStream commits the response on construction)")
  void getMissingMetadataVersionDataGzReturnsError() {
    POST("/systemSettings/keyVersionEnabled?value=true").success();

    HttpResponse response = GET("/metadata/version/DoesNotExist/data.gz");

    assertFalse(response.success(), "response should not be 2xx for a missing snapshot");
    assertTrue(
        response.error().getMessage().contains("DoesNotExist"),
        "error message should reference the missing version name");
  }

  @Test
  @DisplayName(
      "The version endpoint returns the keys of a persisted version as on a pre-migration instance")
  void getPersistedMetadataVersionKeysTest() {
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    String uid =
        POST("/metadata/version/create?type=BEST_EFFORT").content().getString("id").string();
    startNewRequestSessionWithoutSecondLevelCache();

    JsonObject version = GET("/metadata/version?versionName=Version_1").content();

    // play/dev has the same keys; its versions have no lastUpdatedBy
    assertEquals(
        Set.of(
            "attributeValues",
            "created",
            "displayName",
            "hashCode",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "translations",
            "type"),
        Set.copyOf(version.names()));
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(version.getObject("sharing").names()));
    assertEquals(uid, version.getString("id").string());
    assertEquals("Version_1", version.getString("displayName").string());
    assertEquals("BEST_EFFORT", version.getString("type").string());
    assertTrue(version.getArray("translations").isEmpty());
    assertTrue(version.getArray("attributeValues").isEmpty());
  }

  @Test
  @DisplayName(
      "The create endpoint returns the creator, owner and public access set on save as before")
  void createMetadataVersionKeysTest() {
    POST("/systemSettings/keyVersionEnabled?value=true").success();

    JsonObject version = POST("/metadata/version/create?type=ATOMIC").content();

    assertEquals(
        Set.of(
            "attributeValues",
            "created",
            "createdBy",
            "displayName",
            "hashCode",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "translations",
            "type",
            "user"),
        Set.copyOf(version.names()));
    JsonObject sharing = version.getObject("sharing");
    assertEquals(Set.of("owner", "public", "users", "userGroups"), Set.copyOf(sharing.names()));
    assertEquals(getAdminUid(), sharing.getString("owner").string());
    assertEquals("--------", sharing.getString("public").string());
    assertEquals(getAdminUid(), version.getObject("createdBy").getString("id").string());
    assertEquals(getAdminUid(), version.getObject("user").getString("id").string());
  }

  @Test
  @DisplayName("The versions endpoint returns the keys of a persisted version as on play/dev")
  void getPersistedMetadataVersionsKeysTest() {
    POST("/systemSettings/keyVersionEnabled?value=true").success();
    POST("/metadata/version/create?type=ATOMIC").success();
    startNewRequestSessionWithoutSecondLevelCache();

    JsonObject version =
        GET("/metadata/versions").content().getArray("metadataversions").getObject(0);

    assertEquals(Set.of("created", "hashCode", "id", "name", "type"), Set.copyOf(version.names()));
  }

  @Test
  @DisplayName("The schema has the same properties and persisted properties as on play/dev")
  void getMetadataVersionSchemaPropertiesTest() {
    JsonObject schema = GET("/schemas/metadataVersion").content();
    JsonArray properties = schema.getArray("properties");
    Set<String> names = new TreeSet<>();
    Set<String> persisted = new TreeSet<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      names.add(name);
      if (property.getBoolean("persisted").booleanValue()) {
        persisted.add(name);
      }
    }

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "displayName",
            "hashCode",
            "href",
            "id",
            "importDate",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "translation",
            "type",
            "user"),
        names);
    assertEquals(
        Set.of(
            "code",
            "created",
            "hashCode",
            "id",
            "importDate",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "type"),
        persisted);
    assertFalse(schema.getBoolean("metadata").booleanValue());
    assertFalse(schema.getBoolean("shareable").booleanValue());
  }

  /** Flushes and clears the session and the second-level cache, as between two requests. */
  private void startNewRequestSessionWithoutSecondLevelCache() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
  }
}
