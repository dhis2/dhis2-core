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
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.restassured.response.ValidatableResponse;

class CategoryOptionMergeTest extends ApiTest {

  private RestApiActions categoryOptionApiActions;
  private MetadataActions metadataActions;
  private RestApiActions maintenanceApiActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private final String sourceUid1 = "UIDCatOpt1A";
  private final String sourceUid2 = "UIDCatOpt2B";
  private final String targetUid = "UIDCatOpt3A";

  @BeforeAll
  public void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    categoryOptionApiActions = new RestApiActions("categoryOptions");
    metadataActions = new MetadataActions();
    maintenanceApiActions = new RestApiActions("maintenance");
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
  public void setup() {
    loginActions.loginAsSuperUser();
    setupMetadata();
  }

  @Test
  @DisplayName(
      "Valid CategoryOption merge completes successfully with all source CategoryOption refs replaced with target CategoryOption")
  void validCategoryOptionMergeTest() {
    // given
    // generate category option combos
    maintenanceApiActions
        .post("categoryOptionComboUpdate", new QueryParamsBuilder().build())
        .validateStatus(200);

    // confirm state before merge
    ValidatableResponse preMergeState =
        categoryOptionApiActions.get(targetUid).validateStatus(200).validate();

    preMergeState
        .body("organisationUnits", hasSize(equalTo(1)))
        .body("organisationUnits", hasItem(hasEntry("id", "OrgUnitUid3")))
        .body("categories", hasSize(equalTo(1)))
        .body("categories", hasItem(hasEntry("id", "UIDCatego03")))
        .body("categoryOptionCombos", hasSize(equalTo(2)))
        .body("categoryOptionGroups", hasSize(equalTo(1)))
        .body("categoryOptionGroups", hasItem(hasEntry("id", "CatOptGrp03")));

    // login as merge user
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a category option request is submitted, deleting sources
    ApiResponse response =
        categoryOptionApiActions.post("merge", getMergeBody()).validateStatus(200);

    // then a success response received, sources are deleted & source references were merged
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("CategoryOption merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("CategoryOption"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    categoryOptionApiActions.get(sourceUid1).validateStatus(404);
    categoryOptionApiActions.get(sourceUid2).validateStatus(404);
    ValidatableResponse postMergeState =
        categoryOptionApiActions.get(targetUid).validateStatus(200).validate();

    postMergeState
        .body(
            "organisationUnits",
            hasItems(
                hasEntry("id", "OrgUnitUid1"),
                hasEntry("id", "OrgUnitUid2"),
                hasEntry("id", "OrgUnitUid3")))
        .body(
            "categories",
            hasItems(
                hasEntry("id", "UIDCatego01"),
                hasEntry("id", "UIDCatego02"),
                hasEntry("id", "UIDCatego03")))
        .body("categoryOptionCombos", hasSize(equalTo(5)))
        .body(
            "categoryOptionGroups",
            hasItems(
                hasEntry("id", "CatOptGrp01"),
                hasEntry("id", "CatOptGrp02"),
                hasEntry("id", "CatOptGrp03")));
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
    return 
        """
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
