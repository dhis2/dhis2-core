/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static java.util.stream.Collectors.toSet;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.schema.PropertyType;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonProperty;
import org.hisp.dhis.test.webapi.json.domain.JsonSchema;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link SchemaController} using (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class SchemaControllerTest extends H2ControllerIntegrationTestBase {
  @Test
  void testValidateSchema() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        null,
        POST(
                "/schemas/organisationUnit",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}")
            .content(HttpStatus.OK));
  }

  @Test
  void testValidateSchema_NoSuchType() {
    assertWebMessage(
        "Not Found",
        404,
        "ERROR",
        "Type xyz does not exist.",
        POST("/schemas/xyz", "{}").content(HttpStatus.NOT_FOUND));
  }

  @Test
  void testFieldFilteringNameKlass() {
    var schema =
        GET("/schemas/organisationUnit?fields=name,klass")
            .content(HttpStatus.OK)
            .as(JsonSchema.class);
    assertNotNull(schema.getKlass());
    assertNotNull(schema.getName());
    assertNull(schema.getSingular());
    assertNull(schema.getPlural());
    assertFalse(schema.get("properties").exists());
  }

  @Test
  void testFieldFilteringDefaultPropertiesExpansion() {
    var schema =
        GET("/schemas/organisationUnit?fields=name,klass,properties")
            .content(HttpStatus.OK)
            .as(JsonSchema.class);
    assertNotNull(schema.getKlass());
    assertNotNull(schema.getName());
    assertNull(schema.getSingular());
    assertNull(schema.getPlural());
    assertTrue(schema.get("properties").exists());
    assertFalse(schema.getProperties().isEmpty());
    assertNotNull(schema.getProperties().get(0).getName());
    assertNotNull(schema.getProperties().get(0).getKlass());
    assertNotNull(schema.getProperties().get(0).getFieldName());
  }

  @Test
  void testFieldFilteringAllSchemas() {
    var schemas =
        GET("/schemas?fields=name,klass")
            .content(HttpStatus.OK)
            .as(JsonObject.class)
            .getList("schemas", JsonSchema.class);
    for (JsonSchema schema : schemas) {
      assertNotNull(schema.getKlass());
      assertNotNull(schema.getName());
      assertNull(schema.getSingular());
      assertNull(schema.getPlural());
    }
  }

  @Test
  void testAttributeWritable() {
    JsonSchema schema = GET("/schemas/attribute").content().as(JsonSchema.class);
    schema
        .getProperties()
        .forEach(
            p -> {
              if (p.getName().endsWith("Attribute")
                  && p.getPropertyType() == PropertyType.BOOLEAN) {
                assertTrue(p.isWritable());
                assertTrue(p.isPersisted());
              }
            });
  }

  @Test
  void testUserNameIsPersistedButReadOnly() {
    JsonSchema user = GET("/schemas/user").content().as(JsonSchema.class);
    Optional<JsonProperty> maybeName =
        user.getProperties().stream().filter(p -> "name".equals(p.getName())).findFirst();
    assertTrue(maybeName.isPresent());
    JsonProperty name = maybeName.get();
    assertTrue(name.isPersisted());
    assertTrue(name.isReadable());
    assertFalse(name.isWritable());
    assertFalse(name.isRequired());
  }

  @Test
  void testSortableProperties() {
    JsonSchema de = GET("/schemas/dataElement").content().as(JsonSchema.class);
    JsonList<JsonProperty> properties = de.getProperties();
    Set<String> expected =
        Set.of(
            "fieldMask",
            "aggregationType",
            "code",
            "domainType",
            "displayName",
            "created",
            "description",
            "zeroIsSignificant",
            "displayFormName",
            "displayShortName",
            "url",
            "lastUpdated",
            "valueType",
            "formName",
            "name",
            "id",
            "shortName");
    Set<String> actual =
        properties.stream()
            .filter(JsonProperty::isSortable)
            .map(JsonProperty::getName)
            .collect(toSet());
    assertEquals(expected, actual);
  }
}
