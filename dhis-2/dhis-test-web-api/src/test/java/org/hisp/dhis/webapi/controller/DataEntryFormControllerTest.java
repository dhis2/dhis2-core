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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests creating, updating and exporting data entry forms through the API. */
@Transactional
class DataEntryFormControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String FORM_UID = "DataEntFrm1";

  private static final String DATA_SET_UID = "DataSetUid1";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(
        ("{'dataEntryForms':[{'id':'%s','code':'FORM_A','name':'Form A','style':'NORMAL',"
                + "'htmlCode':'<p>A</p>','format':1}]}")
            .formatted(FORM_UID));

    JsonObject created = getForm();
    assertEquals("FORM_A", created.getString("code").string());
    assertEquals("Form A", created.getString("name").string());
    assertEquals("NORMAL", created.getString("style").string());
    assertEquals("<p>A</p>", created.getString("htmlCode").string());
    assertEquals(1, created.getNumber("format").intValue());

    importMetadata(
        ("{'dataEntryForms':[{'id':'%s','code':'FORM_B','name':'Form B','style':'COMPACT',"
                + "'htmlCode':'<p>B</p>','format':2}]}")
            .formatted(FORM_UID));

    JsonObject updated = getForm();
    assertEquals("FORM_B", updated.getString("code").string());
    assertEquals("Form B", updated.getString("name").string());
    assertEquals("COMPACT", updated.getString("style").string());
    assertEquals("<p>B</p>", updated.getString("htmlCode").string());
    assertEquals(2, updated.getNumber("format").intValue());
  }

  @Test
  void testCreateViaDataEntryFormsEndpoint() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataEntryForms",
                "{'name':'Posted form','style':'COMFORTABLE','htmlCode':'<p>posted</p>','format':2}"));

    JsonObject form = getForm(uid);
    assertEquals("Posted form", form.getString("name").string());
    assertEquals("COMFORTABLE", form.getString("style").string());
    assertEquals("<p>posted</p>", form.getString("htmlCode").string());
    assertEquals(2, form.getNumber("format").intValue());
  }

  @Test
  void testPutOfExportedFormWithChangedHtmlCode() {
    importMetadata(
        "{'dataEntryForms':[{'id':'%s','name':'Form A','style':'NORMAL','htmlCode':'<p>old</p>'}]}"
            .formatted(FORM_UID));
    String exported = getForm().toJson();
    assertTrue(exported.contains("\"htmlCode\":\"<p>old</p>\""), exported);
    String changed =
        exported
            .replace("\"htmlCode\":\"<p>old</p>\"", "\"htmlCode\":\"<p>new</p>\"")
            .replace("\"style\":\"NORMAL\"", "\"style\":\"NONE\"");

    assertStatus(HttpStatus.OK, PUT("/dataEntryForms/" + FORM_UID, changed));

    JsonObject updated = getForm();
    assertEquals("Form A", updated.getString("name").string());
    assertEquals("<p>new</p>", updated.getString("htmlCode").string());
    assertEquals("NONE", updated.getString("style").string());
  }

  /** The keys of an exported form, as on a pre-migration instance, with href and access set. */
  @Test
  void testExportedProperties() {
    importMetadata(
        ("{'dataEntryForms':[{'id':'%s','code':'FORM_A','name':'Form A','style':'NORMAL',"
                + "'htmlCode':'<p>A</p>','format':2}]}")
            .formatted(FORM_UID));

    JsonObject form = getForm();
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "displayName",
            "format",
            "href",
            "htmlCode",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "style",
            "translations"),
        Set.copyOf(form.names()));
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(form.getObject("sharing").names()));
    assertTrue(
        form.getString("href").string().endsWith("/dataEntryForms/" + FORM_UID),
        form.getString("href").string());
    assertTrue(form.getObject("access").getBoolean("read").booleanValue());
  }

  /** A data set imported with its form in one payload references it with the same nested keys. */
  @Test
  void testDataSetExportOfNestedForm() {
    importMetadata(
        ("{'dataSets':[{'id':'%s','name':'Data set','shortName':'Data set',"
                + "'periodType':'Monthly','dataEntryForm':{'id':'%s'}}],"
                + "'dataEntryForms':[{'id':'%s','name':'Form A','style':'NORMAL',"
                + "'htmlCode':'<p>A</p>','format':2}]}")
            .formatted(DATA_SET_UID, FORM_UID, FORM_UID));

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    JsonObject dataSet = GET("/dataSets/" + DATA_SET_UID).content(HttpStatus.OK);
    assertEquals(Set.of("id"), Set.copyOf(dataSet.getObject("dataEntryForm").names()));
    assertEquals(FORM_UID, dataSet.getObject("dataEntryForm").getString("id").string());

    JsonObject nested =
        GET("/dataSets/" + DATA_SET_UID + "?fields=dataEntryForm[*]")
            .content(HttpStatus.OK)
            .getObject("dataEntryForm");
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "created",
            "displayName",
            "format",
            "href",
            "htmlCode",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "style",
            "translations"),
        Set.copyOf(nested.names()));
    assertEquals("<p>A</p>", nested.getString("htmlCode").string());

    JsonObject metadata = GET("/dataSets/" + DATA_SET_UID + "/metadata").content(HttpStatus.OK);
    assertEquals(
        Set.of(
            "created",
            "format",
            "htmlCode",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "style",
            "translations"),
        Set.copyOf(metadata.getArray("dataEntryForms").getObject(0).names()));
  }

  /** Same properties, persisted columns and column-derived attributes as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/dataEntryForm").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Map<String, String> actual = new HashMap<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      String attributes =
          property.getBoolean("persisted").booleanValue()
              ? "%s required=%s unique=%s length=%s"
                  .formatted(
                      property.getString("propertyType").string(),
                      property.getBoolean("required").booleanValue(),
                      property.getBoolean("unique").booleanValue(),
                      "translation".equals(name) ? "-" : property.getNumber("length").intValue())
              : "transient";
      actual.put(name, attributes);
    }

    assertEquals(
        Map.ofEntries(
            Map.entry("access", "transient"),
            Map.entry("attributeValues", "transient"),
            Map.entry("code", "IDENTIFIER required=false unique=true length=50"),
            Map.entry("created", "DATE required=false unique=false length=255"),
            Map.entry("createdBy", "transient"),
            Map.entry("displayName", "transient"),
            Map.entry("format", "INTEGER required=false unique=false length=2147483647"),
            Map.entry("href", "transient"),
            Map.entry("htmlCode", "TEXT required=false unique=false length=2147483647"),
            Map.entry("id", "IDENTIFIER required=false unique=true length=11"),
            Map.entry("lastUpdated", "DATE required=false unique=false length=255"),
            Map.entry("lastUpdatedBy", "REFERENCE required=false unique=false length=255"),
            Map.entry("name", "TEXT required=true unique=true length=160"),
            Map.entry("sharing", "transient"),
            Map.entry("style", "CONSTANT required=false unique=false length=40"),
            Map.entry("translation", "COLLECTION required=false unique=false length=-"),
            Map.entry("user", "transient")),
        actual);
    assertFalse(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  private JsonObject getForm() {
    return getForm(FORM_UID);
  }

  /** Reads the form from the database, after a flush and with a cleared session and L2 cache. */
  private JsonObject getForm(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/dataEntryForms/" + uid).content(HttpStatus.OK);
  }
}
