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
import static org.hisp.dhis.tracker.Assertions.assertNoErrors;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.TrackerImportStrategy;
import org.hisp.dhis.tracker.imports.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.tracker.imports.validation.ValidationCode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.util.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ameen Mohamed
 */
class OwnershipTest extends TrackerTest {
  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackerOwnershipManager trackerOwnershipManager;

  private User superUser;

  private User nonSuperUser;

  @Override
  protected void initTest() throws IOException {
    setUpMetadata("tracker/ownership_metadata.json");
    superUser = userService.getUser("M5zQapPyTZI");
    injectSecurityContext(superUser);

    nonSuperUser = userService.getUser("Tu9fv8ezgHl");
    TrackerImportParams params = TrackerImportParams.builder().userId(superUser.getUid()).build();
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/ownership_tei.json")));
    assertNoErrors(
        trackerImportService.importTracker(params, fromJson("tracker/ownership_enrollment.json")));
  }

  @Test
  void testProgramOwnerWhenEnrolled() throws IOException {
    TrackerObjects trackerObjects = fromJson("tracker/ownership_enrollment.json");

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
    assertEquals(
        trackerObjects.getEnrollments().get(0).getTrackedEntity(),
        tepo.getTrackedEntity().getUid());
  }

  @Test
  void testClientDatesForTeiEnrollmentEvent() throws IOException {
    User nonSuperUser = userService.getUser(this.nonSuperUser.getUid());
    injectSecurityContext(nonSuperUser);
    TrackerImportParams params =
        TrackerImportParams.builder().userId(nonSuperUser.getUid()).build();
    TrackerObjects trackerObjects = fromJson("tracker/ownership_event.json");
    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);
    manager.flush();
    TrackerObjects teTrackerObjects = fromJson("tracker/ownership_tei.json");
    TrackerObjects enTrackerObjects = fromJson("tracker/ownership_enrollment.json");
    assertNoErrors(importReport);

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
    Set<Event> events = enrollment.getEvents();
    assertEquals(1, events.size());
    Event event = events.iterator().next();
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
    TrackerObjects trackerObjects = fromJson("tracker/ownership_enrollment.json");
    TrackerImportParams params =
        TrackerImportParams.builder().userId(nonSuperUser.getUid()).build();
    List<Enrollment> enrollments = manager.getAll(Enrollment.class);
    assertEquals(2, enrollments.size());
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
    assertEquals(2, enrollments.size());
    enrollment = enrollments.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    compareEnrollmentBasicProperties(enrollment, updatedEnrollment);
    assertNotNull(enrollment.getCompletedBy());
    assertNotNull(enrollment.getCompletedDate());
  }

  @Test
  void testDeleteEnrollment() throws IOException {
    TrackerImportParams params =
        TrackerImportParams.builder().userId(nonSuperUser.getUid()).build();
    TrackerObjects trackerObjects = fromJson("tracker/ownership_enrollment.json");
    List<Enrollment> enrollments = manager.getAll(Enrollment.class);
    assertEquals(2, enrollments.size());
    enrollments.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    enrollments = manager.getAll(Enrollment.class);
    assertEquals(1, enrollments.size());
  }

  @Test
  void testCreateEnrollmentAfterDeleteEnrollment() throws IOException {
    injectSecurityContext(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams params =
        TrackerImportParams.builder().userId(nonSuperUser.getUid()).build();
    TrackerObjects trackerObjects = fromJson("tracker/ownership_enrollment.json");
    List<Enrollment> enrollments = manager.getAll(Enrollment.class);
    assertEquals(2, enrollments.size());
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    enrollments = manager.getAll(Enrollment.class);
    assertEquals(1, enrollments.size());
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    trackerObjects.getEnrollments().get(0).setEnrollment(CodeGenerator.generateUid());
    updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getCreated());
    enrollments = manager.getAll(Enrollment.class);
    assertEquals(2, enrollments.size());
  }

  @Test
  void testCreateEnrollmentWithoutOwnership() throws IOException {
    injectSecurityContext(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams params =
        TrackerImportParams.builder().userId(nonSuperUser.getUid()).build();
    TrackerObjects trackerObjects = fromJson("tracker/ownership_enrollment.json");
    List<Enrollment> enrollments = manager.getAll(Enrollment.class);
    assertEquals(2, enrollments.size());
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    TrackedEntity te = manager.get(TrackedEntity.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    trackerOwnershipManager.transferOwnership(te, pgm, ou, true, false);
    params.setImportStrategy(TrackerImportStrategy.CREATE);
    trackerObjects.getEnrollments().get(0).setEnrollment(CodeGenerator.generateUid());
    updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  @Test
  void testDeleteEnrollmentWithoutOwnership() throws IOException {
    // Change ownership
    TrackedEntity te = manager.get(TrackedEntity.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    TrackerImportParams params =
        TrackerImportParams.builder().userId(nonSuperUser.getUid()).build();
    TrackerObjects trackerObjects = fromJson("tracker/ownership_enrollment.json");
    trackerOwnershipManager.transferOwnership(te, pgm, ou, true, false);
    params.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  @Test
  void testUpdateEnrollmentWithoutOwnership() throws IOException {
    // Change ownership
    TrackedEntity te = manager.get(TrackedEntity.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    trackerOwnershipManager.transferOwnership(te, pgm, ou, true, false);
    TrackerImportParams params =
        TrackerImportParams.builder().userId(nonSuperUser.getUid()).build();
    TrackerObjects trackerObjects = fromJson("tracker/ownership_enrollment.json");
    params.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    ImportReport updatedReport = trackerImportService.importTracker(params, trackerObjects);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  private void compareEnrollmentBasicProperties(
      Enrollment dbEnrollment, org.hisp.dhis.tracker.imports.domain.Enrollment enrollment) {
    assertEquals(
        DateUtils.fromInstant(enrollment.getEnrolledAt()), dbEnrollment.getEnrollmentDate());
    assertEquals(DateUtils.fromInstant(enrollment.getOccurredAt()), dbEnrollment.getIncidentDate());
    assertEquals(
        DateUtils.fromInstant(enrollment.getCreatedAtClient()), dbEnrollment.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enrollment.getUpdatedAtClient()),
        dbEnrollment.getLastUpdatedAtClient());
    assertEquals(enrollment.getStatus().toString(), dbEnrollment.getStatus().toString());
  }
}
