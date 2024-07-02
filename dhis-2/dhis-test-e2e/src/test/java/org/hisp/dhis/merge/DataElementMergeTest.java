/*
 * Copyright (c) 2004-2024, University of Oslo
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
package org.hisp.dhis.merge;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.actions.LoginActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.UserActions;
import org.hisp.dhis.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataElementMergeTest extends ApiTest {

  private RestApiActions dataElementApiActions;
  private RestApiActions datasetApiActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private String sourceUid1;
  private String sourceUid2;
  private String targetUid;

  @BeforeAll
  public void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    dataElementApiActions = new RestApiActions("dataElements");
    datasetApiActions = new RestApiActions("dataSets");
    loginActions.loginAsSuperUser();

    // add user with required merge auth
    userActions.addUserFull(
        "user",
        "auth",
        "userWithMergeAuth",
        "Test1234!",
        "F_DATA_ELEMENT_MERGE",
        "F_DATAELEMENT_DELETE",
        "F_DATAELEMENT_PUBLIC_ADD");
  }

  @BeforeEach
  public void setup() {
    loginActions.loginAsSuperUser();
  }

  @Test
  @DisplayName(
      "Valid DataElement merge completes successfully with all DataElement associations updated")
  void validDataElementMergeTest() {
    // given
    sourceUid1 = setupDataElement("A", "TEXT", "AGGREGATE");
    sourceUid2 = setupDataElement("B", "TEXT", "AGGREGATE");
    targetUid = setupDataElement("C", "TEXT", "AGGREGATE");

    // login as user with merge auth
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a data element merge request is submitted, deleting sources
    ApiResponse response =
        dataElementApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true, "LAST_UPDATED"))
            .validateStatus(200);

    // then a successful response is received and sources are deleted
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("DATA_ELEMENT merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("DATA_ELEMENT"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // and all the following source data element references have been handled appropriately
    // and sources are deleted & target exists
    dataElementApiActions.get(sourceUid1).validateStatus(404);
    dataElementApiActions.get(sourceUid2).validateStatus(404);
    dataElementApiActions.get(targetUid).validateStatus(200);
  }

  @Test
  @DisplayName("DataElement merge fails when dataset db unique key constraint met")
  void dbConstraintTest() {
    // given
    sourceUid1 = setupDataElement("D", "TEXT", "AGGREGATE");
    sourceUid2 = setupDataElement("E", "TEXT", "AGGREGATE");
    targetUid = setupDataElement("F", "TEXT", "AGGREGATE");
    setupDataSet(sourceUid1, sourceUid2, targetUid);

    // login as user with merge auth
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when
    ApiResponse response =
        dataElementApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, false, "LAST_UPDATED"))
            .validateStatus(409);

    // then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("message", containsString("ERROR: duplicate key value violates unique constraint"))
        .body("message", containsString("datasetelement_unique_key"))
        .body("message", containsString("already exists"));
  }

  @Test
  @DisplayName("Invalid DataElement merge when DataElements have different value types")
  void invalidDataElementMergeValueType() {
    // given
    sourceUid1 = setupDataElement("G", "NUMBER", "AGGREGATE");
    sourceUid2 = setupDataElement("H", "TEXT", "AGGREGATE");
    targetUid = setupDataElement("I", "TEXT", "AGGREGATE");

    // login as user with merge auth
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a data element merge request is submitted, deleting sources
    ApiResponse response =
        dataElementApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true, "LAST_UPDATED"))
            .validateStatus(409);

    // then a successful response is received and sources are deleted
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("response.mergeReport.message", equalTo("DATA_ELEMENT merge has errors"))
        .body(
            "response.mergeReport.mergeErrors.message",
            allOf(
                hasItem(
                    "All source ValueTypes must match target ValueType: `TEXT`. Other ValueTypes found: `[NUMBER]`")))
        .body("response.mergeReport.mergeErrors.errorCode", allOf(hasItem("E1554")));
  }

  @Test
  @DisplayName("Invalid DataElement merge when DataElements have different domain types")
  void invalidDataElementMergeDomainType() {
    // given
    sourceUid1 = setupDataElement("J", "TEXT", "AGGREGATE");
    sourceUid2 = setupDataElement("K", "TEXT", "TRACKER");
    targetUid = setupDataElement("L", "TEXT", "AGGREGATE");

    // login as user with merge auth
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a data element merge request is submitted, deleting sources
    ApiResponse response =
        dataElementApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true, "LAST_UPDATED"))
            .validateStatus(409);

    // then a successful response is received and sources are deleted
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("response.mergeReport.message", equalTo("DATA_ELEMENT merge has errors"))
        .body(
            "response.mergeReport.mergeErrors.message",
            allOf(
                hasItem(
                    "All source DataElementDomains must match target DataElementDomain: `AGGREGATE`. Other DataElementDomains found: `[TRACKER]`")))
        .body("response.mergeReport.mergeErrors.errorCode", allOf(hasItem("E1555")));
  }

  @Test
  @DisplayName("DataElement merge fails when user has not got the required authority")
  void testDataElementMergeNoRequiredAuth() {
    userActions.addUserFull("basic", "User", "basicUser", "Test1234!", "NO_AUTH");
    loginActions.loginAsUser("basicUser", "Test1234!");

    // when
    ApiResponse response =
        dataElementApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true, "LAST_UPDATED"))
            .validateStatus(403);

    // then
    response
        .validate()
        .statusCode(403)
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo("Access is denied, requires one Authority from [F_DATA_ELEMENT_MERGE]"));
  }

  private String setupDataElement(String uniqueChar, String valueType, String domainType) {
    return dataElementApiActions
        .post(createDataElement("source 1" + uniqueChar, valueType, domainType))
        .validateStatus(201)
        .extractUid();
  }

  private void setupDataSet(String sourceUid1, String sourceUid2, String targetUid) {
    datasetApiActions.post(createDataset(sourceUid1, sourceUid2, targetUid)).extractUid();
  }

  private JsonObject getMergeBody(
      String source1, String source2, String target, boolean deleteSources, String mergeStrategy) {
    JsonObject json = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add(source1);
    sources.add(source2);
    json.add("sources", sources);
    json.addProperty("target", target);
    json.addProperty("deleteSources", deleteSources);
    json.addProperty("dataMergeStrategy", mergeStrategy);
    return json;
  }

  private String createDataElement(String name, String valueType, String domainType) {
    return """
      {
           "aggregationType": "DEFAULT",
           "domainType": "%s",
           "name": "%s",
           "shortName": "%s",
           "displayName": "%s",
           "valueType": "%s"
       }
    """
        .formatted(domainType, name, name, name, valueType);
  }

  private String createDataset(String dataEl1, String dataEl2, String dataEl3) {
    return """
      {
        "name": "ds1",
        "shortName": "ds1",
        "periodType": "Daily",
        "dataSetElements": [
          {
              "dataElement": {
                  "id": "%s"
              }
          },
          {
              "dataElement": {
                  "id": "%s"
              }
          },
          {
              "dataElement": {
                  "id": "%s"
              }
          }
        ]
      }
    """
        .formatted(dataEl1, dataEl2, dataEl3);
  }
}
