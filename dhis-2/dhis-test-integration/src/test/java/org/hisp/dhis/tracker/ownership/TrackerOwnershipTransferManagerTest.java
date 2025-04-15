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
package org.hisp.dhis.tracker.ownership;

import static org.hisp.dhis.common.AccessLevel.AUDITED;
import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.tracker.TrackerTestUtils.uids;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
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
import org.hisp.dhis.relationship.RelationshipEntity;
import org.hisp.dhis.relationship.RelationshipType;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AccessStringHelper;
import org.hisp.dhis.test.integration.PostgresIntegrationTestBase;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.acl.TrackedEntityProgramOwnerService;
import org.hisp.dhis.tracker.acl.TrackerOwnershipManager;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityEnrollmentParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.sharing.Sharing;
import org.hisp.dhis.user.sharing.UserAccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class TrackerOwnershipTransferManagerTest extends PostgresIntegrationTestBase {

  @Autowired private TrackerOwnershipManager trackerOwnershipManager;

  @Autowired
  private org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService trackedEntityService;

  @Autowired private OrganisationUnitService organisationUnitService;

  @Autowired private ProgramService programService;

  @Autowired private IdentifiableObjectManager manager;

  @Autowired private TrackedEntityTypeService trackedEntityTypeService;

  @Autowired private TrackedEntityProgramOwnerService trackedEntityProgramOwnerService;

  private TrackedEntityType trackedEntityType;

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

  private final RelationshipType relationshipType = createRelationshipType('A');

  @BeforeEach
  void setUp() {
    organisationUnitA = createOrganisationUnit('A');
    organisationUnitService.addOrganisationUnit(organisationUnitA);
    organisationUnitB = createOrganisationUnit('B');
    organisationUnitService.addOrganisationUnit(organisationUnitB);

    trackedEntityType = createTrackedEntityType('A');
    trackedEntityTypeService.addTrackedEntityType(trackedEntityType);
    trackedEntityType.setSharing(Sharing.builder().publicAccess(AccessStringHelper.FULL).build());
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);

    trackedEntityA1 = createTrackedEntity(organisationUnitA, trackedEntityType);
    trackedEntityB1 = createTrackedEntity(organisationUnitB, trackedEntityType);
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
    programA.setAccessLevel(PROTECTED);
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
    trackedEntityProgramOwnerService.createOrUpdateTrackedEntityProgramOwner(
        trackedEntityA1, programA, organisationUnitA);

    defaultParams =
        new TrackedEntityParams(false, TrackedEntityEnrollmentParams.FALSE, false, false);

    relationshipType
        .getFromConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipType.getFromConstraint().setTrackedEntityType(trackedEntityType);
    relationshipType
        .getToConstraint()
        .setRelationshipEntity(RelationshipEntity.TRACKED_ENTITY_INSTANCE);
    relationshipType.getToConstraint().setTrackedEntityType(trackedEntityType);
    manager.save(relationshipType, false);

    User admin = getAdminUser();
    admin.setOrganisationUnits(Set.of(organisationUnitA, organisationUnitB));
    manager.update(admin);
    injectSecurityContextUser(admin);
  }

  @Test
  void shouldFailWhenGrantingTemporaryOwnershipAndUserNotInSearchScope() {
    injectSecurityContext(userDetailsB);
    assertTrue(trackerOwnershipManager.hasAccess(userDetailsA, trackedEntityA1, programA));
    assertFalse(trackerOwnershipManager.hasAccess(userDetailsB, trackedEntityA1, programA));
    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    UID.of(trackedEntityA1), UID.of(programA), "testing reason"));

    assertEquals(
        "Temporary ownership not created. The owner of the entity-program combination is not in the user's search scope.",
        exception.getMessage());
  }

  @Test
  void shouldNotHaveAccessToEnrollmentWithUserAWhenTransferredToAnotherOrgUnit()
      throws ForbiddenException, BadRequestException, NotFoundException {
    userA.setTeiSearchOrganisationUnits(Set.of(organisationUnitB));
    userService.updateUser(userA);

    transferOwnership(trackedEntityA1, programA, organisationUnitB);

    injectSecurityContextUser(userA);
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), UID.of(programA), defaultParams));
    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", trackedEntityA1.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldHaveAccessToEnrollmentWithUserBWhenTransferredToOwnOrgUnit()
      throws ForbiddenException, NotFoundException, BadRequestException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);

    injectSecurityContextUser(userB);
    assertEquals(
        trackedEntityA1,
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA1), UID.of(programA), defaultParams));
  }

  @Test
  void shouldHaveAccessToEnrollmentWithSuperUserWhenTransferredToOwnOrgUnit()
      throws ForbiddenException, NotFoundException, BadRequestException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);
    superUser.setOrganisationUnits(Set.of(organisationUnitB));
    userService.updateUser(superUser);
    injectSecurityContextUser(superUser);

    assertEquals(
        trackedEntityA1,
        trackedEntityService.getTrackedEntity(
            UID.of(trackedEntityA1), UID.of(programA), defaultParams));
  }

  @Test
  void shouldHaveAccessWhenProgramProtectedAndHasTemporaryAccess()
      throws ForbiddenException, BadRequestException {
    userB.setTeiSearchOrganisationUnits(Set.of(organisationUnitA));
    userService.updateUser(userB);
    userDetailsB = UserDetails.fromUser(userB);
    injectSecurityContext(userDetailsB);

    trackerOwnershipManager.grantTemporaryOwnership(
        UID.of(trackedEntityA1), UID.of(programA), "test protected program");

    assertTrue(trackerOwnershipManager.hasAccess(userDetailsB, trackedEntityA1, programA));
    assertTrue(
        trackerOwnershipManager.hasAccess(
            userDetailsB,
            trackedEntityA1.getUid(),
            trackedEntityA1.getOrganisationUnit(),
            programA));
  }

  private static Stream<Arguments> providePrograms() {
    return Stream.of(
        Arguments.of(OPEN, 'C'), Arguments.of(AUDITED, 'D'), Arguments.of(CLOSED, 'E'));
  }

  @ParameterizedTest
  @MethodSource("providePrograms")
  void shouldFailWhenGrantingTemporaryOwnershipToProgramWithAccessLevelOtherThanProtected(
      AccessLevel accessLevel, char uniqueChar) {
    Program program = createProgram(accessLevel, uniqueChar);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    UID.of(trackedEntityA1), UID.of(program), "test temporary ownership"));

    assertContains(
        "Temporary ownership can only be granted to protected programs.", exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfUserIsSuperuser() {
    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    UID.of(trackedEntityA1), UID.of(programA), "test temporary ownership"));

    assertEquals(
        "Temporary ownership not created. Current user is a superuser.", exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfProgramIsNull() {
    injectSecurityContext(userDetailsB);
    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    UID.of(trackedEntityA1), null, "test temporary ownership"));

    assertEquals("Provided program can't be null.", exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfProgramIsNotTrackerProgram() {
    Program eventProgram = createProgram(PROTECTED, 'F');
    eventProgram.setProgramType(WITHOUT_REGISTRATION);
    injectSecurityContextUser(superUser);
    manager.update(eventProgram);
    injectSecurityContext(userDetailsB);

    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    UID.of(trackedEntityA1), UID.of(eventProgram), "test temporary ownership"));

    assertEquals(
        "Provided program, " + eventProgram.getUid() + ", is not a tracker program.",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfTrackedEntitySuppliedIsNull() {
    injectSecurityContext(userDetailsA);
    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    null, UID.of(programA), "test temporary ownership"));

    assertEquals("Provided tracked entity can't be null.", exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfUserHasNoAccessToProgram() {
    programA.setSharing(Sharing.builder().publicAccess(AccessStringHelper.DEFAULT).build());
    programService.updateProgram(programA);
    injectSecurityContext(userDetailsA);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    UID.of(trackedEntityA1), UID.of(programA), "test temporary ownership"));

    assertStartsWith(
        "Provided program, " + programA.getUid() + ", does not exist.", exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfUserHasNoAccessToTET() {
    trackedEntityType.setSharing(
        Sharing.builder().publicAccess(AccessStringHelper.DEFAULT).build());
    trackedEntityTypeService.updateTrackedEntityType(trackedEntityType);
    trackedEntityA1.setTrackedEntityType(trackedEntityType);
    manager.update(trackedEntityA1);
    injectSecurityContext(userDetailsA);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    UID.of(trackedEntityA1), UID.of(programA), "test temporary ownership"));

    assertStartsWith(
        "Temporary ownership not created. User has no data read access to tracked entity type",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenGrantingTemporaryAccessIfProgramTETDifferentThanTEs() {
    TrackedEntityType differentTET = createTrackedEntityType('B');
    trackedEntityTypeService.addTrackedEntityType(differentTET);
    differentTET.setSharing(Sharing.builder().publicAccess(AccessStringHelper.FULL).build());
    trackedEntityTypeService.updateTrackedEntityType(differentTET);
    trackedEntityA1.setTrackedEntityType(differentTET);
    manager.update(trackedEntityA1);
    injectSecurityContext(userDetailsA);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipManager.grantTemporaryOwnership(
                    UID.of(trackedEntityA1), UID.of(programA), "test temporary ownership"));

    assertStartsWith(
        "Temporary ownership not created. The tracked entity type of the program",
        exception.getMessage());
  }

  @Test
  void shouldNotHaveAccessWhenProgramOpenAndUserNotInSearchScope()
      throws ForbiddenException, BadRequestException, NotFoundException {
    programA.setAccessLevel(OPEN);
    programService.updateProgram(programA);

    transferOwnership(trackedEntityA1, programA, organisationUnitB);

    injectSecurityContextUser(userA);
    NotFoundException exception =
        assertThrows(
            NotFoundException.class,
            () ->
                trackedEntityService.getTrackedEntity(
                    UID.of(trackedEntityA1), UID.of(programA), defaultParams));
    assertEquals(
        String.format("TrackedEntity with id %s could not be found.", trackedEntityA1.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldHaveAccessWhenProgramNotProvidedAndTEEnrolledButHaveAccessToTEOwner()
      throws ForbiddenException, NotFoundException, BadRequestException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);

    injectSecurityContextUser(userB);
    assertEquals(
        trackedEntityA1,
        trackedEntityService.getTrackedEntity(UID.of(trackedEntityA1), null, defaultParams));
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToAccessibleOrgUnit()
      throws ForbiddenException, BadRequestException, NotFoundException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);
    TrackedEntityOperationParams operationParams = createOperationParams();
    injectSecurityContext(userDetailsB);
    List<String> trackedEntities = getTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA1.getUid(), trackedEntityB1.getUid()), trackedEntities);
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToAccessibleOrgUnitAndSuperUser()
      throws ForbiddenException, BadRequestException, NotFoundException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);
    superUser.setOrganisationUnits(Set.of(organisationUnitB));
    userService.updateUser(superUser);
    TrackedEntityOperationParams operationParams = createOperationParams();
    injectSecurityContextUser(superUser);
    List<String> trackedEntities = getTrackedEntities(operationParams);

    assertContainsOnly(
        List.of(trackedEntityA1.getUid(), trackedEntityB1.getUid()), trackedEntities);
  }

  @Test
  void shouldNotFindTrackedEntityWhenTransferredToInaccessibleOrgUnit()
      throws ForbiddenException, BadRequestException, NotFoundException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);

    TrackedEntityOperationParams operationParams = createOperationParams();
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
    TrackedEntityOperationParams operationParams = createOperationParams();

    List<String> trackedEntities = getTrackedEntities(operationParams);

    assertContainsOnly(List.of(trackedEntityA1.getUid()), trackedEntities);
  }

  @Test
  void shouldFindTrackedEntityWhenTransferredToInaccessibleOrgUnitIfSuperUser()
      throws ForbiddenException, BadRequestException, NotFoundException {
    transferOwnership(trackedEntityA1, programA, organisationUnitB);
    injectSecurityContextUser(superUser);

    TrackedEntityOperationParams operationParams = createOperationParams();
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
  void shouldNotTransferOwnershipWhenOrgUnitDoesNotExist() {
    UID madeUpOrgUnit = UID.generate();

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () ->
                trackerOwnershipManager.transferOwnership(
                    UID.of(trackedEntityA1), UID.of(programA), madeUpOrgUnit));
    assertEquals("Org unit supplied does not exist.", exception.getMessage());
  }

  private void transferOwnership(
      TrackedEntity trackedEntity, Program program, OrganisationUnit orgUnit)
      throws ForbiddenException, BadRequestException, NotFoundException {
    trackerOwnershipManager.transferOwnership(
        UID.of(trackedEntity), UID.of(program), UID.of(orgUnit));
  }

  private TrackedEntityOperationParams createOperationParams() {
    return TrackedEntityOperationParams.builder()
        .trackedEntityType(trackedEntityA1.getTrackedEntityType())
        .orgUnitMode(ACCESSIBLE)
        .build();
  }

  private List<String> getTrackedEntities(TrackedEntityOperationParams params)
      throws ForbiddenException, BadRequestException, NotFoundException {
    return uids(trackedEntityService.findTrackedEntities(params));
  }

  private Program createProgram(AccessLevel accessLevel, char uniqueChar) {
    injectSecurityContextUser(superUser);
    Program program = createProgram(uniqueChar);
    program.setAccessLevel(accessLevel);
    manager.save(program);
    program.getSharing().setPublicAccess("rwrw----");
    manager.update(program);
    injectSecurityContext(userDetailsB);

    return program;
  }
}
