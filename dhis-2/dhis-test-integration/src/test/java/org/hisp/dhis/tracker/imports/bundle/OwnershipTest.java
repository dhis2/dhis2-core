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

import static org.hisp.dhis.test.utils.Assertions.assertEqualUids;
import static org.hisp.dhis.tracker.Assertions.assertHasError;
import static org.hisp.dhis.tracker.Assertions.assertHasOnlyErrors;
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.TrackerEvent;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.imports.AtomicMode;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ameen Mohamed
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OwnershipTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerOwnershipManager trackerOwnershipManager;

  private User nonSuperUser;

  @BeforeAll
  void setUp() throws IOException {
    testSetup.importMetadata("tracker/ownership_metadata.json");

    User importUser = userService.getUser("tTgjgobT1oS");
    injectSecurityContextUser(importUser);

    testSetup.importTrackerData("tracker/ownership_te.json");
    testSetup.importTrackerData("tracker/ownership_enrollment.json");

    nonSuperUser = userService.getUser("Tu9fv8ezgHl");
  }

  @Test
  void testProgramOwnerWhenEnrolled() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/ownership_enrollment.json");

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);

    assertEquals(1, trackedEntities.size());
    TrackedEntity te = trackedEntities.get(0);
    assertNotNull(te.getProgramOwners());
    Set<TrackedEntityProgramOwner> tepos = te.getProgramOwners();
    assertEquals(1, tepos.size());
    TrackedEntityProgramOwner tepo = tepos.iterator().next();
    assertNotNull(tepo.getTrackedEntity());
    assertNotNull(tepo.getProgram());
    assertNotNull(tepo.getOrganisationUnit());
    assertTrue(trackerObjects.getEnrollments().get(0).getProgram().isEqualTo(tepo.getProgram()));
    assertTrue(
        trackerObjects.getEnrollments().get(0).getOrgUnit().isEqualTo(tepo.getOrganisationUnit()));
    assertEqualUids(
        trackerObjects.getEnrollments().get(0).getTrackedEntity(), tepo.getTrackedEntity());
  }

  @Test
  void testClientDatesForTrackedEntityEnrollmentEvent() throws IOException {
    User nonSuperUser = userService.getUser(this.nonSuperUser.getUid());
    injectSecurityContextUser(nonSuperUser);
    TrackerObjects trackerObjects = testSetup.importTrackerData("tracker/ownership_event.json");
    manager.flush();
    TrackerObjects teTrackerObjects = testSetup.fromJson("tracker/ownership_te.json");
    TrackerObjects enTrackerObjects = testSetup.fromJson("tracker/ownership_enrollment.json");

    List<TrackedEntity> trackedEntities = manager.getAll(TrackedEntity.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntity te = trackedEntities.get(0);
    assertNotNull(te.getCreatedAtClient());
    assertNotNull(te.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(teTrackerObjects.getTrackedEntities().get(0).getCreatedAtClient()),
        te.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(teTrackerObjects.getTrackedEntities().get(0).getUpdatedAtClient()),
        te.getLastUpdatedAtClient());
    Set<Enrollment> enrollments = te.getEnrollments();
    assertEquals(1, enrollments.size());
    Enrollment enrollment = enrollments.iterator().next();
    assertNotNull(enrollment.getCreatedAtClient());
    assertNotNull(enrollment.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enTrackerObjects.getEnrollments().get(0).getCreatedAtClient()),
        enrollment.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enTrackerObjects.getEnrollments().get(0).getUpdatedAtClient()),
        enrollment.getLastUpdatedAtClient());
    Set<TrackerEvent> events = enrollment.getEvents();
    assertEquals(1, events.size());
    TrackerEvent event = events.iterator().next();
    assertNotNull(event.getCreatedAtClient());
    assertNotNull(event.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(trackerObjects.getEvents().get(0).getCreatedAtClient()),
        event.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(trackerObjects.getEvents().get(0).getUpdatedAtClient()),
        event.getLastUpdatedAtClient());
  }

  @Test
  void testUpdateEnrollment() throws IOException {
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/ownership_enrollment.json");
    TrackerImportParams params = TrackerImportParams.builder().build();
    List<Enrollment> enrollments = manager.getAll(Enrollment.class);
    assertEquals(1, enrollments.size());
    Enrollment enrollment =
        enrollments.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    compareEnrollmentBasicProperties(enrollment, trackerObjects.getEnrollments().get(0));
    assertNull(enrollment.getCompletedBy());
    assertNull(enrollment.getCompletedDate());

    org.hisp.dhis.tracker.imports.domain.Enrollment updatedEnrollment =
        trackerObjects.getEnrollments().get(0);
    updatedEnrollment.setStatus(EnrollmentStatus.COMPLETED);
    updatedEnrollment.setCreatedAtClient(Instant.now());
    updatedEnrollment.setUpdatedAtClient(Instant.now());
    updatedEnrollment.setEnrolledAt(Instant.now());
    updatedEnrollment.setOccurredAt(Instant.now());
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    manager.flush();
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getUpdated());
    enrollments = manager.getAll(Enrollment.class);
    assertEquals(1, enrollments.size());
    enrollment = enrollments.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    compareEnrollmentBasicProperties(enrollment, updatedEnrollment);
    assertNotNull(enrollment.getCompletedBy());
    assertNotNull(enrollment.getCompletedDate());
  }

  @Test
  void testDeleteEnrollment() throws IOException {
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/ownership_enrollment.json");
    List<Enrollment> enrollments = manager.getAll(Enrollment.class);
    assertEquals(1, enrollments.size());
    enrollments.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    enrollments = manager.getAll(Enrollment.class).stream().filter(en -> !en.isDeleted()).toList();
    assertEquals(0, enrollments.size());
  }

  @Test
  void testCreateEnrollmentAfterDeleteEnrollment() throws IOException {
    injectSecurityContextUser(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/ownership_enrollment.json");
    List<Enrollment> enrollments = manager.getAll(Enrollment.class);
    assertEquals(1, enrollments.stream().filter(en -> !en.isDeleted()).count());
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    manager.clear();
    manager.flush();
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    enrollments = manager.getAll(Enrollment.class);
    assertEquals(0, enrollments.stream().filter(en -> !en.isDeleted()).count());
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    trackerObjects.getEnrollments().get(0).setEnrollment(UID.generate());
    updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getCreated());
    enrollments = manager.getAll(Enrollment.class);
    assertEquals(1, enrollments.stream().filter(en -> !en.isDeleted()).count());
  }

  @Test
  void testCreateEnrollmentWithoutOwnership()
      throws IOException, ForbiddenException, BadRequestException, NotFoundException {
    injectSecurityContextUser(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/ownership_enrollment.json");
    List<Enrollment> enrollments = manager.getAll(Enrollment.class);
    assertEquals(1, enrollments.size());
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    trackerOwnershipManager.transferOwnership(
        manager.get(TrackedEntity.class, "IOR1AXXl24H"),
        UID.of("BFcipDERJnf"),
        UID.of("B1nCbRV3qtP"));
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    trackerObjects.getEnrollments().get(0).setEnrollment(UID.generate());
    updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  @Test
  void shouldFailWhenCreatingTEAndEnrollmentAndUserHasNoAccessToEnrollmentOU() throws IOException {
    injectSecurityContextUser(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams params =
        TrackerImportParams.builder().atomicMode(AtomicMode.OBJECT).build();
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/ownership_te_ok_enrollment_no_access.json");

    ImportReport report = trackerImportService.importTracker(params, trackerObjects);

    assertEquals(1, report.getStats().getCreated());
    assertEquals(1, report.getStats().getIgnored());
    assertHasOnlyErrors(report, ValidationCode.E1000);
  }

  @Test
  void testDeleteEnrollmentWithoutOwnership()
      throws IOException, ForbiddenException, BadRequestException, NotFoundException {
    // Change ownership
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/ownership_enrollment.json");
    trackerOwnershipManager.transferOwnership(
        manager.get(TrackedEntity.class, "IOR1AXXl24H"),
        UID.of("BFcipDERJnf"),
        UID.of("B1nCbRV3qtP"));
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  @Test
  void testUpdateEnrollmentWithoutOwnership()
      throws IOException, ForbiddenException, BadRequestException, NotFoundException {
    // Change ownership
    trackerOwnershipManager.transferOwnership(
        manager.get(TrackedEntity.class, "IOR1AXXl24H"),
        UID.of("BFcipDERJnf"),
        UID.of("B1nCbRV3qtP"));
    TrackerImportParams params = TrackerImportParams.builder().build();
    TrackerObjects trackerObjects = testSetup.fromJson("tracker/ownership_enrollment.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  private void compareEnrollmentBasicProperties(
      Enrollment dbEnrollment, org.hisp.dhis.tracker.imports.domain.Enrollment enrollment) {
    assertEquals(
        DateUtils.fromInstant(enrollment.getEnrolledAt()), dbEnrollment.getEnrollmentDate());
    assertEquals(DateUtils.fromInstant(enrollment.getOccurredAt()), dbEnrollment.getOccurredDate());
    assertEquals(
        DateUtils.fromInstant(enrollment.getCreatedAtClient()), dbEnrollment.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enrollment.getUpdatedAtClient()),
        dbEnrollment.getLastUpdatedAtClient());
    assertEquals(enrollment.getStatus().toString(), dbEnrollment.getStatus().toString());
  }
}
