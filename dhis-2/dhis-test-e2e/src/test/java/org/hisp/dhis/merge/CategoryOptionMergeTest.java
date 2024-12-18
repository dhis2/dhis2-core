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
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryOptionMergeTest extends ApiTest {

  private RestApiActions categoryOptionApiActions;
  private MetadataActions metadataActions;
  private RestApiActions maintenanceApiActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private final String sourceUid1 = "CatOptUid1A";
  private final String sourceUid2 = "CatOptUid2B";
  private final String targetUid = "CatOptUid3A";

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
    String emptyParams = new QueryParamsBuilder().build();
    maintenanceApiActions
        .post("categoryOptionComboUpdate/categoryCombo/CatComUid01", emptyParams)
        .validateStatus(200);
    maintenanceApiActions
        .post("categoryOptionComboUpdate/categoryCombo/CatComUid02", emptyParams)
        .validateStatus(200);

    // confirm state before merge
    ValidatableResponse preMergeState =
        categoryOptionApiActions.get(targetUid).validateStatus(200).validate();

    preMergeState
        .body("organisationUnits", hasSize(equalTo(1)))
        .body("organisationUnits", hasItem(hasEntry("id", "OrgUnitUid3")))
        .body("categories", hasSize(equalTo(1)))
        .body("categories", hasItem(hasEntry("id", "CategoUid03")))
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
                hasEntry("id", "CategoUid01"),
                hasEntry("id", "CategoUid02"),
                hasEntry("id", "CategoUid03")))
        .body("categoryOptionCombos", hasSize(equalTo(5)))
        .body(
            "categoryOptionGroups",
            hasItems(
                hasEntry("id", "CatOptGrp01"),
                hasEntry("id", "CatOptGrp02"),
                hasEntry("id", "CatOptGrp03")));
  }

  private void setupMetadata() {
    metadataActions.post(metadata()).validateStatus(200);
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
                      "id": "CatOptUid1A",
                      "name": "cat opt 1A",
                      "shortName": "cat opt 1A",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid1"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid1B",
                      "name": "cat opt 1B",
                      "shortName": "cat opt 1B",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid1"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid2A",
                      "name": "cat opt 2A",
                      "shortName": "cat opt 2A",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid2"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid2B",
                      "name": "cat opt 2B",
                      "shortName": "cat opt 2B",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid2"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid3A",
                      "name": "cat opt 3A",
                      "shortName": "cat opt 3A",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid3"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid3B",
                      "name": "cat opt 3B",
                      "shortName": "cat opt 3B",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid3"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid4A",
                      "name": "cat opt 4A",
                      "shortName": "cat opt 4A",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid4"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid4B",
                      "name": "cat opt 4B",
                      "shortName": "cat opt 4B",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid4"
                          }
                      ]
                  }
              ],
              "categories": [
                  {
                      "id": "CategoUid01",
                      "name": "cat 1",
                      "shortName": "cat 1",
                      "dataDimensionType": "DISAGGREGATION",
                      "categoryOptions": [
                          {
                              "id": "CatOptUid1A"
                          },
                          {
                              "id": "CatOptUid1B"
                          }
                      ]
                  },
                  {
                      "id": "CategoUid02",
                      "name": "cat 2",
                      "shortName": "cat 2",
                      "dataDimensionType": "DISAGGREGATION",
                      "categoryOptions": [
                          {
                              "id": "CatOptUid2A"
                          },
                          {
                              "id": "CatOptUid2B"
                          }
                      ]
                  },
                  {
                      "id": "CategoUid03",
                      "name": "cat 3",
                      "shortName": "cat 3",
                      "dataDimensionType": "DISAGGREGATION",
                      "categoryOptions": [
                          {
                              "id": "CatOptUid3A"
                          },
                          {
                              "id": "CatOptUid3B"
                          }
                      ]
                  },
                  {
                      "id": "CategoUid04",
                      "name": "cat 4",
                      "shortName": "cat 4",
                      "dataDimensionType": "DISAGGREGATION",
                      "categoryOptions": [
                          {
                              "id": "CatOptUid4A"
                          },
                          {
                              "id": "CatOptUid4B"
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
                      "name": "cog 1",
                      "shortName": "cog 1",
                      "dataDimensionType": "DISAGGREGATION",
                      "categoryOptions": [
                          {
                              "id": "CatOptUid1A"
                          },
                          {
                              "id": "CatOptUid1B"
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
                              "id": "CatOptUid2A"
                          },
                          {
                              "id": "CatOptUid2B"
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
                              "id": "CatOptUid3A"
                          },
                          {
                              "id": "CatOptUid3B"
                          }
                      ]
                  },
                  {
                      "id": "CatOptGrp04",
                      "name": "cog 4",
                      "shortName": "cog 4",
                      "dataDimensionType": "DISAGGREGATION",
                      "categoryOptions": [
                          {
                              "id": "CatOptUid4A"
                          },
                          {
                              "id": "CatOptUid4B"
                          }
                      ]
                  }
              ],
              "categoryCombos": [
                  {
                      "id": "CatComUid01",
                      "name": "cat combo 1",
                      "dataDimensionType": "DISAGGREGATION",
                      "categories": [
                          {
                              "id": "CategoUid01"
                          },
                          {
                              "id": "CategoUid02"
                          }
                      ]
                  },
                  {
                      "id": "CatComUid02",
                      "name": "cat combo 2",
                      "dataDimensionType": "DISAGGREGATION",
                      "categories": [
                          {
                              "id": "CategoUid03"
                          },
                          {
                              "id": "CategoUid04"
                          }
                      ]
                  }
              ]
          }
          """;
  }
}
