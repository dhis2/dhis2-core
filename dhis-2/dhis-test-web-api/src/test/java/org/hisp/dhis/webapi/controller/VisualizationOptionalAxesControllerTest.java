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
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the metadata import, merge and Jackson path of the optional axes ({@code Axis}) embedded in
 * visualizations.
 */
@Transactional
class VisualizationOptionalAxesControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String VISUALIZATION_UID = "VizAxesUid1";

  @Test
  void testImportPersistsOptionalAxesInOrder() {
    importMetadata(
        visualization(
            "{'dimensionalItem':'deUidAaaaa','axis':1}",
            "{'dimensionalItem':'deUidBbbbb','axis':0}"));

    assertEquals(List.of("deUidAaaaa:1", "deUidBbbbb:0"), optionalAxes());
  }

  @Test
  void testReimportReplacesOptionalAxes() {
    importMetadata(
        visualization(
            "{'dimensionalItem':'deUidAaaaa','axis':1}",
            "{'dimensionalItem':'deUidBbbbb','axis':0}"));

    importMetadata(visualization("{'dimensionalItem':'deUidCcccc','axis':1}"));

    assertEquals(List.of("deUidCcccc:1"), optionalAxes());
  }

  @Test
  void testPutOfExportedVisualizationRoundTrips() {
    importMetadata(
        visualization(
            "{'dimensionalItem':'deUidAaaaa','axis':1}",
            "{'dimensionalItem':'deUidBbbbb','axis':0}"));
    String exported = GET("/visualizations/" + VISUALIZATION_UID).content(HttpStatus.OK).toJson();

    assertStatus(HttpStatus.OK, PUT("/visualizations/" + VISUALIZATION_UID, exported));

    assertEquals(List.of("deUidAaaaa:1", "deUidBbbbb:0"), optionalAxes());
  }

  /** The optionalAxes property of visualization still refers to the persisted, embedded Axis. */
  @Test
  void testVisualizationOptionalAxesSchemaProperty() {
    JsonObject property = property(GET("/schemas/visualization").content(HttpStatus.OK), "axis");

    assertEquals("optionalAxes", text(property, "fieldName"));
    assertEquals("org.hisp.dhis.visualization.Axis", text(property, "itemKlass"));
    assertEquals("COMPLEX", text(property, "itemPropertyType"));
    assertTrue(property.getBoolean("persisted").booleanValue());
    assertTrue(property.getBoolean("embeddedObject").booleanValue());
  }

  /** The axis schema (an embedded, non-metadata object) is unchanged by the migration. */
  @Test
  void testAxisSchema() {
    JsonObject schema = GET("/schemas/axis").content(HttpStatus.OK);
    assertTrue(schema.getBoolean("persisted").booleanValue());
    assertTrue(schema.getBoolean("embeddedObject").booleanValue());
    assertFalse(schema.getBoolean("metadata").booleanValue());

    JsonObject dimensionalItem = property(schema, "dimensionalItem");
    assertEquals("TEXT", text(dimensionalItem, "propertyType"));
    assertTrue(dimensionalItem.getBoolean("persisted").booleanValue());
    assertTrue(dimensionalItem.getBoolean("required").booleanValue());

    JsonObject axis = property(schema, "axis");
    assertEquals("INTEGER", text(axis, "propertyType"));
    assertTrue(axis.getBoolean("persisted").booleanValue());
    assertTrue(axis.getBoolean("required").booleanValue());
  }

  private static String visualization(String... axes) {
    return ("{'visualizations':[{'id':'%s','name':'Axes visualization','type':'COLUMN',"
            + "'optionalAxes':[%s]}]}")
        .formatted(VISUALIZATION_UID, String.join(",", axes));
  }

  /** The visualization's optional axes as {@code dimensionalItem:axis}, in list order. */
  private List<String> optionalAxes() {
    JsonArray axes =
        GET("/visualizations/" + VISUALIZATION_UID + "?fields=optionalAxes[dimensionalItem,axis]")
            .content(HttpStatus.OK)
            .getArray("optionalAxes");
    List<String> result = new ArrayList<>();
    for (int i = 0; i < axes.size(); i++) {
      JsonObject axis = axes.getObject(i);
      result.add(text(axis, "dimensionalItem") + ":" + axis.getNumber("axis").intValue());
    }
    return result;
  }

  private static JsonObject property(JsonObject schema, String name) {
    JsonArray properties = schema.getArray("properties");
    for (int i = 0; i < properties.size(); i++) {
      if (name.equals(text(properties.getObject(i), "name"))) {
        return properties.getObject(i);
      }
    }
    return fail("no schema property " + name);
  }

  private static String text(JsonObject object, String key) {
    return object.getString(key).string();
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }
}
