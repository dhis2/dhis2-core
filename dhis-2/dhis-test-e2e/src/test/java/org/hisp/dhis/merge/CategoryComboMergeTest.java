/*
 * Copyright (c) 2004-2026, University of Oslo
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
package org.hisp.dhis.merge;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryComboMergeTest extends ApiTest {

  private RestApiActions categoryComboApiActions;
  private RestApiActions dataElementApiActions;
  private RestApiActions dataSetApiActions;
  private MetadataActions metadataActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private final String sourceUid1 = "CatComboS01";
  private final String sourceUid2 = "CatComboS02";
  private final String targetUid = "CatComboTgt";

  @BeforeAll
  void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    categoryComboApiActions = new RestApiActions("categoryCombos");
    dataElementApiActions = new RestApiActions("dataElements");
    dataSetApiActions = new RestApiActions("dataSets");
    metadataActions = new MetadataActions();
    loginActions.loginAsSuperUser();

    // add user with required merge auth
    userActions.addUserFull(
        "user",
        "auth",
        "userWithCcMergeAuth",
        "Test1234!",
        "F_CATEGORY_COMBO_MERGE",
        "F_CATEGORY_COMBO_DELETE",
        "F_CATEGORY_COMBO_PUBLIC_ADD");
  }

  @BeforeEach
  void setup() {
    loginActions.loginAsSuperUser();
    setupMetadata();
  }

  @Test
  @DisplayName(
      "Valid CategoryCombo merge completes successfully with all source refs replaced with target")
  void validCategoryComboMergeTest() {
    // confirm state before merge - DataElements have source CategoryCombo refs
    dataElementApiActions
        .get("DataElemnt1")
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(sourceUid1));
    dataElementApiActions
        .get("DataElemnt2")
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(sourceUid2));

    // DataSets have source CategoryCombo refs
    dataSetApiActions
        .get("DataSet0001")
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(sourceUid1));
    dataSetApiActions
        .get("DataSet0002")
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(sourceUid2));

    // login as merge user
    loginActions.loginAsUser("userWithCcMergeAuth", "Test1234!");

    // when a CategoryCombo merge request is submitted, deleting sources
    ApiResponse response =
        categoryComboApiActions.post("merge", getMergeBody()).validateStatus(200);

    // then a success response received
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("CategoryCombo merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("CategoryCombo"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // sources are deleted
    categoryComboApiActions.get(sourceUid1).validateStatus(404);
    categoryComboApiActions.get(sourceUid2).validateStatus(404);

    // target still exists
    categoryComboApiActions.get(targetUid).validateStatus(200);

    // DataElements now have target CategoryCombo refs
    dataElementApiActions
        .get("DataElemnt1")
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(targetUid));
    dataElementApiActions
        .get("DataElemnt2")
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(targetUid));

    // DataSets now have target CategoryCombo refs
    dataSetApiActions
        .get("DataSet0001")
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(targetUid));
    dataSetApiActions
        .get("DataSet0002")
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(targetUid));
  }

  @Test
  @DisplayName("CategoryCombo merge fails when user has not got the required authority")
  void testCategoryComboMergeNoRequiredAuth() {
    userActions.addUserFull("basic", "User", "basicCcUser", "Test1234!", "NO_AUTH");
    loginActions.loginAsUser("basicCcUser", "Test1234!");

    // when
    ApiResponse response =
        categoryComboApiActions.post("merge", getMergeBody()).validateStatus(403);

    // then
    response
        .validate()
        .statusCode(403)
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo("Access is denied, requires one Authority from [F_CATEGORY_COMBO_MERGE]"));
  }

  @Test
  @DisplayName("CategoryCombo merge fails when sources and target have different Categories")
  void testCategoryComboMergeDifferentCategories() {
    // login as merge user
    loginActions.loginAsUser("userWithCcMergeAuth", "Test1234!");

    // when trying to merge combos with different categories
    JsonObject body = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add("CatComboDif"); // This has a different category
    body.add("sources", sources);
    body.addProperty("target", targetUid);
    body.addProperty("deleteSources", true);

    ApiResponse response = categoryComboApiActions.post("merge", body).validateStatus(409);

    // then validation error
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("message", equalTo("Merge validation error"));
  }

  private JsonObject getMergeBody() {
    JsonObject json = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add(sourceUid1);
    sources.add(sourceUid2);
    json.add("sources", sources);
    json.addProperty("target", targetUid);
    json.addProperty("deleteSources", true);
    return json;
  }

  private void setupMetadata() {
    metadataActions.importMetadata(metadata()).validateStatus(200);
  }

  private String metadata() {
    return """
        {
            "categoryOptions": [
                {
                    "id": "CatOpt00001",
                    "name": "cat option 1",
                    "shortName": "cat option 1"
                },
                {
                    "id": "CatOpt00002",
                    "name": "cat option 2",
                    "shortName": "cat option 2"
                },
                {
                    "id": "CatOptDiff1",
                    "name": "different cat option",
                    "shortName": "different cat option"
                }
            ],
            "categories": [
                {
                    "id": "Category001",
                    "name": "shared category",
                    "shortName": "shared category",
                    "dataDimensionType": "DISAGGREGATION",
                    "categoryOptions": [
                        {
                            "id": "CatOpt00001"
                        },
                        {
                            "id": "CatOpt00002"
                        }
                    ]
                },
                {
                    "id": "CategoryDif",
                    "name": "different category",
                    "shortName": "different category",
                    "dataDimensionType": "DISAGGREGATION",
                    "categoryOptions": [
                        {
                            "id": "CatOptDiff1"
                        }
                    ]
                }
            ],
            "categoryCombos": [
                {
                    "id": "CatComboS01",
                    "name": "source combo 1",
                    "dataDimensionType": "DISAGGREGATION",
                    "categories": [
                        {
                            "id": "Category001"
                        }
                    ]
                },
                {
                    "id": "CatComboS02",
                    "name": "source combo 2",
                    "dataDimensionType": "DISAGGREGATION",
                    "categories": [
                        {
                            "id": "Category001"
                        }
                    ]
                },
                {
                    "id": "CatComboTgt",
                    "name": "target combo",
                    "dataDimensionType": "DISAGGREGATION",
                    "categories": [
                        {
                            "id": "Category001"
                        }
                    ]
                },
                {
                    "id": "CatComboDif",
                    "name": "different combo",
                    "dataDimensionType": "DISAGGREGATION",
                    "categories": [
                        {
                            "id": "CategoryDif"
                        }
                    ]
                }
            ],
            "organisationUnits": [
                {
                    "id": "OrgUnit0001",
                    "name": "org unit 1",
                    "shortName": "org 1",
                    "openingDate": "2023-06-15"
                }
            ],
            "dataElements": [
                {
                    "id": "DataElemnt1",
                    "name": "data element 1",
                    "shortName": "de 1",
                    "aggregationType": "SUM",
                    "valueType": "NUMBER",
                    "domainType": "AGGREGATE",
                    "categoryCombo": {
                        "id": "CatComboS01"
                    }
                },
                {
                    "id": "DataElemnt2",
                    "name": "data element 2",
                    "shortName": "de 2",
                    "aggregationType": "SUM",
                    "valueType": "NUMBER",
                    "domainType": "AGGREGATE",
                    "categoryCombo": {
                        "id": "CatComboS02"
                    }
                }
            ],
            "dataSets": [
                {
                    "id": "DataSet0001",
                    "name": "data set 1",
                    "shortName": "ds 1",
                    "periodType": "Monthly",
                    "categoryCombo": {
                        "id": "CatComboS01"
                    },
                    "organisationUnits": [
                        {
                            "id": "OrgUnit0001"
                        }
                    ]
                },
                {
                    "id": "DataSet0002",
                    "name": "data set 2",
                    "shortName": "ds 2",
                    "periodType": "Monthly",
                    "categoryCombo": {
                        "id": "CatComboS02"
                    },
                    "organisationUnits": [
                        {
                            "id": "OrgUnit0001"
                        }
                    ]
                }
            ],
            "userRoles":[
                {
                    "name": "CC Merge role",
                    "userGroupAccesses": [],
                    "id": "CcMergeRole",
                    "dataSets": [],
                    "authorities": ["F_CATEGORY_COMBO_MERGE"]
                }
            ]
        }
        """;
  }
}
