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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.restassured.response.ValidatableResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.hisp.dhis.ApiTest;
import org.hisp.dhis.test.e2e.actions.LoginActions;
import org.hisp.dhis.test.e2e.actions.RestApiActions;
import org.hisp.dhis.test.e2e.actions.UserActions;
import org.hisp.dhis.test.e2e.actions.metadata.MetadataActions;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.JsonParserUtils;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class CategoryOptionComboMergeTest extends ApiTest {

  private RestApiActions categoryOptionComboApiActions;
  private RestApiActions dataElementApiActions;
  private RestApiActions minMaxActions;
  private MetadataActions metadataActions;
  private RestApiActions visualizationActions;
  private RestApiActions maintenanceApiActions;
  private RestApiActions dataValueSetActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private String sourceUid1;
  private String sourceUid2;
  private String targetUid;
  private String randomCocUid1;
  private String randomCocUid2;
  private String mergeUserId;

  @BeforeAll
  public void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    dataElementApiActions = new RestApiActions("dataElements");
    minMaxActions = new RestApiActions("minMaxDataElements");
    categoryOptionComboApiActions = new RestApiActions("categoryOptionCombos");
    metadataActions = new MetadataActions();
    maintenanceApiActions = new RestApiActions("maintenance");
    dataValueSetActions = new RestApiActions("dataValueSets");
    visualizationActions = new RestApiActions("visualizations");
    loginActions.loginAsSuperUser();

    // add user with required merge auth
    mergeUserId =
        userActions.addUserFull(
            "user", "auth", "userWithMergeAuth", "Test1234!", "F_CATEGORY_OPTION_COMBO_MERGE");
  }

  @BeforeEach
  public void setup() {
    loginActions.loginAsSuperUser();
    setupMetadata();
  }

  @AfterAll
  public void resetSuperUserOrgUnit() {
    loginActions.loginAsSuperUser();
    // reset super user to have same org unit access as setup data
    addOrgUnitAccessForUser(
        loginActions.getLoggedInUserId(),
        "ImspTQPwCqd",
        "O6uvpzGd5pu",
        "g8upMTyEZGZ",
        "YuQRtpLP10I");
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

    String dataElement = setupDataElement("test de 1");
    // import visualization to persist data dimension item which has ref to source coc
    visualizationActions.post(getViz(dataElement, sourceUid1)).validateStatus(201);

    // login as merge user
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when a category option combo request is submitted, deleting sources
    ApiResponse response =
        categoryOptionComboApiActions.post("merge", getMergeBody("DISCARD")).validateStatus(200);

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

  @Test
  @DisplayName(
      "CategoryOptionCombo merge completes successfully with DataValues (cat opt combo) handled correctly")
  void cocMergeDataValuesTest() {
    // Given
    // Generate category option combos
    maintenanceApiActions
        .post("categoryOptionComboUpdate", new QueryParamsBuilder().build())
        .validateStatus(204);

    // Get cat opt combo uids for sources and target, after generating
    sourceUid1 = getCocWithOptions("1A", "2A");
    sourceUid2 = getCocWithOptions("1A", "2B");
    targetUid = getCocWithOptions("3A", "4A");
    randomCocUid1 = getCocWithOptions("1B", "2A");
    randomCocUid2 = getCocWithOptions("1B", "2B");

    addOrgUnitAccessForUser(loginActions.getLoggedInUserId(), "OrgUnitUid1");
    addOrgUnitAccessForUser(mergeUserId, "OrgUnitUid1");

    // Add data values
    addDataValuesCoc();

    // Wait 2 seconds, so that lastUpdated values have a different value,
    // which is crucial for choosing which data values to keep/delete
    Awaitility.await().pollDelay(2, TimeUnit.SECONDS).until(() -> true);

    // Update some data values, ensures different 'lastUpdated' values for duplicate logic
    updateDataValuesCoc();

    // Confirm Data Value state before merge
    ValidatableResponse preMergeState =
        dataValueSetActions
            .get(getDataValueSetQueryParams("OrgUnitUid1"))
            .validateStatus(200)
            .validate();

    preMergeState.body("dataValues", hasSize(14));
    Set<String> uniqueDates =
        new HashSet<>(preMergeState.extract().jsonPath().getList("dataValues.lastUpdated"));
    assertTrue(uniqueDates.size() > 1, "There should be more than 1 unique date present");

    // Login as merge user
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // When a merge request using the data merge strategy 'LAST_UPDATED' is submitted
    ApiResponse response =
        categoryOptionComboApiActions
            .post("merge", getMergeBody("LAST_UPDATED"))
            .validateStatus(200);

    // Then a success response received, sources are deleted & source references were merged
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("CategoryOptionCombo merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("CategoryOptionCombo"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // And sources should no longer exist
    categoryOptionComboApiActions.get(sourceUid1).validateStatus(404);
    categoryOptionComboApiActions.get(sourceUid2).validateStatus(404);

    // And last updated duplicates are kept and earlier duplicates deleted
    ValidatableResponse postMergeState =
        dataValueSetActions
            .get(getDataValueSetQueryParams("OrgUnitUid1"))
            .validateStatus(200)
            .validate();

    postMergeState.body("dataValues", hasSize(8));

    // Check for expected values
    List<String> datValues = postMergeState.extract().jsonPath().getList("dataValues.value");
    assertTrue(datValues.contains("UPDATED source 1 DV 3 - duplicate later - KEEP"));
    assertTrue(datValues.contains("UPDATED source 2 DV 2 - duplicate later - KEEP"));
    assertTrue(datValues.contains("UPDATED target DV 4 - duplicate later - KEEP"));

    assertFalse(datValues.contains("source 1, DV 2 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("source 1, DV 4 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("source 2, DV 3 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("source 2, DV 4 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("target DV 1 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("target DV 2 - duplicate earlier - REMOVE"));

    Set<String> dvCocs =
        new HashSet<>(
            postMergeState.extract().jsonPath().getList("dataValues.categoryOptionCombo"));
    assertTrue(dvCocs.contains(targetUid), "Target COC is present");
    assertFalse(dvCocs.contains(sourceUid1), "Source COC 1 should not be present");
    assertFalse(dvCocs.contains(sourceUid2), "Source COC 2 should not be present");
  }

  @Test
  @DisplayName(
      "CategoryOptionCombo merge completes successfully with DataValues (attr opt combo) handled correctly")
  void aocMergeDataValuesTest() {
    // Given
    // Generate category option combos
    maintenanceApiActions
        .post("categoryOptionComboUpdate", new QueryParamsBuilder().build())
        .validateStatus(204);

    // Get cat opt combo uids for sources and target, after generating
    sourceUid1 = getCocWithOptions("1A", "2A");
    sourceUid2 = getCocWithOptions("1A", "2B");
    targetUid = getCocWithOptions("3A", "4A");
    randomCocUid1 = getCocWithOptions("1B", "2A");
    randomCocUid2 = getCocWithOptions("1B", "2B");

    addOrgUnitAccessForUser(loginActions.getLoggedInUserId(), "OrgUnitUid2");
    addOrgUnitAccessForUser(mergeUserId, "OrgUnitUid2");

    // Add data values
    addDataValuesAoc();

    // Wait 2 seconds, so that lastUpdated values have a different value,
    // which is crucial for choosing which data values to keep/delete
    Awaitility.await().pollDelay(2, TimeUnit.SECONDS).until(() -> true);

    // Update some data values, ensures different 'lastUpdated' values for duplicate logic
    updateDataValuesAoc();

    // Confirm Data Value state before merge
    ValidatableResponse preMergeState =
        dataValueSetActions
            .get(getDataValueSetQueryParams("OrgUnitUid2"))
            .validateStatus(200)
            .validate();

    preMergeState.body("dataValues", hasSize(14));
    Set<String> uniqueDates =
        new HashSet<>(preMergeState.extract().jsonPath().getList("dataValues.lastUpdated"));
    assertTrue(uniqueDates.size() > 1, "There should be more than 1 unique date present");

    // Login as merge user
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // When a merge request using the data merge strategy 'LAST_UPDATED' is submitted
    ApiResponse response =
        categoryOptionComboApiActions
            .post("merge", getMergeBody("LAST_UPDATED"))
            .validateStatus(200);

    // Then a success response received, sources are deleted & source references were merged
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("CategoryOptionCombo merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("CategoryOptionCombo"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // And sources should no longer exist
    categoryOptionComboApiActions.get(sourceUid1).validateStatus(404);
    categoryOptionComboApiActions.get(sourceUid2).validateStatus(404);

    // And last updated duplicates are kept and earlier duplicates deleted
    loginActions.loginAsSuperUser();
    ValidatableResponse postMergeState =
        dataValueSetActions
            .get(getDataValueSetQueryParamsWithAoc("OrgUnitUid2"))
            .validateStatus(200)
            .validate();

    postMergeState.body("dataValues", hasSize(6));

    // Check for expected values
    List<String> datValues = postMergeState.extract().jsonPath().getList("dataValues.value");
    assertTrue(datValues.contains("UPDATED source 1 DV 3 - duplicate later - KEEP"));
    assertTrue(datValues.contains("UPDATED source 2 DV 2 - duplicate later - KEEP"));
    assertTrue(datValues.contains("UPDATED target DV 4 - duplicate later - KEEP"));

    assertFalse(datValues.contains("source 1, DV 2 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("source 1, DV 4 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("source 2, DV 3 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("source 2, DV 4 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("target DV 1 - duplicate earlier - REMOVE"));
    assertFalse(datValues.contains("target DV 2 - duplicate earlier - REMOVE"));

    Set<String> dvAocs =
        new HashSet<>(
            postMergeState.extract().jsonPath().getList("dataValues.attributeOptionCombo"));
    assertTrue(dvAocs.contains(targetUid), "Target COC is present");
    assertFalse(dvAocs.contains(sourceUid1), "Source COC 1 should not be present");
    assertFalse(dvAocs.contains(sourceUid2), "Source COC 2 should not be present");
  }

  @Test
  @DisplayName("CategoryOptionCombo merge fails when user has not got the required authority")
  void categoryOptionComboMergeNoRequiredAuthTest() {
    userActions.addUserFull("basic", "User", "basicUser", "Test1234!", "NO_AUTH");
    loginActions.loginAsUser("basicUser", "Test1234!");

    // when
    ApiResponse response =
        categoryOptionComboApiActions.post("merge", getMergeBody("DISCARD")).validateStatus(403);

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
    maintenanceApiActions
        .post("categoryOptionComboUpdate", new QueryParamsBuilder().build())
        .validateStatus(204);

    // get cat opt combo uids for sources and target, after generating
    sourceUid1 = getCocWithOptions("1A", "2A");
    sourceUid2 = getCocWithOptions("1B", "2B");
    targetUid = getCocWithOptions("3A", "4B");

    String dataElement = setupDataElement("DE test");

    setupMinMaxDataElements(sourceUid1, sourceUid2, targetUid, dataElement);

    // login as user with merge auth
    loginActions.loginAsUser("userWithMergeAuth", "Test1234!");

    // when
    ApiResponse response =
        categoryOptionComboApiActions.post("merge", getMergeBody("DISCARD")).validateStatus(409);

    // then
    response
        .validate()
        .statusCode(409)
        .body("httpStatus", equalTo("Conflict"))
        .body("status", equalTo("ERROR"))
        .body("message", containsString("ERROR: duplicate key value violates unique constraint"))
        .body("message", containsString("minmaxdataelement_unique_key"));
  }

  private String getViz(String dataElement, String coc) {
    return """

          {
            "name": "Test viz with data dimension item - DE operand",
            "displayName": "Test 1",
            "type": "PIVOT_TABLE",
            "filters": [
              {
                "dimension": "ou",
                "items": [
                  {
                    "id": "USER_ORGUNIT"
                  }
                ]
              }
            ],
            "columns": [
              {
                "dimension": "dx",
                "items": [
                  {
                    "id": "%s.%s",
                    "dimensionItemType": "DATA_ELEMENT_OPERAND"
                  }
                ]
              }
            ],
            "rows": [
              {
                "dimension": "pe",
                "items": [
                  {
                    "id": "LAST_10_YEARS"
                  }
                ]
              }
            ]
          }
          """
        .formatted(dataElement, coc);
  }

  private QueryParamsBuilder getDataValueQueryParams() {
    return new QueryParamsBuilder()
        .add("async=false")
        .add("dryRun=false")
        .add("strategy=NEW_AND_UPDATES")
        .add("preheatCache=false")
        .add("dataElementIdScheme=UID")
        .add("orgUnitIdScheme=UID")
        .add("idScheme=UID")
        .add("format=json")
        .add("skipExistingCheck=false");
  }

  private String getDataValueSetQueryParams(String orgUnit) {
    return new QueryParamsBuilder()
        .add("orgUnit=%s")
        .add("startDate=2024-01-01")
        .add("endDate=2050-01-30")
        .add("dataElement=deUid000001")
        .build()
        .formatted(orgUnit);
  }

  private String getDataValueSetQueryParamsWithAoc(String orgUnit) {
    return new QueryParamsBuilder()
        .add("orgUnit=%s")
        .add("startDate=2024-01-01")
        .add("endDate=2050-01-30")
        .add("dataElement=deUid000001")
        .add("attributeOptionCombo=" + targetUid)
        .build()
        .formatted(orgUnit);
  }

  private void addOrgUnitAccessForUser(String loggedInUserId, String... orgUnitUids) {
    JsonArray orgUnits = new JsonArray();
    for (String orgUnit : orgUnitUids) {
      orgUnits.add(JsonObjectBuilder.jsonObject().addProperty("id", orgUnit).build());
    }
    JsonObject userPatch =
        JsonObjectBuilder.jsonObject()
            .addProperty("op", "add")
            .addProperty("path", "/organisationUnits")
            .addArray("value", orgUnits)
            .build();

    userActions.patch(loggedInUserId, Collections.singletonList(userPatch)).validateStatus(200);
  }

  private void addDataValuesCoc() {
    dataValueSetActions
        .post(
            dataValueSetImportCoc(sourceUid1, sourceUid2, targetUid, randomCocUid1, randomCocUid2),
            getDataValueQueryParams())
        .validateStatus(200)
        .validate()
        .body("response.importCount.imported", equalTo(14));
  }

  private void addDataValuesAoc() {
    dataValueSetActions
        .post(
            dataValueSetImportAoc(sourceUid1, sourceUid2, targetUid, randomCocUid1, randomCocUid2),
            getDataValueQueryParams())
        .validateStatus(200);
  }

  private void updateDataValuesCoc() {
    dataValueSetActions
        .post(
            dataValueSetImportUpdateCoc(sourceUid1, sourceUid2, targetUid),
            getDataValueQueryParams())
        .validateStatus(200)
        .validate()
        .body("response.importCount.updated", equalTo(4));
  }

  private void updateDataValuesAoc() {
    dataValueSetActions
        .post(
            dataValueSetImportUpdateAoc(sourceUid1, sourceUid2, targetUid),
            getDataValueQueryParams())
        .validateStatus(200)
        .validate()
        .body("response.importCount.updated", equalTo(4));
  }

  private void setupMetadata() {
    metadataActions.importMetadata(metadata()).validateStatus(200);
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

  private String setupDataElement(String name) {
    return dataElementApiActions
        .post(
            """
            {
               "aggregationType": "DEFAULT",
               "domainType": "AGGREGATE",
               "name": "%s",
               "shortName": "%s",
               "valueType": "TEXT"
             }
             """
                .formatted(name, name))
        .validateStatus(201)
        .extractUid();
  }

  private JsonObject getMergeBody(String dataMergeStrategy) {
    JsonObject json = new JsonObject();
    JsonArray sources = new JsonArray();
    sources.add(sourceUid1);
    sources.add(sourceUid2);
    json.add("sources", sources);
    json.addProperty("target", targetUid);
    json.addProperty("deleteSources", true);
    json.addProperty("dataMergeStrategy", dataMergeStrategy);
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
                          },
                          {
                              "id": "OrgUnitUid2"
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
                          },
                          {
                              "id": "OrgUnitUid2"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid2A",
                      "name": "cat opt 2A",
                      "shortName": "cat opt 2A",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid1"
                          },
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
                              "id": "OrgUnitUid1"
                          },
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
                              "id": "OrgUnitUid1"
                          },
                          {
                              "id": "OrgUnitUid2"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid3B",
                      "name": "cat opt 3B",
                      "shortName": "cat opt 3B",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid1"
                          },
                          {
                              "id": "OrgUnitUid2"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid4A",
                      "name": "cat opt 4A",
                      "shortName": "cat opt 4A",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid1"
                          },
                          {
                              "id": "OrgUnitUid2"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUid4B",
                      "name": "cat opt 4B",
                      "shortName": "cat opt 4B",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUid1"
                          },
                          {
                              "id": "OrgUnitUid2"
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
                      "openingDate": "2023-06-15",
                      "parent": {
                        "id": "DiszpKrYNg8"
                      }
                  },
                  {
                      "id": "OrgUnitUid2",
                      "name": "org 2",
                      "shortName": "org 2",
                      "openingDate": "2024-06-15",
                      "parent": {
                        "id": "DiszpKrYNg8"
                      }
                  },
                  {
                      "id": "OrgUnitUid3",
                      "name": "org 3",
                      "shortName": "org 3",
                      "openingDate": "2023-09-15",
                      "parent": {
                        "id": "DiszpKrYNg8"
                      }
                  },
                  {
                      "id": "OrgUnitUid4",
                      "name": "org 4",
                      "shortName": "org 4",
                      "openingDate": "2023-06-25",
                      "parent": {
                        "id": "DiszpKrYNg8"
                      }
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
              ],
              "dataElements": [
                  {
                      "id": "deUid000001",
                      "aggregationType": "DEFAULT",
                      "domainType": "AGGREGATE",
                      "name": "DE for DVs",
                      "shortName": "DE for DVs",
                      "valueType": "TEXT"
                  }
              ]
          }
          """;
  }

  private JsonObject dataValueSetImportCoc(
      String source1Coc,
      String source2Coc,
      String targetCoc,
      String randomCoc1,
      String randomCoc2) {
    return JsonParserUtils.toJsonObject(
        """
          {
              "dataValues": [
                  {
                      "dataElement": "deUid000001",
                      "period": "202405",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 1, DV 1 - non duplicate earlier - KEEP",
                      "comment": "source 1, DV 1 - non duplicate earlier - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 1, DV 2 - duplicate earlier - REMOVE",
                      "comment": "source 1, DV 2 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202409",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 1, DV 3 - duplicate later - KEEP",
                      "comment": "source 1, DV 3 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202407",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 1, DV 4 - duplicate earlier - REMOVE",
                      "comment": "source 1, DV 4 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202410",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 2, DV 1 - non duplicate later - KEEP",
                      "comment": "source 2, DV 1 - non duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 2, DV 2 - duplicate later - KEEP",
                      "comment": "source 2, DV 2 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202409",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 2, DV 3 - duplicate earlier - REMOVE",
                      "comment": "source 2, DV 3 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202407",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 2, DV 4 - duplicate earlier - REMOVE",
                      "comment": "source 2, DV 4 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "target DV 1 - duplicate earlier - REMOVE",
                      "comment": "target DV 1 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202409",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "target DV 2 - duplicate earlier - REMOVE",
                      "comment": "target DV 2 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202403",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "target DV 3 - not impacted - KEEP",
                      "comment": "target DV 3 - not impacted - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202407",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "target DV 4 - duplicate later- KEEP",
                      "comment": "target DV 4 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "random 1, DV 1 - not impacted",
                      "comment": "random 1, DV 1 - not impacted"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "random 2, DV 2 - not impacted",
                      "comment": "random 2, DV 2 - not impacted"
                  }
              ]
          }
          """
            .formatted(
                source1Coc,
                source1Coc,
                source1Coc,
                source1Coc,
                source2Coc,
                source2Coc,
                source2Coc,
                source2Coc,
                targetCoc,
                targetCoc,
                targetCoc,
                targetCoc,
                randomCoc1,
                randomCoc2));
  }

  private JsonObject dataValueSetImportAoc(
      String source1Coc,
      String source2Coc,
      String targetCoc,
      String randomCoc1,
      String randomCoc2) {
    return JsonParserUtils.toJsonObject(
        """
          {
              "dataValues": [
                  {
                      "dataElement": "deUid000001",
                      "period": "202405",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "source 1, DV 1 - non duplicate earlier - KEEP",
                      "comment": "source 1, DV 1 - non duplicate earlier - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "source 1, DV 2 - duplicate earlier - REMOVE",
                      "comment": "source 1, DV 2 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202409",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "source 1, DV 3 - duplicate later - KEEP",
                      "comment": "source 1, DV 3 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202407",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "source 1, DV 4 - duplicate earlier - REMOVE",
                      "comment": "source 1, DV 4 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202410",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "source 2, DV 1 - non duplicate later - KEEP",
                      "comment": "source 2, DV 1 - non duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "source 2, DV 2 - duplicate later - KEEP",
                      "comment": "source 2, DV 2 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202409",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "source 2, DV 3 - duplicate earlier - REMOVE",
                      "comment": "source 2, DV 3 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202407",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "source 2, DV 4 - duplicate earlier - REMOVE",
                      "comment": "source 2, DV 4 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "target DV 1 - duplicate earlier - REMOVE",
                      "comment": "target DV 1 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202409",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "target DV 2 - duplicate earlier - REMOVE",
                      "comment": "target DV 2 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202403",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "target DV 3 - not impacted - KEEP",
                      "comment": "target DV 3 - not impacted - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202407",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "target DV 4 - duplicate later- KEEP",
                      "comment": "target DV 4 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "random 1, DV 1 - not impacted",
                      "comment": "random 1, DV 1 - not impacted"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "random 2, DV 2 - not impacted",
                      "comment": "random 2, DV 2 - not impacted"
                  }
              ]
          }
          """
            .formatted(
                source1Coc,
                source1Coc,
                source1Coc,
                source1Coc,
                source2Coc,
                source2Coc,
                source2Coc,
                source2Coc,
                targetCoc,
                targetCoc,
                targetCoc,
                targetCoc,
                randomCoc1,
                randomCoc2));
  }

  private JsonObject dataValueSetImportUpdateAoc(
      String source1Coc, String source2Coc, String targetCoc) {
    return JsonParserUtils.toJsonObject(
        """
          {
              "dataValues": [
                  {
                      "dataElement": "deUid000001",
                      "period": "202409",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "UPDATED source 1 DV 3 - duplicate later - KEEP",
                      "comment": "source 1, DV 3 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202410",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "UPDATED source 2 DV 1 - non duplicate later - KEEP",
                      "comment": "source 2, DV 1 - non duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "UPDATED source 2 DV 2 - duplicate later - KEEP",
                      "comment": "source 2, DV 2 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202407",
                      "orgUnit": "OrgUnitUid2",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "%s",
                      "value": "UPDATED target DV 4 - duplicate later - KEEP",
                      "comment": "target DV 4 - duplicate later - KEEP"
                  }
              ]
          }
          """
            .formatted(source1Coc, source2Coc, source2Coc, targetCoc));
  }

  private JsonObject dataValueSetImportUpdateCoc(
      String source1Coc, String source2Coc, String targetCoc) {
    return JsonParserUtils.toJsonObject(
        """
          {
              "dataValues": [
                  {
                      "dataElement": "deUid000001",
                      "period": "202409",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "UPDATED source 1 DV 3 - duplicate later - KEEP",
                      "comment": "source 1, DV 3 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202410",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "UPDATED source 2 DV 1 - non duplicate later - KEEP",
                      "comment": "source 2, DV 1 - non duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202408",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "UPDATED source 2 DV 2 - duplicate later - KEEP",
                      "comment": "source 2, DV 2 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "deUid000001",
                      "period": "202407",
                      "orgUnit": "OrgUnitUid1",
                      "categoryOptionCombo": "%s",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "UPDATED target DV 4 - duplicate later - KEEP",
                      "comment": "target DV 4 - duplicate later - KEEP"
                  }
              ]
          }
          """
            .formatted(source1Coc, source2Coc, source2Coc, targetCoc));
  }
}
