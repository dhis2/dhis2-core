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
package org.hisp.dhis.webapi.controller.tracker.export.event;

import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.hisp.dhis.tracker.export.trackedentity.TrackedEntityParams.FALSE;
import static org.hisp.dhis.webapi.controller.tracker.export.FieldsParamMapper.FIELD_RELATIONSHIPS;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.OrganisationUnitSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.dataelement.DataElement;
import org.hisp.dhis.dataelement.DataElementService;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.feedback.ForbiddenException;
import org.hisp.dhis.feedback.NotFoundException;
import org.hisp.dhis.fieldfiltering.better.Fields;
import org.hisp.dhis.fieldfiltering.better.FieldsParser;
import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.program.Program;
import org.hisp.dhis.program.ProgramService;
import org.hisp.dhis.program.ProgramStage;
import org.hisp.dhis.program.ProgramStageService;
import org.hisp.dhis.security.acl.AclService;
import org.hisp.dhis.trackedentity.TrackedEntity;
import org.hisp.dhis.trackedentity.TrackedEntityAttribute;
import org.hisp.dhis.trackedentity.TrackedEntityAttributeService;
import org.hisp.dhis.tracker.TrackerIdSchemeParams;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.event.EventOperationParams;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityService;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserService;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT) // common setup
@ExtendWith(MockitoExtension.class)
class EventRequestParamsMapperTest {

  private static final UID DE_1_UID = UID.of("OBzmpRP6YUh");

  private static final UID DE_2_UID = UID.of("KSd4PejqBf9");

  private static final UID TEA_1_UID = UID.of("TvjwTPToKHO");

  private static final UID TEA_2_UID = UID.of("cy2oRh2sNr6");

  private static final UID PROGRAM_UID = UID.of("PlZSBEN7iZd");

  @Mock private UserService userService;

  @Mock private ProgramService programService;

  @Mock private OrganisationUnitService organisationUnitService;

  @Mock private ProgramStageService programStageService;

  @Mock private AclService aclService;

  @Mock private TrackedEntityService trackedEntityService;

  @Mock private TrackedEntityAttributeService attributeService;

  @Mock private DataElementService dataElementService;

  @InjectMocks private EventRequestParamsMapper mapper;

  private Program program;

  private OrganisationUnit orgUnit;

  private TrackerIdSchemeParams idSchemeParams;

  @BeforeEach
  void setUp() throws ForbiddenException, NotFoundException {
    User user = new User();

    when(userService.getUserByUsername(null)).thenReturn(user);

    program = new Program();
    program.setUid(PROGRAM_UID.getValue());
    when(programService.getProgram(PROGRAM_UID.getValue())).thenReturn(program);
    when(aclService.canDataRead(user, program)).thenReturn(true);

    ProgramStage programStage = new ProgramStage();
    programStage.setUid("PlZSBEN7iZd");
    when(programStageService.getProgramStage("PlZSBEN7iZd")).thenReturn(programStage);
    when(aclService.canDataRead(user, programStage)).thenReturn(true);

    orgUnit = new OrganisationUnit();
    when(organisationUnitService.getOrganisationUnit(any())).thenReturn(orgUnit);
    when(organisationUnitService.isInUserHierarchy(user, orgUnit)).thenReturn(true);

    TrackedEntity trackedEntity = new TrackedEntity();
    when(trackedEntityService.getTrackedEntity(UID.of("qnR1RK4cTIZ"), null, FALSE))
        .thenReturn(trackedEntity);
    TrackedEntityAttribute tea1 = new TrackedEntityAttribute();
    tea1.setUid(TEA_1_UID.getValue());
    TrackedEntityAttribute tea2 = new TrackedEntityAttribute();
    tea2.setUid(TEA_2_UID.getValue());
    when(attributeService.getAllTrackedEntityAttributes()).thenReturn(List.of(tea1, tea2));
    when(attributeService.getTrackedEntityAttribute(TEA_1_UID.getValue())).thenReturn(tea1);

    DataElement de1 = new DataElement();
    de1.setUid(DE_1_UID.getValue());
    when(dataElementService.getDataElement(DE_1_UID.getValue())).thenReturn(de1);
    DataElement de2 = new DataElement();
    de2.setUid(DE_2_UID.getValue());
    when(dataElementService.getDataElement(DE_2_UID.getValue())).thenReturn(de2);

    idSchemeParams = TrackerIdSchemeParams.builder().build();
  }

