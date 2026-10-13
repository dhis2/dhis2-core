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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the metadata import, merge and Jackson path of the data element dimensions ({@code
 * TrackedEntityDataElementDimension}) embedded in event visualizations.
 */
@Transactional
class EventVisualizationDataElementDimensionsControllerTest
    extends PostgresControllerIntegrationTestBase {

  private static final String EVENT_VISUALIZATION_UID = "EvDeDimVis1";

  private static final String PROGRAM_UID = "EvDeDimPrg1";

  private static final String PROGRAM_STAGE_UID = "EvDeDimStg1";

  private static final String DATA_ELEMENT_UID = "EvDeDimDeA1";

  private static final String LEGEND_SET_A_UID = "EvDeDimLsA1";

  private static final String LEGEND_SET_B_UID = "EvDeDimLsB1";

  private static final ObjectMapper JSON = new ObjectMapper();

  @Autowired private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void setUp() {
    importMetadata(
        """
        {'dataElements':[{'id':'%s','name':'EvDeDim data element','shortName':'EvDeDim DE',
           'valueType':'INTEGER','domainType':'TRACKER','aggregationType':'SUM'}],
         'legendSets':[{'id':'%s','name':'EvDeDim legend set A'},
           {'id':'%s','name':'EvDeDim legend set B'}],
         'programs':[{'id':'%s','name':'EvDeDim program','shortName':'EvDeDim program',
           'programType':'WITHOUT_REGISTRATION'}],
         'programStages':[{'id':'%s','name':'EvDeDim stage','program':{'id':'%s'},
           'programStageDataElements':[{'dataElement':{'id':'%s'}}]}]}
        """
            .formatted(
                DATA_ELEMENT_UID,
                LEGEND_SET_A_UID,
                LEGEND_SET_B_UID,
                PROGRAM_UID,
                PROGRAM_STAGE_UID,
                PROGRAM_UID,
                DATA_ELEMENT_UID));
  }

  @Test
  void testImportPersistsDataElementDimension() {
    importMetadata(eventVisualization(LEGEND_SET_A_UID, "GT:5"));

    assertEquals(List.of(dimension(LEGEND_SET_A_UID, "GT:5")), dimensions());
  }

  /**
   * Exports as clients do (the column with its legend set, program stage and filter), changes the
   * legend set and filter and PUTs the export back: the controller rebuilds the data element
   * dimension from the column.
   */
  @Test
  void testPutOfExportedEventVisualizationUpdatesDimension() throws Exception {
    importMetadata(eventVisualization(LEGEND_SET_A_UID, "GT:5"));
    ObjectNode exported =
        (ObjectNode)
            JSON.readTree(
                GET("/eventVisualizations/"
                        + EVENT_VISUALIZATION_UID
                        + "?fields=*,columns[dimension,filter,legendSet[id],programStage[id],"
                        + "items[id]]")
                    .content(HttpStatus.OK)
                    .toJson());
    ObjectNode column = (ObjectNode) exported.withArray("columns").get(0);
    assertEquals(DATA_ELEMENT_UID, column.get("dimension").asText());
    column.put("filter", "LT:10");
    column.putObject("legendSet").put("id", LEGEND_SET_B_UID);

    assertStatus(
        HttpStatus.OK,
        PUT("/eventVisualizations/" + EVENT_VISUALIZATION_UID, JSON.writeValueAsString(exported)));

    assertEquals(List.of(dimension(LEGEND_SET_B_UID, "LT:10")), dimensions());
  }

  /** The properties of the trackedEntityDataElementDimension schema and their mapping flags. */
  @Test
  void testTrackedEntityDataElementDimensionSchema() {
    JsonObject schema = GET("/schemas/trackedEntityDataElementDimension").content(HttpStatus.OK);
    assertTrue(schema.getBoolean("persisted").booleanValue());
    assertFalse(schema.getBoolean("embeddedObject").booleanValue());

    JsonArray properties = schema.getArray("properties");
    assertEquals(
        Set.of("dataElement", "legendSet", "programStage", "filter"),
        properties.asList(JsonObject.class).stream()
            .map(property -> text(property, "name"))
            .collect(Collectors.toSet()));

    for (String reference : List.of("dataElement", "legendSet", "programStage")) {
      JsonObject property = property(schema, reference);
      assertTrue(property.getBoolean("persisted").booleanValue(), reference);
      assertTrue(property.getBoolean("manyToOne").booleanValue(), reference);
      assertTrue(property.getBoolean("owner").booleanValue(), reference);
      assertFalse(property.getBoolean("required").booleanValue(), reference);
    }

    JsonObject filter = property(schema, "filter");
    assertTrue(filter.getBoolean("persisted").booleanValue());
    assertEquals("TEXT", text(filter, "propertyType"));
    assertEquals(Integer.MAX_VALUE, filter.getNumber("length").intValue());
    assertFalse(filter.getBoolean("required").booleanValue());
  }

  /**
   * A line list of the program stage with the data element as a column. The column is what the
   * event visualization controller rebuilds the data element dimensions from on PUT.
   */
  private static String eventVisualization(String legendSetUid, String filter) {
    return """
        {'eventVisualizations':[{'id':'%s','name':'EvDeDim line list','type':'LINE_LIST',
          'outputType':'EVENT','program':{'id':'%s'},'programStage':{'id':'%s'},
          'columnDimensions':['%s'],
          'dataElementDimensions':[{'dataElement':{'id':'%s'},'programStage':{'id':'%s'},
            'legendSet':{'id':'%s'},'filter':'%s'}]}]}
        """
        .formatted(
            EVENT_VISUALIZATION_UID,
            PROGRAM_UID,
            PROGRAM_STAGE_UID,
            DATA_ELEMENT_UID,
            DATA_ELEMENT_UID,
            PROGRAM_STAGE_UID,
            legendSetUid,
            filter);
  }

  private static String dimension(String legendSetUid, String filter) {
    return String.join(":", DATA_ELEMENT_UID, PROGRAM_STAGE_UID, legendSetUid, filter);
  }

  /**
   * The persisted dimensions as {@code dataElement:programStage:legendSet:filter}, in list order.
   * The session is flushed and cleared and the second-level cache evicted first, so they are read
   * from the database.
   */
  private List<String> dimensions() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET(
            "/eventVisualizations/"
                + EVENT_VISUALIZATION_UID
                + "?fields=dataElementDimensions[dataElement[id],programStage[id],legendSet[id],"
                + "filter]")
        .content(HttpStatus.OK)
        .getArray("dataElementDimensions")
        .asList(JsonObject.class)
        .stream()
        .map(
            dimension ->
                String.join(
                    ":",
                    text(dimension.getObject("dataElement"), "id"),
                    text(dimension.getObject("programStage"), "id"),
                    text(dimension.getObject("legendSet"), "id"),
                    text(dimension, "filter")))
        .toList();
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
