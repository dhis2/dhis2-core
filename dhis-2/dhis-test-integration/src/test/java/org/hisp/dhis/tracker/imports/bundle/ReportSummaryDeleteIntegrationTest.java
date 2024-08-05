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

import static org.hisp.dhis.tracker.Assertions.assertHasError;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_10;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4016;
import static org.hisp.dhis.tracker.imports.validation.ValidationCode.E4020;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.hisp.dhis.dbms.DbmsManager;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.relationship.Relationship;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.TrackerType;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.report.PersistenceReport;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.user.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Zubair Asghar
 */
class ReportSummaryDeleteIntegrationTest extends TrackerTest {

  @Autowired private TrackerImportService trackerImportService;
  @Autowired private DbmsManager dbmsManager;

  private User importUser;

  @BeforeAll
  void setUp() throws IOException {
    setUpMetadata("tracker/tracker_basic_metadata.json");

    importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    TrackerImportParams params = TrackerImportParams.builder().userId(importUser.getUid()).build();

    TrackerObjects trackerObjects = fromJson("tracker/tracker_basic_data_before_deletion.json");
    assertEquals(13, trackerObjects.getTrackedEntities().size());
    assertEquals(2, trackerObjects.getEnrollments().size());
    assertEquals(2, trackerObjects.getEvents().size());
    assertEquals(2, trackerObjects.getRelationships().size());

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    PersistenceReport persistenceReport = importReport.getPersistenceReport();

    assertImportedObjects(13, persistenceReport, TrackerType.TRACKED_ENTITY);
    assertImportedObjects(2, persistenceReport, TrackerType.ENROLLMENT);
    assertImportedObjects(2, persistenceReport, TrackerType.EVENT);
    assertImportedObjects(2, persistenceReport, TrackerType.RELATIONSHIP);
    assertEquals(6, manager.getAll(Enrollment.class).size());
    assertEquals(
        persistenceReport.getTypeReportMap().get(TrackerType.EVENT).getStats().getCreated(),
        manager.getAll(Event.class).size());

    manager.clear();
  }

  @Test
  void shouldFailToDeleteNotExistentRelationship() throws IOException {
    TrackerObjects trackerObjects =
        fromJson("tracker/relationships_existent_and_not_existent_for_deletion.json");
    TrackerImportParams params =
        TrackerImportParams.builder()
            .userId(importUser.getUid())
            .importStrategy(TrackerImportStrategy.DELETE)
            .atomicMode(AtomicMode.OBJECT)
            .build();
    assertEquals(2, trackerObjects.getRelationships().size());

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertDeletedObjects(1, importReport.getPersistenceReport(), TrackerType.RELATIONSHIP);
    assertHasOnlyErrors(importReport, E4016);
  }

