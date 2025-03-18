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

import static org.hisp.dhis.tracker.Assertions.assertHasErrors;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_2;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheat;
import org.hisp.dhis.tracker.imports.preheat.TrackerPreheatService;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.ValidationReport;
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
class EnrollmentImportValidationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TrackerPreheatService trackerPreheatService;

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
    manager.flush();
  }

  @BeforeEach
  void setUpUser() {
    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);
  }

  @Test
  void testEnrollmentValidationOkAll() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data.json"));

    assertNoErrors(importReport);
  }

  @Test
  void testPreheatOwnershipForSubsequentEnrollment() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data.json"));
    assertNoErrors(importReport);

    TrackerObjects secondTrackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data.json");
    TrackerPreheat preheat =
        trackerPreheatService.preheat(secondTrackerObjects, new TrackerIdSchemeParams());
    secondTrackerObjects
        .getEnrollments()
        .forEach(
            e -> {
              assertTrue(
                  e.getOrgUnit()
                      .isEqualTo(
                          preheat
                              .getProgramOwner()
                              .get(e.getTrackedEntity())
                              .get(e.getProgram().getIdentifier())
                              .getOrganisationUnit()));
            });
  }

  @Test
  void testNoWriteAccessToOrg() throws IOException {
    User user = userService.getUser(USER_2);
    TrackerImportParams params = new TrackerImportParams();
    injectSecurityContextUser(user);

    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data.json"));

    assertHasErrors(importReport, 4, ValidationCode.E1000);
  }

  @Test
  void testOnlyProgramAttributesAllowedOnEnrollments() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_error_non_program_attr.json"));

    assertHasErrors(importReport, 3, ValidationCode.E1019);
  }

  @Test
  void testAttributesOk() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_attr-data.json"));

    assertNoErrors(importReport);
    assertEquals(
        1,
        importReport
            .getPersistenceReport()
            .getTypeReportMap()
            .get(TrackerType.ENROLLMENT)
            .getEntityReport()
            .size());
  }

  @Test
  void testDeleteCascadeEnrollments() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_attr-data.json"));

    assertNoErrors(importReport);
    manager.flush();
    importEvents();
    manager.flush();
    injectSecurityContextUser(userService.getUser(USER_4));
    params.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport trackerImportDeleteReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_attr-data.json"));

    assertHasOnlyErrors(trackerImportDeleteReport, ValidationCode.E1103, ValidationCode.E1091);
  }

  protected void importEvents() throws IOException {
    TrackerImportParams params =
        TrackerImportParams.builder().importStrategy(TrackerImportStrategy.CREATE).build();

    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/events-with-registration.json"));

    assertNoErrors(importReport);
  }

  @Test
  void testActiveEnrollmentAlreadyExists() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_double-te-enrollment_part1.json"));

    assertNoErrors(importReport);

    TrackerObjects trackerObjects2 =
        testSetup.fromJson("tracker/validations/enrollments_double-te-enrollment_part2.json");

    importReport = trackerImportService.importTracker(params, trackerObjects2);

    ValidationReport validationResult = importReport.getValidationReport();

    assertHasOnlyErrors(validationResult, ValidationCode.E1015);
  }

  @Test
  void testEnrollmentDeleteOk() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data.json"));
    assertNoErrors(importReport);

    manager.flush();
    manager.clear();

    params.setImportStrategy(TrackerImportStrategy.DELETE);

    ImportReport importReportDelete =
        trackerImportService.importTracker(
            params,
            testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data-delete.json"));

    assertNoErrors(importReportDelete);
    assertEquals(1, importReportDelete.getStats().getDeleted());
  }

  /** Notes with no value are ignored */
  @Test
  void testBadEnrollmentNoteNoValue() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_bad-note-no-value.json"));

    assertNoErrors(importReport);
  }
}
