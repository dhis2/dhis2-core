/*
 * Copyright (c) 2004-2023, University of Oslo
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
package org.hisp.dhis.tracker.export;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.program.ProgramType.WITHOUT_REGISTRATION;
import static org.hisp.dhis.tracker.export.OperationsParamsValidator.validateOrgUnitMode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.Set;
import org.hisp.dhis.common.IdentifiableObjectManager;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
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
import org.hisp.dhis.tracker.deprecated.audit.TrackedEntityAuditService;
import org.hisp.dhis.user.CurrentUserUtil;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperationsParamsValidatorTest {

  private static final String PARENT_ORG_UNIT_UID = "parent-org-unit";

  private final OrganisationUnit captureScopeOrgUnit = createOrgUnit("captureScopeOrgUnit", "uid3");

  private final OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");

  private final Program program = new Program("program");

  private final TrackedEntity trackedEntity = new TrackedEntity();

  private final TrackedEntityType trackedEntityType = new TrackedEntityType();

  private final OrganisationUnit orgUnit = new OrganisationUnit();

  private static final String PROGRAM_UID = "PROGRAM_UID";

  private static final String TRACKED_ENTITY_UID = "TRACKED_ENTITY_UID";

  private static final String TRACKED_ENTITY_TYPE_UID = "TRACKED_ENTITY_TYPE_UID";

  private static final String ORG_UNIT_UID = "ORG_UNIT_UID";

  @Mock private ProgramService programService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private AclService aclService;

  @Mock private IdentifiableObjectManager manager;

  @Mock private TrackedEntityAuditService trackedEntityAuditService;

  @InjectMocks private OperationsParamsValidator paramsValidator;

  @BeforeEach
  public void setUp() {
    OrganisationUnit organisationUnit = createOrgUnit("orgUnit", PARENT_ORG_UNIT_UID);
    organisationUnit.setChildren(Set.of(captureScopeOrgUnit, searchScopeOrgUnit));
  }

  @Test
  void shouldFailWhenOuModeCaptureAndUserHasNoOrgUnitsAssigned() {
    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(new User()));
      Exception exception =
          Assertions.assertThrows(
              BadRequestException.class, () -> validateOrgUnitMode(CAPTURE, program));

      assertEquals("User needs to be assigned data capture org units", exception.getMessage());
    }
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "ACCESSIBLE", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenOuModeRequiresUserScopeOrgUnitAndUserHasNoOrgUnitsAssigned(
      OrganisationUnitSelectionMode orgUnitMode) {

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(new User()));
      Exception exception =
          Assertions.assertThrows(
              BadRequestException.class, () -> validateOrgUnitMode(orgUnitMode, program));

      assertEquals(
          "User needs to be assigned either search or data capture org units",
          exception.getMessage());
    }
  }

  @Test
  void shouldFailWhenOuModeAllAndNotSuperuser() {
    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(new User()));
      Exception exception =
          Assertions.assertThrows(
              BadRequestException.class, () -> validateOrgUnitMode(ALL, program));

      assertEquals(
          "Current user is not authorized to query across all organisation units",
          exception.getMessage());
    }
  }

  @Test
  void shouldThrowBadRequestExceptionWhenProgramDoesNotExist() {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(null);

    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class, () -> paramsValidator.validateTrackerProgram(PROGRAM_UID));

    assertEquals(
        String.format("Program is specified but does not exist: %s", PROGRAM_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnProgramWhenUserHasAccessToProgram()
      throws ForbiddenException, BadRequestException {
    User user = new User();
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(UserDetails.fromUser(user), program)).thenReturn(true);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      assertEquals(program, paramsValidator.validateTrackerProgram(PROGRAM_UID));
    }
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToProgram() {
    User user = new User();
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(UserDetails.fromUser(user), program)).thenReturn(false);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      Exception exception =
          Assertions.assertThrows(
              ForbiddenException.class, () -> paramsValidator.validateTrackerProgram(PROGRAM_UID));

      assertEquals(
          String.format("User has no access to program: %s", program.getUid()),
          exception.getMessage());
    }
  }

  @Test
  void shouldReturnProgramWhenUserHasAccessToProgramTrackedEntityType()
      throws ForbiddenException, BadRequestException {
    User user = new User();
    TrackedEntityType trackedEntityType = new TrackedEntityType("trackedEntityType", "");
    program.setTrackedEntityType(trackedEntityType);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(UserDetails.fromUser(user), program)).thenReturn(true);
    when(aclService.canDataRead(UserDetails.fromUser(user), program.getTrackedEntityType()))
        .thenReturn(true);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      assertEquals(program, paramsValidator.validateTrackerProgram(PROGRAM_UID));
    }
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToProgramTrackedEntityType() {
    User user = new User();
    TrackedEntityType trackedEntityType = new TrackedEntityType("trackedEntityType", "");
    program.setTrackedEntityType(trackedEntityType);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(UserDetails.fromUser(user), program)).thenReturn(true);
    when(aclService.canDataRead(UserDetails.fromUser(user), program.getTrackedEntityType()))
        .thenReturn(false);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      Exception exception =
          Assertions.assertThrows(
              ForbiddenException.class, () -> paramsValidator.validateTrackerProgram(PROGRAM_UID));

      assertEquals(
          String.format(
              "Current user is not authorized to read data from selected program's tracked entity type: %s",
              trackedEntityType.getUid()),
          exception.getMessage());
    }
  }

  @Test
  void shouldThrowBadRequestExceptionWhenRequestingTrackedEntitiesAndProgramIsNotATrackerProgram() {
    User user = new User();
    program.setProgramType(WITHOUT_REGISTRATION);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(UserDetails.fromUser(user), program)).thenReturn(true);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      Exception exception =
          Assertions.assertThrows(
              BadRequestException.class, () -> paramsValidator.validateTrackerProgram(PROGRAM_UID));

      assertEquals(
          String.format("Program specified is not a tracker program: %s", PROGRAM_UID),
          exception.getMessage());
    }
  }

  @Test
  void shouldReturnTrackedEntityWhenTrackedEntityUidExists()
      throws ForbiddenException, BadRequestException {
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID)).thenReturn(trackedEntity);

    assertEquals(
        trackedEntity, paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, new User()));
  }

  @Test
  void shouldThrowBadRequestExceptionWhenTrackedEntityDoesNotExist() {
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID)).thenReturn(null);

    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, new User()));

    assertEquals(
        String.format("Tracked entity is specified but does not exist: %s", TRACKED_ENTITY_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnTrackedEntityWhenUserHasAccessToTrackedEntity()
      throws ForbiddenException, BadRequestException {
    User user = new User();
    TrackedEntityType trackedEntityType = new TrackedEntityType("trackedEntityType", "");
    trackedEntity.setTrackedEntityType(trackedEntityType);
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID)).thenReturn(trackedEntity);
    when(aclService.canDataRead(user, trackedEntity.getTrackedEntityType())).thenReturn(true);

    assertEquals(trackedEntity, paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user));
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToTrackedEntity() {
    User user = new User();
    TrackedEntityType trackedEntityType = new TrackedEntityType("trackedEntityType", "");
    trackedEntity.setTrackedEntityType(trackedEntityType);
    when(manager.get(TrackedEntity.class, TRACKED_ENTITY_UID)).thenReturn(trackedEntity);
    when(aclService.canDataRead(user, trackedEntity.getTrackedEntityType())).thenReturn(false);

    Exception exception =
        Assertions.assertThrows(
            ForbiddenException.class,
            () -> paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user));

    assertEquals(
        String.format(
            "Current user is not authorized to read data from type of selected tracked entity: %s",
            trackedEntity.getUid()),
        exception.getMessage());
  }

  @Test
  void shouldThrowBadRequestExceptionWhenTrackedEntityTypeDoesNotExist() {
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID)).thenReturn(null);

    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateTrackedEntityType(TRACKED_ENTITY_TYPE_UID));

    assertEquals(
        String.format(
            "Tracked entity type is specified but does not exist: %s", TRACKED_ENTITY_TYPE_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnTrackedEntityTypeWhenUserHasAccessToTrackedEntityType()
      throws ForbiddenException, BadRequestException {
    User user = new User();
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID))
        .thenReturn(trackedEntityType);
    when(aclService.canDataRead(UserDetails.fromUser(user), trackedEntityType)).thenReturn(true);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      assertEquals(
          trackedEntityType, paramsValidator.validateTrackedEntityType(TRACKED_ENTITY_TYPE_UID));
    }
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToTrackedEntityType() {
    User user = new User();
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID))
        .thenReturn(trackedEntityType);
    when(aclService.canDataRead(UserDetails.fromUser(user), trackedEntityType)).thenReturn(false);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      Exception exception =
          Assertions.assertThrows(
              ForbiddenException.class,
              () -> paramsValidator.validateTrackedEntityType(TRACKED_ENTITY_TYPE_UID));

      assertEquals(
          String.format(
              "Current user is not authorized to read data from selected tracked entity type: %s",
              trackedEntityType.getUid()),
          exception.getMessage());
    }
  }

  @Test
  void shouldThrowBadRequestExceptionWhenOrgUnitDoesNotExist() {
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT_UID)).thenReturn(null);

    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class,
            () -> paramsValidator.validateOrgUnits(Set.of(ORG_UNIT_UID)));

    assertEquals(
        String.format("Organisation unit does not exist: %s", ORG_UNIT_UID),
        exception.getMessage());
  }

  @Test
  void shouldReturnOrgUnitWhenUserHasAccessToOrgUnit()
      throws ForbiddenException, BadRequestException {
    User user = new User();
    user.setOrganisationUnits(Set.of(orgUnit));
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT_UID)).thenReturn(orgUnit);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      assertEquals(Set.of(orgUnit), paramsValidator.validateOrgUnits(Set.of(ORG_UNIT_UID)));
    }
  }

  @Test
  void shouldThrowForbiddenExceptionWhenUserHasNoAccessToOrgUnit() {
    User user = new User();
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT_UID)).thenReturn(orgUnit);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      Exception exception =
          Assertions.assertThrows(
              ForbiddenException.class,
              () -> paramsValidator.validateOrgUnits(Set.of(ORG_UNIT_UID)));

      assertEquals(
          String.format("Organisation unit is not part of the search scope: %s", orgUnit.getUid()),
          exception.getMessage());
    }
  }

  @Test
  void shouldReturnOrgUnitsWhenUserIsSuperButHasNoAccessToOrgUnit()
      throws ForbiddenException, BadRequestException {
    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Sets.newHashSet("ALL"));
    user.setUserRoles(Set.of(userRole));
    when(organisationUnitService.getOrganisationUnit(ORG_UNIT_UID)).thenReturn(orgUnit);

    try (MockedStatic<CurrentUserUtil> userUtilMockedStatic = mockStatic(CurrentUserUtil.class)) {
      userUtilMockedStatic
          .when(CurrentUserUtil::getCurrentUserDetails)
          .thenReturn(UserDetails.fromUser(user));
      Set<OrganisationUnit> orgUnits = paramsValidator.validateOrgUnits(Set.of(ORG_UNIT_UID));

      assertEquals(Set.of(orgUnit), orgUnits);
    }
  }

  private OrganisationUnit createOrgUnit(String name, String uid) {
    OrganisationUnit orgUnit = new OrganisationUnit(name);
    orgUnit.setUid(uid);
    return orgUnit;
  }
}
