/*
 * Copyright (c) 2004-2025, University of Oslo
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

import static org.hisp.dhis.feedback.ErrorCode.E7605;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hisp.dhis.common.UID;
import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.jsontree.JsonMixed;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataSetCompletionControllerTest extends PostgresControllerIntegrationTestBase {

  private String de;
  private String ou;
  private String ds;
  private final String p = "202504";

  @BeforeEach
  void beforeEach() {
    de = UID.generate().getValue();
    ou = UID.generate().getValue();
    ds = UID.generate().getValue();
  }

  @Test
  @DisplayName(
      "Complete data set not allowed when missing compulsory elements and compulsory elements are required")
  void missingCompulsoryDataElementOperandsTest() {
    // given a data set with compulsory data element operands
    boolean compulsoryElementsAreRequired = true;
    POST("/metadata", metadata(compulsoryElementsAreRequired, withCompulsoryElements()))
        .content(HttpStatus.OK);
    PATCH("/users/%s".formatted(ADMIN_USER_UID), addOrgUnit());

    // when trying to complete the data set that has missing compulsory data
    JsonWebMessage jsonWebMessage =
        POST("/dataEntry/dataSetCompletion", completeDataSetReg())
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);

    // then it should fail
    assertEquals(409, jsonWebMessage.getHttpStatusCode());
    assertEquals("Conflict", jsonWebMessage.getHttpStatus());
    assertEquals("ERROR", jsonWebMessage.getStatus());
    assertEquals(E7605, jsonWebMessage.getErrorCode());
    assertEquals(
        "All compulsory data element operands need to be filled: `test-de-1`",
        jsonWebMessage.getMessage());

    // and complete dataset reg should be empty
    JsonMixed cdsr =
        GET("/api/completeDataSetRegistrations?orgUnit=%s&period=%s&dataSet=%s"
                .formatted(ou, p, ds))
            .content(HttpStatus.OK);

    assertTrue(cdsr.isEmpty());
  }

  @Test
  @DisplayName("Complete data set allowed when compulsory elements are required and filled")
  void compulsoryDataElementOperandsFilledTest() {
    // given a data set with compulsory data element operands
    boolean compulsoryElementsAreRequired = true;
    POST("/metadata", metadata(compulsoryElementsAreRequired, withCompulsoryElements()))
        .content(HttpStatus.OK);
    PATCH("/users/%s".formatted(ADMIN_USER_UID), addOrgUnit());
    switchContextToUser(userService.getUser(ADMIN_USER_UID));

    // and data exists for the compulsory data element
    POST("dataValueSets", dataValue()).content(HttpStatus.OK);

    // when trying to complete the data set that has missing compulsory data, it should succeed
    POST("/dataEntry/dataSetCompletion", completeDataSetReg())
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // and complete dataset reg should be present
    JsonMixed cdsr =
        GET("/api/completeDataSetRegistrations?orgUnit=%s&period=%s&dataSet=%s"
                .formatted(ou, p, ds))
            .content(HttpStatus.OK);

    assertEquals(
        ds,
        cdsr.getArray("completeDataSetRegistrations")
            .get(0)
            .asObject()
            .getString("dataSet")
            .string());
  }

  @Test
  @DisplayName(
      "Complete data set allowed when missing compulsory elements and compulsory elements are not required")
  void missingCompulsoryDataElementOperandsNotCompulsoryTest() {
    // given a data set with compulsory data element operands
    boolean compulsoryElementsAreNotRequired = false;
    POST("/metadata", metadata(compulsoryElementsAreNotRequired, withCompulsoryElements()))
        .content(HttpStatus.OK);
    PATCH("/users/%s".formatted(ADMIN_USER_UID), addOrgUnit());

    // when trying to complete the data set that has missing compulsory data, it should succeed
    POST("/dataEntry/dataSetCompletion", completeDataSetReg())
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // and complete dataset reg should be present
    JsonMixed cdsr =
        GET("/api/completeDataSetRegistrations?orgUnit=%s&period=%s&dataSet=%s"
                .formatted(ou, p, ds))
            .content(HttpStatus.OK);

    assertEquals(
        ds,
        cdsr.getArray("completeDataSetRegistrations")
            .get(0)
            .asObject()
            .getString("dataSet")
            .string());
  }

  @Test
  @DisplayName("Complete data set is allowed when no compulsory data element operands")
  void noCompulsoryDataElementOperandsTest() {
    // given a data set with no compulsory data element operands
    POST("/metadata", metadata(true, "")).content(HttpStatus.OK);
    PATCH("/users/%s".formatted(ADMIN_USER_UID), addOrgUnit());

    // when trying to complete the data set with no compulsory data, it should succeed
    POST("/dataEntry/dataSetCompletion", completeDataSetReg())
        .content(HttpStatus.OK)
        .as(JsonWebMessage.class);

    // and complete dataset reg should have 1 entry
    JsonMixed cdsr =
        GET("/api/completeDataSetRegistrations?orgUnit=%s&period=%s&dataSet=%s"
                .formatted(ou, p, ds))
            .content(HttpStatus.OK);

    assertEquals(
        ds,
        cdsr.getArray("completeDataSetRegistrations")
            .get(0)
            .asObject()
            .getString("dataSet")
            .string());
  }

  private String metadata(boolean compulsory, String withCompulsory) {
    return """
        {
          "dataElements": [
            {
              "id": "%1$s",
              "aggregationType": "DEFAULT",
              "domainType": "AGGREGATE",
              "name": "test-de-1",
              "shortName": "test-de-1",
              "valueType": "TEXT",
              "categoryCombo": {
                "id": "bjDvmb4bfuf"
              }
            }
          ],
          "organisationUnits": [
            {
              "id": "%2$s",
              "name": "test-org-1",
              "shortName": "test-org-1",
              "openingDate": "2023-06-15"
            }
          ],
          "dataSets": [
            {
              "id": "%3$s",
              "name": "test-ds-1",
              "shortName": "test-ds-1",
              "periodType": "Monthly",
              "compulsoryFieldsCompleteOnly": %4$b,
              "dataSetElements": [
                {
                  "dataElement": {
                    "id": "%1$s"
                  }
                }
              ],
              "organisationUnits": [
                {
                  "id": "%2$s"
                }
              ]%5$s
            }
          ]
        }
        """
        .formatted(de, ou, ds, compulsory, withCompulsory);
  }

  private String withCompulsoryElements() {
    return """
            ,
            "compulsoryDataElementOperands": [
              {
                "dataElement": {
                    "id": "%1$s"
                },
                "categoryOptionCombo": {
                    "id": "HllvX50cXC0"
                }
              }
            ]"""
        .formatted(de);
  }

  private String addOrgUnit() {
    return """
        [
          {
            "op": "add",
            "path": "/organisationUnits",
            "value": [
              {
                "id": "%s"
              }
            ]
          }
        ]
        """
        .formatted(ou);
  }

  private String completeDataSetReg() {
    return """
        {
            "dataSet": "%s",
            "period": "%s",
            "orgUnit": "%s",
            "attribute": {},
            "completed": true
        }
        """
        .formatted(ds, p, ou);
  }

  private String dataValue() {
    return """
          {
            "dataSet": "%1$s",
            "period": "%4$s",
            "orgUnit": "%2$s",
            "completedDate": "2025-05-25",
            "dataValues": [
              {
                "dataElement": "%3$s",
                "period": "%4$s",
                "orgUnit": "%2$s",
                "value": "2000",
                "followup": false
              }
            ]
          }"""
        .formatted(ds, ou, de, p);
  }
}
