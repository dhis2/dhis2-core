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
package org.hisp.dhis.tracker.deduplication.merge;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.hisp.dhis.helpers.file.JsonFileReader;
import org.hisp.dhis.test.e2e.Constants;
import org.hisp.dhis.test.e2e.dto.TrackerApiResponse;
import org.hisp.dhis.test.e2e.helpers.JsonObjectBuilder;
import org.hisp.dhis.tracker.deduplication.PotentialDuplicatesApiTest;
import org.hisp.dhis.tracker.imports.databuilder.TrackedEntityDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Gintare Vilkelyte <vilkelyte.gintare@gmail.com>
 */
public class PotentialDuplicatesEnrollmentsTests extends PotentialDuplicatesApiTest {
  @BeforeEach
  public void beforeEach() {
    loginActions.loginAsAdmin();
  }

  @Test
  public void shouldNotBeMergedWhenBothTrackedEntitiesEnrolledInSameProgram() {
    String teA =
        createTrackedEntityWithEnrollmentsAndEvents(Constants.TRACKER_PROGRAM_ID, "nlXNK4b7LVr")
            .extractImportedTrackedEntities()
            .get(0);
    String teB =
        createTrackedEntityWithEnrollmentsAndEvents(Constants.TRACKER_PROGRAM_ID, "nlXNK4b7LVr")
            .extractImportedTrackedEntities()
            .get(0);

    String potentialDuplicate =
        potentialDuplicatesActions
            .postPotentialDuplicate(teA, teB, "OPEN")
            .validateStatus(200)
            .extractString("id");

    potentialDuplicatesActions
        .autoMergePotentialDuplicate(potentialDuplicate)
        .validate()
        .statusCode(409)
        .body("message", containsString("Both entities enrolled in the same program"));
  }

  @Test
  public void shouldBeManuallyMerged() {
    String teA =
        createTrackedEntityWithEnrollmentsAndEvents(TRACKER_PROGRAM_ID, TRACKER_PROGRAM_STAGE_ID)
            .extractImportedTrackedEntities()
            .get(0);

    TrackerApiResponse teBResponse =
        createTrackedEntityWithEnrollmentsAndEvents(
            Constants.ANOTHER_TRACKER_PROGRAM_ID, "PaOOjwLVW2X");
    String teB = teBResponse.extractImportedTrackedEntities().get(0);
    String enrollmentToMerge = teBResponse.extractImportedEnrollments().get(0);

    String potentialDuplicate =
        potentialDuplicatesActions
            .postPotentialDuplicate(teA, teB, "OPEN")
            .validateStatus(200)
            .extractString("id");

    potentialDuplicatesActions
        .manualMergePotentialDuplicate(
            potentialDuplicate,
            new JsonObjectBuilder()
                .addArray("enrollments", Arrays.asList(enrollmentToMerge))
                .build())
        .validate()
        .statusCode(200);

    trackerImportExportActions
        .getTrackedEntity(teA + "?fields=enrollments")
        .validate()
        .statusCode(200)
        .body("enrollments", hasSize(2));
  }

  @Test
  public void shouldAutoMergeWithEnrollmentsAndEvents() throws IOException {
    // arrange
    TrackerApiResponse originalTrackedEntityResponse =
        createTrackedEntityWithEnrollmentsAndEvents(
            Constants.ANOTHER_TRACKER_PROGRAM_ID, "PaOOjwLVW2X");
    String teA = originalTrackedEntityResponse.extractImportedTrackedEntities().get(0);
    String enrollmentA = originalTrackedEntityResponse.extractImportedEnrollments().get(0);

    TrackerApiResponse duplicateTrackedEntityResponse =
        trackerImportExportActions.postAndGetJobReport(
            new JsonFileReader(
                    new File(
                        "src/test/resources/tracker/importer/trackedEntities/trackedEntityWithEnrollmentAndEventsNested.json"))
                .get());

    String teB = duplicateTrackedEntityResponse.extractImportedTrackedEntities().get(0);
    String enrollmentB = duplicateTrackedEntityResponse.extractImportedEnrollments().get(0);

    String potentialDuplicate =
        potentialDuplicatesActions.createAndValidatePotentialDuplicate(teA, teB, "OPEN");

    // act
    potentialDuplicatesActions
        .autoMergePotentialDuplicate(potentialDuplicate)
        .validate()
        .statusCode(200);

    // assert
    potentialDuplicatesActions
        .get(potentialDuplicate)
        .validate()
        .statusCode(200)
        .body("status", equalTo("MERGED"));

    trackerImportExportActions
        .getTrackedEntity(teA + "?fields=*")
        .validate()
        .statusCode(200)
        .body("enrollments", hasSize(2))
        .body("enrollments.enrollment", hasItems(enrollmentA, enrollmentB))
        .rootPath(String.format("enrollments.find{it.enrollment=='%s'}", enrollmentA))
        .body("events", hasSize(greaterThanOrEqualTo(1)))
        .body("events.dataValues", hasSize(greaterThanOrEqualTo(1)))
        .noRootPath()
        .rootPath(String.format("enrollments.find{it.enrollment=='%s'}", enrollmentB))
        .body("events", hasSize(greaterThanOrEqualTo(1)))
        .body("events.dataValues", hasSize(greaterThanOrEqualTo(1)));

    trackerImportExportActions.getTrackedEntity(teB).validate().statusCode(404);
  }

  @Test
  public void shouldMergeWithNonSuperUser() {
    // arrange
    String teB = createTrackedEntityWithoutEnrollment(Constants.ORG_UNIT_IDS[0]);

    TrackerApiResponse imported =
        trackerImportExportActions
            .postAndGetJobReport(
                new TrackedEntityDataBuilder()
                    .buildWithEnrollmentAndEvent(
                        Constants.TRACKED_ENTITY_TYPE,
                        Constants.ORG_UNIT_IDS[0],
                        TRACKER_PROGRAM_ID,
                        TRACKER_PROGRAM_STAGE_ID))
            .validateSuccessfulImport();

    String teA = imported.extractImportedTrackedEntities().get(0);
    String enrollment = imported.extractImportedEnrollments().get(0);
    assertThat(enrollment, notNullValue());

    String potentialDuplicate =
        potentialDuplicatesActions.createAndValidatePotentialDuplicate(teA, teB, "OPEN");

    String username = createUserWithAccessToMerge();
    loginActions.loginAsUser(username, USER_PASSWORD);

    // act
    potentialDuplicatesActions
        .autoMergePotentialDuplicate(potentialDuplicate)
        .validate()
        .statusCode(200);

    // assert
    potentialDuplicatesActions
        .get(potentialDuplicate)
        .validate()
        .statusCode(200)
        .body("status", equalTo("MERGED"));

    trackerImportExportActions
        .getTrackedEntity(teA + "?fields=*")
        .validate()
        .statusCode(200)
        .body("enrollments", hasSize(1))
        .body("enrollments.enrollment", hasItems(enrollment))
        .body(
            String.format("enrollments.find{it.enrollment=='%s'}.events", enrollment), hasSize(1));

    trackerImportExportActions.getTrackedEntity(teB).validate().statusCode(404);
  }

  private String createTrackedEntityWithoutEnrollment(String ouId) {
    JsonObject object = new TrackedEntityDataBuilder().array(Constants.TRACKED_ENTITY_TYPE, ouId);

    return trackerImportExportActions
        .postAndGetJobReport(object)
        .extractImportedTrackedEntities()
        .get(0);
  }
}
