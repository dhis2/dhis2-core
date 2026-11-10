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

/** Tests creating, updating, translating and exporting constants through the API. */
@Transactional
class ConstantControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String CONSTANT_UID = "ConstantUi1";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(
        ("{'constants':[{'id':'%s','code':'PI','name':'Pi','shortName':'Pi short',"
                + "'description':'Circle ratio','value':3.14}]}")
            .formatted(CONSTANT_UID));

    JsonObject created = getConstant();
    assertEquals("PI", created.getString("code").string());
    assertEquals("Pi", created.getString("name").string());
    assertEquals("Pi short", created.getString("shortName").string());
    assertEquals("Circle ratio", created.getString("description").string());
    assertEquals(3.14, created.getNumber("value").doubleValue());

    importMetadata(
        ("{'constants':[{'id':'%s','code':'TAU','name':'Tau','shortName':'Tau short',"
                + "'description':'Two pi','value':6.28}]}")
            .formatted(CONSTANT_UID));

    JsonObject updated = getConstant();
    assertEquals("TAU", updated.getString("code").string());
    assertEquals("Tau", updated.getString("name").string());
    assertEquals("Tau short", updated.getString("shortName").string());
    assertEquals("Two pi", updated.getString("description").string());
    assertEquals(6.28, updated.getNumber("value").doubleValue());
  }

  @Test
  void testCreateViaConstantsEndpoint() {
    String uid =
        assertStatus(
            HttpStatus.CREATED, POST("/constants", "{'name':'Ten','shortName':'Ten','value':10}"));

    JsonObject constant = getConstant(uid);
    assertEquals("Ten", constant.getString("name").string());
    assertEquals(10, constant.getNumber("value").doubleValue());
  }

  @Test
  void testPutOfExportedObjectWithChanges() {
    importMetadata(
        "{'constants':[{'id':'%s','name':'Pi','shortName':'Pi','value':3.14}]}"
            .formatted(CONSTANT_UID));
    String exported = getConstant().toJson();
    assertTrue(exported.contains("\"value\":3.14"), exported);
    assertTrue(exported.contains("\"translations\":[]"), exported);
    String changed =
        exported
            .replace("\"value\":3.14", "\"value\":2.72")
            .replace(
                "\"translations\":[]",
                "\"translations\":[{\"locale\":\"fr\",\"property\":\"NAME\",\"value\":\"Pi fr\"}]");

    assertStatus(HttpStatus.OK, PUT("/constants/" + CONSTANT_UID, changed));

    JsonObject updated = getConstant();
    assertEquals("Pi", updated.getString("name").string());
    assertEquals(2.72, updated.getNumber("value").doubleValue());
    JsonArray translations = updated.getArray("translations");
    assertEquals(1, translations.size());
    assertEquals("Pi fr", translations.getObject(0).getString("value").string());
  }

  @Test
  void testDisplayPropertiesUseStoredTranslations() {
    importMetadata(
        ("{'constants':[{'id':'%s','name':'Pi','shortName':'Pi short','description':'Ratio',"
                + "'value':3.14,'translations':["
                + "{'locale':'fr','property':'NAME','value':'Pi fr'},"
                + "{'locale':'fr','property':'SHORT_NAME','value':'Pi court'},"
                + "{'locale':'fr','property':'DESCRIPTION','value':'Rapport'},"
                + "{'locale':'fr','property':'FORM_NAME','value':'Pi formulaire'}]}]}")
            .formatted(CONSTANT_UID));

    JsonObject translated = getConstant(CONSTANT_UID, "?translate=true&locale=fr");
    assertEquals("Pi fr", translated.getString("displayName").string());
    assertEquals("Pi court", translated.getString("displayShortName").string());
    assertEquals("Rapport", translated.getString("displayDescription").string());
    assertEquals("Pi formulaire", translated.getString("displayFormName").string());

    JsonObject untranslated = getConstant();
    assertEquals("Pi", untranslated.getString("displayName").string());
    assertEquals("Pi short", untranslated.getString("displayShortName").string());
    assertEquals("Ratio", untranslated.getString("displayDescription").string());
    assertEquals("Pi", untranslated.getString("displayFormName").string());
  }

  /** The keys of an exported constant, as on a pre-migration instance, with href and access set. */
  @Test
  void testExportedProperties() {
    importMetadata(
        "{'constants':[{'id':'%s','name':'Pi','shortName':'Pi','value':3.14}]}"
            .formatted(CONSTANT_UID));

    JsonObject constant = getConstant();
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "created",
            "createdBy",
            "displayFormName",
            "displayName",
            "displayShortName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "shortName",
            "translations",
            "user",
            "value"),
        Set.copyOf(constant.names()));
    assertTrue(
        constant.getString("href").string().endsWith("/constants/" + CONSTANT_UID),
        constant.getString("href").string());
    assertTrue(constant.getObject("access").getBoolean("read").booleanValue());
  }

  /** Same properties, persisted properties and property types as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/constant").content(HttpStatus.OK);
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
            "description",
            "displayDescription",
            "displayFormName",
            "displayName",
            "displayShortName",
            "formName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "shortName",
            "translation",
            "user",
            "value"),
        names);
    assertEquals(
        Set.of(
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "description",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "shortName",
            "translation",
            "value"),
        persisted);
    assertEquals("URL", property(properties, "href").getString("propertyType").string());
    assertEquals(
        "The Unique Identifier for this Object.",
        property(properties, "id").getString("description").string());
    assertEquals(
        Integer.MAX_VALUE, property(properties, "description").getNumber("length").intValue());
    assertTrue(property(properties, "value").getBoolean("required").booleanValue());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("nameableObject").booleanValue());
    assertFalse(schema.getBoolean("dataShareable").booleanValue());
  }

  private static JsonObject property(JsonArray properties, String name) {
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      if (name.equals(property.getString("name").string())) {
        return property;
      }
    }
    throw new AssertionError("No schema property " + name);
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  private JsonObject getConstant() {
    return getConstant(CONSTANT_UID);
  }

  private JsonObject getConstant(String uid) {
    return getConstant(uid, "");
  }

  /** Reads the constant from the database, bypassing the session and the second-level cache. */
  private JsonObject getConstant(String uid, String query) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/constants/" + uid + query).content(HttpStatus.OK);
  }
}
