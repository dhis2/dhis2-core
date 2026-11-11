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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * Imports JSON data values the way 2.42 clients sent them (DHIS2-22466): JSON numbers and booleans
 * for values, numeric periods and the {@code followup} spelling. Up to 2.42 the input was mapped
 * with Jackson which accepted all of these.
 *
 * @author Jason P. Pickering <jason@dhis2.org>
 */
class DataValueSetJsonImportControllerTest extends AbstractDataValueControllerTest {

  @Test
  void testValueAsNumber() {
    JsonObject response = importJson("\"period\": \"202201\", \"value\": 5");
    assertStored(response);
    assertEquals("5", getValue("202201").getString("value").string());
  }

  @Test
  void testPeriodAsNumber() {
    JsonObject response = importJson("\"period\": 202202, \"value\": \"6\"");
    assertStored(response);
    assertEquals("6", getValue("202202").getString("value").string());
  }

  /** Value type validation still applies to the text of a JSON number */
  @Test
  void testValueAsNumberStillValidated() {
    JsonObject asNumber =
        POST("/dataValueSets", body("\"period\": \"202203\", \"value\": 5.5"))
            .content(HttpStatus.CONFLICT);
    JsonObject asString =
        POST("/dataValueSets", body("\"period\": \"202203\", \"value\": \"5.5\""))
            .content(HttpStatus.CONFLICT);
    assertEquals(
        asString.getArray("response.conflicts").toJson(),
        asNumber.getArray("response.conflicts").toJson());
  }

  /** 2.42 clients and the 2.43 JSON export use {@code followup} */
  @Test
  void testFollowupLowerCase() {
    importJson("\"period\": \"202204\", \"value\": 7, \"followup\": true");
    assertEquals(true, getValue("202204").getBoolean("followup").booleanValue());
  }

  /** 2.43 counts an upsert of a new value as updated */
  private static void assertStored(JsonObject response) {
    JsonObject count = response.getObject("response.importCount");
    assertEquals(
        1,
        count.getNumber("imported").intValue() + count.getNumber("updated").intValue(),
        count.toJson());
  }

  private JsonObject importJson(String properties) {
    return POST("/dataValueSets", body(properties)).content(HttpStatus.OK);
  }

  private String body(String properties) {
    return """
        {"dataValues":[{"dataElement":"%s","orgUnit":"%s",%s}]}"""
        .formatted(dataElementId, orgUnitId, properties);
  }

  private JsonObject getValue(String period) {
    return GET("/dataValueSets?dataSet={ds}&period={pe}&orgUnit={ou}", dataSetId, period, orgUnitId)
        .content(HttpStatus.OK)
        .getArray("dataValues")
        .getObject(0);
  }
}