  @Test
  void shouldSuccessfullyDeleteRelationships() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/relationships_basic_data_for_deletion.json");
    assertEquals(2, trackerObjects.getRelationships().size());
    TrackerImportParams params =
        TrackerImportParams.builder()
            .userId(importUser.getUid())
            .importStrategy(TrackerImportStrategy.DELETE)
            .build();

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(importReport);
    assertDeletedObjects(2, importReport.getPersistenceReport(), TrackerType.RELATIONSHIP);
  }

  @Test
  void shouldSuccessfullyDeleteTrackedEntityAndNotAccessibleRelationshipLinkedToIt()
      throws IOException {
    TrackerObjects trackerObjects =
        fromJson("tracker/te_with_inaccessible_relationship_for_deletion.json");
    TrackerImportParams params =
        TrackerImportParams.builder()
            .userId(USER_10)
            .importStrategy(TrackerImportStrategy.DELETE)
            .atomicMode(AtomicMode.OBJECT)
            .build();
    assertEquals(1, trackerObjects.getTrackedEntities().size());

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertDeletedObjects(1, importReport.getPersistenceReport(), TrackerType.TRACKED_ENTITY);
    assertHasOnlyErrors(importReport, E4020);
  }

  @Test
  void testTrackedEntityDeletion() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/tracked_entity_basic_data_for_deletion.json");
    TrackerImportParams params =
        TrackerImportParams.builder()
            .userId(importUser.getUid())
            .importStrategy(TrackerImportStrategy.DELETE)
            .build();
    assertEquals(9, trackerObjects.getTrackedEntities().size());

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
    assertDeletedObjects(9, importReport.getPersistenceReport(), TrackerType.TRACKED_ENTITY);
    // remaining
    assertEquals(4, manager.getAll(TrackedEntity.class).size());
    assertEquals(4, manager.getAll(Enrollment.class).size());
    assertEquals(0, manager.getAll(Event.class).size());
    assertEquals(0, manager.getAll(Relationship.class).size());
  }

  @Test
  void testEnrollmentDeletion() throws IOException {
    dbmsManager.clearSession();
    assertEquals(2, manager.getAll(Event.class).size());
    TrackerImportParams params =
        TrackerImportParams.builder()
            .userId(importUser.getUid())
            .importStrategy(TrackerImportStrategy.DELETE)
            .build();
    TrackerObjects trackerObjects = fromJson("tracker/enrollment_basic_data_for_deletion.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
    assertDeletedObjects(1, importReport.getPersistenceReport(), TrackerType.ENROLLMENT);
    // remaining
    assertEquals(5, manager.getAll(Enrollment.class).size());
    assertEquals(1, manager.getAll(Event.class).size());
    assertEquals(2, manager.getAll(Relationship.class).size());
  }

  @Test
  void testEventDeletion() throws IOException {
    TrackerImportParams params =
        TrackerImportParams.builder()
            .userId(importUser.getUid())
            .importStrategy(TrackerImportStrategy.DELETE)
            .build();
    TrackerObjects trackerObjects = fromJson("tracker/event_basic_data_for_deletion.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
    assertDeletedObjects(1, importReport.getPersistenceReport(), TrackerType.EVENT);
    // remaining
    assertEquals(1, manager.getAll(Event.class).size());
    assertEquals(2, manager.getAll(Relationship.class).size());
  }

  @Test
  void testNonExistentEnrollment() throws IOException {
    TrackerImportParams params =
        TrackerImportParams.builder()
            .userId(importUser.getUid())
            .importStrategy(TrackerImportStrategy.DELETE)
            .build();
    TrackerObjects trackerObjects =
        fromJson("tracker/non_existent_enrollment_basic_data_for_deletion.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasError(importReport, ValidationCode.E1081);
  }

  @Test
  void testDeleteMultipleEntities() throws IOException {
    TrackerImportParams params =
        TrackerImportParams.builder()
            .userId(importUser.getUid())
            .importStrategy(TrackerImportStrategy.DELETE)
            .build();
    TrackerObjects trackerObjects = fromJson("tracker/tracker_data_for_deletion.json");

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
    assertDeletedObjects(1, importReport.getPersistenceReport(), TrackerType.ENROLLMENT);
    assertDeletedObjects(1, importReport.getPersistenceReport(), TrackerType.TRACKED_ENTITY);
  }

  private void assertImportedObjects(
      int expected, PersistenceReport persistenceReport, TrackerType trackedEntityType) {
    assertTrue(persistenceReport.getTypeReportMap().containsKey(trackedEntityType));
    assertEquals(
        expected,
        persistenceReport.getTypeReportMap().get(trackedEntityType).getStats().getCreated());
    assertEquals(
        expected,
        persistenceReport.getTypeReportMap().get(trackedEntityType).getEntityReport().size());
  }

  private void assertDeletedObjects(
      int expected, PersistenceReport persistenceReport, TrackerType trackedEntityType) {
    assertTrue(persistenceReport.getTypeReportMap().containsKey(trackedEntityType));
    assertEquals(
        expected,
        persistenceReport.getTypeReportMap().get(trackedEntityType).getStats().getDeleted());
    assertEquals(
        expected,
        persistenceReport.getTypeReportMap().get(trackedEntityType).getEntityReport().size());
  }
}
