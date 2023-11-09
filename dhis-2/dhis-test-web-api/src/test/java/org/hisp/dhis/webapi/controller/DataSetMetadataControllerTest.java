/*
 * Copyright (c) 2004-2023, University of Oslo
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

import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.web.WebClient;
import org.hisp.dhis.webapi.DhisControllerIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class DataSetMetadataControllerTest extends DhisControllerIntegrationTest {

  @Test
  void testGetDatasetMetadata_NoDatasetCatCombo_OneDataElementCatCombo() {
    // given there is only 1 cat combo associated with a 1 data element
    POST("/metadata", WebClient.Body("dataset/data_element_with_catcombo.json"))
        .content(HttpStatus.OK);

    // when the dataset metadata is retrieved
    JsonArray categoryCombos = GET("/dataEntry/metadata").content().getArray("categoryCombos");

    // then there is only 1 default cat combo in the retrieved 'categoryCombos' section
    // expect 2 cat combos in total ('default' and 'ageGroup_sex')
    assertEquals(2, categoryCombos.size());

    // and only 1 default cat combo should be present
    long count =
        categoryCombos.asList(JsonObject.class).stream()
            .filter(cc -> cc.getString("name").string().equals("default"))
            .count();
    assertEquals(1, count);
  }

  @Test
  void testGetDatasetMetadata_OneDatasetCatCombo_NoDataElementCatCombo() {
    // given there is only 1 cat combo associated with a 1 data dataset
    POST("/metadata", WebClient.Body("dataset/dataset_with_catcombo.json")).content(HttpStatus.OK);

    // when the dataset metadata is retrieved
    JsonArray categoryCombos = GET("/dataEntry/metadata").content().getArray("categoryCombos");

    // then there is only 1 default cat combo in the retrieved 'categoryCombos' section
    // expect 2 cat combos in total ('default' and 'ageGroup_sex')
    assertEquals(2, categoryCombos.size());

    // and only 1 default cat combo should be present
    long count =
        categoryCombos.asList(JsonObject.class).stream()
            .filter(cc -> cc.getString("name").string().equals("default"))
            .count();
    assertEquals(1, count);
  }

  @Test
  void testGetDatasetMetadata_NoDatasetCatCombo_NoDataElementCatCombo() {
    // given there are no cat combos associated with a data element or dataset
    POST("/metadata", WebClient.Body("dataset/dataset_and_data_element_with_no_catcombo.json"))
        .content(HttpStatus.OK);

    // when the dataset metadata is retrieved
    JsonArray categoryCombos = GET("/dataEntry/metadata").content().getArray("categoryCombos");

    // then there is only 1 default cat combo in the retrieved 'categoryCombos' section
    // expect 1 default cat combo
    assertEquals(1, categoryCombos.size());

    // and only 1 default cat combo should be present
    long count =
        categoryCombos.asList(JsonObject.class).stream()
            .filter(cc -> cc.getString("name").string().equals("default"))
            .count();
    assertEquals(1, count);
  }
}
