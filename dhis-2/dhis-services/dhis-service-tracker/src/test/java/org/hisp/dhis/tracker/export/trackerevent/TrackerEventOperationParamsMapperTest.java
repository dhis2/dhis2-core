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
package org.hisp.dhis.tracker.export.trackerevent;

import static org.hisp.dhis.common.AccessLevel.CLOSED;
import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ALL;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.LIKE;
import static org.hisp.dhis.common.QueryOperator.NULL;
import static org.hisp.dhis.security.Authorities.F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS;
import static org.hisp.dhis.test.TestBase.createOrganisationUnit;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AccessLevel;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.Authorities;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.export.CategoryOptionComboService;
import org.hisp.dhis.tracker.export.FilterJdbcPredicate;
import org.hisp.dhis.tracker.export.FilterJdbcPredicate.Parameter;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.user.UserRole;
import org.hisp.dhis.user.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.SqlParameterValue;

@ExtendWith(MockitoExtension.class)
class TrackerEventOperationParamsMapperTest {
  private static final String DE_1_UID = "OBzmpRP6YUh";

  private static final String DE_2_UID = "KSd4PejqBf9";

  private static final String TEA_1_UID = "TvjwTPToKHO";

  private static final String TEA_2_UID = "cy2oRh2sNr6";

  @Mock private ProgramStageService programStageService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private AclService aclService;

  @Mock private CategoryOptionComboService categoryOptionComboService;

  @Mock private UserService userService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private DataElementService dataElementService;

  @Mock private OperationsParamsValidator paramsValidator;

  @InjectMocks private TrackerEventOperationParamsMapper mapper;

  private UserDetails user;

  private final Map<String, User> userMap = new HashMap<>();

  private TrackerEventOperationParams.TrackerEventOperationParamsBuilder eventBuilder;

  private TrackedEntityAttribute tea1;

