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
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DataElementMergeTest extends ApiTest {

  private RestApiActions dataElementApiActions;
  private RestApiActions datasetApiActions;
  private MetadataActions metadataApiActions;
  private RestApiActions minMaxActions;
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
    metadataApiActions = new MetadataActions();
    minMaxActions = new RestApiActions("minMaxDataElements");
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
      "Valid DataElement merge completes successfully with all source DataElement refs replaced with target DataElement")
  void validDataElementMergeTest() {
    // given
    sourceUid1 = setupDataElement("q", "TEXT", "AGGREGATE");
    sourceUid2 = setupDataElement("r", "TEXT", "AGGREGATE");
    targetUid = setupDataElement("s", "TEXT", "AGGREGATE");

    // add more metadata with source De refs

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
        .body("response.mergeReport.message", equalTo("DataElement merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("DataElement"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // and all the following source data element references have been handled appropriately
    // and sources are deleted & target exists
    dataElementApiActions.get(sourceUid1).validateStatus(404);
    dataElementApiActions.get(sourceUid2).validateStatus(404);
    dataElementApiActions.get(targetUid).validateStatus(200);
  }

  @Test
  @Disabled(
      "setup started failing on GitHub only 409 response, reason not known, e2e all passing locally")
  @DisplayName("DataElement merge fails when min max DE DB unique key constraint met")
  void dbConstraintMinMaxTest() {
    // given
    sourceUid1 = setupDataElement("9", "TEXT", "AGGREGATE");
    sourceUid2 = setupDataElement("8", "TEXT", "AGGREGATE");
    targetUid = setupDataElement("7", "TEXT", "AGGREGATE");
    setupMinMaxDataElements(sourceUid1, sourceUid2, targetUid);

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
        .body("message", containsString("minmaxdataelement_unique_key"));
  }

  @Test
  @DisplayName("DataElement merge fails when dataset DB unique key constraint met")
  void dbConstraintDataSetTest() {
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
        .body("message", containsString("datasetelement_unique_key"));
  }

  @Test
  @DisplayName("DataElement merge fails when ProgramStageDataElement DB unique key constraint met")
  void dbConstraintPsdeTest() {
    // given
    sourceUid1 = setupDataElement("1", "TEXT", "AGGREGATE");
    sourceUid2 = setupDataElement("2", "TEXT", "AGGREGATE");
    targetUid = setupDataElement("3", "TEXT", "AGGREGATE");
    setupProgramStageDataElements(sourceUid1, sourceUid2, targetUid);

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
        .body("message", containsString("programstagedataelement_unique_key"));
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
        .body("response.mergeReport.message", equalTo("DataElement merge has errors"))
        .body(
            "response.mergeReport.mergeErrors.message",
            allOf(
                hasItem(
                    "All source ValueTypes must match target ValueType: `TEXT`. Other ValueTypes found: `[NUMBER]`")))
        .body("response.mergeReport.mergeErrors.errorCode", allOf(hasItem("E1550")));
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
        .body("response.mergeReport.message", equalTo("DataElement merge has errors"))
        .body(
            "response.mergeReport.mergeErrors.message",
            allOf(
                hasItem(
                    "All source DataElementDomains must match target DataElementDomain: `AGGREGATE`. Other DataElementDomains found: `[TRACKER]`")))
        .body("response.mergeReport.mergeErrors.errorCode", allOf(hasItem("E1551")));
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

  private void setupProgramStageDataElements(
      String sourceUid1, String sourceUid2, String targetUid) {
    metadataApiActions
        .importMetadata(programWithStageAndDataElements(sourceUid1, sourceUid2, targetUid))
        .validateStatus(200);
  }

  private void setupMinMaxDataElements(String sourceUid1, String sourceUid2, String targetUid) {
    metadataApiActions.importMetadata(metadata()).validateStatus(200);
    minMaxActions.post(minMaxDataElements("OrgUnit0Z91", sourceUid1, "CatOptComZ3"));
    minMaxActions.post(minMaxDataElements("OrgUnit0Z91", sourceUid2, "CatOptComZ3"));
    minMaxActions.post(minMaxDataElements("OrgUnit0Z91", targetUid, "CatOptComZ3"));
  }

  private String programWithStageAndDataElements(
      String sourceUid1, String sourceUid2, String targetUid) {
    return """
    {
        "programs": [
            {
                "name": "test program 1",
                "shortName": "test program 1",
                "programType": "WITH_REGISTRATION",
                "organisationUnits": [
                    {
                        "id": "OrgunitZ091"
                    }
                ],
                "programStages": [
                    {
                        "id": "ProgStage91"
                    }
                ]
            }
        ],
        "programStages": [
            {
                "id": "ProgStage91",
                "name": "test programStage 1",
                "programStageDataElements": [
                    {
                        "name": "test psde 1",
                        "dataElement": {
                            "id": "%s"
                        }
                    },
                    {
                        "name": "test psde 2",
                        "dataElement": {
                            "id": "%s"
                        }
                    },
                    {
                        "name": "test psde 3",
                        "dataElement": {
                            "id": "%s"
                        }
                    }
                ]
            }
        ],
        "dataElements": [
            {
                "name": "DataElement 1",
                "shortName": "DataElement 1",
                "aggregationType": "SUM",
                "id": "%s",
                "valueType": "TEXT",
                "domainType": "AGGREGATE"
            },
            {
                "name": "DataElement 2",
                "shortName": "DataElement 2",
                "aggregationType": "SUM",
                "id": "%s",
                "valueType": "TEXT",
                "domainType": "AGGREGATE"
            },
            {
                "name": "DataElement 3",
                "shortName": "DataElement 3",
                "aggregationType": "SUM",
                "id": "%s",
                "valueType": "TEXT",
                "domainType": "AGGREGATE"
            }
        ],
        "organisationUnits": [
            {
                "shortName": "Country1",
                "openingDate": "2023-06-16",
                "id": "OrgunitZ091",
                "description": "descript",
                "name": "Country1"
            }
        ]
    }
    """
        .formatted(sourceUid1, sourceUid2, targetUid, sourceUid1, sourceUid2, targetUid);
  }

  private String metadata() {
    return """
    {
          "organisationUnits": [
             {
                 "id": "OrgUnit0Z91",
                 "name": "test org 1",
                 "shortName": "test org 1",
                 "openingDate": "2023-06-15T23:00:00.000Z"
             }
         ],
         "categoryCombos": [
             {
                 "id": "CatComboZ01",
                 "name": "cat combo 1",
                 "dataDimensionType": "DISAGGREGATION"
             }
         ],
         "categoryOptions": [
             {
                 "id": "CatOptZZ001",
                 "name": "cat opt 1",
                 "shortName": "cat opt 1"
             }
         ],
         "categoryOptionCombos": [
             {
                 "id":"CatOptComZ3",
                 "name": "cat option combo 1",
                 "categoryCombo": {
                     "id": "CatComboZ01"
                 },
                 "categoryOptions": [
                     {
                         "id": "CatOptZZ001"
                     }
                 ]
             }
         ]
     }
    """;
  }

  private String minMaxDataElements(String orgUnit, String de, String coc) {
    return """
    {
         "min": 2,
         "max": 11,
         "generated": false,
         "source": {
             "id": "%s"
         },
         "dataElement": {
             "id": "%s"
         },
         "optionCombo": {
             "id": "%s"
         }
     }
    """
        .formatted(orgUnit, de, coc);
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
