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
package org.hisp.dhis.tracker.export.trackedentity;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.QueryOperator.EQ;
import static org.hisp.dhis.common.QueryOperator.GT;
import static org.hisp.dhis.common.QueryOperator.ILIKE;
import static org.hisp.dhis.common.QueryOperator.LIKE;
import static org.hisp.dhis.common.QueryOperator.LT;
import static org.hisp.dhis.common.QueryOperator.NNULL;
import static org.hisp.dhis.common.QueryOperator.NULL;
import static org.hisp.dhis.test.TestBase.getDate;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.util.DateUtils.parseDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.AssignedUserQueryParam;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.IllegalQueryException;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.common.UidObject;
import org.hisp.dhis.common.ValueType;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.setting.SystemSettingsService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityType;
import org.hisp.dhis.trackedentity.TrackedEntityTypeService;
import org.hisp.dhis.tracker.acl.TrackerProgramService;
import org.hisp.dhis.tracker.export.FilterJdbcPredicate;
import org.hisp.dhis.tracker.export.FilterJdbcPredicate.Parameter;
import org.hisp.dhis.tracker.export.OperationsParamsValidator;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserDetails;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.SqlParameterValue;

@MockitoSettings(strictness = Strictness.LENIENT) // common setup
@ExtendWith(MockitoExtension.class)
class TrackedEntityOperationParamsMapperTest {
  public static final UID TEA_1_UID = UID.of("TvjwTPToKHO");

  public static final UID TEA_2_UID = UID.of("cy2oRh2sNr6");

  private static final UID ORG_UNIT_1_UID = UID.of("lW0T2U7gZUi");

  private static final UID ORG_UNIT_2_UID = UID.of("TK4KA0IIWqa");

  private static final UID PROGRAM_UID = UID.of("XhBYIraw7sv");

  private static final UID PROGRAM_STAGE_UID = UID.of("RpCr2u2pFqw");

  private static final UID TRACKED_ENTITY_TYPE_UID = UID.of("Dp8baZYrLtr");

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private TrackerProgramService trackerProgramService;

  @Mock private TrackedEntityAttributeService attributeService;

  @Mock private TrackedEntityTypeService trackedEntityTypeService;

  @Mock private AclService aclService;

  @Mock private HibernateTrackedEntityStore trackedEntityStore;

  @Mock private OperationsParamsValidator paramsValidator;

  @Mock private SystemSettingsService systemSettingsService;

  @InjectMocks private TrackedEntityOperationParamsMapper mapper;

  private UserDetails user;

  private Program program;

  private ProgramStage programStage;

  private OrganisationUnit orgUnit1;

  private OrganisationUnit orgUnit2;

  private TrackedEntityType trackedEntityType;
  private TrackedEntityAttribute tea1;
  private TrackedEntityAttribute tea2;

