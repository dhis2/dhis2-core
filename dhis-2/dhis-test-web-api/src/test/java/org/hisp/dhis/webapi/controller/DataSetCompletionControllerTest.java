package org.hisp.dhis.webapi.controller;

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

  @BeforeEach
  void beforeEach() {
    de = UID.generate().getValue();
    ou = UID.generate().getValue();
    ds = UID.generate().getValue();
  }

  @Test
  @DisplayName("Complete data set not allowed when missing compulsory data element operands")
  void missingCompulsoryDataElementOperandsTest() {
    // given a data set with compulsory data element operands
    POST("/metadata", metadata()).content(HttpStatus.OK);
    PATCH("/users/%s".formatted(ADMIN_USER_UID), addOrgUnit());

    // when trying to complete the data set that has missing compulsory data
    JsonWebMessage jsonWebMessage =
        POST("/dataEntry/dataSetCompletion", completeDataSetReg())
            .content(HttpStatus.CONFLICT)
            .as(JsonWebMessage.class);

    // then it should fail
    assertEquals(
        "All compulsory data element operands need to be filled: [test-de-1]",
        jsonWebMessage.getMessage());

    // and complete dataset reg should be empty
    JsonMixed cdsr =
        GET("/api/completeDataSetRegistrations?orgUnit=%s&period=202505&dataSet=%s"
                .formatted(ou, ds))
            .content(HttpStatus.OK);

    assertTrue(cdsr.isEmpty());
  }

  @Test
  @DisplayName("Complete data set is allowed when no compulsory data element operands")
  void noCompulsoryDataElementOperandsTest() {
    // given a data set with no compulsory data element operands
    POST("/metadata", metadataWithoutCompulsory()).content(HttpStatus.OK);
    PATCH("/users/%s".formatted(ADMIN_USER_UID), addOrgUnit());

    // when trying to complete the data set with no compulsory data, it should succeed
    assertEquals(
        HttpStatus.OK, POST("/dataEntry/dataSetCompletion", completeDataSetReg()).status());

    // and complete dataset reg should have 1 entry
    JsonMixed cdsr =
        GET("/api/completeDataSetRegistrations?orgUnit=%s&period=202505&dataSet=%s"
                .formatted(ou, ds))
            .content(HttpStatus.OK);

    assertEquals(
        ds,
        cdsr.getArray("completeDataSetRegistrations")
            .get(0)
            .asObject()
            .getString("dataSet")
            .string());
  }

  private String metadata() {
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
              "compulsoryFieldsCompleteOnly": false,
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
              ],
              "compulsoryDataElementOperands": [
                {
                  "dataElement": {
                      "id": "%1$s"
                  },
                  "categoryOptionCombo": {
                      "id": "HllvX50cXC0"
                  }
                }
              ]
            }
          ]
        }
        """
        .formatted(de, ou, ds);
  }

  private String metadataWithoutCompulsory() {
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
              "compulsoryFieldsCompleteOnly": true,
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
              ]
            }
          ]
        }
        """
        .formatted(de, ou, ds);
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
            "period": "202505",
            "orgUnit": "%s",
            "attribute": {},
            "completed": true
        }
        """
        .formatted(ds, ou);
  }
}
