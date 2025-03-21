/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.audit.TrackedEntityAuditService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationsParamsValidatorTest {
  private final Program program = new Program("program");

  private final TrackedEntity trackedEntity = new TrackedEntity();

  private final TrackedEntityType trackedEntityType = new TrackedEntityType();

  private final OrganisationUnit orgUnit = createOrganisationUnit('A');

  private static final UID PROGRAM_UID = UID.generate();

  private static final UID TRACKED_ENTITY_UID = UID.generate();

  private static final UID TRACKED_ENTITY_TYPE_UID = UID.generate();

  private static final UID ORG_UNIT_UID = UID.generate();

  @Mock private ProgramService programService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private AclService aclService;

  @Mock private IdentifiableObjectManager manager;

  @Mock private TrackedEntityAuditService trackedEntityAuditService;

  @InjectMocks private OperationsParamsValidator paramsValidator;

  private final UserDetails user = UserDetails.fromUser(new User());

  @Test
  void shouldFailWhenOuModeCaptureAndUserHasNoOrgUnitsAssigned() {
    Exception exception =
        assertThrows(ForbiddenException.class, () -> validateOrgUnitMode(CAPTURE, program, user));

    assertEquals("User needs to be assigned data capture org units", exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "ACCESSIBLE", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenOuModeRequiresUserScopeOrgUnitAndUserHasNoOrgUnitsAssigned(
      OrganisationUnitSelectionMode orgUnitMode) {

    Exception exception =
        assertThrows(
            ForbiddenException.class, () -> validateOrgUnitMode(orgUnitMode, program, user));

    assertEquals(
        "User needs to be assigned either search or data capture org units",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenOuModeAllAndNotSuperuser() {
    Exception exception =
        assertThrows(ForbiddenException.class, () -> validateOrgUnitMode(ALL, program, user));

    assertEquals(
        "User is not authorized to query across all organisation units", exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenProgramDoesNotExist() {
    when(programService.getProgram(PROGRAM_UID.getValue())).thenReturn(null);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateTrackerProgram(PROGRAM_UID, user));

    assertEquals(
        String.format("Program is specified but does not exist: %s", PROGRAM_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnProgramWhenUserHasAccessToProgram()
      throws ForbiddenException, BadRequestException {
    program.setTrackedEntityType(trackedEntityType);
    when(programService.getProgram(PROGRAM_UID.getValue())).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, trackedEntityType)).thenReturn(true);
    assertEquals(program, paramsValidator.validateTrackerProgram(PROGRAM_UID, user));
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToProgram() {
    when(programService.getProgram(PROGRAM_UID.getValue())).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(false);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () -> paramsValidator.validateTrackerProgram(PROGRAM_UID, user));

    assertEquals(
        String.format("User has no access to program: %s", program.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldReturnProgramWhenUserHasAccessToProgramTrackedEntityType()
      throws ForbiddenException, BadRequestException {
    TrackedEntityType trackedEntityType = new TrackedEntityType("trackedEntityType", "");
    program.setTrackedEntityType(trackedEntityType);
    when(programService.getProgram(PROGRAM_UID.getValue())).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    assertEquals(program, paramsValidator.validateTrackerProgram(PROGRAM_UID, user));
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToProgramTrackedEntityType() {
    TrackedEntityType trackedEntityType = new TrackedEntityType("trackedEntityType", "");
    program.setTrackedEntityType(trackedEntityType);
    when(programService.getProgram(PROGRAM_UID.getValue())).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(false);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () -> paramsValidator.validateTrackerProgram(PROGRAM_UID, user));

    assertEquals(
        String.format(
            "User is not authorized to read data from selected program's tracked entity type: %s",
            trackedEntityType.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenRequestingTrackedEntitiesAndProgramIsNotATrackerProgram() {
    program.setProgramType(WITHOUT_REGISTRATION);
    when(programService.getProgram(PROGRAM_UID.getValue())).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateTrackerProgram(PROGRAM_UID, user));

    assertEquals(
        String.format("Program specified is not a tracker program: %s", PROGRAM_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnTrackedEntityWhenTrackedEntityUidExists()
      throws BadRequestException, ForbiddenException {
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID.getValue())).thenReturn(trackedEntity);

    assertEquals(
        trackedEntity, paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user, false));
  }

  @Test
  void shouldThrowBadRequestExceptionWhenTrackedEntityDoesNotExist() {
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID.getValue())).thenReturn(null);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user, false));

    assertEquals(
        String.format("Tracked entity is specified but does not exist: %s", TRACKED_ENTITY_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnTrackedEntityWhenUserHasAccessToTrackedEntity()
      throws ForbiddenException, BadRequestException {

    TrackedEntityType trackedEntityType = new TrackedEntityType("trackedEntityType", "");
    trackedEntity.setTrackedEntityType(trackedEntityType);
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID.getValue())).thenReturn(trackedEntity);
    when(aclService.canDataRead(user, trackedEntity.getTrackedEntityType())).thenReturn(true);

    assertEquals(
        trackedEntity, paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user, false));
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToTrackedEntity() {

    TrackedEntityType trackedEntityType = new TrackedEntityType("trackedEntityType", "");
    trackedEntity.setTrackedEntityType(trackedEntityType);
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID.getValue())).thenReturn(trackedEntity);
    when(aclService.canDataRead(user, trackedEntity.getTrackedEntityType())).thenReturn(false);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () -> paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user, false));

    assertEquals(
        String.format(
            "User is not authorized to read data from type of selected tracked entity: %s",
            trackedEntity.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenTrackedEntityIsSoftDeletedAndIncludeDeletedIsFalse() {
    TrackedEntity softDeletedTrackedEntity = new TrackedEntity();
    softDeletedTrackedEntity.setDeleted(true);
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID.getValue()))
        .thenReturn(softDeletedTrackedEntity);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user, false));

    assertEquals(
        String.format("Tracked entity is specified but does not exist: %s", TRACKED_ENTITY_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnTrackedEntityWhenTrackedEntityIsSoftDeletedAndIncludeDeletedIsTrue()
      throws BadRequestException, ForbiddenException {
    TrackedEntity softDeletedTrackedEntity = new TrackedEntity();
    softDeletedTrackedEntity.setDeleted(true);
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID.getValue()))
        .thenReturn(softDeletedTrackedEntity);

    assertEquals(
        softDeletedTrackedEntity,
        paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user, true));
  }

  @Test
  void shouldThrowBadRequestExceptionWhenTrackedEntityTypeDoesNotExist() {
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID.getValue()))
        .thenReturn(null);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateTrackedEntityType(TRACKED_ENTITY_TYPE_UID, user));

    assertEquals(
        String.format(
            "Tracked entity type is specified but does not exist: %s", TRACKED_ENTITY_TYPE_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnTrackedEntityTypeWhenUserHasAccessToTrackedEntityType()
      throws ForbiddenException, BadRequestException {

    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID.getValue()))
        .thenReturn(trackedEntityType);
    when(aclService.canDataRead(user, trackedEntityType)).thenReturn(true);

    assertEquals(
        trackedEntityType,
        paramsValidator.validateTrackedEntityType(TRACKED_ENTITY_TYPE_UID, user));
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToTrackedEntityType() {
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID.getValue()))
        .thenReturn(trackedEntityType);
    when(aclService.canDataRead(user, trackedEntityType)).thenReturn(false);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () -> paramsValidator.validateTrackedEntityType(TRACKED_ENTITY_TYPE_UID, user));

    assertEquals(
        String.format(
            "User is not authorized to read data from selected tracked entity type: %s",
            trackedEntityType.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenOrgUnitDoesNotExist() {
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT_UID.getValue())).thenReturn(null);

    Exception exception =
        assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateOrgUnits(Set.of(ORG_UNIT_UID), user));

    assertEquals(
        String.format("Organisation unit does not exist: %s", ORG_UNIT_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnOrgUnitWhenUserHasAccessToOrgUnit()
      throws ForbiddenException, BadRequestException {
    User userWithOrgUnits = new User();
    userWithOrgUnits.setOrganisationUnits(Set.of(orgUnit));
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT_UID.getValue())).thenReturn(orgUnit);

    assertEquals(
        Set.of(orgUnit),
        paramsValidator.validateOrgUnits(
            Set.of(ORG_UNIT_UID), UserDetails.fromUser(userWithOrgUnits)));
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToOrgUnit() {
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT_UID.getValue())).thenReturn(orgUnit);

    Exception exception =
        assertThrows(
            ForbiddenException.class,
            () -> paramsValidator.validateOrgUnits(Set.of(ORG_UNIT_UID), user));

    assertEquals(
        String.format("Organisation unit is not part of the search scope: %s", orgUnit.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldReturnOrgUnitsWhenUserIsSuperButHasNoAccessToOrgUnit()
      throws ForbiddenException, BadRequestException {

    User userWithRoles = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Sets.newHashSet("ALL"));
    userWithRoles.setUserRoles(Set.of(userRole));
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT_UID.getValue())).thenReturn(orgUnit);

    Set<OrganisationUnit> orgUnits =
        paramsValidator.validateOrgUnits(Set.of(ORG_UNIT_UID), UserDetails.fromUser(userWithRoles));

    assertEquals(Set.of(orgUnit), orgUnits);
  }
}
