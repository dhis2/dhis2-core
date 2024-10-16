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
package org.hisp.dhis.webapi.controller.tracker.export.trackedentity;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.ACCESSIBLE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.CAPTURE;
import static org.hisp.dhis.common.OrganisationUnitSelectionMode.SELECTED;
import static org.hisp.dhis.test.utils.Assertions.assertContains;
import static org.hisp.dhis.test.utils.Assertions.assertContainsOnly;
import static org.hisp.dhis.test.utils.Assertions.assertIsEmpty;
import static org.hisp.dhis.test.utils.Assertions.assertStartsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hisp.dhis.common.AssignedUserSelectionMode;
import org.hisp.dhis.common.QueryFilter;
import org.hisp.dhis.common.QueryOperator;
import org.hisp.dhis.common.SortDirection;
import org.hisp.dhis.common.UID;
import org.hisp.dhis.event.EventStatus;
import org.hisp.dhis.feedback.BadRequestException;
import org.hisp.dhis.program.EnrollmentStatus;
import org.hisp.dhis.tracker.export.Order;
import org.hisp.dhis.tracker.export.trackedentity.TrackedEntityOperationParams;
import org.hisp.dhis.user.SystemUser;
import org.hisp.dhis.user.UserDetails;
import org.hisp.dhis.webapi.controller.event.webrequest.OrderCriteria;
import org.hisp.dhis.webapi.webdomain.EndDateTime;
import org.hisp.dhis.webapi.webdomain.StartDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackedEntityRequestParamsMapperTest {
  public static final String TEA_1_UID = "TvjwTPToKHO";

  public static final String TEA_2_UID = "cy2oRh2sNr6";

  private static final String PROGRAM_UID = "XhBYIraw7sv";

  private static final String PROGRAM_STAGE_UID = "RpCr2u2pFqw";

  private static final String TRACKED_ENTITY_TYPE_UID = "Dp8baZYrLtr";

  @Mock private TrackedEntityFieldsParamMapper fieldsParamMapper;

  @InjectMocks private TrackedEntityRequestParamsMapper mapper;

  private TrackedEntityRequestParams trackedEntityRequestParams;

  UserDetails user = new SystemUser();

  @BeforeEach
  public void setUp() {
    trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setAssignedUserMode(AssignedUserSelectionMode.CURRENT);
  }

  @Test
  void shouldMapCorrectlyWhenProgramAndSpecificUpdateDatesSupplied() throws BadRequestException {
    trackedEntityRequestParams.setOuMode(CAPTURE);
    trackedEntityRequestParams.setEnrollmentStatus(EnrollmentStatus.ACTIVE);
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));
    trackedEntityRequestParams.setProgramStage(UID.of(PROGRAM_STAGE_UID));
    trackedEntityRequestParams.setFollowUp(true);
    trackedEntityRequestParams.setUpdatedAfter(StartDateTime.of("2019-01-01"));
    trackedEntityRequestParams.setUpdatedBefore(EndDateTime.of("2020-01-01"));
    trackedEntityRequestParams.setEnrollmentOccurredAfter(StartDateTime.of("2019-05-05"));
    trackedEntityRequestParams.setEnrollmentOccurredBefore(EndDateTime.of("2020-05-05"));
    trackedEntityRequestParams.setEventStatus(EventStatus.COMPLETED);
    trackedEntityRequestParams.setEventOccurredAfter(StartDateTime.of("2019-07-07"));
    trackedEntityRequestParams.setEventOccurredBefore(EndDateTime.of("2020-07-07"));
    trackedEntityRequestParams.setIncludeDeleted(true);

    final TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertThat(params.getProgramUid(), is(PROGRAM_UID));
    assertThat(params.getProgramStageUid(), is(PROGRAM_STAGE_UID));
    assertThat(params.getFollowUp(), is(true));
    assertThat(
        params.getLastUpdatedStartDate(),
        is(trackedEntityRequestParams.getUpdatedAfter().toDate()));
    assertThat(
        params.getLastUpdatedEndDate(), is(trackedEntityRequestParams.getUpdatedBefore().toDate()));
    assertThat(
        params.getProgramIncidentStartDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredAfter().toDate()));
    assertThat(
        params.getProgramIncidentEndDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredBefore().toDate()));
    assertThat(params.getEventStatus(), is(EventStatus.COMPLETED));
    assertThat(
        params.getEventStartDate(),
        is(trackedEntityRequestParams.getEventOccurredAfter().toDate()));
    assertThat(
        params.getEventEndDate(), is(trackedEntityRequestParams.getEventOccurredBefore().toDate()));
    assertThat(
        params.getAssignedUserQueryParam().getMode(), is(AssignedUserSelectionMode.PROVIDED));
    assertThat(params.isIncludeDeleted(), is(true));
  }

  @Test
  void shouldMapCorrectlyWhenTrackedEntityAndSpecificUpdatedRangeSupplied()
      throws BadRequestException {
    trackedEntityRequestParams.setOuMode(CAPTURE);
    trackedEntityRequestParams.setUpdatedWithin("20h");
    trackedEntityRequestParams.setTrackedEntityType(UID.of(TRACKED_ENTITY_TYPE_UID));
    trackedEntityRequestParams.setEventStatus(EventStatus.COMPLETED);
    trackedEntityRequestParams.setEventOccurredAfter(StartDateTime.of("2019-07-07"));
    trackedEntityRequestParams.setEventOccurredBefore(EndDateTime.of("2020-07-07"));
    trackedEntityRequestParams.setIncludeDeleted(true);

    final TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertThat(params.getTrackedEntityTypeUid(), is(TRACKED_ENTITY_TYPE_UID));
    assertThat(params.getLastUpdatedStartDate(), is(trackedEntityRequestParams.getUpdatedAfter()));
    assertThat(params.getLastUpdatedEndDate(), is(trackedEntityRequestParams.getUpdatedBefore()));
    assertThat(
        params.getProgramIncidentStartDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredAfter()));
    assertThat(
        params.getProgramIncidentEndDate(),
        is(trackedEntityRequestParams.getEnrollmentOccurredBefore()));
    assertThat(params.getEventStatus(), is(EventStatus.COMPLETED));
    assertThat(
        params.getEventStartDate(),
        is(trackedEntityRequestParams.getEventOccurredAfter().toDate()));
    assertThat(
        params.getEventEndDate(), is(trackedEntityRequestParams.getEventOccurredBefore().toDate()));
    assertThat(
        params.getAssignedUserQueryParam().getMode(), is(AssignedUserSelectionMode.PROVIDED));
    assertThat(params.isIncludeDeleted(), is(true));
  }

  @Test
  void shouldMapOrgUnitModeGivenOrgUnitModeParam() throws BadRequestException {
    trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setOrgUnitMode(CAPTURE);
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, null, user);

    assertEquals(CAPTURE, params.getOrgUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeGivenOuModeParam() throws BadRequestException {
    trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setOuMode(CAPTURE);
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, null, user);

    assertEquals(CAPTURE, params.getOrgUnitMode());
  }

  @Test
  void shouldMapOrgUnitModeToDefaultGivenNoOrgUnitModeParamIsSet() throws BadRequestException {
    trackedEntityRequestParams = new TrackedEntityRequestParams();
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, null, user);

    assertEquals(ACCESSIBLE, params.getOrgUnitMode());
  }

  @Test
  void shouldThrowIfDeprecatedAndNewOrgUnitModeParameterIsSet() {
    trackedEntityRequestParams.setOuMode(SELECTED);
    trackedEntityRequestParams.setOrgUnitMode(SELECTED);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, null, user));

    assertStartsWith("Only one parameter of 'ouMode' and 'orgUnitMode'", exception.getMessage());
  }

  @Test
  void shouldFailIfDeprecatedAndNewEnrollmentStatusParameterIsSet() {
    trackedEntityRequestParams.setProgramStatus(EnrollmentStatus.ACTIVE);
    trackedEntityRequestParams.setEnrollmentStatus(EnrollmentStatus.ACTIVE);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, null, user));

    assertStartsWith(
        "Only one parameter of 'programStatus' and 'enrollmentStatus'", exception.getMessage());
  }

  @Test
  void testMappingProgramEnrollmentStartDate() throws BadRequestException {
    StartDateTime startDate = StartDateTime.of("2022-12-13");
    trackedEntityRequestParams.setEnrollmentEnrolledAfter(startDate);
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(startDate.toDate(), params.getProgramEnrollmentStartDate());
  }

  @Test
  void testMappingProgram() throws BadRequestException {
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(PROGRAM_UID, params.getProgramUid());
  }

  @Test
  void testMappingProgramStage() throws BadRequestException {
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));
    trackedEntityRequestParams.setProgramStage(UID.of(PROGRAM_STAGE_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(PROGRAM_STAGE_UID, params.getProgramStageUid());
  }

  @Test
  void testMappingTrackedEntityType() throws BadRequestException {
    trackedEntityRequestParams.setTrackedEntityType(UID.of(TRACKED_ENTITY_TYPE_UID));
    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(TRACKED_ENTITY_TYPE_UID, params.getTrackedEntityTypeUid());
  }

  @Test
  void testMappingAssignedUser() throws BadRequestException {
    trackedEntityRequestParams.setAssignedUser("IsdLBTOBzMi;l5ab8q5skbB");
    trackedEntityRequestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertContainsOnly(
        Set.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void testMappingAssignedUsers() throws BadRequestException {
    trackedEntityRequestParams.setAssignedUsers(
        Set.of(UID.of("IsdLBTOBzMi"), UID.of("l5ab8q5skbB")));
    trackedEntityRequestParams.setAssignedUserMode(AssignedUserSelectionMode.PROVIDED);
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertContainsOnly(
        Set.of("IsdLBTOBzMi", "l5ab8q5skbB"),
        params.getAssignedUserQueryParam().getAssignedUsers());
    assertEquals(AssignedUserSelectionMode.PROVIDED, params.getAssignedUserQueryParam().getMode());
  }

  @Test
  void shouldFailIfProgramStatusIsSetWithoutProgram() {
    trackedEntityRequestParams.setTrackedEntityType(UID.of(TRACKED_ENTITY_TYPE_UID));
    trackedEntityRequestParams.setProgramStatus(EnrollmentStatus.ACTIVE);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, null, user));

    assertStartsWith("`program` must be defined when `programStatus`", exception.getMessage());
  }

  @Test
  void shouldFailIfEnrollmentStatusIsSetWithoutProgram() {
    trackedEntityRequestParams.setTrackedEntityType(UID.of(TRACKED_ENTITY_TYPE_UID));
    trackedEntityRequestParams.setEnrollmentStatus(EnrollmentStatus.ACTIVE);

    BadRequestException exception =
        assertThrows(
            BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, null, user));

    assertStartsWith("`program` must be defined when `enrollmentStatus`", exception.getMessage());
  }

  @Test
  void shouldFailIfGivenStatusAndNotOccurredEventDates() {
    trackedEntityRequestParams.setEventStatus(EventStatus.ACTIVE);

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenStatusAndOccurredAfterEventDateButNoOccurredBeforeEventDate() {
    trackedEntityRequestParams.setEventStatus(EventStatus.ACTIVE);
    trackedEntityRequestParams.setEventOccurredAfter(StartDateTime.of("2020-10-10"));

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenOccurredEventDatesAndNotEventStatus() {
    trackedEntityRequestParams.setEventOccurredBefore(EndDateTime.of("2020-11-11"));
    trackedEntityRequestParams.setEventOccurredAfter(StartDateTime.of("2020-10-10"));

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenOrgUnitAndOrgUnits() {
    trackedEntityRequestParams.setOrgUnit("IsdLBTOBzMi");
    trackedEntityRequestParams.setOrgUnits(Set.of(UID.of("IsdLBTOBzMi")));

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenTrackedEntityAndTrackedEntities() {
    trackedEntityRequestParams.setTrackedEntity("IsdLBTOBzMi");
    trackedEntityRequestParams.setTrackedEntities(Set.of(UID.of("IsdLBTOBzMi")));

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenRemovedQueryParameter() {
    trackedEntityRequestParams.setQuery("query");

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenRemovedAttributeParameter() {
    trackedEntityRequestParams.setAttribute("IsdLBTOBzMi");

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldFailIfGivenRemovedIncludeAllAttributesParameter() {
    trackedEntityRequestParams.setIncludeAllAttributes("true");

    assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
  }

  @Test
  void shouldMapOrderParameterInGivenOrderWhenFieldsAreOrderable() throws BadRequestException {
    trackedEntityRequestParams.setOrder(
        OrderCriteria.fromOrderString("createdAt:asc,zGlzbfreTOH,enrolledAt:desc"));
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertEquals(
        List.of(
            new Order("created", SortDirection.ASC),
            new Order(UID.of("zGlzbfreTOH"), SortDirection.ASC),
            new Order("enrollment.enrollmentDate", SortDirection.DESC)),
        params.getOrder());
  }

  @Test
  void testMappingOrderParamsNoOrder() throws BadRequestException {
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    TrackedEntityOperationParams params = mapper.map(trackedEntityRequestParams, user);

    assertIsEmpty(params.getOrder());
  }

  @Test
  void shouldFailGivenInvalidOrderFieldName() {
    trackedEntityRequestParams.setOrder(
        OrderCriteria.fromOrderString("unsupportedProperty1:asc,enrolledAt:asc"));

    Exception exception =
        assertThrows(BadRequestException.class, () -> mapper.map(trackedEntityRequestParams, user));
    assertAll(
        () -> assertStartsWith("order parameter is invalid", exception.getMessage()),
        () -> assertContains("unsupportedProperty1", exception.getMessage()));
  }

  @Test
  void shouldMapFilterParameter() throws BadRequestException {
    trackedEntityRequestParams.setOrgUnitMode(ACCESSIBLE);
    trackedEntityRequestParams.setFilter(TEA_1_UID + ":like:value1," + TEA_2_UID + ":like:value2");
    trackedEntityRequestParams.setProgram(UID.of(PROGRAM_UID));

    Map<String, List<QueryFilter>> filters =
        mapper.map(trackedEntityRequestParams, user).getFilters();

    assertEquals(
        Map.of(
            TEA_1_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "value1")),
            TEA_2_UID,
            List.of(new QueryFilter(QueryOperator.LIKE, "value2"))),
        filters);
  }
}
