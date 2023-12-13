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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT) // common setup
@ExtendWith(MockitoExtension.class)
class EnrollmentOperationParamsMapperTest {

  private static final String ORG_UNIT_1_UID = "lW0T2U7gZUi";

  private static final String ORG_UNIT_2_UID = "TK4KA0IIWqa";

  private static final String PROGRAM_UID = "XhBYIraw7sv";

  private static final String TRACKED_ENTITY_TYPE_UID = "Dp8baZYrLtr";

  private static final String TRACKED_ENTITY_UID = "DGbr8GHG4li";

  @Mock private CurrentUserService currentUserService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramService programService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @Mock private AclService aclService;

  @InjectMocks private EnrollmentOperationParamsMapper mapper;

  private OrganisationUnit orgUnit1;

  private OrganisationUnit orgUnit2;

  private User user;

  private Program program;

  private TrackedEntityType trackedEntityType;

  private TrackedEntity trackedEntity;

  @BeforeEach
  void setUp() {
    user = new User();
    when(currentUserService.getCurrentUser()).thenReturn(user);

    orgUnit1 = new OrganisationUnit("orgUnit1");
    orgUnit1.setUid(ORG_UNIT_1_UID);
    when(organisationUnitService.getOrganisationUnit(orgUnit1.getUid())).thenReturn(orgUnit1);
    orgUnit2 = new OrganisationUnit("orgUnit2");
    orgUnit2.setUid(ORG_UNIT_2_UID);
    orgUnit2.setParent(orgUnit1);
    orgUnit1.setChildren(Set.of(orgUnit2));
    when(organisationUnitService.getOrganisationUnit(orgUnit2.getUid())).thenReturn(orgUnit2);

    user.setTeiSearchOrganisationUnits(Set.of(orgUnit1, orgUnit2));
    user.setOrganisationUnits(Set.of(orgUnit2));

    trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TRACKED_ENTITY_TYPE_UID);
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID))
        .thenReturn(trackedEntityType);

    program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);

    trackedEntity = new TrackedEntity();
    trackedEntity.setUid(TRACKED_ENTITY_UID);
    when(trackedEntityService.getTrackedEntity(TRACKED_ENTITY_UID)).thenReturn(trackedEntity);
  }

  @Test
  void shouldMapWithoutFetchingNullParamsWhenParamsAreNotSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(ACCESSIBLE).build();

    mapper.map(operationParams);

    verifyNoInteractions(programService);
    verifyNoInteractions(organisationUnitService);
    verifyNoInteractions(trackedEntityTypeService);
    verifyNoInteractions(trackedEntityService);
  }

  @Test
  void shouldMapOrgUnitsWhenOrgUnitUidsAreSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(ORG_UNIT_1_UID, ORG_UNIT_2_UID))
            .orgUnitMode(SELECTED)
            .programUid(program.getUid())
            .build();
    when(trackerAccessManager.canAccess(user, program, orgUnit1)).thenReturn(true);
    when(trackerAccessManager.canAccess(user, program, orgUnit2)).thenReturn(true);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit2.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertContainsOnly(Set.of(orgUnit1, orgUnit2), params.getOrganisationUnits());
  }

  @Test
  void shouldThrowExceptionWhenOrgUnitNotFound() {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of("JW6BrFd0HLu"))
            .orgUnitMode(SELECTED)
            .programUid(PROGRAM_UID)
            .build();

    when(trackerAccessManager.canAccess(user, program, orgUnit2)).thenReturn(true);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals("Organisation unit does not exist: JW6BrFd0HLu", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenOrgUnitNotInScope() {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitUids(Set.of(ORG_UNIT_1_UID)).build();
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(false);

    Exception exception = assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "Organisation unit is not part of the search scope: " + ORG_UNIT_1_UID,
        exception.getMessage());
  }

  @Test
  void shouldMapParamsWhenOrgUnitNotInScopeButUserIsSuperuser()
      throws ForbiddenException, BadRequestException {
    User superuser = createUser("ALL");
    when(currentUserService.getCurrentUser()).thenReturn(superuser);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(ORG_UNIT_1_UID))
            .orgUnitMode(ALL)
            .build();
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(false);

    EnrollmentQueryParams queryParams = mapper.map(operationParams);
    assertContainsOnly(
        operationParams.getOrgUnitUids(),
        queryParams.getOrganisationUnits().stream().map(BaseIdentifiableObject::getUid).toList());
    assertEquals(operationParams.getOrgUnitMode(), queryParams.getOrganisationUnitMode());
  }

  @Test
  void shouldFailWhenOrgUnitNotInScopeAndUserHasSearchInAllAuthority() {

    User user = createUser(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name());
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit2));
    when(currentUserService.getCurrentUser()).thenReturn(user);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(ORG_UNIT_1_UID))
            .orgUnitMode(SELECTED)
            .build();
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), this.user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(false);

    Exception exception = assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "Organisation unit is not part of the search scope: " + ORG_UNIT_1_UID,
        exception.getMessage());
  }

  private User createUser(String authority) {
    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Set.of(authority));
    user.setUserRoles(Set.of(userRole));

    return user;
  }

  @Test
  void shouldMapProgramWhenProgramUidIsSpecified() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    EnrollmentOperationParams requestParams =
        EnrollmentOperationParams.builder().programUid(PROGRAM_UID).orgUnitMode(ACCESSIBLE).build();

    EnrollmentQueryParams params = mapper.map(requestParams);

    assertEquals(program, params.getProgram());
  }

  @Test
  void shouldThrowExceptionWhenProgramNotFound() {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().programUid("JW6BrFd0HLu").build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals("Program is specified but does not exist: JW6BrFd0HLu", exception.getMessage());
  }

  @Test
  void shouldFailWhenUserCantReadProgramData() {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(false);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().programUid(PROGRAM_UID).build();

    Exception exception =
        assertThrows(IllegalQueryException.class, () -> mapper.map(operationParams));
    assertEquals(
        String.format(
            "Current user is not authorized to read data from selected program:  %s", PROGRAM_UID),
        exception.getMessage());
  }

  @Test
  void shouldFailWhenUserCantReadProgramTrackedEntityTypeData() {
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(false);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().programUid(PROGRAM_UID).build();

    Exception exception =
        assertThrows(IllegalQueryException.class, () -> mapper.map(operationParams));
    assertEquals(
        String.format(
            "Current user is not authorized to read data from selected program's tracked entity type:  %s",
            TRACKED_ENTITY_TYPE_UID),
        exception.getMessage());
  }

  @Test
  void shouldMapTrackedEntityTypeWhenTrackedEntityTypeUidIsSpecified()
      throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, trackedEntityType)).thenReturn(true);
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .trackedEntityTypeUid(TRACKED_ENTITY_TYPE_UID)
            .orgUnitMode(ACCESSIBLE)
            .build();

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertEquals(trackedEntityType, params.getTrackedEntityType());
  }

  @Test
  void shouldThrowExceptionWhenTrackedEntityTypeNotFound() {
    EnrollmentOperationParams requestParams =
        EnrollmentOperationParams.builder().trackedEntityTypeUid("JW6BrFd0HLu").build();

    Exception exception = assertThrows(BadRequestException.class, () -> mapper.map(requestParams));
    assertEquals(
        "Tracked entity type is specified but does not exist: JW6BrFd0HLu", exception.getMessage());
  }

  @Test
  void shouldFailWhenUserCantReadTrackedEntityTypeData() {
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID))
        .thenReturn(trackedEntityType);
    when(aclService.canDataRead(user, trackedEntityType)).thenReturn(false);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().programUid(TRACKED_ENTITY_TYPE_UID).build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals(
        String.format("Program is specified but does not exist: %s", TRACKED_ENTITY_TYPE_UID),
        exception.getMessage());
  }

  @Test
  void shouldMapTrackedEntityWhenTrackedEntityUidIsSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .trackedEntityUid(TRACKED_ENTITY_UID)
            .orgUnitMode(ACCESSIBLE)
            .build();

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertEquals(trackedEntity, params.getTrackedEntity());
  }

  @Test
  void shouldThrowExceptionTrackedEntityNotFound() {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().trackedEntityUid("JW6BrFd0HLu").build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals(
        "Tracked entity is specified but does not exist: JW6BrFd0HLu", exception.getMessage());
  }

  @Test
  void shouldMapOrderInGivenOrder() throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orderBy("enrollmentDate", SortDirection.ASC)
            .orderBy("created", SortDirection.DESC)
            .orgUnitMode(ACCESSIBLE)
            .build();

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertEquals(
        List.of(
            new Order("enrollmentDate", SortDirection.ASC),
            new Order("created", SortDirection.DESC)),
        params.getOrder());
  }

  @Test
  void shouldMapNullOrderingParamsWhenNoOrderingParamsAreSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(ACCESSIBLE).build();

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertIsEmpty(params.getOrder());
  }

  @Test
  void shouldMapDescendantsOrgUnitModeWhenAccessibleProvided()
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(ACCESSIBLE).build();

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertEquals(DESCENDANTS, params.getOrganisationUnitMode());
    assertEquals(user.getTeiSearchOrganisationUnitsWithFallback(), params.getOrganisationUnits());
  }

  @Test
  void shouldMapDescendantsOrgUnitModeWhenCaptureProvided()
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(CAPTURE).build();

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertEquals(DESCENDANTS, params.getOrganisationUnitMode());
    assertEquals(user.getOrganisationUnits(), params.getOrganisationUnits());
  }

  @Test
  void shouldMapChildrenOrgUnitModeWhenChildrenProvided()
      throws ForbiddenException, BadRequestException {
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit2.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of(orgUnit1.getUid()))
            .orgUnitMode(CHILDREN)
            .build();

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertEquals(CHILDREN, params.getOrganisationUnitMode());
    assertEquals(Set.of(orgUnit1), params.getOrganisationUnits());
  }
}
