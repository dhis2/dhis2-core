/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hisp.dhis.web.HttpStatus.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.webapi.DhisControllerConvenienceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ValidationNotificationTemplateControllerTest extends DhisControllerConvenienceTest {

  @Test
  @DisplayName(
      "Patching a ValidationNotificationTemplate with ValidationRules and org unit levels succeeds")
  void patchTemplateTest() {
    // Given a ValidationNotificationTemplate exists with ValidationRules, which has org unit levels
    assertWebMessage(OK, POST("/metadata", importMetadata()));

    // When a patch request is submitted to update the name
    assertWebMessage(
        OK,
        PATCH(
            "/validationNotificationTemplates/VntUid00001",
            """
            [
                {
                    "op": "replace",
                    "path": "/name",
                    "value": "Test template - new name"
                }
            ]
            """));

    // Then the name should be updated
    JsonMixed response = GET("/validationNotificationTemplates/VntUid00001").content(OK);
    assertEquals("Test template - new name", response.getString("name").string());

    // And ValidationRules are present
    assertEquals(
        "ValRuleUID1",
        response.getArray("validationRules").get(0).asObject().getString("id").string());
  }

  private String importMetadata() {
    return """
        {
          "validationNotificationTemplates": [
            {
              "id": "VntUid00001",
              "name": "Test template",
              "validationRules": [
                {
                  "id": "ValRuleUID1"
                }
              ]
            }
          ],
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
