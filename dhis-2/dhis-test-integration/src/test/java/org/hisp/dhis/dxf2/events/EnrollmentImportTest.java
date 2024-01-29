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
package org.hisp.dhis.dxf2.events;

import static org.hisp.dhis.user.UserRole.AUTHORITY_ALL;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.events.enrollment.Enrollment;
import org.hisp.dhis.dxf2.events.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.events.event.Event;
import org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageInstance;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author <luca@dhis2.org>
 */
class EnrollmentImportTest extends TransactionalIntegrationTest {
  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityInstanceService trackedEntityInstanceService;

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private UserService _userService;

  private TrackedEntityInstance trackedEntityInstance;

  @Autowired private SessionFactory sessionFactory;

  private OrganisationUnit organisationUnitA;

  private Program program;

  private ProgramInstance programInstance;

  private User user;

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;

    organisationUnitA = createOrganisationUnit('A');
    manager.save(organisationUnitA);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);

    trackedEntityInstance = createTrackedEntityInstance(organisationUnitA);
    trackedEntityInstance.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntityInstance);

    program = createProgram('A', new HashSet<>(), organisationUnitA);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    manager.save(program);

    programInstance = new ProgramInstance();
    programInstance.setEnrollmentDate(new Date());
    programInstance.setIncidentDate(new Date());
    programInstance.setProgram(program);
    programInstance.setStatus(ProgramStatus.ACTIVE);
    programInstance.setStoredBy("test");
    programInstance.setName("test");
    programInstance.enrollTrackedEntityInstance(trackedEntityInstance, program);
    manager.save(programInstance);

    user = createAndAddAdminUser(AUTHORITY_ALL);
  }

  @Test
  void shouldSetCreatedByUserInfoWhenCreateEnrollments() {
    String enrollmentUid = CodeGenerator.generateUid();

    Enrollment enrollment =
        enrollment(organisationUnitA.getUid(), program.getUid(), trackedEntityInstance.getUid());
    enrollment.setEnrollment(enrollmentUid);

    ImportSummaries importSummaries =
        enrollmentService.addEnrollments(
            List.of(enrollment), new ImportOptions().setUser(user), null);

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummaries.getStatus()),
        () ->
            assertEquals(
                UserInfoSnapshot.from(user),
                enrollmentService
                    .getEnrollment(enrollmentUid, EnrollmentParams.FALSE)
                    .getCreatedByUserInfo()),
        () ->
            assertEquals(
                UserInfoSnapshot.from(user),
                enrollmentService
                    .getEnrollment(enrollmentUid, EnrollmentParams.FALSE)
                    .getLastUpdatedByUserInfo()));
  }

  @Test
  void shouldSetUpdatedByUserInfoWhenUpdateEnrollments() {
    Enrollment enrollment =
        enrollment(organisationUnitA.getUid(), program.getUid(), trackedEntityInstance.getUid());
    enrollment.setEnrollment(programInstance.getUid());

    ImportSummaries importSummaries =
        enrollmentService.updateEnrollments(
            List.of(enrollment), new ImportOptions().setUser(user), true);

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummaries.getStatus()),
        () ->
            assertEquals(
                UserInfoSnapshot.from(user),
                enrollmentService
                    .getEnrollment(programInstance.getUid(), EnrollmentParams.FALSE)
                    .getLastUpdatedByUserInfo()));
  }

  @Test
  void shouldUpdateLastUpdatedAndCascadeEventDeleteWhenDeleteEnrollments() {
    String entityLastUpdatedDateBefore =
        trackedEntityInstanceService
            .getTrackedEntityInstance(trackedEntityInstance.getUid())
            .getLastUpdated();

    org.hisp.dhis.program.ProgramInstance enrollmentBefore =
        getEntityJpql(
            org.hisp.dhis.program.ProgramInstance.class.getSimpleName(),
            this.programInstance.getUid());

    ProgramStage programStage = createProgramStage('x', program);
    manager.save(programStage);

    ProgramStageInstance eventBefore =
        createEvent(programStage, this.programInstance, organisationUnitA);
    manager.save(eventBefore);

    dbmsManager.clearSession();

    Enrollment enrollment = new Enrollment();
    Event event = new Event();
    event.setEvent(eventBefore.getUid());
    enrollment.setEnrollment(this.programInstance.getUid());

    User user = createAndAddUser("userDelete", organisationUnitA, "ALL");
    injectSecurityContext(user);

    ImportSummaries importSummaries =
        enrollmentService.deleteEnrollments(
            List.of(enrollment), new ImportOptions().setUser(user), true);
    assertEquals(ImportStatus.SUCCESS, importSummaries.getStatus());

    dbmsManager.clearSession();

    org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance entityLastUpdatedAfter =
        trackedEntityInstanceService.getTrackedEntityInstance(trackedEntityInstance.getUid());

    org.hisp.dhis.program.ProgramInstance enrollmentAfter =
        getEntityJpql(
            org.hisp.dhis.program.ProgramInstance.class.getSimpleName(),
            this.programInstance.getUid());

    org.hisp.dhis.program.ProgramStageInstance eventAfter =
        getEntityJpql(
            org.hisp.dhis.program.ProgramStageInstance.class.getSimpleName(), eventBefore.getUid());

    assertTrue(enrollmentAfter.isDeleted());
    assertTrue(
        enrollmentAfter.getLastUpdated().getTime() > enrollmentBefore.getLastUpdated().getTime());
    assertTrue(entityLastUpdatedAfter.getLastUpdated().compareTo(entityLastUpdatedDateBefore) > 0);
    assertEquals(
        entityLastUpdatedAfter.getLastUpdatedByUserInfo().getUid(),
        UserInfoSnapshot.from(user).getUid());
    assertEquals(
        enrollmentAfter.getLastUpdatedByUserInfo().getUid(), UserInfoSnapshot.from(user).getUid());
    assertTrue(eventAfter.isDeleted());
    assertTrue(eventAfter.getLastUpdated().getTime() > eventBefore.getLastUpdated().getTime());
  }

  /** Get with the current session because some Store exclude deleted */
  @SuppressWarnings("unchecked")
  public <T extends SoftDeletableObject> T getEntityJpql(String entity, String uid) {

    return (T)
        sessionFactory
            .getCurrentSession()
            .createQuery("SELECT e FROM " + entity + " e WHERE e.uid = :uid")
            .setParameter("uid", uid)
            .getSingleResult();
  }

  private Enrollment enrollment(String orgUnit, String program, String trackedEntity) {
    Enrollment enrollment = new Enrollment();

    enrollment.setOrgUnit(orgUnit);
    enrollment.setProgram(program);
    enrollment.setTrackedEntityInstance(trackedEntity);
    enrollment.setEnrollmentDate(new Date());
    enrollment.setIncidentDate(new Date());
    return enrollment;
  }

  public static ProgramStageInstance createEvent(
      ProgramStage programStage, ProgramInstance enrollment, OrganisationUnit organisationUnit) {
    ProgramStageInstance event = new ProgramStageInstance();
    event.setAutoFields();

    event.setProgramStage(programStage);
    event.setProgramInstance(enrollment);
    event.setOrganisationUnit(organisationUnit);

    return event;
  }
}
