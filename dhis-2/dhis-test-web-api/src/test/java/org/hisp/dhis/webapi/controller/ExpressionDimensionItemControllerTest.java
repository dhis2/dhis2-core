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

/**
 * Tests creating, updating, translating and exporting expression dimension items through the API,
 * on their own and as data dimension items of a visualization.
 */
@Transactional
class ExpressionDimensionItemControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String ITEM_UID = "ExpUid00001";

  private static final String VISUALIZATION_UID = "VizUid00001";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void testCreateAndUpdateViaMetadataImport() {
    importMetadata(
        ("{'expressionDimensionItems':[{'id':'%s','code':'EXP_A','name':'Expression A',"
                + "'shortName':'Expr A','formName':'Form A','description':'First',"
                + "'expression':'1+1','missingValueStrategy':'NEVER_SKIP',"
                + "'slidingWindow':true}]}")
            .formatted(ITEM_UID));

    JsonObject created = getItem();
    assertEquals("EXP_A", created.getString("code").string());
    assertEquals("Expression A", created.getString("name").string());
    assertEquals("Expr A", created.getString("shortName").string());
    assertEquals("Form A", created.getString("formName").string());
    assertEquals("First", created.getString("description").string());
    assertEquals("1+1", created.getString("expression").string());
    assertEquals("NEVER_SKIP", created.getString("missingValueStrategy").string());
    assertTrue(created.getBoolean("slidingWindow").booleanValue());

    importMetadata(
        ("{'expressionDimensionItems':[{'id':'%s','code':'EXP_B','name':'Expression B',"
                + "'shortName':'Expr B','formName':'Form B','description':'Second',"
                + "'expression':'1+2','missingValueStrategy':'SKIP_IF_ANY_VALUE_MISSING',"
                + "'slidingWindow':false}]}")
            .formatted(ITEM_UID));

    JsonObject updated = getItem();
    assertEquals("EXP_B", updated.getString("code").string());
    assertEquals("Expression B", updated.getString("name").string());
    assertEquals("Expr B", updated.getString("shortName").string());
    assertEquals("Form B", updated.getString("formName").string());
    assertEquals("Second", updated.getString("description").string());
    assertEquals("1+2", updated.getString("expression").string());
    assertEquals("SKIP_IF_ANY_VALUE_MISSING", updated.getString("missingValueStrategy").string());
    assertFalse(updated.getBoolean("slidingWindow").booleanValue());
  }

  @Test
  void testCreateViaEndpointIsPublicReadOnly() {
    String uid =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/expressionDimensionItems",
                "{'name':'Posted','shortName':'Posted','expression':'2+2'}"));

    JsonObject item = getItem(uid, "");
    assertEquals("Posted", item.getString("name").string());
    assertEquals("SKIP_IF_ALL_VALUES_MISSING", item.getString("missingValueStrategy").string());
    assertFalse(item.getBoolean("slidingWindow").booleanValue());
    assertEquals("r-------", item.getObject("sharing").getString("public").string());
  }

  @Test
  void testPutOfExportedObjectWithChanges() {
    importMetadata(
        ("{'expressionDimensionItems':[{'id':'%s','name':'Expression A','shortName':'Expr A',"
                + "'expression':'1+1'}]}")
            .formatted(ITEM_UID));
    String exported = getItem().toJson();
    assertTrue(exported.contains("\"expression\":\"1+1\""), exported);
    assertTrue(exported.contains("\"translations\":[]"), exported);
    String changed =
        exported
            .replace("\"expression\":\"1+1\"", "\"expression\":\"1+3\"")
            .replace(
                "\"translations\":[]",
                "\"translations\":[{\"locale\":\"fr\",\"property\":\"NAME\",\"value\":\"Expression fr\"}]");

    assertStatus(HttpStatus.OK, PUT("/expressionDimensionItems/" + ITEM_UID, changed));

    JsonObject updated = getItem();
    assertEquals("Expression A", updated.getString("name").string());
    assertEquals("1+3", updated.getString("expression").string());
    JsonArray translations = updated.getArray("translations");
    assertEquals(1, translations.size());
    assertEquals("Expression fr", translations.getObject(0).getString("value").string());
  }

  @Test
  void testDisplayPropertiesUseStoredTranslations() {
    importMetadata(
        ("{'expressionDimensionItems':[{'id':'%s','name':'Expression A','shortName':'Expr A',"
                + "'description':'First','formName':'Form A','expression':'2+2',"
                + "'translations':["
                + "{'locale':'fr','property':'NAME','value':'Expression fr'},"
                + "{'locale':'fr','property':'SHORT_NAME','value':'Expr fr'},"
                + "{'locale':'fr','property':'DESCRIPTION','value':'Premier'},"
                + "{'locale':'fr','property':'FORM_NAME','value':'Formulaire A'}]}]}")
            .formatted(ITEM_UID));

    JsonObject translated = getItem(ITEM_UID, "?translate=true&locale=fr");
    assertEquals("Expression fr", translated.getString("displayName").string());
    assertEquals("Expr fr", translated.getString("displayShortName").string());
    assertEquals("Premier", translated.getString("displayDescription").string());
    assertEquals("Formulaire A", translated.getString("displayFormName").string());

    JsonObject untranslated = getItem();
    assertEquals("Expression A", untranslated.getString("displayName").string());
    assertEquals("Expr A", untranslated.getString("displayShortName").string());
    assertEquals("First", untranslated.getString("displayDescription").string());
    assertEquals("Form A", untranslated.getString("displayFormName").string());
  }

  /**
   * The keys of an exported item. The schema has no API endpoint, so no href is set; legendSets is
   * always served as an empty list.
   */
  @Test
  void testExportedProperties() {
    importMetadata(
        ("{'expressionDimensionItems':[{'id':'%s','name':'Expression A','shortName':'Expr A',"
                + "'expression':'2+2'}]}")
            .formatted(ITEM_UID));

    JsonObject item = getItem();
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "created",
            "createdBy",
            "dimensionItem",
            "dimensionItemType",
            "displayFormName",
            "displayName",
            "displayShortName",
            "expression",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "legendSets",
            "missingValueStrategy",
            "name",
            "sharing",
            "shortName",
            "slidingWindow",
            "translations",
            "user"),
        Set.copyOf(item.names()));
    assertEquals(ITEM_UID, item.getString("dimensionItem").string());
    assertEquals("EXPRESSION_DIMENSION_ITEM", item.getString("dimensionItemType").string());
    assertTrue(item.getArray("legendSets").isEmpty());
  }

  /** Same properties, persisted properties and property attributes as before the migration. */
  @Test
  void testSchemaProperties() {
    JsonObject schema = GET("/schemas/expressionDimensionItem").content(HttpStatus.OK);
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
            "aggregateExportAttributeOptionCombo",
            "aggregateExportCategoryOptionCombo",
            "aggregationType",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "description",
            "dimensionItem",
            "dimensionItemType",
            "displayDescription",
            "displayFormName",
            "displayName",
            "displayShortName",
            "expression",
            "formName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "legendSet",
            "legendSets",
            "missingValueStrategy",
            "name",
            "queryMods",
            "sharing",
            "shortName",
            "slidingWindow",
            "translation",
            "user"),
        names);
    assertEquals(
        Set.of(
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "description",
            "expression",
            "formName",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "missingValueStrategy",
            "name",
            "sharing",
            "shortName",
            "slidingWindow",
            "translation"),
        persisted);
    assertEquals("URL", property(properties, "href").getString("propertyType").string());
    assertEquals(
        "The Unique Identifier for this Object.",
        property(properties, "id").getString("description").string());
    assertEquals(
        Integer.MAX_VALUE, property(properties, "description").getNumber("length").intValue());
    assertEquals(
        Integer.MAX_VALUE, property(properties, "expression").getNumber("length").intValue());
    assertEquals(255, property(properties, "formName").getNumber("length").intValue());
    assertEquals(100, property(properties, "missingValueStrategy").getNumber("length").intValue());
    assertFalse(property(properties, "missingValueStrategy").getBoolean("required").booleanValue());
    assertTrue(property(properties, "dimensionItemType").getBoolean("writable").booleanValue());
    assertTrue(property(properties, "legendSets").getBoolean("writable").booleanValue());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("nameableObject").booleanValue());
    assertTrue(schema.getBoolean("defaultPrivate").booleanValue());
  }

  @Test
  void testVisualizationReferencesItem() {
    importVisualizationWithItem();

    JsonObject visualization =
        getVisualization(
            "?fields=dataDimensionItems[dataDimensionItemType,"
                + "expressionDimensionItem[id,name,shortName,expression]]");
    JsonObject item =
        visualization
            .getArray("dataDimensionItems")
            .getObject(0)
            .getObject("expressionDimensionItem");
    assertEquals(ITEM_UID, item.getString("id").string());
    assertEquals("Expression test", item.getString("name").string());
    assertEquals("Exp test", item.getString("shortName").string());
    assertEquals("1+2", item.getString("expression").string());
  }

  @Test
  void testPatchOfVisualizationKeepsItemReference() {
    importVisualizationWithItem();

    assertStatus(
        HttpStatus.OK,
        PATCH(
            "/visualizations/" + VISUALIZATION_UID,
            "[{'op':'replace','path':'/name','value':'Viz renamed'}]"));

    JsonObject visualization =
        getVisualization("?fields=name,dataDimensionItems[expressionDimensionItem[id]]");
    assertEquals("Viz renamed", visualization.getString("name").string());
    assertEquals(
        ITEM_UID,
        visualization
            .getArray("dataDimensionItems")
            .getObject(0)
            .getObject("expressionDimensionItem")
            .getString("id")
            .string());
  }

  private void importVisualizationWithItem() {
    importMetadata(
        ("{'visualizations':[{'id':'%s','name':'Viz test','type':'PIVOT_TABLE',"
                + "'columnDimensions':['dx'],'rowDimensions':['pe'],'filterDimensions':['ou'],"
                + "'dataDimensionItems':[{'dataDimensionItemType':'EXPRESSION_DIMENSION_ITEM',"
                + "'expressionDimensionItem':{'id':'%s'}}]}],"
                + "'expressionDimensionItems':[{'id':'%s','name':'Expression test',"
                + "'shortName':'Exp test','expression':'1+2'}]}")
            .formatted(VISUALIZATION_UID, ITEM_UID, ITEM_UID));
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

  private JsonObject getItem() {
    return getItem(ITEM_UID, "");
  }

  /** Reads the item from the database, bypassing the session and the second-level cache. */
  private JsonObject getItem(String uid, String query) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/expressionDimensionItems/" + uid + query).content(HttpStatus.OK);
  }

  private JsonObject getVisualization(String query) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/visualizations/" + VISUALIZATION_UID + query).content(HttpStatus.OK);
  }
}
