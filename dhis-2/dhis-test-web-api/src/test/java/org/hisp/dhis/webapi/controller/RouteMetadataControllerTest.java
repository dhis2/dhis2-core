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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Tests the metadata import, export and schema of routes. */
@Transactional
class RouteMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String ROUTE_UID = "RouteUid001";

  /**
   * The properties of a route except translations, which the schema names "translation" and the
   * JSON names "translations".
   */
  private static final Set<String> PROPERTIES_WITHOUT_TRANSLATIONS =
      Set.of(
          "access",
          "attributeValues",
          "auth",
          "authorities",
          "code",
          "created",
          "createdBy",
          "description",
          "disabled",
          "displayName",
          "headers",
          "href",
          "id",
          "lastUpdated",
          "lastUpdatedBy",
          "name",
          "responseTimeoutSeconds",
          "sharing",
          "url",
          "user");

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Test
  void shouldCreateAndUpdateRouteViaMetadataImport() {
    importMetadata(route("Route A", "https://stub/a/**", 7));

    JsonObject created = getRoute();
    assertEquals("ROUTE_A", created.getString("code").string());
    assertEquals("Route A", created.getString("name").string());
    assertEquals("A description", created.getString("description").string());
    assertTrue(created.getBoolean("disabled").booleanValue());
    assertEquals("https://stub/a/**", created.getString("url").string());
    assertEquals("1", created.getObject("headers").getString("X-Header").string());
    assertEquals("api-token", created.getObject("auth").getString("type").string());
    assertEquals(List.of("F_ROUTE_A", "F_ROUTE_B"), created.getArray("authorities").stringValues());
    assertEquals(7, created.getNumber("responseTimeoutSeconds").intValue());
    assertEquals("rw------", created.getObject("sharing").getString("public").string());
    assertEquals(1, created.getArray("translations").size());

    importMetadata(route("Route B", "https://stub/b", 9));

    JsonObject updated = getRoute();
    assertEquals("Route B", updated.getString("name").string());
    assertEquals("https://stub/b", updated.getString("url").string());
    assertEquals(9, updated.getNumber("responseTimeoutSeconds").intValue());
  }

  @Test
  void shouldUpdateRouteWhenExportedRouteWithChangesIsPut() {
    importMetadata(route("Route A", "https://stub/a/**", 7));
    JsonObject changed = getRoute();
    changed = withMember(changed, "name", "\"Route changed\"");
    changed = withMember(changed, "description", "\"Changed description\"");
    changed = withMember(changed, "disabled", "false");
    changed = withMember(changed, "authorities", "[\"F_ROUTE_C\"]");
    changed = withMember(changed, "responseTimeoutSeconds", "12");

    assertStatus(HttpStatus.OK, PUT("/routes/" + ROUTE_UID, changed.toJson()));

    JsonObject updated = getRoute();
    assertEquals("Route changed", updated.getString("name").string());
    assertEquals("Changed description", updated.getString("description").string());
    assertFalse(updated.getBoolean("disabled").booleanValue());
    assertEquals(List.of("F_ROUTE_C"), updated.getArray("authorities").stringValues());
    assertEquals(12, updated.getNumber("responseTimeoutSeconds").intValue());
    assertEquals("https://stub/a/**", updated.getString("url").string());
    assertEquals("1", updated.getObject("headers").getString("X-Header").string());
  }

  @Test
  void shouldDeleteRoute() {
    importMetadata(route("Route A", "https://stub/a/**", 7));

    assertStatus(HttpStatus.OK, DELETE("/routes/" + ROUTE_UID));

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    assertEquals(HttpStatus.NOT_FOUND, GET("/routes/" + ROUTE_UID).status());
  }

  @Test
  void shouldExportRouteProperties() {
    importMetadata(route("Route A", "https://stub/a/**", 7));

    JsonObject route = getRoute();

    assertEquals(withTranslationKey("translations"), Set.copyOf(route.names()));
    assertTrue(route.getString("href").string().endsWith("/routes/" + ROUTE_UID));
    assertEquals("Route A", route.getString("displayName").string());
  }

  @Test
  void shouldDescribeRouteSchemaPropertyNames() {
    assertEquals(withTranslationKey("translation"), schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeRouteSchemaPersistedAndRequiredProperties() {
    assertEquals(
        Set.of(
            "attributeValues",
            "auth",
            "authorities",
            "code",
            "created",
            "createdBy",
            "description",
            "disabled",
            "headers",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "responseTimeoutSeconds",
            "sharing",
            "translation",
            "url"),
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
    assertEquals(
        Set.of("authorities", "headers", "name"),
        schemaPropertyNames(property -> property.getBoolean("required").booleanValue()));
  }

  @Test
  void shouldDescribeRouteSchemaTypesAndLengths() {
    Map<String, JsonObject> properties =
        GET("/schemas/route")
            .content(HttpStatus.OK)
            .getArray("properties")
            .asList(JsonObject.class)
            .stream()
            .collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));

    assertEquals(230, properties.get("name").getNumber("length").intValue());
    assertEquals(50, properties.get("code").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("description").getNumber("length").intValue());
    assertEquals(Integer.MAX_VALUE, properties.get("url").getNumber("length").intValue());
    assertEquals("TEXT", properties.get("url").getString("propertyType").string());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
    assertEquals("BOOLEAN", properties.get("disabled").getString("propertyType").string());
    assertEquals(
        "INTEGER", properties.get("responseTimeoutSeconds").getString("propertyType").string());
  }

  @Test
  void shouldDescribeRouteSchemaNameAndFlags() {
    JsonObject schema = GET("/schemas/route").content(HttpStatus.OK);

    assertEquals("identifiableObject", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private Set<String> schemaPropertyNames(Predicate<JsonObject> filter) {
    return GET("/schemas/route")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream()
        .filter(filter)
        .map(property -> property.getString("name").string())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static Set<String> withTranslationKey(String translationKey) {
    Set<String> names = new TreeSet<>(PROPERTIES_WITHOUT_TRANSLATIONS);
    names.add(translationKey);
    return names;
  }

  private static String route(String name, String url, int responseTimeoutSeconds) {
    return """
        {'routes':[{
          'id':'%s','code':'ROUTE_A','name':'%s','description':'A description',
          'disabled':true,'url':'%s','headers':{'X-Header':'1'},
          'auth':{'type':'api-token','token':'secret'},
          'authorities':['F_ROUTE_A','F_ROUTE_B'],'responseTimeoutSeconds':%d,
          'translations':[{'locale':'fr','property':'NAME','value':'Route fr'}],
          'sharing':{'public':'rw------','external':false}
        }]}
        """
        .formatted(ROUTE_UID, name, url, responseTimeoutSeconds);
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

  /** Reads the route from the database, bypassing the session and the L2 cache. */
  private JsonObject getRoute() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/routes/" + ROUTE_UID).content(HttpStatus.OK);
  }
}
