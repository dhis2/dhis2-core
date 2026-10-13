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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests creating, updating and exporting indicator types through the API. */
@Transactional
class IndicatorTypeControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String TYPE_UID = "IndTypeUid1";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(
        "{'indicatorTypes':[{'id':'%s','code':'PER_HUNDRED','name':'Per hundred','factor':100}]}"
            .formatted(TYPE_UID));

    JsonObject created = getType();
    assertEquals("PER_HUNDRED", created.getString("code").string());
    assertEquals("Per hundred", created.getString("name").string());
    assertEquals(100, created.getNumber("factor").intValue());
    assertFalse(created.getBoolean("number").booleanValue());

    importMetadata(
        "{'indicatorTypes':[{'id':'%s','code':'PER_THOUSAND','name':'Per thousand','factor':1000,'number':true}]}"
            .formatted(TYPE_UID));

    JsonObject updated = getType();
    assertEquals("PER_THOUSAND", updated.getString("code").string());
    assertEquals("Per thousand", updated.getString("name").string());
    assertEquals(1000, updated.getNumber("factor").intValue());
    assertTrue(updated.getBoolean("number").booleanValue());
  }

  @Test
  void testCreateViaIndicatorTypesEndpoint() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/indicatorTypes", "{'name':'Number','factor':1,'number':true}"));

    JsonObject type = getType(uid);
    assertEquals("Number", type.getString("name").string());
    assertEquals(1, type.getNumber("factor").intValue());
    assertTrue(type.getBoolean("number").booleanValue());
  }

  @Test
  void testPutOfExportedObjectWithChanges() {
    importMetadata(
        "{'indicatorTypes':[{'id':'%s','name':'Per hundred','factor':100}]}".formatted(TYPE_UID));
    String exported = getType().toJson();
    assertTrue(exported.contains("\"factor\":100,"), exported);
    assertTrue(exported.contains("\"number\":false"), exported);
    assertTrue(exported.contains("\"translations\":[]"), exported);
    String changed =
        exported
            .replace("\"factor\":100", "\"factor\":10000")
            .replace("\"number\":false", "\"number\":true")
            .replace(
                "\"translations\":[]",
                "\"translations\":[{\"locale\":\"fr\",\"property\":\"NAME\",\"value\":\"Pour cent\"}]");

    assertStatus(HttpStatus.OK, PUT("/indicatorTypes/" + TYPE_UID, changed));

    JsonObject updated = getType();
    assertEquals("Per hundred", updated.getString("name").string());
    assertEquals(10000, updated.getNumber("factor").intValue());
    assertTrue(updated.getBoolean("number").booleanValue());
    JsonArray translations = updated.getArray("translations");
    assertEquals(1, translations.size());
    assertEquals("Pour cent", translations.getObject(0).getString("value").string());
  }

  @Test
  void testIndicatorReferencesTypeImportedInSamePayload() {
    importMetadata(
        ("{'indicators':[{'id':'IndicatUid1','name':'Indicator','shortName':'Indicator',"
                + "'numerator':'1','denominator':'1','indicatorType':{'id':'%s'}}],"
                + "'indicatorTypes':[{'id':'%s','name':'Per hundred','factor':100}]}")
            .formatted(TYPE_UID, TYPE_UID));

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    JsonObject indicatorType =
        GET("/indicators/IndicatUid1?fields=indicatorType[id,name]")
            .content(HttpStatus.OK)
            .getObject("indicatorType");
    assertEquals(TYPE_UID, indicatorType.getString("id").string());
    assertEquals("Per hundred", indicatorType.getString("name").string());
  }

  /** The keys of an exported type, as on a pre-migration instance. */
  @Test
  void testExportedProperties() {
    importMetadata(
        "{'indicatorTypes':[{'id':'%s','code':'PER_HUNDRED','name':'Per hundred','factor':100}]}"
            .formatted(TYPE_UID));

    JsonObject type = getType();
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "displayName",
            "factor",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "number",
            "sharing",
            "translations"),
        Set.copyOf(type.names()));
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(type.getObject("sharing").names()));
  }

  /** Same property set and persisted properties as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/indicatorType").content(HttpStatus.OK);
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
            "factor",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "number",
            "sharing",
            "translation",
            "user"),
        names);
    assertEquals(
        Set.of(
            "code",
            "created",
            "factor",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "number",
            "translation"),
        persisted);
    assertFalse(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  private JsonObject getType() {
    return getType(TYPE_UID);
  }

  /** Reads the type from the database, bypassing the session and the second-level cache. */
  private JsonObject getType(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/indicatorTypes/" + uid).content(HttpStatus.OK);
  }
}
