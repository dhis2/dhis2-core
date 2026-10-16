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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonErrorReport;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests creating, updating, exporting and executing SQL views through the API. */
@Transactional
class SqlViewMetadataControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String VIEW_UID = "SqlViewUi01";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(
        ("{'sqlViews':[{'id':'%s','code':'SV_A','name':'View A','description':'First',"
                + "'sqlQuery':'select uid from dataelement','type':'VIEW',"
                + "'cacheStrategy':'NO_CACHE'}]}")
            .formatted(VIEW_UID));

    JsonObject created = getSqlView();
    assertEquals("SV_A", created.getString("code").string());
    assertEquals("View A", created.getString("name").string());
    assertEquals("First", created.getString("description").string());
    assertEquals("select uid from dataelement", created.getString("sqlQuery").string());
    assertEquals("VIEW", created.getString("type").string());
    assertEquals("NO_CACHE", created.getString("cacheStrategy").string());

    importMetadata(
        ("{'sqlViews':[{'id':'%s','code':'SV_B','name':'View B','description':'Second',"
                + "'sqlQuery':'select uid from indicator','type':'QUERY',"
                + "'cacheStrategy':'CACHE_1_HOUR'}]}")
            .formatted(VIEW_UID));

    JsonObject updated = getSqlView();
    assertEquals("SV_B", updated.getString("code").string());
    assertEquals("View B", updated.getString("name").string());
    assertEquals("Second", updated.getString("description").string());
    assertEquals("select uid from indicator", updated.getString("sqlQuery").string());
    assertEquals("QUERY", updated.getString("type").string());
    assertEquals("CACHE_1_HOUR", updated.getString("cacheStrategy").string());
  }

  @Test
  void testCreateViaSqlViewsEndpointDefaultsTypeAndCacheStrategy() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST("/sqlViews", "{'name':'Defaults','sqlQuery':'select uid from dataelement'}"));

    JsonObject sqlView = getSqlView(uid);
    assertEquals("Defaults", sqlView.getString("name").string());
    assertEquals("VIEW", sqlView.getString("type").string());
    assertEquals("RESPECT_SYSTEM_SETTING", sqlView.getString("cacheStrategy").string());
  }

  @Test
  void testPutOfExportedObjectWithChanges() throws JsonProcessingException {
    importMetadata(
        ("{'sqlViews':[{'id':'%s','name':'View A','sqlQuery':'select uid from dataelement',"
                + "'type':'VIEW','cacheStrategy':'NO_CACHE'}]}")
            .formatted(VIEW_UID));
    ObjectNode exported = (ObjectNode) new ObjectMapper().readTree(getSqlView().toJson());
    assertEquals("select uid from dataelement", exported.get("sqlQuery").asText());
    assertEquals("NO_CACHE", exported.get("cacheStrategy").asText());
    exported.put("sqlQuery", "select uid from indicator");
    exported.put("description", "Indicators");
    exported.put("type", "MATERIALIZED_VIEW");
    exported.put("cacheStrategy", "CACHE_6AM_TOMORROW");

    assertStatus(HttpStatus.OK, PUT("/sqlViews/" + VIEW_UID, exported.toString()));

    JsonObject updated = getSqlView();
    assertEquals("View A", updated.getString("name").string());
    assertEquals("select uid from indicator", updated.getString("sqlQuery").string());
    assertEquals("Indicators", updated.getString("description").string());
    assertEquals("MATERIALIZED_VIEW", updated.getString("type").string());
    assertEquals("CACHE_6AM_TOMORROW", updated.getString("cacheStrategy").string());
  }

  @Test
  void testImportWithTranslationsIsRejectedAsNotTranslatable() {
    JsonErrorReport error =
        POST(
                "/metadata",
                ("{'sqlViews':[{'id':'%s','name':'View A','sqlQuery':'select 1',"
                        + "'translations':[{'locale':'fr','property':'NAME','value':'Vue'}]}]}")
                    .formatted(VIEW_UID))
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class)
            .findErrorReport(ErrorCode.E1107);
    assertNotNull(error);
  }

  @Test
  void testDataOfImportedQueryView() {
    importMetadata(
        ("{'sqlViews':[{'id':'%s','name':'View A','type':'QUERY',"
                + "'sqlQuery':'select name, type from sqlview'}]}")
            .formatted(VIEW_UID));
    startNewRequestSession();

    JsonObject data = GET("/sqlViews/" + VIEW_UID + "/data").content(HttpStatus.OK);
    JsonArray rows = data.getObject("listGrid").getArray("rows");
    assertEquals(1, rows.size());
    assertEquals("View A", rows.getArray(0).getString(0).string());
    assertEquals("QUERY", rows.getArray(0).getString(1).string());
  }

  /** The keys of an exported view, with href and access set by the web layer. */
  @Test
  void testExportedProperties() {
    importMetadata(
        ("{'sqlViews':[{'id':'%s','code':'SV_A','name':'View A','description':'First',"
                + "'sqlQuery':'select 1'}]}")
            .formatted(VIEW_UID));

    JsonObject sqlView = getSqlView();
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "cacheStrategy",
            "code",
            "created",
            "createdBy",
            "description",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "sqlQuery",
            "translations",
            "type",
            "user"),
        Set.copyOf(sqlView.names()));
    assertTrue(
        sqlView.getString("href").string().endsWith("/sqlViews/" + VIEW_UID),
        sqlView.getString("href").string());
    assertTrue(sqlView.getObject("access").getBoolean("read").booleanValue());
  }

  /** Same properties, persisted properties and property types as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/sqlView").content(HttpStatus.OK);
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
            "cacheStrategy",
            "code",
            "created",
            "createdBy",
            "description",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "sqlQuery",
            "translation",
            "type",
            "updateJobId",
            "user"),
        names);
    assertEquals(
        Set.of(
            "attributeValues",
            "cacheStrategy",
            "code",
            "created",
            "createdBy",
            "description",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "sharing",
            "sqlQuery",
            "type"),
        persisted);
    assertEquals("URL", property(properties, "href").getString("propertyType").string());
    assertEquals(
        "The Unique Identifier for this Object.",
        property(properties, "id").getString("description").string());
    assertEquals(
        Integer.MAX_VALUE, property(properties, "description").getNumber("length").intValue());
    assertEquals(
        Integer.MAX_VALUE, property(properties, "sqlQuery").getNumber("length").intValue());
    assertEquals(230, property(properties, "name").getNumber("length").intValue());
    assertTrue(property(properties, "name").getBoolean("unique").booleanValue());
    assertTrue(property(properties, "sqlQuery").getBoolean("required").booleanValue());
    assertTrue(property(properties, "type").getBoolean("required").booleanValue());
    assertTrue(property(properties, "cacheStrategy").getBoolean("required").booleanValue());
    assertEquals("sqlView", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("dataShareable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
    assertFalse(schema.getBoolean("translatable").booleanValue());
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

  private JsonObject getSqlView() {
    return getSqlView(VIEW_UID);
  }

  /** Reads the view from the database, bypassing the session and the second-level cache. */
  private JsonObject getSqlView(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/sqlViews/" + uid).content(HttpStatus.OK);
  }
}
