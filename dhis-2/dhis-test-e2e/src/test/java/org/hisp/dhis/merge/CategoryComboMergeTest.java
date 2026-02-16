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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
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
  private RestApiActions categoryOptionComboApiActions;
  private RestApiActions dataElementApiActions;
  private RestApiActions programsApiActions;
  private RestApiActions dataApprovalWorkflowsApiActions;
  private RestApiActions programsIndicatorsApiActions;
  private RestApiActions dataSetApiActions;
  private MetadataActions metadataActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private final String sourceUid1 = "UIDCatCom01";
  private final String sourceUid2 = "UIDCatCom02";
  private final String targetUid = "UIDCatCom03";

  @BeforeAll
  void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    categoryComboApiActions = new RestApiActions("categoryCombos");
    categoryOptionComboApiActions = new RestApiActions("categoryOptionCombos");
    dataElementApiActions = new RestApiActions("dataElements");
    programsApiActions = new RestApiActions("programs");
    dataApprovalWorkflowsApiActions = new RestApiActions("dataApprovalWorkflows");
    programsIndicatorsApiActions = new RestApiActions("programIndicators");
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
  }

  @Test
  @DisplayName(
      "Valid CategoryCombo merge completes successfully with all source refs replaced with target")
  void validCategoryComboMergeTest() {
    // given metadata state
    importMetadata();

    // assert pre merge state
    assertPreMergeState();

    // get COCs for each CC for later assertions
    List<String> srcCc1Cocs = getCocUids(sourceUid1);
    List<String> srcCc2Cocs = getCocUids(sourceUid2);
    List<String> targetCcCocs = getCocUids(targetUid);

    assertEquals(4, srcCc1Cocs.size(), "Source CC1 should have 4 COCs");
    assertEquals(4, srcCc2Cocs.size(), "Source CC2 should have 4 COCs");
    assertEquals(4, targetCcCocs.size(), "Target CC should have 4 COCs");

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

    // assert post merge state
    assertPostMergeState(srcCc1Cocs, srcCc2Cocs, targetCcCocs);
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

  private List<String> getCocUids(String ccUid) {
    return categoryComboApiActions
        .get(ccUid)
        .validateStatus(200)
        .extractList("categoryOptionCombos.id");
  }

  private void assertPostMergeState(
      List<String> srcCC1Cocs, List<String> srcCC2Cocs, List<String> targetCocs) {
    // source and target COCs have the target CC
    assertOptionComboHasCombo(srcCC1Cocs, targetUid);
    assertOptionComboHasCombo(srcCC2Cocs, targetUid);
    assertOptionComboHasCombo(targetCocs, targetUid);

    // data elements have the target CC
    assertDataElementHasCombo("DeUID000001", targetUid);
    assertDataElementHasCombo("DeUID000002", targetUid);
    assertDataElementHasCombo("DeUID000003", targetUid);

    // data sets have the target CC
    assertDataSetHasCombo("DsUID000001", targetUid);
    assertDataSetHasCombo("DsUID000002", targetUid);
    assertDataSetHasCombo("DsUID000003", targetUid);

    // program CC & enrollment CC have the target CC
    assertProgramHasCc("ProgUID0001", targetUid);
    assertProgramHasCc("ProgUID0002", targetUid);
    assertProgramHasCc("ProgUID0003", targetUid);

    // data approval workflows have the target CC
    assertWorkflowHasCc("dawUID00001", targetUid);
    assertWorkflowHasCc("dawUID00002", targetUid);
    assertWorkflowHasCc("dawUID00003", targetUid);

    // program indicators CC & AC have the target CC
    assertProgIndHasCc("prgIndUID01", targetUid);
    assertProgIndHasCc("prgIndUID02", targetUid);
    assertProgIndHasCc("prgIndUID03", targetUid);

    // DataElements have target CategoryCombo refs
    assertDataElementHasCombo("DeUID000001", targetUid);
    assertDataElementHasCombo("DeUID000002", targetUid);
    assertDataElementHasCombo("DeUID000003", targetUid);

    // data sets have target CategoryCombo refs
    assertDataSetHasCombo("DsUID000001", targetUid);
    assertDataSetHasCombo("DsUID000002", targetUid);
    assertDataSetHasCombo("DsUID000003", targetUid);
  }

  private void assertProgIndHasCc(String prgIndUid, String ccUid) {
    programsIndicatorsApiActions
        .get(prgIndUid)
        .validate()
        .body("categoryCombo.id", equalTo(ccUid))
        .body("attributeCombo.id", equalTo(ccUid));
  }

  private void assertWorkflowHasCc(String dawUid, String ccUid) {
    dataApprovalWorkflowsApiActions.get(dawUid).validate().body("categoryCombo.id", equalTo(ccUid));
  }

  private void assertProgramHasCc(String progUid, String ccUid) {
    programsApiActions
        .get(progUid)
        .validate()
        .body("categoryCombo.id", equalTo(ccUid))
        .body("enrollmentCategoryCombo.id", equalTo(ccUid));
  }

  private void assertPreMergeState() {
    assertComboHasCategories(sourceUid1, "UIDCatego01", "UIDCatego02");
    assertComboHasCategories(sourceUid2, "UIDCatego01", "UIDCatego02");
    assertComboHasCategories(targetUid, "UIDCatego01", "UIDCatego02");

    // data elements
    assertDataElementHasCombo("DeUID000001", sourceUid1);
    assertDataElementHasCombo("DeUID000002", sourceUid2);
    assertDataElementHasCombo("DeUID000003", targetUid);

    // data sets
    assertDataSetHasCombo("DsUID000001", sourceUid1);
    assertDataSetHasCombo("DsUID000002", sourceUid2);
    assertDataSetHasCombo("DsUID000003", targetUid);

    // program CC & enrollment CC
    assertProgramHasCc("ProgUID0001", sourceUid1);
    assertProgramHasCc("ProgUID0002", sourceUid2);
    assertProgramHasCc("ProgUID0003", targetUid);

    // data approval workflow
    assertWorkflowHasCc("dawUID00001", sourceUid1);
    assertWorkflowHasCc("dawUID00002", sourceUid2);
    assertWorkflowHasCc("dawUID00003", targetUid);

    // program indicator CC & AC
    assertProgIndHasCc("prgIndUID01", sourceUid1);
    assertProgIndHasCc("prgIndUID02", sourceUid2);
    assertProgIndHasCc("prgIndUID03", targetUid);
  }

  private void importMetadata() {
    String metadata =
        """
                {
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s,
                    %s
                }
                """
            .formatted(
                getCatOptions(),
                getCats(),
                getCombos(),
                getDataSet(sourceUid1, sourceUid2, targetUid),
                getDataElement(),
                getOrgUnit(),
                getPrograms(sourceUid1, sourceUid2, targetUid),
                getDataApprovalWorkflows(sourceUid1, sourceUid2, targetUid),
                getProgramIndicators(sourceUid1, sourceUid2, targetUid));

    metadataActions.importMetadata(metadata).validate().body("response.stats.created", equalTo(25));

    generateCocs();
  }

  private void generateCocs() {
    given()
        .contentType("application/json")
        .when()
        .post("/maintenance/categoryOptionComboUpdate")
        .then()
        .statusCode(200);
  }

  private void assertOptionComboHasCombo(List<String> cocUids, String ccUid) {
    for (String cocUid : cocUids) {
      categoryOptionComboApiActions.get(cocUid).validate().body("categoryCombo.id", equalTo(ccUid));
    }
  }

  private void assertComboHasCategories(String catComboUid, String catUid1, String catUid2) {
    categoryComboApiActions
        .get(catComboUid)
        .validateStatus(200)
        .validate()
        .body(
            "categories.id", containsInAnyOrder(List.of(catUid1, catUid2).toArray(String[]::new)));
  }

  private void assertDataSetHasCombo(String dsUid, String comboUid) {
    dataSetApiActions
        .get(dsUid)
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(comboUid));
  }

  private void assertDataElementHasCombo(String deUid, String catComboUid) {
    dataElementApiActions
        .get(deUid)
        .validateStatus(200)
        .validate()
        .body("categoryCombo.id", equalTo(catComboUid));
  }

  //  @Test
  //  @DisplayName("CategoryCombo merge fails when sources and target have different Categories")
  //  void testCategoryComboMergeDifferentCategories() {
  //    // login as merge user
  //    loginActions.loginAsUser("userWithCcMergeAuth", "Test1234!");
  //
  //    // when trying to merge combos with different categories
  //    JsonObject body = new JsonObject();
  //    JsonArray sources = new JsonArray();
  //    sources.add("CatComboDif"); // This has a different category
  //    body.add("sources", sources);
  //    body.addProperty("target", targetUid);
  //    body.addProperty("deleteSources", true);
  //
  //    ApiResponse response = categoryComboApiActions.post("merge", body).validateStatus(409);
  //
  //    // then validation error
  //    response
  //        .validate()
  //        .statusCode(409)
  //        .body("httpStatus", equalTo("Conflict"))
  //        .body("status", equalTo("ERROR"))
  //        .body("message", equalTo("Merge validation error"));
  //  }

  // todo need test when CC COC has DV to ensure COC still exists & DV exists

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

  private String getCatOptions() {
    return """
        "categoryOptions": [
            {
                "id": "UIDCatOpt1A",
                "name": "cat option 1A",
                "shortName": "cat option 1A"
            },
            {
                "id": "UIDCatOpt1B",
                "name": "cat option 1B",
                "shortName": "cat option 1B"
            },
            {
                "id": "UIDCatOpt2A",
                "name": "cat option 2A",
                "shortName": "cat option 2A"
            },
            {
                "id": "UIDCatOpt2B",
                "name": "cat option 2B",
                "shortName": "cat option 2B"
            }
        ]
        """;
  }

  private String getCats() {
    return """
        "categories": [
            {
                "id": "UIDCatego01",
                "name": "category 1",
                "shortName": "category 1",
                "dataDimensionType": "DISAGGREGATION",
                "categoryOptions": [
                    {
                        "id": "UIDCatOpt1A"
                    },
                    {
                        "id": "UIDCatOpt1B"
                    }
                ]
            },
            {
                "id": "UIDCatego02",
                "name": "category 2",
                "shortName": "category 2",
                "dataDimensionType": "DISAGGREGATION",
                "categoryOptions": [
                    {
                        "id": "UIDCatOpt2A"
                    },
                    {
                        "id": "UIDCatOpt2B"
                    }
                ]
            }
        ]
        """;
  }

  private String getCombos() {
    return """
        "categoryCombos": [
            {
                "id": "UIDCatCom01",
                "name": "category combo 1",
                "dataDimensionType": "DISAGGREGATION",
                "categories": [
                    {
                        "id": "UIDCatego01"
                    },
                    {
                        "id": "UIDCatego02"
                    }
                ]
            },
            {
                "id": "UIDCatCom02",
                "name": "category combo 2",
                "dataDimensionType": "DISAGGREGATION",
                "categories": [
                    {
                        "id": "UIDCatego01"
                    },
                    {
                        "id": "UIDCatego02"
                    }
                ]
            },
            {
                "id": "UIDCatCom03",
                "name": "category combo 3",
                "dataDimensionType": "DISAGGREGATION",
                "categories": [
                    {
                        "id": "UIDCatego01"
                    },
                    {
                        "id": "UIDCatego02"
                    }
                ]
            }
        ]
        """;
  }

  private String getDataSet(String catComboId1, String catComboId2, String catComboId3) {
    return """
        "dataSets": [
            {
                "name": "ds 1",
                "id": "DsUID000001",
                "shortName": "ds 1",
                "periodType": "Monthly",
                "categoryCombo": {
                   "id": "%s"
                },
                "dataSetElements": [
                    {
                        "dataElement": {
                            "id": "DeUID000001"
                        }
                    }
                ],
                "organisationUnits": [
                    {
                        "id": "OrgUnitUID1"
                    }
                ]
            },
            {
                "name": "ds 2",
                "id": "DsUID000002",
                "shortName": "ds 2",
                "periodType": "Monthly",
                "categoryCombo": {
                   "id": "%s"
                },
                "dataSetElements": [
                    {
                        "dataElement": {
                            "id": "DeUID000002"
                        }
                    }
                ],
                "organisationUnits": [
                    {
                        "id": "OrgUnitUID1"
                    }
                ]
            },
            {
                "name": "ds 3",
                "id": "DsUID000003",
                "shortName": "ds 3",
                "periodType": "Monthly",
                "categoryCombo": {
                   "id": "%s"
                },
                "dataSetElements": [
                    {
                        "dataElement": {
                            "id": "DeUID000003"
                        }
                    }
                ],
                "organisationUnits": [
                    {
                        "id": "OrgUnitUID1"
                    }
                ]
            }
        ]
        """
        .formatted(catComboId1, catComboId2, catComboId3);
  }

  private String getDataElement() {
    return """
        "dataElements": [
            {
                "aggregationType": "DEFAULT",
                "domainType": "AGGREGATE",
                "name": "test de 1",
                "shortName": "test de 1",
                "valueType": "TEXT",
                "id": "DeUID000001",
                "categoryCombo": {
                    "id": "UIDCatCom01"
                },
                "dataSetElements": [
                    {
                        "dataElement": {
                            "id": "DeUID000001"
                        },
                        "dataSet": {
                            "id": "DsUID000001"
                        }
                    }
                ]
            },
            {
                "aggregationType": "DEFAULT",
                "domainType": "AGGREGATE",
                "name": "test de 2",
                "shortName": "test de 2",
                "valueType": "TEXT",
                "id": "DeUID000002",
                "categoryCombo": {
                    "id": "UIDCatCom02"
                },
                "dataSetElements": [
                    {
                        "dataElement": {
                            "id": "DeUID000002"
                        },
                        "dataSet": {
                            "id": "DsUID000002"
                        }
                    }
                ]
            },
            {
                "aggregationType": "DEFAULT",
                "domainType": "AGGREGATE",
                "name": "test de 3",
                "shortName": "test de 3",
                "valueType": "TEXT",
                "id": "DeUID000003",
                "categoryCombo": {
                    "id": "UIDCatCom03"
                },
                "dataSetElements": [
                    {
                        "dataElement": {
                            "id": "DeUID000003"
                        },
                        "dataSet": {
                            "id": "DsUID000003"
                        }
                    }
                ]
            }
        ]
        """;
  }

  private String getOrgUnit() {
    return """
        "organisationUnits": [
            {
                "name": "ou 1",
                "id": "OrgUnitUID1",
                "attributeValues": [],
                "shortName": "ou 1",
                "openingDate": "2020-12-31",
                "dataSets": [
                    {
                        "id": "DsUID000001"
                    }
                ]
            }
        ]
        """;
  }

  private String getPrograms(String srCc1, String srcCc2, String targeCc) {
    return """
        "programs": [
            {
               "id": "ProgUID0001",
               "name": "test program 1",
               "shortName": "test program 1",
               "programType": "WITH_REGISTRATION",
               "categoryCombo": {
                   "id": "%s"
               },
               "enrollmentCategoryCombo": {
                   "id": "%s"
               }
            },
            {
                "id": "ProgUID0002",
                "name": "test program 2",
                "shortName": "test program 2",
                "programType": "WITH_REGISTRATION",
                "categoryCombo": {
                    "id": "%s"
                },
                "enrollmentCategoryCombo": {
                    "id": "%s"
                }
            },
            {
                "id": "ProgUID0003",
                "name": "test program 3",
                "shortName": "test program 3",
                "programType": "WITH_REGISTRATION",
                "categoryCombo": {
                    "id": "%s"
                },
                "enrollmentCategoryCombo": {
                    "id": "%s"
                }
            }
        ]
        """
        .formatted(srCc1, srCc1, srcCc2, srcCc2, targeCc, targeCc);
  }

  private String getDataApprovalWorkflows(String srCc1, String srcCc2, String targeCc) {
    return """
    "dataApprovalWorkflows": [
        {
            "id": "dawUID00001",
            "name": "daw test1",
            "periodType": "Daily",
            "categoryCombo": {
                "id": "%s"
            }
        },
        {
            "id": "dawUID00002",
            "name": "daw test2",
            "periodType": "Daily",
            "categoryCombo": {
                "id": "%s"
            }
        },
        {
            "id": "dawUID00003",
            "name": "daw test3",
            "periodType": "Daily",
            "categoryCombo": {
                "id": "%s"
            }
        }
    ]
    """
        .formatted(srCc1, srcCc2, targeCc);
  }

  private String getProgramIndicators(String srCc1, String srcCc2, String targeCc) {
    return """
    "programIndicators": [
        {
             "id": "prgIndUID01",
             "name": "test indicator 1",
             "shortName": "test indicator 1",
             "program": {
                 "id": "ProgUID0001"
             },
              "categoryCombo": {
                 "id": "%s"
             },
             "attributeCombo": {
                 "id": "%s"
             }
         },
        {
             "id": "prgIndUID02",
             "name": "test indicator 2",
             "shortName": "test indicator 2",
             "program": {
                 "id": "ProgUID0002"
             },
              "categoryCombo": {
                 "id": "%s"
             },
             "attributeCombo": {
                 "id": "%s"
             }
         },
        {
             "id": "prgIndUID03",
             "name": "test indicator 3",
             "shortName": "test indicator 3",
             "program": {
                 "id": "ProgUID0003"
             },
              "categoryCombo": {
                 "id": "%s"
             },
             "attributeCombo": {
                 "id": "%s"
             }
         }
    ]
    """
        .formatted(srCc1, srCc1, srcCc2, srcCc2, targeCc, targeCc);
  }
}
