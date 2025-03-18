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

import static org.hisp.dhis.program.EnrollmentStatus.*;
import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.stream.Stream;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentImportTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private EnrollmentService enrollmentService;

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
        enrollmentService.getEnrollment(trackerObjects.getEnrollments().get(0).getUid());

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
        enrollmentService.getEnrollment(trackerObjects.getEnrollments().get(0).getUid());

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
