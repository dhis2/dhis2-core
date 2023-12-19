/*
 * Copyright (c) 2004-2022, University of Oslo
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

import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonCategoryOptionCombo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategoryComboModificationControllerTest extends DhisControllerConvenienceTest {

  String testCatCombo;

  String dataElementId;

  String orgUnitId;
  String categoryColor;
  String categoryTaste;

  @Test
  void testModificationNoData() {

    setTestCatComboModifiableProperties();
    // Remove a category
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            // language=JSON
            """
                { "name" : "COLOR AND TASTE",
                "id" : "%s",
                "shortName": "C_AND_T",
                 "skipTotals" : true,
                  "dataDimensionType" : "DISAGGREGATION",
                  "categories" : [ {"id" : "%s"} ]} """
                .formatted(testCatCombo, categoryColor)));
  }

  @Test
  void testModificationWithData() {

    JsonObject response =
        GET("/categoryCombos/" + testCatCombo + "?fields=categoryOptionCombos[id]").content();
    JsonList<JsonCategoryOptionCombo> catOptionCombos =
        response.getList("categoryOptionCombos", JsonCategoryOptionCombo.class);
    String categoryOptionComboId = catOptionCombos.get(0).getId();
    // language=JSON
    String body =
        """
        {"dataElement": "%s",
        "categoryOptionCombo": "%s",
        "period": "20220102",
        "orgUnit": "%s",
        "value": "24",
        "comment":"OK"}"""
            .formatted(dataElementId, categoryOptionComboId, orgUnitId);

    assertStatus(HttpStatus.CREATED, POST("/dataValues", body));

    // We should not be able to remove a category here
    assertStatus(
        HttpStatus.CONFLICT,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            // language=JSON
            """
                { "name" : "COLOR AND TASTE",
                "id" : "%s",
                "shortName": "C_AND_T",
                "skipTotals" : true,
                "dataDimensionType" :
                "DISAGGREGATION",
                "categories" : [{"id" : "%s"} ]}"""
                .formatted(testCatCombo, categoryColor)));
  }

  void setTestCatComboModifiableProperties() {
    // Modify the initial name
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            // language=JSON
            """
            { "name" : "COLOR AND TASTE",
             "id" : "%s",
             "dataDimensionType" : "DISAGGREGATION",
             "categories" : [{"id" : "%s"} , {"id" : "%s"}]}"""
                .formatted(testCatCombo, categoryColor, categoryTaste)));
    // Add a shortname
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            // language=JSON
            """
            { "name" : "COLOR AND TASTE",
             "id" : "%s",
             "shortName" : "C_AND_T",
             "dataDimensionType" : "DISAGGREGATION",
             "categories" : [{"id" : "%s"} , {"id" : "%s"}]}"""
                .formatted(testCatCombo, categoryColor, categoryTaste)));
    // Skip totals
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            // language=JSON
            """
            { "name" : "COLOR AND TASTE",
             "id" : "%s",
             "shortName" : "C_AND_T",
             "skipTotals" : true,
             "dataDimensionType" : "DISAGGREGATION",
             "categories" : [{"id" : "%s"} , {"id" : "%s"}]}"""
                .formatted(testCatCombo, categoryColor, categoryTaste)));
  }

  private String createCategoryOptions(String name) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categoryOptions",
            // language=JSON
            """
                { "name": "%s", "shortName": "%s" }""".formatted(name, name)));
  }

  private String createSimpleCategory(String name, String categoryOptionId) {
    return assertStatus(
        HttpStatus.CREATED,
        POST(
            "/categories",
            // language=JSON
            """
                { "name": "%s", "shortName": "%s", "dataDimensionType": "DISAGGREGATION" ,
                "categoryOptions" : [{"id" : "%s"} ] }"""
                .formatted(name, name, categoryOptionId)));
  }

  @BeforeEach
  void setupTest() {
    String categoryOptionSour = createCategoryOptions("Sour");
    String categoryOptionRed = createCategoryOptions("Red");
    categoryColor = createSimpleCategory("Color", categoryOptionRed);
    categoryTaste = createSimpleCategory("Taste", categoryOptionSour);

    testCatCombo =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryCombos",
                // language=JSON
                """
                    { "name" : "Taste and color",
                    "dataDimensionType" : "DISAGGREGATION", "categories" : [
                    {"id" : "%s"} , {"id" : "%s"}]} """
                    .formatted(categoryColor, categoryTaste)));

    orgUnitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                // language=JSON
                """
                    {"name":"My Unit", "shortName":"OU1", "openingDate": "2020-01-01"}"""));
    assertStatus(
        HttpStatus.OK,
        POST(
            "/users/{id}/organisationUnits",
            getCurrentUser().getUid(),
            Body( // language=JSON
                """
                    {"additions":[{"id":"%s"}]}""".formatted(orgUnitId))));

    dataElementId = addDataElement("My data element", "DE1", ValueType.INTEGER, null, testCatCombo);
  }
}
