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
package org.hisp.dhis.trackedentity;

import static org.hisp.dhis.common.AccessLevel.AUDITED;
import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.security.acl.AccessStringHelper.DEFAULT;
import static org.hisp.dhis.security.acl.AccessStringHelper.FULL;
import static org.hisp.dhis.user.UserRole.AUTHORITY_ALL;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.dxf2.events.EnrollmentEventsParams;
import org.hisp.dhis.dxf2.events.EnrollmentParams;
import org.hisp.dhis.dxf2.events.EventParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceEnrollmentParams;
import org.hisp.dhis.dxf2.events.TrackedEntityInstanceParams;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramInstance;
import org.hisp.dhis.program.ProgramInstanceService;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramType;
import org.hisp.dhis.test.integration.IntegrationTestBase;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
class TrackerOwnershipManagerTest extends IntegrationTestBase {

  @Autowired private TrackerOwnershipManager trackerOwnershipAccessManager;

  @Autowired private UserService _userService;

  @Autowired private TrackedEntityInstanceService entityInstanceService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired
  private org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstanceService
      trackedEntityInstanceService;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private ProgramInstanceService programInstanceService;

  private TrackedEntityInstance entityInstanceA1;

  private TrackedEntityInstance entityInstanceB1;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Program programA;

  private Program programB;

  private User userA;

  private User userB;

  private User superUser;

  @Override
  protected void setUpTest() throws Exception {
    userService = _userService;
    preCreateInjectAdminUser();

    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);

