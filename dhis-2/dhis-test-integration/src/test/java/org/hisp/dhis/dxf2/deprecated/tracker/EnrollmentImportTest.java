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
package org.hisp.dhis.dxf2.deprecated.tracker;

import static org.hisp.dhis.user.UserRole.AUTHORITY_ALL;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import javax.persistence.EntityManager;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.SoftDeletableObject;
import org.hisp.dhis.dxf2.common.ImportOptions;
import org.hisp.dhis.dxf2.deprecated.tracker.enrollment.EnrollmentService;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstance;
import org.hisp.dhis.dxf2.deprecated.tracker.trackedentity.TrackedEntityInstanceService;
import org.hisp.dhis.dxf2.importsummary.ImportStatus;
import org.hisp.dhis.dxf2.importsummary.ImportSummaries;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Event;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.program.UserInfoSnapshot;
import org.hisp.dhis.test.integration.TransactionalIntegrationTest;
import org.hisp.dhis.trackedentity.TrackedEntity;
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

  @Autowired private EntityManager entityManager;

  private TrackedEntity trackedEntity;

  private OrganisationUnit organisationUnitA;

  private Program program;

  private Enrollment enrollment;

  private User user;

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;

    organisationUnitA = createOrganisationUnit('A');
    manager.save(organisationUnitA);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);

    trackedEntity = createTrackedEntity(organisationUnitA);
    trackedEntity.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntity);

    program = createProgram('A', new HashSet<>(), organisationUnitA);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    manager.save(program);

    enrollment = new Enrollment();
    enrollment.setEnrollmentDate(new Date());
    enrollment.setOccurredDate(new Date());
    enrollment.setProgram(program);
    enrollment.setStatus(ProgramStatus.ACTIVE);
    enrollment.setStoredBy("test");
    enrollment.setName("test");
    enrollment.enrollTrackedEntity(trackedEntity, program);
    manager.save(enrollment);

    user = createAndAddAdminUser(AUTHORITY_ALL);
  }

  @Test
  void shouldSetCreatedByUserInfoWhenCreateEnrollments() {
    String enrollmentUid = CodeGenerator.generateUid();

    org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment =
        enrollment(organisationUnitA.getUid(), program.getUid(), trackedEntity.getUid());
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
    org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment =
        enrollment(organisationUnitA.getUid(), program.getUid(), trackedEntity.getUid());
    enrollment.setEnrollment(this.enrollment.getUid());

    ImportSummaries importSummaries =
        enrollmentService.updateEnrollments(
            List.of(enrollment), new ImportOptions().setUser(user), true);

    assertAll(
        () -> assertEquals(ImportStatus.SUCCESS, importSummaries.getStatus()),
        () ->
            assertEquals(
                UserInfoSnapshot.from(user),
                enrollmentService
                    .getEnrollment(this.enrollment.getUid(), EnrollmentParams.FALSE)
                    .getLastUpdatedByUserInfo()));
  }

  @Test
  void shouldUpdateLastUpdatedAndCascadeEventDeleteWhenDeleteEnrollments() {
    String entityLastUpdatedDateBefore =
        trackedEntityInstanceService
            .getTrackedEntityInstance(trackedEntity.getUid())
            .getLastUpdated();

    Enrollment enrollmentBefore =
        getEntityJpql(Enrollment.class.getSimpleName(), this.enrollment.getUid());

    ProgramStage programStage = createProgramStage('x', program);
    manager.save(programStage);

    Event eventBefore = createEvent(programStage, this.enrollment, organisationUnitA);
    manager.save(eventBefore);

    dbmsManager.clearSession();

    org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment =
        new org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment();
    org.hisp.dhis.dxf2.deprecated.tracker.event.Event event =
        new org.hisp.dhis.dxf2.deprecated.tracker.event.Event();
    event.setEvent(eventBefore.getUid());
    enrollment.setEnrollment(this.enrollment.getUid());

    User user = createAndAddUser("userDelete", organisationUnitA, "ALL");
    injectSecurityContextUser(user);

    ImportSummaries importSummaries =
        enrollmentService.deleteEnrollments(
            List.of(enrollment), new ImportOptions().setUser(user), true);
    assertEquals(ImportStatus.SUCCESS, importSummaries.getStatus());

    dbmsManager.clearSession();

    TrackedEntityInstance entityLastUpdatedAfter =
        trackedEntityInstanceService.getTrackedEntityInstance(trackedEntity.getUid());

    Enrollment enrollmentAfter =
        getEntityJpql(Enrollment.class.getSimpleName(), this.enrollment.getUid());

    Event eventAfter = getEntityJpql(Event.class.getSimpleName(), eventBefore.getUid());

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

  /** Get with the entity manager because some Store exclude deleted */
  @SuppressWarnings("unchecked")
  public <T extends SoftDeletableObject> T getEntityJpql(String entity, String uid) {

    return (T)
        entityManager
            .createQuery("SELECT e FROM " + entity + " e WHERE e.uid = :uid")
            .setParameter("uid", uid)
            .getSingleResult();
  }

  private org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment(
      String orgUnit, String program, String trackedEntity) {
    org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment enrollment =
        new org.hisp.dhis.dxf2.deprecated.tracker.enrollment.Enrollment();
    enrollment.setOrgUnit(orgUnit);
    enrollment.setProgram(program);
    enrollment.setTrackedEntityInstance(trackedEntity);
    enrollment.setEnrollmentDate(new Date());
    enrollment.setIncidentDate(new Date());
    return enrollment;
  }
}
