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
package org.hisp.dhis.tracker.bundle;

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.domain.EnrollmentStatus.ACTIVE;
import static org.hisp.dhis.tracker.domain.EnrollmentStatus.CANCELLED;
import static org.hisp.dhis.tracker.domain.EnrollmentStatus.COMPLETED;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.util.stream.Stream;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class EnrollmentImportTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  private User importUser;

  @Autowired protected UserService _userService;

  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/simple_metadata.json");

    importUser = userService.getUser("M5zQapPyTZI");
    injectSecurityContext(importUser);
  }

  @ParameterizedTest
  @MethodSource("statuses")
  void shouldCorrectlyPopulateCompletedDataWhenCreatingAnEnrollment(EnrollmentStatus status)
      throws IOException {
    TrackerImportParams params = fromJson("tracker/te_enrollment_event.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.getEnrollments().get(0).setStatus(status);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    ProgramInstance enrollment =
        manager.get(ProgramInstance.class, params.getEnrollments().get(0).getUid());

    assertEnrollmentCompletedData(enrollment);
  }

  @ParameterizedTest
  @MethodSource("transitionStatuses")
  void shouldCorrectlyPopulateCompletedDataWhenUpdatingAnEnrollment(
      EnrollmentStatus savedStatus, EnrollmentStatus updatedStatus) throws IOException {
    TrackerImportParams params = fromJson("tracker/te_enrollment_event.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    params.getEnrollments().get(0).setStatus(savedStatus);

    ImportReport importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    params.getEnrollments().get(0).setStatus(updatedStatus);
    importReport = trackerImportService.importTracker(params);

    assertNoErrors(importReport);

    ProgramInstance enrollment =
        manager.get(ProgramInstance.class, params.getEnrollments().get(0).getUid());

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

  private void assertEnrollmentCompletedData(ProgramInstance enrollment) {
    switch (enrollment.getStatus()) {
      case ACTIVE:
        assertAll(
            () -> assertNull(enrollment.getCompletedBy()),
            () -> assertNull(enrollment.getEndDate()));
        break;
      case COMPLETED:
        assertAll(
            () -> assertEquals(importUser.getUsername(), enrollment.getCompletedBy()),
            () -> assertNotNull(enrollment.getEndDate()));
        break;
      case CANCELLED:
        assertAll(
            () -> assertNull(enrollment.getCompletedBy()),
            () -> assertNotNull(enrollment.getEndDate()));
        break;
    }
  }
}