    userA = createUserWithAuth("userA");
    userA.addOrganisationUnit(organisationUnitA);
    userService.updateUser(userA);
    userB = createUserWithAuth("userB");
    userB.addOrganisationUnit(organisationUnitB);
    userService.updateUser(userB);
    superUser = createAndAddAdminUser(AUTHORITY_ALL);
    superUser.setOrganisationUnits(Set.of(organisationUnitA));
    userService.updateUser(superUser);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    Sharing sharing = new Sharing();
    sharing.setPublicAccess(FULL);
    sharing.setUserAccesses(Set.of(new UserAccess(userB, FULL)));
    trackedEntityType.setSharing(sharing);
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);

    entityInstanceA1 = createTrackedEntityInstance(organisationUnitA);
    entityInstanceA1.setTrackedEntityType(trackedEntityType);
    entityInstanceB1 = createTrackedEntityInstance(organisationUnitB);
    entityInstanceB1.setTrackedEntityType(trackedEntityType);
    entityInstanceService.addTrackedEntityInstance(entityInstanceA1);
    entityInstanceService.addTrackedEntityInstance(entityInstanceB1);

    programA = createProgram('A');
    programA.setAccessLevel(AccessLevel.PROTECTED);
    programA.setTrackedEntityType(trackedEntityType);
    programA.setOrganisationUnits(Set.of(organisationUnitA, organisationUnitB));
    programService.addProgram(programA);
    UserAccess userAccess = new UserAccess(userA.getUid(), FULL);
    programA.setSharing(new Sharing(FULL, userAccess));
    programService.updateProgram(programA);
    programB = createProgram('B');
    programB.setAccessLevel(CLOSED);
    programB.setTrackedEntityType(trackedEntityType);
    programService.addProgram(programB);
    programB.setSharing(new Sharing(DEFAULT, userAccess));
    programService.updateProgram(programB);

    ProgramInstance programInstanceA =
        new ProgramInstance(programA, entityInstanceA1, organisationUnitA);
    programInstanceA.setEnrollmentDate(Date.from(Instant.now()));
    programInstanceService.addProgramInstance(programInstanceA);
    ProgramInstance programInstanceB =
        new ProgramInstance(programB, entityInstanceB1, organisationUnitB);
    programInstanceB.setEnrollmentDate(Date.from(Instant.now()));
    programInstanceService.addProgramInstance(programInstanceB);
  }

  @Test
  void shouldFailWhenGrantingTemporaryOwnershipAndUserNotInSearchScope() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertFalse(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipAccessManager.grantTemporaryOwnership(
                    entityInstanceA1, programA, userB, "testing reason"));

    assertEquals(
        "The owner of the entity-program combination is not in the user's search scope.",
        exception.getMessage());
  }

  @Test
  void testTransferOwnership() throws ForbiddenException {
    trackerOwnershipAccessManager.assignOwnership(
        entityInstanceA1, programA, organisationUnitA, false, true);
    assertTrue(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertFalse(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
    trackerOwnershipAccessManager.transferOwnership(
        entityInstanceA1, programA, organisationUnitB, false, true);
    assertFalse(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndUserInCaptureScope() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userA, entityInstanceA1, programA));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userA, entityInstanceA1.getUid(), entityInstanceA1.getOrganisationUnit(), programA));
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndHasTemporaryAccess() throws ForbiddenException {
    userB.setTeiSearchOrganisationUnits(Set.of(organisationUnitA));
    userService.updateUser(userB);

    trackerOwnershipAccessManager.grantTemporaryOwnership(
        entityInstanceA1, programA, userB, "test protected program");

    assertTrue(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userB, entityInstanceA1.getUid(), entityInstanceA1.getOrganisationUnit(), programA));
  }

  @Test
  void shouldNotHaveAccessWhenProgramProtectedAndUserNotInCaptureScopeNorHasTemporaryAccess() {
    assertFalse(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceA1, programA));
    assertFalse(
        trackerOwnershipAccessManager.hasAccess(
            userB, entityInstanceA1.getUid(), entityInstanceA1.getOrganisationUnit(), programA));
  }

  @Test
  void shouldHaveAccessWhenProgramClosedAndUserInCaptureScope() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userB, entityInstanceB1, programB));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userB, entityInstanceB1.getUid(), entityInstanceB1.getOrganisationUnit(), programB));
  }

  private static Stream<Program> providePrograms() {
    return Stream.of(createProgram(OPEN), createProgram(AUDITED), createProgram(CLOSED));
  }

  @ParameterizedTest
  @MethodSource("providePrograms")
  void shouldFailWhenGrantingTemporaryOwnershipToProgramWithAccessLevelOtherThanProtected(
      Program program) {
    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipAccessManager.grantTemporaryOwnership(
                    entityInstanceA1, program, userB, "test temporary ownership"));

    assertContains(
        "Temporary ownership can only be granted to protected programs.", exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfUserIsSuperuser() {
    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipAccessManager.grantTemporaryOwnership(
                    entityInstanceA1, programA, superUser, "test temporary ownership"));

    assertEquals(
        "Temporary ownership not created. Current user is a superuser.", exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfProgramIsNull() {
    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipAccessManager.grantTemporaryOwnership(
                    entityInstanceA1, null, userB, "test temporary ownership"));

    assertEquals(
        "Temporary ownership not created. Program supplied does not exist.",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfProgramIsNotTrackerProgram() {
    Program eventProgram = createProgram(AccessLevel.PROTECTED);
    eventProgram.setProgramType(ProgramType.WITHOUT_REGISTRATION);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipAccessManager.grantTemporaryOwnership(
                    entityInstanceA1, eventProgram, userB, "test temporary ownership"));

    assertEquals(
        "Temporary ownership not created. Program supplied is not a tracker program.",
        exception.getMessage());
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToAccessibleOrgUnit() throws ForbiddenException {
    transferOwnership(entityInstanceA1, programA, organisationUnitB);
    injectSecurityContext(userB);
    TrackedEntityInstanceQueryParams params =
        createOperationParams(userB, null, entityInstanceA1.getTrackedEntityType());

    List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> trackedEntities =
        trackedEntityInstanceService.getTrackedEntityInstances(
            params, createInstanceParams(), false, false);

    assertContainsOnly(
        List.of(entityInstanceA1.getUid(), entityInstanceB1.getUid()),
        trackedEntities.stream()
            .map(
                org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance
                    ::getTrackedEntityInstance)
            .collect(Collectors.toList()));
  }

  @Test
  void shouldNotFindTrackedEntityWhenTransferredToInaccessibleOrgUnit() throws ForbiddenException {
    transferOwnership(entityInstanceA1, programA, organisationUnitB);

    injectSecurityContext(userA);
    TrackedEntityInstanceQueryParams params =
        createOperationParams(userA, null, entityInstanceA1.getTrackedEntityType());

    assertIsEmpty(
        trackedEntityInstanceService.getTrackedEntityInstances(
            params, createInstanceParams(), false, false));
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToInaccessibleOrgUnitIfSuperUser()
      throws ForbiddenException {
    transferOwnership(entityInstanceA1, programA, organisationUnitB);
    TrackedEntityInstanceQueryParams params =
        createOperationParams(superUser, null, entityInstanceA1.getTrackedEntityType());

    injectSecurityContext(superUser);
    List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> trackedEntities =
        trackedEntityInstanceService.getTrackedEntityInstances(
            params, createInstanceParams(), false, false);

    assertContainsOnly(
        List.of(entityInstanceA1.getUid()),
        trackedEntities.stream()
            .map(
                org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance
                    ::getTrackedEntityInstance)
            .collect(Collectors.toList()));
  }

  @Test
  void shouldNotTransferOwnershipWhenOrgUnitNotAssociatedToProgram() {
    OrganisationUnit notAssociatedOrgUnit = createOrganisationUnit('C');
    organisationUnitService.addOrganisationUnit(notAssociatedOrgUnit);
    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () -> transferOwnership(entityInstanceA1, programA, notAssociatedOrgUnit));
    assertEquals(
        String.format(
            "The program %s is not associated to the org unit %s",
            programA.getUid(), notAssociatedOrgUnit.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFindTrackedEntityWhenProgramSuppliedAndUserIsOwner() {
    assignOwnership(entityInstanceA1, programA, organisationUnitA);
    TrackedEntityInstanceQueryParams params = createOperationParams(userA, programA, null);

    injectSecurityContext(userA);

    List<org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance> trackedEntities =
        trackedEntityInstanceService.getTrackedEntityInstances(
            params, createInstanceParams(), false, false);

    assertContainsOnly(
        List.of(entityInstanceA1.getUid()),
        trackedEntities.stream()
            .map(
                org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance
                    ::getTrackedEntityInstance)
            .collect(Collectors.toList()));
  }

  @Test
  void shouldNotFindTrackedEntityWhenProgramSuppliedAndUserIsNotOwner() {
    assignOwnership(entityInstanceA1, programA, organisationUnitA);
    TrackedEntityInstanceQueryParams params = createOperationParams(userB, programA, null);

    injectSecurityContext(userB);

    assertIsEmpty(
        trackedEntityInstanceService.getTrackedEntityInstances(
            params, createInstanceParams(), false, false));
  }

  @Test
  void shouldHaveAccessToEnrollmentWithSuperUserWhenTransferredToOwnOrgUnit()
      throws ForbiddenException {
    TrackedEntityInstanceQueryParams params = createOperationParams(superUser, programA, null);
    trackerOwnershipAccessManager.assignOwnership(
        entityInstanceA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(
        entityInstanceA1, programA, organisationUnitB, false, true);
    superUser.setOrganisationUnits(Set.of(organisationUnitB));
    userService.updateUser(superUser);
    injectSecurityContext(superUser);

    assertEquals(
        List.of(entityInstanceA1.getUid()),
        trackedEntityInstanceService
            .getTrackedEntityInstances(params, createInstanceParams(), false, false)
            .stream()
            .map(
                org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance
                    ::getTrackedEntityInstance)
            .collect(Collectors.toList()));
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToAccessibleOrgUnitAndSuperUser()
      throws ForbiddenException {
    TrackedEntityInstanceQueryParams params =
        createOperationParams(superUser, null, entityInstanceA1.getTrackedEntityType());
    trackerOwnershipAccessManager.assignOwnership(
        entityInstanceA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(
        entityInstanceA1, programA, organisationUnitB, false, true);
    superUser.setOrganisationUnits(Set.of(organisationUnitB));
    userService.updateUser(superUser);
    injectSecurityContext(superUser);

    assertContainsOnly(
        List.of(entityInstanceA1.getUid(), entityInstanceB1.getUid()),
        trackedEntityInstanceService
            .getTrackedEntityInstances(params, createInstanceParams(), false, false)
            .stream()
            .map(
                org.hisp.dhis.dxf2.events.trackedentity.TrackedEntityInstance
                    ::getTrackedEntityInstance)
            .collect(Collectors.toList()));
  }

  private void transferOwnership(
      TrackedEntityInstance trackedEntity, Program program, OrganisationUnit orgUnit)
      throws ForbiddenException {
    trackerOwnershipAccessManager.transferOwnership(trackedEntity, program, orgUnit, false, true);
  }

  private void assignOwnership(
      TrackedEntityInstance trackedEntity, Program program, OrganisationUnit orgUnit) {
    trackerOwnershipAccessManager.assignOwnership(trackedEntity, program, orgUnit, false, true);
  }

  private TrackedEntityInstanceQueryParams createOperationParams(
      User user, Program program, TrackedEntityType trackedEntityType) {
    TrackedEntityInstanceQueryParams params = new TrackedEntityInstanceQueryParams();
    params.setTrackedEntityType(trackedEntityType);
    params.setOrganisationUnitMode(ACCESSIBLE);
    params.setProgram(program);
    params.setUser(user);

    return params;
  }

  private TrackedEntityInstanceParams createInstanceParams() {
    EventParams eventParams = new EventParams(false);
    EnrollmentEventsParams enrollmentEventsParams = new EnrollmentEventsParams(false, eventParams);
    EnrollmentParams enrollmentParams =
        new EnrollmentParams(enrollmentEventsParams, false, false, false, false);

    return new TrackedEntityInstanceParams(
        false,
        new TrackedEntityInstanceEnrollmentParams(false, enrollmentParams),
        false,
        false,
        false,
        false);
  }

  private static Program createProgram(AccessLevel accessLevel) {
    Program program = new Program();
    program.setAccessLevel(accessLevel);

    return program;
  }
}
