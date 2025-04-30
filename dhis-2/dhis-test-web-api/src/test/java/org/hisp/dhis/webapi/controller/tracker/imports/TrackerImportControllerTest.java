/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.webapi.controller.tracker.imports;

import static org.hisp.dhis.http.HttpClientAdapter.Body;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.webapi.Assertions.assertNoDiff;
import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.dxf2.webmessage.responses.TrackerJobWebMessageResponse;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.tracker.imports.TrackerImportController} using
 * (mocked) REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class TrackerImportControllerTest extends PostgresControllerIntegrationTestBase {
  @Test
  void shouldSucceedWhenAllValidParametersArePassed() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Tracker job added",
        POST(
                "/tracker?async=true&reportMode=FULL"
                    + "&importMode=VALIDATE"
                    + "&idScheme=ATTRIBUTE:abcdefghilm"
                    + "&importStrategy=CREATE_AND_UPDATE"
                    + "&atomicMode=OBJECT"
                    + "&flushMode=AUTO"
                    + "&validationMode=FULL",
                "{}")
            .content(HttpStatus.OK));
  }

  @Test
  void shouldImportAsyncByDefault() {
    JsonWebMessage jsonWebMessage =
        assertWebMessage(
            "OK",
            200,
            "OK",
            "Tracker job added",
            POST("/tracker?importMode=VALIDATE", "{}").content(HttpStatus.OK));

    JsonObject response = jsonWebMessage.getResponse();
    String location = response.getString("location").string();
    assertContains("/tracker/jobs/", location);
    String jobId = location.substring(location.lastIndexOf('/') + 1);
    assertEquals(jobId, response.getString("id").string());
    assertEquals(
        TrackerJobWebMessageResponse.class.getSimpleName(),
        response.getString("responseType").string());
  }

  @Test
  void shouldImportAsync() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Tracker job added",
        POST("/tracker?async=true&importMode=VALIDATE", "{}").content(HttpStatus.OK));
  }

  @ParameterizedTest
  @ValueSource(strings = {"false", "FALSE", "FaLSE"})
  void shouldImportSync(String async) {
    JsonMixed body =
        POST("/tracker?async={async}&importMode=VALIDATE", async, Body("{}"))
            .content(HttpStatus.OK);

    assertNoDiff(
"""
{
  "status": "OK",
  "validationReport": {
    "errorReports": [],
    "warningReports": []
  },
  "stats": {
    "created": 0,
    "updated": 0,
    "deleted": 0,
    "ignored": 0,
    "total": 0
  },
  "bundleReport": {
    "typeReportMap": {}
  }
}
""",
        body);
  }

  @Test
  void shouldReturnBadRequestWhenThereIsAnInvalidUidInThePayload() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "JSON parse error: Cannot construct instance of `org.hisp.dhis.common.UID`, problem: UID must be an alphanumeric string of 11 characters starting with a letter, but was: invalid_uid",
        POST(
                "/tracker?async=false",
                """

                    {
                    "trackedEntities": [
                      {
                        "trackedEntity": "invalid_uid",
                        "trackedEntityType": "PrZMWi7rBga",
                        "orgUnit": "PSeMWi7rBgb"
                      }
                    ]
                  }
                  """)
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenEmptyIdSchemeArePassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "idScheme cannot be empty. Valid values are: [UID, CODE, NAME, ATTRIBUTE:attributeUid]",
        POST("/tracker?idScheme=", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidReportModeIsPassedGettingJobReport() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter reportMode. Valid values are: [FULL, ERRORS, WARNINGS]",
        GET("/tracker/jobs/AAA/report?reportMode=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidReportModeIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter reportMode. Valid values are: [FULL, ERRORS, WARNINGS]",
        POST("/tracker?reportMode=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidIdSchemeIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter idScheme. Valid values are: [UID, CODE, NAME, ATTRIBUTE:attributeUid]",
        POST("/tracker?idScheme=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidAttributeIdSchemeIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'ATTRIBUTE:abc' is not valid for parameter idScheme. Valid values are: [UID, CODE, NAME, ATTRIBUTE:attributeUid]",
        POST("/tracker?idScheme=ATTRIBUTE:abc", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidFormatForAttributeIdSchemeIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'ATTRIBUTE:abcdefghilm:invalid' is not valid for parameter idScheme. Valid values are: [UID, CODE, NAME, ATTRIBUTE:attributeUid]",
        POST("/tracker?idScheme=ATTRIBUTE:abcdefghilm:invalid", "{}")
            .content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidImportModeIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter importMode. Valid values are: [COMMIT, VALIDATE]",
        POST("/tracker?importMode=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidImportStrategyIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter importStrategy. Valid values are: [CREATE, UPDATE, CREATE_AND_UPDATE, DELETE]",
        POST("/tracker?importStrategy=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidAtomicModeIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter atomicMode. Valid values are: [ALL, OBJECT]",
        POST("/tracker?atomicMode=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidFlushModeIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter flushMode. Valid values are: [OBJECT, AUTO]",
        POST("/tracker?flushMode=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidValidationModeIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter validationMode. Valid values are: [FULL, FAIL_FAST, SKIP]",
        POST("/tracker?validationMode=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidAsyncIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter async. It should be of type boolean",
        POST("/tracker?async=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidSkipPatternValidationIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter skipPatternValidation. It should be of type boolean",
        POST("/tracker?skipPatternValidation=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidSkipSideEffectsIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter skipSideEffects. It should be of type boolean",
        POST("/tracker?skipSideEffects=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenInvalidSkipRuleEngineIsPassed() {
    assertWebMessage(
        "Bad Request",
        400,
        "ERROR",
        "Value 'INVALID' is not valid for parameter skipRuleEngine. It should be of type boolean",
        POST("/tracker?skipRuleEngine=INVALID", "{}").content(HttpStatus.BAD_REQUEST));
  }
}
