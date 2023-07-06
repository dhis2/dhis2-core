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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.hisp.dhis.datavalue.DataValue;
import org.hisp.dhis.datavalue.DataValueService;
import org.hisp.dhis.feedback.ErrorCode;
import org.hisp.dhis.web.HttpMethod;
import org.hisp.dhis.web.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Test for the {@link org.hisp.dhis.webapi.controller.datavalue.DataValueController}.
 *
 * @author Jan Bernitt
 */
class DataValueControllerTest extends AbstractDataValueControllerTest {
  @Autowired private DataValueService dataValueService;

  @Test
  void testSetDataValuesFollowUp_Empty() {
    assertEquals(
        ErrorCode.E2033,
        PUT("/dataValues/followups", Body("{}")).error(HttpStatus.CONFLICT).getErrorCode());
    assertEquals(
        ErrorCode.E2033,
        PUT("/dataValues/followups", Body("{'values':null}"))
            .error(HttpStatus.CONFLICT)
            .getErrorCode());
    assertEquals(
        ErrorCode.E2033,
        PUT("/dataValues/followups", Body("{'values':[]}"))
            .error(HttpStatus.CONFLICT)
            .getErrorCode());
  }

  @Test
  void testSetDataValuesFollowUp_NonExisting() {
    addDataValue("2021-01", "2", null, false);
    assertEquals(
        ErrorCode.E2032,
        PUT(
                "/dataValues/followups",
                Body(format("{'values':[%s]}", dataValueKeyJSON("2021-02", true))))
            .error(HttpStatus.CONFLICT)
            .getErrorCode());
  }

  @Test
  void testSetDataValuesFollowUp_Single() {
    addDataValue("2021-01", "2", null, false);
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/dataValues/followups",
            Body(format("{'values':[%s]}", dataValueKeyJSON("2021-01", true)))));
    assertFollowups(true);
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/dataValues/followups",
            Body(format("{'values':[%s]}", dataValueKeyJSON("2021-01", false)))));
    assertFollowups(false);
  }

  @Test
  void testSetDataValuesFollowUp_Multi() {
    addDataValue("2021-01", "2", null, false);
    addDataValue("2021-02", "3", null, false);
    addDataValue("2021-03", "4", null, false);
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/dataValues/followups",
            Body(
                format(
                    "{'values':[%s, %s, %s]}",
                    dataValueKeyJSON("2021-01", true),
                    dataValueKeyJSON("2021-02", true),
                    dataValueKeyJSON("2021-03", true)))));
    assertFollowups(true, true, true);
    assertStatus(
        HttpStatus.OK,
        PUT(
            "/dataValues/followups",
            Body(
                format(
                    "{'values':[%s, %s, %s]}",
                    dataValueKeyJSON("2021-01", false),
                    dataValueKeyJSON("2021-02", true),
                    dataValueKeyJSON("2021-03", false)))));
    assertFollowups(false, true, false);
  }

  @Test
  public void testAddDataValueWithBody() {
    String body =
        format(
            "{"
                + "'dataElement':'%s',"
                + "'categoryOptionCombo':'%s',"
                + "'period':'202201',"
                + "'orgUnit':'%s',"
                + "'value':'24',"
                + "'comment':'OK'}",
            dataElementId, categoryOptionComboId, orgUnitId);

    HttpResponse response = POST("/dataValues", body);
    assertStatus(HttpStatus.CREATED, response);
  }

  /** Check if the dataValueSet endpoint return correct fileName. */
  @Test
  void testGetDataValueSetJsonWithAttachment() {
    String dsId =
        assertStatus(
            HttpStatus.CREATED,
            POST(
                "/dataSets/",
                "{'name':'My data set', 'periodType':'Monthly', 'dataSetElements':[{'dataElement':{'id':'"
                    + dataElementId
                    + "'}}]}"));

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

    HttpResponse response = POST("/dataValues", body);
    assertStatus(HttpStatus.CREATED, response);
    switchToUserWithOrgUnitDataView("A", orgUnitId);
    String url =
        "/dataValueSets?orgUnit="
            + orgUnitId
            + "&startDate=2022-01-01&endDate=2022-01-30&dataSet="
            + dsId
            + "&format=json&compression=zip&attachment=dataValues.json.zip";
    MvcResult dataValueResponse =
        webRequestWithMvcResult(
            buildMockRequest(HttpMethod.GET, url, Collections.emptyList(), null, null));
    assertTrue(
        dataValueResponse
            .getResponse()
            .getHeader("Content-Disposition")
            .contains("dataValues_2022-01-01_2022-01-30.json.zip"));
  }

  private void assertFollowups(boolean... expected) {
    List<DataValue> values = dataValueService.getAllDataValues();
    assertEquals(expected.length, values.size());
    int expectedTrue = 0;
    int actualTrue = 0;
    for (int i = 0; i < expected.length; i++) {
      expectedTrue += expected[i] ? 1 : 0;
      actualTrue += values.get(i).isFollowup() ? 1 : 0;
    }
    assertEquals(expectedTrue, actualTrue, "Number of values marked for followup does not match");
  }

  private String dataValueKeyJSON(String period, boolean followup) {
    return format(
        "{'dataElement':'%s', 'period':'%s', 'orgUnit':'%s', 'categoryOptionCombo':'%s', 'followup':%b}",
        dataElementId, period, orgUnitId, categoryOptionComboId, followup);
  }
}
