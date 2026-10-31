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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests the metadata import, export and schema of attributes. */
@Transactional
class AttributeMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String ATTRIBUTE_UID = "AttrUid0001";

  private static final String OPTION_SET_UID = "OptSetUid01";

  /** The schema property names of an attribute, as served by play/dev before the migration. */
  private static final Set<String> SCHEMA_PROPERTIES =
      Set.of(
          "access",
          "attributeValues",
          "categoryAttribute",
          "categoryOptionAttribute",
          "categoryOptionComboAttribute",
          "categoryOptionGroupAttribute",
          "categoryOptionGroupSetAttribute",
          "code",
          "constantAttribute",
          "created",
          "createdBy",
          "dataElementAttribute",
          "dataElementGroupAttribute",
          "dataElementGroupSetAttribute",
          "dataSetAttribute",
          "description",
          "displayDescription",
          "displayFormName",
          "displayName",
          "displayShortName",
          "documentAttribute",
          "eventChartAttribute",
          "eventReportAttribute",
          "formName",
          "href",
          "id",
          "indicatorAttribute",
          "indicatorGroupAttribute",
          "lastUpdated",
          "lastUpdatedBy",
          "legendSetAttribute",
          "mandatory",
          "mapAttribute",
          "name",
          "objectTypes",
          "optionAttribute",
          "optionSet",
          "optionSetAttribute",
          "organisationUnitAttribute",
          "organisationUnitGroupAttribute",
          "organisationUnitGroupSetAttribute",
          "programAttribute",
          "programIndicatorAttribute",
          "programStageAttribute",
          "relationshipTypeAttribute",
          "sectionAttribute",
          "sharing",
          "shortName",
          "sortOrder",
          "sqlViewAttribute",
          "trackedEntityAttributeAttribute",
          "trackedEntityTypeAttribute",
          "translation",
          "unique",
          "user",
          "userAttribute",
          "userGroupAttribute",
          "validationRuleAttribute",
          "validationRuleGroupAttribute",
          "valueType",
          "visualizationAttribute");

  @Autowired private EntityManagerFactory entityManagerFactory;

  /**
   * The option set is imported on its own: attributes have a lower schema order than option sets,
   * so an attribute imported together with its option set would lose the reference.
   */
  @BeforeEach
  void importOptionSet() {
    importMetadata(
        "{'optionSets':[{'id':'%s','name':'Option set A','valueType':'INTEGER'}]}"
            .formatted(OPTION_SET_UID));
  }

  @Test
  void shouldCreateAndUpdateAttributeViaMetadataImport() {
    importMetadata(
        attribute("Attribute A", true, "'dataElementAttribute':true,'userAttribute':true"));

    JsonObject created = getAttribute();
    assertEquals("ATTR_A", created.getString("code").string());
    assertEquals("Attribute A", created.getString("name").string());
    assertEquals("Attr A", created.getString("shortName").string());
    assertEquals("A description", created.getString("description").string());
    assertEquals("INTEGER", created.getString("valueType").string());
    assertTrue(created.getBoolean("mandatory").booleanValue());
    assertTrue(created.getBoolean("unique").booleanValue());
    assertEquals(4, created.getNumber("sortOrder").intValue());
    assertEquals(OPTION_SET_UID, created.getObject("optionSet").getString("id").string());
    assertEquals(
        Set.of("DATA_ELEMENT", "USER"), Set.copyOf(created.getArray("objectTypes").stringValues()));
    assertTrue(created.getBoolean("dataElementAttribute").booleanValue());
    assertTrue(created.getBoolean("userAttribute").booleanValue());
    assertFalse(created.getBoolean("indicatorAttribute").booleanValue());
    assertEquals("rw------", created.getObject("sharing").getString("public").string());
    assertEquals(1, created.getArray("translations").size());

    importMetadata(attribute("Attribute B", false, "'indicatorAttribute':true"));

    JsonObject updated = getAttribute();
    assertEquals("Attribute B", updated.getString("name").string());
    assertFalse(updated.getBoolean("mandatory").booleanValue());
    assertEquals(List.of("INDICATOR"), updated.getArray("objectTypes").stringValues());
  }

  @Test
  void shouldUpdateAttributeWhenExportedAttributeWithChangesIsPut() {
    importMetadata(attribute("Attribute A", true, "'dataElementAttribute':true"));
    JsonObject changed = getAttribute();
    changed = withMember(changed, "name", "\"Attribute changed\"");
    changed = withMember(changed, "mandatory", "false");
    changed = withMember(changed, "dataElementAttribute", "false");
    changed = withMember(changed, "programAttribute", "true");

    assertStatus(HttpStatus.OK, PUT("/attributes/" + ATTRIBUTE_UID, changed.toJson()));

    JsonObject updated = getAttribute();
    assertEquals("Attribute changed", updated.getString("name").string());
    assertFalse(updated.getBoolean("mandatory").booleanValue());
    assertEquals(List.of("PROGRAM"), updated.getArray("objectTypes").stringValues());
    assertEquals("Attr A", updated.getString("shortName").string());
    assertEquals(OPTION_SET_UID, updated.getObject("optionSet").getString("id").string());
  }

  @Test
  void shouldIgnoreAttributeValuesAndFormNameOfAnAttribute() {
    importMetadata(
        attribute(
            "Attribute A",
            true,
            "'formName':'Form A','attributeValues':[{'attribute':{'id':'AttrUid0002'},'value':'x'}]"));

    JsonObject attribute = getAttribute();
    assertTrue(attribute.getArray("attributeValues").isEmpty());
    assertFalse(attribute.has("formName"));
    assertEquals("Attribute A", attribute.getString("displayFormName").string());
  }

  @Test
  void shouldDeleteAttribute() {
    importMetadata(attribute("Attribute A", true, "'dataElementAttribute':true"));

    assertStatus(HttpStatus.OK, DELETE("/attributes/" + ATTRIBUTE_UID));

    startNewRequestSessionWithoutCache();
    assertEquals(HttpStatus.NOT_FOUND, GET("/attributes/" + ATTRIBUTE_UID).status());
  }

  @Test
  void shouldExportAttributeProperties() {
    importMetadata(attribute("Attribute A", true, "'dataElementAttribute':true"));

    JsonObject attribute = getAttribute();

    Set<String> names = new TreeSet<>(SCHEMA_PROPERTIES);
    names.removeAll(Set.of("formName", "translation"));
    names.add("translations");
    assertEquals(names, Set.copyOf(attribute.names()));
    assertTrue(attribute.getString("href").string().endsWith("/attributes/" + ATTRIBUTE_UID));
    assertEquals("Attribute A", attribute.getString("displayName").string());
  }

  @Test
  void shouldDescribeAttributeSchemaPropertyNames() {
    assertEquals(SCHEMA_PROPERTIES, schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeAttributeSchemaPersistedRequiredAndReadOnlyProperties() {
    Set<String> notPersisted =
        Set.of(
            "access",
            "attributeValues",
            "displayDescription",
            "displayFormName",
            "displayName",
            "displayShortName",
            "formName",
            "href",
            "user");
    Set<String> persisted = new TreeSet<>(SCHEMA_PROPERTIES);
    persisted.removeAll(notPersisted);

    assertEquals(
        persisted,
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
    assertEquals(
        Set.of("mandatory", "name", "unique", "valueType"),
        schemaPropertyNames(property -> property.getBoolean("required").booleanValue()));
    assertEquals(
        Set.of(
            "displayDescription",
            "displayFormName",
            "displayName",
            "displayShortName",
            "objectTypes"),
        schemaPropertyNames(property -> !property.getBoolean("writable").booleanValue()));
  }

  @Test
  void shouldDescribeAttributeSchemaTypesAndLengths() {
    Map<String, JsonObject> properties = schemaProperties();

    assertEquals(230, properties.get("name").getNumber("length").intValue());
    assertEquals(50, properties.get("code").getNumber("length").intValue());
    assertEquals(50, properties.get("shortName").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("description").getNumber("length").intValue());
    assertEquals(50, properties.get("valueType").getNumber("length").intValue());
    assertEquals("CONSTANT", properties.get("valueType").getString("propertyType").string());
    assertEquals("COLLECTION", properties.get("objectTypes").getString("propertyType").string());
    assertEquals("REFERENCE", properties.get("optionSet").getString("propertyType").string());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
    assertEquals(
        "BOOLEAN", properties.get("dataElementAttribute").getString("propertyType").string());
  }

  @Test
  void shouldDescribeAttributeSchemaNameAndFlags() {
    JsonObject schema = GET("/schemas/attribute").content(HttpStatus.OK);

    assertEquals("attribute", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
    assertEquals(100, schema.getNumber("order").intValue());
  }

  private Map<String, JsonObject> schemaProperties() {
    return GET("/schemas/attribute")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream()
        .collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));
  }

  private Set<String> schemaPropertyNames(Predicate<JsonObject> filter) {
    return schemaProperties().values().stream()
        .filter(filter)
        .map(property -> property.getString("name").string())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static String attribute(String name, boolean mandatory, String extraMembers) {
    return """
        {'attributes':[{
          'id':'%s','code':'ATTR_A','name':'%s','shortName':'Attr A',
          'description':'A description','valueType':'INTEGER','mandatory':%s,'unique':true,
          'sortOrder':4,'optionSet':{'id':'%s'},%s,
          'translations':[{'locale':'fr','property':'NAME','value':'Attribut fr'}],
          'sharing':{'public':'rw------','external':false}
        }]}
        """
        .formatted(ATTRIBUTE_UID, name, mandatory, OPTION_SET_UID, extraMembers);
  }

  /** Returns a copy of the object with the value of the given member replaced. */
  private static JsonObject withMember(JsonObject object, String member, String json) {
    return JsonMixed.of(object.get(member).node().replaceWith(json)).asObject();
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
    assertEquals("OK", response.content().getString("status").string());
  }

  /** Reads the attribute from the database, bypassing the session and the L2 cache. */
  private JsonObject getAttribute() {
    startNewRequestSessionWithoutCache();
    return GET("/attributes/" + ATTRIBUTE_UID).content(HttpStatus.OK);
  }

  private void startNewRequestSessionWithoutCache() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
  }
}
