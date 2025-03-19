/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.imports.trackedEntities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.dto.ApiResponse;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.RelationshipDataBuilder;
import org.hisp.dhis.tracker.imports.databuilder.TrackedEntityDataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackedEntityImportTests extends TrackerApiTest {
  private String teId;

  @BeforeAll
  public void beforeAll() throws Exception {
    loginActions.loginAsSuperUser();

    teId = super.importTrackedEntity();
  }

  @Test
  public void shouldImportTrackedEntity() {
    // arrange
    JsonObject trackedEntities =
        new TrackedEntityDataBuilder()
            .setTrackedEntityType(Constants.TRACKED_ENTITY_TYPE)
            .setOrgUnit(Constants.ORG_UNIT_IDS[0])
            .array();

    // act
    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(trackedEntities);

    response
        .validateSuccessfulImport()
        .validateTrackedEntities()
        .body("stats.created", equalTo(1))
        .body("objectReports", notNullValue())
        .body("objectReports[0].errorReports", empty());

    // assert that the te was imported
    String teId = response.extractImportedTrackedEntities().get(0);

    ApiResponse teResponse = trackerImportExportActions.getTrackedEntity(teId);

    teResponse.validate().statusCode(200);

    assertThat(
        teResponse.getBody(),
        matchesJSON(trackedEntities.get("trackedEntities").getAsJsonArray().get(0)));
  }

  @Test
  public void shouldImportTrackedEntityAndEnrollmentWithAttributes() throws Exception {
    JsonObject teBody =
        new FileReaderUtils()
            .readJsonAndGenerateData(
                new File(
                    "src/test/resources/tracker/importer/trackedEntities/trackedEntityWithEnrollmentAndAttributes.json"));

    // act
    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(teBody);

    // assert
    response
        .validateSuccessfulImport()
        .validate()
        .body("stats.created", equalTo(2))
        .rootPath("bundleReport.typeReportMap")
        .body("TRACKED_ENTITY.objectReports", hasSize(1))
        .body("ENROLLMENT.objectReports", hasSize(1));

    // assert that the TE was imported
    String teId = response.extractImportedTrackedEntities().get(0);

    ApiResponse teResponse = trackerImportExportActions.getTrackedEntity(teId);

    teResponse.validate().statusCode(200);
  }

  @Test
  public void shouldImportTrackedEntitysWithEnrollmentsEventsAndRelationship() throws Exception {
    JsonObject tePayload =
        new FileReaderUtils()
            .readJsonAndGenerateData(
                new File(
                    "src/test/resources/tracker/importer/trackedEntities/trackedEntitiesWithEnrollmentsEventsAndRelationships.json"));

    JsonObject teToTrackedEntityRelationship =
        new RelationshipDataBuilder()
            .buildTrackedEntityRelationship("v4LGvFNxdYH", "bXanyK15d68", "xLmPUYJX8Ks");

    JsonObjectBuilder.jsonObject(tePayload)
        .addArray("relationships", teToTrackedEntityRelationship);

    // act
    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(tePayload);

    response
        .validateSuccessfulImport()
        .validate()
        .body("stats.created", equalTo(7))
        .rootPath("bundleReport.typeReportMap")
        .body("TRACKED_ENTITY.objectReports", hasSize(2))
        .body("ENROLLMENT.objectReports", hasSize(2))
        .body("EVENT.objectReports", hasSize(2))
        .body("RELATIONSHIP.objectReports", hasSize(1));

    JsonObject teBody = tePayload.get("trackedEntities").getAsJsonArray().get(0).getAsJsonObject();

    ApiResponse trackedEntityResponse =
        trackerImportExportActions
            .getTrackedEntity(teBody.get("trackedEntity").getAsString() + "?fields=*")
            .validateStatus(200);

    // Compare TrackedEntity Relationship
    assertThat(
        trackedEntityResponse.getBody().getAsJsonArray("relationships").get(0),
        matchesJSON(teToTrackedEntityRelationship));

    // Compare Enrollment attributes and events
    JsonArray enrollmentsAttributes =
        trackedEntityResponse
            .getBody()
            .getAsJsonArray("enrollments")
            .get(0)
            .getAsJsonObject()
            .getAsJsonArray("attributes");

    List<String> expectedAttributes = new ArrayList<>();

    enrollmentsAttributes.forEach(
        ea -> expectedAttributes.add(ea.getAsJsonObject().get("attribute").getAsString()));

    assertThat(
        teBody.getAsJsonObject("events"),
        equalTo(trackedEntityResponse.getBody().getAsJsonObject("events")));
    assertThat(expectedAttributes, containsInAnyOrder("kZeSYCgaHTk", "dIVt4l5vIOa"));
  }

  Stream<Arguments> shouldImportTrackedEntityAttributes() {
    return Stream.of(
        Arguments.of("xuAl9UIMcmI", Constants.ORG_UNIT_IDS[1], "ORGANISATION_UNIT"),
        Arguments.of("kZeSYCgaHTk", "TEXT_ATTRIBUTE_VALUE", "TEXT"),
        Arguments.of("aIga5mPOFOJ", "TA_MALE", "TEXT with optionSet"),
        Arguments.of("ypGAwVRNtVY", "10", "NUMBER"),
        Arguments.of("x5yfLot5VCM", "2010-10-01", "DATE"));
  }

  @MethodSource()
  @ParameterizedTest(name = "update te with attribute value type {2}")
  public void shouldImportTrackedEntityAttributes(
      String teaId, String attributeValue, String attValueType) {
    JsonObject payload =
        new TrackedEntityDataBuilder()
            .setId(teId)
            .setOrgUnit(Constants.ORG_UNIT_IDS[0])
            .addAttribute(teaId, attributeValue)
            .array();

    trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();

    trackerImportExportActions
        .getTrackedEntity(teId + "?fields=attributes")
        .validate()
        .body("attributes.value", hasItem(attributeValue));
  }

  @Test
  public void shouldImportExportedTrackedEntity() throws Exception {
    // Tracker should allow users to import what they exported from another instance as is
    // Our e2e tests only work with one instance so this simulates the scenario by importing a
    // tracked entity with nested entities, export it, remove any UID from tracker entities and
    // import that again
    JsonObject teJson =
        new FileReaderUtils()
            .read(
                new File(
                    "src/test/resources/tracker/importer/trackedEntities/trackedEntityWithEnrollmentAndEventsNested.json"))
            .get(JsonObject.class);
    String teUID =
        trackerImportExportActions
            .postAndGetJobReport(teJson, new QueryParamsBuilder().add("async=false"))
            .validateSuccessfulImport()
            .extractImportedTrackedEntities()
            .get(0);

    JsonObjectBuilder trackedEntities =
        trackerImportExportActions
            .getTrackedEntities(
                new QueryParamsBuilder().add("fields", "*").add("trackedEntities", teUID))
            .getBodyAsJsonBuilder()
            .deleteByJsonPath("trackedEntities[0].trackedEntity")
            .deleteByJsonPath("trackedEntities[0].enrollments[0].enrollment")
            .deleteByJsonPath("trackedEntities[0].enrollments[0].events[0].event");

    trackerImportExportActions
        .postAndGetJobReport(trackedEntities.build())
        .validateSuccessfulImport()
        .validate()
        .body("stats.created", equalTo(3))
        .rootPath("bundleReport.typeReportMap")
        .body("TRACKED_ENTITY.objectReports", hasSize(1))
        .body("ENROLLMENT.objectReports", hasSize(1))
        .body("EVENT.objectReports", hasSize(1));
  }
}
