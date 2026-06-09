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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.program.EnrollmentStatus.CANCELLED;
import static org.hisp.dhis.program.EnrollmentStatus.COMPLETED;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.model.Enrollment;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentImportTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private NamedParameterJdbcTemplate jdbcTemplate;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @ParameterizedTest
  @MethodSource("statuses")
  void shouldCorrectlyPopulateCompletedDataWhenCreatingAnEnrollment(EnrollmentStatus status)
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEnrollments().get(0).setStatus(status);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    Enrollment enrollment =
        enrollmentService.getEnrollment(trackerObjects.getEnrollments().get(0).getUID());

    assertEnrollmentCompletedData(enrollment);
  }

  @ParameterizedTest
  @MethodSource("transitionStatuses")
  void shouldCorrectlyPopulateCompletedDataWhenUpdatingAnEnrollment(
      EnrollmentStatus savedStatus, EnrollmentStatus updatedStatus)
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEnrollments().get(0).setStatus(savedStatus);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    trackerObjects.getEnrollments().get(0).setStatus(updatedStatus);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    Enrollment enrollment =
        enrollmentService.getEnrollment(trackerObjects.getEnrollments().get(0).getUID());

    assertEnrollmentCompletedData(enrollment);
  }

  public Stream<Arguments> statuses() {
    return Stream.of(Arguments.of(ACTIVE), Arguments.of(CANCELLED), Arguments.of(COMPLETED));
  }

  public Stream<Arguments> transitionStatuses() {
    return Stream.of(
        Arguments.of(ACTIVE, COMPLETED),
        Arguments.of(ACTIVE, CANCELLED),
        Arguments.of(ACTIVE, COMPLETED),
        Arguments.of(CANCELLED, COMPLETED),
        Arguments.of(CANCELLED, CANCELLED),
        Arguments.of(CANCELLED, COMPLETED),
        Arguments.of(COMPLETED, COMPLETED),
        Arguments.of(COMPLETED, CANCELLED),
        Arguments.of(COMPLETED, COMPLETED));
  }

  @Test
  void shouldInsertNoteRowsAndJoinTableEntriesWhenEnrollmentIsCreatedWithNotes()
      throws IOException {
    testSetup.importTrackerData("tracker/one_te.json");
    ImportReport report =
        trackerImportService.importTracker(
            TrackerImportParams.builder().build(),
            testSetup.fromJson("tracker/one_enrollment_with_notes.json"));
    assertNoErrors(report);

    List<Map<String, Object>> joinRows =
        jdbcTemplate.queryForList(
            "select n.uid as noteuid, n.notetext, en.sort_order"
                + " from enrollment_notes en"
                + " join enrollment e on e.enrollmentid = en.enrollmentid"
                + " join note n on n.noteid = en.noteid"
                + " where e.uid = :uid"
                + " order by en.sort_order",
            new MapSqlParameterSource("uid", "TvctPPhpD8u"));

    assertEquals(2, joinRows.size(), "expected two notes linked to the enrollment");
    assertAll(
        () -> assertEquals("NoteAlpha01", joinRows.get(0).get("noteuid")),
        () -> assertEquals("First enrollment note", joinRows.get(0).get("notetext")),
        () -> assertEquals(1, ((Number) joinRows.get(0).get("sort_order")).intValue()),
        () -> assertEquals("NoteBeta002", joinRows.get(1).get("noteuid")),
        () -> assertEquals("Second enrollment note", joinRows.get(1).get("notetext")),
        () -> assertEquals(2, ((Number) joinRows.get(1).get("sort_order")).intValue()));
  }

  @Test
  void shouldAppendNewNoteAndKeepExistingOnesWhenEnrollmentIsUpdatedWithExtraNote()
      throws IOException {
    testSetup.importTrackerData("tracker/one_te.json");
    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().build(),
            testSetup.fromJson("tracker/one_enrollment_with_notes.json")));

    assertNoErrors(
        trackerImportService.importTracker(
            TrackerImportParams.builder().build(),
            testSetup.fromJson("tracker/one_enrollment_with_extra_note.json")));

    List<Map<String, Object>> joinRows =
        jdbcTemplate.queryForList(
            "select n.uid as noteuid, n.notetext, en.sort_order"
                + " from enrollment_notes en"
                + " join enrollment e on e.enrollmentid = en.enrollmentid"
                + " join note n on n.noteid = en.noteid"
                + " where e.uid = :uid"
                + " order by en.sort_order",
            new MapSqlParameterSource("uid", "TvctPPhpD8u"));

    assertEquals(3, joinRows.size(), "expected three notes (two from create, one appended)");
    assertAll(
        () -> assertEquals("NoteAlpha01", joinRows.get(0).get("noteuid")),
        () -> assertEquals(1, ((Number) joinRows.get(0).get("sort_order")).intValue()),
        () -> assertEquals("NoteBeta002", joinRows.get(1).get("noteuid")),
        () -> assertEquals(2, ((Number) joinRows.get(1).get("sort_order")).intValue()),
        () -> assertEquals("NoteGamma01", joinRows.get(2).get("noteuid")),
        () ->
            assertEquals(
                "Third enrollment note appended on update", joinRows.get(2).get("notetext")),
        () -> assertEquals(3, ((Number) joinRows.get(2).get("sort_order")).intValue()));
  }

  private void assertEnrollmentCompletedData(Enrollment enrollment) {
    switch (enrollment.getStatus()) {
      case ACTIVE ->
          assertAll(
              () -> assertNull(enrollment.getCompletedBy()),
              () -> assertNull(enrollment.getCompletedDate()));
      case COMPLETED ->
          assertAll(
              () -> assertEquals(importUser.getUsername(), enrollment.getCompletedBy()),
              () -> assertNotNull(enrollment.getCompletedDate()));
      case CANCELLED ->
          assertAll(
              () -> assertNull(enrollment.getCompletedBy()),
              () -> assertNotNull(enrollment.getCompletedDate()));
    }
  }
}
