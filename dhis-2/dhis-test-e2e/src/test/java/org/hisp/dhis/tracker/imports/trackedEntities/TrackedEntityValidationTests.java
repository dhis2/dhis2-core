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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonObject;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.actions.metadata.ProgramActions;
import org.hisp.dhis.test.e2e.actions.metadata.TrackedEntityAttributeActions;
import org.hisp.dhis.test.e2e.actions.metadata.TrackedEntityTypeActions;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.test.e2e.helpers.QueryParamsBuilder;
import org.hisp.dhis.test.e2e.utils.DataGenerator;
import org.hisp.dhis.tracker.TrackerApiTest;
import org.hisp.dhis.tracker.imports.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.tracker.imports.databuilder.TrackedEntityDataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class TrackedEntityValidationTests extends TrackerApiTest {
  private String trackedEntityType;

  private String program;

  private String mandatoryTetAttribute;

  private String uniqueTetAttribute;

  private String mandatoryProgramAttribute;

  private String attributeWithOptionSet;

  private String attributeWithMultiText;

  @BeforeAll
  public void beforeAll() {
    loginActions.loginAsSuperUser();

    setupData();
  }

  @Test
  public void shouldValidateUniqueness() {
    String value = DataGenerator.randomString();

    JsonObject payload =
        new TrackedEntityDataBuilder()
            .addAttribute(uniqueTetAttribute, value)
            .addAttribute(mandatoryTetAttribute, value)
            .array(trackedEntityType, Constants.ORG_UNIT_IDS[0]);

    trackerImportExportActions.postAndGetJobReport(payload).validateSuccessfulImport();

    trackerImportExportActions
        .postAndGetJobReport(payload)
        .validateErrorReport()
        .body("", hasSize(greaterThanOrEqualTo(1)))
        .body("errorCode", hasItem("E1064"))
        .body("message", hasItem(containsStringIgnoringCase("non-unique")));
  }

  @Test
  public void shouldReturnErrorReportsWhenTetIncorrect() {
    // arrange
    JsonObject trackedEntities =
        new TrackedEntityDataBuilder().array("", Constants.ORG_UNIT_IDS[0]);

    // act
    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(trackedEntities);

    // assert
    response.validateErrorReport().body("errorCode", hasItem("E1121"));
  }

  @Test
  public void shouldNotReturnErrorWhenMandatoryTetAttributeIsPresent() {
    JsonObject trackedEntities = buildTrackedEntityWithMandatoryAttribute().array();

    // assert
    trackerImportExportActions.postAndGetJobReport(trackedEntities).validateSuccessfulImport();
  }

  @Test
  public void shouldReturnErrorWhenMandatoryAttributesMissing() {
    // arrange
    JsonObject trackedEntities =
        new TrackedEntityDataBuilder().array(trackedEntityType, Constants.ORG_UNIT_IDS[0]);

    // assert
    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(trackedEntities);

    response.validateErrorReport().body("errorCode", hasItem("E1090"));
  }

  @Test
  public void shouldReturnErrorWhenRemovingMandatoryAttributes() {
    JsonObject object = buildTrackedEntityWithEnrollmentAndMandatoryAttributes().array();

    TrackerApiResponse response =
        trackerImportExportActions.postAndGetJobReport(
            object, new QueryParamsBuilder().add("async=false"));

    String trackedEntityId =
        response.validateSuccessfulImport().extractImportedTrackedEntities().get(0);

    String enrollmentId = response.extractImportedEnrollments().get(0);

    JsonObjectBuilder.jsonObject(object)
        .addPropertyByJsonPath("trackedEntities[0].trackedEntity", trackedEntityId)
        .addPropertyByJsonPath("trackedEntities[0].attributes[0].value", null)
        .addPropertyByJsonPath("trackedEntities[0].enrollments[0].enrollment", enrollmentId)
        .addPropertyByJsonPath("trackedEntities[0].enrollments[0].attributes[0].value", null);

    trackerImportExportActions
        .postAndGetJobReport(object, new QueryParamsBuilder().add("async=false"))
        .validateErrorReport()
        .body("", hasSize(2))
        .body("trackerType", hasItems("TRACKED_ENTITY", "ENROLLMENT"))
        .body("errorCode", hasItems("E1076", "E1076"))
        .body(
            "message",
            hasItem(
                allOf(
                    containsStringIgnoringCase("TrackedEntityAttribute"),
                    containsStringIgnoringCase(mandatoryTetAttribute))))
        .body(
            "message",
            hasItem(
                allOf(
                    containsStringIgnoringCase("TrackedEntityAttribute"),
                    containsStringIgnoringCase(mandatoryProgramAttribute))));
  }

  @Test
  public void shouldNotReturnErrorWhenRemovingNotMandatoryAttributes() {
    JsonObject payload = buildTrackedEntityWithMandatoryAndOptionSetAttribute().array();

    String trackedEntityId =
        trackerImportExportActions
            .postAndGetJobReport(payload, new QueryParamsBuilder().add("async=false"))
            .validateSuccessfulImport()
            .extractImportedTrackedEntities()
            .get(0);

    JsonObjectBuilder.jsonObject(payload)
        .addPropertyByJsonPath("trackedEntities[0]", "trackedEntity", trackedEntityId)
        .addPropertyByJsonPath("trackedEntities[0].attributes[1]", "value", null);

    trackerImportExportActions
        .postAndGetJobReport(payload, new QueryParamsBuilder().add("async=false"))
        .validateSuccessfulImport();

    trackerImportExportActions
        .getTrackedEntity(trackedEntityId)
        .validate()
        .body("attributes", hasSize(1));
  }

  @Test
  public void shouldReturnErrorWhenMandatoryProgramAttributeMissing() {
    // arrange
    JsonObject trackedEntities =
        new TrackedEntityDataBuilder()
            .buildWithEnrollment(trackedEntityType, Constants.ORG_UNIT_IDS[0], program);

    // assert
    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(trackedEntities);

    response
        .validateErrorReport()
        .body("trackerType", hasItem("ENROLLMENT"))
        .body("errorCode", hasItem("E1018"));
  }

  @Test
  public void shouldReturnErrorWhenAttributeWithOptionSetInvalid() {
    JsonObject trackedEntities =
        buildTrackedEntityWithMandatoryAttribute()
            .addAttribute(attributeWithOptionSet, DataGenerator.randomString())
            .array();

    trackerImportExportActions
        .postAndGetJobReport(trackedEntities, new QueryParamsBuilder().add("async=false"))
        .validateErrorReport()
        .body("errorCode", hasItem("E1125"))
        .body("trackerType", hasItem("TRACKED_ENTITY"));
  }

  @Test
  public void shouldReturnSuccessWhenAttributeMultiTextIsValid() {
    JsonObject trackedEntities =
        buildTrackedEntityWithMandatoryAttribute()
            .addAttribute(attributeWithMultiText, "TA_NO,TA_YES")
            .array();

    trackerImportExportActions
        .postAndGetJobReport(trackedEntities, new QueryParamsBuilder().add("async=false"))
        .validate()
        .statusCode(200)
        .body("status", equalTo("OK"));
  }

  @Test
  public void shouldReturnErrorWhenUpdatingSoftDeletedTE() {
    JsonObject trackedEntities =
        new TrackedEntityDataBuilder()
            .setTrackedEntityType(Constants.TRACKED_ENTITY_TYPE)
            .setOrgUnit(Constants.ORG_UNIT_IDS[0])
            .array();

    // create TE
    TrackerApiResponse response = trackerImportExportActions.postAndGetJobReport(trackedEntities);

    response.validateSuccessfulImport();

    String trackedEntityId = response.extractImportedTrackedEntities().get(0);
    JsonObject trackedEntitiesToDelete =
        new TrackedEntityDataBuilder().setId(trackedEntityId).array();

    // delete TE
    TrackerApiResponse deleteResponse =
        trackerImportExportActions.postAndGetJobReport(
            trackedEntitiesToDelete, new QueryParamsBuilder().add("importStrategy=DELETE"));

    deleteResponse.validateSuccessfulImport();

    JsonObject trackedEntitiesToImportAgain =
        new TrackedEntityDataBuilder()
            .setId(trackedEntityId)
            .setTrackedEntityType(Constants.TRACKED_ENTITY_TYPE)
            .setOrgUnit(Constants.ORG_UNIT_IDS[0])
            .array();

    // Update TE
    TrackerApiResponse responseImportAgain =
        trackerImportExportActions.postAndGetJobReport(trackedEntitiesToImportAgain);

    responseImportAgain.validateErrorReport().body("errorCode", hasItem("E1114"));
  }

  @Test
  public void shouldNotReturnErrorWhenAttributeWithOptionSetIsPresent() {
    JsonObject trackedEntities = buildTrackedEntityWithMandatoryAndOptionSetAttribute().array();

    trackerImportExportActions.postAndGetJobReport(trackedEntities).validateSuccessfulImport();
  }

  private TrackedEntityDataBuilder buildTrackedEntityWithMandatoryAndOptionSetAttribute() {
    return buildTrackedEntityWithMandatoryAttribute()
        .addAttribute(attributeWithOptionSet, "TA_YES");
  }

  private TrackedEntityDataBuilder buildTrackedEntityWithMandatoryAttribute() {
    return new TrackedEntityDataBuilder()
        .setTrackedEntityType(trackedEntityType)
        .setOrgUnit(Constants.ORG_UNIT_IDS[0])
        .addAttribute(mandatoryTetAttribute, DataGenerator.randomString());
  }

  private TrackedEntityDataBuilder buildTrackedEntityWithEnrollmentAndMandatoryAttributes() {
    return buildTrackedEntityWithMandatoryAttribute()
        .addEnrollment(
            new EnrollmentDataBuilder()
                .addAttribute(mandatoryProgramAttribute, DataGenerator.randomString())
                .setProgram(program)
                .setOrgUnit(Constants.ORG_UNIT_IDS[0]));
  }

  private void setupData() {
    TrackedEntityAttributeActions trackedEntityAttributeActions =
        new TrackedEntityAttributeActions();
    ProgramActions programActions = new ProgramActions();
    TrackedEntityTypeActions trackedEntityTypeActions = new TrackedEntityTypeActions();

    trackedEntityType = trackedEntityTypeActions.create();

    // create attributes
    uniqueTetAttribute = trackedEntityAttributeActions.create("TEXT", true);
    mandatoryTetAttribute = trackedEntityAttributeActions.create("TEXT");
    mandatoryProgramAttribute = trackedEntityAttributeActions.create("TEXT");
    attributeWithOptionSet = trackedEntityAttributeActions.createOptionSetAttribute("ZGkmoWb77MW");
    attributeWithMultiText = trackedEntityAttributeActions.createMultiTextAttribute("ZGkmoWb77MW");

    trackedEntityTypeActions.addAttribute(trackedEntityType, mandatoryTetAttribute, true);
    trackedEntityTypeActions.addAttribute(trackedEntityType, attributeWithOptionSet, false);
    trackedEntityTypeActions.addAttribute(trackedEntityType, uniqueTetAttribute, false);
    trackedEntityTypeActions.addAttribute(trackedEntityType, attributeWithMultiText, false);

    // create a program
    program =
        programActions.createTrackerProgram(trackedEntityType, Constants.ORG_UNIT_IDS).extractUid();
    programActions.addAttribute(program, mandatoryProgramAttribute, true);
  }
}