  @BeforeEach
  void setUp() {
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    orgUnit.setChildren(Set.of(createOrganisationUnit('B'), createOrganisationUnit('C')));

    User testUser = new User();
    testUser.setUid(CodeGenerator.generateUid());
    testUser.setUsername("test");
    testUser.setOrganisationUnits(Set.of(orgUnit));
    user = UserDetails.fromUser(testUser);

    eventBuilder = TrackerEventOperationParams.builder();

    userMap.put("admin", createUserWithAuthority(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS));
    userMap.put("superuser", createUserWithAuthority(Authorities.ALL));

    tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID);
    tea1.setValueType(ValueType.INTEGER);
  }

  @Test
  void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgramStage() {
    ProgramStage programStage = new ProgramStage();
    programStage.setUid("PlZSBEN7iZd");
    TrackerEventOperationParams operationParams = eventBuilder.programStage(programStage).build();

    when(aclService.canDataRead(user, programStage)).thenReturn(false);
    when(programStageService.getProgramStage("PlZSBEN7iZd")).thenReturn(programStage);

    Exception exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(operationParams, user));
    assertEquals(
        "User has no access to program stage: " + programStage.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenMappingWithUnknownProgramStage() {
    TrackerEventOperationParams operationParams =
        eventBuilder.programStage(UID.of("NeU85luyD4w")).build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));
    assertEquals(
        "Program stage is specified but does not exist: NeU85luyD4w", exception.getMessage());
  }

  @Test
  void
      shouldFailWithForbiddenExceptionWhenUserHasNoAccessToCategoryComboGivenAttributeCategoryOptions() {
    TrackerEventOperationParams operationParams =
        eventBuilder
            .attributeCategoryCombo(UID.of("NeU85luyD4w"))
            .attributeCategoryOptions(UID.of("tqrzUqNMHib", "bT6OSf4qnnk"))
            .build();
    CategoryOptionCombo combo = new CategoryOptionCombo();
    combo.setUid("uid");
    when(categoryOptionComboService.getAttributeOptionCombo(
            "NeU85luyD4w", Set.of("tqrzUqNMHib", "bT6OSf4qnnk"), true))
        .thenReturn(combo);
    when(aclService.canDataRead(any(UserDetails.class), any(CategoryOptionCombo.class)))
        .thenReturn(false);

    Exception exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(operationParams, user));

    assertEquals(
        "User has no access to attribute category option combo: " + combo.getUid(),
        exception.getMessage());
  }

  @Test
  void shouldMapGivenAttributeCategoryOptionsWhenUserHasAccessToCategoryCombo()
      throws BadRequestException, ForbiddenException {
    TrackerEventOperationParams operationParams =
        eventBuilder
            .attributeCategoryCombo(UID.of("NeU85luyD4w"))
            .attributeCategoryOptions(UID.of("tqrzUqNMHib", "bT6OSf4qnnk"))
            .build();
    CategoryOptionCombo combo = new CategoryOptionCombo();
    combo.setUid("uid");
    when(categoryOptionComboService.getAttributeOptionCombo(
            "NeU85luyD4w", Set.of("tqrzUqNMHib", "bT6OSf4qnnk"), true))
        .thenReturn(combo);
    when(aclService.canDataRead(any(UserDetails.class), any(CategoryOptionCombo.class)))
        .thenReturn(true);

    TrackerEventQueryParams queryParams = mapper.map(operationParams, user);

    assertEquals(combo, queryParams.getCategoryOptionCombo());
  }

  @Test
  void testMappingAssignedUser() throws BadRequestException, ForbiddenException {
    TrackerEventOperationParams operationParams =
        eventBuilder
            .assignedUsers(UID.of("IsdLBTOBzMi", "l5ab8q5skbB"))
            .assignedUserMode(AssignedUserSelectionMode.PROVIDED)
            .build();

    TrackerEventQueryParams queryParams = mapper.map(operationParams, user);

    assertContainsOnly(
        UID.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        queryParams.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(
        AssignedUserSelectionMode.PROVIDED, queryParams.getAssignedUserQueryParam().getMode());
  }

  @Test
  void shouldMapAttributeFilters() throws BadRequestException, ForbiddenException {
    TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
    tea2.setUid(TEA_2_UID);
    tea2.setValueType(ValueType.TEXT);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_2_UID)).thenReturn(tea2);

    TrackerEventOperationParams operationParams =
        eventBuilder
            .filterByAttribute(UID.of(TEA_1_UID), List.of(new QueryFilter(QueryOperator.EQ, "2")))
            .filterByAttribute(UID.of(TEA_2_UID), List.of(new QueryFilter(QueryOperator.EQ, "foo")))
            .build();

    TrackerEventQueryParams queryParams = mapper.map(operationParams, user);

    Map<TrackedEntityAttribute, List<FilterJdbcPredicate>> attributes = queryParams.getAttributes();
    assertNotNull(attributes);
    assertEquals(2, attributes.size());

    assertContainsOnly(List.of(tea1, tea2), attributes.keySet());

    List<FilterJdbcPredicate> tea1Filters = attributes.get(tea1);
    assertEquals(1, tea1Filters.size());
    assertQueryFilterValue(
        tea1Filters.get(0), "=", new SqlParameterValue(Types.INTEGER, List.of(2)));

    List<FilterJdbcPredicate> tea2Filters = attributes.get(tea2);
    assertEquals(1, tea2Filters.size());
    assertQueryFilterValue(
        tea2Filters.get(0), "=", new SqlParameterValue(Types.VARCHAR, List.of("foo")));
  }

  @Test
  void shouldFailWhenAttributeInGivenAttributeFilterDoesNotExist() {
    UID filterName = UID.generate();
    TrackerEventOperationParams operationParams =
        eventBuilder.filterByAttribute(filterName).build();

    when(trackedEntityAttributeService.getTrackedEntityAttribute(filterName.getValue()))
        .thenReturn(null);

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));

    assertContains(
        "Tracked entity attribute '" + filterName + "' does not exist", exception.getMessage());
  }

  @Test
  void shouldMapOrderInGivenOrder() throws BadRequestException, ForbiddenException {
    DataElement de1 = new DataElement();
    de1.setUid(DE_1_UID);
    when(dataElementService.getDataElement(DE_1_UID)).thenReturn(de1);
    when(dataElementService.getDataElement(TEA_1_UID)).thenReturn(null);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);

    TrackerEventOperationParams operationParams =
        eventBuilder
            .orderBy("created", SortDirection.ASC)
            .orderBy(UID.of(TEA_1_UID), SortDirection.ASC)
            .orderBy("programStage.uid", SortDirection.DESC)
            .orderBy(UID.of(DE_1_UID), SortDirection.DESC)
            .orderBy("scheduledDate", SortDirection.ASC)
            .build();

    TrackerEventQueryParams params = mapper.map(operationParams, user);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(tea1, SortDirection.ASC),
            new Order("programStage.uid", SortDirection.DESC),
            new Order(de1, SortDirection.DESC),
            new Order("scheduledDate", SortDirection.ASC)),
        params.getOrder());
  }

  @Test
  void shouldFailToMapOrderIfUIDIsNeitherDataElementNorAttribute() {
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(null);

    DataElement de1 = new DataElement();
    de1.setUid(DE_1_UID);
    when(dataElementService.getDataElement(TEA_1_UID)).thenReturn(null);

    TrackerEventOperationParams operationParams =
        eventBuilder.orderBy(UID.of(TEA_1_UID), SortDirection.ASC).build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));
    assertStartsWith("Cannot order by '" + TEA_1_UID, exception.getMessage());
  }

  @Test
  void shouldFailToMapGivenInvalidOrderNameWhichIsAValidUID() {
    // This test case shows that some field names are valid UIDs. Previous stages (web) can thus not
    // rule out all
    // invalid field names and UIDs. Such invalid order values will be caught in this mapper.
    assertTrue(CodeGenerator.isValidUid("lastUpdated"));

    TrackerEventOperationParams operationParams =
        eventBuilder.orderBy(UID.of("lastUpdated"), SortDirection.ASC).build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));
    assertStartsWith("Cannot order by 'lastUpdated'", exception.getMessage());
  }

  @Test
  void shouldMapDataElementFilters() throws BadRequestException, ForbiddenException {
    DataElement de1 = new DataElement();
    de1.setUid(DE_1_UID);
    de1.setValueType(ValueType.INTEGER);
    when(dataElementService.getDataElement(DE_1_UID)).thenReturn(de1);
    DataElement de2 = new DataElement();
    de2.setUid(DE_2_UID);
    de2.setValueType(ValueType.TEXT);
    when(dataElementService.getDataElement(DE_2_UID)).thenReturn(de2);

    TrackerEventOperationParams operationParams =
        eventBuilder
            .filterByDataElement(
                UID.of(DE_1_UID),
                List.of(
                    new QueryFilter(QueryOperator.EQ, "2"), new QueryFilter(QueryOperator.NNULL)))
            .filterByDataElement(
                UID.of(DE_2_UID), List.of(new QueryFilter(QueryOperator.EQ, "foo")))
            .build();

    TrackerEventQueryParams queryParams = mapper.map(operationParams, user);

    Map<DataElement, List<FilterJdbcPredicate>> dataElements = queryParams.getDataElements();
    assertNotNull(dataElements);

    assertEquals(2, dataElements.size());

    assertContainsOnly(List.of(de1, de2), dataElements.keySet());

    List<FilterJdbcPredicate> de1Filters = dataElements.get(de1);
    assertEquals(2, de1Filters.size());
    assertQueryFilterValue(
        de1Filters.get(0), "=", new SqlParameterValue(Types.INTEGER, List.of(2)));
    assertQueryFilterValue(de1Filters.get(1), "is not null", null);

    List<FilterJdbcPredicate> de2Filters = dataElements.get(de2);
    assertEquals(1, de2Filters.size());
    assertQueryFilterValue(
        de2Filters.get(0), "=", new SqlParameterValue(Types.VARCHAR, List.of("foo")));
  }

  @Test
  void shouldFailWhenDataElementInGivenDataElementFilterDoesNotExist() {
    UID filterName = UID.generate();
    TrackerEventOperationParams operationParams =
        eventBuilder.filterByDataElement(filterName).build();

    when(dataElementService.getDataElement(filterName.getValue())).thenReturn(null);

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));

    assertContains("Data element '" + filterName + "' does not exist", exception.getMessage());
  }

  private static Stream<Arguments>
      shouldMapOrgUnitWhenProgramProvidedAndRequestedOrgUnitInSearchScope() {
    return Stream.of(
        arguments(SELECTED, OPEN),
        arguments(SELECTED, CLOSED),
        arguments(CHILDREN, OPEN),
        arguments(CHILDREN, CLOSED),
        arguments(DESCENDANTS, OPEN),
        arguments(DESCENDANTS, CLOSED),
        arguments(ACCESSIBLE, OPEN),
        arguments(ACCESSIBLE, CLOSED),
        arguments(CAPTURE, OPEN),
        arguments(CAPTURE, CLOSED));
  }

  @ParameterizedTest
  @MethodSource
  void shouldMapOrgUnitWhenProgramProvidedAndRequestedOrgUnitInSearchScope(
      OrganisationUnitSelectionMode orgUnitMode, AccessLevel accessLevel)
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setUid(CodeGenerator.generateUid());
    program.setAccessLevel(accessLevel);

    OrganisationUnit searchScopeOrgUnit = createOrganisationUnit('A');
    OrganisationUnit searchScopeChildOrgUnit = createOrganisationUnit('B', searchScopeOrgUnit);

    User user = new User();
    user.setUid(CodeGenerator.generateUid());
    user.setUsername("testB");
    user.setOrganisationUnits(Set.of(createOrganisationUnit('C')));
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(organisationUnitService.getOrganisationUnit(searchScopeChildOrgUnit.getUid()))
        .thenReturn(searchScopeChildOrgUnit);

    TrackerEventOperationParams operationParams =
        eventBuilder
            .program(program)
            .orgUnit(searchScopeChildOrgUnit)
            .orgUnitMode(orgUnitMode)
            .build();

    TrackerEventQueryParams queryParams = mapper.map(operationParams, UserDetails.fromUser(user));
    assertEquals(searchScopeChildOrgUnit, queryParams.getOrgUnit());
  }

  @Test
  void shouldMapOrgUnitWhenModeAllProgramProvidedAndRequestedOrgUnitInSearchScope()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setUid(CodeGenerator.generateUid());
    program.setAccessLevel(OPEN);

    OrganisationUnit searchScopeOrgUnit = createOrganisationUnit('A');
    OrganisationUnit searchScopeChildOrgUnit = createOrganisationUnit('B', searchScopeOrgUnit);

    User user = new User();
    user.setUid(CodeGenerator.generateUid());
    user.setUsername("testB");
    user.setOrganisationUnits(Set.of(createOrganisationUnit('C')));
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Set.of(F_TRACKED_ENTITY_INSTANCE_SEARCH_IN_ALL_ORGUNITS.name()));
    user.setUserRoles(Set.of(userRole));

    when(organisationUnitService.getOrganisationUnit(searchScopeChildOrgUnit.getUid()))
        .thenReturn(searchScopeChildOrgUnit);

    TrackerEventOperationParams operationParams =
        eventBuilder.program(program).orgUnit(searchScopeChildOrgUnit).orgUnitMode(ALL).build();

    TrackerEventQueryParams queryParams = mapper.map(operationParams, UserDetails.fromUser(user));
    assertEquals(searchScopeChildOrgUnit, queryParams.getOrgUnit());
  }

  @ParameterizedTest
  @EnumSource(value = OrganisationUnitSelectionMode.class)
  void shouldFailWhenRequestedOrgUnitOutsideOfSearchScope(
      OrganisationUnitSelectionMode orgUnitMode) {
    OrganisationUnit orgUnit = createOrganisationUnit('A');
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    TrackerEventOperationParams operationParams =
        eventBuilder.orgUnit(orgUnit).orgUnitMode(orgUnitMode).build();

    ForbiddenException exception =
        assertThrows(
            ForbiddenException.class,
            () -> mapper.map(operationParams, UserDetails.fromUser(new User())));
    assertEquals(
        "Organisation unit is not part of your search scope: " + orgUnit.getUid(),
        exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"admin", "superuser"})
  void shouldMapOrgUnitAndModeWhenModeAllAndUserIsAuthorized(String userName)
      throws ForbiddenException, BadRequestException {
    User mappedUser = userMap.get(userName);
    mappedUser.setUid(CodeGenerator.generateUid());
    mappedUser.setUsername(userName);

    TrackerEventOperationParams operationParams = eventBuilder.orgUnitMode(ALL).build();

    TrackerEventQueryParams params = mapper.map(operationParams, UserDetails.fromUser(mappedUser));
    assertNull(params.getOrgUnit());
    assertEquals(ALL, params.getOrgUnitMode());
  }

  @Test
  void shouldFailWhenTeaMinCharactersSetAndNotReached() {
    tea1.setMinCharactersToSearch(2);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);

    TrackerEventOperationParams trackerEventOperationParams =
        TrackerEventOperationParams.builder()
            .filterByAttribute(UID.of(TEA_1_UID), List.of(new QueryFilter(EQ, "1")))
            .build();

    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class, () -> mapper.map(trackerEventOperationParams, user));
    assertContains(
        "At least 2 character(s) should be present in the filter to start a search, but the filter for the tracked entity attribute "
            + TEA_1_UID,
        exception.getMessage());
  }

  @Test
  void shouldFailWhenTeaMinCharactersSetWithMultipleFiltersAndNotAllReachTheMinimum() {
    tea1.setMinCharactersToSearch(2);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);

    TrackerEventOperationParams trackerEventOperationParams =
        TrackerEventOperationParams.builder()
            .filterByAttribute(
                UID.of(TEA_1_UID), List.of(new QueryFilter(EQ, "12"), new QueryFilter(LIKE, "1")))
            .build();

    Exception exception =
        Assertions.assertThrows(
            BadRequestException.class, () -> mapper.map(trackerEventOperationParams, user));
    assertContains(
        "At least 2 character(s) should be present in the filter to start a search, but the filter for the tracked entity attribute "
            + TEA_1_UID,
        exception.getMessage());
  }

  @Test
  void shouldMapTeaWhenTeaMinCharactersSetButOperatorIsUnary()
      throws ForbiddenException, BadRequestException {
    tea1.setMinCharactersToSearch(1);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);

    TrackerEventOperationParams trackerEventOperationParams =
        TrackerEventOperationParams.builder()
            .filterByAttribute(UID.of(TEA_1_UID), List.of(new QueryFilter(NULL)))
            .build();

    TrackerEventQueryParams queryParams = mapper.map(trackerEventOperationParams, user);
    assertContainsOnly(List.of(TEA_1_UID), UID.toUidValueSet(queryParams.getAttributes().keySet()));
  }

  @Test
  void shouldMapTeaWhenTeaMinCharactersSetAndReached()
      throws ForbiddenException, BadRequestException {
    tea1.setMinCharactersToSearch(2);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);

    TrackerEventOperationParams trackerEventOperationParams =
        TrackerEventOperationParams.builder()
            .filterByAttribute(UID.of(TEA_1_UID), List.of(new QueryFilter(EQ, "12")))
            .build();

    TrackerEventQueryParams queryParams = mapper.map(trackerEventOperationParams, user);
    assertContainsOnly(List.of(TEA_1_UID), UID.toUidValueSet(queryParams.getAttributes().keySet()));
  }

  private User createUserWithAuthority(Authorities authority) {
    User user = new User();
    UserRole userRole = new UserRole();
    userRole.setAuthorities(Set.of(authority.name()));
    user.setUserRoles(Set.of(userRole));

    return user;
  }

  private static void assertQueryFilterValue(
      FilterJdbcPredicate actual, String sqlOperator, SqlParameterValue value) {
    assertContains(sqlOperator, actual.getSql());

    if (value != null) {
      assertTrue(actual.getParameter().isPresent(), "expected a getParameter but got none");
      Parameter parameter = actual.getParameter().get();
      assertEquals(value.getSqlType(), parameter.value().getSqlType());
      assertEquals(value.getValue(), parameter.value().getValue());
    } else {
      assertTrue(
          actual.getParameter().isEmpty(),
          () -> "getParameter should be empty but got " + actual.getParameter().get());
    }
  }
}
