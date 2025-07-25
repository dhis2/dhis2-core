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
package org.hisp.dhis.tracker.imports.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE_AND_UPDATE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.DELETE;
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.UPDATE;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_2;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_6;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.note.Note;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.TrackerTypeReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EventImportValidationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerImportService trackerImportService;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata("tracker/tracker_basic_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerImportParams params = TrackerImportParams.builder().build();

    assertNoErrors(
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_te-data.json")));
    assertNoErrors(
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data.json")));
  }

  @BeforeEach
  void setUpUser() {
    injectSecurityContextUser(importUser);
  }

  @Test
  void testInvalidEnrollmentPreventsValidEventFromBeingCreated() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/invalid_enrollment_with_valid_event.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1070, ValidationCode.E5000);
  }

  @Test
  void failValidationWhenTrackedEntityAttributeHasWrongOptionValue() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/events-with_invalid_option_value.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1125);
  }

  @Test
  void successWhenTrackedEntityAttributeHasValidOptionValue() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events-with_valid_option_value.json"));

    assertNoErrors(importReport);
  }

  @Test
  void testEventValidationOkAll() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events-with-registration.json"));

    assertNoErrors(importReport);
  }

  @Test
  void testEventValidationOkWithoutAttributeOptionCombo() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/events-without-attribute-option-combo.json"));

    assertNoErrors(importReport);
  }

  @Test
  void testTrackerAndSingleEventUpdateSuccess() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/program_and_tracker_events.json");

    assertNoErrors(trackerImportService.importTracker(params, trackerObjects));

    params.setImportStrategy(UPDATE);
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void testCantWriteAccessCatCombo() throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments-cat-write-access.json");
    TrackerImportParams params = new TrackerImportParams();

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    trackerObjects = testSetup.fromJson("tracker/validations/events-cat-write-access.json");
    params = new TrackerImportParams();
    injectSecurityContextUser(userService.getUser(USER_6));

    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(
        importReport,
        ValidationCode.E1096,
        ValidationCode.E1099,
        ValidationCode.E1104,
        ValidationCode.E1095);
  }

  @Test
  void testNoWriteAccessToOrg() throws IOException {
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/events-with-registration.json");
    TrackerImportParams params = new TrackerImportParams();
    injectSecurityContextUser(userService.getUser(USER_2));
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertHasOnlyErrors(importReport, ValidationCode.E1000);
  }

  @Test
  void testNonRepeatableProgramStage() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/events_non-repeatable-programstage_part1.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    trackerObjects =
        testSetup.fromJson("tracker/validations/events_non-repeatable-programstage_part2.json");

    importReport = trackerImportService.importTracker(new TrackerImportParams(), trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1039);
  }

  @Test
  void shouldSuccessfullyImportRepeatedEventsInEventProgram() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson(
            "tracker/validations/program_events_non-repeatable-programstage_part1.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    trackerObjects =
        testSetup.fromJson(
            "tracker/validations/program_events_non-repeatable-programstage_part2.json");

    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void testEventProgramHasNonDefaultCategoryCombo() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events_non-default-combo.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1055);
  }

  @Test
  void testCategoryOptionComboNotFound() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events_cant-find-cat-opt-combo.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1115);
  }

  @Test
  void testCategoryOptionComboNotFoundGivenSubsetOfCategoryOptions() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/events_cant-find-aoc-with-subset-of-cos.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1117);
  }

  @Test
  void testCOFoundButAOCNotFound() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/events_cant-find-aoc-but-co-exists.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1115);
  }

  @Test
  void testCategoryOptionsNotFound() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events_cant-find-cat-option.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1116);
  }

  @Test
  void testAttributeCategoryOptionNotInProgramCC() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events-aoc-not-in-program-cc.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1054);
  }

  @Test
  void testAttributeCategoryOptionAndCODoNotMatch() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events-aoc-and-co-dont-match.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1117);
  }

  @Test
  void testAttributeCategoryOptionCannotBeFoundForEventProgramCCAndGivenCategoryOption()
      throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson(
                "tracker/validations/events_cant-find-cat-option-combo-for-given-cc-and-co.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1117);
  }

  @Test
  void testWrongDatesInCatCombo() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events_combo-date-wrong.json"));

    assertHasOnlyErrors(importReport, ValidationCode.E1056, ValidationCode.E1057);
  }

  @Test
  void testValidateAndAddNotesToEvent() throws IOException {
    Date now = new Date();
    // When
    ImportReport importReport = createEvent("tracker/validations/events-with-notes-data.json");
    // Then
    // Fetch the UID of the newly created event
    final TrackerEvent event = getEventFromReport(importReport);
    assertThat(event.getNotes(), hasSize(3));
    // Validate note content
    Stream.of("first note", "second note", "third note")
        .forEach(
            t -> {
              Note note = getByNote(event.getNotes(), t);
              assertTrue(CodeGenerator.isValidUid(note.getUid()));
              assertTrue(note.getCreated().getTime() > now.getTime());
              assertNull(note.getCreator());
              assertEquals(importUser.getUid(), note.getLastUpdatedBy().getUid());
            });
  }

  @Test
  void testValidateAndAddNotesToUpdatedEvent() throws IOException {
    Date now = new Date();
    // Given -> Creates an event with 3 notes
    createEvent("tracker/validations/events-with-notes-data.json");
    // When -> Update the event and adds 3 more notes
    ImportReport importReport =
        createEvent("tracker/validations/events-with-notes-update-data.json");
    // Then
    final TrackerEvent event = getEventFromReport(importReport);
    assertThat(event.getNotes(), hasSize(6));
    // validate note content
    Stream.of("first note", "second note", "third note", "4th note", "5th note", "6th note")
        .forEach(
            t -> {
              Note note = getByNote(event.getNotes(), t);
              assertTrue(CodeGenerator.isValidUid(note.getUid()));
              assertTrue(note.getCreated().getTime() > now.getTime());
              assertNull(note.getCreator());
              assertEquals(importUser.getUid(), note.getLastUpdatedBy().getUid());
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
    TrackerEvent event = manager.get(TrackerEvent.class, "uLxFbxfYDQE");
    assertNotNull(event);
    // When -> Soft-delete the event
    manager.delete(event);
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/events-with-notes-data.json");
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(importStrategy);
    // When
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1082);
  }

  @Test
  void testEventDeleteOk() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/events-with-registration.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    manager.flush();
    manager.clear();

    TrackerObjects deleteTrackerObjects =
        testSetup.fromJson("tracker/validations/event-data-delete.json");
    params.setImportStrategy(DELETE);

    ImportReport importReportDelete =
        trackerImportService.importTracker(params, deleteTrackerObjects);
    assertNoErrors(importReportDelete);
    assertEquals(1, importReportDelete.getStats().getDeleted());
  }

  private ImportReport createEvent(String jsonPayload) throws IOException {
    // Given
    TrackerObjects trackerObjects = testSetup.fromJson(jsonPayload);
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(CREATE_AND_UPDATE);
    // When
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    // Then
    assertNoErrors(importReport);
    return importReport;
  }

  private Note getByNote(List<Note> notes, String noteText) {
    for (Note note : notes) {
      if (note.getNoteText().startsWith(noteText) || note.getNoteText().endsWith(noteText)) {
        return note;
      }
    }
    fail("Can't find a note starting or ending with " + noteText);
    return null;
  }

  private TrackerEvent getEventFromReport(ImportReport importReport) {
    final Map<TrackerType, TrackerTypeReport> typeReportMap =
        importReport.getPersistenceReport().getTypeReportMap();
    UID newEvent = typeReportMap.get(TrackerType.EVENT).getEntityReport().get(0).getUid();
    return manager.get(TrackerEvent.class, newEvent);
  }
}
