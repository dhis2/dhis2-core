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

import static org.hamcrest.Matchers.containsString;
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

class CategoryOptionComboMergeTest extends ApiTest {

  private RestApiActions categoryOptionComboApiActions;
  private RestApiActions dataElementApiActions;
  private RestApiActions minMaxActions;
  private MetadataActions metadataActions;
  private RestApiActions maintenanceApiActions;
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
    minMaxActions = new RestApiActions("minMaxDataElements");
    categoryOptionComboApiActions = new RestApiActions("categoryOptionCombos");
    metadataActions = new MetadataActions();
    maintenanceApiActions = new RestApiActions("maintenance");
    loginActions.loginAsSuperUser();

    // add user with required merge auth
    userActions.addUserFull(
        "user", "auth", "userWithMergeAuth", "Test1234!", "F_CATEGORY_OPTION_COMBO_MERGE");
  }

  @BeforeEach
  public void setup() {
    loginActions.loginAsSuperUser();
    setupMetadata();
  }

  @Test
  @DisplayName(
      "Valid CategoryOptionCombo merge completes successfully with all source CategoryOptionCombo refs replaced with target CategoryOptionCombo")
  void validCategoryOptionComboMergeTest() {
    // given
    // generate category option combos
    String emptyParams = new QueryParamsBuilder().build();
    maintenanceApiActions
        .post("categoryOptionComboUpdate/categoryCombo/CatComUid01", emptyParams)
        .validateStatus(200);
    maintenanceApiActions
        .post("categoryOptionComboUpdate/categoryCombo/CatComUid02", emptyParams)
        .validateStatus(200);

    // get cat opt combo uids for sources and target, after generating
    sourceUid1 = getCocWithOptions("1A", "2A");
    sourceUid2 = getCocWithOptions("1B", "2B");
    targetUid = getCocWithOptions("3A", "4B");

    // confirm state before merge
    ValidatableResponse preMergeState =
        categoryOptionComboApiActions.get(targetUid).validateStatus(200).validate();

    preMergeState
        .body("categoryCombo", hasEntry("id", "CatComUid02"))
        .body("categoryOptions", hasSize(equalTo(2)))
        .body("categoryOptions", hasItem(hasEntry("id", "CatOptUid4B")))
        .body("categoryOptions", hasItem(hasEntry("id", "CatOptUid3A")));

    // login as merge user
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a category option combo request is submitted, deleting sources
    ApiResponse response =
        categoryOptionComboApiActions.post("merge", getMergeBody()).validateStatus(200);

    // then a success response received, sources are deleted & source references were merged
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("CategoryOptionCombo merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("CategoryOptionCombo"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    categoryOptionComboApiActions.get(sourceUid1).validateStatus(404);
    categoryOptionComboApiActions.get(sourceUid2).validateStatus(404);
    ValidatableResponse postMergeState =
        categoryOptionComboApiActions.get(targetUid).validateStatus(200).validate();

    postMergeState
        .body("categoryCombo", hasEntry("id", "CatComUid02"))
        .body("categoryOptions", hasSize(equalTo(6)))
        .body(
            "categoryOptions",
            hasItems(
                hasEntry("id", "CatOptUid1A"),
                hasEntry("id", "CatOptUid2B"),
                hasEntry("id", "CatOptUid3A"),
                hasEntry("id", "CatOptUid4B"),
                hasEntry("id", "CatOptUid2A"),
                hasEntry("id", "CatOptUid1B")));
  }

  private void setupMetadata() {
    metadataActions.post(metadata()).validateStatus(200);
  }

  @Test
  @DisplayName("CategoryOptionCombo merge fails when user has not got the required authority")
  void categoryOptionComboMergeNoRequiredAuthTest() {
    userActions.addUserFull("basic", "User", "basicUser", "Test1234!", "NO_AUTH");
    loginActions.loginAsUser("basicUser", "Test1234!");

    // when
    ApiResponse response =
        categoryOptionComboApiActions.post("merge", getMergeBody()).validateStatus(403);

    // then
    response
        .validate()
        .statusCode(403)
        .body("httpStatus", equalTo("Forbidden"))
        .body("status", equalTo("ERROR"))
        .body(
            "message",
            equalTo(
                "Access is denied, requires one Authority from [F_CATEGORY_OPTION_COMBO_MERGE]"));
  }

  @Test
  @DisplayName("Category Option Combo merge fails when min max DE DB unique key constraint met")
  void dbConstraintMinMaxTest() {
    // given
    // generate category option combos
    String emptyParams = new QueryParamsBuilder().build();
    maintenanceApiActions
        .post("categoryOptionComboUpdate/categoryCombo/CatComUid01", emptyParams)
        .validateStatus(200);
    maintenanceApiActions
        .post("categoryOptionComboUpdate/categoryCombo/CatComUid02", emptyParams)
        .validateStatus(200);

    // get cat opt combo uids for sources and target, after generating
    sourceUid1 = getCocWithOptions("1A", "2A");
    sourceUid2 = getCocWithOptions("1B", "2B");
    targetUid = getCocWithOptions("3A", "4B");

    String dataElement = setupDataElement("9", "TEXT", "AGGREGATE");

    setupMinMaxDataElements(sourceUid1, sourceUid2, targetUid, dataElement);

    // login as user with merge auth
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when
    ApiResponse response =
        categoryOptionComboApiActions.post("merge", getMergeBody()).validateStatus(409);

    // then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("message", containsString("ERROR: duplicate key value violates unique constraint"))
        .body("message", containsString("minmaxdataelement_unique_key"));
  }

  private void setupMinMaxDataElements(
      String sourceUid1, String sourceUid2, String targetUid, String dataElement) {
    minMaxActions.post(minMaxDataElements(sourceUid1, dataElement));
    minMaxActions.post(minMaxDataElements(sourceUid2, dataElement));
    minMaxActions.post(minMaxDataElements(targetUid, dataElement));
  }

  private String minMaxDataElements(String coc, String de) {
    return """
          {
               "min": 2,
               "max": 11,
               "generated": false,
               "source": {
                   "id": "OrgUnitUid1"
               },
               "dataElement": {
                   "id": "%s"
               },
               "optionCombo": {
                   "id": "%s"
               }
           }
          """
        .formatted(de, coc);
  }

  private String setupDataElement(String uniqueChar, String valueType, String domainType) {
    return dataElementApiActions
        .post(createDataElement("source 1" + uniqueChar, valueType, domainType))
        .validateStatus(201)
        .extractUid();
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

  private JsonObject getMergeBody() {
    JsonObject json = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add(sourceUid1);
    sources.add(sourceUid2);
    json.add("sources", sources);
    json.addProperty("target", targetUid);
    json.addProperty("deleteSources", true);
    json.addProperty("dataMergeStrategy", "DISCARD");
    return json;
  }

  private String getCocWithOptions(String co1, String co2) {

    return categoryOptionComboApiActions
        .get(
            new QueryParamsBuilder()
                .addAll("filter=name:like:%s".formatted(co1), "filter=name:like:%s".formatted(co2)))
        .validate()
        .extract()
        .jsonPath()
        .get("categoryOptionCombos[0].id")
        .toString();
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
