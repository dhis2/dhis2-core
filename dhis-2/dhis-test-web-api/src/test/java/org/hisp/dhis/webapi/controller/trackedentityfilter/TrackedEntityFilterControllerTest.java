/*
 * Copyright (c) 2004-2025, University of Oslo
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
package org.hisp.dhis.webapi.controller.trackedentityfilter;

import static org.hisp.dhis.http.HttpAssertions.assertStatus;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TrackedEntityFilterControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String IMPORTED_UID = "TeFilter001";

  private static final String EXPECTED_CRITERIA =
      "{\"programStage\":\"NpsdDv6kKSO\",\"followUp\":true,"
          + "\"organisationUnit\":\"h4w96yEMlzO\",\"ouMode\":\"SELECTED\","
          + "\"assignedUserMode\":\"PROVIDED\",\"assignedUsers\":[\"tTgjgobT1oS\"],"
          + "\"order\":\"%s\","
          + "\"displayColumnOrder\":[\"w75KJ2mc4zz\",\"zDhUuAYrxNC\"],"
          + "\"trackedEntityType\":\"ja8NY4PW7Xm\","
          + "\"eventStatus\":\"ACTIVE\",\"enrollmentStatus\":\"COMPLETED\","
          + "\"eventDate\":{\"startBuffer\":0,\"endBuffer\":0,\"startDate\":\"2024-01-01T00:00:00.000\","
          + "\"endDate\":\"2024-12-31T00:00:00.000\",\"type\":\"ABSOLUTE\"},"
          + "\"lastUpdatedDate\":{\"startBuffer\":0,\"endBuffer\":0,\"period\":\"LAST_WEEK\","
          + "\"type\":\"RELATIVE\"},"
          + "\"enrollmentIncidentDate\":{\"startBuffer\":0,\"endBuffer\":0,\"period\":\"THIS_YEAR\","
          + "\"type\":\"RELATIVE\"},"
          + "\"enrollmentCreatedDate\":{\"startBuffer\":-5,\"endBuffer\":5,\"type\":\"RELATIVE\"},"
          + "\"trackedEntityInstances\":[\"TeUid000001\"],"
          + "\"attributeValueFilters\":[{\"attribute\":\"dIVt4l5vIOa\",\"eq\":\"v\",\"sw\":\"s\","
          + "\"ew\":\"e\",\"dateFilter\":{\"startBuffer\":0,\"endBuffer\":0,"
          + "\"startDate\":\"2022-01-01T00:00:00.000\",\"endDate\":\"2022-02-01T00:00:00.000\","
          + "\"type\":\"ABSOLUTE\"},\"null\":true}]}";

  private static final String EXPECTED_EVENT_FILTERS =
      "[{\"programStage\":\"NpsdDv6kKSO\",\"eventStatus\":\"%s\","
          + "\"eventCreatedPeriod\":{\"periodFrom\":-10,\"periodTo\":10},"
          + "\"assignedUserMode\":\"PROVIDED\",\"assignedUsers\":[\"tTgjgobT1oS\"]}]";

  @Autowired private TestSetup testSetup;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private EntityManagerFactory entityManagerFactory;

  private final String programId = "BFcipDERJnf";
  private final String trackedEntityAttributeId = "dIVt4l5vIOa";

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    manager.flush();
    manager.clear();
  }

  @Test
  void shouldCreateTrackedEntityFilter() {
    String operatorFilterId = createOperatorFilter("equals operator filter", "eq", "value");

    JsonTrackedEntityFilter trackedEntityFilter =
        GET("/trackedEntityInstanceFilters/{id}", operatorFilterId)
            .content()
            .as(JsonTrackedEntityFilter.class);

    assertFalse(trackedEntityFilter.isEmpty());
    assertEquals(programId, trackedEntityFilter.getProgram());
    JsonObject attributeObject =
        trackedEntityFilter.getEntityQueryCriteria().getAttributeValueFilters().getObject(0);
    assertEquals(trackedEntityAttributeId, attributeObject.getString("attribute").string());
    assertEquals("value", attributeObject.getString("eq").string());
  }

  @Test
  void shouldUpdateTrackedEntityFilter() {
    String operatorFilterId = createOperatorFilter("equals operator filter", "eq", "value");

    String updatedName = "Updated tracked entity filter";
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/trackedEntityInstanceFilters/" + operatorFilterId,
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "attributeValueFilters": [{"eq": "value", "attribute": "%s"}]
                  },
                  "name": "%s"
                }
            """
                .formatted(programId, trackedEntityAttributeId, updatedName)));

    String response =
        GET("/trackedEntityInstanceFilters/{id}", operatorFilterId).content().toString();
    assertTrue(
        response.contains(updatedName),
        "Could not find the tracked entity filter name: " + updatedName + " in the response");
  }

  @Test
  void shouldDeleteTrackedEntityFilter() {
    String operatorFilterId = createOperatorFilter("equals operator filter", "eq", "value");

    HttpResponse response = DELETE("/trackedEntityInstanceFilters/" + operatorFilterId);
    assertEquals(HttpStatus.OK, response.status());
  }

  @Test
  void shouldFailWhenAssignedUserModeProvidedAndNoAssignedUsers() {
    HttpResponse response =
        POST(
            "/trackedEntityInstanceFilters",
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "assignedUserMode": "PROVIDED"
                  },
                  "name": "name"
                }
            """
                .formatted(programId));

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains(
        "Assigned Users cannot be empty with PROVIDED assigned user mode",
        response.error().getMessage());
  }

  @Test
  void shouldFailWhenOrgUnitModeSelectedAndNoOrgUnitProvided() {
    HttpResponse response =
        POST(
            "/trackedEntityInstanceFilters",
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "ouMode": "SELECTED"
                  },
                  "name": "name"
                }
            """
                .formatted(programId));

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains(
        "Organisation Unit cannot be empty with SELECTED org unit mode",
        response.error().getMessage());
  }

  @Test
  void shouldFailWhenNonExistentAttributeProvided() {
    UID nonExistentAttribute = UID.generate();

    HttpResponse response =
        POST(
            "/trackedEntityInstanceFilters",
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "attributeValueFilters": [{"eq": "value", "attribute": "%s"}]
                  },
                  "name": "name"
                }
            """
                .formatted(programId, nonExistentAttribute.getValue()));

    assertEquals(HttpStatus.CONFLICT, response.status());
    assertContains("No tracked entity attribute found", response.error().getMessage());
  }

  @Test
  void shouldCreateTrackedEntityFilterWhenNullOperatorSupplied() {
    String operatorFilterId = createOperatorFilter("null operator filter", "null", true);

    JsonTrackedEntityFilter trackedEntityFilter =
        GET("/trackedEntityInstanceFilters/{id}", operatorFilterId)
            .content()
            .as(JsonTrackedEntityFilter.class);

    assertFalse(trackedEntityFilter.isEmpty());
    assertEquals(programId, trackedEntityFilter.getProgram());
    JsonObject attributeObject =
        trackedEntityFilter.getEntityQueryCriteria().getAttributeValueFilters().getObject(0);
    assertEquals(trackedEntityAttributeId, attributeObject.getString("attribute").string());
    assertEquals(Boolean.TRUE, attributeObject.getBoolean("null").booleanValue());
  }

  @Test
  void shouldCreateTrackedEntityFilterWhenNonNullOperatorSupplied() {
    String operatorFilterId = createOperatorFilter("non null operator filter", "null", false);

    JsonTrackedEntityFilter trackedEntityFilter =
        GET("/trackedEntityInstanceFilters/{id}", operatorFilterId)
            .content()
            .as(JsonTrackedEntityFilter.class);

    assertFalse(trackedEntityFilter.isEmpty());
    assertEquals(programId, trackedEntityFilter.getProgram());
    JsonObject attributeObject =
        trackedEntityFilter.getEntityQueryCriteria().getAttributeValueFilters().getObject(0);
    assertEquals(trackedEntityAttributeId, attributeObject.getString("attribute").string());
    assertEquals(Boolean.FALSE, attributeObject.getBoolean("null").booleanValue());
  }

  @Test
  void shouldCreateAndUpdateTrackedEntityFilterViaMetadataImport() {
    importMetadata(fullFilter("TEF_A", "Imported filter", "createdAt:desc"));

    JsonObject created = getFilter(IMPORTED_UID);
    assertEquals("TEF_A", created.getString("code").string());
    assertEquals("Imported filter", created.getString("name").string());
    assertEquals("A description", created.getString("description").string());
    assertEquals(3, created.getNumber("sortOrder").intValue());
    assertEquals("#ff0000", created.getObject("style").getString("color").string());
    assertEquals("icon-a", created.getObject("style").getString("icon").string());
    assertEquals(programId, created.getObject("program").getString("id").string());
    assertEquals("rw------", created.getObject("sharing").getString("public").string());
    assertEquals(2, created.getArray("translations").size());

    importMetadata(fullFilter("TEF_B", "Re-imported filter", "updatedAt:asc"));

    JsonObject updated = getFilter(IMPORTED_UID);
    assertEquals("TEF_B", updated.getString("code").string());
    assertEquals("Re-imported filter", updated.getString("name").string());
    assertEquals(
        "updatedAt:asc", updated.getObject("entityQueryCriteria").getString("order").string());
  }

  /** Every filter, date period and ordering field of the criteria is served as imported. */
  @Test
  void shouldServeImportedCriteriaAndEventFiltersUnchanged() {
    importMetadata(fullFilter("TEF_A", "Imported filter", "createdAt:desc"));

    JsonObject filter = getFilter(IMPORTED_UID);

    assertEquals(
        EXPECTED_CRITERIA.formatted("createdAt:desc"),
        filter.getObject("entityQueryCriteria").toJson());
    assertEquals(
        EXPECTED_EVENT_FILTERS.formatted("SCHEDULE"), filter.getArray("eventFilters").toJson());
    assertEquals("COMPLETED", filter.getString("enrollmentStatus").string());
    assertTrue(filter.getBoolean("followup").booleanValue());
    assertEquals(
        -5, filter.getObject("enrollmentCreatedPeriod").getNumber("periodFrom").intValue());
    assertEquals(5, filter.getObject("enrollmentCreatedPeriod").getNumber("periodTo").intValue());
  }

  @Test
  void shouldUpdateTrackedEntityFilterWhenExportedFilterWithChangesIsPut() {
    importMetadata(fullFilter("TEF_A", "Imported filter", "createdAt:desc"));
    String exported = getFilter(IMPORTED_UID).toJson();
    assertTrue(exported.contains("\"name\":\"Imported filter\""), exported);
    assertTrue(exported.contains("\"order\":\"createdAt:desc\""), exported);
    assertTrue(exported.contains("\"eventStatus\":\"SCHEDULE\""), exported);
    assertTrue(exported.contains("\"sortOrder\":3"), exported);
    String changed =
        exported
            .replace("\"name\":\"Imported filter\"", "\"name\":\"Changed filter\"")
            .replace("\"order\":\"createdAt:desc\"", "\"order\":\"updatedAt:asc\"")
            .replace("\"eventStatus\":\"SCHEDULE\"", "\"eventStatus\":\"OVERDUE\"")
            .replace("\"sortOrder\":3", "\"sortOrder\":4")
            .replace("\"value\":\"Filtre\"", "\"value\":\"Filtre change\"");

    assertStatus(HttpStatus.OK, PUT("/trackedEntityInstanceFilters/" + IMPORTED_UID, changed));

    JsonObject updated = getFilter(IMPORTED_UID);
    assertEquals("Changed filter", updated.getString("name").string());
    assertEquals(4, updated.getNumber("sortOrder").intValue());
    assertEquals(
        EXPECTED_CRITERIA.formatted("updatedAt:asc"),
        updated.getObject("entityQueryCriteria").toJson());
    assertEquals(
        EXPECTED_EVENT_FILTERS.formatted("OVERDUE"), updated.getArray("eventFilters").toJson());
    assertTrue(updated.getArray("translations").toJson().contains("\"Filtre change\""));
  }

  /** The keys of an exported tracked entity filter. */
  @Test
  void shouldExportTrackedEntityFilterProperties() {
    importMetadata(fullFilter("TEF_A", "Imported filter", "createdAt:desc"));

    JsonObject filter = getFilter(IMPORTED_UID);

    assertEquals(
        Set.of(
            "access",
            "attributeValues",
            "code",
            "created",
            "createdBy",
            "description",
            "displayDescription",
            "displayName",
            "enrollmentCreatedPeriod",
            "enrollmentStatus",
            "entityQueryCriteria",
            "eventFilters",
            "followup",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "program",
            "sharing",
            "sortOrder",
            "style",
            "translations",
            "user"),
        Set.copyOf(filter.names()));
    assertTrue(
        filter
            .getString("href")
            .string()
            .endsWith("/trackedEntityInstanceFilters/" + IMPORTED_UID));
    assertEquals("Imported filter", filter.getString("displayName").string());
    assertEquals("A description", filter.getString("displayDescription").string());
  }

  /** The schema properties of a tracked entity filter with their persisted flags and lengths. */
  @Test
  void shouldDescribeTrackedEntityFilterSchemaProperties() {
    JsonObject schema = GET("/schemas/trackedEntityInstanceFilter").content(HttpStatus.OK);
    JsonArray properties = schema.getArray("properties");
    Set<String> names = new TreeSet<>();
    Set<String> persisted = new TreeSet<>();
    Set<String> required = new TreeSet<>();
    Map<String, JsonObject> byName = new HashMap<>();
    for (int i = 0; i < properties.size(); i++) {
      JsonObject property = properties.getObject(i);
      String name = property.getString("name").string();
      names.add(name);
      byName.put(name, property);
      if (property.getBoolean("persisted").booleanValue()) {
        persisted.add(name);
      }
      if (property.getBoolean("required").booleanValue()) {
        required.add(name);
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
            "displayName",
            "enrollmentCreatedPeriod",
            "enrollmentStatus",
            "entityQueryCriteria",
            "eventFilters",
            "followup",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "program",
            "sharing",
            "sortOrder",
            "style",
            "translation",
            "user"),
        names);
    assertEquals(
        Set.of(
            "code",
            "created",
            "createdBy",
            "description",
            "entityQueryCriteria",
            "eventFilters",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "program",
            "sharing",
            "sortOrder",
            "style",
            "translation"),
        persisted);
    assertEquals(Set.of("name", "program"), required);
    assertEquals(230, byName.get("name").getNumber("length").intValue());
    assertEquals(255, byName.get("description").getNumber("length").intValue());
    assertEquals(50, byName.get("code").getNumber("length").intValue());
    assertEquals("URL", byName.get("href").getString("propertyType").string());
    assertEquals("trackedEntityFilter", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private String fullFilter(String code, String name, String order) {
    return """
        {'trackedEntityInstanceFilters':[{
          'id':'%s','code':'%s','name':'%s','description':'A description',
          'sortOrder':3,'style':{'color':'#ff0000','icon':'icon-a'},
          'program':{'id':'%s'},
          'translations':[{'locale':'fr','property':'NAME','value':'Filtre'},
                          {'locale':'fr','property':'DESCRIPTION','value':'Une description'}],
          'sharing':{'public':'rw------','external':false},
          'eventFilters':[{'programStage':'NpsdDv6kKSO','eventStatus':'SCHEDULE',
            'eventCreatedPeriod':{'periodFrom':-10,'periodTo':10},
            'assignedUserMode':'PROVIDED','assignedUsers':['tTgjgobT1oS']}],
          'entityQueryCriteria':{
            'programStage':'NpsdDv6kKSO','followUp':true,
            'organisationUnit':'h4w96yEMlzO','ouMode':'SELECTED',
            'assignedUserMode':'PROVIDED','assignedUsers':['tTgjgobT1oS'],
            'order':'%s',
            'displayColumnOrder':['w75KJ2mc4zz','zDhUuAYrxNC'],
            'trackedEntityInstances':['TeUid000001'],'trackedEntityType':'ja8NY4PW7Xm',
            'eventStatus':'ACTIVE','enrollmentStatus':'COMPLETED',
            'eventDate':{'type':'ABSOLUTE','startDate':'2024-01-01','endDate':'2024-12-31'},
            'lastUpdatedDate':{'type':'RELATIVE','period':'LAST_WEEK'},
            'enrollmentIncidentDate':{'type':'RELATIVE','period':'THIS_YEAR'},
            'enrollmentCreatedDate':{'type':'RELATIVE','startBuffer':-5,'endBuffer':5},
            'attributeValueFilters':[{'attribute':'%s','eq':'v','sw':'s','ew':'e','null':true,
              'dateFilter':{'type':'ABSOLUTE','startDate':'2022-01-01','endDate':'2022-02-01'}}]
          }
        }]}
        """
        .formatted(IMPORTED_UID, code, name, programId, order, trackedEntityAttributeId);
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
  }

  /** Reads the filter from the database, bypassing the session and the L2 cache. */
  private JsonObject getFilter(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/trackedEntityInstanceFilters/" + uid).content(HttpStatus.OK);
  }

  private String createOperatorFilter(String filterName, String operator, Object value) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/trackedEntityInstanceFilters",
            """
                {
                  "program": {"id": "%s"},
                  "entityQueryCriteria": {
                    "attributeValueFilters": [{"%s": %s, "attribute": "%s"}]
                  },
                  "name": "%s"
                }
            """
                .formatted(
                    programId,
                    operator,
                    formatValue(value),
                    trackedEntityAttributeId,
                    filterName)));
  }

  private String formatValue(Object value) {
    return (value instanceof Boolean) ? value.toString() : "\"" + value + "\"";
  }
}
