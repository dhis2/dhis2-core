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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.webapi.controller.tracker.TestSetup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventFilterControllerTest extends PostgresControllerIntegrationTestBase {
  private static final String IMPORTED_UID = "EvFilter001";

  private static final String PROGRAM = "BFcipDERJnf";

  private static final String PROGRAM_STAGE = "NpsdDv6kKSO";

  private static final String ORG_UNIT = "h4w96yEMlzO";

  /**
   * The properties of an event filter except code, which has no column, and translations, which the
   * schema names "translation" and the JSON names "translations".
   */
  private static final Set<String> SERVED_PROPERTIES_WITHOUT_TRANSLATIONS =
      Set.of(
          "access",
          "attributeValues",
          "created",
          "createdBy",
          "description",
          "displayDescription",
          "displayName",
          "eventQueryCriteria",
          "href",
          "id",
          "lastUpdated",
          "lastUpdatedBy",
          "name",
          "program",
          "programStage",
          "sharing",
          "user");

  @Autowired private TestSetup testSetup;
  @Autowired private IdentifiableObjectManager manager;
  @Autowired private EntityManagerFactory entityManagerFactory;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    manager.flush();
    manager.clear();
  }

  @Test
  void shouldCreateAndUpdateEventFilterViaMetadataImport() {
    importMetadata(fullFilter("Imported filter", "A description", "occurredAt:desc"));

    JsonObject created = getFilter(IMPORTED_UID);
    assertEquals("Imported filter", created.getString("name").string());
    assertEquals("A description", created.getString("description").string());
    assertEquals(PROGRAM, created.getString("program").string());
    assertEquals(PROGRAM_STAGE, created.getString("programStage").string());
    assertEquals(
        "occurredAt:desc", created.getObject("eventQueryCriteria").getString("order").string());
    assertEquals("rw------", created.getObject("sharing").getString("public").string());
    assertEquals(2, created.getArray("translations").size());

    importMetadata(fullFilter("Re-imported filter", "Another description", "updatedAt:asc"));

    JsonObject updated = getFilter(IMPORTED_UID);
    assertEquals("Re-imported filter", updated.getString("name").string());
    assertEquals("Another description", updated.getString("description").string());
    assertEquals(
        "updatedAt:asc", updated.getObject("eventQueryCriteria").getString("order").string());
  }

  /** Every filter, date period and ordering field of the criteria is served as imported. */
  @Test
  void shouldServeImportedCriteriaUnchanged() {
    importMetadata(fullFilter("Imported filter", "A description", "occurredAt:desc"));

    JsonObject criteria = getFilter(IMPORTED_UID).getObject("eventQueryCriteria");

    assertTrue(criteria.getBoolean("followUp").booleanValue());
    assertEquals(ORG_UNIT, criteria.getString("organisationUnit").string());
    assertEquals("DESCENDANTS", criteria.getString("ouMode").string());
    assertEquals("PROVIDED", criteria.getString("assignedUserMode").string());
    assertEquals(List.of("tTgjgobT1oS"), criteria.getArray("assignedUsers").stringValues());
    assertEquals(
        List.of("occurredAt", "status"), criteria.getArray("displayColumnOrder").stringValues());
    assertEquals("COMPLETED", criteria.getString("status").string());
    assertEquals("ABSOLUTE", criteria.getObject("eventDate").getString("type").string());
    assertEquals(
        "2024-01-01T00:00:00.000", criteria.getObject("eventDate").getString("startDate").string());
    assertEquals(-5, criteria.getObject("dueDate").getNumber("startBuffer").intValue());
    assertEquals("LAST_WEEK", criteria.getObject("lastUpdatedDate").getString("period").string());
    assertEquals("THIS_YEAR", criteria.getObject("completedDate").getString("period").string());
    JsonObject dataFilter = criteria.getArray("dataFilters").getObject(0);
    assertEquals("DataElem001", dataFilter.getString("dataItem").string());
    assertEquals("5", dataFilter.getString("ge").string());
    assertEquals("LAST_7_DAYS", dataFilter.getObject("dateFilter").getString("period").string());
  }

  @Test
  void shouldUpdateEventFilterWhenExportedFilterWithChangesIsPut() {
    importMetadata(fullFilter("Imported filter", "A description", "occurredAt:desc"));
    String exported = getFilter(IMPORTED_UID).toJson();
    String changed =
        replace(exported, "\"name\":\"Imported filter\"", "\"name\":\"Changed filter\"");
    changed =
        replace(changed, "\"description\":\"A description\"", "\"description\":\"Changed text\"");
    changed = replace(changed, "\"order\":\"occurredAt:desc\"", "\"order\":\"updatedAt:asc\"");
    changed = replace(changed, "\"status\":\"COMPLETED\"", "\"status\":\"SCHEDULE\"");

    assertStatus(HttpStatus.OK, PUT("/eventFilters/" + IMPORTED_UID, changed));

    JsonObject updated = getFilter(IMPORTED_UID);
    assertEquals("Changed filter", updated.getString("name").string());
    assertEquals("Changed text", updated.getString("description").string());
    assertEquals(PROGRAM_STAGE, updated.getString("programStage").string());
    JsonObject criteria = updated.getObject("eventQueryCriteria");
    assertEquals("updatedAt:asc", criteria.getString("order").string());
    assertEquals("SCHEDULE", criteria.getString("status").string());
    assertEquals(ORG_UNIT, criteria.getString("organisationUnit").string());
  }

  @Test
  void shouldDeleteEventFilter() {
    importMetadata(fullFilter("Imported filter", "A description", "occurredAt:desc"));

    assertStatus(HttpStatus.OK, DELETE("/eventFilters/" + IMPORTED_UID));

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    assertEquals(HttpStatus.NOT_FOUND, GET("/eventFilters/" + IMPORTED_UID).status());
  }

  /** The keys of an exported event filter. */
  @Test
  void shouldExportEventFilterProperties() {
    importMetadata(fullFilter("Imported filter", "A description", "occurredAt:desc"));

    JsonObject filter = getFilter(IMPORTED_UID);

    assertEquals(withTranslationKey("translations"), Set.copyOf(filter.names()));
    assertTrue(filter.getString("href").string().endsWith("/eventFilters/" + IMPORTED_UID));
    assertEquals("Imported filter", filter.getString("displayName").string());
    assertEquals("A description", filter.getString("displayDescription").string());
  }

  /** The code of an event filter has no column, so it is not served after import. */
  @Test
  void shouldNotServeCodeOfImportedEventFilter() {
    importMetadata(fullFilter("Imported filter", "A description", "occurredAt:desc"));

    assertFalse(getFilter(IMPORTED_UID).has("code"));
  }

  /** The gist of an event filter lists the same default properties as before the migration. */
  @Test
  void shouldServeDefaultGistProperties() {
    importMetadata(fullFilter("Imported filter", "A description", "occurredAt:desc"));
    startNewRequestSession();

    JsonObject gist = GET("/eventFilters/{uid}/gist", IMPORTED_UID).content(HttpStatus.OK);

    assertEquals(
        Set.of(
            "apiEndpoints",
            "created",
            "description",
            "eventQueryCriteria",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "program",
            "programStage"),
        Set.copyOf(gist.names()));
  }

  @Test
  void shouldDescribeEventFilterSchemaPropertyNames() {
    Set<String> expectedNames = withTranslationKey("translation");
    expectedNames.add("code");

    assertEquals(expectedNames, schemaPropertyNames(property -> true));
  }

  @Test
  void shouldDescribeEventFilterSchemaPersistedAndRequiredProperties() {
    assertEquals(
        Set.of(
            "created",
            "createdBy",
            "description",
            "eventQueryCriteria",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "program",
            "programStage",
            "sharing",
            "translation"),
        schemaPropertyNames(property -> property.getBoolean("persisted").booleanValue()));
    assertEquals(
        Set.of("name", "program"),
        schemaPropertyNames(property -> property.getBoolean("required").booleanValue()));
  }

  @Test
  void shouldDescribeEventFilterSchemaLengthsAndFlags() {
    JsonObject schema = GET("/schemas/eventFilter").content(HttpStatus.OK);
    Map<String, JsonObject> properties =
        schema.getArray("properties").asList(JsonObject.class).stream()
            .collect(Collectors.toMap(p -> p.getString("name").string(), p -> p));

    assertEquals(230, properties.get("name").getNumber("length").intValue());
    assertEquals(255, properties.get("description").getNumber("length").intValue());
    assertEquals(11, properties.get("program").getNumber("length").intValue());
    assertEquals(11, properties.get("programStage").getNumber("length").intValue());
    assertEquals("URL", properties.get("href").getString("propertyType").string());
    assertEquals("eventFilter", schema.getString("name").string());
    assertTrue(schema.getBoolean("shareable").booleanValue());
    assertTrue(schema.getBoolean("translatable").booleanValue());
    assertTrue(schema.getBoolean("metadata").booleanValue());
  }

  private Set<String> schemaPropertyNames(Predicate<JsonObject> filter) {
    return GET("/schemas/eventFilter")
        .content(HttpStatus.OK)
        .getArray("properties")
        .asList(JsonObject.class)
        .stream()
        .filter(filter)
        .map(property -> property.getString("name").string())
        .collect(Collectors.toCollection(TreeSet::new));
  }

  private static String fullFilter(String name, String description, String order) {
    return """
        {'eventFilters':[{
          'id':'%s','code':'EVF_A','name':'%s','description':'%s',
          'program':'%s','programStage':'%s',
          'translations':[{'locale':'fr','property':'NAME','value':'Filtre'},
                          {'locale':'fr','property':'DESCRIPTION','value':'Une description'}],
          'sharing':{'public':'rw------','external':false},
          'eventQueryCriteria':{
            'followUp':true,'organisationUnit':'%s','ouMode':'DESCENDANTS',
            'assignedUserMode':'PROVIDED','assignedUsers':['tTgjgobT1oS'],
            'order':'%s','displayColumnOrder':['occurredAt','status'],
            'status':'COMPLETED',
            'eventDate':{'type':'ABSOLUTE','startDate':'2024-01-01','endDate':'2024-12-31'},
            'dueDate':{'type':'RELATIVE','startBuffer':-5,'endBuffer':5},
            'lastUpdatedDate':{'type':'RELATIVE','period':'LAST_WEEK'},
            'completedDate':{'type':'RELATIVE','period':'THIS_YEAR'},
            'dataFilters':[{'dataItem':'DataElem001','ge':'5',
              'dateFilter':{'type':'RELATIVE','period':'LAST_7_DAYS'}}]
          }
        }]}
        """
        .formatted(IMPORTED_UID, name, description, PROGRAM, PROGRAM_STAGE, ORG_UNIT, order);
  }

  /** Replaces the single occurrence of {@code from}, failing if it does not occur exactly once. */
  private static String replace(String json, String from, String to) {
    assertEquals(1, json.split(Pattern.quote(from), -1).length - 1, () -> from + " in " + json);
    return json.replace(from, to);
  }

  private static Set<String> withTranslationKey(String translationKey) {
    Set<String> names = new TreeSet<>(SERVED_PROPERTIES_WITHOUT_TRANSLATIONS);
    names.add(translationKey);
    return names;
  }

  private void importMetadata(String metadata) {
    HttpResponse response = POST("/metadata", metadata);
    assertEquals(HttpStatus.OK, response.status(), () -> response.contentUnchecked().toJson());
    assertEquals("OK", response.content().getString("status").string());
  }

  /** Reads the filter from the database, bypassing the session and the L2 cache. */
  private JsonObject getFilter(String uid) {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/eventFilters/" + uid).content(HttpStatus.OK);
  }
}
