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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests how program stage data elements are imported, exported and updated as embedded objects of
 * their program stage.
 */
@Transactional
class ProgramStageDataElementControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String STAGE_UID = "psdeStageA1";

  private static final String METADATA =
      """
      {
        "dataElements": [
          {"id": "psdeDeAAAA1", "name": "PSDE DE A", "shortName": "PSDE DE A",
           "valueType": "TEXT", "domainType": "TRACKER", "aggregationType": "NONE"},
          {"id": "psdeDeBBBB1", "name": "PSDE DE B", "shortName": "PSDE DE B",
           "valueType": "TEXT", "domainType": "TRACKER", "aggregationType": "NONE"}
        ],
        "programs": [
          {"id": "psdeProgrA1", "name": "PSDE program", "shortName": "PSDE program",
           "programType": "WITHOUT_REGISTRATION", "programStages": [{"id": "psdeStageA1"}]}
        ],
        "programStages": [
          {"id": "psdeStageA1", "name": "PSDE stage", "program": {"id": "psdeProgrA1"},
           "programStageDataElements": [
             {"id": "psdeElemAA1", "code": "PSDE_A", "dataElement": {"id": "psdeDeAAAA1"},
              "compulsory": true, "allowProvidedElsewhere": true, "sortOrder": 2,
              "displayInReports": true, "allowFutureDate": true, "renderOptionsAsRadio": true,
              "skipSynchronization": true, "skipAnalytics": true,
              "renderType": {"MOBILE": {"type": "VALUE"}}},
             {"id": "psdeElemBB1", "dataElement": {"id": "psdeDeBBBB1"}, "sortOrder": 1}
           ]}
        ]
      }
      """;

  @Autowired private EntityManagerFactory entityManagerFactory;

  @Autowired private JdbcTemplate jdbcTemplate;

  /** The property set of the schema, as served before the annotation mapping. */
  @Test
  void testSchemaProperties() {
    JsonArray properties =
        GET("/schemas/programStageDataElement").content(HttpStatus.OK).getArray("properties");

    assertEquals(
        Set.of(
            "access",
            "allowFutureDate",
            "allowProvidedElsewhere",
            "attributeValues",
            "code",
            "compulsory",
            "created",
            "createdBy",
            "dataElement",
            "displayInReports",
            "displayName",
            "href",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "name",
            "programStage",
            "renderOptionsAsRadio",
            "renderType",
            "sharing",
            "skipAnalytics",
            "skipSynchronization",
            "sortOrder",
            "translation",
            "user"),
        propertyNames(properties, false));
    assertEquals(
        Set.of(
            "allowFutureDate",
            "allowProvidedElsewhere",
            "code",
            "compulsory",
            "created",
            "dataElement",
            "displayInReports",
            "id",
            "lastUpdated",
            "lastUpdatedBy",
            "programStage",
            "renderOptionsAsRadio",
            "renderType",
            "skipAnalytics",
            "skipSynchronization",
            "sortOrder"),
        propertyNames(properties, true));
    assertEquals(
        Set.of("compulsory", "dataElement", "skipAnalytics"), requiredPropertyNames(properties));
  }

  @Test
  void testImportWritesEveryColumnAndReimportKeepsOneRowPerDataElement() {
    importMetadata();

    assertEquals(
        Map.ofEntries(
            Map.entry("code", "PSDE_A"),
            Map.entry("stage", STAGE_UID),
            Map.entry("dataelement", "psdeDeAAAA1"),
            Map.entry("compulsory", true),
            Map.entry("allowprovidedelsewhere", true),
            Map.entry("sort_order", 2),
            Map.entry("displayinreports", true),
            Map.entry("allowfuturedate", true),
            Map.entry("renderoptionsasradio", true),
            Map.entry("skipsynchronization", true),
            Map.entry("skipanalytics", true),
            Map.entry("rendertype", "VALUE")),
        row("psdeElemAA1"));

    importMetadata();

    assertEquals(List.of("psdeElemAA1", "psdeElemBB1"), dataElementRowUids());
    assertTrue((Boolean) row("psdeElemAA1").get("compulsory"));
  }

  @Test
  void testExportedDataElementsKeepTheirKeysAndSortOrder() {
    importMetadata();

    JsonArray elements = getStage().getArray("programStageDataElements");

    assertEquals(
        List.of("psdeElemBB1", "psdeElemAA1"),
        elements.asList(JsonObject.class).stream().map(e -> e.getString("id").string()).toList());
    assertEquals(
        Set.of(
            "id",
            "created",
            "lastUpdated",
            "translations",
            "sharing",
            "access",
            "attributeValues",
            "programStage",
            "dataElement",
            "compulsory",
            "allowProvidedElsewhere",
            "sortOrder",
            "displayInReports",
            "allowFutureDate",
            "renderOptionsAsRadio",
            "skipSynchronization",
            "skipAnalytics"),
        Set.copyOf(elements.getObject(0).names()));
    JsonObject elementA = elements.getObject(1);
    assertEquals("PSDE_A", elementA.getString("code").string());
    assertEquals(STAGE_UID, elementA.getString("programStage.id").string());
    assertEquals("VALUE", elementA.getString("renderType.MOBILE.type").string());
    assertEquals(Set.of("users", "userGroups"), Set.copyOf(elementA.getObject("sharing").names()));
  }

  @Test
  void testPutOfTheExportedStageUpdatesItsDataElements() throws JsonProcessingException {
    importMetadata();
    ObjectNode exported = (ObjectNode) new ObjectMapper().readTree(getStage().toJson());
    ObjectNode elementB = (ObjectNode) exported.get("programStageDataElements").get(0);
    assertEquals("psdeElemBB1", elementB.get("id").asText());
    assertFalse(elementB.get("compulsory").asBoolean());
    elementB.put("compulsory", true);
    elementB.put("sortOrder", 5);

    assertStatus(HttpStatus.OK, PUT("/programStages/" + STAGE_UID, exported.toString()));

    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    Map<String, Object> rowB = row("psdeElemBB1");
    assertTrue((Boolean) rowB.get("compulsory"));
    assertEquals(5, rowB.get("sort_order"));
    assertEquals(List.of("psdeElemAA1", "psdeElemBB1"), dataElementRowUids());
    assertEquals(
        List.of("psdeElemAA1", "psdeElemBB1"),
        getStage().getArray("programStageDataElements").asList(JsonObject.class).stream()
            .map(e -> e.getString("id").string())
            .toList());
  }

  private void importMetadata() {
    assertEquals(
        "OK",
        POST("/metadata", METADATA).content(HttpStatus.OK).getString("status").string(),
        "metadata import");
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
  }

  /** The stage as served in a new session, read from the database. */
  private JsonObject getStage() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET("/programStages/{uid}", STAGE_UID).content(HttpStatus.OK);
  }

  private Map<String, Object> row(String uid) {
    return jdbcTemplate.queryForMap(
        "select psde.code, ps.uid as stage, de.uid as dataelement, psde.compulsory,"
            + " psde.allowprovidedelsewhere, psde.sort_order, psde.displayinreports,"
            + " psde.allowfuturedate, psde.renderoptionsasradio, psde.skipsynchronization,"
            + " psde.skipanalytics, psde.rendertype -> 'MOBILE' ->> 'type' as rendertype"
            + " from programstagedataelement psde"
            + " join programstage ps on ps.programstageid = psde.programstageid"
            + " join dataelement de on de.dataelementid = psde.dataelementid"
            + " where psde.uid = ?",
        uid);
  }

  /** Uids of all rows of the stage's data elements. */
  private List<String> dataElementRowUids() {
    return jdbcTemplate.queryForList(
        "select psde.uid from programstagedataelement psde"
            + " join programstage ps on ps.programstageid = psde.programstageid"
            + " where ps.uid = ? order by psde.uid",
        String.class,
        STAGE_UID);
  }

  private static Set<String> propertyNames(JsonArray properties, boolean persistedOnly) {
    return properties.asList(JsonObject.class).stream()
        .filter(p -> !persistedOnly || p.getBoolean("persisted").booleanValue())
        .map(p -> p.getString("name").string())
        .collect(Collectors.toSet());
  }

  private static Set<String> requiredPropertyNames(JsonArray properties) {
    return properties.asList(JsonObject.class).stream()
        .filter(p -> p.getBoolean("required").booleanValue())
        .map(p -> p.getString("name").string())
        .collect(Collectors.toSet());
  }
}