  @BeforeEach
  public void setUp() {

    orgUnit1 = new OrganisationUnit("orgUnit1");
    orgUnit1.setUid(ORG_UNIT_1_UID.getValue());
    orgUnit2 = new OrganisationUnit("orgUnit2");
    orgUnit2.setUid(ORG_UNIT_2_UID.getValue());
    User testUser = new User();
    testUser.setUid(CodeGenerator.generateUid());
    testUser.setOrganisationUnits(Set.of(orgUnit1, orgUnit2));
    user = UserDetails.fromUser(testUser);

    when(organisationUnitService.getOrganisationUnit(orgUnit1.getUid())).thenReturn(orgUnit1);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit1.getUid(), testUser.getEffectiveSearchOrganisationUnits()))
        .thenReturn(true);
    when(organisationUnitService.getOrganisationUnit(orgUnit2.getUid())).thenReturn(orgUnit2);
    when(organisationUnitService.isInUserHierarchy(
            orgUnit2.getUid(), testUser.getEffectiveSearchOrganisationUnits()))
        .thenReturn(true);

    trackedEntityType = new TrackedEntityType();
    trackedEntityType.setUid(TRACKED_ENTITY_TYPE_UID.getValue());
    when(trackedEntityTypeService.getTrackedEntityType(TRACKED_ENTITY_TYPE_UID.getValue()))
        .thenReturn(trackedEntityType);

    program = new Program();
    program.setUid(PROGRAM_UID.getValue());
    program.setTrackedEntityType(trackedEntityType);
    programStage = new ProgramStage();
    programStage.setUid(PROGRAM_STAGE_UID.getValue());
    programStage.setProgram(program);
    program.setProgramStages(Set.of(programStage));

    when(trackerProgramService.getAccessibleTrackerPrograms()).thenReturn(List.of(program));
    when(aclService.canDataRead(user, program.getTrackedEntityType())).thenReturn(true);

    tea1 = new TrackedEntityAttribute();
    tea1.setValueType(ValueType.INTEGER);
    tea1.setUid(TEA_1_UID.getValue());
    tea1.setMinCharactersToSearch(0);

    tea2 = new TrackedEntityAttribute();
    tea2.setValueType(ValueType.TEXT);
    tea2.setUid(TEA_2_UID.getValue());
    tea2.setMinCharactersToSearch(0);
    tea2.setBlockedSearchOperators(Set.of(EQ));

    when(attributeService.getTrackedEntityAttribute(TEA_1_UID.getValue())).thenReturn(tea1);
    when(attributeService.getTrackedEntityAttribute(TEA_2_UID.getValue())).thenReturn(tea2);
  }

  @Test
  void testMapping() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(any(UserDetails.class), any(TrackedEntityType.class)))
        .thenReturn(true);
    when(paramsValidator.validateTrackedEntityType(UID.of(trackedEntityType), user))
        .thenReturn(trackedEntityType);
    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(trackedEntityType));

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .assignedUserQueryParam(
                new AssignedUserQueryParam(
                    AssignedUserSelectionMode.CURRENT, null, UID.of(user.getUid())))
            .orgUnitMode(OrganisationUnitSelectionMode.DESCENDANTS)
            .enrollmentStatus(EnrollmentStatus.ACTIVE)
            .followUp(true)
            .lastUpdatedStartDate(getDate(2019, 1, 1))
            .lastUpdatedEndDate(getDate(2020, 1, 1))
            .lastUpdatedDuration("20")
            .programEnrollmentStartDate(getDate(2019, 5, 5))
            .programEnrollmentEndDate(getDate(2020, 5, 5))
            .trackedEntityType(TRACKED_ENTITY_TYPE_UID)
            .eventStatus(EventStatus.COMPLETED)
            .eventStartDate(getDate(2019, 7, 7))
            .eventEndDate(getDate(2020, 7, 7))
            .includeDeleted(true)
            .build();

    final TrackedEntityQueryParams params = mapper.map(operationParams, user);

    assertThat(params.getTrackedEntityType(), is(trackedEntityType));
    assertThat(params.getEnrollmentStatus(), is(EnrollmentStatus.ACTIVE));
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
  void testMappingProgramEnrollmentStartDate() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    Date date = parseDate("2022-12-13");
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .programEnrollmentStartDate(date)
            .program(program)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams, user);
    assertEquals(date, params.getProgramEnrollmentStartDate());
  }

  @Test
  void testMappingProgramEnrollmentEndDate() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    Date date = parseDate("2022-12-13");
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .programEnrollmentEndDate(date)
            .program(program)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams, user);

    assertEquals(date, params.getProgramEnrollmentEndDate());
  }

  @Test
  void testFilter() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .program(program)
            .filterBy(TEA_1_UID, List.of(new QueryFilter(QueryOperator.EQ, "2")))
            .filterBy(TEA_2_UID, List.of(new QueryFilter(QueryOperator.LIKE, "foo")))
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams, user);

    Map<TrackedEntityAttribute, List<FilterJdbcPredicate>> attributes = params.getFilters();
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
        tea2Filters.get(0), "like", new SqlParameterValue(Types.VARCHAR, List.of("%foo%")));
  }

  @Test
  void testFilterWhenTEAHasMultipleFilters() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .program(program)
            .filterBy(TEA_1_UID, List.of(new QueryFilter(GT, "10"), new QueryFilter(LT, "20")))
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams, user);

    Map<TrackedEntityAttribute, List<FilterJdbcPredicate>> attributes = params.getFilters();
    assertNotNull(attributes);
    assertEquals(1, attributes.size());

    assertContainsOnly(
        List.of(TEA_1_UID.getValue()),
        attributes.keySet().stream().map(UidObject::getUid).toList());

    List<FilterJdbcPredicate> tea1Filters = attributes.get(tea1);
    assertEquals(2, tea1Filters.size());
    assertQueryFilterValue(
        tea1Filters.get(0), ">", new SqlParameterValue(Types.INTEGER, List.of(10)));
    assertQueryFilterValue(
        tea1Filters.get(1), "<", new SqlParameterValue(Types.INTEGER, List.of(20)));
  }

  @Test
  void testMappingProgram() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().orgUnitMode(ACCESSIBLE).program(PROGRAM_UID).build();

    TrackedEntityQueryParams params = mapper.map(operationParams, user);

    assertEquals(program, params.getEnrolledInTrackerProgram());
  }

  @Test
  void testMappingProgramStage() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .program(PROGRAM_UID)
            .programStage(PROGRAM_STAGE_UID)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams, user);

    assertEquals(programStage, params.getProgramStage());
  }

  @Test
  void testMappingProgramStageGivenWithoutProgram() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().programStage(PROGRAM_STAGE_UID).build();

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));
    assertEquals(
        "Program does not contain the specified programStage: " + PROGRAM_STAGE_UID,
        e.getMessage());
  }

  @Test
  void testMappingProgramStageNotFound() {
    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().programStage(UID.of("NeU85luyD4w")).build();

    BadRequestException e =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));
    assertEquals(
        "Program does not contain the specified programStage: NeU85luyD4w", e.getMessage());
  }

  @Test
  void testMappingAssignedUsers() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .program(program)
            .assignedUserQueryParam(
                new AssignedUserQueryParam(
                    AssignedUserSelectionMode.PROVIDED,
                    UID.of("IsdLBTOBzMi", "l5ab8q5skbB"),
                    UID.of(user.getUid())))
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams, user);

    assertContainsOnly(
        UID.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void shouldMapOrderInGivenOrder() throws BadRequestException, ForbiddenException {
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID.getValue());
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID.getValue())).thenReturn(tea1);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .program(program)
            .orderBy("created", SortDirection.ASC)
            .orderBy(TEA_1_UID, SortDirection.ASC)
            .orderBy("createdAtClient", SortDirection.DESC)
            .build();

    TrackedEntityQueryParams params = mapper.map(operationParams, user);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(tea1, SortDirection.ASC),
            new Order("createdAtClient", SortDirection.DESC)),
        params.getOrder());
  }

  @Test
  void shouldFailToMapOrderIfUIDIsNotAnAttribute() throws ForbiddenException, BadRequestException {
    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID.getValue());
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID.getValue())).thenReturn(null);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .program(program)
            .orderBy(TEA_1_UID, SortDirection.ASC)
            .build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));
    assertStartsWith("Cannot order by '" + TEA_1_UID, exception.getMessage());
  }

  @Test
  void shouldFailToMapGivenInvalidOrderNameWhichIsAValidUID()
      throws ForbiddenException, BadRequestException {
    // This test case shows that some field names are valid UIDs. Previous stages (web) can thus not
    // rule out all
    // invalid field names and UIDs. Such invalid order values will be caught in this mapper.
    assertTrue(CodeGenerator.isValidUid("lastUpdated"));
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), user)).thenReturn(program);

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder()
            .orgUnitMode(ACCESSIBLE)
            .orderBy(UID.of("lastUpdated"), SortDirection.ASC)
            .program(program)
            .build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams, user));
    assertStartsWith("Cannot order by 'lastUpdated'", exception.getMessage());
  }

  @Test
  void shouldFailWhenGlobalSearchAndNoAttributeSpecified()
      throws ForbiddenException, BadRequestException {
    User userWithOrgUnits = new User();
    userWithOrgUnits.setTeiSearchOrganisationUnits(Set.of(orgUnit1, orgUnit2));
    userWithOrgUnits.setOrganisationUnits(emptySet());
    UserDetails currentUserWithOrgUnits = UserDetails.fromUser(userWithOrgUnits);

    when(aclService.canDataRead(currentUserWithOrgUnits, program)).thenReturn(true);
    when(paramsValidator.validateTrackerProgram(UID.of(program), currentUserWithOrgUnits))
        .thenReturn(program);
    when(organisationUnitService.getOrganisationUnitsByUid(
            Set.of(orgUnit1.getUid(), orgUnit2.getUid())))
        .thenReturn(List.of(orgUnit1, orgUnit2));

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().orgUnitMode(ACCESSIBLE).program(PROGRAM_UID).build();

    Exception exception =
        assertThrows(
            IllegalQueryException.class,
            () -> mapper.map(operationParams, currentUserWithOrgUnits));

    assertEquals(
        "At least 1 attributes should be mentioned in the search criteria.",
        exception.getMessage());
  }

  @Test
  void shouldFailWhenUserHasNoAccessToAnyTrackedEntityType() {
    User userWithOrgUnits = new User();
    userWithOrgUnits.setTeiSearchOrganisationUnits(Set.of(orgUnit1, orgUnit2));
    userWithOrgUnits.setOrganisationUnits(emptySet());
    UserDetails currentUserWithOrgUnits = UserDetails.fromUser(userWithOrgUnits);
    when(aclService.canDataRead(currentUserWithOrgUnits, program)).thenReturn(true);
    program.setMinAttributesRequiredToSearch(0);
    program.setMaxTeiCountToReturn(1);
    when(trackerProgramService.getAccessibleTrackerPrograms()).thenReturn(List.of(program));

    TrackedEntityOperationParams operationParams =
        TrackedEntityOperationParams.builder().orgUnitMode(ACCESSIBLE).build();

    Exception exception =
        assertThrows(
            ForbiddenException.class, () -> mapper.map(operationParams, currentUserWithOrgUnits));

    assertEquals("User has no access to any Tracked Entity Type", exception.getMessage());
  }

  @Test
  void shouldFailWhenTeaMinCharactersSetAndNotReached()
      throws ForbiddenException, BadRequestException {
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID.getValue())).thenReturn(tea1);
    when(aclService.canDataRead(any(UserDetails.class), any(TrackedEntityType.class)))
        .thenReturn(true);
    when(paramsValidator.validateTrackedEntityType(UID.of(trackedEntityType), user))
        .thenReturn(trackedEntityType);
    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(trackedEntityType));
    tea1.setMinCharactersToSearch(2);

    TrackedEntityOperationParams trackedEntityOperationParams =
        TrackedEntityOperationParams.builder()
            .trackedEntityType(trackedEntityType)
            .filterBy(TEA_1_UID, List.of(new QueryFilter(EQ, "1")))
            .build();

    Exception exception =
        Assertions.assertThrows(
            IllegalQueryException.class, () -> mapper.map(trackedEntityOperationParams, user));
    assertContains(
        "At least 2 character(s) should be present in the filter to start a search, but the filter for the TEA "
            + TEA_1_UID,
        exception.getMessage());
  }

  @Test
  void shouldFailWhenTeaMinCharactersSetWithMultipleFiltersAndNotAllReachTheMinimum()
      throws ForbiddenException, BadRequestException {
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID.getValue())).thenReturn(tea1);
    when(aclService.canDataRead(any(UserDetails.class), any(TrackedEntityType.class)))
        .thenReturn(true);
    when(paramsValidator.validateTrackedEntityType(UID.of(trackedEntityType), user))
        .thenReturn(trackedEntityType);
    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(trackedEntityType));
    tea1.setMinCharactersToSearch(2);

    TrackedEntityOperationParams trackedEntityOperationParams =
        TrackedEntityOperationParams.builder()
            .trackedEntityType(trackedEntityType)
            .filterBy(TEA_1_UID, List.of(new QueryFilter(EQ, "12"), new QueryFilter(LIKE, "1")))
            .build();

    Exception exception =
        Assertions.assertThrows(
            IllegalQueryException.class, () -> mapper.map(trackedEntityOperationParams, user));
    assertContains(
        "At least 2 character(s) should be present in the filter to start a search, but the filter for the TEA "
            + TEA_1_UID,
        exception.getMessage());
  }

  @Test
  void shouldMapTeaWhenTeaMinCharactersSetAndNotReachedButOperatorIsUnary()
      throws ForbiddenException, BadRequestException {
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID.getValue())).thenReturn(tea1);
    when(aclService.canDataRead(any(UserDetails.class), any(TrackedEntityType.class)))
        .thenReturn(true);
    when(paramsValidator.validateTrackedEntityType(UID.of(trackedEntityType), user))
        .thenReturn(trackedEntityType);
    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(trackedEntityType));
    tea1.setMinCharactersToSearch(1);

    TrackedEntityOperationParams trackedEntityOperationParams =
        TrackedEntityOperationParams.builder()
            .trackedEntityType(trackedEntityType)
            .filterBy(TEA_1_UID, List.of(new QueryFilter(NULL)))
            .build();

    TrackedEntityQueryParams queryParams = mapper.map(trackedEntityOperationParams, user);
    assertContainsOnly(
        List.of(TEA_1_UID.getValue()), UID.toUidValueSet(queryParams.getFilters().keySet()));
  }

  @Test
  void shouldMapTeaWhenTeaMinCharactersSetAndReached()
      throws ForbiddenException, BadRequestException {
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID.getValue())).thenReturn(tea1);
    when(aclService.canDataRead(any(UserDetails.class), any(TrackedEntityType.class)))
        .thenReturn(true);
    when(paramsValidator.validateTrackedEntityType(UID.of(trackedEntityType), user))
        .thenReturn(trackedEntityType);
    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(trackedEntityType));
    tea1.setMinCharactersToSearch(2);

    TrackedEntityOperationParams trackedEntityOperationParams =
        TrackedEntityOperationParams.builder()
            .trackedEntityType(trackedEntityType)
            .filterBy(TEA_1_UID, List.of(new QueryFilter(EQ, "12")))
            .build();

    TrackedEntityQueryParams queryParams = mapper.map(trackedEntityOperationParams, user);
    assertContainsOnly(
        List.of(TEA_1_UID.getValue()), UID.toUidValueSet(queryParams.getFilters().keySet()));
  }

  @Test
  void shouldMapAttributeFiltersWhenOperatorsAreNotBlocked()
      throws ForbiddenException, BadRequestException {
    when(attributeService.getTrackedEntityAttribute(TEA_2_UID.getValue())).thenReturn(tea2);
    when(aclService.canDataRead(any(UserDetails.class), any(TrackedEntityType.class)))
        .thenReturn(true);
    when(paramsValidator.validateTrackedEntityType(UID.of(trackedEntityType), user))
        .thenReturn(trackedEntityType);
    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(trackedEntityType));

    TrackedEntityOperationParams trackedEntityOperationParams =
        TrackedEntityOperationParams.builder()
            .trackedEntityType(trackedEntityType)
            .filterBy(
                TEA_2_UID,
                List.of(
                    new QueryFilter(LIKE, "12"),
                    new QueryFilter(NNULL),
                    new QueryFilter(ILIKE, "0")))
            .build();

    TrackedEntityQueryParams queryParams = mapper.map(trackedEntityOperationParams, user);
    assertContainsOnly(
        List.of(TEA_2_UID.getValue()), UID.toUidValueSet(queryParams.getFilters().keySet()));
  }

  @Test
  void shouldNotMapAttributeFiltersWhenOperatorsAreBlocked()
      throws ForbiddenException, BadRequestException {
    when(attributeService.getTrackedEntityAttribute(TEA_2_UID.getValue())).thenReturn(tea2);
    when(aclService.canDataRead(any(UserDetails.class), any(TrackedEntityType.class)))
        .thenReturn(true);
    when(paramsValidator.validateTrackedEntityType(UID.of(trackedEntityType), user))
        .thenReturn(trackedEntityType);
    when(trackedEntityTypeService.getAllTrackedEntityType()).thenReturn(List.of(trackedEntityType));

    TrackedEntityOperationParams trackedEntityOperationParams =
        TrackedEntityOperationParams.builder()
            .trackedEntityType(trackedEntityType)
            .filterBy(TEA_2_UID, List.of(new QueryFilter(LIKE, "12"), new QueryFilter(EQ, "0")))
            .build();

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(trackedEntityOperationParams, user));
    assertStartsWith(
        "Operators [EQ] are blocked for attribute 'cy2oRh2sNr6'", exception.getMessage());
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
