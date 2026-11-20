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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/** Tests the metadata import, export and schema of external map layers. */
@Transactional
class ExternalMapLayerMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String LAYER_UID = "ExtMapLayA1";

  private static final String LEGEND_SET_UID = "LegendSetA1";

  @Test
  void shouldCreateAndUpdateExternalMapLayerViaMetadataImport() {
    importMetadata(layer("Layer A", "WMS", "OVERLAY"));

    JsonObject created = getLayer();
    assertEquals("EML_A", created.getString("code").string());
    assertEquals("Layer A", created.getString("name").string());
    assertEquals("WMS", created.getString("mapService").string());
    assertEquals("https://tiles.example.org", created.getString("url").string());
    assertEquals("Example contributors", created.getString("attribution").string());
    assertEquals("roads,rivers", created.getString("layers").string());
    assertEquals("JPG", created.getString("imageFormat").string());
    assertEquals("OVERLAY", created.getString("mapLayerPosition").string());
    assertEquals(LEGEND_SET_UID, created.getObject("legendSet").getString("id").string());
    assertEquals("https://legend.example.org", created.getString("legendSetUrl").string());
    assertEquals("A description", created.getString("description").string());
    assertEquals("https://about.example.org", created.getString("descriptionUrl").string());
    assertEquals("data:image/png;base64,iVBORw0KGgo=", created.getString("image").string());
    assertEquals("rw------", created.getObject("sharing").getString("public").string());
    assertEquals(3, created.getArray("translations").size());

    importMetadata(layer("Layer B", "TMS", "BASEMAP"));

    JsonObject updated = getLayer();
    assertEquals("Layer B", updated.getString("name").string());
    assertEquals("TMS", updated.getString("mapService").string());
    assertEquals("BASEMAP", updated.getString("mapLayerPosition").string());
  }

  @Test
  void shouldUpdateExternalMapLayerWhenExportedLayerWithChangesIsPut() {
    importMetadata(layer("Layer A", "WMS", "OVERLAY"));
    JsonObject changed = getLayer();
    changed = withMember(changed, "name", "\"Layer changed\"");
    changed = withMember(changed, "imageFormat", "\"PNG\"");
    changed = withMember(changed, "mapService", "\"VECTOR_STYLE\"");
    changed = withMember(changed, "layers", "\"lakes\"");

    assertStatus(HttpStatus.OK, PUT("/externalMapLayers/" + LAYER_UID, changed.toJson()));

    JsonObject updated = getLayer();
    assertEquals("Layer changed", updated.getString("name").string());
    assertEquals("PNG", updated.getString("imageFormat").string());
    assertEquals("VECTOR_STYLE", updated.getString("mapService").string());
    assertEquals("lakes", updated.getString("layers").string());
    assertEquals("OVERLAY", updated.getString("mapLayerPosition").string());
    assertEquals("Example contributors", updated.getString("attribution").string());
    assertEquals(LEGEND_SET_UID, updated.getObject("legendSet").getString("id").string());
    assertEquals(3, updated.getArray("translations").size());
  }

  @Test
  void shouldDeleteExternalMapLayer() {
    importMetadata(layer("Layer A", "WMS", "OVERLAY"));

    assertStatus(HttpStatus.OK, DELETE("/externalMapLayers/" + LAYER_UID));

    startNewRequestSession();
    assertEquals(HttpStatus.NOT_FOUND, GET("/externalMapLayers/" + LAYER_UID).status());
  }

  @Test
  void shouldExportExternalMapLayerProperties() {
    importMetadata(layer("Layer A", "WMS", "OVERLAY"));

    JsonObject layer = getLayer();

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "attribution",
            "code",
            "created",
            "createdBy",
            "description",
            "descriptionUrl",
            "displayAttribution",
            "displayDescription",
            "displayName",
            "href",
            "id",
            "image",
            "imageFormat",
            "lastUpdated",
            "lastUpdatedBy",
            "layers",
            "legendSet",
            "legendSetUrl",
            "mapLayerPosition",
            "mapService",
            "name",
            "sharing",
            "translations",
            "url",
            "user"),
        Set.copyOf(layer.names()));
    assertTrue(layer.getString("href").string().endsWith("/externalMapLayers/" + LAYER_UID));
  }

  @Test
  void shouldTranslateDisplayProperties() {
    importMetadata(layer("Layer A", "WMS", "OVERLAY"));
    startNewRequestSession();

    JsonObject layer =
        GET(
                "/externalMapLayers/{id}?locale=fr&fields=displayName,displayAttribution,displayDescription",
                LAYER_UID)
            .content(HttpStatus.OK);

    assertEquals("Couche A", layer.getString("displayName").string());
    assertEquals("Contributeurs", layer.getString("displayAttribution").string());
    assertEquals("Une description", layer.getString("displayDescription").string());
  }

  @Test
  void shouldDescribeExternalMapLayerSchemaPropertyNames() {
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "attribution",
            "code",
            "created",
            "createdBy",
            "description",
            "descriptionUrl",
            "displayAttribution",
            "displayDescription",
            "displayName",
            "href",
            "id",
            "image",
            "imageFormat",
            "lastUpdated",
            "lastUpdatedBy",
            "layers",
            "legendSet",
            "legendSetUrl",
            "mapLayerPosition",
            "mapService",
            "name",
            "sharing",
            "translation",
            "url",
            "user"),
        schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeExternalMapLayerSchemaPersistedAndRequiredProperties() {
    assertEquals(
        Set.of(
            "attribution",
            "code",
            "created",
            "createdBy",
            "description",
            "descriptionUrl",
            "id",
            "image",
            "imageFormat",
            "lastUpdated",
            "lastUpdatedBy",
            "layers",
            "legendSet",
            "legendSetUrl",
            "mapLayerPosition",
            "mapService",
            "name",
            "sharing",
            "translation",
            "url"),
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
    assertEquals(
        Set.of("imageFormat", "mapLayerPosition", "mapService", "name", "url"),
        schemaPropertyNames(property -> property.getBoolean("required").booleanValue()));
  }

  @Test
  void shouldDescribeExternalMapLayerSchemaTypesAndLengths() {
    Map<String, JsonObject> properties = schemaProperties();

    assertEquals(230, properties.get("name").getNumber("length").intValue());
    assertEquals(50, properties.get("code").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("url").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("attribution").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("layers").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("legendSetUrl").getNumber("length").intValue());
    assertEquals(1024, properties.get("description").getNumber("length").intValue());
    assertEquals(255, properties.get("descriptionUrl").getNumber("length").intValue());
    assertEquals(2097152, properties.get("image").getNumber("length").intValue());
    assertEquals("CONSTANT", properties.get("mapService").getString("propertyType").string());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
  }

  @Test
  void shouldDescribeExternalMapLayerSchemaNameAndFlags() {
    JsonObject schema = GET("/schemas/externalMapLayer").content(HttpStatus.OK);

    assertEquals("externalMapLayer", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private Set<String> schemaPropertyNames(Predicate<JsonObject> filter) {
    return schemaProperties().values().stream()
        .filter(filter)
        .map(property -> property.getString("name").string())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private Map<String, JsonObject> schemaProperties() {
    return GET("/schemas/externalMapLayer")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream()
        .collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));
  }

  private static String layer(String name, String mapService, String mapLayerPosition) {
    return """
        {'legendSets':[{'id':'%s','name':'Legend set A'}],
        'externalMapLayers':[{
          'id':'%s','code':'EML_A','name':'%s','mapService':'%s',
          'url':'https://tiles.example.org','attribution':'Example contributors',
          'layers':'roads,rivers','imageFormat':'JPG','mapLayerPosition':'%s',
          'legendSet':{'id':'%s'},'legendSetUrl':'https://legend.example.org',
          'description':'A description','descriptionUrl':'https://about.example.org',
          'image':'data:image/png;base64,iVBORw0KGgo=',
          'translations':[
            {'locale':'fr','property':'NAME','value':'Couche A'},
            {'locale':'fr','property':'ATTRIBUTION','value':'Contributeurs'},
            {'locale':'fr','property':'DESCRIPTION','value':'Une description'}],
          'sharing':{'public':'rw------','external':false}
        }]}
        """
        .formatted(LEGEND_SET_UID, LAYER_UID, name, mapService, mapLayerPosition, LEGEND_SET_UID);
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

  /** Reads the layer from the database in a new session. */
  private JsonObject getLayer() {
    startNewRequestSession();
    return GET("/externalMapLayers/" + LAYER_UID).content(HttpStatus.OK);
  }
}
