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
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.test.webapi.H2ControllerIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests the {@link org.hisp.dhis.webapi.controller.validation.ValidationRuleController} using
 * (mocked) REST requests.
 */
@Transactional
class ValidationRuleControllerTest extends H2ControllerIntegrationTestBase {

  @Test
  void testGetExpressionDescription() {
    assertWebMessage(
        "OK",
        200,
        "OK",
        "Valid",
        POST("/validationRules/expression/description", "70").content(HttpStatus.OK));
  }

  @Test
  void testGetExpressionDescription_MalformedExpression() {
    assertWebMessage(
        "OK",
        200,
        "ERROR",
        "Expression is not well-formed",
        POST("/validationRules/expression/description", "illegal").content(HttpStatus.OK));
  }

  @Test
  void patchValidationRuleTest() {
    // Given a ValidationValidationRule exists with org unit levels
    assertWebMessage(HttpStatus.OK, POST("/metadata", importMetadata()));

    // When a patch request is submitted to update the name
    assertWebMessage(
        HttpStatus.OK,
        PATCH(
            "/validationRules/ValRuleUID1",
            """
            [
                {
                    "op": "replace",
                    "path": "/name",
                    "value": "test val rule 1 - new name"
                }
            ]
            """));

    // Then the name should be updated
    JsonMixed response = GET("/validationRules/ValRuleUID1").content(HttpStatus.OK);
    assertEquals("test val rule 1 - new name", response.getString("name").string());
  }

  private String importMetadata() {
    return """
        {
          "validationRules": [
            {
              "id": "ValRuleUID1",
              "name": "test val rule 1",
              "organisationUnitLevels": [1, 2],
              "leftSide": {
                "expression": "1 + 1",
                "description": "rule 1"
              },
              "rightSide": {
                "expression": "2 + 2",
                "description": "rule 2"
              },
              "operator": "less_than_or_equal_to",
              "periodType": "Monthly"
            }
          ]
        }
        """;
  }
}
