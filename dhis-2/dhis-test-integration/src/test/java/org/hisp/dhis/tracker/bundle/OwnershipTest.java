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
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.AtomicMode;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.report.ImportReport;
import org.hisp.dhis.tracker.validation.ValidationCode;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
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
  @Autowired protected UserService _userService;
  private User superUser;

  private User nonSuperUser;

  @Override
  protected void initTest() throws IOException {
    userService = _userService;
    setUpMetadata("tracker/ownership_metadata.json");
    superUser = userService.getUser("M5zQapPyTZI");
    injectSecurityContext(superUser);

    nonSuperUser = userService.getUser("Tu9fv8ezgHl");
    TrackerImportParams params = TrackerImportParams.builder().userId(superUser.getUid()).build();
    assertNoErrors(trackerImportService.importTracker(fromJson("tracker/ownership_tei.json")));
    assertNoErrors(
        trackerImportService.importTracker(fromJson("tracker/ownership_enrollment.json")));
  }

  @Test
  void testProgramOwnerWhenEnrolled() throws IOException {
    TrackerImportParams trackerImportParams = fromJson("tracker/ownership_enrollment.json");

    List<TrackedEntityInstance> trackedEntities = manager.getAll(TrackedEntityInstance.class);

    assertEquals(1, trackedEntities.size());
    TrackedEntityInstance te = trackedEntities.get(0);
    assertNotNull(te.getProgramOwners());
    Set<TrackedEntityProgramOwner> tepos = te.getProgramOwners();
    assertEquals(1, tepos.size());
    TrackedEntityProgramOwner tepo = tepos.iterator().next();
    assertNotNull(tepo.getEntityInstance());
    assertNotNull(tepo.getProgram());
    assertNotNull(tepo.getOrganisationUnit());
    assertTrue(
        trackerImportParams.getEnrollments().get(0).getProgram().isEqualTo(tepo.getProgram()));
    assertTrue(
        trackerImportParams
            .getEnrollments()
            .get(0)
            .getOrgUnit()
            .isEqualTo(tepo.getOrganisationUnit()));
    assertEquals(
        trackerImportParams.getEnrollments().get(0).getTrackedEntity(),
        tepo.getEntityInstance().getUid());
  }

  @Test
  void testClientDatesForTeiEnrollmentEvent() throws IOException {
    User nonSuperUser = userService.getUser(this.nonSuperUser.getUid());
    injectSecurityContext(nonSuperUser);
    TrackerImportParams params =
        TrackerImportParams.builder().userId(nonSuperUser.getUid()).build();
    TrackerImportParams trackerImportParams = fromJson("tracker/ownership_event.json");
    trackerImportParams.setUserId(nonSuperUser.getUid());
    ImportReport importReport = trackerImportService.importTracker(trackerImportParams);
    manager.flush();
    TrackerImportParams teTrackerImportParams = fromJson("tracker/ownership_tei.json");
    TrackerImportParams enTrackerImportParams = fromJson("tracker/ownership_enrollment.json");
    assertNoErrors(importReport);

    List<TrackedEntityInstance> trackedEntities = manager.getAll(TrackedEntityInstance.class);
    assertEquals(1, trackedEntities.size());
    TrackedEntityInstance te = trackedEntities.get(0);
    assertNotNull(te.getCreatedAtClient());
    assertNotNull(te.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(
            teTrackerImportParams.getTrackedEntities().get(0).getCreatedAtClient()),
        te.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(
            teTrackerImportParams.getTrackedEntities().get(0).getUpdatedAtClient()),
        te.getLastUpdatedAtClient());
    Set<ProgramInstance> enrollments = te.getProgramInstances();
    assertEquals(1, enrollments.size());
    ProgramInstance enrollment = enrollments.iterator().next();
    assertNotNull(enrollment.getCreatedAtClient());
    assertNotNull(enrollment.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enTrackerImportParams.getEnrollments().get(0).getCreatedAtClient()),
        enrollment.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enTrackerImportParams.getEnrollments().get(0).getUpdatedAtClient()),
        enrollment.getLastUpdatedAtClient());
    Set<ProgramStageInstance> events = enrollment.getProgramStageInstances();
    assertEquals(1, events.size());
    ProgramStageInstance event = events.iterator().next();
    assertNotNull(event.getCreatedAtClient());
    assertNotNull(event.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(trackerImportParams.getEvents().get(0).getCreatedAtClient()),
        event.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(trackerImportParams.getEvents().get(0).getUpdatedAtClient()),
        event.getLastUpdatedAtClient());
  }

  @Test
  void testUpdateProgramInstance() throws IOException {
    TrackerImportParams trackerImportParams = fromJson("tracker/ownership_enrollment.json");
    List<ProgramInstance> enrollments = manager.getAll(ProgramInstance.class);
    assertEquals(2, enrollments.size());
    ProgramInstance enrollment =
        enrollments.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    compareProgramInstanceBasicProperties(enrollment, trackerImportParams.getEnrollments().get(0));
    assertNull(enrollment.getCompletedBy());
    assertNull(enrollment.getEndDate());

    Enrollment updatedProgramInstance = trackerImportParams.getEnrollments().get(0);
    updatedProgramInstance.setStatus(EnrollmentStatus.COMPLETED);
    updatedProgramInstance.setCreatedAtClient(Instant.now());
    updatedProgramInstance.setUpdatedAtClient(Instant.now());
    updatedProgramInstance.setEnrolledAt(Instant.now());
    updatedProgramInstance.setOccurredAt(Instant.now());
    trackerImportParams.setUserId(nonSuperUser.getUid());
    trackerImportParams.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    ImportReport updatedReport = trackerImportService.importTracker(trackerImportParams);
    manager.flush();
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getUpdated());
    enrollments = manager.getAll(ProgramInstance.class);
    assertEquals(2, enrollments.size());
    enrollment = enrollments.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    compareProgramInstanceBasicProperties(enrollment, updatedProgramInstance);
    assertNotNull(enrollment.getCompletedBy());
    assertNotNull(enrollment.getEndDate());
  }

  @Test
  void testDeleteProgramInstance() throws IOException {
    TrackerImportParams trackerImportParams = fromJson("tracker/ownership_enrollment.json");
    List<ProgramInstance> enrollments = manager.getAll(ProgramInstance.class);
    assertEquals(2, enrollments.size());
    enrollments.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    trackerImportParams.setUserId(nonSuperUser.getUid());
    trackerImportParams.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(trackerImportParams);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    enrollments = manager.getAll(ProgramInstance.class);
    assertEquals(1, enrollments.size());
  }

  @Test
  void testCreateProgramInstanceAfterDeleteProgramInstance() throws IOException {
    injectSecurityContext(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams trackerImportParams = fromJson("tracker/ownership_enrollment.json");
    List<ProgramInstance> enrollments = manager.getAll(ProgramInstance.class);
    assertEquals(2, enrollments.size());
    trackerImportParams.setImportStrategy(TrackerImportStrategy.DELETE);
    trackerImportParams.setUserId(nonSuperUser.getUid());
    ImportReport updatedReport = trackerImportService.importTracker(trackerImportParams);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    enrollments = manager.getAll(ProgramInstance.class);
    assertEquals(1, enrollments.size());
    trackerImportParams.setImportStrategy(TrackerImportStrategy.CREATE);
    trackerImportParams.getEnrollments().get(0).setEnrollment(CodeGenerator.generateUid());
    updatedReport = trackerImportService.importTracker(trackerImportParams);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getCreated());
    enrollments = manager.getAll(ProgramInstance.class);
    assertEquals(2, enrollments.size());
  }

  @Test
  void testCreateProgramInstanceWithoutOwnership() throws IOException, ForbiddenException {
    injectSecurityContext(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams trackerImportParams = fromJson("tracker/ownership_enrollment.json");
    List<ProgramInstance> enrollments = manager.getAll(ProgramInstance.class);
    assertEquals(2, enrollments.size());
    trackerImportParams.setUserId(nonSuperUser.getUid());
    trackerImportParams.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(trackerImportParams);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    TrackedEntityInstance te = manager.get(TrackedEntityInstance.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    trackerOwnershipManager.transferOwnership(te, pgm, ou, true, false);
    trackerImportParams.setImportStrategy(TrackerImportStrategy.CREATE);
    trackerImportParams.getEnrollments().get(0).setEnrollment(CodeGenerator.generateUid());
    updatedReport = trackerImportService.importTracker(trackerImportParams);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  @Test
  void shouldFailWhenCreatingTEAndProgramInstanceAndUserHasNoAccessToProgramInstanceOU()
      throws IOException {
    injectSecurityContext(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams trackerImportParams =
        fromJson("tracker/ownership_te_ok_enrollment_no_access.json");
    trackerImportParams.setUserId(nonSuperUser.getUid());
    trackerImportParams.setAtomicMode(AtomicMode.OBJECT);

    ImportReport report = trackerImportService.importTracker(trackerImportParams);
    assertEquals(1, report.getStats().getCreated());
    assertEquals(1, report.getStats().getIgnored());
    assertHasOnlyErrors(report, ValidationCode.E1000);
  }

  @Test
  void testDeleteProgramInstanceWithoutOwnership() throws IOException, ForbiddenException {
    // Change ownership
    TrackedEntityInstance te = manager.get(TrackedEntityInstance.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    TrackerImportParams trackerImportParams = fromJson("tracker/ownership_enrollment.json");
    trackerOwnershipManager.transferOwnership(te, pgm, ou, true, false);
    trackerImportParams.setUserId(nonSuperUser.getUid());
    trackerImportParams.setImportStrategy(TrackerImportStrategy.DELETE);
    ImportReport updatedReport = trackerImportService.importTracker(trackerImportParams);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  @Test
  void testUpdateProgramInstanceWithoutOwnership() throws IOException, ForbiddenException {
    // Change ownership
    TrackedEntityInstance te = manager.get(TrackedEntityInstance.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    trackerOwnershipManager.transferOwnership(te, pgm, ou, true, false);
    TrackerImportParams trackerImportParams = fromJson("tracker/ownership_enrollment.json");
    trackerImportParams.setUserId(nonSuperUser.getUid());
    trackerImportParams.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    ImportReport updatedReport = trackerImportService.importTracker(trackerImportParams);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, ValidationCode.E1102);
  }

  private void compareProgramInstanceBasicProperties(
      ProgramInstance dbProgramInstance, Enrollment enrollment) {
    assertEquals(
        DateUtils.fromInstant(enrollment.getEnrolledAt()), dbProgramInstance.getEnrollmentDate());
    assertEquals(
        DateUtils.fromInstant(enrollment.getOccurredAt()), dbProgramInstance.getIncidentDate());
    assertEquals(
        DateUtils.fromInstant(enrollment.getCreatedAtClient()),
        dbProgramInstance.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enrollment.getUpdatedAtClient()),
        dbProgramInstance.getLastUpdatedAtClient());
    assertEquals(enrollment.getStatus().toString(), dbProgramInstance.getStatus().toString());
  }
}
