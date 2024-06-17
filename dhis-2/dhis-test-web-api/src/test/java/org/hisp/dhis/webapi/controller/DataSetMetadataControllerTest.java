/*
 * Copyright (c) 2004-2024, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
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

import java.util.stream.Stream;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author david mackessy
 */
class DataSetMetadataControllerTest extends DhisControllerIntegrationTest {

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
    POST("/metadata", WebClient.Body(testData)).content(HttpStatus.OK);

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
