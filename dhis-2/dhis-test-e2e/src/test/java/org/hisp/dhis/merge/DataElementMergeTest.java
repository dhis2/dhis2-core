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

import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.restassured.response.ValidatableResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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

class DataElementMergeTest extends ApiTest {

  private RestApiActions dataElementApiActions;
  private RestApiActions dataValueSetActions;
  private RestApiActions maintenanceApiActions;
  private RestApiActions datasetApiActions;
  private RestApiActions categoryComboApiActions;
  private MetadataActions metadataApiActions;
  private RestApiActions minMaxActions;
  private UserActions userActions;
  private LoginActions loginActions;
  private String sourceUid1;
  private String sourceUid2;
  private String targetUid;
  private String randomUid;

  @BeforeAll
  public void before() {
    userActions = new UserActions();
    loginActions = new LoginActions();
    dataElementApiActions = new RestApiActions("dataElements");
    datasetApiActions = new RestApiActions("dataSets");
    metadataApiActions = new MetadataActions();
    minMaxActions = new RestApiActions("minMaxDataElements");
    categoryComboApiActions = new RestApiActions("categoryCombos");
    dataValueSetActions = new RestApiActions("dataValueSets");
    maintenanceApiActions = new RestApiActions("maintenance");
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
  @DisplayName("DataElement merge fails when min max DE DB unique key constraint met")
  void dbConstraintMinMaxTest() {
    // given
    sourceUid1 = setupDataElement("9", "TEXT", "AGGREGATE");
    sourceUid2 = setupDataElement("8", "TEXT", "AGGREGATE");
    targetUid = setupDataElement("7", "TEXT", "AGGREGATE");

    metadataApiActions.importMetadata(metadata()).validateStatus(200);
    // generate category option combos
    maintenanceApiActions
        .post("categoryOptionComboUpdate", new QueryParamsBuilder().build())
        .validateStatus(200);

    // get cat opt combo ID to use in min max data elements
    String cocId =
        categoryComboApiActions
            .get("CatCombUID1")
            .validateStatus(200)
            .validate()
            .extract()
            .jsonPath()
            .get("categoryOptionCombos[0].id")
            .toString();

    setupMinMaxDataElements(sourceUid1, sourceUid2, targetUid, cocId);

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
        .body("message", containsString("minmaxdataelement_pkey"));
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

  @Test
  @DisplayName("DataElement merge completes successfully with DataValues handled correctly")
  void deMergeDataValuesTest() {
    // Given
    sourceUid1 = setupDataElement("j", "TEXT", "AGGREGATE");
    sourceUid2 = setupDataElement("o", "TEXT", "AGGREGATE");
    targetUid = setupDataElement("p", "TEXT", "AGGREGATE");
    randomUid = setupDataElement("z", "TEXT", "AGGREGATE");
    String dataSetUid = setupDataSet(sourceUid1, sourceUid2, targetUid, randomUid);
    assertNotNull(dataSetUid);

    addOrgUnitAccessForUser(loginActions.getLoggedInUserId(), "OrgUnitUID1");

    // Add data values
    addDataValues();

    // Wait 2 seconds, so that lastUpdated values have a different value,
    // which is crucial for choosing which data values to keep/delete
    Awaitility.await().pollDelay(2, TimeUnit.SECONDS).until(() -> true);

    // Update some data values, ensures different 'lastUpdated' values for duplicate logic
    updateDataValues();

    // Confirm Data Value state before merge
    checkDvState(sourceUid1, 4, 1);
    checkDvState(sourceUid2, 4, 1);
    checkDvState(targetUid, 4, 1);
    checkDvState(randomUid, 2, 0);

    // When a merge request using the data merge strategy 'LAST_UPDATED' is submitted
    ApiResponse response =
        dataElementApiActions
            .post("merge", getMergeBody(sourceUid1, sourceUid2, targetUid, true, "LAST_UPDATED"))
            .validateStatus(200);

    // Then a success response received, sources are deleted & source references were merged
    response
        .validate()
        .statusCode(200)
        .body("httpStatus", equalTo("OK"))
        .body("response.mergeReport.message", equalTo("DataElement merge complete"))
        .body("response.mergeReport.mergeErrors", empty())
        .body("response.mergeReport.mergeType", equalTo("DataElement"))
        .body("response.mergeReport.sourcesDeleted", hasItems(sourceUid1, sourceUid2));

    // And sources should no longer exist
    dataElementApiActions.get(sourceUid1).validateStatus(404);
    dataElementApiActions.get(sourceUid2).validateStatus(404);

    // And last updated duplicates are kept and earlier duplicates deleted
    ValidatableResponse postMergeState =
        dataValueSetActions
            .get(getDataValueSetQueryParams("OrgUnitUID1", targetUid))
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

    // And sources and random DE have expected results
    dataValueSetActions
        .get(getDataValueSetQueryParams("OrgUnitUID1", sourceUid1))
        .validateStatus(409)
        .validate()
        .body(
            "message",
            equalTo("At least one data element, data set or data element group must be specified"));

    dataValueSetActions
        .get(getDataValueSetQueryParams("OrgUnitUID1", sourceUid2))
        .validateStatus(409)
        .validate()
        .body(
            "message",
            equalTo("At least one data element, data set or data element group must be specified"));

    dataValueSetActions
        .get(getDataValueSetQueryParams("OrgUnitUID1", randomUid))
        .validateStatus(200)
        .validate()
        .body("dataValues", hasSize(2));
  }

  void checkDvState(String deUid, int expectedDvCount, int expectedDateCount) {
    ValidatableResponse preMergeState =
        dataValueSetActions
            .get(getDataValueSetQueryParams("OrgUnitUID1", deUid))
            .validateStatus(200)
            .validate();

    preMergeState.body("dataValues", hasSize(expectedDvCount));
    Set<String> uniqueDates =
        new HashSet<>(preMergeState.extract().jsonPath().getList("dataValues.lastUpdated"));
    assertTrue(
        uniqueDates.size() > expectedDateCount,
        "There should be more than %s unique date present".formatted(expectedDateCount));
  }

  private void addDataValues() {
    dataValueSetActions
        .post(
            dataValueSetImport(sourceUid1, sourceUid2, targetUid, randomUid),
            getDataValueQueryParams())
        .validateStatus(200)
        .validate()
        .body("response.importCount.updated", equalTo(14));
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

  private void updateDataValues() {
    dataValueSetActions
        .post(
            dataValueSetImportUpdateCoc(sourceUid1, sourceUid2, targetUid),
            getDataValueQueryParams())
        .validateStatus(200)
        .validate()
        .body("response.importCount.updated", equalTo(4));
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

  private String getDataValueSetQueryParams(String orgUnit, String de) {
    return new QueryParamsBuilder()
        .add("orgUnit=%s")
        .add("startDate=2024-01-01")
        .add("endDate=2050-01-30")
        .add("dataElement=%s")
        .build()
        .formatted(orgUnit, de);
  }

  private JsonObject dataValueSetImport(
      String source1De, String source2De, String targetDe, String randomDe) {
    return JsonParserUtils.toJsonObject(
        """
          {
              "dataValues": [
                  {
                      "dataElement": "%s",
                      "period": "202405",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 1, DV 1 - non duplicate earlier - KEEP",
                      "comment": "source 1, DV 1 - non duplicate earlier - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202408",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 1, DV 2 - duplicate earlier - REMOVE",
                      "comment": "source 1, DV 2 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202409",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 1, DV 3 - duplicate later - KEEP",
                      "comment": "source 1, DV 3 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202407",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 1, DV 4 - duplicate earlier - REMOVE",
                      "comment": "source 1, DV 4 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202410",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 2, DV 1 - non duplicate later - KEEP",
                      "comment": "source 2, DV 1 - non duplicate later - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202408",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 2, DV 2 - duplicate later - KEEP",
                      "comment": "source 2, DV 2 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202409",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 2, DV 3 - duplicate earlier - REMOVE",
                      "comment": "source 2, DV 3 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202407",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "source 2, DV 4 - duplicate earlier - REMOVE",
                      "comment": "source 2, DV 4 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202408",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "target DV 1 - duplicate earlier - REMOVE",
                      "comment": "target DV 1 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202409",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "target DV 2 - duplicate earlier - REMOVE",
                      "comment": "target DV 2 - duplicate earlier - REMOVE"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202403",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "target DV 3 - not impacted - KEEP",
                      "comment": "target DV 3 - not impacted - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202407",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "target DV 4 - duplicate later- KEEP",
                      "comment": "target DV 4 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202408",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "random 1, DV 1 - not impacted",
                      "comment": "random 1, DV 1 - not impacted"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202409",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "random 2, DV 2 - not impacted",
                      "comment": "random 2, DV 2 - not impacted"
                  }
              ]
          }
          """
            .formatted(
                source1De, source1De, source1De, source1De, source2De, source2De, source2De,
                source2De, targetDe, targetDe, targetDe, targetDe, randomDe, randomDe));
  }

  private JsonObject dataValueSetImportUpdateCoc(
      String source1De, String source2De, String targetDe) {
    return JsonParserUtils.toJsonObject(
        """
          {
              "dataValues": [
                  {
                      "dataElement": "%s",
                      "period": "202409",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "UPDATED source 1 DV 3 - duplicate later - KEEP",
                      "comment": "source 1, DV 3 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202410",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "UPDATED source 2 DV 1 - non duplicate later - KEEP",
                      "comment": "source 2, DV 1 - non duplicate later - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202408",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "UPDATED source 2 DV 2 - duplicate later - KEEP",
                      "comment": "source 2, DV 2 - duplicate later - KEEP"
                  },
                  {
                      "dataElement": "%s",
                      "period": "202407",
                      "orgUnit": "OrgUnitUID1",
                      "categoryOptionCombo": "HllvX50cXC0",
                      "attributeOptionCombo": "HllvX50cXC0",
                      "value": "UPDATED target DV 4 - duplicate later - KEEP",
                      "comment": "target DV 4 - duplicate later - KEEP"
                  }
              ]
          }
          """
            .formatted(source1De, source2De, source2De, targetDe));
  }

  private String setupDataElement(String uniqueChar, String valueType, String domainType) {
    return dataElementApiActions
        .post(createDataElement("source 1" + uniqueChar, valueType, domainType))
        .validateStatus(201)
        .extractUid();
  }

  private String setupDataSet(String... deIds) {
    return datasetApiActions.post(createDataset(deIds)).validateStatus(201).extractUid();
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

  private void setupMinMaxDataElements(
      String sourceUid1, String sourceUid2, String targetUid, String coc) {
    minMaxActions.post(minMaxDataElements(sourceUid1, coc));
    minMaxActions.post(minMaxDataElements(sourceUid2, coc));
    minMaxActions.post(minMaxDataElements(targetUid, coc));
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

  private String minMaxDataElements(String de, String coc) {
    return """
    {
         "min": 2,
         "max": 11,
         "generated": false,
         "source": {
             "id": "OrgUnitUID1"
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

  private String createDataset(String... deIds) {
    String de =
        Stream.of(deIds)
            .map(uid -> "{'dataElement': {'id': '%s'}}".formatted(uid).replace('\'', '"'))
            .collect(joining(","));
    return """
      {
        "name": "ds1",
        "shortName": "ds1",
        "periodType": "Monthly",
        "dataSetElements": [%s],
        "organisationUnits": [{ "id": "OrgUnitUID1"}]
      }
    """
        .formatted(de);
  }

  private String metadata() {
    return """
          {
              "categoryOptions": [
                  {
                      "id": "CatOptUID1A",
                      "name": "cat option 1A",
                      "shortName": "cat option 1A",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUID1"
                          }
                      ]
                  },
                  {
                      "id": "CatOptUID1B",
                      "name": "cat option 1B",
                      "shortName": "cat option 1B",
                      "organisationUnits": [
                          {
                              "id": "OrgUnitUID1"
                          }
                      ]
                  }
              ],
              "categories": [
                  {
                      "id": "CategoUID01",
                      "name": "category 1",
                      "shortName": "category 1",
                      "dataDimensionType": "DISAGGREGATION",
                      "categoryOptions": [
                          {
                              "id": "CatOptUID1A"
                          }
                      ]
                  },
                  {
                      "id": "CategoUID02",
                      "name": "category 2",
                      "shortName": "category 2",
                      "dataDimensionType": "DISAGGREGATION",
                      "categoryOptions": [
                          {
                              "id": "CatOptUID1B"
                          }
                      ]
                  }
              ],
              "organisationUnits": [
                  {
                      "id": "OrgUnitUID1",
                      "name": "org u 1",
                      "shortName": "org u 1",
                      "openingDate": "2023-06-15",
                      "parent": {
                        "id": "DiszpKrYNg8"
                      }
                  },
                  {
                      "id": "OrgUnitUID2",
                      "name": "org u 2",
                      "shortName": "org u 2",
                      "openingDate": "2024-06-15",
                      "parent": {
                        "id": "DiszpKrYNg8"
                      }
                  }
              ],
              "categoryCombos": [
                  {
                      "id": "CatCombUID1",
                      "name": "category combo 1",
                      "dataDimensionType": "DISAGGREGATION",
                      "categories": [
                          {
                              "id": "CategoUID01"
                          },
                          {
                              "id": "CategoUID02"
                          }
                      ]
                  }
              ]
          }
          """;
  }
}
