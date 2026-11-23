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
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;
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
 * Tests the metadata import, merge and Jackson path of the category option group set dimensions
 * ({@code CategoryOptionGroupSetDimension}) embedded in visualizations.
 */
@Transactional
class VisualizationCategoryOptionGroupSetDimensionsControllerTest
    extends PostgresControllerIntegrationTestBase {

  private static final String VISUALIZATION_UID = "VizCogsDim1";

  private static final String GROUP_SET_UID = "CogsDimSet1";

  private static final String GROUP_A_UID = "CogsDimGrpA";

  private static final String GROUP_B_UID = "CogsDimGrpB";

  private static final String GROUP_C_UID = "CogsDimGrpC";

  private static final ObjectMapper JSON = new ObjectMapper();

  @Autowired private EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void setUp() {
    importMetadata(
        """
        {'categoryOptionGroups':[%s,%s,%s],
         'categoryOptionGroupSets':[{'id':'%s','name':'COGS dimension set',
           'shortName':'COGS dimension set','dataDimensionType':'DISAGGREGATION',
           'dataDimension':true,
           'categoryOptionGroups':[{'id':'%s'},{'id':'%s'},{'id':'%s'}]}]}
        """
            .formatted(
                group(GROUP_A_UID),
                group(GROUP_B_UID),
                group(GROUP_C_UID),
                GROUP_SET_UID,
                GROUP_A_UID,
                GROUP_B_UID,
                GROUP_C_UID));
  }

  @Test
  void testImportPersistsDimensionAndItemsInOrder() {
    importMetadata(visualization(GROUP_C_UID, GROUP_A_UID, GROUP_B_UID));

    assertEquals(List.of(dimension(GROUP_C_UID, GROUP_A_UID, GROUP_B_UID)), dimensions());
  }

  @Test
  void testReimportReplacesItems() {
    importMetadata(visualization(GROUP_C_UID, GROUP_A_UID, GROUP_B_UID));

    importMetadata(visualization(GROUP_B_UID));

    assertEquals(List.of(dimension(GROUP_B_UID)), dimensions());
  }

  /**
   * Exports as clients do (the filter with its items), changes the filter items and PUTs the export
   * back: the controller rebuilds the category option group set dimension from the filter.
   */
  @Test
  void testPutOfExportedVisualizationUpdatesItems() throws Exception {
    importMetadata(visualization(GROUP_C_UID, GROUP_A_UID, GROUP_B_UID));
    ObjectNode exported =
        (ObjectNode)
            JSON.readTree(
                GET("/visualizations/" + VISUALIZATION_UID + "?fields=*,filters[id,items[id]]")
                    .content(HttpStatus.OK)
                    .toJson());
    ArrayNode filterItems = (ArrayNode) exported.withArray("filters").get(0).withArray("items");
    filterItems.removeAll();
    filterItems.addObject().put("id", GROUP_B_UID);
    filterItems.addObject().put("id", GROUP_C_UID);

    assertStatus(
        HttpStatus.OK,
        PUT("/visualizations/" + VISUALIZATION_UID, JSON.writeValueAsString(exported)));

    assertEquals(List.of(dimension(GROUP_B_UID, GROUP_C_UID)), dimensions());
  }

  /** The mapping-derived flags of the categoryOptionGroupSetDimension schema and its properties. */
  @Test
  void testCategoryOptionGroupSetDimensionSchema() {
    JsonObject schema = GET("/schemas/categoryOptionGroupSetDimension").content(HttpStatus.OK);
    assertTrue(schema.getBoolean("persisted").booleanValue());
    assertTrue(schema.getBoolean("embeddedObject").booleanValue());

    JsonObject groupSet = property(schema, "categoryOptionGroupSet");
    assertTrue(groupSet.getBoolean("persisted").booleanValue());
    assertTrue(groupSet.getBoolean("manyToOne").booleanValue());
    assertTrue(groupSet.getBoolean("owner").booleanValue());

    JsonObject groups = property(schema, "categoryOptionGroup");
    assertTrue(groups.getBoolean("persisted").booleanValue());
    assertTrue(groups.getBoolean("ordered").booleanValue());
    assertTrue(groups.getBoolean("manyToMany").booleanValue());
    assertTrue(groups.getBoolean("owner").booleanValue());
  }

  private static String group(String uid) {
    return """
        {'id':'%s','name':'Group %s','shortName':'Group %s','dataDimensionType':'DISAGGREGATION'}
        """
        .formatted(uid, uid, uid);
  }

  /**
   * A visualization filtered by the group set. The filter dimension is what the visualization
   * controller rebuilds the category option group set dimensions from on PUT.
   */
  private static String visualization(String... groupUids) {
    String items =
        Arrays.stream(groupUids).map("{'id':'%s'}"::formatted).collect(Collectors.joining(","));
    return """
        {'visualizations':[{'id':'%s','name':'COGS dimension visualization','type':'COLUMN',
          'filterDimensions':['%s'],
          'categoryOptionGroupSetDimensions':[
            {'categoryOptionGroupSet':{'id':'%s'},'categoryOptionGroups':[%s]}]}]}
        """
        .formatted(VISUALIZATION_UID, GROUP_SET_UID, GROUP_SET_UID, items);
  }

  private static String dimension(String... groupUids) {
    return GROUP_SET_UID + ":" + String.join(",", groupUids);
  }

  /**
   * The persisted dimensions as {@code groupSet:group,group,...}, in list order. The session is
   * flushed and cleared and the second-level cache evicted first, so they are read from the
   * database.
   */
  private List<String> dimensions() {
    startNewRequestSession();
    entityManagerFactory.getCache().evictAll();
    return GET(
            "/visualizations/"
                + VISUALIZATION_UID
                + "?fields=categoryOptionGroupSetDimensions[categoryOptionGroupSet[id],"
                + "categoryOptionGroups[id]]")
        .content(HttpStatus.OK)
        .getArray("categoryOptionGroupSetDimensions")
        .asList(JsonObject.class)
        .stream()
        .map(
            dimension ->
                text(dimension.getObject("categoryOptionGroupSet"), "id")
                    + ":"
                    + dimension.getArray("categoryOptionGroups").asList(JsonObject.class).stream()
                        .map(group -> text(group, "id"))
                        .collect(Collectors.joining(",")))
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
