/*
 * Copyright (c) 2004-2022, University of Oslo
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
package org.hisp.dhis.tracker.importer;

import static org.hamcrest.Matchers.*;

import com.google.gson.JsonObject;
import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.MessageConversationsActions;
import org.hisp.dhis.actions.RestApiActions;
import org.hisp.dhis.actions.metadata.MetadataActions;
import org.hisp.dhis.actions.metadata.ProgramStageActions;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.tracker.importer.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.tracker.importer.databuilder.EventDataBuilder;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class RuleEngineTests extends TrackerNtiApiTest {
  private String trackerProgramId = "U5HE4IRrZ7S";

  private String eventProgramId = "uHi4GZJOD3n";

  private MessageConversationsActions messageConversationsActions;

  @BeforeAll
  public void beforeAll() {
    messageConversationsActions = new MessageConversationsActions();

    loginActions.loginAsSuperUser();

    new MetadataActions()
        .importAndValidateMetadata(
            new File("src/test/resources/tracker/programs_with_program_rules.json"));
  }

  @BeforeEach
  public void beforeEach() {
    loginActions.loginAsSuperUser();
  }

  @ParameterizedTest
  @CsvSource({"nH8zfPSUSN1,true", "yKg8CY252Yk,false"})
  public void shouldShowErrorOnEventWhenProgramRuleStageMatches(
      String programStage, boolean shouldReturnError) throws Exception {
    // arrange
    String tei = importTei();

    String enrollment =
        trackerActions
            .postAndGetJobReport(
                new EnrollmentDataBuilder()
                    .setTei(tei)
                    .setEnrollmentDate(Instant.now().plus(1, ChronoUnit.DAYS).toString())
                    .array(trackerProgramId, Constants.ORG_UNIT_IDS[0]))
            .extractImportedEnrollments()
            .get(0);

    JsonObject payload =
        new EventDataBuilder()
            .setEnrollment(enrollment)
            .array(Constants.ORG_UNIT_IDS[0], trackerProgramId, programStage);
    // act
    TrackerApiResponse response = trackerActions.postAndGetJobReport(payload);

    if (!shouldReturnError) {
      response.validateSuccessfulImport();
      return;
    }

    response
        .validateErrorReport()
        .body("trackerType", Matchers.everyItem(equalTo("EVENT")))
        .body("message", Matchers.contains(containsString("TA on stage error")));

    response.validate().body("stats.created", equalTo(0));
  }

  @Test
  public void shouldShowErrorOnCompleteOnEvents() {
    JsonObject payload =
        new EventDataBuilder()
            .setStatus("COMPLETED")
            .array(Constants.ORG_UNIT_IDS[0], eventProgramId, "Mt6Ac5brjoK");

    TrackerApiResponse response = trackerActions.postAndGetJobReport(payload);

    response
        .validateErrorReport()
        .body("trackerType", hasItem("EVENT"))
        .body("message", hasItem(stringContainsInOrder("ERROR ON COMPLETE ")));
  }

  @Test
  public void shouldShowErrorOnCompleteInTrackerEvents() throws Exception {
    String tei = importTei();
    JsonObject enrollment =
        new EnrollmentDataBuilder()
            .setTei(tei)
            .setEnrollmentDate(Instant.now().plus(1, ChronoUnit.DAYS).toString())
            .array(trackerProgramId, Constants.ORG_UNIT_IDS[0]);

    String enrollmentId =
        trackerActions
            .postAndGetJobReport(enrollment)
            .validateSuccessfulImport()
            .extractImportedEnrollments()
            .get(0);

    JsonObject payload =
        new EventDataBuilder()
            .setEnrollment(enrollmentId)
            .setStatus("COMPLETED")
            .array(Constants.ORG_UNIT_IDS[0], trackerProgramId, "nH8zfPSUSN1");

    trackerActions
        .postAndGetJobReport(payload, new QueryParamsBuilder().add("async=false"))
        .validateErrorReport()
        .body("trackerType", hasItem("EVENT"))
        .body("message", hasItem(stringContainsInOrder("ERROR ON COMPLETE ")));
  }

  @Test
  public void shouldSetMandatoryField() {
    JsonObject payload =
        new EventDataBuilder()
            .addDataValue("ILRgzHhzFkg", "true")
            .array(Constants.ORG_UNIT_IDS[0], eventProgramId, "Mt6Ac5brjoK");

    TrackerApiResponse response = trackerActions.postAndGetJobReport(payload);

    response
        .validateErrorReport()
        .body("errorCode", hasItem("E1301"))
        .body("message", hasItem(stringContainsInOrder("Mandatory DataElement", "is not present")));
  }

  @Test
  public void shouldAssignValue() {
    JsonObject payload =
        new EventDataBuilder().array(Constants.ORG_UNIT_IDS[0], eventProgramId, "Mt6Ac5brjoK");

    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(
            payload, new QueryParamsBuilder().addAll("skipSideEffects=true"));

    response
        .validateSuccessfulImport()
        .validateWarningReport()
        .body("warningCode", contains("E1308"));

    String eventId = response.extractImportedEvents().get(0);

    trackerActions
        .get("/events/" + eventId)
        .validate()
        .body("dataValues", hasSize(1))
        .body("dataValues.value", contains("AUTO_ASSIGNED_COMMENT"));
  }

  @Test
  @Disabled("TODO: fix this test 12098")
  public void shouldSendProgramRuleNotification() {
    JsonObject payload =
        new EventDataBuilder()
            .addDataValue("ILRgzHhzFkg", "true")
            .addDataValue("z3Z4TD3oBCP", "true")
            .addDataValue("BuZ5LGNfGEU", "40")
            .array(Constants.ORG_UNIT_IDS[0], eventProgramId, "Mt6Ac5brjoK");

    loginActions.loginAsAdmin();
    ApiResponse response =
        new RestApiActions("/messageConversations")
            .get("", new QueryParamsBuilder().add("fields=*"));

    int size = response.getBody().getAsJsonArray("messageConversations").size();

    loginActions.loginAsSuperUser();

    trackerActions.postAndGetJobReport(payload).validateSuccessfulImport();

    loginActions.loginAsAdmin();
    messageConversationsActions.waitForNotification(size + 1);
    messageConversationsActions
        .get("", new QueryParamsBuilder().add("fields=*"))
        .validate()
        .statusCode(200)
        .body("messageConversations", hasSize(size + 1))
        .body("messageConversations.subject", hasItem("Program rule triggered"));
  }

  @ParameterizedTest
  @CsvSource({
    "ON_COMPLETE,COMPLETED,true",
    "ON_COMPLETE,ACTIVE,false",
    "ON_UPDATE_AND_INSERT,ACTIVE,true"
  })
  public void shouldShowErrorsBasedOnValidationStrategy(
      String validationStrategy, String eventStatus, boolean shouldFail) {
    String programStage =
        new ProgramStageActions()
            .get(
                "",
                new QueryParamsBuilder()
                    .addAll(
                        "filter=program.id:eq:" + trackerProgramId,
                        "filter=validationStrategy:eq:" + validationStrategy))
            .extractString("programStages.id[0]");

    JsonObject payload =
        new TeiDataBuilder()
            .buildWithEnrollmentAndEvent(
                Constants.TRACKED_ENTITY_TYPE,
                Constants.ORG_UNIT_IDS[0],
                trackerProgramId,
                programStage);

    // program rule is triggered for events with date earlier than today
    new JsonObjectBuilder(payload)
        .addPropertyByJsonPath(
            "trackedEntities[0].enrollments[0].events[0].occurredAt",
            Instant.now().minus(1, ChronoUnit.DAYS).toString())
        .addPropertyByJsonPath("trackedEntities[0].enrollments[0].events[0].status", eventStatus);

    TrackerApiResponse response = trackerActions.postAndGetJobReport(payload);

    if (shouldFail) {
      response.validateErrorReport();

      return;
    }

    response.validateSuccessfulImport();
  }

  @Test
  public void shouldAddErrorForEnrollmentsAndEventsWhenRuleHasNoStage() {
    JsonObject object =
        new TeiDataBuilder()
            .buildWithEnrollmentAndEvent(
                Constants.TRACKED_ENTITY_TYPE,
                Constants.ORG_UNIT_IDS[0],
                trackerProgramId,
                "yKg8CY252Yk");

    JsonObjectBuilder.jsonObject(object)
        .addPropertyByJsonPath(
            "trackedEntities[0].enrollments[0].enrolledAt",
            Instant.now().minus(1, ChronoUnit.DAYS).toString());

    // act

    TrackerApiResponse response = trackerActions.postAndGetJobReport(object);

    response
        .validateWarningReport()
        .body("", hasSize(greaterThanOrEqualTo(1)))
        .body("trackerType", hasItems("ENROLLMENT"))
        .body("warningCode", everyItem(equalTo("E1300")));
  }

  @Test
  public void shouldBeSkippedWhenSkipRuleEngineFlag() {
    JsonObject payload =
        new EventDataBuilder()
            .setStatus("COMPLETED")
            .array(Constants.ORG_UNIT_IDS[0], eventProgramId, "Mt6Ac5brjoK");

    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(
            payload, new QueryParamsBuilder().add("skipRuleEngine=true"));

    response.validateSuccessfulImport();
  }

  @Test
  public void shouldImportWhenWarnings() {
    // arrange

    JsonObject object =
        new TeiDataBuilder()
            .buildWithEnrollmentAndEvent(
                Constants.TRACKED_ENTITY_TYPE,
                Constants.ORG_UNIT_IDS[0],
                trackerProgramId,
                "yKg8CY252Yk");

    JsonObjectBuilder.jsonObject(object)
        .addPropertyByJsonPath(
            "trackedEntities[0].enrollments[0].enrolledAt",
            Instant.now().minus(1, ChronoUnit.DAYS).toString());

    // act

    TrackerApiResponse response = trackerActions.postAndGetJobReport(object);

    response
        .validateWarningReport()
        .body("message[0]", containsString("TA warning"))
        .body("warningCode", everyItem(equalTo("E1300")));

    response.validateSuccessfulImport();
  }
}
