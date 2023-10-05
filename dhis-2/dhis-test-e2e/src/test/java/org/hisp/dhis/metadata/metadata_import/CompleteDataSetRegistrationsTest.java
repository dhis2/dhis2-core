/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.metadata.metadata_import;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.CompleteDataSetRegistrationActions;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.SystemActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author david mackessy
 */
class CompleteDataSetRegistrationsTest extends ApiTest {
  private CompleteDataSetRegistrationActions apiActions;
  private LoginActions loginActions;
  private SystemActions systemActions;
  private RestApiActions dataSetActions;

  @BeforeAll
  public void before() {
    loginActions = new LoginActions();
    apiActions = new CompleteDataSetRegistrationActions();
    systemActions = new SystemActions();
    dataSetActions = new RestApiActions("dataSets");
  }

  @Test
  void completeDataSetRegistrationsAsync() {
    loginActions.loginAsSuperUser();

    // create data set
    String dataSet = dataSetWithOrgUnit(1, "O6uvpzGd5pu");
    ApiResponse dataSetResponse = dataSetActions.post(dataSet);

    assertEquals(201, dataSetResponse.statusCode());
    String dataSetId = dataSetResponse.extractUid();
    assertEquals(11, dataSetId.length());

    // get complete data sets to show none complete with given criteria
    ApiResponse completedResponse = apiActions.getCompleted(dataSetId, "O6uvpzGd5pu", "202301");
    assertEquals("{}", completedResponse.getAsString());

    // complete the data set and post async
    String cds = completeDataSet(dataSetId, "202301", "O6uvpzGd5pu");
    ApiResponse completeAsyncResponse = apiActions.sendAsync(cds);
    assertEquals(200, completeAsyncResponse.statusCode());

    assertTrue(
        completeAsyncResponse
            .getBody()
            .get("message")
            .getAsString()
            .contains("Initiated COMPLETE_DATA_SET_REGISTRATION_IMPORT"));

    String taskId =
        completeAsyncResponse.getBody().getAsJsonObject("response").get("id").getAsString();
    assertEquals(11, taskId.length());

    // wait for job to be completed (24 seconds used as the job schedule loop is 20 seconds)
    ApiResponse taskStatus =
        systemActions.waitUntilTaskCompleted("COMPLETE_DATA_SET_REGISTRATION_IMPORT", taskId, 24);
    assertTrue(taskStatus.getAsString().contains("\"completed\":true"));

    // get complete data sets which should be 1 now
    ApiResponse completedResponse2 = apiActions.getCompleted(dataSetId, "O6uvpzGd5pu", "202301");

    // validate async-completed data set
    completedResponse2
        .validate()
        .statusCode(200)
        .body("completeDataSetRegistrations[0].period", equalTo("202301"))
        .body("completeDataSetRegistrations[0].dataSet", equalTo(dataSetId))
        .body("completeDataSetRegistrations[0].organisationUnit", equalTo("O6uvpzGd5pu"))
        .body("completeDataSetRegistrations[0].completed", equalTo(true))
        .body("completeDataSetRegistrations[0].storedBy", equalTo("tasuperadmin"));
  }

  @Test
  void completeDataSetRegistrationSync() {
    loginActions.loginAsSuperUser();

    // create data set
    String dataSet = dataSetWithOrgUnit(2, "g8upMTyEZGZ");
    ApiResponse dataSetResponse = dataSetActions.post(dataSet);

    assertEquals(201, dataSetResponse.statusCode());
    String dataSetId = dataSetResponse.extractUid();
    assertEquals(11, dataSetId.length());

    // get complete data sets to confirm none completed with given criteria
    ApiResponse completedResponse = apiActions.getCompleted(dataSetId, "g8upMTyEZGZ", "202301");
    assertEquals("{}", completedResponse.getAsString());

    // complete the data set and post sync
    String cds = completeDataSet(dataSetId, "202301", "g8upMTyEZGZ");
    ApiResponse completeSyncResponse = apiActions.sendSync(cds);

    completeSyncResponse
        .validate()
        .statusCode(200)
        .body("message", equalTo("Import was successful."))
        .body("response.status", equalTo("SUCCESS"))
        .body("response.importCount.imported", equalTo(1));

    // get complete data sets which should be 1 now
    ApiResponse completedResponse2 = apiActions.getCompleted(dataSetId, "g8upMTyEZGZ", "202301");

    // validate sync-completed data set
    completedResponse2
        .validate()
        .statusCode(200)
        .body("completeDataSetRegistrations[0].period", equalTo("202301"))
        .body("completeDataSetRegistrations[0].dataSet", equalTo(dataSetId))
        .body("completeDataSetRegistrations[0].organisationUnit", equalTo("g8upMTyEZGZ"))
        .body("completeDataSetRegistrations[0].completed", equalTo(true))
        .body("completeDataSetRegistrations[0].storedBy", equalTo("tasuperadmin"));
  }

  @Test
  void getCompleteDataSetRegistrationWithIdScheme() {
    loginActions.loginAsSuperUser();

    // create data set
    String dataSet = dataSetWithOrgUnit(3, "g8upMTyEZGZ");
    ApiResponse dataSetResponse = dataSetActions.post(dataSet);

    assertEquals(201, dataSetResponse.statusCode());
    String dataSetId = dataSetResponse.extractUid();

    // complete the data set and post sync
    String cds = completeDataSet(dataSetId, "202301", "g8upMTyEZGZ");
    ApiResponse completeSyncResponse = apiActions.sendSync(cds);

    completeSyncResponse
        .validate()
        .statusCode(200)
        .body("message", equalTo("Import was successful."))
        .body("response.status", equalTo("SUCCESS"))
        .body("response.importCount.imported", equalTo(1));

    // get complete data sets with id scheme CODE
    ApiResponse completedResponse2 = apiActions.getCompletedWithIdScheme(dataSetId, "g8upMTyEZGZ", "202301", "CODE");

    // validate sync-completed data set returns CODEs for id scheme
    completedResponse2
        .validate()
        .statusCode(200)
        .body("completeDataSetRegistrations[0].dataSet", equalTo("TEST_CODE 3"))
        .body("completeDataSetRegistrations[0].organisationUnit", equalTo("OU_167609"));
  }

  private String dataSetWithOrgUnit(int uniqueNum, String orgUnit) {
    return """
        {
          "name": "test ds %d",
          "shortName": "test ds %d",
          "code": "TEST_CODE %d",
          "periodType": "Daily",
          "organisationUnits": [
            {
              "id": "%s"
            }
          ]
        }
        """
        .formatted(uniqueNum, uniqueNum, uniqueNum, orgUnit);
  }

  private String completeDataSet(String dataSet, String period, String orgUnit) {
    return """
        {
          "completeDataSetRegistrations": [
            {
              "dataSet": "%s",
              "period": "%s",
              "organisationUnit": "%s",
              "completed": "true"
            }
          ]
        }
        """
        .formatted(dataSet, period, orgUnit);
  }
}
