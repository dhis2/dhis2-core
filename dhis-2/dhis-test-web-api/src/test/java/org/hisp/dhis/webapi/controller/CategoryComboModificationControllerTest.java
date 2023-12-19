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

import static java.lang.String.format;
import static org.hisp.dhis.web.WebClient.Body;
import static org.hisp.dhis.web.WebClientUtils.assertStatus;

import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.jsontree.JsonList;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.web.HttpStatus;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.hisp.dhis.webapi.json.domain.JsonCategoryOptionCombo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CategoryComboModificationControllerTest extends DhisControllerConvenienceTest {
  @Autowired private DataValueService dataValueService;

  String testCatCombo;

  String dataElementId;

  String orgUnitId;
  String categoryColor;
  String categoryTaste;

  @Test
  void testModificationNoData() {
    setupTest();
    setTestCatComboModifiableProperties();
    // Remove a category
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            "{ 'name' : 'COLOR AND TASTE', 'id' : '"
                + testCatCombo
                + "', "
                + "'shortName': 'C_AND_T', 'skipTotals' : true, "
                + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                + "{'id' : '"
                + categoryColor
                + "'} ]} "));
  }

  @Test
  void testModificationWithData() {
    setupTest();

    JsonObject response =
        GET("/categoryCombos/" + testCatCombo + "?fields=categoryOptionCombos[id]").content();
    JsonList<JsonCategoryOptionCombo> catOptionCombos =
        response.getList("categoryOptionCombos", JsonCategoryOptionCombo.class);
    String categoryOptionComboId = catOptionCombos.get(0).getId();

    String body =
        format(
            "{"
                + "'dataElement':'%s',"
                + "'categoryOptionCombo':'%s',"
                + "'period':'20220102',"
                + "'orgUnit':'%s',"
                + "'value':'24',"
                + "'comment':'OK'}",
            dataElementId, categoryOptionComboId, orgUnitId);

    assertStatus(HttpStatus.CREATED, POST("/dataValues", body));

    // We should not be able to remove a category here
    assertStatus(
        HttpStatus.CONFLICT,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            "{ 'name' : 'COLOR AND TASTE', 'id' : '"
                + testCatCombo
                + "', "
                + "'shortName': 'C_AND_T', 'skipTotals' : true, "
                + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                + "{'id' : '"
                + categoryColor
                + "'} ]} "));
  }

  void setTestCatComboModifiableProperties() {
    // Modify the initial name
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            "{ 'name' : 'COLOR AND TASTE', 'id' : '"
                + testCatCombo
                + "', "
                + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                + "{'id' : '"
                + categoryColor
                + "'} , {'id' : '"
                + categoryTaste
                + "'}]} "));
    // Add a shortname
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            "{ 'name' : 'COLOR AND TASTE', 'id' : '"
                + testCatCombo
                + "', "
                + "'shortName': 'C_AND_T', "
                + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                + "{'id' : '"
                + categoryColor
                + "'} , {'id' : '"
                + categoryTaste
                + "'}]} "));
    // Skip totals
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/categoryCombos/" + testCatCombo + "?mergeMode=REPLACE",
            "{ 'name' : 'COLOR AND TASTE', 'id' : '"
                + testCatCombo
                + "', "
                + "'shortName': 'C_AND_T', 'skipTotals' : true, "
                + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                + "{'id' : '"
                + categoryColor
                + "'} , {'id' : '"
                + categoryTaste
                + "'}]} "));
  }

  void setupTest() {
    String categoryOptionSour =
        assertStatus(
            HttpStatus.CREATED,
            POST("/categoryOptions", "{ 'name': 'Sour', 'shortName': 'Sour' }"));

    String categoryOptionRed =
        assertStatus(
            HttpStatus.CREATED, POST("/categoryOptions", "{ 'name': 'Red', 'shortName': 'Red' }"));

    categoryColor =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name': 'Color', 'shortName': 'Color', 'dataDimensionType': 'DISAGGREGATION' ,"
                    + "'categoryOptions' : [{'id' : '"
                    + categoryOptionRed
                    + "'} ] }"));

    categoryTaste =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categories",
                "{ 'name': 'Taste', 'shortName': 'Taste', 'dataDimensionType': 'DISAGGREGATION' ,"
                    + "'categoryOptions' : [{'id' : '"
                    + categoryOptionSour
                    + "'} ] }"));

    testCatCombo =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/categoryCombos",
                "{ 'name' : 'Taste and color', "
                    + "'dataDimensionType' : 'DISAGGREGATION', 'categories' : ["
                    + "{'id' : '"
                    + categoryColor
                    + "'} , {'id' : '"
                    + categoryTaste
                    + "'}]} "));

    orgUnitId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/organisationUnits/",
                "{'name':'My Unit', 'shortName':'OU1', 'openingDate': '2020-01-01'}"));
    assertStatus(
        HttpStatus.OK,
        POST(
            "/users/{id}/organisationUnits",
            getCurrentUser().getUid(),
            Body("{'additions':[{'id':'" + orgUnitId + "'}]}")));

    dataElementId = addDataElement("My data element", "DE1", ValueType.INTEGER, null, testCatCombo);
  }
}
