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
package org.hisp.dhis.tracker.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.TrackerImportStrategy.UPDATE;
import static org.hisp.dhis.tracker.validation.Users.USER_2;
import static org.hisp.dhis.tracker.validation.Users.USER_6;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStageInstanceService;
import org.hisp.dhis.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.trackedentitycomment.TrackedEntityComment;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
import org.hisp.dhis.tracker.report.TrackerTypeReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
class EventImportValidationTest extends TrackerTest {
  @Autowired protected TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private ProgramStageInstanceService programStageServiceInstance;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private UserService _userService;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/tracker_basic_metadata.json");
    injectAdminUser();

    assertNoErrors(
        trackerImportService.importTracker(
            fromJson("tracker/validations/enrollments_te_te-data.json")));
    assertNoErrors(
        trackerImportService.importTracker(
            fromJson("tracker/validations/enrollments_te_enrollments-data.json")));
  }

  @Test
  void testInvalidEnrollmentPreventsValidEventFromBeingCreated() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/invalid_enrollment_with_valid_event.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1070, TrackerErrorCode.E5000);
  }

  @Test
  void failValidationWhenTrackedEntityAttributeHasWrongOptionValue() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events-with_invalid_option_value.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1125);
  }

  @Test
  void successWhenTrackedEntityAttributeHasValidOptionValue() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events-with_valid_option_value.json"));

    assertNoErrors(trackerImportReport);
  }

  @Test
  void testEventValidationOkAll() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events-with-registration.json"));

    assertNoErrors(trackerImportReport);
  }

  @Test
  void testEventValidationOkWithoutAttributeOptionCombo() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events-without-attribute-option-combo.json"));

    assertNoErrors(trackerImportReport);
  }

  @Test
  void testTrackerAndProgramEventUpdateSuccess() throws IOException {
    TrackerImportParams trackerBundleParams =
        fromJson("tracker/validations/program_and_tracker_events.json");
    assertNoErrors(trackerImportService.importTracker(trackerBundleParams));

    trackerBundleParams.setImportStrategy(UPDATE);
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerBundleParams);

    assertNoErrors(trackerImportReport);
  }

  @Test
  void testCantWriteAccessCatCombo() throws IOException {
    TrackerImportParams trackerImportParams =
        fromJson("tracker/validations/events-cat-write-access.json");
    User user = userService.getUser(USER_6);
    trackerImportParams.setUser(user);

    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerImportParams);

    assertHasOnlyErrors(
        trackerImportReport,
        TrackerErrorCode.E1099,
        TrackerErrorCode.E1104,
        TrackerErrorCode.E1096,
        TrackerErrorCode.E1095);
  }

  @Test
  void testNoWriteAccessToOrg() throws IOException {
    TrackerImportParams trackerBundleParams =
        fromJson("tracker/validations/events-with-registration.json");
    User user = userService.getUser(USER_2);
    trackerBundleParams.setUser(user);
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerBundleParams);
    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1000);
  }

  @Test
  void testNonRepeatableProgramStage() throws IOException {
    TrackerImportParams trackerImportParams =
        fromJson("tracker/validations/events_non-repeatable-programstage_part1.json");

    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerImportParams);

    assertNoErrors(trackerImportReport);

    trackerImportParams =
        fromJson("tracker/validations/events_non-repeatable-programstage_part2.json");

    trackerImportReport = trackerImportService.importTracker(trackerImportParams);

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1039);
  }

  @Test
  void shouldSuccessfullyImportRepeatedEventsInEventProgram() throws IOException {
    TrackerImportParams trackerImportParams =
        fromJson("tracker/validations/program_events_non-repeatable-programstage_part1.json");

    TrackerImportReport importReport = trackerImportService.importTracker(trackerImportParams);

    assertNoErrors(importReport);

    trackerImportParams =
        fromJson("tracker/validations/program_events_non-repeatable-programstage_part2.json");

    importReport = trackerImportService.importTracker(trackerImportParams);

    assertNoErrors(importReport);
  }

  @Test
  void testWrongScheduledDateString() {
    assertThrows(
        IOException.class, () -> fromJson("tracker/validations/events_error-no-wrong-date.json"));
  }

  @Test
  void testEventProgramHasNonDefaultCategoryCombo() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events_non-default-combo.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1055);
  }

  @Test
  void testCategoryOptionComboNotFound() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events_cant-find-cat-opt-combo.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1115);
  }

  @Test
  void testCategoryOptionComboNotFoundGivenSubsetOfCategoryOptions() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events_cant-find-aoc-with-subset-of-cos.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1117);
  }

  @Test
  void testCOFoundButAOCNotFound() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events_cant-find-aoc-but-co-exists.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1115);
  }

  @Test
  void testCategoryOptionsNotFound() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events_cant-find-cat-option.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1116);
  }

  @Test
  void testAttributeCategoryOptionNotInProgramCC() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events-aoc-not-in-program-cc.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1054);
  }

  @Test
  void testAttributeCategoryOptionAndCODoNotMatch() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events-aoc-and-co-dont-match.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1117);
  }

  @Test
  void testAttributeCategoryOptionCannotBeFoundForEventProgramCCAndGivenCategoryOption()
      throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson(
                "tracker/validations/events_cant-find-cat-option-combo-for-given-cc-and-co.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1117);
  }

  @Test
  void testWrongDatesInCatCombo() throws IOException {
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(
            fromJson("tracker/validations/events_combo-date-wrong.json"));

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1056, TrackerErrorCode.E1057);
  }

  @Test
  void testValidateAndAddNotesToEvent() throws IOException {
    Date now = new Date();
    // When
    TrackerImportReport trackerImportReport =
        createEvent("tracker/validations/events-with-notes-data.json");
    // Then
    // Fetch the UID of the newly created event
    final ProgramStageInstance programStageInstance = getEventFromReport(trackerImportReport);
    assertThat(programStageInstance.getComments(), hasSize(3));
    // Validate note content
    Stream.of("first note", "second note", "third note")
        .forEach(
            t -> {
              TrackedEntityComment comment = getByComment(programStageInstance.getComments(), t);
              assertTrue(CodeGenerator.isValidUid(comment.getUid()));
              assertTrue(comment.getCreated().getTime() > now.getTime());
              assertTrue(comment.getLastUpdated().getTime() > now.getTime());
              assertNull(comment.getCreator());
              assertEquals(ADMIN_USER_UID, comment.getLastUpdatedBy().getUid());
            });
  }

  @Test
  void testValidateAndAddNotesToUpdatedEvent() throws IOException {
    Date now = new Date();
    // Given -> Creates an event with 3 notes
    createEvent("tracker/validations/events-with-notes-data.json");
    // When -> Update the event and adds 3 more notes
    TrackerImportReport trackerImportReport =
        createEvent("tracker/validations/events-with-notes-update-data.json");
    // Then
    final ProgramStageInstance programStageInstance = getEventFromReport(trackerImportReport);
    assertThat(programStageInstance.getComments(), hasSize(6));
    // validate note content
    Stream.of("first note", "second note", "third note", "4th note", "5th note", "6th note")
        .forEach(
            t -> {
              TrackedEntityComment comment = getByComment(programStageInstance.getComments(), t);
              assertTrue(CodeGenerator.isValidUid(comment.getUid()));
              assertTrue(comment.getCreated().getTime() > now.getTime());
              assertTrue(comment.getLastUpdated().getTime() > now.getTime());
              assertNull(comment.getCreator());
              assertEquals(ADMIN_USER_UID, comment.getLastUpdatedBy().getUid());
            });
  }

  @Test
  void testUpdateDeleteEventFails() {
    testDeletedEventFails(UPDATE);
  }

  @Test
  void testInsertDeleteEventFails() {
    testDeletedEventFails(CREATE_AND_UPDATE);
  }

  @SneakyThrows
  private void testDeletedEventFails(TrackerImportStrategy importStrategy) {
    // Given -> Creates an event
    createEvent("tracker/validations/events-with-notes-data.json");
    ProgramStageInstance event = programStageServiceInstance.getProgramStageInstance("uLxFbxfYDQE");
    assertNotNull(event);
    // When -> Soft-delete the event
    programStageServiceInstance.deleteProgramStageInstance(event);
    TrackerImportParams trackerBundleParams =
        fromJson("tracker/validations/events-with-notes-data.json");
    trackerBundleParams.setImportStrategy(importStrategy);
    // When
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerBundleParams);

    assertHasOnlyErrors(trackerImportReport, TrackerErrorCode.E1082);
  }

  @Test
  void testEventDeleteOk() throws IOException {
    TrackerImportParams trackerBundleParams =
        fromJson("tracker/validations/events-with-registration.json");

    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerBundleParams);

    assertNoErrors(trackerImportReport);

    manager.flush();
    manager.clear();

    TrackerImportParams paramsDelete = fromJson("tracker/validations/event-data-delete.json");
    paramsDelete.setImportStrategy(DELETE);

    TrackerImportReport trackerImportReportDelete =
        trackerImportService.importTracker(paramsDelete);
    assertNoErrors(trackerImportReportDelete);
    assertEquals(1, trackerImportReportDelete.getStats().getDeleted());
  }

  private TrackerImportReport createEvent(String jsonPayload) throws IOException {
    // Given
    TrackerImportParams trackerBundleParams = fromJson(jsonPayload);
    trackerBundleParams.setImportStrategy(CREATE_AND_UPDATE);
    // When
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerBundleParams);
    // Then
    assertNoErrors(trackerImportReport);
    return trackerImportReport;
  }

  private TrackedEntityComment getByComment(
      List<TrackedEntityComment> comments, String commentText) {
    for (TrackedEntityComment comment : comments) {
      if (comment.getCommentText().startsWith(commentText)
          || comment.getCommentText().endsWith(commentText)) {
        return comment;
      }
    }
    fail("Can't find a comment starting or ending with " + commentText);
    return null;
  }

  private ProgramStageInstance getEventFromReport(TrackerImportReport trackerImportReport) {
    final Map<TrackerType, TrackerTypeReport> typeReportMap =
        trackerImportReport.getBundleReport().getTypeReportMap();
    String newEvent = typeReportMap.get(TrackerType.EVENT).getObjectReportMap().get(0).getUid();
    return programStageServiceInstance.getProgramStageInstance(newEvent);
  }
}
