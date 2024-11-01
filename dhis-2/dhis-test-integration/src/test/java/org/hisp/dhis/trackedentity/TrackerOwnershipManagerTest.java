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

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.tracker.TrackerTestUtils.uids;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Enrollment;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityEnrollmentParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ameen Mohamed <ameen@dhis2.org>
 */
class TrackerOwnershipManagerTest extends PostgresIntegrationTestBase {

  @Autowired private TrackerOwnershipManager trackerOwnershipAccessManager;

  @Autowired
  private org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService trackedEntityService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  private TrackedEntity trackedEntityA1;

  private TrackedEntity trackedEntityB1;

  private OrganisationUnit organisationUnitA;

  private OrganisationUnit organisationUnitB;

  private Program programA;

  private Program programB;

  private User userA;
  private User userB;
  private User superUser;

  private UserDetails userDetailsA;
  private UserDetails userDetailsB;

  private TrackedEntityParams defaultParams;

  @BeforeEach
  void setUp() {
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);

    TrackedEntityType trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    trackedEntityType.setSharing(Sharing.builder().publicAccess(AccessStringHelper.FULL).build());
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);

    trackedEntityA1 = createTrackedEntity(organisationUnitA);
    trackedEntityA1.setTrackedEntityType(trackedEntityType);
    trackedEntityB1 = createTrackedEntity(organisationUnitB);
    trackedEntityB1.setTrackedEntityType(trackedEntityType);
    manager.save(trackedEntityA1);
    manager.save(trackedEntityB1);

    userA = createUserWithAuth("userA");
    userA.addOrganisationUnit(organisationUnitA);
    userService.updateUser(userA);
    userB = createUserWithAuth("userB");
    userB.addOrganisationUnit(organisationUnitB);
    userService.updateUser(userB);
    superUser =
        createAndAddUserWithAuth("trackertestownership", organisationUnitA, Authorities.ALL);

    programA = createProgram('A');
    programA.setAccessLevel(AccessLevel.PROTECTED);
    programA.setTrackedEntityType(trackedEntityType);
    programA.setOrganisationUnits(Set.of(organisationUnitA, organisationUnitB));
    programService.addProgram(programA);
    programA.setSharing(Sharing.builder().publicAccess(AccessStringHelper.FULL).build());
    programService.updateProgram(programA);
    programB = createProgram('B');
    programB.setAccessLevel(AccessLevel.CLOSED);
    programB.setTrackedEntityType(trackedEntityType);
    programService.addProgram(programB);
    programB.setSharing(
        Sharing.builder()
            .publicAccess(AccessStringHelper.DEFAULT)
            .users(Map.of(userB.getUid(), new UserAccess(userB, "r-r-----")))
            .build());
    programService.updateProgram(programB);

    userDetailsA = UserDetails.fromUser(userA);
    userDetailsB = UserDetails.fromUser(userB);

    Enrollment trackedEntityA1Enrollment =
        createEnrollment(programA, trackedEntityA1, organisationUnitA);
    manager.save(trackedEntityA1Enrollment);

    defaultParams =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.FALSE, false, false);
  }

  @Test
  void testAssignOwnership() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsA, trackedEntityA1, programA));
    assertFalse(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityB1, programA));
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitB, false, true);
    assertFalse(trackerOwnershipAccessManager.hasAccess(userDetailsA, trackedEntityA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityA1, programA));
  }

  @Test
  void testGrantTemporaryOwnershipWithAudit() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsA, trackedEntityA1, programA));
    assertFalse(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityA1, programA));
    trackerOwnershipAccessManager.grantTemporaryOwnership(
        trackedEntityA1, programA, userDetailsB, "testing reason");
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsA, trackedEntityA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsA, trackedEntityA1, programA));
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityA1, programA));
  }

  @Test
  void shouldNotHaveAccessToEnrollmentWithUserAWhenTransferredToAnotherOrgUnit()
      throws ForbiddenException {
    userA.setTeiSearchOrganisationUnits(Set.of(organisationUnitB));
    userService.updateUser(userA);
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(trackedEntityA1, programA, organisationUnitB);

    injectSecurityContextUser(userA);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), UID.of(programA), defaultParams));
    assertEquals("OWNERSHIP_ACCESS_DENIED", exception.getMessage());
  }

  @Test
  void shouldHaveAccessToEnrollmentWithUserBWhenTransferredToOwnOrgUnit()
      throws ForbiddenException, NotFoundException, BadRequestException {
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(trackedEntityA1, programA, organisationUnitB);

    injectSecurityContextUser(userB);
    assertEquals(
        trackedEntityA1,
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA1), UID.of(programA), defaultParams));
  }

  @Test
  void shouldHaveAccessToEnrollmentWithSuperUserWhenTransferredToOwnOrgUnit()
      throws ForbiddenException, NotFoundException, BadRequestException {
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(trackedEntityA1, programA, organisationUnitB);
    superUser.setOrganisationUnits(Set.of(organisationUnitB));
    userService.updateUser(superUser);

    injectSecurityContextUser(superUser);
    assertEquals(
        trackedEntityA1,
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA1), UID.of(programA), defaultParams));
  }

  @Test
  void shouldHaveAccessToTEWhenProgramNotProvidedButUserHasAccessToAtLeastOneProgram()
      throws ForbiddenException, NotFoundException, BadRequestException {
    injectSecurityContextUser(userA);

    assertEquals(
        trackedEntityA1,
        trackedEntityService.getTrackedEntity(UID.of(trackedEntityA1), null, defaultParams));
  }

  @Test
  void shouldNotHaveAccessToTEWhenProgramNotProvidedAndUserHasNoAccessToAnyProgram() {
    injectSecurityContextUser(userA);
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitB, false, true);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), null, defaultParams));

    assertEquals(
        String.format("User has no access to TrackedEntity:%s", trackedEntityA1.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndUserInCaptureScope() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsA, trackedEntityA1, programA));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userDetailsA,
            trackedEntityA1.getUid(),
            trackedEntityA1.getOrganisationUnit(),
            programA));
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndHasTemporaryAccess() {
    trackerOwnershipAccessManager.grantTemporaryOwnership(
        trackedEntityA1, programA, userDetailsB, "test protected program");
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityA1, programA));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userDetailsB,
            trackedEntityA1.getUid(),
            trackedEntityA1.getOrganisationUnit(),
            programA));
  }

  @Test
  void shouldNotHaveAccessWhenProgramProtectedAndUserNotInSearchScopeNorHasTemporaryAccess() {
    assertFalse(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityA1, programA));
    assertFalse(
        trackerOwnershipAccessManager.hasAccess(
            UserDetails.fromUser(userB),
            trackedEntityA1.getUid(),
            trackedEntityA1.getOrganisationUnit(),
            programA));

    injectSecurityContextUser(userB);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), UID.of(programA), defaultParams));
    assertEquals(TrackerOwnershipManager.NO_READ_ACCESS_TO_ORG_UNIT, exception.getMessage());
  }

  @Test
  void shouldNotHaveAccessWhenProgramProtectedAndUserNotInCaptureScopeNorHasTemporaryAccess() {
    userB.setTeiSearchOrganisationUnits(Set.of(organisationUnitA));
    userService.updateUser(userB);

    assertFalse(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityA1, programA));
    assertFalse(
        trackerOwnershipAccessManager.hasAccess(
            UserDetails.fromUser(userB),
            trackedEntityA1.getUid(),
            trackedEntityA1.getOrganisationUnit(),
            programA));

    injectSecurityContextUser(userB);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), UID.of(programA), defaultParams));
    assertEquals(TrackerOwnershipManager.OWNERSHIP_ACCESS_DENIED, exception.getMessage());
  }

  @Test
  void shouldHaveAccessWhenProgramClosedAndUserInCaptureScope() {
    assertTrue(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityB1, programB));
    assertTrue(
        trackerOwnershipAccessManager.hasAccess(
            userDetailsB,
            trackedEntityB1.getUid(),
            trackedEntityB1.getOrganisationUnit(),
            programB));
  }

  @Test
  void shouldNotHaveAccessWhenProgramClosedAndUserHasTemporaryAccess() {
    trackerOwnershipAccessManager.grantTemporaryOwnership(
        trackedEntityA1, programB, userDetailsB, "test closed program");
    assertFalse(trackerOwnershipAccessManager.hasAccess(userDetailsB, trackedEntityA1, programB));
    assertFalse(
        trackerOwnershipAccessManager.hasAccess(
            UserDetails.fromUser(userB),
            trackedEntityA1.getUid(),
            trackedEntityA1.getOrganisationUnit(),
            programB));

    injectSecurityContextUser(userB);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), UID.of(programB), defaultParams));
    assertEquals(TrackerOwnershipManager.PROGRAM_ACCESS_CLOSED, exception.getMessage());
  }

  @Test
  void shouldHaveAccessWhenProgramOpenAndUserInScope()
      throws ForbiddenException, NotFoundException, BadRequestException {
    programA.setAccessLevel(AccessLevel.OPEN);
    programService.updateProgram(programA);

    assertEquals(
        trackedEntityA1,
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA1), UID.of(programA), defaultParams));
  }

  @Test
  void shouldNotHaveAccessWhenProgramOpenAndUserNotInSearchScope() throws ForbiddenException {
    programA.setAccessLevel(AccessLevel.OPEN);
    programService.updateProgram(programA);

    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(trackedEntityA1, programA, organisationUnitB);

    injectSecurityContextUser(userA);
    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), UID.of(programA), defaultParams));
    assertEquals(TrackerOwnershipManager.NO_READ_ACCESS_TO_ORG_UNIT, exception.getMessage());
  }

  @Test
  void shouldHaveAccessWhenProgramNotProvidedAndTEEnrolledButHaveAccessToTEOwner()
      throws ForbiddenException, NotFoundException, BadRequestException {
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitA, false, true);
    trackerOwnershipAccessManager.transferOwnership(trackedEntityA1, programA, organisationUnitB);

    injectSecurityContextUser(userB);
    assertEquals(
        trackedEntityA1,
        trackedEntityService.getTrackedEntity(UID.of(trackedEntityA1), null, defaultParams));
  }

  @Test
  void shouldNotHaveAccessWhenProgramNotProvidedAndTEEnrolledAndNoAccessToTEOwner() {
    injectSecurityContextUser(userB);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), null, defaultParams));
    assertEquals(
        String.format("User has no access to TrackedEntity:%s", trackedEntityA1.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldHaveAccessWhenProgramNotProvidedAndTENotEnrolledButHaveAccessToTeRegistrationUnit()
      throws ForbiddenException, NotFoundException, BadRequestException {
    injectSecurityContextUser(userB);

    assertEquals(
        trackedEntityB1,
        trackedEntityService.getTrackedEntity(UID.of(trackedEntityB1), null, defaultParams));
  }

  @Test
  void shouldNotHaveAccessWhenProgramNotProvidedAndTENotEnrolledAndNoAccessToTeRegistrationUnit() {
    injectSecurityContextUser(userA);

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityB1), null, defaultParams));
    assertEquals(
        String.format("User has no access to TrackedEntity:%s", trackedEntityB1.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToAccessibleOrgUnit()
      throws ForbiddenException, BadRequestException, NotFoundException {
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitA, false, true);
    transferOwnership(trackedEntityA1, programA, organisationUnitB);
    TrackedEntityOperationParams operationParams = createOperationParams(null);
    injectSecurityContext(userDetailsB);
    List<String> trackedEntities = getTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA1.getUid(), trackedEntityB1.getUid()), trackedEntities);
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToAccessibleOrgUnitAndSuperUser()
      throws ForbiddenException, BadRequestException, NotFoundException {
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitA, false, true);
    transferOwnership(trackedEntityA1, programA, organisationUnitB);
    superUser.setOrganisationUnits(Set.of(organisationUnitB));
    userService.updateUser(superUser);
    TrackedEntityOperationParams operationParams = createOperationParams(null);
    injectSecurityContextUser(superUser);
    List<String> trackedEntities = getTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA1.getUid(), trackedEntityB1.getUid()), trackedEntities);
  }

  @Test
  void shouldNotFindTrackedEntityWhenTransferredToInaccessibleOrgUnit()
      throws ForbiddenException, BadRequestException, NotFoundException {
    trackerOwnershipAccessManager.assignOwnership(
        trackedEntityA1, programA, organisationUnitA, false, true);
    transferOwnership(trackedEntityA1, programA, organisationUnitB);

    TrackedEntityOperationParams operationParams = createOperationParams(null);
    injectSecurityContext(userDetailsA);
    assertIsEmpty(getTrackedEntities(operationParams));
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToInaccessibleOrgUnitIfHasReadAccessToOtherProgram()
      throws ForbiddenException, BadRequestException, NotFoundException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);
    programB.setSharing(Sharing.builder().publicAccess("rwrw----").build());
    programService.updateProgram(programB);
    injectSecurityContext(userDetailsA);

    TrackedEntityOperationParams operationParams = createOperationParams(null);

    List<String> trackedEntities = getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA1.getUid()), trackedEntities);
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToInaccessibleOrgUnitIfSuperUser()
      throws ForbiddenException, BadRequestException, NotFoundException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);
    injectSecurityContextUser(superUser);

    TrackedEntityOperationParams operationParams = createOperationParams(null);
    List<String> trackedEntities = getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA1.getUid()), trackedEntities);
  }

  @Test
  void shouldNotTransferOwnershipWhenOrgUnitNotAssociatedToProgram() {
    OrganisationUnit notAssociatedOrgUnit = createOrganisationUnit('C');
    organisationUnitService.addOrganisationUnit(notAssociatedOrgUnit);
    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () -> transferOwnership(trackedEntityA1, programA, notAssociatedOrgUnit));
    assertEquals(
        String.format(
            "The program %s is not associated to the org unit %s",
            programA.getUid(), notAssociatedOrgUnit.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldFindTrackedEntityWhenProgramSuppliedAndUserIsOwner()
      throws ForbiddenException, BadRequestException, NotFoundException {
    assignOwnership(trackedEntityA1, programA, organisationUnitA);
    TrackedEntityOperationParams operationParams = createOperationParams(UID.of(programA));
    injectSecurityContext(userDetailsA);

    List<String> trackedEntities = getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA1.getUid()), trackedEntities);
  }

  @Test
  void shouldNotFindTrackedEntityWhenProgramSuppliedAndUserIsNotOwner()
      throws ForbiddenException, BadRequestException, NotFoundException {
    assignOwnership(trackedEntityA1, programA, organisationUnitA);
    TrackedEntityOperationParams operationParams = createOperationParams(UID.of(programA));
    injectSecurityContext(userDetailsB);

    assertIsEmpty(getTrackedEntities(operationParams));
  }

  private void transferOwnership(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit)
      throws ForbiddenException {
    trackerOwnershipAccessManager.transferOwnership(trackedEntity, program, orgUnit);
  }

  private void assignOwnership(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit) {
    trackerOwnershipAccessManager.assignOwnership(trackedEntity, program, orgUnit, false, true);
  }

  private TrackedEntityOperationParams createOperationParams(UID programUid) {
    return TrackedEntityOperationParams.builder()
        .trackedEntityType(trackedEntityA1.getTrackedEntityType())
        .orgUnitMode(ACCESSIBLE)
        .program(programUid)
        .build();
  }

  private List<String> getTrackedEntities(TrackedEntityOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(trackedEntityService.getTrackedEntities(params));
  }
}
