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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Tests the metadata import, export and schema of relationship types. */
@Transactional
class RelationshipTypeMetadataControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String TYPE_UID = "RelTypeUid1";

  private static final String TRACKED_ENTITY_TYPE_UID = "TetUid00001";

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void shouldCreateAndUpdateRelationshipTypeViaMetadataImport() {
    importMetadata(relationshipType("Type A", "Child of", true));

    JsonObject created = getRelationshipType();
    assertEquals("RT_A", created.getString("code").string());
    assertEquals("Type A", created.getString("name").string());
    assertEquals("A description", created.getString("description").string());
    assertTrue(created.getBoolean("bidirectional").booleanValue());
    assertTrue(created.getBoolean("referral").booleanValue());
    assertEquals("Parent of", created.getString("fromToName").string());
    assertEquals("Child of", created.getString("toFromName").string());
    assertEquals(
        "TRACKED_ENTITY_INSTANCE",
        created.getObject("fromConstraint").getString("relationshipEntity").string());
    assertEquals(
        TRACKED_ENTITY_TYPE_UID,
        created
            .getObject("fromConstraint")
            .getObject("trackedEntityType")
            .getString("id")
            .string());
    assertEquals(
        TRACKED_ENTITY_TYPE_UID,
        created.getObject("toConstraint").getObject("trackedEntityType").getString("id").string());
    assertEquals("rw------", created.getObject("sharing").getString("public").string());
    assertEquals(3, created.getArray("translations").size());

    importMetadata(relationshipType("Type B", "Other child of", false));

    JsonObject updated = getRelationshipType();
    assertEquals("Type B", updated.getString("name").string());
    assertEquals("Other child of", updated.getString("toFromName").string());
    assertFalse(updated.getBoolean("referral").booleanValue());
    assertEquals(2, countConstraintRows(), "the replaced constraints are deleted");
  }

  @Test
  void shouldUpdateRelationshipTypeWhenExportedTypeWithChangesIsPut() {
    importMetadata(relationshipType("Type A", "Child of", true));
    JsonObject changed = getRelationshipType();
    changed = withMember(changed, "name", "\"Type changed\"");
    changed = withMember(changed, "description", "\"Changed description\"");
    changed = withMember(changed, "bidirectional", "false");
    changed = withMember(changed, "fromToName", "\"Changed from to\"");

    assertStatus(HttpStatus.OK, PUT("/relationshipTypes/" + TYPE_UID, changed.toJson()));

    JsonObject updated = getRelationshipType();
    assertEquals("Type changed", updated.getString("name").string());
    assertEquals("Changed description", updated.getString("description").string());
    assertFalse(updated.getBoolean("bidirectional").booleanValue());
    assertEquals("Changed from to", updated.getString("fromToName").string());
    assertEquals("Child of", updated.getString("toFromName").string());
    assertTrue(updated.getBoolean("referral").booleanValue());
    assertEquals(
        TRACKED_ENTITY_TYPE_UID,
        updated
            .getObject("fromConstraint")
            .getObject("trackedEntityType")
            .getString("id")
            .string());
  }

  @Test
  void shouldDeleteRelationshipTypeAndItsConstraints() {
    importMetadata(relationshipType("Type A", "Child of", true));

    assertStatus(HttpStatus.OK, DELETE("/relationshipTypes/" + TYPE_UID));

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    assertEquals(HttpStatus.NOT_FOUND, GET("/relationshipTypes/" + TYPE_UID).status());
    assertEquals(0, countConstraintRows());
  }

  @Test
  void shouldExportRelationshipTypeProperties() {
    importMetadata(relationshipType("Type A", "Child of", true));

    JsonObject type = getRelationshipType();

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "bidirectional",
            "code",
            "created",
            "createdBy",
            "description",
            "displayFromToName",
            "displayName",
            "displayToFromName",
            "fromConstraint",
            "fromToName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "referral",
            "sharing",
            "toConstraint",
            "toFromName",
            "translations",
            "user"),
        Set.copyOf(type.names()));
    assertTrue(type.getString("href").string().endsWith("/relationshipTypes/" + TYPE_UID));
  }

  @Test
  void shouldTranslateDisplayNames() {
    importMetadata(relationshipType("Type A", "Child of", true));
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();

    JsonObject type =
        GET(
                "/relationshipTypes/{id}?locale=fr&fields=displayName,displayFromToName,displayToFromName",
                TYPE_UID)
            .content(HttpStatus.OK);

    assertEquals("Type fr", type.getString("displayName").string());
    assertEquals("Parent de", type.getString("displayFromToName").string());
    assertEquals("Enfant de", type.getString("displayToFromName").string());
  }

  @Test
  void shouldDescribeRelationshipTypeSchemaPropertyNames() {
    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "bidirectional",
            "code",
            "created",
            "createdBy",
            "description",
            "displayFromToName",
            "displayName",
            "displayToFromName",
            "fromConstraint",
            "fromToName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "referral",
            "sharing",
            "toConstraint",
            "toFromName",
            "translation",
            "user"),
        schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeRelationshipTypeSchemaPersistedAndRequiredProperties() {
    assertEquals(
        Set.of(
            "attributeValues",
            "bidirectional",
            "code",
            "created",
            "createdBy",
            "description",
            "fromConstraint",
            "fromToName",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "referral",
            "sharing",
            "toConstraint",
            "toFromName",
            "translation"),
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
    assertEquals(
        Set.of("bidirectional", "fromConstraint", "fromToName", "name", "referral", "toConstraint"),
        schemaPropertyNames(property -> property.getBoolean("required").booleanValue()));
  }

  @Test
  void shouldDescribeRelationshipTypeSchemaTypesAndLengths() {
    Map<String, JsonObject> properties = schemaProperties();

    assertEquals(230, properties.get("name").getNumber("length").intValue());
    assertEquals(50, properties.get("code").getNumber("length").intValue());
    assertEquals(255, properties.get("description").getNumber("length").intValue());
    assertEquals(255, properties.get("fromToName").getNumber("length").intValue());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
    assertEquals("COMPLEX", properties.get("fromConstraint").getString("propertyType").string());
    assertEquals("BOOLEAN", properties.get("referral").getString("propertyType").string());
  }

  @Test
  void shouldDescribeConstraintsAsOwnedUniqueManyToOneWithOrphanDelete() {
    Map<String, JsonObject> properties = schemaProperties();

    for (String name : new String[] {"fromConstraint", "toConstraint"}) {
      JsonObject constraint = properties.get(name);
      assertTrue(constraint.getBoolean("manyToOne").booleanValue(), name);
      assertFalse(constraint.getBoolean("oneToOne").booleanValue(), name);
      assertTrue(constraint.getBoolean("unique").booleanValue(), name);
      assertTrue(constraint.getBoolean("owner").booleanValue(), name);
      assertTrue(constraint.getBoolean("embeddedObject").booleanValue(), name);
      assertEquals("all,delete,delete-orphan", constraint.getString("cascade").string(), name);
    }
  }

  @Test
  void shouldDescribeRelationshipTypeSchemaNameAndFlags() {
    JsonObject schema = GET("/schemas/relationshipType").content(HttpStatus.OK);

    assertEquals("relationshipType", schema.getString("name").string());
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
    return GET("/schemas/relationshipType")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream()
        .collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));
  }

  private static String relationshipType(String name, String toFromName, boolean referral) {
    return """
        {'trackedEntityTypes':[{'id':'%s','name':'Person','shortName':'Person'}],
        'relationshipTypes':[{
          'id':'%s','code':'RT_A','name':'%s','description':'A description',
          'bidirectional':true,'referral':%s,'fromToName':'Parent of','toFromName':'%s',
          'fromConstraint':{'relationshipEntity':'TRACKED_ENTITY_INSTANCE',
            'trackedEntityType':{'id':'%s'}},
          'toConstraint':{'relationshipEntity':'TRACKED_ENTITY_INSTANCE',
            'trackedEntityType':{'id':'%s'}},
          'translations':[
            {'locale':'fr','property':'NAME','value':'Type fr'},
            {'locale':'fr','property':'RELATIONSHIP_FROM_TO_NAME','value':'Parent de'},
            {'locale':'fr','property':'RELATIONSHIP_TO_FROM_NAME','value':'Enfant de'}],
          'sharing':{'public':'rw------','external':false}
        }]}
        """
        .formatted(
            TRACKED_ENTITY_TYPE_UID,
            TYPE_UID,
            name,
            referral,
            toFromName,
            TRACKED_ENTITY_TYPE_UID,
            TRACKED_ENTITY_TYPE_UID);
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

  private int countConstraintRows() {
    startNewRequestSession();
    return jdbcTemplate.queryForObject(
        "select count(*) from relationshipconstraint", Integer.class);
  }

  /** Reads the relationship type from the database, bypassing the session and the L2 cache. */
  private JsonObject getRelationshipType() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/relationshipTypes/" + TYPE_UID).content(HttpStatus.OK);
  }
}
