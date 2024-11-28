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

class CategoryOptionMergeTest extends ApiTest {

  private RestApiActions categoryOptionApiActions;
  private MetadataActions metadataActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private final String sourceUid1 = "CatOptUid90";
  private final String sourceUid2 = "CatOptUid91";
  private final String targetUid = "CatOptUid92";

  @BeforeAll
  public void before() {
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
  public void setup() {
    loginActions.loginAsSuperUser();
    setupMetadata();
  }

  @Test
  @DisplayName(
      "Valid CategoryOption merge completes successfully with all source CategoryOption refs replaced with target CategoryOption")
  void validDataElementMergeTest() {
    // given
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // confirm state before merge
    ValidatableResponse preMergeState =
        categoryOptionApiActions.get(targetUid).validateStatus(200).validate();

    preMergeState
        .body("organisationUnits", hasSize(equalTo(1)))
        .body("organisationUnits", hasItem(hasEntry("id", "OrgUnitUid2")))
        .body("categories", hasSize(equalTo(1)))
        .body("categories", hasItem(hasEntry("id", "CategoUid92")))
        .body("categoryOptionCombos", hasSize(equalTo(1)))
        .body("categoryOptionCombos", hasItem(hasEntry("id", "CatOptCom92")))
        .body("categoryOptionGroups", hasSize(equalTo(1)))
        .body("categoryOptionGroups", hasItem(hasEntry("id", "CatOptGrp02")));

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
                hasEntry("id", "OrgUnitUid0"),
                hasEntry("id", "OrgUnitUid1"),
                hasEntry("id", "OrgUnitUid2")))
        .body(
            "categories",
            hasItems(
                hasEntry("id", "CategoUid90"),
                hasEntry("id", "CategoUid91"),
                hasEntry("id", "CategoUid92")))
        .body(
            "categoryOptionCombos",
            hasItems(
                hasEntry("id", "CatOptCom90"),
                hasEntry("id", "CatOptCom91"),
                hasEntry("id", "CatOptCom92")))
        .body(
            "categoryOptionGroups",
            hasItems(
                hasEntry("id", "CatOptGrp01"),
                hasEntry("id", "CatOptGrp00"),
                hasEntry("id", "CatOptGrp02")));
  }

  private void setupMetadata() {
    metadataActions.post(metadata()).validateStatus(200);
  }

  @Test
  @DisplayName("CategoryOption merge fails when user has not got the required authority")
  void testDataElementMergeNoRequiredAuth() {
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
                        "id": "CatOptUid90",
                        "name": "cat opt 0",
                        "shortName": "cat opt 0",
                        "organisationUnits": [
                            {
                                "id": "OrgUnitUid0"
                            }
                        ]
                    },
                    {
                        "id": "CatOptUid91",
                        "name": "cat opt 1",
                        "shortName": "cat opt 1",
                        "organisationUnits": [
                            {
                                "id": "OrgUnitUid1"
                            }
                        ]
                    },
                    {
                        "id": "CatOptUid92",
                        "name": "cat opt 2",
                        "shortName": "cat opt 2",
                        "organisationUnits": [
                            {
                                "id": "OrgUnitUid2"
                            }
                        ]
                    },
                    {
                        "id": "CatOptUid93",
                        "name": "cat opt 3",
                        "shortName": "cat opt 3",
                        "organisationUnits": [
                            {
                                "id": "OrgUnitUid3"
                            }
                        ]
                    }
                ],
                "categories": [
                    {
                        "id": "CategoUid90",
                        "name": "cat 0",
                        "shortName": "cat 0",
                        "dataDimensionType": "DISAGGREGATION",
                        "categoryOptions": [
                            {
                                "id": "CatOptUid90"
                            }
                        ]
                    },
                    {
                        "id": "CategoUid91",
                        "name": "cat 1",
                        "shortName": "cat 1",
                        "dataDimensionType": "DISAGGREGATION",
                        "categoryOptions": [
                            {
                                "id": "CatOptUid91"
                            }
                        ]
                    },
                    {
                        "id": "CategoUid92",
                        "name": "cat 2",
                        "shortName": "cat 2",
                        "dataDimensionType": "DISAGGREGATION",
                        "categoryOptions": [
                            {
                                "id": "CatOptUid92"
                            }
                        ]
                    },
                    {
                        "id": "CategoUid93",
                        "name": "cat 3",
                        "shortName": "cat 3",
                        "dataDimensionType": "DISAGGREGATION",
                        "categoryOptions": [
                            {
                                "id": "CatOptUid93"
                            }
                        ]
                    }
                ],
                "categoryOptionCombos": [
                    {
                        "id": "CatOptCom90",
                        "name": "cat option combo 0",
                        "categoryCombo": {
                            "id": "CatComUid90"
                        },
                        "categoryOptions": [
                            {
                                "id": "CatOptUid90"
                            }
                        ]
                    },
                    {
                        "id": "CatOptCom91",
                        "name": "cat option combo 1",
                        "categoryCombo": {
                            "id": "CatComUid91"
                        },
                        "categoryOptions": [
                            {
                                "id": "CatOptUid91"
                            }
                        ]
                    },
                    {
                        "id": "CatOptCom92",
                        "name": "cat option combo 2",
                        "categoryCombo": {
                            "id": "CatComUid92"
                        },
                        "categoryOptions": [
                            {
                                "id": "CatOptUid92"
                            }
                        ]
                    },
                    {
                        "id": "CatOptCom93",
                        "name": "cat option combo 3",
                        "categoryCombo": {
                            "id": "CatComUid93"
                        },
                        "categoryOptions": [
                            {
                                "id": "CatOptUid93"
                            }
                        ]
                    }
                ],
                "organisationUnits": [
                    {
                        "id": "OrgUnitUid0",
                        "name": "org 0",
                        "shortName": "org 0",
                        "openingDate": "2023-06-15"
                    },
                    {
                        "id": "OrgUnitUid1",
                        "name": "org 1",
                        "shortName": "org 1",
                        "openingDate": "2024-06-15"
                    },
                    {
                        "id": "OrgUnitUid2",
                        "name": "org 2",
                        "shortName": "org 2",
                        "openingDate": "2023-09-15"
                    },
                    {
                        "id": "OrgUnitUid3",
                        "name": "org 3",
                        "shortName": "org 3",
                        "openingDate": "2023-06-25"
                    }
                ],
                "categoryOptionGroups": [
                    {
                        "id": "CatOptGrp00",
                        "name": "cog 0",
                        "shortName": "cog 0",
                        "dataDimensionType": "DISAGGREGATION",
                        "categoryOptions": [
                            {
                                "id": "CatOptUid90"
                            }
                        ]
                    },
                    {
                        "id": "CatOptGrp01",
                        "name": "cog 1",
                        "shortName": "cog 1",
                        "dataDimensionType": "DISAGGREGATION",
                        "categoryOptions": [
                            {
                                "id": "CatOptUid91"
                            }
                        ]
                    },
                    {
                        "id": "CatOptGrp02",
                        "name": "cog 2",
                        "shortName": "cog 2",
                        "dataDimensionType": "DISAGGREGATION",
                        "categoryOptions": [
                            {
                                "id": "CatOptUid92"
                            }
                        ]
                    },
                    {
                        "id": "CatOptGrp03",
                        "name": "cog 3",
                        "shortName": "cog 3",
                        "dataDimensionType": "DISAGGREGATION",
                        "categoryOptions": [
                            {
                                "id": "CatOptUid93"
                            }
                        ]
                    }
                ],
                "categoryCombos": [
                    {
                        "id": "CatComUid90",
                        "name": "cat combo 0",
                        "dataDimensionType": "DISAGGREGATION",
                        "categories": [
                            {
                                "id": "CategoUid90"
                            }
                        ],
                        "categoryOptionCombos": [
                            {
                                "id": "CatOptCom90"
                            }
                        ]
                    },
                    {
                        "id": "CatComUid91",
                        "name": "cat combo 1",
                        "dataDimensionType": "DISAGGREGATION",
                        "categories": [
                            {
                                "id": "CategoUid91"
                            }
                        ],
                        "categoryOptionCombos": [
                            {
                                "id": "CatOptCom91"
                            }
                        ]
                    },
                    {
                        "id": "CatComUid92",
                        "name": "cat combo 2",
                        "dataDimensionType": "DISAGGREGATION",
                        "categories": [
                            {
                                "id": "CategoUid92"
                            }
                        ],
                        "categoryOptionCombos": [
                            {
                                "id": "CatOptCom92"
                            }
                        ]
                    },
                    {
                        "id": "CatComUid93",
                        "name": "cat combo 3",
                        "dataDimensionType": "DISAGGREGATION",
                        "categories": [
                            {
                                "id": "CategoUid93"
                            }
                        ],
                        "categoryOptionCombos": [
                            {
                                "id": "CatOptCom93"
                            }
                        ]
                    }
                ]
            }
          """;
  }
}
