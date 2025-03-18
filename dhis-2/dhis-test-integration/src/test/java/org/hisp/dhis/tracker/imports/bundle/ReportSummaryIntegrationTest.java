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

import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.Status;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportSummaryIntegrationTest extends PostgresIntegrationTestBase {

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private TestSetup testSetup;

  private User userA;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata();

    userA = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(userA);
  }

  @Test
  void testStatsCountForOneCreatedTE() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/single_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();

    ImportReport trackerImportTeReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(trackerImportTeReport);
    assertEquals(1, trackerImportTeReport.getStats().getCreated());
    assertEquals(0, trackerImportTeReport.getStats().getUpdated());
    assertEquals(0, trackerImportTeReport.getStats().getIgnored());
    assertEquals(0, trackerImportTeReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneCreatedAndOneUpdatedTE() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/single_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/one_update_te_and_one_new_te.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport trackerImportTeReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(trackerImportTeReport);
    assertEquals(1, trackerImportTeReport.getStats().getCreated());
    assertEquals(1, trackerImportTeReport.getStats().getUpdated());
    assertEquals(0, trackerImportTeReport.getStats().getIgnored());
    assertEquals(0, trackerImportTeReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneCreatedAndOneUpdatedTEAndOneInvalidTE() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/single_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects =
        testSetup.fromJson("tracker/one_update_te_and_one_new_te_and_one_invalid_te.json");
    params.setAtomicMode(AtomicMode.OBJECT);
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport trackerImportTeReport = trackerImportService.importTracker(params, trackerObjects);

    assertNotNull(trackerImportTeReport);
    assertEquals(Status.OK, trackerImportTeReport.getStatus());
    assertEquals(1, trackerImportTeReport.getValidationReport().getErrors().size());
    assertEquals(1, trackerImportTeReport.getStats().getCreated());
    assertEquals(1, trackerImportTeReport.getStats().getUpdated());
    assertEquals(1, trackerImportTeReport.getStats().getIgnored());
    assertEquals(0, trackerImportTeReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneCreatedEnrollment() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/single_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_enrollment.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport trackerImportEnrollmentReport =
        trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(trackerImportEnrollmentReport);
    assertEquals(1, trackerImportEnrollmentReport.getStats().getCreated());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getUpdated());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getIgnored());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneCreatedEnrollmentAndUpdateSameEnrollment() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/single_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_enrollment.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport trackerImportEnrollmentReport =
        trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(trackerImportEnrollmentReport);
    assertEquals(1, trackerImportEnrollmentReport.getStats().getCreated());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getUpdated());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getIgnored());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getDeleted());

    trackerObjects = testSetup.fromJson("tracker/single_enrollment.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    trackerImportEnrollmentReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(trackerImportEnrollmentReport);
    assertEquals(0, trackerImportEnrollmentReport.getStats().getCreated());
    assertEquals(1, trackerImportEnrollmentReport.getStats().getUpdated());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getIgnored());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneUpdateEnrollmentAndOneCreatedEnrollment() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/one_update_te_and_one_new_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_enrollment.json");
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects =
        testSetup.fromJson("tracker/one_update_enrollment_and_one_new_enrollment.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport trackerImportEnrollmentReport =
        trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(trackerImportEnrollmentReport);
    assertEquals(1, trackerImportEnrollmentReport.getStats().getCreated());
    assertEquals(1, trackerImportEnrollmentReport.getStats().getUpdated());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getIgnored());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneUpdateEnrollmentAndOneCreatedEnrollmentAndOneInvalidEnrollment()
      throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/three_tes.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_enrollment.json");
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects =
        testSetup.fromJson("tracker/one_update_and_one_new_and_one_invalid_enrollment.json");

    params.setAtomicMode(AtomicMode.OBJECT);
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport trackerImportEnrollmentReport =
        trackerImportService.importTracker(params, trackerObjects);

    assertNotNull(trackerImportEnrollmentReport);
    assertEquals(Status.OK, trackerImportEnrollmentReport.getStatus());
    assertEquals(1, trackerImportEnrollmentReport.getValidationReport().getErrors().size());
    assertEquals(1, trackerImportEnrollmentReport.getStats().getCreated());
    assertEquals(1, trackerImportEnrollmentReport.getStats().getUpdated());
    assertEquals(1, trackerImportEnrollmentReport.getStats().getIgnored());
    assertEquals(0, trackerImportEnrollmentReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneCreatedEvent() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/single_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_enrollment.json");
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_event.json");
    ImportReport trackerImportEventReport =
        trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(trackerImportEventReport);
    assertEquals(1, trackerImportEventReport.getStats().getCreated());
    assertEquals(0, trackerImportEventReport.getStats().getUpdated());
    assertEquals(0, trackerImportEventReport.getStats().getIgnored());
    assertEquals(0, trackerImportEventReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneUpdateEventAndOneNewEvent() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/single_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_enrollment.json");
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_event.json");
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/one_update_event_and_one_new_event.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport trackerImportEventReport =
        trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(trackerImportEventReport);
    assertEquals(1, trackerImportEventReport.getStats().getCreated());
    assertEquals(1, trackerImportEventReport.getStats().getUpdated());
    assertEquals(0, trackerImportEventReport.getStats().getIgnored());
    assertEquals(0, trackerImportEventReport.getStats().getDeleted());
  }

  @Test
  void testStatsCountForOneUpdateEventAndOneNewEventAndOneInvalidEvent() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/single_te.json");
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_enrollment.json");
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects = testSetup.fromJson("tracker/single_event.json");
    trackerImportService.importTracker(params, trackerObjects);

    trackerObjects =
        testSetup.fromJson("tracker/one_update_and_one_new_and_one_invalid_event.json");
    params.setAtomicMode(AtomicMode.OBJECT);
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);

    ImportReport trackerImportEventReport =
        trackerImportService.importTracker(params, trackerObjects);

    assertNotNull(trackerImportEventReport);
    assertEquals(Status.OK, trackerImportEventReport.getStatus());
    assertEquals(1, trackerImportEventReport.getValidationReport().getErrors().size());
    assertEquals(1, trackerImportEventReport.getStats().getCreated());
    assertEquals(1, trackerImportEventReport.getStats().getUpdated());
    assertEquals(1, trackerImportEventReport.getStats().getIgnored());
    assertEquals(0, trackerImportEventReport.getStats().getDeleted());
  }
}
