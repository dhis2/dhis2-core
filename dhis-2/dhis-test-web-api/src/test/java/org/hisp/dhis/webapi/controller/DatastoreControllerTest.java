/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hisp.dhis.datastore.MetadataDatastoreService.METADATA_STORE_NS;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.web.WebClientUtils.assertSeries;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.hisp.dhis.datastore.DatastoreEntry;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection;
import org.hisp.dhis.datastore.DatastoreNamespaceProtection.ProtectionType;
import org.hisp.dhis.datastore.DatastoreService;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.HttpStatus.Series;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonDatastoreValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests the {@link DatastoreController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
class DatastoreControllerTest extends DhisControllerConvenienceTest {

  /**
   * Only used directly to setup namespace protection as this is by intention not possible using the
   * REST API.
   */
  @Autowired private DatastoreService service;

  @Test
  void testGetNamespaces() {
    // out of the box (as superuser)
    assertSeries(Series.SUCCESSFUL, POST("/dataStore/METADATASTORE/key", "{}"));
    assertEquals(singletonList(METADATA_STORE_NS), GET("/dataStore").content().stringValues());
    // after we created an entry in foo namespace
    assertStatus(HttpStatus.CREATED, POST("/dataStore/colors/blue", "{'answer': 42}"));
    assertEquals(asList(METADATA_STORE_NS, "colors"), GET("/dataStore").content().stringValues());
  }

  @Test
  void testGetNamespaces_HiddenNamespaceNotVisible() {
    // does not have special authorities
    switchToNewUser("anonymous");
    assertEquals(emptyList(), GET("/dataStore").content().stringValues());
  }

  @Test
  void testGetNamespaces_RestrictedNamespaceIsVisible() {
    setUpNamespaceProtection("fruits", ProtectionType.RESTRICTED, "fruits_ns_authority");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/fruits/apple", "{'answer': 42}"));
    // does not have special authorities
    switchToNewUser("anonymous");
    assertEquals(singletonList("fruits"), GET("/dataStore").content().stringValues());
  }

  @Test
  void testGetKeysInNamespace() {
    assertSeries(Series.SUCCESSFUL, POST("/dataStore/METADATASTORE/key", "{}"));
    assertEquals(
        singletonList("key"),
        GET("/dataStore/{namespace}", METADATA_STORE_NS).content().stringValues());
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{'answer': 42}"));
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/dog", "{'answer': true}"));
    assertContainsOnly(List.of("cat", "dog"), GET("/dataStore/pets").content().stringValues());
  }

  @Test
  void testGetKeysInNamespace_MustExist() {
    assertEquals(
        "Namespace not found: 'missing'",
        GET("/dataStore/missing").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void testGetKeysInNamespace_LastUpdatedFilter() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{'answer': 42}"));
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/dog", "{'answer': true}"));
    assertTrue(
        GET("/dataStore/pets?lastUpdated=" + (LocalDate.now().getYear() + 1))
            .content()
            .stringValues()
            .isEmpty());
  }

  @Test
  void testGetKeysInNamespace_ProtectedNamespaceWhenRestricted() {
    setUpNamespaceProtection("pets", ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{'answer': 42}"));
    // as superuser:
    assertEquals(singletonList("cat"), GET("/dataStore/pets").content().stringValues());
    // as a user that is a pets admin
    switchToNewUser("some-user", "pets-admin");
    assertEquals(singletonList("cat"), GET("/dataStore/pets").content().stringValues());
    // as a user that lacks authority
    switchToNewUser("anonymous");
    assertEquals(
        "Namespace 'pets' is protected, access denied",
        GET("/dataStore/pets").error(HttpStatus.FORBIDDEN).getMessage());
  }

  @Test
  void testGetKeysInNamespace_ProtectedNamespaceWhenHidden() {
    setUpNamespaceProtection("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{'answer': 42}"));
    // as superuser:
    assertEquals(singletonList("cat"), GET("/dataStore/pets").content().stringValues());
    // as a user that is a pets admin
    switchToNewUser("some-user", "pets-admin");
    assertEquals(singletonList("cat"), GET("/dataStore/pets").content().stringValues());
    // as a user that lacks authority
    switchToNewUser("anonymous");
    assertEquals(
        "Namespace not found: 'pets'",
        GET("/dataStore/pets").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void testDeleteNamespace() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    assertStatus(HttpStatus.OK, DELETE("/dataStore/pets"));
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/pets"));
  }

  @Test
  void testDeleteNamespace_MustExist() {
    assertStatus(HttpStatus.NOT_FOUND, DELETE("/dataStore/missing"));
  }

  @Test
  void testDeleteNamespace_ProtectedNamespaceWhenRestricted() {
    setUpNamespaceProtection("pets", ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    // user that lacks authority
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.FORBIDDEN, DELETE("/dataStore/pets"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, DELETE("/dataStore/pets"));
  }

  @Test
  void testDeleteNamespace_ProtectedNamespaceWhenHidden() {
    setUpNamespaceProtection("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    // user that lacks authority
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.NOT_FOUND, DELETE("/dataStore/pets"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, DELETE("/dataStore/pets"));
  }

  @Test
  void testDeleteNamespace_ProtectedNamespaceWithSharing() {
    setUpNamespaceProtectionWithSharing("pets", ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    String uid = GET("/dataStore/pets/cat/metaData").content().as(JsonDatastoreValue.class).getId();
    assertStatus(
        HttpStatus.OK,
        POST("/sharing?type=dataStore&id=" + uid, "{'object':{'publicAccess':'--------'}}"));
    switchToNewUser("someone", "pets-admin");
    assertEquals(
        "Access denied for key 'cat' in namespace 'pets'",
        DELETE("/dataStore/pets").error(HttpStatus.FORBIDDEN).getMessage());
    switchToSuperuser();
    assertStatus(HttpStatus.OK, DELETE("/dataStore/pets"));
  }

  @Test
  void testGetKeyJsonValue() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "'dog'"));
    assertEquals("dog", GET("/dataStore/pets/cat").content().string());
  }

  @Test
  void testGetKeyJsonValue_ComplexValue() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{'x':[1,2,3]}"));
    assertEquals(
        asList(1, 2, 3), GET("/dataStore/pets/cat").content().getArray("x").numberValues());
  }

  @Test
  void testGetKeyJsonValue_MustExist() {
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/pets/cat"));
  }

  @Test
  void testGetKeyJsonValue_ProtectedNamespaceWhenHidden() {
    setUpNamespaceProtection("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/pets/cat"));
    switchToNewUser("someone", "pets-admin");
    assertTrue(GET("/dataStore/pets/cat").content().isObject());
  }

  @Test
  void testGetKeyJsonValue_ProtectedNamespaceWhenRestricted() {
    setUpNamespaceProtection("pets", ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.FORBIDDEN, GET("/dataStore/pets/cat"));
    switchToNewUser("someone", "pets-admin");
    assertTrue(GET("/dataStore/pets/cat").content().isObject());
  }

  @Test
  void testGetKeyJsonValue_ProtectedNamespaceWithSharing() {
    setUpNamespaceProtectionWithSharing("pets", ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    String uid = GET("/dataStore/pets/cat/metaData").content().as(JsonDatastoreValue.class).getId();
    assertStatus(
        HttpStatus.OK,
        POST("/sharing?type=dataStore&id=" + uid, "{'object':{'publicAccess':'--------'}}"));
    switchToNewUser("someone", "pets-admin");
    assertEquals(
        "Access denied for key 'cat' in namespace 'pets'",
        GET("/dataStore/pets/cat").error(HttpStatus.FORBIDDEN).getMessage());
    switchToSuperuser();
    assertStatus(HttpStatus.OK, GET("/dataStore/pets/cat"));
  }

  @Test
  void testGetKeyJsonValueMetaData() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    JsonDatastoreValue metaData =
        GET("/dataStore/pets/cat/metaData").content().as(JsonDatastoreValue.class);
    assertEquals("pets", metaData.getNamespace());
    assertEquals("cat", metaData.getKey());
    assertTrue(metaData.getValue().isUndefined(), "metadata should not contain the value");
  }

  @Test
  void testGetKeyJsonValueMetaData_MustExist() {
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/pets/missing/metaData"));
  }

  @Test
  void testGetKeyJsonValueMetaData_ProtectedNamespaceWhenRestricted() {
    setUpNamespaceProtection("pets", ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.FORBIDDEN, GET("/dataStore/pets/cat/metaData"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, GET("/dataStore/pets/cat/metaData"));
  }

  @Test
  void testGetKeyJsonValueMetaData_ProtectedNamespaceWhenHidden() {
    setUpNamespaceProtection("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/pets/cat/metaData"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, GET("/dataStore/pets/cat/metaData"));
  }

  @Test
  void testGetKeyJsonValueMetaData_ProtectedNamespaceWithSharing() {
    setUpNamespaceProtectionWithSharing("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    String uid = GET("/dataStore/pets/cat/metaData").content().as(JsonDatastoreValue.class).getId();
    assertStatus(
        HttpStatus.OK,
        POST("/sharing?type=dataStore&id=" + uid, "{'object':{'publicAccess':'--------'}}"));
    switchToNewUser("someone", "pets-admin");
    assertEquals(
        "Access denied for key 'cat' in namespace 'pets'",
        GET("/dataStore/pets/cat/metaData").error(HttpStatus.FORBIDDEN).getMessage());
    switchToSuperuser();
    assertStatus(HttpStatus.OK, GET("/dataStore/pets/cat/metaData"));
  }

  @Test
  void testAddKeyJsonValue() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
  }

  @Test
  void testAddKeyJsonValue_Encrypt() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat?encrypt=true", "{}"));
    // there is no way to see in the exposed metadata that a value is
    // encrypted, user service
    DatastoreEntry entry = service.getEntry("pets", "cat");
    assertTrue(entry.getEncrypted());
    assertNull(entry.getJbPlainValue());
    assertNotNull(entry.getEncryptedValue());
  }

  @Test
  void testAddKeyJsonValue_AlreadyExists() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    assertEquals(
        "Key 'cat' already exists in namespace 'pets'",
        POST("/dataStore/pets/cat", "{}").error(HttpStatus.CONFLICT).getMessage());
  }

  @Test
  void testAddKeyJsonValue_MustBeJson() {
    assertEquals(
        "Invalid JSON value for key 'cat'",
        POST("/dataStore/pets/cat", "/not JSON/").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void testAddKeyJsonValue_ProtectedNamespaceWhenRestricted() {
    setUpNamespaceProtection("pets", ProtectionType.RESTRICTED, "pets-admin");
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.FORBIDDEN, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
  }

  @Test
  void testAddKeyJsonValue_ProtectedNamespaceWhenHidden() {
    setUpNamespaceProtection("pets", ProtectionType.HIDDEN, "pets-admin");
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    // but:
    assertStatus(HttpStatus.NOT_FOUND, GET("/dataStore/pets/cat"));
  }

  @Test
  void testUpdateKeyJsonValue() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    assertStatus(HttpStatus.OK, PUT("/dataStore/pets/cat", "[1,2,3]"));
    assertEquals(asList(1, 2, 3), GET("/dataStore/pets/cat").content().numberValues());
  }

  @Test
  void testUpdateKeyJsonValue_MustExist() {
    assertEquals(
        "Key 'cat' not found in namespace 'pets'",
        PUT("/dataStore/pets/cat", "[]").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void testUpdateKeyJsonValue_MustBeJson() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    assertEquals(
        "Invalid JSON value for key 'cat'",
        PUT("/dataStore/pets/cat", "+not JSON+").error(HttpStatus.BAD_REQUEST).getMessage());
  }

  @Test
  void testUpdateKeyJsonValue_ProtectedNamespaceWhenRestricted() {
    setUpNamespaceProtection("pets", ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.FORBIDDEN, PUT("/dataStore/pets/cat", "[]"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, PUT("/dataStore/pets/cat", "[]"));
  }

  @Test
  void testUpdateKeyJsonValue_ProtectedNamespaceWhenHidden() {
    setUpNamespaceProtection("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertStatus(HttpStatus.NOT_FOUND, PUT("/dataStore/pets/cat", "[]"));
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, PUT("/dataStore/pets/cat", "[]"));
  }

  @Test
  void testUpdateKeyJsonValue_ProtectedNamespaceWithSharing() {
    setUpNamespaceProtectionWithSharing("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    String uid = GET("/dataStore/pets/cat/metaData").content().as(JsonDatastoreValue.class).getId();
    assertStatus(
        HttpStatus.OK,
        POST("/sharing?type=dataStore&id=" + uid, "{'object':{'publicAccess':'r-------'}}"));
    switchToNewUser("someone", "pets-admin");
    assertEquals(
        "Access denied for key 'cat' in namespace 'pets'",
        PUT("/dataStore/pets/cat", "[]").error(HttpStatus.FORBIDDEN).getMessage());
    switchToSuperuser();
    assertStatus(HttpStatus.OK, PUT("/dataStore/pets/cat", "[]"));
  }

  @Test
  void testDeleteKeyJsonValue() {
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    assertStatus(HttpStatus.OK, DELETE("/dataStore/pets/cat"));
  }

  @Test
  void testDeleteKeyJsonValue_MustExist() {
    assertEquals(
        "Key 'cat' not found in namespace 'pets'",
        DELETE("/dataStore/pets/cat").error(HttpStatus.NOT_FOUND).getMessage());
  }

  @Test
  void testDeleteKeyJsonValue_ProtectedNamespaceWhenRestricted() {
    setUpNamespaceProtection("pets", ProtectionType.RESTRICTED, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertEquals(
        "Namespace 'pets' is protected, access denied",
        DELETE("/dataStore/pets/cat").error(HttpStatus.FORBIDDEN).getMessage());
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, DELETE("/dataStore/pets/cat"));
  }

  @Test
  void testDeleteKeyJsonValue_ProtectedNamespaceWhenHidden() {
    setUpNamespaceProtection("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    switchToNewUser("anonymous");
    assertEquals(
        "Key 'cat' not found in namespace 'pets'",
        DELETE("/dataStore/pets/cat").error(HttpStatus.NOT_FOUND).getMessage());
    switchToNewUser("someone", "pets-admin");
    assertStatus(HttpStatus.OK, DELETE("/dataStore/pets/cat"));
  }

  @Test
  void testDeleteKeyJsonValue_ProtectedNamespaceWithSharing() {
    setUpNamespaceProtectionWithSharing("pets", ProtectionType.HIDDEN, "pets-admin");
    assertStatus(HttpStatus.CREATED, POST("/dataStore/pets/cat", "{}"));
    String uid = GET("/dataStore/pets/cat/metaData").content().as(JsonDatastoreValue.class).getId();
    assertStatus(
        HttpStatus.OK,
        POST("/sharing?type=dataStore&id=" + uid, "{'object':{'publicAccess':'r-------'}}"));
    // a user with required authority cannot delete (ACL fails)
    switchToNewUser("someone", "pets-admin");
    assertEquals(
        "Access denied for key 'cat' in namespace 'pets'",
        DELETE("/dataStore/pets/cat").error(HttpStatus.FORBIDDEN).getMessage());
    // but the owner still can
    switchToSuperuser();
    assertStatus(HttpStatus.OK, DELETE("/dataStore/pets/cat"));
  }

  private void setUpNamespaceProtection(
      String namespace, ProtectionType readWrite, String... authorities) {
    service.addProtection(
        new DatastoreNamespaceProtection(namespace, readWrite, false, authorities));
  }

  private void setUpNamespaceProtectionWithSharing(
      String namespace, ProtectionType readWrite, String... authorities) {
    service.addProtection(
        new DatastoreNamespaceProtection(namespace, readWrite, true, authorities));
  }
}