  @Test
  void testMappingDoesNotFetchOptionalEmptyQueryParametersFromDB() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    mapper.map(eventRequestParams, idSchemeParams);

    verifyNoInteractions(programService);
    verifyNoInteractions(programStageService);
    verifyNoInteractions(organisationUnitService);
    verifyNoInteractions(trackedEntityService);
  }

  @Test
  void testMappingProgram() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgram(PROGRAM_UID);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(UID.of(program), params.getProgram());
  }

  @Test
  void shouldMapOrgUnitModeGivenOrgUnitModeParam() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setOrgUnit(UID.of(orgUnit));
    eventRequestParams.setOrgUnitMode(SELECTED);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(SELECTED, params.getOrgUnitMode());
  }

  @Test
  void shouldFailIfDeprecatedAndNewEnrollmentStatusParameterIsSet() {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setProgramStatus(EnrollmentStatus.ACTIVE);
    eventRequestParams.setEnrollmentStatus(EnrollmentStatus.ACTIVE);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(eventRequestParams, idSchemeParams));

    assertStartsWith(
        "Only one parameter of 'programStatus' and 'enrollmentStatus'", exception.getMessage());
  }

  @Test
  void shouldReturnOrgUnitWhenCorrectOrgUnitMapped() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setOrgUnit(UID.of(orgUnit));

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(UID.of(orgUnit), params.getOrgUnit());
  }

  @Test
  void testMappingTrackedEntity() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setTrackedEntity(UID.of("qnR1RK4cTIZ"));

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(UID.of("qnR1RK4cTIZ"), params.getTrackedEntity());
  }

  @Test
  void testMappingOccurredAfterBefore() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    StartDateTime occurredAfter = StartDateTime.of("2020-01-01");
    eventRequestParams.setOccurredAfter(occurredAfter);
    EndDateTime occurredBefore = EndDateTime.of("2020-09-12");
    eventRequestParams.setOccurredBefore(occurredBefore);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(occurredAfter.toDate(), params.getOccurredAfter());
    assertEquals(occurredBefore.toDate(), params.getOccurredBefore());
  }

  @Test
  void testMappingScheduledAfterBefore() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    StartDateTime scheduledAfter = StartDateTime.of("2021-01-01");
    eventRequestParams.setScheduledAfter(scheduledAfter);
    EndDateTime scheduledBefore = EndDateTime.of("2021-09-12");
    eventRequestParams.setScheduledBefore(scheduledBefore);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(scheduledAfter.toDate(), params.getScheduledAfter());
    assertEquals(scheduledBefore.toDate(), params.getScheduledBefore());
  }

  @Test
  void shouldMapAfterAndBeforeDatesWhenSupplied() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    StartDateTime updatedAfter = StartDateTime.of("2022-01-01");
    eventRequestParams.setUpdatedAfter(updatedAfter);
    EndDateTime updatedBefore = EndDateTime.of("2022-09-12");
    eventRequestParams.setUpdatedBefore(updatedBefore);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(updatedAfter.toDate(), params.getUpdatedAfter());
    assertEquals(updatedBefore.toDate(), params.getUpdatedBefore());
  }

  @Test
  void shouldMapUpdatedWithinDateWhenSupplied() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    String updatedWithin = "6m";
    eventRequestParams.setUpdatedWithin(updatedWithin);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(updatedWithin, params.getUpdatedWithin());
  }

  @Test
  void shouldFailWithBadRequestExceptionWhenTryingToMapAllUpdateDatesTogether() {
    EventRequestParams eventRequestParams = new EventRequestParams();

    StartDateTime updatedAfter = StartDateTime.of("2022-01-01");
    eventRequestParams.setUpdatedAfter(updatedAfter);
    EndDateTime updatedBefore = EndDateTime.of("2022-09-12");
    eventRequestParams.setUpdatedBefore(updatedBefore);
    String updatedWithin = "P6M";
    eventRequestParams.setUpdatedWithin(updatedWithin);

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(eventRequestParams, idSchemeParams));

    assertEquals(
        "Last updated from and/or to and last updated duration cannot be specified simultaneously",
        exception.getMessage());
  }

  @Test
  void testMappingEnrollmentEnrolledAtDates() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    EndDateTime enrolledBefore = EndDateTime.of("2022-01-01");
    eventRequestParams.setEnrollmentEnrolledBefore(enrolledBefore);
    StartDateTime enrolledAfter = StartDateTime.of("2022-02-01");
    eventRequestParams.setEnrollmentEnrolledAfter(enrolledAfter);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(enrolledBefore.toDate(), params.getEnrollmentEnrolledBefore());
    assertEquals(enrolledAfter.toDate(), params.getEnrollmentEnrolledAfter());
  }

  @Test
  void testMappingEnrollmentOccurredAtDates() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    EndDateTime enrolledBefore = EndDateTime.of("2022-01-01");
    eventRequestParams.setEnrollmentOccurredBefore(enrolledBefore);
    StartDateTime enrolledAfter = StartDateTime.of("2022-02-01");
    eventRequestParams.setEnrollmentOccurredAfter(enrolledAfter);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(enrolledBefore.toDate(), params.getEnrollmentOccurredBefore());
    assertEquals(enrolledAfter.toDate(), params.getEnrollmentOccurredAfter());
  }

  @Test
  void testMappingEnrollments() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    eventRequestParams.setEnrollments(Set.of(UID.of("NQnuK2kLm6e")));

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(Set.of(UID.of("NQnuK2kLm6e")), params.getEnrollments());
  }

  @Test
  void testMappingEvents() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setEvents(UID.of("XKrcfuM4Hcw", "M4pNmLabtXl"));

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(UID.of("XKrcfuM4Hcw", "M4pNmLabtXl"), params.getEvents());
  }

  @Test
  void testMappingEventIsNull() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertIsEmpty(params.getEvents());
  }

  @Test
  void testMappingAssignedUsers() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setAssignedUsers(UID.of("IsdLBTOBzMi", "l5ab8q5skbB"));
    eventRequestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertContainsOnly(UID.of("IsdLBTOBzMi", "l5ab8q5skbB"), params.getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserMode());
  }

  @Test
  void testMutualExclusionOfEventsAndFilter() {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setFilter(DE_1_UID + ":ge:1:le:2");
    eventRequestParams.setEvents(UID.of("XKrcfuM4Hcw", "M4pNmLabtXl"));

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(eventRequestParams, idSchemeParams));
    assertEquals(
        "Event UIDs and filters can not be specified at the same time", exception.getMessage());
  }

  @Test
  void shouldMapDataElementFilters() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setFilter(DE_1_UID + ":eq:2," + DE_2_UID + ":like:foo");

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> dataElementFilters = params.getDataElementFilters();
    assertNotNull(dataElementFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(
            DE_1_UID,
            List.of(new QueryFilter(QueryOperator.EQ, "2")),
            DE_2_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo")));
    assertEquals(expected, dataElementFilters);
  }

  @Test
  void shouldMapDataElementFiltersWhenDataElementHasMultipleFilters() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setFilter(DE_1_UID + ":gt:10:lt:20");

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> dataElementFilters = params.getDataElementFilters();
    assertNotNull(dataElementFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(
            DE_1_UID,
            List.of(
                new QueryFilter(QueryOperator.GT, "10"), new QueryFilter(QueryOperator.LT, "20")));
    assertEquals(expected, dataElementFilters);
  }

  @Test
  void shouldMapDataElementFiltersWhenQueryFilterHasUIDOnly() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setFilter(DE_1_UID.getValue());

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> dataElementFilters = params.getDataElementFilters();
    assertNotNull(dataElementFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(DE_1_UID, List.of(new QueryFilter(QueryOperator.NNULL)));
    assertEquals(expected, dataElementFilters);
  }

  @Test
  void shouldMapDataElementFiltersToDefaultIfNoneSet() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> dataElementFilters = params.getDataElementFilters();

    assertNotNull(dataElementFilters);
    assertTrue(dataElementFilters.isEmpty());
  }

  @Test
  void shouldMapAttributeFilters() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setFilterAttributes(TEA_1_UID + ":eq:2," + TEA_2_UID + ":like:foo");

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> attributeFilters = params.getAttributeFilters();
    assertNotNull(attributeFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(
            TEA_1_UID,
            List.of(new QueryFilter(QueryOperator.EQ, "2")),
            TEA_2_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "foo")));
    assertEquals(expected, attributeFilters);
  }

  @Test
  void shouldMapAttributeFiltersWhenAttributeHasMultipleFilters() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setFilterAttributes(TEA_1_UID + ":gt:10:lt:20");

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> attributeFilters = params.getAttributeFilters();
    assertNotNull(attributeFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(
            TEA_1_UID,
            List.of(
                new QueryFilter(QueryOperator.GT, "10"), new QueryFilter(QueryOperator.LT, "20")));
    assertEquals(expected, attributeFilters);
  }

  @Test
  void shouldMapAttributeFiltersWhenOnlyGivenUID() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setFilterAttributes(TEA_1_UID.getValue());

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> attributeFilters = params.getAttributeFilters();
    assertNotNull(attributeFilters);
    Map<UID, List<QueryFilter>> expected =
        Map.of(TEA_1_UID, List.of(new QueryFilter(QueryOperator.NNULL)));
    assertEquals(expected, attributeFilters);
  }

  @Test
  void shouldMapAttributeFiltersToDefaultIfNoneSet() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    Map<UID, List<QueryFilter>> attributeFilters = params.getAttributeFilters();

    assertNotNull(attributeFilters);
    assertTrue(attributeFilters.isEmpty());
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setOrder(
        OrderCriteria.fromOrderString(
            "createdAt:asc,zGlzbfreTOH,programStage:desc,scheduledAt:asc"));

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(UID.of("zGlzbfreTOH"), SortDirection.ASC),
            new Order("programStage.uid", SortDirection.DESC),
            new Order("scheduledDate", SortDirection.ASC)),
        params.getOrder());
  }

  @Test
  void shouldFailGivenInvalidOrderFieldName() {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setOrder(
        OrderCriteria.fromOrderString("unsupportedProperty1:asc,enrolledAt:asc"));

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(eventRequestParams, idSchemeParams));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }

  @Test
  void shouldMapSelectedOrgUnitModeWhenOrgUnitModeNotProvided() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setOrgUnit(UID.of(orgUnit));

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(SELECTED, params.getOrgUnitMode());
  }

  @Test
  void shouldMapAccessibleOrgUnitModeWhenOrgUnitModeNorOrgUnitProvided()
      throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();

    EventOperationParams params = mapper.map(eventRequestParams, idSchemeParams);

    assertEquals(ACCESSIBLE, params.getOrgUnitMode());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"ACCESSIBLE", "CAPTURE"})
  void shouldFailWhenOrgUnitSuppliedAndOrgUnitModeCannotHaveOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    when(organisationUnitService.getOrganisationUnit(orgUnit.getUid())).thenReturn(orgUnit);

    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setOrgUnit(UID.of(orgUnit));
    eventRequestParams.setOrgUnitMode(orgUnitMode);

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(eventRequestParams, idSchemeParams));

    assertStartsWith(
        "orgUnitMode " + orgUnitMode + " cannot be used with orgUnits.", exception.getMessage());
  }

  @ParameterizedTest
  @EnumSource(
      value = OrganisationUnitSelectionMode.class,
      names = {"SELECTED", "DESCENDANTS", "CHILDREN"})
  void shouldFailWhenNoOrgUnitSuppliedAndOrgUnitModeNeedsOrgUnit(
      OrganisationUnitSelectionMode orgUnitMode) {
    EventRequestParams eventRequestParams = new EventRequestParams();
    eventRequestParams.setOrgUnitMode(orgUnitMode);

    Exception exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(eventRequestParams, idSchemeParams));

    assertStartsWith(
        "At least one org unit is required for orgUnitMode: " + orgUnitMode,
        exception.getMessage());
  }

  @Test
  void shouldMapAndIncludeRelationshipsIfInFields() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    Fields fields = FieldsParser.parse(FIELD_RELATIONSHIPS);
    eventRequestParams.setFields(fields);

    EventOperationParams eventOperationParams = mapper.map(eventRequestParams, idSchemeParams);

    assertTrue(eventOperationParams.getFields().isIncludesRelationships());
  }

  @Test
  void shouldMapAndIncludeRelationshipsIfNotInFields() throws BadRequestException {
    EventRequestParams eventRequestParams = new EventRequestParams();
    Fields fields = FieldsParser.parse("event");
    eventRequestParams.setFields(fields);

    EventOperationParams eventOperationParams = mapper.map(eventRequestParams, idSchemeParams);

    assertFalse(eventOperationParams.getFields().isIncludesRelationships());
  }
}
