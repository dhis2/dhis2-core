package org.hisp.dhis.webapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.hisp.dhis.http.HttpStatus;
import org.hisp.dhis.test.webapi.PostgresControllerIntegrationTestBase;
import org.hisp.dhis.test.webapi.json.domain.JsonWebMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataSetCompletionControllerTest extends PostgresControllerIntegrationTestBase {

  private static final String DE = "DeUID990011";
  private static final String OU = "OuUID990011";
  private static final String DS = "DsUID990011";

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
        .formatted(DE, OU, DS);
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
        .formatted(OU);
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
        .formatted(DS, OU);
  }
}
