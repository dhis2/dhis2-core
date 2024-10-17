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
package org.hisp.dhis.tracker.imports.bundle;

import static org.hisp.dhis.program.EnrollmentStatus.*;
import static org.hisp.dhis.program.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.stream.Stream;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.export.enrollment.EnrollmentService;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class EnrollmentImportTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private EnrollmentService enrollmentService;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void shouldPopulateCompletedDataWhenCreatingAnEnrollmentWithStatusCompleted()
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEnrollments().get(0).setStatus(COMPLETED);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    Enrollment enrollment = enrollmentService.getEnrollment("TvctPPhpD8u");

    assertEquals(importUser.getUsername(), enrollment.getCompletedBy());
    assertNotNull(enrollment.getCompletedDate());
  }

  @Test
  void shouldPopulateCompletedDateWhenCreatingAnEnrollmentWithStatusCancelled()
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEnrollments().get(0).setStatus(CANCELLED);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    Enrollment enrollment = enrollmentService.getEnrollment("TvctPPhpD8u");

    assertNull(enrollment.getCompletedBy());
    assertNotNull(enrollment.getCompletedDate());
  }

  @Test
  void shouldNotPopulateCompletedDataWhenCreatingAnEnrollmentWithStatusActive()
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEnrollments().get(0).setStatus(ACTIVE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    Enrollment enrollment = enrollmentService.getEnrollment("TvctPPhpD8u");

    assertNull(enrollment.getCompletedBy());
    assertNull(enrollment.getCompletedDate());
  }

  @ParameterizedTest
  @MethodSource("statuses")
  void shouldNotPopulateCompletedDataWhenUpdatingAnEnrollmentToActiveStatus(EnrollmentStatus status)
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEnrollments().get(0).setStatus(status);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    trackerObjects.getEnrollments().get(0).setStatus(ACTIVE);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    Enrollment enrollment = enrollmentService.getEnrollment("TvctPPhpD8u");

    assertNull(enrollment.getCompletedBy());
    assertNull(enrollment.getCompletedDate());
  }

  @ParameterizedTest
  @MethodSource("statuses")
  void shouldPopulateCompletedDateWhenUpdatingAnEnrollmentToCancelledStatus(EnrollmentStatus status)
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEnrollments().get(0).setStatus(status);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    trackerObjects.getEnrollments().get(0).setStatus(CANCELLED);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    Enrollment enrollment = enrollmentService.getEnrollment("TvctPPhpD8u");

    assertNull(enrollment.getCompletedBy());
    assertNotNull(enrollment.getCompletedDate());
  }

  @ParameterizedTest
  @MethodSource("statuses")
  void shouldPopulateCompletedDataWhenUpdatingAnEnrollmentToCompletedStatus(EnrollmentStatus status)
      throws IOException, ForbiddenException, NotFoundException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = fromJson("tracker/te_enrollment_event.json");
    trackerObjects.getEnrollments().get(0).setStatus(status);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    trackerObjects.getEnrollments().get(0).setStatus(COMPLETED);
    importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);

    Enrollment enrollment = enrollmentService.getEnrollment("TvctPPhpD8u");

    assertEquals(importUser.getUsername(), enrollment.getCompletedBy());
    assertNotNull(enrollment.getCompletedDate());
  }

  public Stream<Arguments> statuses() {
    return Stream.of(Arguments.of(ACTIVE), Arguments.of(CANCELLED), Arguments.of(COMPLETED));
  }
}
