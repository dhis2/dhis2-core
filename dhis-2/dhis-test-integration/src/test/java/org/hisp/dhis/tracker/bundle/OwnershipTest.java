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
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityProgramOwner;
import org.hisp.dhis.trackedentity.TrackerOwnershipManager;
import org.hisp.dhis.tracker.TrackerImportParams;
import org.hisp.dhis.tracker.TrackerImportService;
import org.hisp.dhis.tracker.TrackerImportStrategy;
import org.hisp.dhis.tracker.TrackerTest;
import org.hisp.dhis.tracker.domain.Enrollment;
import org.hisp.dhis.tracker.domain.EnrollmentStatus;
import org.hisp.dhis.tracker.report.TrackerErrorCode;
import org.hisp.dhis.tracker.report.TrackerImportReport;
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
    assertNoErrors(
        trackerImportService.importTracker(
            fromJson("tracker/ownership_tei.json", superUser.getUid())));
    assertNoErrors(
        trackerImportService.importTracker(
            fromJson("tracker/ownership_enrollment.json", superUser.getUid())));
  }

  @Test
  void testProgramOwnerWhenEnrolled() throws IOException {
    TrackerImportParams enrollmentParams =
        fromJson("tracker/ownership_enrollment.json", nonSuperUser.getUid());

    List<TrackedEntityInstance> teis = manager.getAll(TrackedEntityInstance.class);

    assertEquals(1, teis.size());
    TrackedEntityInstance tei = teis.get(0);
    assertNotNull(tei.getProgramOwners());
    Set<TrackedEntityProgramOwner> tepos = tei.getProgramOwners();
    assertEquals(1, tepos.size());
    TrackedEntityProgramOwner tepo = tepos.iterator().next();
    assertNotNull(tepo.getEntityInstance());
    assertNotNull(tepo.getProgram());
    assertNotNull(tepo.getOrganisationUnit());
    assertTrue(enrollmentParams.getEnrollments().get(0).getProgram().isEqualTo(tepo.getProgram()));
    assertTrue(
        enrollmentParams
            .getEnrollments()
            .get(0)
            .getOrgUnit()
            .isEqualTo(tepo.getOrganisationUnit()));
    assertEquals(
        enrollmentParams.getEnrollments().get(0).getTrackedEntity(),
        tepo.getEntityInstance().getUid());
  }

  @Test
  void testClientDatesForTeiEnrollmentEvent() throws IOException {
    User nonSuperUser = userService.getUser(this.nonSuperUser.getUid());
    injectSecurityContext(nonSuperUser);

    TrackerImportParams trackerImportParams =
        fromJson("tracker/ownership_event.json", nonSuperUser);
    TrackerImportReport trackerImportReport =
        trackerImportService.importTracker(trackerImportParams);
    manager.flush();
    TrackerImportParams teiParams = fromJson("tracker/ownership_tei.json", nonSuperUser);
    TrackerImportParams enrollmentParams =
        fromJson("tracker/ownership_enrollment.json", nonSuperUser);
    assertNoErrors(trackerImportReport);

    List<TrackedEntityInstance> teis = manager.getAll(TrackedEntityInstance.class);
    assertEquals(1, teis.size());
    TrackedEntityInstance tei = teis.get(0);
    assertNotNull(tei.getCreatedAtClient());
    assertNotNull(tei.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(teiParams.getTrackedEntities().get(0).getCreatedAtClient()),
        tei.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(teiParams.getTrackedEntities().get(0).getUpdatedAtClient()),
        tei.getLastUpdatedAtClient());
    Set<ProgramInstance> pis = tei.getProgramInstances();
    assertEquals(1, pis.size());
    ProgramInstance pi = pis.iterator().next();
    assertNotNull(pi.getCreatedAtClient());
    assertNotNull(pi.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enrollmentParams.getEnrollments().get(0).getCreatedAtClient()),
        pi.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enrollmentParams.getEnrollments().get(0).getUpdatedAtClient()),
        pi.getLastUpdatedAtClient());
    Set<ProgramStageInstance> psis = pi.getProgramStageInstances();
    assertEquals(1, psis.size());
    ProgramStageInstance psi = psis.iterator().next();
    assertNotNull(psi.getCreatedAtClient());
    assertNotNull(psi.getLastUpdatedAtClient());
    assertEquals(
        DateUtils.fromInstant(trackerImportParams.getEvents().get(0).getCreatedAtClient()),
        psi.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(trackerImportParams.getEvents().get(0).getUpdatedAtClient()),
        psi.getLastUpdatedAtClient());
  }

  @Test
  void testUpdateEnrollment() throws IOException {
    TrackerImportParams enrollmentParams =
        fromJson("tracker/ownership_enrollment.json", nonSuperUser.getUid());
    List<ProgramInstance> pis = manager.getAll(ProgramInstance.class);
    assertEquals(2, pis.size());
    ProgramInstance pi = pis.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    compareEnrollmentBasicProperties(pi, enrollmentParams.getEnrollments().get(0));
    assertNull(pi.getCompletedBy());
    assertNull(pi.getEndDate());

    Enrollment updatedEnrollment = enrollmentParams.getEnrollments().get(0);
    updatedEnrollment.setStatus(EnrollmentStatus.COMPLETED);
    updatedEnrollment.setCreatedAtClient(Instant.now());
    updatedEnrollment.setUpdatedAtClient(Instant.now());
    updatedEnrollment.setEnrolledAt(Instant.now());
    updatedEnrollment.setOccurredAt(Instant.now());
    enrollmentParams.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    TrackerImportReport updatedReport = trackerImportService.importTracker(enrollmentParams);
    manager.flush();
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getUpdated());
    pis = manager.getAll(ProgramInstance.class);
    assertEquals(2, pis.size());
    pi = pis.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    compareEnrollmentBasicProperties(pi, updatedEnrollment);
    assertNotNull(pi.getCompletedBy());
    assertNotNull(pi.getEndDate());
  }

  @Test
  void testDeleteEnrollment() throws IOException {
    TrackerImportParams enrollmentParams =
        fromJson("tracker/ownership_enrollment.json", nonSuperUser.getUid());
    List<ProgramInstance> pis = manager.getAll(ProgramInstance.class);
    assertEquals(2, pis.size());
    pis.stream().filter(e -> e.getUid().equals("TvctPPhpD8u")).findAny().get();
    enrollmentParams.setImportStrategy(TrackerImportStrategy.DELETE);
    TrackerImportReport updatedReport = trackerImportService.importTracker(enrollmentParams);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    pis = manager.getAll(ProgramInstance.class);
    assertEquals(1, pis.size());
  }

  @Test
  void testCreateEnrollmentAfterDeleteEnrollment() throws IOException {
    injectSecurityContext(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams enrollmentParams =
        fromJson("tracker/ownership_enrollment.json", nonSuperUser.getUid());
    List<ProgramInstance> pis = manager.getAll(ProgramInstance.class);
    assertEquals(2, pis.size());
    enrollmentParams.setImportStrategy(TrackerImportStrategy.DELETE);
    TrackerImportReport updatedReport = trackerImportService.importTracker(enrollmentParams);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    pis = manager.getAll(ProgramInstance.class);
    assertEquals(1, pis.size());
    enrollmentParams.setImportStrategy(TrackerImportStrategy.CREATE);
    enrollmentParams.getEnrollments().get(0).setEnrollment(CodeGenerator.generateUid());
    updatedReport = trackerImportService.importTracker(enrollmentParams);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getCreated());
    pis = manager.getAll(ProgramInstance.class);
    assertEquals(2, pis.size());
  }

  @Test
  void testCreateEnrollmentWithoutOwnership() throws IOException {
    injectSecurityContext(userService.getUser(nonSuperUser.getUid()));
    TrackerImportParams enrollmentParams =
        fromJson("tracker/ownership_enrollment.json", nonSuperUser);
    List<ProgramInstance> pis = manager.getAll(ProgramInstance.class);
    assertEquals(2, pis.size());
    enrollmentParams.setImportStrategy(TrackerImportStrategy.DELETE);
    TrackerImportReport updatedReport = trackerImportService.importTracker(enrollmentParams);
    assertNoErrors(updatedReport);
    assertEquals(1, updatedReport.getStats().getDeleted());
    TrackedEntityInstance tei = manager.get(TrackedEntityInstance.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    trackerOwnershipManager.transferOwnership(tei, pgm, ou, true, false);
    enrollmentParams.setImportStrategy(TrackerImportStrategy.CREATE);
    enrollmentParams.getEnrollments().get(0).setEnrollment(CodeGenerator.generateUid());
    updatedReport = trackerImportService.importTracker(enrollmentParams);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, TrackerErrorCode.E1102);
  }

  @Test
  void testDeleteEnrollmentWithoutOwnership() throws IOException {
    // Change ownership
    TrackedEntityInstance tei = manager.get(TrackedEntityInstance.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    TrackerImportParams enrollmentParams =
        fromJson("tracker/ownership_enrollment.json", nonSuperUser.getUid());
    trackerOwnershipManager.transferOwnership(tei, pgm, ou, true, false);
    enrollmentParams.setImportStrategy(TrackerImportStrategy.DELETE);
    TrackerImportReport updatedReport = trackerImportService.importTracker(enrollmentParams);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, TrackerErrorCode.E1102);
  }

  @Test
  void testUpdateEnrollmentWithoutOwnership() throws IOException {
    // Change ownership
    TrackedEntityInstance tei = manager.get(TrackedEntityInstance.class, "IOR1AXXl24H");
    OrganisationUnit ou = manager.get(OrganisationUnit.class, "B1nCbRV3qtP");
    Program pgm = manager.get(Program.class, "BFcipDERJnf");
    trackerOwnershipManager.transferOwnership(tei, pgm, ou, true, false);
    TrackerImportParams enrollmentParams =
        fromJson("tracker/ownership_enrollment.json", nonSuperUser.getUid());
    enrollmentParams.setImportStrategy(TrackerImportStrategy.CREATE_AND_UPDATE);
    TrackerImportReport updatedReport = trackerImportService.importTracker(enrollmentParams);
    assertEquals(1, updatedReport.getStats().getIgnored());
    assertHasError(updatedReport, TrackerErrorCode.E1102);
  }

  private void compareEnrollmentBasicProperties(ProgramInstance pi, Enrollment enrollment) {
    assertEquals(DateUtils.fromInstant(enrollment.getEnrolledAt()), pi.getEnrollmentDate());
    assertEquals(DateUtils.fromInstant(enrollment.getOccurredAt()), pi.getIncidentDate());
    assertEquals(DateUtils.fromInstant(enrollment.getCreatedAtClient()), pi.getCreatedAtClient());
    assertEquals(
        DateUtils.fromInstant(enrollment.getUpdatedAtClient()), pi.getLastUpdatedAtClient());
    assertEquals(enrollment.getStatus().toString(), pi.getStatus().toString());
  }
}
