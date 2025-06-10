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
package org.hisp.dhis.tracker.export.enrollment;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.IdentifiableObject;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.acl.TrackerProgramService;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserService;
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

  private static final UID ORG_UNIT_1_UID = UID.generate();

  private static final UID ORG_UNIT_2_UID = UID.generate();

  private static final UID PROGRAM_UID = UID.generate();

  private static final UID TRACKED_ENTITY_TYPE_UID = UID.generate();

  private static final UID TRACKED_ENTITY_UID = UID.generate();

  @Mock private UserService userService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramService programService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private OperationsParamsValidator paramsValidator;

  @Mock private TrackerProgramService trackerProgramService;

  @InjectMocks private EnrollmentOperationParamsMapper mapper;

  private OrganisationUnit orgUnit1;

  private OrganisationUnit orgUnit2;

  private UserDetails user;

  @BeforeEach
  void setUp() throws ForbiddenException, BadRequestException {
    User testUser = new User();
    testUser.setUsername("admin");

    orgUnit1 = createOrganisationUnit('A');
    orgUnit1.setUid(ORG_UNIT_1_UID.getValue());
    when(organisationUnitService.getOrganisationUnit(orgUnit1.getUid())).thenReturn(orgUnit1);
    orgUnit2 = createOrganisationUnit('B');
    orgUnit2.setUid(ORG_UNIT_2_UID.getValue());
    orgUnit2.setParent(orgUnit1);
    orgUnit1.setChildren(Set.of(orgUnit2));
    when(organisationUnitService.getOrganisationUnit(orgUnit2.getUid())).thenReturn(orgUnit2);
    when(organisationUnitService.getOrganisationUnitsByUid(
            Set.of(orgUnit2.getUid(), orgUnit1.getUid())))
        .thenReturn(List.of(orgUnit1, orgUnit2));
    when(organisationUnitService.getOrganisationUnitsByUid(Set.of(orgUnit2.getUid())))
        .thenReturn(List.of(orgUnit2));
    when(organisationUnitService.getOrganisationUnitsByUid(Set.of(orgUnit1.getUid())))
        .thenReturn(List.of(orgUnit1));
    when(organisationUnitService.getOrganisationUnit(orgUnit2.getUid())).thenReturn(orgUnit2);

    testUser.setTeiSearchOrganisationUnits(Set.of(orgUnit1, orgUnit2));
    testUser.setOrganisationUnits(Set.of(orgUnit2));
    user = UserDetails.fromUser(testUser);

    TrackedEntityType trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TRACKED_ENTITY_TYPE_UID.getValue());
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID.getValue()))
        .thenReturn(trackedEntityType);

    Program program = new Program();
    program.setUid(PROGRAM_UID.getValue());
    program.setTrackedEntityType(trackedEntityType);
    when(programService.getProgram(PROGRAM_UID.getValue())).thenReturn(program);

    TrackedEntity trackedEntity = new TrackedEntity();
    trackedEntity.setUid(TRACKED_ENTITY_UID.getValue());
    trackedEntity.setTrackedEntityType(trackedEntityType);

    when(paramsValidator.validateTrackerProgram(PROGRAM_UID, user)).thenReturn(program);
    when(paramsValidator.validateTrackedEntityType(TRACKED_ENTITY_TYPE_UID, user))
        .thenReturn(trackedEntityType);
    when(paramsValidator.validateTrackedEntity(TRACKED_ENTITY_UID, user, false))
        .thenReturn(trackedEntity);
    when(paramsValidator.validateOrgUnits(Set.of(ORG_UNIT_1_UID, ORG_UNIT_2_UID), user))
        .thenReturn(Set.of(orgUnit1, orgUnit2));
  }

  @Test
  void shouldMapWithoutFetchingNullParamsWhenParamsAreNotSpecified()
      throws BadRequestException, ForbiddenException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(ACCESSIBLE).build();

    mapper.map(operationParams, user);

    verifyNoInteractions(programService);
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

    EnrollmentQueryParams params = mapper.map(operationParams, user);

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

    EnrollmentQueryParams params = mapper.map(operationParams, user);

    assertIsEmpty(params.getOrder());
  }

  @Test
  void shouldMapDescendantsOrgUnitModeWhenAccessibleProvided()
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(ACCESSIBLE).build();

    EnrollmentQueryParams params = mapper.map(operationParams, user);

    assertEquals(DESCENDANTS, params.getOrganisationUnitMode());
    assertEquals(
        user.getUserEffectiveSearchOrgUnitIds(),
        params.getOrganisationUnits().stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet()));
  }

  @Test
  void shouldMapDescendantsOrgUnitModeWhenCaptureProvided()
      throws ForbiddenException, BadRequestException {
    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnitMode(CAPTURE).build();

    EnrollmentQueryParams params = mapper.map(operationParams, user);

    assertEquals(DESCENDANTS, params.getOrganisationUnitMode());
    assertEquals(
        user.getUserOrgUnitIds(),
        params.getOrganisationUnits().stream()
            .map(IdentifiableObject::getUid)
            .collect(Collectors.toSet()));
  }

  @Test
  void shouldMapChildrenOrgUnitModeWhenChildrenProvided()
      throws ForbiddenException, BadRequestException {

    EnrollmentOperationParams operationParams =
        EnrollmentOperationParams.builder().orgUnits(orgUnit1).orgUnitMode(CHILDREN).build();

    EnrollmentQueryParams params = mapper.map(operationParams, user);

    assertEquals(CHILDREN, params.getOrganisationUnitMode());
  }
}
