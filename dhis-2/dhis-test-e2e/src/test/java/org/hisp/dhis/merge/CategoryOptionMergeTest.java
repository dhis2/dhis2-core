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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

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

class CategoryOptionMergeTest extends ApiTest {

  private RestApiActions categoryOptionApiActions;
  private MetadataActions metadataActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private final String sourceUid1 = "UIDCatOpt1A";
  private final String sourceUid2 = "UIDCatOpt2B";
  private final String targetUid = "UIDCatOpt3A";

  @BeforeAll
  void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    categoryOptionApiActions = new RestApiActions("categoryOptions");
    metadataActions = new MetadataActions();
    loginActions.loginAsSuperUser();

    // add user with required merge auth
    userActions.addUserFull(
        "user",
        "auth",
        "userWithMergeAuth",
        "Test1234!",
        "F_CATEGORY_OPTION_MERGE",
        "F_CATEGORY_OPTION_DELETE",
        "F_CATEGORY_OPTION_PUBLIC_ADD");
  }

  @BeforeEach
  void setup() {
    loginActions.loginAsSuperUser();
    setupMetadata();
  }

  @Test
  @DisplayName(
      "CategoryOption merge with multiple sources succeeds when all sources & target share identical Categories")
  void categoryOptionMergeIdenticalCategoriesTest() {
    // given 4 sources (UIDCatOpt3B/3C/3D/3E) and target (UIDCatOpt3A) which all belong to category
    // 3

    // login as merge user
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a same-Category merge with multiple sources is submitted, deleting the sources
    JsonObject body = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add("UIDCatOpt3B");
    sources.add("UIDCatOpt3C");
    sources.add("UIDCatOpt3D");
    sources.add("UIDCatOpt3E");
    body.add("sources", sources);
    body.addProperty("target", targetUid);
    body.addProperty("deleteSources", true);

    ApiResponse response = categoryOptionApiActions.post("merge", body).validateStatus(200);

    // then a success response is received and all sources are deleted
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body(
            "response.mergeReport.message",
            equalTo(
                "CategoryOption merge complete. There will be duplicate CategoryOptionCombos as a result of the merge. "
                    + "These should be merged immediately to help keep system integrity. Duplicates can be found using the data integrity check `category_option_combos_have_duplicates`"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("CategoryOption"))
        .body(
            "response.mergeReport.sourcesDeleted",
            hasItems("UIDCatOpt3B", "UIDCatOpt3C", "UIDCatOpt3D", "UIDCatOpt3E"));

    categoryOptionApiActions.get("UIDCatOpt3B").validateStatus(404);
    categoryOptionApiActions.get("UIDCatOpt3C").validateStatus(404);
    categoryOptionApiActions.get("UIDCatOpt3D").validateStatus(404);
    categoryOptionApiActions.get("UIDCatOpt3E").validateStatus(404);
    categoryOptionApiActions.get(targetUid).validateStatus(200);
  }

  @Test
  @DisplayName("CategoryOption merge is rejected when source and target have different Categories")
  void categoryOptionMergeDifferentCategoriesRejectedTest() {
    // given sources (category 1 & 2) and target (category 3) which do not share identical
    // Category membership

    // login as merge user
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a cross-Category merge is submitted
    ApiResponse response =
        categoryOptionApiActions.post("merge", getMergeBody()).validateStatus(409);

    // then a validation error is returned and no merge is performed
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("WARNING"))
        .body("response.mergeReport.mergeErrors", hasSize(equalTo(2)))
        .body("response.mergeReport.mergeErrors.errorCode", hasItems("E1549"));

    // sources are not deleted
    categoryOptionApiActions.get(sourceUid1).validateStatus(200);
    categoryOptionApiActions.get(sourceUid2).validateStatus(200);
  }

  private void setupMetadata() {
    metadataActions.importMetadata(metadata()).validateStatus(200);
  }

  @Test
  @DisplayName("CategoryOption merge fails when user has not got the required authority")
  void testCategoryOptionMergeNoRequiredAuth() {
    userActions.addUserFull("basic", "User", "basicUser", "Test1234!", "NO_AUTH");
    loginActions.loginAsUser("basicUser", "Test1234!");

    // when
    ApiResponse response =
        categoryOptionApiActions.post("merge", getMergeBody()).validateStatus(403);

    // then
    response
        .validate()
        .statusCode(403)
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo("Access is denied, requires one Authority from [F_CATEGORY_OPTION_MERGE]"));
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
                            "id": "OrgUnitUid2"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt2B",
                    "name": "cat option 2B",
                    "shortName": "cat option 2B",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid2"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt3A",
                    "name": "cat option 3A",
                    "shortName": "cat option 3A",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid3"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt3B",
                    "name": "cat option 3B",
                    "shortName": "cat option 3B",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid3"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt3C",
                    "name": "cat option 3C",
                    "shortName": "cat option 3C",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid3"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt3D",
                    "name": "cat option 3D",
                    "shortName": "cat option 3D",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid3"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt3E",
                    "name": "cat option 3E",
                    "shortName": "cat option 3E",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid3"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt4A",
                    "name": "cat option 4A",
                    "shortName": "cat option 4A",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid4"
                        }
                    ]
                },
                {
                    "id": "UIDCatOpt4B",
                    "name": "cat option 4B",
                    "shortName": "cat option 4B",
                    "organisationUnits": [
                        {
                            "id": "OrgUnitUid4"
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
                            "id": "UIDCatOpt2A"
                        },
                        {
                            "id": "UIDCatOpt2B"
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
                            "id": "UIDCatOpt3A"
                        },
                        {
                            "id": "UIDCatOpt3B"
                        },
                        {
                            "id": "UIDCatOpt3C"
                        },
                        {
                            "id": "UIDCatOpt3D"
                        },
                        {
                            "id": "UIDCatOpt3E"
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
                            "id": "UIDCatOpt4A"
                        },
                        {
                            "id": "UIDCatOpt4B"
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
            "categoryOptionGroups": [
                {
                    "id": "CatOptGrp01",
                    "name": "co group 1",
                    "shortName": "co group 1",
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
                    "id": "CatOptGrp02",
                    "name": "co group 2",
                    "shortName": "co group 2",
                    "dataDimensionType": "DISAGGREGATION",
                    "categoryOptions": [
                        {
                            "id": "UIDCatOpt2A"
                        },
                        {
                            "id": "UIDCatOpt2B"
                        }
                    ]
                },
                {
                    "id": "CatOptGrp03",
                    "name": "co group 3",
                    "shortName": "co group 3",
                    "dataDimensionType": "DISAGGREGATION",
                    "categoryOptions": [
                        {
                            "id": "UIDCatOpt3A"
                        },
                        {
                            "id": "UIDCatOpt3B"
                        }
                    ]
                },
                {
                    "id": "CatOptGrp04",
                    "name": "co group 4",
                    "shortName": "co group 4",
                    "dataDimensionType": "DISAGGREGATION",
                    "categoryOptions": [
                        {
                            "id": "UIDCatOpt4A"
                        },
                        {
                            "id": "UIDCatOpt4B"
                        }
                    ]
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
            ]
        }
        """;
  }
}
