/*
 * Copyright (c) 2004-2024, University of Oslo
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
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.restassured.response.ValidatableResponse;
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

class CategoryMergeTest extends ApiTest {

  private RestApiActions categoryApiActions;
  private RestApiActions visualizationApiActions;
  private MetadataActions metadataActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private final String sourceUid1 = "UIDCatego01";
  private final String sourceUid2 = "UIDCatego02";
  private final String targetUid = "UIDCatego03";

  @BeforeAll
  void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    categoryApiActions = new RestApiActions("categories");
    visualizationApiActions = new RestApiActions("visualizations");
    metadataActions = new MetadataActions();
    loginActions.loginAsSuperUser();

    // add user with required merge auth
    userActions.addUserFull(
        "user",
        "auth",
        "userWithMergeAuth",
        "Test1234!",
        "F_CATEGORY_MERGE",
        "F_CATEGORY_DELETE",
        "F_CATEGORY_PUBLIC_ADD");
  }

  @BeforeEach
  void setup() {
    loginActions.loginAsSuperUser();
    setupMetadata();
  }

  @Test
  @DisplayName(
      "Valid Category merge completes successfully with all source Category refs replaced with target Category")
  void validCategoryMergeTest() {
    // given
    createUsers();
    // confirm state before merge
    ValidatableResponse preMergeState =
        categoryApiActions.get(targetUid).validateStatus(200).validate();

    preMergeState
        .body("categoryOptions", hasSize(equalTo(2)))
        .body(
            "categoryOptions",
            hasItems(hasEntry("id", "UIDCatOpt1A"), hasEntry("id", "UIDCatOpt1B")))
        .body("categoryCombos", hasSize(equalTo(1)))
        .body("categoryCombos", hasItem(hasEntry("id", "UIDCatCom02")));

    // visualization category dimensions have source category refs
    verifyVisualisations(sourceUid1, "VizUid00001");
    verifyVisualisations(sourceUid2, "VizUid00002");

    // user category dimension constraints have source category refs
    verifyUserCatDimensionConstraint(sourceUid1, "UserUid1111");
    verifyUserCatDimensionConstraint(sourceUid2, "UserUid2222");

    // login as merge user
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a category merge request is submitted, deleting sources
    ApiResponse response = categoryApiActions.post("merge", getMergeBody()).validateStatus(200);

    // then a success response received, sources are deleted & source references were merged
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("Category merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("Category"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    categoryApiActions.get(sourceUid1).validateStatus(404);
    categoryApiActions.get(sourceUid2).validateStatus(404);
    ValidatableResponse postMergeState =
        categoryApiActions.get(targetUid).validateStatus(200).validate();

    postMergeState
        .body("categoryOptions", hasSize(equalTo(2)))
        .body(
            "categoryOptions",
            hasItems(hasEntry("id", "UIDCatOpt1A"), hasEntry("id", "UIDCatOpt1B")))
        .body("categoryCombos", hasSize(equalTo(2)))
        .body(
            "categoryCombos",
            hasItems(hasEntry("id", "UIDCatCom01"), hasEntry("id", "UIDCatCom02")));

    // check visualization category dimensions have target category refs now
    verifyVisualisations(targetUid, "VizUid00001", "VizUid00002", "VizUid00003");

    // user category dimension constraints have source category refs
    verifyUserCatDimensionConstraint(targetUid, "UserUid1111");
    verifyUserCatDimensionConstraint(targetUid, "UserUid2222");
  }

  private void verifyUserCatDimensionConstraint(String category, String... users) {
    loginActions.loginAsSuperUser();
    for (String userUid : users) {
      userActions
          .get(userUid)
          .validateStatus(200)
          .validate()
          .body("catDimensionConstraints", hasSize(equalTo(1)))
          .body("catDimensionConstraints", hasItems(hasEntry("id", category)));
    }
  }

  private void verifyVisualisations(String category, String... visualizations) {
    for (String vizUid : visualizations) {
      visualizationApiActions
          .get(vizUid)
          .validateStatus(200)
          .validate()
          .body("categoryDimensions", hasSize(equalTo(1)))
          .body("categoryDimensions[0].category.id", equalTo(category));
    }
  }

  private void setupMetadata() {
    metadataActions.importMetadata(metadata()).validateStatus(200);
  }

  private void createUsers() {
    //
    userActions
        .post(
            """
            {
                "id": "UserUid2222",
                "firstName": "Sam",
                "surname": "Tobin",
                "username": "sammyT",
                "userRoles": [
                    {
                        "id": "UserRole111"
                    }
                ],
                "catDimensionConstraints":[
                        {
                            "id": "UIDCatego02"
                        }
                    ]
            }
            """)
        .validateStatus(201);

    userActions
        .post(
            """
                      {
                          "id": "UserUid1111",
                          "firstName": "Sam",
                          "surname": "Tobin",
                          "username": "sammyT1",
                          "userRoles": [
                              {
                                  "id": "UserRole111"
                              }
                          ],
                          "catDimensionConstraints":[
                                  {
                                      "id": "UIDCatego01"
                                  }
                              ]
                      }
                      """)
        .validateStatus(201);
  }

  @Test
  @DisplayName("Category merge fails when user has not got the required authority")
  void testCategoryMergeNoRequiredAuth() {
    userActions.addUserFull("basic", "User", "basicUser", "Test1234!", "NO_AUTH");
    loginActions.loginAsUser("basicUser", "Test1234!");

    // when
    ApiResponse response = categoryApiActions.post("merge", getMergeBody()).validateStatus(403);

    // then
    response
        .validate()
        .statusCode(403)
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body(
            "message", equalTo("Access is denied, requires one Authority from [F_CATEGORY_MERGE]"));
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

  private String metadata() {
    return """
        {
            "categoryOptions": [
                {
                    "id": "UIDCatOpt1A",
                    "name": "cat option 1A",
                    "shortName": "cat option 1A",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid1"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt1B",
                    "name": "cat option 1B",
                    "shortName": "cat option 1B",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid1"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt2A",
                    "name": "cat option 2A",
                    "shortName": "cat option 2A",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid1"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt2B",
                    "name": "cat option 2B",
                    "shortName": "cat option 2B",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid1"
                        }
                    ]
                }
            ],
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
                            "id": "UIDCatOpt1A"
                        },
                        {
                            "id": "UIDCatOpt1B"
                        }
                    ]
                },
                {
                    "id": "UIDCatego03",
                    "name": "category 3",
                    "shortName": "category 3",
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
                    "id": "UIDCatego04",
                    "name": "category 4",
                    "shortName": "category 4",
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
            ],
            "organisationUnits": [
                {
                    "id": "OrgUnitUid1",
                    "name": "org 1",
                    "shortName": "org 1",
                    "openingDate": "2023-06-15"
                },
                {
                    "id": "OrgUnitUid2",
                    "name": "org 2",
                    "shortName": "org 2",
                    "openingDate": "2024-06-15"
                },
                {
                    "id": "OrgUnitUid3",
                    "name": "org 3",
                    "shortName": "org 3",
                    "openingDate": "2023-09-15"
                },
                {
                    "id": "OrgUnitUid4",
                    "name": "org 4",
                    "shortName": "org 4",
                    "openingDate": "2023-06-25"
                }
            ],
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
                            "id": "UIDCatego03"
                        },
                        {
                            "id": "UIDCatego04"
                        }
                    ]
                }
            ],
            "visualizations":[
                {
                    "id": "VizUid00001",
                    "name": "viz 1",
                    "categoryDimensions": [
                        {
                            "category": {
                                "id": "UIDCatego01"
                            },
                            "categoryOptions": [
                                {
                                    "id": "UIDCatOpt1A"
                                },
                                {
                                    "id": "UIDCatOpt1B"
                                }
                            ]
                        }
                    ],
                    "type": "LINE",
                    "rowDimensions": [
                        "UIDCatego01"
                    ],
                    "rows": [
                        {
                            "id": "UIDCatego01"
                        }
                    ]
                },
                {
                    "id": "VizUid00002",
                    "name": "viz 2",
                    "categoryDimensions": [
                        {
                            "category": {
                                "id": "UIDCatego02"
                            },
                            "categoryOptions": [
                                {
                                    "id": "UIDCatOpt1A"
                                },
                                {
                                    "id": "UIDCatOpt1B"
                                }
                            ]
                        }
                    ],
                    "type": "LINE",
                    "rowDimensions": [
                        "UIDCatego02"
                    ],
                    "rows": [
                        {
                            "id": "UIDCatego02"
                        }
                    ]
                },
                {
                    "id": "VizUid00003",
                    "name": "viz 3",
                    "categoryDimensions": [
                        {
                            "category": {
                                "id": "UIDCatego03"
                            },
                            "categoryOptions": [
                                {
                                    "id": "UIDCatOpt1A"
                                },
                                {
                                    "id": "UIDCatOpt1B"
                                }
                            ]
                        }
                    ],
                    "type": "LINE",
                    "rowDimensions": [
                        "UIDCatego03"
                    ],
                    "rows": [
                        {
                            "id": "UIDCatego03"
                        }
                    ]
                }
            ],
            "userRoles":[
                {
                    "name": "New role",
                    "userGroupAccesses": [],
                    "id": "UserRole111",
                    "dataSets": [],
                    "authorities": ["F_CATEGORY_MERGE"]
                }
            ]
        }
        """;
  }
}
