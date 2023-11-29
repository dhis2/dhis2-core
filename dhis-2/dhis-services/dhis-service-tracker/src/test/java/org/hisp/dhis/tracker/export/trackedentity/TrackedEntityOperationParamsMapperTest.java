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
package org.hisp.dhis.tracker.export.trackedentity;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.DhisConvenienceTest.getDate;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.BaseIdentifiableObject;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStatus;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT) // common setup
@ExtendWith(MockitoExtension.class)
class TrackedEntityOperationParamsMapperTest {
  public static final String TEA_1_UID = "TvjwTPToKHO";

  public static final String TEA_2_UID = "cy2oRh2sNr6";

  private static final String ORG_UNIT_1_UID = "lW0T2U7gZUi";

  private static final String ORG_UNIT_2_UID = "TK4KA0IIWqa";

  private static final String PROGRAM_UID = "XhBYIraw7sv";

  private static final String PROGRAM_STAGE_UID = "RpCr2u2pFqw";

  private static final String TRACKED_ENTITY_TYPE_UID = "Dp8baZYrLtr";

  @Mock private CurrentUserService currentUserService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramService programService;

  @Mock private TrackedEntityAttributeService attributeService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private AclService aclService;

  @Mock private TrackedEntityStore trackedEntityStore;

  @InjectMocks private TrackedEntityOperationParamsMapper mapper;

  private User user;

  private Program program;

  private ProgramStage programStage;

  private OrganisationUnit orgUnit1;

  private OrganisationUnit orgUnit2;

  private TrackedEntityType trackedEntityType;

  @BeforeEach
  public void setUp() {
    orgUnit1 = new OrganisationUnit("orgUnit1");
    orgUnit1.setUid(ORG_UNIT_1_UID);
    orgUnit2 = new OrganisationUnit("orgUnit2");
    orgUnit2.setUid(ORG_UNIT_2_UID);
    user = new User();
    user.setOrganisationUnits(Set.of(orgUnit1, orgUnit2));

    when(currentUserService.getCurrentUser()).thenReturn(user);

    when(organisationUnitService.getOrganisationUnit(orgUnit1.getUid())).thenReturn(orgUnit1);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);
    when(organisationUnitService.getOrganisationUnit(orgUnit2.getUid())).thenReturn(orgUnit2);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit2.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(true);

    trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TRACKED_ENTITY_TYPE_UID);
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID))
        .thenReturn(trackedEntityType);

    program = new Program();
    program.setUid(PROGRAM_UID);
    program.setTrackedEntityType(trackedEntityType);
    programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID);
    programStage.setProgram(program);
    program.setProgramStages(Set.of(programStage));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID);

    TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
    tea2.setUid(TEA_2_UID);

    when(attributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);
    when(attributeService.getTrackedEntityAttribute(TEA_2_UID)).thenReturn(tea2);
  }

  @Test
  void testMapping() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, trackedEntityType)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .assignedUserQueryParam(
                new AssignedUserQueryParam(AssignedUserSelectionMode.CURRENT, user, null))
            .orgUnitMode(OrganisationUnitSelectionMode.DESCENDANTS)
            .programStatus(ProgramStatus.ACTIVE)
            .followUp(true)
            .lastUpdatedStartDate(getDate(2019, 1, 1))
            .lastUpdatedEndDate(getDate(2020, 1, 1))
            .lastUpdatedDuration("20")
            .programEnrollmentStartDate(getDate(2019, 5, 5))
            .programEnrollmentEndDate(getDate(2020, 5, 5))
            .trackedEntityTypeUid(TRACKED_ENTITY_TYPE_UID)
            .eventStatus(EventStatus.COMPLETED)
            .eventStartDate(getDate(2019, 7, 7))
            .eventEndDate(getDate(2020, 7, 7))
            .includeDeleted(true)
            .build();

    final TrackedEntityQueryParams params = mapper.map(operationParams);

    assertThat(params.getTrackedEntityType(), is(trackedEntityType));
    assertThat(params.getProgramStatus(), is(ProgramStatus.ACTIVE));
    assertThat(params.getFollowUp(), is(true));
    assertThat(params.getLastUpdatedStartDate(), is(operationParams.getLastUpdatedStartDate()));
    assertThat(params.getLastUpdatedEndDate(), is(operationParams.getLastUpdatedEndDate()));
    assertThat(
        params.getProgramEnrollmentStartDate(),
        is(operationParams.getProgramEnrollmentStartDate()));
    assertThat(
        params.getProgramEnrollmentEndDate(), is(operationParams.getProgramEnrollmentEndDate()));
    assertThat(params.getEventStatus(), is(EventStatus.COMPLETED));
    assertThat(params.getEventStartDate(), is(operationParams.getEventStartDate()));
    assertThat(params.getEventEndDate(), is(operationParams.getEventEndDate()));
    assertThat(
        params.getAssignedUserQueryParam().getMode(), is(AssignedUserSelectionMode.PROVIDED));
    assertThat(params.isIncludeDeleted(), is(true));
  }

  @Test
  void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB()
      throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, trackedEntityType)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .trackedEntityTypeUid(trackedEntityType.getUid())
            .build();

    mapper.map(operationParams);

    verifyNoInteractions(programService);
  }

  @Test
  void testMappingProgramEnrollmentStartDate() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    Date date = parseDate("2022-12-13");
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programEnrollmentStartDate(date)
            .programUid(program.getUid())
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    assertEquals(date, params.getProgramEnrollmentStartDate());
  }

  @Test
  void testMappingProgramEnrollmentEndDate() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    Date date = parseDate("2022-12-13");
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programEnrollmentEndDate(date)
            .programUid(program.getUid())
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    assertEquals(date, params.getProgramEnrollmentEndDate());
  }

  @Test
  void testFilter() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programUid(program.getUid())
            .filters(
                Map.of(
                    TEA_1_UID,
                    List.of(new QueryFilter(QueryOperator.EQ, "2")),
                    TEA_2_UID,
                    List.of(new QueryFilter(QueryOperator.LIKE, "foo"))))
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    Map<TrackedEntityAttribute, List<QueryFilter>> items = params.getFilters();
    assertNotNull(items);
    // mapping to UIDs as the error message by just relying on QueryItem
    // equals() is not helpful
    assertContainsOnly(
        List.of(TEA_1_UID, TEA_2_UID),
        items.keySet().stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toList()));

    // QueryItem equals() does not take the QueryFilter into account so
    // assertContainsOnly alone does not ensure operators and filter value
    // are correct
    // the following block is needed because of that
    // assertion is order independent as the order of QueryItems is not
    // guaranteed
    Map<String, QueryFilter> expectedFilters =
        Map.of(
            TEA_1_UID,
            new QueryFilter(QueryOperator.EQ, "2"),
            TEA_2_UID,
            new QueryFilter(QueryOperator.LIKE, "foo"));
    assertAll(
        items.entrySet().stream()
            .map(
                i ->
                    (Executable)
                        () -> {
                          String uid = i.getKey().getUid();
                          QueryFilter expected = expectedFilters.get(uid);
                          assertEquals(
                              expected,
                              i.getValue().get(0),
                              () -> String.format("QueryFilter mismatch for TEA with UID %s", uid));
                        })
            .collect(Collectors.toList()));
  }

  @Test
  void testFilterWhenTEAHasMultipleFilters() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programUid(program.getUid())
            .filters(
                Map.of(
                    TEA_1_UID,
                    List.of(
                        new QueryFilter(QueryOperator.GT, "10"),
                        new QueryFilter(QueryOperator.LT, "20"))))
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    Map<TrackedEntityAttribute, List<QueryFilter>> items = params.getFilters();
    assertNotNull(items);
    // mapping to UIDs as the error message by just relying on QueryItem
    // equals() is not helpful
    assertContainsOnly(
        List.of(TEA_1_UID),
        items.keySet().stream().map(BaseIdentifiableObject::getUid).collect(Collectors.toList()));

    // QueryItem equals() does not take the QueryFilter into account so
    // assertContainsOnly alone does not ensure operators and filter value
    // are correct
    assertContainsOnly(
        Set.of(new QueryFilter(QueryOperator.GT, "10"), new QueryFilter(QueryOperator.LT, "20")),
        items.values().stream().findAny().get());
  }

  @Test
  void testMappingProgram() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programUid(PROGRAM_UID)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    assertEquals(program, params.getProgram());
  }

  @Test
  void testMappingProgramNotFound() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().programUid("NeU85luyD4w").build();

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals("Program is specified but does not exist: NeU85luyD4w", e.getMessage());
  }

  @Test
  void testMappingProgramStage() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programUid(PROGRAM_UID)
            .programStageUid(PROGRAM_STAGE_UID)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    assertEquals(programStage, params.getProgramStage());
  }

  @Test
  void testMappingProgramStageGivenWithoutProgram() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().programStageUid(PROGRAM_STAGE_UID).build();

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals(
        "Program does not contain the specified programStage: " + PROGRAM_STAGE_UID,
        e.getMessage());
  }

  @Test
  void testMappingProgramStageNotFound() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().programStageUid("NeU85luyD4w").build();

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals(
        "Program does not contain the specified programStage: NeU85luyD4w", e.getMessage());
  }

  @Test
  void testMappingTrackedEntityType() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, trackedEntityType)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .trackedEntityTypeUid(TRACKED_ENTITY_TYPE_UID)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    assertEquals(trackedEntityType, params.getTrackedEntityType());
  }

  @Test
  void testMappingTrackedEntityTypeNotFound() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().trackedEntityTypeUid("NeU85luyD4w").build();

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals(
        "Tracked entity type is specified but does not exist: NeU85luyD4w", e.getMessage());
  }

  @Test
  void testMappingOrgUnits() throws BadRequestException, ForbiddenException {
    when(organisationUnitService.getOrganisationUnitWithChildren(ORG_UNIT_1_UID))
        .thenReturn(List.of(orgUnit1));
    when(organisationUnitService.getOrganisationUnitWithChildren(ORG_UNIT_2_UID))
        .thenReturn(List.of(orgUnit2));
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .programUid(PROGRAM_UID)
            .organisationUnits(Set.of(ORG_UNIT_1_UID, ORG_UNIT_2_UID))
            .user(user)
            .orgUnitMode(DESCENDANTS)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    assertContainsOnly(Set.of(orgUnit1, orgUnit2), params.getOrgUnits());
  }

  @Test
  void testMappingOrgUnitsNotFound() {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .organisationUnits(Set.of("NeU85luyD4w"))
            .programUid(program.getUid())
            .user(user)
            .build();

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertEquals("Organisation unit does not exist: NeU85luyD4w", e.getMessage());
  }

  @Test
  void shouldThrowExceptionWhenOrgUnitNotInScope() {
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(false);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .organisationUnits(Set.of(ORG_UNIT_1_UID))
            .programUid(program.getUid())
            .build();

    ForbiddenException e =
        assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "Organisation unit is not part of the search scope: " + ORG_UNIT_1_UID, e.getMessage());
  }

  @ParameterizedTest
  @EnumSource(value = OrganisationUnitSelectionMode.class)
  void shouldMapParamsWhenOrgUnitNotInScopeButUserIsSuperuser(
      OrganisationUnitSelectionMode orgUnitMode) throws ForbiddenException, BadRequestException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(false);

    User superuser = createUser("ALL");
    superuser.setOrganisationUnits(Set.of(orgUnit1));

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(orgUnitMode)
            .user(superuser)
            .organisationUnits(Set.of(ORG_UNIT_1_UID))
            .programUid(program.getUid())
            .build();

    TrackedEntityQueryParams queryParams = mapper.map(operationParams);
    assertEquals(user, queryParams.getUser());
    assertContainsOnly(
        Set.of(ORG_UNIT_1_UID),
        queryParams.getOrgUnits().stream().map(BaseIdentifiableObject::getUid).toList());
    assertEquals(orgUnitMode, queryParams.getOrgUnitMode());
  }

  @ParameterizedTest
  @EnumSource(value = OrganisationUnitSelectionMode.class)
  void shouldFailWhenOrgUnitNotInScopeAndUserHasSearchInAllAuthority(
      OrganisationUnitSelectionMode orgUnitMode) {
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), user.getTeiSearchOrganisationUnitsWithFallback()))
        .thenReturn(false);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    User user = createUser(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name());

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(orgUnitMode)
            .user(user)
            .organisationUnits(Set.of(ORG_UNIT_1_UID))
            .programUid(program.getUid())
            .build();

    ForbiddenException e =
        assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "Organisation unit is not part of the search scope: " + ORG_UNIT_1_UID, e.getMessage());
  }

  private User createUser(String authority) {
    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Set.of(authority));
    user.setUserRoles(Set.of(userRole));

    return user;
  }

  @Test
  void testMappingAssignedUsers() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programUid(program.getUid())
            .assignedUserQueryParam(
                new AssignedUserQueryParam(
                    AssignedUserSelectionMode.PROVIDED, null, Set.of("IsdLBTOBzMi", "l5ab8q5skbB")))
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    assertContainsOnly(
        Set.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void shouldMapOrderInGivenOrder() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID);
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programUid(program.getUid())
            .orderBy("created", SortDirection.ASC)
            .orderBy(UID.of(TEA_1_UID), SortDirection.ASC)
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(tea1, SortDirection.ASC),
            new Order("createdAtClient", SortDirection.DESC)),
        params.getOrder());
  }

  @Test
  void shouldFailToMapOrderIfUIDIsNotAnAttribute() {
    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID);
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(null);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .programUid(program.getUid())
            .user(user)
            .orderBy(UID.of(TEA_1_UID), SortDirection.ASC)
            .build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertStartsWith("Cannot order by '" + TEA_1_UID, exception.getMessage());
  }

  @Test
  void shouldFailToMapGivenInvalidOrderNameWhichIsAValidUID() {
    // This test case shows that some field names are valid UIDs. Previous stages (web) can thus not
    // rule out all
    // invalid field names and UIDs. Such invalid order values will be caught in this mapper.
    assertTrue(CodeGenerator.isValidUid("lastUpdated"));
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .orderBy(UID.of("lastUpdated"), SortDirection.ASC)
            .programUid(program.getUid())
            .build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertStartsWith("Cannot order by 'lastUpdated'", exception.getMessage());
  }

  @Test
  void shouldFailWhenGlobalSearchAndNoAttributeSpecified() {
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit1, orgUnit2));
    user.setOrganisationUnits(emptySet());
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programUid(PROGRAM_UID)
            .build();

    Exception IllegalQueryException =
        assertThrows(IllegalQueryException.class, () -> mapper.map(operationParams));

    assertEquals(
        "At least 1 attributes should be mentioned in the search criteria.",
        IllegalQueryException.getMessage());
  }

  @Test
  void shouldaFailWhenGlobalSearchAndNoAttributeSpecified() {
    user.setTeiSearchOrganisationUnits(Set.of(orgUnit1, orgUnit2));
    user.setOrganisationUnits(emptySet());
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    program.setMinAttributesRequiredToSearch(0);
    program.setMaxTeiCountToReturn(1);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);

    when(trackedEntityStore.getTrackedEntityCountWithMaxTrackedEntityLimit(any())).thenReturn(100);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .user(user)
            .programUid(PROGRAM_UID)
            .build();

    Exception IllegalQueryException =
        assertThrows(IllegalQueryException.class, () -> mapper.map(operationParams));

    assertEquals("maxteicountreached", IllegalQueryException.getMessage());
  }
}
