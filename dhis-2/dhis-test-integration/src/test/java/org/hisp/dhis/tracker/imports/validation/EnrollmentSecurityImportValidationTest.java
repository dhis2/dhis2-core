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
import static org.hisp.dhis.tracker.imports.TrackerImportStrategy.CREATE;
import static org.hisp.dhis.tracker.imports.validation.Users.USER_2;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageDataElement;
import org.hisp.dhis.program.ProgramStageDataElementService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.TestSetup;
import org.hisp.dhis.tracker.imports.TrackerImportParams;
import org.hisp.dhis.tracker.imports.TrackerImportService;
import org.hisp.dhis.tracker.imports.domain.Enrollment;
import org.hisp.dhis.tracker.imports.domain.MetadataIdentifier;
import org.hisp.dhis.tracker.imports.domain.TrackerObjects;
import org.hisp.dhis.tracker.imports.report.ImportReport;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentSecurityImportValidationTest extends PostgresIntegrationTestBase {
  @Autowired private TestSetup testSetup;

  @Autowired private TrackerImportService trackerImportService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private ProgramStageDataElementService programStageDataElementService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  private TrackedEntity maleA;

  private TrackedEntity maleB;

  private TrackedEntity femaleA;

  private TrackedEntity femaleB;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Program programA;

  private DataElement dataElementA;

  private DataElement dataElementB;

  private ProgramStage programStageA;

  private ProgramStage programStageB;

  private TrackedEntityType trackedEntityType;

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
  }

  private void setup() {
    injectSecurityContextUser(importUser);
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitB = createOrganisationUnit('B');
    manager.save(organisationUnitA);
    manager.save(organisationUnitB);
    dataElementA = createDataElement('A');
    dataElementB = createDataElement('B');
    dataElementA.setValueType(ValueType.INTEGER);
    dataElementB.setValueType(ValueType.INTEGER);
    manager.save(dataElementA);
    manager.save(dataElementB);
    programStageA = createProgramStage('A', 0);
    programStageB = createProgramStage('B', 0);
    programStageB.setRepeatable(true);
    programA = createProgram('A', new HashSet<>(), organisationUnitA);
    programA.setProgramType(ProgramType.WITH_REGISTRATION);
    trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    TrackedEntityType trackedEntityTypeFromProgram = createTrackedEntityType('C');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityTypeFromProgram);
    manager.save(programA);
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.setTrackedEntityType(trackedEntityType);
    manager.updateNoAcl(programA);
    ProgramStageDataElement programStageDataElement = new ProgramStageDataElement();
    programStageDataElement.setDataElement(dataElementA);
    programStageDataElement.setProgramStage(programStageA);
    programStageDataElementService.addProgramStageDataElement(programStageDataElement);
    programStageA.getProgramStageDataElements().add(programStageDataElement);
    programStageA.setProgram(programA);
    programStageDataElement = new ProgramStageDataElement();
    programStageDataElement.setDataElement(dataElementB);
    programStageDataElement.setProgramStage(programStageB);
    programStageDataElementService.addProgramStageDataElement(programStageDataElement);
    programStageB.getProgramStageDataElements().add(programStageDataElement);
    programStageB.setProgram(programA);
    programStageB.setMinDaysFromStart(2);
    programA.getProgramStages().add(programStageA);
    programA.getProgramStages().add(programStageB);
    manager.save(programStageA);
    manager.save(programStageB);
    manager.update(programA);
    maleA = createTrackedEntity('A', organisationUnitA, trackedEntityType);
    maleB = createTrackedEntity(organisationUnitB, trackedEntityType);
    femaleA = createTrackedEntity(organisationUnitA, trackedEntityType);
    femaleB = createTrackedEntity(organisationUnitB, trackedEntityType);

    manager.save(maleA);
    manager.save(maleB);
    manager.save(femaleA);
    manager.save(femaleB);
    manager.flush();
  }

  @Test
  void testNoWriteAccessToOrg() throws IOException {
    User user = userService.getUser(USER_2);
    injectSecurityContextUser(user);
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(CREATE);

    ImportReport importReport =
        trackerImportService.importTracker(
            params, testSetup.fromJson("tracker/validations/enrollments_te_enrollments-data.json"));

    assertHasErrors(importReport, 4, ValidationCode.E1000);
  }

  @Test
  void shouldFailWhenUserHasNoAccessToTrackedEntityType() throws IOException {
    clearSecurityContext();

    setup();
    programA.setPublicAccess(AccessStringHelper.FULL);
    TrackedEntityType bPJ0FMtcnEh = trackedEntityTypeService.getTrackedEntityType("bPJ0FMtcnEh");
    programA.setTrackedEntityType(bPJ0FMtcnEh);
    manager.updateNoAcl(programA);
    OrganisationUnit orgUnit = manager.get(OrganisationUnit.class, "QfUVllTs6cZ");
    User user =
        createUserWithAuth("user1")
            .setOrganisationUnits(Sets.newHashSet(orgUnit, organisationUnitA));
    userService.addUser(user);
    injectSecurityContextUser(user);
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_no-access-te.json");
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(CREATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1104);
  }

  @Test
  void testUserNoWriteAccessToProgram() throws IOException {
    clearSecurityContext();

    setup();
    programA.setPublicAccess(
        AccessStringHelper.newInstance()
            .enable(AccessStringHelper.Permission.DATA_READ)
            .enable(AccessStringHelper.Permission.READ)
            .build());
    trackedEntityType.setPublicAccess(AccessStringHelper.FULL);
    programA.setTrackedEntityType(trackedEntityType);
    manager.updateNoAcl(programA);
    User user =
        createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(organisationUnitA));
    userService.addUser(user);
    injectSecurityContextUser(user);
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_no-access-program.json");
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(CREATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1091);
  }

  @Test
  void testUserHasWriteAccessToProgram() throws IOException {
    clearSecurityContext();

    setup();
    programA.setPublicAccess(AccessStringHelper.FULL);
    trackedEntityType.setPublicAccess(AccessStringHelper.DATA_READ);
    programA.setTrackedEntityType(trackedEntityType);
    manager.updateNoAcl(programA);
    User user =
        createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(organisationUnitA));
    userService.addUser(user);
    injectSecurityContextUser(user);
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_no-access-program.json");
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(CREATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertNoErrors(importReport);
  }

  @Test
  void testUserHasNoAccessToProgramTeType() throws IOException {
    clearSecurityContext();

    setup();
    programA.setPublicAccess(AccessStringHelper.FULL);
    programA.setTrackedEntityType(trackedEntityType);
    manager.updateNoAcl(programA);
    manager.flush();
    User user =
        createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(organisationUnitA));
    injectSecurityContextUser(user);
    TrackerObjects trackerObjects =
        testSetup.fromJson("tracker/validations/enrollments_program-tetype-missmatch.json");
    TrackerImportParams params = new TrackerImportParams();
    params.setImportStrategy(CREATE);

    ImportReport importReport = trackerImportService.importTracker(params, trackerObjects);

    assertHasOnlyErrors(importReport, ValidationCode.E1104);
  }

  @Test
  void shouldFailWhenTeNotEnrolledAndUserHasNoAccessToTeRegisteringOrgUnit() {
    clearSecurityContext();
    setup();
    TrackedEntity trackedEntityB = createTrackedEntity(trackedEntityType, organisationUnitB);
    User userA = createUser(organisationUnitA);
    TrackerImportParams params = TrackerImportParams.builder().importStrategy(CREATE).build();

    injectSecurityContextUser(userA);
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            TrackerObjects.builder()
                .enrollments(createEnrollment(trackedEntityB, organisationUnitA, programA))
                .build());

    assertHasOnlyErrors(importReport, ValidationCode.E1102);
  }

  @Test
  void shouldEnrollTeWhenTeNotEnrolledButUserHasAccessToTeRegisteringOrgUnit() {
    clearSecurityContext();
    setup();
    Program programB = createProgram(organisationUnitB);
    TrackedEntity trackedEntityB = createTrackedEntity(trackedEntityType, organisationUnitB);
    User userB = createUser(organisationUnitB);
    TrackerImportParams params = TrackerImportParams.builder().importStrategy(CREATE).build();

    injectSecurityContextUser(userB);
    ImportReport importReport =
        trackerImportService.importTracker(
            params,
            TrackerObjects.builder()
                .enrollments(createEnrollment(trackedEntityB, organisationUnitB, programB))
                .build());

    assertNoErrors(importReport);
  }

  private User createUser(OrganisationUnit orgUnit) {
    User user = createUserWithAuth("user1").setOrganisationUnits(Sets.newHashSet(orgUnit));
    userService.addUser(user);

    trackedEntityType
        .getSharing()
        .setUsers(Map.of(user.getUid(), new UserAccess(AccessStringHelper.FULL, user.getUid())));
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);

    return user;
  }

  private Program createProgram(OrganisationUnit orgUnit) {
    Program program = createProgram('B', new HashSet<>(), orgUnit);
    program.setProgramType(ProgramType.WITH_REGISTRATION);
    program.setTrackedEntityType(trackedEntityType);
    manager.save(program);
    program.setSharing(Sharing.builder().publicAccess(AccessStringHelper.FULL).build());
    manager.update(program);

    return program;
  }

  private TrackedEntity createTrackedEntity(
      TrackedEntityType trackedEntityType, OrganisationUnit orgUnit) {
    TrackedEntity trackedEntity = createTrackedEntity('T', orgUnit, trackedEntityType);
    manager.save(trackedEntity);

    return trackedEntity;
  }

  private List<Enrollment> createEnrollment(
      TrackedEntity trackedEntity, OrganisationUnit orgUnit, Program program) {
    return List.of(
        Enrollment.builder()
            .program(MetadataIdentifier.ofUid(program.getUid()))
            .orgUnit(MetadataIdentifier.ofUid(orgUnit.getUid()))
            .trackedEntity(UID.of(trackedEntity))
            .status(EnrollmentStatus.ACTIVE)
            .enrolledAt(Instant.now())
            .occurredAt(Instant.now())
            .enrollment(UID.generate())
            .build());
  }
}
