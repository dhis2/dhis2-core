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
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
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

  @Mock private OperationsParamsValidator paramsValidator;

  @InjectMocks private EnrollmentOperationParamsMapper mapper;

  private OrganisationUnit orgUnit1;

  private OrganisationUnit orgUnit2;

  private User user;

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

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TRACKED_ENTITY_TYPE_UID);
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID))
        .thenReturn(trackedEntityType);

    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);

    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(TRACKED_ENTITY_UID);
    trackedEntity.setTrackedEntityType(trackedEntityType);
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
  }
}
