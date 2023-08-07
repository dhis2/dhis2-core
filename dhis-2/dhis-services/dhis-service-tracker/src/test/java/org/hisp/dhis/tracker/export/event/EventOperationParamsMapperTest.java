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
package org.hisp.dhis.tracker.export.event;

import static org.hisp.dhis.common.AccessLevel.OPEN;
import static org.hisp.dhis.common.AccessLevel.PROTECTED;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CHILDREN;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.DESCENDANTS;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.utils.Assertions.assertContains;
import static org.hisp.dhis.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.category.CategoryOptionCombo;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.CodeGenerator;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.trackedentity.TrackedEntityService;
import org.hisp.dhis.trackedentity.TrackerAccessManager;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.user.CurrentUserService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.webapi.controller.event.mapper.SortDirection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventOperationParamsMapperTest {
  private static final String DE_1_UID = "OBzmpRP6YUh";

  private static final String DE_2_UID = "KSd4PejqBf9";

  private static final String TEA_1_UID = "TvjwTPToKHO";

  private static final String TEA_2_UID = "cy2oRh2sNr6";

  private static final String PROGRAM_UID = "PlZSBEN7iZd";

  @Mock private ProgramService programService;

  @Mock private ProgramStageService programStageService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private AclService aclService;

  @Mock private CategoryOptionComboService categoryOptionComboService;

  @Mock private CurrentUserService currentUserService;

  @Mock private TrackedEntityAttributeService trackedEntityAttributeService;

  @Mock private DataElementService dataElementService;

  @Mock private TrackerAccessManager trackerAccessManager;

  @InjectMocks private EventOperationParamsMapper mapper;

  private ProgramStage programStage;

  private User user;

  private OrganisationUnit orgUnit;

  private final String orgUnitId = "orgUnitId";

  private EventOperationParams.EventOperationParamsBuilder eventBuilder =
      EventOperationParams.builder();

  private final List<OrganisationUnit> orgUnitDescendants =
      List.of(
          createOrgUnit("orgUnit1", "uid1"),
          createOrgUnit("orgUnit2", "uid2"),
          createOrgUnit("captureScopeOrgUnit", "uid3"),
          createOrgUnit("searchScopeOrgUnit", "uid4"));

  @BeforeEach
  public void setUp() {
    orgUnit = createOrgUnit("orgUnit", orgUnitId);
    orgUnit.setChildren(
        Set.of(
            createOrgUnit("captureScopeChild", "captureScopeChildUid"),
            createOrgUnit("searchScopeChild", "searchScopeChildUid")));

    user = new User();
    user.setOrganisationUnits(Set.of(orgUnit));
    when(currentUserService.getCurrentUser()).thenReturn(user);

    // By default set to ACCESSIBLE for tests that don't set an orgUnit. The orgUnitMode needs to be
    // set because its validation is in the EventRequestParamsMapper.
    eventBuilder = eventBuilder.orgUnitMode(ACCESSIBLE);
  }

  @Test
  void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgramStage() {
    programStage = new ProgramStage();
    programStage.setUid("PlZSBEN7iZd");
    EventOperationParams eventOperationParams =
        eventBuilder.programStageUid(programStage.getUid()).build();

    when(aclService.canDataRead(user, programStage)).thenReturn(false);
    when(programStageService.getProgramStage("PlZSBEN7iZd")).thenReturn(programStage);

    Exception exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventOperationParams));
    assertEquals(
        "User has no access to program stage: " + programStage.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenTrackedEntityDoesNotExist() {
    programStage = new ProgramStage();
    programStage.setUid("PlZSBEN7iZd");
    EventOperationParams eventOperationParams =
        eventBuilder.programStageUid(programStage.getUid()).trackedEntityUid("qnR1RK4cTIZ").build();

    when(programStageService.getProgramStage("PlZSBEN7iZd")).thenReturn(programStage);
    when(aclService.canDataRead(user, programStage)).thenReturn(true);
    when(trackedEntityService.getTrackedEntity("qnR1RK4cTIZ")).thenReturn(null);

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(eventOperationParams));
    assertStartsWith(
        "Tracked entity is specified but does not exist: "
            + eventOperationParams.getTrackedEntityUid(),
        exception.getMessage());
  }

  @Test
  void shouldFailWithForbiddenExceptionWhenUserHasNoAccessToProgram() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    EventOperationParams eventOperationParams = eventBuilder.programUid(program.getUid()).build();

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(false);

    Exception exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventOperationParams));
    assertEquals("User has no access to program: " + program.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenMappingWithUnknownProgramStage() {
    EventOperationParams eventOperationParams =
        EventOperationParams.builder().programStageUid("NeU85luyD4w").build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(eventOperationParams));
    assertEquals(
        "Program stage is specified but does not exist: NeU85luyD4w", exception.getMessage());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenMappingWithUnknownProgram() {
    EventOperationParams eventOperationParams =
        EventOperationParams.builder().programUid("NeU85luyD4w").build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(eventOperationParams));
    assertEquals("Program is specified but does not exist: NeU85luyD4w", exception.getMessage());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenMappingCriteriaWithUnknownOrgUnit() {
    EventOperationParams eventOperationParams =
        EventOperationParams.builder().orgUnitUid("NeU85luyD4w").build();
    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(null);

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(eventOperationParams));

    assertEquals("Org unit is specified but does not exist: NeU85luyD4w", exception.getMessage());
  }

  @Test
  void
      shouldFailWithForbiddenExceptionWhenUserHasNoAccessToCategoryComboGivenAttributeCategoryOptions() {
    EventOperationParams eventOperationParams =
        eventBuilder
            .attributeCategoryCombo("NeU85luyD4w")
            .attributeCategoryOptions(Set.of("tqrzUqNMHib", "bT6OSf4qnnk"))
            .build();
    CategoryOptionCombo combo = new CategoryOptionCombo();
    combo.setUid("uid");
    when(categoryOptionComboService.getAttributeOptionCombo(
            "NeU85luyD4w", Set.of("tqrzUqNMHib", "bT6OSf4qnnk"), true))
        .thenReturn(combo);
    when(aclService.canDataRead(any(User.class), any(CategoryOptionCombo.class))).thenReturn(false);

    Exception exception =
        assertThrows(ForbiddenException.class, () -> mapper.map(eventOperationParams));

    assertEquals(
        "User has no access to attribute category option combo: " + combo.getUid(),
        exception.getMessage());
  }

  @Test
  void shouldMapGivenAttributeCategoryOptionsWhenUserHasAccessToCategoryCombo()
      throws BadRequestException, ForbiddenException {
    EventOperationParams operationParams =
        eventBuilder
            .attributeCategoryCombo("NeU85luyD4w")
            .attributeCategoryOptions(Set.of("tqrzUqNMHib", "bT6OSf4qnnk"))
            .build();
    CategoryOptionCombo combo = new CategoryOptionCombo();
    combo.setUid("uid");
    when(categoryOptionComboService.getAttributeOptionCombo(
            "NeU85luyD4w", Set.of("tqrzUqNMHib", "bT6OSf4qnnk"), true))
        .thenReturn(combo);
    when(aclService.canDataRead(any(User.class), any(CategoryOptionCombo.class))).thenReturn(true);

    EventSearchParams searchParams = mapper.map(operationParams);

    assertEquals(combo, searchParams.getCategoryOptionCombo());
  }

  @Test
  void testMappingAssignedUser() throws BadRequestException, ForbiddenException {
    EventOperationParams operationParams =
        eventBuilder
            .assignedUsers(Set.of("IsdLBTOBzMi", "l5ab8q5skbB"))
            .assignedUserMode(AssignedUserSelectionMode.PROVIDED)
            .build();

    EventSearchParams searchParams = mapper.map(operationParams);

    assertContainsOnly(
        Set.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        searchParams.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(
        AssignedUserSelectionMode.PROVIDED, searchParams.getAssignedUserQueryParam().getMode());
  }

  @Test
  void shouldMapAttributeFilters() throws BadRequestException, ForbiddenException {
    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID);
    TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
    tea2.setUid(TEA_2_UID);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_2_UID)).thenReturn(tea2);

    EventOperationParams operationParams =
        eventBuilder
            .attributeFilters(
                Map.of(
                    TEA_1_UID,
                    List.of(new QueryFilter(QueryOperator.EQ, "2")),
                    TEA_2_UID,
                    List.of(new QueryFilter(QueryOperator.LIKE, "foo"))))
            .build();

    EventSearchParams searchParams = mapper.map(operationParams);

    Map<TrackedEntityAttribute, List<QueryFilter>> attributes = searchParams.getAttributes();
    assertNotNull(attributes);
    Map<TrackedEntityAttribute, List<QueryFilter>> expected =
        Map.of(
            tea1,
            List.of(new QueryFilter(QueryOperator.EQ, "2")),
            tea2,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo")));
    assertEquals(expected, attributes);
  }

  @Test
  void shouldFailWhenAttributeInGivenAttributeFilterDoesNotExist() {
    String filterName = "filter";
    EventOperationParams operationParams =
        eventBuilder.attributeFilters(Map.of(filterName, List.of())).build();

    when(trackedEntityAttributeService.getTrackedEntityAttribute(filterName)).thenReturn(null);

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));

    assertContains(
        "Tracked entity attribute '" + filterName + "' does not exist", exception.getMessage());
  }

  @Test
  void shouldMapOrderInGivenOrder() throws BadRequestException, ForbiddenException {
    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(tea1);

    DataElement de1 = new DataElement();
    de1.setUid(DE_1_UID);
    when(dataElementService.getDataElement(DE_1_UID)).thenReturn(de1);
    when(dataElementService.getDataElement(TEA_1_UID)).thenReturn(null);

    EventOperationParams operationParams =
        eventBuilder
            .orderBy("created", SortDirection.ASC)
            .orderBy(UID.of(TEA_1_UID), SortDirection.ASC)
            .orderBy("programStage.uid", SortDirection.DESC)
            .orderBy(UID.of(DE_1_UID), SortDirection.DESC)
            .orderBy("dueDate", SortDirection.ASC)
            .build();

    EventSearchParams params = mapper.map(operationParams);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(tea1, SortDirection.ASC),
            new Order("programStage.uid", SortDirection.DESC),
            new Order(de1, SortDirection.DESC),
            new Order("dueDate", SortDirection.ASC)),
        params.getOrder());
  }

  @Test
  void shouldFailToMapOrderIfUIDIsNeitherDataElementNorAttribute() {
    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID);
    when(trackedEntityAttributeService.getTrackedEntityAttribute(TEA_1_UID)).thenReturn(null);

    DataElement de1 = new DataElement();
    de1.setUid(DE_1_UID);
    when(dataElementService.getDataElement(TEA_1_UID)).thenReturn(null);

    EventOperationParams operationParams =
        eventBuilder.orderBy(UID.of(TEA_1_UID), SortDirection.ASC).build();

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

    EventOperationParams operationParams =
        eventBuilder.orderBy(UID.of("lastUpdated"), SortDirection.ASC).build();

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));
    assertStartsWith("Cannot order by 'lastUpdated'", exception.getMessage());
  }

  @Test
  void shouldMapDataElementFilters() throws BadRequestException, ForbiddenException {
    DataElement de1 = new DataElement();
    de1.setUid(DE_1_UID);
    when(dataElementService.getDataElement(DE_1_UID)).thenReturn(de1);
    DataElement de2 = new DataElement();
    de2.setUid(DE_2_UID);
    when(dataElementService.getDataElement(DE_2_UID)).thenReturn(de2);

    EventOperationParams operationParams =
        eventBuilder
            .dataElementFilters(
                Map.of(
                    DE_1_UID,
                    List.of(new QueryFilter(QueryOperator.EQ, "2")),
                    DE_2_UID,
                    List.of(new QueryFilter(QueryOperator.LIKE, "foo"))))
            .build();

    EventSearchParams searchParams = mapper.map(operationParams);

    Map<DataElement, List<QueryFilter>> dataElements = searchParams.getDataElements();
    assertNotNull(dataElements);
    Map<DataElement, List<QueryFilter>> expected =
        Map.of(
            de1,
            List.of(new QueryFilter(QueryOperator.EQ, "2")),
            de2,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo")));
    assertEquals(expected, dataElements);
  }

  @Test
  void shouldFailWhenDataElementInGivenDataElementFilterDoesNotExist() {
    String filterName = "filter";
    EventOperationParams operationParams =
        eventBuilder.dataElementFilters(Map.of(filterName, List.of())).build();

    when(dataElementService.getDataElement(filterName)).thenReturn(null);

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(operationParams));

    assertContains("Data element '" + filterName + "' does not exist", exception.getMessage());
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenProgramProtectedAndOuModeDescendants()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(PROTECTED);
    program.setUid(PROGRAM_UID);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("captureScopeOrgUnit", "uid3");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(DESCENDANTS)
            .build();

    EventSearchParams searchParams = mapper.map(operationParams);

    assertContainsOnly(List.of(captureScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenProgramOpenAndOuModeDescendants()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(DESCENDANTS)
            .build();

    EventSearchParams searchParams = mapper.map(operationParams);

    assertContainsOnly(List.of(searchScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldFailWhenProgramProtectedAndOuModeDescendantsAndUserHasNoAccessToCaptureScopeOrgUnit() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setAccessLevel(PROTECTED);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(DESCENDANTS)
            .build();

    ForbiddenException exception =
        Assertions.assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramOpenAndOuModeDescendantsAndUserHasNoAccessToSearchScopeOrgUnit() {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(DESCENDANTS)
            .build();

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(organisationUnitService.getOrganisationUnitWithChildren(orgUnitId))
        .thenReturn(orgUnitDescendants);

    ForbiddenException exception =
        Assertions.assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenProgramProtectedAndOuModeChildren()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setAccessLevel(PROTECTED);
    OrganisationUnit captureScopeOrgUnit =
        createOrgUnit("captureScopeChild", "captureScopeChildUid");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(CHILDREN)
            .build();

    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventSearchParams searchParams = mapper.map(operationParams);

    assertContainsOnly(List.of(captureScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenProgramOpenAndOuModeChildren()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeChild", "searchScopeChildUid");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(CHILDREN)
            .build();

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventSearchParams searchParams = mapper.map(operationParams);

    assertContainsOnly(List.of(searchScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldFailWhenProgramProtectedAndOuModeChildrenAndUserHasNoAccessToCaptureScopeOrgUnit() {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    program.setAccessLevel(PROTECTED);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(CHILDREN)
            .build();

    ForbiddenException exception =
        Assertions.assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldFailWhenProgramOpenAndOuModeChildrenAndUserHasNoAccessToSearchScopeOrgUnit() {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("made up org unit", "made up uid");
    User user = new User();
    user.setTeiSearchOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(CHILDREN)
            .build();

    ForbiddenException exception =
        Assertions.assertThrows(ForbiddenException.class, () -> mapper.map(operationParams));
    assertEquals(
        "User does not have access to orgUnit: " + orgUnit.getUid(), exception.getMessage());
  }

  @Test
  void shouldMapCaptureScopeOrgUnitWhenOuModeCapture()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit captureScopeOrgUnit = createOrgUnit("captureScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(captureScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);

    EventOperationParams operationParams =
        EventOperationParams.builder().programUid(program.getUid()).orgUnitMode(CAPTURE).build();

    EventSearchParams searchParams = mapper.map(operationParams);

    assertContainsOnly(List.of(captureScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapSearchScopeOrgUnitWhenOuModeAccessible()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);

    EventOperationParams operationParams = eventBuilder.programUid(program.getUid()).build();

    EventSearchParams searchParams = mapper.map(operationParams);

    assertContainsOnly(List.of(searchScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapRequestedOrgUnitWhenOuModeSelected()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setUid(PROGRAM_UID);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(programService.getProgram(PROGRAM_UID)).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);
    when(currentUserService.getCurrentUser()).thenReturn(user);
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);
    when(trackerAccessManager.canAccess(user, program, orgUnit)).thenReturn(true);

    EventOperationParams operationParams =
        EventOperationParams.builder()
            .programUid(program.getUid())
            .orgUnitUid(orgUnit.getUid())
            .orgUnitMode(SELECTED)
            .build();

    EventSearchParams searchParams = mapper.map(operationParams);

    assertContainsOnly(List.of(orgUnit), searchParams.getAccessibleOrgUnits());
  }

  @Test
  void shouldMapRequestedOrgUnitAsAccessibleWhenNoOrgUnitProvidedAndNoOrgUnitModeProvided()
      throws ForbiddenException, BadRequestException {
    Program program = new Program();
    program.setAccessLevel(OPEN);
    OrganisationUnit searchScopeOrgUnit = createOrgUnit("searchScopeOrgUnit", "uid4");
    User user = new User();
    user.setOrganisationUnits(Set.of(searchScopeOrgUnit));

    when(currentUserService.getCurrentUser()).thenReturn(user);

    EventOperationParams operationParams = eventBuilder.programUid(program.getUid()).build();

    EventSearchParams searchParams = mapper.map(operationParams);

    assertEquals(ACCESSIBLE, searchParams.getOrgUnitMode());
    assertContainsOnly(List.of(searchScopeOrgUnit), searchParams.getAccessibleOrgUnits());
  }

  private OrganisationUnit createOrgUnit(String name, String uid) {
    OrganisationUnit orgUnit = new OrganisationUnit(name);
    orgUnit.setUid(uid);
    return orgUnit;
  }
}
