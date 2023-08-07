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

import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.OrderParam;
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
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    orgUnit2 = new OrganisationUnit("orgUnit2");
    orgUnit2.setUid(ORG_UNIT_2_UID);
    when(organisationUnitService.getOrganisationUnit(orgUnit2.getUid())).thenReturn(orgUnit2);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit2.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    program = new Program();
    program.setUid(PROGRAM_UID);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);

    trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TRACKED_ENTITY_TYPE_UID);
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID))
        .thenReturn(trackedEntityType);

    trackedEntity = new TrackedEntity();
    trackedEntity.setUid(TRACKED_ENTITY_UID);
    when(trackedEntityService.getTrackedEntity(TRACKED_ENTITY_UID)).thenReturn(trackedEntity);
  }

  @Test
  void shouldMapWithoutFetchingNullParamsWhenParamsAreNotSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams = EnrollmentOperationParams.EMPTY;

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
            .programUid(program.getUid())
            .build();
    when(trackerAccessManager.canAccess(user, program, orgUnit1)).thenReturn(true);
    when(trackerAccessManager.canAccess(user, program, orgUnit2)).thenReturn(true);

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertContainsOnly(Set.of(orgUnit1, orgUnit2), params.getOrganisationUnits());
  }

  @Test
  void shouldThrowExceptionWhenOrgUnitNotFound() {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder()
            .orgUnitUids(Set.of("JW6BrFd0HLu", ORG_UNIT_2_UID))
            .programUid(PROGRAM_UID)
            .build();

    when(trackerAccessManager.canAccess(user, program, orgUnit2)).thenReturn(true);

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals("Organisation unit does not exist: JW6BrFd0HLu", exception.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenOrgUnitNotInScope() {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitUids(Set.of(ORG_UNIT_1_UID)).build();
    when(trackerAccessManager.canAccess(user, program, orgUnit1)).thenReturn(false);

    Exception exception = assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "User does not have access to organisation unit: " + ORG_UNIT_1_UID,
        exception.getMessage());
  }

  @Test
  void shouldMapProgramWhenProgramUidIsSpecified() throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams requestParams =
        EnrollmentOperationParams.builder().programUid(PROGRAM_UID).build();

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
  void shouldMapTrackedEntityTypeWhenTrackedEntityTypeUidIsSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().trackedEntityTypeUid(TRACKED_ENTITY_TYPE_UID).build();

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
  void shouldMapTrackedEntityWhenTrackedEntityUidIsSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().trackedEntityUid(TRACKED_ENTITY_UID).build();

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
  void shouldMapOrderingParamsWhenOrderingParamsAreSpecified()
      throws BadRequestException, ForbiddenException {
    OrderParam order1 = new OrderParam("field1", SortDirection.ASC);
    OrderParam order2 = new OrderParam("field2", SortDirection.DESC);
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().order(List.of(order1, order2)).build();

    EnrollmentQueryParams params = mapper.map(operationParams);

    assertEquals(
        List.of(
            new OrderParam("field1", SortDirection.ASC),
            new OrderParam("field2", SortDirection.DESC)),
        params.getOrder());
  }

  @Test
  void shouldMapNullOrderingParamsWhenNoOrderingParamsAreSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams requestParams = EnrollmentOperationParams.EMPTY;

    EnrollmentQueryParams params = mapper.map(requestParams);

    assertNull(params.getOrder());
  }
}
