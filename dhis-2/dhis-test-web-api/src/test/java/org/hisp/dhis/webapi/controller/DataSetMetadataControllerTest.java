/*
 * Copyright (c) 2004-2023, University of Oslo
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author david mackessy
 */
@Transactional
class DataSetMetadataControllerTest extends PostgresControllerIntegrationTestBase {

  @ParameterizedTest
  @MethodSource("defaultCatComboData")
  @DisplayName(
      "The correct amount of category combos and default category combos are present in the payload")
  void getDatasetMetadataDefaultCatComboTest(
      String testData,
      int expectedCatComboSize,
      int expectedDefaultCatComboCount,
      String catComboSizeCondition,
      String defaultCatComboCondition) {
    // given
    POST("/metadata", Path.of(testData)).content(HttpStatus.OK);

    // when the data entry metadata is retrieved
    JsonArray categoryCombos = GET("/dataEntry/metadata").content().getArray("categoryCombos");

    // then
    assertEquals(expectedCatComboSize, categoryCombos.size(), catComboSizeCondition);
    long count =
        categoryCombos.asList(JsonObject.class).stream()
            .filter(cc -> cc.getString("name").string().equals("default"))
            .count();
    assertEquals(expectedDefaultCatComboCount, count, defaultCatComboCondition);
  }

  @Test
  @DisplayName("Dataset display options are correctly saved and retrieved")
  void getDatasetMetadataDisplayOptionsTest() {
    String expectedDisplayOptions = "{\"aDisplay\": \"option\"}";
    String dataSetsBody =
        """
            {
              "dataSets": [
                {
                  "name": "hellobrenda",
                  "shortName": "hellobrenda",
                  "periodType": "Daily",
                  "displayOptions": "{\\"aDisplay\\": \\"option\\"}"
                }
              ]
            }
        """;

    // given
    POST("/metadata", dataSetsBody).content(HttpStatus.OK);

    // when the data entry metadata is retrieves
    JsonObject dataEntryMetadata = GET("/dataEntry/metadata").content();
    JsonArray dataSets = dataEntryMetadata.getArray("dataSets");
    String actualDisplayOptions = dataSets.getObject(0).getString("displayOptions").string();

    // then
    assertEquals(1, dataSets.size());
    assertEquals(expectedDisplayOptions, actualDisplayOptions);
  }

  @Test
  @DisplayName(
      "All distinct data elements are returned with their category combo and option set intact")
  void dataEntryMetadataReturnsDistinctDataElements() {
    POST("/metadata", Path.of("dataset/dataentry_metadata_query_coverage.json"))
        .content(HttpStatus.OK);

    JsonObject metadata = GET("/dataEntry/metadata").content();
    JsonArray dataElements = metadata.getArray("dataElements");

    // deA is a member of both data sets but must be de-duplicated to a single entry
    assertEquals(3, dataElements.size(), "expected the 3 distinct data elements");

    JsonObject deA = byId(dataElements, "deA00000001");
    JsonObject deB = byId(dataElements, "deB00000001");
    JsonObject deC = byId(dataElements, "deC00000001");
    assertNotNull(deA, "deA should be present");
    assertNotNull(deB, "deB should be present");
    assertNotNull(deC, "deC should be present");

    // the data element category combo is preserved
    assertEquals("ccSex000001", deA.getObject("categoryCombo").getString("id").string());
    assertEquals("ccSex000001", deC.getObject("categoryCombo").getString("id").string());

    // the option set is still resolved (it is not part of the batched fetch, loaded separately)
    assertEquals("osColor0001", deB.getObject("optionSet").getString("id").string());
    assertTrue(
        containsId(metadata.getArray("optionSets"), "osColor0001"),
        "the data element option set should be exported");
  }

  @Test
  @DisplayName(
      "Data set element category combo overrides and all derived category combos are returned")
  void dataEntryMetadataReturnsCategoryComboOverrides() {
    POST("/metadata", Path.of("dataset/dataentry_metadata_query_coverage.json"))
        .content(HttpStatus.OK);

    JsonObject metadata = GET("/dataEntry/metadata").content();

    // ccSex is reached via DataElement.categoryCombo, ccAge only via the data set element override
    // on deB (DataElement.dataSetElements) - both paths are eagerly fetched, both must appear
    JsonArray categoryCombos = metadata.getArray("categoryCombos");
    assertTrue(containsId(categoryCombos, "ccSex000001"), "sex combo (from data element) expected");
    assertTrue(containsId(categoryCombos, "ccAge000001"), "age combo (from override) expected");

    // the override is reflected on the data set element itself
    JsonObject dsOne = byId(metadata.getArray("dataSets"), "dsOne000001");
    assertNotNull(dsOne);
    assertEquals(
        "ccAge000001",
        dataSetElement(dsOne, "deB00000001").getObject("categoryCombo").getString("id").string(),
        "deB data set element should carry its category combo override");

    // the shared data element deA appears in both data sets' data set elements
    assertNotNull(dataSetElement(dsOne, "deA00000001"), "deA expected in DS One");
    JsonObject dsTwo = byId(metadata.getArray("dataSets"), "dsTwo000001");
    assertNotNull(dataSetElement(dsTwo, "deA00000001"), "deA expected in DS Two");
    assertNotNull(dataSetElement(dsTwo, "deC00000001"), "deC expected in DS Two");
  }

  private static JsonObject byId(JsonArray array, String id) {
    return array.asList(JsonObject.class).stream()
        .filter(o -> id.equals(o.getString("id").string()))
        .findFirst()
        .orElse(null);
  }

  private static boolean containsId(JsonArray array, String id) {
    return array.asList(JsonObject.class).stream()
        .anyMatch(o -> id.equals(o.getString("id").string()));
  }

  private static JsonObject dataSetElement(JsonObject dataSet, String dataElementId) {
    return dataSet.getArray("dataSetElements").asList(JsonObject.class).stream()
        .filter(dse -> dataElementId.equals(dse.getObject("dataElement").getString("id").string()))
        .findFirst()
        .orElse(null);
  }

  private static Stream<Arguments> defaultCatComboData() {
    return Stream.of(
        Arguments.of(
            "dataset/data_element_and_dataset_with_catcombo.json",
            2,
            0,
            "2 cat combos should be present",
            "0 default cat combo should be present"),
        Arguments.of(
            "dataset/data_element_with_catcombo.json",
            2,
            1,
            "2 cat combos should be present",
            "1 default cat combo should be present"),
        Arguments.of(
            "dataset/dataset_with_catcombo.json",
            2,
            1,
            "2 cat combos should be present",
            "1 default cat combo should be present"),
        Arguments.of(
            "dataset/dataset_and_data_element_with_no_catcombo.json",
            1,
            1,
            "1 cat combos should be present",
            "1 default cat combo should be present"));
  }
}
