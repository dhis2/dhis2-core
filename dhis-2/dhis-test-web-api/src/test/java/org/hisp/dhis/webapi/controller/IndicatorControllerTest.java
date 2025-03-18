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
package org.hisp.dhis.webapi.controller;

import static org.hisp.dhis.test.webapi.Assertions.assertWebMessage;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonArray;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.jsontree.JsonObject;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.indicator.IndicatorController} using (mocked)
 * REST requests.
 *
 * @author Jan Bernitt
 */
@Transactional
class IndicatorControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetExpressionDescription() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Valid",
        POST("/indicators/expression/description", "70").content(HttpStatus.OK));
  }

  @Test
  void testGetExpressionDescription_MalformedExpression() {
    assertWebMessage(
        "OK",
        200,
        "ERROR",
        "Expression is not well-formed",
        POST("/indicators/expression/description", "illegal").content(HttpStatus.OK));
  }

  @Test
  @DisplayName("Invalid merge with source and target missing")
  void testInvalidMerge() {
    JsonMixed mergeResponse =
        POST(
                "/indicators/merge",
                """
                {
                    "sources": ["Uid00000010"],
                    "target": "Uid00000012",
                    "deleteSources": true
                }""")
            .content(HttpStatus.CONFLICT);
    assertEquals("Conflict", mergeResponse.getString("httpStatus").string());
    assertEquals("WARNING", mergeResponse.getString("status").string());
    assertEquals(
        "One or more errors occurred, please see full details in merge report.",
        mergeResponse.getString("message").string());

    JsonArray errors =
        mergeResponse.getObject("response").getObject("mergeReport").getArray("mergeErrors");
    JsonObject error1 = errors.getObject(0);
    JsonObject error2 = errors.getObject(1);
    assertEquals(
        "SOURCE Indicator does not exist: `Uid00000010`", error1.getString("message").string());
    assertEquals(
        "TARGET Indicator does not exist: `Uid00000012`", error2.getString("message").string());
  }

  @Test
  @DisplayName("invalid merge, missing required auth")
  void testMergeNoAuth() {
    switchToNewUser("noAuth", "NoAuth");
    JsonMixed mergeResponse =
        POST(
                "/indicators/merge",
                """
        {
            "sources": ["Uid00000010"],
            "target": "Uid00000012",
            "deleteSources": true
        }""")
            .content(HttpStatus.FORBIDDEN);
    assertEquals("Forbidden", mergeResponse.getString("httpStatus").string());
    assertEquals("ERROR", mergeResponse.getString("status").string());
    assertEquals(
        "Access is denied, requires one Authority from [F_INDICATOR_MERGE]",
        mergeResponse.getString("message").string());
  }
}
