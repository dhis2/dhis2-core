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
package org.hisp.dhis.tracker.importer.enrollments;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.helpers.matchers.MatchesJson.matchesJSON;

import com.google.gson.JsonObject;
import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hisp.dhis.Constants;
import org.hisp.dhis.actions.IdGenerator;
import org.hisp.dhis.dto.ApiResponse;
import org.hisp.dhis.dto.TrackerApiResponse;
import org.hisp.dhis.helpers.JsonObjectBuilder;
import org.hisp.dhis.helpers.QueryParamsBuilder;
import org.hisp.dhis.helpers.file.FileReaderUtils;
import org.hisp.dhis.tracker.TrackerNtiApiTest;
import org.hisp.dhis.tracker.importer.databuilder.EnrollmentDataBuilder;
import org.hisp.dhis.tracker.importer.databuilder.EventDataBuilder;
import org.hisp.dhis.tracker.importer.databuilder.TeiDataBuilder;
import org.hisp.dhis.utils.DataGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class EnrollmentsTests extends TrackerNtiApiTest {
  private static final String OU_ID = Constants.ORG_UNIT_IDS[0];

  private String multipleEnrollmentsProgram;

  private String multipleEnrollmentsProgramStage;

  @BeforeAll
  public void beforeAll() {
    multipleEnrollmentsProgram =
        programActions
            .createTrackerProgram(Constants.TRACKED_ENTITY_TYPE, Constants.ORG_UNIT_IDS)
            .extractUid();

    JsonObject object =
        programActions
            .get(multipleEnrollmentsProgram)
            .getBodyAsJsonBuilder()
            .addProperty("onlyEnrollOnce", "false")
            .addProperty("publicAccess", "rwrw----")
            .build();

    programActions.update(multipleEnrollmentsProgram, object).validateStatus(200);

    multipleEnrollmentsProgramStage =
        programActions.createProgramStage(
            multipleEnrollmentsProgram,
            "Enrollment tests program stage " + DataGenerator.randomString());
  }

  @BeforeEach
  public void beforeEach() {
    loginActions.loginAsAdmin();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "src/test/resources/tracker/importer/teis/teiWithEnrollments.json",
        "src/test/resources/tracker/importer/teis/teiAndEnrollment.json"
      })
  public void shouldImportTeisWithEnrollments(String file) {
    TrackerApiResponse response = trackerActions.postAndGetJobReport(new File(file));

    response
        .validateSuccessfulImport()
        .validate()
        .body("status", Matchers.equalTo("OK"))
        .body("stats.created", equalTo(2));

    response.validateTeis().body("stats.created", Matchers.equalTo(1));

    response.validateEnrollments().body("stats.created", Matchers.equalTo(1));

    // assert that the tei was imported
    String teiId = response.extractImportedTeis().get(0);
    String enrollmentId = response.extractImportedEnrollments().get(0);

    trackerActions
        .get("/enrollments/" + enrollmentId)
        .validate()
        .statusCode(200)
        .body("trackedEntity", equalTo(teiId));
  }

  @ParameterizedTest
  @ValueSource(strings = {"true", "false "})
  public void shouldAllowFutureEnrollments(String shouldAddFutureDays) throws Exception {
    // arrange
    JsonObject object =
        programActions
            .get(multipleEnrollmentsProgram)
            .getBodyAsJsonBuilder()
            .addProperty("selectEnrollmentDatesInFuture", shouldAddFutureDays)
            .build();

    programActions.update(multipleEnrollmentsProgram, object).validateStatus(200);
    String teiId = importTei();
    // act

    String enrollmentDate = LocalDate.now().plus(2, ChronoUnit.DAYS).toString();

    JsonObject enrollment =
        new EnrollmentDataBuilder()
            .setTei(teiId)
            .setEnrollmentDate(enrollmentDate)
            .addEvent(
                new EventDataBuilder()
                    .setProgram(multipleEnrollmentsProgram)
                    .setOu(Constants.ORG_UNIT_IDS[0])
                    .setProgramStage(multipleEnrollmentsProgramStage))
            .array(multipleEnrollmentsProgram, Constants.ORG_UNIT_IDS[0]);

    // assert
    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(
            enrollment, new QueryParamsBuilder().add("async", "false"));

    if (Boolean.parseBoolean(shouldAddFutureDays)) {
      response.validateSuccessfulImport();

      return;
    }

    response
        .validateErrorReport()
        .body("errorCode", hasSize(2))
        .body("errorCode", hasItems("E1020", "E5000"))
        .body("message", hasItems(containsString(enrollmentDate)));
  }

  @Test
  public void shouldAddNote() {
    String enrollmentId =
        trackerActions
            .postAndGetJobReport(
                new TeiDataBuilder()
                    .buildWithEnrollment(Constants.ORG_UNIT_IDS[0], Constants.TRACKER_PROGRAM_ID))
            .extractImportedEnrollments()
            .get(0);

    JsonObject payload =
        trackerActions
            .getEnrollment(enrollmentId)
            .getBodyAsJsonBuilder()
            .addOrAppendToArray(
                "notes",
                new JsonObjectBuilder().addProperty("value", DataGenerator.randomString()).build())
            .wrapIntoArray("enrollments");

    trackerActions
        .postAndGetJobReport(payload)
        .validateSuccessfulImport()
        .validate()
        .body("stats.updated", equalTo(1));

    trackerActions
        .getEnrollment(enrollmentId + "?fields=notes")
        .validate()
        .statusCode(200)
        .rootPath("notes")
        .body("note", notNullValue())
        .body("storedAt", notNullValue())
        .body("updatedAt", notNullValue())
        .body("value", notNullValue())
        .body("storedBy", CoreMatchers.everyItem(equalTo(null)))
        .body("createdBy.username", CoreMatchers.everyItem(equalTo("taadmin")));
  }

  @ValueSource(strings = {"true", "false"})
  @ParameterizedTest
  public void shouldOnlyEnrollOnce(String shouldEnrollOnce) throws Exception {
    // arrange
    String program =
        programActions
            .createTrackerProgram(Constants.TRACKED_ENTITY_TYPE, Constants.ORG_UNIT_IDS)
            .extractUid();

    JsonObject object =
        programActions
            .get(program)
            .getBodyAsJsonBuilder()
            .addProperty("onlyEnrollOnce", shouldEnrollOnce)
            .addProperty("publicAccess", "rwrw----")
            .build();

    programActions.update(program, object).validateStatus(200);

    String tei = super.importTei();

    JsonObject enrollment =
        new EnrollmentDataBuilder()
            .setId(new IdGenerator().generateUniqueId())
            .array(program, Constants.ORG_UNIT_IDS[2], tei, "COMPLETED");

    trackerActions.postAndGetJobReport(enrollment).validateSuccessfulImport();

    // act

    TrackerApiResponse response =
        trackerActions.postAndGetJobReport(
            new EnrollmentDataBuilder().array(program, Constants.ORG_UNIT_IDS[2], tei, "ACTIVE"));

    // assert
    if (Boolean.parseBoolean(shouldEnrollOnce)) {
      response.validateErrorReport().body("errorCode", hasItems("E1016"));
      return;
    }

    response.validateSuccessfulImport();
    trackerActions
        .getTrackedEntity(tei + "?fields=enrollments")
        .validate()
        .body("enrollments", hasSize(2));
  }

  @Test
  public void shouldImportEnrollmentToExistingTei() throws Exception {
    String teiId = importTei();

    JsonObject enrollmentPayload =
        new FileReaderUtils()
            .read(new File("src/test/resources/tracker/importer/enrollments/enrollment.json"))
            .replacePropertyValuesWith("trackedEntity", teiId)
            .get(JsonObject.class);

    TrackerApiResponse response = trackerActions.postAndGetJobReport(enrollmentPayload);

    response
        .validateSuccessfulImport()
        .validateEnrollments()
        .body("stats.created", equalTo(1))
        .body("objectReports", notNullValue())
        .body("objectReports.uid", notNullValue());

    String enrollmentId = response.extractImportedEnrollments().get(0);

    ApiResponse enrollmentResponse = trackerActions.get("/enrollments/" + enrollmentId);

    assertThat(
        enrollmentResponse.getBody(),
        matchesJSON(
            enrollmentPayload.get("enrollments").getAsJsonArray().get(0).getAsJsonObject()));
  }

  @Test
  public void shouldReturnErrorWhenUpdatingSoftDeletedEnrollment() throws Exception {
    String teiId = importTei();
    JsonObject enrollments =
        new EnrollmentDataBuilder()
            .setTei(teiId)
            .setOu(OU_ID)
            .setProgram(multipleEnrollmentsProgram)
            .array();

    // Create Enrollment
    TrackerApiResponse response = trackerActions.postAndGetJobReport(enrollments);

    response.validateSuccessfulImport();

    String enrollmentId = response.extractImportedEnrollments().get(0);
    JsonObject enrollmentsToDelete = new EnrollmentDataBuilder().setId(enrollmentId).array();

    // Delete Enrollment
    TrackerApiResponse deleteResponse =
        trackerActions.postAndGetJobReport(
            enrollmentsToDelete, new QueryParamsBuilder().add("importStrategy=DELETE"));

    deleteResponse.validateSuccessfulImport();

    JsonObject enrollmentsToImportAgain =
        new EnrollmentDataBuilder()
            .setId(enrollmentId)
            .setTei(teiId)
            .setOu(OU_ID)
            .setProgram(multipleEnrollmentsProgram)
            .array();

    // Update Enrollment
    TrackerApiResponse responseImportAgain =
        trackerActions.postAndGetJobReport(enrollmentsToImportAgain);

    responseImportAgain.validateErrorReport().body("errorCode", Matchers.hasItem("E1113"));
  }
}
